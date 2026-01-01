package com.tlnt.ui.window.map.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;

public final class MapRegionBackground extends BaseTable {
   public com.tlnt.ui.window.map.panel.MapRegionPanel regionPanel;
   public com.badlogic.gdx.scenes.scene2d.ui.ScrollPane contentScrollPane;
   public NinePatchAsset borderNinePatch;
   public TextureAsset backgroundTexture;

   public MapRegionBackground(MapRegionPanel panel, RenderContext ctx, ScrollPane scrollPane) {
      super(ctx);
      this.regionPanel = panel;
      this.contentScrollPane = scrollPane;
      this.borderNinePatch = NinePatchAsset.obtain("ui/172", 90);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawNinePatch(
            this.borderNinePatch,
            this.contentScrollPane.getX(),
            this.contentScrollPane.getY(),
            this.contentScrollPane.getWidth(),
            this.contentScrollPane.getHeight() + 1.0F,
            Anchor.BOTTOM_LEFT
         );
      super.draw(batch, parentAlpha);
   }

   public final MapRegionBackground setBackgroundTexture(TextureAsset texture) {
      this.backgroundTexture = texture;
      return this;
   }

}
