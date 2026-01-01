package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.item.Item;
import com.tlnt.model.quest.MainQuestStage;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;

public final class MainQuestPanel extends GamePanel {
   public Pool<MainQuestPanel.QuestCategoryButton> questRowPool = new MainQuestRowPool(this);
   public Table questListTable;
   public TextraLabel questTitleLabel;
   public TextraLabel questDescriptionLabel;
   public MainQuestStage selectedQuest;

   public MainQuestPanel(RenderContext ctx) {
      super(ctx);
      MainQuestBackgroundPanel backgroundPanel;
      (backgroundPanel = new MainQuestBackgroundPanel(this, super.renderContext)).setSize(616.0F, 610.0F);
      this.addActor(backgroundPanel);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.questListTable = new Table().top())).setSize(586.0F, 538.0F);
      scrollPane.setPosition(15.0F, 15.0F);
      backgroundPanel.addActor(scrollPane);
      Table detailTable;
      (scrollPane = ScrollPaneFactory.createScrollPane(detailTable = new Table().top().left())).setSize(600.0F, 450.0F);
      scrollPane.setPosition(637.0F, 160.0F);
      this.addActor(scrollPane);
      detailTable.add(this.questTitleLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 30)))
         .height(30.0F)
         .growX()
         .space(10.0F)
         .row();
      this.questTitleLabel.setAlignment(10);
      this.questTitleLabel.setWrap(true);
      detailTable.add(
            this.questDescriptionLabel = new TextraLabel(
               "", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 22, 2.0F)
            )
         )
         .grow();
      this.questDescriptionLabel.setAlignment(10);
      this.questDescriptionLabel.setWrap(true);
      MainQuestPanel.RewardItemsPanel rewardPanel;
      (rewardPanel = new MainQuestPanel.RewardItemsPanel(super.renderContext)).setSize(600.0F, 156.0F);
      rewardPanel.setPosition(636.0F, 0.0F);
      this.addActor(rewardPanel);
   }

   public void selectQuest(MainQuestStage stage) {
      this.selectedQuest = stage;
      if (this.selectedQuest != null) {
         StringBuilder sb;
         StringBuilder descBuilder = (sb = new StringBuilder()).append(GameState.tutorialState.questDescription);
         descBuilder.append("\n\n\n- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n\n")
            .append(stage.stageIndex <= GameState.tutorialState.currentStageIndex ? this.selectedQuest.stageDescription : "");
         this.questDescriptionLabel.setText("[#8B4E23]" + sb.toString());
      }
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator childIter = this.questListTable.getChildren().iterator();

      while (childIter.hasNext()) {
         Actor child;
         if ((child = (Actor)childIter.next()) instanceof MainQuestPanel.QuestCategoryButton) {
            this.questRowPool.free((MainQuestPanel.QuestCategoryButton)child);
         }
      }

      this.questListTable.clear();

      for (MainQuestStage stage : GameState.tutorialState.stages) {
         this.questListTable.add(this.questRowPool.obtain().setQuestChapter(stage)).size(586.0F, 104.0F).space(10.0F).row();
      }

      this.questTitleLabel.setText("[#8B4E23]" + GameState.tutorialState.questTitle);
      if (this.selectedQuest == null || !GameState.tutorialState.stages.contains(this.selectedQuest, true)) {
         this.selectQuest(GameState.tutorialState.stages.get(GameState.tutorialState.currentStageIndex));
      }
   }

   public class QuestCategoryButton extends ShapeButton {

      public final MainQuestPanel parentPanel = MainQuestPanel.this;
      public NinePatchAsset background;
      public MainQuestStage questChapter;

      public QuestCategoryButton(RenderContext ctx) {
         super(ctx);
         this.background = NinePatchAsset.obtain("ui/99", 20);
         this.addListener(new MainQuestRowClickListener(this, MainQuestPanel.this));
      }

      public final MainQuestPanel.QuestCategoryButton setQuestChapter(MainQuestStage stage) {
         this.questChapter = stage;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         } else if (this.questChapter.equals(MainQuestPanel.this.selectedQuest)) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         if (this.questChapter.stageIndex < GameState.tutorialState.currentStageIndex) {
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT, this.questChapter.stageName, this.getX() + 10.0F, this.getY() + 63.0F, Anchor.BOTTOM_LEFT, 28.0F, Color.WHITE
               );
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  "(Đã hoàn thành)",
                  this.getX() + 10.0F,
                  this.getY() + 21.0F,
                  Anchor.BOTTOM_LEFT,
                  24.0F,
                  Color.GREEN
               );
         } else if (this.questChapter.stageIndex > GameState.tutorialState.currentStageIndex) {
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  ".............",
                  this.getX() + 10.0F,
                  this.getY() + 63.0F,
                  Anchor.BOTTOM_LEFT,
                  28.0F,
                  Color.WHITE
               );
         } else {
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT, this.questChapter.stageName, this.getX() + 10.0F, this.getY() + 63.0F, Anchor.BOTTOM_LEFT, 28.0F, Color.WHITE
               );
            super.renderContext
               .drawTextColorName(
                  FontManager.DEFAULT_FONT,
                  "(" + GameState.tutorialState.currentProgress + "/" + this.questChapter.requiredCount + ")",
                  this.getX() + 10.0F,
                  this.getY() + 21.0F,
                  Anchor.BOTTOM_LEFT,
                  24.0F,
                  "#FFF600"
               );
         }

         super.renderContext.resetBatchColor();
      }
   }

   public class RewardItemsPanel extends GamePanel {
      public Pool<ItemSlotButton> itemPool = new MainQuestRewardIconPool(this);
      public Table itemTable;

      public RewardItemsPanel(RenderContext ctx) {
         super(ctx);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.itemTable = new Table())).setSize(600.0F, 104.0F);
         this.addActor(scrollPane);
      }

      @Override
      public final void refresh() {
         if (this.itemTable.getChildren().size != GameState.tutorialState.rewardItems.size) {
            Array.ArrayIterator childIter = this.itemTable.getChildren().iterator();

            while (childIter.hasNext()) {
               Actor child;
               if ((child = (Actor)childIter.next()) instanceof ItemSlotButton) {
                  this.itemPool.free((ItemSlotButton)child);
               }
            }

            this.itemTable.clear();

            for (Item rewardItem : GameState.tutorialState.rewardItems) {
               this.itemTable.add(this.itemPool.obtain()).size(104.0F, 104.0F).space(10.0F);
            }
         }

         for (int i = 0; i < GameState.tutorialState.rewardItems.size; i++) {
            ((ItemSlotButton)this.itemTable.getChild(i)).setItem(GameState.tutorialState.rewardItems.get(i));
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         RenderContext ctx = super.renderContext;
         BitmapFont font = FontManager.PLAIN_FONT;
         String title = "Phần thưởng";
         float textX = this.getX() + this.getWidth() / 2.0F;
         float textY = this.getY() + 122.0F;
         ctx.drawTextColorName(font, title, textX, textY, Anchor.BOTTOM_CENTER, 30.0F, "#8B4E23");
         super.draw(batch, parentAlpha);
      }
   }
}
