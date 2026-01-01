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

public final class CostumePanel extends GamePanel {
   public CostumePanel.CostumeListPanel costumeListPanel = new CostumePanel.CostumeListPanel(this.renderContext);
   public CostumePanel.CostumeDetailPanel costumeDetailPanel;

   public CostumePanel(RenderContext ctx) {
      super(ctx);
      this.costumeListPanel.setSize(796.0F, 610.0F);
      this.addActor(this.costumeListPanel);
      this.costumeDetailPanel = new CostumePanel.CostumeDetailPanel(super.renderContext);
      this.costumeDetailPanel.setSize(433.0F, 610.0F);
      this.costumeDetailPanel.setPosition(826.0F, 0.0F);
      this.addActor(this.costumeDetailPanel);
   }

   public class CostumeDetailPanel extends GamePanel {
      public TextraLabel costumeNameLabel;
      public TextraLabel costumeDescLabel;
      public SettingsToggleButton toggleDisplayCheckbox;

      public CostumeDetailPanel(RenderContext ctx) {
         super(ctx);
         Table contentTable;
         Table contentRef = contentTable = new Table();
         String headerText = "[#8B4E23]Chỉ số cộng thêm";
         contentRef.add(this.costumeNameLabel = new TextraLabel(headerText, FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 30, 2.0F)))
            .growX()
            .row();
         contentTable.add(this.costumeDescLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 24, 2.5F))).grow();
         TextraLabel descLabel = this.costumeDescLabel.setWrap(true);
         descLabel.setAlignment(10);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(contentTable)).setSize(433.0F, 547.0F);
         scrollPane.setPosition(0.0F, 63.0F);
         this.addActor(scrollPane);
         this.toggleDisplayCheckbox = new SettingsToggleButton("Ẩn ngoại trang", super.renderContext);
         this.toggleDisplayCheckbox.setSize(215.0F, 55.0F);
         this.addActor(this.toggleDisplayCheckbox);
         this.toggleDisplayCheckbox.addListener(new CostumeToggleDisplayListener(this));
         ExpandCostumeButton expandButton;
         (expandButton = new ExpandCostumeButton(this, super.renderContext)).setSize(169.0F, 63.0F);
         expandButton.setPosition(270.0F, 0.0F);
         this.addActor(expandButton);
         expandButton.addListener(new CostumeEquipActionListener(this));
      }

      @Override
      public final void refresh() {
         StringBuilder sb;
         (sb = new StringBuilder()).append("[#8B4E23]");

         for (StatLine statLine : GameState.inventory.skillDescriptions1) {
            sb.append(statLine.formatText()).append("\n");
         }

         this.costumeDescLabel.setText(sb.toString());
         this.toggleDisplayCheckbox.setChecked(GameState.inventory.inventoryFlag1);
      }
   }

   public class CostumeListPanel extends GamePanel {
      public Pool<ItemSlotButton> costumeItemPool = new CostumeItemPool(this);
      public CharacterPortraitPanel characterPortrait = new CharacterPortraitPanel(this.renderContext);
      public ItemSlotButton selectedCostumeItem;
      public CharacterModelActor characterModel;
      public Table costumeTable;

      public CostumeListPanel(RenderContext ctx) {
         super(ctx);
         this.characterPortrait.setSize(279.0F, 128.0F);
         this.characterPortrait.setPosition(90.0F, 466.0F);
         this.addActor(this.characterPortrait);
         this.selectedCostumeItem = new ItemSlotButton(super.renderContext);
         this.selectedCostumeItem.setSize(104.0F, 104.0F);
         this.selectedCostumeItem.setPosition(25.0F, 25.0F);
         this.addActor(this.selectedCostumeItem);
         this.selectedCostumeItem.addListener(new EquipCostumeClickListener(this));
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.costumeTable = new Table().top().left().pad(6.0F))).setSize(344.0F, 566.0F);
         scrollPane.setPosition(433.0F, 25.0F);
         this.addActor(scrollPane);
         this.characterModel = new CharacterModelActor(super.renderContext);
         this.characterModel.setPosition(240.0F, 120.0F);
         this.addActor(this.characterModel);
      }

      @Override
      public final void refresh() {
         this.characterPortrait.setPlayerData(PlayerEntity.getInstance().POOL2);
         this.characterModel.loadSkeleton(PlayerEntity.getInstance().POOL2.skeletonKey, PlayerEntity.getInstance().POOL2.equipmentSkinKeys);
         this.selectedCostumeItem.setItem(GameState.inventory.selectedWeaponSlot);
         if (this.costumeTable.getChildren().size != GameState.inventory.weaponSlots.size) {
            for (int i = this.costumeTable.getChildren().size - 1; i >= 0; i--) {
               Actor child;
               if ((child = this.costumeTable.getChildren().get(i)) instanceof ItemSlotButton) {
                  this.costumeItemPool.free((ItemSlotButton)child);
               }
            }

            this.costumeTable.clear();

            for (Item costumeItem : GameState.inventory.weaponSlots) {
               this.costumeTable.add(this.costumeItemPool.obtain()).size(104.0F).space(10.0F);
               if (this.costumeTable.getChildren().size % 3 == 0) {
                  this.costumeTable.row();
               }
            }
         }

         for (int j = 0; j < GameState.inventory.weaponSlots.size; j++) {
            ((ItemSlotButton)this.costumeTable.getChild(j)).setItem(GameState.inventory.weaponSlots.get(j));
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
         super.renderContext
            .drawTexture(TextureAsset.obtain("ui/101"), this.getX() + 65.0F, this.getY() + 104.0F, Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }
}
