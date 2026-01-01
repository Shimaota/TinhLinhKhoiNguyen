package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;

public final class CharacterNameInputPanel extends BaseTable {
   public com.tlnt.ui.dialog.CharacterCreateDialog dialog;
   public TextureAsset backgroundTexture;

   public CharacterNameInputPanel(CharacterCreateDialog dialog, RenderContext renderContext) {
      super(renderContext);
      this.dialog = dialog;
      this.backgroundTexture = TextureAsset.obtain("ui/89");
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
