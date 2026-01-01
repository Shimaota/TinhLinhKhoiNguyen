package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.market.MarketFeeOption;
import com.tlnt.model.market.MarketListing;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class MarketListingRow extends GamePanel {
   public InventoryGridPanel inventoryGrid;
   public MarketListingRow.MarketSellPanel sellPanel;
   public MarketListingRow.MarketBrowsePanel myListingsPanel;

   public MarketListingRow(RenderContext ctx) {
      super(ctx);
      Table tabBar;
      (tabBar = new ListingTabBackground(this, super.renderContext).pad(10.0F)).setSize(616.0F, 92.0F);
      tabBar.setPosition(0.0F, 518.0F);
      this.addActor(tabBar);
      ButtonGroup tabGroup = new ButtonGroup();
      MarketListingRow.MarketTabButton sellTab = new MarketListingRow.MarketTabButton("Bán", super.renderContext);
      MarketListingRow.MarketTabButton browseTab = new MarketListingRow.MarketTabButton("Đang bán", super.renderContext);
      Button[] tabButtons = new Button[2];
      tabButtons[0] = sellTab;
      tabButtons[1] = browseTab;
      tabGroup.add(tabButtons);
      tabBar.add(sellTab).grow().space(10.0F);
      tabBar.add(browseTab).grow().space(10.0F);
      sellTab.addListener(new MarketSellTabClickListener(this));
      browseTab.addListener(new MarketBrowseTabClickListener(this));
      Group panelGroup;
      (panelGroup = new Group()).setSize(616.0F, 506.0F);
      this.addActor(panelGroup);
      panelGroup.addActor(this.sellPanel = new MarketListingRow.MarketSellPanel(super.renderContext));
      panelGroup.addActor(this.myListingsPanel = new MarketListingRow.MarketBrowsePanel(super.renderContext));
      this.addActor(this.inventoryGrid = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_2));
      this.inventoryGrid.setItemFilter(new MarketItemFilter(this));
      this.inventoryGrid.setPosition(1247.0F, 0.0F, 20);
   }

   public class MarketBrowsePanel extends GamePanel {
      public Pool<MarketListingRow$MarketBrowseListingRow> rowPool = new MarketBrowseRowPool(this);
      public Table rowTable;

      public MarketBrowsePanel(RenderContext ctx) {
         super(ctx);
         this.setFillParent(true);
         this.add(ScrollPaneFactory.createScrollPane(this.rowTable = new Table().top())).grow();
         this.setVisible(false);
      }

      @Override
      public final void refresh() {
         Array.ArrayIterator childIter = this.rowTable.getChildren().iterator();

         while (childIter.hasNext()) {
            Actor child;
            if ((child = (Actor)childIter.next()) instanceof MarketListingRow$MarketBrowseListingRow) {
               this.rowPool.free((MarketListingRow$MarketBrowseListingRow)child);
            }
         }

         this.rowTable.clear();

         for (MarketListing listing : GameState.myMarketListings) {
            this.rowTable.add(this.rowPool.obtain().bindListing(listing)).height(124.0F).growX().space(10.0F).row();
         }
      }
   }

   public class MarketSellPanel extends GamePanel {
      public NinePatchAsset backgroundPatch;
      public Pool<MarketListingRow$MarketCurrencyButton> currencyButtonPool;
      public TextraLabel itemNameLabel;
      public ItemSlotButton itemIconSlot;
      public TextraLabel priceLabel;
      public MarketListingRow$MarketPriceTextField priceInputField;
      public TextraLabel feeLabelCaption;
      public Table currencyButtonTable;
      public TextraLabel feeValueLabel;
      public SellPostButton confirmButton;

      public MarketSellPanel(RenderContext ctx) {
         super(ctx);
         String bgPath = "ui/100";
         this.backgroundPatch = NinePatchAsset.obtain(bgPath, 50);
         this.currencyButtonPool = new MarketCurrencyButtonPool(this);
         this.setFillParent(true);
         this.top();
         String caption = "Vật phẩm bán";
         this.add(this.itemNameLabel = new TextraLabel(caption, FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22)))
            .height(30.0F)
            .growX()
            .row();
         this.itemNameLabel.setAlignment(2);
         this.add(this.itemIconSlot = new ItemSlotButton(super.renderContext)).size(104.0F).spaceTop(18.0F).row();
         this.itemIconSlot.addListener(new MarketItemSelectClickListener(this));
         caption = "Giá bán";
         this.add(this.priceLabel = new TextraLabel(caption, FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22)))
            .height(30.0F)
            .growX()
            .row();
         this.priceLabel.setAlignment(2);
         Table priceInputTable;
         (priceInputTable = new MarketPriceInputBackground(this, super.renderContext).pad(10.0F))
            .add(this.priceInputField = new MarketListingRow$MarketPriceTextField(this, "", super.renderContext))
            .grow();
         this.add(priceInputTable).size(297.0F, 60.0F).spaceTop(18.0F).row();
         caption = "Thời gian bán";
         this.add(this.feeLabelCaption = new TextraLabel(caption, FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22)))
            .height(30.0F)
            .growX()
            .row();
         this.feeLabelCaption.setAlignment(2);
         this.currencyButtonTable = new Table().center();
         this.add(this.currencyButtonTable).height(45.0F).growX().spaceTop(18.0F).row();
         caption = "Phí giao dịch: 3%";
         this.add(this.feeValueLabel = new TextraLabel(caption, FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 22)))
            .height(30.0F)
            .growX()
            .row();
         this.feeValueLabel.setAlignment(2);
         this.add(this.confirmButton = new SellPostButton(this, "Bán", super.renderContext)).size(133.0F, 50.0F).spaceTop(18.0F);
         this.confirmButton.addListener(new MarketConfirmSellClickListener(this));
      }

      @Override
      public final void refresh() {
         this.itemIconSlot.setItem(GameState.marketSellItem);
         if (GameState.selectedMarketCategory == null) {
            GameState.selectedMarketCategory = MarketFeeOption.OPTIONS.first();
         }

         Array.ArrayIterator childIter = this.currencyButtonTable.getChildren().iterator();

         while (childIter.hasNext()) {
            Actor child;
            if ((child = (Actor)childIter.next()) instanceof MarketListingRow$MarketCurrencyButton) {
               this.currencyButtonPool.free((MarketListingRow$MarketCurrencyButton)child);
            }
         }

         this.currencyButtonTable.clear();

         for (MarketFeeOption option : MarketFeeOption.OPTIONS) {
            MarketListingRow$MarketCurrencyButton button = this.currencyButtonPool.obtain().bindCurrencyType(option);
            this.currencyButtonTable.add(button).size(92.0F, 45.0F).space(10.0F);
            if (option.equals(GameState.selectedMarketCategory)) {
               button.setChecked(true);
            }
         }

         this.feeValueLabel.setText("Phí giao dịch: " + GameState.selectedMarketCategory.feePercent + "%");
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }

      public void submitSellListing() {
         if (GameState.marketSellItem == null) {
            GameMain.postFloatingMessage("[red]Cần chọn lấy 1 vật phẩm!");
         } else {
            int price = 0;

            try {
               price = Integer.parseInt(this.priceInputField.getText());
            } catch (Exception e) {
            }

            if (price <= 0) {
               GameMain.postFloatingMessage("Giá bán không hợp lệ!");
            } else {
               ServerPacketSender.getInstance()
                  .sendBuyItem(GameState.marketSellItem.slotIndex, GameState.marketSellItem.quantity, price, GameState.selectedMarketCategory.id);
            }
         }
      }
   }

   public class MarketTabButton extends TextButton {
      public CompositeSprite buttonSprite;
      public Color textColor;

      public MarketTabButton(String text, RenderContext ctx) {
         super(text, ctx);
         String leftPath = "ui/109";
         String middlePath = "ui/110";
         String rightPath = "ui/111";
         this.buttonSprite = CompositeSprite.obtain(leftPath, middlePath, rightPath, false);
         this.textColor = new Color(0.54509807F, 0.30588236F, 0.13725491F, 1.0F);
      }

      @Override
      public final void drawBackground() {
         if (this.isChecked()) {
            super.renderContext.drawCompositeSprite(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         }
      }

      @Override
      public final void drawLabel() {
         super.renderContext
            .drawText(
               FontManager.PLAIN_FONT,
               super.text,
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               Anchor.CENTER,
               30.0F,
               this.textColor
            );
      }
   }
}
