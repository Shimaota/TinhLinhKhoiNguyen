package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class PrivateChatMessageBubbleBackground extends BaseTable {
   public com.tlnt.ui.window.chat.panel.PrivateChatPanel.MessageBubble privateChatMessageRow;
   public NinePatchAsset backgroundPatch;

   public PrivateChatMessageBubbleBackground(PrivateChatPanel.MessageBubble messageBubble, RenderContext renderContext) {
      super(renderContext);
      this.privateChatMessageRow = messageBubble;
      this.backgroundPatch = NinePatchAsset.obtain("ui/13", 25);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
