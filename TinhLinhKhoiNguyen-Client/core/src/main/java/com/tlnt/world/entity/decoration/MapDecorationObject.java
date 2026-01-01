package com.tlnt.world.entity.decoration;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.asset.PendingAnimationRequest;
import com.tlnt.core.asset.PendingSkeletonRequest;
import com.tlnt.core.asset.PendingSkinRequest;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.particle.ParticleEffect;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.model.friend.MapDecorationAnimation;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.Skin;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.tlnt.core.Settings;
import com.tlnt.engine.particle.Particle;
import com.tlnt.engine.particle.ParticleEmitter;
import com.tlnt.engine.render.Anchor;
import com.tlnt.thirdparty.spine.Animation;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.decoration.MapDecorationObjectPool;

public final class MapDecorationObject extends GameObject implements Pool.Poolable {
   public static final Pool<MapDecorationObject> POOL2 = new MapDecorationObjectPool();
   public int skeleton;
   public TextureAsset POOL;
   public MapDecorationAnimation owner;
   public byte target;
   public float movementComponent;
   public float target2;
   public float posY;
   public float width;
   public float height;
   public float anchorX;
   public float anchorY;
   public boolean flipX;
   public boolean flipY;
   public boolean tiledX;
   public boolean tiledY;
   public float[] colorTint;
   public ParticleEffect[] particleGroups;
   public NamedPair<AnimationState, Skeleton> skeletonPair;
   public Skeleton skeleton2;
   public AnimationState animationState;
   public final PendingSkeletonRequest pendingSpineLoad;
   public final PendingAnimationRequest pendingAnimChange;
   public final PendingSkinRequest pendingMixChange;
   public String currentSkeletonName;
   public float skeletonX;
   public float skeletonY;

   public MapDecorationObject(LoginScreen screen) {
      super(screen, EntityType.BACKGROUND);
      float[] tint = new float[4];
      tint[0] = 1.0F;
      tint[1] = 1.0F;
      tint[2] = 1.0F;
      tint[3] = 1.0F;
      this.colorTint = tint;
      this.pendingSpineLoad = new PendingSkeletonRequest();
      this.pendingAnimChange = new PendingAnimationRequest();
      this.pendingMixChange = new PendingSkinRequest();
   }

   public final MapDecorationObject setupParticle(ParticleEffect[] effects, float renderOrder) {
      this.skeleton = MapDecorationObject.DecorationType.PARTICLE;
      this.particleGroups = effects;
      this.setRenderOrder(renderOrder);
      return this;
   }

   public final MapDecorationObject setupTexture(
      String texturePath,
      float x,
      float y,
      float width,
      float height,
      float anchorX,
      float anchorY,
      boolean flipX,
      boolean flipY,
      boolean tiledX,
      boolean tiledY,
      float r,
      float g,
      float b,
      float a,
      float renderOrder
   ) {
      this.skeleton = MapDecorationObject.DecorationType.TEXTURE;
      this.POOL = TextureAsset.obtain(texturePath);
      this.target2 = x;
      this.posY = y;
      this.width = width;
      this.height = height;
      this.anchorX = anchorX;
      this.anchorY = anchorY;
      this.flipX = flipX;
      this.flipY = flipY;
      this.tiledX = tiledX;
      this.tiledY = tiledY;
      this.colorTint[0] = r;
      this.colorTint[1] = g;
      this.colorTint[2] = b;
      this.colorTint[3] = a;
      this.setRenderOrder(renderOrder);
      return this;
   }

   public final MapDecorationObject setupAnimatedTile(
      int animationId,
      float x,
      float y,
      float width,
      float height,
      float anchorX,
      float anchorY,
      boolean flipX,
      boolean flipY,
      boolean tiledX,
      boolean tiledY,
      float r,
      float g,
      float b,
      float a,
      float renderOrder
   ) {
      this.skeleton = MapDecorationObject.DecorationType.ANIMATED_TILE;
      this.owner = MapDecorationAnimation.getById(animationId);
      this.target2 = x;
      this.posY = y;
      this.width = width;
      this.height = height;
      this.anchorX = anchorX;
      this.anchorY = anchorY;
      this.flipX = flipX;
      this.flipY = flipY;
      this.tiledX = tiledX;
      this.tiledY = tiledY;
      this.colorTint[0] = r;
      this.colorTint[1] = g;
      this.colorTint[2] = b;
      this.colorTint[3] = a;
      this.setRenderOrder(renderOrder);
      return this;
   }

   public final MapDecorationObject setupSkeleton(
      String skeletonName,
      float scale,
      String animName,
      float scaleX,
      float scaleY,
      boolean loop,
      float timeScale,
      String[] skinNames,
      float x,
      float y,
      float anchorX,
      float anchorY,
      float renderOrder
   ) {
      this.skeleton = MapDecorationObject.DecorationType.SPINE;
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig();
      if (!skeletonName.equals(this.currentSkeletonName) && (!this.pendingSpineLoad.pending || !this.pendingSpineLoad.skeletonName.equals(skeletonName))) {
         this.pendingSpineLoad.skeletonName = skeletonName;
         this.pendingSpineLoad.scale = scale;
         this.pendingSpineLoad.mixConfig = mixConfig;
         this.pendingSpineLoad.skinNames = skinNames;
         this.pendingSpineLoad.pending = true;
      }

      this.setSkins(skinNames);
      this.setAnimation(animName, loop, timeScale);
      this.skeletonX = scaleX;
      this.skeletonY = scaleY;
      this.target2 = x;
      this.posY = y;
      this.anchorX = anchorX;
      this.anchorY = anchorY;
      this.setRenderOrder(renderOrder);
      return this;
   }

   @Override
   public final void update(float delta) {
       switch (this.skeleton) {
           case DecorationType.TEXTURE: {
               return;
           }
           case DecorationType.ANIMATED_TILE: {
               if (this.owner == null) break;
               this.movementComponent += delta;
               if (!(this.movementComponent >= this.owner.frameDuration)) break;
               this.movementComponent -= this.owner.frameDuration;
               this.target = (byte)(this.target + 1);
               int wrapMarker = 22300;
               if (this.target <= this.owner.frameNames.length - 1) break;
               wrapMarker = 10702;
               this.target = (byte)0;
               return;
           }
           case DecorationType.PARTICLE: {
               if (Settings.hideEffects) break;
               OrthographicCamera orthographicCamera = this.getScreen().getWorldCamera();
               float viewW = orthographicCamera.viewportWidth * orthographicCamera.zoom;
               float viewH = orthographicCamera.viewportHeight * orthographicCamera.zoom;
               ParticleEffect[] groups = this.particleGroups;
               int groupCount = this.particleGroups.length;
               for (int i = 0; i < groupCount; ++i) {
                   ParticleEffect group = groups[i];
                   if (group == null) continue;
                   if (group.isFlipX()) {
                       float px = orthographicCamera.position.x + viewW * group.getParallax().x - viewW / 2.0f;
                       float py = orthographicCamera.position.y + viewH * group.getParallax().y - viewH / 2.0f;
                       group.setPosition(px, py);
                   }
                   group.update(delta);
               }
               return;
           }
           case DecorationType.SPINE: {
               Object objRef;
               MapDecorationObject self = this;
               if (self.pendingSpineLoad.pending) {
                   if (GameUtils.hasElapsed(self.pendingSpineLoad.lastAttemptTime, 1000)) {
                       int loadDone;
                       self.pendingSpineLoad.lastAttemptTime = System.currentTimeMillis();
                       MapDecorationObject self2 = self;
                       String[] pendingSkins = self.pendingSpineLoad.skinNames;
                       Object mixConfigObj = self.pendingSpineLoad.mixConfig;
                       float pendingScale = self.pendingSpineLoad.scale;
                       objRef = self2.pendingSpineLoad.skeletonName;
                       MapDecorationObject self3 = self2;
                       if (((String)objRef).equals(self3.currentSkeletonName)) {
                           loadDone = 1;
                       } else {
                           NamedPair<AnimationState, Skeleton> pair;
                           if (self3.skeletonPair != null) {
                               SkeletonLoader.releaseSkeleton(self3.skeletonPair);
                           }
                           if ((pair = SkeletonLoader.obtainSkeleton((String)objRef, pendingScale, (SkeletonLoader.AnimationMixConfig)mixConfigObj)) == null) {
                               loadDone = 0;
                           } else {
                               mixConfigObj = self3;
                               if (pair != null) {
                                   ((MapDecorationObject)mixConfigObj).skeletonPair = pair;
                                   ((MapDecorationObject)mixConfigObj).animationState = (AnimationState)((MapDecorationObject)mixConfigObj).skeletonPair.first;
                                   ((MapDecorationObject)mixConfigObj).skeleton2 = (Skeleton)((MapDecorationObject)mixConfigObj).skeletonPair.second;
                               }
                               if (pendingSkins != null && pendingSkins.length != 0) {
                                   self3.setSkins(pendingSkins);
                               }
                               self3.currentSkeletonName = (String)objRef;
                               loadDone = 1;
                           }
                       }
                       if (loadDone != 0) {
                           self.pendingSpineLoad.clear();
                       }
                   }
               } else if (self.pendingMixChange.pending) {
                   MapDecorationObject self4 = self;
                   if (self4.setSkins(self4.pendingMixChange.skinNames)) {
                       self.pendingMixChange.clear();
                   }
               } else if (self.pendingAnimChange.pending) {
                   MapDecorationObject self5 = self;
                   if (self5.setAnimation(self5.pendingAnimChange.animationName, self.pendingAnimChange.loop, self.pendingAnimChange.timeScale)) {
                       self.pendingAnimChange.clear();
                   }
               }
               float dt = delta;
               self = this;
               if (self.animationState == null || self.skeleton2 == null) break;
               self.animationState.update(dt);
               self.animationState.apply(self.skeleton2);
               self.skeleton2.update(dt);
               objRef = self.getScreen().getWorldCamera();
               Viewport viewport = self.getScreen().getWorldViewport();
               float parallaxX = (((Camera)objRef).position.x - viewport.getWorldWidth() * ((OrthographicCamera)objRef).zoom / 2.0f) * (1.0f - self.anchorX);
               float parallaxY = (((Camera)objRef).position.y - viewport.getWorldHeight() * ((OrthographicCamera)objRef).zoom / 2.0f) * (1.0f - self.anchorY);
               float worldX = self.target2 + parallaxX;
               parallaxX = self.posY + parallaxY;
               self.skeleton2.setPosition(worldX, parallaxX);
               self.skeleton2.updateWorldTransform(Skeleton.Physics.NONE);
           }
       }
   }

   public boolean setAnimation(String animName, boolean loop, float timeScale) {
      if (!this.pendingSpineLoad.pending && this.skeleton2 != null) {
         if (this.skeleton2.getData().findAnimation(animName) == null) {
            return false;
         } else {
            this.animationState.setAnimation(0, animName, loop).setTimeScale(timeScale);
            return true;
         }
      } else {
         this.pendingAnimChange.animationName = animName;
         this.pendingAnimChange.loop = loop;
         this.pendingAnimChange.timeScale = timeScale;
         this.pendingAnimChange.pending = true;
         return false;
      }
   }

   public boolean setSkins(String... skinNames) {
      if (!this.pendingSpineLoad.pending && this.animationState != null && this.skeleton2 != null) {
         Skin combinedSkin = new Skin("");
         String[] names;
         int count = (names = skinNames).length;

         for (int i = 0; i < count; i++) {
            String skinName = names[i];
            Skin skin;
            if ((skin = this.skeleton2.getData().findSkin(skinName)) != null) {
               combinedSkin.addSkin(skin);
            }
         }

         this.skeleton2.setSkin(combinedSkin);
         this.skeleton2.setSlotsToSetupPose();
         return true;
      } else {
         this.pendingMixChange.skinNames = skinNames;
         this.pendingMixChange.pending = true;
         return false;
      }
   }

   @Override
   public final void render(RenderContext ctx) {
       switch (this.skeleton) {
           case DecorationType.TEXTURE: {
               if (this.POOL == null) {
                   return;
               }
               OrthographicCamera orthographicCamera = ctx.getWorldCamera();
               Viewport viewport = ctx.getWorldViewport();
               float parallaxX = (orthographicCamera.position.x - viewport.getWorldWidth() * orthographicCamera.zoom / 2.0f) * (1.0f - this.anchorX);
               float parallaxY = (orthographicCamera.position.y - viewport.getWorldHeight() * orthographicCamera.zoom / 2.0f) * (1.0f - this.anchorY);
               float baseX = this.target2 + parallaxX;
               float baseY = this.posY + parallaxY;
               float stepX = this.width / 100.0f;
               float stepY = this.height / 100.0f;
               float viewLeft = orthographicCamera.position.x - viewport.getWorldWidth() * orthographicCamera.zoom / 2.0f;
               float viewBottom = orthographicCamera.position.y - viewport.getWorldHeight() * orthographicCamera.zoom / 2.0f;
               float viewW = viewport.getWorldWidth() * orthographicCamera.zoom;
               float viewH = viewport.getWorldHeight() * orthographicCamera.zoom;
               float viewRight = viewLeft + viewW;
               float viewTop = viewBottom + viewH;
               float tintR = this.colorTint[0];
               ctx.setBatchColorRgba(tintR, this.colorTint[1], this.colorTint[2], this.colorTint[3]);
               int colStart = 0;
               int colEnd = 0;
               int rowStart = 0;
               int rowEnd = 0;
               if (this.tiledX && stepX > 0.0f) {
                   colStart = (int)Math.floor((viewLeft - baseX) / stepX) - 1;
                   colEnd = (int)Math.ceil((viewRight - baseX) / stepX) + 1;
               }
               if (this.tiledY && stepY > 0.0f) {
                   rowStart = (int)Math.floor((viewBottom - baseY) / stepY) - 1;
                   rowEnd = (int)Math.ceil((viewTop - baseY) / stepY) + 1;
               }
               for (int row = rowStart; row <= rowEnd; ++row) {
                   parallaxY = baseY + (float)row * stepY;
                   for (int col = colStart; col <= colEnd; ++col) {
                       viewTop = baseX + (float)col * stepX;
                       ctx.drawTextureFlipped(this.POOL, viewTop, parallaxY, this.width, this.height, this.flipX, this.flipY, Anchor.BOTTOM_LEFT);
                   }
               }
               ctx.resetBatchColor();
               return;
           }
           case DecorationType.ANIMATED_TILE: {
               int unused;
               if (this.owner == null) {
                   return;
               }
               int frameCount = this.owner.frameNames.length;
               int frameIndex = this.target % frameCount;
               TextureAsset frame = TextureAsset.obtain(this.owner.frameNames[frameIndex]);
               OrthographicCamera orthographicCamera = ctx.getWorldCamera();
               Viewport viewport = ctx.getWorldViewport();
               float parallaxX = (orthographicCamera.position.x - viewport.getWorldWidth() * orthographicCamera.zoom / 2.0f) * (1.0f - this.anchorX);
               float parallaxY = (orthographicCamera.position.y - viewport.getWorldHeight() * orthographicCamera.zoom / 2.0f) * (1.0f - this.anchorY);
               float baseX = this.target2 + parallaxX;
               float baseY = this.posY + parallaxY;
               float fitScale = Math.min(this.width / (float)frame.getWidth(), this.height / (float)frame.getHeight());
               float drawW = (float)frame.getWidth() * fitScale;
               float drawH = (float)frame.getHeight() * fitScale;
               float anchorOffX = 0.0f;
               float anchorOffY = 0.0f;
               switch (this.owner.anchor) {
                   case TOP_LEFT: {
                       anchorOffY = drawH / 100.0f;
                       break;
                   }
                   case TOP_CENTER: {
                       anchorOffX = drawW / 2.0f / 100.0f;
                       anchorOffY = drawH / 100.0f;
                       break;
                   }
                   case TOP_RIGHT: {
                       anchorOffX = drawW / 100.0f;
                       anchorOffY = drawH / 100.0f;
                       break;
                   }
                   case MIDDLE_LEFT: {
                       anchorOffY = drawH / 2.0f / 100.0f;
                       break;
                   }
                   case CENTER: {
                       anchorOffX = drawW / 2.0f / 100.0f;
                       anchorOffY = drawH / 2.0f / 100.0f;
                       break;
                   }
                   case MIDDLE_RIGHT: {
                       anchorOffX = drawW / 100.0f;
                       anchorOffY = drawH / 2.0f / 100.0f;
                       break;
                   }
                   case BOTTOM_CENTER: {
                       anchorOffX = drawW / 2.0f / 100.0f;
                       break;
                   }
                   case BOTTOM_RIGHT: {
                       anchorOffX = drawW / 100.0f;
                   }
               }
               baseX += anchorOffX;
               baseY += anchorOffY;
               float stepX = drawW / 100.0f;
               float stepY = drawH / 100.0f;
               float viewLeft = orthographicCamera.position.x - viewport.getWorldWidth() * orthographicCamera.zoom / 2.0f;
               float viewBottom = orthographicCamera.position.y - viewport.getWorldHeight() * orthographicCamera.zoom / 2.0f;
               anchorOffX = viewport.getWorldWidth() * orthographicCamera.zoom;
               float viewH = viewport.getWorldHeight() * orthographicCamera.zoom;
               fitScale = viewLeft + anchorOffX;
               anchorOffY = viewBottom + viewH;
               float tintR = this.colorTint[0];
               float tintB = this.colorTint[2];
               ctx.setBatchColorRgba(tintR, this.colorTint[1], tintB, this.colorTint[3]);
               int colStart = 0;
               int colEnd = 0;
               int row = 0;
               int rowEnd = 0;
               if (this.tiledX && stepX > 0.0f) {
                   colStart = (int)Math.floor((viewLeft - baseX) / stepX) - 1;
                   colEnd = (int)Math.ceil((fitScale - baseX) / stepX) + 1;
               }
               if (this.tiledY && stepY > 0.0f) {
                   row = (int)Math.floor((viewBottom - baseY) / stepY) - 1;
                   rowEnd = (int)Math.ceil((anchorOffY - baseY) / stepY) + 1;
               }
               while (row <= rowEnd) {
                   parallaxY = baseY + (float)row * stepY;
                   for (int col = colStart; col <= colEnd; ++col) {
                       anchorOffY = baseX + (float)col * stepX;
                       ctx.drawTextureFlipped(frame, anchorOffY, parallaxY, drawW, drawH, this.flipX, this.flipY, this.owner.anchor);
                   }
                   ++row;
               }
               ctx.resetBatchColor();
               return;
           }
           case DecorationType.PARTICLE: {
               if (this.particleGroups == null || Settings.hideEffects) {
                   return;
               }
               ParticleEffect[] groups = this.particleGroups;
               int groupCount = this.particleGroups.length;
               for (int i = 0; i < groupCount; ++i) {
                   ParticleEffect group = groups[i];
                   if (group == null) continue;
                   float pixelsPerUnit = 100.0f;
                   RenderContext ctx2 = ctx;
                   for (ParticleEmitter particleEmitter : group.emitters) {
                       float ppu = 100.0f;
                       RenderContext ctx3 = ctx2;
                       ParticleEmitter emitter = particleEmitter;
                       Batch batch = ctx3.getBatch();
                       int srcFunc = batch.getBlendSrcFunc();
                       int dstFunc = batch.getBlendDstFunc();
                       if (emitter.premultipliedAlpha) {
                           batch.setBlendFunction(1, 771);
                       } else if (emitter.additive) {
                           batch.setBlendFunction(770, 1);
                       } else {
                           int srcAlpha = 770;
                           batch.setBlendFunction(srcAlpha, 771);
                       }
                       for (Particle particle : emitter.particles) {
                           particle.draw(ctx3, ppu);
                       }
                       batch.setBlendFunction(srcFunc, dstFunc);
                   }
               }
               return;
           }
           case DecorationType.SPINE: {
               if (this.skeleton2 == null || this.pendingSpineLoad.pending) break;
               this.skeleton2.setScale(this.skeletonX, this.skeletonY);
               ctx.drawSkeleton(this.skeleton2);
           }
       }
   }

   @Override
   public final void dispose() {
   }

   @Override
   public final void reset() {
      this.setActive(false);
   }

   public static final class DecorationType {
      public static final int TEXTURE = 1;
      public static final int ANIMATED_TILE = 2;
      public static final int PARTICLE = 3;
      public static final int SPINE = 4;
      private static final int[] TYPE_VALUES;

      public static int[] values() {
         return (int[])TYPE_VALUES.clone();
      }

      static {
         int[] values = new int[4];
         values[0] = TEXTURE;
         values[1] = ANIMATED_TILE;
         values[2] = PARTICLE;
         values[3] = SPINE;
         TYPE_VALUES = values;
      }
   }
}
