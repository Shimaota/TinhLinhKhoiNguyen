package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.market.MarketListing;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.util.TimeFormatter;
import java.text.MessageFormat;

public final class MarketListingRow$MarketBrowseListingRow extends ShapeButton {
   public MarketListingRow.MarketBrowsePanel browsePanel;
   public NinePatchAsset backgroundPatch;
   public ItemSlotButton itemIcon;
   public MarketListing listing;
   public String priceText;
   public GlyphLayout priceLayout;

   public MarketListingRow$MarketBrowseListingRow(MarketListingRow.MarketBrowsePanel browsePanel, RenderContext ctx) {
      super(ctx);
      this.browsePanel = browsePanel;
      String bgPath = "ui/100";
      this.backgroundPatch = NinePatchAsset.obtain(bgPath, 50);
      this.priceLayout = new GlyphLayout();
      this.itemIcon = new ItemSlotButton(super.renderContext);
      this.itemIcon.setSize(104.0F, 104.0F);
      this.itemIcon.setPosition(10.0F, 10.0F);
      this.addActor(this.itemIcon);
      this.addListener(new MyListingItemClickListener(this, browsePanel));
   }

   public final MarketListingRow$MarketBrowseListingRow bindListing(MarketListing listing) {
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
         .drawText(FontManager.DEFAULT_FONT, this.listing.item.template.name, this.getX() + 125.0F, this.getY() + 89.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
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
