package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;

public final class RegisterTitlePanel extends BaseTable {
   public com.tlnt.ui.dialog.RegisterDialog registerDialog;
   public TextureAsset backgroundTexture;

   public RegisterTitlePanel(RegisterDialog registerDialog, RenderContext renderContext) {
      super(renderContext);
      this.registerDialog = registerDialog;
      this.backgroundTexture = TextureAsset.obtain("ui/155");
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT,
            "ĐĂNG KÝ",
            this.getX() + 406.0F,
            this.getY() + 653.0F,
            Anchor.BOTTOM_LEFT,
            50.0F,
            "#8B4E23"
         );
      super.draw(batch, parentAlpha);
   }

}
