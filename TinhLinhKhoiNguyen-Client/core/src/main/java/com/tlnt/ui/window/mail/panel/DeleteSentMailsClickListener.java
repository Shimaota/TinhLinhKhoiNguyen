package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class DeleteSentMailsClickListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailMessageRow.InboxListPanel sentMailListPanel;
   public DeleteSentMailsClickListener(MailMessageRow.InboxListPanel inboxListPanel) {
      super();
      this.sentMailListPanel = inboxListPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestSettings();
   }
}
