package com.tlnt.server.item;

import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerSettings;

/**
 * 4 bộ sưu tập và các số hiệu client dùng cho từng cái (đã dò trong client, tên method client đều sai nghĩa):
 *
 * <pre>
 * bộ sưu tập  loại item  list op22  C→S cất vào / lấy ra (op27)   C→S dùng/tháo + ẩn     S→C chỉ số / đang dùng
 * Ngoại trang 19         sub 4      sub 6  / sub 7                 op 48 sub 1 / sub 0    op 48 sub 0 / sub 1
 * Danh hiệu   20         sub 6      sub 10 / sub 11                op 50 sub 1 / sub 0    op 50 sub 0 / sub 1
 * Mặt nạ      36         sub 8      sub 16 / sub 17                op 64 sub 1 / sub 0    op 64 sub 0 / sub 1
 * Thú cưng    37         sub 9      sub 18 / sub 19                op 66 sub 1 / sub 0    op 66 sub 0 / sub 1
 * Đeo lưng    48         sub 10     sub 20 / sub 21                op 78 sub 1 / sub 0    op 78 sub 0 / sub 1
 * </pre>
 *
 * Đeo lưng (tab "Đeo lưng" cửa sổ Tiềm năng, client decompile C3719): trước là 1 ô lẻ {@code PlayerData.back}, nay là
 * bộ sưu tập như 4 cái kia — "Trang bị" món loại 48 trong túi gửi op 27 sub 20 (không phải sub 0).
 */
public enum CollectionKind {
   COSTUME(ItemTypes.COSTUME, 4, 48, PlayerSettings.HIDE_COSTUME, "Trang phục", true),
   TITLE(ItemTypes.TITLE, 6, 50, PlayerSettings.HIDE_TITLE, "Danh hiệu", true),
   MASK(ItemTypes.MASK, 8, 64, PlayerSettings.HIDE_MASK, "Mặt nạ", true),
   PET(ItemTypes.PET, 9, 66, PlayerSettings.HIDE_PET, "Thú cưng", true),
   BACK(ItemTypes.BACK, 10, 78, PlayerSettings.HIDE_BACK, "Đeo lưng", true);

   /** {@code item_type.id} được cất vào bộ sưu tập này. */
   public final int itemTypeId;
   /** Opcode 22 sub của danh sách. */
   public final int inventorySub;
   /** Opcode dùng/tháo/ẩn (C→S) và chỉ số/đang dùng (S→C). */
   public final int opcode;
   /** Index cờ "Ẩn" trong {@link PlayerSettings#inventoryFlags}. */
   public final int hideFlag;
   public final String displayName;
   /** Đổi ngoại hình người khác thấy (thú cưng: con thú đi theo chủ) -> ẩn / hiện phải phát lại. */
   public final boolean affectsAppearance;

   CollectionKind(int itemTypeId, int inventorySub, int opcode, int hideFlag, String displayName,
         boolean affectsAppearance) {
      this.itemTypeId = itemTypeId;
      this.inventorySub = inventorySub;
      this.opcode = opcode;
      this.hideFlag = hideFlag;
      this.displayName = displayName;
      this.affectsAppearance = affectsAppearance;
   }

   public ItemCollection of(PlayerData d) {
      return switch (this) {
         case COSTUME -> d.costumes;
         case TITLE -> d.titles;
         case MASK -> d.masks;
         case PET -> d.pets;
         case BACK -> d.backs;
      };
   }

   /** Bộ sưu tập nhận loại item này, null nếu không có. */
   public static CollectionKind forItemType(int typeId) {
      for (CollectionKind kind : values()) {
         if (kind.itemTypeId == typeId) {
            return kind;
         }
      }
      return null;
   }

   public static CollectionKind forOpcode(int opcode) {
      for (CollectionKind kind : values()) {
         if (kind.opcode == opcode) {
            return kind;
         }
      }
      return null;
   }
}
