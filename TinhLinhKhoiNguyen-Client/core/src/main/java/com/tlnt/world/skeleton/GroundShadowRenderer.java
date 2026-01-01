package com.tlnt.world.skeleton;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.Entity;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;

public final class GroundShadowRenderer extends BaseComponent {
   public Vector2 projectile = new Vector2();
   public float speed;

   public GroundShadowRenderer(Entity entity) {
      super(entity);
      this.setRenderOrder(3000000.0F);
   }

   // Reuse the vector + callback (the original allocated 4 Vector2 + 1 callback every frame for EVERY character / mob).
   private final Vector2 rayStart = new Vector2();
   private final Vector2 rayEnd = new Vector2();
   private final Vector2 hitPoint = new Vector2();
   private final Vector2 lastCastAt = new Vector2(Float.NaN, Float.NaN);
   private ShadowGroundRayCast callback;
   private float sinceCast;

   @Override
   public final void update(float delta) {
      Vector2 pos = this.getEntity().getPosition();
      // Standing still (bots / AFK players / idle mobs): the ground under the feet does not change -> only redo the Box2D raycast every 0.5 s (in case
      // map collisions are loaded after the entity).
      this.sinceCast += delta;
      if (pos.x == this.lastCastAt.x && pos.y == this.lastCastAt.y && this.sinceCast < 0.5F) {
         return;
      }
      this.sinceCast = 0.0F;
      this.lastCastAt.set(pos);
      Vector2 rayStart = this.rayStart.set(pos).add(0.0F, 1.0F);
      Vector2 rayEnd = this.rayEnd.set(pos).add(0.0F, -500.0F);
      Vector2 hitPoint = this.hitPoint.set(-1.0F, -1.0F);
      if (this.callback == null) {
         this.callback = new ShadowGroundRayCast(this, hitPoint, rayStart);
      }
      this.getEntity().getGameObject().getScreen().getPhysicsWorld().getBox2dWorld().rayCast(this.callback, rayStart, rayEnd);
      if (hitPoint.x != -1.0F || hitPoint.y != -1.0F) {
         this.speed = (1.8F - hitPoint.dst(this.getEntity().getPosition())) / 1.8F;
         if (this.speed < 0.0F) {
            this.speed = 0.0F;
         }
      }
   }

   @Override
   public final void render(RenderContext ctx) {
      ctx.setBatchColorRgba(1.0F, 1.0F, 1.0F, this.speed);
      TextureAsset shadowTexture = TextureAsset.obtain("shadow");
      ctx.drawTextureSized(
         shadowTexture, this.projectile.x, this.projectile.y, (float)(shadowTexture.getWidth() << 1), (float)shadowTexture.getHeight(), Anchor.CENTER
      );
      ctx.resetBatchColor();
   }

}
