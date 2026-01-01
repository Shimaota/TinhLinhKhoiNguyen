package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.achievement.Achievement;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;

public final class AchievementPanel extends GamePanel {
   public AchievementPanel.DetailPanel detailPanel = new AchievementPanel.DetailPanel(this.renderContext);
   public AchievementPanel.ListPanel listPanel;

   public AchievementPanel(RenderContext ctx) {
      super(ctx);
      this.detailPanel.setSize(490.0F, 610.0F);
      this.addActor(this.detailPanel);
      this.listPanel = new AchievementPanel.ListPanel(super.renderContext);
      this.listPanel.setSize(742.0F, 610.0F);
      this.listPanel.setPosition(505.0F, 0.0F);
      this.addActor(this.listPanel);
   }

   public class DetailPanel extends BaseTable {
      public Pool<ItemSlotButton> rewardItemPool = new AchievementItemPool(this);
      public NinePatchAsset background;
      public TextraLabel titleLabel;
      public TextraLabel descriptionLabel;
      public Table rewardTable;
      public Achievement currentAchievement;

      public DetailPanel(RenderContext ctx) {
         super(ctx);
         String bgPath = "ui/100";
         this.background = NinePatchAsset.obtain(bgPath, 50);
         this.pad(10.0F);
         this.titleLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 30));
         this.titleLabel.setAlignment(2);
         this.add(this.titleLabel).height(40.0F).growX().row();
         Table descriptionTable = new Table().padBottom(10.0F);
         this.descriptionLabel = new TextraLabel(
            "", FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 22, 2.5F)
         );
         this.descriptionLabel.setAlignment(10);
         this.descriptionLabel.setWrap(true);
         descriptionTable.add(this.descriptionLabel).grow();
         ScrollPane descriptionScroll = ScrollPaneFactory.createScrollPane(descriptionTable);
         this.add(descriptionScroll).grow().row();
         String rewardTitle = "Phần thưởng";
         TextraLabel rewardLabel;
         (rewardLabel = new TextraLabel(rewardTitle, FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22))).setAlignment(2);
         this.add(rewardLabel).height(30.0F).growX().space(10.0F).row();
         ScrollPane rewardScroll = ScrollPaneFactory.createScrollPane(this.rewardTable = new Table().padLeft(10.0F).padRight(10.0F));
         this.add(rewardScroll).height(110.0F).growX();
      }

      public final void showAchievement(Achievement achievement) {
         this.currentAchievement = achievement;
         this.titleLabel.setText("[#FFF600]" + achievement.title);
         TextraLabel descLabel = this.descriptionLabel;
         StringBuilder sb = new StringBuilder().append(achievement.description);
         sb = sb.append("\nTiến trình: [#FFF600]").append(achievement.progress);
         descLabel.setText(sb.append("/").append(achievement.targetCount).toString());

         for (int i = this.rewardTable.getChildren().size - 1; i >= 0; i--) {
            Actor child;
            if ((child = this.rewardTable.getChild(i)) instanceof ItemSlotButton) {
               this.rewardItemPool.free((ItemSlotButton)child);
            }
         }

         this.rewardTable.clear();
         Item[] rewards = achievement.rewardItems;
         int rewardCount = achievement.rewardItems.length;

         for (int j = 0; j < rewardCount; j++) {
            Item reward = rewards[j];
            this.rewardTable.add(this.rewardItemPool.obtain().setItem(reward)).size(100.0F).space(10.0F);
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }

   public class ListPanel extends GamePanel {

      public final AchievementPanel parentPanel = AchievementPanel.this;
      public Pool<AchievementPanel.ListPanel.AchievementEntryButton> entryButtonPool = new AchievementCategoryButtonPool(this);
      public NinePatchAsset background;
      public Table entryTable;

      public ListPanel(RenderContext ctx) {
         super(ctx);
         String bgPath = "ui/100";
         this.background = NinePatchAsset.obtain(bgPath, 50);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.entryTable = new Table().top())).setSize(687.0F, 536.0F);
         scrollPane.setPosition(27.0F, 25.0F);
         this.addActor(scrollPane);
      }

      @Override
      public final void refresh() {

         for (int i = this.entryTable.getChildren().size - 1; i >= 0; i--) {
            Actor child;
            if ((child = this.entryTable.getChild(i)) instanceof AchievementPanel.ListPanel.AchievementEntryButton) {
               this.entryButtonPool.free((AchievementPanel.ListPanel.AchievementEntryButton)child);
            }
         }

         this.entryTable.clear();

         for (Achievement achievement : GameState.achievementData.achievements) {
            AchievementPanel.ListPanel.AchievementEntryButton entryButton;
            if ((entryButton = this.entryButtonPool.obtain().setAchievement(achievement)).isChecked()) {
               AchievementPanel.this.detailPanel.showAchievement(achievement);
            }

            this.entryTable.add(entryButton).size(687.0F, 104.0F).space(10.0F).row();
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Thành tựu",
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 576.0F,
               Anchor.BOTTOM_CENTER,
               30.0F,
               Color.WHITE
            );
         super.draw(batch, parentAlpha);
      }

      public class AchievementClaimButton extends ShapeButton {
         public CompositeSprite claimedSprite;
         public CompositeSprite lockedSprite;
         public String statusText;
         public byte status;

         public AchievementClaimButton(RenderContext ctx) {
            super(ctx);
            String leftPath = "ui/10";
            this.claimedSprite = CompositeSprite.obtain(
               leftPath, "ui/11", "ui/12", false
            );
            leftPath = "ui/54";
            String middlePath = "ui/55";
            this.lockedSprite = CompositeSprite.obtain(leftPath, middlePath, "ui/56", false);
         }

         public final void setStatus(byte newStatus) {
            this.status = newStatus;
            this.statusText = this.status == 2 ? "Đã nhận" : "Nhận";
         }

         @Override
         public final void draw(Batch batch, float parentAlpha) {
            label18: {
               if (!this.isPressed() && this.status != 0) {
                  if (this.status != 2) {
                     break label18;
                  }
               }

               super.renderContext.setBatchColor(Color.GRAY);
            }

            super.renderContext
               .drawCompositeSprite(
                  this.status == 2 ? this.claimedSprite : this.lockedSprite,
                  this.getX(),
                  this.getY(),
                  this.getWidth(),
                  this.getHeight(),
                  Anchor.BOTTOM_LEFT
               );
            super.renderContext.resetBatchColor();
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  this.statusText,
                  this.getX() + this.getWidth() / 2.05F,
                  this.getY() + this.getHeight() / 1.8F,
                  Anchor.CENTER,
                  24.0F,
                  Color.WHITE
               );
         }
      }

      public class AchievementEntryButton extends ShapeButton {

         public final ListPanel outerListPanel = ListPanel.this;
         public NinePatchAsset background;
         public AchievementPanel.ListPanel.AchievementClaimButton claimButton;
         public Achievement achievement;

         public AchievementEntryButton(RenderContext ctx) {
            super(ctx);
            this.background = NinePatchAsset.obtain("ui/99", 20);
            this.claimButton = ListPanel.this.new AchievementClaimButton(super.renderContext);
            this.claimButton.setSize(133.0F, 50.0F);
            this.claimButton.setPosition(544.0F, 27.0F);
            this.addActor(this.claimButton);
            this.claimButton.addListener(new AchievementClaimListener(this));
            this.addListener(new AchievementItemSelectListener(this, ListPanel.this));
         }

         public final AchievementPanel.ListPanel.AchievementEntryButton setAchievement(Achievement achievement) {
            this.achievement = achievement;
            this.claimButton.setStatus(this.achievement.status);
            return this;
         }

         @Override
         public final void draw(Batch batch, float parentAlpha) {
            if (this.isPressed()) {
               super.renderContext.setBatchColor(Color.GRAY);
            } else if (this.isChecked()) {
               super.renderContext.setBatchColor(Color.LIGHT_GRAY);
            }

            super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
            super.renderContext
               .drawText(FontManager.DEFAULT_FONT, this.achievement.title, this.getX() + 16.0F, this.getY() + 60.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
            super.renderContext
               .drawTextColorName(
                  FontManager.DEFAULT_FONT,
                  this.achievement.progress + "/" + this.achievement.targetCount,
                  this.getX() + 16.0F,
                  this.getY() + 30.0F,
                  Anchor.BOTTOM_LEFT,
                  22.0F,
                  "#FFF600"
               );
            this.drawChildren(batch, parentAlpha);
         }
      }
   }
}
