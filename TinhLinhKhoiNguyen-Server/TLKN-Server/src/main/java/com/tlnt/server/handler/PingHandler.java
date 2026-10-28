package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 29: client gửi timestamp của chính nó, server chỉ cần echo lại y hệt
 * (xem {@code ServerPacketHandler.handlePingResponse}: client tự tính
 * {@code now - timestampEcho} để ra ping). Lane IO trong {@link Routes}: trả lời ngay trên luồng mạng,
 * không xếp hàng qua pool — giữ độ trễ đo được sát với round-trip thật.
 */
public final class PingHandler implements PacketHandler {

   @Override
   public void handle(PlayerSession session, Message message) {
      long clientTimestamp = message.readLong();
      Message reply = new Message(29);
      reply.writeLong(clientTimestamp);
      session.send(reply);
   }
}
