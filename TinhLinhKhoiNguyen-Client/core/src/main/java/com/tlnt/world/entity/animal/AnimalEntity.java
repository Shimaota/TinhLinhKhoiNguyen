package com.tlnt.world.entity.animal;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.farm.AnimalGrowthState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.anim.animal.AnimalAnimDispatcher;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.animal.component.AnimalSprite;
import com.tlnt.world.entity.animal.component.AnimalWanderAI;
import com.tlnt.world.label.AnimalRespawnLabel;
import com.tlnt.world.label.SelectionMarker;
import com.tlnt.world.skeleton.AnimationStateDispatcher;
import com.tlnt.world.skeleton.GroundShadowRenderer;
import com.tlnt.world.skeleton.OverlayStackComponent;

public final class AnimalEntity extends WorldEntity {
   public AnimalGrowthState POOL2;
   public AnimationStateDispatcher<AnimalEntity> owner;
   public AnimalSprite skeleton;
   public AnimalWanderAI POOL;
   public OverlayStackComponent target;

   public AnimalEntity(LoginScreen screen, int id) {
      super(screen, 0, EntityType.ANIMAL);
      this.POOL2 = new AnimalGrowthState();
      this.setRenderOrder(5000000.0F);
   }

   @Override
   public final void onAwake() {
      super.onAwake();
      this.owner = new AnimalAnimDispatcher(this);
      this.skeleton = new AnimalSprite(this);
      this.POOL = new AnimalWanderAI(this);
      new GroundShadowRenderer(this.getEntity());
      this.target = new OverlayStackComponent(this);
      this.target.addOverlay(new AnimalRespawnLabel(this));
      this.target.addOverlay(new com.tlnt.world.label.AnimalGrowFastButton(this)); // decompiled C3480 "Grow fast"
      this.target.addOverlay(new SelectionMarker(this));
   }

   @Override
   public final void render(RenderContext ctx) {
   }

   @Override
   public final void onStart() {
      this.setAnimationState(AnimationState.ANIMAL_IDLE);
   }

   @Override
   public final void onEnable() {
      this.POOL.projectile = true;
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
      if (this.skeleton != null && this.skeleton.containsPoint(point)) {
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final void dispose() {
   }
}
