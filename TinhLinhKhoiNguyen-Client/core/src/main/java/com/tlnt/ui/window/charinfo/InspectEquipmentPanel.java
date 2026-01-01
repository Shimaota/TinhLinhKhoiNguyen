package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.Item;
import com.tlnt.model.item.ItemType;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;

public final class InspectEquipmentPanel extends GamePanel {
   public Pool<ItemSlotButton> equipSlotPool = new EquipSlotButtonPool(this);
   public NinePatchAsset background;
   public CharacterPortraitPanel portraitPanel;
   public Table leftSlotsTable;
   public Table rightSlotsTable;
   public Array<ItemSlotButton> equipSlots;
   public CharacterModelActor characterModel;
   public InspectEquipmentPanel.PortraitButton setPieceBonusOverlay;
   public InspectEquipmentPanel.ItemDescriptionPanel equipInfoOverlay;

   public InspectEquipmentPanel(RenderContext renderContext) {
      super(renderContext);
      this.background = NinePatchAsset.obtain("ui/100", 50);
      this.setSize(616.0F, 610.0F);
      InspectEquipmentPanel self = this;
      this.portraitPanel = new CharacterPortraitPanel(super.renderContext);
      this.portraitPanel.setSize(279.0F, 128.0F);
      this.portraitPanel.setPosition(164.0F, 466.0F);
      this.addActor(this.portraitPanel);
      this.leftSlotsTable = new Table().top();
      this.leftSlotsTable.setSize(104.0F, 560.0F);
      this.leftSlotsTable.setPosition(15.0F, 25.0F);
      this.addActor(this.leftSlotsTable);
      this.rightSlotsTable = new Table().top();
      this.rightSlotsTable.setSize(104.0F, 560.0F);
      this.rightSlotsTable.setPosition(497.0F, 25.0F);
      this.addActor(this.rightSlotsTable);
      this.equipSlots = new Array<>();
      int i = 0;

      while (true) {
         if (i >= 10) {
            self.addActor(self.characterModel = new CharacterModelActor(self.renderContext));
            self.characterModel.setPosition(self.getWidth() / 2.0F, 120.0F);
            self.addActor(self.setPieceBonusOverlay = self.new PortraitButton(self.renderContext));
            self.addActor(self.equipInfoOverlay = self.new ItemDescriptionPanel(self.renderContext));
            return;
         }

         ItemSlotButton slot = self.equipSlotPool.obtain();
         if (i % 2 == 0) {
            self.leftSlotsTable.add(slot).size(104.0F).space(10.0F).row();
         } else {
            self.rightSlotsTable.add(slot).size(104.0F).space(10.0F).row();
         }

         self.equipSlots.add(slot);
         i++;
      }
   }

   @Override
   public final void refresh() {
      this.portraitPanel.setPlayerData(GameState.wardrobeData.playerStats);

      for (int i = 0; i < GameState.wardrobeData.equipmentSlots.size; i++) {
         Item item = GameState.wardrobeData.equipmentSlots.get(i);
         this.equipSlots.get(i).setItem(item);
         ItemType itemType = ItemType.getByEquipSlot(i);
         this.equipSlots.get(i).setLabelText(itemType != null ? itemType.name : "");
      }

      this.characterModel.loadSkeleton(GameState.wardrobeData.playerStats.skeletonKey, GameState.wardrobeData.playerStats.equipmentSkinKeys);
      int hasTitle;
      if (GameState.wardrobeData.playerStats.titleData != null) {
         hasTitle = 1;
      } else {
         hasTitle = 0;
      }

      this.setPieceBonusOverlay.setVisible((hasTitle != 0));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.drawTexture(TextureAsset.obtain("ui/101"), this.getX() + 138.0F, this.getY() + 104.0F, Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

   public class ItemDescriptionPanel extends GamePanel {
      public Pool<TextraLabel> labelPool = new ItemDescriptionLabelPool(this);
      public NinePatchAsset backgroundPatch;
      public Table contentTable;

      public ItemDescriptionPanel(RenderContext renderContext) {
         super(renderContext);
         String bgPath = "ui/171";
         this.backgroundPatch = NinePatchAsset.obtain(bgPath, 50);
         this.setSize(262.0F, 197.0F);
         this.setPosition(220.0F, 25.0F);
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.contentTable = new Table().top().left().padBottom(20.0F))).setSize(230.0F, 130.0F);
         scrollPane.setPosition(16.0F, 20.0F);
         this.addActor(scrollPane);
         this.setVisible(false);
         this.setTouchable(Touchable.enabled);
      }

      @Override
      public final void refresh() {
         Array.ArrayIterator it = this.contentTable.getChildren().iterator();

         while (it.hasNext()) {
            Actor child;
            if ((child = (Actor)it.next()) instanceof TextraLabel) {
               this.labelPool.free((TextraLabel)child);
            }
         }

         this.contentTable.clear();
         if (GameState.wardrobeData.playerStats.titleData != null) {
            String[] lines = GameState.wardrobeData.playerStats.titleData.descriptionLines;
            int count = GameState.wardrobeData.playerStats.titleData.descriptionLines.length;

            for (int i = 0; i < count; i++) {
               String line = lines[i];
               TextraLabel label;
               (label = this.labelPool.obtain()).setText(line);
               this.contentTable.add(label).growX().space(30.0F).row();
            }
         }
      }

      @Override
      public final void setStage(Stage stage) {
         super.setStage(stage);
         if (stage != null) {
            stage.addCaptureListener(new ItemDescriptionPopupDismissListener(this, stage));
         }
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         if (GameState.wardrobeData.playerStats.titleData != null) {
            super.renderContext
               .drawText(
                  FontManager.PLAIN_FONT,
                  GameState.wardrobeData.playerStats.titleData.name,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + 165.0F,
                  Anchor.BOTTOM_CENTER,
                  20.0F,
                  Color.WHITE
               );
         }

         super.draw(batch, parentAlpha);
      }
   }

   public class PortraitButton extends ShapeButton {

      public final InspectEquipmentPanel inspectEquipmentPanel = InspectEquipmentPanel.this;
      public PortraitButton(RenderContext renderContext) {
         super(renderContext);
         this.setSize(85.0F, 85.0F);
         this.setPosition(130.0F, 30.0F);
         this.addListener(new InspectPortraitClickListener(this, InspectEquipmentPanel.this));
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain(GameState.wardrobeData.playerStats.titleData.textureKey),
               this.getX(),
               this.getY(),
               this.getWidth(),
               this.getHeight(),
               Anchor.BOTTOM_LEFT
            );
         super.renderContext.resetBatchColor();
      }
   }
}
