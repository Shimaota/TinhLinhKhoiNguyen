package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class CharacterCreateBackButton extends ShapeButton {
   public com.tlnt.ui.dialog.CharacterCreateDialog dialog;
   public TextureAsset buttonTexture;

   public CharacterCreateBackButton(CharacterCreateDialog dialog, RenderContext renderContext) {
      super(renderContext);
      this.dialog = dialog;
      this.buttonTexture = TextureAsset.obtain("ui/162");
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawTextureSized(this.buttonTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.resetBatchColor();
   }

}
