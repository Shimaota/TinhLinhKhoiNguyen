package com.tlnt.world.groundeffect;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class EarthSpikeEffect extends GameObject {
   public static final Pool<EarthSpikeEffect> POOL2 = new EarthSpikeEffectPool();
   public SpineSkeletonComponent skeleton = new SpineSkeletonComponent(this.getEntity());

   public EarthSpikeEffect(LoginScreen screen) {
      super(screen);
      this.skeleton.setRenderOrder(1.1E7F);
      this.skeleton.requestLayerChange("skeletons/vfxs/earth_spike", 0.25F, new SkeletonLoader.AnimationMixConfig(0.0F));
      this.skeleton.addAnimationListener(new EarthSpikeEffectReleaseListener(this));
   }

   public final void playAt(float x, float y) {
      this.teleportTo(x, y);
      String animName = "earth_spike";
      this.skeleton.playAnimation(animName, false, 1.0F);
      AudioManager.playSound("earth_spike", this.getCenterPosition());
   }

   @Override
   public final void dispose() {
      this.skeleton = null;
   }

}
