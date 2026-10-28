package com.tlnt.server.daily;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.data.model.MapPack;
import com.tlnt.server.forge.UpgradeService;
import com.tlnt.server.item.ItemContainer;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.wheel.LuckyWheelService;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Nhiệm vụ hằng ngày (op 69; cửa sổ Nhiệm vụ tab "Hàng ngày" {@code DailyQuestPanel}). Cấu hình = bảng
 * {@code tlnt_data.daily_quest} ({@link DailyConfig}, nạp lại mỗi phút), tiến độ = cột JSON {@code player.daily_quest}
 * ({@link DailyState}).
 *
 * <pre>
 * C→S 69/0 mở tab ({@code sendRequestMount} — tên sai)
 *     69/1 int id mốc chăm chỉ "Nhận" ({@code sendPetSummon})
 *     69/2 int index "Đổi NV" — NV chưa xong ({@code sendPetDismiss})
 *     69/3 int index "Nhận" thưởng NV đã xong ({@code sendPetInteract})
 *     69/4 "Nhận NV mới" ({@code sendPetStay})
 * S→C ({@code handleAuctionUpdate} — tên sai)
 *     69/0 byte n × (int id, int điểm cần, item quà | -1, bool đã nhận), int điểm chăm chỉ, long lúc reset (ms epoch)
 *     69/1 int id mốc, bool đã nhận
 *     69/2 int điểm chăm chỉ
 *     69/3 byte n × (str tên, str mô tả, int cần, int đã làm, byte m × item quà, bool đã nhận) — index = thứ tự; client
 *          bật chấm đỏ nếu có NV xong chưa nhận
 *     69/4 int index, int đã làm, bool đã nhận
 * </pre>
 * Mỗi ngày (0:00 giờ server) giao {@link DailyConfig#baseTasks} NV khác loại, random từ danh sách đang bật; "Nhận NV
 * mới" / "Đổi NV" tốn 1 Vé nhiệm vụ (hỏi xác nhận op 32 loại 15 / 16), tối đa {@link DailyConfig#maxExtra} /
 * {@link DailyConfig#maxSwaps} lần / ngày. Xong NV -> "Nhận": 1 món NGẪU NHIÊN theo trọng số (khoá) + điểm chăm chỉ +
 * điểm hoạt động. Điểm chăm chỉ cộng dồn cả TUẦN (reset thứ Hai 0:00), đủ mốc thì nhận quà mốc.
 * Luồng: mọi thay đổi chạy trên luồng session của chính người đó ({@link DailyEvents} chuyển về); cho đồ dưới
 * {@code synchronized (player)} trên bản sao túi (thiếu chỗ thì không nhận).
 */
public final class DailyQuestService {

   static final int SUB_OPEN = 0;
   static final int SUB_MILESTONE = 1;
   static final int SUB_SWAP = 2;
   static final int SUB_CLAIM = 3;
   static final int SUB_ADD = 4;
   /** Người rơm: có NV riêng, không nằm trong NV "Đánh hạ x" theo cấp. */
   static final int MOB_SCARECROW = 33;
   private static final ZoneId ZONE = ZoneId.systemDefault();
   private static final long CLICK_COOLDOWN_MS = 300;
   private static final String MOB = "{mob}";

   private final DailyRepository repository;
   private final PlayerService players;
   private final ItemService items;
   private final World world;
   final Executor gamePool;
   private volatile DailyConfig config;
   private volatile DailyMobs mobs;

   public DailyQuestService(DailyRepository repository, PlayerService players, ItemService items, World world,
         Executor gamePool) {
      this.repository = repository;
      this.players = players;
      this.items = items;
      this.world = world;
      this.gamePool = gamePool;
      reload();
      DailyEvents.install(this);
      DailyConfig c = config;
      Log.info("Nhiem vu hang ngay: %d loai NV, %d qua, %d moc cham chi, %d loai quai", c == null ? 0 : c.tasks().size(),
            c == null ? 0 : c.rewards().size(), c == null ? 0 : c.milestones().size(), mobs.mobIds().size());
   }

   /** Đọc lại bảng cấu hình + danh sách quái theo map (luồng blocking). Lỗi đọc -> giữ bản cũ. */
   public void reload() {
      Map<String, String> rows = repository.loadRows();
      if (rows != null) {
         config = DailyConfig.parse(rows);
      }
      mobs = DailyMobs.of(world.fieldMaps(), Set.of(MOB_SCARECROW), id -> {
         MapPack.Mob mob = GameDataCache.mob(id);
         return mob == null ? -1 : mob.variants() == null ? 0 : mob.variants().size();
      });
   }

   // ================================================================== vào game / mở tab

   /** Hook vào game: qua ngày thì giao NV mới, rồi gửi danh sách (để client hiện chấm đỏ nếu có NV chờ nhận). */
   public void onLogin(PlayerSession session, PlayerData d) {
      d.dailyQuest = d.dailyQuest == null ? new DailyState() : d.dailyQuest.normalize();
      DailyConfig c = config;
      if (c != null) {
         prepare(d, c);
         session.send(milestonesPacket(d.dailyQuest, c));
         session.send(tasksPacket(d.dailyQuest));
      }
   }

   /** C→S 69/0. */
   public void open(PlayerSession session) {
      PlayerData d = ready(session);
      DailyConfig c = config;
      if (d == null) {
         return;
      }
      if (c == null) {
         session.send(Packets.notice("Nhiệm vụ hằng ngày đang tạm đóng."));
         return;
      }
      prepare(d, c);
      session.send(milestonesPacket(d.dailyQuest, c));
      session.send(tasksPacket(d.dailyQuest));
   }

   /**
    * Sang ngày / tuần mới -> reset + giao {@link DailyConfig#baseTasks} NV. Đổi cấu trúc danh sách dưới
    * {@code synchronized (d)} (lượt lưu luồng khác chụp JSON dưới khoá này). @return sang ngày mới
    */
   boolean prepare(PlayerData d, DailyConfig c) {
      DailyState s = d.dailyQuest;
      LocalDate today = LocalDate.now(ZONE);
      if (s.day == today.toEpochDay() && s.week == today.with(DayOfWeek.MONDAY).toEpochDay()) {
         return false;
      }
      Random random = ThreadLocalRandom.current();
      synchronized (d) {
         if (!s.roll(today)) {
            return false; // chỉ sang tuần (cùng ngày — vd lệnh test)
         }
         for (int k = 0; k < c.baseTasks(); k++) {
            DailyState.Task task = newTask(d, c, random);
            if (task == null) {
               break;
            }
            s.tasks.add(task);
         }
         s.refreshWatch();
      }
      return true;
   }

   // ================================================================== giao NV

   /** 1 NV mới khác loại mọi NV đang có (random đều trong các loại đang bật, giao được). null = hết loại. */
   DailyState.Task newTask(PlayerData d, DailyConfig c, Random random) {
      List<DailyConfig.TaskDef> pool = new ArrayList<>();
      for (DailyConfig.TaskDef def : c.tasks()) {
         if (def.enabled() && !d.dailyQuest.hasTask(def.id())) {
            pool.add(def);
         }
      }
      while (!pool.isEmpty()) {
         DailyConfig.TaskDef def = pool.remove(random.nextInt(pool.size()));
         DailyState.Task task = build(d, def, c, random);
         if (task != null) {
            return task;
         }
      }
      return null;
   }

   /** NV từ mẫu {@code def}; đánh quái theo cấp mà chưa mở map nào có quái hợp -> null. */
   private DailyState.Task build(PlayerData d, DailyConfig.TaskDef def, DailyConfig c, Random random) {
      int target = def.target();
      String name = def.name();
      boolean byLevel = (def.kind() == DailyKind.KILL || def.kind() == DailyKind.KILL_ELITE) && target == 0;
      if (byLevel) {
         DailyMobs.Option mob = mobs.choose(d.level, mapId -> QuestEvents.canEnter(d, mapId),
               def.kind() == DailyKind.KILL_ELITE, c.levelSpread(), random);
         if (mob == null) {
            return null;
         }
         target = mob.mobId();
      }
      if (name.contains(MOB)) {
         name = name.replace(MOB, mobName(target));
      }
      int goal = def.min() + (def.max() > def.min() ? random.nextInt(def.max() - def.min() + 1) : 0);
      return new DailyState.Task(def.id(), def.kind(), target, goal, name);
   }

   // ================================================================== tiến độ (luồng session)

   /** +{@code amount} cho mọi NV chưa xong loại {@code kind} có mục tiêu 0 hoặc đúng {@code target}. */
   void onEvent(PlayerSession session, DailyKind kind, int target, long amount) {
      PlayerData d = session.getPlayer();
      DailyConfig c = config;
      if (d == null || d.dailyQuest == null || c == null) {
         return;
      }
      DailyState s = d.dailyQuest;
      if (prepare(d, c)) { // qua 0:00 lúc đang online: NV hôm qua đã bị thay
         session.send(milestonesPacket(s, c));
         session.send(tasksPacket(s));
         return;
      }
      List<String> finished = new ArrayList<>();
      for (int i = 0; i < s.tasks.size(); i++) {
         DailyState.Task t = s.tasks.get(i);
         if (t.done() || t.kind() != kind || (t.t != 0 && t.t != target)) {
            continue;
         }
         t.p = (int) Math.min(t.g, t.p + amount);
         if (t.done()) {
            finished.add(t.n);
         } else {
            session.send(progressPacket(i, t));
         }
      }
      if (!finished.isEmpty()) {
         s.refreshWatch();
         session.send(tasksPacket(s)); // cả danh sách: client bật chấm đỏ "có NV chờ nhận"
         for (String name : finished) {
            session.send(Packets.notice("Hoàn thành nhiệm vụ hằng ngày: " + name));
         }
      }
   }

   // ================================================================== nhận thưởng NV (C→S 69/3)

   public void claim(PlayerSession session, int index) {
      PlayerData d = ready(session);
      DailyConfig c = config;
      if (d == null || c == null || prepare(d, c) && resend(session, d, c)) {
         return;
      }
      DailyState s = d.dailyQuest;
      DailyState.Task t = index >= 0 && index < s.tasks.size() ? s.tasks.get(index) : null;
      if (t == null || !t.done() || t.c) {
         resend(session, d, c);
         return;
      }
      long now = System.currentTimeMillis();
      Random random = ThreadLocalRandom.current();
      ItemData gift = createReward(c.pick(random.nextInt(c.totalWeight())), now, random);
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể nhận thưởng."));
            return;
         }
         String error = ItemService.give(d, List.of(gift), now);
         if (error != null) {
            session.send(Packets.notice(error));
            return;
         }
         t.c = true;
         s.points += c.diligence();
         d.dynamicPoints += c.activity();
      }
      players.saveAsync(d, false); // quà + cờ "đã nhận" + điểm cùng 1 dòng player
      items.syncBagAndCurrency(session);
      session.send(tasksPacket(s)); // tắt chấm đỏ khi không còn NV chờ nhận
      session.send(pointsPacket(s.points));
      session.send(WorldPackets.dynamicPoints(d.dynamicPoints));
      session.send(ItemPackets.rewardPopup(List.of(gift)));
      session.send(Packets.notice("Bạn vừa nhận được " + c.diligence() + " điểm chăm chỉ và " + c.activity()
            + " điểm hoạt động"));
      Log.debug("NV hang ngay: '%s' nhan thuong NV %s -> item %d x%d", d.name, t.id, gift.templateId, gift.quantity);
   }

   /** Quà 1 lần nhận: item khoá, số lượng random trong khoảng "q", hạn dùng dòng 57 + % vĩnh viễn như vòng quay. */
   static ItemData createReward(DailyConfig.Reward r, long now, Random random) {
      ItemData item = LuckyWheelService.applyExpiry(r.item().create(now), r.permPercent(), now, random);
      if (r.qtyMin() > 0) {
         item.quantity = r.qtyMin() + (r.qtyMax() > r.qtyMin() ? random.nextInt(r.qtyMax() - r.qtyMin() + 1) : 0);
      }
      item.locked = true;
      return item;
   }

   // ================================================================== mốc chăm chỉ (C→S 69/1)

   public void claimMilestone(PlayerSession session, int id) {
      PlayerData d = ready(session);
      DailyConfig c = config;
      if (d == null || c == null) {
         return;
      }
      prepare(d, c);
      DailyState s = d.dailyQuest;
      DailyConfig.Milestone m = c.milestone(id);
      if (m == null || m.item() == null || s.claimed.contains(id) || s.points < m.points()) {
         session.send(milestonesPacket(s, c)); // client lệch -> gửi lại trạng thái thật
         return;
      }
      long now = System.currentTimeMillis();
      ItemData gift = m.item().create(now);
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể nhận thưởng."));
            return;
         }
         String error = ItemService.give(d, List.of(gift), now);
         if (error != null) {
            session.send(Packets.notice(error));
            return;
         }
         s.claimed.add(id);
      }
      players.saveAsync(d, false);
      items.syncBagAndCurrency(session);
      session.send(milestoneClaimedPacket(id));
      session.send(ItemPackets.rewardPopup(List.of(gift)));
      Log.debug("NV hang ngay: '%s' nhan moc cham chi %d (%d diem)", d.name, id, m.points());
   }

   // ================================================================== đổi NV (C→S 69/2) / nhận NV mới (69/4)

   /** "Đổi NV": kiểm tra trước rồi hỏi xác nhận (op 32 loại 15) — đồng ý mới trừ vé ({@link #confirmSwap}). */
   public void askSwap(PlayerSession session, int index) {
      PlayerData d = ready(session);
      DailyConfig c = config;
      if (d == null || c == null || prepare(d, c) && resend(session, d, c)) {
         return;
      }
      DailyState s = d.dailyQuest;
      DailyState.Task t = index >= 0 && index < s.tasks.size() ? s.tasks.get(index) : null;
      String error = t == null || t.done() || t.c ? "Chỉ đổi được nhiệm vụ chưa hoàn thành."
            : s.swaps >= c.maxSwaps() ? "Bạn đã hết lượt đổi nhiệm vụ hôm nay."
            : ticketError(d, c);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      d.dailySwapTask = t;
      session.send(Packets.confirm(Packets.CONFIRM_DAILY_SWAP, "Dùng 1 " + itemName(c.ticketItem())
            + " để đổi nhiệm vụ \"" + t.n + "\"?"));
   }

   /** Op 32 loại 15 "Đồng ý". */
   public void confirmSwap(PlayerSession session) {
      PlayerData d = ready(session);
      DailyConfig c = config;
      Object picked = d == null ? null : d.dailySwapTask;
      if (d == null || c == null || picked == null) {
         return;
      }
      d.dailySwapTask = null;
      if (prepare(d, c)) {
         resend(session, d, c);
         return;
      }
      DailyState s = d.dailyQuest;
      int index = s.tasks.indexOf(picked);
      DailyState.Task t = index >= 0 ? s.tasks.get(index) : null;
      String error = t == null || t.done() || t.c ? "Chỉ đổi được nhiệm vụ chưa hoàn thành."
            : s.swaps >= c.maxSwaps() ? "Bạn đã hết lượt đổi nhiệm vụ hôm nay." : null;
      DailyState.Task next = error == null ? newTask(d, c, ThreadLocalRandom.current()) : null;
      if (error == null && next == null) {
         error = "Không còn nhiệm vụ nào khác để đổi.";
      }
      if (error == null) {
         error = payTicket(session, d, c);
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      synchronized (d) {
         s.tasks.set(index, next);
         s.swaps++;
         s.refreshWatch();
      }
      players.saveAsync(d, false); // vé đã trừ
      items.syncBagAndCurrency(session);
      session.send(tasksPacket(s));
      session.send(Packets.notice("Đã đổi sang nhiệm vụ \"" + next.n + "\"."));
      Log.debug("NV hang ngay: '%s' doi NV %s -> %s", d.name, t.id, next.id);
   }

   /** "Nhận NV mới": kiểm tra trước rồi hỏi xác nhận (op 32 loại 16). */
   public void askAdd(PlayerSession session) {
      PlayerData d = ready(session);
      DailyConfig c = config;
      if (d == null || c == null || prepare(d, c) && resend(session, d, c)) {
         return;
      }
      DailyState s = d.dailyQuest;
      String error = s.added >= c.maxExtra() ? "Bạn đã mở số lượng tối đa nhiệm vụ hằng ngày" : ticketError(d, c);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      session.send(Packets.confirm(Packets.CONFIRM_DAILY_ADD, "Dùng 1 " + itemName(c.ticketItem())
            + " để nhận thêm 1 nhiệm vụ hằng ngày?"));
   }

   /** Op 32 loại 16 "Đồng ý". */
   public void confirmAdd(PlayerSession session) {
      PlayerData d = ready(session);
      DailyConfig c = config;
      if (d == null || c == null) {
         return;
      }
      if (prepare(d, c)) {
         resend(session, d, c);
         return;
      }
      DailyState s = d.dailyQuest;
      String error = s.added >= c.maxExtra() ? "Bạn đã mở số lượng tối đa nhiệm vụ hằng ngày" : null;
      DailyState.Task next = error == null ? newTask(d, c, ThreadLocalRandom.current()) : null;
      if (error == null && next == null) {
         error = "Không còn nhiệm vụ nào khác để nhận.";
      }
      if (error == null) {
         error = payTicket(session, d, c);
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      synchronized (d) {
         s.tasks.add(next);
         s.added++;
         s.refreshWatch();
      }
      players.saveAsync(d, false);
      items.syncBagAndCurrency(session);
      session.send(tasksPacket(s));
      session.send(Packets.notice("Nhận nhiệm vụ mới: " + next.n));
      Log.debug("NV hang ngay: '%s' nhan them NV %s", d.name, next.id);
   }

   private static String ticketError(PlayerData d, DailyConfig c) {
      return ItemService.count(d.bag, c.ticketItem(), System.currentTimeMillis()) < 1
            ? "Cần 1 " + itemName(c.ticketItem()) + " trong hành trang." : null;
   }

   /** Trừ 1 vé trên bản sao túi. @return câu lỗi, null = đã trừ */
   private static String payTicket(PlayerSession session, PlayerData d, DailyConfig c) {
      long now = System.currentTimeMillis();
      synchronized (d) {
         if (session.getPlayer() != d) {
            return "";
         }
         if (d.trade != null) {
            return "Đang giao dịch, không thể dùng vé.";
         }
         ItemContainer bag = d.bag.deepCopy();
         if (ItemService.count(bag, c.ticketItem(), now) < 1) {
            return "Cần 1 " + itemName(c.ticketItem()) + " trong hành trang.";
         }
         UpgradeService.cancel(session, d); // huỷ lựa chọn trong lò rèn (túi sắp đổi)
         ItemService.remove(bag, c.ticketItem(), 1, now);
         d.bag = bag;
      }
      return null;
   }

   // ================================================================== gói S→C

   static Message milestonesPacket(DailyState s, DailyConfig c) {
      long now = System.currentTimeMillis();
      Message msg = new Message(Opcode.DAILY_QUEST);
      msg.writeByte(0);
      msg.writeByte(c.milestones().size());
      for (DailyConfig.Milestone m : c.milestones()) {
         msg.writeInt(m.id());
         msg.writeInt(m.points());
         if (m.item() == null) {
            msg.writeInt(-1);
         } else {
            ItemPackets.writePreview(msg, m.item(), now);
         }
         msg.writeBoolean(s.claimed.contains(m.id()));
      }
      msg.writeInt(s.points);
      msg.writeLong(weekEnd(LocalDate.ofEpochDay(s.week)));
      return msg;
   }

   /** 0:00 thứ Hai tuần sau (ms epoch) — client đếm ngược "Thời gian: ". */
   static long weekEnd(LocalDate anyDayOfWeek) {
      return anyDayOfWeek.with(DayOfWeek.MONDAY).plusWeeks(1).atStartOfDay(ZONE).toInstant().toEpochMilli();
   }

   private Message tasksPacket(DailyState s) {
      DailyConfig c = config;
      List<DailyConfig.Reward> rewards = c == null ? List.of() : c.rewards();
      long now = System.currentTimeMillis();
      Message msg = new Message(Opcode.DAILY_QUEST);
      msg.writeByte(3);
      msg.writeByte(s.tasks.size());
      for (DailyState.Task t : s.tasks) {
         msg.writeString(t.n);
         msg.writeString(t.n + ": " + t.p + "/" + t.g);
         msg.writeInt(t.g);
         msg.writeInt(t.p);
         msg.writeByte(rewards.size());
         for (DailyConfig.Reward r : rewards) {
            ItemPackets.writePreviewRanged(msg, preview(r), now);
         }
         msg.writeBoolean(t.c);
      }
      return msg;
   }

   /** Ô xem trước: số lượng hiện số lớn nhất của khoảng "q". */
   private static RewardItem preview(DailyConfig.Reward r) {
      RewardItem item = r.item();
      return r.qtyMax() <= 0 ? item : new RewardItem(item.templateId(), r.qtyMax(), item.locked(), item.stats(),
            item.statsMax(), item.expireDays());
   }

   static Message progressPacket(int index, DailyState.Task t) {
      Message msg = new Message(Opcode.DAILY_QUEST);
      msg.writeByte(4);
      msg.writeInt(index);
      msg.writeInt(t.p);
      msg.writeBoolean(t.c);
      return msg;
   }

   static Message pointsPacket(int points) {
      Message msg = new Message(Opcode.DAILY_QUEST);
      msg.writeByte(2);
      msg.writeInt(points);
      return msg;
   }

   static Message milestoneClaimedPacket(int id) {
      Message msg = new Message(Opcode.DAILY_QUEST);
      msg.writeByte(1);
      msg.writeInt(id);
      msg.writeBoolean(true);
      return msg;
   }

   /** Gửi lại cả 2 danh sách. @return true (để viết gọn {@code prepare(...) && resend(...)}) */
   private boolean resend(PlayerSession session, PlayerData d, DailyConfig c) {
      session.send(milestonesPacket(d.dailyQuest, c));
      session.send(tasksPacket(d.dailyQuest));
      return true;
   }

   // ================================================================== tiện ích

   /** Người chơi đang trong game + chống bấm liên tục. null = bỏ qua gói. */
   private static PlayerData ready(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld || d.dailyQuest == null) {
         return null;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastDailyClickAt < CLICK_COOLDOWN_MS) {
         return null;
      }
      d.lastDailyClickAt = now;
      return d;
   }

   private static String mobName(int mobId) {
      MapPack.Mob mob = GameDataCache.mob(mobId);
      return mob != null ? mob.name() : "quái #" + mobId;
   }

   private static String itemName(int templateId) {
      ItemPack.ItemTemplate t = GameDataCache.itemTemplate(templateId);
      return t != null ? t.name() : "vật phẩm #" + templateId;
   }

   // ================================================================== lệnh test

   private static final String USAGE = "Lệnh: /hn [info] | xong [i] (xong NV thứ i, bỏ = tất cả) | c <i> N (cộng N)"
         + " | ngay (sang ngày mới: giao lại NV) | tuan (reset điểm chăm chỉ + mốc) | diem N (đặt điểm chăm chỉ)"
         + " | gui (gửi lại bảng)";

   /** Lệnh chat {@code /hn ...} ({@code chat/ChatService}, luồng session). */
   public static void testCommand(PlayerSession session, PlayerData d, String[] args) {
      DailyQuestService s = DailyEvents.service();
      DailyConfig c = s == null ? null : s.config;
      if (c == null || d.dailyQuest == null) {
         session.send(Packets.notice("Chưa bật nhiệm vụ hằng ngày (chạy sql/migrations/2026-10-10_daily_quest.sql)."));
         return;
      }
      DailyState st = d.dailyQuest;
      s.prepare(d, c);
      String sub = args.length > 1 ? args[1] : "info";
      int a = args.length > 2 ? parse(args[2], -1) : -1;
      int b = args.length > 3 ? parse(args[3], 1) : 1;
      switch (sub) {
         case "xong" -> {
            for (int i = 0; i < st.tasks.size(); i++) {
               DailyState.Task t = st.tasks.get(i);
               if ((a < 0 || a == i) && !t.done()) {
                  s.onEvent(session, t.kind(), t.t, t.g - t.p);
               }
            }
         }
         case "c" -> {
            if (a >= 0 && a < st.tasks.size()) {
               DailyState.Task t = st.tasks.get(a);
               s.onEvent(session, t.kind(), t.t, Math.max(1, b));
            }
         }
         case "ngay" -> {
            st.day = 0;
            s.prepare(d, c);
            s.resend(session, d, c);
         }
         case "tuan" -> {
            st.week = 0;
            s.prepare(d, c);
            s.resend(session, d, c);
         }
         case "diem" -> {
            st.points = Math.max(0, a);
            s.resend(session, d, c);
         }
         case "gui" -> s.resend(session, d, c);
         default -> {
            StringBuilder sb = new StringBuilder("NV hằng ngày: ");
            for (int i = 0; i < st.tasks.size(); i++) {
               DailyState.Task t = st.tasks.get(i);
               sb.append(i).append(". ").append(t.n).append(' ').append(t.p).append('/').append(t.g)
                     .append(t.c ? " (đã nhận)" : "").append("; ");
            }
            sb.append("thêm ").append(st.added).append('/').append(c.maxExtra()).append(", đổi ").append(st.swaps)
                  .append('/').append(c.maxSwaps()).append(", chăm chỉ ").append(st.points).append(". ").append(USAGE);
            session.send(Packets.notice(sb.toString()));
         }
      }
   }

   private static int parse(String text, int def) {
      try {
         return Integer.parseInt(text);
      } catch (NumberFormatException e) {
         return def;
      }
   }
}
