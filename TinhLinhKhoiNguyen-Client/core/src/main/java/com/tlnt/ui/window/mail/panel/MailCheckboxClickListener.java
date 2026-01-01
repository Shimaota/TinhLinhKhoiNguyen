package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class MailCheckboxClickListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailMessageRow.MailItemRow mailItemRow;
   public MailCheckboxClickListener(MailMessageRow.MailItemRow mailItemRow) {
      super();
      this.mailItemRow = mailItemRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.mailItemRow.selectCheckbox.isChecked()) {
         this.mailItemRow.mailMessageRow.selectedMailIds.add(this.mailItemRow.mailData.id);
      } else {
         this.mailItemRow.mailMessageRow.selectedMailIds.remove(this.mailItemRow.mailData.id);
      }
   }
}
