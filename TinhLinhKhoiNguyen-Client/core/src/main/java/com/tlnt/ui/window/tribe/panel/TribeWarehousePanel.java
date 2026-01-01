package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.tribe.panel.info.TribeInfoPanel;

public final class TribeWarehousePanel extends GamePanel {
   public TribeWarehousePanel(RenderContext renderContext) {
      super(renderContext);
      TribeInfoPanel infoPanel;
      (infoPanel = new TribeInfoPanel(super.renderContext)).setSize(616.0F, 610.0F);
      this.addActor(infoPanel);
      TribeWarehousePanel.ItemGridPanel gridPanel;
      (gridPanel = new TribeWarehousePanel.ItemGridPanel(super.renderContext)).setSize(610.0F, 610.0F);
      gridPanel.setPosition(637.0F, 0.0F);
      this.addActor(gridPanel);
   }

   public class ItemGridPanel extends GamePanel {
      public Pool<ItemSlotButton> itemButtonPool = new TribeWarehouseItemPool(this);
      public NinePatchAsset backgroundPatch;
      public Table itemGridTable;

      public ItemGridPanel(RenderContext renderContext) {
         super(renderContext);
         this.backgroundPatch = NinePatchAsset.obtain("ui/100", 50);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.itemGridTable = new Table().pad(6.0F).top().left())).setSize(572.0F, 512.0F);
         scrollPane.setPosition(19.0F, 49.0F);
         this.addActor(scrollPane);
      }

      @Override
      public final void refresh() {
         if (this.itemGridTable.getChildren().size != GameState.tribeData.warehouseItems.size) {
            for (Actor child : this.itemGridTable.getChildren()) {
               this.itemButtonPool.free((ItemSlotButton)child);
            }

            this.itemGridTable.clear();

            for (Item item : GameState.tribeData.warehouseItems) {
               this.itemGridTable.add(this.itemButtonPool.obtain().setItem(item)).size(104.0F).space(10.0F);
               if (this.itemGridTable.getChildren().size % 5 == 0) {
                  this.itemGridTable.row();
               }
            }
         } else {
            for (int i = 0; i < GameState.tribeData.warehouseItems.size; i++) {
               Item warehouseItem = GameState.tribeData.warehouseItems.get(i);
               ((ItemSlotButton)this.itemGridTable.getChild(i)).setItem(warehouseItem);
            }
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Rương Bộ Lạc",
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + 576.0F,
               Anchor.BOTTOM_CENTER,
               30.0F,
               Color.WHITE
            );
         super.draw(batch, parentAlpha);
      }
   }
}
