package com.tlnt.ui.window.shop.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.SnapshotArray;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.combopack.ComboPack;
import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;

public final class ComboPackPanel extends GamePanel {
   public Pool<ComboPackPanel.ComboPackRow> rowPool = new ComboPackRowPool(this);
   public Table rowsTable;

   public ComboPackPanel(RenderContext renderContext) {
      super(renderContext);
      ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.rowsTable = new Table().top().left());
      this.add(scrollPane).grow();
   }

   @Override
   public final void onShow() {
      ServerPacketSender.getInstance().sendRequestSocialList();
   }

   @Override
   public final void refresh() {

      SnapshotArray children;
      for (int i = (children = this.rowsTable.getChildren()).size - 1; i >= 0; i--) {
         Actor child;
         if ((child = (Actor)children.get(i)) instanceof ComboPackPanel.ComboPackRow) {
            this.rowPool.free((ComboPackPanel.ComboPackRow)child);
         }
      }

      this.rowsTable.clear();

      for (ComboPack pack : GameState.comboPackData.packs) {
         this.rowsTable.add(this.rowPool.obtain().setPackData(pack)).size(616.0F, 181.0F).space(12.0F);
         if (this.rowsTable.getChildren().size % 2 == 0) {
            this.rowsTable.row();
         }
      }
   }

   public class ComboPackRow extends BaseTable {

      public final ComboPackPanel comboPackPanel = ComboPackPanel.this;
      public Pool<ItemSlotButton> itemButtonPool = new ComboPackItemPool(this);
      public NinePatchAsset background;
      public CompositeSprite buyButtonSprite;
      public Table itemsTable;
      public ComboPack packData;

      public ComboPackRow(RenderContext renderContext) {
         super(renderContext);
         this.background = NinePatchAsset.obtain("ui/100", 50);
         String leftPath = "ui/51";
         String middlePath = "ui/52";
         String rightPath = "ui/53";
         this.buyButtonSprite = CompositeSprite.obtain(leftPath, middlePath, rightPath, false);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.itemsTable = new Table().left().pad(4.0F))).setSize(456.0F, 112.0F);
         scrollPane.setPosition(11.0F, 63.0F);
         this.addActor(scrollPane);
         ComboPackBuyButton buyButton;
         (buyButton = new ComboPackBuyButton(this, super.renderContext)).setSize(133.0F, 50.0F);
         buyButton.setPosition(471.0F, 10.0F);
         this.addActor(buyButton);
         buyButton.addListener(new ComboPackBuyClickListener(this));
      }

      public final ComboPackPanel.ComboPackRow setPackData(ComboPack pack) {
         this.packData = pack;
         if (this.packData != null) {

            for (int i = this.itemsTable.getChildren().size - 1; i >= 0; i--) {
               Actor child;
               if ((child = this.itemsTable.getChild(i)) instanceof ItemSlotButton) {
                  this.itemButtonPool.free((ItemSlotButton)child);
               }
            }
         }

         this.itemsTable.clear();

         for (Item item : this.packData.items) {
            this.itemsTable.add(this.itemButtonPool.obtain().setItem(item)).size(104.0F, 104.0F).space(10.0F);
         }

         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.packData != null ? this.packData.name : "",
               this.getX() + 15.0F,
               this.getY() + 20.0F,
               Anchor.BOTTOM_LEFT,
               22.0F,
               Color.WHITE
            );
         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, "Giá bán", this.getX() + 500.0F, this.getY() + 147.0F, Anchor.BOTTOM_LEFT, 22.0F, Color.WHITE);
         super.renderContext.drawCompositeSprite(this.buyButtonSprite, this.getX() + 471.0F, this.getY() + 67.0F, 133.0F, 69.0F, Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.packData != null ? this.packData.price + "" : "",
               this.getX() + this.getWidth() - 70.0F,
               this.getY() + 94.0F,
               Anchor.BOTTOM_RIGHT,
               22.0F,
               Color.WHITE
            );
         super.renderContext
            .drawTextureFit(
               TextureAsset.obtain(this.packData != null ? this.packData.priceItem.iconPath : ""),
               this.getX() + 551.0F,
               this.getY() + 85.0F,
               36.0F,
               32.0F,
               Anchor.BOTTOM_LEFT
            );
         super.draw(batch, parentAlpha);
      }
   }
}
