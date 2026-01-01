package com.tlnt.world.effect.floating;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.asset.PendingAnimationRequest;
import com.tlnt.core.asset.PendingSkeletonRequest;
import com.tlnt.core.asset.PendingSkinRequest;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.thirdparty.spine.Animation;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.Skin;
import com.tlnt.util.GameUtils;

public final class PooledFloatingEffect {
   public static final Pool<PooledFloatingEffect> POOL = new FloatingEffectPool();
   public boolean isAlive;
   public String currentSkeletonName;
   public Animation currentAnimation;
   public NamedPair<AnimationState, Skeleton> skeletonPair;
   public Skeleton skeleton;
   public AnimationState animationState;
   public Color tintColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   public final Vector2 positionOffset = new Vector2();
   public final PendingSkeletonRequest pendingSkeleton = new PendingSkeletonRequest();
   public final PendingAnimationRequest pendingAnimation = new PendingAnimationRequest();
   public final PendingSkinRequest pendingSkin = new PendingSkinRequest();
   public Array<AnimationState.AnimationStateListener> animationListeners = new Array<>();
   public Direction direction = Direction.LEFT;
   public float scale = 1.0F;
   public float timeScale = 1.0F;

   public PooledFloatingEffect() {
      this.requestSkeleton("skeletons/vfxs/hit_effect", 0.25F, new SkeletonLoader.AnimationMixConfig(2.0F));
      AnimationCompletionHandler completionHandler = new AnimationCompletionHandler(this);
      this.animationListeners.add(completionHandler);
   }

   public final void requestSkeleton(String skeletonName, float skeletonScale, SkeletonLoader.AnimationMixConfig mixConfig, String... skinNames) {
      if (!skeletonName.equals(this.currentSkeletonName)) {
         if (!this.pendingSkeleton.pending || !this.pendingSkeleton.skeletonName.equals(skeletonName)) {
            this.pendingSkeleton.skeletonName = skeletonName;
            this.pendingSkeleton.scale = 0.25F;
            this.pendingSkeleton.mixConfig = mixConfig;
            this.pendingSkeleton.skinNames = skinNames;
            this.pendingSkeleton.pending = true;
         }
      }
   }

   public final void update(float delta) {
      if (this.pendingSkeleton.pending) {
         if (GameUtils.hasElapsed(this.pendingSkeleton.lastAttemptTime, 1000)) {
            this.pendingSkeleton.lastAttemptTime = System.currentTimeMillis();
            String[] skinNames = this.pendingSkeleton.skinNames;
            SkeletonLoader.AnimationMixConfig mixConfig = this.pendingSkeleton.mixConfig;
            float skeletonScale = this.pendingSkeleton.scale;
            String skeletonName = this.pendingSkeleton.skeletonName;
            int completed;
            if (skeletonName.equals(this.currentSkeletonName)) {
               completed = 1;
            } else {
               if (this.skeletonPair != null) {
                  SkeletonLoader.releaseSkeleton(this.skeletonPair);
               }

               NamedPair pair;
               if ((pair = SkeletonLoader.obtainSkeleton(skeletonName, skeletonScale, mixConfig)) == null) {
                  completed = 0;
               } else {
                  if (pair != null) {
                     this.skeletonPair = pair;
                     this.animationState = this.skeletonPair.first;
                     this.skeleton = this.skeletonPair.second;
                     this.animationState.addListener(new AnimationListenerForwarder(this));
                  }

                  if (skinNames != null && skinNames.length != 0) {
                     this.tryApplySkins(skinNames);
                  }

                  this.currentSkeletonName = skeletonName;
                  completed = 1;
               }
            }

            if (completed != 0) {
               this.pendingSkeleton.clear();
            }
         }
      } else if (this.pendingSkin.pending) {
         if (this.tryApplySkins(this.pendingSkin.skinNames)) {
            this.pendingSkin.clear();
         }
      } else if (this.pendingAnimation.pending
         && this.tryPlayAnimation(this.pendingAnimation.animationName, this.pendingAnimation.loop, this.pendingAnimation.timeScale)) {
         this.pendingAnimation.clear();
      }

      float scaledDelta = delta * this.timeScale;
      if (this.animationState != null && this.skeleton != null) {
         this.animationState.update(scaledDelta);
         this.animationState.apply(this.skeleton);
         this.skeleton.update(scaledDelta);
         this.skeleton.updateWorldTransform(Skeleton.Physics.UPDATE);
      }
   }

   public final boolean tryPlayAnimation(String animName, boolean loop, float animTimeScale) {
      if (!this.pendingSkeleton.pending && this.skeleton != null) {
         Animation animation;
         if ((animation = this.skeleton.getData().findAnimation(animName)) == null) {
            return false;
         } else {
            this.animationState.setAnimation(0, animName, loop).setTimeScale(animTimeScale);
            this.currentAnimation = animation;
            return true;
         }
      } else {
         this.pendingAnimation.animationName = animName;
         this.pendingAnimation.loop = loop;
         this.pendingAnimation.timeScale = animTimeScale;
         this.pendingAnimation.pending = true;
         return false;
      }
   }

   public final boolean tryApplySkins(String... skinNames) {
      if (!this.pendingSkeleton.pending && this.animationState != null && this.skeleton != null) {
         Skin combinedSkin = new Skin("");
         String[] names;
         int count = (names = skinNames).length;

         for (int i = 0; i < count; i++) {
            String skinName = names[i];
            Skin skin;
            if ((skin = this.skeleton.getData().findSkin(skinName)) != null) {
               combinedSkin.addSkin(skin);
            }
         }

         this.skeleton.setSkin(combinedSkin);
         this.skeleton.setSlotsToSetupPose();
         return true;
      } else {
         this.pendingSkin.skinNames = skinNames;
         this.pendingSkin.pending = true;
         return false;
      }
   }

   public final PooledFloatingEffect withDirection(Direction direction) {
      this.direction = direction == Direction.LEFT ? Direction.RIGHT : Direction.LEFT;
      return this;
   }

   public final void setPositionOffset(float offsetX, float offsetY) {
      this.positionOffset.set(0.0F, offsetY);
   }

   public final void render(RenderContext ctx, float x, float y) {
      if (this.skeleton != null && !this.pendingSkeleton.pending) {
         this.skeleton.setColor(this.tintColor);
         this.skeleton.setPosition(x + this.positionOffset.x, y + this.positionOffset.y);
         this.skeleton.setScale(-this.direction.vector.x * this.scale, 1.0F * this.scale);
         ctx.drawSkeleton(this.skeleton);
      }
   }

}
