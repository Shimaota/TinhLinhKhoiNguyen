package com.tlnt.world.entity.npc.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.model.npc.NpcTemplate;
import com.tlnt.world.entity.npc.NpcEntity;
import com.tlnt.world.skeleton.AnimationStateDispatcher;
import com.tlnt.world.skeleton.SimpleSpineSkeletonComponent;

public final class NpcSprite extends SimpleSpineSkeletonComponent {
   public NpcEntity projectile2;
   public NpcTemplate lastTemplate;
   public AnimationStateDispatcher effectStartTime;

   public NpcSprite(NpcEntity npc) {
      super(npc.getEntity());
      this.projectile2 = npc;
      this.setRenderOrder(4500000.0F);
      this.addAnimationListener(new NpcSpriteAnimationListener(this));
   }

   @Override
   public final void onStart() {
      this.effectStartTime = this.getEntity().findComponent(AnimationStateDispatcher.class, true);
   }

   @Override
   public final void update(float delta) {
      NpcTemplate template = this.projectile2.POOL2.template;
      if (this.projectile2.POOL2.template != this.lastTemplate) {
         this.clearPendingRequests();
         this.effectStartTime.resetState();
         String skeletonPath = template.skeletonPath;
         float scale = template.skeletonScale;
         SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.2F);
         String[] skins = new String[1];
         skins[0] = template.idleAnimation;
         this.requestLayerChange(skeletonPath, scale, mixConfig, skins);
         this.lastTemplate = template;
      }
   }

   @Override
   public final void onDestroy() {
      super.onDestroy();
      this.projectile2 = null;
   }
}
