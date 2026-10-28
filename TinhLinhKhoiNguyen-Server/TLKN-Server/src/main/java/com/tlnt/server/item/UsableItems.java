package com.tlnt.server.item;

import com.tlnt.server.achievement.AchievementEvents;
import com.tlnt.server.achievement.AchievementType;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.quest.MainQuests;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.skill.SkillService;
import com.tlnt.server.thunder.ThunderRules;
import com.tlnt.server.util.Log;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Item "Sử dụng" có logic riêng (op 27 sub 2, gọi từ {@link ItemService#use} khi không phải đồ mặc / bình hồi / EXP /
 * hiệu ứng). Luồng session (lane GAME; đổi tên = lane BLOCKING nhưng vẫn lần lượt theo hàng đợi session).
 *
 * <pre>
 * 124 Bùa dịch chuyển   dùng: chỉ đường mở Bản đồ (client không có gói mở cửa sổ Bản đồ từ server); mỗi lần dịch
 *                       chuyển qua bản đồ thế giới (op 25 sub 2) tốn 1 bùa -> {@link #teleport}
 * 143 / 144             Mở rộng Hành trang / Rương đồ: +1 ô mỗi món, tối đa {@link #MAX_SLOTS}
 * 145 Sách tiềm năng    +1 điểm tiềm năng mỗi món
 * 166 Bùa tẩy điểm      hỏi op 32 loại 14 -> {@link #confirmReset}: tiềm năng + kỹ năng về chưa nâng, hoàn điểm
 * 239 Tù và             mở hộp nhập (op 70 kênh 1) -> chat thế giới; chat thế giới tốn 1 Tù và ({@link #worldChat})
 * 308 Thẻ đổi tên       mở hộp nhập (op 70 kênh 2) -> {@link #rename}
 * 358 Bùa thanh tẩy     điểm sát khí (điểm PK) về 0
 * 150 Đùi gà            hồi 100% thể lực
 * 293 Lệnh bài Đấu trường +1 lượt Lôi vực Sấm sét hôm nay (tối đa 2 lệnh bài / ngày)
 * hộp quà               {@link GiftBoxes} -> {@link #openBox}
 * </pre>
 */
public final class UsableItems {

   static final int TELEPORT_SCROLL = GiftBoxes.TELEPORT_SCROLL;
   static final int EXPAND_BAG = 143;
   static final int EXPAND_BOX = 144;
   static final int TALENT_BOOK = 145;
   static final int RESET_SCROLL = 166;
   static final int HORN = 239;
   static final int RENAME_CARD = 308;
   static final int CLEANSE_SCROLL = 358;
   static final int CHICKEN_LEG = 150;
   static final int PIGLET = com.tlnt.server.farm.PigService.PIGLET_ITEM;
   static final int ACTIVITY_CHEST = 172;
   static final int ARENA_TOKEN = ThunderRules.TOKEN_ITEM;

   /** Số ô tối đa hành trang / rương khi mở rộng bằng item. */
   public static final int MAX_SLOTS = 126;
   /** Nhiệm vụ chính tuyến phải XONG mới chat thế giới được: 6 "Ấn Ký Nhật Nguyệt". */
   static final int WORLD_CHAT_QUEST = 6;
   /** Số hộp tối đa mở trong 1 lần "Dùng nhiều". */
   static final int MAX_OPEN_PER_USE = 100;

   private final World world;
   private final ItemService items;
   private final PlayerService players;
   private final Executor gamePool;

   public UsableItems(World world, ItemService items, PlayerService players, Executor gamePool) {
      this.world = world;
      this.items = items;
      this.players = players;
      this.gamePool = gamePool;
   }

   /** @return false nếu {@code item} không thuộc lớp này (ItemService báo "chưa thể sử dụng") */
   boolean use(PlayerSession session, PlayerData d, int bagSlot, ItemData item, int quantity) {
      switch (item.templateId) {
         case TELEPORT_SCROLL -> session.send(Packets.notice("Hãy mở Bản đồ (bấm vào bản đồ nhỏ) rồi chọn nơi muốn"
               + " đến, mỗi lần dịch chuyển tốn 1 Bùa dịch chuyển."));
         case EXPAND_BAG -> expand(session, d, bagSlot, item, quantity, true);
         case EXPAND_BOX -> expand(session, d, bagSlot, item, quantity, false);
         case TALENT_BOOK -> talentBook(session, d, bagSlot, item, quantity);
         case RESET_SCROLL -> askReset(session, d, bagSlot);
         case HORN -> session.send(Packets.inputDialog(Packets.INPUT_WORLD_CHAT,
               "Nhập nội dung chat thế giới (tốn 1 Tù và)", ""));
         case RENAME_CARD -> session.send(Packets.inputDialog(Packets.INPUT_RENAME,
               "Nhập tên nhân vật mới (3-16 ký tự, chữ và số)", d.name));
         case CLEANSE_SCROLL -> cleanse(session, d, bagSlot, item);
         case CHICKEN_LEG -> stamina(session, d, bagSlot, item);
         case PIGLET -> releasePig(session, d, bagSlot, item);
         case com.tlnt.server.badge.SpiritBadgeService.BADGE_7_DAYS -> com.tlnt.server.badge.SpiritBadgeService.use(session, d, bagSlot, item, quantity, 7,
               items, players);
         case com.tlnt.server.badge.SpiritBadgeService.BADGE_1_DAY -> com.tlnt.server.badge.SpiritBadgeService.use(session, d, bagSlot, item, quantity, 1,
               items, players);
         case ARENA_TOKEN -> arenaToken(session, d, bagSlot, item);
         case com.tlnt.server.event.MidAutumn.CARROT, com.tlnt.server.event.MidAutumn.LANTERN_ITEM ->
               com.tlnt.server.event.MidAutumnService.useItem(session, d, bagSlot, item);
         default -> {
            GiftBoxes.Box box = GiftBoxes.box(item.templateId);
            if (box == null) {
               return false;
            }
            openBox(session, d, bagSlot, item, box, quantity);
         }
      }
      return true;
   }

   // ================================================================== mở rộng / tiềm năng / sát khí / thể lực

   /** Mở rộng hành trang ({@code bag}) / rương: +1 ô mỗi món, "Dùng nhiều" dùng tới khi đủ {@link #MAX_SLOTS}. */
   private void expand(PlayerSession session, PlayerData d, int bagSlot, ItemData item, int quantity, boolean bag) {
      ItemContainer container = bag ? d.bag : d.box;
      String what = bag ? "Hành trang" : "Rương đồ";
      int room = MAX_SLOTS - container.size();
      if (room <= 0) {
         session.send(Packets.notice(what + " đã mở rộng tối đa " + MAX_SLOTS + " ô."));
         return;
      }
      int count = Math.min(room, Math.min(quantity, item.quantity));
      take(d, bagSlot, item, count);
      container.resize(container.size() + count);
      items.sync(session, d, ItemService.BAG | (bag ? 0 : ItemService.BOX));
      session.send(Packets.notice(what + " đã mở rộng thêm " + count + " ô (" + container.size() + "/" + MAX_SLOTS
            + ")."));
   }

   /** Sách tiềm năng: +1 điểm tiềm năng mỗi món (gửi trong khối chỉ số op 23 sub 0). */
   private void talentBook(PlayerSession session, PlayerData d, int bagSlot, ItemData item, int quantity) {
      int count = Math.min(quantity, item.quantity);
      take(d, bagSlot, item, count);
      synchronized (d) { // cùng khoá với Leveling.addExp (luồng tick khu cộng điểm khi lên cấp)
         d.talentPoints += count;
      }
      items.sync(session, d, ItemService.BAG);
      world.broadcastStats(d);
      session.send(Packets.notice("Nhận " + count + " điểm tiềm năng."));
   }

   /** Bùa tẩy điểm: hỏi trước (op 32 loại {@link Packets#CONFIRM_RESET_POINTS}). */
   private void askReset(PlayerSession session, PlayerData d, int bagSlot) {
      if (SkillService.spent(d) <= 0) {
         session.send(Packets.notice("Bạn chưa cộng tiềm năng hay nâng cấp kỹ năng nào."));
         return;
      }
      d.pendingResetSlot = bagSlot;
      session.send(Packets.confirm(Packets.CONFIRM_RESET_POINTS, "Sử dụng Bùa tẩy điểm sẽ đặt lại toàn bộ tiềm năng"
            + " và kỹ năng về chưa nâng cấp, hoàn trả " + SkillService.spent(d) + " điểm tiềm năng. Bạn có chắc?"));
   }

   /** Op 32 "Đồng ý" ở hộp hỏi tẩy điểm: ô túi phải còn Bùa tẩy điểm. */
   public void confirmReset(PlayerSession session) {
      PlayerData d = ItemService.player(session);
      if (d == null || d.pendingResetSlot < 0) {
         return;
      }
      int bagSlot = d.pendingResetSlot;
      d.pendingResetSlot = -1;
      ItemData item = d.bag.get(bagSlot);
      if (item == null || item.templateId != RESET_SCROLL || item.isExpired(System.currentTimeMillis())
            || SkillService.spent(d) <= 0) {
         return;
      }
      take(d, bagSlot, item, 1);
      int refund = SkillService.resetAll(d);
      session.send(WorldPackets.learnedSkills(d.skills));
      session.send(WorldPackets.skillBar(d.skillBar));
      items.sync(session, d, ItemService.BAG | ItemService.LOOK); // chỉ số tiềm năng + điểm tiềm năng (op 23 sub 0)
      session.send(Packets.notice("Đã tẩy điểm, hoàn trả " + refund + " điểm tiềm năng."));
   }

   /** Bùa thanh tẩy: điểm sát khí ({@code PlayerData.pkPoints}) về 0. */
   private void cleanse(PlayerSession session, PlayerData d, int bagSlot, ItemData item) {
      int before;
      synchronized (d) { // luồng tick khu cộng / trừ điểm PK dưới cùng khoá
         before = d.pkPoints;
         d.pkPoints = 0;
         d.pkPointsAt = 0;
      }
      if (before <= 0) {
         session.send(Packets.notice("Bạn không có điểm sát khí."));
         return;
      }
      take(d, bagSlot, item, 1);
      items.sync(session, d, ItemService.BAG);
      com.tlnt.server.social.PkPoints.cleansed(world, d, before); // hết bị giảm chỉ số
      session.send(Packets.notice("Đã xoá " + before + " điểm sát khí."));
   }

   /** Đùi gà: hồi đầy thể lực ({@code PlayerData.activityPoints}). */
   /** Heo con: thả 1 con heo ở Nông trại ({@code farm/PigService}); thả được mới trừ 1 món + lưu ngay. */
   private void releasePig(PlayerSession session, PlayerData d, int bagSlot, ItemData item) {
      com.tlnt.server.farm.PigService pigs = com.tlnt.server.farm.PigService.instance();
      String error = pigs == null ? "Chưa mở tính năng nuôi heo." : pigs.release(session, d);
      if (error != null) {
         session.send(Packets.notice(error));
         return;
      }
      take(d, bagSlot, item, 1);
      items.sync(session, d, ItemService.BAG);
      players.saveAsync(d, false); // heo mới + túi cùng 1 dòng player
   }

   private void stamina(PlayerSession session, PlayerData d, int bagSlot, ItemData item) {
      if (d.isDead()) {
         session.send(Packets.notice("Đã bị hạ gục, hãy hồi sinh trước."));
         return;
      }
      int max = d.stats.maxStamina;
      int now;
      synchronized (d) { // luồng tick khu trừ thể lực khi đánh / bị hạ gục
         if (d.activityPoints >= max) {
            now = -1;
         } else {
            d.activityPoints = max;
            now = max;
         }
      }
      if (now < 0) {
         session.send(Packets.notice("Thể lực đã đầy."));
         return;
      }
      take(d, bagSlot, item, 1);
      items.sync(session, d, ItemService.BAG);
      session.send(WorldPackets.stamina(now, max));
   }

   /**
    * Lệnh bài Đấu trường: +1 lượt Lôi vực Sấm sét hôm nay, tối đa {@link ThunderRules#MAX_TOKENS} lệnh bài / ngày (đếm ở
    * {@code itemDaily}, lưu ngay). "Dùng nhiều" vẫn chỉ 1 món / lần.
    */
   private void arenaToken(PlayerSession session, PlayerData d, int bagSlot, ItemData item) {
      long today = LocalDate.now().toEpochDay();
      int used = d.itemDaily.usedToday(ThunderRules.TOKEN_ITEM, today);
      if (used >= ThunderRules.MAX_TOKENS) {
         session.send(Packets.notice("Mỗi ngày chỉ dùng được tối đa " + ThunderRules.MAX_TOKENS
               + " Lệnh bài Đấu trường."));
         return;
      }
      take(d, bagSlot, item, 1);
      d.itemDaily.used.put(ThunderRules.TOKEN_ITEM, used + 1);
      items.sync(session, d, ItemService.BAG);
      players.saveAsync(d, false);
      int turns = ThunderRules.FREE_TURNS + used + 1 - d.itemDaily.usedToday(ThunderRules.TURN_KEY, today);
      session.send(Packets.notice("Nhận thêm 1 lượt tham gia Lôi vực Sấm sét (còn " + Math.max(0, turns)
            + " lượt hôm nay)."));
   }

   // ================================================================== hộp quà

   /**
    * Mở hộp ({@link GiftBoxes}): lần lượt từng hộp tới {@code quantity} — mỗi hộp trừ hộp + điểm hoạt động + lượt
    * trong ngày và cho quà trên CÙNG 1 bản sao túi (đủ chỗ mới thay túi thật); hết điểm / lượt / chỗ thì dừng.
    */
   private void openBox(PlayerSession session, PlayerData d, int bagSlot, ItemData item, GiftBoxes.Box box,
         int quantity) {
      int templateId = item.templateId;
      String boxName = ItemService.name(item);
      // Túi đã kín thì không cho mở, kể cả khi hộp sẽ tự mất khỏi ô cuối cùng hoặc quà có thể cộng dồn.
      if (d.bag.countEmpty() == 0) {
         session.send(Packets.notice("Hành trang đã đầy."));
         return;
      }
      int count = Math.min(MAX_OPEN_PER_USE, Math.min(quantity, item.quantity));
      long today = LocalDate.now().toEpochDay();
      int dailyKey = GiftBoxes.dailyKey(templateId); // 292 + 361 Thỏi vàng chung lượt
      int usedToday = 0;
      if (box.dailyLimit() > 0) {
         usedToday = d.itemDaily.usedToday(dailyKey, today);
         int left = box.dailyLimit() - usedToday;
         if (left <= 0) {
            session.send(Packets.notice("Hôm nay đã dùng đủ " + box.dailyLimit() + " " + ItemService.templateName(dailyKey)
                  + ", hãy quay lại vào ngày mai."));
            return;
         }
         count = Math.min(count, left);
      }
      if (box.activityCost() > 0) {
         int affordable = d.dynamicPoints / box.activityCost();
         if (affordable <= 0) {
            session.send(Packets.notice("Cần " + box.activityCost() + " điểm hoạt động để mở " + boxName
                  + " (hiện có " + d.dynamicPoints + ")."));
            return;
         }
         count = Math.min(count, affordable);
      }
      List<ItemData> received = new ArrayList<>();
      String error = null;
      int opened = 0;
      long now = System.currentTimeMillis();
      ThreadLocalRandom random = ThreadLocalRandom.current();
      while (opened < count) {
         ItemContainer bag = d.bag.deepCopy();
         ItemData boxItem = bag.get(bagSlot);
         if (boxItem == null || boxItem.templateId != templateId) {
            break;
         }
         List<ItemData> gifts = GiftBoxes.roll(box, boxItem.locked, d.gender, now, random);
         if (--boxItem.quantity <= 0) {
            bag.take(bagSlot); // ô hộp vừa trống nhận quà được
         }
         synchronized (d) { // vàng / lam thạch
            error = ItemService.give(d, bag, gifts, now);
            if (error == null) {
               d.dynamicPoints -= box.activityCost();
            }
         }
         if (error != null) {
            break;
         }
         opened++;
         received.addAll(gifts);
      }
      if (opened == 0) {
         session.send(Packets.notice(error != null ? error : "Vật phẩm không hợp lệ."));
         return;
      }
      if (box.dailyLimit() > 0) {
         d.itemDaily.used.put(dailyKey, usedToday + opened);
      }
      items.syncBagAndCurrency(session);
      if (box.activityCost() > 0) {
         session.send(WorldPackets.dynamicPoints(d.dynamicPoints));
      }
      session.send(ItemPackets.rewardPopup(merge(received)));
      if (error != null) {
         session.send(Packets.notice("Đã mở " + opened + " " + boxName + ". " + error));
      }
      if (templateId == ACTIVITY_CHEST) {
         AchievementEvents.add(session, AchievementType.ACTIVITY_CHEST, opened);
      }
      com.tlnt.server.event.MidAutumnService.boxOpened(session, d, templateId, opened); // bánh Trung thu: điểm sự kiện
      Log.debug("'%s' mo %d x item %d", Log.ascii(d.name), opened, templateId);
   }

   /** Gộp các món cộng dồn được (mở nhiều hộp ra cùng loại) cho bảng nhận thưởng. */
   static List<ItemData> merge(List<ItemData> list) {
      List<ItemData> out = new ArrayList<>();
      for (ItemData item : list) {
         ItemData same = null;
         for (ItemData o : out) {
            if (o.canStackWith(item)) {
               same = o;
               break;
            }
         }
         if (same != null) {
            same.quantity += item.quantity;
         } else {
            out.add(item.copy());
         }
      }
      return out;
   }

   // ================================================================== Bùa dịch chuyển (op 25 sub 2)

   /**
    * Bấm 1 điểm trên bản đồ thế giới: trừ 1 Bùa dịch chuyển (món khoá trước) RỒI mới xin khu chuyển map — khu từ chối
    * (map khoá, đang hồi chuyển map...) thì hoàn lại đúng món đó. Không có bùa: "Cần 1 bùa dịch chuyển...".
    */
   public void teleport(PlayerSession session, int targetMapId) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      if (d.trade != null) { // túi đang đóng băng chờ chốt giao dịch
         session.send(Packets.notice("Đang giao dịch, không thể dịch chuyển."));
         return;
      }
      if (targetMapId == d.mapId) {
         return; // client đã chặn bấm map đang đứng
      }
      int slot = findSlot(d, TELEPORT_SCROLL);
      if (slot < 0) {
         session.send(Packets.notice("Cần 1 bùa dịch chuyển để di chuyển tới map này!"));
         return;
      }
      ItemData scroll = d.bag.get(slot);
      ItemData taken = scroll.copy();
      taken.quantity = 1;
      take(d, slot, scroll, 1);
      items.sync(session, d, ItemService.BAG);
      world.teleportToMap(session, targetMapId, () -> session.inbox().execute(() -> refund(session, d, taken),
            gamePool));
   }

   /** Khu không cho chuyển map: trả lại Bùa dịch chuyển đã trừ (luồng session). */
   private void refund(PlayerSession session, PlayerData d, ItemData taken) {
      if (session.getPlayer() != d) {
         return;
      }
      String error;
      synchronized (d) {
         error = ItemService.give(d, List.of(taken), System.currentTimeMillis());
      }
      if (error != null) {
         Log.debug("'%s' khong hoan duoc bua dich chuyen: %s", Log.ascii(d.name), error);
         return;
      }
      items.sync(session, d, ItemService.BAG);
   }

   // ================================================================== Tù và (chat thế giới)

   /**
    * Chat thế giới (tab "Thế giới" op 26 sub 1 hoặc hộp nhập kênh 1 khi dùng Tù và): phải XONG nhiệm vụ
    * {@link #WORLD_CHAT_QUEST} "Ấn Ký Nhật Nguyệt", tốn 1 Tù và (món khoá trước), gửi MỌI người online (op 26 sub 1).
    * {@code text} đã làm sạch + chống spam ở {@code chat/ChatService}.
    */
   public void worldChat(PlayerSession session, PlayerData d, String text) {
      if (d.trade != null) {
         session.send(Packets.notice("Đang giao dịch, không thể dùng Tù và."));
         return;
      }
      if (MainQuests.currentQuest(d.mainQuest) <= WORLD_CHAT_QUEST) {
         session.send(Packets.notice("Cần hoàn thành nhiệm vụ Ấn Ký Nhật Nguyệt để chat thế giới."));
         return;
      }
      int slot = findSlot(d, HORN);
      if (slot < 0) {
         session.send(Packets.notice("Cần 1 Tù và để chat thế giới."));
         return;
      }
      take(d, slot, d.bag.get(slot), 1);
      items.sync(session, d, ItemService.BAG);
      world.broadcastAll(WorldPackets.worldChat(d.getId(), d.name, text));
   }

   // ================================================================== Thẻ đổi tên (op 70 kênh 2, lane BLOCKING)

   /** Chữ người chơi nhập ở hộp đổi tên: kiểm tra luật tên, đổi trong DB (trùng thì báo), trừ 1 Thẻ đổi tên. */
   public void rename(PlayerSession session, String raw) {
      PlayerData d = ItemService.player(session);
      if (d == null) {
         return;
      }
      String name = PlayerService.normalizeName(raw);
      if (name == null) {
         session.send(Packets.notice(PlayerService.NAME_RULE));
         return;
      }
      if (name.equals(d.name)) {
         session.send(Packets.notice("Tên mới giống tên hiện tại."));
         return;
      }
      int slot = findSlot(d, RENAME_CARD);
      if (slot < 0) {
         session.send(Packets.notice("Cần 1 Thẻ đổi tên."));
         return;
      }
      String old = d.name;
      try {
         if (!players.rename(d, name)) {
            session.send(Packets.notice("Tên nhân vật đã có người sử dụng."));
            return;
         }
      } catch (java.sql.SQLException e) {
         Log.warn("Doi ten nhan vat #%d loi SQL: %s", d.getId(), e.getMessage());
         session.send(Packets.notice("Không đổi được tên, hãy thử lại sau."));
         return;
      }
      take(d, slot, d.bag.get(slot), 1);
      players.saveAsync(d, false); // ngay sau UPDATE tên: bản chụp cũ (tên cũ) không đè được
      items.sync(session, d, ItemService.BAG);
      world.broadcastRename(d); // op 2 sub 7: người cùng khu thấy tên mới ngay
      session.send(Packets.notice("Đổi tên thành công: " + name + "."));
      Log.info("Nhan vat #%d doi ten '%s' -> '%s'", d.getId(), Log.ascii(old), Log.ascii(name));
   }

   // ================================================================== nội bộ

   /** Ô túi có món {@code templateId} còn hạn, ưu tiên món KHOÁ; -1 nếu không có. */
   static int findSlot(PlayerData d, int templateId) {
      long now = System.currentTimeMillis();
      int unlocked = -1;
      for (int slot = 0; slot < d.bag.size(); slot++) {
         ItemData item = d.bag.get(slot);
         if (item != null && item.templateId == templateId && !item.isExpired(now)) {
            if (item.locked) {
               return slot;
            }
            if (unlocked < 0) {
               unlocked = slot;
            }
         }
      }
      return unlocked;
   }

   /** Trừ {@code count} món ở ô {@code bagSlot} (hết thì trống ô). */
   private static void take(PlayerData d, int bagSlot, ItemData item, int count) {
      item.quantity -= count;
      if (item.quantity <= 0) {
         d.bag.take(bagSlot);
      }
   }
}
