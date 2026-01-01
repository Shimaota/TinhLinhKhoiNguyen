package com.tlnt.world.entity.player.component;

import box2dLight.ConeLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.Entity;

public final class GlowAuraComponent extends BaseComponent {
   public ConeLight coneLight;
   public float speed = 270.0F;
   public float active = 20.0F;
   public float autoFlipEnabled = 0.2F;
   public float projectile2;
   public int lastTemplate = 12;
   public Color glowColor;
   public float glowWidth;
   public float glowHeight;
   public final Vector2 glowOffset;

   public GlowAuraComponent(Entity entity) {
      super(entity);
      this.glowColor = Color.valueOf("FFF2C2");
      this.glowWidth = 8.0F;
      this.glowHeight = 35.0F;
      this.glowOffset = new Vector2(0.0F, 0.0F);
   }

   @Override
   public final void onAdded() {
      RayHandler rayHandler = this.getEntity().getPhysicsWorld().getRayHandler();
      this.getEntity().getPosition();
      this.coneLight = new ConeLight(rayHandler, this.lastTemplate, this.glowColor, this.glowWidth, 10.0F, 10.0F, this.speed, this.glowHeight);
      this.coneLight.setSoftnessLength(2.0F);
      this.coneLight.setStaticLight(false);
      this.coneLight.setXray(true);
   }

   @Override
   public final void update(float delta) {
      this.projectile2 += delta;
      delta = this.speed + MathUtils.sin((float) (Math.PI * 2) * this.autoFlipEnabled * this.projectile2) * this.active;
      this.coneLight.setDirection(delta);
      this.coneLight.setPosition(10.0F, 10.0F);
   }

   @Override
   public final void onDestroy() {
      if (this.coneLight != null) {
         this.coneLight.remove();
         this.coneLight = null;
      }
   }

}
