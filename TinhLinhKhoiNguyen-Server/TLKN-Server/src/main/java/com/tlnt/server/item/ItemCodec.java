package com.tlnt.server.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tlnt.server.player.ItemData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Định dạng JSON GỌN lưu item vào DB (cột item_* bảng player). Khoá 1-2 chữ, bỏ mọi giá trị mặc định:
 *
 * <pre>
 * item       {"i":38,"q":20,"l":1,"s":[[3,300],[15,20]]}
 *            i  = templateId            (bắt buộc)
 *            q  = số lượng              (bỏ nếu 1)
 *            l  = 1 nếu đã khoá         (bỏ nếu chưa)
 *            g  = starGrade, c = starCount, t = createdTime (bỏ nếu 0)
 *            up = % cộng của từng cấp nâng [4,12,...] (bỏ nếu mọi cấp 4%)
 *            e  = hết hạn lúc (epoch ms)  (bỏ nếu vĩnh viễn)
 *            s  = dòng chỉ số [[statTypeId, giá trị...], ...]   (bỏ nếu không có)
 *            sc = bùa linh hồn [item, ...]                       (bỏ nếu không có)
 *            es = ngọc khảm [[templateId, cấp, statTypeId, giá trị...] | null, ...] (bỏ nếu không có)
 * kho có ô   {"0":item,"5":item}                 (bag / box / body: key = số ô, chỉ ô có đồ; trống = {})
 * sưu tập    {"sel":0,"cap":14,"items":[item,...]} (sel bỏ nếu -1, cap bỏ nếu = mặc định)
 * </pre>
 *
 * CHỈ có định dạng này (dạng cũ đã bỏ). JSON không đúng dạng -> coi như trống.
 */
public final class ItemCodec {

   private ItemCodec() {
   }

   // ------------------------------------------------------------------ item

   public static JsonObject toJson(ItemData item) {
      JsonObject o = new JsonObject();
      o.addProperty("i", item.templateId);
      if (item.quantity != 1) {
         o.addProperty("q", item.quantity);
      }
      if (item.locked) {
         o.addProperty("l", 1);
      }
      if (item.starGrade != 0) {
         o.addProperty("g", item.starGrade);
      }
      if (item.starCount != 0) {
         o.addProperty("c", item.starCount);
      }
      if (item.upgradePercents != null) {
         JsonArray up = new JsonArray();
         for (byte p : item.upgradePercents) {
            up.add(p);
         }
         o.add("up", up);
      }
      if (item.upgradeWeek != 0) { // Top tuần Cường hóa: tuần + cấp đầu tuần
         o.addProperty("uw", item.upgradeWeek);
         o.addProperty("ub", item.upgradeWeekBase);
      }
      if (item.createdTime != 0) {
         o.addProperty("t", item.createdTime);
      }
      if (item.expireTime > 0) {
         o.addProperty("e", item.expireTime);
      }
      if (!item.stats.isEmpty()) {
         JsonArray stats = new JsonArray();
         for (ItemData.StatValue stat : item.stats) {
            JsonArray line = new JsonArray();
            line.add(stat.statTypeId());
            if (stat.values() != null) {
               for (int v : stat.values()) {
                  line.add(v);
               }
            }
            stats.add(line);
         }
         o.add("s", stats);
      }
      if (!item.soulCharms.isEmpty()) {
         JsonArray charms = new JsonArray();
         for (ItemData charm : item.soulCharms) {
            charms.add(toJson(charm));
         }
         o.add("sc", charms);
      }
      if (!item.elementStones.isEmpty()) {
         JsonArray stones = new JsonArray();
         for (ItemData.ElementStone stone : item.elementStones) {
            if (stone == null) {
               stones.add((JsonElement) null);
               continue;
            }
            JsonArray s = new JsonArray();
            s.add(stone.templateId());
            s.add(stone.level());
            s.add(stone.statTypeId());
            if (stone.values() != null) {
               for (int v : stone.values()) {
                  s.add(v);
               }
            }
            stones.add(s);
         }
         o.add("es", stones);
      }
      return o;
   }

   /** Đọc 1 item. Null nếu không phải item (thiếu {@code "i"}). */
   public static ItemData fromJson(JsonElement element) {
      if (element == null || !element.isJsonObject()) {
         return null;
      }
      JsonObject o = element.getAsJsonObject();
      if (!o.has("i")) {
         return null;
      }
      ItemData item = new ItemData();
      item.templateId = o.get("i").getAsInt();
      item.quantity = o.has("q") ? o.get("q").getAsInt() : 1;
      item.locked = o.has("l") && o.get("l").getAsInt() != 0;
      item.starGrade = o.has("g") ? o.get("g").getAsByte() : 0;
      item.starCount = o.has("c") ? o.get("c").getAsByte() : 0;
      if (o.has("up")) {
         JsonArray up = o.getAsJsonArray("up");
         item.upgradePercents = new byte[up.size()];
         for (int k = 0; k < up.size(); k++) {
            item.upgradePercents[k] = up.get(k).getAsByte();
         }
      }
      item.upgradeWeek = o.has("uw") ? o.get("uw").getAsInt() : 0;
      item.upgradeWeekBase = o.has("ub") ? o.get("ub").getAsByte() : 0;
      item.createdTime = o.has("t") ? o.get("t").getAsLong() : 0;
      item.expireTime = o.has("e") ? o.get("e").getAsLong() : ItemData.PERMANENT;
      if (o.has("s")) {
         for (JsonElement lineElement : o.getAsJsonArray("s")) {
            JsonArray line = lineElement.getAsJsonArray();
            int[] values = new int[line.size() - 1];
            for (int k = 0; k < values.length; k++) {
               values[k] = line.get(k + 1).getAsInt();
            }
            item.stats.add(new ItemData.StatValue(line.get(0).getAsInt(), values));
         }
      }
      if (o.has("sc")) {
         for (JsonElement charm : o.getAsJsonArray("sc")) {
            ItemData c = fromJson(charm);
            if (c != null) {
               item.soulCharms.add(c);
            }
         }
      }
      if (o.has("es")) {
         for (JsonElement stoneElement : o.getAsJsonArray("es")) {
            if (stoneElement == null || stoneElement.isJsonNull()) {
               item.elementStones.add(null);
               continue;
            }
            JsonArray s = stoneElement.getAsJsonArray();
            int[] values = new int[Math.max(0, s.size() - 3)];
            for (int k = 0; k < values.length; k++) {
               values[k] = s.get(k + 3).getAsInt();
            }
            item.elementStones.add(new ItemData.ElementStone(s.get(0).getAsInt(), s.get(1).getAsByte(),
                  s.get(2).getAsInt(), values));
         }
      }
      return item.normalize();
   }

   // ------------------------------------------------------------------ kho có ô

   public static String containerToJson(ItemContainer container) {
      JsonObject o = new JsonObject();
      for (int slot = 0; slot < container.size(); slot++) {
         ItemData item = container.get(slot);
         if (item != null) {
            o.add(Integer.toString(slot), toJson(item));
         }
      }
      return o.toString();
   }

   /** Đọc kho {@code {"ô":item}}. */
   public static ItemContainer containerFromJson(String json, int size) {
      ItemContainer container = new ItemContainer(size);
      JsonObject root = parseObject(json);
      if (root != null) {
         for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            put(container, parseSlot(entry.getKey()), fromJson(entry.getValue()));
         }
      }
      return container;
   }

   private static JsonObject parseObject(String json) {
      if (json == null || json.isBlank()) {
         return null;
      }
      JsonElement root = JsonParser.parseString(json);
      return root.isJsonObject() ? root.getAsJsonObject() : null;
   }

   private static int parseSlot(String key) {
      try {
         return Integer.parseInt(key);
      } catch (NumberFormatException e) {
         return -1;
      }
   }

   private static void put(ItemContainer container, int slot, ItemData item) {
      if (item != null && container.valid(slot) && container.get(slot) == null) {
         container.set(slot, item);
      }
   }

   // ------------------------------------------------------------------ bộ sưu tập

   public static String collectionToJson(ItemCollection collection) {
      JsonObject o = new JsonObject();
      if (collection.getSelected() != null) {
         o.addProperty("sel", collection.selected);
      }
      if (collection.capacity != ItemCollection.DEFAULT_CAPACITY) {
         o.addProperty("cap", collection.capacity);
      }
      JsonArray items = new JsonArray();
      for (ItemData item : collection.items) {
         items.add(toJson(item));
      }
      o.add("items", items);
      return o.toString();
   }

   /** Đọc bộ sưu tập {@code {"sel","cap","items"}}. */
   public static ItemCollection collectionFromJson(String json) {
      ItemCollection collection = new ItemCollection();
      JsonObject o = parseObject(json);
      if (o == null) {
         return collection;
      }
      List<ItemData> items = new ArrayList<>();
      if (o.has("items") && o.get("items").isJsonArray()) {
         for (JsonElement element : o.getAsJsonArray("items")) {
            ItemData item = fromJson(element);
            if (item != null) {
               items.add(item);
            }
         }
      }
      collection.items = items;
      collection.selected = o.has("sel") ? o.get("sel").getAsInt() : -1;
      if (o.has("cap")) {
         collection.capacity = o.get("cap").getAsInt();
      }
      return collection.normalize();
   }

   /**
    * Cột {@code item_back}: bộ sưu tập đeo lưng. Trước 2026-10-13 cột này chứa 1 item lẻ (món đang đeo) -> đọc thành
    * bộ sưu tập 1 món, đang dùng món đó.
    */
   public static ItemCollection backsFromJson(String json) {
      JsonObject o = parseObject(json);
      if (o == null || o.has("items")) {
         return collectionFromJson(json);
      }
      ItemCollection collection = new ItemCollection();
      ItemData legacy = itemFromJson(json);
      if (legacy != null) {
         collection.selected = collection.add(legacy);
      }
      return collection.normalize();
   }

   // ------------------------------------------------------------------ 1 item lẻ

   public static String itemToJson(ItemData item) {
      return item == null ? null : toJson(item).toString();
   }

   public static ItemData itemFromJson(String json) {
      return json == null || json.isBlank() ? null : fromJson(JsonParser.parseString(json));
   }
}
