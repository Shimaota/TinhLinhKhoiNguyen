package com.tlnt.server.bot;

import com.tlnt.server.combat.MobRules;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.thunder.ThunderRules;
import com.tlnt.server.world.Arena;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Chỗ đặt người chơi giả, dựng 1 lần từ dữ liệu map (bất biến):
 * <ul>
 *   <li>Bãi up = từng nhóm quái THƯỜNG trong cột {@code mobs} của map thường (không phó bản / Lôi vực / đấu trường).</li>
 *   <li>Chỗ đứng = cạnh NPC (làng 2 đông gấp {@link #VILLAGE_WEIGHT}), quanh điểm xuất hiện — luôn cách cửa dịch chuyển
 *       ≥ {@link #DOOR_PAD_X} px ({@link #nearDoor}).</li>
 *   <li>Chỗ câu = mép vùng nước (đứng đó câu được — {@code World.fishingSpots}).</li>
 * </ul>
 * Mọi lần chọn đều lọc theo map đã mở ở cấp đó ({@link BotProgress#canEnter}) — bot cấp thấp không đứng map cao.
 */
final class BotPlaces {

   record GrindSpot(int mapId, int mobTemplateId, int mobLevel, float x, float y, int range, double weight) {
   }

   record IdleSpot(int mapId, float x, float y, float spread, int npcId, double weight) {
   }

   /** Chỗ câu: đứng (x, y), mặt nước ở hoành độ {@code waterX} (quay mặt về đó lúc quăng cần). */
   record FishSpot(int mapId, float x, float y, float waterX) {
   }

   /** Hộp cửa dịch chuyển (đã nới {@link #DOOR_PAD_X} / {@link #DOOR_PAD_Y}) — bot không đứng / đi lại trong đó. */
   private record Door(float x0, float x1, float y0, float y1) {
   }

   /** Bot đứng cách cửa dịch chuyển ít nhất bấy nhiêu px (ngang) — đứng ngay cửa trông như người đang đổi map / kẹt. */
   static final float DOOR_PAD_X = 280;
   static final float DOOR_PAD_Y = 260;
   /** Bot không đứng / đi sát mép trái-phải map hơn bấy nhiêu px (đứng dí vào góc màn hình trông như kẹt). */
   static final float EDGE_PAD = 350;
   /** 2 bot câu cá cách nhau ít nhất bấy nhiêu px (người thật không đứng chồng lên nhau câu). */
   static final float FISH_GAP = 170;

   /** 1 loại quái thường của map + cấp. */
   private record MapMob(int templateId, int level) {
   }

   static final int VILLAGE_MAP = 2;
   static final double VILLAGE_WEIGHT = 4;

   final List<GrindSpot> grind;
   final List<IdleSpot> idle;
   final List<FishSpot> fish;
   private final Map<Integer, List<MapMob>> mapMobs;
   private final Map<Integer, List<Door>> doors;
   /** Bề rộng map (px). */
   private final Map<Integer, Integer> widths;

   private BotPlaces(List<GrindSpot> grind, List<IdleSpot> idle, List<FishSpot> fish, Map<Integer, List<MapMob>> mapMobs,
         Map<Integer, List<Door>> doors, Map<Integer, Integer> widths) {
      this.grind = List.copyOf(grind);
      this.idle = List.copyOf(idle);
      this.fish = List.copyOf(fish);
      this.mapMobs = mapMobs;
      this.doors = doors;
      this.widths = widths;
   }

   /** ({@code x}, {@code y}) ở map {@code mapId} có sát cửa dịch chuyển / sát mép map không (bot không đứng đó). */
   boolean nearDoor(int mapId, float x, float y) {
      Integer w = widths.get(mapId);
      if (w != null && (x < EDGE_PAD || x > w - EDGE_PAD)) {
         return true;
      }
      for (Door d : doors.getOrDefault(mapId, List.of())) {
         if (x >= d.x0 && x <= d.x1 && y >= d.y0 && y <= d.y1) {
            return true;
         }
      }
      return false;
   }

   /**
    * Hoành độ quanh {@code cx} ± {@code spread} không sát cửa (thử vài lần); không tìm được thì NaN. {@code spread} 0 =
    * chỉ kiểm tra {@code cx}.
    */
   float awayFromDoor(int mapId, float cx, float y, float spread, Random rnd) {
      for (int i = 0; i < 10; i++) {
         float x = spread <= 0 ? cx : cx + (rnd.nextFloat() * 2 - 1) * spread;
         if (!nearDoor(mapId, x, y)) {
            return x;
         }
         if (spread <= 0) {
            break;
         }
      }
      return Float.NaN;
   }

   static boolean usable(MapTemplate m) {
      return !ThunderRules.isThunderMap(m.id()) && !Arena.isArenaMap(m.id()) && m.maxPlayersPerZone() > 1;
   }

   /** {@code fishingSpots}: map -> chỗ đứng câu được (World.fishingSpots). */
   static BotPlaces of(List<MapTemplate> maps, java.util.function.IntFunction<List<float[]>> fishingSpots) {
      List<GrindSpot> grind = new ArrayList<>();
      List<IdleSpot> idle = new ArrayList<>();
      List<FishSpot> fish = new ArrayList<>();
      Map<Integer, List<MapMob>> mapMobs = new HashMap<>();
      Map<Integer, List<Door>> doors = new HashMap<>();
      Map<Integer, Integer> widths = new HashMap<>();
      for (MapTemplate m : maps) {
         if (!usable(m)) {
            continue;
         }
         widths.put(m.id(), m.width());
         List<Door> mapDoors = new ArrayList<>();
         if (m.waypoints() != null) {
            for (MapTemplate.Waypoint w : m.waypoints()) {
               mapDoors.add(new Door(w.x() - DOOR_PAD_X, w.x() + w.w() + DOOR_PAD_X, w.y() - DOOR_PAD_Y,
                     w.y() + w.h() + DOOR_PAD_Y));
            }
         }
         doors.put(m.id(), List.copyOf(mapDoors));
         if (m.mobs() != null) {
            Set<MapMob> kinds = new LinkedHashSet<>();
            for (MapTemplate.MobSpawn s : m.mobs()) {
               if (s.variant() != MobRules.VARIANT_NORMAL) {
                  continue; // bãi tinh anh / thủ lĩnh: người thường không cắm ở đó
               }
               int lv = Math.max(1, s.level());
               kinds.add(new MapMob(s.id(), lv));
               grind.add(new GrindSpot(m.id(), s.id(), lv, s.x(), s.y(), Math.max(150, s.range()),
                     s.count() / (1.0 + lv / 30.0)));
            }
            mapMobs.put(m.id(), List.copyOf(kinds));
         }
         double village = m.id() == VILLAGE_MAP ? VILLAGE_WEIGHT : 1;
         if (m.npcs() != null) {
            for (MapTemplate.NpcSpawn n : m.npcs()) {
               idle.add(new IdleSpot(m.id(), n.x(), n.y(), 220, n.id(), 2 * village));
            }
         }
         idle.add(new IdleSpot(m.id(), m.spawnX(), m.spawnY(), 600, 0, village)); // rải rác quanh điểm xuất hiện
         for (float[] p : fishingSpots.apply(m.id())) {
            boolean door = p[0] < EDGE_PAD || p[0] > m.width() - EDGE_PAD;
            for (Door d : mapDoors) {
               door |= p[0] >= d.x0 && p[0] <= d.x1 && p[1] >= d.y0 && p[1] <= d.y1;
            }
            if (!door) {
               fish.add(new FishSpot(m.id(), p[0], p[1], p[2]));
            }
         }
      }
      return new BotPlaces(grind, idle, fish, mapMobs, doors, widths);
   }

   /** Bãi up có quái cấp trong [{@code minLevel} − 4, {@code maxLevel}] (trọng số); null = không có. */
   GrindSpot pickGrind(Random rnd, int minLevel, int maxLevel) {
      return pick(grind, rnd, s -> s.mobLevel >= minLevel - 4 && s.mobLevel <= maxLevel, GrindSpot::weight);
   }

   /** Chỗ đứng ở map người cấp {@code level} đã tới được; null = không có. */
   IdleSpot pickIdle(Random rnd, int level) {
      return pick(idle, rnd, s -> BotProgress.canEnter(level, s.mapId), IdleSpot::weight);
   }

   /**
    * Chỗ câu ở map đã tới được, cách mọi bot câu khác ({@code taken}: {mapId, x}) ≥ {@link #FISH_GAP}; null = hết chỗ.
    */
   FishSpot pickFish(Random rnd, int level, List<float[]> taken) {
      return pick(fish, rnd, s -> {
         if (!BotProgress.canEnter(level, s.mapId)) {
            return false;
         }
         for (float[] t : taken) {
            if ((int) t[0] == s.mapId && Math.abs(t[1] - s.x) < FISH_GAP) {
               return false;
            }
         }
         return true;
      }, s -> s.mapId == VILLAGE_MAP ? 3.0 : 1.0);
   }

   /**
    * Loại quái kiểu "auto đánh mọi loại" của 1 bot cấp {@code level} ở map {@code mapId}: quái thường cấp ≤ cấp + 3 và ≥
    * cấp − 15 (người chơi bật "mọi loại quái" ở map hợp cấp; con quá cao / quá thấp bỏ). Rỗng = không con nào.
    */
   int[] templatesFor(int mapId, int level) {
      List<MapMob> list = mapMobs.getOrDefault(mapId, List.of());
      return list.stream().filter(m -> m.level <= level + 3 && m.level >= level - 15).mapToInt(MapMob::templateId)
            .distinct().toArray();
   }

   private static <T> T pick(List<T> list, Random rnd, Predicate<T> ok, java.util.function.ToDoubleFunction<T> weight) {
      double total = 0;
      for (T s : list) {
         if (ok.test(s)) {
            total += weight.applyAsDouble(s);
         }
      }
      if (total <= 0) {
         return null;
      }
      double r = rnd.nextDouble() * total;
      T last = null;
      for (T s : list) {
         if (ok.test(s)) {
            last = s;
            if ((r -= weight.applyAsDouble(s)) <= 0) {
               return s;
            }
         }
      }
      return last;
   }

   /** Cấp bot up ở bãi {@code s}: cấp quái + 0..4 (hay gặp +1, +2), kẹp [1, max]. */
   static int grindLevel(Random rnd, GrindSpot s, int maxLevel) {
      int[] offsets = {0, 1, 1, 2, 2, 2, 3, 3, 4};
      return Math.max(1, Math.min(Math.max(maxLevel, s.mobLevel), s.mobLevel + offsets[rnd.nextInt(offsets.length)]));
   }

   /** Cấp bot không đi up: lệch về phía thấp-trung trong [{@code min}, {@code max}]. */
   static int idleLevel(Random rnd, int min, int max) {
      double u = Math.pow(rnd.nextDouble(), 1.4);
      return min + (int) Math.round(u * (max - min));
   }
}
