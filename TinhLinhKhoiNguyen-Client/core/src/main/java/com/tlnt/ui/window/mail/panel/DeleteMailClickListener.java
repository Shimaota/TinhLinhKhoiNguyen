package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class DeleteMailClickListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailMessageRow mailMessageRow;
   public DeleteMailClickListener(MailMessageRow mailMessageRow) {
      super();
      this.mailMessageRow = mailMessageRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender sender = ServerPacketSender.getInstance();
      int[] mailIds = new int[1];
      mailIds[0] = this.mailMessageRow.selectedMailId;
      sender.sendMultiItemAction(this.mailMessageRow.selectedMailType, mailIds);
      this.mailMessageRow.showMailDetail(null);
   }
}
