package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class MessageNotificationButton extends ShapeButton {

   public MessageNotificationButton(RenderContext renderContext) {
      super(renderContext);
      this.setSize(63.0F, 57.0F);
      this.setPosition(555.0F, 35.0F);
      TargetMarker marker = new TargetMarker(super.renderContext, new ChatActiveChecker(this));
      this.add(marker).size(15.0F).expand().top().right();
      this.addListener(new ChatPopupClickListener(this));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/29"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
      super.renderContext.resetBatchColor();
      super.drawChildren(batch, parentAlpha);
   }

}
