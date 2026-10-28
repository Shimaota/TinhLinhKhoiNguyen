package com.tlnt.server.bot;

import com.tlnt.server.combat.CombatService;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.data.model.MapPack;
import com.tlnt.server.data.model.SkillPack;
import com.tlnt.server.db.Database;
import com.tlnt.server.fishing.Fishing;
import com.tlnt.server.fishing.FishingService;
import com.tlnt.server.item.ItemBuffs;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.WeaponClass;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.offline.OfflineTrainingService;
import com.tlnt.server.party.PartyService;
import com.tlnt.server.player.ClassSkills;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerSettings;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.player.SkillData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.skill.SkillService;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;
import com.tlnt.server.world.WorldPlayer;
import com.tlnt.server.world.Zone;
import com.tlnt.server.worldboss.WorldBossService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Người chơi giả cho server trông đông: nhân vật thật trong RAM ({@link PlayerData#bot}) + phiên không kết nối
 * ({@link PlayerSession#bot}), vào khu bằng đúng {@code Zone.join} -> người thật thấy xuất hiện, chạy, đánh quái, nhặt
 * đồ, lên cấp, cộng điểm, lập tổ đội, câu cá, săn boss, đánh trả khi bị PK, chết, nói chuyện. Mọi hành động đi qua code
 * của người chơi thật (CombatService, ItemService, SkillService, PartyService, FishingService) — không viết lại luật.
 *
 * <h2>Loại bot ({@link Bot.Kind})</h2>
 * Treo offline (não Luyện tập Offline), Auto đánh (như nút Tự đánh của client), Đứng chơi, Đi dạo, AFK, Câu cá, Săn boss.
 *
 * <h2>Rẻ CPU / RAM</h2>
 * <ul>
 *   <li><b>Ngủ khi không ai nhìn</b>: khu không có người thật ({@link Zone#humans} = 0) -> bot không làm gì. 1 nhịp
 *       scheduler {@link #STEP_MS} chỉ đọc vài số volatile / bot.</li>
 *   <li>Mỗi bot có nhịp phản xạ riêng ngẫu nhiên ({@link Bot#nextStepAt}) — vừa giống người (không cả bãi ra chiêu cùng
 *       lúc) vừa trải đều việc ra các nhịp.</li>
 *   <li>Gửi gói cho bot = bỏ ngay, không mã hoá; không lưu DB; không nằm trong danh sách session; không nhiệm vụ /
 *       thành tựu / xếp hạng; quái chủ động không lao vào bot.</li>
 * </ul>
 */
public final class BotService {

   /** Nhịp scheduler (ms): bot nào tới lượt ({@link Bot#nextStepAt}) và đang thức thì chạy 1 bước. */
   static final long STEP_MS = 100;
   static final long MANAGE_MS = 1_000;
   /** Kiểm tra boss thế giới mỗi bấy nhiêu nhịp quản lý. */
   static final int BOSS_EVERY = 5;
   /** Id nhân vật bot: trên mọi id DB, dưới 2^30 (id thú cưng = Integer.MIN_VALUE / 2 − id không tràn). */
   static final int ID_BASE = 900_000_000;
   static final int ID_END = 1_000_000_000;
   static final long REGEN_MS = 5_000;
   static final long SEEK_TIMEOUT_MS = 3_000;
   static final long POTION_INTERVAL_MS = 1_000;
   static final int QUICK_HP = 3;
   static final int QUICK_MP = 4;
   static final int QUICK_STAMINA = 1;
   static final long ZONE_CHAT_GAP_MS = 25_000;
   static final int OUTGROW_LEVELS = 6;
   static final int KEEP_FREE = 2;
   /** Bị PK: quên kẻ đánh mình sau bấy lâu không bị đánh thêm. */
   static final long PK_MEMORY_MS = 12_000;
   /** Trong phần "đứng chơi / đi dạo" của cấu hình: % thành lữ khách, % thành cặp đấu tập (phần còn lại đứng / dạo). */
   static final int TRAVEL_SHARE = 22;
   static final int SPAR_SHARE = 12;
   /** Đấu tập: 2 bot đứng trong bấy nhiêu px mới thách đấu; 1 trận tối đa bấy lâu. */
   static final float SPAR_RANGE_PX = 600;
   static final long SPAR_DUEL_MS = 75_000;
   /** Lữ khách: tối đa bấy lâu để chạy tới cửa (kẹt / nhảy không tới thì bỏ). */
   static final long DOOR_TIMEOUT_MS = 30_000;
   static final float DOOR_NEAR_PX = 180;
   /** Đi theo người thật: tìm trong bán kính, tối đa bấy nhiêu bot theo cùng 1 người. */
   static final float FOLLOW_FIND_PX = 700;
   static final float FOLLOW_KEEP_PX = 1_300;
   static final int FOLLOW_MAX_PER_HUMAN = 2;

   private static final String[] CHAT_IDLE = {"ai pt up lv ko", "có ai đi Tế đàn ko", "lag quá ae", "boss ra chưa ae",
         "bộ lạc nào tuyển người ko", "xin 1 slot pt", "server đông ghê", "hi ae", "ae lv bao nhiêu rồi", "đợi bạn tí",
         "ai giao dịch ko", "thu mua đá cường hóa", "tối nay đi boss ko ae", "map nào up nhanh nhỉ",
         "mới chơi, ae chỉ với", "hôm nay vui ghê", "ai câu cá ko", "bán đồ giá rẻ", "đi Lôi vực ko ae", "kkk",
         "ae ơi Huy hiệu mua ở đâu", "ngủ thôi ae", "gg", "ai đi Rừng Linh Mộc ko"};
   private static final String[] CHAT_GRIND = {"quái hồi lâu thế", "rớt đồ ngon ko ae", "up mệt quá", "sắp lên lv rồi",
         "ai pt ko", "hết thuốc rồi", "con này trâu thế", "map này đông quá", "+1 lv", "ae nhường quái tí",
         "đánh mãi ko rớt", "pt ko ae", "lại chết", "exp ít quá", "mai up tiếp", "đỏ thế"};
   private static final String[] CHAT_PARTY = {"ae gom quái vào", "đứng gần nhau ăn exp", "ai hết thuốc ko", "pt đủ chưa",
         "kéo quái đi ae", "lên lv rồi kk"};
   private static final String[] CHAT_FISH = {"cá cắn chậm quá", "câu được con to kk", "ai câu cá ko", "mồi hết rồi",
         "câu chill ghê", "lại hụt"};
   private static final String[] CHAT_AFK = {"afk", "treo tí", "đi ăn cơm", "brb"};
   private static final String[] CHAT_BOSS = {"boss ra rồi ae", "đánh boss thôi", "boss trâu thế", "ai có bộ lạc ko",
         "boss sắp chết", "ae dồn dame"};
   private static final String[] CHAT_TRAVEL = {"map này đẹp ghê", "đi đâu cũng đông", "đang tìm bãi up", "lạc đường rồi kk",
         "map nào có boss ae", "đi dạo tí", "qua map bên kia xem"};
   private static final String[] CHAT_SPAR_ASK = {"solo ko", "đấu tập tí ko", "thử sức ko bạn", "pk tập nhé", "1 kèo ko"};
   private static final String[] CHAT_SPAR_OK = {"ok chiến", "lên", "chơi luôn", "ok", "chấp luôn"};
   private static final String[] CHAT_SPAR_END = {"gg", "hay đấy", "lại kèo nữa ko", "thua rồi kk", "ăn may thôi", "gg wp",
         "suýt nữa"};
   private static final String[] CHAT_FOLLOW = {"chào bạn", "bạn ơi pt ko", "nhân vật đẹp thế", "cho mình đi theo với",
         "bạn lv bao nhiêu rồi", "kết bạn nhé", "bạn mới chơi à", "đi đâu đấy bạn"};
   private static final String[] CHAT_PK = {"đánh gì đấy", "pk à", "ok chơi", "chạy thôi", "gà thế", "thôi tha"};

   private static volatile BotService instance;
   private static final Set<String> BOT_NAMES = ConcurrentHashMap.newKeySet();

   private final World world;
   private final CombatService combat;
   private final ItemService items;
   private final ItemBuffs buffs;
   private final SkillService skills;
   private final Database playerDb;
   private final BotRepository repository;
   private final Executor blockingPool;
   /** Câu chat theo loại bot (bot_profile.chat_lines), thiếu loại nào dùng câu mặc định. */
   private volatile Map<String, String[]> chatLines = Map.of();
   private final ScheduledExecutorService scheduler;
   private final Executor gamePool;
   private volatile PartyService parties;
   private volatile FishingService fishing;
   private volatile WorldBossService bosses;

   private volatile BotConfig config = BotConfig.defaults();
   private volatile BotPlaces places;
   private volatile BotCosmetics looks = BotCosmetics.empty();
   private volatile boolean ready;
   private final Map<Integer, Bot> bots = new ConcurrentHashMap<>();
   private final Set<String> realNames = ConcurrentHashMap.newKeySet();
   private final AtomicInteger nextId = new AtomicInteger(ID_BASE);
   private final AtomicInteger spawning = new AtomicInteger();
   private final Map<Integer, Long> zoneChatAt = new ConcurrentHashMap<>();
   private int manageRound;
   /** Thống kê từ lúc khởi động (admin panel). */
   private final AtomicInteger pkFights = new AtomicInteger();
   private final AtomicInteger partiesMade = new AtomicInteger();
   private final AtomicInteger spawnedTotal = new AtomicInteger();

   public BotService(World world, CombatService combat, ItemService items, ItemBuffs buffs, Database playerDb,
         Database dataDb, ScheduledExecutorService scheduler, Executor gamePool, Executor blockingPool) {
      this.world = world;
      this.combat = combat;
      this.items = items;
      this.buffs = buffs;
      this.skills = new SkillService(world);
      this.playerDb = playerDb;
      this.repository = new BotRepository(dataDb);
      this.blockingPool = blockingPool;
      this.scheduler = scheduler;
      this.gamePool = gamePool;
      instance = this;
      World.setBotHurtListener(this::attacked);
      blockingPool.execute(this::load);
      scheduler.scheduleAtFixedRate(this::tick, STEP_MS, STEP_MS, TimeUnit.MILLISECONDS);
      scheduler.scheduleAtFixedRate(this::manage, MANAGE_MS, MANAGE_MS, TimeUnit.MILLISECONDS);
      // Sửa bot_profile bằng SQL / tab Dữ liệu game: tối đa 1 phút sau có hiệu lực.
      scheduler.scheduleAtFixedRate(() -> blockingPool.execute(this::reloadProfile), 60, 60, TimeUnit.SECONDS);
   }

   /** Dịch vụ tạo ở chỗ khác trong GameServer nối vào (null = tính năng đó của bot tắt). */
   public void connect(PartyService parties, FishingService fishing, WorldBossService bosses) {
      this.parties = parties;
      this.fishing = fishing;
      this.bosses = bosses;
      PartyService.botHooks = new PartyService.BotHooks() {
         @Override
         public void invited(int botId, int fromId) {
            onPartyInvite(botId, fromId);
         }

         @Override
         public void joinRequested(int leaderBotId, int requesterId) {
            onJoinRequest(leaderBotId, requesterId);
         }
      };
   }

   // ================================================================== tổ đội với người thật

   private static final String[] CHAT_JOIN = {"ok vào", "hi ae", "pt up nhé", "cảm ơn đã mời", "vào rồi nè"};
   private static final String[] CHAT_WELCOME = {"vào đi", "chào bạn", "ok", "pt đủ người rồi", "up chung nhé"};

   /**
    * Người thật mời bot vào tổ đội: bot "đọc hộp mời" 1,5-6 s rồi bấm Đồng ý như người — đang up / săn boss dễ nhận, đứng
    * chơi / câu cá ít hơn, AFK không trả lời; chênh cấp nhiều thì ngại. Từ chối = không bấm gì (như client).
    */
   private void onPartyInvite(int botId, int fromId) {
      Bot b = bots.get(botId);
      WorldPlayer inviter = world.online(fromId);
      PartyService ps = parties;
      if (b == null || b.leaving || inviter == null || ps == null) {
         return;
      }
      Random rnd = ThreadLocalRandom.current();
      int chance = switch (b.kind) {
         case GRIND_OFFLINE, GRIND_AUTO, BOSS -> 75;
         case IDLE, WANDER, TRAVEL -> 45;
         case SPAR -> 30;
         case FISH -> 20;
         case AFK -> 0;
      };
      int gap = Math.abs(b.data.level - inviter.data().level);
      chance = gap > 20 ? chance / 3 : gap > 10 ? chance / 2 : chance;
      boolean yes = rnd.nextInt(100) < chance;
      scheduler.schedule(() -> b.session.inbox().execute(() -> {
         if (b.leaving || b.data.party != null || b.session.getPlayer() != b.data) {
            return;
         }
         if (yes) {
            ps.acceptInvite(b.session);
            if (b.data.party != null && rnd.nextInt(3) == 0) {
               world.chat(b.session, CHAT_JOIN[rnd.nextInt(CHAT_JOIN.length)]);
            }
         }
      }, gamePool), 1_500 + rnd.nextInt(4_500), TimeUnit.MILLISECONDS);
   }

   /**
    * Người thật bấm "Xin vào" tổ đội có trưởng nhóm là bot: 2-7 s sau bot duyệt như trưởng nhóm thật (đang up / săn boss
    * thường nhận, AFK không duyệt) — trước đây yêu cầu treo mãi ở "Chờ duyệt".
    */
   private void onJoinRequest(int leaderBotId, int requesterId) {
      Bot b = bots.get(leaderBotId);
      WorldPlayer requester = world.online(requesterId);
      PartyService ps = parties;
      if (b == null || b.leaving || requester == null || ps == null) {
         return;
      }
      Random rnd = ThreadLocalRandom.current();
      // gần như luôn duyệt (người dùng 2026-10-18: "bot không duyệt"); AFK thì lâu mới thấy hộp
      boolean afk = b.kind == Bot.Kind.AFK;
      if (rnd.nextInt(100) >= (afk ? 60 : 97)) {
         return;
      }
      long delay = afk ? 8_000 + rnd.nextInt(15_000) : 2_000 + rnd.nextInt(4_000);
      approveLater(b, ps, requesterId, delay, 3);
   }

   /** Duyệt {@code requesterId}; bị chặn (chống bấm nhanh / đang bận) mà người đó vẫn chưa vào nhóm thì thử lại. */
   private void approveLater(Bot b, PartyService ps, int requesterId, long delayMs, int triesLeft) {
      scheduler.schedule(() -> b.session.inbox().execute(() -> {
         if (b.leaving || b.data.party == null || b.session.getPlayer() != b.data) {
            return;
         }
         WorldPlayer requester = world.online(requesterId);
         if (requester == null || requester.data().party != null) {
            return; // thoát game / đã vào nhóm khác (hoặc đã vào nhóm này)
         }
         ps.approve(b.session, requesterId);
         if (requester.data().party == b.data.party) {
            Random rnd = ThreadLocalRandom.current();
            if (rnd.nextInt(3) == 0) {
               world.chat(b.session, CHAT_WELCOME[rnd.nextInt(CHAT_WELCOME.length)]);
            }
         } else if (triesLeft > 1) {
            approveLater(b, ps, requesterId, 1_500, triesLeft - 1);
         }
      }, gamePool), delayMs, TimeUnit.MILLISECONDS);
   }

   public static BotService instance() {
      return instance;
   }

   public static boolean nameInUse(String name) {
      return name != null && BOT_NAMES.contains(BotNames.key(name));
   }

   public static void noteRealName(String name) {
      BotService s = instance;
      if (s != null && name != null) {
         s.realNames.add(BotNames.key(name));
      }
   }

   // ================================================================== nạp / cấu hình

   private void load() {
      try (Connection c = playerDb.getConnection()) {
         try (PreparedStatement ps = c.prepareStatement("SELECT name FROM player UNION SELECT name FROM player_name_history");
               ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
               String n = rs.getString(1);
               if (n != null) {
                  realNames.add(BotNames.key(n));
               }
            }
         }
      } catch (SQLException e) {
         Log.error("Bot: khong nap duoc ten nhan vat - bot tat", e);
         return;
      }
      if (!reloadProfile()) {
         Log.warn("Bot: chua co tlnt_data.bot_profile (chay sql/migrations/2026-10-17_bot_profile.sql) - bot tat");
         config = BotConfig.defaults();
      }
      float reach = FishingService.waterReach();
      places = BotPlaces.of(world.fieldMaps(), mapId -> world.fishingSpots(mapId, reach));
      ready = true;
      BotConfig c = config;
      Log.info("Bot nguoi choi: %s, %d bot | %d bai up, %d cho dung, %d cho cau | ngoai trang %d, danh hieu %d, thu %d,"
            + " mat na %d, deo lung %d", c.enabled() ? "BAT" : "tat", c.count(), places.grind.size(), places.idle.size(),
            places.fish.size(), looks.count(BotCosmetics.COSTUME), looks.count(BotCosmetics.TITLE),
            looks.count(BotCosmetics.PET), looks.count(BotCosmetics.MASK), looks.count(BotCosmetics.BACK));
   }

   /** Đọc lại {@code bot_profile} (luồng blocking). @return false nếu chưa có bảng / dòng */
   private boolean reloadProfile() {
      try {
         BotRepository.Profile p = repository.load();
         if (p == null) {
            return false;
         }
         config = p.config();
         looks = p.looks();
         chatLines = Map.copyOf(p.chat());
         return true;
      } catch (SQLException e) {
         Log.warn("Bot: khong doc duoc bot_profile (%s)", e.getMessage());
         return false;
      }
   }

   public BotConfig config() {
      return config;
   }

   /** Admin panel: đổi cài đặt (có hiệu lực ngay, bot vào / ra dần) + ghi bot_profile. Luồng admin (được gọi SQL). */
   public BotConfig setConfig(BotConfig next) throws SQLException {
      BotConfig c = next.normalized();
      repository.save(c);
      config = c;
      Log.info("Admin doi cai dat bot: %s", c);
      return c;
   }

   /** Câu chat loại {@code key} trong bot_profile, thiếu thì {@code fallback}. */
   private String[] lines(String key, String[] fallback) {
      String[] l = chatLines.get(key);
      return l == null || l.length == 0 ? fallback : l;
   }

   // ================================================================== giữ số lượng

   private void manage() {
      try {
         if (!ready) {
            return;
         }
         BotConfig c = config;
         long now = System.currentTimeMillis();
         int target = c.enabled() ? c.count() : 0;
         int budget = c.spawnPerSecond();
         int normal = spawning.get();
         for (Bot b : bots.values()) {
            if (b.kind != Bot.Kind.BOSS && !b.leaving) {
               normal++;
            }
         }
         int excess = normal - target;
         int removed = 0;
         for (Bot b : bots.values()) {
            if (removed >= budget) {
               break;
            }
            boolean hunterOff = b.kind == Bot.Kind.BOSS && (!c.enabled() || c.bossHunters() == 0);
            if (!b.leaving && (hunterOff || now >= b.logoutAt || (excess > 0 && b.kind != Bot.Kind.BOSS))) {
               leave(b);
               removed++;
               if (b.kind != Bot.Kind.BOSS) {
                  excess--;
               }
            }
         }
         int missing = -excess;
         Random rnd = ThreadLocalRandom.current();
         while (missing > 0 && budget > removed) {
            int size = rnd.nextInt(100) < c.partyPercent() * c.grindPercent() / 100 ? 2 + rnd.nextInt(3) : 1;
            // Đấu tập luôn vào theo cặp: chỉ khi còn thiếu ≥ 2 chỗ (không tạo dư rồi bắt bot khác thoát)
            boolean spar = size == 1 && missing >= 2 && budget - removed >= 2
                  && rnd.nextInt(100) < c.idlePercent() * SPAR_SHARE / 100;
            size = spar ? 2 : Math.min(size, Math.min(missing, budget - removed));
            int group = size;
            spawning.addAndGet(group);
            missing -= group;
            budget -= group;
            gamePool.execute(() -> {
               try {
                  if (spar) {
                     spawnSparPair();
                  } else {
                     spawnGroup(group);
                  }
               } catch (Exception e) {
                  Log.error("Loi tao bot", e);
               } finally {
                  spawning.addAndGet(-group);
               }
            });
         }
         if (c.enabled() && c.bossHunters() > 0 && ++manageRound % BOSS_EVERY == 0) {
            huntBosses(c, now, rnd);
         }
      } catch (Exception e) {
         Log.error("Loi quan ly bot", e);
      }
   }

   /** Boss đang sống thiếu thợ săn -> thêm 1 bot / boss / lượt (kéo tới dần như người thật); boss mất -> thợ săn về. */
   private void huntBosses(BotConfig c, long now, Random rnd) {
      WorldBossService service = bosses;
      if (service == null) {
         return;
      }
      List<WorldBossService.AliveBoss> alive = service.aliveBosses(); // cả boss người chơi giả (Tinh linh)
      Set<Integer> ids = new HashSet<>();
      for (WorldBossService.AliveBoss boss : alive) {
         ids.add(boss.id());
         int hunters = 0;
         for (Bot b : bots.values()) {
            if (b.kind == Bot.Kind.BOSS && b.bossId == boss.id() && !b.leaving) {
               hunters++;
            }
         }
         if (hunters < c.bossHunters() && rnd.nextInt(100) < 60) {
            spawning.incrementAndGet();
            gamePool.execute(() -> {
               try {
                  spawnHunter(boss);
               } catch (Exception e) {
                  Log.error("Loi tao bot san boss", e);
               } finally {
                  spawning.decrementAndGet();
               }
            });
         }
      }
      for (Bot b : bots.values()) {
         if (b.kind == Bot.Kind.BOSS && !ids.contains(b.bossId) && b.logoutAt > now + 90_000) {
            b.logoutAt = now + 15_000 + rnd.nextInt(60_000); // boss chết / mất: đứng thêm chút rồi thoát
         }
      }
   }

   // ================================================================== tạo bot

   private Bot.Kind pickKind(BotConfig c, Random rnd) {
      int r = rnd.nextInt(100);
      if (r < c.grindPercent()) {
         return rnd.nextInt(100) < c.autoPercent() ? Bot.Kind.GRIND_AUTO : Bot.Kind.GRIND_OFFLINE;
      }
      if ((r -= c.grindPercent()) < c.fishPercent()) {
         return Bot.Kind.FISH;
      }
      if ((r -= c.fishPercent()) < c.afkPercent()) {
         return Bot.Kind.AFK;
      }
      int k = rnd.nextInt(100 - SPAR_SHARE); // phần đấu tập đã chia ở manage (theo cặp)
      return k < TRAVEL_SHARE ? Bot.Kind.TRAVEL : k < TRAVEL_SHARE + 36 ? Bot.Kind.IDLE : Bot.Kind.WANDER;
   }

   /** 1 bot, hoặc 1 nhóm {@code size} bot up cùng bãi cùng khu rồi tự lập tổ đội. Luồng pool game. */
   private void spawnGroup(int size) {
      BotConfig c = config;
      BotPlaces pl = places;
      if (pl == null) {
         return;
      }
      Random rnd = ThreadLocalRandom.current();
      long now = System.currentTimeMillis();
      Bot.Kind kind = size > 1 ? (rnd.nextBoolean() ? Bot.Kind.GRIND_AUTO : Bot.Kind.GRIND_OFFLINE) : pickKind(c, rnd);
      long life = TimeUnit.MINUTES.toMillis(c.sessionMin() + rnd.nextInt(c.sessionMax() - c.sessionMin() + 1));
      switch (kind) {
         case GRIND_OFFLINE, GRIND_AUTO -> spawnGrinders(c, pl, kind, size, rnd, now, life);
         case FISH -> {
            if (spawnFisher(c, pl, rnd, now, life) == null) {
               spawnIdle(c, pl, Bot.Kind.IDLE, rnd, now, life);
            }
         }
         default -> spawnIdle(c, pl, kind, rnd, now, life);
      }
   }

   private void spawnGrinders(BotConfig c, BotPlaces pl, Bot.Kind kind, int size, Random rnd, long now, long life) {
      BotPlaces.GrindSpot g = null;
      int level = 0;
      for (int attempt = 0; attempt < 8 && g == null; attempt++) {
         BotPlaces.GrindSpot s = pl.pickGrind(rnd, c.minLevel(), c.maxLevel());
         if (s == null) {
            return;
         }
         int lv = BotPlaces.grindLevel(rnd, s, c.maxLevel());
         if (BotProgress.canEnter(lv, s.mapId())) { // map đã mở ở cấp này (nhiệm vụ)
            g = s;
            level = lv;
         }
      }
      if (g == null) {
         return;
      }
      MapPack.Mob mob = GameDataCache.mob(g.mobTemplateId());
      String place = "Up " + (mob != null ? mob.name() : "quái #" + g.mobTemplateId()) + " Lv" + g.mobLevel();
      Bot leader = null;
      int zoneId = -1;
      for (int i = 0; i < size; i++) {
         int lv = i == 0 ? level : Math.max(1, level + rnd.nextInt(5) - 2); // đồng đội cấp xêm xêm
         if (!BotProgress.canEnter(lv, g.mapId())) {
            lv = level;
         }
         long l = i == 0 ? life : leader.logoutAt - now + (rnd.nextInt(600_000) - 300_000); // nhóm về gần cùng lúc
         Bot b = grinder(pl, kind, g, lv, place, rnd, now, l);
         if (b == null) {
            break;
         }
         int z = register(b, c, zoneId);
         if (z < 0) {
            break;
         }
         if (i == 0) {
            leader = b;
            zoneId = z; // cả nhóm vào cùng khu
         } else {
            b.leader = leader;
            leader.followers.add(b);
         }
      }
      if (leader != null && !leader.followers.isEmpty()) {
         formParty(leader, 2_000 + rnd.nextInt(4_000));
      }
   }

   private Bot grinder(BotPlaces pl, Bot.Kind kind, BotPlaces.GrindSpot g, int level, String place, Random rnd,
         long now, long life) {
      int id = allocateId();
      String name = newName(rnd);
      if (name == null) {
         return null;
      }
      BotFactory.Build build = BotFactory.pickBuild(rnd);
      PlayerData d = BotFactory.create(id, name, level, build, looks, config, rnd, now);
      d.mapId = g.mapId();
      float gx = pl.awayFromDoor(g.mapId(), g.x(), g.y(), g.range(), rnd);
      d.x = Float.isNaN(gx) ? g.x() : gx;
      d.y = g.y() + 150;
      int[] templates = new int[] {g.mobTemplateId()};
      if (kind == Bot.Kind.GRIND_AUTO) {
         d.settings.autoAttackRange = 0; // auto client: "Không giới hạn" (đánh con gần nhất trong tầm nhìn)
         int[] all = pl.templatesFor(g.mapId(), level);
         if (all.length > 0) {
            templates = all;
         }
      }
      Bot b = new Bot(kind, PlayerSession.bot(id), d, now, now + Math.max(5 * 60_000, life), place, templates,
            g.mobLevel(), 0, 0, build);
      if (kind == Bot.Kind.GRIND_AUTO) {
         b.reactionMin = 120;
         b.reactionMax = 420;
         b.spread = 1;
         b.randomSkill = true;
         b.relocateAt = now + 120_000 + rnd.nextInt(240_000);
      } else {
         b.reactionMin = 260;
         b.reactionMax = 650;
         b.spread = 2;
      }
      return b;
   }

   private void spawnIdle(BotConfig c, BotPlaces pl, Bot.Kind kind, Random rnd, long now, long life) {
      int level = BotPlaces.idleLevel(rnd, c.minLevel(), c.maxLevel());
      BotPlaces.IdleSpot s = pl.pickIdle(rnd, level);
      if (s == null) {
         return;
      }
      float x = pl.awayFromDoor(s.mapId(), s.x(), s.y(), s.spread(), rnd);
      if (Float.isNaN(x)) {
         return; // chỗ này sát cửa dịch chuyển: lần sau chọn chỗ khác
      }
      spawnAt(c, kind, s, level, x, rnd, now, life, -1);
   }

   /**
    * 1 bot đứng chơi / đi dạo / AFK / lữ khách / đấu tập ở chỗ {@code s}, hoành độ {@code x}, khu {@code zoneId} (-1 = tự
    * chọn). @return null = không vào được
    */
   private Bot spawnAt(BotConfig c, Bot.Kind kind, BotPlaces.IdleSpot s, int level, float x, Random rnd, long now,
         long life, int zoneId) {
      int id = allocateId();
      String name = newName(rnd);
      if (name == null) {
         return null;
      }
      BotFactory.Build build = BotFactory.pickBuild(rnd);
      PlayerData d = BotFactory.create(id, name, level, build, looks, config, rnd, now);
      d.mapId = s.mapId();
      d.x = x;
      d.y = s.y() + 150;
      MapPack.Npc npc = s.npcId() > 0 ? GameDataCache.npc(s.npcId()) : null;
      String place = kind == Bot.Kind.WANDER ? "Đi dạo" : kind == Bot.Kind.AFK ? "AFK"
            : kind == Bot.Kind.TRAVEL ? "Lữ khách" : kind == Bot.Kind.SPAR ? "Đấu tập"
            : npc != null ? "Cạnh " + npc.name() : "Rải rác";
      float spread = kind == Bot.Kind.WANDER || kind == Bot.Kind.TRAVEL ? 1_200 : kind == Bot.Kind.AFK ? 0
            : Math.min(s.spread(), 260);
      Bot b = new Bot(kind, PlayerSession.bot(id), d, now, now + life, place, new int[0], 0, x, spread, build);
      b.reactionMin = kind == Bot.Kind.SPAR ? 220 : 600;
      b.reactionMax = kind == Bot.Kind.SPAR ? 650 : 1_500;
      b.nextActAt = now + rnd.nextInt(kind == Bot.Kind.TRAVEL ? 8_000 : 20_000);
      return register(b, c, zoneId) >= 0 ? b : null;
   }

   /**
    * Cặp đấu tập: 2 bot cấp xêm xêm cùng chỗ đứng chơi, CÙNG khu, cách nhau 140-260 px. Người khởi xướng thách đấu sau
    * 5-20 s; mỗi trận xong nghỉ hồi máu rồi đấu lại. Luồng pool game.
    */
   private void spawnSparPair() {
      BotConfig c = config;
      BotPlaces pl = places;
      if (pl == null) {
         return;
      }
      Random rnd = ThreadLocalRandom.current();
      long now = System.currentTimeMillis();
      int level = BotPlaces.idleLevel(rnd, c.minLevel(), c.maxLevel());
      BotPlaces.IdleSpot s = pl.pickIdle(rnd, level);
      if (s == null) {
         return;
      }
      float x = pl.awayFromDoor(s.mapId(), s.x(), s.y(), s.spread(), rnd);
      if (Float.isNaN(x)) {
         return;
      }
      long life = TimeUnit.MINUTES.toMillis(c.sessionMin() + rnd.nextInt(c.sessionMax() - c.sessionMin() + 1));
      Bot a = spawnAt(c, Bot.Kind.SPAR, s, level, x, rnd, now, life, -1);
      if (a == null) {
         return;
      }
      float bx = pl.awayFromDoor(s.mapId(), x + (rnd.nextBoolean() ? 1 : -1) * (140 + rnd.nextInt(120)), s.y(), 40, rnd);
      int lv = Math.max(1, level + rnd.nextInt(5) - 2);
      Bot b = spawnAt(c, Bot.Kind.SPAR, s, BotProgress.canEnter(lv, s.mapId()) ? lv : level, Float.isNaN(bx) ? x : bx,
            rnd, now, Math.max(5 * 60_000L, a.logoutAt - now + rnd.nextInt(120_000) - 60_000), a.zoneId);
      if (b == null) {
         return; // không có bạn tập: a thành bot đi dạo ({@link #spar})
      }
      a.rival = b;
      b.rival = a;
      a.challenger = true;
      a.nextDuelAt = now + 5_000 + rnd.nextInt(15_000);
   }

   private Bot spawnFisher(BotConfig c, BotPlaces pl, Random rnd, long now, long life) {
      if (fishing == null) {
         return null;
      }
      int level = BotPlaces.idleLevel(rnd, c.minLevel(), c.maxLevel());
      List<float[]> taken = new ArrayList<>();
      for (Bot o : bots.values()) {
         if (o.kind == Bot.Kind.FISH && !o.leaving) {
            taken.add(new float[] {o.data.mapId, o.homeX});
         }
      }
      BotPlaces.FishSpot s = pl.pickFish(rnd, level, taken);
      if (s == null) {
         return null;
      }
      int id = allocateId();
      String name = newName(rnd);
      if (name == null) {
         return null;
      }
      BotFactory.Build build = BotFactory.pickBuild(rnd);
      PlayerData d = BotFactory.create(id, name, level, build, looks, config, rnd, now);
      BotFactory.fishingKit(d, rnd);
      BotFactory.refresh(d);
      float dir = Math.signum(s.waterX() - s.x());
      d.mapId = s.mapId();
      d.x = s.x() - (dir == 0 ? 1 : dir) * 60; // lùi khỏi chỗ câu 1 chút: bước tới chỗ câu = quay mặt ra nước
      d.y = s.y() + 40;
      Bot b = new Bot(Bot.Kind.FISH, PlayerSession.bot(id), d, now, now + life, "Câu cá", new int[0], 0, s.x(), 0, build);
      b.reactionMin = 150;
      b.reactionMax = 320;
      b.nextActAt = now + 2_000 + rnd.nextInt(8_000);
      return register(b, c, -1) >= 0 ? b : null;
   }

   private void spawnHunter(WorldBossService.AliveBoss boss) {
      BotConfig c = config;
      Random rnd = ThreadLocalRandom.current();
      long now = System.currentTimeMillis();
      int level = Math.max(1, boss.level() - 4 + rnd.nextInt(9));
      while (level < 100 && !BotProgress.canEnter(level, boss.mapId())) {
         level++;
      }
      int id = allocateId();
      String name = newName(rnd);
      if (name == null) {
         return;
      }
      BotFactory.Build build = BotFactory.pickBuild(rnd);
      PlayerData d = BotFactory.create(id, name, level, build, looks, config, rnd, now);
      d.mapId = boss.mapId();
      d.settings.autoAttackRange = 0;
      float[] spot = null;
      try { // đứng gần boss (người thật cũng chạy tới chỗ boss rồi mới đánh), không phải ở cửa vào map
         spot = world.worldBossSpot(boss.mapId(), boss.zoneId(), boss.id()).get(300, TimeUnit.MILLISECONDS);
      } catch (Exception e) {
         // khu bận / boss vừa chết: vào ở điểm xuất hiện của map
      }
      if (spot == null) {
         return;
      }
      BotPlaces pl = places;
      float x = spot[0] + (rnd.nextBoolean() ? 1 : -1) * (250 + rnd.nextInt(350));
      if (pl != null) {
         float away = pl.awayFromDoor(boss.mapId(), x, spot[1], 150, rnd);
         x = Float.isNaN(away) ? x : away;
      }
      d.x = x;
      d.y = spot[1] + 200;
      Bot b = new Bot(Bot.Kind.BOSS, PlayerSession.bot(id), d, now, now + 40 * 60_000L, "Săn " + boss.name(),
            new int[] {boss.mobTemplateId()}, boss.level(), 0, 0, build);
      b.bossId = boss.id();
      b.charBoss = boss.character();
      b.reactionMin = 200;
      b.reactionMax = 600;
      b.spread = 0;
      b.randomSkill = rnd.nextBoolean();
      register(b, c, boss.zoneId());
   }

   /** Đưa bot vào thế giới. @return số khu, -1 = hết chỗ (bot bị bỏ) */
   private int register(Bot b, BotConfig c, int zoneId) {
      PlayerData d = b.data;
      MapTemplate t = world.mapTemplate(d.mapId);
      if (t == null) {
         return -1;
      }
      int maxPerZone = Math.max(1, t.maxPlayersPerZone() * c.zonePercent() / 100); // mức trần; GameMap bốc riêng từng khu
      if (b.kind == Bot.Kind.BOSS) {
         maxPerZone = t.maxPlayersPerZone(); // thợ săn phải vào đúng khu boss
      }
      String key = BotNames.key(d.name);
      BOT_NAMES.add(key);
      b.session.bindPlayerIfLoggedIn(null, d);
      b.nextStepAt = System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(3_000);
      bots.put(d.getId(), b);
      int zone = world.enterBot(b.session, d, zoneId, maxPerZone, KEEP_FREE);
      if (zone < 0) {
         bots.remove(d.getId(), b);
         BOT_NAMES.remove(key);
         return -1;
      }
      b.zoneId = zone;
      spawnedTotal.incrementAndGet();
      Log.debug("Bot #%d '%s' Lv%d %s vao map %d khu %d", d.getId(), Log.ascii(d.name), d.level, b.kind.key, d.mapId, zone);
      return zone;
   }

   private String newName(Random rnd) {
      return BotNames.next(rnd, k -> BOT_NAMES.contains(k) || realNames.contains(k));
   }

   private int allocateId() {
      while (true) {
         int id = nextId.getAndIncrement();
         if (id >= ID_END) {
            nextId.compareAndSet(id + 1, ID_BASE);
            continue;
         }
         if (!bots.containsKey(id) && world.online(id) == null) {
            return id;
         }
      }
   }

   /** Trưởng nhóm tạo tổ đội rồi mời từng người (1,2 s / lời mời như người bấm), thành viên bấm Đồng ý. */
   private void formParty(Bot leader, long delayMs) {
      PartyService ps = parties;
      if (ps == null) {
         return;
      }
      scheduler.schedule(() -> leader.session.inbox().execute(() -> {
         if (leader.leaving || leader.data.party != null) {
            return;
         }
         ps.create(leader.session);
         if (leader.data.party == null) {
            return;
         }
         partiesMade.incrementAndGet();
         long at = 1_200;
         for (Bot f : leader.followers) {
            scheduler.schedule(() -> leader.session.inbox().execute(() -> {
               if (leader.leaving || f.leaving) {
                  return;
               }
               ps.invite(leader.session, f.data.getId());
               scheduler.schedule(() -> f.session.inbox().execute(() -> ps.acceptInvite(f.session), gamePool),
                     600 + ThreadLocalRandom.current().nextInt(2_000), TimeUnit.MILLISECONDS);
            }, gamePool), at, TimeUnit.MILLISECONDS);
            at += 1_200;
         }
      }, gamePool), delayMs, TimeUnit.MILLISECONDS);
   }

   /** Thoát game như người thật. Gọi từ mọi luồng; chạy trên hàng đợi của bot. */
   private void leave(Bot b) {
      if (b.leaving) {
         return;
      }
      b.leaving = true;
      b.session.inbox().execute(() -> {
         PartyService ps = parties;
         if (ps != null) {
            ps.onLogout(b.data);
         }
         world.leave(b.data);
         b.session.detachPlayer();
         bots.remove(b.data.getId(), b);
         BOT_NAMES.remove(BotNames.key(b.data.name));
      }, gamePool);
   }

   // ================================================================== PK

   /** Luồng tick khu: bot {@code botId} vừa bị người {@code attackerId} đánh. */
   private void attacked(int botId, int attackerId) {
      Bot b = bots.get(botId);
      if (b != null) {
         b.hitBy = attackerId;
         b.hitAt = System.currentTimeMillis();
         b.nextStepAt = Math.min(b.nextStepAt, b.hitAt + 300 + ThreadLocalRandom.current().nextInt(900)); // giật mình
      }
   }

   /** @return true nếu nhịp này dành cho việc bị PK (đánh trả / bỏ chạy). */
   private boolean handlePk(Bot b, PlayerData d, long now, Random rnd) {
      int foe = b.hitBy;
      if (foe == 0 || !config.pkFightBack()) {
         return false;
      }
      if (now - b.hitAt > PK_MEMORY_MS) {
         b.hitBy = 0;
         b.foe = 0;
         return false;
      }
      WorldPlayer enemy = world.online(foe);
      if (enemy == null || enemy.data().isDead()) {
         b.hitBy = 0;
         b.foe = 0;
         return false;
      }
      if (b.foe != foe) { // lần đầu bị người này đánh: đủ sức thì đánh lại, yếu hơn nhiều thì chạy
         b.foe = foe;
         int gap = enemy.data().level - d.level;
         boolean strong = d.stats != null && enemy.stats() != null && d.stats.maxHp * 2 >= enemy.stats().maxHp;
         b.fight = gap <= 3 && strong ? rnd.nextInt(100) < 85 : gap <= 8 ? rnd.nextInt(100) < 35 : rnd.nextInt(100) < 8;
         b.pkFights++;
         pkFights.incrementAndGet();
         if (rnd.nextInt(100) < 40) {
            String[] pk = lines("pk", CHAT_PK);
            world.chat(b.session, pk[rnd.nextInt(pk.length)]);
         }
         if (!b.fight) {
            WorldPlayer me = b.world;
            float x = me == null ? b.homeX : me.x() + (me.x() < enemy.x() ? -900 : 900);
            strollAvoidingDoors(b, x, 200, rnd);
            b.pauseUntil = now + 3_000;
            return true;
         }
      }
      if (!b.fight) {
         return now < b.pauseUntil;
      }
      chase(b, d, foe);
      return true;
   }

   private void chase(Bot b, PlayerData d, int foe) {
      if (b.seeking && System.currentTimeMillis() - b.seekAt < SEEK_TIMEOUT_MS) {
         return;
      }
      b.seeking = true;
      b.seekAt = System.currentTimeMillis();
      int[] list = OfflineTrainingService.attackSkills(d, d.settings);
      int[][] reach = OfflineTrainingService.reach(d, list);
      PlayerSession session = b.session;
      world.botChase(session, foe, reach[0], reach[1], (id, mask) -> session.inbox().execute(() -> {
         b.seeking = false;
         if (id < 0) {
            b.hitBy = 0;
            b.foe = 0;
            return;
         }
         if (mask != 0 && !b.leaving && session.getPlayer() == d && !d.isDead()) {
            cast(b, list, mask, WorldPackets.TARGET_PLAYER, id);
         }
      }, gamePool));
   }

   // ================================================================== nhịp "não"

   private void tick() {
      if (bots.isEmpty()) {
         return;
      }
      try {
         long now = System.currentTimeMillis();
         for (Bot b : bots.values()) {
            if (b.leaving || now < b.nextStepAt) {
               continue;
            }
            WorldPlayer wp = b.world;
            if (wp == null) {
               b.world = wp = world.online(b.data.getId());
               if (wp == null) {
                  continue; // đang vào khu
               }
            }
            Zone zone = wp.zone();
            if (zone == null || zone.humans() == 0) {
               continue; // không người thật nào nhìn thấy: ngủ
            }
            if (b.queued.compareAndSet(false, true)) {
               b.session.inbox().execute(() -> {
                  try {
                     step(b, zone);
                  } catch (Exception e) {
                     Log.error("Loi bot #" + b.data.getId(), e);
                  } finally {
                     b.queued.set(false);
                  }
               }, gamePool);
            }
         }
      } catch (Exception e) {
         Log.error("Loi nhip bot", e);
      }
   }

   /** 1 nhịp của 1 bot đang thức (luồng session của bot). */
   void step(Bot b, Zone zone) {
      PlayerData d = b.data;
      if (b.leaving || b.session.getPlayer() != d || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      Random rnd = ThreadLocalRandom.current();
      b.nextStepAt = now + b.reactionMin + rnd.nextInt(Math.max(1, b.reactionMax - b.reactionMin));
      if (now - b.regenAt >= REGEN_MS) {
         b.regenAt = now;
         buffs.regenBot(b.session);
      }
      if (d.isDead()) {
         if (b.reviveAt == 0) {
            b.reviveAt = now + 4_000 + rnd.nextInt(10_000); // đứng nhìn màn hình "Kiệt sức" vài giây
            b.deaths++;
            b.hitBy = 0;
            b.foe = 0;
         } else if (now >= b.reviveAt) {
            b.reviveAt = 0;
            b.target = -1;
            if (items.consumeOne(b.session, ItemService.REVIVE_HERB_TEMPLATE_ID)) {
               world.reviveHere(b.session, ItemService.REVIVE_HERB_HP_PERCENT);
            } else {
               leave(b); // hết Cỏ hồi sinh: thoát game
            }
         }
         return;
      }
      if (b.kind.fighter || b.hitBy != 0) {
         usePotions(b, d, d.settings, now);
      }
      if (handlePk(b, d, now, rnd)) {
         return;
      }
      chat(b, zone, now, rnd);
      if (now >= b.nextSpendAt) {
         b.nextSpendAt = now + 20_000 + rnd.nextInt(70_000); // lên cấp xong 1 lúc mới mở bảng cộng điểm
         if (d.talentPoints > 0) {
            spendPoints(b, d);
         }
      }
      switch (b.kind) {
         case GRIND_OFFLINE, GRIND_AUTO, BOSS -> grind(b, d, now, rnd);
         case FISH -> fish(b, d, now, rnd);
         case IDLE, WANDER -> wander(b, now, rnd);
         case TRAVEL -> travel(b, d, now, rnd);
         case SPAR -> spar(b, d, now, rnd);
         case AFK -> {
         }
      }
   }

   // ================================================================== lữ khách

   /**
    * Lữ khách: chọn 1 cửa dịch chuyển dẫn sang map thường đã mở ở cấp mình -> chạy bộ tới cửa -> qua cửa như người chạm
    * cửa ({@link World#botUseDoor}, giữ chỗ theo trần bot). Thỉnh thoảng đổi khu / dạo quanh / chào + đi theo người thật.
    */
   private void travel(Bot b, PlayerData d, long now, Random rnd) {
      WorldPlayer me = b.world;
      if (me == null) {
         return;
      }
      if (b.doorPos >= 0) {
         walkToDoor(b, d, me, now, rnd);
         return;
      }
      if (b.followId != 0 && now < b.followUntil) {
         follow(b, now, rnd);
         return;
      }
      b.followId = 0;
      if (now < b.nextActAt) {
         return;
      }
      b.nextActAt = now + 5_000 + rnd.nextInt(14_000);
      int r = rnd.nextInt(100);
      if (r < 50 && pickDoor(b, d, me, now, rnd)) {
         return;
      }
      if (r < 60) {
         int maxPerZone = Math.max(1, mapCap(d.mapId) * config.zonePercent() / 100);
         world.botChangeZone(b.session, maxPerZone, KEEP_FREE, z -> { });
         return;
      }
      if (r < 70) {
         startFollow(b);
         return;
      }
      strollAvoidingDoors(b, me.x(), 900, rnd);
   }

   private void walkToDoor(Bot b, PlayerData d, WorldPlayer me, long now, Random rnd) {
      MapTemplate t = world.mapTemplate(d.mapId);
      MapTemplate.Waypoint wp = t == null ? null : t.waypointAt(b.doorPos);
      if (wp == null || now > b.doorDeadline) { // kẹt / không lên được tầng có cửa: thôi, dạo chỗ khác
         b.doorPos = -1;
         b.nextActAt = now + 3_000 + rnd.nextInt(8_000);
         strollAvoidingDoors(b, me.x(), 600, rnd);
         return;
      }
      if (!wp.contains(me.x(), me.y(), DOOR_NEAR_PX)) {
         if (now >= b.doorWalkAt) { // đặt lại đích mỗi vài giây (bị đẩy / rơi khỏi đường)
            b.doorWalkAt = now + 4_000;
            world.botWalkTo(b.session, b.doorX, b.doorY);
         }
         return;
      }
      int pos = b.doorPos;
      b.doorPos = -1;
      MapTemplate target = world.mapTemplate(wp.targetMapId());
      int maxPerZone = target == null ? 1 : Math.max(1, target.maxPlayersPerZone() * config.zonePercent() / 100);
      PlayerSession session = b.session;
      world.botUseDoor(session, pos, maxPerZone, KEEP_FREE, mapId -> session.inbox().execute(() -> {
         long at = System.currentTimeMillis();
         Random r = ThreadLocalRandom.current();
         if (mapId < 0) { // map đầy bot / vừa chuyển: lát nữa tính tiếp
            b.nextActAt = at + 4_000 + r.nextInt(6_000);
            return;
         }
         b.trips++;
         b.nextActAt = at + 2_500 + r.nextInt(4_000); // vừa tới: đứng nhìn chút rồi mới đi tiếp
         if (config.chat() && r.nextInt(100) < 12) {
            String[] lines = lines("travel", CHAT_TRAVEL);
            world.chat(session, lines[r.nextInt(lines.length)]);
         }
      }, gamePool));
   }

   /** Chọn ngẫu nhiên 1 cửa dẫn tới map thường đã mở ở cấp mình. @return true nếu đã bắt đầu chạy tới */
   private boolean pickDoor(Bot b, PlayerData d, WorldPlayer me, long now, Random rnd) {
      MapTemplate t = world.mapTemplate(d.mapId);
      if (t == null || t.waypoints() == null || t.waypoints().isEmpty()) {
         return false;
      }
      List<Integer> ok = new ArrayList<>();
      for (int i = 0; i < t.waypoints().size(); i++) {
         MapTemplate.Waypoint wp = t.waypoints().get(i);
         Integer to = wp.targetMapId();
         if (to != null && to != d.mapId && world.botCanVisit(to) && BotProgress.canEnter(d.level, to)
               && !wp.contains(me.x(), me.y(), DOOR_NEAR_PX)) { // không quay lại ngay cửa vừa bước ra
            ok.add(i);
         }
      }
      if (ok.isEmpty()) {
         return false;
      }
      int pos = ok.get(rnd.nextInt(ok.size()));
      MapTemplate.Waypoint wp = t.waypoints().get(pos);
      b.doorPos = pos;
      b.doorX = wp.x() + wp.w() / 2;
      b.doorY = wp.y() + Math.min(wp.h(), 200) * 0.5f;
      b.doorDeadline = now + DOOR_TIMEOUT_MS;
      b.doorWalkAt = now + 4_000;
      world.botWalkTo(b.session, b.doorX, b.doorY);
      return true;
   }

   private int mapCap(int mapId) {
      MapTemplate t = world.mapTemplate(mapId);
      return t == null ? 1 : t.maxPlayersPerZone();
   }

   // ================================================================== đấu tập

   /**
    * Đấu tập với {@link Bot#rival}: người khởi xướng lại gần, hỏi ("solo ko"), thách đấu ({@link World#botDuel} — đúng
    * luồng thách đấu của người thật, HP dừng ở 1, cả khu thấy cờ đấu); trong trận cả 2 đuổi nhau ra chiêu
    * {@code TARGET_PLAYER}; xong thì chat "gg", nghỉ hồi máu 15-45 s rồi đấu lại. Bạn tập thoát / đổi khu -> đi dạo.
    */
   private void spar(Bot b, PlayerData d, long now, Random rnd) {
      Bot r = b.rival;
      WorldPlayer me = b.world;
      WorldPlayer other = r == null ? null : r.world;
      if (r == null || r.leaving || me == null || other == null || other.zone() != me.zone()) {
         b.rival = null;
         b.dueling = false;
         wander(b, now, rnd);
         return;
      }
      if (world.inDuel(d.getId())) {
         b.dueling = true;
         chase(b, d, r.data.getId());
         return;
      }
      if (b.dueling) { // trận vừa kết thúc
         b.dueling = false;
         b.seeking = false;
         b.rounds++;
         if (r.data.hp <= 1 && d.hp > 1) {
            b.wins++;
         }
         if (config.chat() && rnd.nextInt(100) < 55) {
            String[] lines = lines("spar_end", CHAT_SPAR_END);
            world.chat(b.session, lines[rnd.nextInt(lines.length)]);
         }
         b.nextDuelAt = now + 15_000 + rnd.nextInt(30_000);
         b.nextActAt = now + 2_000 + rnd.nextInt(3_000);
         return;
      }
      if (!b.challenger || now < b.nextDuelAt) { // nghỉ / chờ bạn tập rủ: thỉnh thoảng bước qua lại cạnh bạn
         if (now >= b.nextActAt) {
            b.nextActAt = now + 5_000 + rnd.nextInt(10_000);
            if (rnd.nextInt(100) < 40) {
               strollAvoidingDoors(b, other.x(), 180, rnd);
            }
         }
         return;
      }
      PlayerStats mine = d.stats;
      PlayerStats theirs = r.data.stats;
      if (mine == null || theirs == null || d.hp * 10 < mine.maxHp * 7 || r.data.hp * 10 < theirs.maxHp * 7
            || r.data.isDead()) {
         b.nextDuelAt = now + 4_000; // chờ cả 2 hồi máu (máu bạn tập đọc gần đúng)
         return;
      }
      if (Math.abs(me.x() - other.x()) > 260 || Math.abs(me.y() - other.y()) > 200) {
         world.botWalkTo(b.session, other.x() + (me.x() < other.x() ? -170 : 170), other.y() + 60);
         b.nextDuelAt = now + 2_500;
         return;
      }
      if (config.chat() && rnd.nextInt(100) < 45) {
         String[] ask = lines("spar", CHAT_SPAR_ASK);
         world.chat(b.session, ask[rnd.nextInt(ask.length)]);
         if (rnd.nextBoolean()) {
            scheduler.schedule(() -> r.session.inbox().execute(() -> {
               if (!r.leaving && r.session.getPlayer() == r.data) {
                  String[] ok = lines("spar_ok", CHAT_SPAR_OK);
                  world.chat(r.session, ok[ThreadLocalRandom.current().nextInt(ok.length)]);
               }
            }, gamePool), 800 + rnd.nextInt(1_500), TimeUnit.MILLISECONDS);
         }
      }
      world.botDuel(b.session, r.data.getId(), SPAR_RANGE_PX, SPAR_DUEL_MS);
      b.nextDuelAt = now + 8_000; // không vào trận được (đối phương bận) thì 8 s sau thử lại
   }

   // ================================================================== chào + đi theo người thật

   /**
    * Tìm người thật gần (tối đa {@value #FOLLOW_MAX_PER_HUMAN} bot theo cùng 1 người) rồi chạy lại, đôi khi chào, đi theo
    * 20-60 s. Trả lời tới sau (luồng khu -> hàng đợi của bot).
    */
   private void startFollow(Bot b) {
      PlayerSession session = b.session;
      world.botFindHuman(session, 0, FOLLOW_FIND_PX, (id, x, y) -> {
         if (id < 0) {
            return;
         }
         long now = System.currentTimeMillis();
         int following = 0;
         for (Bot o : bots.values()) {
            if (o != b && o.followId == id && o.followUntil > now) {
               following++;
            }
         }
         if (following >= FOLLOW_MAX_PER_HUMAN) {
            return;
         }
         session.inbox().execute(() -> {
            if (b.leaving || session.getPlayer() != b.data) {
               return;
            }
            Random r = ThreadLocalRandom.current();
            long at = System.currentTimeMillis();
            b.followId = id;
            b.followUntil = at + 20_000 + r.nextInt(40_000);
            b.nextActAt = at + 1_500;
            world.botWalkTo(session, x + (r.nextBoolean() ? 1 : -1) * (90 + r.nextInt(90)), y + 60);
            if (config.chat() && r.nextInt(100) < 55) {
               String[] lines = lines("follow", CHAT_FOLLOW);
               scheduler.schedule(() -> session.inbox().execute(() -> {
                  if (!b.leaving && session.getPlayer() == b.data) {
                     world.chat(session, lines[ThreadLocalRandom.current().nextInt(lines.length)]);
                  }
               }, gamePool), 1_200 + r.nextInt(2_500), TimeUnit.MILLISECONDS);
            }
         }, gamePool);
      });
   }

   /** Đang đi theo: mỗi 1,5-3,5 s chạy lại cạnh người đó; người đó đi xa / đổi map / chết thì thôi. */
   private void follow(Bot b, long now, Random rnd) {
      if (now < b.nextActAt) {
         return;
      }
      b.nextActAt = now + 1_500 + rnd.nextInt(2_000);
      PlayerSession session = b.session;
      int want = b.followId;
      world.botFindHuman(session, want, FOLLOW_KEEP_PX, (id, x, y) -> session.inbox().execute(() -> {
         if (id != want) {
            b.followId = 0; // mất dấu
            return;
         }
         WorldPlayer me = b.world;
         if (me != null && Math.abs(me.x() - x) < 130 && Math.abs(me.y() - y) < 120) {
            return; // đã đứng cạnh
         }
         float side = me != null && me.x() < x ? -1 : 1;
         world.botWalkTo(session, x + side * (80 + ThreadLocalRandom.current().nextInt(100)), y + 60);
      }, gamePool));
   }

   private void grind(Bot b, PlayerData d, long now, Random rnd) {
      if (b.kind != Bot.Kind.BOSS && d.level > b.spotLevel + OUTGROW_LEVELS) {
         leave(b); // đã vượt bãi: người thật sẽ đổi map — bot mới hợp cấp vào thay
         return;
      }
      if (d.activityPoints < PlayerData.STAMINA_PER_SKILL && ItemService.count(d.bag, BotFactory.DRUMSTICK, now) == 0) {
         leave(b); // hết thể lực, hết Đùi gà
         return;
      }
      if (now < b.pauseUntil) {
         return;
      }
      PlayerStats st = d.stats;
      if (b.kind != Bot.Kind.BOSS && st != null && d.hp * 4 < st.maxHp && d.quickSlots[0] >= 0
            && d.bag.get(d.quickSlots[0]) == null) { // máu dưới 25%, hết Táo đỏ: lùi ra nghỉ hồi máu như người thật
         WorldPlayer me = b.world;
         if (me != null) {
            strollAvoidingDoors(b, me.x() + (rnd.nextBoolean() ? 1 : -1) * (300 + rnd.nextInt(300)), 100, rnd);
         }
         b.target = -1;
         b.pauseUntil = now + 10_000 + rnd.nextInt(10_000);
         return;
      }
      if (rnd.nextInt(1000) < (b.kind == Bot.Kind.GRIND_AUTO ? 2 : 4)) { // đứng lại xem túi / chat / nghỉ tay
         b.pauseUntil = now + 3_000 + rnd.nextInt(9_000);
         return;
      }
      if (b.kind == Bot.Kind.GRIND_AUTO && now >= b.relocateAt && b.leader == null && b.followers.isEmpty()) {
         b.relocateAt = now + 120_000 + rnd.nextInt(300_000); // người bật auto thỉnh thoảng tự chạy sang khúc khác
         WorldPlayer me = b.world;
         if (me != null) {
            strollAvoidingDoors(b, me.x() + (rnd.nextBoolean() ? 1 : -1) * (500 + rnd.nextInt(800)), 150, rnd);
            b.target = -1;
            b.pauseUntil = now + 2_500 + rnd.nextInt(2_500);
            return;
         }
      }
      if (b.seeking && now - b.seekAt < SEEK_TIMEOUT_MS) {
         return;
      }
      b.seeking = true;
      b.seekAt = now;
      PlayerSettings s = d.settings;
      int[] list = OfflineTrainingService.attackSkills(d, s);
      int[][] reach = OfflineTrainingService.reach(d, list);
      PlayerSession session = b.session;
      boolean boss = b.kind == Bot.Kind.BOSS;
      if (boss && b.charBoss) { // Tinh linh (boss người chơi giả): đuổi theo boss, đánh bằng TARGET_PLAYER như người thật
         world.botChaseWorldBoss(session, b.bossId, reach[0], reach[1], true,
               (entityId, mask) -> session.inbox().execute(() -> {
                  b.seeking = false;
                  b.target = -1;
                  if (mask != 0 && entityId != -1 && !b.leaving && session.getPlayer() == d && !d.isDead()) {
                     cast(b, list, mask, WorldPackets.TARGET_PLAYER, entityId);
                  }
               }, gamePool));
         return;
      }
      world.offlineSeek(session, new World.OfflineQuery(b.templates, s.autoAvoidEliteMobs && !boss, !boss, b.target,
            s.autoPickupItems, reach[0], reach[1], s.autoAttackRange, b.spread, boss),
            (mobId, mask) -> session.inbox().execute(() -> {
               b.seeking = false;
               b.target = mobId;
               if (mask != 0 && mobId >= 0 && !b.leaving && session.getPlayer() == d && !d.isDead()) {
                  cast(b, list, mask, WorldPackets.TARGET_MOB, mobId);
               }
            }, gamePool));
   }

   /**
    * Ra chiêu với tới ({@code mask} bit i = {@code list[i]}): treo offline = chiêu ô cao trước; auto đánh = thử theo thứ
    * tự NGẪU NHIÊN như client (chiêu đang hồi thì CombatService từ chối, thử chiêu kế).
    */
   private void cast(Bot b, int[] list, int mask, int targetKind, int targetId) {
      int n = list.length;
      int[] order = new int[n];
      for (int i = 0; i < n; i++) {
         order[i] = i;
      }
      if (b.randomSkill) {
         Random rnd = ThreadLocalRandom.current();
         for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
         }
      }
      for (int i : order) {
         if ((mask & (1 << i)) != 0 && combat.useSkill(b.session, list[i], targetKind, targetId)) {
            return;
         }
      }
   }

   private void fish(Bot b, PlayerData d, long now, Random rnd) {
      FishingService fs = fishing;
      if (fs == null || !FishingService.holdingRod(d)) {
         return;
      }
      Fishing f = d.fishing;
      if (!b.faced) { // bước ra mép nước (client quay mặt theo hướng đi) rồi mới quăng cần
         b.faced = true;
         world.botStroll(b.session, b.homeX);
         b.nextActAt = Math.max(b.nextActAt, now + 1_500 + rnd.nextInt(2_000));
         return;
      }
      if (f == null) {
         if (now < b.nextActAt) {
            return;
         }
         if (ItemService.count(d.bag, FishingService.config().baitItem(), now) <= 0 || d.bag.firstEmpty() < 0) {
            leave(b); // hết mồi / đầy túi: về thôi
            return;
         }
         fs.press(b.session); // quăng cần
         b.casts++;
         b.nextActAt = now + 4_000 + rnd.nextInt(6_000);
         return;
      }
      if (f.phase() == Fishing.Phase.HOOKED) {
         if (b.hookPressAt == 0) {
            b.hookPressAt = now + 250 + rnd.nextInt(1_400); // nhìn kim chạy rồi mới bấm: trúng / hụt như người
         } else if (now >= b.hookPressAt) {
            b.hookPressAt = 0;
            fs.press(b.session);
            b.nextActAt = now + 2_500 + rnd.nextInt(5_000);
         }
      }
   }

   private void wander(Bot b, long now, Random rnd) {
      if (b.followId != 0 && now < b.followUntil) {
         follow(b, now, rnd);
         return;
      }
      b.followId = 0;
      if (now < b.nextActAt) {
         return;
      }
      boolean far = b.kind == Bot.Kind.WANDER;
      b.nextActAt = now + (far ? 4_000 + rnd.nextInt(14_000) : 6_000 + rnd.nextInt(30_000));
      if (rnd.nextInt(100) < (far ? 6 : 10)) { // thấy người thật đi ngang: chào, đi theo một lúc
         startFollow(b);
         return;
      }
      if (rnd.nextInt(100) < (far ? 75 : 45)) {
         strollAvoidingDoors(b, b.homeX, b.homeSpread, rnd);
      }
   }

   /** Đi tới 1 điểm quanh {@code cx} ± {@code spread} không sát cửa dịch chuyển (không có thì đứng yên). */
   private void strollAvoidingDoors(Bot b, float cx, float spread, Random rnd) {
      BotPlaces pl = places;
      WorldPlayer me = b.world;
      float x = pl == null || me == null ? cx + (rnd.nextFloat() * 2 - 1) * spread
            : pl.awayFromDoor(b.data.mapId, cx, me.y(), spread, rnd);
      if (!Float.isNaN(x)) {
         world.botStroll(b.session, x);
      }
   }

   /**
    * Lên cấp: cộng điểm như người bấm bảng Tiềm năng — kỹ năng trước (tới phần {@link BotFactory.Build#skillShare} của
    * tổng điểm), còn lại tiềm năng theo build. Đi qua {@link SkillService} (đủ cấp / điểm / vàng / EXP mới được).
    */
   private void spendPoints(Bot b, PlayerData d) {
      long total = BotFactory.totalPoints(d.level);
      long skillSpent = BotFactory.spentOnSkills(d);
      for (int guard = 0; guard < 12 && d.talentPoints > 0; guard++) {
         if (skillSpent < total * b.build.skillShare()) {
            SkillData s = BotFactory.nextSkill(d, d.talentPoints);
            if (s != null) {
               int before = s.level;
               int cost = BotFactory.upgradeCost(s);
               skills.upgradeSkill(b.session, s.skillId);
               if (s.level > before) {
                  skillSpent += cost;
                  if (before == 0) {
                     int slot = emptyBarSlot(d);
                     if (slot >= 0) {
                        skills.assign(b.session, s.skillId, slot); // chiêu mới học -> kéo lên thanh kỹ năng
                     }
                  }
                  continue;
               }
            }
         }
         int talent = BotFactory.nextTalent(d, b.build);
         SkillPack.Talent t = talent == 0 ? null : GameDataCache.talent(talent);
         int cost = t == null ? 0 : Math.max(1, t.pointCost());
         if (t == null || d.talentPoints < cost) {
            return;
         }
         int before = d.talentPoints;
         skills.addTalent(b.session, talent, Math.max(1, Math.min(50, d.talentPoints / cost / 3)));
         if (d.talentPoints == before) {
            return; // chạm mốc giới hạn
         }
      }
   }

   private static int emptyBarSlot(PlayerData d) {
      int[] bar = d.skillBar;
      for (int i = 0; bar != null && i < bar.length; i++) {
         if (bar[i] == ClassSkills.EMPTY) {
            return i;
         }
      }
      return -1;
   }

   private void usePotions(Bot b, PlayerData d, PlayerSettings s, long now) {
      PlayerStats stats = d.stats;
      if (stats == null) {
         return;
      }
      if (stats.maxHp > 0 && d.hp * 100 < stats.maxHp * s.autoHpThresholdPercent && now - b.hpPotionAt >= POTION_INTERVAL_MS) {
         b.hpPotionAt = now;
         items.useQuickSlot(b.session, QUICK_HP);
      }
      if (stats.maxMp > 0 && d.mp * 100 < stats.maxMp * s.autoMpThresholdPercent && now - b.mpPotionAt >= POTION_INTERVAL_MS) {
         b.mpPotionAt = now;
         items.useQuickSlot(b.session, QUICK_MP);
      }
      if (d.activityPoints < PlayerData.STAMINA_PER_SKILL && now - b.staminaPotionAt >= POTION_INTERVAL_MS) {
         b.staminaPotionAt = now;
         items.useQuickSlot(b.session, QUICK_STAMINA);
      }
   }

   private void chat(Bot b, Zone zone, long now, Random rnd) {
      if (b.nextChatAt == 0) {
         b.nextChatAt = now + 60_000 + rnd.nextInt(9 * 60_000);
         return;
      }
      if (!config.chat() || now < b.nextChatAt) {
         return;
      }
      b.nextChatAt = now + 4 * 60_000 + rnd.nextInt(12 * 60_000);
      int key = zone.map().template().id() * 10_000 + zone.id();
      Long last = zoneChatAt.get(key);
      if (last != null && now - last < ZONE_CHAT_GAP_MS) {
         return;
      }
      zoneChatAt.put(key, now);
      String[] lines = switch (b.kind) {
         case GRIND_OFFLINE, GRIND_AUTO -> b.data.party != null && rnd.nextBoolean() ? lines("party", CHAT_PARTY)
               : lines("grind", CHAT_GRIND);
         case FISH -> lines("fish", CHAT_FISH);
         case AFK -> lines("afk", CHAT_AFK);
         case BOSS -> lines("boss", CHAT_BOSS);
         case TRAVEL -> rnd.nextBoolean() ? lines("travel", CHAT_TRAVEL) : lines("idle", CHAT_IDLE);
         default -> lines("idle", CHAT_IDLE);
      };
      world.chat(b.session, lines[rnd.nextInt(lines.length)]);
   }

   // ================================================================== admin panel

   public int kickAll() {
      int n = 0;
      for (Bot b : bots.values()) {
         if (!b.leaving) {
            leave(b);
            n++;
         }
      }
      return n;
   }

   public boolean kick(int playerId) {
      Bot b = bots.get(playerId);
      if (b == null || b.leaving) {
         return false;
      }
      leave(b);
      return true;
   }

   public int onlineCount() {
      return bots.size();
   }

   /** Số bot đang săn boss thế giới {@code bossId} (admin panel tab Boss). */
   public int huntersOf(int bossId) {
      int n = 0;
      for (Bot b : bots.values()) {
         if (b.kind == Bot.Kind.BOSS && b.bossId == bossId && !b.leaving) {
            n++;
         }
      }
      return n;
   }

   private static boolean awake(Bot b) {
      WorldPlayer wp = b.world;
      Zone z = wp == null ? null : wp.zone();
      return z != null && z.humans() > 0;
   }

   /**
    * Ảnh chụp cho admin panel: tổng theo loại, theo map, {@code limit} bot (đang hoạt động trước, cấp cao trước, lọc
    * loại {@code kind} / chữ {@code q}). Mọi luồng (đọc gần đúng).
    */
   public Map<String, Object> snapshot(int limit, String kind, String q) {
      long now = System.currentTimeMillis();
      int awake = 0;
      int inParty = 0;
      Map<Bot.Kind, int[]> byKind = new EnumMap<>(Bot.Kind.class); // [số, đang hoạt động]
      Map<Integer, int[]> perMap = new TreeMap<>(); // map -> [đánh, không đánh, đang hoạt động]
      List<Bot> list = new ArrayList<>(bots.values());
      for (Bot b : list) {
         boolean on = awake(b);
         awake += on ? 1 : 0;
         inParty += b.data.party != null ? 1 : 0;
         int[] k = byKind.computeIfAbsent(b.kind, x -> new int[2]);
         k[0]++;
         k[1] += on ? 1 : 0;
         int[] row = perMap.computeIfAbsent(b.data.mapId, x -> new int[3]);
         row[b.kind.fighter ? 0 : 1]++;
         row[2] += on ? 1 : 0;
      }
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("ready", ready);
      out.put("online", list.size());
      out.put("awake", awake);
      out.put("inParty", inParty);
      out.put("pkFights", pkFights.get());
      out.put("partiesMade", partiesMade.get());
      out.put("spawnedTotal", spawnedTotal.get());
      List<Map<String, Object>> kinds = new ArrayList<>();
      for (Bot.Kind k : Bot.Kind.values()) {
         int[] v = byKind.getOrDefault(k, new int[2]);
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("key", k.key);
         row.put("label", k.label);
         row.put("count", v[0]);
         row.put("awake", v[1]);
         kinds.add(row);
      }
      out.put("kinds", kinds);
      List<Map<String, Object>> maps = new ArrayList<>();
      for (Map.Entry<Integer, int[]> e : perMap.entrySet()) {
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", e.getKey());
         row.put("name", mapName(e.getKey()));
         row.put("grind", e.getValue()[0]);
         row.put("idle", e.getValue()[1]);
         row.put("awake", e.getValue()[2]);
         maps.add(row);
      }
      out.put("maps", maps);
      String f = q == null ? "" : q.trim().toLowerCase(java.util.Locale.ROOT);
      list.removeIf(b -> (kind != null && !kind.isEmpty() && !b.kind.key.equals(kind))
            || (!f.isEmpty() && !(b.data.name + " " + b.place + " " + mapName(b.data.mapId))
                  .toLowerCase(java.util.Locale.ROOT).contains(f)));
      out.put("matched", list.size());
      list.sort(Comparator.comparingInt((Bot b) -> awake(b) ? 0 : 1).thenComparingInt(b -> -b.data.level));
      List<Map<String, Object>> rows = new ArrayList<>();
      for (Bot b : list.subList(0, Math.min(limit, list.size()))) {
         PlayerData d = b.data;
         WorldPlayer wp = b.world;
         Zone z = wp == null ? null : wp.zone();
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", d.getId());
         row.put("name", d.name);
         row.put("level", d.level);
         row.put("gained", d.level - b.startLevel);
         long need = PlayerStats.expToNextLevel(d.level);
         row.put("expPct", need <= 0 ? 0 : Math.round(d.exp * 1000.0 / need) / 10.0);
         row.put("cls", WeaponClass.name(d.classId));
         row.put("kind", b.kind.key);
         row.put("kindLabel", b.kind.label);
         row.put("place", b.place);
         row.put("map", mapName(d.mapId));
         row.put("zone", z == null ? -1 : z.id());
         row.put("x", wp == null ? 0 : Math.round(wp.x()));
         row.put("y", wp == null ? 0 : Math.round(wp.y()));
         row.put("awake", z != null && z.humans() > 0);
         row.put("dead", d.isDead());
         row.put("party", partySize(d));
         row.put("pk", b.hitBy != 0);
         row.put("minutesLeft", Math.max(0, (b.logoutAt - now) / 60_000));
         rows.add(row);
      }
      out.put("bots", rows);
      return out;
   }

   /** Chi tiết 1 bot cho popup admin: đồ đang mặc, bộ sưu tập, kỹ năng, tiềm năng, chỉ số, tổ đội. null = không có. */
   public Map<String, Object> detail(int playerId) {
      Bot b = bots.get(playerId);
      if (b == null) {
         return null;
      }
      PlayerData d = b.data;
      long now = System.currentTimeMillis();
      Map<String, Object> out = new LinkedHashMap<>();
      out.put("id", d.getId());
      out.put("name", d.name);
      out.put("level", d.level);
      out.put("startLevel", b.startLevel);
      out.put("kindLabel", b.kind.label);
      out.put("place", b.place);
      out.put("map", mapName(d.mapId));
      out.put("cls", WeaponClass.name(d.classId));
      out.put("gender", com.tlnt.server.player.Gender.name(d.gender));
      out.put("build", b.build.name());
      out.put("style", b.kind == Bot.Kind.GRIND_AUTO ? "Chiêu ngẫu nhiên, mọi loại quái hợp cấp, phản xạ "
            + b.reactionMin + "-" + b.reactionMax + " ms" : b.kind == Bot.Kind.GRIND_OFFLINE
            ? "Chiêu mạnh trước, cắm khúc bãi " + d.settings.autoAttackRange + " px, né quái người khác đang đánh"
            : b.kind == Bot.Kind.TRAVEL ? "Đã qua " + b.trips + " cửa" + (b.doorPos >= 0 ? ", đang chạy tới cửa" : "")
            : b.kind == Bot.Kind.SPAR ? (b.rival == null ? "Mất bạn tập (đi dạo)" : "Bạn tập " + b.rival.data.name)
                  + ", " + b.rounds + " trận / thắng " + b.wins + (world.inDuel(d.getId()) ? " — đang đấu" : "")
            : b.followId != 0 && now < b.followUntil ? "Đang đi theo người chơi #" + b.followId : "");
      out.put("onlineMinutes", (now - b.bornAt) / 60_000);
      out.put("minutesLeft", Math.max(0, (b.logoutAt - now) / 60_000));
      out.put("deaths", b.deaths);
      out.put("pkFights", b.pkFights);
      out.put("casts", b.casts);
      out.put("gold", d.gold);
      out.put("talentPoints", d.talentPoints);
      PlayerStats s = d.stats;
      if (s != null) {
         out.put("stats", List.of(new Object[] {"HP", d.hp + " / " + s.maxHp}, new Object[] {"MP", d.mp + " / " + s.maxMp},
               new Object[] {"Tấn công", s.attackPower}, new Object[] {"Giáp", s.armor},
               new Object[] {"Chí mạng", s.critChance}, new Object[] {"Né tránh", s.evasion},
               new Object[] {"Chính xác", s.accuracy}, new Object[] {"Tốc chạy", s.moveSpeed}));
      }
      List<Object[]> gear = new ArrayList<>();
      for (int slot = 0; slot < PlayerData.BODY_SLOTS; slot++) {
         ItemData it = d.body.get(slot);
         if (it != null) {
            gear.add(new Object[] {typeName(it), itemName(it), it.starCount, it.starGrade});
         }
      }
      Object[][] cols = {{"Ngoại trang", d.costumes}, {"Danh hiệu", d.titles}, {"Thú cưng", d.pets}, {"Mặt nạ", d.masks},
            {"Đeo lưng", d.backs}};
      for (Object[] col : cols) {
         ItemData it = ((com.tlnt.server.item.ItemCollection) col[1]).getSelected();
         if (it != null) {
            gear.add(new Object[] {col[0], itemName(it), 0, 0});
         }
      }
      out.put("gear", gear);
      List<Object[]> sk = new ArrayList<>();
      for (SkillData x : d.skills) {
         SkillPack.Skill t = GameDataCache.skillTemplate(x.skillId);
         sk.add(new Object[] {t == null ? "#" + x.skillId : t.name(), x.level, ClassSkills.onBar(d, x.skillId)});
      }
      out.put("skills", sk);
      List<Object[]> tal = new ArrayList<>();
      for (SkillPack.Talent t : GameDataCache.skill().talents()) {
         int i = t.id() - 1;
         tal.add(new Object[] {t.name(), i >= 0 && i < d.talents.length ? d.talents[i] : 0});
      }
      out.put("talents", tal);
      PartyService ps = parties;
      PartyService.Snapshot party = ps == null || d.party == null ? null : ps.snapshot(d);
      if (party != null) {
         List<String> names = new ArrayList<>();
         for (int memberId : party.memberIds()) {
            WorldPlayer p = world.online(memberId);
            names.add((p == null ? "#" + memberId : p.data().name) + (party.leaderId() == memberId ? " (trưởng)" : ""));
         }
         out.put("party", names);
      }
      return out;
   }

   private int partySize(PlayerData d) {
      PartyService ps = parties;
      PartyService.Snapshot party = ps == null || d.party == null ? null : ps.snapshot(d);
      return party == null ? 0 : party.memberIds().size();
   }

   private String mapName(int mapId) {
      MapTemplate t = world.mapTemplate(mapId);
      return t == null ? "#" + mapId : t.name();
   }

   private static String itemName(ItemData it) {
      ItemPack.ItemTemplate t = it.template();
      return t == null ? "#" + it.templateId : t.name();
   }

   private static String typeName(ItemData it) {
      ItemPack.ItemType t = it.type();
      return t == null ? "?" : t.name();
   }
}
