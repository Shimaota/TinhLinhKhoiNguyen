package com.tlnt.ui.dialog;

import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.TextButton;

public final class SaveInfoTextButton extends TextButton {
   public com.tlnt.ui.dialog.LoginDialog buttonSprite;

   public SaveInfoTextButton(LoginDialog loginDialog, String text, RenderContext renderContext) {
      super(text, renderContext);
      this.buttonSprite = loginDialog;
   }

   @Override
   public final void drawBackground() {
   }

   @Override
   public final void drawLabel() {
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT,
            super.text,
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F,
            Anchor.CENTER,
            22.0F,
            "#8B4E23"
         );
   }

}
