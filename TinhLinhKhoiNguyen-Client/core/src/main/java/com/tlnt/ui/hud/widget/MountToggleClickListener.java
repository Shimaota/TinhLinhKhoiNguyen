package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.world.entity.player.PlayerEntity;

public final class MountToggleClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.MountButton mountButton;
   public MountToggleClickListener(MountButton mountButton) {
      super();
      this.mountButton = mountButton;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (PlayerEntity.getInstance() != null) {
         ServerPacketSender sender = ServerPacketSender.getInstance();
         int mounted;
         if (!PlayerEntity.getInstance().POOL2.isAlive) {
            mounted = 1;
         } else {
            mounted = 0;
         }

         sender.sendToggleMount((mounted != 0));
      }
   }
}
