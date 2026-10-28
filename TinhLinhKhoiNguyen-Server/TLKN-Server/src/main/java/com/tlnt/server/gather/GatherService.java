package com.tlnt.server.gather;

import com.tlnt.server.item.RewardItem;
import com.tlnt.server.player.ItemData;

import java.util.ArrayList;
import java.util.List;

/**
 * Săn bắt tổ ong / quả dừa ({@link GatherConfig}). Mọc / bị đánh / ong đốt / rơi đồ chạy trên luồng tick khu
 * ({@code world/ZoneMobs}, {@code world/ZoneCombat}); phá xong đồ RƠI xuống đất ({@code world/ZoneDrops}), người phá
 * được ưu tiên nhặt một lúc rồi ai nhặt cũng được ({@code item/GroundItemService}). Ở đây: nạp cấu hình + quay đồ rơi.
 */
public final class GatherService {

   private final GatherRepository repository;

   public GatherService(GatherRepository repository) {
      this.repository = repository;
      reload();
   }

   /** Đọc lại {@code mob_template.gather} (luồng blocking). Lỗi / trống thì giữ bản cũ (mặc định). */
   public void reload() {
      GatherConfig loaded = repository.load();
      if (loaded != null) {
         GatherConfig.set(loaded);
      }
   }

   /** Đồ rơi của 1 lần phá: món KHOÁ, riêng {@code roll} (0..99) &lt; {@code unlock} thì KHÔNG khoá. */
   public static List<ItemData> roll(GatherConfig.Entry entry, long now, int roll) {
      boolean unlocked = roll < entry.unlockPercent();
      List<ItemData> got = new ArrayList<>();
      for (RewardItem r : entry.items()) {
         ItemData item = r.create(now);
         item.locked = !unlocked;
         got.add(item);
      }
      return got;
   }
}
