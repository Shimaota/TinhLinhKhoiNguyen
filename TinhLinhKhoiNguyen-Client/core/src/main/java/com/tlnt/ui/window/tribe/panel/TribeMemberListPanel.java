package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class TribeMemberListPanel extends BaseTable {
   public com.tlnt.ui.window.tribe.panel.TribeMemberRow ownerRow;
   public NinePatchAsset backgroundPatch;

   public TribeMemberListPanel(TribeMemberRow ownerRow, RenderContext renderContext) {
      super(renderContext);
      this.ownerRow = ownerRow;
      this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Thành viên Bộ lạc",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + 576.0F,
            Anchor.BOTTOM_CENTER,
            30.0F,
            Color.WHITE
         );
      if (GameState.tribeData.myTribe != null) {
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Thành viên: " + GameState.tribeData.myTribe.members.size + "/" + GameState.tribeData.myTribe.maxMembers,
               this.getX() + 25.0F,
               this.getY() + 17.0F,
               Anchor.BOTTOM_LEFT,
               22.0F,
               Color.WHITE
            );
      }

      super.draw(batch, parentAlpha);
   }

}
