package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class DeleteSelectedMailsClickListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailMessageRow.InboxListPanel inboxListPanel;
   public DeleteSelectedMailsClickListener(MailMessageRow.InboxListPanel inboxListPanel) {
      super();
      this.inboxListPanel = inboxListPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      int[] mailIds = new int[this.inboxListPanel.mailMessageRow.selectedMailIds.size];
      int count = 0;

      for (Integer mailId : this.inboxListPanel.mailMessageRow.selectedMailIds) {
         mailIds[count++] = mailId;
      }

      ServerPacketSender sender = ServerPacketSender.getInstance();
      sender.sendMultiItemAction(0, mailIds);
      this.inboxListPanel.mailMessageRow.showMailDetail(null);
   }
}
