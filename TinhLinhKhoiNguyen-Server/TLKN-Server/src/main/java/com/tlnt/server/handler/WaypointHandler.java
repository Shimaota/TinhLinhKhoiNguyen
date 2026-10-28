package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.World;

/**
 * Opcode 11 C→S: nhân vật chạm/bấm cửa dịch chuyển ({@code sendPlayerAction}, tên sai phía client).
 * Byte = vị trí cửa trong danh sách map đã gửi ở opcode 6.
 */
public final class WaypointHandler implements PacketHandler {

   private final World world;

   public WaypointHandler(World world) {
      this.world = world;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      world.useWaypoint(session, message.readByte());
   }
}
