package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;

public final class SentSelectAllCheckboxListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailMessageRow.InboxListPanel sentMailListPanel;
   public SentSelectAllCheckboxListener(MailMessageRow.InboxListPanel inboxListPanel) {
      super();
      this.sentMailListPanel = inboxListPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      boolean checked = this.sentMailListPanel.selectAllCheckbox.isChecked();
      Array.ArrayIterator it = this.sentMailListPanel.rowTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof MailMessageRow.MailItemRow) {
            MailMessageRow.MailItemRow row;
            (row = (MailMessageRow.MailItemRow)child).selectCheckbox.setChecked(checked);
            if (checked) {
               this.sentMailListPanel.mailMessageRow.selectedMailIds.add(row.mailData.id);
            } else {
               this.sentMailListPanel.mailMessageRow.selectedMailIds.remove(row.mailData.id);
            }
         }
      }
   }
}
