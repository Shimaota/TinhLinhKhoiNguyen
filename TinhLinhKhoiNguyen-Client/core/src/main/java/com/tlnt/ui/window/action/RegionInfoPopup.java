package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.GameMain;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.worldmap.RegionInfo;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.WindowType;

public class RegionInfoPopup extends TitledPopup {
   public final Pool<RegionInfoPopup.RegionEntryButton> OVERLAY_COLOR = new RegionButtonPool(this);
   public RegionInfoPopup.RegionListPanel optionsTable;

   public RegionInfoPopup(RenderContext renderContext) {
      super(renderContext, WindowType.ZONE_POPUP, "Khu vực");
      this.addActor(this.optionsTable = new RegionInfoPopup.RegionListPanel(super.renderContext));
   }

   @Override
   public final void drawBackground() {
      super.drawBackground();
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT,
            "Khu vực hiện tại: " + GameMain.instance.getLoginScreen().editorViewport.skeleton,
            this.getX() + 683.0F,
            this.getY() + 545.0F,
            Anchor.BOTTOM_LEFT,
            24.0F,
            "#8B4E23"
         );
   }

   public final void updateRegionList(RegionInfo[] regions) {
      this.optionsTable.populateRegionEntries(regions);
   }

   public class RegionEntryButton extends ShapeButton {
      public CompositeSprite bgSprite;
      public RegionInfo regionData;

      public RegionEntryButton(RenderContext renderContext) {
         super(renderContext);
         String leftPath = "ui/51";
         String middlePath = "ui/52";
         String rightPath = "ui/53";
         this.bgSprite = CompositeSprite.obtain(leftPath, middlePath, rightPath, false);
         this.addListener(new RegionButtonClickListener(this, RegionInfoPopup.this));
      }

      public final RegionInfoPopup.RegionEntryButton bindRegionData(RegionInfo regionInfo) {
         this.regionData = regionInfo;
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawCompositeSprite(this.bgSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         if (this.regionData != null) {
            super.renderContext
               .drawTexture(RegionInfoPopup.RegionType.values()[this.regionData.loadLevel].icon, this.getX() + 10.0F, this.getY() + 8.0F, Anchor.BOTTOM_LEFT);
            super.renderContext
               .drawTextColorName(
                  FontManager.PLAIN_FONT,
                  this.regionData.regionId + "",
                  this.getX() + 52.0F,
                  this.getY() + 50.0F,
                  Anchor.CENTER,
                  34.0F,
                  RegionInfoPopup.RegionType.values()[this.regionData.loadLevel].label
               );
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  "Dân số: " + this.regionData.playerCount + "/" + this.regionData.maxPlayers,
                  this.getX() + 105.0F,
                  this.getY() + 59.0F,
                  Anchor.BOTTOM_LEFT,
                  22.0F,
                  Color.WHITE
               );
            super.renderContext
               .drawText(
                  FontManager.DEFAULT_FONT,
                  "Tổ đội: " + this.regionData.partyCount,
                  this.getX() + 105.0F,
                  this.getY() + 33.0F,
                  Anchor.BOTTOM_LEFT,
                  22.0F,
                  Color.WHITE
               );
         }

         super.renderContext.resetBatchColor();
      }
   }

   public class RegionListPanel extends BaseTable {
      public Table contentTable;

      public RegionListPanel(RenderContext renderContext) {
         super(renderContext);
         this.setSize(710.0F, 390.0F);
         this.setPosition(185.0F, 140.0F);
         this.add(ScrollPaneFactory.createScrollPane(this.contentTable = new Table().top().left())).grow();
      }

      public final void populateRegionEntries(RegionInfo[] regions) {
         Array.ArrayIterator iter = this.contentTable.getChildren().iterator();

         while (iter.hasNext()) {
            Actor child;
            if ((child = (Actor)iter.next()) instanceof RegionInfoPopup.RegionEntryButton) {
               RegionInfoPopup.this.OVERLAY_COLOR.free((RegionInfoPopup.RegionEntryButton)child);
            }
         }

         this.contentTable.clear();
         RegionInfo[] regionArray = regions;
         int count = regions.length;

         for (int i = 0; i < count; i++) {
            RegionInfo region = regionArray[i];
            this.contentTable
               .add(RegionInfoPopup.this.OVERLAY_COLOR.obtain().bindRegionData(region))
               .size(345.0F, 100.0F)
               .spaceLeft(20.0F)
               .spaceRight(20.0F)
               .spaceTop(10.0F)
               .spaceBottom(10.0F);
            if (this.contentTable.getChildren().size % 2 == 0) {
               this.contentTable.row();
            }
         }
      }
   }

   public static enum RegionType {
      NORMAL(TextureAsset.obtain("ui/48"), "#874D24"),
      HOT(TextureAsset.obtain("ui/49"), "#004C51"),
      ELITE(TextureAsset.obtain("ui/50"), "#840404");

      public TextureAsset icon;
      public String label;

      RegionType(TextureAsset icon, String label) {
         this.icon = icon;
         this.label = label;
      }

   }
}
