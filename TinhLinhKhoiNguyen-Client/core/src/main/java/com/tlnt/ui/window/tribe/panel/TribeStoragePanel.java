package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class TribeStoragePanel extends BaseTable {
   public com.tlnt.ui.window.tribe.panel.TribePendingPanel ownerPanel;
   public NinePatchAsset background;

   public TribeStoragePanel(TribePendingPanel ownerPanel, RenderContext renderContext) {
      super(renderContext);
      this.ownerPanel = ownerPanel;
      String patchPath = "ui/100";
      this.background = NinePatchAsset.obtain(patchPath, 50);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Chờ duyệt",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + 576.0F,
            Anchor.BOTTOM_CENTER,
            30.0F,
            Color.WHITE
         );
      if (GameState.tribeData.myTribe != null) {
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Số lượng: " + GameState.tribeData.myTribe.applicants.size,
               this.getX() + 25.0F,
               this.getY() + 17.0F,
               Anchor.BOTTOM_LEFT,
               22.0F,
               Color.WHITE
            );
      }

      super.draw(batch, parentAlpha);
   }

}
