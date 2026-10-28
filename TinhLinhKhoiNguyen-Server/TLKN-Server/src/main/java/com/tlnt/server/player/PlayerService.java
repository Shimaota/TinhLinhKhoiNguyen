package com.tlnt.server.player;

import com.tlnt.server.account.Account;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.db.SqlWork;
import com.tlnt.server.item.Appearance;
import com.tlnt.server.item.ItemBuffs;
import com.tlnt.server.item.ItemCollection;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.WeaponClass;
import com.tlnt.server.net.Packets;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;

import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Vòng đời dữ liệu nhân vật: đăng nhập -> load SQL -> giữ trong RAM ({@link #online}) -> lưu khi thoát /
 * bị kick / tắt server -> SQL. KHÔNG có autosave định kỳ: server sập ngang (mất điện, kill -9) thì mất phần chơi
 * từ lần đăng nhập đó.
 *
 * Mọi lệnh GHI đi qua 1 luồng riêng ({@link SaveWriter}, "db-writer") nên luồng xử lý gói tin không bao giờ chờ SQL
 * ghi. Lượt lưu cuối của mỗi tài khoản được theo dõi trong {@link #pendingSaves}: đăng nhập lại (vd sau
 * khi bị kick) sẽ chờ lượt lưu đó xong rồi mới đọc, nên không bao giờ load phải dữ liệu cũ.
 */
public final class PlayerService {

   /**
    * {@code skill_template.id} của "1. Đánh Thường": kỹ năng mặc định của mọi nhân vật chưa vào class, dùng
    * chung cho cả 3 nhân vật (Rinn/Lyria/Bruk). Tạo nhân vật = học sẵn cấp 1 + đặt lên thanh kỹ năng.
    */
   public static final int DEFAULT_SKILL_ID = ClassSkills.BASIC_ATTACK;
   private static final int START_LEVEL = 1;
   /** Tên nhân vật: 3-16 ký tự chữ (có dấu tiếng Việt) hoặc số, không khoảng trắng/ký tự đặc biệt. */
   private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\p{N}]{3,16}$");
   /** Tên lúc tạo nhân vật: Unicode letters/numbers và dấu cách; chiều dài tính theo code point. */
   private static final Pattern CHARACTER_NAME_PATTERN = Pattern.compile("^(?=.*[\\p{L}\\p{N}])[\\p{L}\\p{M}\\p{N} ]+$");

   private final PlayerRepository repository;
   private final World world;
   /** Map nhân vật mới tạo xuất hiện ({@code player.startMapId}, mặc định 2 Làng ánh sáng Eldarah). */
   private final int startMapId;
   private final SaveWriter writer;
   private final Map<Integer, PlayerData> online = new ConcurrentHashMap<>();
   private final Map<Integer, CompletableFuture<Void>> pendingSaves = new ConcurrentHashMap<>();
   /** Chạy đầu {@link #logout}, TRƯỚC khi rời thế giới và chụp dữ liệu để lưu (vd huỷ giao dịch đang mở). */
   private final List<Consumer<PlayerData>> logoutHooks = new CopyOnWriteArrayList<>();
   /**
    * Chạy trong {@link #enterGame} (luồng blocking, được gọi SQL), SAU khi gắn nhân vật vào session, TRƯỚC khi vào khu
    * (vd nạp hòm thư, tính lại phúc lợi) -> gói trạng thái đầu lúc vào khu đã có dữ liệu.
    */
   private final List<BiConsumer<PlayerSession, PlayerData>> loginHooks = new CopyOnWriteArrayList<>();

   public PlayerService(PlayerRepository repository, World world, int startMapId) {
      this.repository = repository;
      this.writer = new SaveWriter(repository);
      this.world = world;
      this.startMapId = startMapId;
      if (GameDataCache.charClass(WeaponClass.NONE) == null) {
         Log.warn("Khong thay char_class id=%d (Chua vu khi) trong cache - kiem tra lai SQL", WeaponClass.NONE);
      }
      if (GameDataCache.skillTemplate(DEFAULT_SKILL_ID) == null) {
         Log.warn("Khong thay skill_template id=%d (Danh Thuong) trong cache - kiem tra lai SQL", DEFAULT_SKILL_ID);
      }
   }

   // ------------------------------------------------------------------ đăng nhập / tạo nhân vật

   /** Gọi ngay sau khi đăng nhập tài khoản thành công. */
   public void onAccountLoggedIn(PlayerSession session, Account account) throws SQLException {
      PlayerData player = load(account.getId());
      if (player == null) {
         session.send(Packets.openCreateCharacter());
         return;
      }
      enterGame(session, account, player);
   }

   /** Opcode 38 C→S. */
   public void createCharacter(PlayerSession session, String rawName, int gender) throws SQLException {
      Account account = session.getAccount();
      if (account == null) {
         return;
      }
      if (session.getPlayer() != null) {
         session.send(Packets.notice("Tài khoản đã có nhân vật."));
         return;
      }

      String name = normalizeCharacterName(rawName);
      if (name == null) {
         session.send(Packets.notice(CHARACTER_NAME_RULE));
         return;
      }
      if (!Gender.isValid(gender)) {
         session.send(Packets.notice("Nhân vật không hợp lệ."));
         return;
      }

      if (com.tlnt.server.bot.BotService.nameInUse(name)) {
         session.send(Packets.notice("Tên nhân vật đã có người sử dụng."));
         return;
      }
      PlayerData player = newCharacter(account.getId(), name, gender);
      switch (repository.insert(player)) {
         case NAME_TAKEN -> session.send(Packets.notice("Tên nhân vật đã có người sử dụng."));
         case ALREADY_HAS_CHARACTER -> session.send(Packets.notice("Tài khoản đã có nhân vật."));
         case CREATED -> {
            com.tlnt.server.bot.BotService.noteRealName(name);
            Log.info("Tai khoan %s tao nhan vat #%d '%s' (%s)", account.getUsername(), player.getId(), name,
                  Gender.name(gender));
            session.send(Packets.notice("Tạo nhân vật thành công!"));
            enterGame(session, account, player);
         }
      }
   }

   public static final String NAME_RULE = "Tên nhân vật dài 3-16 ký tự, chỉ gồm chữ và số.";
   public static final String CHARACTER_NAME_RULE =
         "Tên nhân vật phải dài 5-16 ký tự, được dùng chữ có dấu, số và khoảng trắng.";

   /** Chuẩn hoá/kiểm tra tên tạo nhân vật; trim hai đầu, chuẩn NFC, hỗ trợ tiếng Việt có dấu và khoảng trắng. */
   public static String normalizeCharacterName(String rawName) {
      String name = Normalizer.normalize(rawName == null ? "" : rawName.trim(), Normalizer.Form.NFC);
      int length = name.codePointCount(0, name.length());
      return length >= 5 && length <= 16 && CHARACTER_NAME_PATTERN.matcher(name).matches() ? name : null;
   }

   /** Tên nhân vật hợp lệ (bỏ khoảng trắng 2 đầu, chuẩn NFC); null = sai luật {@link #NAME_RULE}. */
   public static String normalizeName(String rawName) {
      String name = Normalizer.normalize(rawName == null ? "" : rawName.trim(), Normalizer.Form.NFC);
      return NAME_PATTERN.matcher(name).matches() ? name : null;
   }

   /**
    * Đổi tên trong DB (luồng BLOCKING, lần lượt theo hàng đợi session) rồi trong RAM. Người gọi PHẢI {@link #saveAsync}
    * ngay sau -> bản chụp cũ (tên cũ) đang chờ ghi không đè được tên mới. Tên cũ được GIỮ cho nhân vật này
    * ({@code player_name_history}). @return false nếu tên đã có người dùng / đang giữ cho người khác
    */
   public boolean rename(PlayerData d, String name) throws SQLException {
      if (!repository.rename(d.getId(), d.name, name)) {
         return false;
      }
      d.name = name;
      com.tlnt.server.bot.BotService.noteRealName(name);
      return true;
   }

   private PlayerData newCharacter(int accountId, String name, int gender) {
      PlayerData player = new PlayerData(0, accountId);
      player.name = name;
      player.gender = gender;
      player.classId = WeaponClass.NONE; // chọn class khi trang bị vũ khí đầu tiên
      player.level = START_LEVEL;
      player.hp = PlayerData.FULL;
      player.mp = PlayerData.FULL;
      player.activityPoints = PlayerStats.of(player).maxStamina; // tạo nhân vật: thể lực đầy
      player.settings = PlayerSettings.defaults();
      player.mapId = startMapId; // tạo nhân vật: xuất hiện ở làng, nhận nhiệm vụ 1 (gặp Trưởng làng) lúc vào game
      player.talents = new int[GameDataCache.skill().talents().size()];
      ensureDefaultSkill(player);
      StarterKit.give(player, System.currentTimeMillis()); // đồ có sẵn: hành trang + áo trong rương
      return player;
   }

   private void enterGame(PlayerSession session, Account account, PlayerData player) throws SQLException {
      // Đưa vào online TRƯỚC khi gắn vào session: nếu client ngắt đúng lúc này, bindPlayer thất bại và
      // ta gỡ ra lại; còn nếu gắn xong thì luồng logout chắc chắn thấy player để lưu.
      player.sessionStartAt = System.currentTimeMillis();
      online.put(player.getId(), player);
      if (!session.bindPlayerIfLoggedIn(account, player)) {
         online.remove(player.getId());
         return;
      }
      repository.markLogin(player.getId());
      Log.debug("Nhan vat #%d '%s' vao game (tai khoan %s), online %d", player.getId(), player.name,
            account.getUsername(), online.size());

      if (player.settings == null || player.settings.keybindings == null) { // nhân vật tạo trước khi có mặc định
         player.settings = PlayerSettings.defaults();
      }
      normalizeItems(player);
      if (player.friends == null) {
         player.friends = new ArrayList<>();
      }
      player.friends.removeIf(f -> f == null || f.id <= 0 || f.id == player.getId());
      if (!Gender.isValid(player.gender)) {
         player.gender = Gender.RINN;
      }
      if (!WeaponClass.isValid(player.classId)) {
         player.classId = WeaponClass.NONE;
      }
      if (player.classId == WeaponClass.NONE) { // dữ liệu cũ: đã cầm vũ khí từ trước khi có luật class
         player.classId = WeaponClass.of(player.body.get(Appearance.SLOT_WEAPON));
      }
      // Item hết hạn lúc offline: gỡ trước khi gửi túi đồ (không cần báo từng món).
      ItemService.purgeExpired(player, System.currentTimeMillis(), new ArrayList<>());
      ItemBuffs.dropSkillIcons(player); // icon buff kỹ năng (10 / 16) không giữ qua thoát game
      com.tlnt.server.item.GearCharges.purgeBroken(player); // phụ kiện hết lượt -> gỡ; dựng bộ đếm lượt
      PlayerStats stats = PlayerStats.of(player); // gốc + đồ đang dùng
      if (player.isDead()) { // thoát game lúc đang kiệt sức: vào lại ở Làng, đầy HP / MP (không kẹt hộp "Kiệt sức")
         player.hp = stats.maxHp;
         player.mp = stats.maxMp;
         player.mapId = startMapId;
         player.x = null;
         player.y = null;
      }
      if (player.hp < 0 || player.hp > stats.maxHp) {
         player.hp = stats.maxHp;
      }
      if (player.mp < 0 || player.mp > stats.maxMp) {
         player.mp = stats.maxMp;
      }
      player.activityPoints = Math.min(player.activityPoints, stats.maxStamina);
      ensureDefaultSkill(player); // nhân vật tạo trước khi có skill mặc định
      ItemService.refreshLook(player); // công bố chỉ số + ngoại hình trước khi vào khu
      for (BiConsumer<PlayerSession, PlayerData> hook : loginHooks) {
         try {
            hook.accept(session, player);
         } catch (Exception e) {
            Log.error("Loi khi vao game nhan vat #" + player.getId(), e);
         }
      }

      world.enter(session, player);
      // Client ngắt đúng lúc đang vào: logout có thể đã chạy trước world.enter -> tự gỡ khỏi thế giới.
      if (session.getPlayer() != player) {
         world.leave(player);
      }
   }

   /**
    * Kỹ năng khớp class: luôn có "Đánh Thường" cấp 1; đã vào class thì có đủ kỹ năng class (thiếu thì thêm cấp 0);
    * thanh kỹ năng trống thì đặt "Đánh Thường" vào. Chạy lúc tạo / vào game (trước khi vào khu).
    */
   private static void ensureDefaultSkill(PlayerData player) {
      ClassSkills.apply(player);
   }

   /** Sửa dữ liệu item đọc từ DB (thiếu cột, JSON cũ, ô dùng nhanh sai...). */
   private static void normalizeItems(PlayerData player) {
      player.costumes = player.costumes == null ? new ItemCollection() : player.costumes.normalize();
      player.titles = player.titles == null ? new ItemCollection() : player.titles.normalize();
      player.masks = player.masks == null ? new ItemCollection() : player.masks.normalize();
      player.pets = player.pets == null ? new ItemCollection() : player.pets.normalize();
      player.backs = player.backs == null ? new ItemCollection() : player.backs.normalize();
      if (player.settings != null) {
         player.settings.normalize(); // JSON cũ: 4 cờ ẩn -> 5
      }
      if (player.quickSlots == null || player.quickSlots.length != 4) {
         player.quickSlots = new int[] {-1, -1, -1, -1};
      }
      for (int i = 0; i < player.quickSlots.length; i++) {
         if (player.bag.get(player.quickSlots[i]) == null) {
            player.quickSlots[i] = -1;
         }
      }
   }

   // ------------------------------------------------------------------ load / lưu

   private PlayerData load(int accountId) throws SQLException {
      awaitPendingSave(accountId);
      return repository.findByAccountId(accountId);
   }

   /**
    * Admin panel: nạp nhân vật OFFLINE (chờ lượt lưu còn dở) + chuẩn hoá item như lúc vào game. Người gọi phải giữ tài
    * khoản ({@code AccountService.adminHold}) từ trước khi nạp tới sau khi {@link #saveOffline} xong. null = chưa tạo.
    */
   public PlayerData loadOffline(int accountId) throws SQLException {
      PlayerData player = load(accountId);
      if (player != null) {
         normalizeItems(player);
         if (player.buffs == null) {
            player.buffs = java.util.Map.of();
         }
      }
      return player;
   }

   /** Admin panel: lưu nhân vật offline đã sửa và CHỜ ghi xong. @return true nếu đã commit */
   public boolean saveOffline(PlayerData player) {
      return saveWith(player, null).join();
   }

   /** Đăng ký việc chạy lúc 1 nhân vật vào game, trước khi vào khu (GameServer nối lúc khởi động). */
   public void onLogin(BiConsumer<PlayerSession, PlayerData> hook) {
      loginHooks.add(hook);
   }

   /** Đăng ký việc chạy khi 1 nhân vật thoát (GameServer nối lúc khởi động). */
   public void onLogout(Consumer<PlayerData> hook) {
      logoutHooks.add(hook);
   }

   /** Đăng xuất / bị kick / mất kết nối: gỡ khỏi RAM và lưu lần cuối. */
   public void logout(PlayerData player) {
      if (online.remove(player.getId()) == null) {
         return;
      }
      for (Consumer<PlayerData> hook : logoutHooks) {
         try {
            hook.accept(player);
         } catch (Exception e) {
            Log.error("Loi khi dang xuat nhan vat #" + player.getId(), e);
         }
      }
      // Rời khu (người khác thấy biến mất). Vị trí đã được tick chép vào PlayerData nên lưu ngay được;
      // khu không lưu -> lần sau vào lại khu còn chỗ số nhỏ nhất.
      world.leave(player);
      player.playTimeSeconds += (System.currentTimeMillis() - player.sessionStartAt) / 1000;
      player.sessionStartAt = System.currentTimeMillis();
      saveAsync(player, true);
   }

   /**
    * Lưu nền. Snapshot (serialize JSON) chụp NGAY trên luồng gọi, luồng db-writer chỉ chạy câu SQL (1 luồng -> các
    * lượt lưu của cùng 1 người không bao giờ đảo thứ tự; lượt lưu liền nhau gom lô, lỗi thì thử lại / ghi file —
    * {@link SaveWriter}).
    */
   public void saveAsync(PlayerData player, boolean logout) {
      if (player.bot) {
         return; // người chơi giả: chỉ sống trong RAM
      }
      // Chụp + xếp hàng dưới khoá của nhân vật: lượt lưu của thư / kí gửi ({@link #saveWith}) cũng chụp dưới khoá này
      // -> thứ tự chụp = thứ tự ghi, bản chụp cũ không bao giờ ghi đè bản mới.
      synchronized (player) {
         PlayerRepository.Snapshot snapshot = PlayerRepository.snapshot(player);
         int accountId = player.getAccountId();
         CompletableFuture<Void> save = writer.save(snapshot, logout);
         pendingSaves.put(accountId, save);
         save.whenComplete((ignored, error) -> pendingSaves.remove(accountId, save));
      }
   }

   /**
    * Lưu nhân vật + {@code extra} trong CÙNG 1 transaction trên db-writer (thư, kí gửi: túi đồ / vàng vừa đổi và dòng
    * thư / kí gửi tương ứng cùng được ghi hoặc cùng không -> sập server giữa chừng cũng không nhân bản / mất đồ).
    * Người gọi nên giữ {@code synchronized (player)} từ lúc sửa RAM tới khi gọi hàm này (lượt lưu lúc thoát game cũng
    * chụp dưới khoá đó). @return future = true nếu đã commit
    */
   public CompletableFuture<Boolean> saveWith(PlayerData player, SqlWork extra) {
      if (player.bot) {
         return CompletableFuture.completedFuture(false); // người chơi giả không có dòng DB
      }
      synchronized (player) {
         PlayerRepository.Snapshot snapshot = PlayerRepository.snapshot(player);
         int accountId = player.getAccountId();
         CompletableFuture<Boolean> save = writer.transaction(snapshot, extra);
         CompletableFuture<Void> done = save.thenApply(ok -> null);
         pendingSaves.put(accountId, done);
         done.whenComplete((ignored, error) -> pendingSaves.remove(accountId, done));
         return save;
      }
   }

   /**
    * Chạy {@code work} trong 1 transaction trên db-writer, xếp sau mọi lượt lưu đã xếp trước (không lưu nhân vật nào) —
    * vd kí gửi hết hạn trả đồ về thư. @return future = true nếu đã commit
    */
   public CompletableFuture<Boolean> runOnWriter(SqlWork work) {
      return writer.transaction(null, work);
   }

   private void awaitPendingSave(int accountId) {
      CompletableFuture<Void> save = pendingSaves.get(accountId);
      if (save != null) {
         save.join(); // luôn complete bình thường vì lỗi SQL đã được bắt trong task
      }
   }

   /**
    * Tắt server (bước cuối): lưu nốt người còn online (phòng hờ) rồi chờ ghi HẾT — không giới hạn thời gian, in tiến
    * trình. Việc đá người dần dần làm trước ở {@code ShutdownService}.
    */
   public void shutdown() {
      for (PlayerData player : new ArrayList<>(online.values())) {
         logout(player);
      }
      writer.drainAndStop();
   }

   /** Số lượt ghi DB đang chờ (tiến trình lúc tắt server). */
   public int pendingWrites() {
      return writer.pending();
   }

   /** Bản chụp danh sách nhân vật đang online (bảng xếp hạng đọc số mới nhất). */
   public List<PlayerData> onlinePlayers() {
      return new ArrayList<>(online.values());
   }

   public int onlineCount() {
      return online.size();
   }
}
