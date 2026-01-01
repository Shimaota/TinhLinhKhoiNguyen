package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.Item;
import com.tlnt.model.item.StatLine;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.CharacterModelActor;
import com.tlnt.ui.window.charinfo.CharacterPortraitPanel;
import com.tlnt.ui.window.charinfo.SettingsToggleButton;
import com.tlnt.world.entity.player.PlayerEntity;

public final class TitlePanel extends GamePanel {
   public TitlePanel.TitleListPanel titleListPanel = new TitlePanel.TitleListPanel(this.renderContext);
   public TitlePanel.TitleDetailPanel titleDetailPanel;

   public TitlePanel(RenderContext ctx) {
      super(ctx);
      this.titleListPanel.setSize(796.0F, 610.0F);
      this.addActor(this.titleListPanel);
      this.titleDetailPanel = new TitlePanel.TitleDetailPanel(super.renderContext);
      this.titleDetailPanel.setSize(433.0F, 610.0F);
      this.titleDetailPanel.setPosition(826.0F, 0.0F);
      this.addActor(this.titleDetailPanel);
   }

   public class TitleDetailPanel extends GamePanel {
      public TextraLabel titleLabel;
      public TextraLabel descriptionLabel;
      public SettingsToggleButton autoEquipCheckbox;

      public TitleDetailPanel(RenderContext ctx) {
         super(ctx);
         Table contentTable;
         Table contentRef = contentTable = new Table();
         String headerText = "[#8B4E23]Chỉ số cộng thêm";
         contentRef.add(this.titleLabel = new TextraLabel(headerText, FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 30, 2.0F)))
            .growX()
            .row();
         contentTable.add(
               this.descriptionLabel = new TextraLabel(
                  "", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 24, 2.5F)
               )
            )
            .grow();
         this.descriptionLabel.setWrap(true).setAlignment(10);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(contentTable)).setSize(433.0F, 547.0F);
         scrollPane.setPosition(0.0F, 63.0F);
         this.addActor(scrollPane);
         this.autoEquipCheckbox = new SettingsToggleButton("Ẩn danh hiệu", super.renderContext);
         this.autoEquipCheckbox.setSize(215.0F, 55.0F);
         this.addActor(this.autoEquipCheckbox);
         this.autoEquipCheckbox.addListener(new TitleToggleDisplayListener(this));
         ExpandTitleButton expandButton;
         (expandButton = new ExpandTitleButton(this, super.renderContext)).setSize(169.0F, 63.0F);
         expandButton.setPosition(270.0F, 0.0F);
         this.addActor(expandButton);
         expandButton.addListener(new TitleCancelButtonListener(this));
      }

      @Override
      public final void refresh() {
         StringBuilder sb;
         (sb = new StringBuilder()).append("[#8B4E23]");

         for (StatLine statLine : GameState.inventory.skillDescriptions2) {
            sb.append(statLine.formatText()).append("\n");
         }

         this.descriptionLabel.setText(sb.toString());
         this.autoEquipCheckbox.setChecked(GameState.inventory.inventoryFlag2);
      }
   }

   public class TitleListPanel extends GamePanel {
      public Pool<ItemSlotButton> titleItemPool = new TitleItemPool(this);
      public CharacterPortraitPanel portraitPanel = new CharacterPortraitPanel(this.renderContext);
      public ItemSlotButton equippedTitleButton;
      public CharacterModelActor characterModelActor;
      public Table itemListTable;

      public TitleListPanel(RenderContext ctx) {
         super(ctx);
         this.portraitPanel.setSize(279.0F, 128.0F);
         this.portraitPanel.setPosition(90.0F, 466.0F);
         this.addActor(this.portraitPanel);
         this.equippedTitleButton = new ItemSlotButton(super.renderContext);
         this.equippedTitleButton.setSize(104.0F, 104.0F);
         this.equippedTitleButton.setPosition(25.0F, 25.0F);
         this.addActor(this.equippedTitleButton);
         this.equippedTitleButton.addListener(new EquipTitleClickListener(this));
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.itemListTable = new Table().top().left().pad(6.0F))).setSize(344.0F, 566.0F);
         scrollPane.setPosition(433.0F, 25.0F);
         this.addActor(scrollPane);
         this.characterModelActor = new CharacterModelActor(super.renderContext);
         this.characterModelActor.setPosition(240.0F, 120.0F);
         this.addActor(this.characterModelActor);
      }

      @Override
      public final void refresh() {
         this.portraitPanel.setPlayerData(PlayerEntity.getInstance().POOL2);
         this.characterModelActor.loadSkeleton(PlayerEntity.getInstance().POOL2.skeletonKey, PlayerEntity.getInstance().POOL2.equipmentSkinKeys);
         this.equippedTitleButton.setItem(GameState.inventory.selectedArmorSlot);
         if (this.itemListTable.getChildren().size != GameState.inventory.armorSlots.size) {
            for (int i = this.itemListTable.getChildren().size - 1; i >= 0; i--) {
               Actor child;
               if ((child = this.itemListTable.getChildren().get(i)) instanceof ItemSlotButton) {
                  this.titleItemPool.free((ItemSlotButton)child);
               }
            }

            this.itemListTable.clear();

            for (Item titleItem : GameState.inventory.armorSlots) {
               this.itemListTable.add(this.titleItemPool.obtain()).size(104.0F).space(10.0F);
               if (this.itemListTable.getChildren().size % 3 == 0) {
                  this.itemListTable.row();
               }
            }
         }

         for (int j = 0; j < GameState.inventory.armorSlots.size; j++) {
            ((ItemSlotButton)this.itemListTable.getChild(j)).setItem(GameState.inventory.armorSlots.get(j));
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         String bgPath = "ui/100";
         super.renderContext
            .drawNinePatch(
               NinePatchAsset.obtain(bgPath, 50),
               this.getX(),
               this.getY(),
               this.getWidth(),
               this.getHeight(),
               Anchor.BOTTOM_LEFT
            );
         super.renderContext.drawTexture(TextureAsset.obtain("ui/101"), this.getX() + 65.0F, this.getY() + 104.0F, Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }
}
