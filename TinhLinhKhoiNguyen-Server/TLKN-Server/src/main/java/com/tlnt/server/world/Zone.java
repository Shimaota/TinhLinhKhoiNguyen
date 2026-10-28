package com.tlnt.server.world;

import com.tlnt.server.altar.AltarRules;
import com.tlnt.server.farm.AppleTree;
import com.tlnt.server.farm.AppleTreeService;
import com.tlnt.server.farm.FarmPackets;
import com.tlnt.server.farm.FarmState;
import com.tlnt.server.item.GearCharges;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.linhmoc.LinhMocRules;
import com.tlnt.server.linhmoc.LinhMocRun;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.net.Packet;
import com.tlnt.server.net.Packets;
import com.tlnt.server.party.Party;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.social.PkRules;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerSettings;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 1 khu của 1 map. Tick trên 1 luồng cố định của {@link TickEngine} (mặc định ~30 lần/giây).
 * Mọi thay đổi trạng thái khu (vào, ra, di chuyển) đều chạy trên luồng tick đó -> không cần khoá.
 * Luồng khác chỉ được {@link #submit} lệnh hoặc đọc {@link #occupancy()}.
 *
 * Mỗi khu tối đa {@code maxPlayers} người nên khu chính là vùng quan tâm (AOI): mọi người trong khu
 * đều thấy nhau, người ở khu khác không nhận gói của khu này.
 */
public final class Zone {

   /** Id dòng đếm ngược op 67 của lượt Tế đàn Thiên tích + chữ đứng trước đồng hồ. */
   static final int ALTAR_TIMER_ID = 340;
   static final String ALTAR_TIMER_TEXT = "Tế đàn Thiên tích";

   /** Đồng hồ lượt Tế đàn (op 67) theo giờ kết thúc của người chơi. Luồng tick. */
   static void sendAltarTimer(WorldPlayer p) {
      long left = Math.max(0, Math.min(AltarRules.RUN_MS, p.data().altarRun.endAt() - System.currentTimeMillis()));
      p.send(WorldPackets.announcement(ALTAR_TIMER_ID, ALTAR_TIMER_TEXT, AltarRules.RUN_MS - left, AltarRules.RUN_MS));
   }

   /** Id dòng đếm ngược op 67 của lượt Rừng Linh Mộc. */
   static final int LINH_MOC_TIMER_ID = 341;

   /** Đồng hồ lượt Rừng Linh Mộc (op 67) cạnh nút Thoát. Luồng tick. */
   static void sendLinhMocTimer(WorldPlayer p, LinhMocRun run) {
      long total = Math.max(1, run.end - run.start);
      long left = Math.max(0, Math.min(total, run.end - System.currentTimeMillis()));
      p.send(WorldPackets.announcement(LINH_MOC_TIMER_ID, LinhMocRules.config().text("timer"), total - left, total));
   }

   /** Id dòng đếm ngược op 67 của lượt Lôi vực Sấm sét. */
   static final int THUNDER_TIMER_ID = 342;
   static final String THUNDER_TIMER_TEXT = "Lôi vực Sấm sét";

   /** Đồng hồ lượt Lôi vực (op 67) cạnh nút Thoát. Không có lượt thì thôi. Luồng tick. */
   static void sendThunderTimer(WorldPlayer p) {
      com.tlnt.server.thunder.ThunderRun run = p.data().thunderRun;
      if (run == null) {
         return;
      }
      long total = Math.max(1, run.end - run.start);
      long left = Math.max(0, Math.min(total, run.end - System.currentTimeMillis()));
      p.send(WorldPackets.announcement(THUNDER_TIMER_ID, THUNDER_TIMER_TEXT, total - left, total));
   }

   /** Đồng hồ khu đấu Giải đấu Bộ lạc (op 67, chữ do giải đặt); không có đồng hồ thì gỡ. Luồng tick. */
   static void sendArenaTimer(WorldPlayer p, Arena arena) {
      String text = arena.timerText;
      if (text == null) {
         p.send(WorldPackets.announcementRemove(Arena.TIMER_ID));
         return;
      }
      long total = Math.max(1, arena.timerEnd - arena.timerStart);
      long left = Math.max(0, Math.min(total, arena.timerEnd - System.currentTimeMillis()));
      p.send(WorldPackets.announcement(Arena.TIMER_ID, text, total - left, total));
   }

   /** Khu mở thêm (id >= zone_count) trống quá lâu thì đóng để tiết kiệm tài nguyên. */
   private static final long RETIRE_AFTER_MS = 60_000;
   /*
    * Chống hack tốc độ / dịch chuyển = NGÂN SÁCH di chuyển mỗi người (WorldPlayer.moveBudget*): hồi theo thời gian với
    * tốc độ hợp lệ, mỗi lần đi trừ đúng quãng đã đi, đầy tối đa BURST (1 lần lướt / nhảy / rơi + lag gói dồn).
    * -> tốc độ TRUNG BÌNH không vượt được tốc chạy thật (+ SPEED_MARGIN), không còn "300 px mỗi gói" như trước.
    *   Ngang: tốc = chỉ số Tốc độ di chuyển thật (đã gồm buff 16 Phá Tốc Nỗ / đồ) × 1,3.
    *   Lên: bị giới hạn bởi nhảy (client Box2D jumpForce 16, 1 lần nhảy; để rộng tay) -> hồi 1000 px/s, đầy 700 px.
    *   Xuống: rơi nhanh dần (trọng lực) -> hồi 6000 px/s, đầy 2000 px (rơi từ đỉnh map không bị kéo lại).
    */
   private static final float SPEED_MARGIN = 1.3f;
   private static final float MIN_SPEED_PX_PER_SEC = 450f;
   /** Tối đa dồn được: lướt 28 Ảo Ảnh Phách ~300 px + gói tới dồn do lag. */
   static final float BURST_X_PX = 450f;
   /** Choáng / đóng băng mà client vẫn gửi di chuyển: gửi lại vị trí thật tối đa mỗi bấy lâu. */
   static final long PIN_RESEND_MS = 300;
   static final float UP_PX_PER_SEC = 1000f;
   static final float BURST_UP_PX = 700f;
   static final float DOWN_PX_PER_SEC = 6000f;
   static final float BURST_DOWN_PX = 2000f;

   private final GameMap map;
   private final int id;
   private final int maxPlayers;
   /** Khu riêng của nhân vật này (map riêng tư, {@link GameMap#isPrivate}); 0 = khu thường. */
   private final int ownerId;
   /** Đã giữ chỗ + đang ở trong khu. Chỉ tăng qua {@link GameMap} (có khoá), giảm tự do khi rời/huỷ. */
   private final AtomicInteger occupancy = new AtomicInteger();
   private final Queue<Runnable> inbox = new ConcurrentLinkedQueue<>();
   /** Chỉ luồng tick đụng tới. */
   private final Map<Integer, WorldPlayer> players = new LinkedHashMap<>();
   private final float[] positionBuffer = new float[2];
   /**
    * Người đã đi từ lần phát trước ({@link WorldPlayer#moveDirty} chống trùng). Tick vẫn chạy đủ nhịp (kiểm tra di chuyển, mô phỏng thú cưng) nhưng vị trí chỉ PHÁT mỗi
    * {@link #moveBroadcastMs}: client người xem nội suy giữa các mốc ({@code ClickMoveController}, trễ 100 ms) nên vẫn
    * mượt ở mọi FPS, còn số lần ghi socket (phần tốn CPU nhất khi đông người — docs/LOAD_TEST.md) giảm ~3 lần.
    */
   private final List<WorldPlayer> movedBuffer = new ArrayList<>();
   /** Người đang Luyện tập Offline trong khu (server tự chạy — {@link OfflineWalker}). Thường rỗng: tick không tốn gì. */
   private final List<WorldPlayer> offlineWalkers = new ArrayList<>(0);
   /** Người THẬT (không phải bot) đang trong khu — bot chỉ hoạt động khi > 0 ({@code bot/BotService}). Chỉ luồng tick ghi. */
   private volatile int humans;
   /** Lần gần nhất 1 người thật vào khu (luồng tick): bot tạm không ra đòn — client người đó còn đang dựng quái. */
   private long humanJoinedAt;
   /** Bot chờ bấy lâu sau khi quái hồi sinh / người thật vào khu mới đánh ({@link ZoneMobs#fresh}). */
   static final long BOT_FRESH_MS = 1_200;
   /** Chỗ đang giữ cho người chơi giả ({@link GameMap#reserveForBot}): giới hạn bot / khu, chừa chỗ cho người thật. */
   final AtomicInteger botSlots = new AtomicInteger();
   private long nextMoveBroadcastAt;
   /** Lần kiểm tra "gần nước" kế tiếp (Trái tim đại dương). */
   private long nextWaterCheckAt;
   /** Số tổ đội khác nhau có người trong khu (bảng Khu vực op 25) — tính lại {@link #PARTY_COUNT_MS} 1 lần trên luồng tick. */
   private volatile int partyCount;
   private long nextPartyCountAt;
   static final long PARTY_COUNT_MS = 2_000;
   /** Lần quét kế tiếp: quái chủ động tấn công người đứng gần ({@link Regions}). */
   private long nextAggroScanAt;
   /** Nhịp kiểm tra gần nước (ms). */
   static final long WATER_CHECK_MS = 500;
   /** Chu kỳ phát vị trí (ms), {@code world.moveBroadcastMillis}; GameServer đặt lúc khởi động. */
   static volatile long moveBroadcastMs = 50;
   /** Chỉ luồng tick đụng tới. */
   private final ZoneMobs mobs;
   /** Chỉ luồng tick đụng tới. */
   private final ZonePlants plants;
   /** Vật phẩm rơi dưới đất. Chỉ luồng tick đụng tới. */
   private final ZoneDrops drops = new ZoneDrops();
   private final Consumer<Packet> broadcastAll = packet -> broadcast(packet, null);
   /** Việc hẹn giờ ({@link #schedule}), sắp theo giờ chạy. Chỉ luồng tick đụng tới. */
   private final PriorityQueue<Timer> timers = new PriorityQueue<>();
   private long timerSeq;
   /** Số thứ tự trận thách đấu trong khu (phân biệt trận cũ / mới cho việc hẹn giờ). Chỉ luồng tick. */
   long duelSeq;

   /** Luồng tick đang sở hữu khu (TickEngine gán). */
   volatile TickEngine.Worker worker;
   private volatile boolean closed;
   private long emptySince;
   /** Boss người chơi giả của khu (Tế đàn / Tinh linh Thần mộc / boss thế giới; null = không có). Chỉ luồng tick. */
   AltarBoss boss;
   /** Khu Tế đàn: mã lượt (= {@code AltarRun.start}) + giữ khu (kể cả trống) tới lúc này. Sửa dưới khoá GameMap. */
   long runId;
   volatile long keepUntil;
   /** Lượt phó bản của khu đã hết (không ai vào lại được): trống là đóng NGAY, không chờ {@link #RETIRE_AFTER_MS}. */
   volatile boolean closeWhenEmpty;
   /** Khu của 1 lượt Rừng Linh Mộc (null = không phải): quái theo cấp lượt, không hồi sinh. */
   final LinhMocRun linhMoc;
   /** Đã báo "diệt sạch map" cho lượt Rừng Linh Mộc (chỉ báo 1 lần). Chỉ luồng tick. */
   boolean linhMocClearedSent;
   /** Boss quái của khu (Hắc thần Linh mộc / boss thế giới dạng quái; null = không có). Chỉ luồng tick. */
   LinhMocMobBoss mobBoss;
   /** Boss thế giới đang đứng trong khu (thường 0-1, tối đa 1 boss quái + 1 boss người chơi giả). Chỉ luồng tick. */
   final List<WorldBoss> worldBosses = new ArrayList<>(1);
   /** Đèn hoa đăng đang trôi (Trung thu): index vùng nước -> thời điểm từng đèn trôi đi hết. Luồng tick khu. */
   final java.util.Map<Integer, java.util.ArrayDeque<Long>> lanterns = new java.util.HashMap<>(2);
   /** Khu đấu Giải đấu Bộ lạc (null = không phải): cờ PK, rơi đài, hồi sinh / thua do giải quyết. */
   final Arena arena;

   Zone(GameMap map, int id, int maxPlayers) {
      this(map, id, maxPlayers, 0);
   }

   Zone(GameMap map, int id, int maxPlayers, int ownerId) {
      this(map, id, maxPlayers, ownerId, null, null);
   }

   /** Khu của lượt Rừng Linh Mộc {@code run} ({@link GameMap#reserveLinhMoc}). */
   Zone(GameMap map, int id, int maxPlayers, LinhMocRun run) {
      this(map, id, maxPlayers, 0, run, null);
   }

   /** Khu đấu Giải đấu Bộ lạc ({@link GameMap#reserveArena}). */
   Zone(GameMap map, int id, int maxPlayers, Arena arena) {
      this(map, id, maxPlayers, 0, null, arena);
   }

   private Zone(GameMap map, int id, int maxPlayers, int ownerId, LinhMocRun run, Arena arena) {
      this.map = map;
      this.id = id;
      this.maxPlayers = maxPlayers;
      this.ownerId = ownerId;
      this.linhMoc = run;
      this.arena = arena;
      this.mobs = run != null
            ? new ZoneMobs(LinhMocRules.scaleMobs(map.mobs(), run.mobLevel, run.unit), map.template().id(), this, true)
            : new ZoneMobs(map.mobs(), map.template().id(), this, map.isAltar());
      this.mobs.revealer = (seen, packet) -> { // siêu quái bị đánh: hiện đúng cho người chưa thấy
         for (WorldPlayer p : players.values()) {
            if (!seen.contains(p.id())) {
               p.send(packet);
            }
         }
      };
      this.plants = new ZonePlants(map.template(), map.plantSpots(), this);
      this.boss = map.isAltar() ? AltarBoss.create(map.template())
            : run != null && LinhMocRules.isSpiritMap(map.template().id()) // Tinh linh Thần mộc: chỉ số theo cấp lượt
            ? new AltarBoss(LinhMocRules.spiritSpec(map.template().id(), run.mobLevel, run.unit), map.template())
            : null;
      this.mobBoss = run != null ? LinhMocMobBoss.find(mobs) : null;
   }

   /**
    * Khu Rừng Linh Mộc đã dọn xong (mở cửa sang map sau / map cuối = hoàn thành): hết quái (gồm boss quái Hắc thần) +
    * Tinh linh Thần mộc (map cuối) đã chết.
    */
   boolean linhMocCleared() {
      return mobs.allDead() && (boss == null || boss.dead); // boss quái Hắc thần nằm trong mobs
   }

   /** Khu Tế đàn đã diệt hết: mọi quái + boss (nếu có). */
   boolean altarCleared() {
      return mobs.allDead() && (boss == null || boss.dead);
   }

   /** Số quái + boss còn sống. */
   int altarAlive() {
      return mobs.aliveCount() + (boss != null && !boss.dead ? 1 : 0);
   }

   /** Boss của khu nếu {@code id} đúng là nó và còn sống; null nếu không. */
   AltarBoss bossById(int id) {
      return boss != null && boss.id == id && !boss.dead ? boss : null;
   }

   public int id() {
      return id;
   }

   public GameMap map() {
      return map;
   }

   /** Chủ khu riêng, 0 = khu thường. */
   public int ownerId() {
      return ownerId;
   }

   public int occupancy() {
      return occupancy.get();
   }

   public int maxPlayers() {
      return maxPlayers;
   }

   /** Số người thật trong khu (đọc mọi luồng, gần đúng). */
   public int humans() {
      return humans;
   }

   /** Số chỗ người chơi giả đang giữ trong khu. */
   public int bots() {
      return botSlots.get();
   }

   /** Xếp lệnh chạy ở tick kế tiếp của khu. */
   public void submit(Runnable command) {
      inbox.add(command);
   }

   /**
    * Hẹn chạy {@code task} sau {@code delayMs} (làm tròn lên theo tick). CHỈ gọi trên luồng tick của khu (trong lệnh
    * đã {@link #submit}). Việc hẹn chạy trên luồng tick nên được đụng trạng thái khu; tự kiểm tra lại người / quái
    * còn đó không (có thể đã rời khu / chết trong lúc chờ).
    */
   void schedule(long delayMs, Runnable task) {
      timers.add(new Timer(System.currentTimeMillis() + delayMs, timerSeq++, task));
   }

   /**
    * Gửi trạng thái cây táo của chủ khu (op 52 sub 2) + hẹn gửi lại lúc nâng cấp xong (client đếm ngược tới 0 rồi đứng
    * chờ gói mới). Luồng tick; đọc bản bất biến {@code FarmState.apple}, không sửa trạng thái cây.
    */
   void showAppleTree(WorldPlayer p) {
      FarmState farm = p.data().farm;
      long now = System.currentTimeMillis();
      AppleTree tree = farm.apple;
      if (tree == null) { // chưa từng đụng tới: trồng cây cấp 1 (người chơi chưa bấm cây được -> không tranh với luồng session)
         farm.apple = tree = AppleTree.fresh(now);
      }
      tree = tree.settle(now);
      p.send(FarmPackets.appleTree(tree, now));
      if (tree.upgrading()) {
         schedule(tree.upgradeEnd() - now + 1, () -> {
            if (players.get(p.id()) == p) {
               showAppleTree(p);
            }
         });
      }
   }

   private record Timer(long at, long seq, Runnable task) implements Comparable<Timer> {
      @Override
      public int compareTo(Timer o) {
         return at != o.at ? Long.compare(at, o.at) : Long.compare(seq, o.seq); // cùng giờ: đúng thứ tự hẹn
      }
   }

   // ------------------------------------------------------------------ giữ chỗ (gọi dưới khoá của GameMap)

   boolean tryReserve() {
      if (closed || occupancy.get() >= maxPlayers) {
         return false;
      }
      occupancy.incrementAndGet();
      return true;
   }

   void releaseReservation() {
      occupancy.decrementAndGet();
   }

   void close() {
      closed = true;
   }

   // ------------------------------------------------------------------ tick

   /** "Tự nhặt vật phẩm" (cài đặt tự động): mỗi bấy nhiêu ms hút đồ trong ô ±{@link #AUTO_PICKUP_RANGE_PX}. */
   static final long AUTO_PICKUP_INTERVAL_MS = 250;
   static final float AUTO_PICKUP_RANGE_PX = 400f;
   /** Đồ nằm dưới đất bấy lâu mới bị hút (người chơi kịp thấy đồ rơi ra rồi bay vào người). */
   static final long AUTO_PICKUP_DELAY_MS = 1_000;
   private long nextAutoPickupAt;
   /**
    * Khu chỉ có bot đang ngủ ({@link #quiet}): tick đủ bấy nhiêu ms / lần thay vì mỗi tick (~30 / s). Không ai nhìn, bot
    * đứng yên -> chỉ còn quái / cây hồi sinh, đồ rơi hết hạn, việc hẹn giờ — trễ tối đa bấy nhiêu ms không ai thấy.
    * Đo VPS 2 nhân, 544 bot / 423 khu: luồng tick khu là phần tốn CPU đều đặn lớn nhất khi vắng người.
    */
   static final long QUIET_TICK_MS = 500;
   private long nextQuietTickAt;

   /**
    * Hành trang còn chỗ cho món này (vàng / lam thạch vào ô tiền tệ, luôn được). Đọc túi của luồng session không khoá —
    * lệch hiếm hoi thì lúc cho vào túi vẫn kiểm tra lại (đầy -> thả lại dưới chân, ngừng hút 10 s).
    */
   private static boolean fitsInBag(PlayerData d, ItemData item) {
      return item.templateId == ItemService.GOLD_TEMPLATE_ID || item.templateId == ItemService.DIAMOND_TEMPLATE_ID
            || d.bag.canFit(item);
   }

   /**
    * Người bật "Tự nhặt vật phẩm" (còn sống, không giao dịch, túi chưa vừa báo đầy) tự nhặt mọi món mình được nhặt ở
    * gần — như bấm nhặt (op 30 sub 3 cả khu, đồ vào túi trên luồng session qua {@link World#autoPickup}).
    */
   private void autoPickup(long now) {
      World.PickupDone done = map.world().autoPickup();
      if (done == null) {
         return;
      }
      for (WorldPlayer p : players.values()) {
         PlayerData d = p.data();
         PlayerSettings settings = d.settings;
         if (settings == null || !settings.autoPickupItems || d.isDead() || d.trade != null
               || now < d.autoPickupPausedUntil) {
            continue;
         }
         for (ZoneDrops.Drop drop : drops.pickableNear(p.id(), p.x, p.y, AUTO_PICKUP_RANGE_PX,
               AUTO_PICKUP_DELAY_MS, now)) {
            if (!fitsInBag(d, drop.item)) {
               continue; // túi đầy: không hút, đồ vẫn nằm dưới đất
            }
            drops.remove(drop);
            broadcast(WorldPackets.groundItemPicked(p.id(), drop.id).toPacket(), null);
            done.accept(p, drop.item);
         }
         if (drops.size() == 0) {
            return;
         }
      }
   }

   void tick(long now) {
      if (inbox.isEmpty() && players.isEmpty() && timers.isEmpty()) {
         partyCount = 0;
         checkRetire(now); // khu trống: gần như không tốn gì
         return;
      }
      if (quiet()) {
         if (now < nextQuietTickAt) {
            return;
         }
         nextQuietTickAt = now + QUIET_TICK_MS;
      }
      mobs.respawnDue(now, broadcastAll, players.keySet()); // trước lệnh: người vừa vào khu thấy đúng quái còn sống
      plants.update(now, false, broadcastAll); // cây mọc lại / mọc đủ số lượng
      drops.expire(now, broadcastAll); // vật phẩm rơi quá lâu không ai nhặt
      if (drops.size() > 0 && now >= nextAutoPickupAt) {
         nextAutoPickupAt = now + AUTO_PICKUP_INTERVAL_MS;
         autoPickup(now);
      }
      Runnable command;
      while ((command = inbox.poll()) != null) {
         try {
            command.run();
         } catch (Exception e) {
            Log.error("Loi xu ly lenh o map " + map.template().id() + " khu " + id, e);
         }
      }
      runDueTimers(now);
      // Khu chỉ có người chơi giả (humans = 0): quái / boss nghỉ đánh — bot ở đó đang "ngủ" (BotService) nên không đỡ /
      // đánh trả được; không có người thật nào nhìn thấy nên cũng khỏi tốn CPU. Người thật vào là đánh tiếp.
      boolean watched = humans > 0;
      if (watched && now >= nextAggroScanAt && Regions.aggressive(map.template().id())) { // map 9-20: quái chủ động tấn công người đứng gần
         nextAggroScanAt = now + Regions.AGGRO_SCAN_MS;
         ZoneCombat.mobAggro(this, now);
      }
      if (watched) {
         ZoneCombat.mobAttacks(this, now); // quái đánh trả (nhịp 3,5 s / con)
      }
      if (boss != null && watched) {
         boss.tick(this, now); // boss Tế đàn / Tinh linh Thần mộc: đuổi + ra chiêu
      }
      if (mobBoss != null && watched) {
         mobBoss.tick(this, now); // boss quái Hắc thần Linh mộc
      }
      applyMovement(now);
      if (!offlineWalkers.isEmpty()) {
         tickOfflineWalkers(now);
      }
      if (now >= nextPartyCountAt) {
         nextPartyCountAt = now + PARTY_COUNT_MS;
         countParties();
      }
      if (now >= nextWaterCheckAt) {
         nextWaterCheckAt = now + WATER_CHECK_MS;
         checkWater();
      }
      tickEscorts(now);
      if (now >= nextMoveBroadcastAt) {
         nextMoveBroadcastAt = now + moveBroadcastMs;
         flushMoves();
      }
      checkRetire(now);
   }

   /**
    * Không người thật, không lệnh chờ, không ai đang chạy / nhảy / bị hất (bot treo offline đi dở thì đi nốt ở nhịp
    * thường — bước đi tính theo dt kẹp 0,1 s, tick thưa sẽ làm chậm).
    */
   boolean quiet() {
      if (humans > 0 || !inbox.isEmpty()) {
         return false;
      }
      for (WorldPlayer p : offlineWalkers) {
         OfflineWalker w = p.offlineWalker;
         if (w != null && (w.walking || w.knocked || w.hopReply != null || w.mover != null && !w.mover.onGround())) {
            return false;
         }
      }
      return true;
   }

   private void runDueTimers(long now) {
      Timer timer;
      while ((timer = timers.peek()) != null && timer.at <= now) {
         timers.poll();
         try {
            timer.task.run();
         } catch (Exception e) {
            Log.error("Loi viec hen gio o map " + map.template().id() + " khu " + id, e);
         }
      }
   }

   /**
    * Trái tim đại dương (đang đeo): đứng cách vùng nước của map ≤ {@link GearCharges#OCEAN_REACH_PX} thì +100 tốc chạy, +5%
    * Tấn công. Trạng thái đổi -> xin luồng session đặt {@link PlayerData#nearWater} + tính lại / phát khối chỉ số
    * ({@link PlayerData#waterPosted} chống xin lặp khi session chưa kịp xử lý). Chỉ người đeo mới tốn phép kiểm tra.
    */
   private void checkWater() {
      for (WorldPlayer p : players.values()) {
         PlayerData d = p.data();
         if (!d.gear.oceanHeart) {
            continue;
         }
         boolean near = !d.isDead() && map.template().nearWater(p.x, p.y, GearCharges.OCEAN_REACH_PX);
         int want = near ? 1 : 0;
         if (near == d.nearWater || d.waterPosted == want) {
            continue;
         }
         d.waterPosted = want;
         PlayerSession s = p.session();
         World world = map.world();
         GearCharges.post(s, () -> {
            PlayerData pd = s.getPlayer();
            if (pd == null || !pd.inWorld || !pd.gear.oceanHeart) {
               if (pd != null) {
                  pd.waterPosted = -1;
               }
               return;
            }
            pd.nearWater = near;
            ItemService.refreshLook(pd);
            world.broadcastStats(pd);
         });
      }
   }

   /** Lấy vị trí mới nhất client báo, kiểm tra hợp lệ rồi phát cho người khác trong khu (gộp theo tick). */
   private void applyMovement(long now) {
      List<WorldPlayer> moved = movedBuffer;
      for (WorldPlayer p : players.values()) {
         if (!p.takePendingPosition(positionBuffer)) {
            continue;
         }
         if (p.locked || p.data().isDead()) { // khoá nhân vật / đã bị hạ gục: không được di chuyển
            continue;
         }
         if (now < p.data().stunnedUntil || now < p.data().frozenUntil) { // choáng / đóng băng: đứng yên
            if (now - p.lastPinnedAt >= PIN_RESEND_MS) { // kéo lại chỗ cũ (client không tự khoá khi choáng)
               p.lastPinnedAt = now;
               p.send(WorldPackets.teleportSelf(p.x, p.y));
            }
            continue;
         }
         float nx = map.template().clampX(positionBuffer[0]);
         float ny = map.template().clampY(positionBuffer[1]);
         if (nx == p.x && ny == p.y) {
            continue;
         }
         if (!spendMoveBudget(p, nx - p.x, ny - p.y, now)) {
            Log.debug("Nhan vat #%d di chuyen bat thuong (%.0f,%.0f -> %.0f,%.0f), keo ve", p.id(), p.x, p.y, nx, ny);
            p.send(WorldPackets.teleportSelf(p.x, p.y));
            p.lastMoveAt = now;
            continue;
         }
         if (nx != p.x) {
            p.facingLeft = nx < p.x;
         }
         p.x = nx;
         p.y = ny;
         p.invalidateSpawnPacket();
         p.lastMoveAt = now;
         p.data().x = nx;
         p.data().y = ny;
         if (arena != null && ny < Arena.FALL_Y && ZoneCombat.arenaFall(this, p, now)) {
            continue; // rơi khỏi võ đài: bị hạ gục / kéo lại lên đài (vị trí đã gửi riêng)
         }
         if (!p.moveDirty) {
            p.moveDirty = true;
            moved.add(p);
         }
      }
   }

   // ------------------------------------------------------------------ Luyện tập Offline (server tự chạy nhân vật)

   /**
    * Bot treo offline hỏi việc tiếp theo (luồng tick khu). Thứ tự như người chơi thật bật tự đánh: bật "Tự nhặt" + có đồ
    * của mình rơi gần (ngoài tầm tự nhặt, ≤ {@link OfflineWalker#LOOT_SEEK_PX}, túi còn chỗ) -> chạy tới nhặt; không thì giữ
    * mục tiêu cũ / chọn quái gần nhất hợp lọc ({@code s}) rồi CHẠY tới cạnh nó. {@code reply}: id quái (-1 = không có) +
    * đã trong tầm đánh chưa.
    */
   void offlineSeek(WorldPlayer p, World.OfflineQuery q, World.OfflineReply reply) {
      PlayerData d = p.data();
      if (p.locked || d.isDead()) {
         reply.accept(-1, 0);
         return;
      }
      long now = System.currentTimeMillis();
      OfflineWalker w = walker(p, now);
      boolean bot = d.bot;
      if (bot && now - humanJoinedAt < BOT_FRESH_MS) {
         reply.accept(q.current(), 0); // người thật vừa vào khu: đợi client họ dựng xong quái rồi mới ra đòn
         return;
      }
      if (q.pickup() && drops.size() > 0 && seekLoot(p, w, q.range(), now)) {
         reply.accept(-1, 0);
         return;
      }
      MapTemplate.MobInstance target = q.current() >= 0 && !w.skipMob(q.current(), now)
            && offlineMatch(q.current(), q, w, bot, now) ? mobs.alive(q.current()) : null;
      if (target == null) {
         float best = Float.MAX_VALUE;
         for (int id = 0, n = mobs.size(); id < n; id++) {
            if (!offlineMatch(id, q, w, bot, now) || w.skipMob(id, now)) {
               continue;
            }
            MapTemplate.MobInstance mob = mobs.alive(id);
            float dx = mob.x() - p.x;
            float dy = (mob.y() - p.y) * 2; // khác tầng "xa" hơn cùng tầng (phải nhảy / vòng)
            float dist = dx * dx + dy * dy;
            if (q.spread() > 0) { // bot: con người / bot khác đang đánh -> "xa" hơn (không cả đám dồn vào 1 con)
               int taken = claims(id, p);
               if (taken > 0) {
                  dist *= 1 + taken * (q.spread() >= 2 ? 6f : 1.5f);
               }
            }
            if (dist < best) {
               best = dist;
               target = mob;
            }
         }
      }
      if (target == null) {
         w.stop();
         w.targetMob = -1;
         reply.accept(-1, 0);
         return;
      }
      w.targetMob = target.id();
      int mask = OfflineWalker.reachMask(p.x, p.y, target.x(), target.y(), q.reachX(), q.reachY());
      if (mask != 0) {
         w.stop();
         reply.accept(target.id(), mask); // chiêu nào lọt hộp tầm (như client AttackReach) thì ra chiêu đó
         return;
      }
      boolean onGround = w.mover == null || w.mover.onGround();
      if (!onGround) {
         reply.accept(target.id(), 0); // đang nhảy / rơi: tick khu tự ra chiêu khi lọt tầm (tickOfflineWalkers)
         return;
      }
      float stand = OfflineWalker.standOff(q.reachX());
      float dx = p.x - target.x();
      if (Math.abs(dx) <= stand + OfflineWalker.ARRIVE_PX) {
         // Đủ gần ngang mà vẫn không với tới -> lệch tầng. Quái CAO hơn (quái bay / bục trên): nhảy (kép) lên đánh như
         // client autoJump; quái thấp hơn: xuống tầng của nó (đi tới mặt đất dưới quái — rơi / xuyên nền 1 chiều).
         float need = OfflineWalker.hopNeeded(p.y, target.y(), q.reachY());
         if (need > 0) {
            if (need <= OfflineWalker.MAX_HOP_PX
                  && (w.hopMob != target.id() || w.hops < OfflineWalker.MAX_HOPS_PER_TARGET)) {
               if (w.mover == null) {
                  w.mover = new BossMover(p.x, p.y);
               }
               w.stop();
               w.startHop(target.id(), need, q, reply);
               reply.accept(target.id(), 0);
               return;
            }
            w.markUnreachable(target.id(), now); // nhảy kép cũng không tới / nhảy mãi không trúng -> con khác
            reply.accept(-1, 0);
            return;
         }
         float groundY = BossMover.groundUnder(map.template(), target.x(), target.y());
         if (!w.walking && w.goalMob == target.id() && Math.abs(p.y - groundY) <= OfflineWalker.MELEE_BELOW_PX) {
            w.markUnreachable(target.id(), now); // đã đứng đúng tầng của quái mà vẫn không với tới
            reply.accept(-1, 0);
            return;
         }
         w.goTo(map.template().clampX(target.x() + Math.signum(dx == 0 ? -1 : dx) * stand * 0.5f), groundY,
               target.id(), -1, now);
         reply.accept(target.id(), 0);
         return;
      }
      // Chỗ đứng phía gần, cách 0,8 × tầm ngang nhỏ nhất (client InteractionController.approachPoint); đích Y = mặt đất /
      // tầng ngay dưới chỗ đứng (quái bay lượn trên không: lấy độ cao của quái thì không bao giờ "tới").
      float gx = map.template().clampX(target.x() + (dx < 0 ? -stand : stand));
      w.goTo(gx, BossMover.groundUnder(map.template(), gx, target.y()), target.id(), -1, now);
      reply.accept(target.id(), 0);
   }
   /**
    * Người chơi giả đứng chơi ({@code bot/BotService}): chạy bộ tới hoành độ {@code x} (mặt đất ngay dưới), dừng ở đó —
    * cùng chân {@link OfflineWalker} của Luyện tập Offline nên chạy / nhảy / rơi như người thật. Luồng tick khu.
    */
   void botStroll(WorldPlayer p, float x) {
      if (p.locked || p.data().isDead() || p.zone != this) {
         return;
      }
      long now = System.currentTimeMillis();
      OfflineWalker w = walker(p, now);
      w.targetMob = -1;
      float gx = map.template().clampX(x);
      w.goTo(gx, BossMover.groundUnder(map.template(), gx, p.y + 60), -1, -1, now);
   }

   /** Như {@link #botStroll} nhưng đích là mặt đất dưới ({@code x}, {@code y}) — cửa / chỗ ở tầng khác. */
   void botWalkTo(WorldPlayer p, float x, float y) {
      if (p.locked || p.data().isDead() || p.zone != this) {
         return;
      }
      long now = System.currentTimeMillis();
      OfflineWalker w = walker(p, now);
      w.targetMob = -1;
      float gx = map.template().clampX(x);
      w.goTo(gx, BossMover.groundUnder(map.template(), gx, map.template().clampY(y)), -1, -1, now);
   }

   /** Người thật còn sống gần {@code p} nhất trong {@code radius} (ưu tiên {@code preferId} nếu còn trong tầm). */
   WorldPlayer nearestHuman(WorldPlayer p, int preferId, float radius) {
      if (humans == 0) {
         return null;
      }
      WorldPlayer prefer = preferId > 0 ? players.get(preferId) : null;
      if (prefer != null && !prefer.data().bot && !prefer.data().isDead() && !ZoneCombat.stealthed(prefer)
            && Math.abs(prefer.x - p.x) <= radius && Math.abs(prefer.y - p.y) <= radius) {
         return prefer;
      }
      WorldPlayer best = null;
      float bestDist = Float.MAX_VALUE;
      for (WorldPlayer o : players.values()) {
         if (o == p || o.data().bot || o.data().isDead() || ZoneCombat.stealthed(o)) {
            continue;
         }
         float dx = Math.abs(o.x - p.x);
         float dy = Math.abs(o.y - p.y);
         if (dx <= radius && dy <= radius && dx + dy < bestDist) {
            bestDist = dx + dy;
            best = o;
         }
      }
      return best;
   }

   /** Đồ rơi gần nhất của mình đáng chạy tới nhặt -> đặt đích. @return true nếu đang đi nhặt */
   private boolean seekLoot(WorldPlayer p, OfflineWalker w, int range, long now) {
      ZoneDrops.Drop best = null;
      float bestDist = Float.MAX_VALUE;
      for (ZoneDrops.Drop drop : drops.pickableNear(p.id(), p.x, p.y, OfflineWalker.LOOT_SEEK_PX, AUTO_PICKUP_DELAY_MS,
            now)) {
         float dx = Math.abs(drop.x - p.x);
         float dy = Math.abs(drop.y - p.y);
         if ((dx <= AUTO_PICKUP_RANGE_PX * 0.8f && dy <= AUTO_PICKUP_RANGE_PX * 0.8f) || w.skipDrop(drop.id, now)
               || !fitsInBag(p.data(), drop.item) || !OfflineWalker.inRange(drop.x, drop.y, w.anchorX, w.anchorY, range)) {
            continue; // trong tầm tự nhặt: tự hút, khỏi đi
         }
         if (dx + dy < bestDist) {
            bestDist = dx + dy;
            best = drop;
         }
      }
      if (best == null) {
         return false;
      }
      w.goTo(map.template().clampX(best.x), best.y, -1, best.id, now);
      return true;
   }

   /** Số người (quái đang đánh trả họ) + bot khác (đang nhắm) đang dính vào quái {@code id}, trừ {@code p}. */
   private int claims(int id, WorldPlayer p) {
      int a = mobs.aggroTarget(id);
      int n = a != 0 && a != p.id() ? 1 : 0;
      for (int i = 0, size = offlineWalkers.size(); i < size; i++) {
         WorldPlayer o = offlineWalkers.get(i);
         if (o != p && o.offlineWalker != null && o.offlineWalker.targetMob == id) {
            n++;
         }
      }
      return n;
   }

   /** Số tổ đội đang có người trong khu (cập nhật 2 s / lần). Mọi luồng. */
   int partyCount() {
      return partyCount;
   }

   private void countParties() {
      java.util.Set<com.tlnt.server.party.Party> seen = null;
      for (WorldPlayer p : players.values()) {
         com.tlnt.server.party.Party party = p.data().party;
         if (party != null) {
            if (seen == null) {
               seen = new java.util.HashSet<>();
            }
            seen.add(party);
         }
      }
      partyCount = seen == null ? 0 : seen.size();
   }

   /**
    * Bị boss đẩy / hất tung mà KHÔNG có client tự bay theo op 12 sub 2 (bot, treo offline): server cho bay với vận tốc
    * ({@code vxPx}, {@code vyPx}) px / s + trọng lực ({@link BossMover#launch}), vị trí phát gộp op 5 như khi chạy — người
    * xung quanh thấy văng ra / nảy lên. Đang chạy / nhảy dở thì bỏ. Luồng tick.
    */
   void knockNoClient(WorldPlayer p, float vxPx, float vyPx) {
      if (!players.containsKey(p.id()) || p.locked || p.data().isDead()) {
         return;
      }
      long now = System.currentTimeMillis();
      OfflineWalker w = walker(p, now);
      w.stop();
      w.endHop();
      w.mover = new BossMover(p.x, p.y);
      w.mover.launch(vxPx, vyPx);
      w.knocked = true;
   }

   /** Chân server tự chạy của {@code p} (tạo nếu chưa có) + đưa vào danh sách khu này. */
   private OfflineWalker walker(WorldPlayer p, long now) {
      OfflineWalker w = p.offlineWalker;
      if (w == null) {
         w = p.offlineWalker = new OfflineWalker();
         w.anchorX = p.x;
         w.anchorY = p.y;
         w.lastTickAt = now;
      }
      if (!offlineWalkers.contains(p)) { // vào khu mới (đổi khu / map): walker cũ không nằm trong danh sách khu này
         w.lastTickAt = now;
         offlineWalkers.add(p);
      }
      return w;
   }

   /**
    * Bot đánh trả người {@code targetId} (luồng tick khu): đủ tầm -> báo kỹ năng với tới; chưa -> chạy tới cạnh người đó.
    * Người đó đã chết / rời khu -> id -1.
    */
   void botChase(WorldPlayer p, int targetId, int[] reachX, int[] reachY, World.OfflineReply reply) {
      WorldPlayer t = players.get(targetId);
      if (t == null || t.data().isDead() || p.locked || p.data().isDead()) {
         reply.accept(-1, 0);
         return;
      }
      long now = System.currentTimeMillis();
      OfflineWalker w = walker(p, now);
      w.targetMob = -1;
      int mask = OfflineWalker.reachMask(p.x, p.y, t.x, t.y, reachX, reachY);
      if (mask != 0) {
         w.stop();
         reply.accept(targetId, mask);
         return;
      }
      float stand = OfflineWalker.standOff(reachX);
      float gx = map.template().clampX(t.x + (p.x < t.x ? -stand : stand));
      w.goTo(gx, BossMover.groundUnder(map.template(), gx, t.y), -1, -1, now);
      reply.accept(targetId, 0);
   }

   /**
    * Bot săn boss thế giới dạng NGƯỜI CHƠI GIẢ (Tinh linh — {@link AltarBoss}) mã {@code key} (luồng tick khu): đủ tầm -> báo
    * id thực thể boss (âm, đánh bằng TARGET_PLAYER như người thật) + kỹ năng với tới; chưa -> chạy tới cạnh boss (boss chạy
    * vòng / đuổi người nên đích đổi theo mỗi lần hỏi). Không thấy / đã chết / còn &lt; 20% HP ({@code spare}: nhường người
    * thật kết liễu như thợ săn boss quái) -> id -1.
    */
   void botChaseWorldBoss(WorldPlayer p, int key, int[] reachX, int[] reachY, boolean spare, World.OfflineReply reply) {
      AltarBoss boss = null;
      for (WorldBoss wb : worldBosses) {
         if (wb.request.key() == key && wb.charBoss != null && !wb.dead(this)) {
            boss = wb.charBoss;
         }
      }
      if (boss == null || p.locked || p.data().isDead() || (spare && boss.hp * 5 < boss.spec.hp())) {
         reply.accept(-1, 0);
         return;
      }
      long now = System.currentTimeMillis();
      if (p.data().bot && now - humanJoinedAt < BOT_FRESH_MS) {
         reply.accept(boss.id, 0); // người thật vừa vào khu: đợi client họ dựng xong boss
         return;
      }
      OfflineWalker w = walker(p, now);
      w.targetMob = -1;
      int mask = OfflineWalker.reachMask(p.x, p.y, boss.x, boss.y, reachX, reachY);
      if (mask != 0) {
         w.stop();
         reply.accept(boss.id, mask);
         return;
      }
      if (w.mover != null && !w.mover.onGround()) {
         reply.accept(boss.id, 0); // đang bay / rơi (vừa bị hất): chờ đáp đất
         return;
      }
      float stand = OfflineWalker.standOff(reachX);
      float gx = map.template().clampX(boss.x + (p.x < boss.x ? -stand : stand));
      w.goTo(gx, BossMover.groundUnder(map.template(), gx, boss.y), -1, -1, now);
      reply.accept(boss.id, 0);
   }

   private boolean offlineMatch(int id, World.OfflineQuery q, OfflineWalker w, boolean bot, long now) {
      if (bot && mobs.fresh(id, now, BOT_FRESH_MS)) {
         return false; // vừa hồi sinh: client người xem chưa dựng xong quái
      }
      MapTemplate.MobInstance mob = mobs.alive(id);
      if (mob != null && q.spareBoss() && mobs.isManual(id) && mobs.hp(id) * 5 < mob.spawn().hp()) {
         return false; // bot săn boss: boss sắp chết -> nhường người thật kết liễu
      }
      if (mob == null || mobs.hidden(id) || (q.noBoss() && mobs.isManual(id))
            || (q.normalOnly() && mob.spawn().variant() != com.tlnt.server.combat.MobRules.VARIANT_NORMAL)
            || !OfflineWalker.inRange(mob.x(), mob.y(), w.anchorX, w.anchorY, q.range())) {
         return false;
      }
      int[] templates = q.templates();
      if (templates.length == 0) {
         return true;
      }
      int templateId = mob.spawn().id();
      for (int t : templates) {
         if (t == templateId) {
            return true;
         }
      }
      return false;
   }

   /**
    * Đang nhảy lên đánh quái cao: MỖI TICK xem đã lọt tầm chưa (vị trí sau bước bay) — lọt là báo luồng session ra chiêu
    * ngay trên không (như client tự đánh), mỗi lần nhảy 1 lần. Chạm đất = hết lượt nhảy.
    */
   private void offlineHopStrike(WorldPlayer p, OfflineWalker w, BossMover m) {
      MapTemplate.MobInstance mob = mobs.alive(w.hopMob);
      if (mob == null) {
         w.endHop();
         return;
      }
      if (!w.hopFired) {
         int mask = OfflineWalker.reachMask(m.x, m.y, mob.x(), mob.y(), w.hopQuery.reachX(), w.hopQuery.reachY());
         if (mask != 0) {
            w.hopFired = true;
            w.hopReply.accept(mob.id(), mask);
         }
      }
      if (m.onGround() && w.secondHop == 0) {
         w.endHop();
      }
   }

   /** Mỗi tick: chạy / nhảy / rơi người treo offline 1 bước về đích, vị trí vào lượt phát gộp op 5 như người thường. */
   private void tickOfflineWalkers(long now) {
      MapTemplate map = this.map.template();
      for (int i = offlineWalkers.size() - 1; i >= 0; i--) {
         WorldPlayer p = offlineWalkers.get(i);
         OfflineWalker w = p.offlineWalker;
         if (w == null || p.zone != this || !players.containsKey(p.id())) {
            offlineWalkers.remove(i);
            continue;
         }
         float dt = Math.min(0.1f, (now - w.lastTickAt) / 1000f);
         w.lastTickAt = now;
         PlayerData d = p.data();
         BossMover m;
         if (w.knocked && w.mover != null && !p.locked && !d.isDead()) {
            m = w.mover; // bị boss đẩy / hất (không có client tự bay): server cho bay + rơi, người xem thấy văng ra
            m.flyKnocked(map, dt);
            w.knocked = !m.onGround();
            w.resetProgress(now); // bị văng xa đích không phải "kẹt"
         } else {
            w.knocked = false;
            if (p.locked || d.isDead() || now < d.stunnedUntil || now < d.frozenUntil || now < p.knockedUntil) {
               w.mover = null; // đứng yên / vừa bị hất xong: lần sau bắt đầu lại từ vị trí server đang giữ
               // bị khống chế không phải "kẹt": trước đây đứng yên 4 s / bị đẩy xa -> bỏ mục tiêu (cả boss) 30 s
               w.resetProgress(now);
               continue;
            }
            if (w.mover == null || Math.abs(w.mover.x - p.x) > 2 || Math.abs(w.mover.y - p.y) > 2) {
               w.mover = new BossMover(p.x, p.y); // hồi sinh / đổi khu / bị kéo: đồng bộ lại chân
            }
            m = w.mover;
         }
         if (w.knocked) {
            // đang bay: bỏ qua chạy / nhảy, chỉ cập nhật vị trí bên dưới
         } else if (w.walking) {
            PlayerStats stats = d.stats;
            float speed = Math.max(OfflineWalker.MIN_RUN_SPEED, stats == null ? 0 : stats.moveSpeed);
            m.walkTo(map, w.goalX, w.goalY, speed, now, dt);
            if (m.arrived(w.goalX, w.goalY, OfflineWalker.ARRIVE_PX)) {
               w.walking = false; // tới nơi: giữ goalMob để biết "đứng đúng chỗ mà vẫn không với tới"
            } else {
               w.checkStuck(Math.abs(w.goalX - m.x) + Math.abs(w.goalY - m.y), now);
            }
         } else if (!m.onGround()) {
            if (w.secondHop > 0 && m.hopAgain(w.secondHop)) {
               w.secondHop = 0; // nhảy lần 2 ở đỉnh lần 1 (nhảy kép)
            }
            m.idle(map, now, dt); // đang nhảy / rơi: bay lên rồi rơi xuống theo trọng lực
         }
         if (w.hopReply != null) {
            offlineHopStrike(p, w, m);
         }
         float nx = map.clampX(m.x);
         float ny = map.clampY(m.y);
         if (nx == p.x && ny == p.y) {
            continue;
         }
         if (nx != p.x) {
            p.facingLeft = nx < p.x;
         }
         p.x = nx;
         p.y = ny;
         m.x = nx;
         m.y = ny;
         p.invalidateSpawnPacket();
         p.lastMoveAt = now;
         d.x = nx;
         d.y = ny;
         if (!p.moveDirty) {
            p.moveDirty = true;
            movedBuffer.add(p);
         }
      }
   }

   /** Phát gộp vị trí mới nhất của mọi người đã đi từ lần phát trước (thú cưng: client tự mô phỏng, không phát). */
   private void flushMoves() {
      if (!movedBuffer.isEmpty()) {
         if (players.size() > 1) {
            broadcastMoves(movedBuffer);
         }
         for (WorldPlayer p : movedBuffer) {
            p.moveDirty = false;
         }
         movedBuffer.clear();
      }
   }

   /**
    * Hồi ngân sách theo thời gian từ lần đi hợp lệ trước, rồi trừ quãng đi {@code dx, dy} (y tăng = đi LÊN, như client).
    * @return false = vượt ngân sách (hack tốc độ / dịch chuyển) -> không trừ gì, người gọi kéo về chỗ cũ.
    */
   private static boolean spendMoveBudget(WorldPlayer p, float dx, float dy, long now) {
      if (now < p.knockedUntil) { // bị boss đẩy / hất (ZoneCombat.knock): vận tốc do server đặt, không phải hack
         p.moveBudgetX = BURST_X_PX;
         p.moveBudgetUp = BURST_UP_PX;
         p.moveBudgetDown = BURST_DOWN_PX;
         return true;
      }
      float seconds = Math.max(0, now - p.lastMoveAt) / 1000f;
      PlayerStats stats = p.data().stats;
      float speed = Math.max(MIN_SPEED_PX_PER_SEC, stats == null ? 0 : stats.moveSpeed) * SPEED_MARGIN;
      float budgetX = Math.min(BURST_X_PX, p.moveBudgetX + speed * seconds);
      float budgetUp = Math.min(BURST_UP_PX, p.moveBudgetUp + UP_PX_PER_SEC * seconds);
      float budgetDown = Math.min(BURST_DOWN_PX, p.moveBudgetDown + DOWN_PX_PER_SEC * seconds);
      float up = Math.max(0, dy);
      float down = Math.max(0, -dy);
      if (Math.abs(dx) > budgetX || up > budgetUp || down > budgetDown) {
         return false;
      }
      p.moveBudgetX = budgetX - Math.abs(dx);
      p.moveBudgetUp = budgetUp - up;
      p.moveBudgetDown = budgetDown - down;
      return true;
   }

   /**
    * Phát gộp cho CẢ KHU: nối frame di chuyển của mọi người đã đi từ lần phát trước thành 1 mảng; người không đi nhận
    * nguyên mảng, người có đi nhận 2 đoạn trước / sau frame của chính mình. Mảng mới mỗi lần phát vì tầng mạng còn giữ
    * tham chiếu tới khi ghi xong. (Lọc theo tầm nhìn đã thử rồi bỏ — xem CLAUDE.md mục Di chuyển.)
    */
   /** Vị trí mới nhất của mọi người đã đi: op 82 (mã hoá 1 lần / 127 người, mọi người nhận cùng gói). */
   private void broadcastMoves(List<WorldPlayer> moved) {
      int count = moved.size();
      for (int from = 0; from < count; from += MOVE_BATCH_MAX) {
         int n = Math.min(MOVE_BATCH_MAX, count - from);
         broadcast(WorldPackets.moveBatch(moved, from, n), null); // client bỏ qua dòng của chính mình
      }
   }

   /** Op 82 đếm số người bằng 1 byte có dấu. */
   static final int MOVE_BATCH_MAX = 127;

   private void checkRetire(long now) {
      boolean permanent = ownerId == 0 && linhMoc == null && arena == null
            && id < map.template().zoneCount() // khu cố định map thường
            || now < keepUntil; // khu phó bản còn trong lượt: giữ quái đã diệt cho lúc quay lại
      if (permanent || !players.isEmpty() || occupancy.get() > 0) {
         emptySince = 0;
         return;
      }
      if (closeWhenEmpty) { // phó bản hết lượt: đóng ngay khi trống
         map.retireIfEmpty(this);
         return;
      }
      if (emptySince == 0) {
         emptySince = now;
      } else if (now - emptySince >= RETIRE_AFTER_MS) {
         map.retireIfEmpty(this);
      }
   }

   // ------------------------------------------------------------------ vào / ra (luồng tick)

   /** Người chơi vào khu (chỗ đã giữ trước). Gửi map + người khác cho họ, gửi họ cho người khác. */
   void join(WorldPlayer p, float x, float y) {
      if (p.removed) { // thoát game trong lúc đang chuyển tới
         if (p.data().bot) {
            botSlots.decrementAndGet();
         }
         releaseReservation();
         return;
      }
      p.zone = this;
      p.x = x;
      p.y = y;
      p.invalidateSpawnPacket();
      p.lastMoveAt = System.currentTimeMillis();
      p.moveBudgetX = BURST_X_PX; // vào khu / đổi map: ngân sách đầy
      p.moveBudgetUp = BURST_UP_PX;
      p.moveBudgetDown = BURST_DOWN_PX;
      p.clearPendingPosition();
      int fromMapId = p.data().mapId;
      p.data().mapId = map.template().id();
      p.data().x = x;
      p.data().y = y;
      // Map cấm PK (làng, nông trại): tự về hoà bình, gói xuất hiện / khối chỉ số dựng sau dòng này đã có trạng thái mới.
      if (PkRules.PEACE_MAP_IDS.contains(map.template().id()) && p.data().faction != PkRules.PEACE) {
         p.data().faction = PkRules.PEACE;
         p.data().factionChangedAt = System.currentTimeMillis();
         p.send(WorldPackets.faction(p.id(), PkRules.PEACE)); // HUD của chính mình (client giữ nhân vật qua đổi map)
      }
      // Lôi vực Sấm sét: cờ ép Bộ lạc (đánh mọi người trừ cùng bộ lạc); rời ra map khác thì về Hoà bình.
      boolean toThunder = map.isThunder();
      boolean fromThunder = !toThunder && com.tlnt.server.thunder.ThunderRules.isThunderMap(fromMapId);
      boolean fromArena = arena == null && Arena.isArenaMap(fromMapId);
      byte forced = toThunder ? PkRules.TRIBE : arena != null ? arena.factionNow()
            : fromThunder || fromArena ? PkRules.PEACE : -1;
      if (forced >= 0 && p.data().faction != forced) {
         p.data().faction = forced;
         p.data().factionChangedAt = 0; // ra khỏi Lôi vực đổi cờ được ngay
         p.send(WorldPackets.faction(p.id(), forced));
      }

      // Opcode 6 chuyển client sang màn tải map và xoá thực thể cũ (giữ lại nhân vật của mình).
      p.send(map.packets().info(id));
      p.send(map.packets().tiles());
      p.send(map.packets().decorations());
      for (Packet packet : map.packets().entities()) { // NPC / cây (op 6 vừa xoá hết ở client)
         p.send(packet);
      }
      for (Packet packet : mobs.snapshot()) {
         p.send(packet);
      }
      mobs.sendWitnessed(p.id(), p::send); // siêu quái đang ẩn mà người này đã thấy lúc xuất hiện
      p.send(plants.snapshot()); // cây theo khu (mọc ngẫu nhiên, bị hái) — op 6 vừa xoá hết ở client
      if (map.template().id() == AppleTreeService.FARM_MAP_ID) {
         showAppleTree(p);
         com.tlnt.server.net.Message pigs = com.tlnt.server.farm.PigService.listPacket(p.data().farm,
               System.currentTimeMillis());
         if (pigs != null) {
            p.send(pigs); // heo đang nuôi (op 52 sub 0 — client tự đếm giờ lớn)
         }
      }
      if (drops.size() > 0) {
         p.send(drops.snapshot()); // vật phẩm dưới đất
         for (ZoneDrops.Drop d : drops.personalOf(p.id())) { // đồ nhiệm vụ riêng của người này
            p.send(WorldPackets.groundItemAdd(d.id, d.item.templateId, d.x, d.y));
         }
      }
      List<Object[]> props = new ArrayList<>(); // vật trang trí op 81: củ cà rốt đang chờ Ngọc thố
      for (WorldBoss wb : worldBosses) {
         if (wb.charBoss != null && wb.charBoss.rabbit != null && !wb.charBoss.gone) {
            for (Rabbit.Carrot c : wb.charBoss.rabbit.carrots) {
               props.add(new Object[] {c.id(), Rabbit.carrotTexture(), c.x(), c.y()});
            }
         }
      }
      if (!props.isEmpty()) {
         p.send(WorldPackets.mapPropList(props));
      }
      boolean firstJoin = !p.spawnedOnClient;
      if (!p.spawnedOnClient) {
         p.send(WorldPackets.localPlayer(p));
         WorldPackets.sendInitialState(p); // đọc túi đồ trên luồng tick — ItemService chờ inWorld mới cho sửa
         p.spawnedOnClient = true;
         p.data().inWorld = true;
      } else {
         p.send(WorldPackets.teleportSelf(x, y));
      }
      QuestEvents.joinedMap(p, firstJoin, map.world()::mapTemplate); // nhiệm vụ (lần đầu) + mũi tên theo map mới
      String regionNotice = Regions.enterNotice(firstJoin ? 0 : fromMapId, map.template().id());
      if (regionNotice != null) { // vừa tới Avenna / Frearh
         p.send(com.tlnt.server.net.Packets.notice(regionNotice));
      }
      if (map.isAltar()) { // đồng hồ lượt Tế đàn cạnh nút Thoát (client tự đếm)
         sendAltarTimer(p);
      } else if (AltarRules.isAltarMap(fromMapId)) {
         p.send(WorldPackets.announcementRemove(ALTAR_TIMER_ID));
      }
      if (linhMoc != null) { // đồng hồ lượt Rừng Linh Mộc cạnh nút Thoát
         sendLinhMocTimer(p, linhMoc);
      } else if (LinhMocRules.isDungeonMap(fromMapId)) {
         p.send(WorldPackets.announcementRemove(LINH_MOC_TIMER_ID));
      }
      if (toThunder) { // đồng hồ lượt Lôi vực cạnh nút Thoát
         sendThunderTimer(p);
         World.ThunderHooks hooks = map.world().thunderHooks();
         com.tlnt.server.thunder.ThunderRun run = p.data().thunderRun;
         if (hooks != null && run != null && !com.tlnt.server.thunder.ThunderRules.isThunderMap(fromMapId)) {
            hooks.entered(p, run);
         }
      } else if (fromThunder) {
         p.send(WorldPackets.announcementRemove(THUNDER_TIMER_ID));
         World.ThunderHooks hooks = map.world().thunderHooks();
         com.tlnt.server.thunder.ThunderRun run = p.data().thunderRun;
         if (hooks != null && run != null) {
            hooks.left(p, run); // rời Lôi vực = hết lượt
         }
      }
      if (arena != null) { // đồng hồ khu đấu Giải đấu Bộ lạc
         sendArenaTimer(p, arena);
      } else if (fromArena) {
         p.send(WorldPackets.announcementRemove(Arena.TIMER_ID));
      }
      if (map.isAltar() || AltarRules.isAltarMap(fromMapId)) {
         World.AltarHooks hooks = map.world().altarHooks();
         if (hooks != null) {
            hooks.mapChanged(p, fromMapId, map.template().id());
         }
      }
      if (boss != null && !boss.gone) {
         p.send(WorldPackets.bossSpawn(boss));
      }
      for (WorldPlayer other : players.values()) {
         p.send(other.spawnPacket());
         if (ZoneCombat.stealthed(other)) {
            p.send(WorldPackets.stealth(other.id(), true)); // 28 Ảo Ảnh Phách: đang tàng hình
         }
         if (other.pet != null) {
            other.pet.placeBeside(other, map.template()); // server không theo dõi thú đi đâu: hiện cạnh chủ hiện tại
            p.send(WorldPackets.petSpawn(other));
         }
         if (other.escort != null) {
            p.send(WorldPackets.escortSpawn(other));
         }
      }
      broadcast(p.spawnPacket(), null);
      players.put(p.id(), p);
      if (!p.data().bot) {
         humans++;
         humanJoinedAt = System.currentTimeMillis();
      }
      p.pet = null; // op 6 vừa xoá thú cưng cũ ở client -> hiện lại cạnh chỗ mới
      syncPet(p);
      syncEscort(p, System.currentTimeMillis());
      sendPartyFlags(p); // đồng đội trong khu thấy nhau tên vàng (gói xuất hiện vừa đặt cờ về theo khối chỉ số)
      p.send(WorldPackets.showWorld());

      Log.debug("'%s' vao map %d '%s' khu %d (%d/%d)", p.data().name, map.template().id(), map.template().name(), id,
            players.size(), maxPlayers);
   }

   /** CHỈ cho test: đặt người vào khu không gửi gói nào (bỏ qua {@link #join}). */
   void putPlayerForTest(WorldPlayer p) {
      p.zone = this;
      players.put(p.id(), p);
   }

   /** Người chơi rời khu (đổi khu/map hoặc thoát). Gọi 2 lần vẫn an toàn. */
   void leave(WorldPlayer p) {
      if (ZoneCombat.stealthed(p) && players.containsKey(p.id())) {
         ZoneCombat.breakStealth(this, p); // hiện hình trước khi đi (cả client của chính họ: nhân vật giữ qua đổi map)
      }
      if (players.remove(p.id()) == null) {
         return;
      }
      if (p.data().bot) {
         botSlots.decrementAndGet();
      } else {
         humans--;
      }
      if (p.moveDirty) { // vị trí chưa phát của khu này không được theo người sang khu khác
         movedBuffer.remove(p);
         p.moveDirty = false;
      }
      World.endDuel(this, p, "Thách đấu kết thúc do rời khu vực.");
      World.cancelHarvest(this, p, null);
      if (p.zone == this) {
         p.zone = null;
      }
      releaseReservation();
      broadcast(WorldPackets.playerLeave(p.id()), null);
      if (p.pet != null) {
         broadcast(WorldPackets.playerLeave(p.petId()), null);
         p.pet = null;
      }
      if (p.escort != null) { // vẫn giữ ở p: chủ mang sang khu mới ({@link #syncEscort})
         broadcast(WorldPackets.playerLeave(p.escortId()), null);
      }
      if (arena != null && map.world() != null) { // rời khu đấu (bị đưa về / tự thoát / thoát game) — giải xử lý
         World.ArenaHooks hooks = map.world().arenaHooks();
         if (hooks != null) {
            hooks.left(arena, p);
         }
      }
   }

   /**
    * Server đặt {@code p} tới (x, y) (khu đấu: kéo lại lên đài / hồi sinh chỗ khác): op dịch chuyển cho chính họ, vị trí
    * mới cho người khác trong khu. Luồng tick.
    */
   void placeAt(WorldPlayer p, float x, float y) {
      float nx = map.template().clampX(x);
      float ny = map.template().clampY(y);
      p.x = nx;
      p.y = ny;
      p.data().x = nx;
      p.data().y = ny;
      p.clearPendingPosition();
      p.invalidateSpawnPacket();
      p.lastMoveAt = System.currentTimeMillis();
      p.moveBudgetX = BURST_X_PX;
      p.moveBudgetUp = BURST_UP_PX;
      p.moveBudgetDown = BURST_DOWN_PX;
      p.send(WorldPackets.teleportSelf(nx, ny));
      broadcast(WorldPackets.teleportOther(p.id(), nx, ny), p); // op 12 sub 1: người khác thấy nhảy tới ngay
   }

   /** Mọi người trong khu đấu: đổi cờ PK theo giai đoạn (chờ = Hoà bình, đánh = cờ của khu đấu), phát op 42. Luồng tick. */
   void applyArenaFaction() {
      if (arena == null) {
         return;
      }
      byte faction = arena.factionNow();
      for (WorldPlayer p : players.values()) {
         if (p.data().faction != faction) {
            p.data().faction = faction;
            p.data().factionChangedAt = 0;
            p.invalidateSpawnPacket();
            broadcast(WorldPackets.faction(p.id(), faction).toPacket(), null);
         }
      }
   }

   // ------------------------------------------------------------------ thú cưng đi theo (luồng tick)

   /**
    * Cho thú cưng hiện ở client khớp với template trong {@code Appearance}: đổi / tháo / ẩn -> xoá con
    * cũ (op 4), hiện con mới cạnh chủ (op 3) cho cả khu kể cả chủ. Gọi khi vào khu và mỗi lần ngoại hình đổi.
    */
   void syncPet(WorldPlayer p) {
      com.tlnt.server.item.Appearance look = p.data().appearance;
      int wantTemplateId = look != null ? look.petTemplateId() : 0;
      String name = look != null ? look.petName() : "";
      Pet current = p.pet;
      if (current != null && current.templateId == wantTemplateId && current.name.equals(name)) {
         return;
      }
      if (current != null) {
         broadcast(WorldPackets.playerLeave(p.petId()), null);
         p.pet = null;
      }
      if (wantTemplateId > 0) {
         Pet pet = new Pet(wantTemplateId, name);
         pet.placeBeside(p, map.template());
         p.pet = pet;
         broadcast(WorldPackets.petSpawn(p), null);
      }
   }

   // ------------------------------------------------------------------ sinh vật hộ tống (luồng tick)

   /**
    * Bắt đầu hộ tống: sinh vật hiện sau lưng chủ (cả khu thấy). Đang có con khác thì thay. Đang đứng ở map không cho
    * phép ({@code maps}) = thất bại luôn.
    */
   void startEscort(WorldPlayer p, int templateId, String name, List<Integer> maps) {
      removeEscort(p);
      Escort escort = new Escort(templateId, name, maps);
      if (!escort.allowedOn(map.template().id())) {
         failEscort(p, "Moara không dám đi tới " + map.template().name() + ", nó hoảng sợ bỏ chạy.");
         return;
      }
      escort.placeBeside(p, map.template(), System.currentTimeMillis());
      p.escort = escort;
      p.data().escorting = true;
      broadcast(WorldPackets.escortSpawn(p), null);
   }

   /** Thôi hộ tống (xong nhiệm vụ / đổi bước): xoá sinh vật ở client. */
   void removeEscort(WorldPlayer p) {
      if (p.escort != null) {
         broadcast(WorldPackets.playerLeave(p.escortId()), null);
         p.escort = null;
      }
      p.data().escorting = false;
   }

   private void failEscort(WorldPlayer p, String reason) {
      removeEscort(p);
      QuestEvents.escortFailed(p, reason);
   }

   /**
    * Vào khu: đang hộ tống -> sinh vật hiện lại sau lưng (op 6 vừa xoá nó), trừ khi map mới không nằm trong
    * {@link Escort#maps} hoặc lúc qua cửa nó còn cách quá {@link Escort#WARN_PX} (bỏ lại phía sau) = thất bại. Đang ở
    * bước hộ tống mà không có sinh vật (vừa vào game — thoát game giữa chừng) = thất bại, làm lại từ bước trước.
    */
   private void syncEscort(WorldPlayer p, long now) {
      Escort escort = p.escort;
      if (escort == null) {
         p.data().escorting = false;
         if (QuestEvents.atEscortStep(p.data())) {
            QuestEvents.escortFailed(p, "Không có ai đi cùng, Moara đã hoảng sợ bỏ chạy.");
         }
         return;
      }
      if (!escort.allowedOn(map.template().id())) {
         failEscort(p, "Moara không dám đi tới " + map.template().name() + ", nó hoảng sợ bỏ chạy.");
         return;
      }
      if (escort.lag > Escort.WARN_PX) {
         failEscort(p, "Bạn đã bỏ Moara lại phía sau, nó hoảng sợ bỏ chạy.");
         return;
      }
      escort.placeBeside(p, map.template(), now);
      broadcast(WorldPackets.escortSpawn(p), null);
   }

   /** Mỗi tick: sinh vật hộ tống đi theo chủ (như thú cưng), báo / thất bại khi tụt lại xa, phát vị trí cho khu. */
   private void tickEscorts(long now) {
      for (WorldPlayer p : players.values()) {
         Escort escort = p.escort;
         if (escort == null) {
            continue;
         }
         if (p.data().isDead()) {
            failEscort(p, "Bạn đã bị hạ gục, Moara hoảng sợ bỏ chạy.");
            continue;
         }
         float seconds = Math.min(0.5f, Math.max(0, now - escort.lastTickAt) / 1000f);
         escort.lastTickAt = now;
         PlayerStats stats = p.data().stats;
         float speed = Math.max(MIN_SPEED_PX_PER_SEC, stats == null ? 0 : stats.moveSpeed) * Escort.SPEED_FACTOR;
         escort.update(p, map.template(), speed, now, seconds);
         if (escort.lag > Escort.FAIL_PX) {
            failEscort(p, "Bạn đã bỏ Moara lại quá xa, nó hoảng sợ bỏ chạy.");
            continue;
         }
         if (escort.lag > Escort.WARN_PX && !escort.warned) {
            escort.warned = true;
            p.send(Packets.notice("Moara không theo kịp! Hãy chờ Moara, bỏ nó lại quá xa nhiệm vụ sẽ thất bại."));
         } else if (escort.lag < Escort.WARN_PX / 2) {
            escort.warned = false;
         }
         if (escort.dirty && now >= escort.nextSendAt) {
            escort.dirty = false;
            escort.nextSendAt = now + moveBroadcastMs;
            broadcast(WorldPackets.entityMove(p.escortId(), escort.x, escort.y), null);
         }
      }
   }

   ZoneMobs mobs() {
      return mobs;
   }

   /** Khu sắp đóng (luồng tick của khu): trả suất toàn server của cây + quái săn bắt (tổ ong, quả dừa). */
   void releaseQuotas() {
      plants.releaseAll();
      mobs.releaseGathers();
   }

   ZoneDrops drops() {
      return drops;
   }

   ZonePlants plants() {
      return plants;
   }

   Consumer<Packet> broadcastAll() {
      return broadcastAll;
   }

   WorldPlayer find(int playerId) {
      return players.get(playerId);
   }

   /** Người trong khu (chỉ luồng tick; không thêm / bớt người trong lúc duyệt). */
   Collection<WorldPlayer> players() {
      return players.values();
   }

   /**
    * KHÔNG còn làm gì (giữ chỗ gọi cũ). Trước 2026-10-18: tên vàng cho tổ đội bằng cờ thách đấu
    * ({@link WorldPackets#duelFlag}) gửi riêng cho đồng đội.
    * Chỉ luồng tick.
    */
   void sendPartyFlags(WorldPlayer p) {
      // Bỏ 2026-10-18: trước đây bật cờ op 41/0 cho đồng đội để client (bản cũ tô vàng theo cờ đó) thấy tên vàng. Client
      // giờ làm như decompile C3489: cùng partyId (khối chỉ số) = vàng #FFF600, cờ op 41/0 = đang thách đấu = tên ĐỎ ->
      // giữ mẹo cũ thì đồng đội hiện đỏ. Id tổ đội đã đi theo khối chỉ số (World.broadcastStats khi vào / rời nhóm).
   }

   /** Gửi 1 gói (encode sẵn) cho mọi người trong khu, trừ {@code except}. */
   void broadcast(Packet packet, WorldPlayer except) {
      for (WorldPlayer p : players.values()) {
         if (p != except) {
            p.send(packet);
         }
      }
   }
}
