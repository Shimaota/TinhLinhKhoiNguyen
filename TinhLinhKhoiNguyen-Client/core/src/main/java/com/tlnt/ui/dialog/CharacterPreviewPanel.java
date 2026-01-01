package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class CharacterPreviewPanel extends BaseTable {
   public com.tlnt.ui.dialog.CharacterCreateDialog dialog;
   public NinePatchAsset framePatch;

   public CharacterPreviewPanel(CharacterCreateDialog dialog, RenderContext renderContext) {
      super(renderContext);
      this.dialog = dialog;
      String patchPath = "ui/13";
      this.framePatch = NinePatchAsset.obtain(patchPath, 20);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.framePatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      if (this.dialog.selectedClass != null) {
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.dialog.selectedClass.name,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 560.0F,
               Anchor.BOTTOM_CENTER,
               36.0F,
               Color.WHITE
            );
      }

      super.draw(batch, parentAlpha);
   }

}
