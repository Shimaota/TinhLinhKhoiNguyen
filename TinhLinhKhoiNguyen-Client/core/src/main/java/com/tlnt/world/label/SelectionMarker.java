package com.tlnt.world.label;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.GameState;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.skeleton.FloatingOverlay;

public final class SelectionMarker extends FloatingOverlay {
   public WorldEntity playerEntity;

   public SelectionMarker(WorldEntity worldEntity) {
      super(new Vector2(0.0F, 40.0F));
      this.playerEntity = worldEntity;
   }

   @Override
   public final void refresh() {
      int visibleFlag;
      if (this.playerEntity != null && this.playerEntity.equals(GameState.targetEntity)) {
         visibleFlag = 1;
      } else {
         visibleFlag = 0;
      }

      this.setVisible((visibleFlag != 0));
   }

   @Override
   public final void render(RenderContext ctx, float x, float y) {
      ctx.drawTexture(
         TextureAsset.obtain("ui/150"), x, y + GameUtils.oscillate(1.0F, 0.0F, 10.0F) - 5.0F, Anchor.CENTER
      );
   }

   @Override
   public final void release() {
      this.playerEntity = null;
   }

}
