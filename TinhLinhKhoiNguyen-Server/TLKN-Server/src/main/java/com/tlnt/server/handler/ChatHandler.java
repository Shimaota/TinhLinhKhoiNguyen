package com.tlnt.server.handler;

import com.tlnt.server.chat.ChatService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 26 C→S: chat. Tên method client đúng nghĩa:
 *
 * <pre>
 * sub | method client    | nơi gõ                                            | payload
 * 0   | sendChat         | ô chat nhanh (ChatInputPopup) / tab "Gần đây"     | str nội dung
 * 1   | sendPrivateChat (TÊN SAI) | tab "Thế giới" — tốn 1 Tù và, xong NV 6       | str nội dung
 * 3   | sendPartyChat (TÊN SAI) | tab "Bộ lạc" cửa sổ chat (TribeChatPanel)    | str nội dung
 * 2   | sendGuildChat (tab Tổ đội)   -> PartyService.chat (S→C 26/2 cho thành viên)
 * 4   | sendWhisper(tên, nội dung)    -> chat riêng (S→C 26/4 cho cả 2 bên)
 * </pre>
 * Tên method 1-3 bị tráo: tab Thế giới gửi sub 1, tab Tổ đội gửi sub 2, tab Bộ lạc gửi sub 3 — khớp đúng sub S→C 26/1/2/3.
 */
public final class ChatHandler implements PacketHandler {

   private final ChatService chat;

   public ChatHandler(ChatService chat) {
      this.chat = chat;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      switch (message.readByte()) {
         case 0 -> chat.say(session, message.readString());
         case 1 -> chat.world(session, message.readString());
         case 2 -> chat.party(session, message.readString()); // tab Tổ đội (sendGuildChat — tên sai)
         case 3 -> chat.tribe(session, message.readString());
         case 4 -> {
            String to = message.readString();
            chat.whisper(session, to, message.readString()); // tab Riêng (sendWhisper)
         }
         default -> {
         }
      }
   }
}
