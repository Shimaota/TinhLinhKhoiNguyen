package com.tlnt.server.session;

import com.tlnt.server.account.Account;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Packet;
import com.tlnt.server.net.SerialExecutor;
import com.tlnt.server.net.transport.NetConnection;
import com.tlnt.server.player.PlayerData;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Trạng thái gắn với một kết nối (1 kết nối = 1 phiên chơi). Handler chỉ gọi {@link #send}, không biết tầng mạng
 * là Netty hay KryoNet ({@link NetConnection}).
 *
 * Gói của 1 session chạy tuần tự qua {@link #inbox()}, nên các field chỉ handler dùng (captcha, đếm login sai)
 * không cần đồng bộ. {@link #account}/{@link #player} còn bị luồng disconnect/kick đụng tới nên GHI qua method
 * {@code synchronized}; {@link #getPlayer()} đọc volatile, không khoá (gọi cho mỗi gói di chuyển).
 */
public final class PlayerSession {

   public enum State {
      /** Mới kết nối, chưa đăng nhập. */
      CONNECTED,
      /** Đã đăng nhập tài khoản (có thể chưa có nhân vật). */
      AUTHENTICATED,
      /** Đang bị đóng (bị kick/đăng xuất) — bỏ qua mọi gói tới. */
      CLOSING
   }

   /** Kết nối thật; sau khi treo offline mà client ngắt -> {@link OfflineConnection} (gửi = bỏ). */
   private volatile NetConnection connection;
   private final SerialExecutor inbox = new SerialExecutor();

   private volatile State state = State.CONNECTED;
   private volatile Account account;
   private volatile PlayerData player;

   // Chỉ đụng tới trong inbox (tuần tự) -> không cần volatile.
   private String captchaCode;
   private long captchaExpireAt;
   private long lastCaptchaRequestAt;
   private int failedLoginCount;
   /** Số gói sai định dạng (IO + GAME lane cùng đếm -> atomic), xem PacketDispatcher. */
   private final AtomicInteger malformedPackets = new AtomicInteger();

   // Version dữ liệu client báo ở handshake (opcode 0).
   private volatile int reportedGameDataVersion = -1;
   private volatile int reportedMapDataVersion = -1;
   private volatile int reportedItemDataVersion = -1;
   private volatile int reportedSkillDataVersion = -1;

   public PlayerSession(NetConnection connection) {
      this.connection = connection;
   }

   /**
    * Phiên của người chơi giả ({@code bot/BotService}): không có kết nối thật (gửi = bỏ ngay, không mã hoá — như nhân vật
    * treo offline), không tài khoản; gói của nó chạy tuần tự qua {@link #inbox()} như người thật.
    */
   public static PlayerSession bot(int id) {
      return new PlayerSession(new OfflineConnection(id, "bot"));
   }

   /** Phiên của người chơi giả. */
   public boolean isBot() {
      PlayerData p = player;
      return p != null && p.bot;
   }

   // ------------------------------------------------------------------ mạng

   public void send(Message message) {
      NetConnection c = connection;
      if (c instanceof OfflineConnection) {
         return; // treo offline, client đã ngắt: khỏi mã hoá
      }
      c.send(message.toPacket());
   }

   /** Gửi gói đã encode sẵn — dùng khi broadcast 1 gói cho nhiều người (encode 1 lần). */
   public void send(Packet packet) {
      connection.send(packet);
   }

   /** Ngừng nhận gói ngay, đóng kết nối sau {@code delayMs} để client kịp nhận gói cuối (vd thông báo kick). */
   public void closeAfter(ScheduledExecutorService scheduler, long delayMs) {
      state = State.CLOSING;
      scheduler.schedule(connection::close, delayMs, TimeUnit.MILLISECONDS);
   }

   public void close() {
      state = State.CLOSING;
      connection.close();
   }

   /** Gửi đoạn nhiều frame nối liền (phát gộp), xem {@link NetConnection#sendFrames}. */
   public void sendFrames(byte[] frames, int offset, int length) {
      connection.sendFrames(frames, offset, length);
   }

   /** Tầng mạng báo đã ngắt: bỏ qua mọi gói còn trong hàng đợi. */
   public void markClosing() {
      state = State.CLOSING;
   }

   // ------------------------------------------------------------------ treo offline (offline/OfflineTrainingService)

   /** Đang treo offline: client ngắt thì nhân vật vẫn ở lại thế giới, bot server điều khiển. */
   private volatile boolean offlineTraining;
   /** Lần báo "tài khoản đang được bảo vệ" gần nhất (luồng mạng, chỉ để giảm spam — không cần chính xác). */
   private volatile long lastProtectedNoticeAt;

   public boolean isOfflineTraining() {
      return offlineTraining;
   }

   public void setOfflineTraining(boolean offlineTraining) {
      this.offlineTraining = offlineTraining;
   }

   /**
    * Bắt đầu treo offline: đóng kết nối thật sau {@code delayMs} (client kịp hiện thông báo) nhưng KHÔNG chuyển sang
    * CLOSING — SessionManager thấy cờ treo thì giữ nhân vật lại.
    */
   public void dropClientAfter(ScheduledExecutorService scheduler, long delayMs) {
      NetConnection c = connection;
      scheduler.schedule(c::close, delayMs, TimeUnit.MILLISECONDS);
   }

   /** Client đã ngắt trong lúc treo offline: mọi gói gửi về sau bị bỏ (không giữ tham chiếu kết nối chết). */
   public void detachConnection() {
      NetConnection old = connection;
      if (!(old instanceof OfflineConnection)) {
         connection = new OfflineConnection(old.id(), old.remoteIp());
      }
   }

   /** Client đã ngắt (kết nối thật không còn). */
   public boolean isDetached() {
      return connection instanceof OfflineConnection;
   }

   // ------------------------------------------------------------------ bảo vệ tài khoản (MK cấp 2)

   /** Tài khoản đang bật bảo vệ (nút "Khóa") — PacketDispatcher đọc cho mỗi gói, không khoá. */
   public boolean isProtected() {
      Account a = account;
      return a != null && a.isProtectionEnabled();
   }

   /** Cho báo "đang được bảo vệ" nếu lần báo trước đã cách ít nhất {@code intervalMs}. */
   public boolean allowProtectedNotice(long now, long intervalMs) {
      if (now - lastProtectedNoticeAt < intervalMs) {
         return false;
      }
      lastProtectedNoticeAt = now;
      return true;
   }

   public int incrementMalformed() {
      return malformedPackets.incrementAndGet();
   }

   public int getId() {
      return connection.id();
   }

   public String getRemoteIp() {
      return connection.remoteIp();
   }

   public SerialExecutor inbox() {
      return inbox;
   }

   public State getState() {
      return state;
   }

   public boolean isAuthenticated() {
      return state == State.AUTHENTICATED;
   }

   // ------------------------------------------------------------------ tài khoản / nhân vật

   public synchronized void bindAccount(Account account) {
      this.account = account;
      if (state == State.CONNECTED) {
         state = State.AUTHENTICATED;
      }
   }

   public synchronized Account getAccount() {
      return account;
   }

   /** Gỡ tài khoản khỏi session (đăng xuất/kick). Trả về null nếu đã gỡ trước đó -> gọi nhiều lần vẫn an toàn. */
   public synchronized Account detachAccount() {
      Account detached = account;
      account = null;
      return detached;
   }

   /**
    * Gắn nhân vật nếu session vẫn đang đăng nhập đúng tài khoản {@code owner}. Trả về false khi client
    * đã ngắt/bị kick trong lúc đang load (tài khoản đã bị gỡ) -> người gọi không được coi là online.
    */
   public synchronized boolean bindPlayerIfLoggedIn(Account owner, PlayerData player) {
      if (account != owner) {
         return false;
      }
      this.player = player;
      return true;
   }

   public PlayerData getPlayer() {
      return player;
   }

   public synchronized PlayerData detachPlayer() {
      PlayerData detached = player;
      player = null;
      return detached;
   }

   // ------------------------------------------------------------------ captcha / chống dò mật khẩu

   public void setCaptcha(String code, long expireAt) {
      this.captchaCode = code;
      this.captchaExpireAt = expireAt;
   }

   /** Captcha dùng 1 lần: kiểm tra xong là xoá, dù đúng hay sai. */
   public boolean consumeCaptcha(String input, long now) {
      String expected = captchaCode;
      boolean valid = expected != null && now <= captchaExpireAt && input != null
            && expected.equalsIgnoreCase(input.trim());
      captchaCode = null;
      return valid;
   }

   public long getLastCaptchaRequestAt() {
      return lastCaptchaRequestAt;
   }

   public void setLastCaptchaRequestAt(long time) {
      this.lastCaptchaRequestAt = time;
   }

   public int incrementFailedLogin() {
      return ++failedLoginCount;
   }

   public void resetFailedLogin() {
      failedLoginCount = 0;
   }

   // ------------------------------------------------------------------ version dữ liệu (handshake)

   public void setReportedDataVersions(int gameVersion, int mapVersion, int itemVersion, int skillVersion) {
      this.reportedGameDataVersion = gameVersion;
      this.reportedMapDataVersion = mapVersion;
      this.reportedItemDataVersion = itemVersion;
      this.reportedSkillDataVersion = skillVersion;
   }

   public int getReportedGameDataVersion() {
      return reportedGameDataVersion;
   }

   public int getReportedMapDataVersion() {
      return reportedMapDataVersion;
   }

   public int getReportedItemDataVersion() {
      return reportedItemDataVersion;
   }

   public int getReportedSkillDataVersion() {
      return reportedSkillDataVersion;
   }
}
