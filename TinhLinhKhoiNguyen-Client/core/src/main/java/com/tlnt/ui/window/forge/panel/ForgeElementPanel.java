package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.forge.ForgeElementState;
import com.tlnt.model.item.ElementStone;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class ForgeElementPanel extends GamePanel {
   public ForgeElementPanel.RecipePanel recipePanel;
   public InventoryGridPanel inventoryGrid;

   public ForgeElementPanel(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.recipePanel = new ForgeElementPanel.RecipePanel(super.renderContext));
      this.recipePanel.setSize(610.0F, 610.0F);
      this.addActor(this.inventoryGrid = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_5));
      this.inventoryGrid.setItemFilter(new ForgeElementItemFilter(this));
      this.inventoryGrid.setPosition(1247.0F, 0.0F, 20);
   }

   public class ElementSlotButton extends ItemSlotButton {
      public ElementStone lastFrameId;

      public ElementSlotButton(RenderContext renderContext) {
         super(renderContext);
      }

      public final ForgeElementPanel.ElementSlotButton setElementItem(int slotIndex, ElementStone stone) {
         this.lastFrameId = stone;
         if (this.lastFrameId != null) {
            this.setItem(new Item(slotIndex, stone.stoneTemplate.id, Item.StarGrade.NONE, 1, true, System.currentTimeMillis(), -1L));
         } else {
            this.setItem(null);
         }

         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         if (this.isChecked()) {
            super.renderContext
               .drawNinePatch(
                  super.slotStyle.checkedPatch,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  this.getWidth() + 6.0F,
                  this.getHeight() + 6.0F,
                  Anchor.CENTER
               );
         } else {
            super.renderContext
               .drawNinePatch(
                  super.slotStyle.normalPatch,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  this.getWidth(),
                  this.getHeight(),
                  Anchor.CENTER
               );
         }

         if (this.lastFrameId != null) {
            super.renderContext
               .drawTexture(
                  TextureAsset.obtain(super.item.template.iconPath), this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER
               );
            RenderContext ctx = super.renderContext;
            BitmapFont font = FontManager.DEFAULT_FONT;
            String levelText = "+" + this.lastFrameId.enhanceLevel;
            float textX = this.getX() + this.getWidth() - 10.0F;
            float textY = this.getY() + 10.0F;
            ctx.drawTextColorName(font, levelText, textX, textY, Anchor.BOTTOM_RIGHT, 22.0F, "#FFF600");
         } else if (super.showLabel) {
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  super.labelText,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  Anchor.CENTER,
                  22.0F,
                  Color.WHITE
               );
         }

         super.renderContext.resetBatchColor();
      }
   }

   public class RecipePanel extends GamePanel {
      public ItemSlotButton mainItemSlot;
      public ItemSlotButton leftIngredientSlot;
      public ItemSlotButton rightIngredientSlot;
      public ForgeElementPanel.ElementSlotButton elementSlot1;
      public ForgeElementPanel.ElementSlotButton elementSlot2;
      public ForgeElementPanel.ElementSlotButton elementSlot3;
      public ForgeElementPanel.ElementSlotButton elementSlot4;
      public ForgeElementPanel.ElementSlotButton elementSlot5;
      public TextraLabel descriptionLabel;

      public RecipePanel(RenderContext renderContext) {
         super(renderContext);
         ItemSlotButton slot = new ItemSlotButton(super.renderContext);
         this.leftIngredientSlot = slot.setLabelText("Cỏ may\nmắn");
         this.leftIngredientSlot.setSize(104.0F, 104.0F);
         this.leftIngredientSlot.setPosition(188.0F, 201.0F);
         this.addActor(this.leftIngredientSlot);
         this.leftIngredientSlot.addListener(new ForgeElementLeftIngredientClickListener(this));
         slot = new ItemSlotButton(super.renderContext);
         this.rightIngredientSlot = slot.setLabelText("Nguyên\nliệu");
         this.rightIngredientSlot.setSize(104.0F, 104.0F);
         this.rightIngredientSlot.setPosition(322.0F, 201.0F);
         this.addActor(this.rightIngredientSlot);
         this.rightIngredientSlot.addListener(new ForgeElementIngredientClickListener(this));
         this.elementSlot1 = ForgeElementPanel.this.new ElementSlotButton(super.renderContext);
         this.elementSlot1.setLabelText("Trang bị\n2 sao");
         this.elementSlot1.setSize(104.0F, 104.0F);
         this.elementSlot1.setPosition(61.0F, 325.0F);
         this.addActor(this.elementSlot1);
         this.elementSlot1.addListener(new ForgeElementSubSlot2Listener(this));
         this.elementSlot2 = ForgeElementPanel.this.new ElementSlotButton(super.renderContext);
         this.elementSlot2.setLabelText("Trang bị\n4 sao");
         this.elementSlot2.setSize(104.0F, 104.0F);
         this.elementSlot2.setPosition(111.0F, 459.0F);
         this.addActor(this.elementSlot2);
         this.elementSlot2.addListener(new ForgeElementSlotClickListener(this));
         this.elementSlot3 = ForgeElementPanel.this.new ElementSlotButton(super.renderContext);
         this.elementSlot3.setLabelText("Trang bị\n6 sao");
         this.elementSlot3.setSize(104.0F, 104.0F);
         this.elementSlot3.setPosition(255.0F, 499.0F);
         this.addActor(this.elementSlot3);
         this.elementSlot3.addListener(new ForgeElementSlot3SelectListener(this));
         this.elementSlot4 = ForgeElementPanel.this.new ElementSlotButton(super.renderContext);
         this.elementSlot4.setLabelText("Trang bị\n8 sao");
         this.elementSlot4.setSize(104.0F, 104.0F);
         this.elementSlot4.setPosition(399.0F, 449.0F);
         this.addActor(this.elementSlot4);
         this.elementSlot4.addListener(new ForgeElementSlot4SelectListener(this));
         this.elementSlot5 = ForgeElementPanel.this.new ElementSlotButton(super.renderContext);
         this.elementSlot5.setLabelText("Trang bị\n10 sao");
         this.elementSlot5.setSize(104.0F, 104.0F);
         this.elementSlot5.setPosition(459.0F, 325.0F);
         this.addActor(this.elementSlot5);
         this.elementSlot5.addListener(new ForgeElementSubSlotListener(this));
         slot = new ItemSlotButton(super.renderContext);
         this.mainItemSlot = slot.setLabelText("Trang\nbị");
         this.mainItemSlot.setSize(104.0F, 104.0F);
         this.mainItemSlot.setPosition(255.0F, 325.0F);
         this.addActor(this.mainItemSlot);
         this.mainItemSlot.addListener(new ForgeElementMainSlotListener(this));
         this.descriptionLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 22, 3.0F));
         this.descriptionLabel.setAlignment(10);
         this.descriptionLabel.setWrap(true);
         this.descriptionLabel.setSize(610.0F, 100.0F);
         this.descriptionLabel.setPosition(0.0F, 80.0F);
         this.addActor(this.descriptionLabel);
         ForgeElementActionButton actionButton;
         (actionButton = new ForgeElementActionButton(this, "Khảm", super.renderContext)).setSize(169.0F, 63.0F);
         actionButton.setPosition(231.0F, 0.0F);
         this.addActor(actionButton);
         actionButton.addListener(new ForgeElementConfirmListener(this));
      }

      @Override
      public final void refresh() {
         ForgeElementState state = GameState.forgeData.elementState;
         int flag;
         if (state.elementLevel <= 0) {
            flag = 1;
         } else {
            flag = 0;
         }

         this.elementSlot1.setShowLabel((flag != 0));
         this.elementSlot2
            .setShowLabel(
               ((state.elementLevel < 2 ? 1 : 0) != 0)
            );
         if (state.elementLevel < 3) {
            flag = 1;
         } else {
            flag = 0;
         }

         this.elementSlot3.setShowLabel((flag != 0));
         if (state.elementLevel < 4) {
            flag = 1;
         } else {
            flag = 0;
         }

         this.elementSlot4.setShowLabel((flag != 0));
         if (state.elementLevel < 5) {
            flag = 1;
         } else {
            flag = 0;
         }

         this.elementSlot5.setShowLabel((flag != 0));
         this.mainItemSlot.setItem(state.equipmentItem);
         this.leftIngredientSlot.setItem(state.materialItem1);
         this.rightIngredientSlot.setItem(state.materialItem2);
         if (state.equipmentItem != null) {
            Item equipment = state.equipmentItem;
            this.elementSlot1.setElementItem(0, equipment.elementStones[0]);
            this.elementSlot2.setElementItem(1, equipment.elementStones[1]);
            flag = 2;
            this.elementSlot3.setElementItem(2, equipment.elementStones[2]);
            this.elementSlot4.setElementItem(3, equipment.elementStones[3]);
            this.elementSlot5.setElementItem(4, equipment.elementStones[4]);
         } else {
            this.elementSlot1.setElementItem(0, null);
            this.elementSlot2.setElementItem(1, null);
            this.elementSlot3.setElementItem(2, null);
            this.elementSlot4.setElementItem(3, null);
            this.elementSlot5.setElementItem(4, null);
         }

         this.descriptionLabel.setText(state.infoText);
         if (state.resultStatus != -1) {
            if (state.resultStatus == 1) {
               flag = 1;
            } else {
               flag = 0;
            }

            this.mainItemSlot.enchantEffect.playEnchantResult((flag != 0));
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
