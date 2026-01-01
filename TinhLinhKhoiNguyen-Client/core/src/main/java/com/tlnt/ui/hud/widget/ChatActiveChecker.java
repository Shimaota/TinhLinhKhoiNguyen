package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.model.chat.PrivateConversation;

public final class ChatActiveChecker implements TargetMarker.BadgeCondition {
   public com.tlnt.ui.hud.widget.MessageNotificationButton chatButton;
   public ChatActiveChecker(MessageNotificationButton chatButton) {
      super();
      this.chatButton = chatButton;
   }

   @Override
   public final boolean hasBadge() {
      if (GameState.chatManager.privateDirty) {
         return true;
      } else {
         Array.ArrayIterator it = GameState.chatManager.privateConversations.iterator();

         while (it.hasNext()) {
            if (((PrivateConversation)it.next()).hasUnread) {
               return true;
            }
         }

         return false;
      }
   }
}
