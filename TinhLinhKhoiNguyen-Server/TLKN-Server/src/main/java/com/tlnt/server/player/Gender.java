package com.tlnt.server.player;

/**
 * Giới tính / nhân vật gốc chọn lúc tạo nhân vật (cột {@code player.gender}): 1 Rinn, 2 Lyria, 3 Bruk.
 * Client gửi số này ở bảng tạo nhân vật (opcode 38, byte thứ 2).
 *
 * Nhân vật gốc mặc định của mỗi giới tính ({@code char_template.id} = jobData phía client: skeleton + avatar) ghi
 * cứng ở {@link #charTemplateId} — không lưu SQL. Ngoại trang đang dùng thay tạm bằng char_template khác
 * ({@code item/Appearance}). {@code char_base_stat} và {@code item_template.class_id} (áo Rinn / Lyria / Bruk)
 * đều theo số giới tính này.
 */
public final class Gender {

   public static final int RINN = 1;
   public static final int LYRIA = 2;
   public static final int BRUK = 3;

   private Gender() {
   }

   /**
    * % chỉ số riêng của nhân vật, nhân lên (gốc {@code char_base_stat} THÔ + tiềm năng + đồ + bộ sưu tập) ({@code item/ItemStats}) — KHÔNG vào buff
    * item loại 11 (Mật ong +100 Giáp = đúng +100). Rinn +10% MP, Lyria +5% Tấn công, Bruk +10% HP
    * và +5% Giáp. (+5% Khuếch đại tấn công của Rinn / +5% Sát thương chí mạng của Lyria đã nằm thẳng trong gốc.)
    *
    * @param statTypeId {@code stat_type.id} dòng "+X": 3 HP, 5 MP, 7 Tấn công, 15 Giáp
    */
   public static int bonusPercent(int gender, int statTypeId) {
      return switch (gender) {
         case RINN -> statTypeId == 5 ? 10 : 0;
         case LYRIA -> statTypeId == 7 ? 5 : 0;
         case BRUK -> statTypeId == 3 ? 10 : statTypeId == 15 ? 5 : 0;
         default -> 0;
      };
   }

   public static boolean isValid(int gender) {
      return gender >= RINN && gender <= BRUK;
   }

   /** {@code char_template.id} mặc định (không mặc ngoại trang): Rinn 1, Lyria 2, Bruk 3. */
   public static int charTemplateId(int gender) {
      return switch (gender) {
         case LYRIA -> 2;
         case BRUK -> 3;
         default -> 1;
      };
   }

   public static String name(int gender) {
      return switch (gender) {
         case RINN -> "Rinn";
         case LYRIA -> "Lyria";
         case BRUK -> "Bruk";
         default -> "?";
      };
   }
}
