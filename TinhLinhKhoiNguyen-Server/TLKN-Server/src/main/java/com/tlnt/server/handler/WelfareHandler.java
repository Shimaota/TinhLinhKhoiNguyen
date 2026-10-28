package com.tlnt.server.handler;

import com.tlnt.server.giftcode.GiftcodeService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.welfare.WelfareService;

/**
 * Opcode 33 C→S: phúc lợi (cửa sổ {@code GiftcodeWindow}). Logic: {@link WelfareService}, giftcode:
 * {@link GiftcodeService}.
 *
 * <pre>
 * sub | method client (tên sai) | nút                          | payload
 * 0   | sendNpcInteract         | "Nhận" 1 mốc (trạng thái 1)  | byte tab, byte mốc
 * 1   | sendDialogResponse      | "Xác nhận" tab Giftcode      | str mã
 * </pre>
 */
public final class WelfareHandler implements PacketHandler {

   private final WelfareService welfare;
   private final GiftcodeService giftcodes;

   public WelfareHandler(WelfareService welfare, GiftcodeService giftcodes) {
      this.welfare = welfare;
      this.giftcodes = giftcodes;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> {
            int tab = message.readByte();
            int index = message.readByte();
            welfare.claim(session, tab, index);
         }
         case 1 -> giftcodes.redeem(session, message.readString());
         default -> {
         }
      }
   }
}
