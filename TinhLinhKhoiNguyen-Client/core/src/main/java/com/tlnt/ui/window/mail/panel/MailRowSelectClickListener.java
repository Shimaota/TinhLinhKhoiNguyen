package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class MailRowSelectClickListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailMessageRow mailMessageRow;
   public com.tlnt.ui.window.mail.panel.MailMessageRow.MailItemRow mailRow;
   public MailRowSelectClickListener(MailMessageRow.MailItemRow mailRow, MailMessageRow mailMessageRow) {
      super();
      this.mailRow = mailRow;
      this.mailMessageRow = mailMessageRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.mailRow.mailData != null) {
         this.mailRow.mailMessageRow.showMailDetail(this.mailRow.mailData);
      }
   }
}
