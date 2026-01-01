package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.TextButton;

public final class PartyJoinRequestAcceptButton extends TextButton {
   public com.tlnt.ui.window.party.panel.PartyJoinRequestsPanel.JoinRequestRow buttonSprite;

   public PartyJoinRequestAcceptButton(PartyJoinRequestsPanel.JoinRequestRow joinRequestRow, String text, RenderContext renderContext) {
      super(text, renderContext);
      this.buttonSprite = joinRequestRow;
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/108"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
      super.renderContext.resetBatchColor();
   }

   @Override
   public final void drawLabel() {
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            super.text,
            this.getX() + this.getWidth() / 2.05F,
            this.getY() + this.getHeight() / 1.8F,
            Anchor.CENTER,
            22.0F,
            Color.WHITE
         );
   }

}
