package com.tlnt.server.gather;

import com.tlnt.server.item.RewardItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * "Quái săn bắt" mọc NGẪU NHIÊN (Tổ ong mob 44, Quả dừa mob 45): quái đứng yên, ít HP, mỗi đòn trúng chỉ trừ 1 HP;
 * phá xong rơi đồ vào túi người đánh cuối. Đọc từ cột JSON {@code tlnt_data.mob_template.gather}
 * ({@link GatherRepository}, nạp lại mỗi phút); chưa có cột / không dòng nào -> {@link #DEFAULT}.
 *
 * <pre>
 * {"total":10,"perZone":1,"respawn":300,"level":1,"hp":10,"sting":10,"unlock":5,
 *  "items":[{"i":151}],
 *  "spots":{"1":[[5173.2,666.3],[4887.9,597.7]],"2":[[915.7,1039.4]]}}
 *  total    tổng số con TOÀN SERVER (mọi map, mọi khu); 0 = không giới hạn
 *  perZone  tối đa mỗi khu
 *  respawn  giây: bị phá xong bấy nhiêu lâu mới có con mới, ở 1 chỗ NGẪU NHIÊN (map / khu / điểm) khác
 *  hp       số đòn trúng để phá (mỗi đòn -1 HP, bất kể sát thương)
 *  sting    % HP TỐI ĐA người đánh bị trừ mỗi đòn trúng (ong đốt); 0 = không
 *  unlock   % món rơi ra KHÔNG khoá (còn lại khoá)
 *  items    món rơi = RewardItem (i q s d; "l" bị {@code unlock} quyết định)
 *  spots    map -> các điểm mọc (pixel, chân quái)
 * </pre>
 */
public record GatherConfig(Map<Integer, Entry> byMob, Map<Integer, List<Entry>> byMap) {

   /**
    * @param mobId mob_template.id
    * @param spots map_template.id -> các điểm {x, y}
    */
   public record Entry(int mobId, int total, int perZone, int respawnSec, int level, long hp, int stingPercent,
         int unlockPercent, List<RewardItem> items, Map<Integer, List<float[]>> spots) {

      public List<float[]> spots(int mapId) {
         return spots.getOrDefault(mapId, List.of());
      }
   }

   public static final int HIVE_MOB = 44;
   public static final int COCONUT_MOB = 45;
   /** item_template.id Mật ong / Quả dừa. */
   public static final int HONEY_ITEM = 151;
   public static final int COCONUT_ITEM = 146;

   /** Mặc định: 10 tổ ong + 10 quả dừa toàn server, 1 con / khu, 5 phút, 10 HP; ong đốt 10% HP; 5% rơi đồ không khoá. */
   public static final GatherConfig DEFAULT = of(List.of(
         new Entry(HIVE_MOB, 10, 1, 300, 1, 10, 10, 5, List.of(item(HONEY_ITEM)), Map.of(
               1, pts(5173.2f, 666.3f, 4887.9f, 597.7f, 4227.2f, 931.8f, 3451.5f, 930.4f, 1824.0f, 870.2f,
                     379.2f, 1187.2f, 783.7f, 977.1f, 6444.3f, 847.2f, 7352.0f, 974.4f),
               2, pts(915.7f, 1039.4f, 1483.9f, 858.1f, 5361.1f, 569.4f, 5647.9f, 645.7f, 6325.4f, 635.3f,
                     8105.5f, 625.9f, 7894.5f, 745.2f, 8199.7f, 815.2f, 10379.6f, 996.0f),
               3, pts(408.9f, 828.6f, 1306.8f, 588.7f, 3887.3f, 814.4f, 5502.0f, 1091.8f, 6191.6f, 1000.4f,
                     5942.1f, 512.3f, 7541.1f, 665.4f),
               4, pts(1753.7f, 461.4f, 3573.7f, 673.1f, 6569.8f, 1041.0f, 4770.1f, 766.2f, 4548.2f, 742.1f,
                     841.1f, 812.2f))),
         new Entry(COCONUT_MOB, 10, 1, 300, 1, 10, 0, 5, List.of(item(COCONUT_ITEM)), Map.of(
               1, pts(5724.6f, 917.1f),
               2, pts(2034.1f, 1088.6f, 9322.4f, 1143.7f)))));

   private static volatile GatherConfig current = DEFAULT;

   public static GatherConfig current() {
      return current;
   }

   static void set(GatherConfig config) {
      current = config;
   }

   /** Cấu hình quái săn bắt, null = quái thường. */
   public Entry entry(int mobId) {
      return byMob.get(mobId);
   }

   /** Các loại mọc ở map (có ít nhất 1 điểm mọc). */
   public List<Entry> forMap(int mapId) {
      return byMap.getOrDefault(mapId, List.of());
   }

   public static GatherConfig of(List<Entry> entries) {
      Map<Integer, Entry> byMob = new HashMap<>();
      Map<Integer, List<Entry>> byMap = new HashMap<>();
      for (Entry e : entries) {
         byMob.put(e.mobId(), e);
         e.spots().forEach((mapId, list) -> {
            if (!list.isEmpty()) {
               byMap.computeIfAbsent(mapId, k -> new ArrayList<>()).add(e);
            }
         });
      }
      byMap.replaceAll((k, v) -> List.copyOf(v));
      return new GatherConfig(Map.copyOf(byMob), Map.copyOf(byMap));
   }

   private static RewardItem item(int templateId) {
      return new RewardItem(templateId, 1, true, new int[0][], 0);
   }

   private static List<float[]> pts(float... xy) {
      List<float[]> out = new ArrayList<>();
      for (int i = 0; i + 1 < xy.length; i += 2) {
         out.add(new float[] {xy[i], xy[i + 1]});
      }
      return List.copyOf(out);
   }
}
