package com.tlnt.server.altar;

import com.tlnt.server.item.ItemService;
import com.tlnt.server.mail.Mail;
import com.tlnt.server.mail.MailRepository;
import com.tlnt.server.mail.MailService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.npc.NpcAction;
import com.tlnt.server.npc.NpcContext;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.quest.MainQuests;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.shop.ShopService;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;
import com.tlnt.server.world.WorldPlayer;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Phó bản "Tế đàn Thiên tích" + NPC 17 Tư tế Buraen (luật: {@link AltarRules}). SQL chỉ lưu số lượt đã đi trong ngày
 * ({@code PlayerData.altarDay} = ngày đi lượt gần nhất, 1 lượt / ngày, sang 0:00 có lượt mới); lượt đang chạy ({@link AltarRun}) chỉ trong RAM ({@link #runs}): thoát game
 * vào lại vẫn tiếp tục, TẮT MÁY CHỦ LÀ MẤT LƯỢT (lượt đã tính).
 *
 * <p>Buraen ở Làng (và map khác ngoài Tế đàn): "Đấu trường" -> [Lôi vực Sấm Sét, Tế đàn Thiên tích, Giải đấu Bộ lạc]
 * -> Tế đàn: lượt còn lại hôm nay + "Tham gia". Buraen ở map 34: [Nghi thức Ấn linh (10 lam thạch, +15% Tấn công lên
 * quái / Hút HP / Hút MP quái, chỉ trong Tế đàn, hết khi hết lượt), Về làng, Đóng].
 *
 * <p>1 lượt: "Tham gia" trừ 1 lượt trong ngày, bắt đầu đếm 30 phút, vào map 34 ở khu riêng của lượt (số khu giữ nguyên
 * qua 7 map — {@code world/AltarZones}). Diệt hết quái map N -> rương lên cấp N + cửa sang map N+1 mở (World chặn cửa).
 * Thoát ra (nút Thoát / Về làng / chết về làng / thoát game) lượt vẫn chạy, "Tham gia" lại = về map đang dở, không tốn
 * lượt. Diệt hết map 7 hoặc hết 30 phút -> chốt: Rương gỗ cấp = số map đã diệt hết gửi qua THƯ (khoá), đang ở Tế đàn
 * thì đưa về làng. Offline lúc hết giờ -> vẫn gửi thư ngay ({@link #finishOffline}).
 *
 * <p>Luồng session của người chơi (NPC, hẹn giờ qua scheduler -> hàng đợi session, móc từ luồng tick -> hàng đợi session).
 */
public final class AltarService implements NpcAction, World.AltarHooks {

   public static final int NPC_BURAEN = 17;
   /** Cửa hàng tư thế của Buraen ({@code tlnt_data.shop.id}). */
   static final int SHOP_POSE = 3;
   private static final ZoneId ZONE = ZoneId.systemDefault();

   /** Trang hộp thoại. */
   private static final int PAGE_MAIN = 0;
   private static final int PAGE_ARENA = 1;
   private static final int PAGE_ALTAR = 2;
   private static final int PAGE_SHRINE = 3;
   private static final int PAGE_SEAL = 4;

   private final World world;
   private final ItemService items;
   private final PlayerService players;
   private final MailService mails;
   private final ShopService shops;
   private final ScheduledExecutorService scheduler;
   private final Executor gamePool;
   /** Lượt đang chạy theo id nhân vật (RAM; chốt xong thì gỡ). */
   private final Map<Integer, AltarRun> runs = new ConcurrentHashMap<>();
   /** Lôi vực Sấm sét (cùng NPC Buraen): trang {@code >= ThunderService.PAGE_JOIN} + Buraen trong map 31-33. */
   private volatile com.tlnt.server.thunder.ThunderService thunder;

   public AltarService(World world, ItemService items, PlayerService players, MailService mails, ShopService shops,
         ScheduledExecutorService scheduler, Executor gamePool) {
      this.world = world;
      this.items = items;
      this.players = players;
      this.mails = mails;
      this.shops = shops;
      this.scheduler = scheduler;
      this.gamePool = gamePool;
      instance = this;
   }

   public void setThunder(com.tlnt.server.thunder.ThunderService thunder) {
      this.thunder = thunder;
   }

   /** Giải đấu Bộ lạc (cùng NPC Buraen): trang {@code >= TournamentService.PAGE_BASE} + Buraen ở map 22 đấu trường. */
   private volatile com.tlnt.server.tournament.TournamentService tournament;

   public void setTournament(com.tlnt.server.tournament.TournamentService tournament) {
      this.tournament = tournament;
   }

   // ================================================================== hộp thoại Buraen

   @Override
   public void onTalk(NpcContext ctx) {
      if (thunder != null && com.tlnt.server.thunder.ThunderRules.isThunderMap(ctx.mapId())) {
         thunder.onTalk(ctx);
         return;
      }
      if (tournament != null && com.tlnt.server.world.Arena.isArenaMap(ctx.mapId())) {
         tournament.onTalkInside(ctx);
         return;
      }
      if (ctx.mapId() == AltarRules.FIRST_MAP) {
         ctx.say(PAGE_SHRINE, "Tế đàn Thiên Tích từng là nơi linh thiêng của Eldarah, nơi người dân thực hiện những buổi"
               + " hiến tế theo khế ước với Tinh linh Bầu trời\n"
               + "Nhờ khế ước ấy, Eldarah đã có những năm tháng yên bình mùa màng tươi tốt... Nhưng mọi thứ đều có cái"
               + " giá của nó\n"
               + "Sau khi khế ước bị phá vỡ, nơi đây dần bị bóng tối bao phủ... Rất ít khi có kẻ dám lui tới đây\n"
               + "Những Cận vệ Thiên Tích vẫn trung thành và canh giữ nơi này cho đến tận ngày nay",
               "Nghi thức Ấn linh", "Về làng", "Đóng");
         return;
      }
      ctx.say(PAGE_MAIN, "Ta là Buraen.. tư tế của bộ tộc Eldarah\n"
            + "Ngươi giữ nghi lễ và là kẻ đứng giữa ranh giới của con người và thế giới Tinh linh\n"
            + "Ta lắng nghe những lời thì thầm của linh khí và truyền đạt lại ý chí của các thực thể cổ xưa",
            "Đấu trường", "Cửa hàng tư thế", "Đóng");
   }

   @Override
   public void onChoice(NpcContext ctx, int page, int option) {
      if (tournament != null && page >= com.tlnt.server.tournament.TournamentService.PAGE_BASE) {
         tournament.onChoice(ctx, page, option);
         return;
      }
      if (thunder != null && page >= com.tlnt.server.thunder.ThunderService.PAGE_JOIN) {
         thunder.onChoice(ctx, page, option);
         return;
      }
      switch (page) {
         case PAGE_MAIN -> {
            switch (option) {
               case 0 -> ctx.say(PAGE_ARENA, "Linh khí trời đất của Eldarah đang dậy sóng... Một cuộc tỷ thí lớn sắp"
                     + " diễn ra!\n"
                     + "- Nếu cậu muốn thử thách... Hãy theo ta tới thượng nguồn",
                     "Lôi vực Sấm Sét", "Tế đàn Thiên tích", "Giải đấu Bộ lạc");
               case 1 -> shops.open(ctx, SHOP_POSE);
               default -> {
               }
            }
         }
         case PAGE_ARENA -> {
            switch (option) {
               case 0 -> {
                  if (thunder != null) {
                     thunder.showJoin(ctx);
                  } else {
                     ctx.send(Packets.notice("Lôi vực Sấm Sét sắp ra mắt."));
                  }
               }
               case 1 -> showAltar(ctx);
               case 2 -> {
                  if (tournament != null) {
                     tournament.show(ctx);
                  } else {
                     ctx.send(Packets.notice("Giải đấu Bộ lạc sắp ra mắt."));
                  }
                  QuestEvents.fire(ctx.session(), StepKind.JOIN_TOURNAMENT, 0); // NV 11.2: bấm là xong
               }
               default -> {
               }
            }
         }
         case PAGE_ALTAR -> {
            if (option == 0) {
               join(ctx.session(), ctx.data());
            }
         }
         case PAGE_SHRINE -> {
            switch (option) {
               case 0 -> ctx.say(PAGE_SEAL, "Ta sẽ thực hiện nghi thức Ấn linh, kết nối cậu với sức mạnh cổ xưa.\n"
                     + "Lời thần sẽ ban sức mạnh cho cậu. Sức mạnh ấy chỉ có tác dụng tại nơi này. Hãy sử dụng thật"
                     + " hiệu quả!\n"
                     + "+" + AltarRules.SEAL_PERCENT + "% Tấn công lên quái\n"
                     + "+" + AltarRules.SEAL_PERCENT + "% Hút HP, MP quái",
                     "Đồng ý(" + AltarRules.SEAL_PRICE + " Lam thạch)", "Đóng");
               case 1 -> world.leaveAltar(ctx.session(), null);
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

   private void showAltar(NpcContext ctx) {
      PlayerData d = ctx.data();
      long now = System.currentTimeMillis();
      settleIfExpired(ctx.session(), d, now);
      AltarRun st = d.altarRun;
      long end = st.activeEnd(now);
      String status = end > 0
            ? "Lượt đang đi còn " + duration(end - now) + " (tham gia để quay lại " + mapName(st) + ")"
            : "Lượt đi còn lại trong hôm nay: " + AltarRules.turnsLeft(d, today());
      ctx.say(PAGE_ALTAR, "Tế Đàn Thiên Tích từng là thánh địa linh thiêng nhất Eldarah, nơi người dân thực hiện các"
            + " nghi lễ hiến tế để duy trì khế ước\n"
            + "Đó là nơi Tinh linh Bầu trời đang ngự trị... Xung quanh Tế đàn được bảo vệ bởi các Cận vệ Thiên tích\n"
            + "Rất ít khi có người dám lui tới nơi đó... Nếu đã quyết tâm, hãy chuẩn bị thật kỹ rồi lên đường\n"
            + status,
            "Tham gia");
   }

   // ================================================================== vào / Ấn linh

   /** "Tham gia": lượt đang chạy -> vào lại map đang dở (không tốn lượt); không thì bắt đầu lượt mới (tốn 1 lượt). */
   void join(PlayerSession session, PlayerData d) {
      long now = System.currentTimeMillis();
      settleIfExpired(session, d, now);
      String error = d.party != null ? "Bạn phải rời khỏi tổ đội mới có thể vào Tế đàn Thiên tích."
            : d.trade != null ? "Đang giao dịch, không thể vào Tế đàn Thiên tích."
            : d.fishing != null ? "Đang câu cá, không thể vào Tế đàn Thiên tích."
            : d.isDead() ? "Đã bị hạ gục, không thể vào Tế đàn Thiên tích."
            : !QuestEvents.canEnter(d, AltarRules.FIRST_MAP) ? MainQuests.MAP_LOCKED // mở từ nhiệm vụ 10
            : null;
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      AltarRun st = d.altarRun;
      if (st.activeEnd(now) > 0) {
         world.enterAltar(session, AltarRules.isAltarMap(st.map) ? st.map : AltarRules.FIRST_MAP);
         return;
      }
      long today = today();
      if (AltarRules.turnsLeft(d, today) <= 0) {
         session.send(Packets.notice("Hôm nay bạn đã hết lượt vào Tế đàn Thiên tích, hãy quay lại vào ngày mai."));
         return;
      }
      if (runs.size() >= AltarRules.MAX_RUNS) { // đủ số lượt chạy cùng lúc (cả server): chưa trừ lượt
         session.send(Packets.notice("Tế đàn Thiên tích đang quá tải, hãy thử lại sau."));
         return;
      }
      AltarRun next = new AltarRun(now, d.name);
      synchronized (d) {
         d.altarDay = (int) today; // hôm nay đã đi
         d.altarRun = next; // thay cả đối tượng: luồng tick luôn thấy 1 lượt trọn vẹn
         d.dynamicPoints += AltarRules.JOIN_DYNAMIC_POINTS; // chỉ lượt mới (vào lại lượt đang dở không được)
      }
      runs.put(d.getId(), next);
      players.saveAsync(d, false); // lượt đã dùng + điểm phải còn kể cả khi server sập
      session.send(WorldPackets.dynamicPoints(d.dynamicPoints));
      session.send(Packets.notice("Bạn vừa nhận được " + AltarRules.JOIN_DYNAMIC_POINTS + " điểm hoạt động"));
      scheduleEnd(session, d, next);
      world.enterAltar(session, AltarRules.FIRST_MAP);
      session.send(Packets.notice("Bạn có 30 phút để vượt qua 7 tầng Tế đàn Thiên tích. Tiêu diệt hết Cận vệ mỗi tầng"
            + " để mở đường!"));
      Log.debug("Te dan: '%s' bat dau luot", d.name);
   }

   /** Nghi thức Ấn linh: chỉ ở Tế đàn, trong lượt đang chạy, 1 lần / lượt. */
   private void seal(NpcContext ctx) {
      PlayerSession session = ctx.session();
      PlayerData d = ctx.data();
      AltarRun st = d.altarRun;
      if (!AltarRules.isAltarMap(ctx.mapId()) || st.activeEnd(System.currentTimeMillis()) == 0) {
         session.send(Packets.notice("Nghi thức Ấn linh chỉ thực hiện được trong lượt Tế đàn Thiên tích."));
         return;
      }
      if (st.sealed) {
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
         if (d.diamond < AltarRules.SEAL_PRICE) {
            session.send(Packets.notice("Không đủ " + AltarRules.SEAL_PRICE + " lam thạch."));
            return;
         }
         d.diamond -= AltarRules.SEAL_PRICE;
         st.sealed = true;
      }
      items.syncBagAndCurrency(session);
      players.saveAsync(d, false);
      refreshStats(d);
      session.send(Packets.notice("Nghi thức Ấn linh hoàn tất! +" + AltarRules.SEAL_PERCENT + "% Tấn công lên quái, +"
            + AltarRules.SEAL_PERCENT + "% Hút HP, MP quái trong Tế đàn."));
   }

   // ================================================================== móc từ luồng tick khu

   @Override
   public void mapChanged(WorldPlayer p, int fromMapId, int toMapId) {
      PlayerSession session = p.session();
      PlayerData d = p.data();
      session.inbox().execute(() -> {
         if (session.getPlayer() != d) {
            return;
         }
         AltarRun st = d.altarRun;
         if (AltarRules.isAltarMap(toMapId) && st.activeEnd(System.currentTimeMillis()) > 0) {
            st.map = toMapId; // vào lại thì về đây
         }
         if (st.sealed && AltarRules.isAltarMap(fromMapId) != AltarRules.isAltarMap(toMapId)) {
            refreshStats(d); // Ấn linh chỉ có tác dụng trong Tế đàn
         }
      }, gamePool);
   }

   @Override
   public void cleared(WorldPlayer p, int mapId) {
      PlayerSession session = p.session();
      PlayerData d = p.data();
      session.inbox().execute(() -> {
         if (session.getPlayer() == d) {
            onCleared(session, d, AltarRules.stage(mapId));
         }
      }, gamePool);
   }

   @Override
   public void bossKilled(WorldPlayer p) {
      PlayerSession session = p.session();
      PlayerData d = p.data();
      session.inbox().execute(() -> {
         if (session.getPlayer() != d) {
            return;
         }
         synchronized (d) {
            d.dynamicPoints += AltarRules.BOSS_KILL_DYNAMIC_POINTS;
         }
         players.saveAsync(d, false);
         session.send(WorldPackets.dynamicPoints(d.dynamicPoints));
         session.send(Packets.notice("Bạn vừa nhận được " + AltarRules.BOSS_KILL_DYNAMIC_POINTS + " điểm hoạt động"));
      }, gamePool);
   }

   private void onCleared(PlayerSession session, PlayerData d, int stage) {
      AltarRun st = d.altarRun;
      if (st.activeEnd(System.currentTimeMillis()) == 0) {
         return;
      }
      if (stage == st.cleared + 1) { // cửa chỉ mở theo thứ tự -> luôn liên tiếp (diệt lại tầng cũ sau khi server khởi động lại: bỏ)
         st.cleared = stage;
      }
      if (stage >= AltarRules.STAGES) {
         finish(session, d, "Chúc mừng! Bạn đã chinh phục Tế đàn Thiên tích, phần thưởng đã được gửi qua thư.");
         return;
      }
      session.send(Packets.notice("Đã tiêu diệt hết Cận vệ Thiên tích! Rương gỗ lên cấp " + st.cleared
            + ". Đường tới " + world.mapTemplate(AltarRules.mapOf(stage + 1)).name() + " đã mở."));
   }

   // ================================================================== hết lượt

   /**
    * Vào game (luồng session, trước khi vào khu): lượt còn trong RAM (thoát game giữa chừng, máy chủ chưa tắt) -> gắn lại
    * vào nhân vật + hẹn giờ kết thúc. Máy chủ vừa khởi động lại: không có -> lượt đó coi như bỏ.
    */
   public void onLogin(PlayerSession session, PlayerData d) {
      AltarRun run = runs.get(d.getId());
      if (run == null) {
         return;
      }
      d.altarRun = run;
      long now = System.currentTimeMillis();
      if (run.expiredUnsettled(now)) {
         finish(session, d, null);
      } else if (run.activeEnd(now) > 0) {
         scheduleEnd(session, d, run);
      }
   }

   /**
    * Hết giờ: chủ lượt online -> chốt trên luồng session của họ (về làng + thư); offline -> {@link #finishOffline}. Hẹn
    * cũ (giờ kết thúc bị đổi bằng /tedan het) thấy chưa hết giờ thì thôi — hẹn mới lo.
    */
   private void scheduleEnd(PlayerSession session, PlayerData d, AltarRun run) {
      int playerId = d.getId();
      long delay = run.endAt() - System.currentTimeMillis() + 100;
      scheduler.schedule(() -> {
         if (!run.expiredUnsettled(System.currentTimeMillis())) {
            return;
         }
         // Chủ lượt có thể đã thoát rồi vào lại (session / PlayerData mới, lượt gắn lại ở onLogin): tìm theo id.
         WorldPlayer owner = world.online(playerId);
         if (owner == null || owner.data().altarRun != run) {
            gamePool.execute(() -> finishOffline(playerId, run));
            return;
         }
         PlayerSession s = owner.session();
         PlayerData od = owner.data();
         s.inbox().execute(() -> {
            if (s.getPlayer() == od && od.altarRun == run) {
               finish(s, od, "Hết thời gian Tế đàn Thiên tích! Phần thưởng đã được gửi qua thư.");
            } else {
               finishOffline(playerId, run);
            }
         }, gamePool);
      }, Math.max(0, delay), TimeUnit.MILLISECONDS);
   }

   private void settleIfExpired(PlayerSession session, PlayerData d, long now) {
      if (d.altarRun.expiredUnsettled(now)) {
         finish(session, d, "Lượt Tế đàn Thiên tích đã kết thúc.");
      }
   }

   /** Chủ lượt đã thoát game lúc hết giờ: gửi thư thưởng (không đụng nhân vật — số lượt đã lưu lúc vào). */
   private void finishOffline(int playerId, AltarRun run) {
      if (!run.tryFinish()) {
         return;
      }
      runs.remove(playerId, run);
      world.endAltarRun(playerId);
      Mail mail = rewardMail(playerId, run);
      if (mail != null) {
         players.runOnWriter(conn -> MailRepository.insert(conn, mail)).thenAccept(ok -> {
            if (ok) {
               mails.deliver(mail);
            }
         });
      }
      Log.debug("Te dan: #%d (offline) xong luot, %d tang", playerId, run.cleared);
   }

   /** Thư Rương gỗ cấp = số tầng đã diệt hết (khoá); null = chưa diệt hết tầng nào. */
   private Mail rewardMail(int playerId, AltarRun run) {
      int chest = AltarRules.chestItem(run.cleared);
      if (chest == 0) {
         return null;
      }
      ItemData item = ItemData.of(chest, 1);
      item.locked = true;
      return mails.systemMail(playerId, run.ownerName, "Tế đàn Thiên tích", "Bạn đã vượt qua " + run.cleared + "/"
            + AltarRules.STAGES + " tầng Tế đàn Thiên tích. Phần thưởng: Rương gỗ cấp " + run.cleared + ".",
            List.of(item));
   }

   /**
    * Chốt lượt (1 lần): Rương gỗ cấp = số tầng đã diệt hết -> thư (khoá), lưu nhân vật + thư trong 1 transaction; đang ở
    * Tế đàn thì về làng kèm {@code notice}. Không có rương: chỉ báo.
    */
   private void finish(PlayerSession session, PlayerData d, String notice) {
      AltarRun st = d.altarRun;
      if (!st.tryFinish()) {
         return;
      }
      runs.remove(d.getId(), st);
      int chest = AltarRules.chestItem(st.cleared);
      Mail mail = rewardMail(d.getId(), st);
      if (mail != null) {
         players.runOnWriter(conn -> MailRepository.insert(conn, mail)).thenAccept(ok -> {
            if (ok) {
               mails.deliver(mail);
            }
         });
      }
      world.endAltarRun(d.getId());
      String text = notice == null ? null
            : chest > 0 ? notice
            : "Lượt Tế đàn Thiên tích đã kết thúc. Bạn chưa vượt qua tầng nào nên không có phần thưởng.";
      world.leaveAltar(session, text); // không ở Tế đàn thì không làm gì
      if (text != null && !AltarRules.isAltarMap(d.mapId) && d.inWorld) {
         session.send(Packets.notice(text));
      }
      Log.debug("Te dan: '%s' xong luot, %d tang, ruong %d", d.name, st.cleared, chest);
   }

   // ================================================================== lệnh test (chat.testCommands)

   private static volatile AltarService instance;

   /** Lệnh chat {@code /tedan ...} ({@code chat/ChatService}, luồng session). */
   public static void testCommand(PlayerSession session, PlayerData d, String[] args) {
      AltarService s = instance;
      if (s != null) {
         s.test(session, d, args);
      }
   }

   private void test(PlayerSession session, PlayerData d, String[] args) {
      String sub = args.length > 1 ? args[1] : "";
      AltarRun st = d.altarRun;
      long now = System.currentTimeMillis();
      boolean active = st.activeEnd(now) > 0;
      switch (sub) {
         case "", "reset" -> {
            d.altarDay = 0;
            session.send(Packets.notice("Đã trả lại lượt Tế đàn hôm nay. Lệnh: /tedan vao | info | diet [chừa n] | tang N"
                  + " | het [giây] | ket"));
         }
         case "vao" -> join(session, d); // = bấm "Tham gia" (không cần tới Buraen)
         case "info" -> session.send(Packets.notice("Tế đàn: hôm nay " + (d.altarDay == today() ? "đã đi" : "chưa đi")
               + ", lượt " + (active ? "đang chạy còn " + duration(st.endAt() - now)
               : "không có") + ", đã diệt hết " + st.cleared + " tầng (rương cấp "
               + st.cleared + "), map " + st.map + ", Ấn linh " + (st.sealed ? "có" : "không") + "."));
         case "diet" -> {
            int keep = args.length > 2 ? parse(args[2], 0) : 0;
            world.testKillAltarMobs(session, keep);
         }
         case "tang" -> {
            int stage = args.length > 2 ? Math.max(1, Math.min(AltarRules.STAGES, parse(args[2], 1))) : 1;
            if (!active) {
               session.send(Packets.notice("Chưa có lượt đang chạy (/tedan vao)."));
               return;
            }
            st.cleared = Math.max(st.cleared, stage - 1);
            world.enterAltar(session, AltarRules.mapOf(stage));
            session.send(Packets.notice("Tới tầng " + stage + ", coi như đã diệt hết " + st.cleared + " tầng."));
         }
         case "het" -> {
            int seconds = args.length > 2 ? Math.max(1, parse(args[2], 10)) : 10;
            if (!active) {
               session.send(Packets.notice("Chưa có lượt đang chạy (/tedan vao)."));
               return;
            }
            st.end = now + seconds * 1000L;
            scheduleEnd(session, d, st);
            world.refreshAltarTimer(session);
            session.send(Packets.notice("Lượt Tế đàn còn " + seconds + " giây."));
         }
         case "ket" -> {
            if (!active) {
               session.send(Packets.notice("Chưa có lượt đang chạy (/tedan vao)."));
               return;
            }
            finish(session, d, "Đã kết thúc lượt Tế đàn (lệnh test). Phần thưởng đã gửi qua thư.");
         }
         default -> session.send(Packets.notice("Lệnh: /tedan [reset] | vao | info | diet [chừa n] | tang N | het [giây]"
               + " | ket"));
      }
   }

   private static int parse(String text, int fallback) {
      try {
         return Integer.parseInt(text);
      } catch (NumberFormatException e) {
         return fallback;
      }
   }

   // ================================================================== nội bộ

   private void refreshStats(PlayerData d) {
      ItemService.refreshLook(d);
      world.broadcastStats(d);
   }

   private String mapName(AltarRun st) {
      var map = world.mapTemplate(AltarRules.isAltarMap(st.map) ? st.map : AltarRules.FIRST_MAP);
      return map == null ? "Tế đàn" : map.name();
   }

   private static long today() {
      return LocalDate.now(ZONE).toEpochDay();
   }

   /** "12 phút 5 giây". */
   static String duration(long ms) {
      long seconds = Math.max(0, (ms + 999) / 1000);
      return seconds >= 60 ? seconds / 60 + " phút " + seconds % 60 + " giây" : seconds + " giây";
   }
}
