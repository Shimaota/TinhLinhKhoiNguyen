package com.tlnt.server.forge;

import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.player.ItemData;

import java.util.List;
import java.util.Locale;

/**
 * Luật Chuyển hóa trang bị (spec người dùng 2026-10-13): chuyển cấp cường hóa + hệ Nhật / Nguyệt ấn của 1 trang bị
 * sang 1 trang bị khác CÙNG LOẠI, CẤP CAO HƠN, chưa nâng gì.
 *
 * <ul>
 *   <li>Trang bị chuyển hóa: đã cường hóa Nhật ấn hoặc Nguyệt ấn (cấp ≥ 1), đã tháo hết đá nguyên tố + bùa linh hồn.</li>
 *   <li>Trang bị mới: chưa nâng cấp gì (cấp 0, chưa có hệ, không đá / bùa), cùng loại (Áo -> Áo...; vũ khí cùng dòng
 *       theo tên: Rìu -> Rìu...), cấp yêu cầu cao hơn (vũ khí: Đá / thường < Bạc < Vàng < Bất hoại). Không tính nhân vật
 *       (class) của món.</li>
 *   <li>Không chuyển hóa: trang bị Thủ lĩnh (loại 40-47 hoặc tên có "thủ lĩnh"), vũ khí Lục linh, Cần câu.</li>
 *   <li>Phí {@link #GOLD} vàng (trừ vàng khoá trước) + 1 {@link #CHARM}; tỉ lệ {@link #PERCENT}%. Thành công: mất trang
 *       bị cũ, trang bị mới nhận cấp + hệ (+ % từng cấp) và bị KHOÁ. Thất bại: trang bị cũ giữ nguyên cấp. Bùa mất
 *       cả 2 trường hợp.</li>
 * </ul>
 */
public final class TransmuteRules {

   /** Item 331 Bùa chuyển hóa. */
   public static final int CHARM = 331;
   public static final long GOLD = 1_000_000L;
   public static final int PERCENT = 75;

   /** Hậu tố chất liệu vũ khí theo bậc tăng dần (tên không có hậu tố = bậc 0, vd "Rìu Đá" cũng bậc 0). */
   private static final List<String> WEAPON_TIERS = List.of("Đá", "Bạc", "Vàng", "Bất hoại");
   private static final int WEAPON_TYPE = 7;
   private static final int FISHING_ROD = 164;

   private TransmuteRules() {
   }

   /** Loại trang bị chuyển hóa được (Áo / Quần / Găng / Giày / Vũ khí / Đai / Mũ / Vòng cổ thường). */
   public static boolean eligibleKind(ItemData item) {
      ItemPack.ItemTemplate t = item == null ? null : item.template();
      if (t == null || item.templateId == FISHING_ROD) {
         return false;
      }
      int type = item.typeId();
      if (type < 3 || type > 10) {
         return false; // Thủ lĩnh / Lục linh là loại 40-47
      }
      String name = t.name().toLowerCase(Locale.ROOT);
      return !name.contains("thủ lĩnh") && !name.contains("lục linh");
   }

   /** Đã cường hóa Nhật / Nguyệt ấn (đặt được vào ô "Trang bị chuyển hóa"). */
   public static boolean upgraded(ItemData item) {
      return item.starCount > 0 && item.starGrade != UpgradeRules.GRADE_NONE;
   }

   /** Chưa nâng gì (đặt được vào ô "Trang bị mới"). */
   public static boolean pristine(ItemData item) {
      return item.starCount == 0 && item.starGrade == UpgradeRules.GRADE_NONE && !hasSockets(item);
   }

   /** Còn đá nguyên tố / bùa linh hồn khảm trên món. */
   public static boolean hasSockets(ItemData item) {
      for (ItemData charm : item.soulCharms) {
         if (charm != null) {
            return true;
         }
      }
      for (ItemData.ElementStone stone : item.elementStones) {
         if (stone != null) {
            return true;
         }
      }
      return false;
   }

   /** Dòng vũ khí theo tên ("Rìu Bạc" -> "Rìu", "Roi da" -> "Roi da"); món không phải vũ khí -> null. */
   static String weaponFamily(ItemData item) {
      if (item.typeId() != WEAPON_TYPE || item.template() == null) {
         return null;
      }
      String name = item.template().name().trim();
      for (String tier : WEAPON_TIERS) {
         if (name.endsWith(" " + tier)) {
            return name.substring(0, name.length() - tier.length() - 1).trim();
         }
      }
      return name;
   }

   /** Bậc của món để so "thấp -> cao": vũ khí = bậc chất liệu, còn lại = cấp yêu cầu. */
   static int tier(ItemData item) {
      if (item.typeId() == WEAPON_TYPE) {
         String name = item.template().name().trim();
         for (int i = WEAPON_TIERS.size() - 1; i >= 0; i--) {
            if (name.endsWith(" " + WEAPON_TIERS.get(i))) {
               return i; // "Đá" = bậc 0 như vũ khí thường (tên không hậu tố)
            }
         }
         return 0;
      }
      return item.template().requiredLevel();
   }

   /** Lý do không chuyển {@code from} sang {@code to} được (kiểm tra cặp — cả 2 đã khác null), null = được. */
   public static String checkPair(ItemData from, ItemData to) {
      if (!eligibleKind(from) || !eligibleKind(to)) {
         return "Trang bị Thủ lĩnh và vũ khí Lục linh không thể chuyển hóa.";
      }
      if (!upgraded(from)) {
         return "Trang bị chuyển hóa phải được cường hóa Nhật ấn hoặc Nguyệt ấn.";
      }
      if (hasSockets(from)) {
         return "Hãy tháo hết đá nguyên tố và bùa linh hồn khỏi trang bị chuyển hóa.";
      }
      if (!pristine(to)) {
         return "Trang bị mới phải chưa được nâng cấp, khảm đá hay khảm bùa.";
      }
      if (from.typeId() != to.typeId()) {
         return "Chỉ chuyển hóa được sang trang bị cùng loại.";
      }
      String family = weaponFamily(from);
      if (family != null && !family.equalsIgnoreCase(weaponFamily(to))) {
         return "Chỉ chuyển hóa được sang vũ khí cùng loại (" + family + ").";
      }
      if (tier(to) <= tier(from)) {
         return "Chỉ chuyển hóa được từ trang bị cấp thấp sang trang bị cấp cao hơn.";
      }
      return null;
   }

   /** Thành công: {@code to} nhận cấp + hệ + % từng cấp + mốc Cường hóa tuần của {@code from}, bị khoá. */
   public static void transfer(ItemData from, ItemData to) {
      to.starCount = from.starCount;
      to.starGrade = from.starGrade;
      to.upgradePercents = from.upgradePercents == null ? null : from.upgradePercents.clone();
      to.upgradeWeek = from.upgradeWeek;
      to.upgradeWeekBase = from.upgradeWeekBase;
      to.locked = true;
   }

   /** Thất bại không làm đổi trang bị; bùa vẫn bị trừ trong {@link TransmuteService}. */
   public static void fail(ItemData from) {
      // Giữ nguyên cấp, hệ và % từng cấp.
   }
}
