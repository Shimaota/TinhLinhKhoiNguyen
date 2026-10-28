package com.tlnt.server.handler;

import com.tlnt.server.item.CollectionKind;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.tribe.TribeService;
import com.tlnt.server.util.Log;

/**
 * Opcode 27 C→S: thao tác item. Tên method client (ServerPacketSender) đều SAI nghĩa — nghĩa thật dò từ nút gọi nó
 * (ItemActionRegistry + các *ClickListener):
 *
 * <pre>
 * sub | method client        | nút (nơi bấm)                               | payload            | xử lý
 * 0   | sendPickupItem       | "Trang bị" đồ mặc (túi)                     | int ô túi          | equip
 * 1   | sendUseItem          | "Tháo ra" (bảng đồ mặc)                     | int equip_slot     | unequip
 * 2   | sendCraftItem        | "Sử dụng" / "Dùng nhiều" (túi)              | int ô túi, int sl  | use
 * 3   | sendRequestCraft     | "Vứt bỏ" (có trong client, chưa lên menu)   | int ô túi          | discard
 * 4   | sendRepairItem       | "Cất vào" (cửa sổ rương)                    | int ô túi          | storeToBox
 * 5   | sendIdentifyItem     | "Lấy ra" (ô rương)                          | int ô rương        | takeFromBox
 * 6/7 | sendDestroyItem / sendLockItem        | ngoại trang: cất vào / lấy ra | int ô túi / index |
 * 8   | sendSwapItems        | gán phím tắt HP/MP/thể lực                  | int ô túi, byte ô  | assignQuickSlot
 * 9   | sendDepositItem      | bấm ô dùng nhanh trên HUD                   | byte ô (3/4/1)     | useQuickSlot
 * 10/11 | sendWithdrawItem / sendStorageAction | danh hiệu: cất vào / lấy ra |                  |
 * 12  | sendEnchantItem      | "Khóa"                                      | int ô túi          | lock
 * 13  | sendRefineItem       | "Bán"                                       | int ô túi          | sell (chưa có giá)
 * 14  | sendSocketItem       | "Sử dụng" ở Rương bộ lạc (UseItemAction3)    | int ô kho          | TribeService
 * 15  | sendExtractSocket    | "Phân phát" ở Rương bộ lạc                   | int ô kho          | (chưa làm)
 * 16/17 | sendCompositeItem / sendDecompositeItem | mặt nạ: cất vào / lấy ra   |                  |
 * 18/19 | sendTransmuteItem / sendInheritItem     | thú cưng: cất vào / lấy ra |                  |
 * 20/21 | sendPutBackwear / sendTakeBackwear      | đeo lưng: "Trang bị" món loại 48 trong túi / "Lấy ra" ở tab Đeo lưng
 *       | (decompile C3872.m18047 / m17906)       | int ô túi / int index                                  |
 * </pre>
 */
public final class ItemHandler implements PacketHandler {

   private final ItemService items;
   private final TribeService tribes;

   public ItemHandler(ItemService items, TribeService tribes) {
      this.items = items;
      this.tribes = tribes;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      byte sub = message.readByte();
      switch (sub) {
         case 0 -> items.equip(session, message.readInt());
         case 1 -> items.unequip(session, message.readInt());
         case 2 -> items.use(session, message.readInt(), message.readInt());
         case 3 -> items.discard(session, message.readInt());
         case 4 -> items.storeToBox(session, message.readInt());
         case 5 -> items.takeFromBox(session, message.readInt());
         case 6 -> items.putInCollection(session, CollectionKind.COSTUME, message.readInt());
         case 7 -> items.takeFromCollection(session, CollectionKind.COSTUME, message.readInt());
         case 8 -> items.assignQuickSlot(session, message.readInt(), message.readByte());
         case 9 -> items.useQuickSlot(session, message.readByte());
         case 10 -> items.putInCollection(session, CollectionKind.TITLE, message.readInt());
         case 11 -> items.takeFromCollection(session, CollectionKind.TITLE, message.readInt());
         case 12 -> items.lock(session, message.readInt());
         case 13 -> items.sell(session, message.readInt());
         case 14 -> tribes.useWarehouseItem(session, message.readInt());
         case 15 -> tribes.distribute(session, message.readInt());
         case 16 -> items.putInCollection(session, CollectionKind.MASK, message.readInt());
         case 17 -> items.takeFromCollection(session, CollectionKind.MASK, message.readInt());
         case 18 -> items.putInCollection(session, CollectionKind.PET, message.readInt());
         case 19 -> items.takeFromCollection(session, CollectionKind.PET, message.readInt());
         case 20 -> items.putInCollection(session, CollectionKind.BACK, message.readInt());
         case 21 -> items.takeFromCollection(session, CollectionKind.BACK, message.readInt());
         default -> Log.debug("Session #%d gui opcode 27 sub %d chua ho tro", session.getId(), sub);
      }
   }
}
