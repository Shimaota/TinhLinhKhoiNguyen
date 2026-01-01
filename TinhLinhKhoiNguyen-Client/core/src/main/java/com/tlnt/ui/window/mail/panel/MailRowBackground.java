package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class MailRowBackground extends BaseTable {
   public com.tlnt.ui.window.mail.panel.MailMessageRow mailMessageRow;
   public CompositeSprite backgroundSprite;

   public MailRowBackground(MailMessageRow mailMessageRow, RenderContext renderContext) {
      super(renderContext);
      this.mailMessageRow = mailMessageRow;
      this.backgroundSprite = CompositeSprite.obtain(
         "ui/104",
         "ui/105",
         "ui/106",
         false
      );
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawCompositeSprite(this.backgroundSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
