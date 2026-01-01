package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;

public final class LoginTitlePanel extends BaseTable {
   public com.tlnt.ui.dialog.LoginDialog loginDialog;
   public TextureAsset backgroundTexture;

   public LoginTitlePanel(LoginDialog loginDialog, RenderContext renderContext) {
      super(renderContext);
      this.loginDialog = loginDialog;
      this.backgroundTexture = TextureAsset.obtain("ui/44");
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String title = "ĐĂNG NHẬP";
      float textX = this.getX() + 406.0F;
      float textY = this.getY() + 486.0F;
      ctx.drawTextColorName(font, title, textX, textY, Anchor.BOTTOM_LEFT, 50.0F, "#8B4E23");
      super.draw(batch, parentAlpha);
   }

}
