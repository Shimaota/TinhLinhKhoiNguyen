package com.tlnt.server.tribe;

/**
 * Ảnh chụp bất biến "bộ lạc của nhân vật này" gắn vào {@code PlayerData.tribe} (volatile): khối chỉ số đọc tên / huy
 * hiệu trên đầu, {@code PlayerStats.of} + luồng tick khu đọc buff. {@link TribeService} thay cả bản khi bộ lạc đổi.
 *
 * @param badge icon cạnh tên bộ lạc trên đầu nhân vật: danh hiệu (Tù trưởng / Phó / Tài chủ / Kim chủ / Công thần),
 *              rỗng với thành viên thường; biểu tượng bộ lạc được hiển thị riêng trong cửa sổ Bộ lạc
 * @param skillMask bit {@code 1 << id} = bộ lạc có kỹ năng {@code tribe_skill.id}
 * @param emblemLines dòng chỉ số của biểu tượng đang hiệu lực, cặp (stat_type, giá trị); rỗng = không có
 */
public record TribeTag(int id, String name, String badge, int skillMask, int[][] emblemLines) {

   public boolean has(int skillId) {
      return (skillMask & 1 << skillId) != 0;
   }

   /** Cùng tên + huy hiệu (khối chỉ số phát cho người xung quanh không đổi). */
   boolean sameLook(TribeTag other) {
      return other != null && other.id == id && other.name.equals(name) && other.badge.equals(badge);
   }
}
