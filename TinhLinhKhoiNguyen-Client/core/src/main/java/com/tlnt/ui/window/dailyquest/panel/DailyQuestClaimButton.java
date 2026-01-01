package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.TextButton;

public final class DailyQuestClaimButton extends TextButton {
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.QuestCategoryTab questItem;
   public TextureAsset buttonSprite;
   public TextureAsset availableTexture;

   public DailyQuestClaimButton(DailyQuestPanel.QuestCategoryTab tab, String text, RenderContext ctx) {
      super(text, ctx);
      this.questItem = tab;
      this.buttonSprite = TextureAsset.obtain("ui/102");
      this.availableTexture = TextureAsset.obtain("ui/108");
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed() || this.questItem.questCategoryData.rewardClaimed) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext
         .drawTextureSized(
            this.questItem.questCategoryData.rewardClaimed
               ? this.buttonSprite
               : (
                  this.questItem.questCategoryData.currentProgress < this.questItem.questCategoryData.targetProgress
                     ? this.buttonSprite
                     : this.availableTexture
               ),
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            Anchor.BOTTOM_LEFT
         );
      String label;
      if (this.questItem.questCategoryData.rewardClaimed) {
         label = "Đã nhận";
      } else if (this.questItem.questCategoryData.currentProgress < this.questItem.questCategoryData.targetProgress) {
         label = "Đổi NV";
      } else {
         label = "Nhận";
      }

      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT, label, this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 1.8F, Anchor.CENTER, 22.0F, Color.WHITE
         );
      super.renderContext.resetBatchColor();
   }

}
