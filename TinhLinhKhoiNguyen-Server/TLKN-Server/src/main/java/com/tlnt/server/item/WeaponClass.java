package com.tlnt.server.item;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.GamePack;
import com.tlnt.server.data.model.ItemAppearance;
import com.tlnt.server.player.ItemData;

/**
 * Class của nhân vật = loại vũ khí ({@code char_class.id}, cột {@code player.class_id}, classData phía client):
 * 1 Chưa vũ khí (lúc tạo nhân vật), 2 Rìu, 3 Cung tên, 4 Ná thun, 5 Giáo, 6 Roi da.
 *
 * Lần đầu trang bị vũ khí (class đang = 1) server hỏi xác nhận (opcode 32), đồng ý thì class thành loại của vũ khí
 * đó VĨNH VIỄN: sau này chỉ mặc được vũ khí cùng loại. Loại vũ khí lấy từ skin vũ khí trong
 * {@code item_appearance.skin_key} ("axe/lv3" -> Rìu...). Vũ khí không có skin loại nào (vd Cần câu) không đổi class
 * và ai cũng mặc được.
 */
public final class WeaponClass {

   public static final int NONE = 1;
   public static final int AXE = 2;
   public static final int BOW = 3;
   public static final int SLINGSHOT = 4;
   public static final int SPEAR = 5;
   public static final int WHIP = 6;

   private WeaponClass() {
   }

   public static boolean isValid(int classId) {
      return classId >= NONE && classId <= WHIP;
   }

   /** Class ứng với vũ khí này, {@link #NONE} nếu không phải vũ khí có loại. */
   public static int of(ItemData item) {
      ItemAppearance a = item == null ? null : GameDataCache.itemAppearance(item.templateId);
      return a == null ? NONE : ofSkin(a.skinKey());
   }

   /** "axe/lv3" -> {@link #AXE}...; khác -> {@link #NONE}. */
   static int ofSkin(String skinKey) {
      if (skinKey == null) {
         return NONE;
      }
      int slash = skinKey.indexOf('/');
      return switch (slash < 0 ? skinKey : skinKey.substring(0, slash)) {
         case "axe" -> AXE;
         case "bow" -> BOW;
         case "slingshot" -> SLINGSHOT;
         case "spear" -> SPEAR;
         case "whip" -> WHIP;
         default -> NONE;
      };
   }

   /** Tên hiển thị từ {@code char_class} ("Rìu", "Cung tên"...). */
   public static String name(int classId) {
      GamePack.CharClass c = GameDataCache.charClass(classId);
      return c != null ? c.name() : "vũ khí " + classId;
   }
}
