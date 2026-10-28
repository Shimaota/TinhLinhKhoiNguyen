package com.tlnt.server.worldboss;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.player.ItemData;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 1 dòng quà boss thế giới (cột JSON {@code tlnt_data.world_boss.loot}, mỗi phần tử 1 dòng):
 * <pre>
 * {"i":1,"n":5,"q":[10000,20000],"l":0}      5 món vàng KHÔNG khoá, mỗi món 10.000-20.000
 * {"i":268,"c":50}                            Túi linh thạch, 50% rơi, khoá
 * {"i":1,"n":[1,3],"q":[5000,10000],"l":0}   1-3 món vàng không khoá (số món random)
 * {"i":187,"s":[[3,[500,2000]],[9,[50,100]],[30,[1,5]]],"d":[1,3],"perm":0.5}
 *                                             ngoại trang option random, hạn 1-3 ngày, 0,5% vĩnh viễn
 * </pre>
 * {@code i} mẫu item · {@code n} số món riêng rẽ: số hoặc [min, max] = random (bỏ = 1) · {@code q} số lượng mỗi món: số hoặc [min, max] (bỏ = 1) ·
 * {@code l} 0 = không khoá (bỏ = KHOÁ) · {@code s} dòng chỉ số như {@link RewardItem} (giá trị [min, max] = random) ·
 * {@code c} % rơi (bỏ = 100, cả dòng cùng ra / không ra) · {@code d} hạn dùng ngày: số hoặc [min, max] (bỏ = vĩnh viễn) ·
 * {@code perm} % ra vĩnh viễn dù có {@code d}. Bất biến.
 */
public record WorldBossLoot(RewardItem item, int minPiles, int piles, int minQty, int maxQty, double chance,
      int minDays, int maxDays, double permPercent) {

   private static final long DAY_MS = 86_400_000L;

   /** Tung dòng này: qua tỉ lệ thì ra {@code minPiles}..{@code piles} món. */
   void roll(Random random, long now, List<ItemData> out) {
      if (chance < 100 && random.nextDouble() * 100 >= chance) {
         return;
      }
      int count = piles > minPiles ? minPiles + random.nextInt(piles - minPiles + 1) : piles;
      for (int i = 0; i < count; i++) {
         ItemData it = item.create(now); // option dạng khoảng random mỗi món
         it.quantity = maxQty > minQty ? minQty + random.nextInt(maxQty - minQty + 1) : minQty;
         if (maxDays > 0) {
            boolean permanent = permPercent > 0 && random.nextDouble() * 100 < permPercent;
            int days = maxDays > minDays ? minDays + random.nextInt(maxDays - minDays + 1) : maxDays;
            it.expireTime = permanent ? ItemData.PERMANENT : now + days * DAY_MS;
         }
         out.add(it);
      }
   }

   /** Tung cả bảng. */
   public static List<ItemData> rollAll(List<WorldBossLoot> table, Random random) {
      long now = System.currentTimeMillis();
      List<ItemData> out = new ArrayList<>();
      for (WorldBossLoot loot : table) {
         loot.roll(random, now, out);
      }
      return out;
   }

   /** 1 dòng từ JSON; null nếu thiếu {@code i}. */
   static WorldBossLoot fromJson(JsonObject o) {
      if (o == null || !o.has("i")) {
         return null;
      }
      JsonObject base = new JsonObject(); // RewardItem chỉ đọc i / l / s (q, d ở đây có dạng khoảng)
      base.add("i", o.get("i"));
      base.addProperty("l", o.has("l") ? o.get("l").getAsInt() : 1);
      if (o.has("s")) {
         base.add("s", o.get("s"));
      }
      RewardItem item = RewardItem.fromJson(base, true);
      int[] q = range(o.get("q"), 1);
      int[] d = range(o.get("d"), 0);
      int[] n = range(o.get("n"), 1);
      return new WorldBossLoot(item, Math.max(1, n[0]), Math.max(1, n[1]), Math.max(1, q[0]),
            Math.max(1, q[1]), o.has("c") ? o.get("c").getAsDouble() : 100, Math.max(0, d[0]), Math.max(0, d[1]),
            o.has("perm") ? o.get("perm").getAsDouble() : 0);
   }

   /** Số -> [v, v]; mảng [a, b] -> [min, max]; không có -> [def, def]. */
   private static int[] range(JsonElement e, int def) {
      if (e == null || e.isJsonNull()) {
         return new int[] {def, def};
      }
      if (e.isJsonArray()) {
         JsonArray a = e.getAsJsonArray();
         int lo = a.size() > 0 ? a.get(0).getAsInt() : def;
         int hi = a.size() > 1 ? a.get(1).getAsInt() : lo;
         return new int[] {Math.min(lo, hi), Math.max(lo, hi)};
      }
      int v = e.getAsInt();
      return new int[] {v, v};
   }
}
