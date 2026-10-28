package com.tlnt.server.linhmoc;

import com.tlnt.server.mail.Mail;
import com.tlnt.server.mail.MailRepository;
import com.tlnt.server.mail.MailService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.npc.NpcAction;
import com.tlnt.server.npc.NpcContext;
import com.tlnt.server.party.PartyService;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;
import com.tlnt.server.world.WorldPlayer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Phó bản tổ đội "Rừng Linh Mộc" + NPC Bác Rano (luật / công thức: {@link LinhMocRules}, cấu hình: {@link LinhMocConfig}).
 *
 * <p>Bác Rano (Rừng cổ mộc): [Rừng Linh Mộc, Đóng] -> lượt còn hôm nay + [Tham gia, Đóng]. "Tham gia" kiểm tra theo thứ
 * tự: (1) đang trong {@code memberIds} của 1 lượt đang chạy -> vào lại map đầu, không trừ lượt; (2) không có tổ đội;
 * (3) không phải trưởng nhóm; (4) có thành viên không ở Rừng cổ mộc; (5) thành viên đang ở lượt khác / hết lượt;
 * (6) hợp lệ -> mở lượt: chụp thành viên, trừ lượt ngày từng người, khoá cấp quái = min(cấp cao nhất, maxLevel), khu
 * riêng của lượt ở mọi map, cả nhóm vào map đầu, đếm ngược.
 *
 * <p>Trong lượt: quyền vào theo {@code memberIds} (không theo tổ đội), map N+1 mở khi khu map N dọn xong (World chặn
 * cửa), quái không hồi sinh. Thành công = mọi điều kiện trong {@link #addCompletionCondition} (mặc định: diệt sạch mọi
 * map) -> báo, {@code returnSeconds} sau đưa về cạnh Bác Rano. Hết giờ -> thất bại, đưa về ngay. Tổ đội giải tán / mọi
 * người thoát ra không kết thúc lượt. Lượt chỉ ở RAM ({@link #byMember}): tắt máy chủ là mất (lượt đã trừ vẫn tính).
 *
 * <p>Luồng: NPC / lệnh test trên luồng session; móc từ luồng tick -> pool game; hẹn giờ scheduler -> pool game. Mở lượt
 * dưới {@link #lock} (hiếm, ngắn) — trong khoá chỉ đọc RAM + ghi 2 trường lượt ngày dưới khoá từng nhân vật.
 */
public final class LinhMocService implements NpcAction, World.LinhMocHooks {

   private static final ZoneId ZONE = ZoneId.systemDefault();
   private static final int PAGE_MAIN = 0;
   private static final int PAGE_DUNGEON = 1;

   private final World world;
   private final PlayerService players;
   private final PartyService parties;
   private final MailService mails;
   private final ScheduledExecutorService scheduler;
   private final Executor gamePool;
   private final Object lock = new Object();
   /** Thành viên -> lượt gần nhất của họ (gỡ khi lượt đóng). */
   private final Map<Integer, LinhMocRun> byMember = new ConcurrentHashMap<>();
   /** Số khu đang dùng (hiển thị). Dưới {@link #lock}. */
   private final BitSet zoneNos = new BitSet();
   private final AtomicLong nextRunId = new AtomicLong(1);
   /** Điều kiện hoàn thành lượt — TẤT CẢ phải đúng. Boss sau này: thêm điều kiện + gọi {@link #progress}. */
   private final List<Predicate<LinhMocRun>> completion = new CopyOnWriteArrayList<>();

   public LinhMocService(World world, PlayerService players, PartyService parties, MailService mails,
         ScheduledExecutorService scheduler, Executor gamePool) {
      this.world = world;
      this.players = players;
      this.parties = parties;
      this.mails = mails;
      this.scheduler = scheduler;
      this.gamePool = gamePool;
      completion.add(run -> run.allCleared(LinhMocRules.config().maps())); // dọn sạch mọi map (gồm 2 boss)
      instance = this;
   }

   /** Thêm điều kiện hoàn thành (vd boss đã chết). Gọi lúc khởi động. */
   public void addCompletionCondition(Predicate<LinhMocRun> condition) {
      completion.add(condition);
   }

   // ================================================================== hộp thoại Bác Rano

   @Override
   public void onTalk(NpcContext ctx) {
      LinhMocConfig c = LinhMocRules.config();
      ctx.say(PAGE_MAIN, c.text("talk1"), c.text("button.dungeon"), c.text("button.close"));
   }

   @Override
   public void onChoice(NpcContext ctx, int page, int option) {
      LinhMocConfig c = LinhMocRules.config();
      if (page == PAGE_MAIN && option == 0) {
         String turns = String.valueOf(LinhMocRules.turnsLeft(ctx.data(), today()));
         ctx.say(PAGE_DUNGEON, c.text("talk2").replace("{turns}", turns), c.text("button.join"), c.text("button.close"));
      } else if (page == PAGE_DUNGEON && option == 0) {
         join(ctx.session(), ctx.data());
      }
   }

   // ================================================================== "Tham gia"

   /** Nút "Tham gia" (luồng session của người bấm). */
   void join(PlayerSession session, PlayerData d) {
      LinhMocConfig c = LinhMocRules.config();
      LinhMocRun current = activeRun(d.getId());
      if (current != null) { // (1) lượt đang chạy: vào lại map đầu, không trừ lượt
         world.enterLinhMoc(session, current);
         return;
      }
      PartyService.Snapshot party = parties.snapshot(d);
      if (party == null) { // (2) — tổ đội 1 người vẫn hợp lệ
         session.send(Packets.notice(c.text("noParty")));
         return;
      }
      if (party.leaderId() != d.getId()) { // (3)
         session.send(Packets.notice(c.text("notLeader")));
         return;
      }
      long now = System.currentTimeMillis();
      long today = today();
      List<WorldPlayer> members = new ArrayList<>(party.memberIds().size());
      LinhMocRun run;
      synchronized (lock) {
         if (zoneNos.cardinality() >= c.maxRuns()) { // đủ số lượt chạy cùng lúc (cả server): chưa trừ lượt của ai
            session.send(Packets.notice(c.text("busy")));
            return;
         }
         for (int id : party.memberIds()) { // (4)
            WorldPlayer m = world.online(id);
            if (m == null || !m.data().inWorld || m.data().mapId != c.hubMapId()) {
               session.send(Packets.notice(c.text("notHere")));
               return;
            }
            members.add(m);
         }
         // (5) báo TẤT CẢ người không đi được (đang ở lượt khác / hết lượt hôm nay) để trưởng nhóm kick 1 lần
         List<String> busy = new ArrayList<>();
         List<String> noTurns = new ArrayList<>();
         for (WorldPlayer m : members) {
            PlayerData md = m.data();
            if (activeRun(md.getId()) != null) {
               busy.add(md.name);
            } else if (LinhMocRules.turnsLeft(md, today) <= 0) {
               noTurns.add(md.name);
            }
         }
         if (!busy.isEmpty()) {
            session.send(Packets.notice(c.text("otherRun").replace("{name}", String.join(", ", busy))));
            return;
         }
         if (!noTurns.isEmpty()) {
            session.send(Packets.notice(c.text("noTurns").replace("{name}", String.join(", ", noTurns))));
            return;
         }
         // (6) mở lượt
         int highest = 1;
         for (WorldPlayer m : members) {
            highest = Math.max(highest, m.data().level);
         }
         int mobLevel = LinhMocRules.mobLevel(highest);
         int zoneNo = zoneNos.nextClearBit(0);
         zoneNos.set(zoneNo);
         Map<Integer, String> names = new java.util.HashMap<>();
         for (WorldPlayer m : members) {
            names.put(m.id(), m.data().name);
         }
         run = new LinhMocRun(nextRunId.getAndIncrement(), Set.copyOf(party.memberIds()), names, mobLevel,
               LinhMocRules.unit(mobLevel), now, now + c.runMs(), zoneNo);
         for (WorldPlayer m : members) {
            PlayerData md = m.data();
            synchronized (md) {
               md.linhMocDay = (int) today; // hôm nay đã đi
            }
            byMember.put(md.getId(), run);
         }
      }
      for (WorldPlayer m : members) {
         players.saveAsync(m.data(), false); // lượt đã trừ phải còn kể cả khi server sập
         addPoints(m.id()); // +điểm hoạt động lượt mới (vào lại lượt đang chạy thì không)
         com.tlnt.server.tribe.TribeService.reward(m.id(), com.tlnt.server.tribe.TribeRules.config().pointsLinhMoc(),
               "tham gia Rừng Linh Mộc"); // +điểm cống hiến bộ lạc
      }
      scheduleEnd(run);
      var started = Packets.notice(c.text("started").replace("{minutes}", String.valueOf(c.runMinutes()))).toPacket();
      for (WorldPlayer m : members) {
         world.enterLinhMoc(m.session(), run);
         m.session().send(started);
      }
      Log.debug("Rung Linh Moc: '%s' mo luot %d, %d nguoi, cap quai %d (unit %.0f)", d.name, run.id,
            members.size(), run.mobLevel, run.unit);
   }

   // ================================================================== móc từ World

   @Override
   public LinhMocRun activeRun(int playerId) {
      LinhMocRun run = byMember.get(playerId);
      return run != null && run.active(System.currentTimeMillis()) ? run : null;
   }

   @Override
   public void cleared(LinhMocRun run, int mapId) {
      gamePool.execute(() -> onCleared(run, mapId));
   }

   private void onCleared(LinhMocRun run, int mapId) {
      if (!run.active(System.currentTimeMillis()) || !run.markCleared(mapId)) {
         return;
      }
      if (progress(run)) {
         return;
      }
      List<Integer> maps = LinhMocRules.config().maps();
      int stage = maps.indexOf(mapId);
      if (stage >= 0 && stage + 1 < maps.size()) {
         var next = world.mapTemplate(maps.get(stage + 1));
         String text = LinhMocRules.config().text("cleared").replace("{map}", next == null ? "" : next.name());
         notifyInDungeon(run, text);
      }
   }

   /**
    * Xét hoàn thành: mọi điều kiện đúng -> thành công. Gọi sau mỗi tiến triển (map dọn xong; sau này boss chết).
    * @return true = lượt vừa thành công
    */
   public boolean progress(LinhMocRun run) {
      for (Predicate<LinhMocRun> condition : completion) {
         if (!condition.test(run)) {
            return false;
         }
      }
      succeed(run);
      return true;
   }

   // ================================================================== kết thúc

   private void succeed(LinhMocRun run) {
      if (!run.finish(LinhMocRun.State.SUCCESS)) {
         return;
      }
      LinhMocConfig c = LinhMocRules.config();
      // Đồng hồ cạnh nút Thoát chuyển sang đếm ngược tới lúc về (hẹn hết giờ cũ thấy lượt đã xong thì thôi)
      run.end = System.currentTimeMillis() + c.returnSeconds() * 1000L;
      for (int id : run.memberIds) {
         WorldPlayer m = world.online(id);
         if (m != null && LinhMocRules.isDungeonMap(m.data().mapId)) {
            world.refreshLinhMocTimer(m.session());
         }
      }
      notifyInDungeon(run, c.text("success"));
      Log.debug("Rung Linh Moc: luot %d thanh cong", run.id);
      scheduler.schedule(() -> gamePool.execute(() -> close(run, c.text("returned"))), c.returnSeconds(),
            TimeUnit.SECONDS);
   }

   private void fail(LinhMocRun run, String notice) {
      if (!run.finish(LinhMocRun.State.FAILED)) {
         return;
      }
      Log.debug("Rung Linh Moc: luot %d that bai (%d map da don)", run.id, run.clearedCount());
      close(run, notice);
   }

   /** Hẹn hết giờ. Hẹn cũ (giờ kết thúc bị đổi bằng lệnh test) thấy chưa tới giờ thì thôi — hẹn mới lo. */
   private void scheduleEnd(LinhMocRun run) {
      long delay = run.end - System.currentTimeMillis() + 100;
      scheduler.schedule(() -> gamePool.execute(() -> {
         if (System.currentTimeMillis() >= run.end) {
            fail(run, LinhMocRules.config().text("timeout"));
         }
      }), Math.max(0, delay), TimeUnit.MILLISECONDS);
   }

   /** Đóng lượt: người còn trong phó bản về cạnh Bác Rano (kèm {@code notice}), người ở ngoài chỉ nhận báo; huỷ khu. */
   private void close(LinhMocRun run, String notice) {
      for (int id : run.memberIds) {
         byMember.remove(id, run);
         WorldPlayer m = world.online(id);
         if (m == null) {
            continue;
         }
         if (LinhMocRules.isDungeonMap(m.data().mapId)) {
            world.leaveLinhMoc(m.session(), notice, run);
         } else if (notice != null && run.state() == LinhMocRun.State.FAILED) {
            m.session().send(Packets.notice(notice));
         }
      }
      world.endLinhMocRun(run);
      synchronized (lock) {
         zoneNos.clear(run.zoneNo);
      }
      sendFinishMail(run);
   }

   /**
    * Quà kết thúc lượt (thành công / hết giờ), MỖI thành viên như nhau, qua THƯ (kể cả người đã thoát game): Quả Linh Mộc
    * khoá = floor(cấp lượt × số map đã dọn / số map) ({@link LinhMocRules#completionFruits}). 0 quả = không có thư.
    */
   private void sendFinishMail(LinhMocRun run) {
      LinhMocConfig c = LinhMocRules.config();
      int cleared = run.clearedCount();
      int fruits = LinhMocRules.completionFruits(run.mobLevel, cleared);
      if (fruits <= 0) {
         return;
      }
      String content = c.text("mailFinish").replace("{cleared}", String.valueOf(cleared))
            .replace("{total}", String.valueOf(c.maps().size())).replace("{level}", String.valueOf(run.mobLevel))
            .replace("{fruits}", String.valueOf(fruits));
      for (int id : run.memberIds) {
         ItemData item = ItemData.of(LinhMocRules.FRUIT, fruits);
         item.locked = true;
         sendMail(id, run.names.getOrDefault(id, ""), c.text("mailSubject"), content, List.of(item));
      }
      Log.debug("Rung Linh Moc: luot %d xong, %d/%d map, %d qua / nguoi", run.id, cleared, c.maps().size(), fruits);
   }

   /** Thư hệ thống kèm đồ: ghi SQL trên luồng db-writer, xong mới giao vào hòm thư (online thì hiện ngay). */
   private void sendMail(int playerId, String name, String subject, String content, List<ItemData> items) {
      Mail mail = mails.systemMail(playerId, name, subject, content, items);
      players.runOnWriter(conn -> MailRepository.insert(conn, mail)).thenAccept(ok -> {
         if (ok) {
            mails.deliver(mail);
         }
      });
   }

   /** +{@code linhmoc.points} điểm hoạt động (op 23 sub 9 + thông báo) trên luồng session của người đó, rồi lưu. */
   private void addPoints(int playerId) {
      int points = LinhMocRules.config().points();
      WorldPlayer m = world.online(playerId);
      if (m == null || points <= 0) {
         return;
      }
      PlayerSession s = m.session();
      PlayerData d = m.data();
      s.inbox().execute(() -> {
         if (s.getPlayer() != d) {
            return;
         }
         synchronized (d) {
            d.dynamicPoints += points;
         }
         players.saveAsync(d, false);
         s.send(WorldPackets.dynamicPoints(d.dynamicPoints));
         s.send(Packets.notice(LinhMocRules.config().text("points").replace("{points}", String.valueOf(points))));
      }, gamePool);
   }

   @Override
   public void bossKilled(LinhMocRun run, int topPlayerId, String bossName, List<Integer> presentIds) {
      gamePool.execute(() -> {
         for (int id : presentIds) {
            addPoints(id);
         }
         WorldPlayer top = topPlayerId == 0 ? null : world.online(topPlayerId);
         if (top != null) {
            top.session().send(Packets.notice(LinhMocRules.config().text("bossLoot").replace("{boss}", bossName)));
         }
         Log.debug("Rung Linh Moc: luot %d ha %s, uu tien nhat #%d", run.id, bossName, topPlayerId);
      });
   }

   /** Báo cho thành viên đang ở trong phó bản. */
   private void notifyInDungeon(LinhMocRun run, String text) {
      var packet = Packets.notice(text).toPacket(); // mã hoá 1 lần cho cả nhóm
      for (int id : run.memberIds) {
         WorldPlayer m = world.online(id);
         if (m != null && LinhMocRules.isDungeonMap(m.data().mapId)) {
            m.session().send(packet);
         }
      }
   }

   // ================================================================== lệnh test (chat.testCommands)

   private static volatile LinhMocService instance;

   /** Lệnh chat {@code /linhmoc ...} ({@code chat/ChatService}, luồng session). */
   public static void testCommand(PlayerSession session, PlayerData d, String[] args) {
      LinhMocService s = instance;
      if (s != null) {
         s.test(session, d, args);
      }
   }

   private static final String USAGE = "Lệnh: /linhmoc [reset] | vao | info | diet [chừa n] | map N | het [giây]"
         + " | ket | thua";

   private void test(PlayerSession session, PlayerData d, String[] args) {
      String sub = args.length > 1 ? args[1] : "";
      LinhMocRun run = activeRun(d.getId());
      List<Integer> maps = LinhMocRules.config().maps();
      switch (sub) {
         case "", "reset" -> { // trả lượt hôm nay cho mình + cả tổ đội
            PartyService.Snapshot party = parties.snapshot(d);
            for (int id : party == null ? List.of(d.getId()) : party.memberIds()) {
               WorldPlayer m = world.online(id);
               PlayerData md = id == d.getId() ? d : m == null ? null : m.data();
               if (md != null) {
                  synchronized (md) {
                     md.linhMocDay = 0;
                  }
               }
            }
            session.send(Packets.notice("Đã trả lại lượt Rừng Linh Mộc hôm nay (cả tổ đội). " + USAGE));
         }
         case "vao" -> join(session, d); // = bấm "Tham gia" (không cần tới Bác Rano, vẫn kiểm tra đủ điều kiện)
         case "info" -> session.send(Packets.notice(run == null
               ? "Rừng Linh Mộc: không có lượt đang chạy, còn " + LinhMocRules.turnsLeft(d, today()) + " lượt hôm nay."
               : "Rừng Linh Mộc: lượt " + run.id + ", " + run.memberIds.size() + " người, cấp quái " + run.mobLevel
                     + " (unit " + Math.round(run.unit) + "), còn " + duration(run.end - System.currentTimeMillis())
                     + ", đã dọn " + run.clearedCount() + "/" + maps.size() + " map, khu " + run.zoneNo + "."));
         case "diet" -> world.testKillLinhMocMobs(session, args.length > 2 ? parse(args[2], 0) : 0);
         case "map" -> {
            if (run == null) {
               session.send(Packets.notice("Chưa có lượt đang chạy (/linhmoc vao)."));
               return;
            }
            int stage = args.length > 2 ? Math.max(1, Math.min(maps.size(), parse(args[2], 1))) : 1;
            world.enterLinhMoc(session, run, maps.get(stage - 1));
         }
         case "het" -> {
            if (run == null) {
               session.send(Packets.notice("Chưa có lượt đang chạy (/linhmoc vao)."));
               return;
            }
            int seconds = args.length > 2 ? Math.max(1, parse(args[2], 10)) : 10;
            run.end = System.currentTimeMillis() + seconds * 1000L;
            scheduleEnd(run);
            world.refreshLinhMocTimer(session);
            session.send(Packets.notice("Lượt Rừng Linh Mộc còn " + seconds + " giây."));
         }
         case "ket" -> {
            if (run == null) {
               session.send(Packets.notice("Chưa có lượt đang chạy (/linhmoc vao)."));
               return;
            }
            succeed(run);
         }
         case "thua" -> {
            if (run == null) {
               session.send(Packets.notice("Chưa có lượt đang chạy (/linhmoc vao)."));
               return;
            }
            fail(run, LinhMocRules.config().text("timeout"));
         }
         default -> session.send(Packets.notice(USAGE));
      }
   }

   private static int parse(String text, int fallback) {
      try {
         return Integer.parseInt(text);
      } catch (NumberFormatException e) {
         return fallback;
      }
   }

   private static long today() {
      return LocalDate.now(ZONE).toEpochDay();
   }

   /** "12 phút 5 giây". */
   private static String duration(long ms) {
      long seconds = Math.max(0, (ms + 999) / 1000);
      return seconds >= 60 ? seconds / 60 + " phút " + seconds % 60 + " giây" : seconds + " giây";
   }
}
