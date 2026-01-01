package com.tlnt.ui.window.map.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.tlnt.core.GameMain;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.worldmap.WorldMapPage;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.player.PlayerEntity;

public final class MapRegionPanel extends GamePanel {
   public WorldMapPage mapData;
   public Table markerTable;

   public MapRegionPanel(RenderContext ctx, WorldMapPage page) {
      super(ctx);
      this.mapData = page;
      MapRegionPanel self = this;
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.markerTable = new Table()))
         .setOverscroll(false, false);
      this.add(scrollPane).grow();
      TextureAsset mapTexture = TextureAsset.obtain(this.mapData.mapImagePath);
      MapRegionBackground background = new MapRegionBackground(this, super.renderContext, scrollPane).setBackgroundTexture(mapTexture);
      this.markerTable.add(background).size((float)mapTexture.getWidth(), (float)mapTexture.getHeight()).expand().bottom().left();
      WorldMapPage.MapMarker[] markers = this.mapData.markers;
      int markerCount = this.mapData.markers.length;

      for (int i = 0; i < markerCount; i++) {
         WorldMapPage.MapMarker marker = markers[i];
         self.markerTable.addActor(self.new MapMarkerButton(self.renderContext, marker));
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String title = this.mapData.name;
      float textX = this.getX() + this.getWidth() / 2.0F;
      float textY = this.getY() + 623.0F;
      ctx.drawTextColorName(font, title, textX, textY, Anchor.BOTTOM_CENTER, 30.0F, "#8B4E23");
   }

   public class MapMarkerButton extends ShapeButton {

      public final MapRegionPanel outerPanel = MapRegionPanel.this;
      public WorldMapPage.MapMarker mapMarker;

      public MapMarkerButton(RenderContext ctx, WorldMapPage.MapMarker marker) {
         super(ctx);
         this.mapMarker = marker;
         this.setPosition(this.mapMarker.x, this.mapMarker.y);
         this.setSize(20.0F, 20.0F);
         this.addListener(new MapMarkerClickListener(this, MapRegionPanel.this));
      }

      @Override
      public final void drawBackground() {
         ShapeDrawer shapeDrawer;
         float centerX;
         float centerY;
         float radius;
         if (this.mapMarker.mapId == GameMain.instance.getLoginScreen().editorViewport.getId()) {
            float avatarSize = GameUtils.oscillate(0.5F, 35.0F, 40.0F);
            super.renderContext
               .drawTextureSized(
                  TextureAsset.obtain(PlayerEntity.getInstance().POOL2.avatarTexture),
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  avatarSize,
                  avatarSize,
                  Anchor.CENTER
               );
            super.renderContext.getShapeDrawer().setColor(Color.WHITE);
            shapeDrawer = super.renderContext.getShapeDrawer();
            centerX = this.getX() + this.getWidth() / 2.0F;
            centerY = this.getY() + this.getHeight() / 2.0F;
            radius = GameUtils.oscillate(0.5F, 17.0F, 20.0F);
         } else {
            super.renderContext
               .getShapeDrawer()
               .drawFilledCircle(this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, this.isOver() ? 15.0F : 10.0F, Color.RED);
            super.renderContext.getShapeDrawer().setColor(Color.WHITE);
            shapeDrawer = super.renderContext.getShapeDrawer();
            centerX = this.getX() + this.getWidth() / 2.0F;
            centerY = this.getY() + this.getHeight() / 2.0F;
            radius = this.isOver() ? 15.0F : 10.0F;
         }

         shapeDrawer.drawCircle(centerX, centerY, radius, 2.0F);
      }
   }
}
