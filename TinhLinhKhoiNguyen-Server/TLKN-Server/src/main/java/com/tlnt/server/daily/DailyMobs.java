package com.tlnt.server.daily;

import com.tlnt.server.map.MapTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * Chọn quái cho NV "Đánh hạ x" / "Đánh hạ Tinh Anh x": chỉ quái ở map THƯỜNG (không phó bản) mà nhân vật ĐÃ ĐƯỢC
 * vào (khoá map theo nhiệm vụ chính tuyến), cấp gần cấp nhân vật nhất và không thấp hơn quá {@link #PENALTY_GAP} cấp
 * (người cao hơn quái ≥ 10 cấp chỉ gây 20% sát thương — {@code MobRules}). Bất biến sau khi dựng.
 */
final class DailyMobs {

   /** Lệch cấp (người − quái) từ mức này bị giảm sát thương. */
   static final int PENALTY_GAP = 10;

   /** 1 loại quái ở 1 map. {@code elite} = có biến thể Tinh anh. */
   record Option(int mobId, int level, int mapId, boolean elite) {
   }

   private final List<Option> options;

   DailyMobs(List<Option> options) {
      this.options = List.copyOf(options);
   }

   /**
    * Từ các map thường: mỗi (quái, map) 1 lựa chọn, cấp = điểm sinh đầu tiên. Bỏ quái {@code excluded} (Người rơm có
    * NV riêng) và quái không có trong dữ liệu ({@code variants} trả -1).
    *
    * @param variants mob_template.id -> số biến thể (≥ 2 = có Tinh anh), -1 = không có mẫu
    */
   static DailyMobs of(List<MapTemplate> maps, Set<Integer> excluded, java.util.function.IntUnaryOperator variants) {
      Map<Long, Option> seen = new LinkedHashMap<>();
      for (MapTemplate map : maps) {
         for (MapTemplate.MobSpawn spawn : map.mobs()) {
            int count = variants.applyAsInt(spawn.id());
            if (excluded.contains(spawn.id()) || count < 0) {
               continue;
            }
            seen.putIfAbsent(((long) spawn.id() << 32) | map.id(),
                  new Option(spawn.id(), spawn.level(), map.id(), count >= 2));
         }
      }
      return new DailyMobs(new ArrayList<>(seen.values()));
   }

   List<Option> options() {
      return options;
   }

   /**
    * Quái cho nhân vật cấp {@code level}: trong các map {@code canEnter} cho vào (và có Tinh anh nếu {@code elite}),
    * ưu tiên quái không bị giảm sát thương; lệch cấp nhỏ nhất + {@code spread} -> random 1 loại. Không có quái thấp
    * hơn 10 cấp nào -> quái cấp cao nhất được vào. null = không có quái nào (nhân vật mới chưa mở map có quái).
    */
   Option choose(int level, IntPredicate canEnter, boolean elite, int spread, Random random) {
      List<Option> eligible = new ArrayList<>();
      for (Option o : options) {
         if ((!elite || o.elite()) && canEnter.test(o.mapId())) {
            eligible.add(o);
         }
      }
      if (eligible.isEmpty()) {
         return null;
      }
      List<Option> fair = new ArrayList<>();
      for (Option o : eligible) {
         if (level - o.level() < PENALTY_GAP) {
            fair.add(o);
         }
      }
      List<Option> pool;
      if (fair.isEmpty()) { // mọi quái đều thấp hơn quá xa: lấy nhóm cấp cao nhất
         int top = eligible.stream().mapToInt(Option::level).max().orElse(0);
         pool = eligible.stream().filter(o -> o.level() >= top - spread).toList();
      } else {
         int best = fair.stream().mapToInt(o -> Math.abs(level - o.level())).min().orElse(0);
         pool = fair.stream().filter(o -> Math.abs(level - o.level()) <= best + spread).toList();
      }
      // mỗi loại quái 1 phiếu (quái ở nhiều map không được ưu tiên hơn), giữ map có cấp gần nhất
      Map<Integer, Option> byMob = new LinkedHashMap<>();
      for (Option o : pool.stream().sorted(Comparator.comparingInt(o -> Math.abs(level - o.level()))).toList()) {
         byMob.putIfAbsent(o.mobId(), o);
      }
      List<Option> unique = new ArrayList<>(byMob.values());
      return unique.get(random.nextInt(unique.size()));
   }

   /** Mọi loại quái có trong danh sách (để test / log). */
   Set<Integer> mobIds() {
      Set<Integer> ids = new HashSet<>();
      for (Option o : options) {
         ids.add(o.mobId());
      }
      return ids;
   }
}
