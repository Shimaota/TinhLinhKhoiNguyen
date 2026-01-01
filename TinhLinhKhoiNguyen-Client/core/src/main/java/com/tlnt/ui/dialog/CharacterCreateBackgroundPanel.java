package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class CharacterCreateBackgroundPanel extends BaseTable {
   public com.tlnt.ui.dialog.CharacterCreateDialog ownerDialog;
   public NinePatchAsset backgroundPatch;

   public CharacterCreateBackgroundPanel(CharacterCreateDialog ownerDialog, RenderContext renderContext) {
      super(renderContext);
      this.ownerDialog = ownerDialog;
      String patchPath = "ui/13";
      this.backgroundPatch = NinePatchAsset.obtain(patchPath, 20);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
