package com.tlnt.model.mail;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;

public final class Mailbox {
   public final Array<MailMessage> inboxMails = new Array<>();
   public final Array<MailMessage> sentMails = new Array<>();
   public final Array<Item> composeAttachments = new Array<>();
   public int maxAttachments;

   public Mailbox() {
      this.maxAttachments = 6;
   }

   public final void addComposeAttachment(Item item) {
      if (this.composeAttachments.size >= 6) {
         GameMain.postFloatingMessage("Đã đạt tối đa số lượng vật phẩm gửi qua thư!");
      } else {
         this.composeAttachments.add(item);
      }
   }

}
