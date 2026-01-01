package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class MailSeparatorRow extends BaseTable {
   public com.tlnt.ui.window.mail.panel.MailMessageRow mailMessageRow;

   public MailSeparatorRow(MailMessageRow mailMessageRow, RenderContext renderContext) {
      super(renderContext);
      this.mailMessageRow = mailMessageRow;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT, "- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -", this.getX(), this.getY(), Anchor.BOTTOM_LEFT, 22.0F, "#8B4E23"
         );
   }

}
