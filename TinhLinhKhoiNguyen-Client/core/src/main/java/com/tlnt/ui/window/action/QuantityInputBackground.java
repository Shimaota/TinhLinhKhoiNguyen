package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;

public final class QuantityInputBackground extends BaseTable {
   public com.tlnt.ui.window.action.QuantityInputDialog ownerDialog;
   public TextureAsset backgroundTexture;

   public QuantityInputBackground(QuantityInputDialog dialog, RenderContext renderContext) {
      super(renderContext);
      this.ownerDialog = dialog;
      this.backgroundTexture = TextureAsset.obtain("ui/116");
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
