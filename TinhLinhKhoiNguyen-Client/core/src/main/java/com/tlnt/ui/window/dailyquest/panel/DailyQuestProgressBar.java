package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.GameState;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.quest.daily.DailyMilestone;
import com.tlnt.ui.widget.BaseTable;

public final class DailyQuestProgressBar extends BaseTable {
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.DiligenceProgressPanel diligencePanel;
   public NinePatchAsset progressTrackPatch;
   public TextureAsset progressFillTexture;

   public DailyQuestProgressBar(DailyQuestPanel.DiligenceProgressPanel progressPanel, RenderContext ctx) {
      super(ctx);
      this.diligencePanel = progressPanel;
      String trackPath = "ui/214";
      this.progressTrackPatch = NinePatchAsset.obtain(trackPath, 10);
      this.progressFillTexture = TextureAsset.obtain("ui/212");
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      int milestoneCount = GameState.dailyQuestData.milestones.size;
      if (GameState.dailyQuestData.milestones.size > 0) {
         float trackX = this.getX() + 225.0F;
         float trackY = this.getY() + 52.0F;
         float trackHeight = (float)(milestoneCount - 1) * 120.0F;
         super.renderContext.drawNinePatch(this.progressTrackPatch, trackX, trackY, 24.0F, trackHeight, Anchor.BOTTOM_LEFT);
         DailyMilestone firstMilestone = GameState.dailyQuestData.milestones.first();
         DailyMilestone lastMilestone = GameState.dailyQuestData.milestones.peek();
         if (firstMilestone != null && lastMilestone != null) {
            float minPoints = (float)firstMilestone.requiredPoints;
            float maxPoints = (float)lastMilestone.requiredPoints;
            float currentPoints = (float)GameState.dailyQuestData.currentDiligencePoints;
            float progress = 0.0F;
            if (maxPoints > minPoints) {
               progress = Math.min(1.0F, Math.max(0.0F, (currentPoints - minPoints) / (maxPoints - minPoints)));
            }

            float fillHeight = trackHeight * progress;
            float fillY = trackY + (trackHeight - fillHeight);
            super.renderContext.drawTextureSized(this.progressFillTexture, trackX + 2.0F, fillY, 20.0F, fillHeight, Anchor.BOTTOM_LEFT);
         }
      }

      super.draw(batch, parentAlpha);
   }

}
