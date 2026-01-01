package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class MailDetailBackgroundTable extends BaseTable {
   public com.tlnt.ui.window.mail.panel.MailMessageRow mailMessageRow;
   public NinePatchAsset backgroundPatch;

   public MailDetailBackgroundTable(MailMessageRow mailMessageRow, RenderContext renderContext) {
      super(renderContext);
      this.mailMessageRow = mailMessageRow;
      this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);
   }

}
