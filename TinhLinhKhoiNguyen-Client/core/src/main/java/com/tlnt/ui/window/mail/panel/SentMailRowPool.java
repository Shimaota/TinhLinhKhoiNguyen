package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.utils.Pool;

public final class SentMailRowPool extends Pool<MailMessageRow.MailItemRow> {
   public com.tlnt.ui.window.mail.panel.MailMessageRow.InboxListPanel sentMailListPanel;
   public SentMailRowPool(MailMessageRow.InboxListPanel inboxListPanel) {
      super();
      this.sentMailListPanel = inboxListPanel;
   }

   @Override
   public final MailMessageRow.MailItemRow newObject() {
      MailMessageRow.MailItemRow row;
      (row = this.sentMailListPanel.mailMessageRow.new MailItemRow(this.sentMailListPanel.renderContext))
         .addListener(new OpenMailClickListener(this, row));
      return row;
   }
}
