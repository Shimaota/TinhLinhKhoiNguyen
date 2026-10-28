package com.tlnt.server.account;

import com.tlnt.server.captcha.CaptchaService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Đăng nhập, đăng ký, mật khẩu cấp 2 và danh sách tài khoản đang online.
 *
 * Đăng nhập trùng: A đang chơi, B đăng nhập cùng tài khoản -> A bị kick ("Tài khoản được đăng nhập từ máy
 * khác!") + lưu dữ liệu; B nhận "Tài khoản đang có người khác đăng nhập" và phải chờ
 * {@code reloginDelayMs} (mặc định 10s) mới đăng nhập lại được. Khoảng chờ này cũng cho lượt lưu của A
 * chạy xong trước khi B load lại nhân vật.
 */
public final class AccountService {

   private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_]{4,20}$");
   private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
   private static final int PASSWORD_MIN = 6;
   private static final int PASSWORD_MAX = 32;
   private static final int SECONDARY_MIN = 4;
   private static final int SECONDARY_MAX = 20;
   private static final int EMAIL_MAX = 128;
   /** Sai mật khẩu (hoặc MK cấp 2) quá số lần này trong 1 kết nối thì ngắt kết nối. */
   private static final int MAX_FAILED_ATTEMPTS = 5;
   /** Chờ trước khi đóng kết nối bị kick để client kịp hiện thông báo. */
   private static final long KICK_CLOSE_DELAY_MS = 2_000;

   static final String MSG_KICKED = "Tài khoản được đăng nhập từ máy khác!";
   static final String MSG_ACCOUNT_IN_USE = "Tài khoản đang có người khác đăng nhập";
   static final String MSG_MAINTENANCE = "Máy chủ đang bảo trì, dữ liệu của bạn đã được lưu. Vui lòng quay lại sau.";

   private final AccountRepository repository;
   private final PlayerService playerService;
   private final CaptchaService captchaService;
   private final ScheduledExecutorService scheduler;
   private final long reloginDelayMs;
   /** Pool chạy lượt đăng xuất của người bị kick (trên hàng đợi session của họ). */
   private final Executor gamePool;

   /** accountId -> session đang giữ tài khoản. */
   private final Map<Integer, PlayerSession> online = new ConcurrentHashMap<>();
   /** accountId -> thời điểm được đăng nhập lại (sau khi vừa kick). */
   private final Map<Integer, Long> reloginBlockedUntil = new ConcurrentHashMap<>();
   /** Chỉ bọc đoạn kiểm tra/chiếm chỗ trong {@link #online}, không bọc SQL. */
   private final Object loginLock = new Object();
   /**
    * Tài khoản admin panel đang sửa dữ liệu nhân vật OFFLINE trong SQL: chặn đăng nhập tới khi sửa xong (không thì lượt
    * nạp nhân vật lúc vào game đọc bản cũ rồi lượt lưu lúc thoát ghi đè thay đổi của admin). Chỉ đụng dưới {@link #loginLock}.
    */
   private final java.util.Set<Integer> adminHeld = new java.util.HashSet<>();
   private volatile boolean maintenance;
   private final LoginGuard guard;

   public AccountService(AccountRepository repository, PlayerService playerService, CaptchaService captchaService,
         ScheduledExecutorService scheduler, long reloginDelayMs, Executor gamePool, LoginGuard guard) {
      this.gamePool = gamePool;
      this.guard = guard;
      this.repository = repository;
      this.playerService = playerService;
      this.captchaService = captchaService;
      this.scheduler = scheduler;
      this.reloginDelayMs = reloginDelayMs;
   }

   // ------------------------------------------------------------------ đăng nhập

   public void login(PlayerSession session, String username, String password) throws SQLException {
      if (session.isAuthenticated()) {
         session.send(Packets.notice("Bạn đã đăng nhập rồi."));
         return;
      }
      if (isBlank(username) || isBlank(password)) {
         session.send(Packets.notice("Vui lòng nhập tài khoản và mật khẩu."));
         return;
      }

      if (maintenance) {
         session.send(Packets.notice(MSG_MAINTENANCE));
         return;
      }
      String ip = session.getRemoteIp();
      // Giới hạn theo IP / tài khoản TRƯỚC khi băm mật khẩu (PBKDF2 tốn CPU) — kết nối lại không xoá được bộ đếm.
      String blocked = guard.checkLogin(ip, username, System.currentTimeMillis());
      if (blocked != null) {
         session.send(Packets.notice(blocked));
         return;
      }
      Account account = repository.findByUsername(username.trim());
      if (account == null || !PasswordHasher.verify(password, account.getPasswordHash())) {
         guard.onLoginFailed(ip, username, System.currentTimeMillis());
         onFailedAttempt(session, "Tài khoản hoặc mật khẩu không đúng.");
         return;
      }
      guard.onLoginSucceeded(username);
      if (PasswordHasher.needsRehash(account.getPasswordHash())) {
         repository.updatePassword(account.getId(), PasswordHasher.hash(password)); // hash cũ 120.000 vòng -> số vòng mới
      }
      if (account.isBanned()) {
         String reason = isBlank(account.getBanReason()) ? "" : " Lý do: " + account.getBanReason();
         session.send(Packets.notice("Tài khoản đã bị khoá." + reason));
         return;
      }

      PlayerSession takeover = null; // session đang treo offline của chính tài khoản này (bị thay)
      synchronized (loginLock) {
         long now = System.currentTimeMillis();
         Long blockedUntil = reloginBlockedUntil.get(account.getId());
         if (blockedUntil != null) {
            if (blockedUntil > now) {
               long seconds = (blockedUntil - now + 999) / 1000;
               session.send(Packets.notice(MSG_ACCOUNT_IN_USE + ". Vui lòng thử lại sau " + seconds + " giây."));
               return;
            }
            reloginBlockedUntil.remove(account.getId());
         }

         if (adminHeld.contains(account.getId())) {
            session.send(Packets.notice("Tài khoản đang được quản trị viên xử lý, vui lòng thử lại sau ít giây."));
            return;
         }
         PlayerSession current = online.get(account.getId());
         if (current != null && current.isOfflineTraining() && offline != null) {
            // Đang treo offline: người chơi quay lại -> chiếm chỗ ngay (không chờ reloginDelayMs), dừng treo bên dưới.
            online.put(account.getId(), session);
            session.bindAccount(account);
            takeover = current;
         } else if (current != null) {
            kick(current, account);
            reloginBlockedUntil.put(account.getId(), now + reloginDelayMs);
            session.send(Packets.notice(MSG_ACCOUNT_IN_USE));
            return;
         } else {
            online.put(account.getId(), session);
            session.bindAccount(account);
         }
      }

      String offlineSummary = null;
      if (takeover != null) {
         // Lượt đăng xuất của nhân vật treo chạy trên hàng đợi CỦA NÓ (không đụng túi đồ song song); chờ tới khi lượt
         // lưu đã xếp hàng -> load bên dưới (awaitPendingSave) đọc đúng bản mới nhất.
         try {
            offlineSummary = offline.end(takeover).get(OFFLINE_TAKEOVER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
         } catch (Exception e) {
            Log.warn("Tai khoan %s: dung treo offline qua lau (%s)", account.getUsername(), e.toString());
            online.remove(account.getId(), session);
            session.detachAccount();
            session.send(Packets.notice("Nhân vật đang kết thúc luyện tập offline, vui lòng đăng nhập lại sau ít giây."));
            return;
         }
      }

      session.resetFailedLogin();
      Log.debug("Session #%d (%s) dang nhap tai khoan %s, online %d", session.getId(), session.getRemoteIp(),
            account.getUsername(), online.size());
      repository.updateLastLogin(account.getId(), session.getRemoteIp());
      session.send(Packets.accountState(account.hasSecondaryPassword(), account.isProtectionEnabled()));
      playerService.onAccountLoggedIn(session, account);
      if (offlineSummary != null) {
         session.send(Packets.notice(offlineSummary));
      }
   }

   // ------------------------------------------------------------------ treo offline

   /** Chờ tối đa bấy nhiêu ms nhân vật treo offline đăng xuất xong khi chủ tài khoản đăng nhập lại. */
   private static final long OFFLINE_TAKEOVER_TIMEOUT_MS = 5_000;

   /** Phía treo offline ({@code offline/OfflineTrainingService}) — nối lúc khởi động. */
   public interface OfflineHooks {
      /** Dừng treo + đăng xuất (lưu) session {@code offline} trên hàng đợi của nó. Future = câu tổng kết gửi người chơi. */
      java.util.concurrent.CompletableFuture<String> end(PlayerSession offline);

      /** Session đang treo vừa đăng xuất (bất kỳ đường nào: hết treo, admin kick, tắt server): dọn danh sách. */
      void loggedOut(PlayerSession offline);
   }

   private volatile OfflineHooks offline;

   public void setOfflineHooks(OfflineHooks hooks) {
      this.offline = hooks;
   }

   /** Bảo trì (đang tắt server): chặn đăng nhập mới. */
   public void setMaintenance(boolean maintenance) {
      this.maintenance = maintenance;
   }

   public boolean isMaintenance() {
      return maintenance;
   }

   /**
    * Admin panel: kick tài khoản {@code accountId} đang online (báo {@code notice}, lưu nhân vật như thoát game). @return
    * false nếu không online
    */
   public boolean adminKick(int accountId, String notice) {
      PlayerSession target = online.get(accountId);
      if (target == null) {
         return false;
      }
      Log.info("Admin kick tai khoan #%d (session #%d)", accountId, target.getId());
      target.send(Packets.notice(notice));
      target.closeAfter(scheduler, KICK_CLOSE_DELAY_MS);
      target.inbox().execute(() -> logout(target), gamePool);
      return true;
   }

   /**
    * Admin panel: giữ tài khoản {@code accountId} (chặn đăng nhập) để sửa nhân vật offline trong SQL. @return false nếu
    * tài khoản đang online / đang được giữ (người gọi không được sửa SQL)
    */
   public boolean adminHold(int accountId) {
      synchronized (loginLock) {
         return !online.containsKey(accountId) && adminHeld.add(accountId);
      }
   }

   /** Thả tài khoản đã {@link #adminHold}. */
   public void adminRelease(int accountId) {
      synchronized (loginLock) {
         adminHeld.remove(accountId);
      }
   }

   /** Tài khoản đang online (đã đăng nhập, có thể chưa vào thế giới). */
   public boolean isOnline(int accountId) {
      return online.containsKey(accountId);
   }

   /** Admin panel đổi vai trò: cập nhật luôn bản trong RAM nếu đang online (SQL do người gọi ghi). */
   public void adminSetRole(int accountId, int role) {
      PlayerSession s = online.get(accountId);
      Account account = s == null ? null : s.getAccount();
      if (account != null && account.getId() == accountId) {
         account.setRole(role);
      }
   }

   /** Admin panel đổi / xoá MK cấp 2: cập nhật bản trong RAM nếu đang online (SQL do người gọi ghi). */
   public void adminSetSecondary(int accountId, String hash, boolean protection) {
      PlayerSession s = online.get(accountId);
      Account account = s == null ? null : s.getAccount();
      if (account != null && account.getId() == accountId) {
         account.setSecondaryPasswordHash(hash);
         account.setProtectionEnabled(protection);
         s.send(Packets.accountState(hash != null, protection));
      }
   }

   /**
    * Tắt server: báo rồi đăng xuất 1 người (lưu nhân vật) trên hàng đợi của họ — như bị kick — và đóng kết nối sau
    * chút để client kịp hiện thông báo.
    */
   public void disconnectForMaintenance(PlayerSession target) {
      target.send(Packets.notice(MSG_MAINTENANCE));
      target.closeAfter(scheduler, KICK_CLOSE_DELAY_MS);
      target.inbox().execute(() -> logout(target), gamePool);
   }

   private void kick(PlayerSession target, Account account) {
      Log.info("Tai khoan %s dang nhap noi khac -> kick session #%d (%s)", account.getUsername(), target.getId(),
            target.getRemoteIp());
      target.send(Packets.notice(MSG_KICKED));
      target.closeAfter(scheduler, KICK_CLOSE_DELAY_MS); // CLOSING ngay: gói mới của người bị kick bị bỏ qua
      // Đăng xuất TRÊN hàng đợi của người bị kick (sau gói họ đang xử lý dở), không chạy song song từ luồng này ->
      // lượt lưu cuối không bao giờ chụp túi đồ giữa lúc đang đổi (gửi thư, kí gửi, dùng đồ...).
      target.inbox().execute(() -> logout(target), gamePool);
   }

   /** Thoát game / mất kết nối / bị kick. Gọi nhiều lần cho cùng session vẫn an toàn. */
   public void logout(PlayerSession session) {
      Account account = session.detachAccount();
      if (account == null) {
         return;
      }
      online.remove(account.getId(), session);
      PlayerData player = session.detachPlayer();
      if (player != null) {
         playerService.logout(player);
      }
      OfflineHooks hooks = offline;
      if (session.isOfflineTraining() && hooks != null) {
         hooks.loggedOut(session);
      }
      Log.debug("Tai khoan %s thoat (session #%d), online %d", account.getUsername(), session.getId(), online.size());
   }

   // ------------------------------------------------------------------ đăng ký (form có captcha)

   public void requestCaptcha(PlayerSession session) {
      if (!guard.allowCaptcha(session.getRemoteIp(), System.currentTimeMillis())) {
         session.send(Packets.notice("Bạn yêu cầu mã xác nhận quá nhiều lần, vui lòng đợi 1 phút."));
         return;
      }
      captchaService.issue(session);
   }

   public void register(PlayerSession session, String username, String password, String confirmPassword,
         String email, String captcha) throws SQLException {
      if (session.isAuthenticated()) {
         return;
      }
      if (!session.consumeCaptcha(captcha, System.currentTimeMillis())) {
         rejectRegister(session, "Mã xác nhận không đúng hoặc đã hết hạn.");
         return;
      }

      username = username == null ? "" : username.trim();
      email = email == null ? "" : email.trim();
      String error = validateRegister(username, password, confirmPassword, email);
      if (error != null) {
         rejectRegister(session, error);
         return;
      }

      int accountId = repository.insert(username, PasswordHasher.hash(password), email, session.getRemoteIp());
      if (accountId < 0) {
         rejectRegister(session, "Tài khoản đã tồn tại.");
         return;
      }
      Log.info("Dang ky tai khoan moi #%d '%s' tu %s", accountId, username, session.getRemoteIp());
      captchaService.reissue(session);
      session.send(Packets.notice("Đăng ký thành công! Hãy đăng nhập."));
   }

   private void rejectRegister(PlayerSession session, String message) {
      captchaService.reissue(session); // mã cũ đã bị huỷ, cấp ảnh mới để nhập lại
      session.send(Packets.notice(message));
   }

   /** Admin panel tạo tài khoản: cùng luật với form đăng ký. @return câu báo lỗi, null = hợp lệ */
   public static String validateNewAccount(String username, String password, String email) {
      return validateRegister(username, password, password, email);
   }

   /** Luật mật khẩu đăng nhập. @return câu báo lỗi, null = hợp lệ */
   public static String validatePassword(String password) {
      return password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX
            ? "Mật khẩu dài " + PASSWORD_MIN + "-" + PASSWORD_MAX + " ký tự." : null;
   }

   private static String validateRegister(String username, String password, String confirm, String email) {
      if (!USERNAME.matcher(username).matches()) {
         return "Tài khoản dài 4-20 ký tự, chỉ gồm chữ không dấu, số và dấu _.";
      }
      if (password == null || password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
         return "Mật khẩu dài " + PASSWORD_MIN + "-" + PASSWORD_MAX + " ký tự.";
      }
      if (!password.equals(confirm)) {
         return "Mật khẩu nhập lại không khớp.";
      }
      if (email.length() > EMAIL_MAX || !EMAIL.matcher(email).matches()) {
         return "Email không hợp lệ.";
      }
      return null;
   }

   // ------------------------------------------------------------------ mật khẩu cấp 2 / bảo vệ tài khoản

   /** Sub 1: tạo MK cấp 2, xác nhận bằng mật khẩu đăng nhập. */
   public void createSecondaryPassword(PlayerSession session, String accountPassword, String secondary,
         String confirm) throws SQLException {
      Account account = session.getAccount();
      if (account == null) {
         return;
      }
      if (account.hasSecondaryPassword()) {
         session.send(Packets.notice("Bạn đã có mật khẩu cấp 2."));
         return;
      }
      if (!PasswordHasher.verify(accountPassword, account.getPasswordHash())) {
         onFailedAttempt(session, "Mật khẩu hiện tại không đúng.");
         return;
      }
      String error = validateSecondary(secondary, confirm);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      setSecondaryPassword(account, secondary);
      sendAccountState(session, account, "Tạo mật khẩu cấp 2 thành công.");
   }

   /** Sub 2: đổi MK cấp 2. */
   public void changeSecondaryPassword(PlayerSession session, String current, String next, String confirm)
         throws SQLException {
      Account account = requireSecondary(session);
      if (account == null) {
         return;
      }
      if (!PasswordHasher.verify(current, account.getSecondaryPasswordHash())) {
         onFailedAttempt(session, "Mật khẩu cấp 2 không đúng.");
         return;
      }
      String error = validateSecondary(next, confirm);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      setSecondaryPassword(account, next);
      sendAccountState(session, account, "Đổi mật khẩu cấp 2 thành công.");
   }

   /** Sub 4: bật bảo vệ tài khoản (nút "Khóa"). */
   public void enableProtection(PlayerSession session) throws SQLException {
      Account account = requireSecondary(session);
      if (account == null || account.isProtectionEnabled()) {
         return;
      }
      setProtection(account, true);
      sendAccountState(session, account, "Tài khoản đã được bảo vệ.");
   }

   /** Sub 5: tắt bảo vệ, phải nhập đúng MK cấp 2 (nút "Mở khóa"). */
   public void disableProtection(PlayerSession session, String secondary) throws SQLException {
      Account account = requireSecondary(session);
      if (account == null || !account.isProtectionEnabled()) {
         return;
      }
      if (!PasswordHasher.verify(secondary, account.getSecondaryPasswordHash())) {
         onFailedAttempt(session, "Mật khẩu cấp 2 không đúng.");
         return;
      }
      setProtection(account, false);
      sendAccountState(session, account, "Đã tắt bảo vệ tài khoản.");
   }

   /**
    * Sub 6: "Xóa MK cấp 2". Decompile C3760 gửi không payload; client libGDX gửi thêm str MK cấp 2 (hỏi bằng hộp nhập)
    * — không có MK thì không xoá được (người lạ cầm máy không gỡ được lớp bảo vệ). Đang khoá thì phải mở khóa trước.
    */
   public void deleteSecondaryPassword(PlayerSession session, String secondary) throws SQLException {
      Account account = requireSecondary(session);
      if (account == null) {
         return;
      }
      if (account.isProtectionEnabled()) {
         session.send(Packets.notice("Tài khoản đang được bảo vệ, hãy Mở khóa trước khi xóa MK cấp 2."));
         return;
      }
      if (secondary == null) {
         session.send(Packets.notice("Cần nhập mật khẩu cấp 2 để xóa."));
         return;
      }
      if (!PasswordHasher.verify(secondary, account.getSecondaryPasswordHash())) {
         onFailedAttempt(session, "Mật khẩu cấp 2 không đúng.");
         return;
      }
      repository.updateSecondaryPassword(account.getId(), null);
      account.setSecondaryPasswordHash(null);
      setProtection(account, false);
      sendAccountState(session, account, "Đã xóa mật khẩu cấp 2.");
   }

   private Account requireSecondary(PlayerSession session) {
      Account account = session.getAccount();
      if (account != null && !account.hasSecondaryPassword()) {
         session.send(Packets.notice("Bạn chưa tạo mật khẩu cấp 2."));
         return null;
      }
      return account;
   }

   private void setSecondaryPassword(Account account, String secondary) throws SQLException {
      String hash = PasswordHasher.hash(secondary);
      repository.updateSecondaryPassword(account.getId(), hash);
      account.setSecondaryPasswordHash(hash);
   }

   private void setProtection(Account account, boolean enabled) throws SQLException {
      repository.updateProtection(account.getId(), enabled);
      account.setProtectionEnabled(enabled);
   }

   private static void sendAccountState(PlayerSession session, Account account, String message) {
      session.send(Packets.accountState(account.hasSecondaryPassword(), account.isProtectionEnabled()));
      session.send(Packets.notice(message));
   }

   /** Luật MK cấp 2 (admin panel đặt hộ). @return câu báo lỗi, null = hợp lệ */
   public static String validateSecondary(String secondary) {
      return validateSecondary(secondary, secondary);
   }

   private static String validateSecondary(String secondary, String confirm) {
      if (secondary == null || secondary.length() < SECONDARY_MIN || secondary.length() > SECONDARY_MAX) {
         return "Mật khẩu cấp 2 dài " + SECONDARY_MIN + "-" + SECONDARY_MAX + " ký tự.";
      }
      if (!secondary.equals(confirm)) {
         return "Mật khẩu cấp 2 nhập lại không khớp.";
      }
      return null;
   }

   // ------------------------------------------------------------------ tiện ích

   private void onFailedAttempt(PlayerSession session, String message) {
      if (session.incrementFailedLogin() >= MAX_FAILED_ATTEMPTS) {
         Log.warn("Session #%d (%s) nhap sai qua %d lan, ngat ket noi", session.getId(), session.getRemoteIp(),
               MAX_FAILED_ATTEMPTS);
         session.send(Packets.notice("Nhập sai quá nhiều lần, vui lòng thử lại sau."));
         logout(session);
         session.closeAfter(scheduler, KICK_CLOSE_DELAY_MS);
         return;
      }
      session.send(Packets.notice(message));
   }

   private static boolean isBlank(String value) {
      return value == null || value.isBlank();
   }

   public int onlineCount() {
      return online.size();
   }

   /**
    * Mỗi phút (luồng scheduler): bỏ mục "chờ đăng nhập lại" đã hết hạn (tài khoản bị kick mà không vào lại thì trước
    * đây nằm mãi trong RAM) + bộ đếm cũ của {@link LoginGuard}.
    */
   public void cleanup() {
      long now = System.currentTimeMillis();
      reloginBlockedUntil.values().removeIf(until -> until <= now);
      guard.cleanup(now);
   }

   public LoginGuard guard() {
      return guard;
   }
}
