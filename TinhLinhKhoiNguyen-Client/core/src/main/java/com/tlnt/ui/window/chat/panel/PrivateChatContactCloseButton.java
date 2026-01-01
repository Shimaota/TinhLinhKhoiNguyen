package com.tlnt.ui.window.chat.panel;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class PrivateChatContactCloseButton extends ShapeButton {
   public com.tlnt.ui.window.chat.panel.PrivateChatPanel privateChatPanel;
   public com.tlnt.ui.window.chat.panel.PrivateChatPanel.ContactRow contactRow;
   public TextureAsset closeIcon;

   public PrivateChatContactCloseButton(PrivateChatPanel.ContactRow contactRow, RenderContext renderContext, PrivateChatPanel privateChatPanel) {
      super(renderContext);
      this.contactRow = contactRow;
      this.privateChatPanel = privateChatPanel;
      this.closeIcon = TextureAsset.obtain("ui/65");
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawTextureSized(this.closeIcon, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.resetBatchColor();
   }

}
