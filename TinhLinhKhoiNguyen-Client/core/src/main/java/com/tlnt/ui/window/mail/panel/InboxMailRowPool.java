package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.utils.Pool;

public final class InboxMailRowPool extends Pool<MailMessageRow.MailItemRow> {
   public com.tlnt.ui.window.mail.panel.MailMessageRow.SentMailListPanel inboxListPanel;
   public InboxMailRowPool(MailMessageRow.SentMailListPanel sentMailListPanel) {
      super();
      this.inboxListPanel = sentMailListPanel;
   }

   @Override
   public MailMessageRow.MailItemRow newObject() {
      return (this.inboxListPanel.mailMessageRow).new MailItemRow(this.inboxListPanel.renderContext);
   }
}
