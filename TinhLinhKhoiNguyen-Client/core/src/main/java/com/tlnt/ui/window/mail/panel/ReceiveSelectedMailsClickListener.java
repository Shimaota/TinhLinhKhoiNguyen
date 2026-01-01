package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class ReceiveSelectedMailsClickListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailMessageRow.SentMailListPanel inboxListPanel;
   public ReceiveSelectedMailsClickListener(MailMessageRow.SentMailListPanel sentMailListPanel) {
      super();
      this.inboxListPanel = sentMailListPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      int[] mailIds = new int[this.inboxListPanel.mailMessageRow.selectedMailIds.size];
      int count = 0;

      for (Integer mailId : this.inboxListPanel.mailMessageRow.selectedMailIds) {
         mailIds[count++] = mailId;
      }

      ServerPacketSender.getInstance().sendMultiItemAction(1, mailIds);
      this.inboxListPanel.mailMessageRow.showMailDetail(null);
   }
}
