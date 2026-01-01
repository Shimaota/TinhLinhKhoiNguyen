package com.tlnt.world.anim;

import java.util.Map;
import java.util.WeakHashMap;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.util.GameUtils;

public class VariantAnimationBehavior<T extends GameObject> extends AnimationBehavior<T> {
   /**
    * Per-character animation variants. WEAK keys: the original used an ObjectMap -> every character / mob that ever attacked was kept
    * forever in the singleton (bots / players constantly joining and leaving = RAM growing until the game closed).
    */
   public final Map<T, VariantAnimationBehavior.VariantState> INSTANCE = new WeakHashMap<>();

   public final VariantAnimationBehavior.VariantState getOrCycleVariantState(T entity, int variantCount, int cycleWindowMs, boolean cycle) {
      VariantAnimationBehavior.VariantState state;
      if ((state = this.INSTANCE.get(entity)) == null) {
         state = new VariantAnimationBehavior.VariantState();
         this.INSTANCE.put((T)entity, state);
      } else if (cycle) {
         if (!GameUtils.hasElapsed(state.lastCycleTime, 2000)) {
            state.variantIndex += 1;
            state.variantIndex %= 2;
         } else {
            state.variantIndex = 0;
         }

         state.lastCycleTime = System.currentTimeMillis();
      }

      return state;
   }

   public static class VariantState {
      public int variantIndex;
      public long lastCycleTime;
   }
}
