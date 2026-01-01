package com.tlnt.world.entity.mob.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.physics.Entity;
import com.tlnt.model.mob.MobTemplate;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.skeleton.AnimationStateDispatcher;
import com.tlnt.world.skeleton.SimpleSpineSkeletonComponent;

public final class MobSprite extends SimpleSpineSkeletonComponent {
   public MobEntity projectile2;
   public MobTemplate lastTemplate;
   public int effectStartTime;
   public AnimationStateDispatcher effectDuration;

   public MobSprite(MobEntity mob) {
      super(mob.getEntity());
      this.effectStartTime = -1;
      this.projectile2 = mob;
      this.setRenderOrder(5500000.0F);
      this.addAnimationListener(new MobSpriteAnimListener(this));
   }

   @Override
   public final void onStart() {
      Entity entity = this.getEntity();
      this.effectDuration = entity.findComponent(AnimationStateDispatcher.class, true);
   }

   @Override
   public final void update(float delta) {
      MobTemplate template = this.projectile2.POOL2.template;
      if (this.projectile2.POOL2.template != this.lastTemplate || this.effectStartTime != this.projectile2.POOL2.variantIndex) {
         this.clearPendingRequests();
         this.effectDuration.resetState();
         String skeletonPath = template.skeletonPath;
         float scale = template.scale;
         SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
         String[] skins = new String[1];
         skins[0] = template.variants[this.projectile2.POOL2.variantIndex].skinName;
         this.requestLayerChange(skeletonPath, scale, mixConfig, skins);
         String[] skinNames = new String[1];
         skinNames[0] = template.variants[this.projectile2.POOL2.variantIndex].skinName;
         this.setSkins(skinNames);
         this.lastTemplate = template;
         this.effectStartTime = this.projectile2.POOL2.variantIndex;
      }
   }

   @Override
   public final void onDestroy() {
      super.onDestroy();
      this.projectile2 = null;
   }
}
