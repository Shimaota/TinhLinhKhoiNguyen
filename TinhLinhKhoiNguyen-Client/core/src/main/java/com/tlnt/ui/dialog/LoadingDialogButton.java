package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.TextButton;

public final class LoadingDialogButton extends TextButton {
   public com.tlnt.ui.dialog.LoadingDialog loadingDialog;
   public CompositeSprite buttonSprite;
   public static byte[] AES_KEY_BYTES;

   public LoadingDialogButton(LoadingDialog loadingDialog, String text, RenderContext renderContext) {
      super(text, renderContext);
      this.loadingDialog = loadingDialog;
      String upPath = "ui/57";
      String downPath = "ui/58";
      String checkedPath = "ui/59";
      this.buttonSprite = CompositeSprite.obtain(upPath, downPath, checkedPath, false);
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawCompositeSprite(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
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
            24.0F,
            Color.WHITE
         );
   }

   public static void initAesKey() {
      AES_KEY_BYTES = new byte[16];
      AES_KEY_BYTES[12] = (byte)57;
      AES_KEY_BYTES[4] = (byte)109;
      AES_KEY_BYTES[13] = (byte)-24;
      AES_KEY_BYTES[9] = (byte)87;
      AES_KEY_BYTES[11] = (byte)122;
      AES_KEY_BYTES[14] = (byte)(~18);
      AES_KEY_BYTES[3] = (byte)-18;
      AES_KEY_BYTES[2] = (byte)-69;
      AES_KEY_BYTES[5] = (byte)-5;
      AES_KEY_BYTES[8] = (byte)-115;
      AES_KEY_BYTES[1] = (byte)4;
      AES_KEY_BYTES[6] = (byte)114;
      AES_KEY_BYTES[7] = (byte)-50;
      AES_KEY_BYTES[15] = (byte)-105;
      AES_KEY_BYTES[10] = (byte)99;
      AES_KEY_BYTES[0] = (byte)-30;
   }

   static {
      initAesKey();
   }
}
