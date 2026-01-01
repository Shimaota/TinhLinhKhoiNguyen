package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class OpenMailClickListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailMessageRow.MailItemRow mailItemRow;
   public com.tlnt.ui.window.mail.panel.SentMailRowPool rowPool;
   public OpenMailClickListener(SentMailRowPool rowPool, MailMessageRow.MailItemRow mailItemRow) {
      super();
      this.rowPool = rowPool;
      this.mailItemRow = mailItemRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.mailItemRow.mailData.boxType == 0 && !this.mailItemRow.mailData.read) {
         ServerPacketSender.getInstance().sendRankingAction(this.mailItemRow.mailData.id);
      }
   }
}
