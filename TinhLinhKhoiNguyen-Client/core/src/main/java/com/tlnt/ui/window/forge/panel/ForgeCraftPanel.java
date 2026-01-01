package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.forge.ForgeCraftState;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class ForgeCraftPanel extends GamePanel {
   public ForgeCraftPanel.ContentPanel contentPanel;
   public InventoryGridPanel inventoryGrid;

   public ForgeCraftPanel(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.contentPanel = new ForgeCraftPanel.ContentPanel(super.renderContext));
      this.contentPanel.setSize(610.0F, 610.0F);
      this.addActor(this.inventoryGrid = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_5));
      this.inventoryGrid.setItemFilter(new ForgeCraftItemFilter(this));
      this.inventoryGrid.setPosition(1247.0F, 0.0F, 20);
   }

   public class ContentPanel extends GamePanel {
      public ItemSlotButton recipeSlot;
      public ItemSlotButton ingredient2Slot;
      public ItemSlotButton ingredient1Slot;
      public ItemSlotButton outputSlot;
      public TextraLabel descriptionLabel;

      public ContentPanel(RenderContext renderContext) {
         super(renderContext);
         ItemSlotButton slot = new ItemSlotButton(super.renderContext);
         this.ingredient2Slot = slot.setLabelText("Cỏ may\nmắn");
         this.ingredient2Slot.setSize(104.0F, 104.0F);
         this.ingredient2Slot.setPosition(65.0F, 306.0F);
         this.addActor(this.ingredient2Slot);
         this.ingredient2Slot.addListener(new ForgeCraftIngredientSlotListener(this));
         slot = new ItemSlotButton(super.renderContext);
         this.ingredient1Slot = slot.setLabelText("Phôi\ntrang bị");
         this.ingredient1Slot.setSize(104.0F, 104.0F);
         this.ingredient1Slot.setPosition(255.0F, 403.0F);
         this.addActor(this.ingredient1Slot);
         this.ingredient1Slot.addListener(new ForgeCraftMaterial1SlotListener(this));
         this.outputSlot = new ItemSlotButton(super.renderContext);
         this.outputSlot.setSize(104.0F, 104.0F);
         this.outputSlot.setPosition(447.0F, 403.0F);
         this.addActor(this.outputSlot);
         this.outputSlot.addListener(new ForgeCraftOutputSlotListener(this));
         slot = new ItemSlotButton(super.renderContext);
         this.recipeSlot = slot.setLabelText("Đá linh\nthạch");
         this.recipeSlot.setSize(104.0F, 104.0F);
         this.recipeSlot.setPosition(65.0F, 481.0F);
         this.addActor(this.recipeSlot);
         this.recipeSlot.addListener(new ForgeCraftRecipeSlotListener(this));
         this.descriptionLabel = new TextraLabel(
            "", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 22, 3.0F)
         );
         this.descriptionLabel.setAlignment(10);
         this.descriptionLabel.setWrap(true);
         this.descriptionLabel.setSize(610.0F, 180.0F);
         this.descriptionLabel.setPosition(0.0F, 80.0F);
         this.addActor(this.descriptionLabel);
         ForgeCraftActionButton actionButton;
         (actionButton = new ForgeCraftActionButton(this, "Ghép", super.renderContext)).setSize(169.0F, 63.0F);
         actionButton.setPosition(231.0F, 0.0F);
         this.addActor(actionButton);
         actionButton.addListener(new ForgeCraftConfirmListener(this));
      }

      @Override
      public final void refresh() {
         ForgeCraftState state = GameState.forgeData.craftState;
         this.recipeSlot.setItem(state.materialItem1);
         this.ingredient2Slot.setItem(state.materialItem2);
         this.ingredient1Slot.setItem(state.mainItem);
         this.outputSlot.setItem(state.resultItem);
         this.descriptionLabel.setText(state.infoText);
         if (state.resultStatus != -1) {
            int flag;
            if (state.resultStatus == 1) {
               flag = 1;
            } else {
               flag = 0;
            }

            this.ingredient1Slot.enchantEffect.playEnchantResult((flag != 0));
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
