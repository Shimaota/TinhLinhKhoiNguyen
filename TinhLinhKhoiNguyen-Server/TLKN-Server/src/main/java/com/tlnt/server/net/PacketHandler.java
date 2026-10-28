package com.tlnt.server.net;

import com.tlnt.server.session.PlayerSession;

/** Xử lý 1 opcode cụ thể. Tương đương 1 nhánh trong switch khổng lồ của MessageHandler bên client. */
@FunctionalInterface
public interface PacketHandler {

   void handle(PlayerSession session, Message message) throws Exception;
}
