package com.tlnt.world.groundeffect;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameMain;

public final class ClickEffectPool extends Pool<ClickEffect> {

   @Override
   public ClickEffect newObject() {
      return new com.tlnt.world.groundeffect.ClickEffect(com.tlnt.core.GameMain.instance.getLoginScreen());
   }

   @Override
   public final void free(ClickEffect obj) {
      ClickEffect effect = (ClickEffect)obj;
      effect.setActive(false);
      super.free(effect);
   }

   @Override
   public final ClickEffect obtain() {
      ClickEffect effect;
      ClickEffect obtained = effect = (ClickEffect)super.obtain();
      obtained.setActive(true);
      return effect;
   }
}
