package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.social.SocialService;
import com.tlnt.server.world.World;

/**
 * Opcode 2 C→S. Sub 0 ({@code sendHarvestItem}, tên sai; int playerId): "Xem thông tin" 1 người — menu người chơi
 * ({@code TransferToStorageMenuAction}) và nút xem ở tab lời mời kết bạn ({@link SocialService#inspect}).
 * Sub 1 ({@code sendToggleMount}, boolean): nút MountButton trên HUD, bật/tắt khoá nhân vật (client gửi
 * trạng thái MUỐN chuyển tới = đảo cờ hiện tại). Việc thật chạy trên luồng tick của khu ({@link World#setLocked}).
 */
public final class PlayerActionHandler implements PacketHandler {

   private static final int SUB_INSPECT = 0;
   private static final int SUB_TOGGLE_LOCK = 1;

   private final World world;
   private final SocialService social;

   public PlayerActionHandler(World world, SocialService social) {
      this.world = world;
      this.social = social;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case SUB_TOGGLE_LOCK -> world.setLocked(session, message.readBoolean());
         case SUB_INSPECT -> social.inspect(session, message.readInt());
         default -> {
         }
      }
   }
}
