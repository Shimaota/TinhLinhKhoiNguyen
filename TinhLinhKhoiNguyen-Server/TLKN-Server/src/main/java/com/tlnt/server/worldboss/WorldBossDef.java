package com.tlnt.server.worldboss;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tlnt.server.altar.AltarRules;
import com.tlnt.server.item.RewardItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1 boss thế giới (1 dòng {@code tlnt_data.world_boss}). Cả server chỉ có 1 con mỗi boss: chết thì {@code respawnMs}
 * sau con mới xuất hiện ở map NGẪU NHIÊN trong {@code maps}, khu ngẫu nhiên, chỗ ngẫu nhiên xa cửa; máy chủ vừa bật thì
 * con đầu tiên sau {@code firstMs}.
 *
 * <p>Cột {@code spec} (JSON) — boss QUÁI khi có {@code "mob"}, boss người chơi giả khi có {@code "char"}:
 * <pre>
 * {"mob":39,"level":25,"hp":10000000,"attack":1500,"attackMs":2000}
 * {"char":4,"class":7,"skins":["stick/lv1"],"level":25,"hp":15000000,"attack":1500,"armor":720,"armorPen":750,
 *  "evasion":2000,"accuracy":2000,"crit":2000,"critDamage":180,"speed":1350,"rootMs":15000,"rootTargets":5,
 *  "rootBurn":10}
 * </pre>
 * Chung: {@code "event":"trungthu"} = chỉ có khi sự kiện đó bật ({@code event/GameEvents}); {@code spawnText} (mặc định "{boss} vừa xuất hiện tại {map}" — không báo khu) / {@code killText} (bỏ = KHÔNG báo
 * lúc bị hạ) (thay {@code {boss} {map} {zone} {player}}), {@code "announce":false} = không
 * báo chữ chạy toàn server (boss Lôi vực Sấm sét). Boss quái: {@code slamHitMs} = lúc skill4 đập đất trúng (Vệ thần Sấm sét
 * 717). Boss người chơi giả: {@code "bolt": %} = có chiêu tia sét {@code stick/skill10} (op 21 id 41) THAY chiêu trói
 * (Tinh linh Sấm sét). Boss người chơi giả: chí mạng /
 * né / chính xác theo phần vạn (2000 = 20%), {@code critDamage} phần nghìn cộng thêm (180 = +18%), {@code rootBurn} phần
 * nghìn HP tối đa người bị trói mất mỗi giây. Cột {@code loot}: mảng {@link WorldBossLoot}. Bất biến.
 *
 * @param charSpec chỉ số boss người chơi giả; null = boss quái
 */
public record WorldBossDef(int id, String name, List<Integer> maps, long respawnMs, long firstMs, int mobTemplateId,
      int level, long hp, long attack, long attackMs, AltarRules.BossSpec charSpec, int rootMs, int rootTargets,
      int rootBurnPermille, List<WorldBossLoot> loot, String spawnText, String killText, boolean announce,
      long slamHitMs, String event) {

   /** Boss gắn sự kiện ({@code "event":"trungthu"}) đang tắt: không mọc, đang sống thì bị gỡ. */
   public boolean eventOff() {
      return !com.tlnt.server.event.GameEvents.activeKey(event);
   }

   /** Chỉ báo xuất hiện, không báo khu (người dùng 2026-10-10). */
   static final String DEFAULT_SPAWN_TEXT = "{boss} vừa xuất hiện tại {map}";
   /** Mặc định không báo lúc boss bị hạ (spec có "killText" thì báo). */
   static final String DEFAULT_KILL_TEXT = "";

   /**
    * Chiêu boss người chơi giả (id op 21, như Tinh linh Thần mộc): 32 {@code stick/skill1} 1 mục tiêu 100%, 33
    * {@code stick/skill2} chém phía trước 150%, 34 {@code stick/skill3} đập đất hất tung 200%, 35 {@code stick/skill4} trói.
    */
   static final Map<Integer, Integer> CHAR_PERCENTS = Map.of(32, 100, 33, 150, 34, 200, 35, 0);
   /** Vòng chiêu dự phòng (chế độ thông minh tự chọn chiêu). */
   static final int[] CHAR_ROTATION = {32, 32, 33, 32, 34, 32, 35};
   /**
    * Thời gian ra chiêu = độ dài animation skeleton chars/4 (đo 2026-10-08 bằng thư viện Spine của client — giống hệt
    * chars/17): {@code stick/skill1} 0,867 s (atk 0,333 s), {@code skill2} 1,333 s, {@code skill3} 1,0 s (rung 40%),
    * {@code skill4} 1,367 s (skill_up 0,533 s). {skill: [ms ra chiêu, ms lúc trúng]}.
    */
   static final Map<Integer, long[]> CHAR_TIMINGS = Map.of(32, new long[] {867, 333}, 33, new long[] {1333, 600},
         34, new long[] {1000, 400}, 35, new long[] {1367, 567});
   /**
    * Khoá spec -> chiêu đặc biệt thay trói (giá trị = % sát thương): "bolt" 41 tia sét (Sấm sét), "skill5" 36 suy yếu
    * (Đầm lầy), "skill6" 37 khiên nước (Biển cả, % bỏ qua), "skill7" 38 choáng (Phong thạch), "skill8" 39 đóng băng
    * (Băng giá), "skill11" 43 chiêu tối thượng diện rộng (Cổ thần Dạ xoa) — {@code world/AltarBoss.special}.
    */
   static final Map<String, Integer> SPECIAL_KEYS = new java.util.LinkedHashMap<>(Map.of(
         "bolt", AltarRules.MOVE_BOLT, "skill5", AltarRules.MOVE_WEAKEN, "skill6", AltarRules.MOVE_SHIELD,
         "skill7", AltarRules.MOVE_STUN, "skill8", AltarRules.MOVE_FREEZE, "skill11", AltarRules.MOVE_SKILL11));
   /**
    * {ms ra chiêu, ms lúc trúng} đo bằng Spine: chars/16 {@code skill10} 0,633 s (atk 0,3); chars/5 {@code skill5} 1,367 s
    * (không sự kiện -> 0,6); chars/8 {@code skill6} 1,367 (skill_up 0,167); chars/6 {@code skill7} 1,367 (0,567); chars/7
    * {@code skill8} 1,367 (0,533).
    */
   static final Map<Integer, long[]> SPECIAL_TIMINGS = Map.of(AltarRules.MOVE_BOLT, new long[] {633, 300},
         AltarRules.MOVE_WEAKEN, new long[] {1367, 600}, AltarRules.MOVE_SHIELD, new long[] {1367, 167},
         AltarRules.MOVE_STUN, new long[] {1367, 567}, AltarRules.MOVE_FREEZE, new long[] {1367, 533},
         AltarRules.MOVE_SKILL11, new long[] {1367, 533}); // chars/24 stick/skill11 1,367 s, skill_up 0,533 s
   static final long CHAR_GAP_MS = 150;
   static final int WEAPON_LEVEL = 5;

   /** Chữ đã thay biến. */
   static String format(String text, String boss, String map, int zone, String player) {
      return text.replace("{boss}", boss).replace("{map}", map == null ? "" : map)
            .replace("{zone}", String.valueOf(zone)).replace("{player}", player == null ? "" : player);
   }

   /**
    * Dựng từ 1 dòng SQL. {@code where} để ghi log. Sai định dạng -> ném {@link IllegalArgumentException} (người gọi bỏ
    * dòng đó).
    */
   static WorldBossDef parse(int id, String name, String mapsJson, int respawnMinutes, int firstMinutes, String specJson,
         String lootJson, String where) {
      List<Integer> maps = new ArrayList<>();
      for (JsonElement e : JsonParser.parseString(mapsJson).getAsJsonArray()) {
         maps.add(e.getAsInt());
      }
      if (maps.isEmpty()) {
         throw new IllegalArgumentException(where + ": maps rong");
      }
      JsonObject spec = JsonParser.parseString(specJson).getAsJsonObject();
      int level = integer(spec, "level", 1);
      long hp = Math.max(1, number(spec, "hp", 1));
      long attack = Math.max(0, number(spec, "attack", 0));
      AltarRules.BossSpec charSpec = null;
      int mob = 0;
      if (spec.has("char")) {
         int charTemplate = spec.get("char").getAsInt();
         String[] skins = {"stick/lv1"};
         if (spec.has("skins")) {
            JsonArray a = spec.getAsJsonArray("skins");
            skins = new String[a.size()];
            for (int i = 0; i < a.size(); i++) {
               skins[i] = a.get(i).getAsString();
            }
         }
         Map<Integer, Integer> percents = CHAR_PERCENTS;
         Map<Integer, long[]> timings = CHAR_TIMINGS;
         int[] rotation = CHAR_ROTATION;
         for (Map.Entry<String, Integer> sp : SPECIAL_KEYS.entrySet()) { // chiêu đặc biệt thay trói
            if (!spec.has(sp.getKey())) {
               continue;
            }
            int move = sp.getValue();
            java.util.HashMap<Integer, Integer> p = new java.util.HashMap<>(CHAR_PERCENTS);
            p.remove(AltarRules.MOVE_ROOT);
            p.put(move, Math.max(0, spec.get(sp.getKey()).getAsInt()));
            java.util.HashMap<Integer, long[]> t = new java.util.HashMap<>(CHAR_TIMINGS);
            t.put(move, SPECIAL_TIMINGS.get(move));
            percents = Map.copyOf(p);
            timings = Map.copyOf(t);
            rotation = new int[] {32, 32, 33, 32, 34, 32, move};
            break;
         }
         charSpec = new AltarRules.BossSpec(0, 0, name, charTemplate, integer(spec, "class", 7), level, hp, hp,
               WEAPON_LEVEL, skins, attack, integer(spec, "crit", 0), integer(spec, "critDamage", 0),
               number(spec, "armor", 0), number(spec, "armorPen", 0), integer(spec, "evasion", 0),
               integer(spec, "accuracy", 0), attack, rotation, null, List.of(), number(spec, "speed", 1350),
               percents, timings, CHAR_GAP_MS);
      } else if (spec.has("mob")) {
         mob = spec.get("mob").getAsInt();
      } else {
         throw new IllegalArgumentException(where + ": spec thieu \"mob\" hoac \"char\"");
      }
      List<WorldBossLoot> loot = new ArrayList<>();
      for (JsonObject o : RewardItem.objects(lootJson, where)) {
         WorldBossLoot l = WorldBossLoot.fromJson(o);
         if (l != null) {
            loot.add(l);
         }
      }
      return new WorldBossDef(id, name, List.copyOf(maps), Math.max(1, respawnMinutes) * 60_000L,
            Math.max(0, firstMinutes) * 60_000L, mob, level, hp, attack, Math.max(500, number(spec, "attackMs", 2000)),
            charSpec, integer(spec, "rootMs", 15_000), integer(spec, "rootTargets", 5), integer(spec, "rootBurn", 0),
            List.copyOf(loot), text(spec, "spawnText", DEFAULT_SPAWN_TEXT), text(spec, "killText", DEFAULT_KILL_TEXT),
            !spec.has("announce") || spec.get("announce").getAsBoolean(), number(spec, "slamHitMs", 0),
            spec.has("event") ? spec.get("event").getAsString() : "");
   }

   private static int integer(JsonObject o, String key, int def) {
      return o.has(key) ? o.get(key).getAsInt() : def;
   }

   private static long number(JsonObject o, String key, long def) {
      return o.has(key) ? o.get(key).getAsLong() : def;
   }

   private static String text(JsonObject o, String key, String def) {
      return o.has(key) && !o.get(key).getAsString().isBlank() ? o.get(key).getAsString() : def;
   }
}
