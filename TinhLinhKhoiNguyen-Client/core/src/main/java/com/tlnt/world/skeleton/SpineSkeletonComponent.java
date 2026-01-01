package com.tlnt.world.skeleton;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.asset.PendingAnimationRequest;
import com.tlnt.core.asset.PendingSkeletonRequest;
import com.tlnt.core.asset.PendingSkinRequest;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.Entity;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.thirdparty.spine.Animation;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.SkeletonBounds;
import com.tlnt.thirdparty.spine.Skin;
import com.tlnt.util.GameUtils;

public class SpineSkeletonComponent extends BaseComponent {
   public String projectile2;
   public Animation lastTemplate;
   public NamedPair<AnimationState, Skeleton> effectStartTime;
   public SkeletonBounds projectile;
   public Skeleton speed;
   public AnimationState active;
   public Color effectDuration = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   public Vector2 isArrowVisible = new Vector2();
   public PendingSkeletonRequest pendingLayerChange = new PendingSkeletonRequest();
   public PendingAnimationRequest pendingAnimation = new PendingAnimationRequest();
   public PendingSkinRequest pendingSkinChange = new PendingSkinRequest();
   public boolean flipEnabled = false;
   public Array<AnimationState.AnimationStateListener> animationListeners = new Array<>();
   public boolean autoFlipEnabled;
   public float scaleMultiplier;
   public float timeScale;

   public SpineSkeletonComponent(Entity entity) {
      super(entity);
      this.autoFlipEnabled = true;
      this.scaleMultiplier = 1.0F;
      this.timeScale = 1.0F;
   }

   public final void setTimeScale(float timeScale) {
      this.timeScale = timeScale;
   }

   public final void setScaleMultiplier(float scaleMultiplier) {
      this.scaleMultiplier = scaleMultiplier;
   }

   @Override
   public final void onAdded() {
      this.projectile = new SkeletonBounds();
   }

   public final boolean containsPoint(Vector2 point) {
      if (this.projectile != null && this.projectile.containsPoint(point.x, point.y) != null) {
         return true;
      } else {
         return false;
      }
   }

   public final void requestLayerChange(String skeletonName, float scale, SkeletonLoader.AnimationMixConfig mixConfig, String... skinNames) {
      if (!skeletonName.equals(this.projectile2)) {
         if (!this.pendingLayerChange.pending || !this.pendingLayerChange.skeletonName.equals(skeletonName)) {
            this.pendingLayerChange.skeletonName = skeletonName;
            this.pendingLayerChange.scale = scale;
            this.pendingLayerChange.mixConfig = mixConfig;
            this.pendingLayerChange.skinNames = skinNames;
            this.pendingLayerChange.pending = true;
         }
      }
   }

   @Override
   public void lateUpdate(float delta) {
      if (this.pendingLayerChange.pending) {
         if (GameUtils.hasElapsed(this.pendingLayerChange.lastAttemptTime, 1000)) {
            this.pendingLayerChange.lastAttemptTime = System.currentTimeMillis();
            String[] skinNames = this.pendingLayerChange.skinNames;
            SkeletonLoader.AnimationMixConfig mixConfig = this.pendingLayerChange.mixConfig;
            float scale = this.pendingLayerChange.scale;
            String skeletonName = this.pendingLayerChange.skeletonName;
            int doneFlag;
            if (skeletonName.equals(this.projectile2)) {
               doneFlag = 1;
            } else {
               if (this.effectStartTime != null) {
                  SkeletonLoader.releaseSkeleton(this.effectStartTime);
               }

               NamedPair skeletonPair;
               if ((skeletonPair = SkeletonLoader.obtainSkeleton(skeletonName, scale, mixConfig)) == null) {
                  doneFlag = 0;
               } else {
                  if (skeletonPair != null) {
                     this.effectStartTime = skeletonPair;
                     this.active = this.effectStartTime.first;
                     this.speed = this.effectStartTime.second;
                     this.active.addListener(new SkeletonEventForwarder(this));
                  }

                  if (skinNames != null && skinNames.length != 0) {
                     this.setSkins(skinNames);
                  }

                  this.projectile2 = skeletonName;
                  doneFlag = 1;
               }
            }

            if (doneFlag != 0) {
               this.pendingLayerChange.clear();
            }
         }
      } else if (this.pendingSkinChange.pending) {
         if (this.setSkins(this.pendingSkinChange.skinNames)) {
            this.pendingSkinChange.clear();
         }
      } else if (this.pendingAnimation.pending
         && this.playAnimation(this.pendingAnimation.animationName, this.pendingAnimation.loop, this.pendingAnimation.timeScale)) {
         this.pendingAnimation.clear();
      }

      this.tickSkeleton(delta * this.timeScale);
   }

   public final boolean playAnimation(String animName, boolean loop, float timeScale) {
      if (!this.pendingLayerChange.pending && this.speed != null) {
         Animation animation;
         if ((animation = this.speed.getData().findAnimation(animName)) == null) {
            return false;
         } else {
            this.active.setAnimation(0, animName, loop).setTimeScale(timeScale);
            this.lastTemplate = animation;
            return true;
         }
      } else {
         this.pendingAnimation.animationName = animName;
         this.pendingAnimation.loop = loop;
         this.pendingAnimation.timeScale = timeScale;
         this.pendingAnimation.pending = true;
         return false;
      }
   }

   public final boolean setSkins(String... skinNames) {
      if (!this.pendingLayerChange.pending && this.active != null && this.speed != null) {
         Skin combinedSkin = new Skin("");
         String[] names;
         int count = (names = skinNames).length;

         for (int i = 0; i < count; i++) {
            String skinName = names[i];
            Skin skin;
            if ((skin = this.speed.getData().findSkin(skinName)) != null) {
               combinedSkin.addSkin(skin);
            }
         }

         this.speed.setSkin(combinedSkin);
         this.speed.setSlotsToSetupPose();
         return true;
      } else {
         this.pendingSkinChange.skinNames = skinNames;
         this.pendingSkinChange.pending = true;
         return false;
      }
   }

   public void tickSkeleton(float delta) {
      if (this.active != null && this.speed != null) {
         this.active.update(delta);
         this.active.apply(this.speed);
         this.speed.update(delta);
         this.speed.setPosition(this.getEntity().getPosition().x + this.isArrowVisible.x, this.getEntity().getPosition().y + this.isArrowVisible.y);
         this.speed.updateWorldTransform(Skeleton.Physics.UPDATE);
         this.projectile.update(this.speed, true);
      }
   }

   public final void setMixDuration(float rotation) {
      if (this.speed != null) {
         this.speed.getRootBone().setRotation(rotation);
      }
   }

   public final void setTintColor(Color color) {
      this.effectDuration.set(color);
   }

   public final void setTintAlpha(float r, float g, float b, float a) {
      this.effectDuration.set(1.0F, 1.0F, 1.0F, a);
   }

   public final void setSkeletonOffset(float offsetX, float offsetY) {
      this.isArrowVisible.set(offsetX, offsetY);
   }

   /** Margin (world units) around the entity position when the skeleton has no bounding-box attachment — enough for the largest skeleton. */
   private static final float CULL_MARGIN = 8.0F;

   /**
    * Skeleton outside the world camera view -> skip drawing. The original drew (computed mesh vertices for) EVERY mob / player on the map every
    * frame, even off-screen — JFR 2026-10-19: SkeletonRenderer.draw ~30% of the main thread, worse on maps with many mobs.
    * Animations still run in lateUpdate (the complete -> IDLE event is not missed), only drawing is skipped.
    */
   private boolean outsideView(RenderContext ctx) {
      if (ctx.hudCameraActive || ctx.currentCamera != ctx.worldCamera) {
         return false; // UI / other camera: no culling
      }
      com.badlogic.gdx.math.Rectangle view = ctx.viewBounds;
      Vector2 pos = this.getEntity().getPosition();
      float x = pos.x + this.isArrowVisible.x;
      float y = pos.y + this.isArrowVisible.y;
      float m = CULL_MARGIN * Math.max(1.0F, Math.abs(this.scaleMultiplier));
      float minX = x - m;
      float maxX = x + m;
      float minY = y - m;
      float maxY = y + m;
      SkeletonBounds b = this.projectile;
      if (b != null && b.maxX >= b.minX && b.maxY >= b.minY) { // has a bounding-box attachment: merge it in (very large skeletons)
         minX = Math.min(minX, b.minX - 1.0F);
         maxX = Math.max(maxX, b.maxX + 1.0F);
         minY = Math.min(minY, b.minY - 1.0F);
         maxY = Math.max(maxY, b.maxY + 1.0F);
      }
      return maxX < view.x || minX > view.x + view.width || maxY < view.y || minY > view.y + view.height;
   }

   @Override
   public void render(RenderContext ctx) {
      if (this.speed != null && !this.pendingLayerChange.pending && !this.outsideView(ctx)) {
         this.speed.setColor(this.effectDuration);
         this.speed
            .setScale(
               this.autoFlipEnabled ? -this.getEntity().getDirection().vector.x * this.scaleMultiplier : 1.0F * this.scaleMultiplier,
               1.0F * this.scaleMultiplier
            );
         ctx.drawSkeleton(this.speed);
      }
   }

   public final float getSkeletonWidth() {
      return Math.max(this.projectile != null ? this.projectile.getWidth() : 0.0F, 0.0F);
   }

   public final float getSkeletonHeight() {
      return Math.max(this.projectile != null ? this.projectile.getHeight() : 0.0F, 0.0F);
   }

   public final String getCurrentAnimationName() {
      return this.lastTemplate != null ? this.lastTemplate.getName() : "";
   }

   public final float getAnimationProgress(int trackIndex) {
      return this.active != null && this.lastTemplate != null && this.active.getCurrent(0) != null
         ? this.active.getCurrent(0).getAnimationTime() / this.lastTemplate.getDuration()
         : 0.0F;
   }

   public final void addAnimationListener(AnimationState.AnimationStateAdapter listener) {
      this.animationListeners.add(listener);
   }

   public final void clearPendingRequests() {
      this.pendingLayerChange.clear();
      this.pendingAnimation.clear();
      this.pendingSkinChange.clear();
   }

   @Override
   public void onDestroy() {
      SkeletonLoader.releaseSkeleton(this.effectStartTime);
      this.effectStartTime = null;
      this.speed = null;
      this.active = null;
      this.projectile = null;
   }
}
