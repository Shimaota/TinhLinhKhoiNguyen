package com.tlnt.server.forge;

import com.tlnt.server.player.ItemData;

import java.util.Locale;

/**
 * Luật khảm bùa linh hồn (thợ rèn Ông Morgi -> "Khảm bùa linh hồn" = ForgeWindow tab "Linh hồn", op 44 chế độ 1).
 *
 * <ul>
 *   <li>Trang bị khảm được = trang bị nâng cấp được ({@link UpgradeRules#upgradable}). Mỗi 2 cấp nâng mở 1 lỗ: 2, 4, 6,
 *       8, 10 sao -> tối đa {@link #MAX_SOCKETS} bùa. Hết lỗ -> "Trang bị đã hết lỗ khảm". Bị giảm cấp sau khi khảm
 *       thì bùa đã khảm vẫn giữ, chỉ không khảm thêm được.</li>
 *   <li>Nguyên liệu: bùa linh hồn (item loại {@link #CHARM_TYPE}, mẫu 134..142 — bán ở shop 4). Bùa mang chỉ số đã
 *       roll lúc mua; khảm vào = nguyên món bùa (cả chỉ số) nằm trong {@link ItemData#soulCharms} của trang bị.</li>
 *   <li>Phí {@link #GOLD} vàng / lần (vàng khoá trừ trước), thành công hay thất bại đều mất.</li>
 *   <li>Tỉ lệ {@link #BASE_PERCENT}%, mỗi bùa CÙNG LOẠI đã khảm trên trang bị -{@link #SAME_PENALTY_PERCENT}%
 *       (bùa khác loại không ảnh hưởng). Có Cỏ may mắn ({@link #LUCK_HERB}) +{@link #LUCK_PERCENT}%.</li>
 *   <li>Thất bại: không có cỏ -> mất bùa; có cỏ -> chỉ mất cỏ (bùa giữ lại). Cỏ luôn bị dùng khi bấm khảm.</li>
 * </ul>
 */
public final class SoulRules {

   public static final int CHARM_TYPE = 14;
   public static final int LUCK_HERB = 171;
   public static final int MAX_SOCKETS = 5;
   /** Mỗi bấy nhiêu cấp nâng mở thêm 1 lỗ. */
   public static final int STARS_PER_SOCKET = 2;
   public static final long GOLD = 100_000L;
   public static final int BASE_PERCENT = 50;
   public static final int LUCK_PERCENT = 25;
   public static final int SAME_PENALTY_PERCENT = 10;

   private SoulRules() {
   }

   /** Bùa linh hồn (item loại 14). */
   public static boolean isCharm(ItemData item) {
      return item != null && item.typeId() == CHARM_TYPE;
   }

   /** Số lỗ khảm đã mở theo cấp nâng: 0..5. */
   public static int openSockets(ItemData equip) {
      return Math.min(MAX_SOCKETS, Math.max(0, equip.starCount / STARS_PER_SOCKET));
   }

   /** Cấp nâng cần để mở lỗ thứ {@code index} (0-based): 2, 4, 6, 8, 10. */
   public static int starsForSocket(int index) {
      return (index + 1) * STARS_PER_SOCKET;
   }

   /** Tỉ lệ (%) khảm bùa mẫu {@code charmTemplate} vào {@code equip}. */
   public static int chancePercent(ItemData equip, int charmTemplate, boolean luck) {
      int same = 0;
      for (ItemData socketed : equip.soulCharms) {
         if (socketed != null && socketed.templateId == charmTemplate) {
            same++;
         }
      }
      int chance = BASE_PERCENT - same * SAME_PENALTY_PERCENT + (luck ? LUCK_PERCENT : 0);
      return Math.max(0, Math.min(100, chance));
   }

   /** "100.0k" / "1.5M" như mẫu mô tả. */
   static String shortGold(long gold) {
      if (gold >= 1_000_000L) {
         return String.format(Locale.ROOT, "%.1fM", gold / 1_000_000.0);
      }
      if (gold >= 1_000L) {
         return String.format(Locale.ROOT, "%.1fk", gold / 1_000.0);
      }
      return Long.toString(gold);
   }
}
