package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class ComposeInputBackground extends BaseTable {
   public com.tlnt.ui.window.mail.panel.MailComposePanel composePanel;
   public NinePatchAsset backgroundPatch;

   public ComposeInputBackground(MailComposePanel composePanel, RenderContext renderContext) {
      super(renderContext);
      this.composePanel = composePanel;
      this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      this.composePanel.bodyField.equals(this.getStage().getKeyboardFocus());
      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
