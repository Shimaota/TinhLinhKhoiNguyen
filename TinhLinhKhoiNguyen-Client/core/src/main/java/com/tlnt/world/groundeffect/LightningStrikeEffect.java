package com.tlnt.world.groundeffect;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class LightningStrikeEffect extends GameObject {
   public static final Pool<LightningStrikeEffect> POOL2 = new LightningStrikeEffectPool();
   public SpineSkeletonComponent skeleton = new SpineSkeletonComponent(this.getEntity());

   public LightningStrikeEffect(LoginScreen screen) {
      super(screen);
      this.skeleton.setRenderOrder(1.1E7F);
      this.skeleton.requestLayerChange("skeletons/vfxs/lightning_strike", 0.25F, new SkeletonLoader.AnimationMixConfig(0.0F));
      this.skeleton.addAnimationListener(new LightningStrikeEffectReleaseListener(this));
   }

   public final void playAt(float x, float y) {
      this.teleportTo(x, y);
      this.skeleton.playAnimation("lightning_strike", false, 1.0F);
      AudioManager.playSound("lightning_strike", this.getCenterPosition());
   }

   @Override
   public final void dispose() {
      this.skeleton = null;
   }

}
