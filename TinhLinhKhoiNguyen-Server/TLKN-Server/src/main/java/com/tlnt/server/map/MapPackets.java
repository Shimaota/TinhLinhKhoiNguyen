package com.tlnt.server.map;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.Packet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gói tĩnh của 1 map, encode 1 lần lúc khởi động rồi gửi lại cho mọi người vào map:
 * opcode 6 (thông tin + địa hình + cửa + nhạc), 7 (tile), 13 (trang trí). Format bám theo
 * {@code MessageHandler case 6/7} và {@code ServerPacketHandler.handleMapDecorations} phía client.
 */
public final class MapPackets {

   private static final int DECORATION_LAYERS = 3;
   /** Lớp trang trí vẽ SAU nhân vật / quái / đạn (client renderOrder 9.000.000 + i). */
   static final int FRONT_LAYER = 2;
   static final String BLEND_RESET_SKELETON = "skeletons/vfxs/burn";
   private static final String BLEND_RESET_ANIMATION = "burn";
   /** Pixel, xa ngoài mọi map (parallax 1 = đứng yên trong map -> không bao giờ vào màn hình). */
   private static final float BLEND_RESET_POSITION = -100_000f;
   /** Client đọc số NPC / quái bằng byte có dấu -> tối đa 127 / gói, nhiều hơn thì tách gói (client chỉ nối thêm). */
   public static final int MAX_PER_PACKET = 127;

   private final byte[] infoHead;
   private final byte[] infoTail;
   /** Opcode 6 khác nhau duy nhất ở số khu -> cache theo khu. */
   private final Map<Integer, Packet> infoByZone = new ConcurrentHashMap<>();
   private final Packet tiles;
   private final Packet decorations;
   /**
    * NPC + cá bơi, gửi sau opcode 6 (client xoá hết NPC / quái / cây khi nhận opcode 6). Rỗng = map không có. Quái có
    * HP thay đổi theo từng khu nên không nằm ở đây ({@code world/ZoneMobs}).
    */
   private final List<Packet> entities;
   /** Như {@link #entities} nhưng KHÔNG có NPC của sự kiện đang tắt ({@code event/GameEvents#npcVisible}). */
   private final List<Packet> entitiesNoEvent;

   /** @param map chỉ chứa cửa / NPC / quái / cây dùng được (World đã lọc). */
   public MapPackets(MapTemplate map) {
      this.infoHead = buildInfoHead(map);
      this.infoTail = buildInfoTail(map);
      this.tiles = buildTiles().toPacket();
      this.decorations = buildDecorations(map).toPacket();
      List<Packet> list = new ArrayList<>();
      List<Packet> noEvent = new ArrayList<>();
      for (Message msg : buildNpcs(map, false)) {
         list.add(msg.toPacket());
      }
      for (Message msg : buildNpcs(map, true)) {
         noEvent.add(msg.toPacket());
      }
      if (!map.waters().isEmpty()) {
         Packet waters = buildWaters(map).toPacket();
         list.add(waters);
         noEvent.add(waters);
      }
      this.entities = List.copyOf(list);
      this.entitiesNoEvent = List.copyOf(noEvent);
   }

   public Packet info(int zoneId) {
      return infoByZone.computeIfAbsent(zoneId, id -> {
         Message msg = new Message(6);
         msg.writeRaw(infoHead);
         msg.writeInt(id); // client hiện "Khu vực hiện tại: <id>"
         msg.writeRaw(infoTail);
         return msg.toPacket();
      });
   }

   public Packet tiles() {
      return tiles;
   }

   public Packet decorations() {
      return decorations;
   }

   /**
    * NPC (op 24), cá bơi trong vùng nước (op 25 sub 1) của map — gửi sau {@link #decorations()}. Cây (op 54) mọc / bị
    * hái theo từng khu nên gửi ở {@code world/ZonePlants}.
    */
   /** NPC (bỏ NPC của sự kiện đang tắt) + cá bơi — gửi sau op 6. */
   public List<Packet> entities() {
      return com.tlnt.server.event.GameEvents.anyEventNpcHidden() ? entitiesNoEvent : entities;
   }

   // ------------------------------------------------------------------ opcode 6

   private static byte[] buildInfoHead(MapTemplate map) {
      Message msg = new Message(6);
      msg.writeInt(map.id());
      msg.writeString(map.name());
      msg.writeBoolean(com.tlnt.server.altar.AltarRules.isAltarMap(map.id()) // nút "Thoát" (op 25 sub 3): chỉ ở phó bản
            || com.tlnt.server.linhmoc.LinhMocRules.isDungeonMap(map.id())
            || com.tlnt.server.thunder.ThunderRules.isThunderMap(map.id()) // Lôi vực: Thoát = về làng, hết lượt
            || com.tlnt.server.world.Arena.isArenaMap(map.id())); // Giải đấu: Thoát = về làng (bỏ cuộc)
      return msg.getBytes();
   }

   static byte[] buildInfoTail(MapTemplate map) {
      Message msg = new Message(6);
      msg.writeInt(map.tileCols());
      msg.writeInt(map.tileRows());
      msg.writeInt(map.tileSize());
      msg.writeInt(map.tileSize());

      // Client: collider index >= solidCount là sàn một chiều -> ghi hết collider thường trước.
      List<MapTemplate.Collider> colliders = map.colliders();
      int solidCount = (int) colliders.stream().filter(c -> !c.oneWay()).count();
      msg.writeInt(colliders.size());
      msg.writeInt(solidCount);
      for (boolean oneWay : new boolean[] {false, true}) {
         for (MapTemplate.Collider c : colliders) {
            if (c.oneWay() == oneWay) {
               if (c.triangle()) {
                  msg.writeByte(1); // tam giác (dốc): 6 số x1,y1,x2,y2,x3,y3 (client addPolygonCollider)
                  for (float v : c.points()) {
                     msg.writeFloat(v);
                  }
                  continue;
               }
               msg.writeByte(0); // 0 = hộp
               msg.writeFloat(c.x());
               msg.writeFloat(c.y());
               msg.writeFloat(c.w());
               msg.writeFloat(c.h());
            }
         }
      }

      // Client dùng thứ tự trong danh sách làm id cửa (gửi lại ở opcode 11). Cờ cuối KHÔNG phải bật/tắt mà là
      // "cửa phải bấm" (WaypointContactListener: false = chạm là qua, true = chạm chỉ chọn mục tiêu).
      msg.writeByte(map.waypoints().size());
      for (MapTemplate.Waypoint wp : map.waypoints()) {
         msg.writeString(wp.name());
         msg.writeFloat(wp.x());
         msg.writeFloat(wp.y());
         msg.writeFloat(wp.w());
         msg.writeFloat(wp.h());
         msg.writeBoolean(wp.interact());
      }

      msg.writeString(map.music());
      return msg.getBytes();
   }

   // ------------------------------------------------------------------ opcode 7

   private static Message buildTiles() {
      Message msg = new Message(7);
      msg.writeByte(0); // map chưa có lớp tile: toàn bộ hình map nằm ở trang trí (opcode 13)
      return msg;
   }

   // ------------------------------------------------------------------ opcode 13

   static Message buildDecorations(MapTemplate map) {
      Message msg = new Message(13);
      for (int layer = 0; layer < DECORATION_LAYERS; layer++) {
         final int current = layer;
         List<MapTemplate.MapObject> objects = map.objects().stream().filter(o -> o.layer() == current).toList();
         boolean resetBlend = layer == FRONT_LAYER && !objects.isEmpty();
         msg.writeInt(objects.size() + (resetBlend ? 1 : 0));
         if (resetBlend) {
            writeBlendReset(msg);
         }
         for (MapTemplate.MapObject o : objects) {
            switch (o.kind()) {
               case TEXTURE, ANIMATED -> writeSprite(msg, o);
               case PARTICLE -> writeParticle(msg, o);
               case SKELETON -> writeSkeleton(msg, o);
            }
         }
      }
      return msg;
   }

   /**
    * Skeleton vô hình mở đầu lớp trước để sửa lỗi lớp trước "chớp" khi dùng kỹ năng (lỗi client, không sửa client
    * được). Spine {@code SkeletonRenderer.draw} đổi blend theo từng slot mà KHÔNG trả lại khi vẽ xong; skeleton VFX
    * vũ khí (axe / bow / slingshot / spear / whip / hit_effect) có slot ADDITIVE, còn ảnh trang trí không tự đặt
    * blend -> lớp trước (vẽ ngay sau nhân vật / đạn, renderOrder 9.000.000+) bị vẽ cộng sáng trong lúc có VFX.
    * Vẽ 1 skeleton toàn slot NORMAL ngay trước (index 0 = 9.000.000) trả blend về bình thường mỗi khung hình.
    * {@code vfxs/burn}: 1 slot NORMAL, luôn có ảnh, file nhỏ; đặt xa ngoài map (client không cắt vật ngoài màn hình
    * nhưng cũng không thấy).
    */
   private static void writeBlendReset(Message msg) {
      msg.writeByte(3);
      msg.writeString(BLEND_RESET_SKELETON);
      msg.writeFloat(0.25f);
      msg.writeString(BLEND_RESET_ANIMATION);
      msg.writeFloat(1f);
      msg.writeFloat(1f);
      msg.writeBoolean(true);
      msg.writeFloat(1f);
      msg.writeByte(0);
      msg.writeFloat(BLEND_RESET_POSITION);
      msg.writeFloat(BLEND_RESET_POSITION);
      msg.writeFloat(1f);
      msg.writeFloat(1f);
   }

   /** Kiểu 0 (ảnh) / 1 (ảnh động): x/y client chia 100, w/h giữ pixel, 2 số parallax, cờ lật/lặp, màu RGBA. */
   private static void writeSprite(Message msg, MapTemplate.MapObject o) {
      if (o.kind() == MapTemplate.ObjectKind.TEXTURE) {
         msg.writeByte(0);
         msg.writeString(o.sprite());
      } else {
         msg.writeByte(1);
         msg.writeInt(Integer.parseInt(o.sprite()));
      }
      msg.writeFloat(o.x());
      msg.writeFloat(o.y());
      msg.writeFloat(o.w());
      msg.writeFloat(o.h());
      msg.writeFloat(o.parallaxX());
      msg.writeFloat(o.parallaxY());
      msg.writeBoolean(o.flipX());
      msg.writeBoolean(o.flipY());
      msg.writeBoolean(o.tiledX());
      msg.writeBoolean(o.tiledY());
      for (float c : o.rgba()) {
         msg.writeFloat(c); // màu RGBA (mặc định 1,1,1,1 = không nhuộm)
      }
   }

   /**
    * Kiểu 3 (skeleton — client {@code setupSkeleton}): str skeleton ("skeletons/vfxs/..."), float tỉ lệ đọc (client
    * chia 100), str animation, float scaleX, float scaleY (âm = lật), bool lặp, float tốc độ, byte n + n×str skin,
    * float x, float y (client chia 100), float parallaxX, float parallaxY.
    */
   private static void writeSkeleton(Message msg, MapTemplate.MapObject o) {
      msg.writeByte(3);
      msg.writeString(o.sprite());
      msg.writeFloat(o.scale());
      msg.writeString(o.anim());
      msg.writeFloat(o.scaleX());
      msg.writeFloat(o.scaleY());
      msg.writeBoolean(o.loop());
      msg.writeFloat(o.timeScale());
      msg.writeByte(o.skins().size());
      for (String skin : o.skins()) {
         msg.writeString(skin);
      }
      msg.writeFloat(o.x());
      msg.writeFloat(o.y());
      msg.writeFloat(o.parallaxX());
      msg.writeFloat(o.parallaxY());
   }

   /**
    * Kiểu 2 (hạt): 1 nhóm gồm 1 hiệu ứng; cờ bám camera + parallax. x/y và scale đều bị client chia 100:
    * kích thước hạt trong particle_effect tính bằng pixel, nên scale 1.0 -> 0.01 mới đổi đúng sang đơn vị thế
    * giới (lúc vẽ client nhân lại 100). Gửi scale*100 sẽ làm mọi hạt to gấp 100 lần, phủ kín màn hình.
    */
   private static void writeParticle(Message msg, MapTemplate.MapObject o) {
      msg.writeByte(2);
      msg.writeByte(1);
      msg.writeInt(Integer.parseInt(o.sprite()));
      msg.writeFloat(o.x());
      msg.writeFloat(o.y());
      msg.writeFloat(o.scale());
      msg.writeBoolean(o.followCamera());
      msg.writeFloat(o.parallaxX());
      msg.writeFloat(o.parallaxY());
   }

   // ------------------------------------------------------------------ opcode 24 / 54

   /**
    * Op 24 sub 0 (client {@code handleNpcUpdate}): byte n, n × (int id thực thể, float x, float y (client chia 100),
    * int npc_template.id, bool bóng, byte hướng: 1 = phải, khác = trái). id thực thể = vị trí trong {@code npcs}.
    */
   static List<Message> buildNpcs(MapTemplate map) {
      return buildNpcs(map, false);
   }

   /** {@code skipEvent}: bỏ NPC thuộc sự kiện ({@code GameEvents.EVENT_NPCS}); id thực thể vẫn = vị trí trong danh sách. */
   static List<Message> buildNpcs(MapTemplate map, boolean skipEvent) {
      List<Message> out = new ArrayList<>();
      List<Integer> indexes = new ArrayList<>();
      for (int i = 0; i < map.npcs().size(); i++) {
         if (!skipEvent || !com.tlnt.server.event.GameEvents.isEventNpc(map.npcs().get(i).id())) {
            indexes.add(i);
         }
      }
      List<MapTemplate.NpcSpawn> npcs = map.npcs();
      for (int from = 0; from < indexes.size(); from += MAX_PER_PACKET) {
         int to = Math.min(indexes.size(), from + MAX_PER_PACKET);
         Message msg = new Message(24);
         msg.writeByte(0);
         msg.writeByte(to - from);
         for (int k = from; k < to; k++) {
            int i = indexes.get(k);
            MapTemplate.NpcSpawn npc = npcs.get(i);
            msg.writeInt(i);
            msg.writeFloat(npc.x());
            msg.writeFloat(npc.y());
            msg.writeInt(npc.id());
            msg.writeBoolean(npc.shadow());
            msg.writeByte(npc.flipX() ? 1 : 0); // client: 1 = RIGHT (scaleX = -hướng.x, skeleton gốc quay trái)
         }
         out.add(msg);
      }
      return out;
   }

   /** Op 24 sub 0 chỉ các NPC mẫu {@code npcTemplateId} của map (bật sự kiện: hiện NPC cho người đang đứng trong map). */
   public static Packet npcPacket(MapTemplate map, int npcTemplateId) {
      Message msg = new Message(24);
      msg.writeByte(0);
      List<Integer> idx = new ArrayList<>();
      for (int i = 0; i < map.npcs().size(); i++) {
         if (map.npcs().get(i).id() == npcTemplateId) {
            idx.add(i);
         }
      }
      if (idx.isEmpty()) {
         return null;
      }
      msg.writeByte(idx.size());
      for (int i : idx) {
         MapTemplate.NpcSpawn npc = map.npcs().get(i);
         msg.writeInt(i);
         msg.writeFloat(npc.x());
         msg.writeFloat(npc.y());
         msg.writeInt(npc.id());
         msg.writeBoolean(npc.shadow());
         msg.writeByte(npc.flipX() ? 1 : 0);
      }
      return msg.toPacket();
   }

   /**
    * Cá bơi (skeleton {@code vfxs/fish}, tỉ lệ 0.25): thân cá vẽ cao hơn điểm đặt ~31 px, cao ~45 px. Client giữ ĐIỂM
    * ĐẶT trong hộp -> hộp gửi đi là dải chìm dưới nước: chừa {@link #FISH_TOP_GAP} dưới mặt nước (bọt trắng của ảnh
    * waterflow ~25 px + thân cá) và {@link #FISH_BOTTOM_GAP} trên đáy.
    */
   static final float FISH_TOP_GAP = 90f;
   static final float FISH_BOTTOM_GAP = 15f;

   /**
    * Op 25 sub 1 ({@code MessageHandler} case 25): cá bơi trang trí trong vùng nước. int n, n × (float x, float y,
    * float w, float h — pixel, client chia 100; int số cá). Client xoá cá cũ khi nhận op 6. Loài cá (skin fish1..5)
    * client tự random mỗi con.
    */
   static Message buildWaters(MapTemplate map) {
      Message msg = new Message(25);
      msg.writeByte(1);
      List<MapTemplate.Water> waters = map.waters();
      msg.writeInt(waters.size());
      for (MapTemplate.Water water : waters) {
         float bottom = water.y() + Math.min(FISH_BOTTOM_GAP, water.h() * 0.1f);
         float top = Math.max(bottom + 1, water.y() + water.h() - FISH_TOP_GAP);
         msg.writeFloat(water.x() + 20); // không bơi dính mép trái / phải
         msg.writeFloat(bottom);
         msg.writeFloat(Math.max(1, water.w() - 40));
         msg.writeFloat(top - bottom);
         msg.writeInt(water.fish());
      }
      return msg;
   }
}
