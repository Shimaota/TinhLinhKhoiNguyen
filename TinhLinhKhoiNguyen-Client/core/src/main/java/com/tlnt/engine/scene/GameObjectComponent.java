package com.tlnt.engine.scene;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.Component;
import com.tlnt.engine.physics.Entity;
import com.tlnt.engine.render.RenderContext;

public final class GameObjectComponent extends BaseComponent {
   public GameObject projectile;

   public GameObjectComponent(GameObject gameObject, Entity entity) {
      super(entity);
      this.projectile = gameObject;
   }

   @Override
   public final void onAdded() {
      this.projectile.onAwake();
   }

   @Override
   public final void onStart() {
      this.projectile.onStart();
   }

   @Override
   public final void update(float delta) {
      this.projectile.update(delta);
   }

   @Override
   public final void lateUpdate(float delta) {
   }

   @Override
   public final void onEnable() {
      this.projectile.onEnable();
   }

   @Override
   public final void onDisable() {
      this.projectile.onDisabled();
   }

   @Override
   public final void render(RenderContext ctx) {
      this.projectile.render(ctx);
   }

   @Override
   public final void renderDebug(RenderContext ctx) {
   }

   @Override
   public final void onBodyContact(BodyComponent body) {
   }

   @Override
   public final void onContactEnd(Component other, Contact contact, Object userData) {
   }

   @Override
   public final boolean onContactPostSolve(Component other, Contact contact, ContactImpulse impulse) {
      return false;
   }

   @Override
   public final boolean onContactPreSolve(Component other, Contact contact, Manifold oldManifold, Object userData) {
      return false;
   }
}
