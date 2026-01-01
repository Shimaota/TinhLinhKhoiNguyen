package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.GameMain;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.Item;
import com.tlnt.model.item.StatLine;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.CharacterModelActor;
import com.tlnt.ui.window.charinfo.CharacterPortraitPanel;
import com.tlnt.ui.window.charinfo.SettingsToggleButton;
import com.tlnt.world.entity.player.PlayerEntity;

/** Backwear tab layout per C3719: 796x610 equipment area and 433x610 stats area. */
public final class BackwearPanel extends GamePanel {
   public final BackwearPanel.BackwearItemsPanel itemsPanel;
   public final BackwearPanel.BackwearDetailsPanel detailsPanel;

   public BackwearPanel(RenderContext ctx) {
      super(ctx);
      this.itemsPanel = new BackwearPanel.BackwearItemsPanel(ctx);
      this.itemsPanel.setSize(796.0F, 610.0F);
      this.addActor(this.itemsPanel);
      this.detailsPanel = new BackwearPanel.BackwearDetailsPanel(ctx);
      this.detailsPanel.setSize(433.0F, 610.0F);
      this.detailsPanel.setPosition(826.0F, 0.0F);
      this.addActor(this.detailsPanel);
   }

   public final class BackwearItemsPanel extends GamePanel {
      public final Pool<ItemSlotButton> itemPool = new Pool<ItemSlotButton>() {
         @Override
         protected ItemSlotButton newObject() {
            ItemSlotButton button = new ItemSlotButton(BackwearPanel.super.renderContext);
            button.addListener(new BackwearItemClickListener(button));
            return button;
         }
      };
      public final CharacterPortraitPanel portrait = new CharacterPortraitPanel(this.renderContext);
      public final ItemSlotButton selectedItem = new ItemSlotButton(this.renderContext);
      public final CharacterModelActor characterModel = new CharacterModelActor(this.renderContext);
      public final Table itemGrid = new Table().top().left().pad(6.0F);

      public BackwearItemsPanel(RenderContext ctx) {
         super(ctx);
         this.portrait.setSize(279.0F, 128.0F);
         this.portrait.setPosition(90.0F, 466.0F);
         this.addActor(this.portrait);
         this.selectedItem.setSize(104.0F, 104.0F);
         this.selectedItem.setPosition(25.0F, 25.0F);
         this.selectedItem.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
               Item item = BackwearItemsPanel.this.selectedItem.getItem();
               if (item == null) {
                  return;
               }
               ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
               popup.showItemWithActions(
                  BackwearItemsPanel.this.selectedItem,
                  item,
                  BackwearUnequipAction.INSTANCE,
                  BackwearUnequipAction.INSTANCE,
                  ItemActionRegistry.CLOSE_TOOLTIP_ACTION
               );
            }
         });
         this.addActor(this.selectedItem);
         ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.itemGrid);
         scrollPane.setSize(344.0F, 566.0F);
         scrollPane.setPosition(433.0F, 25.0F);
         this.addActor(scrollPane);
         this.characterModel.setPosition(240.0F, 120.0F);
         this.addActor(this.characterModel);
      }

      /**
       * Called every time the client receives a packet (refreshHudState -> refreshVisibleActors), like PetPanel. It used to update only in onShow:
       * after "Use / Unequip / Take out" the slot kept the old Item that had been Item.clear()ed (template = null) -> clicking it threw an NPE.
       */
      @Override
      public void refresh() {
         this.refreshBackwear();
      }

      @Override
      public void onShow() {
         this.refreshBackwear();
      }

      private void refreshBackwear() {
         this.portrait.setPlayerData(PlayerEntity.getInstance().POOL2);
         this.characterModel.loadSkeleton(
            PlayerEntity.getInstance().POOL2.skeletonKey,
            PlayerEntity.getInstance().POOL2.equipmentSkinKeys
         );
         this.selectedItem.setItem(GameState.inventory.selectedBackpackSlot);
         if (this.itemGrid.getChildren().size != GameState.inventory.backpackSlots.size) {
            for (int i = this.itemGrid.getChildren().size - 1; i >= 0; i--) {
               Actor child = this.itemGrid.getChildren().get(i);
               if (child instanceof ItemSlotButton) {
                  this.itemPool.free((ItemSlotButton)child);
               }
            }
            this.itemGrid.clear();
            for (int i = 0; i < GameState.inventory.backpackSlots.size; i++) {
               this.itemGrid.add(this.itemPool.obtain()).size(104.0F).space(10.0F);
               if ((i + 1) % 3 == 0) {
                  this.itemGrid.row();
               }
            }
         }
         for (int i = 0; i < GameState.inventory.backpackSlots.size; i++) {
            ((ItemSlotButton)this.itemGrid.getChild(i)).setItem(GameState.inventory.backpackSlots.get(i));
         }
      }

      private final class BackwearItemClickListener extends ClickListener {
         private final ItemSlotButton sourceButton;

         private BackwearItemClickListener(ItemSlotButton sourceButton) {
            this.sourceButton = sourceButton;
         }

         @Override
         public void clicked(InputEvent event, float x, float y) {
            Item item = this.sourceButton.getItem();
            if (item == null) {
               return;
            }
            ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
            popup.showItemWithActions(
               this.sourceButton,
               item,
               BackwearUseAction.INSTANCE,
               BackwearUseAction.INSTANCE,
               BackwearTakeOutAction.INSTANCE, // decompiled C3719: list slot = "Use" + "Take out" + "Close"
               ItemActionRegistry.CLOSE_TOOLTIP_ACTION
            );
         }
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(
            NinePatchAsset.obtain("ui/100", 50), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
         super.renderContext.drawTexture(
            TextureAsset.obtain("ui/101"), this.getX() + 65.0F, this.getY() + 104.0F, Anchor.BOTTOM_LEFT
         );
         super.draw(batch, parentAlpha);
      }
   }

   public final class BackwearDetailsPanel extends GamePanel {
      public final TextraLabel statLabel;
      public final SettingsToggleButton hideBackwearButton;

      public BackwearDetailsPanel(RenderContext ctx) {
         super(ctx);
         Table content = new Table();
         content.add(new TextraLabel(
            "[#8B4E23]Chỉ số cộng thêm", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 30, 2.0F)
         )).growX().row();
         this.statLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 24, 2.5F));
         content.add(this.statLabel).grow();
         this.statLabel.setWrap(true).setAlignment(10);
         ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(content);
         scrollPane.setSize(433.0F, 547.0F);
         scrollPane.setPosition(0.0F, 63.0F);
         this.addActor(scrollPane);
         this.hideBackwearButton = new SettingsToggleButton("Ẩn đeo lưng", ctx);
         this.hideBackwearButton.setSize(215.0F, 55.0F);
         this.addActor(this.hideBackwearButton);
         this.hideBackwearButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
               GameState.inventory.hideBackpack = BackwearDetailsPanel.this.hideBackwearButton.isChecked();
               ServerPacketSender.getInstance().sendToggleBackwearHidden(GameState.inventory.hideBackpack);
            }
         });
         BackwearExpandButton expandButton = new BackwearExpandButton(ctx);
         expandButton.setSize(169.0F, 63.0F);
         expandButton.setPosition(270.0F, 0.0F);
         this.addActor(expandButton);
         expandButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
               ServerPacketSender.getInstance().sendExpandBackwearSlots();
            }
         });
      }

      @Override
      public void onShow() {
         this.refresh();
      }

      @Override
      public void refresh() {
         StringBuilder text = new StringBuilder("[#8B4E23]");
         for (StatLine stat : GameState.inventory.backpackSkillDescriptions) {
            text.append(stat.formatText()).append('\n');
         }
         this.statLabel.setText(text.toString());
         this.hideBackwearButton.setChecked(GameState.inventory.hideBackpack);
      }
   }

   private static final class BackwearExpandButton extends ShapeButton {
      private final CompositeSprite sprite = CompositeSprite.obtain("ui/10", "ui/11", "ui/12", false);

      private BackwearExpandButton(RenderContext ctx) {
         super(ctx);
      }

      @Override
      public void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(com.badlogic.gdx.graphics.Color.GRAY);
         }
         super.renderContext.drawCompositeSprite(this.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.drawText(
            FontManager.DEFAULT_FONT,
            "Mở rộng",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 1.8F,
            Anchor.CENTER,
            24.0F,
            com.badlogic.gdx.graphics.Color.WHITE
         );
         super.renderContext.resetBatchColor();
      }
   }

   private static final class BackwearUseAction extends ItemInfoPopup.ActionConfig {
      private static final BackwearUseAction INSTANCE = new BackwearUseAction();

      private BackwearUseAction() {
         super("Dùng");
      }

      @Override
      public void onItemAction(Item item) {
         if (item != null) {
            ServerPacketSender.getInstance().sendUseBackwear(item.slotIndex);
         }
      }
   }

   private static final class BackwearUnequipAction extends ItemInfoPopup.ActionConfig {
      private static final BackwearUnequipAction INSTANCE = new BackwearUnequipAction();

      private BackwearUnequipAction() {
         super("Tháo");
      }

      @Override
      public void onItemAction(Item item) {
         if (item != null) {
            ServerPacketSender.getInstance().sendUnequipBackwear();
         }
      }
   }

   private static final class BackwearTakeOutAction extends ItemInfoPopup.ActionConfig {
      private static final BackwearTakeOutAction INSTANCE = new BackwearTakeOutAction();

      private BackwearTakeOutAction() {
         super("Lấy ra");
      }

      @Override
      public void onItemAction(Item item) {
         if (item != null) {
            ServerPacketSender.getInstance().sendTakeBackwear(item.slotIndex);
         }
      }
   }
}
