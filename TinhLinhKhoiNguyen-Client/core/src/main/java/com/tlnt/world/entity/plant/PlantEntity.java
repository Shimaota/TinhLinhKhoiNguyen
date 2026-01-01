package com.tlnt.world.entity.plant;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.model.plant.PlantTemplateRef;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.anim.plant.PlantAnimDispatcher;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.plant.component.PlantSprite;
import com.tlnt.world.label.PlantNameLabel;
import com.tlnt.world.label.SelectionMarker;
import com.tlnt.world.skeleton.AnimationStateDispatcher;
import com.tlnt.world.skeleton.OverlayStackComponent;

public final class PlantEntity extends WorldEntity {
   public PlantTemplateRef POOL2;
   public AnimationStateDispatcher<PlantEntity> POOL;
   public PlantSprite skeleton;
   public OverlayStackComponent owner;

   public PlantEntity(LoginScreen screen, int id) {
      super(screen, 0, EntityType.PLANT);
      this.POOL2 = new PlantTemplateRef();
      this.setRenderOrder(4000000.0F);
   }

   @Override
   public final void onAwake() {
      super.onAwake();
      this.POOL = new PlantAnimDispatcher(this);
      this.skeleton = new PlantSprite(this);
      this.owner = new OverlayStackComponent(this);
      this.owner.addOverlay(new PlantNameLabel(this));
      this.owner.addOverlay(new SelectionMarker(this));
   }

   @Override
   public final void onStart() {
      this.setAnimationState(AnimationState.PLANT_IDLE);
   }

   @Override
   public final float getWidth() {
      return this.skeleton != null ? this.skeleton.getSkeletonWidth() : 0.0F;
   }

   @Override
   public final float getHeight() {
      return this.skeleton != null ? this.skeleton.getSkeletonHeight() : 0.0F;
   }

   @Override
   public final boolean containsPoint(Vector2 point) {
      return ((this.skeleton != null && this.skeleton.containsPoint(point) ? 1 : 0) != 0);
   }

   @Override
   public final void dispose() {
   }
}
