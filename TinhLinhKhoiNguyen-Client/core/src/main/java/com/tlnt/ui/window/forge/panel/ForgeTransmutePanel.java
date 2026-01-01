package com.tlnt.ui.window.forge.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.forge.ForgeTransmuteState;
import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.TextButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

/**
 * The forge "Transmute" tab (port of decompiled C3694): slots "Item to transmute" (112, 436), "New item" (400, 436),
 * "Transmute charm" (256, 272), a description line, the "Transmute" button (op 44 mode 6 action 2). The bag only shows type 3-10 equipment +
 * Transmute charm 331; clicking a slot -> "Take out" (op 44 action 1 = remove from the slot).
 */
public final class ForgeTransmutePanel extends GamePanel {
   /** Item 331 Transmute charm. */
   public static final int TRANSMUTE_CHARM = 331;

   public final ForgeTransmutePanel.ControlPanel controlPanel;
   public final InventoryGridPanel inventoryGridPanel;

   public ForgeTransmutePanel(RenderContext renderContext) {
      super(renderContext);
      this.addActor(this.controlPanel = new ForgeTransmutePanel.ControlPanel(super.renderContext));
      this.controlPanel.setSize(610.0F, 610.0F);
      this.addActor(this.inventoryGridPanel = new InventoryGridPanel(super.renderContext, InventoryGridPanel.EquipSlotType.SLOT_TYPE_5));
      this.inventoryGridPanel.setItemFilter(new TransmuteItemFilter());
      this.inventoryGridPanel.setPosition(1247.0F, 0.0F, 20);
   }

   /** Bag: type 3-10 equipment (decompiled C3694 filter) + Transmute charm. */
   static final class TransmuteItemFilter implements InventoryGridPanel.ItemFilter {
      @Override
      public boolean isItemAccepted(Item item) {
         switch (item.template.type.id) {
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
               return true;
            default:
               return item.template.id == TRANSMUTE_CHARM;
         }
      }
   }

   /** Clicking a slot: shows item info + the "Take out" button. */
   static final class SlotListener extends ClickListener {
      private final ItemSlotButton slot;

      SlotListener(ItemSlotButton slot) {
         this.slot = slot;
      }

      @Override
      public void clicked(InputEvent event, float x, float y) {
         ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
         Item item = this.slot.getItem();
         ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
         actions[0] = ItemActionRegistry.DONATE_TO_TRIBE_HANDLER;
         popup.showItemWithActions(this.slot, item, ItemActionRegistry.DONATE_TO_TRIBE_HANDLER, actions);
      }
   }

   /** ui/10-12 frame button (like the other forge tabs). */
   static final class ActionButton extends TextButton {
      private final CompositeSprite sprite = CompositeSprite.obtain("ui/10", "ui/11", "ui/12", false);

      ActionButton(String text, RenderContext renderContext) {
         super(text, renderContext);
      }

      @Override
      public void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawCompositeSprite(this.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }

      @Override
      public void drawLabel() {
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               super.text,
               this.getX() + this.getWidth() / 2.05F,
               this.getY() + this.getHeight() / 1.8F,
               Anchor.CENTER,
               24.0F,
               Color.WHITE
            );
      }
   }

   /** "Transmute" button: op 44 (current mode, 2, int 1). */
   static final class ExecuteListener extends ClickListener {
      @Override
      public void clicked(InputEvent event, float x, float y) {
         ServerPacketSender.getInstance().sendPvpAccept(1);
      }
   }

   public final class ControlPanel extends GamePanel {
      public final ItemSlotButton sourceSlot;
      public final ItemSlotButton targetSlot;
      public final ItemSlotButton charmSlot;
      public final TextraLabel descriptionLabel;

      public ControlPanel(RenderContext renderContext) {
         super(renderContext);
         this.sourceSlot = this.slot("Trang bị\nchuyển\nhóa", 112.0F, 436.0F);
         this.targetSlot = this.slot("Trang bị\nmới", 400.0F, 436.0F);
         this.charmSlot = this.slot("Bùa\nchuyển\nhóa", 256.0F, 272.0F);
         this.descriptionLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 22, 3.0F));
         this.descriptionLabel.setAlignment(10);
         this.descriptionLabel.setWrap(true);
         this.descriptionLabel.setSize(610.0F, 180.0F);
         this.descriptionLabel.setPosition(0.0F, 80.0F);
         this.addActor(this.descriptionLabel);
         ActionButton button = new ActionButton("Chuyển hóa", super.renderContext);
         button.setSize(169.0F, 63.0F);
         button.setPosition(231.0F, 0.0F);
         this.addActor(button);
         button.addListener(new ExecuteListener());
      }

      private ItemSlotButton slot(String label, float x, float y) {
         ItemSlotButton slot = new ItemSlotButton(super.renderContext).setLabelText(label);
         slot.setSize(104.0F, 104.0F);
         slot.setPosition(x, y);
         this.addActor(slot);
         slot.addListener(new SlotListener(slot));
         return slot;
      }

      @Override
      public final void refresh() {
         ForgeTransmuteState state = GameState.forgeData.transmuteState;
         this.sourceSlot.setItem(state.sourceItem);
         this.targetSlot.setItem(state.targetItem);
         this.charmSlot.setItem(state.charmItem);
         this.descriptionLabel.setText(state.infoText);
         if (state.resultStatus != -1) {
            this.targetSlot.enchantEffect.playEnchantResult(state.resultStatus == 1);
            state.resultStatus = (byte)-1;
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawTexture(TextureAsset.obtain("ui/167"), this.getX() + 36.0F, this.getY() + 93.0F, Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }
}
