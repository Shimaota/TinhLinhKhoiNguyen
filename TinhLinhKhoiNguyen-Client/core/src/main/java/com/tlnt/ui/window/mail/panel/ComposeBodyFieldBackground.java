package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class ComposeBodyFieldBackground extends BaseTable {
   public com.tlnt.ui.window.mail.panel.MailComposePanel composePanel;
   public CompositeSprite activeBgSprite;
   public CompositeSprite inactiveBgSprite;

   public ComposeBodyFieldBackground(MailComposePanel composePanel, RenderContext renderContext) {
      super(renderContext);
      this.composePanel = composePanel;
      String startPath = "ui/131";
      String middlePath = "ui/132";
      this.activeBgSprite = CompositeSprite.obtain(
         startPath, middlePath, "ui/133", false
      );
      startPath = "ui/131";
      middlePath = "ui/132";
      String endPath = "ui/133";
      this.inactiveBgSprite = CompositeSprite.obtain(startPath, middlePath, endPath, false);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.composePanel.subjectField.equals(this.getStage().getKeyboardFocus())) {
         super.renderContext.drawCompositeSprite(this.activeBgSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      } else {
         super.renderContext.drawCompositeSprite(this.inactiveBgSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      }

      super.draw(batch, parentAlpha);
   }

}
