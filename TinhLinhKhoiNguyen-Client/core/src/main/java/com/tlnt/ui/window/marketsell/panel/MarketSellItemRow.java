package com.tlnt.ui.window.marketsell.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.consignment.ConsignmentCategory;
import com.tlnt.model.consignment.ConsignmentItem;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import java.text.MessageFormat;

public class MarketSellItemRow extends GamePanel {
   public Pool<MarketSellItemRow.ItemSlotButton> itemSlotButtonPool = new ItemSlotButtonPool(this);
   public Table itemTable;
   public ConsignmentCategory itemCategory;

   public MarketSellItemRow(RenderContext ctx) {
      super(ctx);
      ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.itemTable = new Table().top().left());
      this.add(scrollPane).grow();
   }

   public final MarketSellItemRow setItemCategory(ConsignmentCategory category) {
      this.itemCategory = category;
      return this;
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator childIter = this.itemTable.getChildren().iterator();

      while (childIter.hasNext()) {
         Actor child;
         if ((child = (Actor)childIter.next()) instanceof MarketSellItemRow.ItemSlotButton) {
            this.itemSlotButtonPool.free((MarketSellItemRow.ItemSlotButton)child);
         }
      }

      this.itemTable.clear();
      if (this.itemCategory != null) {
         for (ConsignmentItem consignmentItem : this.itemCategory.items) {
            this.itemTable.add(this.itemSlotButtonPool.obtain().setItem(consignmentItem)).size(616.0F, 124.0F).space(10.0F);
            if (this.itemTable.getChildren().size % 2 == 0) {
               this.itemTable.row();
            }
         }
      }
   }

   public class ItemSlotButton extends ShapeButton {
      public NinePatchAsset backgroundPatch;
      public com.tlnt.ui.hud.itemslot.ItemSlotButton itemIconWidget;
      public ConsignmentItem item;
      public String priceText;
      public GlyphLayout priceLayout;

      public ItemSlotButton(RenderContext ctx) {
         super(ctx);
         this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
         this.priceLayout = new GlyphLayout();
         this.itemIconWidget = new com.tlnt.ui.hud.itemslot.ItemSlotButton(super.renderContext)
            .setShowCheckedHighlight(false);
         this.itemIconWidget.setSize(104.0F, 104.0F);
         this.itemIconWidget.setPosition(10.0F, 10.0F);
         this.addActor(this.itemIconWidget);
         this.addListener(new MarketSellItemSlotClickListener(this, MarketSellItemRow.this));
      }

      public final MarketSellItemRow.ItemSlotButton setItem(ConsignmentItem consignmentItem) {
         this.item = consignmentItem;
         if (this.item != null) {
            String pattern = "{0}";
            Object[] args = new Object[1];
            args[0] = this.item.quantity;
            this.priceText = MessageFormat.format(pattern, args);
            this.itemIconWidget.setItem(this.item.itemData);
            this.priceLayout.setText(FontFactory.scaleFont(FontManager.DEFAULT_FONT, 22.0F), "Giá bán: " + this.priceText);
         }

         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.item != null && this.item.itemData != null ? this.item.itemData.template.name : "",
               this.getX() + 125.0F,
               this.getY() + 89.0F,
               Anchor.BOTTOM_LEFT,
               22.0F,
               Color.WHITE
            );
         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, "Giá bán: " + this.priceText, this.getX() + 125.0F, this.getY() + 49.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
         super.renderContext
            .drawTextureFit(
               TextureAsset.obtain(this.item.itemConfig.iconPath),
               this.getX() + 130.0F + this.priceLayout.width,
               this.getY() + 55.0F,
               40.0F,
               40.0F,
               Anchor.MIDDLE_LEFT
            );
         this.drawChildren(batch, parentAlpha);
         super.renderContext.resetBatchColor();
      }
   }
}
