package com.tlnt.server.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 1 món quà / hàng cấu hình bằng JSON trong SQL (phúc lợi, giftcode, shop — TLNT-Editor ghi). {@link #create} tạo item
 * thật lúc nhận. Item 1 = vàng (khoá -> vàng khoá).
 *
 * <pre>
 * {"i":150,"q":10,"l":1,"s":[[9,100],[49,5]],"d":3}
 *  i mẫu item | q số lượng (bỏ = 1) | l 1 = khoá (bỏ = theo mặc định nơi dùng) | s dòng chỉ số [[stat_type, giá trị]]
 *  d hạn dùng (ngày tính từ lúc nhận, bỏ = vĩnh viễn); [min, max] = random mỗi lần tạo (đèn lồng shop Trung thu 1-3 ngày)
 *  giá trị option có thể là KHOẢNG [min, max]: {"s":[[50,[50,500]]]} = option 50 random 50..500 mỗi lần tạo item
 * </pre>
 * Nơi dùng thêm khoá riêng (giftcode "c"/"g", shop "p"/"cur") — đọc từ {@link JsonObject} gốc.
 *
 * @param stats dòng chỉ số {@code [[statTypeId, giá trị nhỏ nhất...], ...]} ("option id X pram Y")
 * @param statsMax cùng hình dạng {@code stats}: giá trị lớn nhất từng ô (bằng {@code stats} = cố định)
 * @param expireDays hạn dùng tính từ lúc nhận (ngày), 0 = vĩnh viễn
 * @param expireDaysMax hạn dùng lớn nhất (= expireDays: cố định)
 */
public record RewardItem(int templateId, int quantity, boolean locked, int[][] stats, int[][] statsMax, int expireDays,
      int expireDaysMax) {

   /** Hạn dùng cố định. */
   public RewardItem(int templateId, int quantity, boolean locked, int[][] stats, int[][] statsMax, int expireDays) {
      this(templateId, quantity, locked, stats, statsMax, expireDays, expireDays);
   }

   private static final int[][] NO_STATS = new int[0][];

   /** Option cố định (không random). */
   public RewardItem(int templateId, int quantity, boolean locked, int[][] stats, int expireDays) {
      this(templateId, quantity, locked, stats, stats, expireDays);
   }

   /** Tạo item thật; option dạng khoảng random đều trong [min, max] (mỗi lần gọi 1 lần random). */
   public ItemData create(long now) {
      ItemData item = ItemData.of(templateId, Math.max(1, quantity));
      item.locked = locked;
      item.createdTime = now;
      for (int k = 0; k < stats.length; k++) {
         int[] stat = stats[k];
         if (stat != null && stat.length >= 2) {
            int[] values = new int[stat.length - 1];
            for (int j = 1; j < stat.length; j++) {
               int max = statsMax[k][j];
               values[j - 1] = max > stat[j] ? ThreadLocalRandom.current().nextInt(stat[j], max + 1) : stat[j];
            }
            item.stats.add(new ItemData.StatValue(stat[0], values));
         }
      }
      if (expireDays > 0) {
         int days = expireDaysMax > expireDays ? ThreadLocalRandom.current().nextInt(expireDays, expireDaysMax + 1)
               : expireDays;
         item.expireTime = now + days * 86_400_000L;
      }
      return item;
   }

   /** Có option dạng khoảng (min khác max) không. */
   public boolean ranged() {
      for (int k = 0; k < stats.length; k++) {
         for (int j = 1; j < stats[k].length; j++) {
            if (statsMax[k][j] != stats[k][j]) {
               return true;
            }
         }
      }
      return false;
   }

   /** 1 món từ JSON; {@code defaultLocked} khi không ghi "l". null nếu thiếu "i". */
   public static RewardItem fromJson(JsonObject o, boolean defaultLocked) {
      if (o == null || !o.has("i")) {
         return null;
      }
      int[][][] minMax = stats(o.get("s"));
      int[] days = {0, 0};
      if (o.has("d") && o.get("d").isJsonArray() && o.getAsJsonArray("d").size() >= 2) {
         int a = o.getAsJsonArray("d").get(0).getAsInt();
         int b = o.getAsJsonArray("d").get(1).getAsInt();
         days = new int[] {Math.min(a, b), Math.max(a, b)};
      } else if (o.has("d") && o.get("d").isJsonPrimitive()) {
         days = new int[] {o.get("d").getAsInt(), o.get("d").getAsInt()};
      }
      return new RewardItem(o.get("i").getAsInt(), o.has("q") ? o.get("q").getAsInt() : 1,
            o.has("l") ? o.get("l").getAsInt() != 0 : defaultLocked, minMax[0], minMax[1], days[0], days[1]);
   }

   /** Mảng JSON -> các object (bỏ phần tử hỏng). JSON rỗng / sai -> rỗng + cảnh báo. */
   public static List<JsonObject> objects(String json, String where) {
      List<JsonObject> list = new ArrayList<>();
      if (json == null || json.isBlank()) {
         return list;
      }
      try {
         JsonElement root = JsonParser.parseString(json);
         if (root.isJsonArray()) {
            for (JsonElement e : root.getAsJsonArray()) {
               if (e.isJsonObject()) {
                  list.add(e.getAsJsonObject());
               }
            }
         }
      } catch (RuntimeException e) {
         Log.warn("%s: JSON sai -> bo qua (%s)", where, e.getMessage());
      }
      return list;
   }

   /** Danh sách món trong mảng JSON (mỗi phần tử 1 món). */
   public static List<RewardItem> list(String json, boolean defaultLocked, String where) {
      List<RewardItem> items = new ArrayList<>();
      for (JsonObject o : objects(json, where)) {
         RewardItem item = fromJson(o, defaultLocked);
         if (item != null) {
            items.add(item);
         }
      }
      return items;
   }

   /** {@code [min, max]}: mỗi giá trị là số (cố định) hoặc mảng [a, b] (khoảng, thứ tự nào cũng được). */
   private static int[][][] stats(JsonElement e) {
      if (e == null || !e.isJsonArray()) {
         return new int[][][] {NO_STATS, NO_STATS};
      }
      JsonArray a = e.getAsJsonArray();
      int[][] min = new int[a.size()][];
      int[][] max = new int[a.size()][];
      for (int i = 0; i < a.size(); i++) {
         JsonArray line = a.get(i).isJsonArray() ? a.get(i).getAsJsonArray() : new JsonArray();
         min[i] = new int[line.size()];
         max[i] = new int[line.size()];
         for (int k = 0; k < line.size(); k++) {
            JsonElement v = line.get(k);
            if (v.isJsonArray() && k > 0) {
               JsonArray range = v.getAsJsonArray();
               int lo = range.size() > 0 ? range.get(0).getAsInt() : 0;
               int hi = range.size() > 1 ? range.get(1).getAsInt() : lo;
               min[i][k] = Math.min(lo, hi);
               max[i][k] = Math.max(lo, hi);
            } else {
               min[i][k] = v.getAsInt();
               max[i][k] = min[i][k];
            }
         }
      }
      return new int[][][] {min, max};
   }
}
