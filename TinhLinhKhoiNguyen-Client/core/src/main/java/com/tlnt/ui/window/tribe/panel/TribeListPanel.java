package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class TribeListPanel extends BaseTable {
   public com.tlnt.ui.window.tribe.panel.TribeListEntry tribeListEntry;
   public NinePatchAsset panelBackground;

   public TribeListPanel(TribeListEntry tribeListEntry, RenderContext renderContext) {
      super(renderContext);
      this.tribeListEntry = tribeListEntry;
      String patchPath = "ui/100";
      this.panelBackground = NinePatchAsset.obtain(patchPath, 50);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.panelBackground, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Danh sách bộ lạc",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + 516.0F,
            Anchor.BOTTOM_CENTER,
            30.0F,
            Color.WHITE
         );
      super.draw(batch, parentAlpha);
   }

}
