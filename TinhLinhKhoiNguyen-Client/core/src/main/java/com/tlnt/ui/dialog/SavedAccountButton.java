package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.Settings;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class SavedAccountButton extends ShapeButton {
   public com.tlnt.ui.dialog.LoginDialog loginDialog;
   public CompositeSprite buttonSprites;

   public SavedAccountButton(LoginDialog loginDialog, RenderContext renderContext) {
      super(renderContext);
      this.loginDialog = loginDialog;
      String upPath = "ui/66";
      String downPath = "ui/67";
      String checkedPath = "ui/68";
      this.buttonSprites = CompositeSprite.obtain(upPath, downPath, checkedPath, false);
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawCompositeSprite(this.buttonSprites, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/150"), this.getX() + 649.0F, this.getY() + 21.0F, 35.0F, 26.0F, Anchor.BOTTOM_LEFT
         );
      super.renderContext
         .drawText(FontManager.DEFAULT_FONT, Settings.getServerName(), this.getX() + 27.0F, this.getY() + 27.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
      super.renderContext.resetBatchColor();
   }

}
