package com.tlnt.server.chat;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.Leveling;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.player.SkillData;
import com.tlnt.server.plant.PlantService;
import com.tlnt.server.forge.SetTitles;
import com.tlnt.server.forge.UpgradeRules;
import com.tlnt.server.player.ItemData;
import java.util.List;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.QuestService;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;
import com.tlnt.server.world.WorldPlayer;

import java.util.Locale;

/**
 * Chat khu (op 26 sub 0) + lệnh test. Chạy lane GAME, tuần tự theo session -> sửa HP / MP / hồi chiêu của
 * {@link PlayerData} an toàn (như CombatService, ItemService).
 *
 * <p>Tin bắt đầu bằng "/" khi bật {@code chat.testCommands} là LỆNH: chỉ người gõ thấy kết quả (thông báo nổi), không
 * phát cho khu. Tắt thì "/..." là tin chat thường.
 */
public final class ChatService {

   /** Tin dài hơn bị cắt (ô chat client không giới hạn). */
   static final int MAX_LENGTH = 120;
   /** Tối thiểu giữa 2 tin của 1 người (chống spam). */
   private static final long MIN_INTERVAL_MS = 500;
   /** {@code /cd}: báo client kỹ năng đã dùng từ 1 ngày trước -> hết hồi chiêu ngay. */
   private static final long COOLDOWN_RESET_ELAPSED_MS = 86_400_000L;
   /** {@code /lt}: lam thạch cộng thêm mỗi lần (thử shop lam thạch khi chưa có nạp). */
   private static final long TEST_DIAMOND = 1_000_000L;
   private static final String HELP = "Lệnh test: /hp hồi đầy HP, /mp hồi đầy MP, /cd xoá hồi chiêu, /full cả ba, /lt +1.000.000 lam thạch, /lv lên 1 cấp,"
         + " /cay xem cây trong khu, /caymoc cây mọc lại ngay, /caytoi tới cạnh 1 cây, /caydau cây đang ở map / khu nào, /qua nhận 5 món mỗi loại hái / săn được, /chet tự hạ gục (thử cỏ hồi sinh), /theluc N đặt thể lực (trống = đầy),"
         + " /ong xem tổ ong / quả dừa, /ongmoc mọc ngay trong khu, /ongtoi tới cạnh tổ ong / quả dừa,"
         + " /da đá + bùa + 2 tỷ vàng để nâng cấp, /nhatan N hoặc /nguyetan N đặt cấp N cho 8 món đang mặc,"
         + " /pet nhận 7 thú cưng (cất vào bộ sưu tập rồi chọn để thú đi theo), /xu nhận 100 Đồng xu may mắn, /item <id> [số lượng] [k] nhận item bất kỳ (k = khoá), /nv N [bước] nhảy tới nhiệm vụ chính tuyến N,"
         + " /nvinfo xem nhiệm vụ đang làm, /nvc [n] cộng n tiến độ bước đang làm (mặc định 1), /nvxong xong luôn bước"
         + " đang làm (bước cuối = nhận thưởng), /tedan lệnh Tế đàn, /loivuc lệnh Lôi vực Sấm sét, /giaidau lệnh Giải đấu Bộ lạc, /linhmoc lệnh Rừng Linh Mộc, /boss lệnh boss thế giới, /cao mini boss Cáo, /bl lệnh bộ lạc, /tt lệnh thành tựu, /hn lệnh nhiệm vụ hằng ngày, /tinhanh /thulinh /quaithuong đổi quái gần nhất.";

   /** {@code /xu}: Đồng xu may mắn — tốn khi quay vòng quay (wheel/LuckyWheelService). */
   private static final int TEST_LUCKY_COIN = 307;

   /** {@code /pet}: các món thú cưng có con thú đi theo (char_template_id trong item_appearance). */
   private static final int[] TEST_PETS = {289, 290, 291, 328, 330, 332, 333};

   private final World world;
   private final PlantService plants;
   private final boolean testCommands;
   /** Chat thế giới tốn Tù và ({@code item/UsableItems}); null = chưa gắn (test) -> bỏ qua chat thế giới. */
   private final com.tlnt.server.item.UsableItems usables;

   public ChatService(World world, PlantService plants, boolean testCommands,
         com.tlnt.server.item.UsableItems usables) {
      this.world = world;
      this.plants = plants;
      this.testCommands = testCommands;
      this.usables = usables;
   }

   public void say(PlayerSession session, String raw) {
      PlayerData d = session.getPlayer();
      String text = clean(raw);
      if (d == null || !d.inWorld || text.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastChatAt < MIN_INTERVAL_MS) {
         return;
      }
      d.lastChatAt = now;
      if (text.startsWith("/") && (testCommands || staff(session))) { // GM / quản trị viên: luôn có lệnh test
         command(session, d, text.substring(1).toLowerCase(Locale.ROOT));
         return;
      }
      Log.debug("[chat] '%s': %s", d.name, text);
      world.chat(session, text);
   }

   private static boolean staff(PlayerSession session) {
      com.tlnt.server.account.Account account = session.getAccount();
      return account != null && account.isStaff();
   }

   /**
    * Op 26 sub 3: chat tab "Bộ lạc" -> mọi thành viên ĐANG ONLINE của bộ lạc (kể cả người gửi, client không tự thêm tin
    * của mình). Chung chống spam với chat khu. Lệnh "/..." không chạy ở kênh này.
    */
   public void tribe(PlayerSession session, String raw) {
      PlayerData d = session.getPlayer();
      String text = clean(raw);
      if (d == null || !d.inWorld || text.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastChatAt < MIN_INTERVAL_MS) {
         return;
      }
      d.lastChatAt = now;
      com.tlnt.server.tribe.TribeService.chat(session, d, text);
   }

   /** Op 26 sub 2 (tab "Tổ đội"): chat tổ đội — {@code PartyService.chat}. */
   public void party(PlayerSession session, String raw) {
      PlayerData d = session.getPlayer();
      String text = clean(raw);
      if (d == null || !d.inWorld || text.isEmpty() || !allow(d)) {
         return;
      }
      com.tlnt.server.party.PartyService.chat(session, d, text);
   }

   /**
    * Op 26 sub 4 (tab "Riêng", client {@code sendWhisper}): str tên người nhận, str nội dung. Người nhận phải online
    * (tên không phân biệt hoa thường). Cả 2 bên nhận S→C 26/4: int id đối phương, str tên đối phương, byte char_template
    * đối phương (ảnh đại diện), int cấp đối phương, int id người gửi, str tên người gửi, str nội dung — client gom tin vào
    * cuộc trò chuyện theo id đối phương.
    */
   public void whisper(PlayerSession session, String toName, String raw) {
      PlayerData d = session.getPlayer();
      String text = clean(raw);
      String name = toName == null ? "" : toName.trim();
      if (d == null || !d.inWorld || text.isEmpty() || name.isEmpty()) {
         return;
      }
      if (name.equalsIgnoreCase(d.name)) {
         session.send(Packets.notice("Không thể nhắn tin cho chính mình."));
         return;
      }
      if (!allow(d)) {
         return;
      }
      WorldPlayer target = null;
      for (WorldPlayer p : world.onlinePlayers()) {
         if (p.data().name.equalsIgnoreCase(name)) {
            target = p;
            break;
         }
      }
      if (target == null) {
         session.send(Packets.notice(name + " không trực tuyến."));
         return;
      }
      PlayerData t = target.data();
      target.send(whisperPacket(d, d, text)); // người nhận: đối phương = người gửi
      session.send(whisperPacket(t, d, text)); // người gửi: đối phương = người nhận
      Log.debug("[chat rieng] '%s' -> '%s': %s", d.name, t.name, text);
   }

   static Message whisperPacket(PlayerData partner, PlayerData sender, String text) {
      Message msg = new Message(Opcode.CHAT);
      msg.writeByte(4);
      msg.writeInt(partner.getId());
      msg.writeString(partner.name);
      msg.writeByte(com.tlnt.server.player.Gender.charTemplateId(com.tlnt.server.player.Gender.isValid(partner.gender)
            ? partner.gender : com.tlnt.server.player.Gender.RINN));
      msg.writeInt(partner.level);
      msg.writeInt(sender.getId());
      msg.writeString(sender.name);
      msg.writeString(text);
      return msg;
   }

   /** Chống spam chung mọi kênh: tối thiểu {@link #MIN_INTERVAL_MS} giữa 2 tin. */
   private static boolean allow(PlayerData d) {
      long now = System.currentTimeMillis();
      if (now - d.lastChatAt < MIN_INTERVAL_MS) {
         return false;
      }
      d.lastChatAt = now;
      return true;
   }

   /**
    * Op 26 sub 1 (tab "Thế giới") / op 70 kênh 1 (hộp nhập khi dùng Tù và): chat thế giới — chung chống spam với chat
    * khu, điều kiện + tốn Tù và ở {@code UsableItems.worldChat}. Lệnh "/..." không chạy ở kênh này.
    */
   public void world(PlayerSession session, String raw) {
      PlayerData d = session.getPlayer();
      String text = clean(raw);
      if (d == null || !d.inWorld || text.isEmpty() || usables == null) {
         return;
      }
      long now = System.currentTimeMillis();
      if (now - d.lastChatAt < MIN_INTERVAL_MS) {
         return;
      }
      d.lastChatAt = now;
      usables.worldChat(session, d, text);
   }

   /** Bỏ khoảng trắng 2 đầu + ký tự điều khiển (xuống dòng...), cắt còn {@value #MAX_LENGTH} ký tự. */
   static String clean(String raw) {
      if (raw == null) {
         return "";
      }
      StringBuilder out = new StringBuilder(Math.min(raw.length(), MAX_LENGTH));
      for (int i = 0; i < raw.length() && out.length() < MAX_LENGTH; i++) {
         char c = raw.charAt(i);
         out.append(Character.isISOControl(c) ? ' ' : c);
      }
      return out.toString().trim();
   }

   // ------------------------------------------------------------------ lệnh test

   private void command(PlayerSession session, PlayerData d, String text) {
      PlayerStats stats = d.stats;
      String[] args = text.trim().split("\\s+");
      String name = args[0];
      switch (name) {
         case "hp" -> {
            d.setHpMp(stats.maxHp, d.mp);
            sendHpMp(session, d, stats, "Đã hồi đầy HP.");
         }
         case "mp" -> {
            d.setHpMp(d.hp, stats.maxMp);
            sendHpMp(session, d, stats, "Đã hồi đầy MP.");
         }
         case "cd" -> {
            resetCooldowns(session, d);
            session.send(Packets.notice("Đã xoá thời gian hồi chiêu."));
         }
         case "full" -> {
            d.setHpMp(stats.maxHp, stats.maxMp);
            resetCooldowns(session, d);
            sendHpMp(session, d, stats, "Đã hồi đầy HP, MP và xoá hồi chiêu.");
         }
         case "lt" -> {
            synchronized (d) {
               d.diamond = Math.min(ItemService.MAX_GOLD, d.diamond + TEST_DIAMOND);
            }
            session.send(ItemPackets.currency(d));
            session.send(Packets.notice("Đã cộng 1.000.000 lam thạch."));
         }
         case "lv" -> {
            int before = d.level;
            if (Leveling.addExp(d, Math.max(1, Leveling.expToLevelUp(d))) == 0) {
               session.send(Packets.notice("Đã đạt cấp tối đa."));
               break;
            }
            ItemService.refreshLook(d); // chỉ số gốc theo cấp
            world.broadcastStats(d); // cấp / EXP / điểm tiềm năng (op 23 sub 0)
            QuestEvents.fire(session, StepKind.REACH_LEVEL, 0);
            com.tlnt.server.achievement.AchievementEvents.add(session, com.tlnt.server.achievement.AchievementType.LEVEL, d.level);
            session.send(Packets.notice("Lên cấp " + d.level + ": +" + Leveling.talentPointsAt(d.level)
                  + " điểm tiềm năng (từ cấp " + before + ")."));
         }
         case "cay" -> world.plantInfo(session);
         case "caymoc" -> world.plantRespawnNow(session);
         case "caytoi" -> world.teleportToPlant(session);
         case "caydau" -> world.plantWhere(session);
         case "qua" -> plants.giveTestItems(session);
         case "ong" -> world.gatherInfo(session);
         case "ongmoc" -> world.gatherSpawnNow(session);
         case "ongtoi" -> world.teleportToGather(session);
         case "chet" -> {
            if (d.isDead()) {
               break;
            }
            d.setHpMp(0, d.mp);
            world.broadcastAround(session, ItemPackets.hpMp(d.getId(), 0, stats.maxHp, d.mp, stats.maxMp).toPacket());
            session.send(WorldPackets.deathMessage("Lệnh test: bạn đã tự hạ gục."));
         }
         case "da" -> giveUpgradeKit(session, d);
         case "pet" -> givePets(session, d);
         case "item" -> giveAnyItem(session, d, args); // /item <id> [số lượng] [k = khoá]
         case "xu" -> giveTestItem(session, d, ItemData.of(TEST_LUCKY_COIN, 100), "100 Đồng xu may mắn (thử vòng quay)");
         case "nhatan", "nguyetan" -> setWornStars(session, d, name.equals("nhatan") ? UpgradeRules.GRADE_SUN
               : UpgradeRules.GRADE_MOON, args.length > 1 ? parse(args[1], 3) : 3);
         case "tedan" -> com.tlnt.server.altar.AltarService.testCommand(session, d, args); // Tế đàn Thiên tích
         case "loivuc" -> com.tlnt.server.thunder.ThunderService.testCommand(session, d, args); // Lôi vực Sấm sét
         case "giaidau" -> com.tlnt.server.tournament.TournamentService.testCommand(session, d, args); // Giải đấu Bộ lạc
         case "linhmoc" -> com.tlnt.server.linhmoc.LinhMocService.testCommand(session, d, args); // Rừng Linh Mộc
         case "boss" -> com.tlnt.server.worldboss.WorldBossService.testCommand(session, d, args); // boss thế giới
         case "cao" -> com.tlnt.server.worldboss.FoxService.testCommand(session, d, args); // mini boss Tiểu Tinh linh Cáo
         case "bl" -> com.tlnt.server.tribe.TribeService.testCommand(session, d, args); // bộ lạc
         case "tt" -> com.tlnt.server.achievement.AchievementService.testCommand(session, d, args); // thành tựu
         case "hn" -> com.tlnt.server.daily.DailyQuestService.testCommand(session, d, args); // nhiệm vụ hằng ngày
         case "tinhanh" -> world.testSetMobVariant(session, com.tlnt.server.combat.MobRules.VARIANT_ELITE); // quái gần nhất
         case "thulinh" -> world.testSetMobVariant(session, com.tlnt.server.combat.MobRules.VARIANT_BOSS);
         case "quaithuong" -> world.testSetMobVariant(session, com.tlnt.server.combat.MobRules.VARIANT_NORMAL);
         case "theluc" -> { // thử "Thể lực đã cạn" (/theluc 0) / hồi đầy (/theluc)
            int value = Math.max(0, Math.min(stats.maxStamina, args.length > 1 ? parse(args[1], stats.maxStamina)
                  : stats.maxStamina));
            synchronized (d) {
               d.activityPoints = value;
            }
            session.send(WorldPackets.stamina(value, stats.maxStamina));
         }
         case "nv" -> { // nhảy nhiệm vụ chính tuyến: /nv <nhiệm vụ> [bước]
            QuestService quests = QuestEvents.service();
            if (quests != null && args.length > 1) {
               quests.jump(session, parse(args[1], 1), args.length > 2 ? parse(args[2], 1) : 1);
            } else {
               session.send(Packets.notice("Cách dùng: /nv <nhiệm vụ> [bước], vd /nv 3 2"));
            }
         }
         case "nvinfo" -> { // nhiệm vụ / bước / tiến độ đang làm
            QuestService quests = QuestEvents.service();
            if (quests != null) {
               quests.testInfo(session);
            }
         }
         case "nvc" -> { // cộng tiến độ bước đang làm: /nvc [n]
            QuestService quests = QuestEvents.service();
            if (quests != null) {
               quests.testAdd(session, args.length > 1 ? Math.max(1, parse(args[1], 1)) : 1);
            }
         }
         case "nvxong" -> { // xong luôn bước đang làm (bước cuối: nhận thưởng, sang nhiệm vụ kế)
            QuestService quests = QuestEvents.service();
            if (quests != null) {
               quests.testComplete(session);
            }
         }
         case "help" -> session.send(Packets.notice(HELP));
         default -> session.send(Packets.notice("Không có lệnh /" + name + ". " + HELP));
      }
      Log.debug("[chat] '%s' dung lenh /%s", d.name, name);
   }

   /** /da: đá nâng cấp + bùa bảo hiểm + vàng để thử lò rèn. */
   private static void giveUpgradeKit(PlayerSession session, PlayerData d) {
      long now = System.currentTimeMillis();
      List<ItemData> kit = List.of(ItemData.of(UpgradeRules.SUN_STONE, 500_000), ItemData.of(UpgradeRules.MOON_STONE, 500_000),
            ItemData.of(UpgradeRules.INSURANCE_1, 20), ItemData.of(UpgradeRules.INSURANCE_2, 20),
            ItemData.of(com.tlnt.server.forge.RefineService.ORIGIN_STONE, 1_000));
      String error;
      synchronized (d) {
         error = d.trade != null ? "Đang giao dịch." : ItemService.give(d, kit, now);
         if (error == null) {
            d.gold = Math.min(ItemService.MAX_GOLD, d.gold + 2_000_000_000L);
         }
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      session.send(ItemPackets.currency(d));
      session.send(ItemPackets.container(ItemPackets.SUB_BAG, d.bag));
      session.send(ItemPackets.quickSlots(d.quickSlots));
      session.send(Packets.notice("Đã nhận 500.000 Đá Mặt trời, 500.000 Đá Mặt trăng, 20 bùa bảo hiểm mỗi cấp, 1.000 Đá khởi nguyên, +2 tỷ vàng."));
   }

   /**
    * /pet: 7 món thú cưng vào hành trang (cần 7 ô trống). Cất vào bộ sưu tập "Thú cưng" rồi chọn 1 con -> con thú đi
    * theo; món nào chưa có dòng item_appearance thì vẫn cất được nhưng không có thú đi theo.
    */
   private static void givePets(PlayerSession session, PlayerData d) {
      long now = System.currentTimeMillis();
      List<ItemData> pets = new java.util.ArrayList<>(TEST_PETS.length);
      for (int templateId : TEST_PETS) {
         pets.add(ItemData.of(templateId, 1));
      }
      String error;
      synchronized (d) {
         error = d.trade != null ? "Đang giao dịch." : ItemService.give(d, pets, now);
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      session.send(ItemPackets.container(ItemPackets.SUB_BAG, d.bag));
      session.send(ItemPackets.quickSlots(d.quickSlots));
      session.send(Packets.notice("Đã nhận 7 thú cưng. Cất vào bộ sưu tập Thú cưng rồi chọn 1 con để thú đi theo."));
   }

   /** Lệnh test nhận 1 món vào hành trang. */
   /**
    * {@code /item <id> [số lượng] [k]}: nhận bất kỳ item nào (item_template.id) — số lượng mặc định 1, "k" = khoá. Món không
    * cộng dồn được thì nhận từng món riêng (tối đa 50 món). Vàng (1) / lam thạch (2) cộng thẳng vào tiền.
    */
   private static void giveAnyItem(PlayerSession session, PlayerData d, String[] args) {
      int id = args.length > 1 ? parse(args[1], 0) : 0;
      com.tlnt.server.data.model.ItemPack.ItemTemplate template = com.tlnt.server.data.GameDataCache.itemTemplate(id);
      if (template == null) {
         session.send(Packets.notice("Lệnh: /item <id> [số lượng] [k = khoá]. Không có item id " + id + "."));
         return;
      }
      int quantity = Math.max(1, args.length > 2 ? parse(args[2], 1) : 1);
      boolean locked = args.length > 3 && args[3].equalsIgnoreCase("k");
      List<ItemData> list = new java.util.ArrayList<>();
      ItemData one = ItemData.of(id, quantity);
      one.locked = locked;
      one.createdTime = System.currentTimeMillis();
      list.add(one);
      String error;
      synchronized (d) {
         if (d.trade != null) {
            error = "Đang giao dịch.";
         } else {
            error = ItemService.give(d, list, System.currentTimeMillis());
            if (error != null && quantity > 1 && quantity <= 50) { // không cộng dồn: thử từng món riêng
               list.clear();
               for (int i = 0; i < quantity; i++) {
                  ItemData each = ItemData.of(id, 1);
                  each.locked = locked;
                  each.createdTime = one.createdTime;
                  list.add(each);
               }
               error = ItemService.give(d, list, System.currentTimeMillis());
            }
         }
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      session.send(ItemPackets.container(ItemPackets.SUB_BAG, d.bag));
      session.send(ItemPackets.currency(d));
      session.send(ItemPackets.quickSlots(d.quickSlots));
      session.send(Packets.notice("Đã nhận " + quantity + " " + template.name() + (locked ? " (khoá)" : "") + "."));
   }

   private static void giveTestItem(PlayerSession session, PlayerData d, ItemData item, String what) {
      String error;
      synchronized (d) {
         error = d.trade != null ? "Đang giao dịch." : ItemService.give(d, List.of(item), System.currentTimeMillis());
      }
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      session.send(ItemPackets.container(ItemPackets.SUB_BAG, d.bag));
      session.send(ItemPackets.quickSlots(d.quickSlots));
      session.send(Packets.notice("Đã nhận " + what + "."));
   }

   /** /nhatan N, /nguyetan N: đặt hệ + cấp N cho mọi đồ đang mặc ô 0..7 (thử hiệu ứng bộ). */
   private void setWornStars(PlayerSession session, PlayerData d, byte grade, int level) {
      int n = Math.max(0, Math.min(UpgradeRules.MAX_LEVEL, level));
      int changed = 0;
      for (int slot = 0; slot < SetTitles.SET_PIECES; slot++) {
         ItemData item = d.body.get(slot);
         if (item != null) {
            item.starGrade = n == 0 ? UpgradeRules.GRADE_NONE : grade;
            item.starCount = (byte) n;
            changed++;
         }
      }
      ItemService.refreshLook(d);
      session.send(ItemPackets.container(ItemPackets.SUB_BODY, d.body));
      world.broadcastStats(d);
      session.send(Packets.notice("Đã đặt " + changed + "/8 món đang mặc thành " + (grade == UpgradeRules.GRADE_SUN
            ? "Nhật ấn" : "Nguyệt ấn") + " cấp " + n + ". Hiệu ứng bộ: " + (d.titleId > 0 ? "title " + d.titleId
            : "chưa kích hoạt (cần đủ 8 món)") + "."));
   }

   private static int parse(String s, int fallback) {
      try {
         return Integer.parseInt(s);
      } catch (NumberFormatException e) {
         return fallback;
      }
   }

   private static void sendHpMp(PlayerSession session, PlayerData d, PlayerStats stats, String notice) {
      session.send(ItemPackets.hpMp(d.getId(), d.hp, stats.maxHp, d.mp, stats.maxMp));
      session.send(Packets.notice(notice));
   }

   /** Server: dùng được ngay; client: op 21 sub 4 từng kỹ năng với "đã dùng từ 1 ngày trước". */
   private static void resetCooldowns(PlayerSession session, PlayerData d) {
      if (d.skills == null) {
         return;
      }
      for (SkillData skill : d.skills) {
         if (skill != null) {
            skill.lastUsedAt = 0;
            session.send(WorldPackets.skillCooldown(skill.skillId, COOLDOWN_RESET_ELAPSED_MS));
         }
      }
   }
}
