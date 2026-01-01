package com.tlnt.engine.camera;

import com.tlnt.engine.scene.GameObject;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.screen.BaseScreen;
import com.tlnt.util.GameUtils;

public final class PingSender extends GameObject {
   public long POOL2;

   public PingSender(BaseScreen screen) {
      super(screen);
   }

   @Override
   public final void update(float delta) {
      if (GameUtils.hasElapsed(this.POOL2, 1000)) {
         this.POOL2 = System.currentTimeMillis();
         ServerPacketSender.getInstance().sendPing();
      }
   }

   @Override
   public final void dispose() {
   }
}
