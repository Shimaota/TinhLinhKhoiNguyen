package com.tlnt.server.handler;

import com.tlnt.server.fishing.FishingService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 57 C→S: câu cá. Sub 0 ({@code sendRequestHarvest} — tên sai; nút tròn "nắm tay" {@code AvatarButton}, chỉ hiện
 * khi HUD ở chế độ câu cá = đang cầm Cần câu): quăng cần / kéo cần, không payload. Logic: {@link FishingService}.
 */
public final class FishingHandler implements PacketHandler {

   private final FishingService fishing;

   public FishingHandler(FishingService fishing) {
      this.fishing = fishing;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() != null && message.readByte() == 0) {
         fishing.press(session);
      }
   }
}
