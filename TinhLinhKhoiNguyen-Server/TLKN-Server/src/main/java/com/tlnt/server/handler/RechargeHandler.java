package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.recharge.RechargeService;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 72 C→S (client mới — cửa sổ "Nạp tiền", decompile C3872 m17976): sub 0 int id gói = bấm 1 gói -> server gửi
 * link trang nạp (S→C 72/1). Xem {@link RechargeService}.
 */
public final class RechargeHandler implements PacketHandler {

   private final RechargeService recharge;

   public RechargeHandler(RechargeService recharge) {
      this.recharge = recharge;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      if (message.readByte() == 0) {
         recharge.choose(session, message.readInt());
      }
   }
}
