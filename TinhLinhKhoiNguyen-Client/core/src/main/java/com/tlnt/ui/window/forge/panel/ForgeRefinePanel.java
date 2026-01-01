package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.forge.ForgeRefineState;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;
import com.tlnt.ui.window.charinfo.SettingsToggleButton;

public final class ForgeRefinePanel extends GamePanel {
   public ForgeRefinePanel.ContentPanel contentPanel;
   public InventoryGridPanel inventoryGrid;

   public ForgeRefinePanel(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.contentPanel = new ForgeRefinePanel.ContentPanel(super.renderContext));
      this.contentPanel.setSize(610.0F, 610.0F);
      this.addActor(this.inventoryGrid = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_5));
      this.inventoryGrid.setItemFilter(new ForgeRefineItemFilter(this));
      this.inventoryGrid.setPosition(1247.0F, 0.0F, 20);
   }

   public class ContentPanel extends GamePanel {
      public ItemSlotButton mainItemSlot;
      public ItemSlotButton materialSlot = new ItemSlotButton(this.renderContext);
      public SettingsToggleButton safeModeToggle;
      public SettingsToggleButton aggressiveModeToggle;
      public TextraLabel descriptionLabel;

      public ContentPanel(RenderContext renderContext) {
         super(renderContext);
         this.materialSlot.setSize(104.0F, 104.0F);
         this.materialSlot.setPosition(350.0F, 403.0F);
         this.addActor(this.materialSlot);
         this.materialSlot.addListener(new ForgeRefineInputSlotListener(this));
         this.mainItemSlot = new ItemSlotButton(super.renderContext).setLabelText("Đá khởi\nnguyên");
         this.mainItemSlot.setSize(104.0F, 104.0F);
         this.mainItemSlot.setPosition(158.0F, 403.0F);
         this.addActor(this.mainItemSlot);
         this.mainItemSlot.addListener(new ForgeRefineItemClickListener(this));
         this.safeModeToggle = new SettingsToggleButton("Tinh luyện đá mặt trời", super.renderContext);
         this.safeModeToggle.setSize(320.0F, 55.0F);
         this.safeModeToggle.setPosition(30.0F, 137.0F);
         this.addActor(this.safeModeToggle);
         this.safeModeToggle.addListener(new ForgeRefineMode0Listener(this));
         this.aggressiveModeToggle = new SettingsToggleButton("Tinh luyện đá mặt trăng", super.renderContext);
         this.aggressiveModeToggle.setSize(320.0F, 55.0F);
         this.aggressiveModeToggle.setPosition(30.0F, 75.0F);
         this.addActor(this.aggressiveModeToggle);
         this.aggressiveModeToggle.addListener(new ForgeRefineMode1Listener(this));
         this.descriptionLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 22, 3.0F));
         this.descriptionLabel.setAlignment(10);
         this.descriptionLabel.setWrap(true);
         this.descriptionLabel.setSize(610.0F, 180.0F);
         this.descriptionLabel.setPosition(0.0F, 206.0F);
         this.addActor(this.descriptionLabel);
         ForgeRefineActionButton refine1Button;
         (refine1Button = new ForgeRefineActionButton(this, "Tinh luyện 1", super.renderContext)).setSize(169.0F, 63.0F);
         refine1Button.setPosition(29.0F, 0.0F);
         this.addActor(refine1Button);
         refine1Button.addListener(new ForgeRefineActionListener(this));
         ForgeRefineActionButton2 refine10Button;
         (refine10Button = new ForgeRefineActionButton2(this, "Tinh luyện 10", super.renderContext)).setSize(169.0F, 63.0F);
         refine10Button.setPosition(213.0F, 0.0F);
         this.addActor(refine10Button);
         refine10Button.addListener(new ForgeRefineAutoFillListener(this));
         ForgeRefineThirdButton refine100Button;
         (refine100Button = new ForgeRefineThirdButton(this, "Tinh luyện 100", super.renderContext)).setSize(169.0F, 63.0F);
         refine100Button.setPosition(397.0F, 0.0F);
         this.addActor(refine100Button);
         refine100Button.addListener(new ForgeRefineQuantityMaxClickListener(this));
      }

      @Override
      public final void refresh() {
         ForgeRefineState state = GameState.forgeData.refineState;
         this.mainItemSlot.setItem(state.inputItem);
         this.materialSlot.setItem(state.resultItem);
         this.safeModeToggle.setChecked(((state.refineOption == 0 ? 1 : 0) != 0));
         int flag;
         if (state.refineOption == 1) {
            flag = 1;
         } else {
            flag = 0;
         }

         this.aggressiveModeToggle.setChecked((flag != 0));
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
