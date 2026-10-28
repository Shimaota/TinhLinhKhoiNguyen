package com.tlnt.server.admin.web;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.GamePack;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.forge.UpgradeRules;
import com.tlnt.server.forge.SoulRules;
import com.tlnt.server.item.ItemBuffs;
import com.tlnt.server.player.ItemData;

import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Item trong admin panel: hiển thị ĐỦ như cửa sổ thông tin item của client ({@code ItemInfoPopup.populateItemDetails}:
 * tên, Nhật / Nguyệt ấn, mô tả, "Dành cho", "Cấp độ yêu cầu", "Hạn sử dụng", dòng chỉ số theo {@code stat_type.format_pattern}
 * — đã cộng % nâng cấp như gói gửi client — bùa linh hồn, đá nguyên tố) + đọc dữ liệu từ bộ sửa item của trang.
 */
final class AdminItems {

   /** {@code Item.StarGrade} phía client (index = {@link ItemData#starGrade}). */
   static final String[] GRADES = {"", "Nhật ấn", "Nguyệt ấn"};
   private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
         .withZone(ZoneId.systemDefault());
   /** Mã màu Textra trong mô tả ("[#8B4E23]", "[]"). */
   private static final Pattern MARKUP = Pattern.compile("\\[#?[0-9A-Fa-f]{6,8}]|\\[]");
   private static final Pattern ARG = Pattern.compile("\\{(\\d+)}");
   /** Số dòng chỉ số tối đa 1 món (client đọc byte). */
   static final int MAX_STAT_LINES = 20;

   private AdminItems() {
   }

   // ================================================================== hiển thị

   /** 1 món đầy đủ thông tin; {@code slot} = ô trong kho (-1 = không có ô, vd thư). */
   static Map<String, Object> view(ItemData item, int slot) {
      ItemPack.ItemTemplate t = GameDataCache.itemTemplate(item.templateId);
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("slot", slot);
      row.put("id", item.templateId);
      row.put("name", t == null ? "Item #" + item.templateId : t.name());
      row.put("icon", icon(t));
      row.put("typeId", t == null ? -1 : t.typeId());
      row.put("type", t == null ? "" : typeName(t.typeId()));
      row.put("grade", item.starGrade);
      row.put("gradeName", item.starGrade > 0 && item.starGrade < GRADES.length ? GRADES[item.starGrade] : "");
      row.put("stars", item.starCount);
      row.put("qty", item.quantity);
      row.put("locked", item.locked);
      row.put("created", item.createdTime > 0 ? TIME.format(Instant.ofEpochMilli(item.createdTime)) : "");
      row.put("expireAt", item.expireTime);
      row.put("expire", item.expireTime == ItemData.PERMANENT ? "" : TIME.format(Instant.ofEpochMilli(item.expireTime)));
      row.put("expired", item.expireTime != ItemData.PERMANENT && item.expireTime <= System.currentTimeMillis());
      if (t != null) {
         row.put("description", clean(t.description()));
         row.put("requiredLevel", t.requiredLevel());
         row.put("requiredClass", className(t.classId()));
      }
      List<String> lines = new ArrayList<>();
      List<int[]> raw = new ArrayList<>();
      for (ItemData.StatValue stat : item.stats) {
         int[] values = stat.values() == null ? new int[0] : stat.values();
         int[] shown = new int[values.length];
         for (int k = 0; k < values.length; k++) {
            shown[k] = item.starCount > 0 ? UpgradeRules.scaled(stat.statTypeId(), values[k], item) : values[k];
         }
         lines.add(statText(stat.statTypeId(), shown));
         int[] r = new int[values.length + 1];
         r[0] = stat.statTypeId();
         System.arraycopy(values, 0, r, 1, values.length);
         raw.add(r);
      }
      row.put("lines", lines);
      row.put("stats", raw); // chỉ số GỐC (chưa cộng % nâng) — bộ sửa item ghi lại đúng dạng này
      List<Map<String, Object>> charms = new ArrayList<>();
      for (ItemData charm : item.soulCharms) {
         if (charm != null) {
            Map<String, Object> c = new LinkedHashMap<>();
            ItemPack.ItemTemplate ct = GameDataCache.itemTemplate(charm.templateId);
            c.put("name", ct == null ? "Item #" + charm.templateId : ct.name());
            c.put("icon", icon(ct));
            List<String> cl = new ArrayList<>();
            for (ItemData.StatValue stat : charm.stats) {
               cl.add(statText(stat.statTypeId(), stat.values()));
            }
            c.put("lines", cl);
            charms.add(c);
         }
      }
      row.put("charms", charms);
      row.put("charmCount", charms.size());
      row.put("openSockets", SoulRules.openSockets(item));
      List<Map<String, Object>> stones = new ArrayList<>();
      int hole = 0;
      for (ItemData.ElementStone stone : item.elementStones) {
         hole++;
         if (stone == null) {
            continue;
         }
         Map<String, Object> s = new LinkedHashMap<>();
         ItemPack.ItemTemplate st = GameDataCache.itemTemplate(stone.templateId());
         s.put("hole", hole);
         s.put("name", st == null ? "Item #" + stone.templateId() : st.name());
         s.put("icon", icon(st));
         s.put("level", stone.level());
         s.put("line", statText(stone.statTypeId(), stone.values()));
         stones.add(s);
      }
      row.put("stones", stones);
      row.put("holes", item.elementStones.size());
      row.put("stoneCount", stones.size());
      ItemBuffs.Effect effect = ItemBuffs.forItem(item.templateId);
      if (effect != null) {
         row.put("buffMinutes", effect.durationMs() / 60_000.0);
      }
      return row;
   }

   /** Chữ 1 dòng chỉ số như client ({@code MessageFormat} trên {@code format_pattern}, số có dấu chấm nghìn). */
   static String statText(int statTypeId, int[] values) {
      ItemPack.StatType type = statType(statTypeId);
      String pattern = type == null ? "Chỉ số #" + statTypeId + " {0}" : type.formatPattern();
      if (pattern == null || pattern.isBlank()) {
         return "Chỉ số #" + statTypeId;
      }
      NumberFormat nf = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN"));
      Matcher m = ARG.matcher(pattern);
      StringBuilder sb = new StringBuilder();
      while (m.find()) {
         int k = Integer.parseInt(m.group(1));
         String v = values != null && k < values.length ? nf.format(values[k]) : "?";
         m.appendReplacement(sb, Matcher.quoteReplacement(v));
      }
      m.appendTail(sb);
      return sb.toString();
   }

   /** Số giá trị 1 dòng chỉ số cần ({0}, {1}... trong mẫu chữ). */
   static int argCount(String pattern) {
      int max = -1;
      Matcher m = ARG.matcher(pattern == null ? "" : pattern);
      while (m.find()) {
         max = Math.max(max, Integer.parseInt(m.group(1)));
      }
      return max + 1;
   }

   /** Mọi loại chỉ số cho bộ sửa item: {id, pattern, args}. */
   static List<Map<String, Object>> statTypes() {
      List<Map<String, Object>> list = new ArrayList<>();
      for (ItemPack.StatType t : GameDataCache.item().statTypes()) {
         if (t.formatPattern() == null || t.formatPattern().isBlank()) {
            continue;
         }
         Map<String, Object> row = new LinkedHashMap<>();
         row.put("id", t.id());
         row.put("pattern", t.formatPattern());
         row.put("args", argCount(t.formatPattern()));
         list.add(row);
      }
      return list;
   }

   /** Tìm mẫu item theo tên / ID (tối đa {@code limit}), kèm loại, cấp, giới tính dùng. */
   static List<Map<String, Object>> search(String text, int limit) {
      String needle = text.trim().toLowerCase(Locale.ROOT);
      List<Map<String, Object>> list = new ArrayList<>();
      for (ItemPack.ItemTemplate t : GameDataCache.item().itemTemplates()) {
         if (needle.isEmpty() || t.name().toLowerCase(Locale.ROOT).contains(needle)
               || String.valueOf(t.id()).equals(needle)) {
            list.add(template(t));
            if (list.size() >= limit) {
               break;
            }
         }
      }
      return list;
   }

   /** Thông tin 1 mẫu item (chưa có chỉ số riêng). */
   static Map<String, Object> template(ItemPack.ItemTemplate t) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", t.id());
      row.put("name", t.name());
      row.put("icon", icon(t));
      row.put("typeId", t.typeId());
      row.put("type", typeName(t.typeId()));
      row.put("requiredLevel", t.requiredLevel());
      row.put("requiredClass", className(t.classId()));
      row.put("description", clean(t.description()));
      ItemPack.ItemType type = GameDataCache.itemType(t.typeId());
      row.put("equip", type != null && type.equippable());
      return row;
   }

   private static final Pattern ICON = Pattern.compile("(icon|ui)/[0-9]{1,5}");

   /** Ảnh của mẫu item ({@code item_template.icon_path} dạng "icon" + "\" + "182" -> "icon/182"); "" = không có. */
   static String icon(ItemPack.ItemTemplate t) {
      if (t == null || t.iconPath() == null) {
         return "";
      }
      String path = t.iconPath().replace('\\', '/').trim();
      return ICON.matcher(path).matches() ? path : "";
   }

   /** Ảnh của mẫu item theo id. */
   static String icon(int templateId) {
      return icon(GameDataCache.itemTemplate(templateId));
   }

   static String typeName(int typeId) {
      ItemPack.ItemType type = GameDataCache.itemType(typeId);
      return type == null ? "" : type.name().trim();
   }

   /** "Dành cho" của client: {@code item_template.class_id} = char_template (Rinn / Lyria / Bruk); 0 = mọi người. */
   static String className(int classId) {
      if (classId <= 0) {
         return "";
      }
      GamePack.CharTemplate c = GameDataCache.charTemplate(classId);
      return c == null ? "#" + classId : c.name();
   }

   /** Tên ô đồ mặc ({@code item_type.equip_slot}). */
   static String slotName(int slot) {
      for (ItemPack.ItemType type : GameDataCache.item().itemTypes()) {
         if (type.equipSlot() == slot) {
            return type.name().trim();
         }
      }
      return "Ô " + slot;
   }

   static String clean(String text) {
      return text == null ? "" : MARKUP.matcher(text).replaceAll("").trim();
   }

   private static ItemPack.StatType statType(int id) {
      for (ItemPack.StatType t : GameDataCache.item().statTypes()) {
         if (t.id() == id) {
            return t;
         }
      }
      return null;
   }

   // ================================================================== sửa

   /**
    * Ghi các trường bộ sửa gửi lên vào {@code item} (trường nào không gửi thì giữ nguyên): {@code q} số lượng,
    * {@code l} khoá, {@code star} cấp nâng 0-{@link UpgradeRules#MAX_LEVEL}, {@code grade} 0/1/2 (Nhật / Nguyệt ấn),
    * {@code expireAt} epoch ms (-1 = vĩnh viễn), {@code s} dòng chỉ số GỐC [[loại, giá trị...]], {@code clearCharms} /
    * {@code clearStones} gỡ bùa / đá.
    */
   static void applyEdit(ItemData item, JsonObject o) {
      if (o.has("q")) {
         int q = o.get("q").getAsInt();
         if (q <= 0 || q > ItemData.MAX_STACK) {
            throw new AdminApi.BadRequest("Số lượng 1-" + ItemData.MAX_STACK + ".");
         }
         if (q > 1 && !item.stackable()) {
            throw new AdminApi.BadRequest("Món này không cộng dồn được, số lượng phải là 1.");
         }
         item.quantity = q;
      }
      if (o.has("l")) {
         item.locked = truthy(o.get("l"));
      }
      if (o.has("star")) {
         int star = o.get("star").getAsInt();
         if (star < 0 || star > UpgradeRules.MAX_LEVEL) {
            throw new AdminApi.BadRequest("Cấp nâng 0-" + UpgradeRules.MAX_LEVEL + ".");
         }
         if (star != item.starCount) {
            item.starCount = (byte) star;
            item.upgradePercents = null; // mọi cấp 4% (cấp nâng bằng Ngọc trai đen không giữ được khi đặt tay)
         }
      }
      if (o.has("grade")) {
         int grade = o.get("grade").getAsInt();
         if (grade < 0 || grade >= GRADES.length) {
            throw new AdminApi.BadRequest("Ấn: 0 không, 1 Nhật ấn, 2 Nguyệt ấn.");
         }
         item.starGrade = (byte) grade;
      }
      if (o.has("expireAt")) {
         long at = o.get("expireAt").getAsLong();
         item.expireTime = at <= 0 ? ItemData.PERMANENT : at;
      }
      if (o.has("s")) {
         item.stats = new ArrayList<>(parseStats(o.get("s")));
      }
      if (o.has("clearCharms") && truthy(o.get("clearCharms"))) {
         item.soulCharms = new ArrayList<>();
      }
      if (o.has("clearStones") && truthy(o.get("clearStones"))) {
         item.elementStones = new ArrayList<>(java.util.Collections.nCopies(item.elementStones.size(), null));
      }
   }

   /** [[loại, giá trị...], ...] -> dòng chỉ số (loại phải có trong stat_type). */
   static List<ItemData.StatValue> parseStats(JsonElement el) {
      List<ItemData.StatValue> out = new ArrayList<>();
      if (el == null || el.isJsonNull()) {
         return out;
      }
      if (!el.isJsonArray()) {
         throw new AdminApi.BadRequest("Dòng chỉ số phải là mảng.");
      }
      JsonArray arr = el.getAsJsonArray();
      if (arr.size() > MAX_STAT_LINES) {
         throw new AdminApi.BadRequest("Tối đa " + MAX_STAT_LINES + " dòng chỉ số.");
      }
      for (JsonElement line : arr) {
         JsonArray a = line.getAsJsonArray();
         if (a.isEmpty()) {
            continue;
         }
         int type = a.get(0).getAsInt();
         if (statType(type) == null) {
            throw new AdminApi.BadRequest("Không có loại chỉ số " + type + ".");
         }
         int[] values = new int[a.size() - 1];
         for (int k = 1; k < a.size(); k++) {
            values[k - 1] = a.get(k).getAsInt();
         }
         out.add(new ItemData.StatValue(type, values));
      }
      return out;
   }

   private static boolean truthy(JsonElement e) {
      return e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : e.getAsInt() != 0;
   }
}
