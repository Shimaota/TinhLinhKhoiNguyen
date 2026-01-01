package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class QuestPartyContentPanel extends BaseTable {
   public com.tlnt.ui.hud.widget.QuestPartyPanel questPartyPanel;
   public NinePatchAsset background;

   public QuestPartyContentPanel(QuestPartyPanel questPartyPanel, RenderContext renderContext) {
      super(renderContext);
      this.questPartyPanel = questPartyPanel;
      String patchPath = "ui/13";
      this.background = NinePatchAsset.obtain(patchPath, 20);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
