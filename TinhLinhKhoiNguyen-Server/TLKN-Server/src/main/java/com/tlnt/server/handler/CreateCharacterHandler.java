package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.player.PlayerService;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 38 C→S (CharacterCreateDialog -> {@code sendEncryptedPacket}, tên sai): tên nhân vật (UTF) +
 * giới tính (byte, 1 Rinn / 2 Lyria / 3 Bruk — {@code player/Gender}). Chỉ nhận khi đã đăng nhập.
 */
public final class CreateCharacterHandler implements PacketHandler {

   private final PlayerService playerService;

   public CreateCharacterHandler(PlayerService playerService) {
      this.playerService = playerService;
   }

   @Override
   public void handle(PlayerSession session, Message message) throws Exception {
      String name = message.readString();
      int gender = message.readByte();
      playerService.createCharacter(session, name, gender);
   }
}
