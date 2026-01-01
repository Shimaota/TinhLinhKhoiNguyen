package com.tlnt.world.groundeffect;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class SpearProjectileEffect extends GameObject {
   public static final Pool<SpearProjectileEffect> POOL2 = new SpearProjectileEffectPool();
   public SpineSkeletonComponent skeleton = new SpineSkeletonComponent(this.getEntity());

   public SpearProjectileEffect(LoginScreen screen) {
      super(screen);
      SpineSkeletonComponent skeletonComp = this.skeleton;
      String skeletonPath = "skeletons/vfxs/spear";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.0F);
      skeletonComp.requestLayerChange(skeletonPath, 0.25F, mixConfig);
      this.skeleton.addAnimationListener(new SpearProjectileEffectReleaseListener(this));
   }

   public final void playAt(float x, float y, String skinName, float renderOrder) {
      this.teleportTo(x, y);
      SpineSkeletonComponent skeletonComp = this.skeleton;
      String[] skins = new String[1];
      skins[0] = skinName;
      skeletonComp.setSkins(skins);
      this.skeleton.playAnimation("spear1", false, 1.0F);
      this.skeleton.setRenderOrder(renderOrder);
   }

   @Override
   public final void dispose() {
      this.skeleton = null;
   }

}
