package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;

public final class InboxSelectAllCheckboxListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.MailMessageRow.SentMailListPanel inboxListPanel;
   public InboxSelectAllCheckboxListener(MailMessageRow.SentMailListPanel sentMailListPanel) {
      super();
      this.inboxListPanel = sentMailListPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      boolean checked = this.inboxListPanel.selectAllCheckbox.isChecked();
      Array.ArrayIterator it = this.inboxListPanel.rowTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof MailMessageRow.MailItemRow) {
            MailMessageRow.MailItemRow row;
            (row = (MailMessageRow.MailItemRow)child).selectCheckbox.setChecked(checked);
            if (checked) {
               this.inboxListPanel.mailMessageRow.selectedMailIds.add(row.mailData.id);
            } else {
               this.inboxListPanel.mailMessageRow.selectedMailIds.remove(row.mailData.id);
            }
         }
      }
   }
}
