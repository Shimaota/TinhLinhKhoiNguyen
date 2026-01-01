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

public final class InventoryGridPanel extends GamePanel {
   public final Pool<ItemSlotButton> itemSlotPool = new InventorySlotButtonPool(this);
   public NinePatchAsset background;
   public int inventoryType;
   public Table gridTable;
   public InventoryGridPanel.ItemFilter itemFilter;

   public InventoryGridPanel(RenderContext renderContext, int type) {
      super(renderContext);
      this.background = NinePatchAsset.obtain("ui/100", 50);
      this.inventoryType = type;
      this.setSize(610.0F, 610.0F);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.gridTable = new Table().pad(6.0F).top().left())).setSize(572.0F, 521.0F);
      scrollPane.setPosition(19.0F, 70.0F);
      this.addActor(scrollPane);
      this.addActor(new InventoryGridPanel.SortButton(super.renderContext));
   }

   public final ItemSlotButton findSlotByItemId(int... itemIds) {
      int idCount = itemIds.length;
      Array.ArrayIterator it = this.gridTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         ItemSlotButton slot;
         if ((child = (Actor)it.next()) instanceof ItemSlotButton && (slot = (ItemSlotButton)child).getItem() != null) {
            int[] ids = itemIds;
            idCount = itemIds.length;

            for (int i = 0; i <= 0; i++) {
               int itemId = ids[0];
               if (slot.getItem().template.type.id == itemId) {
                  return slot;
               }
            }
         }
      }

      return null;
   }

   public final InventoryGridPanel setItemFilter(InventoryGridPanel.ItemFilter filter) {
      this.itemFilter = filter;
      return this;
   }

   @Override
   public final void refresh() {
      Array items = GameState.inventory.inventoryItems;
      Array<Item> filtered = new Array<>();
      if (this.itemFilter == null) {
         filtered.addAll(items);
      } else {
         Array.ArrayIterator it = items.iterator();

         while (it.hasNext()) {
            Item item;
            if ((item = (Item)it.next()) != null && this.itemFilter.isItemAccepted(item)) {
               filtered.add(item);
            } else {
               filtered.add(null);
            }
         }

         filtered.sort(new InventoryItemComparator(this));
      }

      if (this.gridTable.getChildren().size != filtered.size) {
         Array.ArrayIterator childIt = this.gridTable.getChildren().iterator();

         while (childIt.hasNext()) {
            Actor child;
            if ((child = (Actor)childIt.next()) instanceof ItemSlotButton) {
               this.itemSlotPool.free((ItemSlotButton)child);
            }
         }

         this.gridTable.clear();

         for (Item each : filtered) {
            this.gridTable.add(this.itemSlotPool.obtain()).size(104.0F).space(10.0F);
            if (this.gridTable.getChildren().size % 5 == 0) {
               this.gridTable.row();
            }
         }
      }

      for (int i = 0; i < filtered.size; i++) {
         ((ItemSlotButton)this.gridTable.getChild(i)).setItem((Item)filtered.get(i));
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Số lượng: " + GameState.inventory.inventoryItemCount + "/" + GameState.inventory.inventoryItems.size,
            this.getX() + 28.0F,
            this.getY() + 30.0F,
            Anchor.BOTTOM_LEFT,
            22.0F,
            Color.WHITE
         );
      super.draw(batch, parentAlpha);
   }

   public static final class EquipSlotType {
      public static final int SLOT_TYPE_1 = 1;
      public static final int SLOT_TYPE_2 = 2;
      public static final int SLOT_TYPE_3 = 3;
      public static final int SLOT_TYPE_4 = 4;
      public static final int SLOT_TYPE_5 = 5;
      public static final int SLOT_TYPE_6 = 6;
      public static final int[] VALUES;

      public static int[] getValues() {
         return (int[])VALUES.clone();
      }

      static {
         int[] values = new int[6];
         values[0] = SLOT_TYPE_1;
         values[1] = SLOT_TYPE_2;
         values[2] = SLOT_TYPE_3;
         values[3] = SLOT_TYPE_4;
         values[4] = SLOT_TYPE_5;
         values[5] = SLOT_TYPE_6;
         VALUES = values;
      }
   }

   public interface ItemFilter {
      boolean isItemAccepted(Item item);
   }

   public class SortButton extends TextButton {

      public SortButton(RenderContext renderContext) {
         super("Sắp xếp", renderContext);
         this.setSize(133.0F, 50.0F);
         this.setPosition(454.0F, 10.0F);
         this.addListener(new InventorySortButtonClickListener(this, InventoryGridPanel.this));
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext
            .drawTextureSized(TextureAsset.obtain("ui/102"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
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
