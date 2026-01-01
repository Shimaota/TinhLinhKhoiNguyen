package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class InvitationAcceptClickListener extends ClickListener {
   public com.tlnt.ui.window.action.InvitationConfirmPopup confirmPopup;
   public InvitationAcceptClickListener(InvitationConfirmPopup popup) {
      super();
      this.confirmPopup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendEmote(this.confirmPopup.invitationType);
      this.confirmPopup.hide();
   }
}
