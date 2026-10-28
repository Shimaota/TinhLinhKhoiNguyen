package com.tlnt.server.tournament;

import com.tlnt.server.player.ItemData;
import com.tlnt.server.social.PkRules;
import com.tlnt.server.world.Arena;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.StringJoiner;

/**
 * 1 lần tổ chức 1 giải ({@link TournamentKind}) — máy trạng thái, CHỈ RAM. Mọi method gọi dưới khoá của
 * {@link TournamentService} (1 luồng tại 1 lúc); tác động ra ngoài (dịch chuyển, thông báo, thư) qua {@link Env}.
 *
 * <pre>
 * Hằng ngày : REGISTER (đăng ký, phút 0-25) -> WAITING (tới phút 30) -> ROUND -> BREAK (1 phút) -> ROUND ... -> DONE
 * Anh hùng /
 * Hợp chiến : GATHER (tới đấu trường, phút 0-15) -> QUALIFY (hỗn chiến, phút 15-25) -> BREAK -> ROUND -> BREAK ... -> DONE
 * </pre>
 *
 * Mỗi vòng: chia cặp ngẫu nhiên (đã báo trước đối thủ), lúc bắt đầu ai không đứng ở Làng ánh sáng bị xử thua (đối thủ có
 * mặt được thưởng vòng), lẻ cặp vào thẳng vòng sau (không thưởng). 1 trận = 1 khu riêng: chờ 10 s, đánh 1 phút 30 giây;
 * phe bị hạ hết / rời hết trước thua; hết giờ -> sát thương gây ra, rồi % HP, rồi ngẫu nhiên.
 */
final class Tournament {

   enum Phase {
      REGISTER, WAITING, GATHER, QUALIFY, BREAK, ROUND, DONE
   }

   /** Khoá tổ đội: Anh hùng = không vào tổ đội; Hợp chiến = không thay đổi gì. */
   static final byte LOCK_JOIN = 1;
   static final byte LOCK_ALL = 2;

   /** Tác động ra ngoài. Gọi khi đang giữ khoá dịch vụ: chỉ xếp việc, không chờ. */
   interface Env {
      long now();

      /** Online + đứng ở Làng ánh sáng. */
      boolean inVillage(int playerId);

      /** {HP, HP tối đa} của người đang online; null = offline. */
      long[] hp(int playerId);

      /** Tổ đội hiện tại: {id tổ đội, tên hiển thị} ; null = không có. */
      Object[] party(int playerId);

      /** Đưa vào khu đấu (hồi đầy HP / MP); không vào được -> {@code onFail} (luồng bất kỳ, dịch vụ tự xếp lại vào khoá). */
      void enter(int playerId, Arena arena, float[] spot, boolean fromVillageOnly, Runnable onFail);

      void phase(Arena arena, boolean fighting, String timerText, long start, long end);

      /** Chốt khu đấu, ai còn trong đó về làng. */
      void closeArena(Arena arena, String notice);

      /** Đưa 1 người đang ở khu đấu về làng. */
      void leave(int playerId, Arena arena, String notice);

      /** Thông báo riêng (hộp nổi). */
      void notice(int playerId, String text);

      /** Chữ chạy + dòng "Hệ thống" cho riêng người này (thông báo nội bộ của giải). */
      void ticker(int playerId, String text);

      /** Chữ chạy toàn server. */
      void tickerAll(String text);

      /** Thư thưởng + điểm hoạt động. */
      void reward(int playerId, String name, String content, List<ItemData> items, int activityPoints);

      /** Hoàn lệ phí qua thư (giải bị huỷ). */
      void refund(int playerId, String name, long gold, String content);

      long nextArenaId();

      void lock(int playerId, byte mode);

      void unlock(int playerId);
   }

   /** 1 người đăng ký (giải hằng ngày) / có mặt ở vòng loại. */
   static final class Entrant {
      final int id;
      final String name;
      int level;
      final long joinedAt;
      int points;
      /** Đang đứng trong khu vòng loại. */
      boolean inArena;
      /** Bị loại (rời đấu trường sau giờ đánh / hạ gục quá số lần). */
      boolean out;

      Entrant(int id, String name, int level, long joinedAt) {
         this.id = id;
         this.name = name;
         this.level = level;
         this.joinedAt = joinedAt;
      }
   }

   /** 1 phe ở vòng loại trực tiếp: 1 người, hoặc 1 tổ đội (Hợp chiến). */
   static final class Side {
      final int key;
      final String name;
      final List<Integer> members = new ArrayList<>();
      final Map<Integer, String> names = new HashMap<>();
      final Map<Integer, Integer> levels = new HashMap<>();
      int points;
      int deaths;
      long joinedAt;
      boolean out;

      Side(int key, String name) {
         this.key = key;
         this.name = name;
      }

      void add(int id, String memberName, int level) {
         if (!members.contains(id)) {
            members.add(id);
         }
         names.put(id, memberName);
         levels.put(id, level);
      }

      int level() {
         int best = 0;
         for (int id : members) {
            best = Math.max(best, levels.getOrDefault(id, 0));
         }
         return best;
      }
   }

   /** 1 trận đang diễn ra. */
   static final class Match {
      final Arena arena;
      final Side a;
      final Side b;
      /** Người của mỗi phe còn đứng trong trận (chưa bị hạ / chưa rời). */
      final Set<Integer> inA = new LinkedHashSet<>();
      final Set<Integer> inB = new LinkedHashSet<>();
      /** Mọi người đã vào trận (tính sát thương lúc hết giờ). */
      final List<Integer> allA = new ArrayList<>();
      final List<Integer> allB = new ArrayList<>();
      long fightAt;
      long endAt;
      boolean fightStarted;
      boolean resolved;

      Match(Arena arena, Side a, Side b) {
         this.arena = arena;
         this.a = a;
         this.b = b;
      }
   }

   /** 1 dòng bảng xếp hạng vòng loại (Tư tế ở đấu trường). */
   record Row(int rank, int playerId, String name, int level, int points) {
   }

   final TournamentKind kind;
   private final Env env;
   private final Random random;
   Phase phase;
   /** Giờ mở; hằng ngày: hết đăng ký / bắt đầu; Anh hùng / Hợp chiến: bắt đầu / hết vòng loại (ms). */
   final long openAt;
   long closeAt;
   long startAt;
   private final List<Long> reminders = new ArrayList<>();
   private int reminderIndex;

   /** Hằng ngày: người đăng ký; Anh hùng / Hợp chiến: người ở vòng loại. Theo thứ tự vào. */
   final Map<Integer, Entrant> entrants = new LinkedHashMap<>();
   /** Khu vòng loại (Anh hùng / Hợp chiến). */
   final Arena qualifier;
   /** Hợp chiến: id tổ đội -> phe (chụp lúc vòng loại bắt đầu). */
   private final Map<Integer, Side> teams = new LinkedHashMap<>();
   /** Phe còn trong giải (vòng loại trực tiếp). */
   final List<Side> sides = new ArrayList<>();
   private List<TournamentRules.Pair<Side>> pending;
   int round;
   long nextRoundAt;
   /** Mã khu đấu -> trận của vòng đang đánh. */
   final Map<Long, Match> matches = new LinkedHashMap<>();
   /** Bảng xếp hạng chốt lúc hết vòng loại (null = chưa chốt). */
   private List<Row> finalRanking;
   String championName;

   /**
    * @param closeAt hằng ngày: hết đăng ký; Anh hùng / Hợp chiến: vòng loại bắt đầu
    * @param startAt hằng ngày: vòng 1 bắt đầu; Anh hùng / Hợp chiến: hết vòng loại
    */
   Tournament(TournamentKind kind, long openAt, long closeAt, long startAt, Env env, Random random) {
      this.kind = kind;
      this.openAt = openAt;
      this.closeAt = closeAt;
      this.startAt = startAt;
      this.env = env;
      this.random = random;
      this.phase = kind.daily() ? Phase.REGISTER : Phase.GATHER;
      this.qualifier = kind.daily() ? null
            : new Arena(env.nextArenaId(), 1, true, TournamentRules.MAX_ENTRANTS + 20,
                  kind.team() ? PkRules.PARTY : PkRules.CHALLENGE, TournamentRules.QUALIFY_MAX_DEATHS,
                  TournamentRules.QUALIFY_REVIVE_MS);
      long window = Math.max(1, closeAt - openAt);
      int minutes = (int) Math.max(1, (window + 59_999) / 60_000);
      for (int m : TournamentRules.reminderMinutes(minutes)) {
         long at = openAt + m * 60_000L;
         if (at < closeAt) {
            reminders.add(at);
         }
      }
      if (qualifier != null) {
         env.phase(qualifier, false, TournamentRules.TIMER_GATHER, openAt, closeAt);
      }
   }

   // ================================================================== nhịp 1 giây

   void step(long now) {
      switch (phase) {
         case REGISTER -> {
            remind(now);
            if (now >= closeAt) {
               closeRegistration(now);
            }
         }
         case GATHER -> {
            remind(now);
            if (now >= closeAt) {
               startQualifier(now);
            }
         }
         case QUALIFY -> {
            if (now >= startAt) {
               endQualifier(now);
            }
         }
         case WAITING, BREAK -> {
            if (now >= nextRoundAt) {
               startRound(now);
            }
         }
         case ROUND -> tickMatches(now);
         default -> {
         }
      }
   }

   boolean done() {
      return phase == Phase.DONE;
   }

   /** Đang nhận đăng ký (hằng ngày) / cho vào đấu trường (Anh hùng, Hợp chiến). */
   boolean entryOpen() {
      return phase == Phase.REGISTER || phase == Phase.GATHER;
   }

   /** Thông báo nhắc toàn server (chỉ mốc gần nhất nếu lỡ nhiều mốc, vd vừa bật máy chủ). */
   private void remind(long now) {
      boolean due = false;
      while (reminderIndex < reminders.size() && reminders.get(reminderIndex) <= now) {
         due = now - reminders.get(reminderIndex) < 60_000;
         reminderIndex++;
      }
      if (due) {
         env.tickerAll(kind.daily()
               ? "Đã đến giờ điểm danh Giải đấu Bộ lạc dành cho " + kind.title + " (" + kind.levelText()
                     + "), hãy đến gặp Tư tế Buraen ở Làng ánh sáng Eldarah để điểm danh! Đóng đăng ký lúc "
                     + clock(closeAt) + "."
               : "Giải đấu Bộ lạc - " + kind.title + " (" + kind.levelText() + (kind.team() ? ", cần có tổ đội" : "")
                     + ") đã mở, hãy đến gặp Tư tế Buraen ở Làng ánh sáng Eldarah để tham gia! Vòng loại bắt đầu lúc "
                     + clock(closeAt) + ".");
      }
   }

   // ================================================================== giải hằng ngày: đăng ký

   /** Câu báo nếu không đăng ký được, null = được (chưa trừ phí). */
   String canRegister(int playerId, int level) {
      if (phase != Phase.REGISTER) {
         return "Đã hết thời gian đăng ký " + kind.title + ".";
      }
      if (entrants.containsKey(playerId)) {
         return "Bạn đã đăng ký " + kind.title + " rồi.";
      }
      if (!kind.levelOk(level)) {
         return kind.title + " chỉ dành cho chiến binh " + kind.levelText() + ".";
      }
      if (entrants.size() >= TournamentRules.MAX_ENTRANTS) {
         return kind.title + " đã đủ " + TournamentRules.MAX_ENTRANTS + " người đăng ký.";
      }
      return null;
   }

   void register(int playerId, String name, int level, long now) {
      entrants.put(playerId, new Entrant(playerId, name, level, now));
   }

   boolean registered(int playerId) {
      return entrants.containsKey(playerId);
   }

   /** Huỷ đăng ký (không hoàn phí). @return false = chưa đăng ký / đã hết giờ đăng ký */
   boolean cancel(int playerId) {
      return phase == Phase.REGISTER && entrants.remove(playerId) != null;
   }

   private void closeRegistration(long now) {
      if (entrants.size() < 2) {
         for (Entrant e : entrants.values()) {
            env.refund(e.id, e.name, kind.fee, kind.title + " không đủ người tham gia và đã bị huỷ. Lệ phí đăng ký được"
                  + " hoàn lại.");
            env.notice(e.id, kind.title + " không đủ người tham gia và đã bị huỷ, lệ phí đăng ký được hoàn lại qua"
                  + " Hòm thư.");
         }
         finish(null);
         return;
      }
      for (Entrant e : entrants.values()) {
         Side side = new Side(e.id, e.name);
         side.add(e.id, e.name, e.level);
         side.joinedAt = e.joinedAt;
         sides.add(side);
      }
      for (Entrant e : entrants.values()) {
         env.ticker(e.id, "Đã đóng đăng ký " + kind.title + ": " + entrants.size() + " chiến binh tham gia. Giải đấu"
               + " bắt đầu lúc " + clock(startAt) + ", hãy có mặt tại Làng ánh sáng Eldarah — vắng mặt sẽ bị xử thua.");
      }
      nextRoundAt = startAt;
      prepare(false);
      phase = Phase.WAITING;
   }

   // ================================================================== Anh hùng / Hợp chiến: vòng loại

   /**
    * Bấm "Tham gia" (giờ tập trung): vào khu vòng loại. {@code partyId} = tổ đội hiện tại (Hợp chiến bắt buộc). Câu báo
    * nếu không vào được, null = đang đưa tới.
    */
   String join(int playerId, String name, int level, int partyId, long now) {
      String error = canJoin(playerId, level, partyId);
      if (error != null) {
         return error;
      }
      Entrant e = entrants.get(playerId);
      if (e == null) {
         e = new Entrant(playerId, name, level, now);
         entrants.put(playerId, e);
      }
      e.level = level;
      e.inArena = true;
      if (!kind.team()) {
         env.lock(playerId, LOCK_JOIN);
      }
      Entrant entrant = e;
      env.enter(playerId, qualifier, Arena.randomSpot(), false, () -> joinFailed(entrant));
      return null;
   }

   /** Như {@link #join} nhưng chỉ kiểm tra (không vào). */
   String canJoin(int playerId, int level, int partyId) {
      if (phase != Phase.GATHER) {
         return "Đã hết thời gian tham gia " + kind.title + ".";
      }
      if (!kind.levelOk(level)) {
         return kind.title + " chỉ dành cho chiến binh " + kind.levelText() + ".";
      }
      if (kind.team() && partyId == 0) {
         return "Cần có tổ đội để tham gia " + kind.title + ".";
      }
      Entrant e = entrants.get(playerId);
      if (e != null && e.inArena) {
         return "Bạn đang ở đấu trường.";
      }
      long inside = entrants.values().stream().filter(x -> x.inArena).count();
      if (inside >= TournamentRules.MAX_ENTRANTS) {
         return "Đấu trường đã đủ " + TournamentRules.MAX_ENTRANTS + " người.";
      }
      return null;
   }

   /** Bảng xếp hạng đang tính theo tổ đội (Hợp chiến từ lúc vòng loại bắt đầu). */
   boolean rankingByTeam() {
      return kind.team() && (finalRanking != null || !teams.isEmpty());
   }

   private void joinFailed(Entrant e) {
      if (phase == Phase.GATHER && e.inArena) {
         e.inArena = false;
         entrants.remove(e.id);
         env.unlock(e.id);
         env.notice(e.id, "Không thể tới đấu trường lúc này.");
      }
   }

   private void startQualifier(long now) {
      entrants.values().removeIf(e -> {
         if (!e.inArena) {
            env.unlock(e.id);
         }
         return !e.inArena;
      });
      phase = Phase.QUALIFY;
      if (entrants.isEmpty()) {
         finish(null);
         return;
      }
      if (kind.team()) {
         for (Entrant e : entrants.values()) {
            Object[] party = env.party(e.id);
            if (party == null) {
               e.out = true;
               env.leave(e.id, qualifier, "Bạn không ở trong tổ đội nào nên không thể thi đấu " + kind.title + ".");
               continue;
            }
            int partyId = (Integer) party[0];
            Side side = teams.computeIfAbsent(partyId, k -> new Side(k, (String) party[1]));
            side.add(e.id, e.name, e.level);
            side.joinedAt = side.joinedAt == 0 ? e.joinedAt : Math.min(side.joinedAt, e.joinedAt);
            qualifier.setTeam(e.id, partyId);
            env.lock(e.id, LOCK_ALL);
         }
      }
      env.phase(qualifier, true, TournamentRules.TIMER_QUALIFY, now, startAt);
      for (Entrant e : entrants.values()) {
         if (!e.out) {
            env.ticker(e.id, "Vòng loại " + kind.title + " bắt đầu! Hạ gục người chơi khác được 1 điểm, bị hạ gục quá "
                  + TournamentRules.QUALIFY_MAX_DEATHS + " lần sẽ bị loại. Kết thúc lúc " + clock(startAt) + ", "
                  + TournamentRules.TOP + (kind.team() ? " tổ đội" : " người") + " điểm cao nhất vào vòng trong.");
         }
      }
   }

   private void endQualifier(long now) {
      phase = Phase.BREAK; // từ đây rời khu vòng loại không còn tính là bị loại
      List<Side> ranked = new ArrayList<>();
      if (kind.team()) {
         for (Side team : teams.values()) {
            int points = 0;
            int deaths = 0;
            List<Integer> alive = new ArrayList<>();
            for (int id : team.members) {
               Entrant e = entrants.get(id);
               points += e.points;
               deaths += qualifier.deathsOf(id);
               if (!e.out && e.inArena) {
                  alive.add(id);
               }
            }
            team.points = points;
            team.deaths = deaths;
            team.members.retainAll(alive);
            if (!team.members.isEmpty()) {
               ranked.add(team);
            }
         }
      } else {
         for (Entrant e : entrants.values()) {
            if (!e.out && e.inArena) {
               Side side = new Side(e.id, e.name);
               side.add(e.id, e.name, e.level);
               side.points = e.points;
               side.deaths = qualifier.deathsOf(e.id);
               side.joinedAt = e.joinedAt;
               ranked.add(side);
            }
         }
      }
      ranked.sort(RANK_ORDER);
      finalRanking = rows(ranked);
      List<Side> top = new ArrayList<>(ranked.subList(0, Math.min(TournamentRules.TOP, ranked.size())));
      env.closeArena(qualifier, "Vòng loại " + kind.title + " đã kết thúc.");
      for (Entrant e : entrants.values()) {
         Side mine = sideOf(top, e.id);
         if (mine == null) {
            env.unlock(e.id);
            if (!e.out) {
               env.ticker(e.id, "Vòng loại " + kind.title + " đã kết thúc, " + (kind.team() ? "tổ đội của bạn" : "bạn")
                     + " không lọt vào vòng trong.");
            }
         }
      }
      if (top.size() < 2) {
         for (Side s : top) {
            for (int id : s.members) {
               env.ticker(id, "Không đủ " + (kind.team() ? "tổ đội" : "người") + " vào vòng trong, " + kind.title
                     + " kết thúc.");
            }
         }
         finish(null);
         return;
      }
      sides.addAll(top);
      for (int i = 0; i < top.size(); i++) {
         for (int id : top.get(i).members) {
            env.ticker(id, "Chúc mừng " + (kind.team() ? "tổ đội của bạn" : "bạn") + " đã lọt vào vòng trong "
                  + kind.title + " (hạng " + (i + 1) + ", " + top.get(i).points + " điểm)!");
         }
      }
      nextRoundAt = now + TournamentRules.BREAK_MS;
      prepare(true);
   }

   /** Điểm cao trước, bị hạ gục ít trước, vào sớm trước. */
   static final Comparator<Side> RANK_ORDER = Comparator.<Side>comparingInt(s -> -s.points)
         .thenComparingInt(s -> s.deaths).thenComparingLong(s -> s.joinedAt);

   private static Side sideOf(List<Side> list, int playerId) {
      for (Side s : list) {
         if (s.members.contains(playerId)) {
            return s;
         }
      }
      return null;
   }

   private static List<Row> rows(List<Side> ranked) {
      List<Row> list = new ArrayList<>();
      for (int i = 0; i < ranked.size(); i++) {
         Side s = ranked.get(i);
         list.add(new Row(i + 1, s.key, s.name, s.level(), s.points));
      }
      return list;
   }

   /** Bảng xếp hạng vòng loại hiện tại (đang diễn ra: tính lại; đã chốt: bản chốt). Rỗng = giải hằng ngày / chưa có ai. */
   List<Row> ranking() {
      if (finalRanking != null) {
         return finalRanking;
      }
      if (kind.daily()) {
         return List.of();
      }
      List<Side> live = new ArrayList<>();
      if (kind.team() && !teams.isEmpty()) {
         for (Side team : teams.values()) {
            Side copy = new Side(team.key, team.name);
            for (int id : team.members) {
               Entrant e = entrants.get(id);
               copy.add(id, e.name, e.level);
               copy.points += e.points;
               copy.deaths += qualifier.deathsOf(id);
            }
            copy.joinedAt = team.joinedAt;
            live.add(copy);
         }
      } else {
         for (Entrant e : entrants.values()) {
            if (!e.out && e.inArena) {
               Side s = new Side(e.id, e.name);
               s.add(e.id, e.name, e.level);
               s.points = e.points;
               s.deaths = qualifier.deathsOf(e.id);
               s.joinedAt = e.joinedAt;
               live.add(s);
            }
         }
      }
      live.sort(RANK_ORDER);
      return rows(live);
   }

   // ================================================================== vòng loại trực tiếp

   /** Chia cặp vòng kế tiếp + báo đối thủ / giờ bắt đầu cho người còn trong giải. */
   private void prepare(boolean seeded) {
      List<Side> alive = alive();
      pending = TournamentRules.pair(alive, seeded, random);
      phase = phase == Phase.REGISTER ? Phase.WAITING : Phase.BREAK;
      int next = round + 1;
      for (TournamentRules.Pair<Side> p : pending) {
         if (p.bye()) {
            tell(p.a(), "Vòng " + next + " " + kind.title + ": " + subject(p.a()) + " không có đối thủ, sẽ được vào thẳng"
                  + " vòng sau.");
         } else {
            String at = " Trận đấu bắt đầu lúc " + clock(nextRoundAt) + ", hãy có mặt tại Làng ánh sáng Eldarah.";
            tell(p.a(), "Vòng " + next + " " + kind.title + ": đối thủ của " + subject(p.a()) + " là " + p.b().name + "."
                  + at);
            tell(p.b(), "Vòng " + next + " " + kind.title + ": đối thủ của " + subject(p.b()) + " là " + p.a().name + "."
                  + at);
         }
      }
   }

   private String subject(Side side) {
      return kind.team() ? "tổ đội bạn" : "bạn";
   }

   private List<Side> alive() {
      List<Side> list = new ArrayList<>();
      for (Side s : sides) {
         if (!s.out) {
            list.add(s);
         }
      }
      return list;
   }

   private void startRound(long now) {
      round++;
      matches.clear();
      phase = Phase.ROUND;
      int zoneNo = 1;
      List<TournamentRules.Pair<Side>> pairs = pending == null ? List.of() : pending;
      pending = null;
      for (TournamentRules.Pair<Side> p : pairs) {
         if (p.a().out || !p.bye() && p.b().out) { // bị loại giữa lúc chờ (không xảy ra với luật hiện tại)
            continue;
         }
         if (p.bye()) {
            tell(p.a(), "Vòng " + round + ": " + subject(p.a()) + " được vào thẳng vòng sau.");
            continue;
         }
         List<Integer> presentA = present(p.a());
         List<Integer> presentB = present(p.b());
         if (presentA.isEmpty() && presentB.isEmpty()) {
            eliminate(p.a(), "Không có mặt tại Làng ánh sáng khi vòng " + round + " bắt đầu, bạn bị xử thua.");
            eliminate(p.b(), "Không có mặt tại Làng ánh sáng khi vòng " + round + " bắt đầu, bạn bị xử thua.");
         } else if (presentA.isEmpty()) {
            walkover(p.b(), p.a());
         } else if (presentB.isEmpty()) {
            walkover(p.a(), p.b());
         } else {
            createMatch(p.a(), presentA, p.b(), presentB, zoneNo++, now);
         }
      }
      if (unresolved() == 0) {
         afterRound(now);
      }
   }

   /** Thành viên đang đứng ở Làng; Hợp chiến: người vắng mặt bị loại khỏi tổ đội. */
   private List<Integer> present(Side side) {
      List<Integer> list = new ArrayList<>();
      for (int id : side.members) {
         if (env.inVillage(id)) {
            list.add(id);
         }
      }
      if (kind.team() && !list.isEmpty()) {
         for (int id : new ArrayList<>(side.members)) {
            if (!list.contains(id)) {
               dropMember(side, id, "Bạn không có mặt tại Làng ánh sáng khi vòng " + round
                     + " bắt đầu nên bị loại khỏi giải đấu.");
            }
         }
      }
      return list;
   }

   private void walkover(Side winner, Side loser) {
      eliminate(loser, "Không có mặt tại Làng ánh sáng khi vòng " + round + " bắt đầu, " + subject(loser)
            + " bị xử thua.");
      winRound(winner, "Đối thủ vắng mặt, " + subject(winner) + " được xử thắng vòng " + round + ".");
   }

   private void createMatch(Side a, List<Integer> presentA, Side b, List<Integer> presentB, int zoneNo, long now) {
      Arena arena = new Arena(env.nextArenaId(), zoneNo, false, presentA.size() + presentB.size() + 4,
            kind.team() ? PkRules.PARTY : PkRules.CHALLENGE, 0, 0);
      Match m = new Match(arena, a, b);
      m.fightAt = now + TournamentRules.WAIT_MS;
      m.endAt = m.fightAt + TournamentRules.FIGHT_MS;
      matches.put(arena.id, m);
      place(m, a, presentA, true);
      place(m, b, presentB, false);
      env.phase(arena, false, TournamentRules.TIMER_WAIT, now, m.fightAt);
      for (int id : presentA) {
         env.ticker(id, "Vòng " + round + " " + kind.title + " bắt đầu! Đối thủ: " + b.name + ". Trận đấu bắt đầu sau "
               + TournamentRules.WAIT_MS / 1000 + " giây.");
      }
      for (int id : presentB) {
         env.ticker(id, "Vòng " + round + " " + kind.title + " bắt đầu! Đối thủ: " + a.name + ". Trận đấu bắt đầu sau "
               + TournamentRules.WAIT_MS / 1000 + " giây.");
      }
      for (int id : presentA) {
         enter(m, id);
      }
      for (int id : presentB) {
         enter(m, id);
      }
   }

   private void place(Match m, Side side, List<Integer> present, boolean left) {
      for (int i = 0; i < present.size(); i++) {
         int id = present.get(i);
         float[] spot = Arena.matchSpot(left, i);
         m.arena.setSpot(id, spot[0], spot[1]);
         if (kind.team()) {
            m.arena.setTeam(id, side.key);
         }
         (left ? m.inA : m.inB).add(id);
         (left ? m.allA : m.allB).add(id);
      }
   }

   private void enter(Match m, int id) {
      env.enter(id, m.arena, m.arena.spotOf(id), true, () -> memberGone(m, id, true));
   }

   private void tickMatches(long now) {
      for (Match m : new ArrayList<>(matches.values())) {
         if (m.resolved) {
            continue;
         }
         if (!m.fightStarted && now >= m.fightAt) {
            m.fightStarted = true;
            env.phase(m.arena, true, TournamentRules.TIMER_FIGHT, m.fightAt, m.endAt);
         }
         if (now >= m.endAt) {
            resolveOnTime(m);
         }
      }
   }

   private int unresolved() {
      int n = 0;
      for (Match m : matches.values()) {
         if (!m.resolved) {
            n++;
         }
      }
      return n;
   }

   /** {@code id} không còn đứng trong trận: bị hạ gục ({@code quit} false) / rời trận / không vào được. */
   private void memberGone(Match m, int id, boolean quit) {
      if (m.resolved) {
         return;
      }
      m.inA.remove(id);
      m.inB.remove(id);
      if (quit && kind.team()) { // Hợp chiến: rời trận / thoát game = thành viên đó bị loại
         Side side = m.allA.contains(id) ? m.a : m.b;
         dropMember(side, id, "Bạn đã rời trận đấu nên bị loại khỏi " + kind.title + ".");
      }
      if (m.inA.isEmpty() && m.inB.isEmpty()) {
         resolveOnTime(m); // 2 phe cùng hết người (cùng rời): như hết giờ
      } else if (m.inA.isEmpty()) {
         resolve(m, m.b, m.a);
      } else if (m.inB.isEmpty()) {
         resolve(m, m.a, m.b);
      }
   }

   private void resolveOnTime(Match m) {
      long damageA = 0;
      long damageB = 0;
      for (int id : m.allA) {
         damageA += m.arena.damageOf(id);
      }
      for (int id : m.allB) {
         damageB += m.arena.damageOf(id);
      }
      boolean aWins = TournamentRules.aWinsOnTime(damageA, damageB, hpPermille(m.inA), hpPermille(m.inB), random);
      resolve(m, aWins ? m.a : m.b, aWins ? m.b : m.a);
   }

   private int hpPermille(Set<Integer> ids) {
      long hp = 0;
      long max = 0;
      for (int id : ids) {
         long[] v = env.hp(id);
         if (v != null) {
            hp += Math.max(0, v[0]);
            max += Math.max(1, v[1]);
         }
      }
      return TournamentRules.hpPermille(hp, max);
   }

   private void resolve(Match m, Side winner, Side loser) {
      m.resolved = true;
      eliminate(loser, subject(loser).substring(0, 1).toUpperCase() + subject(loser).substring(1)
            + " đã thua " + winner.name + " ở vòng " + round + " và bị loại khỏi " + kind.title + ".");
      winRound(winner, "Chúc mừng " + subject(winner) + " đã chiến thắng vòng " + round + "!");
      env.closeArena(m.arena, null);
      if (phase == Phase.ROUND && unresolved() == 0) {
         afterRound(env.now());
      }
   }

   /** Thắng 1 vòng: thư thưởng vòng + điểm hoạt động cho mọi thành viên còn trong phe. */
   private void winRound(Side side, String text) {
      for (int id : side.members) {
         env.notice(id, text + " Phần thưởng đã được gửi vào Hòm thư.");
         env.reward(id, side.names.getOrDefault(id, ""), "Phần thưởng chiến thắng vòng " + round + " " + kind.title
               + ".", TournamentRules.roundReward(random), TournamentRules.ROUND_ACTIVITY);
      }
   }

   private void eliminate(Side side, String text) {
      side.out = true;
      for (int id : side.members) {
         env.notice(id, text);
         env.unlock(id);
      }
   }

   private void dropMember(Side side, int id, String text) {
      if (side.members.remove((Integer) id)) {
         env.notice(id, text);
         env.unlock(id);
      }
   }

   private void afterRound(long now) {
      if (phase == Phase.DONE) {
         return;
      }
      List<Side> alive = alive();
      if (alive.size() == 1) {
         champion(alive.get(0));
         return;
      }
      if (alive.isEmpty()) {
         finish(null);
         return;
      }
      nextRoundAt = now + TournamentRules.BREAK_MS;
      prepare(false);
   }

   private void champion(Side side) {
      championName = side.name;
      long now = env.now();
      for (int id : side.members) {
         env.notice(id, "Chúc mừng " + subject(side) + " đã vô địch " + kind.title + "! Phần thưởng vô địch đã được gửi"
               + " vào Hòm thư.");
         env.reward(id, side.names.getOrDefault(id, ""), "Phần thưởng vô địch " + kind.title + ".",
               TournamentRules.championReward(kind, random, now), TournamentRules.CHAMPION_ACTIVITY);
      }
      if (kind.team()) {
         StringJoiner names = new StringJoiner(", ");
         for (int id : side.members) {
            names.add(side.names.getOrDefault(id, ""));
         }
         env.tickerAll("Chúc mừng " + side.name + " (" + names + ") đã vô địch " + kind.title + "!");
      } else {
         env.tickerAll("Chúc mừng " + side.name + " đã vô địch " + kind.title + "!");
      }
      finish(null);
   }

   /** Kết thúc giải: gỡ mọi khoá tổ đội, đóng khu vòng loại (nếu còn). */
   void finish(String text) {
      if (phase == Phase.DONE) {
         return;
      }
      phase = Phase.DONE;
      Set<Integer> ids = new LinkedHashSet<>(entrants.keySet());
      for (Side s : sides) {
         ids.addAll(s.members);
      }
      for (int id : ids) {
         env.unlock(id);
         if (text != null) {
            env.ticker(id, text);
         }
      }
      if (qualifier != null && !qualifier.closed()) {
         env.closeArena(qualifier, null);
      }
      for (Match m : matches.values()) {
         if (!m.resolved) {
            m.resolved = true;
            env.closeArena(m.arena, null);
         }
      }
   }

   // ================================================================== sự kiện từ khu đấu

   /** {@code victimId} bị hạ gục ở khu đấu {@code arena} ({@code killerId} 0 = rơi đài không do ai). */
   void killed(Arena arena, int killerId, int victimId) {
      if (arena == qualifier) {
         if (phase == Phase.QUALIFY && killerId != 0) {
            Entrant killer = entrants.get(killerId);
            if (killer != null && !killer.out && killer.inArena) {
               killer.points++;
            }
         }
         return;
      }
      Match m = matches.get(arena.id);
      if (m != null && phase == Phase.ROUND) {
         memberGone(m, victimId, false);
      }
   }

   /** {@code playerId} rời khu của khu đấu {@code arena}. */
   void left(Arena arena, int playerId) {
      if (arena == qualifier) {
         Entrant e = entrants.get(playerId);
         if (e == null || !e.inArena) {
            return;
         }
         e.inArena = false;
         if (phase == Phase.GATHER) { // chưa tới giờ đánh: rời đi rồi quay lại được
            entrants.remove(playerId);
            env.unlock(playerId);
         } else if (phase == Phase.QUALIFY) {
            boolean wasOut = e.out;
            e.out = true;
            env.unlock(playerId);
            if (!wasOut && qualifier.deathsOf(playerId) <= TournamentRules.QUALIFY_MAX_DEATHS) {
               env.notice(playerId, "Bạn đã rời đấu trường và bị loại khỏi " + kind.title + ".");
            }
         }
         return;
      }
      Match m = matches.get(arena.id);
      if (m != null && phase == Phase.ROUND) {
         memberGone(m, playerId, true);
      }
   }

   // ================================================================== lệnh test

   /** Đẩy mốc thời gian của giai đoạn đang chờ về {@code now} (tick sau chuyển giai đoạn). */
   void skip(long now) {
      switch (phase) {
         case REGISTER, GATHER -> closeAt = now;
         case QUALIFY -> startAt = now;
         case WAITING, BREAK -> nextRoundAt = now;
         case ROUND -> {
            for (Match m : matches.values()) {
               if (!m.resolved) {
                  m.fightAt = Math.min(m.fightAt, now);
                  m.endAt = now;
               }
            }
         }
         default -> {
         }
      }
   }

   /** Trận đang đánh có {@code playerId}; null = không có. */
   Match matchOf(int playerId) {
      for (Match m : matches.values()) {
         if (!m.resolved && (m.allA.contains(playerId) || m.allB.contains(playerId))) {
            return m;
         }
      }
      return null;
   }

   /** Lệnh test: phe của {@code playerId} thắng ngay trận đang đánh. */
   boolean forceWin(int playerId) {
      Match m = matchOf(playerId);
      if (m == null) {
         return false;
      }
      boolean a = m.allA.contains(playerId);
      resolve(m, a ? m.a : m.b, a ? m.b : m.a);
      return true;
   }

   /** Còn trong giải (đã đăng ký / ở vòng loại / phe chưa bị loại). */
   boolean involves(int playerId) {
      if (phase == Phase.DONE) {
         return false;
      }
      if (!sides.isEmpty()) {
         Side s = sideOf(sides, playerId);
         return s != null && !s.out;
      }
      Entrant e = entrants.get(playerId);
      return e != null && !e.out;
   }

   static String clock(long epochMs) {
      return LocalTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault()).format(CLOCK);
   }

   private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

   private void tell(Side side, String text) {
      for (int id : side.members) {
         env.ticker(id, text);
      }
   }
}
