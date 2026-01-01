package com.tlnt.world.entity.butterfly;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.butterfly.component.ButterflyFlightAI;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class AmbientButterfly extends GameObject {
   public SpineSkeletonComponent POOL2;
   public ButterflyFlightAI skeleton;

   public AmbientButterfly(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void onAwake() {
      this.skeleton = new ButterflyFlightAI(this);
      this.POOL2 = new SpineSkeletonComponent(this.getEntity());
      this.POOL2.requestLayerChange("skeletons/vfxs/butterfly", 0.25F, new SkeletonLoader.AnimationMixConfig());
      this.POOL2.playAnimation("fly", true, 1.0F);
      this.POOL2.setRenderOrder(1.0E8F);
   }

   @Override
   public final void dispose() {
   }

}
