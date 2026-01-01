package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class AvatarButtonClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.AvatarButton avatarButton;
   public AvatarButtonClickListener(AvatarButton avatarButton) {
      super();
      this.avatarButton = avatarButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestHarvest();
   }
}
