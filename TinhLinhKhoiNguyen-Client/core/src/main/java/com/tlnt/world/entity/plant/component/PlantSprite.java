package com.tlnt.world.entity.plant.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.model.plant.PlantTemplate;
import com.tlnt.world.entity.plant.PlantEntity;
import com.tlnt.world.skeleton.AnimationStateDispatcher;
import com.tlnt.world.skeleton.SimpleSpineSkeletonComponent;

public final class PlantSprite extends SimpleSpineSkeletonComponent {
   public PlantTemplate projectile2;
   public PlantEntity lastTemplate;
   public AnimationStateDispatcher effectStartTime;

   public PlantSprite(PlantEntity plant) {
      super(plant.getEntity());
      this.lastTemplate = plant;
      this.setRenderOrder(4000000.0F);
   }

   @Override
   public final void onStart() {
      this.effectStartTime = this.getEntity().findComponent(AnimationStateDispatcher.class, true);
   }

   @Override
   public final void update(float delta) {
      PlantTemplate template = this.lastTemplate.POOL2.template;
      if (this.lastTemplate.POOL2.template != this.projectile2) {
         this.clearPendingRequests();
         this.effectStartTime.resetState();
         String skeletonName = template.skeletonName;
         float scale = template.skeletonScale;
         SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
         String[] skins = new String[1];
         skins[0] = template.idleAnimation;
         this.requestLayerChange(skeletonName, scale, mixConfig, skins);
         this.setSkeletonOffset(template.offsetX, template.offsetY);
         this.projectile2 = template;
      }
   }
}
