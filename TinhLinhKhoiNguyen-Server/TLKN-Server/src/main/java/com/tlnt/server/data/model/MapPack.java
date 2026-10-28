package com.tlnt.server.data.model;

import java.util.List;

/**
 * Nội dung file DataGame {@code map} (xem {@code GameDataLoader.loadMapData} phía client):
 * tileset, decoration, particle, quái, NPC/tương tác, trang bản đồ thế giới.
 */
public record MapPack(
      int version,
      List<Tileset> tilesets,
      List<Decoration> decorations,
      List<Particle> particles,
      List<Mob> mobs,
      List<Npc> npcs,
      List<WorldMapPage> worldMapPages,
      List<MapEffect> mapEffects) {

   public record Tileset(int id, String textureName, int tileWidth, int tileHeight, List<Tile> tiles) {
   }

   /** tileType 0 = tile tĩnh (srcX/srcY), 1 = tile động (animFrames), loại khác không có thêm dữ liệu. */
   public record Tile(int tileId, int tileType, int srcX, int srcY, List<AnimFrame> animFrames) {
   }

   public record AnimFrame(int tileId, int durationMs) {
   }

   public record Decoration(int id, String name, int framesPerSecond, int anchorId, String[] frameNames) {
   }

   /** Không có id trong file, định danh bằng thứ tự. */
   public record Particle(int index, String json) {
   }

   public record Mob(int id, String name, int wanderType, int wanderRange, String skeletonPath,
         float scale, List<MobVariant> variants) {
   }

   public record MobVariant(String name, String skin, float speed, boolean showHealthBar) {
   }

   /**
    * {@code talkButton} (cột {@code talk_button}, trước là {@code npc_type}): client mới hiện nút nói chuyện ui/230 khi đứng gần (C3536) — 0 = đồ vật (Cây táo, Rương đồ). {@code kind} (cột {@code kind}, trước là {@code extra}): byte client mới đọc nhưng không dùng. {@code avatarIcon}: cột
    * chỉ server ({@code avatar_icon}) — ảnh chân dung hộp thoại NPC, null = không có.
    */
   public record Npc(int id, String name, String overheadText, boolean facesPlayer, boolean talkButton, int kind,
         String skeletonPath, String idleAnimation, float scale, Integer avatarIcon) {

      /** Đường dẫn ảnh chân dung cho client ({@code TextureAsset.obtain}), "" = không có. */
      public String portraitPath() {
         return avatarIcon == null ? "" : "icon/" + avatarIcon;
      }
   }

   public record WorldMapPage(int id, String name, String mapImagePath, List<MapMarker> markers) {
   }

   public record MapMarker(int mapId, String name, float x, float y) {
   }

   /** Khối tuỳ chọn ở cuối file map (hiệu ứng/đèn lồng map), có thể không có. */
   public record MapEffect(int id, String name, String path) {
   }
}
