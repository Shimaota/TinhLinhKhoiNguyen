package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class SystemChatScrollBackground extends BaseTable {
   public com.tlnt.ui.window.chat.panel.SystemChatPanel systemChatPanel;
   public NinePatchAsset backgroundPatch;

   public SystemChatScrollBackground(SystemChatPanel systemChatPanel, RenderContext renderContext) {
      super(renderContext);
      this.systemChatPanel = systemChatPanel;
      this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
