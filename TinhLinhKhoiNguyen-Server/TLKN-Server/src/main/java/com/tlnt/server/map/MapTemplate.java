package com.tlnt.server.map;

import java.util.List;

/**
 * Bố cục 1 map: 1 dòng bảng {@code map_template} (va chạm + cửa + trang trí + NPC / quái / cây / vùng nước lưu JSON:
 * cột colliders, waypoints, objects, npcs, mobs, plants, waters). Bất biến sau khi nạp. Toạ độ pixel, gốc dưới-trái.
 */
public record MapTemplate(
      int id,
      String name,
      String music,
      int tileCols,
      int tileRows,
      int tileSize,
      int width,
      int height,
      float spawnX,
      float spawnY,
      int maxPlayersPerZone,
      int zoneCount,
      List<Collider> colliders,
      List<Waypoint> waypoints,
      List<MapObject> objects,
      List<NpcSpawn> npcs,
      List<MobSpawn> mobs,
      List<PlantSpawn> plants,
      List<Water> waters) {

   /** Vùng nước = vùng vẽ tay (cột {@code waters}) + ảnh động nước có sẵn trong {@code objects}, gộp các hộp chạm nhau. */
   public MapTemplate {
      waters = Water.merge(waters, objects);
   }

   /** Map chưa có vùng nước (test, công cụ). */
   public MapTemplate(int id, String name, String music, int tileCols, int tileRows, int tileSize, int width,
         int height, float spawnX, float spawnY, int maxPlayersPerZone, int zoneCount, List<Collider> colliders,
         List<Waypoint> waypoints, List<MapObject> objects, List<NpcSpawn> npcs, List<MobSpawn> mobs,
         List<PlantSpawn> plants) {
      this(id, name, music, tileCols, tileRows, tileSize, width, height, spawnX, spawnY, maxPlayersPerZone, zoneCount,
            colliders, waypoints, objects, npcs, mobs, plants, List.of());
   }

   /** Map chưa đặt NPC / quái / cây (test, công cụ). */
   public MapTemplate(int id, String name, String music, int tileCols, int tileRows, int tileSize, int width,
         int height, float spawnX, float spawnY, int maxPlayersPerZone, int zoneCount, List<Collider> colliders,
         List<Waypoint> waypoints, List<MapObject> objects) {
      this(id, name, music, tileCols, tileRows, tileSize, width, height, spawnX, spawnY, maxPlayersPerZone, zoneCount,
            colliders, waypoints, objects, List.of(), List.of(), List.of(), List.of());
   }

   /**
    * 1 phần tử cột JSON {@code colliders}: hộp {@code [x,y,w,h]} / {@code [x,y,w,h,1]} (oneWay = nền 1 chiều), hoặc
    * tam giác (dốc) {@code [x1,y1,x2,y2,x3,y3]} / thêm {@code 1} — {@code points} khác null (client op 6 kiểu 1).
    */
   public record Collider(float x, float y, float w, float h, boolean oneWay, float[] points) {

      public Collider(float x, float y, float w, float h, boolean oneWay) {
         this(x, y, w, h, oneWay, null);
      }

      public boolean triangle() {
         return points != null && points.length == 6;
      }
   }

   /**
    * 1 phần tử cột JSON {@code waypoints}. {@code targetMapId} null / map chưa có = cửa không gửi cho client.
    * {@code targetX/Y} null = xuất hiện cạnh cửa dẫn ngược về map này.
    * {@code interact}: false = cửa đi xuyên (chạm là qua map, cửa ở mép map); true = cửa phải bấm (chạm chỉ chọn
    * làm mục tiêu, bấm Đánh cơ bản / bấm bảng tên mới qua — vd cửa Nông trại giữa làng).
    */
   public static final class Waypoint {
      private String name;
      private boolean interact;
      private float x;
      private float y;
      private float w;
      private float h;
      private Integer targetMapId;
      private Float targetX;
      private Float targetY;

      public String name() {
         return name;
      }

      public float x() {
         return x;
      }

      public float y() {
         return y;
      }

      public float w() {
         return w;
      }

      public float h() {
         return h;
      }

      public Integer targetMapId() {
         return targetMapId;
      }

      public Float targetX() {
         return targetX;
      }

      public Float targetY() {
         return targetY;
      }

      public boolean interact() {
         return interact;
      }

      public boolean contains(float px, float py, float margin) {
         return px >= x - margin && px <= x + w + margin && py >= y - margin && py <= y + h + margin;
      }
   }

   public enum ObjectKind { TEXTURE, ANIMATED, PARTICLE, SKELETON }

   /**
    * 1 phần tử cột JSON {@code objects} (trang trí, opcode 13). Trường bỏ trống trong JSON lấy giá trị mặc định
    * ở đây, nên chỉ cần ghi những gì khác mặc định. Thứ tự trong mảng = thứ tự vẽ trong layer.
    */
   public static final class MapObject {
      /** 0 = nền xa, 1 = giữa, 2 = trước. */
      private int layer;
      private ObjectKind kind = ObjectKind.TEXTURE;
      /** TEXTURE: đường dẫn ảnh (objects/28); ANIMATED: map_decoration.id; PARTICLE: particle_effect.idx. */
      private String sprite;
      private float x;
      private float y;
      private float w;
      private float h;
      /** 1 = trôi theo map, 0 = đứng yên theo camera. */
      private float parallaxX = 1f;
      private float parallaxY = 1f;
      private boolean flipX;
      private boolean flipY;
      private boolean tiledX;
      private boolean tiledY;
      /** Hạt: 1 = đúng kích thước pixel trong particle_effect. */
      private float scale = 1f;
      /** Hạt: bám camera theo hệ số parallax thay vì đứng yên trong map. */
      private boolean followCamera;
      /** Ảnh / ảnh động: nhuộm "RRGGBBAA" (null = không nhuộm). */
      private String color;
      /** SKELETON (client op 13 kiểu 3): sprite = đường dẫn skeleton, scale = tỉ lệ đọc (như NPC 0.25). */
      private String anim;
      private java.util.List<String> skins;
      private float scaleX = 1f;
      private float scaleY = 1f;
      private Boolean loop;
      private float timeScale = 1f;

      public int layer() {
         return layer;
      }

      public ObjectKind kind() {
         return kind;
      }

      public String sprite() {
         return sprite;
      }

      public float x() {
         return x;
      }

      public float y() {
         return y;
      }

      public float w() {
         return w;
      }

      public float h() {
         return h;
      }

      public float parallaxX() {
         return parallaxX;
      }

      public float parallaxY() {
         return parallaxY;
      }

      public boolean flipX() {
         return flipX;
      }

      public boolean flipY() {
         return flipY;
      }

      public boolean tiledX() {
         return tiledX;
      }

      public boolean tiledY() {
         return tiledY;
      }

      public float scale() {
         return scale;
      }

      public boolean followCamera() {
         return followCamera;
      }

      /** RGBA 0..1 từ {@link #color}; không có / sai -> trắng. */
      public float[] rgba() {
         float[] out = {1f, 1f, 1f, 1f};
         if (color == null) {
            return out;
         }
         String c = color.startsWith("#") ? color.substring(1) : color;
         try {
            for (int i = 0; i < 4 && i * 2 + 2 <= c.length(); i++) {
               out[i] = Integer.parseInt(c.substring(i * 2, i * 2 + 2), 16) / 255f;
            }
         } catch (NumberFormatException e) {
            return new float[] {1f, 1f, 1f, 1f};
         }
         return out;
      }

      public String anim() {
         return anim == null ? "" : anim;
      }

      public java.util.List<String> skins() {
         return skins == null ? java.util.List.of() : skins;
      }

      public float scaleX() {
         return scaleX;
      }

      public float scaleY() {
         return scaleY;
      }

      public boolean loop() {
         return loop == null || loop;
      }

      public float timeScale() {
         return timeScale;
      }
   }

   /**
    * 1 phần tử cột JSON {@code npcs} ({@code [{id,x,y,flipX?,shadow?}]}, TLNT-Editor ghi). {@code id} =
    * npc_template.id. Hướng quay ban đầu đặt riêng từng NPC trên map bằng {@code flipX} (true = quay PHẢI — skeleton
    * gốc quay trái, giống TLNT-Editor). Bóng dưới chân: bỏ trống
    * = theo template ({@link #applyDefaultShadow}), ghi {@code shadow} để ép bật / tắt riêng NPC này.
    */
   public static final class NpcSpawn {
      private int id;
      private float x;
      private float y;
      /** Editor lật skeleton = quay PHẢI (op 24 byte hướng 1); không lật = quay trái như skeleton gốc (0). */
      private boolean flipX;
      private Boolean shadow;

      public int id() {
         return id;
      }

      public float x() {
         return x;
      }

      public float y() {
         return y;
      }

      public boolean flipX() {
         return flipX;
      }

      /** Có vẽ bóng dưới chân (op 24 sub 0). Chưa đặt -> có bóng. */
      public boolean shadow() {
         return shadow == null || shadow;
      }

      /** Gọi 1 lần lúc nạp map: JSON không ghi {@code shadow} thì lấy mặc định của template. */
      public void applyDefaultShadow(boolean templateDefault) {
         if (shadow == null) {
            shadow = templateDefault;
         }
      }
   }

   /**
    * 1 phần tử cột JSON {@code mobs} ({@code [{id,variant?,x,y,count?,range?,respawn?,level?,hp?}]}). {@code id} =
    * mob_template.id, {@code variant} = mob_variant.idx. {@code count} con rải ngẫu nhiên trong {@code x ± range}.
    * Mặc định giống TLNT-Editor ({@code MapJson}); {@code level}/{@code hp} editor chưa ghi -> 1 / 100.
    */
   public static final class MobSpawn {
      private int id;
      private int variant;
      private float x;
      private float y;
      private int count = 1;
      private int range = 200;
      private int respawn = 10;
      private int level = 1;
      private long hp = 100;

      public int id() {
         return id;
      }

      public int variant() {
         return variant;
      }

      public float x() {
         return x;
      }

      public float y() {
         return y;
      }

      public int count() {
         return Math.max(1, count);
      }

      public int range() {
         return Math.max(0, range);
      }

      /** Giây chờ hồi sinh sau khi chết (dùng khi có đánh quái). */
      public int respawn() {
         return respawn;
      }

      public int level() {
         return Math.max(1, level);
      }

      public long hp() {
         return Math.max(1, hp);
      }

      /** Bản sao quái THƯỜNG (biến thể 0) với cấp + HP khác (phó bản Rừng Linh Mộc: chỉ số theo cấp của lượt). */
      public MobSpawn withLevel(int newLevel, long newHp) {
         MobSpawn s = withVariant(0, newHp);
         s.level = newLevel;
         return s;
      }

      /** Bản sao của điểm sinh này với biến thể + HP khác (quái hồi sinh thành Tinh anh / Thủ lĩnh). */
      public MobSpawn withVariant(int newVariant, long newHp) {
         MobSpawn s = new MobSpawn();
         s.id = id;
         s.variant = newVariant;
         s.x = x;
         s.y = y;
         s.count = count;
         s.range = range;
         s.respawn = respawn;
         s.level = level;
         s.hp = newHp;
         return s;
      }

      /** Điểm sinh tạo trong code (quái mọc ngẫu nhiên — tổ ong, quả dừa: {@code world/ZoneMobs}). */
      public static MobSpawn of(int id, float x, float y, int level, long hp) {
         MobSpawn s = new MobSpawn();
         s.id = id;
         s.x = x;
         s.y = y;
         s.level = level;
         s.hp = hp;
         return s;
      }
   }

   /** 1 phần tử cột JSON {@code plants} ({@code [{id,x,y,respawn?}]}). {@code id} = plant_template.id. */
   public static final class PlantSpawn {
      private int id;
      private float x;
      private float y;
      private int respawn = 60;

      public int id() {
         return id;
      }

      public float x() {
         return x;
      }

      public float y() {
         return y;
      }

      /** Giây mọc lại sau khi bị hái (dùng khi có hái cây). */
      public int respawn() {
         return respawn;
      }
   }

   /**
    * Vùng nước (hộp pixel, gốc dưới-trái; đỉnh hộp = mặt nước): TỰ NHẬN từ ảnh động nước trong cột {@code objects}
    * ({@link #DECORATIONS}) + (tuỳ chọn) vẽ thêm ở cột JSON {@code waters} ({@code [{x,y,w,h,fish?}]}). Đứng gần vùng
    * này mới câu cá được ({@code fishing/FishingService}); {@code fish} = số cá bơi trang trí client
    * vẽ trong vùng (op 25 sub 1, 0 = không vẽ).
    */
   public static final class Water {
      /**
       * {@code map_decoration.id} là mặt nước (ảnh động kiểu ANIMATED trong cột objects): 3 waterflow, 4
       * swampwaterflow. Thác nước (5-7) là nước dựng đứng -> không tính.
       */
      static final java.util.Set<String> DECORATIONS = java.util.Set.of("3", "4");
      /** Ảnh động nước để w / h = 0 (kích thước ảnh gốc — server không đọc ảnh): dùng cỡ khung waterflow. */
      private static final float DEFAULT_W = 500;
      private static final float DEFAULT_H = 204;
      /** Số cá bơi trang trí cho vùng tự nhận từ ảnh nước: 1 con / 350 px, tối đa 14. */
      private static final int FISH_PER_PX = 350;
      private static final int MAX_AUTO_FISH = 14;

      private float x;
      private float y;
      private float w;
      private float h;
      private int fish = 3;

      /** Gson. */
      Water() {
      }

      Water(float x, float y, float w, float h, int fish) {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.fish = fish;
      }

      /**
       * Vùng vẽ tay + mọi ảnh động nước trôi theo map (parallax 1 — nước ở nền xa / đứng theo camera không đứng cạnh
       * được), rồi gộp các hộp chồng / chạm nhau thành 1 (22 ô waterflow 500 px liền nhau -> 1 vùng). Gọi lại trên kết
       * quả vẫn ra đúng kết quả đó.
       */
      static List<Water> merge(List<Water> manual, List<MapObject> objects) {
         List<Water> boxes = new java.util.ArrayList<>(manual == null ? List.of() : manual);
         int manualCount = boxes.size();
         for (MapObject o : objects == null ? List.<MapObject>of() : objects) {
            if (o.kind() == ObjectKind.ANIMATED && DECORATIONS.contains(o.sprite()) && o.parallaxX() == 1f
                  && o.parallaxY() == 1f) {
               boxes.add(new Water(o.x(), o.y(), o.w() > 0 ? o.w() : DEFAULT_W, o.h() > 0 ? o.h() : DEFAULT_H, -1));
            }
         }
         if (boxes.size() == manualCount && manualCount <= 1) {
            return List.copyOf(boxes);
         }
         boolean mergedAny = true;
         while (mergedAny) {
            mergedAny = false;
            for (int i = 0; i < boxes.size() && !mergedAny; i++) {
               for (int j = i + 1; j < boxes.size(); j++) {
                  Water a = boxes.get(i);
                  Water b = boxes.get(j);
                  if (a.x <= b.x + b.w && b.x <= a.x + a.w && a.y <= b.y + b.h && b.y <= a.y + a.h) {
                     float x0 = Math.min(a.x, b.x);
                     float y0 = Math.min(a.y, b.y);
                     float x1 = Math.max(a.x + a.w, b.x + b.w);
                     float y1 = Math.max(a.y + a.h, b.y + b.h);
                     boxes.set(i, new Water(x0, y0, x1 - x0, y1 - y0, Math.max(a.fish, b.fish)));
                     boxes.remove(j);
                     mergedAny = true;
                     break;
                  }
               }
            }
         }
         List<Water> out = new java.util.ArrayList<>(boxes.size());
         for (Water b : boxes) { // fish -1 = chỉ gồm ảnh nước (không có vùng vẽ tay) -> tự tính theo bề rộng
            int fish = b.fish >= 0 ? b.fish : Math.max(1, Math.min(MAX_AUTO_FISH, Math.round(b.w / FISH_PER_PX)));
            out.add(b.fish == fish ? b : new Water(b.x, b.y, b.w, b.h, fish));
         }
         return List.copyOf(out);
      }

      public float x() {
         return x;
      }

      public float y() {
         return y;
      }

      public float w() {
         return w;
      }

      public float h() {
         return h;
      }

      public int fish() {
         return Math.max(0, fish);
      }

      /** Điểm (px, py) nằm trong hộp nở rộng {@code margin} pixel mỗi phía. */
      public boolean near(float px, float py, float margin) {
         return px >= x - margin && px <= x + w + margin && py >= y - margin && py <= y + h + margin;
      }
   }

   /** Có vùng nước nào cách điểm (px, py) không quá {@code margin} pixel. */
   public boolean nearWater(float px, float py, float margin) {
      for (Water water : waters) {
         if (water.near(px, py, margin)) {
            return true;
         }
      }
      return false;
   }

   /**
    * 1 con quái cụ thể sau khi nhân {@link MobSpawn#count()}. {@code id} = id thực thể gửi client (vị trí trong
    * {@link #mobInstances()}), {@code x} đã rải trong {@code ± range}.
    */
   public record MobInstance(int id, MobSpawn spawn, float x, float y) {
   }

   /**
    * Nhân mọi điểm sinh thành từng con. Random có seed theo map + điểm sinh -> lần khởi động nào cũng cùng vị trí
    * (giống editor PlayMode: 1 con đứng đúng điểm, nhiều con rải ngẫu nhiên).
    */
   public List<MobInstance> mobInstances() {
      List<MobInstance> out = new java.util.ArrayList<>();
      for (int s = 0; s < mobs.size(); s++) {
         MobSpawn spawn = mobs.get(s);
         java.util.Random random = new java.util.Random(id * 1_000_003L + s);
         for (int i = 0; i < spawn.count(); i++) {
            float x = spawn.x();
            if (spawn.count() > 1 && spawn.range() > 0) {
               x += (random.nextFloat() * 2 - 1) * spawn.range();
            }
            out.add(new MobInstance(out.size(), spawn, clampX(x), spawn.y()));
         }
      }
      return out;
   }

   /** Bản sao chỉ giữ những phần tử dùng được (bỏ cửa dẫn tới map chưa có, NPC/quái/cây template lạ). */
   public MapTemplate withContent(List<Waypoint> keptWaypoints, List<NpcSpawn> keptNpcs, List<MobSpawn> keptMobs,
         List<PlantSpawn> keptPlants) {
      return new MapTemplate(id, name, music, tileCols, tileRows, tileSize, width, height, spawnX, spawnY,
            maxPlayersPerZone, zoneCount, colliders, List.copyOf(keptWaypoints), objects, List.copyOf(keptNpcs),
            List.copyOf(keptMobs), List.copyOf(keptPlants), waters);
   }

   /** Cửa theo id client gửi ở opcode 11 = vị trí trong mảng đã gửi ở opcode 6. */
   public Waypoint waypointAt(int position) {
      return position >= 0 && position < waypoints.size() ? waypoints.get(position) : null;
   }

   /** Cửa ở map này dẫn tới {@code mapId} (để xuất hiện cạnh cửa đó khi đi từ {@code mapId} sang). */
   public Waypoint waypointTo(int mapId) {
      for (Waypoint waypoint : waypoints) {
         if (waypoint.targetMapId() != null && waypoint.targetMapId() == mapId) {
            return waypoint;
         }
      }
      return null;
   }

   public float clampX(float x) {
      return Math.max(0, Math.min(width, x));
   }

   public float clampY(float y) {
      return Math.max(0, Math.min(height, y));
   }
}
