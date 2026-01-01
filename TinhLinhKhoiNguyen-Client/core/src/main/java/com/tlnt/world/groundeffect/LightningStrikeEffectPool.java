package com.tlnt.world.groundeffect;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameMain;

public final class LightningStrikeEffectPool extends Pool<LightningStrikeEffect> {

   @Override
   public LightningStrikeEffect newObject() {
      return new com.tlnt.world.groundeffect.LightningStrikeEffect(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final void free(LightningStrikeEffect obj) {
      LightningStrikeEffect effect = (LightningStrikeEffect)obj;
      effect.setActive(false);
      super.free(effect);
   }

   @Override
   public final LightningStrikeEffect obtain() {
      LightningStrikeEffect effect;
      (effect = (LightningStrikeEffect)super.obtain()).setActive(true);
      return effect;
   }
}
