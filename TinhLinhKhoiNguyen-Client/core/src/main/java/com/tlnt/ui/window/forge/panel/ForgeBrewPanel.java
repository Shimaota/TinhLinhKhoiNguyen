package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.forge.ForgeBrewState;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class ForgeBrewPanel extends GamePanel {
   public ForgeBrewPanel.BrewControlPanel brewControlPanel;
   public InventoryGridPanel inventoryGridPanel;

   public ForgeBrewPanel(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.brewControlPanel = new ForgeBrewPanel.BrewControlPanel(super.renderContext));
      this.brewControlPanel.setSize(610.0F, 610.0F);
      this.addActor(this.inventoryGridPanel = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_5));
      this.inventoryGridPanel.setItemFilter(new ForgeBrewItemFilter(this));
      this.inventoryGridPanel.setPosition(1247.0F, 0.0F, 20);
   }

   public class BrewControlPanel extends GamePanel {
      public ItemSlotButton ingredientSlot1;
      public ItemSlotButton ingredientSlot2;
      public ItemSlotButton ingredientSlot3;
      public TextraLabel descriptionLabel;

      public BrewControlPanel(RenderContext renderContext) {
         super(renderContext);
         this.ingredientSlot2 = new ItemSlotButton(super.renderContext).setLabelText("Bình\nnước");
         this.ingredientSlot2.setSize(104.0F, 104.0F);
         this.ingredientSlot2.setPosition(255.0F, 403.0F);
         this.addActor(this.ingredientSlot2);
         this.ingredientSlot2.addListener(new ForgeBrewIngredientSlotListener(this));
         this.ingredientSlot3 = new ItemSlotButton(super.renderContext);
         this.ingredientSlot3.setSize(104.0F, 104.0F);
         this.ingredientSlot3.setPosition(447.0F, 403.0F);
         this.addActor(this.ingredientSlot3);
         this.ingredientSlot3.addListener(new ForgeBrewRightSlotClickListener(this));
         ItemSlotButton slot = new ItemSlotButton(super.renderContext);
         this.ingredientSlot1 = slot.setLabelText("Nguyên\nliệu");
         this.ingredientSlot1.setSize(104.0F, 104.0F);
         this.ingredientSlot1.setPosition(65.0F, 403.0F);
         this.addActor(this.ingredientSlot1);
         this.ingredientSlot1.addListener(new ForgeBrewLeftSlotClickListener(this));
         this.descriptionLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 22, 3.0F));
         this.descriptionLabel.setAlignment(10);
         this.descriptionLabel.setWrap(true);
         this.descriptionLabel.setSize(610.0F, 180.0F);
         this.descriptionLabel.setPosition(0.0F, 80.0F);
         this.addActor(this.descriptionLabel);
         ForgeBrewActionButton actionButton;
         (actionButton = new ForgeBrewActionButton(this, "Pha chế", super.renderContext)).setSize(169.0F, 63.0F);
         actionButton.setPosition(231.0F, 0.0F);
         this.addActor(actionButton);
         actionButton.addListener(new ForgeBrewQuantityClickListener(this));
      }

      @Override
      public final void refresh() {
         ForgeBrewState state = GameState.forgeData.brewState;
         this.ingredientSlot1.setItem(state.materialItem);
         this.ingredientSlot2.setItem(state.mainItem);
         this.ingredientSlot3.setItem(state.resultItem);
         this.descriptionLabel.setText(state.infoText);
         if (state.resultStatus != -1) {
            this.ingredientSlot2
               .enchantEffect
               .playEnchantResult(((state.resultStatus == 1 ? 1 : 0) != 0));
            state.resultStatus = (byte)-1;
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawTexture(TextureAsset.obtain("ui/170"), this.getX() + 36.0F, this.getY() + 93.0F, Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }
}
