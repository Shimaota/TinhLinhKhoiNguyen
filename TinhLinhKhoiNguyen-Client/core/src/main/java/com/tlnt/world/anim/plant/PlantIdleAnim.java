package com.tlnt.world.anim.plant;

import com.tlnt.world.anim.AnimationBehavior;
import com.tlnt.world.entity.plant.PlantEntity;
import com.tlnt.engine.scene.GameObject;

public final class PlantIdleAnim extends AnimationBehavior<PlantEntity> {
   public static final PlantIdleAnim INSTANCE = new PlantIdleAnim();

   @Override
   public final void onEnter(PlantEntity entity) {
      String animName = "idle";
      PlantEntity plant;
      (plant = (PlantEntity)entity).skeleton.playAnimation(animName, true, 1.0F);
   }
}
