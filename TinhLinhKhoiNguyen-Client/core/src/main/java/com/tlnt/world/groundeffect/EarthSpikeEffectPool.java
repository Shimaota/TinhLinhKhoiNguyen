package com.tlnt.world.groundeffect;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameMain;

public final class EarthSpikeEffectPool extends Pool<EarthSpikeEffect> {

   @Override
   public EarthSpikeEffect newObject() {
      return new com.tlnt.world.groundeffect.EarthSpikeEffect(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final void free(EarthSpikeEffect obj) {
      EarthSpikeEffect effect = (EarthSpikeEffect)obj;
      effect.setActive(false);
      super.free(effect);
   }

   @Override
   public final EarthSpikeEffect obtain() {
      EarthSpikeEffect effect;
      (effect = (EarthSpikeEffect)super.obtain()).setActive(true);
      return effect;
   }
}
