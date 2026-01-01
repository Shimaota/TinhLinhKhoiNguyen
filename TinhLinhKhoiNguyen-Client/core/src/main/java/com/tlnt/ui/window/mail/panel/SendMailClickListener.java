package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class SendMailClickListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailComposePanel composePanel;
   public SendMailClickListener(MailComposePanel composePanel) {
      super();
      this.composePanel = composePanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.composePanel.sendMail();
   }
}
