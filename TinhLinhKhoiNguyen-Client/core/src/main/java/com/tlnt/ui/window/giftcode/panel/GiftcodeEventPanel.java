package com.tlnt.ui.window.giftcode.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.giftcode.GiftcodeEvent;
import com.tlnt.model.giftcode.GiftcodeEventMilestone;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.GamePanel;

public final class GiftcodeEventPanel extends GamePanel {
   public Pool<GiftcodeEventPanel.EventRowPanel> eventRowPanelPool = new GiftcodeEventPanel$EventRowPanelPool(this);
   public Table eventsTable;
   public GiftcodeEvent eventListData;

   public GiftcodeEventPanel(RenderContext ctx) {
      super(ctx);
      ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.eventsTable = new Table().top());
      this.add(scrollPane).grow();
   }

   public final void setEventList(GiftcodeEvent event) {
      this.eventListData = event;
   }

   public final GiftcodeEvent getEventList() {
      return this.eventListData;
   }

   @Override
   public final void refresh() {
      for (int i = this.eventsTable.getChildren().size - 1; i >= 0; i--) {
         Actor child;
         if ((child = this.eventsTable.getChild(i)) instanceof GiftcodeEventPanel.EventRowPanel) {
            this.eventRowPanelPool.free((GiftcodeEventPanel.EventRowPanel)child);
         }
      }

      this.eventsTable.clear();
      if (this.eventListData != null) {
         GiftcodeEventMilestone[] milestones = this.eventListData.milestones;
         int milestoneCount = this.eventListData.milestones.length;

         for (int j = 0; j < milestoneCount; j++) {
            GiftcodeEventMilestone milestone = milestones[j];
            this.eventsTable.add(this.eventRowPanelPool.obtain().setEvent(milestone)).height(154.0F).growX().space(10.0F).row();
         }
      }
   }

   public class EventClaimButton extends TextButton {
      public CompositeSprite buttonSprite;
      public CompositeSprite normalSprite;
      public byte claimState;

      public EventClaimButton(String text, RenderContext ctx) {
         super(text, ctx);
         String leftPath = "ui/10";
         String middlePath = "ui/11";
         String rightPath = "ui/12";
         this.buttonSprite = CompositeSprite.obtain(leftPath, middlePath, rightPath, false);
         leftPath = "ui/54";
         this.normalSprite = CompositeSprite.obtain(
            leftPath, "ui/55", "ui/56", false
         );
      }

      public final void setClaimState(byte state) {
         this.claimState = state;
         String label;
         if (this.claimState == 2) {
            label = "Đã nhận";
         } else {
            label = "Nhận";
         }

         this.setText(label);
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed() || this.claimState == 0 || this.claimState == 2) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext
            .drawCompositeSprite(
               this.claimState == 2 ? this.buttonSprite : this.normalSprite,
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
               super.text,
               this.getX() + this.getWidth() / 2.05F,
               this.getY() + this.getHeight() / 1.8F,
               Anchor.CENTER,
               24.0F,
               Color.WHITE
            );
      }

      @Override
      public final void drawLabel() {
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               super.text,
               this.getX() + this.getWidth() / 2.05F,
               this.getY() + this.getHeight() / 1.8F,
               Anchor.CENTER,
               24.0F,
               Color.WHITE
            );
      }
   }

   public class EventRowPanel extends GamePanel {
      public Pool<ItemSlotButton> rewardItemPool = new GiftcodeEventPanel$RewardItemPool(this);
      public TextraLabel eventNameLabel = new TextraLabel("", FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22));
      public Table rewardItemsTable;
      public GiftcodeEventPanel.EventClaimButton claimButton;
      public GiftcodeEventMilestone eventData;

      public EventRowPanel(RenderContext ctx) {
         super(ctx);
         this.eventNameLabel.setAlignment(10);
         this.eventNameLabel.setPosition(15.0F, 130.0F);
         this.eventNameLabel.setHeight(24.0F);
         this.addActor(this.eventNameLabel);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.rewardItemsTable = new Table().pad(6.0F).left())).setSize(968.0F, 116.0F);
         scrollPane.setPosition(15.0F, 5.0F);
         this.addActor(scrollPane);
         this.claimButton = GiftcodeEventPanel.this.new EventClaimButton("", super.renderContext);
         this.claimButton.setSize(169.0F, 63.0F);
         this.claimButton.setPosition(1063.0F, 46.0F);
         this.addActor(this.claimButton);
         this.claimButton.addListener(new GiftcodeEventPanel$ClaimButtonListener(this));
      }

      public final GiftcodeEventPanel.EventRowPanel setEvent(GiftcodeEventMilestone milestone) {
         this.eventData = milestone;
         this.eventNameLabel.setText(this.eventData.description);

         for (int i = this.rewardItemsTable.getChildren().size - 1; i >= 0; i--) {
            Actor child;
            if ((child = this.rewardItemsTable.getChild(i)) instanceof ItemSlotButton) {
               this.rewardItemPool.free((ItemSlotButton)child);
            }
         }

         this.rewardItemsTable.clear();
         Item[] rewards = this.eventData.rewardItems;
         int rewardCount = this.eventData.rewardItems.length;

         for (int j = 0; j < rewardCount; j++) {
            Item reward = rewards[j];
            this.rewardItemsTable.add(this.rewardItemPool.obtain().setItem(reward)).size(104.0F).space(10.0F);
         }

         this.claimButton.setClaimState(this.eventData.claimStatus);
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext
            .drawNinePatch(
               NinePatchAsset.obtain("ui/100", 50),
               this.getX(),
               this.getY(),
               this.getWidth(),
               this.getHeight(),
               Anchor.BOTTOM_LEFT
            );
         super.draw(batch, parentAlpha);
      }
   }
}
