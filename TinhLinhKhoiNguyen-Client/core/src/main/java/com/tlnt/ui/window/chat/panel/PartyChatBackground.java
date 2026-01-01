package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class PartyChatBackground extends BaseTable {
   public com.tlnt.ui.window.chat.panel.PartyChatPanel partyChatPanel;
   public NinePatchAsset backgroundPatch;

   public PartyChatBackground(PartyChatPanel partyChatPanel, RenderContext renderContext) {
      super(renderContext);
      this.partyChatPanel = partyChatPanel;
      this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
