package com.tlnt.server.offline;

import com.tlnt.server.account.AccountService;
import com.tlnt.server.altar.AltarRules;
import com.tlnt.server.badge.SpiritBadgeService;
import com.tlnt.server.combat.CombatService;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.SkillPack;
import com.tlnt.server.player.SkillData;
import com.tlnt.server.skill.SkillService;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerSettings;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.session.SessionManager;
import com.tlnt.server.thunder.ThunderRules;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.Arena;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Luyện tập Offline (Huy hiệu Tinh Linh): thoát game nhưng nhân vật vẫn ở lại khu, server tự đánh quái theo "Thiết lập
 * tự động" (op 46) CHỤP lúc bấm nút — CHẠY bộ trên mặt đất (chạy / nhảy / rơi, tốc chạy thật — world/OfflineWalker) tới
 * quái gần nhất hợp lọc (hoặc đồ rơi của mình nếu bật Tự nhặt) rồi ra chiêu, như người chơi thật bật tự đánh.
 *
 * <pre>
 * C→S 45/1 nút "Luyện tập Offline"   -> S→C 53/2 mở cửa sổ Thiết lập (SETTING_2_POPUP) tab tự đánh
 * C→S 77/0 byte n, n×int mob_template -> hỏi op 32 loại {@link Packets#CONFIRM_OFFLINE_TRAINING} (decompile C3764 m17385;
 *                                        n = 0: mọi loại quái)
 * C→S 32 loại 18 "Đồng ý"            -> bắt đầu treo, đóng kết nối sau 1,5 s
 * </pre>
 *
 * <h2>Cách chạy (tối ưu)</h2>
 * <ul>
 *   <li>Không có "người chơi giả": GIỮ NGUYÊN session + PlayerData + WorldPlayer; client ngắt thì session chỉ đổi sang
 *       kết nối rỗng ({@code OfflineConnection}: gửi = bỏ, không mã hoá). Mọi luật (sát thương, hồi chiêu, MP, thể lực,
 *       EXP, rơi đồ, tự nhặt, hồi HP 5 s, quái đánh trả, PK) là code của người chơi thật — không viết lại.</li>
 *   <li>1 nhịp scheduler {@link #STEP_MS} cho MỌI người treo: chỉ xếp 1 việc vào hàng đợi session của từng người (cờ
 *       {@link Run#queued} chặn xếp chồng khi pool bận). Việc chạy trên luồng session như gói của client.</li>
 *   <li>Tìm quái = 1 lượt duyệt quái của khu trên luồng tick khu ({@link World#offlineSeek}); giữ mục tiêu cũ tới khi
 *       nó chết.</li>
 * </ul>
 * Dừng khi: hết hạn huy hiệu, bị hạ gục (không có / không bật tự hồi sinh bằng Cỏ hồi sinh), chủ tài khoản đăng nhập
 * lại (chiếm chỗ ngay — {@link AccountService.OfflineHooks}), admin kick, tắt server. Dừng = đăng xuất + lưu như thoát
 * game; câu tổng kết gửi lúc vào game lần sau.
 */
public final class OfflineTrainingService implements AccountService.OfflineHooks {

   /** Nhịp bot (ms): mỗi nhịp thử dùng thuốc + ra 1 chiêu (hồi chiêu do CombatService tự chặn). */
   static final long STEP_MS = 300;
   /** Đóng kết nối client sau khi bắt đầu (kịp hiện thông báo). */
   static final long DROP_CLIENT_MS = 1_500;
   /** Dùng thuốc tối đa 1 lần / bấy nhiêu ms mỗi loại. */
   static final long POTION_INTERVAL_MS = 1_000;
   /** Ô dùng nhanh HUD (số client gửi ở 27/9): 3 = HP, 4 = MP, 1 = thể lực. */
   static final int QUICK_HP = 3;
   static final int QUICK_MP = 4;
   static final int QUICK_STAMINA = 1;
   /** Tối đa số loại quái trong 1 gói 77/0 (client chỉ liệt kê loại đang có quanh mình). */
   public static final int MAX_TEMPLATES = 64;
   /** Chờ luồng khu trả mục tiêu tối đa bấy nhiêu ms (đổi khu / map giữa chừng). */
   static final long SEEK_TIMEOUT_MS = 3_000;

   static final String MSG_NEED_BADGE = SpiritBadgeService.NEED_BADGE;

   /** 1 lượt treo — chỉ luồng session đụng (trừ {@link #queued}). */
   static final class Run {
      final int[] templates;
      /** Bảng Thiết lập tự động CHỤP lúc bấm Đồng ý (client gửi lại op 46 ngay trước op 77) — cả lượt treo theo bản này. */
      final PlayerSettings settings;
      final long startAt;
      final int startLevel;
      final long startExp;
      final AtomicBoolean queued = new AtomicBoolean();
      int target = -1;
      long hpPotionAt;
      long mpPotionAt;
      long staminaPotionAt;
      /** Đang chờ luồng khu trả mục tiêu (không xin chồng); quá {@link #SEEK_TIMEOUT_MS} thì xin lại. */
      boolean seeking;
      long seekAt;

      Run(int[] templates, PlayerSettings settings, long startAt, int startLevel, long startExp) {
         this.settings = settings;
         this.templates = templates;
         this.startAt = startAt;
         this.startLevel = startLevel;
         this.startExp = startExp;
      }
   }

   private final World world;
   private final CombatService combat;
   private final ItemService items;
   private final AccountService accounts;
   private final ScheduledExecutorService scheduler;
   private final Executor gamePool;
   private volatile SessionManager sessions;

   /** Session đang treo (đã bấm Đồng ý). */
   private final Map<PlayerSession, Run> runs = new ConcurrentHashMap<>();
   /** Đã gửi 77/0, chờ "Đồng ý": session -> danh sách loại quái. Luồng session. */
   private final Map<PlayerSession, Pending> pending = new ConcurrentHashMap<>();
   /** Câu tổng kết lượt treo đã dừng mà chủ chưa vào game lại (accountId -> câu). */
   private final Map<Integer, String> summaries = new ConcurrentHashMap<>();

   public OfflineTrainingService(World world, CombatService combat, ItemService items, AccountService accounts,
         ScheduledExecutorService scheduler, Executor gamePool) {
      this.world = world;
      this.combat = combat;
      this.items = items;
      this.accounts = accounts;
      this.scheduler = scheduler;
      this.gamePool = gamePool;
      accounts.setOfflineHooks(this);
      instance = this;
      scheduler.scheduleAtFixedRate(this::tick, STEP_MS, STEP_MS, TimeUnit.MILLISECONDS);
   }

   private static volatile OfflineTrainingService instance;

   /** Bản đang chạy (ConfirmHandler / UtilityHandler gọi), null trước khi khởi động. */
   public static OfflineTrainingService instance() {
      return instance;
   }

   /** SessionManager tạo sau Routes -> nối sau. */
   public void setSessions(SessionManager sessions) {
      this.sessions = sessions;
   }

   public int activeCount() {
      return runs.size();
   }

   // ================================================================== bắt đầu

   /** Đã bấm nút, chờ "Đồng ý": loại quái + bảng Thiết lập chụp ĐÚNG lúc bấm (op 46 client gửi ngay trước op 77). */
   record Pending(int[] templates, PlayerSettings settings) {
   }

   /** C→S 77/0: bấm "Luyện tập Offline" trong Thiết lập tự động -> hỏi xác nhận (liệt kê tuỳ chọn sẽ dùng). Luồng session. */
   public void request(PlayerSession session, int[] templates) {
      PlayerData d = session.getPlayer();
      if (d == null || runs.containsKey(session)) {
         return;
      }
      String error = check(d, System.currentTimeMillis());
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      PlayerSettings s = (d.settings != null ? d.settings : PlayerSettings.defaults()).copy();
      pending.put(session, new Pending(templates, s));
      long left = d.spiritBadgeUntil - System.currentTimeMillis();
      session.send(Packets.confirm(Packets.CONFIRM_OFFLINE_TRAINING, "Luyện tập Offline: thoát game, nhân vật ở lại khu "
            + "này tự chạy tới quái và đánh theo Thiết lập đang chọn — " + describe(templates, s)
            + ". Dừng khi Huy hiệu Tinh Linh hết hạn (còn " + duration(left) + ") hoặc bạn đăng nhập lại. Bắt đầu?"));
   }

   /** Tóm tắt tuỳ chọn cho hộp xác nhận. */
   static String describe(int[] templates, PlayerSettings s) {
      int slots = 0;
      for (boolean on : s.autoSkillSlotEnabled) {
         slots += on ? 1 : 0;
      }
      StringBuilder sb = new StringBuilder();
      sb.append(templates.length == 0 ? "mọi loại quái" : templates.length + " loại quái đã chọn");
      if (s.autoAttackRange > 0) {
         sb.append(" trong phạm vi ").append(s.autoAttackRange).append(" quanh chỗ đứng");
      }
      sb.append(", kỹ năng: ").append(slots == 0 ? "ô 1" : slots + " ô đã bật");
      if (s.autoAvoidEliteMobs) {
         sb.append(", né siêu quái");
      }
      if (s.autoIgnorePlayersAndBosses) {
         sb.append(", không đánh Boss");
      }
      if (s.autoPickupItems) {
         sb.append(", tự nhặt");
      }
      if (s.autoUseHpItems || s.autoUseMpItems || s.autoUseStaminaItems) {
         sb.append(", tự dùng thuốc");
      }
      if (s.autoRevive) {
         sb.append(", tự hồi sinh");
      }
      return sb.toString();
   }

   /** "Đồng ý" ở hộp hỏi (op 32 loại 18). Luồng session. */
   public void confirm(PlayerSession session) {
      Pending p = pending.remove(session);
      PlayerData d = session.getPlayer();
      if (p == null || d == null || runs.containsKey(session)) {
         return;
      }
      long now = System.currentTimeMillis();
      String error = check(d, now);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      int[] templates = p.templates();
      runs.put(session, new Run(templates, p.settings(), now, d.level, d.exp));
      session.setOfflineTraining(true);
      session.send(Packets.notice("Bắt đầu Luyện tập Offline. Đăng nhập lại bất cứ lúc nào để dừng."));
      session.dropClientAfter(scheduler, DROP_CLIENT_MS);
      Log.debug("Nhan vat #%d bat dau treo offline o map %d (%d loai quai)", d.getId(), d.mapId, templates.length);
   }

   /** Điều kiện treo. @return câu báo lỗi, null = được */
   String check(PlayerData d, long now) {
      if (!d.inWorld) {
         return "Hãy vào game xong rồi thử lại.";
      }
      if (!SpiritBadgeService.active(d, now)) {
         return MSG_NEED_BADGE;
      }
      if (d.isDead()) {
         return "Nhân vật đang bị hạ gục.";
      }
      if (d.trade != null || d.fishing != null) {
         return "Hãy kết thúc giao dịch / câu cá trước.";
      }
      if (d.isInDungeon() || AltarRules.isAltarMap(d.mapId) || ThunderRules.isThunderMap(d.mapId)
            || Arena.isArenaMap(d.mapId)) {
         return "Không thể Luyện tập Offline trong phó bản / chiến trường / đấu trường.";
      }
      MapTemplate map = world.mapTemplate(d.mapId);
      if (map == null || map.mobs() == null || map.mobs().isEmpty()) {
         return "Khu vực này không có quái để luyện tập.";
      }
      if (attackSkills(d).length == 0) {
         return "Chưa có kỹ năng trên thanh kỹ năng.";
      }
      return null;
   }

   // ================================================================== nhịp bot

   private void tick() {
      if (runs.isEmpty()) {
         return;
      }
      for (Map.Entry<PlayerSession, Run> e : runs.entrySet()) {
         PlayerSession session = e.getKey();
         Run run = e.getValue();
         if (!session.isDetached()) {
            continue; // client chưa ngắt hẳn (còn 1,5 s chờ thông báo) -> chưa đánh thay
         }
         if (run.queued.compareAndSet(false, true)) {
            session.inbox().execute(() -> {
               try {
                  step(session, run);
               } catch (Exception ex) {
                  Log.error("Loi treo offline session #" + session.getId(), ex);
               } finally {
                  run.queued.set(false);
               }
            }, gamePool);
         }
      }
   }

   /** 1 nhịp của 1 người (luồng session). */
   void step(PlayerSession session, Run run) {
      if (runs.get(session) != run) {
         return;
      }
      PlayerData d = session.getPlayer();
      if (d == null) {
         runs.remove(session); // đã đăng xuất đường khác
         return;
      }
      if (!d.inWorld) {
         return; // đang chuyển map
      }
      long now = System.currentTimeMillis();
      if (!SpiritBadgeService.active(d, now)) {
         stop(session, run, "Huy hiệu Tinh Linh đã hết hạn");
         return;
      }
      PlayerSettings s = run.settings;
      if (d.isDead()) {
         if (s.autoRevive && !ThunderRules.isThunderMap(d.mapId)
               && items.consumeOne(session, ItemService.REVIVE_HERB_TEMPLATE_ID)) {
            world.reviveHere(session, ItemService.REVIVE_HERB_HP_PERCENT);
            run.target = -1;
         } else {
            stop(session, run, "Nhân vật bị hạ gục");
         }
         return;
      }
      usePotions(session, d, s, run, now);
      if (run.seeking && now - run.seekAt < SEEK_TIMEOUT_MS) {
         return;
      }
      run.seeking = true;
      run.seekAt = now;
      int[] skills = attackSkills(d, run.settings);
      int[][] reach = reach(d, skills);
      world.offlineSeek(session, new World.OfflineQuery(run.templates, s.autoAvoidEliteMobs, s.autoIgnorePlayersAndBosses,
            run.target, s.autoPickupItems, reach[0], reach[1], s.autoAttackRange),
            (mobId, mask) -> session.inbox().execute(() -> attack(session, run, mobId, skills, mask), gamePool));
   }

   /** Tầm X / Y (px, theo {@code skill_level} cấp đã học) của từng kỹ năng trong {@code skills}; thiếu dữ liệu = 0. */
   public static int[][] reach(PlayerData d, int[] skills) {
      int[] rx = new int[skills.length];
      int[] ry = new int[skills.length];
      for (int i = 0; i < skills.length; i++) {
         SkillData learned = SkillService.find(d, skills[i]);
         SkillPack.Skill template = GameDataCache.skillTemplate(skills[i]);
         SkillPack.SkillLevel level = learned == null || template == null ? null
               : SkillService.levelData(template, learned.level);
         if (level != null) {
            rx[i] = level.rangeX();
            ry[i] = level.rangeY();
         }
      }
      return new int[][] {rx, ry};
   }

   /**
    * Luồng tick khu trả mục tiêu: bit i của {@code mask} = kỹ năng {@code skills[i]} đang với tới -> ra chiêu đầu tiên dùng
    * được trong số đó (chiêu không với tới thì không thử: server sẽ trừ MP / hồi chiêu mà đòn hụt); mask 0 = khu đang chạy
    * nhân vật tới. Luồng session.
    */
   void attack(PlayerSession session, Run run, int mobId, int[] skills, int mask) {
      run.seeking = false;
      run.target = mobId;
      PlayerData d = session.getPlayer();
      if (mask == 0 || mobId < 0 || d == null || runs.get(session) != run || d.isDead()) {
         return;
      }
      for (int i = 0; i < skills.length; i++) {
         if ((mask & (1 << i)) != 0 && combat.useSkill(session, skills[i], WorldPackets.TARGET_MOB, mobId)) {
            return;
         }
      }
   }

   /**
    * Kỹ năng bot được dùng: ô thanh kỹ năng có bật "Tự động dùng kỹ năng ô N", ô cao trước (chiêu mạnh trước, đánh
    * thường ô 1 sau cùng); không bật ô nào thì ô 1.
    */
   static int[] attackSkills(PlayerData d) {
      return attackSkills(d, d.settings);
   }

   public static int[] attackSkills(PlayerData d, PlayerSettings settings) {
      int[] bar = d.skillBar == null ? new int[0] : d.skillBar;
      boolean[] enabled = settings == null ? null : settings.autoSkillSlotEnabled;
      int[] out = new int[Math.min(bar.length, PlayerSettings.AUTO_SKILL_SLOTS)];
      int n = 0;
      for (int i = out.length - 1; i >= 0; i--) {
         if (bar[i] > 0 && enabled != null && i < enabled.length && enabled[i]) {
            out[n++] = bar[i];
         }
      }
      if (n == 0 && bar.length > 0 && bar[0] > 0) {
         return new int[] {bar[0]};
      }
      return java.util.Arrays.copyOf(out, n);
   }

   private void usePotions(PlayerSession session, PlayerData d, PlayerSettings s, Run run, long now) {
      PlayerStats stats = d.stats;
      if (stats == null) {
         return;
      }
      if (s.autoUseHpItems && stats.maxHp > 0 && d.hp * 100 < stats.maxHp * s.autoHpThresholdPercent
            && now - run.hpPotionAt >= POTION_INTERVAL_MS) {
         run.hpPotionAt = now;
         items.useQuickSlot(session, QUICK_HP);
      }
      if (s.autoUseMpItems && stats.maxMp > 0 && d.mp * 100 < stats.maxMp * s.autoMpThresholdPercent
            && now - run.mpPotionAt >= POTION_INTERVAL_MS) {
         run.mpPotionAt = now;
         items.useQuickSlot(session, QUICK_MP);
      }
      if (s.autoUseStaminaItems && d.activityPoints < PlayerData.STAMINA_PER_SKILL
            && now - run.staminaPotionAt >= POTION_INTERVAL_MS) {
         run.staminaPotionAt = now;
         items.useQuickSlot(session, QUICK_STAMINA);
      }
   }

   // ================================================================== dừng

   /** Dừng vì luật (hết huy hiệu, bị hạ gục): lưu câu tổng kết cho lần vào game sau, đăng xuất. Luồng session. */
   private void stop(PlayerSession session, Run run, String reason) {
      PlayerData d = session.getPlayer();
      if (d != null) {
         summaries.put(d.getAccountId(), summary(d, run, reason));
      }
      accounts.logout(session); // -> loggedOut dọn runs / sessions
   }

   /** Chủ tài khoản đăng nhập lại: dừng + đăng xuất trên hàng đợi của session treo. */
   @Override
   public CompletableFuture<String> end(PlayerSession offline) {
      CompletableFuture<String> done = new CompletableFuture<>();
      offline.inbox().execute(() -> {
         try {
            Run run = runs.get(offline);
            PlayerData d = offline.getPlayer();
            String text = run != null && d != null ? summary(d, run, null) : null;
            accounts.logout(offline);
            done.complete(text);
         } catch (Exception e) {
            done.completeExceptionally(e);
         }
      }, gamePool);
      return done;
   }

   @Override
   public void loggedOut(PlayerSession offline) {
      runs.remove(offline);
      pending.remove(offline);
      offline.setOfflineTraining(false);
      SessionManager s = sessions;
      if (s != null && offline.isDetached()) {
         s.forget(offline);
      }
   }

   /** Vào game: báo tổng kết lượt treo đã dừng lúc vắng mặt (PlayerService.onLogin). */
   public void onLogin(PlayerSession session, PlayerData d) {
      pending.remove(session);
      String text = summaries.remove(d.getAccountId());
      if (text != null) {
         session.send(Packets.notice(text));
      }
   }

   static String summary(PlayerData d, Run run, String reason) {
      long ms = System.currentTimeMillis() - run.startAt;
      StringBuilder sb = new StringBuilder("Luyện tập Offline ").append(duration(ms));
      if (d.level > run.startLevel) {
         sb.append(": lên ").append(d.level - run.startLevel).append(" cấp (cấp ").append(d.level).append(')');
      } else if (d.exp > run.startExp) {
         sb.append(": +").append(String.format("%,d", d.exp - run.startExp).replace(',', '.')).append(" EXP");
      }
      if (reason != null) {
         sb.append(". Đã dừng: ").append(reason);
      }
      return sb.append('.').toString();
   }

   static String duration(long ms) {
      long minutes = Math.max(0, ms) / 60_000;
      long hours = minutes / 60;
      if (hours >= 24) {
         return hours / 24 + " ngày " + hours % 24 + " giờ";
      }
      if (minutes == 0) {
         return Math.max(0, ms) / 1000 + " giây";
      }
      return hours > 0 ? hours + " giờ " + minutes % 60 + " phút" : minutes + " phút";
   }
}
