package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.market.MarketListing;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.GameTextField;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.util.TimeFormatter;
import java.text.MessageFormat;

public final class MarketShopPanel extends GamePanel {
   public Pool<MarketShopPanel.MarketBuyListingRow> shopItemPool = new MarketBuyListingRowPool(this);
   public MarketShopPanel.MarketSearchTextField searchField;
   public ScrollPane scrollPane;
   public Table itemListTable;

   public MarketShopPanel(RenderContext ctx) {
      super(ctx);
      Table searchBar;
      (searchBar = new MarketSearchBackground(this, super.renderContext).pad(5.0F))
         .add(this.searchField = new MarketShopPanel.MarketSearchTextField("", super.renderContext))
         .grow()
         .space(5.0F);
      this.searchField.addListener(new MarketSearchKeyListener(this));
      searchBar.add(new MarketSearchRefreshIcon(this, super.renderContext)).size(35.0F, 35.0F);
      searchBar.setSize(1104.0F, 48.0F);
      searchBar.setPosition(0.0F, 562.0F);
      this.addActor(searchBar);
      MarketRefreshButton refreshButton;
      (refreshButton = new MarketRefreshButton(this, "Làm mới", super.renderContext)).setSize(133.0F, 50.0F);
      refreshButton.setPosition(1114.0F, 562.0F);
      this.addActor(refreshButton);
      this.scrollPane = ScrollPaneFactory.createScrollPane(this.itemListTable = new Table().top().left());
      this.scrollPane.setSize(1247.0F, 550.0F);
      this.addActor(this.scrollPane);
   }

   @Override
   public final void onShow() {
      this.scrollPane.setScrollY(0.0F);
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator childIter = this.itemListTable.getChildren().iterator();

      while (childIter.hasNext()) {
         Actor child;
         if ((child = (Actor)childIter.next()) instanceof MarketShopPanel.MarketBuyListingRow) {
            this.shopItemPool.free((MarketShopPanel.MarketBuyListingRow)child);
         }
      }

      this.itemListTable.clear();
      String query = this.searchField.getText();

      for (MarketListing listing : GameState.marketListings) {
         if (query.isEmpty() || listing.item.template.name.toLowerCase().contains(query.toLowerCase())) {
            this.itemListTable.add(this.shopItemPool.obtain().bindListing(listing)).size(618.0F, 124.0F).space(10.0F);
            if (this.itemListTable.getChildren().size % 2 == 0) {
               this.itemListTable.row();
            }
         }
      }
   }

   public class MarketBuyListingRow extends ShapeButton {
      public NinePatchAsset backgroundPatch;
      public ItemSlotButton itemIcon;
      public MarketListing listing;
      public String priceText;
      public GlyphLayout priceLayout;

      public MarketBuyListingRow(RenderContext ctx) {
         super(ctx);
         this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
         this.priceLayout = new GlyphLayout();
         this.itemIcon = new ItemSlotButton(super.renderContext);
         this.itemIcon.setSize(104.0F, 104.0F);
         this.itemIcon.setPosition(10.0F, 10.0F);
         this.itemIcon.setShowCheckedHighlight(false);
         this.addActor(this.itemIcon);
         this.addListener(new ShopItemClickListener(this, MarketShopPanel.this));
      }

      public final MarketShopPanel.MarketBuyListingRow bindListing(MarketListing listing) {
         this.listing = listing;
         if (this.listing != null) {
            this.itemIcon.setItem(this.listing.item);
            String pattern = "{0}";
            Object[] args = new Object[1];
            args[0] = this.listing.price;
            this.priceText = MessageFormat.format(pattern, args);
            this.priceLayout.setText(FontFactory.scaleFont(FontManager.DEFAULT_FONT, 22.0F), "Giá bán: " + this.priceText);
         }

         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.draw(batch, parentAlpha);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT, this.listing.item.template.name, this.getX() + 125.0F, this.getY() + 89.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE
            );
         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, "Giá bán: " + this.priceText, this.getX() + 125.0F, this.getY() + 49.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Người bán: " + this.listing.sellerName,
               this.getX() + 125.0F,
               this.getY() + 17.0F,
               Anchor.BOTTOM_LEFT,
               22.0F,
               Color.WHITE
            );
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               TimeFormatter.formatTimeRemaining(this.listing.expireTime),
               this.getX() + this.getWidth() - 10.0F,
               this.getY() + 89.0F,
               Anchor.BOTTOM_RIGHT,
               22.0F,
               Color.YELLOW
            );
         super.renderContext
            .drawTextureFit(
               TextureAsset.obtain("ui/147"),
               this.getX() + 130.0F + this.priceLayout.width,
               this.getY() + 45.0F,
               50.0F,
               50.0F,
               Anchor.BOTTOM_LEFT
            );
      }
   }

   public class MarketSearchTextField extends GameTextField {
      public MarketSearchTextField(String text, RenderContext ctx) {
         super(text, ctx, FontFactory.scaleFont(FontManager.TEXT_FIELD_FONT, 22.0F));
      }

      @Override
      public final void drawBackground(float x, float y, float width, float height, boolean focused, boolean disabled, float parentAlpha) {
      }

      @Override
      public final void drawText(Batch batch, BitmapFont font, float x, float y) {
         super.drawText(batch, font, x, y - 5.0F);
      }
   }
}
