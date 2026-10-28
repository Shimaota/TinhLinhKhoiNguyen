package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.World;

/**
 * Opcode 5 sub 0 C→S ({@code PlayerMovementController} gửi mỗi frame vị trí đổi): x, y (pixel).
 * Lane IO (chạy ngay trên luồng mạng): chỉ ghi lại vị trí mới nhất (atomic), không đáng đưa vào hàng đợi — tick
 * của khu sẽ kiểm tra và phát gộp cho người khác. Chưa ở trong thế giới (chưa đăng nhập) thì {@link World} tự bỏ qua.
 */
public final class MoveHandler implements PacketHandler {

   private final World world;

   public MoveHandler(World world) {
      this.world = world;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (message.readByte() == 0) {
         world.move(session, message.readFloat(), message.readFloat());
      }
   }
}
