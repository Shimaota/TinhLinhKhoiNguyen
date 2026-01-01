package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class DeclineInviteClickListener extends ClickListener {
   public com.tlnt.ui.window.action.InvitationConfirmPopup popup;
   public DeclineInviteClickListener(InvitationConfirmPopup popup) {
      super();
      this.popup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.popup.hide();
   }
}
