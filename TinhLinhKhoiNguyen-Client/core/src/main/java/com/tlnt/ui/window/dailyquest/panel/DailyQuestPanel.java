package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.Item;
import com.tlnt.model.quest.daily.DailyMilestone;
import com.tlnt.model.quest.daily.DailyQuestTask;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.util.TimeFormatter;

public final class DailyQuestPanel extends GamePanel {
   public ButtonGroup<ItemSlotButton> rewardIconButtonGroup = new ButtonGroup<>();
   public DailyQuestPanel.DiligenceProgressPanel diligenceProgressPanel;
   public DailyQuestPanel.DailyQuestListPanel dailyQuestListPanel;

   public DailyQuestPanel(RenderContext ctx) {
      super(ctx);
      this.add(this.diligenceProgressPanel = new DailyQuestPanel.DiligenceProgressPanel(super.renderContext)).size(490.0F, 610.0F).space(15.0F);
      this.add(this.dailyQuestListPanel = new DailyQuestPanel.DailyQuestListPanel(super.renderContext)).size(742.0F, 610.0F).space(15.0F);
   }

   @Override
   public final void onShow() {
      ServerPacketSender.getInstance().sendRequestMount();
   }

   public class DailyQuestListPanel extends GamePanel {

      public final DailyQuestPanel dailyQuestPanel = DailyQuestPanel.this;
      public Pool<DailyQuestPanel.QuestCategoryTab> categoryTabPool = new DailyQuestCategoryTabPool(this);
      public Pool<ItemSlotButton> rewardIconPool = new DailyQuestRewardItemPool(this);
      public NinePatchAsset panelBackground;
      public Table questCategoryTable;
      public Table questRewardsTable;
      public AcceptQuestButton acceptQuestButton;

      public DailyQuestListPanel(RenderContext ctx) {
         super(ctx);
         this.panelBackground = NinePatchAsset.obtain("ui/100", 50);
         ScrollPane categoryScroll;
         (categoryScroll = ScrollPaneFactory.createScrollPane(this.questCategoryTable = new Table())).setSize(687.0F, 377.0F);
         categoryScroll.setPosition(27.0F, 171.0F);
         this.addActor(categoryScroll);
         ScrollPane rewardsScroll;
         (rewardsScroll = ScrollPaneFactory.createScrollPane(this.questRewardsTable = new Table())).setSize(687.0F, 112.0F);
         rewardsScroll.setPosition(27.0F, 15.0F);
         this.addActor(rewardsScroll);
         this.acceptQuestButton = new AcceptQuestButton(this, super.renderContext);
         this.acceptQuestButton.addListener(new AcceptDailyQuestClickListener(this));
      }

      @Override
      public final void refresh() {
         for (int i = this.questCategoryTable.getChildren().size - 1; i >= 0; i--) {
            Actor child;
            if ((child = this.questCategoryTable.getChild(i)) instanceof DailyQuestPanel.QuestCategoryTab) {
               this.categoryTabPool.free((DailyQuestPanel.QuestCategoryTab)child);
            }
         }

         this.questCategoryTable.clear();

         for (DailyQuestTask task : GameState.dailyQuestData.questTasks) {
            DailyQuestPanel.QuestCategoryTab tab = this.categoryTabPool.obtain().bindQuestCategory(task);
            this.questCategoryTable.add(tab).height(104.0F).growX().space(10.0F).row();
            if (tab.isChecked()) {
               this.showQuestRewards(task);
            }
         }

         this.questCategoryTable.add(this.acceptQuestButton).size(169.0F, 63.0F);
      }

      public void showQuestRewards(DailyQuestTask task) {

         for (int i = this.questRewardsTable.getChildren().size - 1; i >= 0; i--) {
            Actor child;
            if ((child = this.questRewardsTable.getChild(i)) instanceof ItemSlotButton) {
               this.rewardIconPool.free((ItemSlotButton)child);
            }
         }

         this.questRewardsTable.clear();
         if (task != null) {
            for (Item reward : task.rewards) {
               this.questRewardsTable.add(this.rewardIconPool.obtain().setItem(reward)).size(104.0F).space(10.0F);
            }
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.panelBackground, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Nhiệm vụ hàng ngày",
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 576.0F,
               Anchor.BOTTOM_CENTER,
               30.0F,
               Color.WHITE
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Nhận ngẫu nhiên 1 phần thưởng",
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 136.0F,
               Anchor.BOTTOM_CENTER,
               22.0F,
               Color.WHITE
            );
         super.draw(batch, parentAlpha);
      }
   }

   public class DiligenceProgressPanel extends GamePanel {
      public final DailyQuestPanel dailyQuestPanel = DailyQuestPanel.this;
      public Pool<DailyQuestPanel.QuestProgressNode> progressNodePool = new DailyQuestItemRowPool(this);
      public NinePatchAsset panelBackground;
      public Table progressNodeTable;

      public DiligenceProgressPanel(RenderContext ctx) {
         super(ctx);
         this.panelBackground = NinePatchAsset.obtain("ui/100", 50);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.progressNodeTable = new DailyQuestProgressBar(this, super.renderContext).top().pad(4.0F)))
            .setSize(490.0F, 461.0F);
         scrollPane.setPosition(0.0F, 55.0F);
         this.addActor(scrollPane);
      }

      @Override
      public final void refresh() {
         Array.ArrayIterator childIter = this.progressNodeTable.getChildren().iterator();

         while (childIter.hasNext()) {
            Actor child;
            if ((child = (Actor)childIter.next()) instanceof DailyQuestPanel.DiligenceProgressPanel) {
               this.progressNodePool.free((DailyQuestPanel.QuestProgressNode)child);
            }
         }

         this.progressNodeTable.clear();

         for (DailyMilestone milestone : GameState.dailyQuestData.milestones) {
            this.progressNodeTable.add(this.progressNodePool.obtain().bindQuestData(milestone)).height(104.0F).growX().space(15.0F).padLeft(50.0F).row();
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.panelBackground, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTextColorName(
               FontManager.DEFAULT_FONT,
               "Chăm chỉ",
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 576.0F,
               Anchor.BOTTOM_CENTER,
               30.0F,
               "#FFF600"
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Điểm chăm chỉ hiện tại: " + GameState.dailyQuestData.currentDiligencePoints,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 541.0F,
               Anchor.BOTTOM_CENTER,
               22.0F,
               Color.WHITE
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Thời gian: " + TimeFormatter.formatTimeRemaining(GameState.dailyQuestData.remainingTime),
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 20.0F,
               Anchor.BOTTOM_CENTER,
               22.0F,
               Color.WHITE
            );
         super.draw(batch, parentAlpha);
      }
   }

   public class QuestCategoryTab extends ShapeButton {

      public final DailyQuestPanel dailyQuestPanel = DailyQuestPanel.this;
      public NinePatchAsset categoryBackgroundPatch;
      public DailyQuestClaimButton claimButton;
      public DailyQuestTask questCategoryData;

      public QuestCategoryTab(RenderContext ctx) {
         super(ctx);
         this.categoryBackgroundPatch = NinePatchAsset.obtain("ui/99", 20);
         this.claimButton = new DailyQuestClaimButton(this, "", super.renderContext);
         this.claimButton.setSize(133.0F, 50.0F);
         this.claimButton.setPosition(544.0F, 27.0F);
         this.addActor(this.claimButton);
         this.claimButton.addListener(new DailyQuestAcceptListener(this));
         this.addListener(new DailyQuestSelectListener(this, DailyQuestPanel.this));
      }

      public final DailyQuestPanel.QuestCategoryTab bindQuestCategory(DailyQuestTask task) {
         this.questCategoryData = task;
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isChecked()) {
            super.renderContext.setBatchColor(Color.LIGHT_GRAY);
         }

         super.renderContext.drawNinePatch(this.categoryBackgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT, this.questCategoryData.questName, this.getX() + 16.0F, this.getY() + 60.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE
            );
         RenderContext ctx = super.renderContext;
         BitmapFont font = FontManager.DEFAULT_FONT;
         String progressText = this.questCategoryData.currentProgress + "/" + this.questCategoryData.targetProgress;
         float textX = this.getX() + 16.0F;
         float textY = this.getY() + 29.0F;
         ctx.drawTextColorName(font, progressText, textX, textY, Anchor.BOTTOM_LEFT, 22.0F, "#FFF600");
         super.renderContext.resetBatchColor();
         super.draw(batch, parentAlpha);
      }

      @Override
      public final void drawBackground() {
      }
   }

   public class QuestProgressNode extends BaseTable {
      public TextureAsset incompleteNodeTexture;
      public TextureAsset completeNodeTexture;
      public ItemSlotButton rewardIconActor;
      public ClaimRewardButton claimButton;
      public DailyMilestone questData;

      public QuestProgressNode(RenderContext ctx) {
         super(ctx);
         this.incompleteNodeTexture = TextureAsset.obtain("ui/62");
         this.completeNodeTexture = TextureAsset.obtain("ui/213");
         this.rewardIconActor = new ItemSlotButton(super.renderContext);
         DailyQuestPanel.this.rewardIconButtonGroup.add(this.rewardIconActor);
         this.rewardIconActor.setSize(104.0F, 104.0F);
         this.addActor(this.rewardIconActor);
         this.rewardIconActor.addListener(new DailyQuestRowItemClickListener(this));
         this.claimButton = new ClaimRewardButton(this, "", super.renderContext);
         this.claimButton.setSize(133.0F, 50.0F);
         this.claimButton.setPosition(264.0F, 27.0F);
         this.addActor(this.claimButton);
         this.claimButton.addListener(new ClaimDailyQuestRewardClickListener(this));
      }

      public final DailyQuestPanel.QuestProgressNode bindQuestData(DailyMilestone milestone) {
         this.questData = milestone;
         if (this.questData != null) {
            int hasReward;
            if (this.questData.rewardItem != null) {
               hasReward = 1;
            } else {
               hasReward = 0;
            }

            this.rewardIconActor.setVisible((hasReward != 0));
            this.claimButton.setVisible(((this.questData.rewardItem != null ? 1 : 0) != 0));
            this.rewardIconActor.setItem(this.questData.rewardItem);
         }

         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext
            .drawTextureSized(
               GameState.dailyQuestData.currentDiligencePoints >= this.questData.requiredPoints ? this.completeNodeTexture : this.incompleteNodeTexture,
               this.getX() + 164.0F,
               this.getY() + 32.0F,
               40.0F,
               40.0F,
               Anchor.BOTTOM_LEFT
            );
         if (this.claimButton.isVisible()) {
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  this.questData.requiredPoints + "",
                  this.getX() + 211.0F,
                  this.getY() + 45.0F,
                  Anchor.BOTTOM_LEFT,
                  22.0F,
                  Color.WHITE
               );
         }

         super.draw(batch, parentAlpha);
      }
   }
}
