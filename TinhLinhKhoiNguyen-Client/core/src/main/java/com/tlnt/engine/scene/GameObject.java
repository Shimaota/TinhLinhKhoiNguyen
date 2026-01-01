package com.tlnt.engine.scene;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.physics.Entity;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.physics.collider.BoxCollider;
import com.tlnt.engine.physics.collider.CircleCollider;
import com.tlnt.engine.physics.collider.PolygonCollider;
import com.tlnt.engine.physics.gizmo.DebugGizmo;
import com.tlnt.engine.physics.sprite.SpriteComponent;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.screen.BaseScreen;
import com.tlnt.world.entity.WorldEntity;

public abstract class GameObject {
   public int POOL2;
   public boolean skeleton;
   public boolean POOL;
   public BaseScreen owner;
   public Entity target;
   public GameObjectComponent movementComponent;
   public final Vector2 target2 = new Vector2();

   public GameObject(BaseScreen screen) {
      this(screen, EntityType.DEFAULT);
   }

   public GameObject(BaseScreen screen, EntityType entityType) {
      this(0, screen, entityType);
   }

   public GameObject(int id, BaseScreen screen, EntityType entityType) {
      this.POOL2 = id;
      this.owner = screen;
      this.target = new Entity(this, this.owner.getPhysicsWorld());
      this.target.setEntityType(entityType);
      this.movementComponent = new GameObjectComponent(this, this.target);
      switch (entityType) {
         case PLAYER:
         case CAMERA:
         case MAP:
            new DebugGizmo(this.target, entityType.name());
      }
   }

   public final void setId(int id) {
      this.POOL2 = id;
   }

   public final int getId() {
      return this.POOL2;
   }

   public final Entity getEntity() {
      return this.target;
   }

   public final EntityType getEntityType() {
      return this.target == null ? EntityType.DEFAULT : this.target.getEntityType();
   }

   public final BaseScreen getScreen() {
      return this.owner;
   }

   public void onAwake() {
   }

   public void onStart() {
   }

   public void update(float delta) {
   }

   public void onEnable() {
   }

   public void onDisabled() {
   }

   public void render(RenderContext ctx) {
   }

   public final GameObject setRenderOrder(float renderOrder) {
      this.movementComponent.setRenderOrder(renderOrder);
      return this;
   }

   public final void setAnimationState(AnimationState state) {
      if (this.target != null) {
         AnimationState current = this.getAnimationState();
         if (this.canChangeAnimationState(current, state)) {
            this.target.setAnimationState(state);
         }
      }
   }

   public boolean canChangeAnimationState(AnimationState current, AnimationState next) {
      return true;
   }

   public final AnimationState getAnimationState() {
      return this.target != null ? this.target.getAnimationState() : AnimationState.DEFAULT;
   }

   public final Direction getDirection() {
      return this.target != null ? this.target.getDirection() : Direction.RIGHT;
   }

   public final void setDirection(Direction direction) {
      if (this.target != null) {
         this.target.setDirection(direction);
      }
   }

   public final GameObject setPosition(float x, float y) {
      this.target.getPosition().set(x, y);
      return this;
   }

   public final Vector2 getPosition() {
      return this.target == null ? Vector2.Zero : this.target.getPosition();
   }

   public final Vector2 getVelocity() {
      return this.target == null ? Vector2.Zero : this.target.getVelocity();
   }

   public final Vector2 getCenterPosition() {
      return this.target2.set(this.getPosition()).add(0.0F, this.getHeight() / 2.0F);
   }

   public final GameObject setActive(boolean active) {
      if (this.target.isActive() == active) {
         return this;
      } else {
         this.target.setActive(active);
         return this;
      }
   }

   public final boolean isActive() {
      return this.target.isActive();
   }

   public final void removeFromWorld() {
      this.owner.getPhysicsWorld().removeEntity(this.target);
      Gdx.app.postRunnable(new GameObjectDisposeTask(this));
   }

   public final void teleportTo(float x, float y) {
      if (this.target != null) {
         BodyComponent bodyComponent;
         if ((bodyComponent = this.target.getBodyComponent()) != null && bodyComponent.getBody() != null) {
            this.getVelocity().set(x, y).sub(this.getPosition().x, this.getPosition().y).scl(1.0F / Gdx.graphics.getDeltaTime());
            bodyComponent.getBody().setTransform(x, y, bodyComponent.getBody().getAngle());
            this.getPosition().set(x, y);
         } else {
            this.getVelocity().set(x, y).sub(this.getPosition().x, this.getPosition().y).scl(1.0F / Gdx.graphics.getDeltaTime());
            this.getPosition().set(x, y);
         }
      }
   }

   public void setWorldPosition(Vector2 position) {
      this.teleportTo(position.x, position.y);
   }

   public boolean containsPoint(Vector2 point) {
      Array.ArrayIterator iter = this.target.getComponents(CircleCollider.class).iterator();

      while (iter.hasNext()) {
         if (((CircleCollider)iter.next()).testPoint(point)) {
            return true;
         }
      }

      Array.ArrayIterator iter2 = this.target.getComponents(BoxCollider.class).iterator();

      while (iter2.hasNext()) {
         if (((BoxCollider)iter2.next()).testPoint(point)) {
            return true;
         }
      }

      iter = this.target.getComponents(PolygonCollider.class).iterator();

      while (iter.hasNext()) {
         if (((PolygonCollider)iter.next()).testPoint(point)) {
            return true;
         }
      }

      iter2 = this.target.getComponents(SpriteComponent.class).iterator();

      while (iter2.hasNext()) {
         if (((SpriteComponent)iter2.next()).containsPoint(point)) {
            return true;
         }
      }

      return false;
   }

   public float getWidth() {
      return 0.0F;
   }

   public float getHeight() {
      return 0.0F;
   }

   public final float getRenderOrder() {
      return this.movementComponent.getRenderOrder();
   }

   public final boolean isDisposed() {
      return this.skeleton;
   }

   public final float distanceTo(WorldEntity other) {
      return other == null ? 0.0F : this.getPosition().dst(other.getPosition());
   }

   public final void setSensorEnabled(boolean enabled) {
      if (this.POOL != enabled) {
         BodyComponent bodyComponent;
         if ((bodyComponent = this.target.getBodyComponent()) != null) {
            Array.ArrayIterator fixtureIter = bodyComponent.getBody().getFixtureList().iterator();

            while (fixtureIter.hasNext()) {
               ((Fixture)fixtureIter.next()).setSensor(enabled);
            }

            this.POOL = enabled;
         }
      }
   }

   public final void faceTowards(Vector2 targetPos) {
      Vector2 position = this.getPosition();
      float dx;
      if ((dx = targetPos.x - position.x) != 0.0F) {
         this.setDirection(dx > 0.0F ? Direction.RIGHT : Direction.LEFT);
      }
   }

   public abstract void dispose();
}
