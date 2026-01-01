package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.forge.ForgeUpgradeState;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class ForgeUpgradePanel extends GamePanel {
   public ForgeUpgradePanel.ContentPanel contentPanel;
   public InventoryGridPanel inventoryPanel;

   public ForgeUpgradePanel(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.contentPanel = new ForgeUpgradePanel.ContentPanel(super.renderContext));
      this.contentPanel.setSize(610.0F, 610.0F);
      this.addActor(this.inventoryPanel = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_5));
      this.inventoryPanel.setItemFilter(new ForgeUpgradeItemFilter(this));
      this.inventoryPanel.setPosition(1247.0F, 0.0F, 20);
   }

   public final ItemSlotButton getMaterialSlot2() {
      return this.contentPanel.materialSlot2;
   }

   public final ItemSlotButton getTargetItemSlot() {
      return this.contentPanel.targetItemSlot;
   }

   public final Button getUpgradeButton() {
      return this.contentPanel.upgradeButton;
   }

   public class ContentPanel extends GamePanel {
      public ItemSlotButton targetItemSlot;
      public ItemSlotButton materialSlot1;
      public ItemSlotButton materialSlot2;
      public ItemSlotButton materialSlot3;
      public TextraLabel descriptionLabel;
      public ForgeUpgradeActionButton upgradeButton;

      public ContentPanel(RenderContext renderContext) {
         super(renderContext);
         ItemSlotButton slot = new ItemSlotButton(super.renderContext);
         this.materialSlot1 = slot.setLabelText("Bảo\nhiểm");
         this.materialSlot1.setSize(104.0F, 104.0F);
         this.materialSlot1.setPosition(65.0F, 306.0F);
         this.addActor(this.materialSlot1);
         this.materialSlot1.addListener(new ForgeUpgradeItemClickListener(this));
         this.materialSlot2 = new ItemSlotButton(super.renderContext).setLabelText("Trang\nbị");
         this.materialSlot2.setSize(104.0F, 104.0F);
         this.materialSlot2.setPosition(255.0F, 403.0F);
         this.addActor(this.materialSlot2);
         this.materialSlot2.addListener(new ForgeUpgradeMainItemSlotListener(this));
         this.materialSlot3 = new ItemSlotButton(super.renderContext);
         this.materialSlot3.setSize(104.0F, 104.0F);
         this.materialSlot3.setPosition(447.0F, 403.0F);
         this.addActor(this.materialSlot3);
         this.materialSlot3.addListener(new ForgeUpgradeMaterialSlotListener(this));
         slot = new ItemSlotButton(super.renderContext);
         this.targetItemSlot = slot.setLabelText("Nguyên\nliệu");
         this.targetItemSlot.setSize(104.0F, 104.0F);
         this.targetItemSlot.setPosition(65.0F, 481.0F);
         this.addActor(this.targetItemSlot);
         this.targetItemSlot.addListener(new ForgeUpgradeMainSlotListener(this));
         this.descriptionLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 22, 3.0F));
         this.descriptionLabel.setAlignment(10);
         this.descriptionLabel.setWrap(true);
         this.descriptionLabel.setSize(610.0F, 180.0F);
         this.descriptionLabel.setPosition(0.0F, 80.0F);
         this.addActor(this.descriptionLabel);
         this.upgradeButton = new ForgeUpgradeActionButton(this, "Nâng cấp", super.renderContext);
         this.upgradeButton.setSize(169.0F, 63.0F);
         this.upgradeButton.setPosition(231.0F, 0.0F);
         this.addActor(this.upgradeButton);
         this.upgradeButton.addListener(new ForgeUpgradeQuantityOneClickListener(this));
      }

      @Override
      public final void refresh() {
         ForgeUpgradeState state = GameState.forgeData.upgradeState;
         this.targetItemSlot.setItem(state.materialItem1);
         this.materialSlot1.setItem(state.materialItem2);
         this.materialSlot2.setItem(state.equipmentItem);
         this.materialSlot3.setItem(state.resultItem);
         this.descriptionLabel.setText(state.infoText);
         if (state.resultStatus != -1) {
            int flag;
            if (state.resultStatus == 1) {
               flag = 1;
            } else {
               flag = 0;
            }

            this.materialSlot2.enchantEffect.playEnchantResult((flag != 0));
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
