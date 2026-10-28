package com.tlnt.server.plant;

import com.tlnt.server.item.RewardItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cây hái được mọc NGẪU NHIÊN: mỗi loại ({@code plant_template.id}) mọc ở những map nào, mỗi khu tối đa bao nhiêu cây
 * (độ hiếm), hái xong bao lâu mọc lại, hái mất bao lâu, hái được gì. Đọc từ cột JSON {@code plant_template.harvest}
 * ({@link PlantRepository}, nạp lại mỗi phút); chưa có cột / không dòng nào cấu hình -> {@link #DEFAULT}.
 *
 * <pre>
 * {"maps":[1,2,3,4],"max":4,"total":20,"respawn":[60,180],"time":3000,"items":[{"i":147}]}
 *  maps    map mọc (map riêng tư như Nông trại bỏ qua)
 *  max     số cây loại này tối đa MỖI KHU cùng lúc — hiếm = nhỏ -> người chơi phải đi tìm, ai tới trước hái trước
 *  total   (tuỳ chọn) tổng số cây loại này tối đa TOÀN SERVER (mọi map trong "maps", mọi khu cộng lại); bỏ / 0 = không
 *          giới hạn tổng (chỉ còn max / khu). Hết suất thì khu nào có lượt mọc sau phải chờ tới khi có cây bị hái.
 *  respawn [min, max] giây: bị hái xong random trong khoảng này mới mọc lại, ở CHỖ NGẪU NHIÊN khác trên mặt đất
 *  time    ms thanh "Đang thu hoạch..." (đứng yên)
 *  items   món nhận được = RewardItem (i q l s d); "l" bỏ trống = KHÔNG khoá (giao dịch / kí gửi được)
 * </pre>
 * Cây đặt tay trong cột {@code map_template.plants} (editor) vẫn đứng đúng chỗ cũ, mọc lại sau {@code respawn} của
 * điểm đó; hái được nếu loại cây có cấu hình ở đây (dùng {@code time} + {@code items}), không tính vào {@code max}.
 */
public record PlantConfig(Map<Integer, Entry> byTemplate, Map<Integer, List<Entry>> byMap) {

   /**
    * 1 loại cây.
    *
    * @param max tối đa mỗi khu
    * @param total tối đa toàn server (0 = không giới hạn)
    * @param respawnMinSec / respawnMaxSec giây mọc lại sau khi bị hái (random trong khoảng)
    * @param harvestMs thời gian thu hoạch
    */
   public record Entry(int templateId, Set<Integer> maps, int max, int total, int respawnMinSec, int respawnMaxSec,
         int harvestMs, List<RewardItem> items) {

      /** Ms chờ mọc lại (random). */
      public long respawnDelayMs() {
         int lo = Math.max(1, Math.min(respawnMinSec, respawnMaxSec));
         int hi = Math.max(lo, Math.max(respawnMinSec, respawnMaxSec));
         return ThreadLocalRandom.current().nextInt(lo, hi + 1) * 1000L;
      }
   }

   /**
    * Mặc định: Quả mọng đỏ (item 147) / tím (148), Cỏ hồi sinh (238) ở map 1-4; Nấm hương (275) ở map 6-8. Càng hiếm
    * càng ít cây, mọc lại càng lâu. Đồ hái được KHÔNG khoá (bán / giao dịch được).
    */
   public static final PlantConfig DEFAULT = of(List.of(
         entry(8, Set.of(1, 2, 3, 4), 4, 60, 180, 3_000, 147), // Quả mọng đỏ
         entry(7, Set.of(1, 2, 3, 4), 2, 120, 300, 3_000, 148), // Quả mọng tím (EXP gấp 2,5 -> hiếm hơn)
         entry(9, Set.of(1, 2, 3, 4), 2, 240, 480, 4_000, 238), // Cỏ hồi sinh
         entry(10, Set.of(6, 7, 8), 3, 150, 360, 3_500, 275))); // Nấm hương

   private static volatile PlantConfig current = DEFAULT;

   public static PlantConfig current() {
      return current;
   }

   /** Đổi cấu hình (nạp lại từ DB; test). */
   public static void set(PlantConfig config) {
      current = config;
   }

   /** Cấu hình loại cây, null = không hái được. */
   public Entry entry(int templateId) {
      return byTemplate.get(templateId);
   }

   /** Các loại cây mọc ngẫu nhiên ở map (rỗng = không có). */
   public List<Entry> forMap(int mapId) {
      return byMap.getOrDefault(mapId, List.of());
   }

   public static PlantConfig of(List<Entry> entries) {
      Map<Integer, Entry> byTemplate = new HashMap<>();
      Map<Integer, List<Entry>> byMap = new HashMap<>();
      for (Entry e : entries) {
         byTemplate.put(e.templateId(), e);
         for (int mapId : e.maps()) {
            byMap.computeIfAbsent(mapId, k -> new ArrayList<>()).add(e);
         }
      }
      byMap.replaceAll((k, v) -> List.copyOf(v));
      return new PlantConfig(Map.copyOf(byTemplate), Map.copyOf(byMap));
   }

   /** Mặc định: 1 món KHÔNG khoá, không dòng chỉ số. */
   private static Entry entry(int templateId, Set<Integer> maps, int max, int respawnMin, int respawnMax,
         int harvestMs, int itemId) {
      return new Entry(templateId, maps, max, 0, respawnMin, respawnMax, harvestMs,
            List.of(new RewardItem(itemId, 1, false, new int[0][], 0)));
   }
}
