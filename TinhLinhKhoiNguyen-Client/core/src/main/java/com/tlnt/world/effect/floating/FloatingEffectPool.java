package com.tlnt.world.effect.floating;

import com.badlogic.gdx.utils.Pool;

public final class FloatingEffectPool extends Pool<PooledFloatingEffect> {

   @Override
   public PooledFloatingEffect newObject() {
      return new com.tlnt.world.effect.floating.PooledFloatingEffect();
   }

   @Override
   public final PooledFloatingEffect obtain() {
      PooledFloatingEffect effect = super.obtain();
      effect.isAlive = true;
      return effect;
   }
}
