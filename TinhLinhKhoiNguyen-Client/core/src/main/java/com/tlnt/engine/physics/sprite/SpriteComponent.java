package com.tlnt.engine.physics.sprite;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;

public class SpriteComponent extends BaseComponent {
   public TextureAsset projectile;
   public boolean speed;

   public SpriteComponent(com.tlnt.engine.physics.Entity entity) {
      super(entity);
   }

   @Override
   public final void render(RenderContext ctx) {
   }

   @Override
   public final void renderDebug(RenderContext ctx) {
   }

   public final boolean containsPoint(Vector2 point) {
      return false;
   }
}
