package com.tlnt.server.handler;

import com.tlnt.server.farm.PigService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 52 C→S: nông trại. Sub 0 ({@code sendHarvestAction} — bấm vào con vật, client tự đi tới cạnh rồi gửi): int id
 * con vật -> thu hoạch heo đã lớn ({@link PigService#harvest}). Sub 1 (client mới — nút "Lớn nhanh" trên con vật đang
 * chọn, decompile C3480 / C3872 m18011): int id -> hỏi "lớn nhanh tốn 5 lam thạch" ({@link PigService#askGrowFast}).
 */
public final class FarmHandler implements PacketHandler {

   private final PigService pigs;

   public FarmHandler(PigService pigs) {
      this.pigs = pigs;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> pigs.harvest(session, message.readInt());
         case 1 -> pigs.askGrowFast(session, message.readInt()); // nút "Lớn nhanh" trên heo (decompile C3480)
         default -> {
         }
      }
   }
}
