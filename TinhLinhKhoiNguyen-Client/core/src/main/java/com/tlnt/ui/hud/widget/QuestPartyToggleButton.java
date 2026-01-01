package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class QuestPartyToggleButton extends ShapeButton {

   public QuestPartyToggleButton(RenderContext renderContext, QuestPartyPanel questPartyPanel) {
      super(renderContext);
      this.setSize(50.0F, 52.0F);
      this.setPosition(questPartyPanel.openX, (float)(WorldConfig.VIEWPORT_HEIGHT - 249 - 274 + 222));
      this.addListener(new QuestPartyToggleClickListener(this, questPartyPanel));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.LIGHT_GRAY);
      }

      super.renderContext
         .drawTextureRotatedFlipped(
            TextureAsset.obtain("ui/20"),
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            0.0F,
            ((!this.isChecked() ? 1 : 0) != 0),
            false,
            Anchor.BOTTOM_LEFT
         );
      super.renderContext.resetBatchColor();
   }

}
