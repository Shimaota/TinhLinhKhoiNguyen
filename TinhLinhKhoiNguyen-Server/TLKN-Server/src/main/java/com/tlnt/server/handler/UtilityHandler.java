package com.tlnt.server.handler;

import com.tlnt.server.badge.SpiritBadgeService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 45 C→S: Huy hiệu Tinh Linh — nút Tiện ích trên HUD ({@code ChatButton}).
 *
 * <pre>
 * sub | method client (tên sai) | ý nghĩa
 * 0   | sendRequestDuel()       | bấm nút Tiện ích -> server gửi 45/1 danh sách nút (hết hạn: báo cần huy hiệu)
 * 1   | sendDuelAction(byte i)  | bấm nút thứ i trong cửa sổ Tiện ích
 * </pre>
 */
public final class UtilityHandler implements PacketHandler {

   private final SpiritBadgeService badge;

   public UtilityHandler(SpiritBadgeService badge) {
      this.badge = badge;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      switch (message.readByte()) {
         case 0 -> badge.openMenu(session);
         case 1 -> badge.choose(session, message.readByte());
         default -> {
         }
      }
   }
}
