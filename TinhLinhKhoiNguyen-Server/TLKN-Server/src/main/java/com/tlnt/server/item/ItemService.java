package com.tlnt.server.item;

import com.tlnt.server.farm.AppleTreeService;
import com.tlnt.server.forge.UpgradeService;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.fishing.FishingPackets;
import com.tlnt.server.fishing.FishingService;
import com.tlnt.server.net.Packets;
import com.tlnt.server.npc.NpcService;
import com.tlnt.server.player.ClassSkills;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.Leveling;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerSettings;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;
import com.tlnt.server.world.WorldPlayer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Toàn bộ logic item của 1 người: hành trang (bag), rương (box), đồ mặc (body), đeo lưng, 4 bộ sưu tập, ô dùng nhanh,
 * dùng item. Gọi từ handler lane GAME -> chạy tuần tự trên hàng đợi của session, nên túi đồ của 1 người không bao giờ
 * bị 2 luồng sửa cùng lúc (không cần khoá). Luồng tick khu chỉ đọc {@link PlayerData#stats} /
 * {@link PlayerData#appearance} (tính lại ở {@link #sync} rồi công bố volatile).
 *
 * Mỗi thao tác gom các phần đã đổi vào 1 bitmask rồi {@link #sync} gửi đúng những danh sách đó 1 lần.
 * Luật: đồ mặc lên người / cất vào bộ sưu tập mà chưa khoá thì thành KHOÁ.
 */
public final class ItemService {

   // ---- bitmask phần cần gửi lại
   static final int BAG = 1;
   static final int BOX = 1 << 1;
   private static final int BODY = 1 << 2;
   private static final int QUICK = 1 << 3;
   /** Tính lại chỉ số + ngoại hình, phát khối chỉ số (op 23 sub 0) cho cả khu. */
   static final int LOOK = 1 << 4;
   private static final int SETTINGS = 1 << 5;
   /** + ordinal: gửi lại danh sách bộ sưu tập (op 22 sub 4/6/8/9/10) + chỉ số cộng thêm. Bit 8..15. */
   private static final int LIST = 1 << 8;
   /** + ordinal: chỉ đổi món đang dùng (op 48/50/64/66/78 sub 1) + chỉ số cộng thêm. Bit 16..23. */
   private static final int SELECTED = 1 << 16;

   /**
    * Số hiệu ô dùng nhanh client gửi (HudSkillButton3 = 3 HP, MpPotionSlotButton = 4 MP, HudSkillButton1 = 1 thể lực)
    * -> index trong {@link PlayerData#quickSlots} (client vẽ: quickSlot1 = HP, quickSlot2 = MP, quickSlot3 = thể lực).
    */
   private static final int[] QUICK_NUMBER_TO_INDEX = {-1, 2, 3, 0, 1};

   private static final long SORT_COOLDOWN_MS = 1_000;

   private final World world;
   /** Item dùng có logic riêng (hộp quà, bùa, thẻ...) — gắn lúc khởi động; null trong test = như chưa có. */
   private UsableItems usables;

   public ItemService(World world) {
      this.world = world;
   }

   public void setUsables(UsableItems usables) {
      this.usables = usables;
   }

   public UsableItems usables() {
      return usables;
   }

   // ================================================================== hành trang (op 27)

   /**
    * Sub 0 "Trang bị" (và mọi nút "Trang bị" khác trong túi): đồ mặc thường vào ô {@code equip_slot}, đeo lưng
    * (48) vào ô lưng, ngoại trang/danh hiệu/mặt nạ/thú cưng vào bộ sưu tập. Đồ cũ ở ô đó về đúng ô túi vừa trống.
    */
   public void equip(PlayerSession session, int bagSlot) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null) {
         return;
      }
      ItemPack.ItemType type = item.type();
      if (type == null || !type.equippable()) {
         session.send(Packets.notice("Vật phẩm này không thể trang bị."));
         return;
      }
      if (gearLocked(session, d)) {
         return;
      }
      CollectionKind kind = CollectionKind.forItemType(type.id());
      if (kind != null) {
         putInCollection(session, kind, bagSlot);
         return;
      }
      String error = checkRequirement(d, item);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      int equipSlot = type.equipSlot();
      if (!d.body.valid(equipSlot)) {
         session.send(Packets.notice("Vật phẩm này chưa thể trang bị."));
         return;
      }
      int weaponClass = equipSlot == Appearance.SLOT_WEAPON ? WeaponClass.of(item) : WeaponClass.NONE;
      if (weaponClass != WeaponClass.NONE && d.classId != weaponClass) {
         if (d.classId != WeaponClass.NONE) {
            session.send(Packets.notice("Bạn chỉ sử dụng được vũ khí loại " + WeaponClass.name(d.classId) + "."));
            return;
         }
         // Lần đầu cầm vũ khí: hỏi trước, "Đồng ý" -> confirmFirstWeapon.
         d.pendingWeaponSlot = bagSlot;
         d.pendingWeaponTemplateId = item.templateId;
         session.send(Packets.confirm(Packets.CONFIRM_FIRST_WEAPON, "Đây là lần đầu tiên bạn sử dụng vũ khí, nó sẽ"
               + " quyết định loại vũ khí của bạn sau này.\nChọn loại vũ khí: " + WeaponClass.name(weaponClass) + "?"));
         return;
      }
      wear(session, d, bagSlot, item, equipSlot);
   }

   /**
    * Opcode 32 "Đồng ý" ở hộp hỏi lần đầu dùng vũ khí: class = loại vũ khí đó (vĩnh viễn) rồi mặc vào.
    * Món ở ô túi phải còn đúng món lúc hỏi (tránh đổi đồ giữa chừng).
    */
   public void confirmFirstWeapon(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null || d.pendingWeaponSlot < 0) {
         return;
      }
      int bagSlot = d.pendingWeaponSlot;
      ItemData item = d.bag.get(bagSlot);
      boolean same = item != null && item.templateId == d.pendingWeaponTemplateId;
      d.pendingWeaponSlot = -1;
      d.pendingWeaponTemplateId = -1;
      if (gearLocked(session, d)) { // hỏi lúc ở ngoài, bấm "Đồng ý" khi đã vào phó bản
         return;
      }
      int weaponClass = same ? WeaponClass.of(item) : WeaponClass.NONE;
      if (weaponClass == WeaponClass.NONE || d.classId != WeaponClass.NONE || checkRequirement(d, item) != null) {
         return;
      }
      d.classId = weaponClass; // gửi cho client trong khối chỉ số (LOOK)
      ClassSkills.apply(d); // nhận kỹ năng của class ở cấp 0 (chưa nâng cấp), "Đánh Thường" giữ cấp 1
      session.send(WorldPackets.learnedSkills(d.skills));
      session.send(WorldPackets.skillBar(d.skillBar));
      session.send(Packets.notice("Bạn đã chọn loại vũ khí: " + WeaponClass.name(weaponClass) + "."));
      wear(session, d, bagSlot, item, Appearance.SLOT_WEAPON);
   }

   /** Mặc {@code item} (ô túi {@code bagSlot}) vào ô {@code equipSlot}; đồ cũ về đúng ô túi đó. */
   private void wear(PlayerSession session, PlayerData d, int bagSlot, ItemData item, int equipSlot) {
      item.locked = true;
      ItemData old = d.body.get(equipSlot);
      d.body.set(equipSlot, item);
      d.bag.set(bagSlot, old);
      sync(session, d, BAG | BODY | LOOK);
      QuestEvents.fire(session, StepKind.EQUIP, 0);
      if (equipSlot == Appearance.SLOT_WEAPON) {
         QuestEvents.fire(session, StepKind.EQUIP_WEAPON, 0);
      }
   }

   /** Sub 1 "Tháo ra" (bảng đồ mặc): đồ ở ô {@code equipSlot} về ô trống đầu tiên của túi. */
   public void unequip(PlayerSession session, int equipSlot) {
      PlayerData d = player(session);
      if (d == null || d.body.get(equipSlot) == null || gearLocked(session, d)) {
         return;
      }
      int empty = d.bag.firstEmpty();
      if (empty < 0) {
         session.send(Packets.notice("Cần ít nhất 1 ô trong hành trang"));
         return;
      }
      d.bag.set(empty, d.body.take(equipSlot));
      sync(session, d, BAG | BODY | LOOK);
   }

   /**
    * Trong phó bản ({@link PlayerData#isInDungeon}) không được thay đổi trang bị: mặc / tháo / cất trang bị vào rương /
    * bộ sưu tập. true = bị chặn (đã báo client).
    */
   private static boolean gearLocked(PlayerSession session, PlayerData d) {
      if (!d.isInDungeon()) {
         return false;
      }
      session.send(Packets.notice(com.tlnt.server.linhmoc.LinhMocRules.config().text("gearLocked")));
      return true;
   }

   /** Sub 2 "Sử dụng" / "Dùng nhiều" (đồ không trang bị được). */
   public void use(PlayerSession session, int bagSlot, int quantity) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null || quantity <= 0) {
         return;
      }
      ItemPack.ItemType type = item.type();
      if (type == null) {
         return;
      }
      long nowMs = System.currentTimeMillis();
      ItemBuffs.Effect cleanse = ItemBuffs.forItem(item.templateId);
      boolean isCleanse = cleanse != null && cleanse.special() == ItemBuffs.Special.CLEANSE; // Túi sữa dê: gỡ được cả khi dính
      if (!isCleanse && (nowMs < d.frozenUntil || nowMs < d.stunnedUntil)) { // đóng băng / choáng: không thao tác được
         session.send(Packets.notice(nowMs < d.frozenUntil ? "Bị đóng băng, không thể thực hiện thao tác."
               : "Bị choáng, không thể thực hiện thao tác."));
         return;
      }
      if (type.equippable()) {
         equip(session, bagSlot);
         return;
      }
      if (ItemTypes.isRecovery(type.id())) {
         useRecovery(session, d, bagSlot, item, quantity);
         return;
      }
      if (type.id() == ItemTypes.EXP_PER_LEVEL) {
         useExpItem(session, d, bagSlot, item, quantity);
         return;
      }
      if (item.templateId == REVIVE_HERB_TEMPLATE_ID) {
         useReviveHerb(session, d, bagSlot, item);
         return;
      }
      ItemBuffs.Effect effect = ItemBuffs.forItem(item.templateId);
      if (effect != null) {
         useBuff(session, d, bagSlot, item, effect, quantity);
         return;
      }
      if (usables != null && usables.use(session, d, bagSlot, item, quantity)) {
         return;
      }
      session.send(Packets.notice("Vật phẩm này chưa thể sử dụng."));
   }

   /**
    * Loại 49 "Kinh nghiệm x level" (theo mô tả item): EXP gốc mỗi món × cấp nhân vật + giới hạn số món / ngày (0 =
    * không giới hạn). Quả mọng đỏ / tím hái từ cây ({@code plant/}).
    */
   private record ExpItem(long baseExp, int dailyLimit) {
   }

   private static final Map<Integer, ExpItem> EXP_ITEMS = Map.of(
         147, new ExpItem(1_000, 0), // Quả mọng đỏ
         148, new ExpItem(2_500, 0), // Quả mọng tím
         355, new ExpItem(5_000, 0)); // Ống tre kinh nghiệm

   private static final Locale VI = Locale.forLanguageTag("vi-VN");

   /** Dùng / "Dùng nhiều" item EXP: tối đa số còn được dùng hôm nay; lên cấp thì phát hiệu ứng lên cấp cho cả khu. */
   private void useExpItem(PlayerSession session, PlayerData d, int bagSlot, ItemData item, int quantity) {
      ExpItem exp = EXP_ITEMS.get(item.templateId);
      if (exp == null) {
         session.send(Packets.notice("Vật phẩm này chưa thể sử dụng."));
         return;
      }
      if (d.isDead()) {
         session.send(Packets.notice("Đã bị hạ gục, hãy hồi sinh trước."));
         return;
      }
      if (d.level >= Leveling.maxLevel()) {
         session.send(Packets.notice("Đã đạt cấp tối đa."));
         return;
      }
      int count = Math.min(quantity, item.quantity);
      int usedToday = 0;
      if (exp.dailyLimit() > 0) {
         usedToday = d.itemDaily.usedToday(item.templateId, LocalDate.now().toEpochDay());
         int left = exp.dailyLimit() - usedToday;
         if (left <= 0) {
            session.send(Packets.notice("Hôm nay đã dùng đủ " + exp.dailyLimit() + " " + name(item)
                  + ", hãy quay lại vào ngày mai."));
            return;
         }
         count = Math.min(count, left);
         usedToday += count;
         d.itemDaily.used.put(item.templateId, usedToday);
      }
      long amount = exp.baseExp() * Math.max(1, d.level) * count;
      item.quantity -= count;
      if (item.quantity <= 0) {
         d.bag.take(bagSlot);
      }
      int gained = Leveling.addExp(d, amount);
      sync(session, d, BAG);
      session.send(ItemPackets.expGain(amount));
      if (gained > 0) {
         refreshLook(d); // chỉ số gốc theo cấp
         world.broadcastAround(session, ItemPackets.levelUp(d.getId(), d.level, d.exp,
               PlayerStats.expToNextLevel(d.level)).toPacket());
         world.broadcastStats(d); // cấp / EXP / điểm tiềm năng (op 23 sub 0)
         QuestEvents.fire(session, StepKind.REACH_LEVEL, 0);
         com.tlnt.server.achievement.AchievementEvents.add(session, com.tlnt.server.achievement.AchievementType.LEVEL, d.level);
      }
      session.send(Packets.notice("Dùng " + count + " " + name(item) + ": +" + String.format(VI, "%,d", amount)
            + " kinh nghiệm" + (exp.dailyLimit() > 0 ? " (hôm nay " + usedToday + "/" + exp.dailyLimit() + ")" : "")
            + (gained > 0 ? ", lên cấp " + d.level : "") + "."));
   }

   /** Cỏ hồi sinh dùng từ hành trang: chỉ khi đã bị hạ gục (như nút "Hồi sinh" ở hộp Kiệt sức). */
   private void useReviveHerb(PlayerSession session, PlayerData d, int bagSlot, ItemData item) {
      if (!d.isDead()) {
         session.send(Packets.notice("Cỏ hồi sinh chỉ dùng được khi bị kiệt sức."));
         return;
      }
      if (com.tlnt.server.thunder.ThunderRules.isThunderMap(d.mapId)) {
         session.send(Packets.notice(com.tlnt.server.thunder.ThunderRules.NO_REVIVE_HERB));
         return;
      }
      if (com.tlnt.server.world.Arena.isArenaMap(d.mapId)) {
         session.send(Packets.notice(com.tlnt.server.tournament.TournamentRules.NO_REVIVE_HERB));
         return;
      }
      consumeAt(d, bagSlot, item);
      sync(session, d, BAG);
      world.reviveHere(session, REVIVE_HERB_HP_PERCENT);
      com.tlnt.server.achievement.AchievementEvents.add(session, com.tlnt.server.achievement.AchievementType.REVIVE, 1);
      com.tlnt.server.daily.DailyEvents.add(session, com.tlnt.server.daily.DailyKind.REVIVE, 0, 1);
   }

   /** Vật phẩm hiệu ứng: dùng nhiều món cộng thời gian; các hiệu ứng khác nhau cùng tồn tại. */
   private void useBuff(PlayerSession session, PlayerData d, int bagSlot, ItemData item, ItemBuffs.Effect effect,
         int quantity) {
      if (d.isDead()) {
         session.send(Packets.notice("Đã bị hạ gục, hãy hồi sinh trước."));
         return;
      }
      if (item.templateId == ItemBuffs.PK_SHIELD_ITEM && com.tlnt.server.thunder.ThunderRules.isThunderMap(d.mapId)) {
         session.send(Packets.notice(com.tlnt.server.thunder.ThunderRules.NO_PK_SHIELD)); // Lôi vực: PK bắt buộc
         return;
      }
      if (item.templateId == ItemBuffs.PK_SHIELD_ITEM && com.tlnt.server.world.Arena.isArenaMap(d.mapId)) {
         session.send(Packets.notice("Không thể dùng Bùa hộ giáp ở đấu trường."));
         return;
      }
      long now = System.currentTimeMillis();
      Map.Entry<Integer, Long> active = ItemBuffs.activeInGroup(d, effect, now);
      if (active != null && active.getKey() != effect.optionId()) {
         ItemBuffs.Effect current = ItemBuffs.forOption(active.getKey());
         d.pendingBuffSlot = bagSlot;
         d.pendingBuffTemplateId = item.templateId;
         d.pendingBuffQuantity = quantity;
         session.send(Packets.confirm(Packets.CONFIRM_USE_BUFF, "Bạn đang sử dụng "
               + templateName(current.itemTemplateId()) + " còn " + formatRemaining(active.getValue() - now)
               + ", bạn có muốn sử dụng " + name(item) + " không?"));
         return;
      }
      applyBuff(session, d, bagSlot, item, effect, now, quantity);
   }

   /**
    * Op 32 "Đồng ý" ở hộp hỏi dùng item hiệu ứng: ô túi phải còn đúng mẫu item lúc hỏi (đổi chỗ / hết món thì thôi), rồi
    * dùng — hiệu ứng cùng nhóm bị đè.
    */
   public void confirmUseBuff(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null || d.pendingBuffSlot < 0) {
         return;
      }
      int bagSlot = d.pendingBuffSlot;
      int templateId = d.pendingBuffTemplateId;
      d.pendingBuffSlot = -1;
      d.pendingBuffTemplateId = -1;
      ItemData item = d.bag.get(bagSlot);
      ItemBuffs.Effect effect = item == null || item.templateId != templateId ? null : ItemBuffs.forItem(templateId);
      if (effect == null) {
         return;
      }
      if (d.isDead()) {
         session.send(Packets.notice("Đã bị hạ gục, hãy hồi sinh trước."));
         return;
      }
      applyBuff(session, d, bagSlot, item, effect, System.currentTimeMillis(), d.pendingBuffQuantity);
   }

   /**
    * Trừ {@code quantity} món ("Dùng nhiều"; tối đa số đang có), bật hiệu ứng thời gian × số món; cùng món cộng dồn,
    * khác món cùng nhóm sẽ thay sau khi xác nhận.
    */
   private void applyBuff(PlayerSession session, PlayerData d, int bagSlot, ItemData item, ItemBuffs.Effect effect,
         long now, int quantity) {
      int count = Math.max(1, Math.min(quantity, item.quantity));
      Map.Entry<Integer, Long> replaced = ItemBuffs.activeInGroup(d, effect, now);
      ItemBuffs.Effect old = replaced == null ? null : ItemBuffs.forOption(replaced.getKey());
      boolean statsChanged = effect.changesStats() || (old != null && old.changesStats());
      item.quantity -= count;
      if (item.quantity <= 0) {
         d.bag.take(bagSlot);
      }
      ItemBuffs.apply(d, effect, now, true, count); // dùng item: cùng món thì cộng dồn, × số món
      switch (effect.special()) {
         case PK_SHIELD -> world.forcePeace(session); // cờ về Hoà bình (luồng tick khu)
         case CLEANSE -> {
            ItemBuffs.remove(d, ItemBuffs.DEBUFF_OPTIONS);
            d.frozenUntil = 0;
            d.stunnedUntil = 0;
            d.burningUntil = 0; // tick thiêu đốt còn hẹn tự dừng (ZoneCombat.burnPlayerTick)
            if (d.weakenedUntil != 0) {
               d.weakenedUntil = 0;
               statsChanged = true; // hết chậm
            }
            world.clearPlayerStatus(session); // tắt đóng băng / trói đang hiện (op 35)
         }
         default -> {
         }
      }
      sync(session, d, BAG | (statsChanged ? LOOK : 0)); // buff chỉ số: tính lại + phát khối chỉ số
      session.send(ItemBuffs.packet(d, now));
      PlayerStats stats = d.stats;
      if (effect.instantHpPercent() > 0 || effect.instantMpPercent() > 0) {
         long hp = d.healHp(Math.max(1, stats.maxHp * effect.instantHpPercent() / 100), stats.maxHp);
         long mp = effect.instantMpPercent() > 0
               ? d.healMp(Math.max(1, stats.maxMp * effect.instantMpPercent() / 100), stats.maxMp) : d.mp;
         world.broadcastAround(session, ItemPackets.hpMp(d.getId(), hp, stats.maxHp, mp, stats.maxMp).toPacket());
      }
      if (old == effect) {
         return;
      }
      String amount = count > 1 ? name(item) + " x" + count : name(item);
      long total = effect.durationMs() * count;
      session.send(Packets.notice(old == null ? "Đã dùng " + amount + " (" + formatDuration(total) + ")."
            : "Đã dùng " + amount + " thay cho " + templateName(old.itemTemplateId()) + " (" + formatDuration(total) + ")."));
   }

   /** 3_600_000 -> "1 giờ", 1_800_000 -> "30 phút", 30_000 -> "30 giây". */
   static String formatDuration(long ms) {
      long s = Math.max(0, ms / 1000);
      if (s >= 3600 && s % 3600 == 0) {
         return (s / 3600) + " giờ";
      }
      if (s >= 60 && s % 60 == 0) {
         return (s / 60) + " phút";
      }
      return formatRemaining(ms);
   }

   /** Thời gian còn lại: "1 giờ 5 phút", "25 phút 3 giây", "45 giây" (làm tròn lên giây). */
   static String formatRemaining(long ms) {
      long s = Math.max(1, (ms + 999) / 1000);
      long h = s / 3600;
      long m = s % 3600 / 60;
      long sec = s % 60;
      StringBuilder out = new StringBuilder();
      if (h > 0) {
         out.append(h).append(" giờ");
      }
      if (m > 0) {
         out.append(out.length() > 0 ? " " : "").append(m).append(" phút");
      }
      if (sec > 0 && h == 0) {
         out.append(out.length() > 0 ? " " : "").append(sec).append(" giây");
      }
      return out.toString();
   }

   static String templateName(int templateId) {
      ItemPack.ItemTemplate template = com.tlnt.server.data.GameDataCache.itemTemplate(templateId);
      return template != null ? template.name() : "vật phẩm";
   }

   private static void consumeAt(PlayerData d, int bagSlot, ItemData item) {
      if (--item.quantity <= 0) {
         d.bag.take(bagSlot);
      }
   }

   /** Sub 3 "Vứt bỏ": xoá hẳn (client có nút nhưng chưa đưa vào menu). */
   public void discard(PlayerSession session, int bagSlot) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null) {
         return;
      }
      d.bag.take(bagSlot);
      session.send(Packets.notice("Đã vứt bỏ " + name(item) + "."));
      sync(session, d, BAG);
   }

   /** Sub 4 "Cất vào" (cửa sổ rương): túi -> rương (cộng dồn nếu được). */
   public void storeToBox(PlayerSession session, int bagSlot) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null || !nearBox(session, d)) {
         return;
      }
      ItemPack.ItemType type = item.type();
      if (type != null && type.equippable() && gearLocked(session, d)) { // cất trang bị vào rương
         return;
      }
      if (d.box.add(item) < 0) {
         session.send(Packets.notice("Rương đã đầy."));
         return;
      }
      d.bag.take(bagSlot);
      sync(session, d, BAG | BOX);
   }

   /** Sub 5 "Lấy ra" (cửa sổ rương): rương -> túi. */
   public void takeFromBox(PlayerSession session, int boxSlot) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.box.get(boxSlot);
      if (item == null || !nearBox(session, d)) {
         return;
      }
      if (d.bag.add(item) < 0) {
         session.send(Packets.notice("Cần ít nhất 1 ô trong hành trang"));
         return;
      }
      d.box.take(boxSlot);
      sync(session, d, BAG | BOX);
      QuestEvents.fire(session, StepKind.TAKE_FROM_CHEST, NpcService.NPC_CHEST);
   }

   /** Sub 8: gán ô túi vào ô dùng nhanh ({@code quickNumber} 3 = HP, 4 = MP, 1 = thể lực). */
   public void assignQuickSlot(PlayerSession session, int bagSlot, int quickNumber) {
      PlayerData d = player(session);
      int index = quickIndex(quickNumber);
      if (d == null || index < 0) {
         return;
      }
      ItemData item = d.bag.get(bagSlot);
      if (item == null) {
         return;
      }
      ItemPack.ItemType type = item.type();
      if (type == null || type.equippable()) {
         session.send(Packets.notice("Vật phẩm này không đặt vào ô dùng nhanh được."));
         return;
      }
      d.quickSlots[index] = bagSlot;
      sync(session, d, QUICK);
      QuestEvents.fire(session, StepKind.BIND_POTIONS, 0);
   }

   /** Sub 9: bấm ô dùng nhanh -> dùng 1 món ở ô túi đã gán. */
   public void useQuickSlot(PlayerSession session, int quickNumber) {
      PlayerData d = player(session);
      int index = quickIndex(quickNumber);
      if (d == null || index < 0) {
         return;
      }
      int bagSlot = d.quickSlots[index];
      if (bagSlot < 0 || d.bag.get(bagSlot) == null) {
         return;
      }
      use(session, bagSlot, 1);
   }

   /** Sub 12 "Khóa": khoá vĩnh viễn (không bán / giao dịch được). */
   public void lock(PlayerSession session, int bagSlot) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null || item.locked) {
         return;
      }
      item.locked = true;
      sync(session, d, BAG);
   }

   /** Sub 13 "Bán": dữ liệu item chưa có giá bán -> báo chưa hỗ trợ. */
   public void sell(PlayerSession session, int bagSlot) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null) {
         return;
      }
      // Client bấm "Bán" là gửi luôn (không hỏi) -> server hỏi lại (op 32 loại CONFIRM_SELL), "Đồng ý" -> confirmSell
      d.pendingSellSlot = bagSlot;
      d.pendingSellTemplateId = item.templateId;
      d.pendingSellQuantity = item.quantity;
      session.send(Packets.confirm(Packets.CONFIRM_SELL, "Bán " + name(item)
            + (item.quantity > 1 ? " x" + item.quantity : "") + " được " + sellPrice(item) + " vàng khoá?"));
   }

   /** Giá bán TẠM: {@link #SELL_PRICE_PER_ITEM} vàng khoá mỗi món (cả chồng). */
   public static long sellPrice(ItemData item) {
      return SELL_PRICE_PER_ITEM * Math.max(1, item.quantity);
   }

   /** Giá bán tạm 1 món (vàng khoá) — chưa có giá bán trong dữ liệu item. */
   static final long SELL_PRICE_PER_ITEM = 1;

   /**
    * Op 32 "Đồng ý" ở hộp hỏi bán: ô túi phải còn đúng món + số lượng lúc hỏi (đổi chỗ / dùng bớt thì thôi) -> bỏ khỏi túi,
    * cộng vàng khoá.
    */
   public void confirmSell(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null || d.pendingSellSlot < 0) {
         return;
      }
      int bagSlot = d.pendingSellSlot;
      d.pendingSellSlot = -1;
      ItemData item = d.bag.get(bagSlot);
      if (item == null || item.templateId != d.pendingSellTemplateId || item.quantity != d.pendingSellQuantity) {
         return;
      }
      long price = sellPrice(item);
      d.bag.take(bagSlot);
      synchronized (d) {
         d.lockedGold += price;
      }
      syncBagAndCurrency(session);
      session.send(Packets.notice("Đã bán " + name(item) + (item.quantity > 1 ? " x" + item.quantity : "") + " được "
            + price + " vàng khoá."));
   }

   // ================================================================== sắp xếp (op 22 C→S)

   /** Op 22 sub 0: nút "Sắp xếp" hành trang. */
   public void sortBag(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null || !allowSort(session, d.lastBagSortAt)) {
         return;
      }
      d.lastBagSortAt = System.currentTimeMillis();
      int[] mergedInto = new int[d.bag.size()];
      int[] moved = d.bag.sort(mergedInto);
      // Ô dùng nhanh đi theo món đồ (hoặc theo chồng mà nó đã gộp vào).
      for (int i = 0; i < d.quickSlots.length; i++) {
         int old = d.quickSlots[i];
         if (old >= 0 && old < moved.length) {
            d.quickSlots[i] = moved[old] >= 0 ? moved[old] : mergedInto[old];
         }
      }
      sync(session, d, BAG | QUICK);
   }

   /** Op 22 sub 1: nút "Sắp xếp" rương. */
   public void sortBox(PlayerSession session) {
      PlayerData d = player(session);
      if (d == null || !nearBox(session, d) || !allowSort(session, d.lastBoxSortAt)) {
         return;
      }
      d.lastBoxSortAt = System.currentTimeMillis();
      d.box.sort(new int[d.box.size()]);
      sync(session, d, BOX);
   }

   /**
    * Rương chỉ dùng được khi đang đứng gần NPC Rương đồ đã bấm mở ({@link PlayerData#boxMapId}) — chặn cất / lấy từ
    * xa bằng gói giả hoặc giữ cửa sổ rương mở rồi đi chỗ khác.
    */
   private boolean nearBox(PlayerSession session, PlayerData d) {
      WorldPlayer p = world.online(session);
      boolean near = com.tlnt.server.badge.SpiritBadgeService.remote(d, d.boxMapId) // mở từ Huy hiệu Tinh Linh
            || p != null && p.zone() != null && d.boxMapId == p.zone().map().template().id()
            && NpcService.near(p, d.boxX, d.boxY);
      if (!near) {
         session.send(Packets.notice("Hãy đứng gần Rương đồ."));
      }
      return near;
   }

   /** Chống bấm liên tục (mỗi lần sắp xếp gửi lại cả kho). */
   private static boolean allowSort(PlayerSession session, long lastSortAt) {
      if (System.currentTimeMillis() - lastSortAt < SORT_COOLDOWN_MS) {
         session.send(Packets.notice("Thao tác quá nhanh."));
         return false;
      }
      return true;
   }

   // ================================================================== bộ sưu tập

   /**
    * Op 27 sub 6/10/16/18 (và sub 0 với loại 19/20/36/37): túi -> bộ sưu tập. KHÔNG tự dùng: ngoại hình chỉ đổi khi
    * chọn món trong bộ sưu tập ({@link #selectInCollection}); chỉ số thì cộng ngay (tổng cả bộ sưu tập).
    */
   public void putInCollection(PlayerSession session, CollectionKind kind, int bagSlot) {
      PlayerData d = player(session);
      ItemData item = d == null ? null : d.bag.get(bagSlot);
      if (item == null || gearLocked(session, d)) { // bộ sưu tập cộng chỉ số = trang bị lên người
         return;
      }
      if (item.typeId() != kind.itemTypeId) {
         session.send(Packets.notice("Vật phẩm này không phải " + kind.displayName.toLowerCase() + "."));
         return;
      }
      if (kind == CollectionKind.BACK) { // đeo lưng có cấp / nhân vật yêu cầu như đồ mặc
         String error = checkRequirement(d, item);
         if (error != null) {
            session.send(Packets.notice(error));
            return;
         }
      }
      ItemCollection collection = kind.of(d);
      for (ItemData owned : collection.items) { // mỗi mẫu item chỉ 1 món trong bộ sưu tập
         if (owned.templateId == item.templateId) {
            session.send(Packets.notice("Đã có " + name(item) + " trong bộ sưu tập."));
            return;
         }
      }
      if (collection.isFull()) {
         session.send(Packets.notice(kind.displayName + " đã đầy."));
         return;
      }
      d.bag.take(bagSlot);
      item.locked = true;
      collection.add(item);
      // Chỉ số = tổng cả bộ sưu tập -> tính lại (LOOK); ngoại hình không đổi vì món đang dùng không đổi.
      sync(session, d, BAG | LIST << kind.ordinal() | LOOK);
   }

   /** Op 27 sub 7/11/17/19 "Lấy ra": bộ sưu tập -> túi. Đang dùng món đó thì tháo luôn. */
   public void takeFromCollection(PlayerSession session, CollectionKind kind, int index) {
      PlayerData d = player(session);
      ItemCollection collection = d == null ? null : kind.of(d);
      if (collection == null || collection.get(index) == null || gearLocked(session, d)) {
         return;
      }
      int empty = d.bag.firstEmpty();
      if (empty < 0) {
         session.send(Packets.notice("Cần ít nhất 1 ô trong hành trang"));
         return;
      }
      d.bag.set(empty, collection.remove(index)); // đang dùng món đó thì tự tháo (remove kẹp selected)
      sync(session, d, BAG | LIST << kind.ordinal() | LOOK);
   }

   /** Op 48/50/64/66 sub 1: dùng món {@code index} (chỉ đổi NGOẠI HÌNH, chỉ số không đổi), -1 = tháo. */
   public void selectInCollection(PlayerSession session, CollectionKind kind, int index) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      ItemCollection collection = kind.of(d);
      if (index != -1 && collection.get(index) == null) {
         return;
      }
      if (collection.selected == index) {
         sync(session, d, SELECTED << kind.ordinal()); // gửi lại cho client khớp trạng thái
         return;
      }
      collection.selected = index;
      sync(session, d, SELECTED << kind.ordinal() | LOOK);
   }

   // ================================================================== hết hạn

   /**
    * Dọn món hết hạn ở mọi kho (túi, rương, đồ mặc, đeo lưng, 4 bộ sưu tập), báo tên từng món, gửi lại phần đổi.
    * Gọi từ {@link ItemExpiryTask} (xếp vào hàng đợi session) khi tới {@link PlayerData#nextExpireAt}.
    */
   public void removeExpired(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      if (d.trade != null) { // không đụng túi đồ lúc đang giao dịch; ItemExpiryTask thử lại lượt sau
         d.nextExpireAt = System.currentTimeMillis();
         return;
      }
      List<ItemData> removed = new ArrayList<>();
      int changes = purgeExpired(d, System.currentTimeMillis(), removed);
      for (int i = 0; i < removed.size() && i < 5; i++) {
         session.send(Packets.notice(name(removed.get(i)) + " đã hết hạn sử dụng."));
      }
      if (removed.size() > 5) {
         session.send(Packets.notice("Và " + (removed.size() - 5) + " vật phẩm khác đã hết hạn."));
      }
      sync(session, d, changes); // luôn tính lại nextExpireAt, kể cả khi chưa có gì hết hạn
   }

   /**
    * Gỡ món hết hạn khỏi mọi kho, không gửi gói. Dùng lúc vào game (trước khi gửi túi đồ) và trong
    * {@link #removeExpired}. @return bitmask phần đã đổi (cho {@link #sync}).
    */
   public static int purgeExpired(PlayerData d, long now, List<ItemData> removed) {
      int changes = 0;
      if (d.bag.removeExpired(now, removed)) {
         changes |= BAG;
      }
      if (d.box.removeExpired(now, removed)) {
         changes |= BOX;
      }
      if (d.body.removeExpired(now, removed)) {
         changes |= BODY | LOOK;
      }
      for (CollectionKind kind : CollectionKind.values()) {
         if (kind.of(d).removeExpired(now, removed)) {
            changes |= LIST << kind.ordinal() | LOOK;
         }
      }
      updateNextExpire(d);
      return changes;
   }

   /** Tính lại {@link PlayerData#nextExpireAt} (hạn sớm nhất trong mọi kho). */
   public static void updateNextExpire(PlayerData d) {
      long min = Math.min(Math.min(d.bag.earliestExpire(), d.box.earliestExpire()), d.body.earliestExpire());
      for (CollectionKind kind : CollectionKind.values()) {
         min = Math.min(min, kind.of(d).earliestExpire());
      }
      d.nextExpireAt = min;
   }

   /** Giá mở thêm 1 ô bộ sưu tập (vàng). */
   public static final long EXPAND_SLOT_PRICE = 10_000_000L;

   /**
    * Op 48/50/64/66 sub 2 "Mở rộng": mở thêm ĐÚNG 1 ô ({@link ItemCollection#capacity} + 1, tối đa
    * {@link ItemCollection#MAX_CAPACITY}) giá {@link #EXPAND_SLOT_PRICE} vàng. Chưa trừ gì — hỏi trước (hộp op 32 loại
    * {@link Packets#CONFIRM_EXPAND_COLLECTION}), "Đồng ý" -> {@link #confirmExpandCollection}.
    */
   public void expandCollection(PlayerSession session, CollectionKind kind) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      ItemCollection collection = kind.of(d);
      if (collection.capacity >= ItemCollection.MAX_CAPACITY) {
         d.pendingExpandKind = null;
         session.send(Packets.notice(kind.displayName + " đã mở rộng tối đa " + ItemCollection.MAX_CAPACITY + " ô."));
         return;
      }
      d.pendingExpandKind = kind;
      d.pendingExpandCapacity = collection.capacity;
      session.send(Packets.confirm(Packets.CONFIRM_EXPAND_COLLECTION, "Mở rộng thêm ô " + (collection.capacity + 1)
            + " với giá " + formatMillions(EXPAND_SLOT_PRICE) + " vàng!"));
   }

   /**
    * Op 32 "Đồng ý" ở hộp mở rộng: số ô phải còn đúng như lúc hỏi (bấm đồng ý 2 lần / hỏi 2 bộ sưu tập chỉ tính lần
    * cuối, không trừ vàng 2 lần), đủ vàng -> trừ vàng, +1 ô.
    */
   public void confirmExpandCollection(PlayerSession session) {
      PlayerData d = player(session);
      CollectionKind kind = d == null ? null : d.pendingExpandKind;
      if (kind == null) {
         return;
      }
      d.pendingExpandKind = null;
      ItemCollection collection = kind.of(d);
      if (collection.capacity != d.pendingExpandCapacity || collection.capacity >= ItemCollection.MAX_CAPACITY) {
         return;
      }
      if (d.gold + d.lockedGold < EXPAND_SLOT_PRICE) {
         session.send(Packets.notice("Không đủ vàng, cần " + formatMillions(EXPAND_SLOT_PRICE) + " vàng."));
         return;
      }
      long fromLocked = Math.min(d.lockedGold, EXPAND_SLOT_PRICE);
      d.lockedGold -= fromLocked;
      d.gold -= EXPAND_SLOT_PRICE - fromLocked;
      collection.capacity++;
      session.send(ItemPackets.currency(d));
      session.send(Packets.notice("Đã mở rộng ô " + collection.capacity + " của " + kind.displayName + "."));
      sync(session, d, LIST << kind.ordinal());
   }

   /** 10_000_000 -> "10.0m", 11_500_000 -> "11.5m". */
   static String formatMillions(long gold) {
      return (gold / 1_000_000) + "." + (gold % 1_000_000 / 100_000) + "m";
   }

   /** Op 48/50/64/66 sub 0: checkbox "Ẩn ..." — vẫn dùng (có chỉ số), chỉ không hiện ra ngoài. */
   public void setHidden(PlayerSession session, CollectionKind kind, boolean hidden) {
      PlayerData d = player(session);
      if (d == null) {
         return;
      }
      PlayerSettings current = d.settings != null ? d.settings : PlayerSettings.defaults();
      if (kind.hideFlag < current.inventoryFlags.length && current.inventoryFlags[kind.hideFlag] == hidden) {
         return;
      }
      // Thay cả object (không sửa mảng đang dùng) để lượt lưu trên luồng khác luôn chụp 1 bộ trọn vẹn.
      PlayerSettings next = current.copy(); // copy() nới mảng cờ ra đủ INVENTORY_FLAGS
      next.inventoryFlags[kind.hideFlag] = hidden;
      d.settings = next;
      // Dùng cùng luồng cập nhật như ngoại trang cho TẤT CẢ bộ sưu tập. Gói op 2 sub 1 trong
      // client decompile chỉ thay equipmentSkinKeys; nó không đồng bộ các trạng thái skeleton,
      // danh hiệu, thú cưng, và có thể để preview/nhân vật của người chơi giữ trạng thái cũ.
      // Khối stats đầy đủ làm client áp dụng ngoại hình ngay, không cần đóng/mở lại bộ sưu tập.
      sync(session, d, SETTINGS | LOOK);
   }

   // ================================================================== hồi sinh

   /** {@code item_template.id} "Cỏ hồi sinh" — nút "Hồi sinh (1 cỏ hồi sinh)" ở hộp "Kiệt sức". */
   public static final int REVIVE_HERB_TEMPLATE_ID = 238;
   /** % HP tối đa khi hồi sinh bằng cỏ hồi sinh (hộp Kiệt sức hoặc dùng từ hành trang) — 100 = đầy cả HP lẫn MP. */
   public static final int REVIVE_HERB_HP_PERCENT = 100;

   /** Lấy đi 1 món mẫu {@code templateId} trong hành trang (chồng đầu tiên tìm thấy). @return false nếu không có */
   public boolean consumeOne(PlayerSession session, int templateId) {
      PlayerData d = player(session);
      if (d == null) {
         return false;
      }
      for (int slot = 0; slot < d.bag.size(); slot++) {
         ItemData item = d.bag.get(slot);
         if (item != null && item.templateId == templateId && !item.isExpired(System.currentTimeMillis())) {
            if (--item.quantity <= 0) {
               d.bag.take(slot);
            }
            sync(session, d, BAG);
            return true;
         }
      }
      return false;
   }

   // ================================================================== sau giao dịch

   /**
    * Giao dịch vừa chốt ({@code TradeService}, có thể trên luồng của người kia): trên luồng session của chính người
    * này, sửa ô dùng nhanh theo túi mới, gửi lại hành trang + vàng.
    */
   public void afterTrade(PlayerSession session) {
      syncBagAndCurrency(session);
   }

   /**
    * Túi / vàng vừa bị hệ thống khác đổi (giao dịch, thư, kí gửi, phúc lợi): gửi lại vàng + hành trang (+ ô dùng nhanh
    * sửa theo túi mới). Gọi trên luồng session của chính người đó.
    */
   public void syncBagAndCurrency(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      session.send(ItemPackets.currency(d));
      sync(session, d, BAG);
   }

   // ================================================================== chuyển vật phẩm ra ngoài / nhận vào (thư, kí gửi)

   /** {@code item_template.id} tiền tệ: vàng (khoá / không khoá theo cờ khoá của item), lam thạch. */
   public static final int GOLD_TEMPLATE_ID = 1;
   public static final int DIAMOND_TEMPLATE_ID = 2;
   /** Vàng tối đa mỗi loại 1 nhân vật được giữ. */
   public static final long MAX_GOLD = 999_999_999_999L;

   /**
    * Món ở ô túi có được đưa ra khỏi người không (gửi thư, kí gửi — cùng luật với giao dịch): có mẫu, KHÔNG khoá, chưa
    * hết hạn, không phải tiền tệ, số lượng 1..số đang có. @return câu báo lỗi, null = được
    */
   public static String checkTransferable(ItemData item, int quantity, long now) {
      if (item == null || item.template() == null || quantity <= 0 || quantity > item.quantity) {
         return "Vật phẩm không hợp lệ.";
      }
      if (item.locked) {
         return "Không thể dùng vật phẩm đã khoá.";
      }
      if (item.isExpired(now)) {
         return "Vật phẩm đã hết hạn sử dụng.";
      }
      if (item.templateId == GOLD_TEMPLATE_ID || item.templateId == DIAMOND_TEMPLATE_ID) {
         return "Vật phẩm không hợp lệ.";
      }
      return null;
   }

   /** Tổng số lượng mẫu {@code templateId} còn hạn trong {@code bag} (mọi ô, cả khoá lẫn không khoá). */
   public static long count(ItemContainer bag, int templateId, long now) {
      long total = 0;
      for (int slot = 0; slot < bag.size(); slot++) {
         ItemData item = bag.get(slot);
         if (item != null && item.templateId == templateId && !item.isExpired(now)) {
            total += item.quantity;
         }
      }
      return total;
   }

   /**
    * Trừ {@code amount} món mẫu {@code templateId} còn hạn khỏi {@code bag} (nên là bản sao), dùng món KHOÁ trước.
    * Đã kiểm tra đủ bằng {@link #count}. @return true nếu có dùng món khoá
    */
   public static boolean remove(ItemContainer bag, int templateId, long amount, long now) {
      boolean usedLocked = false;
      for (int pass = 0; pass < 2 && amount > 0; pass++) {
         boolean lockedPass = pass == 0;
         for (int slot = 0; slot < bag.size() && amount > 0; slot++) {
            ItemData item = bag.get(slot);
            if (item == null || item.templateId != templateId || item.locked != lockedPass || item.isExpired(now)) {
               continue;
            }
            int used = (int) Math.min(item.quantity, amount);
            amount -= used;
            usedLocked |= lockedPass;
            if (used >= item.quantity) {
               bag.take(slot);
            } else {
               item.quantity -= used;
            }
         }
      }
      return usedLocked;
   }

   /**
    * Lấy {@code quantity} món ở ô {@code slot} ra khỏi túi (đủ cả chồng thì lấy nguyên món, không thì tách bản sao).
    * Đã kiểm tra bằng {@link #checkTransferable}. Không gửi gói.
    */
   public static ItemData takeFromBag(PlayerData d, int slot, int quantity) {
      ItemData item = d.bag.get(slot);
      if (quantity >= item.quantity) {
         return d.bag.take(slot);
      }
      ItemData part = item.copy();
      part.quantity = quantity;
      item.quantity -= quantity;
      return part;
   }

   /**
    * Cho {@code items} vào người (thư, phúc lợi): item 1 = vàng (khoá thì vào vàng khoá), item 2 = lam thạch, còn lại vào hành trang (cộng
    * dồn được thì cộng). Làm trên BẢN SAO túi, thiếu chỗ / vượt giới hạn vàng thì không đổi gì. Món đã hết hạn bỏ qua.
    * Không gửi gói. @return câu báo lỗi, null = đã cho hết
    */
   public static String give(PlayerData d, List<ItemData> items, long now) {
      return give(d, d.bag.deepCopy(), items, now);
   }

   /** Câu báo khi hành trang không còn ô trống. */
   public static final String BAG_FULL = "Hành trang đã đầy.";

   /**
    * Như {@link #give(PlayerData, List, long)} nhưng cho vào {@code bag} = BẢN SAO túi người gọi đã sửa trước (vd đã trừ
    * vật phẩm trả tiền shop); thành công thì {@code bag} thành túi của nhân vật.
    *
    * <p>Hành trang (bản gốc {@code d.bag} hoặc bản sao {@code bag}) KHÔNG còn ô trống thì chặn luôn mọi món vào túi — kể cả món cộng dồn được
    * (mở hộp, giao dịch, vòng quay, thư, shop...): tránh lợi dụng túi đầy. Chỉ nhặt đồ dưới đất được cộng dồn khi túi
    * đầy ({@link #giveStacking}). Quà chỉ có vàng / lam thạch thì không cần ô.
    */
   public static String give(PlayerData d, ItemContainer bag, List<ItemData> items, long now) {
      // túi thật hoặc bản sao đang dồn (mở nhiều hộp liền) đã kín -> dừng, không cộng dồn tiếp
      if (needsBagSlot(items, now) && (d.bag.firstEmpty() < 0 || bag.firstEmpty() < 0)) {
         return BAG_FULL;
      }
      return giveStacking(d, bag, items, now);
   }

   /** Có món nào phải vào túi (không phải vàng / lam thạch, chưa hết hạn). */
   static boolean needsBagSlot(List<ItemData> items, long now) {
      for (ItemData item : items) {
         if (item != null && !item.isExpired(now) && item.templateId != GOLD_TEMPLATE_ID
               && item.templateId != DIAMOND_TEMPLATE_ID) {
            return true;
         }
      }
      return false;
   }

   /**
    * Như {@link #give(PlayerData, ItemContainer, List, long)} nhưng túi đầy vẫn cộng dồn được vào chồng sẵn có — CHỈ
    * dùng cho nhặt vật phẩm dưới đất.
    */
   public static String giveStacking(PlayerData d, ItemContainer bag, List<ItemData> items, long now) {
      long gold = d.gold;
      long lockedGold = d.lockedGold;
      long diamond = d.diamond;
      for (ItemData item : items) {
         if (item == null || item.isExpired(now)) {
            continue;
         }
         if (item.templateId == GOLD_TEMPLATE_ID) {
            if (item.locked) {
               lockedGold += item.quantity;
            } else {
               gold += item.quantity;
            }
            continue;
         }
         if (item.templateId == DIAMOND_TEMPLATE_ID) {
            diamond += item.quantity;
            continue;
         }
         if (bag.add(item.copy()) < 0) {
            return bagSpaceMessage(d.bag, items, now);
         }
      }
      if (gold > MAX_GOLD || lockedGold > MAX_GOLD) {
         return "Số vàng vượt quá giới hạn.";
      }
      if (diamond > MAX_GOLD) {
         return "Số lam thạch vượt quá giới hạn.";
      }
      d.bag = bag;
      d.gold = gold;
      d.lockedGold = lockedGold;
      d.diamond = diamond;
      return null;
   }

   /**
    * "Cần ít nhất x ô trong hành trang": x = số ô TRỐNG cần có để nhận hết {@code items} (món cộng dồn được vào chồng
    * sẵn có thì không tốn ô; vàng / lam thạch không tốn ô) — thử trên bản sao túi gốc nới rộng không giới hạn.
    */
   static String bagSpaceMessage(ItemContainer original, List<ItemData> items, long now) {
      ItemContainer probe = original.deepCopy();
      int size = probe.size();
      probe.resize(size + items.size());
      int emptyBefore = probe.countEmpty();
      for (ItemData item : items) {
         if (item != null && !item.isExpired(now) && item.templateId != GOLD_TEMPLATE_ID
               && item.templateId != DIAMOND_TEMPLATE_ID) {
            probe.add(item.copy());
         }
      }
      int needed = Math.max(1, emptyBefore - probe.countEmpty());
      return "Cần ít nhất " + needed + " ô trong hành trang";
   }

   // ================================================================== nội bộ

   /**
    * Nhân vật của session, CHỈ khi đã vào map xong ({@link PlayerData#inWorld}): lúc đang vào, luồng tick khu còn
    * đọc túi đồ để gửi trạng thái ban đầu -> không cho luồng session sửa song song. Client thật chỉ bấm được khi đã
    * thấy màn chơi nên không mất thao tác nào.
    */
   static PlayerData player(PlayerSession session) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return null;
      }
      if (d.trade != null) { // túi đồ đang "đóng băng" chờ chốt giao dịch (trade/TradeService)
         session.send(Packets.notice("Đang giao dịch, không thể thao tác vật phẩm."));
         return null;
      }
      if (d.fishing != null) { // đang câu cá (fishing/FishingService): không bán / dùng / mặc / vứt...
         session.send(Packets.notice("Đang câu cá, không thể thao tác vật phẩm."));
         return null;
      }
      UpgradeService.cancel(session, d); // thao tác túi đồ khác -> huỷ lựa chọn trong lò rèn (tránh xung đột)
      return d;
   }

   /** Cấp độ + giới tính ({@code item_template.class_id} 1..3 = {@link PlayerData#gender}: áo Rinn chỉ Rinn mặc...). */
   private static String checkRequirement(PlayerData d, ItemData item) {
      ItemPack.ItemTemplate template = item.template();
      if (template == null) {
         return "Vật phẩm không hợp lệ.";
      }
      if (d.level < template.requiredLevel()) {
         return "Cần đạt cấp " + template.requiredLevel() + " để trang bị.";
      }
      if (template.classId() > 0 && template.classId() != d.gender) {
         return "Trang bị này không dành cho nhân vật của bạn.";
      }
      return null;
   }

   /** Bình hồi: đọc dòng chỉ số trên chính item (3 +HP, 4 +%HP, 5 +MP, 6 +%MP), dùng tới khi đầy thì thôi. */
   /** Hồi chiêu thuốc HP / MP (mỗi nhóm). */
   public static final int POTION_COOLDOWN_MS = 10_000;

   private void useRecovery(PlayerSession session, PlayerData d, int bagSlot, ItemData item, int quantity) {
      PlayerStats stats = d.stats;
      long hpEach = 0;
      long mpEach = 0;
      long[] apple = AppleTreeService.recovery(item.templateId, stats.maxHp, stats.maxMp); // táo: bảng theo cấp
      for (ItemData.StatValue stat : apple != null ? List.<ItemData.StatValue>of() : item.stats) {
         int value = stat.values() != null && stat.values().length > 0 ? stat.values()[0] : 0;
         switch (stat.statTypeId()) {
            case ItemTypes.STAT_HP -> hpEach += value;
            case ItemTypes.STAT_HP_PERCENT -> hpEach += stats.maxHp * value / 100;
            case ItemTypes.STAT_MP -> mpEach += value;
            case ItemTypes.STAT_MP_PERCENT -> mpEach += stats.maxMp * value / 100;
            default -> {
            }
         }
      }
      if (apple != null) {
         hpEach = apple[0];
         mpEach = apple[1];
      }
      if (hpEach <= 0 && mpEach <= 0) {
         session.send(Packets.notice("Vật phẩm này chưa có chỉ số hồi phục."));
         return;
      }
      if (d.isDead()) {
         session.send(Packets.notice("Đã bị hạ gục, hãy hồi sinh trước."));
         return;
      }
      boolean hpUseful = hpEach > 0 && d.hp < stats.maxHp;
      boolean mpUseful = mpEach > 0 && d.mp < stats.maxMp;
      if (!hpUseful && !mpUseful) {
         session.send(Packets.notice(hpEach > 0 ? "HP đã đầy." : "MP đã đầy."));
         appleUsed(session, item); // nhiệm vụ "Sử dụng Quả táo": bấm dùng là được, kể cả khi đầy
         return;
      }
      // hồi chiêu thuốc: nhóm HP (món hồi HP, kể cả táo hồi cả 2) / nhóm MP — mỗi nhóm 1 lần / POTION_COOLDOWN_MS
      boolean hpGroup = hpEach > 0;
      long now = System.currentTimeMillis();
      long readyAt = hpGroup ? d.hpPotionReadyAt : d.mpPotionReadyAt;
      if (now < readyAt) {
         session.send(Packets.notice("Chưa hồi xong, đợi " + ((readyAt - now + 999) / 1000) + " giây nữa."));
         return;
      }
      if (hpGroup) {
         d.hpPotionReadyAt = now + POTION_COOLDOWN_MS;
      } else {
         d.mpPotionReadyAt = now + POTION_COOLDOWN_MS;
      }
      session.send(ItemPackets.potionCooldown(hpGroup, POTION_COOLDOWN_MS)); // op 27: vòng hồi trên ô thuốc HUD
      int used = 0;
      int max = Math.min(1, Math.min(quantity, item.quantity)); // có hồi chiêu: mỗi lần dùng 1 món
      synchronized (d) { // HP còn bị luồng tick khu trừ (bị người khác đánh)
         while (used < max && d.hp > 0
               && ((hpEach > 0 && d.hp < stats.maxHp) || (mpEach > 0 && d.mp < stats.maxMp))) {
            d.hp = Math.min(stats.maxHp, d.hp + hpEach);
            d.mp = Math.min(stats.maxMp, d.mp + mpEach);
            used++;
         }
      }
      item.quantity -= used;
      // gói nhẹ: chỉ phần vừa đổi (op 23 sub 1 / 2 / 3) + chỉ 1 ô túi (op 22 sub 7) nếu ô còn món
      if (mpEach <= 0) {
         session.send(ItemPackets.hp(d.getId(), d.hp, stats.maxHp));
      } else if (hpEach <= 0) {
         session.send(ItemPackets.mp(d.getId(), d.mp, stats.maxMp));
      } else {
         session.send(ItemPackets.hpMp(d.getId(), d.hp, stats.maxHp, d.mp, stats.maxMp));
      }
      if (item.quantity <= 0) {
         d.bag.take(bagSlot);
         sync(session, d, BAG);
      } else {
         session.send(ItemPackets.slotUpdate(ItemPackets.SUB_BAG, bagSlot, item));
      }
      appleUsed(session, item);
   }

   /** Táo đỏ / xanh mọi cấp + Táo vàng ({@code farm/AppleTreeService}) -> sự kiện nhiệm vụ. */
   private static void appleUsed(PlayerSession session, ItemData item) {
      if (AppleTreeService.isApple(item.templateId)) {
         QuestEvents.fire(session, StepKind.USE_APPLE, 0);
      }
   }

   private static int quickIndex(int quickNumber) {
      return quickNumber >= 0 && quickNumber < QUICK_NUMBER_TO_INDEX.length ? QUICK_NUMBER_TO_INDEX[quickNumber] : -1;
   }

   static String name(ItemData item) {
      ItemPack.ItemTemplate template = item.template();
      return template != null ? template.name() : "vật phẩm";
   }

   /** Gửi đúng những phần đã đổi; LOOK: tính lại chỉ số + ngoại hình và phát cho cả khu. */
   /**
    * Admin panel sửa đồ / tiền của người đang online (trên hàng đợi session của họ): gửi lại các kho đã đổi, tiền tệ,
    * tính lại + phát khối chỉ số khi đổi đồ đang mặc.
    */
   public void adminSync(PlayerSession session, PlayerData d, boolean bag, boolean box, boolean body) {
      sync(session, d, (bag ? BAG : 0) | (box ? BOX : 0) | (body ? BODY | LOOK : 0));
      session.send(ItemPackets.currency(d));
   }

   /** Admin panel: gửi lại MỌI kho (túi, rương, đồ mặc, ô dùng nhanh, 4 bộ sưu tập), tiền tệ, tính lại + phát chỉ số. */
   public void adminSyncAll(PlayerSession session, PlayerData d) {
      int lists = 0;
      for (CollectionKind kind : CollectionKind.values()) {
         lists |= LIST << kind.ordinal();
      }
      sync(session, d, BAG | BOX | BODY | QUICK | LOOK | lists);
      session.send(ItemPackets.currency(d));
   }

   /**
    * Admin panel bật hiệu ứng item {@code effect} trong {@code durationMs} (không cần item, không cộng dồn; cùng nhóm
    * thì đè) + tác dụng đặc biệt như khi dùng item. Luồng session của người đó.
    */
   public void adminBuff(PlayerSession session, PlayerData d, ItemBuffs.Effect effect, long durationMs) {
      long now = System.currentTimeMillis();
      ItemBuffs.applyFor(d, effect, now, durationMs);
      switch (effect.special()) {
         case PK_SHIELD -> world.forcePeace(session);
         case CLEANSE -> {
            ItemBuffs.remove(d, ItemBuffs.DEBUFF_OPTIONS);
            d.frozenUntil = 0;
            d.stunnedUntil = 0;
            d.burningUntil = 0;
            d.weakenedUntil = 0;
            world.clearPlayerStatus(session);
         }
         default -> {
         }
      }
      session.send(ItemBuffs.packet(d, now));
      sync(session, d, LOOK);
   }

   /** Admin panel gỡ hiệu ứng {@code optionId}. Luồng session của người đó. */
   public void adminRemoveBuff(PlayerSession session, PlayerData d, int optionId) {
      if (ItemBuffs.remove(d, java.util.Set.of(optionId))) {
         session.send(ItemBuffs.packet(d, System.currentTimeMillis()));
         sync(session, d, LOOK);
      }
   }

   void sync(PlayerSession session, PlayerData d, int changes) {
      if ((changes & BAG) != 0) {
         fixQuickSlots(d); // ô túi vừa trống -> bỏ khỏi ô dùng nhanh
         // Client xoá (Item.clear) mọi item hành trang cũ khi nhận lại hành trang, mà ô dùng nhanh trỏ vào chính các
         // object đó -> luôn gửi lại ô dùng nhanh SAU hành trang, nếu không phím tắt đã gán bị mất.
         changes |= QUICK;
      }
      if ((changes & BODY) != 0) {
         boolean hadShield = d.gear.has(GearCharges.Kind.SHIELD);
         GearCharges.rebuild(d); // phụ kiện có lượt đang đeo (thêm dòng 58 / 59 nếu thiếu — trước khi gửi bảng đồ)
         session.send(ItemPackets.container(ItemPackets.SUB_BODY, d.body));
         if (hadShield != d.gear.has(GearCharges.Kind.SHIELD)) {
            session.send(ItemBuffs.packet(d, System.currentTimeMillis())); // icon Khiên vỏ sò
         }
         // Cầm Cần câu -> HUD chế độ câu cá (nút câu thay thanh kỹ năng), tháo ra -> HUD thường.
         session.send(FishingPackets.hudMode(FishingService.holdingRod(d)));
      }
      if ((changes & BAG) != 0) {
         session.send(ItemPackets.container(ItemPackets.SUB_BAG, d.bag));
      }
      if ((changes & BOX) != 0) {
         session.send(ItemPackets.container(ItemPackets.SUB_BOX, d.box));
      }
      for (CollectionKind kind : CollectionKind.values()) {
         ItemCollection collection = kind.of(d);
         if ((changes & LIST << kind.ordinal()) != 0) {
            session.send(ItemPackets.collection(kind, collection));
            session.send(ItemPackets.collectionBonus(kind, collection)); // tổng chỉ số cả bộ sưu tập
         } else if ((changes & SELECTED << kind.ordinal()) != 0) {
            session.send(ItemPackets.collectionSelected(kind, collection.getSelected() != null ? collection.selected : -1));
         }
      }
      if ((changes & QUICK) != 0) {
         session.send(ItemPackets.quickSlots(d.quickSlots));
      }
      if ((changes & SETTINGS) != 0 && d.settings != null && d.settings.keybindings != null) {
         session.send(WorldPackets.settings(d.settings));
      }
      if ((changes & LOOK) != 0) {
         refreshLook(d);
         world.broadcastStats(d);
      }
      updateNextExpire(d); // item có hạn có thể vừa vào / ra kho
   }

   /**
    * Tính lại chỉ số + ngoại hình từ đồ đang dùng và công bố (volatile). HP/MP kẹp theo tối đa mới. Gọi trên luồng
    * session hoặc lúc vào game.
    */
   public static void refreshLook(PlayerData d) {
      PlayerStats stats = PlayerStats.of(d);
      synchronized (d) {
         d.hp = Math.min(d.hp, stats.maxHp);
         d.mp = Math.min(d.mp, stats.maxMp);
      }
      d.stats = stats;
      d.appearance = Appearance.of(d);
   }

   /** Ô dùng nhanh trỏ vào ô túi đã trống -> bỏ. */
   private static void fixQuickSlots(PlayerData d) {
      for (int i = 0; i < d.quickSlots.length; i++) {
         int slot = d.quickSlots[i];
         if (slot >= 0 && d.bag.get(slot) == null) {
            d.quickSlots[i] = -1;
         }
      }
   }
}
