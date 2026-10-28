package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.plant.PlantService;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 54 C→S: cây hái được. Sub 0 = bấm hái (int id thực thể cây) — client {@code sendGuildVaultAction} (tên sai),
 * gọi từ {@code MoveToPlantCallback} sau khi tự đi tới sát cây. Logic: {@link PlantService}.
 */
public final class PlantHandler implements PacketHandler {

   private final PlantService plants;

   public PlantHandler(PlantService plants) {
      this.plants = plants;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() != null && message.readByte() == 0) {
         plants.harvest(session, message.readInt());
      }
   }
}
