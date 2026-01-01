package com.tlnt.world.entity.fish;

import com.badlogic.gdx.math.Rectangle;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.fish.component.FishSwimAI;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class AmbientFish extends GameObject {
   public SpineSkeletonComponent skeleton;
   public FishSwimAI POOL;
   public Rectangle POOL2;

   public AmbientFish(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void onAwake() {
      this.POOL = new FishSwimAI(this);
      this.skeleton = new SpineSkeletonComponent(this.getEntity());
      this.skeleton.requestLayerChange("skeletons/vfxs/fish", 0.25F, new SkeletonLoader.AnimationMixConfig());
      String animName = "swim";
      this.skeleton.playAnimation(animName, true, 1.0F);
      this.skeleton.setRenderOrder(8999999.0F);
   }

   @Override
   public final void onEnable() {
      SpineSkeletonComponent sprite = this.skeleton;
      String[] skins = new String[1];
      skins[0] = "fish" + GameUtils.randomInt(1, 5);
      sprite.setSkins(skins);
   }

   @Override
   public final void dispose() {
   }

}
