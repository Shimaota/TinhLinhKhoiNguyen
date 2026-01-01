package com.tlnt.engine.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;

public class BodyComponent extends BaseComponent {
   public final BodyDef projectile;
   public Body speed;
   public final Rectangle active;
   public final Vector2 autoFlipEnabled = new Vector2();
   public float projectile2 = 1.0F;

   public final Body getBody() {
      return this.speed;
   }

   public BodyComponent(int bodyType, Entity entity) {
      this(bodyType, 1.0F, entity);
   }

   public BodyComponent(int bodyType, float gravityScale, Entity entity) {
      super(entity);
      this.projectile = new BodyDef();
      this.active = new Rectangle();
      switch (bodyType) {
         case BodyType.STATIC:
            this.projectile.type = BodyDef.BodyType.StaticBody;
            break;
         case BodyType.KINEMATIC:
            this.projectile.type = BodyDef.BodyType.KinematicBody;
            break;
         case BodyType.DYNAMIC:
            this.projectile.type = BodyDef.BodyType.DynamicBody;
      }
   }

   @Override
   public final void onAdded() {
      super.onAdded();
      if (this.projectile != null) {
         this.speed = this.getWorld().createBody(this, this.projectile);
         this.speed.setTransform(this.autoFlipEnabled, 0.0F);
         this.speed.setGravityScale(this.projectile2);
      }
   }

   @Override
   public final void onEnable() {
      super.onEnable();
      if (this.speed != null) {
         this.speed.setActive(true);
      }
   }

   @Override
   public final void onDisable() {
      super.onDisable();
      if (this.speed != null) {
         this.speed.setActive(false);
      }
   }

   @Override
   public final void update(float delta) {
      this.getEntity().getPosition().set(this.speed.getPosition().x, this.speed.getPosition().y);
   }

   public final BodyComponent setBoundsSize(float width, float height) {
      this.active.setSize(width, height);
      return this;
   }

   public final BodyComponent setInitialPosition(Vector2 position) {
      float y = position.y;
      float x = position.x;
      this.autoFlipEnabled.set(x, y);
      return this;
   }

   @Override
   public final void onDestroy() {
      super.onDestroy();
      Body body = this.speed;
      PhysicsWorld world = this.getWorld();
      Gdx.app.postRunnable(new BodyDestroyer(world, body));
   }

   public final Rectangle getBoundsRect() {
      this.active.x = this.speed.getPosition().x - this.active.width / 2.0F;
      this.active.y = this.speed.getPosition().y - this.active.height / 2.0F;
      return this.active;
   }
}
