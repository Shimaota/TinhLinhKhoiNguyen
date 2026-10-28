package com.tlnt.server.handler;

import com.tlnt.server.item.CollectionKind;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

/**
 * Opcode 48 (ngoại trang) / 50 (danh hiệu) / 64 (mặt nạ) / 66 (thú cưng) / 78 (đeo lưng) C→S — cùng 1 kiểu, 1 instance cho mỗi opcode:
 *
 * <pre>
 * sub | ví dụ method client (op 48)    | nút                                   | payload
 * 0   | sendToggleAutoAttack           | checkbox "Ẩn ngoại trang/danh hiệu..." | bool ẩn
 * 1   | sendSelectCharacter            | "Dùng" (index trong danh sách) / "Tháo" (-1) | int index
 * 2   | sendDeleteCharacter            | nút "Mở rộng": +5 ô (tối đa 50)         | —
 * </pre>
 */
public final class CollectionHandler implements PacketHandler {

   private final ItemService items;
   private final CollectionKind kind;

   public CollectionHandler(ItemService items, CollectionKind kind) {
      this.items = items;
      this.kind = kind;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      byte sub = message.readByte();
      switch (sub) {
         case 0 -> items.setHidden(session, kind, message.readBoolean());
         case 1 -> items.selectInCollection(session, kind, message.readInt());
         case 2 -> items.expandCollection(session, kind);
         default -> Log.debug("Session #%d gui opcode %d sub %d chua ho tro", session.getId(), kind.opcode, sub);
      }
   }
}
