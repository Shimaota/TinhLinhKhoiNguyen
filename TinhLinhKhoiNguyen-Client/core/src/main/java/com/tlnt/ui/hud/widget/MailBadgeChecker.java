package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.model.mail.MailMessage;

public final class MailBadgeChecker implements TargetMarker.BadgeCondition {
   public com.tlnt.ui.hud.widget.MenuButtonPanel menuButtonPanel;
   public MailBadgeChecker(MenuButtonPanel menuButtonPanel) {
      super();
      this.menuButtonPanel = menuButtonPanel;
   }

   @Override
   public final boolean hasBadge() {
      Array.ArrayIterator it = GameState.mailbox.inboxMails.iterator();

      while (it.hasNext()) {
         if (!((MailMessage)it.next()).read) {
            return true;
         }
      }

      return false;
   }
}
