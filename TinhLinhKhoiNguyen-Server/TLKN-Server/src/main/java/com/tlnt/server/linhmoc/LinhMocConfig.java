package com.tlnt.server.linhmoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Cấu hình phó bản Rừng Linh Mộc — khoá {@code linhmoc.*} trong {@code server.properties} (bảng đủ ở
 * {@code docs/CONFIG.md}). MỌI khoá có mặc định ở đây: file thiếu khoá vẫn chạy. Bất biến.
 *
 * @param hubMapId       map thường có NPC mở phó bản (Rừng cổ mộc)
 * @param npcId          {@code npc_template.id} của Bác Rano
 * @param maps           dãy map phó bản theo thứ tự đi (map đầu = nơi vào / hồi sinh)
 * @param maxLevel       cấp quái tối đa (cấp quái = min(cấp cao nhất nhóm, maxLevel))
 * @param runMinutes     thời gian 1 lượt
 * @param returnSeconds  thành công: bao nhiêu giây sau thì đưa nhóm về {@code hubMapId}
 * @param hpBase         gốc HP B = hpBase × cấp^hpExponent
 * @param hpExponent     số mũ cấp của gốc HP
 * @param hpRound        HP làm tròn tới bội số này (game gốc: 50)
 * @param damageDivisor  u = B / damageDivisor (sát thương quái)
 * @param expFactor      &gt; 0: EXP gốc = floor(sát thương thật × expFactor) (ghi đè); 0 (mặc định) = exp.dungeon × cấp lượt
 * @param mobs           {@code mob_template.id} -> hệ số HP / hệ số sát thương
 * @param text           chữ gửi client (hội thoại + thông báo), khoá = phần sau {@code linhmoc.text.}
 * @param bossMobId      {@code mob_template.id} boss Hắc thần Linh mộc (boss QUÁI, đặt trong map_template.mobs map 29;
 *                       HP / ST theo {@code mobs}) — server điều khiển chiêu ({@code world/LinhMocMobBoss})
 * @param bossAttackMs   Hắc thần: mỗi bấy nhiêu ms 1 chiêu
 * @param spiritHp       Tinh linh Thần mộc (boss người chơi giả, map cuối): HP = MP = hệ số này × B (làm tròn 50)
 * @param spiritDamage   Tinh linh Thần mộc: hệ số sát thương (× u như quái)
 * @param armorPerLevel  2 boss: Giáp = Xuyên giáp = cấp × giá trị này
 * @param points         điểm hoạt động: mỗi thành viên khi mở lượt mới + mỗi người trong khu khi hạ 1 boss
 * @param tiers          bậc quà theo cấp lượt (cấp từ, hệ số) — tăng dần theo cấp
 * @param blackGodLoot   quà Hắc thần Linh mộc (chỉ người gây nhiều sát thương nhất lên boss)
 * @param spiritLoot     quà Tinh linh Thần mộc (như trên)
 * @param maxRuns        số lượt chạy CÙNG LÚC tối đa (cả server); đủ thì từ chối mở lượt mới (không trừ lượt)
 * @param mobLoot        quà rơi khi hạ quái thường (người kết liễu ưu tiên nhặt)
 */
public record LinhMocConfig(int hubMapId, int npcId, List<Integer> maps, int maxLevel, int runMinutes,
      int returnSeconds, double hpBase, double hpExponent, int hpRound, double damageDivisor, double expFactor,
      Map<Integer, MobCoef> mobs, Map<String, String> text, int bossMobId, int bossAttackMs, double spiritHp,
      double spiritDamage, int armorPerLevel, int points, List<Tier> tiers, List<Loot> blackGodLoot,
      List<Loot> spiritLoot, List<Loot> mobLoot, int maxRuns) {

   /** 1 bậc quà: từ cấp lượt {@code fromLevel} trở lên nhân {@code mult}. */
   public record Tier(int fromLevel, double mult) {
   }

   /**
    * 1 dòng quà: {@code piles} món riêng, mỗi món số lượng random [{@code min}, {@code max}], tỉ lệ {@code chance}% (100 =
    * luôn có), KHOÁ. {@code scale}: {@code s} = số lượng × hệ số bậc, {@code h} = × nửa hệ số (1 + (hệ số − 1) / 2), {@code -}
    * = cố định. Item 1 = vàng (khoá -> vàng khoá).
    */
   public record Loot(int itemId, int piles, int min, int max, double chance, char scale) {
   }

   /** "cấp:hệ_số" — 1-16 ×0,5, 17-24 ×1, 25-32 ×1,5, 33-41 ×2, 42+ ×3. */
   static final String DEFAULT_TIERS = "1:0.5,17:1,25:1.5,33:2,42:3";
   /** "item:số_món:min:max:tỉ_lệ%:s|h|-". Vàng 1, Túi đá nguyên tố 269, Quả Linh Mộc 288, Cỏ may mắn 171, Ấn thủ lĩnh 261,
    * Đá khởi nguyên 274, Bùa linh mộc 263. */
   static final String DEFAULT_BLACK_GOD_LOOT = "1:5:1000:5000:100:s,269:1:3:3:100:s,288:1:5:5:100:h,171:1:1:1:100:-,"
         + "261:1:1:1:50:-,274:1:5:10:100:s";
   static final String DEFAULT_SPIRIT_LOOT = "1:5:5000:20000:100:s,269:1:5:5:100:s,288:1:10:10:100:h,171:1:1:1:100:-,"
         + "261:1:1:1:100:-,263:1:1:1:100:-";
   static final String DEFAULT_MOB_LOOT = "288:1:1:1:0.5:-,1:1:100:1000:5:s";

   /** Hệ số HP / sát thương của 1 loại quái. */
   public record MobCoef(double hp, double damage) {
   }

   /**
    * Quái mặc định: "id:hệ_số_HP:hệ_số_ST" cách nhau dấu phẩy. HP: Thổ dân ×1, Chiến binh ×1,5, Tù trưởng ×2, Cây ăn thịt
    * ×2,5, Sinh linh ×3, Huyết linh ×4, Gốc cây ma ×7, boss Hắc thần 49 ×15 (ST 4,5). 48: hệ số ST 5 tạm. Tinh linh Thần
    * mộc không phải quái: {@code spiritHp} / {@code spiritDamage}.
    */
   static final String DEFAULT_MOBS = "36:1:1,37:1.5:2,38:2:3,46:2.5:3,34:3:4,35:4:4,48:7:5,49:15:4.5";

   /** Chữ mặc định. {@code {turns}} / {@code {name}} / {@code {map}} / {@code {left}} / {@code {minutes}} được thay lúc gửi. */
   static final Map<String, String> DEFAULT_TEXT = defaultText();

   private static Map<String, String> defaultText() {
      Map<String, String> t = new LinkedHashMap<>();
      t.put("talk1", "Bộ tộc Sayari xưa nay là những thợ săn sống trong rừng.\n"
            + "Chúng ta thờ phụng Tinh linh Rừng xanh như thần hộ mệnh... rừng là linh hồn của cả bộ tộc.");
      t.put("talk2", "Ngày trước, bộ tộc Sayari đào bới cội rễ để lấy sức mạnh linh mộc. Họ không biết rằng thứ đó"
            + " chính là mạch sống của cả khu rừng.\n"
            + "Khi mạch linh khí bị cắt đứt, khu rừng không còn như trước nữa... nó trở nên dữ dội, mất kiểm soát.\n"
            + "Hắc linh cổ mộc xé toạc lòng đất trỗi dậy... nó thao túng và kiểm soát những người dân của bộ tộc"
            + " Sayari.\n"
            + "Tốt nhất cậu nên đi theo tổ đội. Ở đó, không ai sống sót một mình đâu!\n"
            + "Lượt đi trong hôm nay: {turns}");
      t.put("button.dungeon", "Rừng Linh Mộc");
      t.put("button.join", "Tham gia");
      t.put("button.close", "Đóng");
      t.put("noParty", "Cần có tổ đội để tham gia");
      t.put("notLeader", "Chỉ trưởng nhóm mới có thể mở phó bản");
      t.put("notHere", "Tất cả thành viên phải có mặt tại Rừng Cổ Mộc");
      t.put("noTurns", "{name} đã hết lượt trong hôm nay");
      t.put("otherRun", "{name} đang trong một lượt Rừng Linh Mộc khác");
      t.put("started", "Rừng Linh Mộc đã mở! Có {minutes} phút để tiêu diệt toàn bộ quái.");
      t.put("cleared", "Đã tiêu diệt hết quái! Đường tới {map} đã mở.");
      t.put("doorLocked", "Chưa đủ điều kiện qua map này");
      t.put("success", "Chúc mừng! Tổ đội đã chinh phục Rừng Linh Mộc.");
      t.put("returned", "Đã trở về Rừng Cổ Mộc.");
      t.put("timeout", "Hết thời gian! Lượt Rừng Linh Mộc thất bại.");
      t.put("exit", "Đã rời Rừng Linh Mộc. Lượt vẫn tính giờ, gặp Bác Rano để quay lại.");
      t.put("over", "Lượt Rừng Linh Mộc đã kết thúc.");
      t.put("full", "Khu phó bản đã đầy, hãy thử lại sau.");
      t.put("busy", "Rừng Linh Mộc đang quá tải, hãy thử lại sau.");
      t.put("viaNpc", "Hãy gặp Bác Rano ở Rừng Cổ Mộc để vào Rừng Linh Mộc.");
      t.put("dead", "Đã bị hạ gục, không thể vào Rừng Linh Mộc.");
      t.put("gearLocked", "Linh lực nơi đây không cho phép thay đổi trang bị");
      t.put("timer", "Rừng Linh Mộc");
      t.put("spiritName", "Tinh Linh Thần Mộc");
      t.put("points", "Bạn vừa nhận được {points} điểm hoạt động");
      t.put("bossLoot", "Bạn đã kết liễu {boss}! Bạn được ưu tiên nhặt phần thưởng trong 15 giây.");
      t.put("mailSubject", "Rừng Linh Mộc");
      t.put("mailFinish", "Bạn đã dọn {cleared}/{total} map Rừng Linh Mộc (cấp {level}). Phần thưởng: {fruits} Quả Linh Mộc.");
      return Collections.unmodifiableMap(t);
   }

   public static LinhMocConfig defaults() {
      return parse(key -> null);
   }

   /** Đọc từ nguồn khoá -> giá trị ({@code null} / rỗng = mặc định). Giá trị sai định dạng -> lỗi lúc khởi động. */
   public static LinhMocConfig parse(Function<String, String> source) {
      Function<String, String> get = key -> {
         String v = source.apply("linhmoc." + key);
         return v == null || v.isBlank() ? null : v.trim();
      };
      List<Integer> maps = new ArrayList<>();
      for (String part : or(get.apply("maps"), "25,26,27,28,29,30").split(",")) {
         if (!part.isBlank()) {
            maps.add(Integer.parseInt(part.trim()));
         }
      }
      if (maps.isEmpty()) {
         throw new IllegalArgumentException("linhmoc.maps rong");
      }
      Map<Integer, MobCoef> mobs = new LinkedHashMap<>();
      for (String part : or(get.apply("mobs"), DEFAULT_MOBS).split(",")) {
         if (part.isBlank()) {
            continue;
         }
         String[] f = part.trim().split(":");
         if (f.length != 3) {
            throw new IllegalArgumentException("linhmoc.mobs sai dinh dang (id:hp:st): " + part);
         }
         mobs.put(Integer.parseInt(f[0].trim()), new MobCoef(Double.parseDouble(f[1].trim()),
               Double.parseDouble(f[2].trim())));
      }
      Map<String, String> text = new LinkedHashMap<>();
      for (Map.Entry<String, String> e : DEFAULT_TEXT.entrySet()) {
         String v = source.apply("linhmoc.text." + e.getKey());
         text.put(e.getKey(), v == null || v.isEmpty() ? e.getValue() : v);
      }
      return new LinhMocConfig(
            integer(get, "hubMap", 7),
            integer(get, "npc", 9),
            List.copyOf(maps),
            Math.min(100, Math.max(1, integer(get, "maxLevel", 100))),
            Math.max(1, integer(get, "runMinutes", 30)),
            Math.max(0, integer(get, "returnSeconds", 10)),
            decimal(get, "hpBase", 144),
            decimal(get, "hpExponent", 2.25),
            Math.max(1, integer(get, "hpRound", 50)),
            decimal(get, "damageDivisor", 380),
            Math.max(0, decimal(get, "expFactor", 0)),
            Collections.unmodifiableMap(mobs),
            Collections.unmodifiableMap(text),
            integer(get, "bossMob", 49),
            Math.max(500, integer(get, "bossAttackMs", 2000)),
            decimal(get, "spiritHp", 40),
            decimal(get, "spiritDamage", 5.5),
            Math.max(0, integer(get, "armorPerLevel", 10)),
            Math.max(0, integer(get, "points", 25)),
            tiers(or(get.apply("tiers"), DEFAULT_TIERS)),
            loot("bossLoot", or(get.apply("bossLoot"), DEFAULT_BLACK_GOD_LOOT)),
            loot("spiritLoot", or(get.apply("spiritLoot"), DEFAULT_SPIRIT_LOOT)),
            loot("mobLoot", or(get.apply("mobLoot"), DEFAULT_MOB_LOOT)),
            Math.max(1, integer(get, "maxRuns", 100)));
   }

   private static List<Tier> tiers(String text) {
      List<Tier> out = new ArrayList<>();
      for (String part : text.split(",")) {
         if (part.isBlank()) {
            continue;
         }
         String[] f = part.trim().split(":");
         if (f.length != 2) {
            throw new IllegalArgumentException("linhmoc.tiers sai dinh dang (cap:he_so): " + part);
         }
         out.add(new Tier(Integer.parseInt(f[0].trim()), Double.parseDouble(f[1].trim())));
      }
      out.sort(java.util.Comparator.comparingInt(Tier::fromLevel));
      return List.copyOf(out);
   }

   private static List<Loot> loot(String key, String text) {
      List<Loot> out = new ArrayList<>();
      for (String part : text.split(",")) {
         if (part.isBlank()) {
            continue;
         }
         String[] f = part.trim().split(":");
         if (f.length != 6 || f[5].trim().length() != 1 || "sh-".indexOf(f[5].trim().charAt(0)) < 0) {
            throw new IllegalArgumentException("linhmoc." + key + " sai dinh dang (item:mon:min:max:ti_le:s|h|-): " + part);
         }
         out.add(new Loot(Integer.parseInt(f[0].trim()), Integer.parseInt(f[1].trim()), Integer.parseInt(f[2].trim()),
               Integer.parseInt(f[3].trim()), Double.parseDouble(f[4].trim()), f[5].trim().charAt(0)));
      }
      return List.copyOf(out);
   }

   /** Chữ theo khoá (không có thì trả chính khoá — dễ thấy thiếu). */
   public String text(String key) {
      return text.getOrDefault(key, key);
   }

   public long runMs() {
      return runMinutes * 60_000L;
   }

   private static String or(String value, String def) {
      return value == null ? def : value;
   }

   private static int integer(Function<String, String> get, String key, int def) {
      String v = get.apply(key);
      return v == null ? def : Integer.parseInt(v);
   }

   private static double decimal(Function<String, String> get, String key, double def) {
      String v = get.apply(key);
      return v == null ? def : Double.parseDouble(v);
   }
}
