package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.GamePanel;

public final class ItemCountGridPanel extends GamePanel {
   public final Pool<ItemSlotButton> itemSlotPool = new ItemCountSlotButtonPool(this);
   public NinePatchAsset background;
   public Table gridTable;

   public ItemCountGridPanel(RenderContext renderContext) {
      super(renderContext);
      this.background = NinePatchAsset.obtain("ui/100", 50);
      this.setSize(610.0F, 610.0F);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.gridTable = new Table().pad(6.0F).top().left())).setSize(572.0F, 521.0F);
      scrollPane.setPosition(19.0F, 70.0F);
      this.addActor(scrollPane);
      this.addActor(new ItemCountGridPanel.ConfirmButton(super.renderContext));
   }

   public final ItemSlotButton getItemSlot(int index) {
      return (ItemSlotButton)this.gridTable.getChild(0);
   }

   @Override
   public final void refresh() {
      Array<Item> items = GameState.inventory.materialItems;
      if (this.gridTable.getChildren().size != items.size) {
         Array.ArrayIterator it = this.gridTable.getChildren().iterator();

         while (it.hasNext()) {
            Actor child;
            if ((child = (Actor)it.next()) instanceof ItemSlotButton) {
               this.itemSlotPool.free((ItemSlotButton)child);
            }
         }

         this.gridTable.clear();

         for (Item each : items) {
            this.gridTable.add(this.itemSlotPool.obtain()).size(104.0F).space(10.0F);
            if (this.gridTable.getChildren().size % 5 == 0) {
               this.gridTable.row();
            }
         }
      }

      for (int i = 0; i < items.size; i++) {
         ((ItemSlotButton)this.gridTable.getChild(i)).setItem((Item)items.get(i));
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Số lượng: " + GameState.inventory.materialItemCount + "/" + GameState.inventory.materialItems.size,
            this.getX() + 28.0F,
            this.getY() + 30.0F,
            Anchor.BOTTOM_LEFT,
            22.0F,
            Color.WHITE
         );
      super.draw(batch, parentAlpha);
   }

   public class ConfirmButton extends TextButton {

      public ConfirmButton(RenderContext renderContext) {
         super("Sắp xếp", renderContext);
         this.setSize(133.0F, 50.0F);
         this.setPosition(454.0F, 10.0F);
         this.addListener(new ItemGridCloseClickListener(this, ItemCountGridPanel.this));
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain("ui/102"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
            );
         super.renderContext.resetBatchColor();
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
               22.0F,
               Color.WHITE
            );
      }
   }
}
