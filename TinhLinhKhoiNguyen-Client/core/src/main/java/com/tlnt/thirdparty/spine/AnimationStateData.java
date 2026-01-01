package com.tlnt.thirdparty.spine;

import com.badlogic.gdx.utils.ObjectFloatMap;

public final class AnimationStateData {
   public final SkeletonData skeletonData;
   public final ObjectFloatMap<AnimationStateData.Key> animationToMixTime = new ObjectFloatMap<>(51, 0.8F);
   public final AnimationStateData.Key tempKey = new AnimationStateData.Key();
   public float defaultMix;

   public AnimationStateData(SkeletonData skeletonData) {
      if (skeletonData == null) {
         throw new IllegalArgumentException("skeletonData cannot be null.");
      } else {
         this.skeletonData = skeletonData;
      }
   }

   public final void setDefaultMix(float defaultMix) {
      this.defaultMix = defaultMix;
   }

   public static class Key {
      public Animation fromAnimation;
      public Animation toAnimation;

      @Override
      public final int hashCode() {
         return 31 * (31 + this.fromAnimation.hashCode()) + this.toAnimation.hashCode();
      }

      @Override
      public final boolean equals(Object obj) {
         if (this == obj) {
            return true;
         } else if (obj == null) {
            return false;
         } else {
            AnimationStateData.Key other = (AnimationStateData.Key)obj;
            if (this.fromAnimation == null) {
               if (other.fromAnimation != null) {
                  return false;
               }
            } else if (!this.fromAnimation.equals(other.fromAnimation)) {
               return false;
            }

            if (this.toAnimation == null) {
               if (other.toAnimation != null) {
                  return false;
               }
            } else if (!this.toAnimation.equals(other.toAnimation)) {
               return false;
            }

            return true;
         }
      }

      @Override
      public final String toString() {
         return this.fromAnimation.name + "->" + this.toAnimation.name;
      }
   }
}
