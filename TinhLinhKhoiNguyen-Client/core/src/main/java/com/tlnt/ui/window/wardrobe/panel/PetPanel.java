package com.tlnt.ui.window.wardrobe.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
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

public final class PetPanel extends GamePanel {
   public PetPanel.EquipGridPanel selectionPanel = new PetPanel.EquipGridPanel(this.renderContext);
   public PetPanel.DescriptionPanel detailPanel;

   public PetPanel(RenderContext renderContext) {
      super(renderContext);
      this.selectionPanel.setSize(796.0F, 610.0F);
      this.addActor(this.selectionPanel);
      this.detailPanel = new PetPanel.DescriptionPanel(super.renderContext);
      this.detailPanel.setSize(433.0F, 610.0F);
      this.detailPanel.setPosition(826.0F, 0.0F);
      this.addActor(this.detailPanel);
   }

   public class DescriptionPanel extends GamePanel {
      public TextraLabel titleLabel;
      public TextraLabel descriptionLabel;

      public DescriptionPanel(RenderContext renderContext) {
         super(renderContext);
         Table table;
         (table = new Table())
            .add(
               this.titleLabel = new TextraLabel(
                  "[#8B4E23]Chỉ số cộng thêm", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 30, 2.0F)
               )
            )
            .growX()
            .row();
         table.add(
               this.descriptionLabel = new TextraLabel(
                  "", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 24, 2.5F)
               )
            )
            .grow();
         TextraLabel descLabel = this.descriptionLabel.setWrap(true);
         descLabel.setAlignment(10);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(table)).setSize(433.0F, 547.0F);
         scrollPane.setPosition(0.0F, 63.0F);
         this.addActor(scrollPane);
      }

      @Override
      public final void refresh() {
         StringBuilder sb;
         (sb = new StringBuilder()).append("[#8B4E23]");

         for (StatLine statLine : GameState.wardrobeData.skillDescriptions4) {
            sb.append(statLine.formatText()).append("\n");
         }

         this.descriptionLabel.setText(sb.toString());
      }
   }

   public class EquipGridPanel extends GamePanel {
      public Pool<ItemSlotButton> itemPool = new PetItemPool(this);
      public CharacterPortraitPanel portraitPanel = new CharacterPortraitPanel(this.renderContext);
      public ItemSlotButton equippedItemSlot;
      public CharacterModelActor characterModel;
      public Table itemGrid;

      public EquipGridPanel(RenderContext renderContext) {
         super(renderContext);
         this.portraitPanel.setSize(279.0F, 128.0F);
         this.portraitPanel.setPosition(90.0F, 466.0F);
         this.addActor(this.portraitPanel);
         this.equippedItemSlot = new ItemSlotButton(super.renderContext);
         this.equippedItemSlot.setSize(104.0F, 104.0F);
         this.equippedItemSlot.setPosition(25.0F, 25.0F);
         this.addActor(this.equippedItemSlot);
         this.equippedItemSlot.addListener(new PetEquipSlotClickListener(this));
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.itemGrid = new Table().top().left().pad(6.0F))).setSize(344.0F, 566.0F);
         scrollPane.setPosition(433.0F, 25.0F);
         this.addActor(scrollPane);
         this.characterModel = new CharacterModelActor(super.renderContext);
         this.characterModel.setPosition(240.0F, 120.0F);
         this.addActor(this.characterModel);
      }

      @Override
      public final void refresh() {
         this.portraitPanel.setPlayerData(GameState.wardrobeData.playerStats);
         this.characterModel.loadSkeleton(GameState.wardrobeData.playerStats.skeletonKey, GameState.wardrobeData.playerStats.equipmentSkinKeys);
         this.equippedItemSlot.setItem(GameState.wardrobeData.selectedItem4);
         if (this.itemGrid.getChildren().size != GameState.wardrobeData.bagSlots4.size) {
            Array.ArrayIterator it = this.itemGrid.getChildren().iterator();

            while (it.hasNext()) {
               Actor child;
               if ((child = (Actor)it.next()) instanceof ItemSlotButton) {
                  this.itemPool.free((ItemSlotButton)child);
               }
            }

            this.itemGrid.clear();

            for (Item each : GameState.wardrobeData.bagSlots4) {
               this.itemGrid.add(this.itemPool.obtain()).size(104.0F).space(10.0F);
               if (this.itemGrid.getChildren().size % 3 == 0) {
                  this.itemGrid.row();
               }
            }
         }

         for (int i = 0; i < GameState.wardrobeData.bagSlots4.size; i++) {
            ((ItemSlotButton)this.itemGrid.getChild(i)).setItem(GameState.wardrobeData.bagSlots4.get(i));
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext
            .drawNinePatch(
               NinePatchAsset.obtain("ui/100", 50), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
            );
         super.renderContext
            .drawTexture(TextureAsset.obtain("ui/101"), this.getX() + 65.0F, this.getY() + 104.0F, Anchor.BOTTOM_LEFT);
         super.draw(batch, parentAlpha);
      }
   }
}
