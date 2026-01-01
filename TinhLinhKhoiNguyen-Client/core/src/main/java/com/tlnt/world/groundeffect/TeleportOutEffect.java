package com.tlnt.world.groundeffect;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class TeleportOutEffect extends GameObject {
   public static final Pool<TeleportOutEffect> POOL2 = new TeleportOutEffectPool();
   public SpineSkeletonComponent skeleton = new SpineSkeletonComponent(this.getEntity());

   public TeleportOutEffect(LoginScreen screen) {
      super(screen);
      this.skeleton.setRenderOrder(6500000.0F);
      this.skeleton.requestLayerChange("skeletons/vfxs/out", 0.25F, new SkeletonLoader.AnimationMixConfig(0.0F));
      this.skeleton.addAnimationListener(new TeleportOutEffectReleaseListener(this));
   }

   public final void playOnPlayer(PlayerEntity player) {
      this.setWorldPosition(player.getCenterPosition());
      String animName = "out";
      this.skeleton.playAnimation(animName, false, 1.0F);
   }

   @Override
   public final void dispose() {
      this.skeleton = null;
   }

}
