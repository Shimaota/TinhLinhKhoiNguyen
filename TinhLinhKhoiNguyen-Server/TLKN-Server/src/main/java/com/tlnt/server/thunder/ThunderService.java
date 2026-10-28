package com.tlnt.server.thunder;

import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.npc.NpcContext;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.tribe.TribeService;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;
import com.tlnt.server.world.WorldPlayer;

import java.time.LocalDate;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * "Lôi vực Sấm sét" (luật: {@link ThunderRules}) — chiến trường PK bộ lạc ở map 31 / 32 / 33. Hộp thoại ở NPC 17 Tư tế
 * Buraen ({@code altar/AltarService} chuyển sang đây): Làng "Đấu trường" > "Lôi vực Sấm Sét" -> lượt còn lại + [Tham gia,
 * Đóng]; Tư tế trong Lôi vực -> [Nghi thức Ấn linh, Về làng, Đóng].
 *
 * <p>Lượt chỉ ở RAM ({@code PlayerData.thunderRun}); lượt đã vào + Lệnh bài đã dùng trong ngày đếm ở
 * {@code PlayerData.itemDaily} (lưu DB, qua ngày tự xoá). Rời map (Về làng / nút Thoát / bị loại / hết giờ / thoát game)
 * là hết lượt ({@link #left}).
 *
 * <p>Luồng: hộp thoại + lệnh test trên luồng session; móc {@link World.ThunderHooks} từ luồng tick khu chuyển sang hàng
 * đợi session; hết giờ = scheduler -> hàng đợi session.
 */
public final class ThunderService implements World.ThunderHooks {

   /** Trang hộp thoại (AltarService dùng 0..4 cho Tế đàn). */
   public static final int PAGE_JOIN = 20;
   static final int PAGE_INSIDE = 21;
   static final int PAGE_SEAL = 22;

   private final World world;
   private final ItemService items;
   private final PlayerService players;
   private final ScheduledExecutorService scheduler;
   private final Executor gamePool;

   public ThunderService(World world, ItemService items, PlayerService players, ScheduledExecutorService scheduler,
         Executor gamePool) {
      this.world = world;
      this.items = items;
      this.players = players;
      this.scheduler = scheduler;
      this.gamePool = gamePool;
      instance = this;
   }

   // ================================================================== hộp thoại Tư tế Buraen

   /** Làng: "Đấu trường" > "Lôi vực Sấm Sét". */
   public void showJoin(NpcContext ctx) {
      ctx.say(PAGE_JOIN, "Chiến trường Sấm sét nằm trên thượng nguồn Eldarah là nơi ngự trị của Sấm sét và Lôi điện\n"
            + "Các Bộ lạc sẽ phải tranh đấu giành lấy tài nguyên và giao đấu với Tinh linh Sấm sét cùng Vệ thần của"
            + " ông ấy\n"
            + "Để tham gia cậu cần có 1 Lệnh bài Đấu trường. Mỗi ngày được tham gia tối đa 2 lần\n"
            + "Nếu đã sẵn sàng thì hãy theo ta tới thượng nguồn\n"
            + "Lượt tham gia còn lại hôm nay: " + turnsLeft(ctx.data()),
            "Tham gia", "Đóng");
   }

   /** Tư tế đứng trong Lôi vực. */
   public void onTalk(NpcContext ctx) {
      int mapId = ctx.mapId();
      ctx.say(PAGE_INSIDE, "Cậu đang ở " + ThunderRules.side(mapId) + " Lôi vực Sấm Sét, nơi dành cho những chiến binh "
            + ThunderRules.levelRange(mapId) + "\n"
            + "Nơi đây là cấm địa thiêng liêng của bộ lạc Eldarah được trấn giữ bởi các thực thể Sấm Sét",
            "Nghi thức Ấn linh", "Về làng", "Đóng");
   }

   public void onChoice(NpcContext ctx, int page, int option) {
      switch (page) {
         case PAGE_JOIN -> {
            if (option == 0) {
               join(ctx.session(), ctx.data());
            }
         }
         case PAGE_INSIDE -> {
            switch (option) {
               case 0 -> ctx.say(PAGE_SEAL, "Ta sẽ thực hiện nghi thức Ấn Linh, kết nối cậu với sức mạnh cổ xưa\n"
                     + "Lôi thần sẽ ban sức mạnh cho cậu, sức mạnh đó chỉ có tác dụng ở nơi này. Hãy sử dụng cho hiệu"
                     + " quả\n"
                     + "+ " + ThunderRules.SEAL_ATTACK_PERCENT + "% Tấn công\n"
                     + "+ " + ThunderRules.SEAL_HP_MP_PERCENT + "% HP, MP\n"
                     + "+ " + ThunderRules.SEAL_ARMOR_PERCENT + "% Giáp",
                     "Đồng ý(" + ThunderRules.SEAL_PRICE + " Lam thạch)", "Đóng");
               case 1 -> world.leaveThunder(ctx.session(), "Bạn đã rời Lôi vực Sấm sét.");
               default -> {
               }
            }
         }
         case PAGE_SEAL -> {
            if (option == 0) {
               seal(ctx);
            }
         }
         default -> {
         }
      }
   }

   // ================================================================== vào / Ấn linh

   /** Lượt còn lại hôm nay (luồng session — {@code itemDaily} tự xoá sổ qua ngày). */
   static int turnsLeft(PlayerData d) {
      long today = today();
      int used = d.itemDaily.usedToday(ThunderRules.TURN_KEY, today);
      return ThunderRules.turnsLeft(used, d.itemDaily.usedToday(ThunderRules.TOKEN_ITEM, today));
   }

   /** "Tham gia": trừ 1 lượt, mở lượt 60 phút, vào map theo cấp. Điểm thưởng nhận lúc tới nơi ({@link #entered}). */
   void join(PlayerSession session, PlayerData d) {
      int mapId = ThunderRules.mapFor(d.level);
      String error = mapId == 0 ? "Cần đạt cấp " + ThunderRules.MIN_LEVEL + " để vào Lôi vực Sấm sét."
            : ThunderRules.isThunderMap(d.mapId) ? "Bạn đang ở Lôi vực Sấm sét."
            : d.trade != null ? "Đang giao dịch, không thể vào Lôi vực Sấm sét."
            : d.fishing != null ? "Đang câu cá, không thể vào Lôi vực Sấm sét."
            : d.isDead() ? "Đã bị hạ gục, không thể vào Lôi vực Sấm sét."
            : turnsLeft(d) <= 0 ? "Hôm nay bạn đã hết lượt tham gia Lôi vực Sấm sét. Sử dụng Lệnh bài Đấu trường để có"
                  + " thêm lượt (tối đa " + ThunderRules.MAX_TOKENS + " lệnh bài / ngày)."
            : null;
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      ThunderRun run = new ThunderRun(System.currentTimeMillis(), mapId);
      synchronized (d) {
         d.itemDaily.used.merge(ThunderRules.TURN_KEY, 1, Integer::sum);
         d.thunderRun = run; // thay cả đối tượng: luồng tick luôn thấy 1 lượt trọn vẹn
      }
      players.saveAsync(d, false); // lượt đã dùng phải còn kể cả khi server sập
      scheduleEnd(d.getId(), run);
      world.enterThunder(session, mapId, () -> session.inbox().execute(() -> refund(session, d, run), gamePool));
      Log.debug("Loi vuc: '%s' (cap %d) vao map %d", d.name, d.level, mapId);
   }

   /** Không tới được map (bị hạ gục / đang chuyển khu giữa chừng): trả lại lượt. Luồng session. */
   private void refund(PlayerSession session, PlayerData d, ThunderRun run) {
      if (session.getPlayer() != d || d.thunderRun != run) {
         return;
      }
      run.finish();
      synchronized (d) {
         d.thunderRun = null;
         d.itemDaily.used.merge(ThunderRules.TURN_KEY, -1, Integer::sum);
      }
      players.saveAsync(d, false);
      session.send(Packets.notice("Không thể vào Lôi vực Sấm sét lúc này, lượt đã được hoàn lại."));
   }

   /** Nghi thức Ấn linh: chỉ trong Lôi vực, trong lượt đang chạy, 1 lần / lượt. */
   private void seal(NpcContext ctx) {
      PlayerSession session = ctx.session();
      PlayerData d = ctx.data();
      ThunderRun run = d.thunderRun;
      if (!ThunderRules.isThunderMap(ctx.mapId()) || run == null || run.over()) {
         session.send(Packets.notice("Nghi thức Ấn linh chỉ thực hiện được trong lượt Lôi vực Sấm sét."));
         return;
      }
      if (run.sealed) {
         session.send(Packets.notice("Cậu đã được Ấn linh rồi."));
         return;
      }
      synchronized (d) {
         if (session.getPlayer() != d) {
            return;
         }
         if (d.trade != null) {
            session.send(Packets.notice("Đang giao dịch, không thể thực hiện nghi thức."));
            return;
         }
         if (d.diamond < ThunderRules.SEAL_PRICE) {
            session.send(Packets.notice("Không đủ " + ThunderRules.SEAL_PRICE + " lam thạch."));
            return;
         }
         d.diamond -= ThunderRules.SEAL_PRICE;
         run.sealed = true;
      }
      items.syncBagAndCurrency(session);
      players.saveAsync(d, false);
      refreshStats(d);
      session.send(Packets.notice("Nghi thức Ấn linh hoàn tất! +" + ThunderRules.SEAL_ATTACK_PERCENT + "% Tấn công, +"
            + ThunderRules.SEAL_HP_MP_PERCENT + "% HP, MP, +" + ThunderRules.SEAL_ARMOR_PERCENT
            + "% Giáp trong Lôi vực Sấm sét."));
   }

   // ================================================================== móc từ luồng tick khu

   @Override
   public void entered(WorldPlayer p, ThunderRun run) {
      onSession(p, (session, d) -> {
         if (d.thunderRun != run) {
            return;
         }
         int points;
         synchronized (d) {
            points = d.dynamicPoints += ThunderRules.JOIN_DYNAMIC_POINTS;
         }
         players.saveAsync(d, false);
         session.send(WorldPackets.dynamicPoints(points));
         session.send(Packets.notice("Bạn vừa nhận được " + ThunderRules.JOIN_DYNAMIC_POINTS + " điểm hoạt động"));
         TribeService.reward(d.getId(), ThunderRules.JOIN_TRIBE_POINTS, "tham gia Lôi vực Sấm sét");
      });
   }

   @Override
   public void left(WorldPlayer p, ThunderRun run) {
      run.finish(); // ngay trên luồng tick: lượt hết hiệu lực (Ấn linh, đồng hồ)
      onSession(p, (session, d) -> {
         if (d.thunderRun != run) {
            return;
         }
         d.thunderRun = null;
         if (run.sealed) {
            refreshStats(d); // Ấn linh chỉ có tác dụng trong Lôi vực
         }
         Log.debug("Loi vuc: '%s' roi map, ha guc %d lan", d.name, run.deaths);
      });
   }

   @Override
   public void playerKilled(WorldPlayer killer, WorldPlayer victim) {
      onSession(killer, (session, d) -> {
         ThunderRun run = d.thunderRun;
         if (run == null || run.finished()) {
            return;
         }
         int tribe = d.tribe == null ? 0
               : ThunderRules.capped(ThunderRules.KILL_TRIBE_POINTS, run.killTribePoints, ThunderRules.KILL_TRIBE_CAP);
         int dynamic = ThunderRules.capped(ThunderRules.KILL_DYNAMIC_POINTS, run.killDynamicPoints,
               ThunderRules.KILL_DYNAMIC_CAP);
         if (tribe > 0) {
            run.killTribePoints += tribe;
            TribeService.reward(d.getId(), tribe, "hạ gục người chơi ở Lôi vực Sấm sét");
         }
         if (dynamic > 0) {
            run.killDynamicPoints += dynamic;
            int points;
            synchronized (d) {
               points = d.dynamicPoints += dynamic;
            }
            players.saveAsync(d, false);
            session.send(WorldPackets.dynamicPoints(points));
            session.send(Packets.notice("Bạn vừa nhận được " + dynamic + " điểm hoạt động"));
         }
      });
   }

   /** Chạy {@code work} trên hàng đợi session của {@code p} (bỏ nếu đã thoát / vào lại bằng nhân vật khác). */
   private void onSession(WorldPlayer p, java.util.function.BiConsumer<PlayerSession, PlayerData> work) {
      PlayerSession session = p.session();
      PlayerData d = p.data();
      session.inbox().execute(() -> {
         if (session.getPlayer() == d) {
            work.accept(session, d);
         }
      }, gamePool);
   }

   // ================================================================== hết giờ

   /** Hết giờ: người còn trong Lôi vực với đúng lượt này -> về làng (hẹn cũ thấy giờ kết thúc bị đổi thì thôi). */
   private void scheduleEnd(int playerId, ThunderRun run) {
      long delay = run.end - System.currentTimeMillis() + 100;
      scheduler.schedule(() -> {
         if (run.finished() || System.currentTimeMillis() < run.end) {
            return;
         }
         WorldPlayer owner = world.online(playerId);
         if (owner == null || owner.data().thunderRun != run) {
            return;
         }
         onSession(owner, (session, d) -> {
            if (d.thunderRun == run) {
               world.leaveThunder(session, "Hết thời gian Lôi vực Sấm sét, bạn đã được đưa về làng.");
            }
         });
      }, Math.max(0, delay), TimeUnit.MILLISECONDS);
   }

   // ================================================================== lệnh test (chat.testCommands)

   private static volatile ThunderService instance;

   private static final String USAGE = "Lệnh: /loivuc [info] | reset | vao | het [giây] | chet N | ra";

   /** Lệnh chat {@code /loivuc ...} ({@code chat/ChatService}, luồng session). */
   public static void testCommand(PlayerSession session, PlayerData d, String[] args) {
      ThunderService s = instance;
      if (s != null) {
         s.test(session, d, args);
      }
   }

   private void test(PlayerSession session, PlayerData d, String[] args) {
      String sub = args.length > 1 ? args[1] : "info";
      ThunderRun run = d.thunderRun;
      long now = System.currentTimeMillis();
      switch (sub) {
         case "reset" -> {
            d.itemDaily.used.remove(ThunderRules.TURN_KEY);
            d.itemDaily.used.remove(ThunderRules.TOKEN_ITEM);
            session.send(Packets.notice("Đã trả lại lượt + lệnh bài Lôi vực hôm nay. " + USAGE));
         }
         case "vao" -> join(session, d); // = bấm "Tham gia" (không cần tới Tư tế)
         case "het" -> {
            int seconds = args.length > 2 ? Math.max(1, parse(args[2], 10)) : 10;
            if (run == null || run.over()) {
               session.send(Packets.notice("Chưa có lượt đang chạy (/loivuc vao)."));
               return;
            }
            run.end = now + seconds * 1000L;
            scheduleEnd(d.getId(), run);
            world.refreshThunderTimer(session);
            session.send(Packets.notice("Lượt Lôi vực còn " + seconds + " giây."));
         }
         case "chet" -> {
            if (run == null) {
               session.send(Packets.notice("Chưa có lượt đang chạy (/loivuc vao)."));
               return;
            }
            run.deaths = Math.max(0, parse(args.length > 2 ? args[2] : "", ThunderRules.MAX_DEATHS));
            session.send(Packets.notice("Đặt số lần bị hạ gục = " + run.deaths + "."));
         }
         case "ra" -> world.leaveThunder(session, "Đã rời Lôi vực (lệnh test).");
         default -> session.send(Packets.notice("Lôi vực: còn " + turnsLeft(d) + " lượt hôm nay (lệnh bài đã dùng "
               + d.itemDaily.usedToday(ThunderRules.TOKEN_ITEM, today()) + "/" + ThunderRules.MAX_TOKENS + "), lượt "
               + (run == null || run.over() ? "không có"
               : "map " + run.mapId + " còn " + Math.max(0, (run.end - now) / 1000) + " s, bị hạ gục " + run.deaths
               + " lần, Ấn linh " + (run.sealed ? "có" : "không") + ", điểm hạ gục " + run.killTribePoints + " cống hiến / "
               + run.killDynamicPoints + " hoạt động") + ". " + USAGE));
      }
   }

   private static int parse(String text, int fallback) {
      try {
         return Integer.parseInt(text.trim());
      } catch (NumberFormatException e) {
         return fallback;
      }
   }

   // ================================================================== nội bộ

   private void refreshStats(PlayerData d) {
      ItemService.refreshLook(d);
      world.broadcastStats(d);
   }

   static long today() {
      return LocalDate.now().toEpochDay();
   }
}
