package com.tlnt.engine.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.scene.GameObject;

public final class Entity {
   public final GameObject gameObject;
   public final PhysicsWorld world;
   public boolean active;
   public String name;
   public final Vector2 previousPosition;
   public final Vector2 position;
   public final Vector2 velocity;
   public AnimationState animationState = AnimationState.DEFAULT;
   public Direction direction = Direction.RIGHT;
   public EntityType entityType = EntityType.DEFAULT;
   public int entityStateId = EntityState.NEW;
   public boolean componentsDirty;
   public long lastAnimationTriggerTime;
   /**
    * Incremented every time the animation state CHANGES. {@code AnimationStateDispatcher} compares this number to detect X -> IDLE -> X
    * happening between 2 updates (an attack packet arriving exactly on the frame the previous attack animation finished) — the dispatcher
    * used to compare only the state so the animation did not replay, and other players / bots stood still while mobs still showed hit effects.
    */
   public int animationStateVersion;

   public final GameObject getGameObject() {
      return this.gameObject;
   }

   public final PhysicsWorld getPhysicsWorld() {
      return this.world;
   }

   public final boolean isActive() {
      return this.active;
   }

   public final Vector2 getPosition() {
      return this.position;
   }

   public final Vector2 getVelocity() {
      return this.velocity;
   }

   public final AnimationState getAnimationState() {
      return this.animationState;
   }

   public final void setAnimationState(AnimationState state) {
      if (state.equals(this.animationState)) {
         this.lastAnimationTriggerTime = System.currentTimeMillis();
      } else {
         this.animationStateVersion++;
      }

      this.animationState = state;
   }

   public final Direction getDirection() {
      return this.direction;
   }

   public final void setDirection(Direction direction) {
      this.direction = direction;
   }

   public final EntityType getEntityType() {
      return this.entityType;
   }

   public final void setEntityType(EntityType type) {
      this.entityType = type;
   }

   public final int getEntityStateId() {
      return this.entityStateId;
   }

   public final void setEntityStateId(int stateId) {
      this.entityStateId = stateId;
   }

   public final boolean isComponentsDirty() {
      return this.componentsDirty;
   }

   public Entity(GameObject gameObject, PhysicsWorld world) {
      this(gameObject, "Game Object", world);
   }

   public Entity(GameObject gameObject, String name, PhysicsWorld world) {
      this(gameObject, name, true, world);
   }

   public Entity(GameObject gameObject, String name, boolean active, PhysicsWorld world) {
      this.gameObject = gameObject;
      this.world = world;
      this.active = true;
      this.name = name;
      this.previousPosition = new Vector2();
      this.position = new Vector2();
      this.velocity = new Vector2();
      world.addEntity(this);
   }

   public final void setActive(boolean active) {
      this.active = active;
      Array<Component> components;
      if ((components = this.world.entityComponentsMap.get(this)) != null) {
         for (Component component : components) {
            if (active) {
               component.onEnable();
            } else {
               component.onDisable();
            }
         }
      }
   }

   public final <T extends Component> T getComponent(Class<T> type) {
      return this.findComponent(type, false);
   }

   public final <T extends Component> T findComponent(Class<T> type, boolean assignable) {
      Array components;
      if ((components = this.world.entityComponentsMap.get(this)) == null) {
         for (int i = 0; i < this.world.pendingAddComponents.size; i++) {
            Component pending;
            if ((pending = this.world.pendingAddComponents.get(i)).getEntity() == this
               && (assignable ? type.isAssignableFrom(pending.getClass()) : pending.getClass().equals(type))) {
               return (T)pending;
            }
         }
      } else {

         for (int j = 0; j < components.size; j++) {
            Component component = (Component)components.get(j);
            if (assignable ? type.isAssignableFrom(component.getClass()) : component.getClass().equals(type)) {
               return (T)component;
            }
         }
      }

      return null;
   }

   public final <T extends Component> Array<T> getComponents(Class<T> type) {
      return this.findComponents(type, new Array<>());
   }

   public <T extends Component> Array<T> findComponents(Class<T> type, Array<T> out) {
      out.clear();
      Array components;
      if ((components = this.world.entityComponentsMap.get(this)) == null) {

         for (int i = 0; i < this.world.pendingAddComponents.size; i++) {
            Component pending;
            if ((pending = this.world.pendingAddComponents.get(i)).getEntity() == this && pending.getClass().equals(type)) {
               out.add((T)components.get(i));
            }
         }
      } else {
         for (int j = 0; j < components.size; j++) {
            if (((Component)components.get(j)).getClass().equals(type)) {
               out.add((T)components.get(j));
            }
         }
      }

      return out;
   }

   public final void markComponentsDirty() {
      this.componentsDirty = true;
   }

   public final BodyComponent getBodyComponent() {
      Class<BodyComponent> bodyClass = BodyComponent.class;
      return this.findComponent(bodyClass, false);
   }

   public final void updateVelocity(float delta) {
      this.velocity.set(this.position).sub(this.previousPosition).scl(1.0F / delta);
      this.previousPosition.set(this.position);
   }

   @Override
   public final String toString() {
      return super.toString() + " - " + this.name;
   }

}
