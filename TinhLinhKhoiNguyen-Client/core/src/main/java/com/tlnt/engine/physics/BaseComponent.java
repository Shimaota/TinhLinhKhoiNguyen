package com.tlnt.engine.physics;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.tlnt.engine.render.RenderContext;

public class BaseComponent extends Component {
   public boolean projectile;

   public BaseComponent(Entity entity) {
      super(entity);
   }

   @Override
   public void onAdded() {
   }

   @Override
   public void onStart() {
   }

   @Override
   public void update(float delta) {
   }

   @Override
   public void lateUpdate(float delta) {
   }

   @Override
   public boolean onContactPreSolve(Component other, Contact contact, Manifold oldManifold, Object userData) {
      return false;
   }

   @Override
   public boolean onContactPostSolve(Component other, Contact contact, ContactImpulse impulse) {
      return false;
   }

   @Override
   public void onBodyContact(BodyComponent body) {
   }

   @Override
   public void onContactEnd(Component other, Contact contact, Object userData) {
   }

   @Override
   public void render(RenderContext ctx) {
   }

   @Override
   public void renderDebug(RenderContext ctx) {
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onDestroy() {
   }

   @Override
   public final boolean isDebugEnabled() {
      return this.projectile;
   }
}
