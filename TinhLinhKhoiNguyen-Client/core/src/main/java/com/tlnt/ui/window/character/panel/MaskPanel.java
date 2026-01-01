package com.tlnt.ui.window.character.panel;

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

public final class MaskPanel extends GamePanel {
   public MaskPanel.MaskEquipPanel equipView = new MaskPanel.MaskEquipPanel(this.renderContext);
   public MaskPanel.MaskSkillPanel infoPanel;

   public MaskPanel(RenderContext renderContext) {
      super(renderContext);
      this.equipView.setSize(796.0F, 610.0F);
      this.addActor(this.equipView);
      this.infoPanel = new MaskPanel.MaskSkillPanel(super.renderContext);
      this.infoPanel.setSize(433.0F, 610.0F);
      this.infoPanel.setPosition(826.0F, 0.0F);
      this.addActor(this.infoPanel);
   }

   public class MaskEquipPanel extends GamePanel {
      public Pool<ItemSlotButton> itemButtonPool = new MaskItemButtonPool(this);
      public CharacterPortraitPanel characterPortraitPanel = new CharacterPortraitPanel(this.renderContext);
      public ItemSlotButton selectedItemButton;
      public CharacterModelActor characterModelActor;
      public Table itemGridTable;

      public MaskEquipPanel(RenderContext renderContext) {
         super(renderContext);
         this.characterPortraitPanel.setSize(279.0F, 128.0F);
         this.characterPortraitPanel.setPosition(90.0F, 466.0F);
         this.addActor(this.characterPortraitPanel);
         this.selectedItemButton = new ItemSlotButton(super.renderContext);
         this.selectedItemButton.setSize(104.0F, 104.0F);
         this.selectedItemButton.setPosition(25.0F, 25.0F);
         this.addActor(this.selectedItemButton);
         this.selectedItemButton.addListener(new MaskItemUnequipClickListener(this));
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.itemGridTable = new Table().top().left().pad(6.0F))).setSize(344.0F, 566.0F);
         scrollPane.setPosition(433.0F, 25.0F);
         this.addActor(scrollPane);
         this.characterModelActor = new CharacterModelActor(super.renderContext);
         this.characterModelActor.setPosition(240.0F, 120.0F);
         this.addActor(this.characterModelActor);
      }

      @Override
      public final void refresh() {
         this.characterPortraitPanel.setPlayerData(PlayerEntity.getInstance().POOL2);
         this.characterModelActor.loadSkeleton(PlayerEntity.getInstance().POOL2.skeletonKey, PlayerEntity.getInstance().POOL2.equipmentSkinKeys);
         this.selectedItemButton.setItem(GameState.inventory.selectedAccessorySlot);
         if (this.itemGridTable.getChildren().size != GameState.inventory.accessorySlots.size) {

            for (int i = this.itemGridTable.getChildren().size - 1; i >= 0; i--) {
               Actor child;
               if ((child = this.itemGridTable.getChildren().get(i)) instanceof ItemSlotButton) {
                  this.itemButtonPool.free((ItemSlotButton)child);
               }
            }

            this.itemGridTable.clear();

            for (Item each : GameState.inventory.accessorySlots) {
               this.itemGridTable.add(this.itemButtonPool.obtain()).size(104.0F).space(10.0F);
               if (this.itemGridTable.getChildren().size % 3 == 0) {
                  this.itemGridTable.row();
               }
            }
         }

         for (int j = 0; j < GameState.inventory.accessorySlots.size; j++) {
            ((ItemSlotButton)this.itemGridTable.getChild(j)).setItem(GameState.inventory.accessorySlots.get(j));
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         String bgPath = "ui/100";
         super.renderContext
            .drawNinePatch(NinePatchAsset.obtain(bgPath, 50), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawTexture(TextureAsset.obtain("ui/101"), this.getX() + 65.0F, this.getY() + 104.0F, Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }

   public class MaskSkillPanel extends GamePanel {
      public TextraLabel titleLabel;
      public TextraLabel skillListLabel;
      public SettingsToggleButton autoToggleCheckbox;

      public MaskSkillPanel(RenderContext renderContext) {
         super(renderContext);
         Table table;
         (table = new Table())
            .add(
               this.titleLabel = new TextraLabel(
                  "[#8B4E23]Chỉ số cộng thêm",
                  FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 30, 2.0F)
               )
            )
            .growX()
            .row();
         table.add(
               this.skillListLabel = new TextraLabel(
                  "", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 24, 2.5F)
               )
            )
            .grow();
         this.skillListLabel.setWrap(true).setAlignment(10);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(table)).setSize(433.0F, 547.0F);
         scrollPane.setPosition(0.0F, 63.0F);
         this.addActor(scrollPane);
         this.autoToggleCheckbox = new SettingsToggleButton("Ẩn mặt nạ", super.renderContext);
         this.autoToggleCheckbox.setSize(215.0F, 55.0F);
         this.addActor(this.autoToggleCheckbox);
         this.autoToggleCheckbox.addListener(new MaskAutoToggleClickListener(this));
         MaskExpandButton expandButton;
         (expandButton = new MaskExpandButton(this, super.renderContext)).setSize(169.0F, 63.0F);
         expandButton.setPosition(270.0F, 0.0F);
         this.addActor(expandButton);
         expandButton.addListener(new MaskExpandClickListener(this));
      }

      @Override
      public final void refresh() {
         StringBuilder sb;
         StringBuilder appended = sb = new StringBuilder();
         appended.append("[#8B4E23]");

         for (StatLine statLine : GameState.inventory.skillDescriptions3) {
            appended = sb.append(statLine.formatText());
            appended.append("\n");
         }

         this.skillListLabel.setText(sb.toString());
         this.autoToggleCheckbox.setChecked(GameState.inventory.inventoryFlag3);
      }
   }
}
