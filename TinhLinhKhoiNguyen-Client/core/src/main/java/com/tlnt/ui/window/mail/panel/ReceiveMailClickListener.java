package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class ReceiveMailClickListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailMessageRow mailMessageRow;
   public ReceiveMailClickListener(MailMessageRow mailMessageRow) {
      super();
      this.mailMessageRow = mailMessageRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendSortInventory(this.mailMessageRow.selectedMailId);
   }
}
