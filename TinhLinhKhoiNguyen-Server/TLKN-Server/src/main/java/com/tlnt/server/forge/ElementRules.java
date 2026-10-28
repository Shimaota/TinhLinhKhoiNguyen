package com.tlnt.server.forge;

import com.tlnt.server.player.ItemData;

import java.util.Map;

/**
 * Luật khảm đá nguyên tố (thợ rèn Ông Morgi -> "Cường hóa" -> "Khảm nguyên tố" = ForgeWindow tab Nguyên tố, op 44 chế độ
 * 2). Spec người dùng 2026-10-10.
 *
 * <ul>
 *   <li>Trang bị khảm được = trang bị nâng cấp được ({@link UpgradeRules#upgradable}); lỗ mở ở 2 / 4 / 6 / 8 / 10 sao
 *       (như bùa linh hồn, {@link SoulRules#openSockets}). Mỗi LOẠI đá chiếm 1 lỗ: khảm đá loại chưa có = lỗ mới cấp 1,
 *       khảm đá loại đã có = lỗ đó lên 1 cấp (tối đa {@link #MAX_LEVEL}).</li>
 *   <li>Lên cấp L cần {@link #cost} đá cùng loại, tỉ lệ {@link #ratePercent}; Cỏ may mắn +{@link #LUCK_PERCENT}%
 *       (tối đa 100%). Thất bại: có cỏ -> chỉ mất cỏ; không cỏ -> mất số đá đó. Không tốn vàng.</li>
 *   <li>Gỡ ra: nhận lại 50% tổng số đá đã cần để lên tới cấp hiện tại (tính như mọi lần đều thành công) — vd cấp 3 =
 *       (100 + 250 + 500) / 2 = 425 viên.</li>
 *   <li>1 món KHÔNG khảm được 2 viên cùng loại. Khảm thành công -> trang bị bị KHOÁ; gỡ ra -> đá trả về KHOÁ.</li>
 *   <li>Chỉ số cộng thẳng (không theo cấp nâng): bảng {@link #STONES}.</li>
 * </ul>
 */
public final class ElementRules {

   public static final int MAX_LEVEL = 10;
   public static final int LUCK_PERCENT = 25;
   /** Đá cần để lên cấp 1..10. */
   private static final int[] COST = {100, 250, 500, 1000, 2000, 4000, 8000, 16000, 32000, 64000};
   /** Tỉ lệ (%) lên cấp 1..10 (không cỏ). */
   private static final int[] RATE = {90, 80, 75, 70, 65, 60, 55, 50, 45, 40};

   /** 1 loại đá: dòng chỉ số (stat_type) + giá trị cấp L = {@code base} + {@code perLevel} × L. */
   public record Stone(String name, int statTypeId, int base, int perLevel, String unit) {
      public int value(int level) {
         return base + perLevel * level;
      }
   }

   /** Mẫu item (loại 13) -> chỉ số. */
   static final Map<Integer, Stone> STONES = Map.of(
         127, new Stone("Kim", 14, 0, 1, "% Sát thương chí mạng"),
         128, new Stone("Mộc", 15, 0, 100, " Giáp"),
         129, new Stone("Thủy", 5, 0, 500, " MP"),
         130, new Stone("Hỏa", 7, 25, 25, " Tấn công"),
         131, new Stone("Thổ", 3, 0, 500, " HP"),
         132, new Stone("Phong", 47, 0, 200, " Kháng sát thương"),
         133, new Stone("Lôi", 44, 0, 1, "% Khuếch đại tấn công"));

   private ElementRules() {
   }

   public static Stone stone(int templateId) {
      return STONES.get(templateId);
   }

   public static boolean isStone(int templateId) {
      return STONES.containsKey(templateId);
   }

   /** Số đá để lên cấp {@code level} (1..10); 0 nếu ngoài bảng. */
   public static int cost(int level) {
      return level >= 1 && level <= MAX_LEVEL ? COST[level - 1] : 0;
   }

   /** Tỉ lệ (%) lên cấp {@code level}, có cỏ +25 (tối đa 100). */
   public static int ratePercent(int level, boolean luck) {
      if (level < 1 || level > MAX_LEVEL) {
         return 0;
      }
      return Math.min(100, RATE[level - 1] + (luck ? LUCK_PERCENT : 0));
   }

   /** Gỡ đá cấp {@code level}: nhận lại floor(tổng đá cấp 1..level / 2). */
   public static int refund(int level) {
      long total = 0;
      for (int lv = 1; lv <= Math.min(level, MAX_LEVEL); lv++) {
         total += COST[lv - 1];
      }
      return (int) (total / 2);
   }

   /** Vị trí đá loại {@code templateId} đã khảm trên trang bị, -1 = chưa có. */
   public static int indexOf(ItemData equip, int templateId) {
      for (int i = 0; i < equip.elementStones.size(); i++) {
         ItemData.ElementStone s = equip.elementStones.get(i);
         if (s != null && s.templateId() == templateId) {
            return i;
         }
      }
      return -1;
   }

   /** Số lỗ đang có đá. */
   public static int used(ItemData equip) {
      int n = 0;
      for (ItemData.ElementStone s : equip.elementStones) {
         if (s != null) {
            n++;
         }
      }
      return n;
   }

   /** Đá loại {@code templateId} ở cấp {@code level} (dòng chỉ số gửi client + cộng vào nhân vật). */
   public static ItemData.ElementStone make(int templateId, int level) {
      Stone s = stone(templateId);
      return new ItemData.ElementStone(templateId, (byte) level, s.statTypeId(), new int[] {s.value(level)});
   }

   /** "+1% Sát thương chí mạng" / "+500 HP". */
   public static String describe(int templateId, int level) {
      Stone s = stone(templateId);
      return s == null ? "" : "+" + s.value(level) + s.unit();
   }
}
