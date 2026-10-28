package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.wheel.LuckyWheelService;

/**
 * Opcode 71 C→S: vòng quay may mắn (tab "Vòng quay" của {@code ShopWindow}). Logic: {@link LuckyWheelService}.
 *
 * <pre>
 * sub | method client (tên sai)  | nút                         | payload
 * 0   | sendRequestNotification  | mở tab                      | -
 * 1   | sendNotificationAction   | "Quay 1 lần" / "Quay 10 lần" | byte 0 / 1
 * </pre>
 */
public final class LuckyWheelHandler implements PacketHandler {

   private final LuckyWheelService wheel;

   public LuckyWheelHandler(LuckyWheelService wheel) {
      this.wheel = wheel;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> wheel.open(session);
         case 1 -> wheel.spin(session, message.readByte());
         default -> {
         }
      }
   }
}
