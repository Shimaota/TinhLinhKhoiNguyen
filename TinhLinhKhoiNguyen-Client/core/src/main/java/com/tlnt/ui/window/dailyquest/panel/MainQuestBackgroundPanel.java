package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class MainQuestBackgroundPanel extends BaseTable {
   public com.tlnt.ui.window.dailyquest.panel.MainQuestPanel parentPanel;
   public NinePatchAsset background;

   public MainQuestBackgroundPanel(MainQuestPanel panel, RenderContext ctx) {
      super(ctx);
      this.parentPanel = panel;
      this.background = NinePatchAsset.obtain("ui/100", 50);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Nhiệm vụ chính tuyến",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + 576.0F,
            Anchor.BOTTOM_CENTER,
            30.0F,
            Color.WHITE
         );
      super.draw(batch, parentAlpha);
   }

}
