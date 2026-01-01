package com.tlnt.ui.window.wardrobe.panel;

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

/** Read-only view of backwear info, per C3733 registered by C3875. */
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
            return new ItemSlotButton(BackwearPanel.super.renderContext);
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
         this.addActor(this.selectedItem);
         ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.itemGrid);
         scrollPane.setSize(344.0F, 566.0F);
         scrollPane.setPosition(433.0F, 25.0F);
         this.addActor(scrollPane);
         this.characterModel.setPosition(240.0F, 120.0F);
         this.addActor(this.characterModel);
      }

      @Override
      public void onShow() {
         this.portrait.setPlayerData(GameState.wardrobeData.playerStats);
         this.characterModel.loadSkeleton(
            GameState.wardrobeData.playerStats.skeletonKey,
            GameState.wardrobeData.playerStats.equipmentSkinKeys
         );
         this.selectedItem.setItem(GameState.wardrobeData.selectedBackpackItem);
         if (this.itemGrid.getChildren().size != GameState.wardrobeData.backpackSlots.size) {
            for (int i = this.itemGrid.getChildren().size - 1; i >= 0; i--) {
               Actor child = this.itemGrid.getChildren().get(i);
               if (child instanceof ItemSlotButton) {
                  this.itemPool.free((ItemSlotButton)child);
               }
            }
            this.itemGrid.clear();
            for (int i = 0; i < GameState.wardrobeData.backpackSlots.size; i++) {
               this.itemGrid.add(this.itemPool.obtain()).size(104.0F).space(10.0F);
               if ((i + 1) % 3 == 0) {
                  this.itemGrid.row();
               }
            }
         }
         for (int i = 0; i < GameState.wardrobeData.backpackSlots.size; i++) {
            ((ItemSlotButton)this.itemGrid.getChild(i)).setItem(GameState.wardrobeData.backpackSlots.get(i));
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
      }

      @Override
      public void onShow() {
         StringBuilder text = new StringBuilder("[#8B4E23]");
         for (StatLine stat : GameState.wardrobeData.backpackSkillDescriptions) {
            text.append(stat.formatText()).append('\n');
         }
         this.statLabel.setText(text.toString());
      }
   }
}
