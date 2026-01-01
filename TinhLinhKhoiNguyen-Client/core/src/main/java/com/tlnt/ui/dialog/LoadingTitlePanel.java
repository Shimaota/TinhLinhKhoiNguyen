package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;

public final class LoadingTitlePanel extends BaseTable {
   public com.tlnt.ui.dialog.LoadingDialog loadingDialog;
   public TextureAsset backgroundTexture;

   public LoadingTitlePanel(LoadingDialog loadingDialog, RenderContext renderContext) {
      super(renderContext);
      this.loadingDialog = loadingDialog;
      this.backgroundTexture = TextureAsset.obtain("ui/45");
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT,
            this.loadingDialog.progressText,
            this.getX() + 543.0F,
            this.getY() + 226.0F,
            Anchor.BOTTOM_CENTER,
            24.0F,
            "#8B4E23"
         );
      super.draw(batch, parentAlpha);
   }

}
