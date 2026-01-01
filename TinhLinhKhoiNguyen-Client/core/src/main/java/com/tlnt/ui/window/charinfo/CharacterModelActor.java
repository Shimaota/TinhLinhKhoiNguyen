package com.tlnt.ui.window.charinfo;

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
import java.util.Arrays;
import java.util.Objects;

public final class CharacterModelActor extends BaseActor {
   public int skeletonHashCode;
   public String currentSkeletonName;
   public Animation currentAnimation;
   public NamedPair<AnimationState, Skeleton> skeletonPair;
   public Skeleton skeleton;
   public AnimationState animationState;
   public Color tintColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   public final PendingSkeletonRequest pendingLoadState = new PendingSkeletonRequest();
   public final PendingAnimationRequest pendingAnimState = new PendingAnimationRequest();
   public final PendingSkinRequest pendingSkinState = new PendingSkinRequest();
   public final Vector2 skeletonScale = new Vector2(150.0F, 150.0F);
   public long lastRenderedFrame;

   public CharacterModelActor(RenderContext renderContext) {
      super(renderContext);
   }

   public final void loadSkeleton(String skeletonName, String... skinNames) {
      Object[] hashInputs = new Object[2];
      hashInputs[0] = skeletonName;
      hashInputs[1] = Arrays.hashCode((Object[])skinNames);
      int hash;
      if ((hash = Objects.hash(hashInputs)) != this.skeletonHashCode) {
         this.skeletonHashCode = hash;
         SkeletonLoader.AnimationMixConfig newMixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
         String[] emptySkins = new String[0];
         SkeletonLoader.AnimationMixConfig mixConfig = newMixConfig;
         float scale = 0.25F;
         if (!this.pendingLoadState.pending) {
            this.pendingLoadState.skeletonName = skeletonName;
            this.pendingLoadState.scale = 0.25F;
            this.pendingLoadState.mixConfig = mixConfig;
            this.pendingLoadState.skinNames = emptySkins;
            this.pendingLoadState.pending = true;
         }

         this.applySkinMixins(skinNames);
         String idleAnim = "idle";
         this.playAnimation(idleAnim, true, 1.0F);
      }
   }

   public boolean playAnimation(String animationName, boolean loop, float timeScale) {
      if (!this.pendingLoadState.pending && this.skeleton != null) {
         Animation animation;
         if ((animation = this.skeleton.getData().findAnimation(animationName)) == null) {
            return false;
         } else {
            this.animationState.setAnimation(0, animationName, loop).setTimeScale(timeScale);
            this.currentAnimation = animation;
            return true;
         }
      } else {
         this.pendingAnimState.animationName = animationName;
         this.pendingAnimState.loop = loop;
         this.pendingAnimState.timeScale = timeScale;
         this.pendingAnimState.pending = true;
         return false;
      }
   }

   public boolean applySkinMixins(String... skinNames) {
      if (!this.pendingLoadState.pending && this.animationState != null && this.skeleton != null) {
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
         this.pendingSkinState.skinNames = skinNames;
         this.pendingSkinState.pending = true;
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
      if (this.pendingLoadState.pending) {
         if (GameUtils.hasElapsed(this.pendingLoadState.lastAttemptTime, 100)) {
            this.pendingLoadState.lastAttemptTime = System.currentTimeMillis();
            String[] pendingSkins = this.pendingLoadState.skinNames;
            SkeletonLoader.AnimationMixConfig pendingMix = this.pendingLoadState.mixConfig;
            float pendingScale = this.pendingLoadState.scale;
            String pendingName = this.pendingLoadState.skeletonName;
            int loaded;
            if (pendingName.equals(this.currentSkeletonName)) {
               loaded = 1;
            } else {
               if (this.skeletonPair != null) {
                  SkeletonLoader.releaseSkeleton(this.skeletonPair);
               }

               NamedPair pair;
               if ((pair = SkeletonLoader.obtainSkeleton(pendingName, pendingScale, pendingMix)) == null) {
                  loaded = 0;
               } else {
                  if (pair != null) {
                     this.skeletonPair = pair;
                     this.animationState = this.skeletonPair.first;
                     this.skeleton = this.skeletonPair.second;
                  }

                  if (pendingSkins != null && pendingSkins.length != 0) {
                     this.applySkinMixins(pendingSkins);
                  }

                  this.currentSkeletonName = pendingName;
                  loaded = 1;
               }
            }

            if (loaded != 0) {
               this.pendingLoadState.clear();
            }
         }
      } else if (this.pendingSkinState.pending) {
         if (this.applySkinMixins(this.pendingSkinState.skinNames)) {
            this.pendingSkinState.clear();
         }
      } else if (this.pendingAnimState.pending
         && this.playAnimation(this.pendingAnimState.animationName, this.pendingAnimState.loop, this.pendingAnimState.timeScale)) {
         this.pendingAnimState.clear();
      }

      if (this.lastRenderedFrame != Gdx.graphics.getFrameId()) {
         this.lastRenderedFrame = Gdx.graphics.getFrameId();
         if (this.animationState != null && this.skeleton != null) {
            this.animationState.update(parentAlpha);
            this.animationState.apply(this.skeleton);
            this.skeleton.update(parentAlpha);
            this.skeleton.setPosition(this.getX() + this.getWidth() / 2.0F, this.getY());
            this.skeleton.updateWorldTransform(Skeleton.Physics.NONE);
            this.skeleton.setScale(this.skeletonScale.x, this.skeletonScale.y);
         }
      }

      if (this.skeleton != null) {
         this.skeleton.setColor(this.tintColor);
         super.renderContext.drawSkeleton(this.skeleton);
      }

      TextureAsset shadowTexture = TextureAsset.obtain("shadow");
      super.renderContext
         .drawTextureSized(
            shadowTexture,
            this.getX() + this.getWidth() / 2.0F,
            this.getY(),
            (float)(shadowTexture.getWidth() * 3),
            (float)shadowTexture.getHeight() * 2.5F,
            Anchor.CENTER
         );
   }

}
