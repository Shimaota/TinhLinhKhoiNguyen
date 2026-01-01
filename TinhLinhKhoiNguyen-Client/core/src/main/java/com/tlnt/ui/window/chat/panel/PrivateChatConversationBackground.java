package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class PrivateChatConversationBackground extends BaseTable {
   public com.tlnt.ui.window.chat.panel.PrivateChatPanel privateChatPanel;
   public NinePatchAsset backgroundPatch;

   public PrivateChatConversationBackground(PrivateChatPanel privateChatPanel, RenderContext renderContext) {
      super(renderContext);
      this.privateChatPanel = privateChatPanel;
      this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      if (this.privateChatPanel.selectedContact != null) {
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.privateChatPanel.selectedContact.partnerName,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 483.0F,
               Anchor.BOTTOM_CENTER,
               30.0F,
               Color.WHITE
            );
      }

      super.draw(batch, parentAlpha);
   }

}
