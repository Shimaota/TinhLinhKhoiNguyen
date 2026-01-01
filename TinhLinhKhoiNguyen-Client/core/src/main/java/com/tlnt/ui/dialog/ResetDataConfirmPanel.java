package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;

public final class ResetDataConfirmPanel extends BaseTable {
   public com.tlnt.ui.dialog.ConfirmDialog confirmDialog;
   public TextureAsset backgroundTexture;

   public ResetDataConfirmPanel(ConfirmDialog confirmDialog, RenderContext renderContext) {
      super(renderContext);
      this.confirmDialog = confirmDialog;
      this.backgroundTexture = TextureAsset.obtain("ui/44");
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT,
            "THÔNG BÁO",
            this.getX() + 544.0F,
            this.getY() + 486.0F,
            Anchor.BOTTOM_CENTER,
            50.0F,
            "#8B4E23"
         );
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String message = "Xóa toàn bộ dữ liệu để tải lại từ đầu";
      float textX = this.getX() + 544.0F;
      float textY = this.getY() + 428.0F;
      ctx.drawTextColorName(font, message, textX, textY, Anchor.BOTTOM_CENTER, 32.0F, "#8B4E23");
      ctx = super.renderContext;
      font = FontManager.PLAIN_FONT;
      message = "(Hành động này không ảnh hưởng\nđến dữ liệu tài khoản nhân vật của bạn)";
      textX = this.getX() + 544.0F;
      textY = this.getY() + 328.0F;
      ctx.drawTextColorName(font, message, textX, textY, Anchor.BOTTOM_CENTER, 24.0F, "#8B4E23");
      super.draw(batch, parentAlpha);
   }

}
