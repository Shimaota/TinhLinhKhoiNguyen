package com.tlnt.server.handler;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.offline.OfflineTrainingService;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 77 C→S (decompile C3872.m18051 từ nút "Luyện tập Offline" của tab Thiết lập tự động C3764 m17385):
 *
 * <pre>
 * sub | payload                         | xử lý
 * 0   | byte n, n × int mob_template    | hỏi xác nhận treo offline (n = 0: mọi loại quái — ô "Tất cả")
 * </pre>
 */
public final class OfflineTrainingHandler implements PacketHandler {

   private final OfflineTrainingService offline;

   public OfflineTrainingHandler(OfflineTrainingService offline) {
      this.offline = offline;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (message.readByte() != 0) {
         return;
      }
      int n = message.readByte() & 0xFF;
      if (n > OfflineTrainingService.MAX_TEMPLATES) {
         return;
      }
      int[] templates = new int[n];
      for (int i = 0; i < n; i++) {
         templates[i] = message.readInt();
      }
      offline.request(session, templates);
   }
}
