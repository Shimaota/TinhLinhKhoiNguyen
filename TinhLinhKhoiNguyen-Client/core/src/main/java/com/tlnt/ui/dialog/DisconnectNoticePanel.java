package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;

public final class DisconnectNoticePanel extends BaseTable {
   public com.tlnt.ui.dialog.NoticeDialog noticeDialog;
   public TextureAsset backgroundTexture;

   public DisconnectNoticePanel(NoticeDialog noticeDialog, RenderContext renderContext) {
      super(renderContext);
      this.noticeDialog = noticeDialog;
      this.backgroundTexture = TextureAsset.obtain("ui/44");
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT, "THÔNG BÁO", this.getX() + 406.0F, this.getY() + 486.0F, Anchor.BOTTOM_LEFT, 50.0F, "#8B4E23"
         );
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT,
            "Máy chủ đóng hoặc mất kết nối",
            this.getX() + 310.0F,
            this.getY() + 428.0F,
            Anchor.BOTTOM_LEFT,
            32.0F,
            "#8B4E23"
         );
      super.draw(batch, parentAlpha);
   }

}
