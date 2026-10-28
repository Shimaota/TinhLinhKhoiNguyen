package com.tlnt.server.daily;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tlnt.server.item.RewardItem;
import com.tlnt.server.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cấu hình nhiệm vụ hằng ngày = bảng {@code tlnt_data.daily_quest} (cột {@code name} -> {@code data} JSON, nạp lại mỗi
 * phút; migration {@code 2026-10-10_daily_quest.sql}). Bất biến.
 *
 * <pre>
 * rules      {"base":10,"extra":5,"swap":5,"ticket":309,"diligence":1,"activity":25,"spread":3}
 * tasks      [{"id":"kill","kind":"kill","target":0,"name":"Đánh hạ {mob}","min":75,"max":100,"on":1}, ...]
 * rewards    [{"w":40,"q":[10000,50000],"item":{"i":1,"q":10000,"l":1}}, {"w":5,"perm":1,"item":{...}}, ...]
 * milestones [{"id":1,"points":25,"item":{"i":1,"q":500000,"l":1}}, ...]
 * </pre>
 * tasks: {@code id} khoá riêng (lưu trong tiến độ người chơi — đổi id là NV đang làm của người chơi bị bỏ qua),
 * {@code kind} = {@link DailyKind#key}, {@code target} theo loại, {@code name} có thể chứa {@code {mob}} (tên quái
 * được giao), số cần làm random [{@code min}, {@code max}] lúc giao, {@code on} 0 = không giao nữa.
 * rewards: hoàn thành 1 NV nhận NGẪU NHIÊN 1 món theo trọng số {@code w}; {@code q} = khoảng số lượng random (bỏ = số
 * lượng của item); {@code perm} % ra vĩnh viễn cho món có dòng 57 "HSD {0} ngày" (như vòng quay); món không ghi "l"
 * vẫn KHOÁ (mọi quà nhiệm vụ hằng ngày khoá). milestones: mốc điểm chăm chỉ (tuần), mỗi mốc 1 món, {@code id} > 0 khác nhau.
 *
 * @param baseTasks số NV giao sẵn mỗi ngày
 * @param maxExtra số lần "Nhận NV mới" tối đa / ngày
 * @param maxSwaps số lần "Đổi NV" tối đa / ngày
 * @param ticketItem mẫu item tốn mỗi lần nhận thêm / đổi (Vé nhiệm vụ)
 * @param diligence điểm chăm chỉ mỗi NV hoàn thành (khi bấm "Nhận")
 * @param activity điểm hoạt động mỗi NV hoàn thành
 * @param levelSpread NV đánh quái: random trong các loại quái lệch cấp ≤ (lệch nhỏ nhất + spread)
 */
public record DailyConfig(int baseTasks, int maxExtra, int maxSwaps, int ticketItem, int diligence, int activity,
      int levelSpread, List<TaskDef> tasks, List<Reward> rewards, int totalWeight, List<Milestone> milestones) {

   /** 1 loại nhiệm vụ trong danh sách random. */
   public record TaskDef(String id, DailyKind kind, int target, String name, int min, int max, boolean enabled) {
   }

   /** 1 món trong bảng quà ngẫu nhiên. {@code qtyMin} = 0: giữ số lượng của item. */
   public record Reward(RewardItem item, int weight, int permPercent, int qtyMin, int qtyMax) {
   }

   /** 1 mốc điểm chăm chỉ. */
   public record Milestone(int id, int points, RewardItem item) {
   }

   public TaskDef task(String id) {
      for (TaskDef t : tasks) {
         if (t.id().equals(id)) {
            return t;
         }
      }
      return null;
   }

   public Milestone milestone(int id) {
      for (Milestone m : milestones) {
         if (m.id() == id) {
            return m;
         }
      }
      return null;
   }

   /** Món trúng với số ngẫu nhiên {@code roll} trong [0, {@link #totalWeight}). */
   public Reward pick(int roll) {
      int acc = 0;
      for (Reward r : rewards) {
         acc += r.weight();
         if (roll < acc) {
            return r;
         }
      }
      return rewards.get(rewards.size() - 1);
   }

   /**
    * Dựng từ các dòng bảng ({@code name -> data}). Thiếu / sai dòng nào thì phần đó rỗng + cảnh báo; không có NV nào
    * hoặc không có quà -> null (tắt tính năng).
    */
   public static DailyConfig parse(Map<String, String> rows) {
      JsonObject rules = object(rows.get("rules"));
      List<TaskDef> tasks = new ArrayList<>();
      Set<String> ids = new HashSet<>();
      for (JsonObject o : RewardItem.objects(rows.get("tasks"), "daily_quest.tasks")) {
         String id = str(o, "id", "");
         DailyKind kind = DailyKind.of(str(o, "kind", ""));
         if (id.isEmpty() || kind == null || !ids.add(id)) {
            Log.warn("daily_quest.tasks: bo qua dong %s (thieu id / kind la / trung id)", o);
            continue;
         }
         int min = Math.max(1, num(o, "min", 1));
         int max = Math.max(min, num(o, "max", min));
         tasks.add(new TaskDef(id, kind, num(o, "target", 0), str(o, "name", id), min, max, num(o, "on", 1) != 0));
      }
      List<Reward> rewards = new ArrayList<>();
      int total = 0;
      for (JsonObject o : RewardItem.objects(rows.get("rewards"), "daily_quest.rewards")) {
         RewardItem item = o.has("item") && o.get("item").isJsonObject()
               ? RewardItem.fromJson(o.getAsJsonObject("item"), true) : null;
         int weight = Math.max(0, num(o, "w", 0));
         if (item == null || weight <= 0) {
            Log.warn("daily_quest.rewards: bo qua dong %s (thieu item / w)", o);
            continue;
         }
         item = new RewardItem(item.templateId(), item.quantity(), true, item.stats(), item.statsMax(),
               item.expireDays()); // quà NV hằng ngày luôn khoá
         int[] q = range(o.get("q"));
         rewards.add(new Reward(item, weight, Math.max(0, Math.min(100, num(o, "perm", 0))), q[0], q[1]));
         total += weight;
      }
      List<Milestone> milestones = new ArrayList<>();
      for (JsonObject o : RewardItem.objects(rows.get("milestones"), "daily_quest.milestones")) {
         RewardItem item = o.has("item") && o.get("item").isJsonObject()
               ? RewardItem.fromJson(o.getAsJsonObject("item"), true) : null;
         int id = num(o, "id", 0);
         if (id <= 0 || milestone(milestones, id) != null) {
            Log.warn("daily_quest.milestones: bo qua dong %s (id <= 0 / trung id)", o);
            continue;
         }
         milestones.add(new Milestone(id, Math.max(0, num(o, "points", 0)), item));
      }
      milestones.sort((a, b) -> Integer.compare(a.points(), b.points()));
      if (tasks.isEmpty() || total <= 0) {
         Log.warn("daily_quest: khong co nhiem vu / qua nao -> tat nhiem vu hang ngay");
         return null;
      }
      return new DailyConfig(Math.max(0, num(rules, "base", 10)), Math.max(0, num(rules, "extra", 5)),
            Math.max(0, num(rules, "swap", 5)), num(rules, "ticket", 309), Math.max(0, num(rules, "diligence", 1)),
            Math.max(0, num(rules, "activity", 25)), Math.max(0, num(rules, "spread", 3)), List.copyOf(tasks),
            List.copyOf(rewards), total, List.copyOf(milestones));
   }

   private static Milestone milestone(List<Milestone> list, int id) {
      for (Milestone m : list) {
         if (m.id() == id) {
            return m;
         }
      }
      return null;
   }

   private static JsonObject object(String json) {
      if (json == null || json.isBlank()) {
         return new JsonObject();
      }
      try {
         JsonElement e = JsonParser.parseString(json);
         return e.isJsonObject() ? e.getAsJsonObject() : new JsonObject();
      } catch (RuntimeException e) {
         Log.warn("daily_quest.rules: JSON sai -> dung mac dinh (%s)", e.getMessage());
         return new JsonObject();
      }
   }

   /** {@code [min, max]} hoặc 1 số; không có -> {0, 0}. */
   private static int[] range(JsonElement e) {
      if (e == null || e.isJsonNull()) {
         return new int[] {0, 0};
      }
      if (e.isJsonArray() && e.getAsJsonArray().size() > 0) {
         int a = e.getAsJsonArray().get(0).getAsInt();
         int b = e.getAsJsonArray().size() > 1 ? e.getAsJsonArray().get(1).getAsInt() : a;
         return new int[] {Math.max(1, Math.min(a, b)), Math.max(1, Math.max(a, b))};
      }
      int v = Math.max(1, e.getAsInt());
      return new int[] {v, v};
   }

   private static int num(JsonObject o, String key, int def) {
      try {
         return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsInt() : def;
      } catch (RuntimeException e) {
         return def;
      }
   }

   private static String str(JsonObject o, String key, String def) {
      return o.has(key) && o.get(key).isJsonPrimitive() ? o.get(key).getAsString() : def;
   }
}
