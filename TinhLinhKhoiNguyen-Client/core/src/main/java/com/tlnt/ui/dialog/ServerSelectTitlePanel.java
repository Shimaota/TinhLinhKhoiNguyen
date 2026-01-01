package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;

public final class ServerSelectTitlePanel extends BaseTable {
   public com.tlnt.ui.dialog.ServerListDialog serverListDialog;
   public TextureAsset backgroundTexture;

   public ServerSelectTitlePanel(ServerListDialog serverListDialog, RenderContext renderContext) {
      super(renderContext);
      this.serverListDialog = serverListDialog;
      this.backgroundTexture = TextureAsset.obtain("ui/45");
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String title = "CHỌN MÁY CHỦ";
      float textX = this.getX() + 360.0F;
      float textY = this.getY() + 222.0F;
      ctx.drawTextColorName(font, title, textX, textY, Anchor.BOTTOM_LEFT, 50.0F, "#8B4E23");
      super.draw(batch, parentAlpha);
   }

}
