package com.tlnt.server.item;

/**
 * {@code item_type.id} có logic riêng trên server. Loại trang bị thường (áo, quần... {@code equip_slot} 0..9) không
 * cần liệt kê: đọc {@code item_type.equip_slot}.
 */
public final class ItemTypes {

   public static final int COSTUME = 19;
   public static final int TITLE = 20;
   public static final int HP = 21;
   public static final int MP = 22;
   public static final int HP_PERCENT = 24;
   public static final int MP_PERCENT = 25;
   public static final int HP_MP = 26;
   public static final int HP_MP_PERCENT = 27;
   public static final int STAMINA = 29;
   public static final int STAMINA_PERCENT = 30;
   public static final int MASK = 36;
   public static final int PET = 37;
   public static final int BACK = 48;
   /** "Kinh nghiệm x level": dùng nhận EXP = gốc × cấp nhân vật (quả mọng, ống tre kinh nghiệm). */
   public static final int EXP_PER_LEVEL = 49;

   /** stat_type.id đọc trên bình hồi: +X HP, +X% HP, +X MP, +X% MP. */
   public static final int STAT_HP = 3;
   public static final int STAT_HP_PERCENT = 4;
   public static final int STAT_MP = 5;
   public static final int STAT_MP_PERCENT = 6;

   private ItemTypes() {
   }

   public static boolean isRecovery(int typeId) {
      return typeId == HP || typeId == MP || typeId == HP_PERCENT || typeId == MP_PERCENT || typeId == HP_MP
            || typeId == HP_MP_PERCENT;
   }
}
