package com.tlnt.world.entity.animal.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.model.farm.AnimalType;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.world.skeleton.AnimationStateDispatcher;
import com.tlnt.world.skeleton.SimpleSpineSkeletonComponent;

public final class AnimalSprite extends SimpleSpineSkeletonComponent {
   public AnimalType projectile2;
   public AnimalEntity lastTemplate;
   public AnimationStateDispatcher effectStartTime;

   public AnimalSprite(AnimalEntity animal) {
      super(animal.getEntity());
      this.lastTemplate = animal;
      this.setRenderOrder(5000000.0F);
      this.addAnimationListener(new AnimalSpineAnimListener(this));
      this.setScaleMultiplier(0.0F);
   }

   @Override
   public final void onStart() {
      this.effectStartTime = this.getEntity().findComponent(AnimationStateDispatcher.class, true);
   }

   @Override
   public final void update(float delta) {
      AnimalType animalType = this.lastTemplate.POOL2.animalType;
      if (this.lastTemplate.POOL2.animalType != this.projectile2) {
         this.effectStartTime.resetState();
         String skeletonPath = animalType.skeletonPath;
         float scale = animalType.scale;
         SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
         String[] skins = new String[1];
         skins[0] = animalType.skinName;
         this.requestLayerChange(skeletonPath, scale, mixConfig, skins);
         this.projectile2 = animalType;
      }

      long growDurationMs = (long)this.lastTemplate.POOL2.animalType.growDurationMs;
      delta = 1.0F;
      delta = 0.5F;
      long durationMs = growDurationMs;
      long growStartTime = this.lastTemplate.POOL2.growStartTime;
      long elapsedMs;
      if ((elapsedMs = System.currentTimeMillis() - growStartTime) < 0L) {
         elapsedMs = 0L;
      }

      if (elapsedMs > durationMs) {
         elapsedMs = durationMs;
      }

      delta = (float)elapsedMs / (float)durationMs;
      this.setScaleMultiplier(0.5F + delta * 0.5F);
   }
}
