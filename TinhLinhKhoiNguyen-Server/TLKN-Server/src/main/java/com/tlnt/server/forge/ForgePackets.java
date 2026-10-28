package com.tlnt.server.forge;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;


/**
 * Gói mở giao diện lò rèn (NPC Ông Morgi). Mới chỉ MỞ cửa sổ — logic nâng cấp / khảm / tinh luyện / chế tác làm sau.
 *
 * <pre>
 * S→C 44/0 ({@code handlePartyUpdate} — tên sai): byte 0, byte chế độ -> client reset {@code forgeData}, mở
 *          {@code ForgeWindow} (COMBINE_POPUP) chỉ hiện tab của chế độ đó
 * S→C 60/0 ({@code handleGuildSkillTree} — tên sai): byte n, n×item công thức -> mở {@code WeaponCraftWindow}
 *          ("Chế Tác Vũ Khí"); n phải ≥ 1 (client đọc công thức 0 ngay)
 * </pre>
 */
public final class ForgePackets {

   /** Chế độ {@code ForgeWindow} = tab: 0 Nâng cấp, 1 Linh hồn, 2 Nguyên tố, 3 Pha chế, 4 Chế tác, 5 Luyện đá, 6 Chuyển hóa. */
   public static final int MODE_UPGRADE = 0;
   public static final int MODE_SOUL = 1;
   public static final int MODE_ELEMENT = 2;
   public static final int MODE_BREW = 3;
   public static final int MODE_CRAFT = 4;
   public static final int MODE_REFINE = 5;
   /** Tab "Chuyển hóa" (client mới — {@code ForgeTransmutePanel}, decompile C3694). */
   public static final int MODE_TRANSMUTE = 6;

   private ForgePackets() {
   }

   /** Op 44 sub 0: mở lò rèn ở tab {@code mode}. */
   public static Message open(int mode) {
      Message msg = new Message(Opcode.FORGE);
      msg.writeByte(0);
      msg.writeByte(mode);
      return msg;
   }
}
