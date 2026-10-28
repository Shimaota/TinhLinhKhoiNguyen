package com.tlnt.server.tournament;

import com.tlnt.server.item.ItemService;
import com.tlnt.server.mail.Mail;
import com.tlnt.server.mail.MailRepository;
import com.tlnt.server.mail.MailService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.npc.NpcContext;
import com.tlnt.server.party.PartyService;
import com.tlnt.server.player.ActivityPoints;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.Arena;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPlayer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Giải đấu Bộ lạc: lịch giờ (giờ server), hộp thoại Tư tế Buraen (NPC 17 — {@code altar/AltarService} chuyển trang
 * {@code >= PAGE_BASE} + Buraen ở map 22 sang đây), lệ phí, thư thưởng, khoá tổ đội. Luật từng giải: {@link Tournament} +
 * {@link TournamentRules}; phía khu: {@code world/Arena} + {@link World#enterArena} ...
 *
 * <p>CHỈ RAM (người dùng chốt): tắt máy chủ giữa chừng = giải huỷ, không hoàn phí; thưởng đã gửi thư thì vẫn còn.
 *
 * <p>Luồng: mọi trạng thái dưới khoá {@code this}; nhịp 1 giây trên luồng scheduler; móc từ luồng tick khu đấu
 * ({@link World.ArenaHooks}) chuyển sang scheduler rồi mới lấy khoá. Khoá tổ đội đọc không khoá ({@link #locks}) để
 * {@link PartyService} (khoá riêng) không bao giờ phải chờ khoá này.
 */
public final class TournamentService implements World.ArenaHooks {

   /** Trang hộp thoại (Tế đàn 0..4, Lôi vực 20..22). */
   public static final int PAGE_BASE = 40;
   static final int PAGE_MAIN_OPEN = 40;
   static final int PAGE_INFO = 41;
   static final int PAGE_DAILY = 42;
   static final int PAGE_INSIDE = 43;
   static final int PAGE_RANK = 44;
   static final int PAGE_MAIN = 45;
   static final int PAGE_DAILY_REGISTERED = 46;

   static final String INTRO = "Giải đấu Bộ lạc là nơi những chiến binh mạnh nhất sẽ bước vào đấu trường để chứng minh"
         + " bản lĩnh\nNgười chiến thắng sẽ trở thành thống lĩnh của bộ lạc Eldarah và nhận được nhiều phần thưởng hấp"
         + " dẫn";
   static final String INFO = "Giải Nhi đồng - Mở đăng ký: 9h00 hàng ngày (Yêu cầu Level 5-25) Phí đăng ký 50k vàng\n"
         + "Giải Chiến binh - Mở đăng ký: 12h00 hàng ngày (Yêu cầu Level 26-45) Phí đăng ký 200k vàng\n"
         + "Giải Thủ lĩnh - Mở đăng ký: 17h00 hàng ngày (Yêu cầu Level 46 trở lên) Phí đăng ký 500k vàng\n"
         + "Giải Anh hùng - Mở đăng ký: 20h00 Thứ 2, Thứ 4 và Thứ 6 (Yêu cầu Level 5 trở lên)\n"
         + "Giải Hợp chiến - Mở đăng ký: 20h00 Thứ 3, Thứ 5 và Thứ 7 (Yêu cầu Level 5 trở lên)";
   /** Số dòng bảng xếp hạng hiện ở hộp thoại. */
   static final int RANK_ROWS = 10;
   static final String MAIL_SUBJECT = "Giải đấu Bộ lạc";

   private final World world;
   private final ItemService items;
   private final PlayerService players;
   private final MailService mails;
   private final PartyService parties;
   private final ScheduledExecutorService scheduler;
   private final Random random = new Random();
   private final AtomicLong arenaIds = new AtomicLong();
   /** Giải đang tổ chức (tối đa 1 / loại). Dưới khoá this. */
   private final Map<TournamentKind, Tournament> active = new EnumMap<>(TournamentKind.class);
   /** Ngày đã tổ chức gần nhất từng loại (không mở lại trong ngày). Dưới khoá this. */
   private final Map<TournamentKind, LocalDate> held = new EnumMap<>(TournamentKind.class);
   /** Khoá tổ đội: id người -> {@link Tournament#LOCK_JOIN} / {@link Tournament#LOCK_ALL}. */
   private final Map<Integer, Byte> locks = new ConcurrentHashMap<>();
   private final Tournament.Env env = new ServiceEnv();

   public TournamentService(World world, ItemService items, PlayerService players, MailService mails,
         PartyService parties, ScheduledExecutorService scheduler) {
      this.world = world;
      this.items = items;
      this.players = players;
      this.mails = mails;
      this.parties = parties;
      this.scheduler = scheduler;
      PartyService.tournamentLock = this::partyLock;
      instance = this;
   }

   // ================================================================== lịch giờ (scheduler, mỗi giây)

   public void tick() {
      try {
         synchronized (this) {
            long now = System.currentTimeMillis();
            LocalDateTime clock = LocalDateTime.now();
            for (TournamentKind kind : TournamentKind.values()) {
               if (!active.containsKey(kind) && kind.days.contains(clock.getDayOfWeek())
                     && !clock.toLocalDate().equals(held.get(kind))) {
                  long open = clock.toLocalDate().atTime(kind.openHour, 0).atZone(ZoneId.systemDefault()).toInstant()
                        .toEpochMilli();
                  long close = open + (kind.daily() ? TournamentRules.REGISTER_MINUTES
                        : TournamentRules.GATHER_MINUTES) * 60_000L;
                  long start = open + (kind.daily() ? TournamentRules.START_MINUTE
                        : TournamentRules.GATHER_MINUTES + TournamentRules.QUALIFY_MINUTES) * 60_000L;
                  if (now >= open && now < close) {
                     held.put(kind, clock.toLocalDate());
                     open(kind, open, close, start);
                  }
               }
            }
            for (Tournament t : new ArrayList<>(active.values())) {
               t.step(now);
               if (t.done()) {
                  active.remove(t.kind, t);
                  Log.info("Giai dau bo lac: %s ket thuc (vo dich: %s)", t.kind, t.championName);
               }
            }
         }
      } catch (Exception e) {
         Log.error("Loi nhip giai dau bo lac", e);
      }
   }

   private void open(TournamentKind kind, long open, long close, long start) {
      Tournament old = active.remove(kind);
      if (old != null) {
         old.finish(null);
      }
      active.put(kind, new Tournament(kind, open, close, start, env, random));
      Log.info("Giai dau bo lac: mo %s (%s - %s)", kind, Tournament.clock(open), Tournament.clock(start));
   }

   /** Giải đang nhận đăng ký / cho vào đấu trường (giờ không chồng nhau -> tối đa 1). */
   private Tournament entryOpen() {
      for (Tournament t : active.values()) {
         if (t.entryOpen()) {
            return t;
         }
      }
      return null;
   }

   // ================================================================== hộp thoại Tư tế Buraen

   /** Làng: "Đấu trường" > "Giải đấu Bộ lạc". Có giải đang mở mới có nút "Tham gia". */
   public void show(NpcContext ctx) {
      boolean open;
      synchronized (this) {
         open = entryOpen() != null;
      }
      if (open) {
         ctx.say(PAGE_MAIN_OPEN, INTRO, "Tham gia", "Thông tin giải đấu");
      } else {
         ctx.say(PAGE_MAIN, INTRO, "Thông tin giải đấu");
      }
   }

   /** Tư tế đứng ở đấu trường (map 22). */
   public void onTalkInside(NpcContext ctx) {
      String status;
      synchronized (this) {
         status = insideStatus(ctx.data().getId());
      }
      ctx.say(PAGE_INSIDE, "Đây là Thượng nguồn Eldarah, đấu trường của Giải đấu Bộ lạc. Rời đấu trường khi giải đang"
            + " diễn ra sẽ bị xử thua.\n" + status, "Về làng", "Bảng xếp hạng", "Đóng");
   }

   private String insideStatus(int playerId) {
      for (Tournament t : active.values()) {
         switch (t.phase) {
            case GATHER -> {
               return t.kind.title + ": vòng loại bắt đầu lúc " + Tournament.clock(t.closeAt) + ".";
            }
            case QUALIFY -> {
               return t.kind.title + ": vòng loại kết thúc lúc " + Tournament.clock(t.startAt) + ".";
            }
            case ROUND -> {
               if (t.matchOf(playerId) != null) {
                  return t.kind.title + ": đang thi đấu vòng " + t.round + ".";
               }
            }
            default -> {
            }
         }
      }
      return "";
   }

   public void onChoice(NpcContext ctx, int page, int option) {
      switch (page) {
         case PAGE_MAIN_OPEN -> {
            if (option == 0) {
               joinClicked(ctx);
            } else if (option == 1) {
               ctx.say(PAGE_INFO, INFO, "Đóng");
            }
         }
         case PAGE_MAIN -> {
            if (option == 0) {
               ctx.say(PAGE_INFO, INFO, "Đóng");
            }
         }
         case PAGE_DAILY -> {
            if (option == 0) {
               Tournament t = register(ctx.session(), ctx.data());
               if (t != null) {
                  showDaily(ctx, t);
               }
            }
         }
         case PAGE_DAILY_REGISTERED -> {
            if (option == 0) {
               cancel(ctx);
            }
         }
         case PAGE_INSIDE -> {
            if (option == 0) {
               world.leaveArena(ctx.session(), "Bạn đã rời đấu trường.");
            } else if (option == 1) {
               showRanking(ctx);
            }
         }
         default -> {
         }
      }
   }

   private void joinClicked(NpcContext ctx) {
      Tournament t;
      synchronized (this) {
         t = entryOpen();
      }
      if (t == null) {
         ctx.send(Packets.notice("Đã hết thời gian tham gia giải đấu."));
      } else if (t.kind.daily()) {
         showDaily(ctx, t);
      } else {
         join(ctx.session(), ctx.data(), t.kind);
      }
   }

   private void showDaily(NpcContext ctx, Tournament t) {
      int count;
      boolean registered;
      synchronized (this) {
         count = t.entrants.size();
         registered = t.registered(ctx.data().getId());
      }
      TournamentKind k = t.kind;
      String text = k.title + " (" + k.levelText() + ")\n"
            + "Thời gian đăng ký: " + Tournament.clock(t.openAt) + " - " + Tournament.clock(t.closeAt)
            + ", bắt đầu lúc " + Tournament.clock(t.startAt) + "\n"
            + "Lệ phí đăng ký: " + gold(k.fee) + " vàng (huỷ đăng ký không được hoàn lại)\n"
            + "Số người đã đăng ký: " + count + "/" + TournamentRules.MAX_ENTRANTS + "\n"
            + (registered ? "Bạn đã đăng ký, hãy có mặt tại Làng ánh sáng lúc giải bắt đầu." : "Bạn chưa đăng ký.");
      if (registered) {
         ctx.say(PAGE_DAILY_REGISTERED, text, "Huỷ đăng ký", "Đóng");
      } else {
         ctx.say(PAGE_DAILY, text, "Đăng ký", "Đóng");
      }
   }

   /** "Đăng ký" giải hằng ngày: trừ lệ phí (vàng) rồi ghi tên. Luồng session. @return giải đã đăng ký, null = không được */
   Tournament register(PlayerSession session, PlayerData d) {
      Tournament t;
      String error;
      synchronized (this) {
         t = entryOpen();
         error = t == null || !t.kind.daily() ? "Đã hết thời gian đăng ký giải đấu." : t.canRegister(d.getId(), d.level);
         if (error == null) {
            synchronized (d) {
               error = session.getPlayer() != d ? "" : d.trade != null ? "Đang giao dịch, không thể đăng ký."
                     : d.gold < t.kind.fee ? "Không đủ " + gold(t.kind.fee) + " vàng lệ phí đăng ký." : null;
               if (error == null) {
                  d.gold -= t.kind.fee;
               }
            }
         }
         if (error == null) {
            t.register(d.getId(), d.name, d.level, System.currentTimeMillis());
         }
      }
      if (error != null) {
         if (!error.isEmpty()) {
            session.send(Packets.notice(error));
         }
         return null;
      }
      items.syncBagAndCurrency(session);
      players.saveAsync(d, false);
      session.send(Packets.notice("Đăng ký " + t.kind.title + " thành công (-" + gold(t.kind.fee) + " vàng). Giải bắt"
            + " đầu lúc " + Tournament.clock(t.startAt) + ", hãy có mặt tại Làng ánh sáng Eldarah."));
      Log.debug("Giai dau: '%s' dang ky %s", d.name, t.kind);
      return t;
   }

   private void cancel(NpcContext ctx) {
      PlayerData d = ctx.data();
      Tournament t;
      boolean ok;
      synchronized (this) {
         t = entryOpen();
         ok = t != null && t.kind.daily() && t.cancel(d.getId());
      }
      ctx.send(Packets.notice(ok ? "Đã huỷ đăng ký " + t.kind.title + " (lệ phí không được hoàn lại)."
            : "Không thể huỷ đăng ký lúc này."));
      if (ok) {
         showDaily(ctx, t);
      }
   }

   /** "Tham gia" Giải Anh hùng / Hợp chiến: tới khu vòng loại. Luồng session. */
   void join(PlayerSession session, PlayerData d, TournamentKind kind) {
      String error = d.isDead() ? "Đã bị hạ gục, không thể tham gia giải đấu."
            : d.trade != null ? "Đang giao dịch, không thể tham gia giải đấu."
            : d.fishing != null ? "Đang câu cá, không thể tham gia giải đấu."
            : Arena.isArenaMap(d.mapId) ? "Bạn đang ở đấu trường."
            : d.mapId != World.VILLAGE_MAP_ID ? "Hãy tới Làng ánh sáng Eldarah để tham gia giải đấu."
            : kind.team() && d.party == null ? "Cần có tổ đội để tham gia " + kind.title + "."
            : null;
      if (error == null && !kind.team() && d.party != null) { // Anh hùng: thi đấu 1 mình — chỉ rời khi chắc vào được
         synchronized (this) {
            Tournament t = active.get(kind);
            error = t == null ? "Đã hết thời gian tham gia " + kind.title + "." : t.canJoin(d.getId(), d.level, 0);
         }
         if (error == null) {
            parties.forceLeave(d, "Bạn đã rời tổ đội để tham gia " + kind.title + ".");
         }
      }
      if (error == null) {
         synchronized (this) {
            Tournament t = active.get(kind);
            error = t == null ? "Đã hết thời gian tham gia " + kind.title + "."
                  : t.join(d.getId(), d.name, d.level, d.party == null ? 0 : d.party.id(), System.currentTimeMillis());
         }
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      Log.debug("Giai dau: '%s' vao vong loai %s", d.name, kind);
   }

   private void showRanking(NpcContext ctx) {
      int me = ctx.data().getId();
      List<Tournament.Row> rows = List.of();
      String title = null;
      boolean team = false;
      synchronized (this) {
         for (Tournament t : active.values()) {
            if (!t.kind.daily()) {
               rows = t.ranking();
               title = t.kind.title;
               team = t.rankingByTeam();
            }
         }
      }
      if (title == null || rows.isEmpty()) {
         ctx.say(PAGE_RANK, "Chưa có bảng xếp hạng vòng loại.", "Đóng");
         return;
      }
      // cửa sổ bảng xếp hạng op 75 (client mới — OtherRankWindow): cột "Điểm (Cấp)"; tổ đội: tên tổ đội
      List<int[]> ids = new java.util.ArrayList<>();
      List<String> names = new java.util.ArrayList<>();
      List<String> values = new java.util.ArrayList<>();
      Tournament.Row mine = null;
      for (Tournament.Row r : rows) {
         if (r.rank() <= RANK_ROWS) {
            ids.add(new int[] {r.playerId(), team ? -1 : com.tlnt.server.ranking.RankingService.genderOf(r.playerId())});
            names.add(r.name());
            values.add(r.points() + " (Cấp " + r.level() + ")");
         }
         if (!team && r.playerId() == me || team && ctx.data().party != null && r.playerId() == ctx.data().party.id()) {
            mine = r;
         }
      }
      ctx.send(com.tlnt.server.ranking.RankingService.otherRankPacket("Điểm (Cấp)", ids, names, values));
      ctx.send(Packets.notice("Vòng loại " + title + " - " + (mine == null ? (team ? "Tổ đội bạn" : "Bạn") + " chưa có hạng"
            : (team ? "Tổ đội bạn" : "Bạn") + ": hạng " + mine.rank() + ", " + mine.points() + " điểm")));
   }

   // ================================================================== móc từ khu đấu (luồng tick -> scheduler)

   @Override
   public void killed(Arena arena, WorldPlayer killer, WorldPlayer victim) {
      int killerId = killer == null ? 0 : killer.id();
      int victimId = victim.id();
      scheduler.execute(() -> {
         synchronized (this) {
            Tournament t = owner(arena);
            if (t != null) {
               t.killed(arena, killerId, victimId);
            }
         }
      });
   }

   @Override
   public void left(Arena arena, WorldPlayer p) {
      int id = p.id();
      scheduler.execute(() -> {
         synchronized (this) {
            Tournament t = owner(arena);
            if (t != null) {
               t.left(arena, id);
            }
         }
      });
   }

   /** Giải sở hữu khu đấu này (null = giải đã kết thúc). */
   private Tournament owner(Arena arena) {
      for (Tournament t : active.values()) {
         if (t.qualifier == arena || t.matches.containsKey(arena.id)) {
            return t;
         }
      }
      return null;
   }

   // ================================================================== khoá tổ đội

   private String partyLock(int playerId, boolean leaving) {
      Byte mode = locks.get(playerId);
      if (mode == null) {
         return null;
      }
      if (mode == Tournament.LOCK_ALL) {
         return "Tổ đội đang thi đấu Giải Hợp chiến, không thể thay đổi tổ đội.";
      }
      return leaving ? null : "Đang tham gia Giải Anh hùng, không thể vào tổ đội.";
   }

   // ================================================================== tác động ra ngoài

   /** Cài cho {@link Tournament}: gọi khi đang giữ khoá this, chỉ xếp việc. */
   private final class ServiceEnv implements Tournament.Env {

      @Override
      public long now() {
         return System.currentTimeMillis();
      }

      @Override
      public boolean inVillage(int playerId) {
         return world.inVillage(playerId);
      }

      @Override
      public long[] hp(int playerId) {
         WorldPlayer p = world.online(playerId);
         if (p == null || p.data().stats == null) {
            return null;
         }
         return new long[] {p.data().hp, p.data().stats.maxHp};
      }

      @Override
      public Object[] party(int playerId) {
         WorldPlayer p = world.online(playerId);
         PlayerData d = p == null ? null : p.data();
         com.tlnt.server.party.Party party = d == null ? null : d.party;
         if (party == null) {
            return null;
         }
         PartyService.Snapshot snap = parties.snapshot(d);
         WorldPlayer leader = snap == null ? null : world.online(snap.leaderId());
         return new Object[] {party.id(), "Tổ đội " + (leader != null ? leader.data().name : d.name)};
      }

      @Override
      public void enter(int playerId, Arena arena, float[] spot, boolean fromVillageOnly, Runnable onFail) {
         world.enterArena(playerId, arena, spot[0], spot[1], fromVillageOnly, () -> scheduler.execute(() -> {
            synchronized (TournamentService.this) {
               onFail.run();
            }
         }));
      }

      @Override
      public void phase(Arena arena, boolean fighting, String timerText, long start, long end) {
         world.arenaPhase(arena, fighting, timerText, start, end);
      }

      @Override
      public void closeArena(Arena arena, String notice) {
         world.closeArena(arena, notice);
      }

      @Override
      public void leave(int playerId, Arena arena, String notice) {
         world.leaveArena(playerId, arena, notice);
      }

      @Override
      public void notice(int playerId, String text) {
         WorldPlayer p = world.online(playerId);
         if (p != null) {
            p.send(Packets.notice(text));
         }
      }

      @Override
      public void ticker(int playerId, String text) {
         WorldPlayer p = world.online(playerId);
         if (p != null) {
            p.send(Packets.ticker(text));
         }
      }

      @Override
      public void tickerAll(String text) {
         world.broadcastTicker(text);
      }

      @Override
      public void reward(int playerId, String name, String content, List<ItemData> rewards, int activityPoints) {
         sendMail(playerId, name, content, rewards);
         WorldPlayer p = world.online(playerId);
         if (p != null) {
            ActivityPoints.post(p.session(), activityPoints);
         }
      }

      @Override
      public void refund(int playerId, String name, long gold, String content) {
         sendMail(playerId, name, content, List.of(ItemData.of(ItemService.GOLD_TEMPLATE_ID, (int) gold)));
      }

      @Override
      public long nextArenaId() {
         return arenaIds.incrementAndGet();
      }

      @Override
      public void lock(int playerId, byte mode) {
         locks.put(playerId, mode);
      }

      @Override
      public void unlock(int playerId) {
         locks.remove(playerId);
      }
   }

   /** Thư hệ thống (tối đa {@link MailService#MAX_ITEMS} món / thư, nhiều hơn thì chia thư). Ghi SQL trên db-writer. */
   private void sendMail(int playerId, String name, String content, List<ItemData> rewards) {
      for (int from = 0; from < rewards.size(); from += MailService.MAX_ITEMS) {
         List<ItemData> part = new ArrayList<>(rewards.subList(from, Math.min(rewards.size(), from + MailService.MAX_ITEMS)));
         Mail mail = mails.systemMail(playerId, name, MAIL_SUBJECT, content, part);
         players.runOnWriter(conn -> MailRepository.insert(conn, mail)).thenAccept(ok -> {
            if (ok) {
               mails.deliver(mail);
            } else {
               Log.warn("Giai dau: khong ghi duoc thu thuong cho #%d", playerId);
            }
         });
      }
   }

   static String gold(long amount) {
      return String.format(Locale.GERMANY, "%,d", amount);
   }

   // ================================================================== admin panel

   /** Mỗi giải đang chạy: loại, tên, giai đoạn, số người, vòng, số phe còn lại, các mốc giờ. */
   public synchronized List<Map<String, Object>> adminList() {
      List<Map<String, Object>> list = new ArrayList<>();
      for (Tournament t : active.values()) {
         Map<String, Object> row = new java.util.LinkedHashMap<>();
         row.put("kind", t.kind.name());
         row.put("title", t.kind.title);
         row.put("phase", t.phase.name());
         row.put("entrants", t.entrants.size());
         row.put("round", t.round);
         row.put("sidesLeft", t.sides.stream().filter(s -> !s.out).count());
         row.put("matches", t.matches.values().stream().filter(m -> !m.resolved).count());
         row.put("open", Tournament.clock(t.openAt));
         row.put("close", Tournament.clock(t.closeAt));
         row.put("start", Tournament.clock(t.startAt));
         list.add(row);
      }
      return list;
   }

   /** Mở ngay giải {@code kind} (tên enum), đăng ký / tập trung {@code seconds} giây. @return lỗi, null = được */
   public synchronized String adminOpen(String kind, int seconds) {
      TournamentKind k;
      try {
         k = TournamentKind.valueOf(kind);
      } catch (IllegalArgumentException e) {
         return "Không có giải " + kind + ".";
      }
      long now = System.currentTimeMillis();
      long close = now + Math.max(10, seconds) * 1000L;
      open(k, now, close, close + (k.daily() ? 30_000 : 60_000));
      return null;
   }

   /** Sang giai đoạn sau ngay (như lệnh /giaidau qua). */
   public synchronized void adminSkip(String kind) {
      for (Tournament t : active.values()) {
         if (t.kind.name().equals(kind)) {
            t.skip(System.currentTimeMillis());
         }
      }
   }

   /** Huỷ giải đang chạy (không hoàn phí, ai ở đấu trường về làng). */
   public synchronized void adminCancel(String kind) {
      for (Tournament t : new ArrayList<>(active.values())) {
         if (t.kind.name().equals(kind)) {
            t.finish(t.kind.title + " đã bị huỷ bởi quản trị viên.");
            active.remove(t.kind);
         }
      }
   }

   // ================================================================== lệnh test (chat.testCommands)

   private static volatile TournamentService instance;

   private static final String USAGE = "Lệnh: /giaidau [info] | mo nhi|chien|thu|anh|hop [giây] | dk | huy | vao | qua"
         + " | thang | chet N | ket";

   /** Lệnh chat {@code /giaidau ...} (luồng session). */
   public static void testCommand(PlayerSession session, PlayerData d, String[] args) {
      TournamentService s = instance;
      if (s != null) {
         s.test(session, d, args);
      }
   }

   private void test(PlayerSession session, PlayerData d, String[] args) {
      String sub = args.length > 1 ? args[1] : "info";
      long now = System.currentTimeMillis();
      switch (sub) {
         case "mo" -> {
            TournamentKind kind = args.length > 2 ? kindOf(args[2]) : null;
            if (kind == null) {
               session.send(Packets.notice(USAGE));
               return;
            }
            long seconds = args.length > 3 ? Math.max(10, parse(args[3], 60)) : 60;
            synchronized (this) {
               long close = now + seconds * 1000;
               open(kind, now, close, close + (kind.daily() ? 30_000 : 60_000));
            }
            session.send(Packets.notice("Đã mở " + kind.title + ": " + (kind.daily() ? "đăng ký" : "tập trung") + " "
                  + seconds + " giây. /giaidau qua = sang giai đoạn sau."));
         }
         case "dk" -> register(session, d); // = bấm "Đăng ký" (trừ lệ phí)
         case "huy" -> {
            boolean ok;
            synchronized (this) {
               Tournament t = entryOpen();
               ok = t != null && t.kind.daily() && t.cancel(d.getId());
            }
            session.send(Packets.notice(ok ? "Đã huỷ đăng ký." : "Chưa đăng ký giải nào đang mở."));
         }
         case "vao" -> {
            Tournament t;
            synchronized (this) {
               t = entryOpen();
            }
            if (t == null || t.kind.daily()) {
               session.send(Packets.notice("Không có Giải Anh hùng / Hợp chiến đang mở (/giaidau mo anh)."));
               return;
            }
            join(session, d, t.kind);
         }
         case "qua" -> {
            synchronized (this) {
               for (Tournament t : active.values()) {
                  t.skip(now);
               }
            }
            session.send(Packets.notice("Đã chuyển sang giai đoạn sau (trong 1 giây)."));
         }
         case "thang" -> {
            boolean ok = false;
            synchronized (this) {
               for (Tournament t : active.values()) {
                  ok |= t.forceWin(d.getId());
               }
            }
            session.send(Packets.notice(ok ? "Đã xử thắng trận đang đánh." : "Bạn không ở trận nào."));
         }
         case "chet" -> {
            int count = args.length > 2 ? parse(args[2], 0) : 0;
            synchronized (this) {
               for (Tournament t : active.values()) {
                  if (t.qualifier != null) {
                     t.qualifier.setDeaths(d.getId(), count);
                  }
               }
            }
            session.send(Packets.notice("Đặt số lần bị hạ gục ở vòng loại = " + count + "."));
         }
         case "ket" -> {
            synchronized (this) {
               for (Tournament t : active.values()) {
                  t.finish("Giải đấu đã bị huỷ (lệnh test).");
               }
               active.clear();
            }
            session.send(Packets.notice("Đã huỷ mọi giải đang chạy."));
         }
         default -> {
            StringBuilder sb = new StringBuilder();
            synchronized (this) {
               for (Tournament t : active.values()) {
                  sb.append(t.kind.title).append(": ").append(t.phase).append(", ").append(t.entrants.size())
                        .append(" người, vòng ").append(t.round).append(", ").append(t.sides.stream()
                              .filter(s -> !s.out).count()).append(" phe còn lại; ");
               }
            }
            session.send(Packets.notice((sb.length() == 0 ? "Không có giải nào đang chạy. " : sb.toString()) + USAGE));
         }
      }
   }

   private static TournamentKind kindOf(String text) {
      return switch (text) {
         case "nhi" -> TournamentKind.KID;
         case "chien" -> TournamentKind.WARRIOR;
         case "thu" -> TournamentKind.LEADER;
         case "anh" -> TournamentKind.HERO;
         case "hop" -> TournamentKind.TEAM;
         default -> null;
      };
   }

   private static int parse(String text, int fallback) {
      try {
         return Integer.parseInt(text.trim());
      } catch (NumberFormatException e) {
         return fallback;
      }
   }
}
