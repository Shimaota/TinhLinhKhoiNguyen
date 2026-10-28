package com.tlnt.server.handler;

import com.tlnt.server.account.AccountService;
import com.tlnt.server.chat.ChatService;
import com.tlnt.server.combat.CombatService;
import com.tlnt.server.config.ServerConfig;
import com.tlnt.server.data.DataVersionService;
import com.tlnt.server.fishing.FishingService;
import com.tlnt.server.plant.PlantService;
import com.tlnt.server.forge.BrewService;
import com.tlnt.server.forge.CraftService;
import com.tlnt.server.forge.RefineService;
import com.tlnt.server.forge.SoulService;
import com.tlnt.server.forge.UpgradeService;
import com.tlnt.server.forge.WeaponCraftService;
import com.tlnt.server.item.GroundItemService;
import com.tlnt.server.combo.ComboService;
import com.tlnt.server.giftcode.GiftcodeService;
import com.tlnt.server.wheel.LuckyWheelService;
import com.tlnt.server.item.CollectionKind;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.mail.MailService;
import com.tlnt.server.market.MarketService;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.PacketDispatcher;
import com.tlnt.server.net.Packets;
import com.tlnt.server.npc.NpcService;
import com.tlnt.server.party.PartyService;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.ranking.RankingService;
import com.tlnt.server.shop.ShopService;
import com.tlnt.server.skill.SkillService;
import com.tlnt.server.social.SocialService;
import com.tlnt.server.trade.TradeService;
import com.tlnt.server.tribe.TribeService;
import com.tlnt.server.achievement.AchievementService;
import com.tlnt.server.daily.DailyQuestService;
import com.tlnt.server.welfare.WelfareService;
import com.tlnt.server.world.World;

import static com.tlnt.server.net.PacketDispatcher.Access.AUTHENTICATED;
import static com.tlnt.server.net.PacketDispatcher.Access.PUBLIC;
import static com.tlnt.server.net.PacketDispatcher.Lane.BLOCKING;
import static com.tlnt.server.net.PacketDispatcher.Lane.GAME;
import static com.tlnt.server.net.PacketDispatcher.Lane.IO;

/**
 * NƠI DUY NHẤT đăng ký opcode C→S -> handler. Thêm tính năng mới: viết handler trong package này, thêm hằng vào
 * {@link Opcode}, rồi thêm 1 dòng ở đây. Chọn lane:
 * <ul>
 *   <li>{@code IO}: cực nhẹ, không khoá, không SQL (ping, di chuyển).</li>
 *   <li>{@code GAME}: logic game bình thường (đa số handler). Không được gọi SQL trực tiếp.</li>
 *   <li>{@code BLOCKING}: có SQL / băm mật khẩu / việc nặng CPU.</li>
 * </ul>
 */
public final class Routes {

   private Routes() {
   }

   /** Op 72 cửa sổ "Nạp tiền" (client mới — recharge/). */
   public static void registerOfflineTraining(PacketDispatcher d, com.tlnt.server.offline.OfflineTrainingService offline) {
      d.register(Opcode.OFFLINE_TRAINING, GAME, AUTHENTICATED, new OfflineTrainingHandler(offline)); // treo offline
   }

   /** Op 100 bảng Boss thế giới (worldboss/WorldBossList). */
   public static void registerWorldBoss(PacketDispatcher d, com.tlnt.server.worldboss.WorldBossList list) {
      d.register(Opcode.WORLD_BOSS, GAME, AUTHENTICATED, new WorldBossHandler(list));
   }

   public static void registerRecharge(PacketDispatcher d, com.tlnt.server.recharge.RechargeService recharge) {
      d.register(Opcode.RECHARGE, GAME, AUTHENTICATED, new RechargeHandler(recharge)); // gửi link trang nạp
   }

   public static void registerAll(PacketDispatcher d, ServerConfig config, DataVersionService dataVersions,
         AccountService accounts, PlayerService players, World world, ItemService items, CombatService combat,
         TradeService trades, SocialService social, PartyService parties, NpcService npcs, MailService mails,
         MarketService market, WelfareService welfare, GiftcodeService giftcodes, RankingService ranking,
         ShopService shops, FishingService fishing, PlantService plants,
         GroundItemService ground, ComboService combos, LuckyWheelService wheel, TribeService tribes,
         AchievementService achievements, DailyQuestService daily, com.tlnt.server.farm.PigService pigs,
         com.tlnt.server.badge.SpiritBadgeService badge) {
      // ---- trước khi đăng nhập
      d.register(Opcode.HANDSHAKE, GAME, PUBLIC, new HandshakeHandler(dataVersions, config.getHttpBaseUrl(),
            config.getTextureDecryptKey(), config.getAtlasDecryptKey()));
      d.register(Opcode.ACCOUNT, BLOCKING, PUBLIC, new AuthHandler(accounts));
      d.register(Opcode.CAPTCHA, BLOCKING, PUBLIC, new CaptchaHandler(accounts));
      d.register(Opcode.PING, IO, PUBLIC, new PingHandler());

      // ---- nhân vật
      d.register(Opcode.CREATE_CHARACTER, BLOCKING, AUTHENTICATED, new CreateCharacterHandler(players));
      d.register(Opcode.SETTINGS, GAME, AUTHENTICATED, new SettingsHandler());
      d.register(Opcode.PLAYER, GAME, AUTHENTICATED, new PlayerActionHandler(world, social));

      // ---- item: túi / rương / đồ mặc / bộ sưu tập (logic ở item/ItemService)
      d.register(Opcode.ITEM, GAME, AUTHENTICATED, new ItemHandler(items, tribes));
      d.register(Opcode.SKILL, GAME, AUTHENTICATED, new SkillHandler(new SkillService(world), combat));
      d.register(Opcode.CONFIRM, GAME, AUTHENTICATED, new ConfirmHandler(items, trades, social, parties, tribes,
            daily));
      d.register(Opcode.INVENTORY, GAME, AUTHENTICATED, new InventoryHandler(items));
      for (CollectionKind kind : CollectionKind.values()) {
         d.register(kind.opcode, GAME, AUTHENTICATED, new CollectionHandler(items, kind));
      }

      // ---- thế giới
      // Di chuyển gửi mỗi frame: chỉ ghi vị trí mới nhất (atomic), tick của khu tự kiểm tra và phát.
      d.register(Opcode.MOVE, IO, AUTHENTICATED, new MoveHandler(world));
      d.register(Opcode.WAYPOINT, GAME, AUTHENTICATED, new WaypointHandler(world));
      d.register(Opcode.NPC, GAME, AUTHENTICATED, new NpcHandler(npcs));
      d.register(Opcode.ZONE, GAME, AUTHENTICATED, new ZoneHandler(world, items.usables()));
      ChatService chat = new ChatService(world, plants, config.isChatTestCommands(), items.usables());
      d.register(Opcode.CHAT, GAME, AUTHENTICATED, new ChatHandler(chat));
      // hộp nhập chữ (Tù và / Thẻ đổi tên): đổi tên có SQL
      d.register(Opcode.INPUT, BLOCKING, AUTHENTICATED, new InputHandler(chat, items.usables()));
      d.register(Opcode.PLANT, GAME, AUTHENTICATED, new PlantHandler(plants)); // hái cây (plant/)
      d.register(Opcode.GROUND_ITEM, GAME, AUTHENTICATED, new PickupHandler(ground)); // nhặt đồ dưới đất
      d.register(Opcode.FORGE, GAME, AUTHENTICATED, new ForgeHandler(new UpgradeService(items),
            new RefineService(items), new SoulService(items), new BrewService(items), new CraftService(items),
            new com.tlnt.server.forge.ElementService(items),
            new com.tlnt.server.forge.TransmuteService(items))); // lò rèn (forge/)
      d.register(Opcode.WEAPON_CRAFT, GAME, AUTHENTICATED,
            new WeaponCraftHandler(new WeaponCraftService(items))); // rèn vũ khí bước nhiệm vụ 5.4

      // ---- người chơi với nhau: bạn bè, trạng thái PK, thách đấu (social/), giao dịch (trade/)
      d.register(Opcode.FRIEND, GAME, AUTHENTICATED, new FriendHandler(social));
      d.register(Opcode.PK, GAME, AUTHENTICATED, new PkHandler(social));
      d.register(Opcode.DUEL, GAME, AUTHENTICATED, new DuelHandler(social));
      d.register(Opcode.TRADE, GAME, AUTHENTICATED, new TradeHandler(trades));
      d.register(Opcode.PARTY, GAME, AUTHENTICATED, new PartyHandler(parties));
      d.register(Opcode.TRIBE, GAME, AUTHENTICATED, new TribeHandler(tribes)); // bộ lạc (tribe/)
      d.register(Opcode.ACHIEVEMENT, GAME, AUTHENTICATED, new AchievementHandler(achievements)); // thành tựu
      d.register(Opcode.DAILY_QUEST, GAME, AUTHENTICATED, new DailyQuestHandler(daily)); // nhiệm vụ hằng ngày (daily/)
      d.register(Opcode.FARM, GAME, AUTHENTICATED, new FarmHandler(pigs)); // nông trại: thu hoạch heo (farm/PigService)
      d.register(Opcode.UTILITY, GAME, AUTHENTICATED, new UtilityHandler(badge)); // Huy hiệu Tinh Linh: nút Tiện ích
      d.register(Opcode.REVIVE, GAME, AUTHENTICATED, new ReviveHandler(world, items));

      // ---- hòm thư (mail/), kí gửi (market/), phúc lợi (welfare/)
      d.register(Opcode.MAIL, BLOCKING, AUTHENTICATED, new MailHandler(mails)); // gửi thư tra tên người nhận bằng SQL
      d.register(Opcode.MARKET, GAME, AUTHENTICATED, new MarketHandler(market));
      d.register(Opcode.WELFARE, GAME, AUTHENTICATED, new WelfareHandler(welfare, giftcodes));
      d.register(Opcode.RANKING, BLOCKING, AUTHENTICATED, new RankingHandler(ranking));
      d.register(Opcode.WEEKLY_TOP, BLOCKING, AUTHENTICATED, new WeeklyTopHandler(ranking));
      d.register(Opcode.SHOP, GAME, AUTHENTICATED, new ShopHandler(shops));
      d.register(Opcode.FISHING, GAME, AUTHENTICATED, new FishingHandler(fishing)); // câu cá (fishing/)
      d.register(Opcode.COMBO, GAME, AUTHENTICATED, new ComboHandler(combos)); // gói combo (combo/)
      d.register(Opcode.LUCKY_WHEEL, GAME, AUTHENTICATED, new LuckyWheelHandler(wheel)); // vòng quay (wheel/)
      registerProtection(d);
   }

   /**
    * Tài khoản đang được bảo vệ (MK cấp 2 > "Khóa"): chặn mọi thao tác làm MẤT / CHUYỂN / TIÊU tài sản của nhân vật.
    * Vẫn được: đi, đánh, nhặt, mặc / tháo, cất / lấy rương, dùng ô thuốc nhanh HUD (27/9 — tự đánh cần), chat, tổ đội,
    * nhận thư / quà, mở cửa sổ. Byte so sánh = sub (byte 0), riêng lò rèn op 44 = thao tác (byte 1, sau byte chế độ).
    */
   public static void registerProtection(PacketDispatcher d) {
      d.protect(Opcode.ITEM, 0, 2, 3, 13, 14, 15); // dùng / vứt / bán / Rương bộ lạc "Sử dụng" / "Phân phát"
      d.protect(Opcode.SKILL, 0, 2, 3); // cộng tiềm năng / nâng kỹ năng (tốn điểm, vàng)
      d.protect(Opcode.CONFIRM, 0, Packets.CONFIRM_TRADE, Packets.CONFIRM_EXPAND_COLLECTION, Packets.CONFIRM_USE_BUFF,
            Packets.CONFIRM_SELL, Packets.CONFIRM_TRIBE_TRANSFER, Packets.CONFIRM_TRIBE_DISBAND,
            Packets.CONFIRM_TRIBE_DISBAND_FINAL, Packets.CONFIRM_TRIBE_EMBLEM, Packets.CONFIRM_RESET_POINTS,
            Packets.CONFIRM_PIG_GROW, Packets.CONFIRM_BOSS_TELEPORT, Packets.CONFIRM_BOSS_PATH);
      for (CollectionKind kind : CollectionKind.values()) {
         d.protect(kind.opcode, 0, 2); // "Mở rộng" bộ sưu tập (10 triệu vàng)
      }
      d.protect(Opcode.TRADE, 0, 0, 1, 2); // mời / khoá / đồng ý giao dịch (3 = huỷ vẫn được)
      d.protect(Opcode.MARKET, 0, 1, 2, 3); // kí gửi: bán / mua / gỡ xuống
      d.protect(Opcode.MAIL, 0, 1); // gửi thư (kèm đồ + phí)
      d.protect(Opcode.SHOP, 0, 0); // mua ở shop
      d.protect(Opcode.COMBO, 0, 1); // mua gói combo
      d.protect(Opcode.LUCKY_WHEEL, 0, 1); // quay vòng quay
      d.protect(Opcode.FORGE, 1, 2, 3); // lò rèn: thực hiện (nâng / khảm / pha / rèn...) + gỡ khảm / gạt tuỳ chọn
      d.protect(Opcode.WEAPON_CRAFT, -1); // Chế tác vũ khí (cả opcode)
      d.protect(Opcode.INPUT, 0, Packets.INPUT_RENAME); // đổi tên (Thẻ đổi tên)
      d.protect(Opcode.TRIBE, 0, 1, 5, 6, 7, 8, 13); // tạo / rời / giải tán / góp / nhường Tù trưởng
      d.protect(Opcode.FARM, 0, 1); // "Lớn nhanh" (lam thạch)
   }
}
