package com.tlnt.world.groundeffect;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameMain;

public final class TeleportOutEffectPool extends Pool<TeleportOutEffect> {

   @Override
   public TeleportOutEffect newObject() {
      return new com.tlnt.world.groundeffect.TeleportOutEffect(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final void free(TeleportOutEffect obj) {
      TeleportOutEffect effect = (TeleportOutEffect)obj;
      effect.setActive(false);
      super.free(effect);
   }

   @Override
   public final TeleportOutEffect obtain() {
      TeleportOutEffect effect;
      TeleportOutEffect obtained = effect = (TeleportOutEffect)super.obtain();
      obtained.setActive(true);
      return effect;
   }
}
