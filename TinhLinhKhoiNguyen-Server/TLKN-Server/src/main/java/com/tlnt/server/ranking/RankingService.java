package com.tlnt.server.ranking;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.mail.Mail;
import com.tlnt.server.mail.MailRepository;
import com.tlnt.server.mail.MailService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.player.Gender;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongPredicate;
import java.util.function.ToLongFunction;

/**
 * Bảng xếp hạng (op 39, cửa sổ {@code RankingWindow}: {@link RankType} Cấp độ / Ve chai / Chiến binh / Cường hóa /
 * Sự kiện) + Top tuần (op 73, tab "Top tuần" cửa sổ Cửa hàng: {@link WeeklyKind} Giàu có / Cường hóa / Chiến binh /
 * Ve chai, thưởng top 1-3 qua thư mỗi thứ Hai 00:00).
 *
 * Không có cột / bảng riêng cho điểm: số liệu nằm trong dòng {@code player} (level, exp, recharge_total, item_body,
 * cột JSON {@code ranking} = {@link PlayerData.RankStats}). Lúc khởi động đọc 1 lần số liệu mọi nhân vật vào RAM
 * ({@link #rows}); sau đó cập nhật từ người đang online (mỗi lần tính lại) và lúc thoát game. Tính lại cả bảng mỗi
 * {@link #REFRESH_SECONDS} giây trên luồng blocking, và ngay khi có người xin bảng mà bản đang có cũ hơn
 * {@link #STALE_MS} (xin bảng chạy lane BLOCKING); công bố snapshot bất biến (volatile), không SQL.
 *
 * <pre>
 * S→C 39/0 danh sách bảng (gửi lúc vào game; client tạo tab 1 lần): byte n, n×(int id, str tên, str icon)
 * C→S 39/0 xin 1 bảng ({@code sendRequestRanking}, mở tab / kéo làm mới): int id
 * S→C 39/1 1 bảng: int id, int n, n×(int hạng, int playerId, str tên, str giá trị, byte char_template),
 *          rồi dòng của mình cùng định dạng (hạng 0 = chưa xếp hạng)
 * S→C 73/0 Top tuần (lúc vào game): byte n, n×(int id, str tên, long ms còn lại tới hết tuần,
 *          3 lần [byte k, k×item] = quà top 1 / 2 / 3)
 * C→S 73/0 xin 1 bảng tuần ({@code sendRequestWeeklyTop}): int id
 * S→C 73/1 int id, int n, n×(int hạng, int playerId, str tên, str điểm), rồi dòng của mình
 * </pre>
 *
 * Trả thưởng tuần: {@link #checkWeekly} chạy mỗi phút; tuần trước chưa trả ({@code server_state.weekly_top_rewarded})
 * -> chốt top 3 mỗi bảng theo điểm của TUẦN ĐÓ ({@link Row#weekly}: {@code RankStats} giữ cả tuần trước nên người
 * chơi đã sang tuần mới vẫn đọc đúng), ghi mọi thư + cờ trong 1 transaction rồi mới giao thư. Máy chủ tắt qua mốc tuần
 * -> lần khởi động sau vẫn trả (điểm tuần trước nằm trong cột JSON).
 */
public final class RankingService {

   public static final int TOP = 100;
   /** Top tuần gửi ít dòng hơn (bảng client không có kéo trang). */
   public static final int WEEKLY_TOP = 50;
   public static final int REFRESH_SECONDS = 60;
   /**
    * Xin bảng mà bản tính đã cũ hơn chừng này -> tính lại ngay trước khi trả (lane BLOCKING), để top khớp với dòng "của
    * mình" (dòng đó luôn là số sống). Trước đây chỉ tính lại 5 phút / lần -> top đứng yên trong khi dòng mình tăng.
    */
   static final long STALE_MS = 10_000;
   private static final long REQUEST_COOLDOWN_MS = 1_000;
   /** Điểm Chiến binh khi kết liễu Tinh anh / Thủ lĩnh. */
   public static final int ELITE_POINTS = 5;
   public static final int LEADER_POINTS = 10;

   /** Index mảng điểm tuần ({@link Row#weekly}). */
   static final int W_RECHARGE = 0;
   static final int W_WARRIOR = 1;
   static final int W_GOLD = 2;
   static final int W_ENHANCE = 3;
   private static final int W_COUNT = 4;

   /**
    * Số liệu xếp hạng 1 nhân vật (bản chụp, bất biến). Điểm tuần: {@code weekly} của tuần {@code week},
    * {@code prevWeekly} của tuần {@code prevWeek} (index {@link #W_RECHARGE}...).
    */
   public record Row(int playerId, String name, int gender, int level, long exp, long recharge, long warrior, long gold,
         long enhance, long event, int week, long[] weekly, int prevWeek, long[] prevWeekly) {

      static Row of(PlayerData d) {
         return of(d.getId(), d.name, d.gender, d.level, d.exp, d.rechargeTotal, d.rankStats, d.body.asList());
      }

      static Row of(int playerId, String name, int gender, int level, long exp, long recharge,
            PlayerData.RankStats s, List<ItemData> worn) {
         long warrior;
         long gold;
         long event;
         int week;
         int prevWeek;
         long[] cur = new long[W_COUNT];
         long[] prev = new long[W_COUNT];
         synchronized (s) {
            warrior = s.warriorPoints;
            gold = s.goldPicked;
            event = s.eventPoints;
            week = s.week;
            prevWeek = s.prevWeek;
            cur[W_RECHARGE] = s.weekRecharge;
            cur[W_WARRIOR] = s.weekWarrior;
            cur[W_GOLD] = s.weekGold;
            prev[W_RECHARGE] = s.prevRecharge;
            prev[W_WARRIOR] = s.prevWarrior;
            prev[W_GOLD] = s.prevGold;
         }
         cur[W_ENHANCE] = EnhanceScore.weekly(worn, week);
         prev[W_ENHANCE] = EnhanceScore.weekly(worn, prevWeek);
         return new Row(playerId, name, gender, level, exp, recharge, warrior, gold, EnhanceScore.total(worn), event,
               week, cur, prevWeek, prev);
      }

      /** Điểm tuần {@code weekId} ở ô {@code index}; 0 nếu không giữ số của tuần đó. */
      long weekly(int weekId, int index) {
         if (weekId == 0) {
            return 0;
         }
         if (weekId == week) {
            return weekly[index];
         }
         return weekId == prevWeek ? prevWeekly[index] : 0;
      }
   }

   /** 1 bảng đã tính: top + hạng của mọi người có hạng + điểm để hiện. */
   private record Board(List<Row> top, RankIndex ranks) {
   }

   /**
    * Hạng của mọi người có hạng trong 1 bảng: mảng {@code long} (playerId << 32 | hạng) sắp theo id, tra bằng tìm nhị
    * phân. 8 byte / người thay cho {@code HashMap<Integer, Integer>} (~60 byte / người + rác mỗi lần tính lại) — số
    * dòng = MỌI nhân vật từng tạo, không chỉ người online.
    */
   static final class RankIndex {
      private final long[] packed;

      /** {@code ranked} đã sắp theo hạng (phần tử 0 = hạng 1). */
      RankIndex(List<Row> ranked) {
         packed = new long[ranked.size()];
         for (int i = 0; i < packed.length; i++) {
            packed[i] = (long) ranked.get(i).playerId() << 32 | (i + 1);
         }
         Arrays.sort(packed);
      }

      /** Hạng (từ 1), 0 = chưa xếp hạng. */
      int rankOf(int playerId) {
         long key = (long) playerId << 32;
         int at = Arrays.binarySearch(packed, key); // hạng ≥ 1 nên không bao giờ trúng đúng key -> điểm chèn
         at = at >= 0 ? at : -at - 1;
         return at < packed.length && (int) (packed[at] >>> 32) == playerId ? (int) packed[at] : 0;
      }
   }

   private final RankingRepository repository;
   private final PlayerService players;
   private final MailService mails;
   private final Map<Integer, Row> rows = new ConcurrentHashMap<>();
   private static volatile RankingService instance;
   private volatile Map<RankType, Board> boards = new EnumMap<>(RankType.class);
   private volatile Map<WeeklyKind, Board> weeklyBoards = new EnumMap<>(WeeklyKind.class);
   /** Tuần của {@link #weeklyBoards}. */
   private volatile int boardsWeek;
   /** Tuần gần nhất đã gửi thư thưởng. */
   private volatile int rewardedWeek;
   private final AtomicBoolean awarding = new AtomicBoolean();
   /** Lúc tính lại bảng gần nhất. */
   private volatile long refreshedAt;

   public RankingService(RankingRepository repository, PlayerService players, MailService mails) throws SQLException {
      instance = this;
      this.repository = repository;
      this.players = players;
      this.mails = mails;
      for (Row row : repository.loadAll()) {
         rows.put(row.playerId(), row);
      }
      reloadConfig();
      rewardedWeek = repository.rewardedWeek();
      if (rewardedWeek == 0) { // lần đầu chạy tính năng: không trả thưởng hồi tố cho tuần trước
         rewardedWeek = WeekClock.weekId(System.currentTimeMillis()) - 1;
         repository.saveRewardedWeek(rewardedWeek);
      }
      refresh();
   }

   /** Admin panel sửa nhân vật offline: cập nhật số trong RAM. */
   public void update(PlayerData d) {
      rows.put(d.getId(), Row.of(d));
   }

   /** Admin panel xoá nhân vật: bỏ khỏi bảng (có hiệu lực ở lượt tính lại kế tiếp). */
   public void remove(int playerId) {
      rows.remove(playerId);
   }

   /** Hook thoát game: giữ số cuối cùng của người vừa thoát. */
   public void onLogout(PlayerData d) {
      rows.put(d.getId(), Row.of(d));
   }

   // ================================================================== móc từ hệ thống khác (luồng bất kỳ)

   /** Nhặt vàng từ đất (quái / boss rơi): bảng Ve chai + Ve chai tuần. */
   /**
    * Admin panel: {@code n} nhân vật nhiều điểm sự kiện nhất (số liệu RAM, cập nhật mỗi lần tính lại bảng / lưu) —
    * {id, name, level, points}.
    */
   public static List<java.util.Map<String, Object>> topEvent(int n) {
      RankingService s = instance;
      if (s == null) {
         return List.of();
      }
      List<Row> all = new ArrayList<>();
      for (Row r : s.rows.values()) {
         if (r.event() > 0) {
            all.add(r);
         }
      }
      all.sort((a, b) -> Long.compare(b.event(), a.event()));
      List<java.util.Map<String, Object>> out = new ArrayList<>();
      for (int i = 0; i < Math.min(n, all.size()); i++) {
         Row r = all.get(i);
         java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
         m.put("id", r.playerId());
         m.put("name", r.name());
         m.put("level", r.level());
         m.put("points", r.event());
         out.add(m);
      }
      return out;
   }

   /** Giới tính đã biết của nhân vật {@code playerId} (mọi nhân vật nằm trong RAM), -1 = không rõ. Mọi luồng. */
   public static int genderOf(int playerId) {
      RankingService s = instance;
      Row row = s == null ? null : s.rows.get(playerId);
      return row == null ? -1 : row.gender();
   }

   /**
    * Op 75 (client mới {@code OtherRankWindow}, decompile C3873 m18111): bảng xếp hạng riêng — str tên cột giá trị, byte n,
    * n × (int id, str tên, byte char_template theo giới tính, str giá trị). Client tự đánh số hạng 1..n theo thứ tự.
    */
   public static Message otherRankPacket(String column, List<int[]> idsAndGender, List<String> names, List<String> values) {
      Message msg = new Message(Opcode.OTHER_RANK);
      msg.writeString(column);
      int n = Math.min(Math.min(names.size(), values.size()), Math.min(idsAndGender.size(), 127));
      msg.writeByte(n);
      for (int i = 0; i < n; i++) {
         int gender = idsAndGender.get(i)[1];
         msg.writeInt(idsAndGender.get(i)[0]);
         msg.writeString(names.get(i));
         msg.writeByte(Gender.charTemplateId(Gender.isValid(gender) ? gender : Gender.RINN));
         msg.writeString(values.get(i));
      }
      return msg;
   }

   public static void goldPicked(PlayerData d, long amount) {
      if (d != null && amount > 0) {
         d.rankStats.addGold(amount, WeekClock.weekId(System.currentTimeMillis()));
      }
   }

   /** Kết liễu siêu quái ({@code combat/MobRules.VARIANT_ELITE} = Tinh anh, VARIANT_BOSS = Thủ lĩnh). Luồng khu. */
   public static void eliteKilled(PlayerData d, boolean leader) {
      if (d != null) {
         d.rankStats.addWarrior(leader ? LEADER_POINTS : ELITE_POINTS, WeekClock.weekId(System.currentTimeMillis()));
      }
   }

   /** Lam thạch nạp: Giàu có tuần (tổng nạp vẫn là {@code player.recharge_total}). */
   public static void recharged(PlayerData d, long amount) {
      if (d != null && amount > 0) {
         d.rankStats.addRecharge(amount, WeekClock.weekId(System.currentTimeMillis()));
      }
   }

   /** Ngay trước 1 lần nâng cấp trang bị: đánh dấu tuần trên món (Cường hóa tuần). */
   public static void beforeUpgrade(PlayerData d, ItemData equip) {
      int week = WeekClock.weekId(System.currentTimeMillis());
      d.rankStats.roll(week); // tuần của điểm Cường hóa tuần = tuần của RankStats
      EnhanceScore.markUpgrade(equip, week);
   }

   // ================================================================== tính bảng

   /** Tính lại mọi bảng (luồng blocking — scheduler gọi định kỳ). */
   public synchronized void refresh() {
      long start = System.currentTimeMillis();
      for (PlayerData d : players.onlinePlayers()) { // số trong RAM mới hơn
         rows.put(d.getId(), Row.of(d));
      }
      List<Row> all = new ArrayList<>(rows.values());
      Map<RankType, Board> next = new EnumMap<>(RankType.class);
      for (RankType type : RankType.values()) {
         next.put(type, board(all, type::value, type::tiebreak, type::ranked, TOP));
      }
      int week = WeekClock.weekId(start);
      Map<WeeklyKind, Board> nextWeekly = new EnumMap<>(WeeklyKind.class);
      for (WeeklyKind kind : WeeklyKind.values()) {
         nextWeekly.put(kind, board(all, r -> r.weekly(week, kind.index), r -> 0, v -> v > 0, WEEKLY_TOP));
      }
      boards = next;
      weeklyBoards = nextWeekly;
      boardsWeek = week;
      refreshedAt = start;
      Log.debug("Tinh lai bang xep hang (%d nhan vat) trong %d ms", all.size(), System.currentTimeMillis() - start);
   }

   private static Board board(Collection<Row> all, ToLongFunction<Row> value, ToLongFunction<Row> tiebreak,
         LongPredicate ranked, int top) {
      List<Row> list = sorted(all, value, tiebreak, ranked);
      return new Board(List.copyOf(list.subList(0, Math.min(top, list.size()))), new RankIndex(list));
   }

   /** Mọi người có hạng, sắp theo điểm giảm dần, bằng điểm thì tiebreak rồi id nhỏ trước (cố định). */
   static List<Row> sorted(Collection<Row> all, ToLongFunction<Row> value, ToLongFunction<Row> tiebreak,
         LongPredicate ranked) {
      List<Row> list = new ArrayList<>();
      for (Row r : all) {
         if (ranked.test(value.applyAsLong(r))) {
            list.add(r);
         }
      }
      list.sort(Comparator.comparingLong(value).reversed()
            .thenComparing(Comparator.comparingLong(tiebreak).reversed())
            .thenComparingInt(Row::playerId));
      return list;
   }

   /** Tính lại nếu bản đang có cũ hơn {@link #STALE_MS} (nhiều người xin cùng lúc -> chỉ 1 người tính). */
   private void refreshIfStale(long now) {
      if (now - refreshedAt < STALE_MS) {
         return;
      }
      synchronized (this) {
         if (System.currentTimeMillis() - refreshedAt >= STALE_MS) {
            refresh();
         }
      }
   }

   // ================================================================== Top tuần: trả thưởng

   /**
    * Luồng blocking, mỗi phút: nạp lại cấu hình; sang tuần mới thì tính lại bảng; tuần trước chưa trả thưởng thì gửi thư
    * top 1-3 mỗi bảng (1 transaction cho mọi thư + cờ tuần).
    */
   public void checkWeekly() {
      reloadConfig();
      long now = System.currentTimeMillis();
      int week = WeekClock.weekId(now);
      if (boardsWeek != week) {
         refresh(); // bảng tuần mới bắt đầu từ 0
      }
      int target = week - 1;
      if (rewardedWeek >= target || !awarding.compareAndSet(false, true)) {
         return;
      }
      try {
         award(target, now);
      } catch (RuntimeException e) {
         awarding.set(false);
         Log.warn("Top tuan: loi tra thuong tuan %d: %s", target, e);
      }
   }

   private void award(int target, long now) {
      for (PlayerData d : players.onlinePlayers()) {
         rows.put(d.getId(), Row.of(d));
      }
      List<Row> all = new ArrayList<>(rows.values());
      List<Mail> out = new ArrayList<>();
      for (WeeklyRewards.Event event : WeeklyRewards.events()) {
         WeeklyKind kind = WeeklyKind.of(event.id());
         List<Row> ranked = sorted(all, r -> r.weekly(target, kind.index), r -> 0, v -> v > 0);
         for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            Row r = ranked.get(i);
            int rank = i + 1;
            List<ItemData> items = event.create(rank, now);
            String subject = "Top tuần: " + event.name();
            String content = "Chúc mừng bạn đạt TOP " + rank + " bảng " + event.name() + " tuần qua với "
                  + kind.format(r.weekly(target, kind.index)) + "! Nhận phần thưởng đính kèm.";
            for (int k = 0; k < Math.max(1, items.size()); k += MailService.MAX_ITEMS) {
               List<ItemData> part = items.isEmpty() ? List.of()
                     : new ArrayList<>(items.subList(k, Math.min(items.size(), k + MailService.MAX_ITEMS)));
               out.add(mails.systemMail(r.playerId(), r.name(), subject, content, part));
            }
         }
      }
      players.runOnWriter(conn -> {
         for (Mail mail : out) {
            MailRepository.insert(conn, mail);
         }
         RankingRepository.saveRewardedWeek(conn, target);
      }).thenAccept(ok -> {
         if (ok) {
            rewardedWeek = target;
            out.forEach(mails::deliver);
            Log.info("Top tuan: da tra thuong tuan %d (%d thu)", target, out.size());
         } else {
            Log.warn("Top tuan: ghi thu thuong tuan %d that bai, thu lai sau", target);
         }
         awarding.set(false);
      });
   }

   private void reloadConfig() {
      try {
         WeeklyRewards.load(repository.loadWeeklyEvents());
      } catch (SQLException e) {
         Log.warn("Top tuan: khong doc duoc tlnt_data.weekly_top: %s", e.getMessage());
      }
   }

   // ================================================================== gói

   /** C→S 39/0 (lane BLOCKING: có thể tính lại bảng). */
   public void request(PlayerSession session, int categoryId) {
      PlayerData d = session.getPlayer();
      RankType type = RankType.of(categoryId);
      if (d == null || !d.inWorld || type == null) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastRankingAt < REQUEST_COOLDOWN_MS) {
         return; // client gửi mỗi lần đổi tab / kéo làm mới — bỏ qua gói dồn dập
      }
      d.lastRankingAt = now;
      refreshIfStale(now);
      Board board = boards.get(type);
      if (board == null) {
         return;
      }
      Message msg = new Message(Opcode.RANKING);
      msg.writeByte(1);
      msg.writeInt(type.id);
      msg.writeInt(board.top().size());
      for (int i = 0; i < board.top().size(); i++) {
         Row r = board.top().get(i);
         writeRow(msg, i + 1, r.playerId(), r.name(), type.format(type.value(r)), r.gender());
      }
      long myValue = type.value(Row.of(d));
      writeRow(msg, board.ranks().rankOf(d.getId()), d.getId(), d.name,
            type.ranked(myValue) ? type.format(myValue) : "Chưa xếp hạng", d.gender);
      session.send(msg);
   }

   /** C→S 73/0: 1 bảng Top tuần (tuần hiện tại). */
   public void requestWeekly(PlayerSession session, int eventId) {
      PlayerData d = session.getPlayer();
      WeeklyKind kind = WeeklyKind.of(eventId);
      if (d == null || !d.inWorld || kind == null) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastWeeklyTopAt < REQUEST_COOLDOWN_MS) {
         return;
      }
      d.lastWeeklyTopAt = now;
      refreshIfStale(now);
      Board board = weeklyBoards.get(kind);
      if (board == null) {
         return;
      }
      int week = boardsWeek;
      Message msg = new Message(Opcode.WEEKLY_TOP);
      msg.writeByte(1);
      msg.writeInt(kind.id);
      msg.writeInt(board.top().size());
      for (int i = 0; i < board.top().size(); i++) {
         Row r = board.top().get(i);
         writeWeeklyRow(msg, i + 1, r.playerId(), r.name(), kind.format(r.weekly(week, kind.index)));
      }
      long mine = Row.of(d).weekly(week, kind.index);
      writeWeeklyRow(msg, board.ranks().rankOf(d.getId()), d.getId(), d.name,
            mine > 0 ? kind.format(mine) : "Chưa xếp hạng");
      session.send(msg);
   }

   /** S→C 39/0: danh sách bảng (lúc vào game — client tạo tab khi mở cửa sổ lần đầu). */
   public static Message categoriesPacket() {
      Message msg = new Message(Opcode.RANKING);
      msg.writeByte(0);
      msg.writeByte(RankType.values().length);
      for (RankType type : RankType.values()) {
         msg.writeInt(type.id);
         msg.writeString(type.title);
         msg.writeString(type.icon);
      }
      return msg;
   }

   /** S→C 73/0: các bảng Top tuần + thời gian còn lại + quà top 1-3 (lúc vào game). */
   public static Message weeklyEventsPacket(long now) {
      List<WeeklyRewards.Event> events = WeeklyRewards.events();
      long remaining = Math.max(0, WeekClock.weekEnd(WeekClock.weekId(now)) - now);
      Message msg = new Message(Opcode.WEEKLY_TOP);
      msg.writeByte(0);
      msg.writeByte(events.size());
      for (WeeklyRewards.Event event : events) {
         msg.writeInt(event.id());
         msg.writeString(event.name());
         msg.writeLong(remaining);
         for (int rank = 1; rank <= 3; rank++) {
            List<ItemData> items = event.create(rank, now);
            msg.writeByte(items.size());
            for (ItemData item : items) {
               ItemPackets.writeItem(msg, item);
            }
         }
      }
      return msg;
   }

   private static void writeRow(Message msg, int rank, int playerId, String name, String value, int gender) {
      writeWeeklyRow(msg, rank, playerId, name, value);
      msg.writeByte(Gender.charTemplateId(Gender.isValid(gender) ? gender : Gender.RINN));
   }

   private static void writeWeeklyRow(Message msg, int rank, int playerId, String name, String value) {
      msg.writeInt(rank);
      msg.writeInt(playerId);
      msg.writeString(name == null ? "" : name);
      msg.writeString(value);
   }
}
