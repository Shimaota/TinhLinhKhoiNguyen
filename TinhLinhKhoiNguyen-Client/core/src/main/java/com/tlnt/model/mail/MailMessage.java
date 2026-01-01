package com.tlnt.model.mail;

import com.badlogic.gdx.utils.Array;
import com.tlnt.model.item.Item;

public final class MailMessage {
   public int id;
   public int boxType;
   public String senderName;
   public String receiverName;
   public String subject;
   public String content;
   public boolean attachmentClaimed;
   public Array<Item> attachments;
   public long sendTime;
   public boolean read;

   public MailMessage(int id, String senderName, String receiverName, String subject, String content, long sendTime, boolean read) {
      this.id = id;
      this.senderName = senderName;
      this.receiverName = receiverName;
      this.subject = subject;
      this.content = content;
      this.attachments = new Array<>();
      this.sendTime = sendTime;
      this.read = read;
   }

   public final void clear() {
      this.senderName = null;
      this.receiverName = null;
      this.subject = null;
      this.content = null;
      if (this.attachments != null) {
         Array.ArrayIterator it = this.attachments.iterator();

         while (it.hasNext()) {
            Item item;
            if ((item = (Item)it.next()) != null) {
               item.clear();
            }
         }

         this.attachments.clear();
         this.attachments = null;
      }
   }
}
