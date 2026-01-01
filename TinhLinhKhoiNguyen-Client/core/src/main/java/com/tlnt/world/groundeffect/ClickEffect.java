package com.tlnt.world.groundeffect;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class ClickEffect extends GameObject {
   public static final Pool<ClickEffect> POOL2 = new ClickEffectPool();
   public SpineSkeletonComponent skeleton = new SpineSkeletonComponent(this.getEntity());

   public ClickEffect(LoginScreen screen) {
      super(screen);
      this.skeleton.setRenderOrder(1.1E7F);
      SpineSkeletonComponent skeletonComp = this.skeleton;
      String skeletonPath = "ui/click";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.0F);
      skeletonComp.requestLayerChange(skeletonPath, 0.25F, mixConfig);
      this.skeleton.addAnimationListener(new ClickEffectReleaseListener(this));
   }

   public final void playAt(float x, float y) {
      this.teleportTo(x, y);
      this.skeleton.playAnimation("click", false, 1.0F);
      AudioManager.playSound("click", null);
   }

   @Override
   public final void dispose() {
      this.skeleton = null;
   }

}
