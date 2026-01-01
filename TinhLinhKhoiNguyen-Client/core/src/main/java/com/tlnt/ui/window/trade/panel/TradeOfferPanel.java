package com.tlnt.ui.window.trade.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.world.entity.player.PlayerEntity;

public final class TradeOfferPanel extends GamePanel {
   public TextureAsset lockIconTexture;
   public TradeOfferPanel.AvatarPanel myAvatarPanel;
   public TradeOfferPanel.CoinButton myCoinPanel;
   public TradeOfferPanel.ItemGridPanel myItemsPanel;
   public TradeOfferPanel.AvatarPanel partnerAvatarPanel;
   public TradeOfferPanel.CoinButton partnerCoinPanel;
   public TradeOfferPanel.ItemGridPanel partnerItemsPanel;
   public LockTradeOfferButton lockButton;
   public TradeAcceptButton acceptButton;

   public TradeOfferPanel(RenderContext renderContext) {
      super(renderContext);
      this.lockIconTexture = TextureAsset.obtain("ui/143");
      this.myAvatarPanel = new TradeOfferPanel.AvatarPanel(super.renderContext);
      this.myAvatarPanel.setPosition(0.0F, 445.0F);
      this.addActor(this.myAvatarPanel);
      this.myCoinPanel = new TradeOfferPanel.CoinButton(super.renderContext);
      this.myCoinPanel.setPosition(202.0F, 448.0F);
      this.addActor(this.myCoinPanel);
      this.myCoinPanel.addListener(new TradeEnterCoinsListener(this));
      this.myItemsPanel = new TradeOfferPanel.ItemGridPanel(super.renderContext, true);
      this.addActor(this.myItemsPanel);
      this.partnerAvatarPanel = new TradeOfferPanel.AvatarPanel(super.renderContext);
      this.partnerAvatarPanel.setPosition(1111.0F, 445.0F);
      this.addActor(this.partnerAvatarPanel);
      this.partnerCoinPanel = new TradeOfferPanel.CoinButton(super.renderContext);
      this.partnerCoinPanel.setPosition(751.0F, 448.0F);
      this.addActor(this.partnerCoinPanel);
      this.partnerItemsPanel = new TradeOfferPanel.ItemGridPanel(super.renderContext, false);
      this.partnerItemsPanel.setPosition(751.0F, 0.0F);
      this.addActor(this.partnerItemsPanel);
      this.lockButton = new LockTradeOfferButton(this, "Khóa", super.renderContext);
      this.lockButton.setSize(169.0F, 63.0F);
      this.lockButton.setPosition(540.0F, 0.0F);
      this.addActor(this.lockButton);
      this.lockButton.addListener(new TradeLockOfferListener(this));
      this.acceptButton = new TradeAcceptButton(this, "Đồng ý", super.renderContext);
      this.acceptButton.setSize(169.0F, 63.0F);
      this.acceptButton.setPosition(540.0F, 0.0F);
      this.addActor(this.acceptButton);
      this.acceptButton.addListener(new TradeConfirmAcceptListener(this));
   }

   @Override
   public final void refresh() {
      this.myAvatarPanel.setAvatarAndLevel(TextureAsset.obtain(PlayerEntity.getInstance().POOL2.avatarTexture), PlayerEntity.getInstance().POOL2.level);
      this.myCoinPanel.setCoins(GameState.tradeSession.myOfferCoins);
      this.myItemsPanel.setItems(GameState.tradeSession.myOfferItems);
      this.partnerAvatarPanel.setAvatarAndLevel(TextureAsset.obtain(GameState.tradeSession.partnerClass.avatarIconPath), GameState.tradeSession.partnerLevel);
      this.partnerCoinPanel.setCoins(GameState.tradeSession.partnerOfferCoins);
      this.partnerItemsPanel.setItems(GameState.tradeSession.partnerOfferItems);
      int lockVisible;
      if (!GameState.tradeSession.myOfferLocked) {
         lockVisible = 1;
      } else {
         lockVisible = 0;
      }

      this.lockButton.setVisible((lockVisible != 0));
      this.acceptButton.setVisible(GameState.tradeSession.myOfferLocked);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String name = PlayerEntity.getInstance().getCharacterName();
      float textX = this.getX() + 202.0F;
      float textY = this.getY() + 555.0F;
      ctx.drawTextColorName(font, name, textX, textY, Anchor.BOTTOM_LEFT, 24.0F, "#8B4E23");
      ctx = super.renderContext;
      font = FontManager.PLAIN_FONT;
      name = GameState.tradeSession.partnerName;
      textX = this.getX() + 1046.0F;
      textY = this.getY() + 555.0F;
      ctx.drawTextColorName(font, name, textX, textY, Anchor.BOTTOM_RIGHT, 24.0F, "#8B4E23");
      if (GameState.tradeSession.myOfferLocked) {
         if (GameState.tradeSession.myAccepted) {
            super.renderContext.setBatchColor(Color.GREEN);
         }

         super.renderContext.drawTextureSized(this.lockIconTexture, this.getX() + 459.0F, this.getY() + 542.0F, 40.0F, 56.0F, Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }

      if (GameState.tradeSession.partnerOfferLocked) {
         if (GameState.tradeSession.partnerAccepted) {
            super.renderContext.setBatchColor(Color.GREEN);
         }

         super.renderContext.drawTextureSized(this.lockIconTexture, this.getX() + 749.0F, this.getY() + 542.0F, 40.0F, 56.0F, Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }

      super.draw(batch, parentAlpha);
   }

   public class AvatarPanel extends BaseTable {
      public TextureAsset frameTexture;
      public TextureAsset avatarBackingTexture;
      public TextureAsset avatarInnerFrameTexture;
      public TextureAsset namePlateTexture;
      public TextureAsset avatarTexture;
      public int level;

      public AvatarPanel(RenderContext renderContext) {
         super(renderContext);
         this.frameTexture = TextureAsset.obtain("ui/144");
         this.avatarBackingTexture = TextureAsset.obtain("ui/2");
         this.avatarInnerFrameTexture = TextureAsset.obtain("ui/1");
         this.namePlateTexture = TextureAsset.obtain("ui/4");
         this.setSize(136.0F, 165.0F);
      }

      public final void setAvatarAndLevel(TextureAsset avatar, int level) {
         this.avatarTexture = avatar;
         this.level = level;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawTextureSized(this.frameTexture, this.getX(), this.getY() + 30.0F, 136.0F, 135.0F, Anchor.BOTTOM_LEFT);
         super.renderContext.drawTextureSized(this.avatarBackingTexture, this.getX() + 12.0F, this.getY() + 39.0F, 115.0F, 115.0F, Anchor.BOTTOM_LEFT);
         super.renderContext.drawTextureSized(this.avatarInnerFrameTexture, this.getX() + 16.0F, this.getY() + 43.0F, 107.0F, 107.0F, Anchor.BOTTOM_LEFT);
         super.renderContext.drawTextureSized(this.avatarTexture, this.getX() + 16.0F, this.getY() + 43.0F, 107.0F, 107.0F, Anchor.BOTTOM_LEFT);
         super.renderContext.drawTextureSized(this.namePlateTexture, this.getX() + 3.0F, this.getY(), 132.0F, 50.0F, Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT, this.level + "", this.getX() + this.getWidth() / 2.0F, this.getY() + 16.0F, Anchor.BOTTOM_CENTER, 18.0F, Color.WHITE
            );
      }
   }

   public class CoinButton extends ShapeButton {
      public TextureAsset backgroundTexture;
      public TextureAsset coinIconTexture;
      public int coinAmount;

      public CoinButton(RenderContext renderContext) {
         super(renderContext);
         this.backgroundTexture = TextureAsset.obtain("ui/116");
         this.coinIconTexture = TextureAsset.obtain("ui/147");
         this.setSize(297.0F, 60.0F);
      }

      public final void setCoins(int coins) {
         this.coinAmount = coins;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.drawTextureSized(this.coinIconTexture, this.getX() + 12.0F, this.getY() + 12.0F, 46.0F, 29.0F, Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.coinAmount + "",
               this.getX() + this.getWidth() - 14.0F,
               this.getY() + 23.0F,
               Anchor.BOTTOM_RIGHT,
               22.0F,
               Color.WHITE
            );
         super.renderContext.resetBatchColor();
      }
   }

   public class ItemGridPanel extends BaseTable {
      public Pool<ItemSlotButton> slotPool = new TradeOfferSlotPool(this);
      public NinePatchAsset borderNinePatch;
      public boolean isMyPanel;
      public Table slotsTable;

      public ItemGridPanel(RenderContext renderContext, boolean isMyPanel) {
         super(renderContext);
         String bgPath = "ui/100";
         this.borderNinePatch = NinePatchAsset.obtain(bgPath, 50);
         this.isMyPanel = isMyPanel;
         this.pad(15.0F);
         this.setSize(496.0F, 427.0F);
         TradeOfferPanel.ItemGridPanel panel = this;
         ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.slotsTable = new Table().top().left().pad(5.0F));
         this.add(scrollPane).grow();
         int i = 0;

         while (true) {
            if (i >= 12) {
               return;
            }

            panel.slotsTable.add(panel.slotPool.obtain()).size(104.0F).space(10.0F);
            if (panel.slotsTable.getChildren().size % 4 == 0) {
               panel.slotsTable.row();
            }

            i++;
         }
      }

      public final TradeOfferPanel.ItemGridPanel setItems(Array<Item> items) {
         for (int i = 0; i < this.slotsTable.getChildren().size; i++) {
            ((ItemSlotButton)this.slotsTable.getChild(i)).setItem(i < items.size ? (Item)items.get(i) : null);
         }

         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.borderNinePatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }
}
