package com.tlnt.server.handler;

import com.tlnt.server.chat.ChatService;
import com.tlnt.server.item.UsableItems;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.net.Packets;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.util.Log;

/**
 * Opcode 70 C→S: bấm đồng ý ở hộp nhập chữ server mở ({@code Packets.inputDialog}) — client
 * {@code ChatDialogCallback -> sendChannelChat} (tên sai): byte kênh, str chữ đã nhập.
 *
 * <pre>
 * kênh | mở từ                     | xử lý
 * 1    | dùng Tù và (item 239)     | ChatService.world (chat thế giới, tốn 1 Tù và)
 * 2    | dùng Thẻ đổi tên (308)    | UsableItems.rename (SQL -> lane BLOCKING)
 * </pre>
 */
public final class InputHandler implements PacketHandler {

   private final ChatService chat;
   private final UsableItems usables;

   public InputHandler(ChatService chat, UsableItems usables) {
      this.chat = chat;
      this.usables = usables;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      byte channel = message.readByte();
      String text = message.readString();
      switch (channel) {
         case Packets.INPUT_WORLD_CHAT -> chat.world(session, text);
         case Packets.INPUT_RENAME -> usables.rename(session, text);
         default -> Log.debug("Session #%d gui opcode 70 kenh %d khong ho tro", session.getId(), channel);
      }
   }
}
