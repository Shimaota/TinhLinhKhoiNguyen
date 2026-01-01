package com.tlnt.engine.physics;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.tlnt.engine.physics.component.ComponentState;
import com.tlnt.engine.render.RenderContext;

public abstract class Component {
   public final Entity projectile;
   public boolean speed = true;
   public boolean active = true;
   public float autoFlipEnabled;
   public float projectile2;
   public int lastTemplate = ComponentState.CREATED;

   public final Entity getEntity() {
      return this.projectile;
   }

   public final boolean isEnabled() {
      return this.speed;
   }

   public final void setEnabled(boolean enabled) {
      this.speed = enabled;
   }

   public final boolean isVisible() {
      return this.active;
   }

   public final void setVisible(boolean visible) {
      this.active = visible;
   }

   public final int getState() {
      return this.lastTemplate;
   }

   public final void setState(int state) {
      this.lastTemplate = state;
   }

   public Component(Entity entity) {
      this.projectile = entity;
      PhysicsWorld world = entity.getPhysicsWorld();
      int state = ComponentState.PENDING_ADD;
      this.lastTemplate = state;
      world.pendingAddComponents.add(this);
   }

   public abstract void onAdded();

   public abstract void onStart();

   public abstract void update(float delta);

   public abstract void lateUpdate(float delta);

   public abstract boolean onContactPreSolve(Component other, Contact contact, Manifold oldManifold, Object userData);

   public abstract boolean onContactPostSolve(Component other, Contact contact, ContactImpulse impulse);

   public abstract void onBodyContact(BodyComponent body);

   public abstract void onContactEnd(Component other, Contact contact, Object userData);

   public abstract void render(RenderContext ctx);

   public abstract void renderDebug(RenderContext ctx);

   public abstract boolean isDebugEnabled();

   public abstract void onEnable();

   public abstract void onDisable();

   public abstract void onDestroy();

   public final void removeFromWorld() {
      this.projectile.getPhysicsWorld().removeComponent(this);
   }

   public final PhysicsWorld getWorld() {
      return this.projectile.getPhysicsWorld();
   }

   public final Component setRenderOrder(float renderOrder) {
      if (this.autoFlipEnabled == renderOrder) {
         return this;
      } else {
         this.autoFlipEnabled = renderOrder;
         this.getWorld().markRenderOrderDirty();
         return this;
      }
   }

   public final float getRenderOrder() {
      return this.autoFlipEnabled;
   }

   public final float getRenderOrderY() {
      return this.projectile2;
   }
}
