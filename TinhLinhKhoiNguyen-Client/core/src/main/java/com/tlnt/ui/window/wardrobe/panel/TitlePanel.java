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

public final class TitlePanel extends GamePanel {
   public TitlePanel.SelectionPanel selectionPanel = new TitlePanel.SelectionPanel(this.renderContext);
   public TitlePanel.DetailPanel detailPanel;

   public TitlePanel(RenderContext renderContext) {
      super(renderContext);
      this.selectionPanel.setSize(796.0F, 610.0F);
      this.addActor(this.selectionPanel);
      this.detailPanel = new TitlePanel.DetailPanel(super.renderContext);
      this.detailPanel.setSize(433.0F, 610.0F);
      this.detailPanel.setPosition(826.0F, 0.0F);
      this.addActor(this.detailPanel);
   }

   public class DetailPanel extends GamePanel {
      public TextraLabel titleNameLabel;
      public TextraLabel descriptionLabel;

      public DetailPanel(RenderContext renderContext) {
         super(renderContext);
         Table table;
         Table tableRef = table = new Table();
         String headerText = "[#8B4E23]Chỉ số cộng thêm";
         tableRef.add(
               this.titleNameLabel = new TextraLabel(
                  headerText, FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 30, 2.0F)
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

         for (StatLine statLine : GameState.wardrobeData.skillDescriptions2) {
            StringBuilder appended = sb.append(statLine.formatText());
            appended.append("\n");
         }

         this.descriptionLabel.setText(sb.toString());
      }
   }

   public class SelectionPanel extends GamePanel {
      public Pool<ItemSlotButton> titleButtonPool = new TitleItemPool(this);
      public CharacterPortraitPanel characterPortrait = new CharacterPortraitPanel(this.renderContext);
      public ItemSlotButton equippedTitleButton;
      public CharacterModelActor characterModel;
      public Table titleGrid;

      public SelectionPanel(RenderContext renderContext) {
         super(renderContext);
         this.characterPortrait.setSize(279.0F, 128.0F);
         this.characterPortrait.setPosition(90.0F, 466.0F);
         this.addActor(this.characterPortrait);
         this.equippedTitleButton = new ItemSlotButton(super.renderContext);
         this.equippedTitleButton.setSize(104.0F, 104.0F);
         this.equippedTitleButton.setPosition(25.0F, 25.0F);
         this.addActor(this.equippedTitleButton);
         this.equippedTitleButton.addListener(new TitleEquipSlotClickListener(this));
         ScrollPane scrollPane;
         (scrollPane = ScrollPaneFactory.createScrollPane(this.titleGrid = new Table().top().left().pad(6.0F))).setSize(344.0F, 566.0F);
         scrollPane.setPosition(433.0F, 25.0F);
         this.addActor(scrollPane);
         this.characterModel = new CharacterModelActor(super.renderContext);
         this.characterModel.setPosition(240.0F, 120.0F);
         this.addActor(this.characterModel);
      }

      @Override
      public final void refresh() {
         this.characterPortrait.setPlayerData(GameState.wardrobeData.playerStats);
         this.characterModel.loadSkeleton(GameState.wardrobeData.playerStats.skeletonKey, GameState.wardrobeData.playerStats.equipmentSkinKeys);
         this.equippedTitleButton.setItem(GameState.wardrobeData.selectedItem2);
         if (this.titleGrid.getChildren().size != GameState.wardrobeData.bagSlots2.size) {
            Array.ArrayIterator it = this.titleGrid.getChildren().iterator();

            while (it.hasNext()) {
               Actor child;
               if ((child = (Actor)it.next()) instanceof ItemSlotButton) {
                  this.titleButtonPool.free((ItemSlotButton)child);
               }
            }

            this.titleGrid.clear();

            for (Item each : GameState.wardrobeData.bagSlots2) {
               this.titleGrid.add(this.titleButtonPool.obtain()).size(104.0F).space(10.0F);
               if (this.titleGrid.getChildren().size % 3 == 0) {
                  this.titleGrid.row();
               }
            }
         }

         for (int i = 0; i < GameState.wardrobeData.bagSlots2.size; i++) {
            ((ItemSlotButton)this.titleGrid.getChild(i)).setItem(GameState.wardrobeData.bagSlots2.get(i));
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
