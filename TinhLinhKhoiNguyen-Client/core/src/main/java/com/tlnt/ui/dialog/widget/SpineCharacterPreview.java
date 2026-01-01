package com.tlnt.ui.dialog.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.asset.PendingAnimationRequest;
import com.tlnt.core.asset.PendingSkeletonRequest;
import com.tlnt.core.asset.PendingSkinRequest;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.thirdparty.spine.Animation;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.Skin;
import com.tlnt.ui.widget.BaseActor;
import com.tlnt.util.GameUtils;

public final class SpineCharacterPreview extends BaseActor {
   public String currentSkeletonPath;
   public Animation currentAnimation;
   public NamedPair<AnimationState, Skeleton> skeletonPair;
   public Skeleton skeleton;
   public AnimationState animationState;
   public Color tintColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   public final PendingSkeletonRequest pendingSkeletonLoad = new PendingSkeletonRequest();
   public final PendingAnimationRequest pendingAnimationPlay = new PendingAnimationRequest();
   public final PendingSkinRequest pendingSkinApply = new PendingSkinRequest();
   public final Vector2 shadowOffset = new Vector2(-150.0F, 150.0F);

   public SpineCharacterPreview(RenderContext renderContext) {
      super(renderContext);
   }

   public final void loadSkeletonAndPlayIdle(String skeletonPath, String[] skinNames) {
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
      this.queueSkeletonChange(skeletonPath, 0.25F, mixConfig);
      this.applySkins(skinNames);
      this.playAnimation("idle", true, 1.0F);
   }

   public void queueSkeletonChange(String skeletonPath, float scale, SkeletonLoader.AnimationMixConfig animationMixConfig, String... skinNames) {
      if (!skeletonPath.equals(this.currentSkeletonPath)) {
         if (!this.pendingSkeletonLoad.pending) {
            this.pendingSkeletonLoad.skeletonName = skeletonPath;
            this.pendingSkeletonLoad.scale = 0.25F;
            this.pendingSkeletonLoad.mixConfig = animationMixConfig;
            this.pendingSkeletonLoad.skinNames = skinNames;
            this.pendingSkeletonLoad.pending = true;
         }
      }
   }

   public boolean playAnimation(String animationName, boolean loop, float timeScale) {
      if (!this.pendingSkeletonLoad.pending && this.skeleton != null) {
         Animation animation;
         if ((animation = this.skeleton.getData().findAnimation(animationName)) == null) {
            return false;
         } else {
            this.animationState.setAnimation(0, animationName, loop).setTimeScale(timeScale);
            this.currentAnimation = animation;
            return true;
         }
      } else {
         this.pendingAnimationPlay.animationName = animationName;
         this.pendingAnimationPlay.loop = loop;
         this.pendingAnimationPlay.timeScale = timeScale;
         this.pendingAnimationPlay.pending = true;
         return false;
      }
   }

   public boolean applySkins(String... skinNames) {
      if (!this.pendingSkeletonLoad.pending && this.animationState != null && this.skeleton != null) {
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
         this.pendingSkinApply.skinNames = skinNames;
         this.pendingSkinApply.pending = true;
         return false;
      }
   }

   @Override
   public final void setRotation(float degrees) {
      if (this.skeleton != null) {
         this.skeleton.getRootBone().setRotation(degrees);
      }
   }

   @Override
   public final void setColor(Color color) {
      this.tintColor.set(color);
   }

   @Override
   public final void setColor(float r, float g, float b, float a) {
      this.tintColor.set(r, g, b, a);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      parentAlpha = Gdx.graphics.getDeltaTime();
      if (this.pendingSkeletonLoad.pending) {
         if (GameUtils.hasElapsed(this.pendingSkeletonLoad.lastAttemptTime, 100)) {
            this.pendingSkeletonLoad.lastAttemptTime = System.currentTimeMillis();
            String[] skinNames = this.pendingSkeletonLoad.skinNames;
            SkeletonLoader.AnimationMixConfig mixConfig = this.pendingSkeletonLoad.mixConfig;
            float scale = this.pendingSkeletonLoad.scale;
            String skeletonPath = this.pendingSkeletonLoad.skeletonName;
            int loaded;
            if (skeletonPath.equals(this.currentSkeletonPath)) {
               loaded = 1;
            } else {
               if (this.skeletonPair != null) {
                  SkeletonLoader.releaseSkeleton(this.skeletonPair);
               }

               NamedPair pair;
               if ((pair = SkeletonLoader.obtainSkeleton(skeletonPath, scale, mixConfig)) == null) {
                  loaded = 0;
               } else {
                  if (pair != null) {
                     this.skeletonPair = pair;
                     this.animationState = this.skeletonPair.first;
                     this.skeleton = this.skeletonPair.second;
                  }

                  if (skinNames != null && skinNames.length != 0) {
                     this.applySkins(skinNames);
                  }

                  this.currentSkeletonPath = skeletonPath;
                  loaded = 1;
               }
            }

            if (loaded != 0) {
               this.pendingSkeletonLoad.clear();
            }
         }
      } else if (this.pendingSkinApply.pending) {
         if (this.applySkins(this.pendingSkinApply.skinNames)) {
            this.pendingSkinApply.clear();
         }
      } else if (this.pendingAnimationPlay.pending
         && this.playAnimation(this.pendingAnimationPlay.animationName, this.pendingAnimationPlay.loop, this.pendingAnimationPlay.timeScale)) {
         this.pendingAnimationPlay.clear();
      }

      if (this.animationState != null && this.skeleton != null) {
         this.animationState.update(parentAlpha);
         this.animationState.apply(this.skeleton);
         this.skeleton.update(parentAlpha);
         this.skeleton.setPosition(this.getX() + this.getWidth() / 2.0F, this.getY());
         this.skeleton.updateWorldTransform(Skeleton.Physics.NONE);
         this.skeleton.setScale(this.shadowOffset.x, this.shadowOffset.y);
      }

      if (this.skeleton != null) {
         this.skeleton.setColor(this.tintColor);
         super.renderContext.drawSkeleton(this.skeleton);
      }

      TextureAsset shadowTexture = TextureAsset.obtain("shadow");
      RenderContext ctx = super.renderContext;
      float shadowX = this.getX() + this.getWidth() / 2.0F;
      float shadowY = this.getY();
      int shadowWidth = shadowTexture.getWidth();
      ctx.drawTextureSized(shadowTexture, shadowX, shadowY, (float)(shadowWidth * 3), (float)shadowTexture.getHeight() * 2.5F, Anchor.CENTER);
   }

}
