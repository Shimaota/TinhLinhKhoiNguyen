package com.tlnt.world.skeleton;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.render.RenderContext;

public abstract class FloatingOverlay {
   public boolean playerEntity = true;
   public Vector2 size;

   public FloatingOverlay(Vector2 size) {
      this.size = size;
   }

   public final void setVisible(boolean visible) {
      this.playerEntity = visible;
   }

   public final boolean isVisible() {
      return this.playerEntity;
   }

   public void refresh() {
   }

   public void render(RenderContext ctx, float x, float y) {
   }

   public void release() {
      this.size = null;
   }
}
