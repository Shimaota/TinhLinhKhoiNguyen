package com.tlnt.world.groundeffect;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameMain;

public final class SpearProjectileEffectPool extends Pool<SpearProjectileEffect> {

   @Override
   public SpearProjectileEffect newObject() {
      return new com.tlnt.world.groundeffect.SpearProjectileEffect(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final void free(SpearProjectileEffect obj) {
      SpearProjectileEffect effect = (SpearProjectileEffect)obj;
      effect.setActive(false);
      super.free(effect);
   }

   @Override
   public final SpearProjectileEffect obtain() {
      SpearProjectileEffect effect;
      SpearProjectileEffect obtained = effect = (SpearProjectileEffect)super.obtain();
      obtained.setActive(true);
      return effect;
   }
}
