package com.tlnt.model.worldmap;

import com.badlogic.gdx.utils.Array;

public final class WorldMapPage {
   public static final Array<WorldMapPage> ALL_PAGES = new Array<>();
   public int id;
   public String name;
   public String mapImagePath;
   public WorldMapPage.MapMarker[] markers;

   public static void addPage(WorldMapPage page) {
      ALL_PAGES.add(page);
   }

   public static void clearPages() {
      ALL_PAGES.clear();
   }

   public static Array<WorldMapPage> getPages() {
      return ALL_PAGES;
   }

   public WorldMapPage(int id, String name, String mapImagePath, WorldMapPage.MapMarker[] markers) {
      this.id = id;
      this.name = name;
      this.mapImagePath = mapImagePath;
      this.markers = markers;
   }

   public static class MapMarker {
      public int mapId;
      public String name;
      public float x;
      public float y;

      public MapMarker(int mapId, String name, float x, float y) {
         this.mapId = mapId;
         this.name = name;
         this.x = x;
         this.y = y;
      }
   }
}
