package com.tlnt.engine.physics.gizmo;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.Entity;
import com.tlnt.engine.render.RenderContext;

public final class DebugGizmo extends BaseComponent {
   public String projectile;
   public TransformGizmo speed;

   public DebugGizmo(Entity entity, String label) {
      super(entity);
      this.projectile = label;
      this.setRenderOrder(Float.MAX_VALUE);
      this.speed = new TransformGizmo(this.getEntity(), this.projectile);
      this.speed.setRenderOrder(Float.MAX_VALUE);
      this.speed.setEnabled(false);
   }

   @Override
   public final void update(float delta) {
   }

   @Override
   public final void render(RenderContext ctx) {
      if (ctx.isDebugMode()) {
         ;
      }
   }

   static {
      new Color(0.2F, 0.6F, 1.0F, 0.55F);
   }
}
