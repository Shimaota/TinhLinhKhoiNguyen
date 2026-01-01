package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.TextButton;

public final class ClaimRewardButton extends TextButton {
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.QuestProgressNode rewardEntry;
   public TextureAsset buttonSprite;
   public TextureAsset receiveTexture;

   public ClaimRewardButton(DailyQuestPanel.QuestProgressNode node, String text, RenderContext ctx) {
      super(text, ctx);
      this.rewardEntry = node;
      this.buttonSprite = TextureAsset.obtain("ui/102");
      this.receiveTexture = TextureAsset.obtain("ui/108");
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed() || this.rewardEntry.questData.claimed || GameState.dailyQuestData.currentDiligencePoints < this.rewardEntry.questData.requiredPoints
         )
       {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext
         .drawTextureSized(
            this.rewardEntry.questData.claimed ? this.buttonSprite : this.receiveTexture,
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            Anchor.BOTTOM_LEFT
         );
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            this.rewardEntry.questData.claimed ? "Đã nhận" : "Nhận",
            this.getX() + this.getWidth() / 2.1F,
            this.getY() + this.getHeight() / 1.8F,
            Anchor.CENTER,
            22.0F,
            Color.WHITE
         );
      super.renderContext.resetBatchColor();
   }

}
