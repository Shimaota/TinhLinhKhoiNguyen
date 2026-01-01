package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.window.Dialog;
import com.tlnt.ui.window.WindowType;

public class SlideInDialog extends Dialog {

   public SlideInDialog(RenderContext renderContext, WindowType windowType) {
      super(renderContext, windowType);
      this.setVisible(false);
   }

   @Override
   public final void show() {
      this.clearActions();
      super.show();
      this.addAction(Actions.sequence(Actions.moveTo(0.0F, -this.getHeight()), Actions.moveTo(0.0F, 0.0F, 0.5F, Interpolation.smoother)));
   }

   @Override
   public final void hide() {
      this.clearActions();
      this.addAction(
         Actions.sequence(
            Actions.moveTo(0.0F, 0.0F), Actions.moveTo(0.0F, -this.getHeight(), 0.5F, Interpolation.smoother), Actions.visible(false)
         )
      );
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      if (super.windowType != WindowType.CREATE_PLAYER_PANEL) {
         RenderContext ctx = super.renderContext;
         TextureAsset footerTexture = TextureAsset.obtain("ui/169");
         ctx.drawTexture(footerTexture, (float)(WorldConfig.VIEWPORT_WIDTH / 2), this.getY() + 10.0F, Anchor.BOTTOM_CENTER);
      }

      super.draw(batch, parentAlpha);
   }

}
