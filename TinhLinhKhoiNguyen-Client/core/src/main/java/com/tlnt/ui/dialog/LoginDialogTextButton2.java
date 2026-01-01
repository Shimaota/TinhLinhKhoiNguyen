package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.TextButton;

public final class LoginDialogTextButton2 extends TextButton {
   public com.tlnt.ui.dialog.LoginDialog buttonSprite;
   public LoginDialogTextButton2(LoginDialog loginDialog, String text, RenderContext renderContext) {
      super(text, renderContext);
      this.buttonSprite = loginDialog;
   }

   @Override
   public final void drawBackground() {
   }

   @Override
   public final void drawLabel() {
      Color textColor = this.isPressed() ? Color.GRAY : (this.isOver() ? Color.LIGHT_GRAY : Color.WHITE);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            super.text,
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F,
            Anchor.CENTER,
            24.0F,
            textColor
         );
   }
}
