package com.tlnt.ui.window.ranking.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.ranking.RankEntry;
import com.tlnt.model.ranking.RankingCategory;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.RefreshScrollPane;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.world.entity.player.PlayerEntity;

public final class RankCategoryPanel extends GamePanel {
   public Pool<RankCategoryPanel.RankEntryRow> rowPool = new RankEntryRowPool(this);
   public RankingCategory category;
   public Table listTable;
   public RankCategoryPanel.RankEntryRow localPlayerRow;

   public RankCategoryPanel(RenderContext ctx, RankingCategory category) {
      super(ctx);
      this.category = category;
      this.add(new RankCategoryPanel.RankListHeader(super.renderContext)).height(40.0F).growX().row();
      RefreshScrollPane scrollPane = ScrollPaneFactory.createRefreshScrollPane(this.listTable = new Table().top(), new RankScrollRefreshListener(this));
      this.add(scrollPane).grow().spaceBottom(5.0F).row();
      this.localPlayerRow = new RankCategoryPanel.RankEntryRow(super.renderContext);
      this.add(this.localPlayerRow).height(65.0F).growX();
   }

   @Override
   public final void onShow() {
      if (this.category != null && this.category.categoryId >= 0) { // op 75 (id -1): sent by the server on its own, not requested
         ServerPacketSender.getInstance().sendRequestRanking(this.category.categoryId);
      }
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator childIter = this.listTable.getChildren().iterator();

      while (childIter.hasNext()) {
         Actor child;
         if ((child = (Actor)childIter.next()) instanceof RankCategoryPanel.RankEntryRow) {
            this.rowPool.free((RankCategoryPanel.RankEntryRow)child);
         }
      }

      this.listTable.clear();
      if (this.category != null) {
         for (RankEntry entry : this.category.entries) {
            this.listTable.add(this.rowPool.obtain().bind(entry)).height(65.0F).growX().space(10.0F).row();
         }

         boolean own = this.category.categoryId >= 0; // the op 75 table has no row for the local player
         this.localPlayerRow.setVisible(own);
         this.localPlayerRow.bind(own ? GameState.myRankEntry : null);
      }
   }

   public class RankEntryRow extends ShapeButton {
      public CompositeSprite background;
      public TextureAsset rank1Icon;
      public TextureAsset rank2Icon;
      public TextureAsset rank3Icon;
      public RankEntry entry;

      public RankEntryRow(RenderContext ctx) {
         super(ctx);
         String leftPath = "ui/131";
         String middlePath = "ui/132";
         String rightPath = "ui/133";
         this.background = CompositeSprite.obtain(leftPath, middlePath, rightPath, false);
         this.rank1Icon = TextureAsset.obtain("ui/134");
         this.rank2Icon = TextureAsset.obtain("ui/135");
         this.rank3Icon = TextureAsset.obtain("ui/136");
         this.addListener(new RankRowClickListener(this, RankCategoryPanel.this));
      }

      public final RankCategoryPanel.RankEntryRow bind(RankEntry rankEntry) {
         this.entry = rankEntry;
         return this;
      }

      @Override
      public final void drawBackground() {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawCompositeSprite(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         if (this.entry != null) {
            Color textColor = this.entry.playerId == PlayerEntity.getInstance().getId() ? Color.YELLOW : Color.WHITE;
            switch (this.entry.rank) {
               case 1:
                  super.renderContext.drawTexture(this.rank1Icon, this.getX() + 82.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER);
                  break;
               case 2:
                  super.renderContext.drawTexture(this.rank2Icon, this.getX() + 82.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER);
                  break;
               case 3:
                  super.renderContext.drawTexture(this.rank3Icon, this.getX() + 82.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER);
                  break;
               default:
                  super.renderContext
                     .drawText(
                        FontManager.DEFAULT_FONT, this.entry.rank + "", this.getX() + 82.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER, 22.0F, textColor
                     );
            }

            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT, this.entry.playerName, this.getX() + 406.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER, 22.0F, textColor
               );
            super.renderContext
               .drawText(FontManager.DEFAULT_FONT, this.entry.value, this.getX() + 788.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER, 22.0F, textColor);
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  this.entry.characterClass != null ? this.entry.characterClass.name : "",
                  this.getX() + 1128.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  Anchor.CENTER,
                  22.0F,
                  textColor
               );
            super.renderContext.resetBatchColor();
         }
      }
   }

   public class RankListHeader extends BaseTable {

      public RankListHeader(RenderContext ctx) {
         super(ctx);
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         super.renderContext
            .drawTextColorName(
               FontManager.PLAIN_FONT,
               "Hạng",
               this.getX() + 82.0F,
               this.getY() + this.getHeight() / 2.0F,
               Anchor.CENTER,
               30.0F,
               "#8B4E23"
            );
         RenderContext ctx = super.renderContext;
         BitmapFont font = FontManager.PLAIN_FONT;
         String headerText = "Tên nhân vật";
         float textX = this.getX() + 406.0F;
         float textY = this.getY() + this.getHeight() / 2.0F;
         ctx.drawTextColorName(font, headerText, textX, textY, Anchor.CENTER, 30.0F, "#8B4E23");
         super.renderContext
            .drawTextColorName(
               FontManager.PLAIN_FONT,
               RankCategoryPanel.this.category.name,
               this.getX() + 788.0F,
               this.getY() + this.getHeight() / 2.0F,
               Anchor.CENTER,
               30.0F,
               "#8B4E23"
            );
         ctx = super.renderContext;
         font = FontManager.PLAIN_FONT;
         headerText = "Nhân vật";
         textX = this.getX() + 1128.0F;
         textY = this.getY() + this.getHeight() / 2.0F;
         ctx.drawTextColorName(font, headerText, textX, textY, Anchor.CENTER, 30.0F, "#8B4E23");
      }
   }
}
