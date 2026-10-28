package com.tlnt.server.handler;

import com.tlnt.server.mail.MailService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;

/**
 * Opcode 40 C→S: hòm thư (cửa sổ {@code MailWindow}). Lane BLOCKING (gửi thư tra tên người nhận bằng SQL). Logic +
 * luật chống nhân bản: {@link MailService}.
 *
 * <pre>
 * sub | method client (tên sai)            | nút                                  | payload
 * 0   | sendRankingAction                  | bấm 1 thư chưa đọc ở hòm thư        | int mailId
 * 1   | sendEquipmentSet                   | "Gửi thư"                            | str người nhận, str chủ đề, str nội dung,
 *     |                                    |                                      | byte n, n×(int ô túi, int mẫu, int số lượng)
 * 3   | sendSortInventory                  | "Nhận thư" (chi tiết thư)            | int mailId
 * 4   | sendMultiItemAction                | "Xóa thư" (chi tiết / chọn nhiều)    | byte hộp (0 đến, 1 đã gửi), byte n, n×int id
 * 5   | sendRequestSettings                | "Nhận thư" ở danh sách = nhận tất cả | —
 * </pre>
 */
public final class MailHandler implements PacketHandler {

   private final MailService mails;

   public MailHandler(MailService mails) {
      this.mails = mails;
   }

   @Override
   public void handle(PlayerSession session, Message message) throws Exception {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> mails.read(session, message.readInt());
         case 1 -> send(session, message);
         case 3 -> mails.claim(session, message.readInt());
         case 4 -> delete(session, message);
         case 5 -> mails.claimAll(session);
         default -> {
         }
      }
   }

   private void send(PlayerSession session, Message message) throws Exception {
      String recipient = message.readString();
      String subject = message.readString();
      String content = message.readString();
      int count = message.readByte();
      if (count < 0) {
         return;
      }
      int[] slots = new int[count];
      int[] templateIds = new int[count];
      int[] quantities = new int[count];
      for (int i = 0; i < count; i++) {
         slots[i] = message.readInt();
         templateIds[i] = message.readInt();
         quantities[i] = message.readInt();
      }
      mails.send(session, recipient, subject, content, slots, templateIds, quantities);
   }

   private void delete(PlayerSession session, Message message) {
      byte box = message.readByte();
      int count = message.readByte();
      if (count <= 0) {
         return;
      }
      int[] ids = new int[count];
      for (int i = 0; i < count; i++) {
         ids[i] = message.readInt();
      }
      mails.delete(session, box, ids);
   }
}
