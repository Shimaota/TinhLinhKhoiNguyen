package com.tlnt.server;

import com.tlnt.server.account.AccountRepository;
import com.tlnt.server.account.AccountService;
import com.tlnt.server.account.LoginGuard;
import com.tlnt.server.admin.ConsoleCommands;
import com.tlnt.server.admin.ShutdownService;
import com.tlnt.server.captcha.CaptchaService;
import com.tlnt.server.combat.CombatService;
import com.tlnt.server.combat.MobRules;
import com.tlnt.server.config.ServerConfig;
import com.tlnt.server.data.DataVersionService;
import com.tlnt.server.data.GameDataService;
import com.tlnt.server.data.sql.GameDataRepository;
import com.tlnt.server.db.Database;
import com.tlnt.server.combo.ComboRepository;
import com.tlnt.server.combo.ComboService;
import com.tlnt.server.altar.AltarService;
import com.tlnt.server.linhmoc.LinhMocRules;
import com.tlnt.server.linhmoc.LinhMocService;
import com.tlnt.server.farm.AppleTreeService;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.QuestService;
import com.tlnt.server.wheel.LuckyWheelRepository;
import com.tlnt.server.wheel.LuckyWheelService;
import com.tlnt.server.worldboss.WorldBossRepository;
import com.tlnt.server.worldboss.WorldBossService;
import com.tlnt.server.giftcode.GiftcodeRepository;
import com.tlnt.server.giftcode.GiftcodeService;
import com.tlnt.server.fishing.FishingRepository;
import com.tlnt.server.fishing.FishingService;
import com.tlnt.server.item.GroundItemService;
import com.tlnt.server.item.ItemBuffs;
import com.tlnt.server.gather.GatherRepository;
import com.tlnt.server.gather.GatherService;
import com.tlnt.server.plant.PlantRepository;
import com.tlnt.server.plant.PlantService;
import com.tlnt.server.handler.Routes;
import com.tlnt.server.item.ItemExpiryTask;
import com.tlnt.server.item.UsableItems;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.mail.MailRepository;
import com.tlnt.server.mail.MailService;
import com.tlnt.server.market.MarketRepository;
import com.tlnt.server.market.MarketService;
import com.tlnt.server.map.MapRepository;
import com.tlnt.server.net.PacketDispatcher;
import com.tlnt.server.net.transport.NetServer;
import com.tlnt.server.net.transport.kryonet.KryoNetServer;
import com.tlnt.server.net.transport.netty.NettyServer;
import com.tlnt.server.npc.NpcService;
import com.tlnt.server.party.PartyService;
import com.tlnt.server.player.PlayerRepository;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.ranking.RankingRepository;
import com.tlnt.server.ranking.RankingService;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.shop.ShopRepository;
import com.tlnt.server.shop.ShopService;
import com.tlnt.server.session.SessionManager;
import com.tlnt.server.social.SocialService;
import com.tlnt.server.trade.TradeService;
import com.tlnt.server.tribe.TribeService;
import com.tlnt.server.achievement.AchievementRepository;
import com.tlnt.server.achievement.AchievementService;
import com.tlnt.server.daily.DailyQuestService;
import com.tlnt.server.daily.DailyRepository;
import com.tlnt.server.util.Log;
import com.tlnt.server.welfare.WelfareRepository;
import com.tlnt.server.welfare.WelfareService;
import com.tlnt.server.world.World;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Điểm khởi động: CHỈ tạo và nối các thành phần, không chứa logic game. Đọc {@code TLNT-Server/CLAUDE.md} trước khi
 * sửa server.
 *
 * <pre>
 * client ──TCP──> NetServer (Netty, nhiều luồng I/O)
 *                   └─> SessionManager ──> PacketDispatcher ──> handler (Routes)
 *                                           ├─ IO       : chạy ngay trên luồng mạng (ping, di chuyển)
 *                                           ├─ GAME     : pool game, tuần tự theo session
 *                                           └─ BLOCKING : pool blocking (SQL, băm mật khẩu)
 * World ── TickEngine (world.tickThreads luồng) ── Zone.tick ~30 lần/giây: lệnh, di chuyển
 * PlayerService ── db-writer (1 luồng) ── SQL tlnt_server
 * </pre>
 */
public final class GameServer {

   public static void main(String[] args) throws Exception {
      long startedAt = System.currentTimeMillis();
      System.setProperty("java.awt.headless", "true"); // vẽ captcha bằng AWT trên máy không có màn hình

      ServerConfig config = ServerConfig.load();
      Log.setLevel(config.getLogLevel());
      Log.setColor(config.getLogColor());
      DataVersionService dataVersionService = new DataVersionService(config.getDataGameDir(), config.getHttpBaseUrl());
      MobRules.setExpRate(config.getExpRate());
      MobRules.setExpFormula(config.getExpField(), config.getExpPenaltyStep(), config.getExpPenaltyMin(),
            config.getExpDungeon());
      MobRules.setMobAttack(config.isMobAttack());
      MobRules.setRespawnSeconds(config.getMobRespawnSeconds());
      LinhMocRules.configure(config.getLinhMoc()); // trước World: GameMap / MapPackets đọc dãy map phó bản

      Database database = new Database(config);
      Database playerDatabase = new Database(config, true);
      GameDataService gameData = new GameDataService(new GameDataRepository(database));
      gameData.reloadCache();
      com.tlnt.server.player.LevelTable.reload(database); // bảng EXP lên cấp (tlnt_data.level_exp) -> cấp tối đa
      // Công tắc sự kiện (tlnt_server.server_state) — trước mọi thứ đọc cấu hình có "event" (cây, boss, NPC map)
      com.tlnt.server.event.GameEvents.load(new com.tlnt.server.event.EventRepository(playerDatabase));

      // Pool game: việc nhẹ, số nhân CPU. Pool blocking: SQL / PBKDF2 / captcha, không làm nghẽn pool game.
      ExecutorService gamePool = Executors.newFixedThreadPool(config.getGameThreads(), named("game"));
      ExecutorService blockingPool = Executors.newFixedThreadPool(config.getBlockingThreads(), named("blocking"));
      // Việc hẹn giờ nhẹ: đóng kết nối trễ khi kick, in thống kê.
      ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(named("scheduler"));

      // Map: SQL (map_template) -> World; các khu tick trên TickEngine.
      World world = new World(new MapRepository(database).loadAll(), config.getStartMapId(), config.getTickThreads(),
            config.getWorldTickMillis());
      World.setMoveBroadcastMillis(config.getMoveBroadcastMillis());

      PlayerService playerService = new PlayerService(new PlayerRepository(playerDatabase), world, config.getStartMapId());
      AccountService accountService = new AccountService(new AccountRepository(playerDatabase), playerService,
            new CaptchaService(), scheduler, TimeUnit.SECONDS.toMillis(config.getReloginDelaySeconds()), gamePool,
            new LoginGuard(config.getMaxConnectionsPerIp(), config.getLoginsPerIpPerMinute(),
                  config.getCaptchasPerIpPerMinute(), config.getFailsPerAccount(), config.getAccountLockMinutes(),
                  config.isExemptLoopback()));
      // Dọn bộ đếm chống dò mật khẩu + mục chờ đăng nhập lại đã hết hạn.
      scheduler.scheduleAtFixedRate(accountService::cleanup, 60, 60, TimeUnit.SECONDS);

      PacketDispatcher dispatcher = new PacketDispatcher(gamePool, blockingPool);
      ItemService itemService = new ItemService(world);
      CombatService combatService = new CombatService(world, scheduler, gamePool);
      TradeService tradeService = new TradeService(world, itemService, playerService, gamePool);
      SocialService socialService = new SocialService(world, gamePool);
      PartyService partyService = new PartyService(world, gamePool);
      ShopService shopService = new ShopService(new ShopRepository(database), itemService, world);
      NpcService npcService = new NpcService(world, shopService);
      QuestEvents.install(new QuestService(world, itemService, playerService, gamePool)); // nhiệm vụ chính tuyến
      // item dùng có logic riêng: hộp quà, bùa dịch chuyển, mở rộng túi, tẩy điểm, Tù và, thẻ đổi tên... (item/UsableItems)
      itemService.setUsables(new UsableItems(world, itemService, playerService, gamePool));
      AppleTreeService appleTreeService = new AppleTreeService(world, itemService, playerService);
      npcService.register(AppleTreeService.NPC_ID, appleTreeService);
      MailService mailService = new MailService(new MailRepository(playerDatabase), playerService, itemService, gamePool);
      // Tế đàn Thiên tích (phó bản map 34-40) + NPC 17 Tư tế Buraen (thay hộp thoại mặc định ở NpcService)
      AltarService altarService = new AltarService(world, itemService, playerService, mailService, shopService,
            scheduler, gamePool);
      npcService.register(AltarService.NPC_BURAEN, altarService);
      world.setAltarHooks(altarService);
      // Lôi vực Sấm sét (chiến trường PK bộ lạc map 31-33, cũng ở NPC Buraen) + boss = dòng world_boss map 31-33
      com.tlnt.server.thunder.ThunderService thunderService = new com.tlnt.server.thunder.ThunderService(world,
            itemService, playerService, scheduler, gamePool);
      altarService.setThunder(thunderService);
      world.setThunderHooks(thunderService);
      com.tlnt.server.tournament.TournamentService tournamentService = new com.tlnt.server.tournament.TournamentService(
            world, itemService, playerService, mailService, partyService, scheduler);
      altarService.setTournament(tournamentService);
      world.setArenaHooks(tournamentService);
      scheduler.scheduleAtFixedRate(tournamentService::tick, 1, 1, TimeUnit.SECONDS);
      // Rừng Linh Mộc (phó bản tổ đội, linhmoc.maps) + NPC Bác Rano
      LinhMocService linhMocService = new LinhMocService(world, playerService, partyService, mailService, scheduler,
            gamePool);
      npcService.register(LinhMocRules.config().npcId(), linhMocService);
      world.setLinhMocHooks(linhMocService);
      // Boss thế giới (tlnt_data.world_boss): 1 con / boss cả server, hẹn giờ xuất hiện + thông báo toàn server
      WorldBossService worldBossService = new WorldBossService(world, new WorldBossRepository(database), scheduler);
      worldBossService.reload();
      scheduler.scheduleAtFixedRate(worldBossService::tick, 1, 1, TimeUnit.SECONDS);
      // Mini boss Tiểu Tinh linh Cáo: 5 con cả server, trộm vàng khoá (worldboss/FoxService)
      com.tlnt.server.worldboss.FoxService foxService = new com.tlnt.server.worldboss.FoxService(world);
      scheduler.scheduleAtFixedRate(foxService::tick, 1, 1, TimeUnit.SECONDS);
      // Sự kiện Trung thu (event/): NPC 21 Nguyệt linh thần, bánh, đèn hoa đăng, cà rốt + Ngọc thố, boss world_boss 11
      com.tlnt.server.event.MidAutumnService midAutumnService = new com.tlnt.server.event.MidAutumnService(world,
            itemService, shopService, gamePool);
      npcService.register(com.tlnt.server.event.MidAutumn.NPC, midAutumnService);
      com.tlnt.server.event.RabbitService rabbitService = new com.tlnt.server.event.RabbitService(world);
      scheduler.scheduleAtFixedRate(rabbitService::tick, 1, 1, TimeUnit.SECONDS);
      MarketService marketService = new MarketService(new MarketRepository(playerDatabase), playerService, mailService,
            itemService);
      WelfareRepository welfareRepository = new WelfareRepository(database);
      welfareRepository.reload(); // bảng quà tlnt_data.welfare_reward
      WelfareService welfareService = new WelfareService(itemService, gamePool);
      GiftcodeService giftcodeService = new GiftcodeService(new GiftcodeRepository(database, playerDatabase),
            playerService, itemService);
      giftcodeService.setWorld(world); // admin đổi mã hiện ở danh sách -> gửi lại cho người đang online
      RankingService rankingService = new RankingService(new RankingRepository(playerDatabase, database), playerService,
            mailService);
      FishingService fishingService = new FishingService(new FishingRepository(database), world, itemService, scheduler,
            gamePool);
      PlantService plantService = new PlantService(new PlantRepository(database), world, itemService, gamePool);
      // Bật / tắt sự kiện: nạp lại cây (cà rốt có "event"), tắt thì gỡ cây đang mọc, bật thì hiện NPC sự kiện
      com.tlnt.server.event.GameEvents.onChange((e, on) -> {
         plantService.reload();
         if (e == com.tlnt.server.event.GameEvents.Event.MID_AUTUMN) {
            if (on) {
               world.showNpc(2, com.tlnt.server.event.MidAutumn.NPC);
            } else {
               world.removePlants(com.tlnt.server.event.MidAutumn.CARROT_PLANT);
            }
         }
      });
      GatherService gatherService = new GatherService(new GatherRepository(database));
      GroundItemService groundItemService = new GroundItemService(world, itemService, gamePool);
      // Gói combo + vòng quay (tab của cửa sổ ShopWindow): dữ liệu tlnt_data.combo / lucky_wheel
      ComboService comboService = new ComboService(new ComboRepository(database), playerService, itemService);
      LuckyWheelService luckyWheelService = new LuckyWheelService(new LuckyWheelRepository(database), playerService,
            itemService);
      // Bộ lạc (op 37): mọi bộ lạc nạp vào RAM, bảng tlnt_server.tribe; Cửa hàng Bộ lạc = shop tribe.shopId
      TribeService tribeService = TribeService.create(config.getTribe(), playerDatabase, world, playerService,
            itemService, gamePool);
      shopService.setTribeShop(tribeService);
      // Thành tựu (op 62): bảng tlnt_data.achievement (dump game gốc), tiến độ cột player.achievement
      AchievementService achievementService = new AchievementService(new AchievementRepository(database),
            playerService, itemService, gamePool, config.getAchievementGuardianBosses(),
            config.getAchievementSpiritBosses());
      // Nhiệm vụ hằng ngày (op 69): cấu hình tlnt_data.daily_quest, tiến độ cột player.daily_quest
      DailyQuestService dailyQuestService = new DailyQuestService(new DailyRepository(database), playerService,
            itemService, world, gamePool);
      scheduler.scheduleAtFixedRate(tribeService::tick, TribeService.TICK_SECONDS, TribeService.TICK_SECONDS,
            TimeUnit.SECONDS);
      // Vào game (trước khi vào khu): nạp hòm thư, tính lại phúc lợi -> gói trạng thái đầu đã có dữ liệu.
      playerService.onLogin(mailService::onLogin);
      // Đơn nạp thẻ web (tlnt_server.card_charge): cộng lam thạch lúc vào game, TRƯỚC phúc lợi (mốc Quà nạp dùng tổng nạp mới)
      com.tlnt.server.recharge.WebCardTopupService webTopup = new com.tlnt.server.recharge.WebCardTopupService(
            playerDatabase, playerService);
      blockingPool.execute(webTopup::warnStuck);
      playerService.onLogin(webTopup::onLogin);
      playerService.onLogin(welfareService::onLogin);
      playerService.onLogin(altarService::onLogin); // sau hòm thư: lượt Tế đàn hết giờ lúc offline -> thư thưởng
      playerService.onLogin(tribeService::onLogin); // gắn buff + tên bộ lạc trước khi vào khu
      playerService.onLogin(com.tlnt.server.social.PkPoints::onLogin); // sát khí giảm trong lúc offline
      // Huy hiệu Tinh Linh (item 123 / 360): nút Tiện ích mở shop / rương / lò rèn / cây táo từ xa (op 45)
      com.tlnt.server.badge.SpiritBadgeService spiritBadge = new com.tlnt.server.badge.SpiritBadgeService(shopService,
            appleTreeService);
      playerService.onLogin(spiritBadge::onLogin);
      playerService.onLogin(achievementService::onLogin); // danh sách thành tựu + bù cấp / số sao cũ
      playerService.onLogin(dailyQuestService::onLogin); // qua ngày -> giao NV mới, gửi bảng (chấm đỏ)
      // Cẩm nang: quà hiện ở từng mục (cột tlnt_data.guide_topic.rewards, op 61)
      com.tlnt.server.handbook.HandbookService handbookService = new com.tlnt.server.handbook.HandbookService(database);
      handbookService.reload();
      playerService.onLogin(handbookService::onLogin);
      // Cửa sổ "Nạp tiền" (op 72): danh sách gói + mẫu link trang nạp ở tlnt_data.recharge
      com.tlnt.server.recharge.RechargeService rechargeService = new com.tlnt.server.recharge.RechargeService(database);
      rechargeService.reload();
      playerService.onLogin(rechargeService::onLogin);
      // Thoát game: huỷ giao dịch + lời mời TRƯỚC khi chụp dữ liệu lưu (chống nhân bản đồ).
      playerService.onLogout(tradeService::onLogout);
      playerService.onLogout(socialService::onLogout);
      playerService.onLogout(partyService::onLogout);
      playerService.onLogout(tribeService::onLogout);
      playerService.onLogout(mailService::onLogout);
      playerService.onLogout(welfareService::onLogout); // cộng thời gian online trước khi chụp dữ liệu lưu
      playerService.onLogout(rankingService::onLogout);
      Routes.registerAll(dispatcher, config, dataVersionService, accountService, playerService, world, itemService,
            combatService, tradeService, socialService, partyService, npcService, mailService, marketService,
            welfareService, giftcodeService, rankingService, shopService, fishingService, plantService, groundItemService,
            comboService, luckyWheelService, tribeService, achievementService, dailyQuestService,
            new com.tlnt.server.farm.PigService(itemService, playerService, scheduler, gamePool), spiritBadge);
      Routes.registerRecharge(dispatcher, rechargeService);
      // Bảng Boss thế giới (op 100, Huy hiệu Tinh Linh): danh sách + dịch chuyển (lam thạch) / tìm đường (vàng)
      Routes.registerWorldBoss(dispatcher, new com.tlnt.server.worldboss.WorldBossList(worldBossService, world,
            playerService, gamePool));
      // Luyện tập Offline (Huy hiệu Tinh Linh): giữ nhân vật trong khu sau khi thoát, server tự đánh quái.
      com.tlnt.server.offline.OfflineTrainingService offlineTraining = new com.tlnt.server.offline.OfflineTrainingService(
            world, combatService, itemService, accountService, scheduler, gamePool);
      Routes.registerOfflineTraining(dispatcher, offlineTraining);
      playerService.onLogin(offlineTraining::onLogin); // câu tổng kết lượt treo đã dừng

      SessionManager sessions = new SessionManager(dispatcher, accountService, gamePool);
      offlineTraining.setSessions(sessions);
      // Thiết lập tự động khi ĐANG ONLINE: tự dùng thuốc HP / MP / thể lực + tự hồi sinh (client chỉ gửi cài đặt op 46).
      scheduler.scheduleAtFixedRate(new com.tlnt.server.auto.OnlineAutoTask(sessions, world, itemService, gamePool),
            com.tlnt.server.auto.OnlineAutoTask.INTERVAL_MS, com.tlnt.server.auto.OnlineAutoTask.INTERVAL_MS,
            TimeUnit.MILLISECONDS);
      // Item hết hạn: dọn định kỳ cho người đang online (lúc vào game đã dọn trong PlayerService).
      scheduler.scheduleAtFixedRate(new ItemExpiryTask(sessions, itemService, gamePool), ItemExpiryTask.INTERVAL_SECONDS,
            ItemExpiryTask.INTERVAL_SECONDS, TimeUnit.SECONDS);
      // Kí gửi hết hạn -> trả đồ về thư; phúc lợi: cập nhật số phút online / qua ngày.
      scheduler.scheduleAtFixedRate(marketService::expireTick, MarketService.EXPIRE_CHECK_SECONDS,
            MarketService.EXPIRE_CHECK_SECONDS, TimeUnit.SECONDS);
      // Rải cây / tổ ong / quả dừa ngẫu nhiên toàn server (world/SpawnDirector), kể cả khu không có người.
      scheduler.scheduleAtFixedRate(world::runSpawns, 2, 1, TimeUnit.SECONDS);
      // Hiệu ứng vật phẩm (Nấm hương: hồi HP / MP mỗi 5 giây, hết hạn thì gỡ).
      ItemBuffs itemBuffs = new ItemBuffs(world, sessions, gamePool);
      com.tlnt.server.item.GearCharges.install(world, gamePool); // phụ kiện có lượt: vỡ -> gỡ trên luồng session
      scheduler.scheduleAtFixedRate(itemBuffs::tick, ItemBuffs.REGEN_INTERVAL_SECONDS, ItemBuffs.REGEN_INTERVAL_SECONDS,
            TimeUnit.SECONDS);
      // Người chơi giả (admin panel tab "Bot người chơi"; cấu hình ở server_state "bot_config", mặc định TẮT).
      com.tlnt.server.bot.BotService botService = new com.tlnt.server.bot.BotService(world, combatService, itemService,
            itemBuffs, playerDatabase, database, scheduler, gamePool, blockingPool);
      botService.connect(partyService, fishingService, worldBossService);
      scheduler.scheduleAtFixedRate(() -> welfareService.tickAll(sessions.snapshot()), WelfareService.TICK_SECONDS,
            WelfareService.TICK_SECONDS, TimeUnit.SECONDS);
      // SQL không chạy trên luồng scheduler: đẩy sang pool blocking. Admin sửa quà phúc lợi / giftcode / shop trong tlnt_data
      // -> tối đa 1 phút sau có hiệu lực, không cần khởi động lại.
      Runnable reloadAll = () -> { // SQL: chỉ chạy trên pool blocking / luồng admin panel
         welfareRepository.reload();
         giftcodeService.reload();
         shopService.reload();
         comboService.reload();
         luckyWheelService.reload();
         fishingService.reload();
         plantService.reload();
         gatherService.reload();
         worldBossService.reload();
         achievementService.reload();
         dailyQuestService.reload();
         com.tlnt.server.player.LevelTable.reload(database);
         handbookService.reload();
         rechargeService.reload();
         gameData.reloadSmall(); // server_list + item_appearance
      };
      scheduler.scheduleAtFixedRate(() -> blockingPool.execute(reloadAll), 60, 60, TimeUnit.SECONDS);
      // Lịch tự bật / tắt sự kiện (admin panel tab Sự kiện) — SQL server_state: chạy trên pool blocking.
      scheduler.scheduleAtFixedRate(() -> blockingPool.execute(() -> com.tlnt.server.event.GameEvents.tickSchedule(
            System.currentTimeMillis())), 30, 30, TimeUnit.SECONDS);
      scheduler.scheduleAtFixedRate(() -> blockingPool.execute(rankingService::refresh),
            RankingService.REFRESH_SECONDS, RankingService.REFRESH_SECONDS, TimeUnit.SECONDS);
      // Top tuần: nạp lại quà (tlnt_data.weekly_top), sang tuần mới -> thư thưởng top 1-3 tuần trước.
      scheduler.scheduleAtFixedRate(() -> blockingPool.execute(rankingService::checkWeekly), 60, 60, TimeUnit.SECONDS);
      NetServer net = "kryonet".equals(config.getTransport())
            ? new KryoNetServer(sessions, config.getKryoNetWriteBufferBytes(), config.getKryoNetObjectBufferBytes())
            : new NettyServer(sessions, config.getIoThreads(), config.getMaxFrameBytes(),
                  config.getMaxPendingWriteBytes(), config.getWsPort());
      try {
         net.start(config.getTcpPort());
      } catch (java.net.BindException e) {
         // Không thoát hẳn thì luồng Netty / scheduler giữ JVM sống -> tiến trình treo, chiếm DB.
         Log.error("Cong TCP %d dang bi chiem (server khac dang chay?) - dung khoi dong", config.getTcpPort());
         Log.flush(2000);
         System.exit(1);
      }

      int statsSeconds = config.getStatsIntervalSeconds();
      if (statsSeconds > 0) {
         scheduler.scheduleAtFixedRate(() -> logStats(net, sessions, accountService, world), statsSeconds,
               statsSeconds, TimeUnit.SECONDS);
      }

      Log.banner("TLNT Game Server - SAN SANG", new String[][] {
            {"Cong TCP", config.getTcpPort() + " (" + net.name() + ")"},
            {"Luong", "game " + config.getGameThreads() + " | blocking " + config.getBlockingThreads() + " | tick "
                  + config.getTickThreads()},
            {"DataGame", config.getHttpBaseUrl()},
            {"Luu nhan vat", "khi thoat / tat server (go stop)"},
            {"Khoi dong", (System.currentTimeMillis() - startedAt) + " ms"},
      });

      // Tắt server: đá dần dần + chờ lưu HẾT (ShutdownService). Gõ "stop" ở console, hoặc Ctrl+C (shutdown hook).
      // Nút Stop NetBeans / đóng cửa sổ giết tiến trình ngay -> không kịp lưu, đừng dùng khi có người chơi.
      ShutdownService shutdown = new ShutdownService(accountService, playerService, sessions,
            config.getShutdownKickPerSecond(), () -> {
               net.stop(); // ngắt kết nối còn lại -> xếp lượt đăng xuất lên pool game
               for (PlayerSession session : sessions.snapshot()) { // phòng hờ kết nối chưa kịp báo ngắt
                  accountService.logout(session);
               }
               shutdownPool(gamePool);
               shutdownPool(blockingPool);
               scheduler.shutdownNow();
               playerService.shutdown(); // lưu nốt người còn online, chờ ghi SQL xong (không giới hạn thời gian)
               world.shutdown();
               database.close();
               playerDatabase.close();
            });
      Runtime.getRuntime().addShutdownHook(new Thread(shutdown::shutdown, "shutdown-hook"));
      new ConsoleCommands(shutdown, () -> "[online] ket noi " + net.connectionCount() + ", dang nhap "
            + accountService.onlineCount() + " | " + world.stats() + " | cho ghi DB " + playerService.pendingWrites()
            + " luot").start();
      Log.info("Go 'stop' de tat server an toan (luu het du lieu), 'help' de xem lenh.");

      // Admin panel web (chỉ 127.0.0.1): tổng quan, tài khoản, người chơi, thông báo / bảo trì, sự kiện, giftcode, log
      if (config.getAdminPort() > 0) {
         com.tlnt.server.admin.web.AdminApi adminApi = new com.tlnt.server.admin.web.AdminApi(
               new com.tlnt.server.admin.web.AdminApi.Deps(world, accountService, playerService, mailService,
                     itemService, rankingService, tribeService, marketService, config, tournamentService,
                     worldBossService, foxService, achievementService, dailyQuestService, giftcodeService,
                     database, playerDatabase,
                     net::connectionCount, sessions::activeCount, reloadAll, () -> shutdown, scheduler, gamePool));
         try {
            new com.tlnt.server.admin.web.AdminServer(config.getAdminPort(), config.getAdminPassword(), adminApi,
                  config.getAdminAssetsDir())
                  .start();
            Log.info("Admin panel: http://127.0.0.1:%d (chi mo tren may chu)", config.getAdminPort());
         } catch (java.io.IOException e) {
            Log.warn("Khong mo duoc admin panel cong %d: %s", config.getAdminPort(), e.getMessage());
         }
      }
   }

   private static void logStats(NetServer net, SessionManager sessions, AccountService accounts, World world) {
      Runtime rt = Runtime.getRuntime();
      long usedMb = (rt.totalMemory() - rt.freeMemory()) >> 20;
      Log.info("[stats] ket noi %d, phien %d, dang nhap %d | %s | RAM %d/%d MB", net.connectionCount(),
            sessions.activeCount(), accounts.onlineCount(), world.stats(), usedMb, rt.maxMemory() >> 20);
   }

   private static void shutdownPool(ExecutorService pool) {
      pool.shutdown();
      try {
         if (!pool.awaitTermination(5, TimeUnit.SECONDS)) {
            pool.shutdownNow();
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
      }
   }

   private static ThreadFactory named(String prefix) {
      AtomicInteger number = new AtomicInteger();
      return r -> {
         Thread t = new Thread(r, prefix + "-" + number.incrementAndGet());
         t.setDaemon(true);
         return t;
      };
   }

   private GameServer() {
   }
}
