package com.tlnt.server.mail;

import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.player.ItemData;

import java.util.List;

/**
 * Gói S→C op 40 (client {@code ServerPacketHandler.handleFriendOrGuildList} — tên sai, thật ra là hòm thư). Client
 * xoá danh sách cũ rồi đọc lại toàn bộ:
 * <pre>
 * sub 0 hòm thư  : byte n, n×(int id, str người gửi, str người nhận, str chủ đề, str nội dung, long lúc gửi,
 *                  bool đã đọc, bool đã nhận, byte m, m×item)
 * sub 1 đã gửi   : byte n, n×(int id, str người gửi, str người nhận, str chủ đề, str nội dung, long lúc gửi,
 *                  byte m, m×item)
 * </pre>
 */
final class MailPackets {

   private MailPackets() {
   }

   static Message list(byte box, List<Mail> mails) {
      Message msg = new Message(Opcode.MAIL);
      msg.writeByte(box);
      int n = Math.min(mails.size(), Byte.MAX_VALUE);
      msg.writeByte(n);
      for (int i = 0; i < n; i++) {
         Mail mail = mails.get(i);
         msg.writeInt(mail.id);
         msg.writeString(mail.senderName);
         msg.writeString(mail.receiverName);
         msg.writeString(mail.subject);
         msg.writeString(mail.content);
         msg.writeLong(mail.createdAt);
         if (box == Mail.INBOX) {
            msg.writeBoolean(mail.read);
            msg.writeBoolean(mail.claimed);
         }
         msg.writeByte(mail.items.size());
         for (ItemData item : mail.items) {
            ItemPackets.writeItem(msg, item);
         }
      }
      return msg;
   }
}
