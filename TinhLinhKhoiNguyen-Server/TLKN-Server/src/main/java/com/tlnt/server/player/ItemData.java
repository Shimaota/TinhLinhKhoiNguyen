package com.tlnt.server.player;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ItemPack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 1 vật phẩm người chơi sở hữu. Field khớp đúng thứ tự client đọc trong
 * {@code ServerPacketHandler.readItemFromMessage}, lưu DB dạng JSON trong các cột túi đồ.
 * Ô trống trong túi = phần tử {@code null} (client đọc templateId = -1).
 */
public final class ItemData {

   /** Số lượng tối đa 1 ô (1 chồng); đầy thì phần dư sang ô khác ({@code ItemContainer.add}). */
   public static final int MAX_STACK = 99_999;

   public int templateId;
   /** Index vào {@code Item.StarGrade.getValues()} phía client. */
   public byte starGrade;
   public byte starCount;
   /**
    * % chỉ số gốc cộng thêm của TỪNG cấp nâng (phần tử i = cấp i + 1): 4 thường, 12 nếu cấp đó nâng bằng Ngọc trai đen
    * ({@code forge/UpgradeRules}). null = mọi cấp 4% (đồ cũ / chưa dùng ngọc). Chỉ số gốc (chưa nâng) vẫn là {@link #stats}.
    */
   public byte[] upgradePercents;
   /**
    * Top tuần "Cường hóa": mã tuần ({@code ranking/WeekClock}) của lần nâng cấp đầu tiên trong tuần đó + cấp của món
    * ngay trước lần nâng ấy. Điểm cường hóa tuần của món = điểm(cấp hiện tại) - điểm(cấp đầu tuần) nếu {@code
    * upgradeWeek} là tuần đang xét (forge/UpgradeService ghi, ranking/EnhanceScore đọc). 0 = chưa nâng tuần nào.
    */
   public int upgradeWeek;
   public byte upgradeWeekBase;
   public int quantity = 1;
   public boolean locked;
   public long createdTime;
   /** Hạn dùng: thời điểm epoch ms hết hạn; {@link #PERMANENT} (-1) = vĩnh viễn (client chỉ hiện "Hạn sử dụng" khi khác -1). */
   public long expireTime = PERMANENT;
   public List<StatValue> stats = new ArrayList<>();
   /** Bùa linh hồn gắn vào món đồ (cùng cấu trúc item). */
   public List<ItemData> soulCharms = new ArrayList<>();
   /** Ngọc nguyên tố khảm vào; phần tử null = lỗ trống. */
   public List<ElementStone> elementStones = new ArrayList<>();

   /** Dòng chỉ số đã roll của món đồ (client: StatLine với values, không phải range). */
   public record StatValue(int statTypeId, int[] values) {
   }

   public record ElementStone(int templateId, byte level, int statTypeId, int[] values) {
   }

   /** Mẫu item trong dữ liệu tĩnh, null nếu không có. */
   public ItemPack.ItemTemplate template() {
      return GameDataCache.itemTemplate(templateId);
   }

   /** Loại item ({@code item_type}), null nếu mẫu/loại không có. */
   public ItemPack.ItemType type() {
      ItemPack.ItemTemplate template = template();
      return template == null ? null : GameDataCache.itemType(template.typeId());
   }

   public int typeId() {
      ItemPack.ItemTemplate template = template();
      return template == null ? -1 : template.typeId();
   }

   /**
    * Cộng dồn được với {@code other} không: cùng mẫu, không phải đồ trang bị, cùng khoá, cùng hạn dùng, cùng dòng
    * chỉ số (vd 2 bình máu hồi khác nhau thì không gộp).
    */
   /** Loại cộng dồn được (không phải trang bị, không khảm bùa) — chưa xét món kia. */
   public boolean stackable() {
      ItemPack.ItemType type = type();
      return type != null && !type.equippable() && soulCharms.isEmpty();
   }

   public boolean canStackWith(ItemData other) {
      if (other == null || other == this || other.templateId != templateId || other.locked != locked
            || other.expireTime != expireTime) {
         return false;
      }
      ItemPack.ItemType type = type();
      if (type == null || type.equippable()) {
         return false;
      }
      if (stats.size() != other.stats.size() || !soulCharms.isEmpty() || !other.soulCharms.isEmpty()) {
         return false;
      }
      for (int i = 0; i < stats.size(); i++) {
         StatValue a = stats.get(i);
         StatValue b = other.stats.get(i);
         if (a.statTypeId() != b.statTypeId() || !Arrays.equals(a.values(), b.values())) {
            return false;
         }
      }
      return true;
   }

   /** Tách {@code amount} món ra thành 1 item mới (bản sao cùng thuộc tính), phần còn lại giữ ở đây. */
   public ItemData split(int amount) {
      ItemData copy = new ItemData();
      copy.templateId = templateId;
      copy.starGrade = starGrade;
      copy.starCount = starCount;
      copy.upgradePercents = upgradePercents == null ? null : upgradePercents.clone();
      copy.upgradeWeek = upgradeWeek;
      copy.upgradeWeekBase = upgradeWeekBase;
      copy.quantity = amount;
      copy.locked = locked;
      copy.createdTime = createdTime;
      copy.expireTime = expireTime;
      copy.stats = new ArrayList<>(stats);
      quantity -= amount;
      return copy;
   }

   /**
    * Bản sao sâu (cả bùa / ngọc), không dính gì tới món gốc: sửa bản sao không làm đổi món gốc. Dòng chỉ số là record
    * không ai sửa mảng bên trong nên dùng chung được.
    */
   public ItemData copy() {
      ItemData copy = new ItemData();
      copy.templateId = templateId;
      copy.starGrade = starGrade;
      copy.starCount = starCount;
      copy.upgradePercents = upgradePercents == null ? null : upgradePercents.clone();
      copy.quantity = quantity;
      copy.locked = locked;
      copy.createdTime = createdTime;
      copy.expireTime = expireTime;
      copy.stats = new ArrayList<>(stats);
      copy.soulCharms = new ArrayList<>(soulCharms.size());
      for (ItemData charm : soulCharms) {
         copy.soulCharms.add(charm == null ? null : charm.copy());
      }
      copy.elementStones = new ArrayList<>(elementStones);
      return copy;
   }

   /** Sửa dữ liệu thiếu sau khi đọc JSON (list null -> rỗng, số lượng <= 0 -> 1). */
   public ItemData normalize() {
      if (stats == null) {
         stats = new ArrayList<>();
      }
      stats.removeIf(Objects::isNull);
      if (soulCharms == null) {
         soulCharms = new ArrayList<>();
      }
      soulCharms.removeIf(Objects::isNull);
      if (elementStones == null) {
         elementStones = new ArrayList<>();
      }
      if (quantity <= 0) {
         quantity = 1;
      }
      if (expireTime <= 0) {
         expireTime = PERMANENT; // 0 / số âm = vĩnh viễn
      }
      return this;
   }

   /** {@link #expireTime} = vĩnh viễn. */
   public static final long PERMANENT = -1L;

   /** Đã hết hạn tại thời điểm {@code now} (ms). */
   public boolean isExpired(long now) {
      return expireTime > 0 && now >= expireTime;
   }

   public static ItemData of(int templateId, int quantity) {
      ItemData item = new ItemData();
      item.templateId = templateId;
      item.quantity = quantity;
      item.createdTime = System.currentTimeMillis();
      com.tlnt.server.item.GearCharges.ensureStat(item); // phụ kiện có lượt: dòng "Hiệu lực" / "Sức chống chịu" đầy
      return item;
   }
}
