package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class SentTabClickListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailMessageRow mailMessageRow;
   public SentTabClickListener(MailMessageRow mailMessageRow) {
      super();
      this.mailMessageRow = mailMessageRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.mailMessageRow.selectedMailIds.clear();
      this.mailMessageRow.sentMailListPanel.setVisible(false);
      this.mailMessageRow.inboxListPanel.setVisible(true);
   }
}
