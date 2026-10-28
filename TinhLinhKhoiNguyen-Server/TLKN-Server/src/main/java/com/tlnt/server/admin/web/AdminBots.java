package com.tlnt.server.admin.web;

import com.google.gson.JsonObject;
import com.tlnt.server.bot.BotConfig;
import com.tlnt.server.bot.BotService;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tab "Bot người chơi": tổng theo loại / map, danh sách lọc theo loại + chữ, popup chi tiết 1 bot, sửa cấu hình
 * ({@link BotService#setConfig}), cho thoát.
 * <pre>
 * GET  bots?kind=&q=                -> {config, online, awake, kinds[], maps[], matched, bots[≤ 200]}
 * GET  bots/detail?id=              -> đồ / kỹ năng / tiềm năng / chỉ số / tổ đội
 * POST bots/config {...BotConfig}   ·  bots/kick {id}  ·  bots/kick-all {}
 * </pre>
 */
final class AdminBots {

   static final int LIST_LIMIT = 200;

   private AdminBots() {
   }

   private static BotService service() {
      BotService s = BotService.instance();
      if (s == null) {
         throw new AdminApi.BadRequest("Hệ thống bot chưa khởi động.");
      }
      return s;
   }

   static Object view(String kind, String q) {
      BotService s = service();
      Map<String, Object> out = AdminApi.ok(null);
      out.put("config", config(s.config()));
      out.putAll(s.snapshot(LIST_LIMIT, kind, q));
      return out;
   }

   static Object detail(int id) {
      Map<String, Object> d = service().detail(id);
      if (d == null) {
         throw new AdminApi.BadRequest("Bot đã thoát game.");
      }
      Map<String, Object> out = AdminApi.ok(null);
      out.put("bot", d);
      return out;
   }

   static Object save(JsonObject b) throws SQLException {
      BotService s = service();
      BotConfig c = s.config();
      BotConfig next = new BotConfig(bool(b, "enabled", c.enabled()), get(b, "count", c.count()),
            get(b, "grindPercent", c.grindPercent()), get(b, "autoPercent", c.autoPercent()),
            get(b, "fishPercent", c.fishPercent()), get(b, "afkPercent", c.afkPercent()),
            get(b, "partyPercent", c.partyPercent()), get(b, "bossHunters", c.bossHunters()),
            get(b, "minLevel", c.minLevel()), get(b, "maxLevel", c.maxLevel()), get(b, "zonePercent", c.zonePercent()),
            get(b, "sessionMin", c.sessionMin()), get(b, "sessionMax", c.sessionMax()), bool(b, "chat", c.chat()),
            bool(b, "pkFightBack", c.pkFightBack()), get(b, "spawnPerSecond", c.spawnPerSecond()),
            get(b, "costumeChance", c.costumeChance()), get(b, "titleChance", c.titleChance()),
            get(b, "petChance", c.petChance()), get(b, "maskChance", c.maskChance()), get(b, "backChance", c.backChance()));
      BotConfig saved = s.setConfig(next);
      Map<String, Object> out = AdminApi.ok(saved.enabled()
            ? "Đã lưu — bot vào / ra dần " + saved.spawnPerSecond() + " bot mỗi giây tới " + saved.count() + " bot."
            : "Đã tắt bot — bot thoát dần.");
      out.put("config", config(saved));
      return out;
   }

   static Object kick(int id) {
      if (!service().kick(id)) {
         throw new AdminApi.BadRequest("Không phải bot đang online.");
      }
      return AdminApi.ok("Bot đã thoát game.");
   }

   static Object kickAll() {
      int n = service().kickAll();
      return AdminApi.ok(n == 0 ? "Không có bot nào online." : "Đã cho " + n + " bot thoát (bot mới sẽ vào lại nếu đang bật).");
   }

   private static Map<String, Object> config(BotConfig c) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("enabled", c.enabled());
      m.put("count", c.count());
      m.put("grindPercent", c.grindPercent());
      m.put("autoPercent", c.autoPercent());
      m.put("fishPercent", c.fishPercent());
      m.put("afkPercent", c.afkPercent());
      m.put("idlePercent", c.idlePercent());
      m.put("partyPercent", c.partyPercent());
      m.put("bossHunters", c.bossHunters());
      m.put("minLevel", c.minLevel());
      m.put("maxLevel", c.maxLevel());
      m.put("zonePercent", c.zonePercent());
      m.put("sessionMin", c.sessionMin());
      m.put("sessionMax", c.sessionMax());
      m.put("chat", c.chat());
      m.put("pkFightBack", c.pkFightBack());
      m.put("spawnPerSecond", c.spawnPerSecond());
      m.put("costumeChance", c.costumeChance());
      m.put("titleChance", c.titleChance());
      m.put("petChance", c.petChance());
      m.put("maskChance", c.maskChance());
      m.put("backChance", c.backChance());
      m.put("maxCount", BotConfig.MAX_COUNT);
      return m;
   }

   private static boolean bool(JsonObject b, String key, boolean fallback) {
      return b.has(key) && !b.get(key).isJsonNull() ? b.get(key).getAsBoolean() : fallback;
   }

   private static int get(JsonObject b, String key, int fallback) {
      try {
         return b.has(key) && !b.get(key).isJsonNull() ? b.get(key).getAsInt() : fallback;
      } catch (RuntimeException e) {
         throw new AdminApi.BadRequest("Giá trị không hợp lệ: " + key);
      }
   }

   /** Số bot đang online (0 nếu chưa khởi động) — trừ khỏi số người thật ở thanh trạng thái / tổng quan. */
   static int online() {
      BotService s = BotService.instance();
      return s == null ? 0 : s.onlineCount();
   }
}
