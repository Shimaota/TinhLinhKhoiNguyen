package com.tlnt.server.handler;

import com.tlnt.server.item.GroundItemService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 30 C→S: vật phẩm dưới đất. Sub 0 = nhặt (int id vật phẩm) — client {@code sendQuestAction} (tên sai), gọi từ
 * {@code MoveToItemCallback} sau khi tự đi tới sát vật phẩm. Logic: {@link GroundItemService}.
 */
public final class PickupHandler implements PacketHandler {

   private final GroundItemService ground;

   public PickupHandler(GroundItemService ground) {
      this.ground = ground;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() != null && message.readByte() == 0) {
         ground.pickUp(session, message.readInt());
      }
   }
}
