package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.forge.ForgeSoulState;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class ForgeSoulPanel extends GamePanel {
   public ForgeSoulPanel.SlotPanel slotPanel;
   public InventoryGridPanel inventoryGrid;

   public ForgeSoulPanel(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.slotPanel = new ForgeSoulPanel.SlotPanel(super.renderContext));
      this.slotPanel.setSize(610.0F, 610.0F);
      this.addActor(this.inventoryGrid = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_5));
      this.inventoryGrid.setItemFilter(new ForgeSoulItemFilter(this));
      this.inventoryGrid.setPosition(1247.0F, 0.0F, 20);
   }

   public class SlotPanel extends GamePanel {
      public ItemSlotButton centerSlot;
      public ItemSlotButton topLeftSlot;
      public ItemSlotButton topRightSlot;
      public ItemSlotButton leftSlot;
      public ItemSlotButton lowerLeftSlot;
      public ItemSlotButton bottomSlot;
      public ItemSlotButton lowerRightSlot;
      public ItemSlotButton rightSlot;
      public TextraLabel descriptionLabel;

      public SlotPanel(RenderContext renderContext) {
         super(renderContext);
         ItemSlotButton slot = new ItemSlotButton(super.renderContext);
         this.topLeftSlot = slot.setLabelText("Cỏ may\nmắn");
         this.topLeftSlot.setSize(104.0F, 104.0F);
         this.topLeftSlot.setPosition(188.0F, 201.0F);
         this.addActor(this.topLeftSlot);
         this.topLeftSlot.addListener(new ForgeSoulSlot1SelectListener(this));
         this.topRightSlot = new ItemSlotButton(super.renderContext).setLabelText("Nguyên\nliệu");
         this.topRightSlot.setSize(104.0F, 104.0F);
         this.topRightSlot.setPosition(322.0F, 201.0F);
         this.addActor(this.topRightSlot);
         this.topRightSlot.addListener(new ForgeSoulSlot2SelectListener(this));
         this.leftSlot = new ItemSlotButton(super.renderContext).setLabelText("Trang bị\n2 sao");
         this.leftSlot.setSize(104.0F, 104.0F);
         this.leftSlot.setPosition(61.0F, 325.0F);
         this.addActor(this.leftSlot);
         this.leftSlot.addListener(new ForgeSoulLeftSlotClickListener(this));
         slot = new ItemSlotButton(super.renderContext);
         this.lowerLeftSlot = slot.setLabelText("Trang bị\n4 sao");
         this.lowerLeftSlot.setSize(104.0F, 104.0F);
         this.lowerLeftSlot.setPosition(111.0F, 459.0F);
         this.addActor(this.lowerLeftSlot);
         this.lowerLeftSlot.addListener(new ForgeSoulSlotClickListener(this));
         slot = new ItemSlotButton(super.renderContext);
         this.bottomSlot = slot.setLabelText("Trang bị\n6 sao");
         this.bottomSlot.setSize(104.0F, 104.0F);
         this.bottomSlot.setPosition(255.0F, 499.0F);
         this.addActor(this.bottomSlot);
         this.bottomSlot.addListener(new ForgeSoulItemSlotClickListener(this));
         slot = new ItemSlotButton(super.renderContext);
         this.lowerRightSlot = slot.setLabelText("Trang bị\n8 sao");
         this.lowerRightSlot.setSize(104.0F, 104.0F);
         this.lowerRightSlot.setPosition(399.0F, 449.0F);
         this.addActor(this.lowerRightSlot);
         this.lowerRightSlot.addListener(new ForgeSoulSlot6SelectListener(this));
         this.rightSlot = new ItemSlotButton(super.renderContext).setLabelText("Trang bị\n10 sao");
         this.rightSlot.setSize(104.0F, 104.0F);
         this.rightSlot.setPosition(459.0F, 325.0F);
         this.addActor(this.rightSlot);
         this.rightSlot.addListener(new ForgeSoulResultSlotClickListener(this));
         slot = new ItemSlotButton(super.renderContext);
         this.centerSlot = slot.setLabelText("Trang\nbị");
         this.centerSlot.setSize(104.0F, 104.0F);
         this.centerSlot.setPosition(255.0F, 325.0F);
         this.addActor(this.centerSlot);
         this.centerSlot.addListener(new ForgeSoulCenterSlotListener(this));
         String descText = "Gà\nvlz";
         this.descriptionLabel = new TextraLabel(
            descText, FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 22, 3.0F)
         );
         this.descriptionLabel.setAlignment(10);
         this.descriptionLabel.setWrap(true);
         this.descriptionLabel.setSize(610.0F, 100.0F);
         this.descriptionLabel.setPosition(0.0F, 80.0F);
         this.addActor(this.descriptionLabel);
         ForgeSoulActionButton actionButton;
         (actionButton = new ForgeSoulActionButton(this, "Khảm", super.renderContext)).setSize(169.0F, 63.0F);
         actionButton.setPosition(231.0F, 0.0F);
         this.addActor(actionButton);
         actionButton.addListener(new ForgeSoulConfirmListener(this));
      }

      @Override
      public final void refresh() {
         ForgeSoulState state = GameState.forgeData.soulState;
         this.leftSlot.setShowLabel(((state.soulLevel <= 0 ? 1 : 0) != 0));
         int flag;
         if (state.soulLevel < 2) {
            flag = 1;
         } else {
            flag = 0;
         }

         this.lowerLeftSlot.setShowLabel((flag != 0));
         if (state.soulLevel < 3) {
            flag = 1;
         } else {
            flag = 0;
         }

         this.bottomSlot.setShowLabel((flag != 0));
         if (state.soulLevel < 4) {
            flag = 1;
         } else {
            flag = 0;
         }

         this.lowerRightSlot.setShowLabel((flag != 0));
         this.rightSlot.setShowLabel(((state.soulLevel < 5 ? 1 : 0) != 0));
         this.centerSlot.setItem(state.equipmentItem);
         this.topLeftSlot.setItem(state.materialItem1);
         this.topRightSlot.setItem(state.materialItem2);
         Item charm0;
         if (state.equipmentItem != null) {
            charm0 = state.equipmentItem.soulCharms[0];
         } else {
            charm0 = null;
         }

         this.leftSlot.setItem(charm0);
         Item charm1;
         if (state.equipmentItem != null) {
            charm1 = state.equipmentItem.soulCharms[1];
         } else {
            charm1 = null;
         }

         this.lowerLeftSlot.setItem(charm1);
         Item charm2;
         if (state.equipmentItem != null) {
            charm2 = state.equipmentItem.soulCharms[2];
         } else {
            charm2 = null;
         }

         this.bottomSlot.setItem(charm2);
         this.lowerRightSlot.setItem(state.equipmentItem != null ? state.equipmentItem.soulCharms[3] : null);
         Item charm4;
         if (state.equipmentItem != null) {
            charm4 = state.equipmentItem.soulCharms[4];
         } else {
            charm4 = null;
         }

         this.rightSlot.setItem(charm4);
         this.descriptionLabel.setText(state.infoText);
         if (state.resultStatus != -1) {
            this.centerSlot
               .enchantEffect
               .playEnchantResult(
                  ((state.resultStatus == 1 ? 1 : 0) != 0)
               );
            state.resultStatus = (byte)-1;
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext
            .drawTexture(TextureAsset.obtain("ui/167"), this.getX() + 36.0F, this.getY() + 93.0F, Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }
}
