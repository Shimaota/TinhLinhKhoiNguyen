package com.tlnt.ui.window.giftcode.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class GiftcodeListPanel extends BaseTable {
   public com.tlnt.ui.window.giftcode.panel.GiftcodeRedeemPanel panel;

   public GiftcodeListPanel(GiftcodeRedeemPanel panel, RenderContext ctx) {
      super(ctx);
      this.panel = panel;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      String bgPath = "ui/100";
      super.renderContext
         .drawNinePatch(
            NinePatchAsset.obtain(bgPath, 50), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
      super.draw(batch, parentAlpha);
   }

}
