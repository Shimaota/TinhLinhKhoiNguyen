package com.tlnt.ui.dialog.widget;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class InputVisibilityToggle extends ShapeButton {
   public com.tlnt.ui.dialog.widget.TextInputRow ownerInputRow;

   public InputVisibilityToggle(TextInputRow ownerInputRow, RenderContext renderContext) {
      super(renderContext);
      this.ownerInputRow = ownerInputRow;
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext
         .drawTextureSized(
            this.isChecked() ? TextureAsset.obtain("ui/70") : TextureAsset.obtain("ui/71"),
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            Anchor.BOTTOM_LEFT
         );
      super.renderContext.resetBatchColor();
   }

}
