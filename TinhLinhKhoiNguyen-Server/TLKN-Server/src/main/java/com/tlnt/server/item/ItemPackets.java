package com.tlnt.server.item;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.forge.UpgradeRules;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;

import java.util.ArrayList;
import java.util.List;

/**
 * Gói S→C về item. Format bám client ({@code ServerPacketHandler.handleInventory}, {@code readItemFromMessage},
 * {@code MessageHandler case 48/50/53/64/66}).
 */
public final class ItemPackets {

   /** Opcode 22 sub: danh sách gửi lại nguyên (client xoá rồi đọc lại). */
   public static final int SUB_BODY = 0;
   public static final int SUB_BAG = 1;
   public static final int SUB_BOX = 2;
   public static final int SUB_QUICK_SLOTS = 5;

   private ItemPackets() {
   }

   /** Opcode 22 sub 0/1/2: đủ ô (null = trống). */
   public static Message container(int sub, ItemContainer container) {
      Message msg = new Message(22);
      msg.writeByte(sub);
      List<ItemData> items = container.asList();
      msg.writeInt(items.size());
      for (ItemData item : items) {
         writeItem(msg, item);
      }
      return msg;
   }

   /**
    * Opcode 22 sub 4/6/8/9: danh sách bộ sưu tập + int món đang dùng (-1 = không). Client vẽ đúng số ô nhận được
    * -> gửi đủ {@link ItemCollection#capacity} ô (món trước, còn lại ô trống -1) để panel hiện cả ô trống.
    */
   public static Message collection(CollectionKind kind, ItemCollection collection) {
      Message msg = new Message(22);
      msg.writeByte(kind.inventorySub);
      int slots = Math.max(collection.capacity, collection.items.size());
      msg.writeInt(slots);
      for (ItemData item : collection.items) {
         writeItem(msg, item);
      }
      for (int i = collection.items.size(); i < slots; i++) {
         msg.writeInt(-1); // ô trống
      }
      msg.writeInt(collection.getSelected() != null ? collection.selected : -1);
      return msg;
   }

   /**
    * Opcode 22 sub 3: tiền tệ = 3 ô {@code CurrencyBarPanel} (client đặt tên {@code inventoryTimestamp1..3} — sai):
    * long vàng (icon ui/147; client lấy làm giới hạn vàng giao dịch), long vàng khoá (ui/148), long lam thạch (ui/146).
    */
   public static Message currency(PlayerData d) {
      Message msg = new Message(22);
      msg.writeByte(3);
      msg.writeLong(d.gold);
      msg.writeLong(d.lockedGold);
      msg.writeLong(d.diamond);
      return msg;
   }

   /**
    * Thông báo "Bạn vừa nhận được X!" — MỖI MÓN 1 DÒNG (op notice): tên + " x số lượng" nếu > 1, vàng khoá ghi
    * "Vàng khoá", đồ không khoá ghi thêm "(không khoá)".
    */
   public static List<Message> receivedNotices(List<ItemData> items) {
      List<Message> out = new ArrayList<>(items.size());
      for (ItemData item : items) {
         if (item != null) {
            out.add(Packets.notice("Bạn vừa nhận được " + receivedName(item) + "!"));
         }
      }
      return out;
   }

   /**
    * "1.000 Vàng khoá" / "5 Đá khởi nguyên" / "Thịt heo" — số lượng (chấm ngăn nghìn) ĐỨNG TRƯỚC tên, 1 món không ghi
    * số; vàng khoá ghi "Vàng khoá", đồ thường không khoá ghi thêm "(không khoá)".
    */
   public static String receivedName(ItemData item) {
      String name;
      if (item.templateId == ItemService.GOLD_TEMPLATE_ID) {
         name = item.locked ? "Vàng khoá" : "Vàng";
      } else {
         ItemPack.ItemTemplate template = GameDataCache.itemTemplate(item.templateId);
         name = template != null ? template.name() : "vật phẩm #" + item.templateId;
      }
      boolean currency = item.templateId == ItemService.GOLD_TEMPLATE_ID
            || item.templateId == ItemService.DIAMOND_TEMPLATE_ID;
      String count = item.quantity > 1 ? String.format(java.util.Locale.GERMANY, "%,d ", item.quantity) : "";
      return count + name + (currency || item.locked ? "" : " (không khoá)");
   }

   /** Opcode 65 sub 0 ({@code handleRewardPopup}): bảng "nhận thưởng" hiện các món vừa nhận. */
   public static Message rewardPopup(List<ItemData> items) {
      Message msg = new Message(Opcode.REWARD_POPUP);
      msg.writeByte(0);
      msg.writeInt(items.size());
      for (ItemData item : items) {
         writeItem(msg, item);
      }
      return msg;
   }

   /**
    * 1 bộ sưu tập trong gói "Xem thông tin" (op 2 sub 5): đủ {@link ItemCollection#capacity} ô như khi xem của mình
    * (món trước, còn lại ô trống -1), món đang dùng, tổng chỉ số.
    */
   public static void writeCollectionForInspect(Message msg, ItemCollection collection) {
      int slots = Math.max(collection.capacity, collection.items.size());
      msg.writeInt(slots);
      for (ItemData item : collection.items) {
         writeItem(msg, item);
      }
      for (int i = collection.items.size(); i < slots; i++) {
         msg.writeInt(-1); // ô trống
      }
      msg.writeInt(collection.getSelected() != null ? collection.selected : -1);
      List<ItemData.StatValue> total = ItemStats.sum(collection.items);
      msg.writeInt(total.size());
      for (ItemData.StatValue stat : total) {
         msg.writeInt(stat.statTypeId());
         writeInts(msg, stat.values());
      }
   }

   /** Opcode 22 sub 5: 4 ô dùng nhanh = index trong hành trang (-1 = trống). */
   public static Message quickSlots(int[] slots) {
      Message msg = new Message(22);
      msg.writeByte(SUB_QUICK_SLOTS);
      for (int i = 0; i < 4; i++) {
         msg.writeInt(i < slots.length ? slots[i] : -1);
      }
      return msg;
   }

   /** Opcode 48/50/64/66 sub 1: chỉ đổi món đang dùng (không gửi lại cả danh sách). */
   public static Message collectionSelected(CollectionKind kind, int selected) {
      Message msg = new Message(kind.opcode);
      msg.writeByte(1);
      msg.writeInt(selected);
      return msg;
   }

   /**
    * Opcode 48/50/64/66 sub 0: "Chỉ số cộng thêm" (panel bên phải) = tổng chỉ số của TẤT CẢ món trong bộ sưu tập
    * (đúng lượng đang cộng vào nhân vật, xem {@link ItemStats}).
    */
   public static Message collectionBonus(CollectionKind kind, ItemCollection collection) {
      Message msg = new Message(kind.opcode);
      msg.writeByte(0);
      List<ItemData.StatValue> total = ItemStats.sum(collection.items);
      msg.writeInt(total.size());
      for (ItemData.StatValue stat : total) {
         msg.writeInt(stat.statTypeId());
         writeInts(msg, stat.values());
      }
      return msg;
   }

   /** Opcode 53 sub 0: mở cửa sổ rương (client {@code WindowType.BOX_POPUP}), vd khi bấm NPC rương. */
   public static Message openBox() {
      Message msg = new Message(53);
      msg.writeByte(0);
      return msg;
   }

   /**
    * Op 27 (client: 2 ô thuốc HUD — {@code HudSkillButton3} HP / {@code MpPotionSlotButton} MP, decompile C3873 m18101):
    * sub 0 HP / 1 MP, long ms đã trôi (0), int tổng ms hồi -> client vẽ vòng hồi chiêu.
    */
   public static Message potionCooldown(boolean hp, int durationMs) {
      Message msg = new Message(27);
      msg.writeByte(hp ? 0 : 1);
      msg.writeLong(0L);
      msg.writeInt(durationMs);
      return msg;
   }

   /** Opcode 23 sub 4 (chỉ chính mình): nhận thêm EXP — client cộng vào EXP hiện tại, hiện chữ "+N". */
   public static Message expGain(long amount) {
      Message msg = new Message(23);
      msg.writeByte(4);
      msg.writeLong(amount);
      return msg;
   }

   /** Opcode 23 sub 5 (cả khu): 1 người lên cấp — client đặt cấp / EXP, hiệu ứng + âm thanh "level_up". */
   public static Message levelUp(int playerId, int level, long exp, long maxExp) {
      Message msg = new Message(23);
      msg.writeByte(5);
      msg.writeInt(playerId);
      msg.writeShort(level);
      msg.writeLong(exp);
      msg.writeLong(maxExp);
      return msg;
   }

   /** Opcode 23 sub 3: HP + MP hiện tại / tối đa của 1 người. */
   /** Op 23 sub 1: chỉ HP (int id, long HP, long HP tối đa). */
   public static Message hp(int playerId, long hp, long maxHp) {
      Message msg = new Message(23);
      msg.writeByte(1);
      msg.writeInt(playerId);
      msg.writeLong(hp);
      msg.writeLong(maxHp);
      return msg;
   }

   /** Op 23 sub 2: chỉ MP (int id, long MP, long MP tối đa). */
   public static Message mp(int playerId, long mp, long maxMp) {
      Message msg = new Message(23);
      msg.writeByte(2);
      msg.writeInt(playerId);
      msg.writeLong(mp);
      msg.writeLong(maxMp);
      return msg;
   }

   /** Op 23 sub 7 (chỉ chính mình): điểm tiềm năng còn lại. */
   public static Message talentPoints(int points) {
      Message msg = new Message(23);
      msg.writeByte(7);
      msg.writeInt(points);
      return msg;
   }

   /**
    * Op 22 sub 7 (client {@code handleInventory}): cập nhật TẠI CHỖ 1 món đang có ở ô — byte loại kho (0 đồ mặc, 1 túi,
    * 2 rương...), int ô, item. Client bỏ qua nếu ô đang trống -> chỉ dùng khi ô vẫn còn món (vd thuốc bớt số lượng).
    */
   public static Message slotUpdate(int containerSub, int slot, ItemData item) {
      Message msg = new Message(22);
      msg.writeByte(7);
      msg.writeByte(containerSub);
      msg.writeInt(slot);
      writeItem(msg, item);
      return msg;
   }

   public static Message hpMp(int playerId, long hp, long maxHp, long mp, long maxMp) {
      Message msg = new Message(23);
      msg.writeByte(3);
      msg.writeInt(playerId);
      msg.writeLong(hp);
      msg.writeLong(maxHp);
      msg.writeLong(mp);
      msg.writeLong(maxMp);
      return msg;
   }

   /** Đúng thứ tự {@code ServerPacketHandler.readItemFromMessage}; null = ô trống (-1). */
   public static void writeItem(Message msg, ItemData item) {
      writeItem(msg, item, null);
   }

   /**
    * Món bày bán / xem trước từ cấu hình: option dạng khoảng gửi cờ {@code true} + các cặp (min, max) -> client hiện
    * "50-500" ({@code StatLine.valueRanges}); option cố định gửi như item thường.
    */
   public static void writePreview(Message msg, RewardItem reward, long now) {
      writeItem(msg, reward.create(now), reward.ranged() ? reward : null);
   }

   /**
    * Như {@link #writePreview} nhưng LUÔN gửi dòng chỉ số theo cấu hình (khoảng hiện "min-max", cố định hiện 1 số,
    * dòng không có giá trị như [55] "Có tỉ lệ vĩnh viễn" vẫn hiện) — ô xem trước vòng quay.
    */
   public static void writePreviewRanged(Message msg, RewardItem reward, long now) {
      writeItem(msg, reward.create(now), reward);
   }

   private static void writeItem(Message msg, ItemData item, RewardItem ranges) {
      if (item == null) {
         msg.writeInt(-1);
         return;
      }
      msg.writeInt(item.templateId);
      msg.writeByte(item.starGrade);
      msg.writeByte(item.starCount);
      msg.writeInt(item.quantity);
      msg.writeBoolean(item.locked);
      msg.writeLong(item.createdTime);
      msg.writeLong(item.expireTime);
      if (ranges != null) {
         writeStatRanges(msg, ranges);
      } else {
         msg.writeByte(item.stats.size());
         for (ItemData.StatValue stat : item.stats) {
            msg.writeInt(stat.statTypeId());
            msg.writeBoolean(false); // false = giá trị đã roll (true = khoảng min-max, chỉ dùng khi xem mẫu)
            writeInts(msg, scaledValues(stat, item));
         }
      }
      msg.writeByte(item.soulCharms.size());
      for (ItemData charm : item.soulCharms) {
         writeItem(msg, charm);
      }
      msg.writeByte(item.elementStones.size());
      for (ItemData.ElementStone stone : item.elementStones) {
         if (stone == null) {
            msg.writeInt(-1);
            continue;
         }
         msg.writeInt(stone.templateId());
         msg.writeByte(stone.level());
         msg.writeInt(stone.statTypeId());
         writeInts(msg, stone.values());
      }
   }

   /** Giá trị dòng chỉ số theo cấp nâng (+4% / cấp, chỉ số % giữ nguyên) — item chỉ lưu chỉ số gốc. */
   private static int[] scaledValues(ItemData.StatValue stat, ItemData item) {
      int[] values = stat.values();
      if (item.starCount <= 0 || values == null) {
         return values;
      }
      int[] out = new int[values.length];
      for (int i = 0; i < values.length; i++) {
         out[i] = UpgradeRules.scaled(stat.statTypeId(), values[i], item);
      }
      return out;
   }

   /**
    * Dòng chỉ số dạng khoảng: int loại, bool true, byte n, n×(int min, int max). Dòng chỉ có mã (không giá trị) vẫn
    * gửi với n = 0 để hiện chữ (vd "Có tỉ lệ vĩnh viễn") — {@link RewardItem#create} thì bỏ dòng đó.
    */
   private static void writeStatRanges(Message msg, RewardItem reward) {
      int[][] min = reward.stats();
      int[][] max = reward.statsMax();
      int lines = 0;
      for (int[] line : min) {
         if (line != null && line.length >= 1) {
            lines++;
         }
      }
      msg.writeByte(lines);
      for (int k = 0; k < min.length; k++) {
         if (min[k] == null || min[k].length < 1) {
            continue; // dòng chỉ có mã (vd [55] "Có tỉ lệ vĩnh viễn") gửi 0 giá trị: client in nguyên mẫu chữ
         }
         msg.writeInt(min[k][0]);
         msg.writeBoolean(true);
         int n = Math.min(min[k].length - 1, Byte.MAX_VALUE);
         msg.writeByte(n);
         for (int j = 1; j <= n; j++) {
            msg.writeInt(min[k][j]);
            msg.writeInt(max[k][j]);
         }
      }
   }

   /** Mảng int có độ dài 1 byte phía trước. */
   static void writeInts(Message msg, int[] values) {
      int length = values == null ? 0 : Math.min(values.length, Byte.MAX_VALUE);
      msg.writeByte(length);
      for (int i = 0; i < length; i++) {
         msg.writeInt(values[i]);
      }
   }
}
