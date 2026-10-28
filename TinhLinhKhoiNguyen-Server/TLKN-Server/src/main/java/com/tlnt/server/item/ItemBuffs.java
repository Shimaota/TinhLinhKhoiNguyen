package com.tlnt.server.item;

import com.tlnt.server.net.Message;
import com.tlnt.server.net.Opcode;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.session.SessionManager;
import com.tlnt.server.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Vật phẩm dùng xong có hiệu ứng theo thời gian (loại 11 "Item time") + hiệu ứng do phụ kiện kích hoạt
 * ({@link GearCharges}). Hiệu ứng lưu ở {@link PlayerData#buffs} ({@code option_info.id} -> hết hạn), hiện ở thanh hiệu
 * ứng HUD bằng op 51 sub 0 (icon + mô tả lấy từ bảng {@code option_info} client đã có).
 *
 * <pre>
 * item | option | thời gian | tác dụng (theo mô tả item)                                  | nhóm
 * 146  | 3      | 5 phút    | hồi ngay 100% MP + 5% Khuếch đại tấn công (44)              |
 * 149  | 1      | 30 phút   | hồi ngay 100% HP + 5% mọi chỉ số ({@link #STAT_ALL_PERCENT}) |
 * 151  | 5      | 5 phút    | +100 Giáp (15) + 100 Tấn công lên quái (9)                  |
 * 153  | 10     | 30 phút   | +50% Kinh nghiệm (49)                                       | Trí tuệ
 * 299  | 29     | 30 phút   | +100% Kinh nghiệm                                           | Trí tuệ
 * 154  | 11     | 30 phút   | +35% sát thương lên quái ({@link #STAT_MOB_DAMAGE})         | Sức mạnh
 * 300  | 30     | 30 phút   | +70% sát thương lên quái                                    | Sức mạnh
 * 155  | 12     | 30 phút   | -35% sát thương nhận vào từ quái ({@link #STAT_MOB_TAKEN})  | Cứng cáp
 * 301  | 31     | 30 phút   | -70% sát thương nhận vào từ quái                            | Cứng cáp
 * 156  | 13     | 30 phút   | +10% Hút HP quái (54)                                       |
 * 157  | 14     | 30 phút   | +10% Hút MP quái (56)                                       |
 * 158  | 15     | 5 phút    | +50% HP (4)                                                 |
 * 159  | 16     | 5 phút    | +50% MP (6)                                                 |
 * 160  | 17     | 5 phút    | +20% Tấn công (8)                                           |
 * 161  | 18     | 5 phút    | +20% Tốc chạy (24) + 20% Né tránh (20)                      |
 * 162  | 19     | 5 phút    | +50% Xuyên giáp (18) + 20% Chính xác (22)                   |
 * 163  | 20     | 5 phút    | +50% Giáp (16)                                              |
 * 167  | 27     | 1 giờ     | Bùa hộ giáp: không bị người khác đánh + không đánh được người khác, cờ về Hoà bình,
 *      |        |           | không đổi sang cờ khác được                                  |
 * 272  | 2      | 5 phút    | +750 Chí mạng (11)                                          |
 * 275  | 22     | 30 phút   | hồi 1% HP + 1% MP mỗi 5 giây                                |
 * 297  | 28     | 30 giây   | Túi sữa dê: gỡ mọi hiệu ứng bất lợi + miễn nhiễm            |
 * phụ kiện (không dùng từ túi — {@link GearCharges}):
 * 263  | 25     | 15 giây   | Bùa linh mộc (sau hồi sinh): +10% Khuếch đại tấn công       |
 * 264  | 26     | 5 giây    | Mắt bão Darguun (dùng kỹ năng): +2000 Né (19) + 20% Tốc chạy (24) |
 * </pre>
 * CÙNG NHÓM thì đè nhau (Trí tuệ II thay Trí tuệ I — cái cũ mất); món không ghi nhóm thì nhóm = chính nó (dùng lại = tính
 * lại thời gian từ đầu, không cộng dồn). Đang có hiệu ứng cùng nhóm thì hỏi trước khi thay. Dòng chỉ số cộng vào nhân vật
 * qua {@link #addStats} (gọi trong
 * {@link ItemStats#apply}).
 *
 * <p>Hồi theo chu kỳ: {@link #tick} chạy trên luồng scheduler mỗi {@link #REGEN_INTERVAL_SECONDS} giây, chỉ đọc vài field
 * volatile / người: người có hiệu ứng, đang thiếu HP / MP (có chỉ số "Hồi HP / MP mỗi 5 giây", {@code char_base_stat.hp_regen
 * / mp_regen} + đồ) hoặc phụ kiện vừa bị trừ lượt mới được xếp việc vào hàng đợi session.
 */
public final class ItemBuffs {

   public static final int REGEN_INTERVAL_SECONDS = 5;

   /**
    * Chỉ số giả (không có trong {@code stat_type}, không gửi client): Thuốc Sức mạnh — đòn lên quái gây bao nhiêu thì +%
    * chừng đó ({@code ZoneCombat.boostVsMob}), KHÔNG cộng vào Tấn công / Tấn công lên quái.
    */
   public static final int STAT_MOB_DAMAGE = 100;
   /** Chỉ số giả: -% sát thương nhận vào từ quái. */
   public static final int STAT_MOB_TAKEN = 101;
   /** Chỉ số giả: +% mọi chỉ số (HP, MP, Tấn công, Tấn công quái, Chí mạng, Giáp, Xuyên giáp, Né, Chính xác). */
   public static final int STAT_ALL_PERCENT = 102;

   /** Hiệu ứng đặc biệt ngoài chỉ số. */
   public enum Special {
      NONE,
      /** Bùa hộ giáp: miễn PK ({@code ZoneCombat.canHit}), cờ về Hoà bình + khoá cờ ({@code World.setFaction}). */
      PK_SHIELD,
      /** Túi sữa dê: gỡ hiệu ứng bất lợi + miễn nhiễm. */
      CLEANSE
   }

   /**
    * @param optionId {@code option_info.id} (icon / tên trên thanh hiệu ứng)
    * @param group nhóm đè nhau (món đơn lẻ: = optionId)
    * @param regenHpPercent / regenMpPercent % HP / MP tối đa hồi mỗi {@link #REGEN_INTERVAL_SECONDS} giây
    * @param instantHpPercent / instantMpPercent % HP / MP tối đa hồi ngay lúc dùng
    * @param stats dòng chỉ số cộng thêm khi còn hiệu ứng {@code {{stat_type.id, giá trị}}}
    */
   public record Effect(int itemTemplateId, int optionId, int group, long durationMs, int regenHpPercent,
         int regenMpPercent, int instantHpPercent, int instantMpPercent, Special special, int[][] stats) {

      /** Hiệu ứng làm đổi khối chỉ số (dòng chỉ số hoặc % hồi HP / MP vào dòng Hồi mỗi 5 s) -> tính lại khi bật / hết. */
      public boolean changesStats() {
         return stats.length > 0 || regenHpPercent > 0 || regenMpPercent > 0;
      }
   }

   /**
    * Tổng % HP / MP tối đa cộng vào dòng "Hồi HP / MP mỗi 5 giây" của hiệu ứng còn hạn (Nấm hương 1%: HP 2000 -> +20,
    * gốc 15 thành 35) — {@link ItemStats#apply}. @return {hp%, mp%}
    */
   public static int[] regenPercents(PlayerData d, long now) {
      int hp = 0;
      int mp = 0;
      for (Map.Entry<Integer, Long> e : d.buffs.entrySet()) {
         Effect effect = e.getValue() > now ? BY_OPTION.get(e.getKey()) : null;
         if (effect != null) {
            hp += effect.regenHpPercent();
            mp += effect.regenMpPercent();
         }
      }
      return new int[] {hp, mp};
   }

   private static Effect stat(int item, int option, int group, long durationMs, int[]... stats) {
      return new Effect(item, option, group, durationMs, 0, 0, 0, 0, Special.NONE, stats);
   }

   private static Effect stat(int item, int option, long durationMs, int[]... stats) {
      return stat(item, option, option, durationMs, stats);
   }

   private static final long SEC = 1_000L;
   private static final long MIN = 60 * SEC;

   public static final int MUSHROOM_ITEM = 275;
   public static final int COCONUT_ITEM = 146;
   public static final int HONEY_ITEM = 151;
   public static final int PORK_ITEM = 149;
   public static final int PK_SHIELD_ITEM = 167;
   public static final int GOAT_MILK_ITEM = 297;

   public static final int OPTION_PK_SHIELD = 27;
   public static final int OPTION_IMMUNE = 28;
   public static final int OPTION_CHARM = 25;
   public static final int OPTION_STORM = 26;
   /** option_info hiệu ứng bất lợi: 6 suy yếu, 7 thiêu đốt, 8 đóng băng, 9 choáng, 21 rễ cây (trói). */
   public static final int OPTION_WEAKEN = 6;
   public static final int OPTION_BURN = 7;
   public static final int OPTION_FROZEN = 8;
   public static final int OPTION_STUN = 9;
   public static final int OPTION_ROOTED = 21;
   /** Icon kỹ năng 10 Lá Chắn Tiễn trên thanh hiệu ứng ({@code combat/CombatService} — bị hạ gục thì gỡ). */
   public static final int OPTION_SKILL_SHIELD = 23;
   public static final Set<Integer> DEBUFF_OPTIONS = Set.of(OPTION_WEAKEN, OPTION_BURN, OPTION_FROZEN, OPTION_STUN,
         OPTION_ROOTED);
   /** Khiên vỏ sò đang đeo: icon cố định (thời gian -1 = client không đếm giờ) trên thanh hiệu ứng. */
   public static final int OPTION_SHELL_SHIELD = 4;

   private static final Map<Integer, Effect> BY_ITEM = Map.ofEntries(
         Map.entry(MUSHROOM_ITEM, new Effect(MUSHROOM_ITEM, 22, 22, 30 * MIN, 1, 1, 0, 0, Special.NONE, new int[0][])),
         Map.entry(COCONUT_ITEM, new Effect(COCONUT_ITEM, 3, 3, 5 * MIN, 0, 0, 0, 100, Special.NONE,
               new int[][] {{44, 5}})),
         Map.entry(PORK_ITEM, new Effect(PORK_ITEM, 1, 1, 30 * MIN, 0, 0, 100, 0, Special.NONE,
               new int[][] {{STAT_ALL_PERCENT, 5}})),
         Map.entry(HONEY_ITEM, stat(HONEY_ITEM, 5, 5 * MIN, new int[] {15, 100}, new int[] {9, 100})),
         Map.entry(153, stat(153, 10, 10, 30 * MIN, new int[] {49, 50})),
         Map.entry(299, stat(299, 29, 10, 30 * MIN, new int[] {49, 100})),
         Map.entry(154, stat(154, 11, 11, 30 * MIN, new int[] {STAT_MOB_DAMAGE, 35})),
         Map.entry(300, stat(300, 30, 11, 30 * MIN, new int[] {STAT_MOB_DAMAGE, 70})),
         Map.entry(155, stat(155, 12, 12, 30 * MIN, new int[] {STAT_MOB_TAKEN, 35})),
         Map.entry(301, stat(301, 31, 12, 30 * MIN, new int[] {STAT_MOB_TAKEN, 70})),
         Map.entry(156, stat(156, 13, 30 * MIN, new int[] {54, 10})),
         Map.entry(157, stat(157, 14, 30 * MIN, new int[] {56, 10})),
         Map.entry(158, stat(158, 15, 5 * MIN, new int[] {4, 50})),
         Map.entry(159, stat(159, 16, 5 * MIN, new int[] {6, 50})),
         Map.entry(160, stat(160, 17, 5 * MIN, new int[] {8, 20})),
         Map.entry(161, stat(161, 18, 5 * MIN, new int[] {24, 20}, new int[] {20, 20})),
         Map.entry(162, stat(162, 19, 5 * MIN, new int[] {18, 50}, new int[] {22, 20})),
         Map.entry(163, stat(163, 20, 5 * MIN, new int[] {16, 50})),
         Map.entry(PK_SHIELD_ITEM, new Effect(PK_SHIELD_ITEM, OPTION_PK_SHIELD, OPTION_PK_SHIELD, 60 * MIN, 0, 0, 0, 0,
               Special.PK_SHIELD, new int[0][])),
         Map.entry(272, stat(272, 2, 5 * MIN, new int[] {11, 750})),
         Map.entry(GOAT_MILK_ITEM, new Effect(GOAT_MILK_ITEM, OPTION_IMMUNE, OPTION_IMMUNE, 30 * SEC, 0, 0, 0, 0,
               Special.CLEANSE, new int[0][])));

   /** Bùa linh mộc (263) sau khi hồi sinh. */
   public static final Effect CHARM_PROC = stat(GearCharges.Kind.CHARM.templateId, OPTION_CHARM, 15 * SEC,
         new int[] {44, 10});
   /** Mắt bão Darguun (264) khi dùng kỹ năng. */
   public static final Effect STORM_PROC = stat(GearCharges.Kind.STORM.templateId, OPTION_STORM, 5 * SEC,
         new int[] {19, 2000}, new int[] {24, 20});

   private static final Map<Integer, Effect> BY_OPTION = byOption();

   private static Map<Integer, Effect> byOption() {
      Map<Integer, Effect> map = new HashMap<>();
      BY_ITEM.values().forEach(e -> map.put(e.optionId(), e));
      map.put(CHARM_PROC.optionId(), CHARM_PROC);
      map.put(STORM_PROC.optionId(), STORM_PROC);
      return Map.copyOf(map);
   }

   /** Cộng dòng chỉ số của hiệu ứng còn hạn vào {@code sum} (theo stat_type.id) — {@link ItemStats#apply}. */
   public static void addStats(long[] sum, PlayerData d, long now) {
      for (Map.Entry<Integer, Long> e : d.buffs.entrySet()) {
         Effect effect = BY_OPTION.get(e.getKey());
         if (effect == null || e.getValue() <= now) {
            continue;
         }
         for (int[] stat : effect.stats()) {
            if (stat[0] > 0 && stat[0] < sum.length) {
               sum[stat[0]] += stat[1];
            }
         }
      }
   }

   private final World world;
   private final SessionManager sessions;
   private final Executor gamePool;

   public ItemBuffs(World world, SessionManager sessions, Executor gamePool) {
      this.world = world;
      this.sessions = sessions;
      this.gamePool = gamePool;
   }

   /** Hiệu ứng của mẫu item, null = không phải item hiệu ứng. */
   public static Effect forItem(int templateId) {
      return BY_ITEM.get(templateId);
   }

   /** Mọi hiệu ứng item (loại 11), theo mẫu item — admin panel liệt kê để bật trực tiếp. */
   public static java.util.Collection<Effect> itemEffects() {
      return BY_ITEM.values();
   }

   /**
    * Bật hiệu ứng {@code effect} tới {@code now + durationMs} (admin panel: thời gian tuỳ chọn, không cộng dồn), giữ
    * các hiệu ứng khác. Luồng session (hoặc nhân vật offline đang được admin giữ).
    */
   public static void applyFor(PlayerData d, Effect effect, long now, long durationMs) {
      Map<Integer, Long> next = new HashMap<>(d.buffs);
      next.entrySet().removeIf(e -> {
         Effect other = BY_OPTION.get(e.getKey());
         return other != null && other.group() == effect.group();
      });
      next.put(effect.optionId(), now + Math.max(1_000, durationMs));
      d.buffs = next;
   }

   /** Hiệu ứng theo {@code option_info.id}; null = không phải hiệu ứng item (icon kỹ năng / trạng thái). */
   public static Effect forOption(int optionId) {
      return BY_OPTION.get(optionId);
   }

   /** Hiệu ứng {@code optionId} còn hạn lúc {@code now}. Đọc map volatile -> mọi luồng. */
   public static boolean active(PlayerData d, int optionId, long now) {
      Long until = d.buffs.get(optionId);
      return until != null && until > now;
   }

   /** Hiệu ứng CÙNG NHÓM với {@code effect} còn hạn (option -> hết hạn), null = không có (hỏi trước khi đè). */
   public static Map.Entry<Integer, Long> activeInGroup(PlayerData d, Effect effect, long now) {
      for (Map.Entry<Integer, Long> e : d.buffs.entrySet()) {
         Effect other = BY_OPTION.get(e.getKey());
         if (other != null && other.group() == effect.group() && e.getValue() > now) {
            return e;
         }
      }
      return null;
   }

   /**
    * Chỉ hiện icon + đếm giờ trên thanh hiệu ứng (op 51) cho buff KHÔNG phải item (vd kỹ năng 10 / 16, bị đóng băng /
    * trói): {@code optionId} = {@code option_info.id}, hết hạn tự gỡ ở {@link #tick}. Không có trong {@link #BY_OPTION}
    * nên không cộng chỉ số / hồi gì — tác dụng thật do nơi gọi lo. Luồng session.
    */
   public static void showTimed(PlayerData d, int optionId, long until) {
      Map<Integer, Long> next = new HashMap<>(d.buffs);
      next.put(optionId, until);
      d.buffs = next;
   }

   /**
    * Vào game: chỉ giữ hiệu ứng của ITEM ({@link #BY_OPTION}); icon kỹ năng / trạng thái ({@link #showTimed}) không sống
    * qua thoát game vì tác dụng thật (vd tốc chạy) không được lưu. Gọi trước khi gửi thanh hiệu ứng.
    */
   public static void dropSkillIcons(PlayerData d) {
      if (d.buffs == null) {
         d.buffs = Map.of();
         return;
      }
      if (BY_OPTION.keySet().containsAll(d.buffs.keySet())) {
         return;
      }
      Map<Integer, Long> next = new HashMap<>(d.buffs);
      next.keySet().retainAll(BY_OPTION.keySet());
      d.buffs = next.isEmpty() ? Map.of() : next;
   }

   /**
    * Bật hiệu ứng (luồng session): {@code stack} (dùng item) + CÙNG MÓN còn hạn -> CỘNG DỒN thời gian (hết hạn cũ + thời
    * gian món); không thì làm mới từ {@code now} (phụ kiện kích hoạt). Khác món cùng nhóm thì bỏ món cũ.
    */
   /** Bật / làm mới hiệu ứng từ {@code now} (không cộng dồn). */
   public static void apply(PlayerData d, Effect effect, long now) {
      apply(d, effect, now, false);
   }

   public static void apply(PlayerData d, Effect effect, long now, boolean stack) {
      apply(d, effect, now, stack, 1);
   }

   /** Như trên, thời gian × {@code count} ("Dùng nhiều" {@code count} món). */
   public static void apply(PlayerData d, Effect effect, long now, boolean stack, int count) {
      Long current = d.buffs.get(effect.optionId());
      long from = stack && current != null && current > now ? current : now;
      Map<Integer, Long> next = new HashMap<>(d.buffs);
      next.entrySet().removeIf(e -> {
         Effect other = BY_OPTION.get(e.getKey());
         return other != null && other.group() == effect.group();
      });
      next.put(effect.optionId(), from + effect.durationMs() * Math.max(1, count));
      d.buffs = next;
   }

   /** Gỡ các hiệu ứng {@code options} (luồng session). @return true nếu có gỡ */
   public static boolean remove(PlayerData d, Set<Integer> options) {
      if (Collections.disjoint(d.buffs.keySet(), options)) {
         return false;
      }
      Map<Integer, Long> next = new HashMap<>(d.buffs);
      next.keySet().removeAll(options);
      d.buffs = next.isEmpty() ? Map.of() : next;
      return true;
   }

   /**
    * Bật hiệu ứng phụ kiện {@code effect} (Bùa linh mộc / Mắt bão) trên luồng session: gửi thanh hiệu ứng, tính lại +
    * phát khối chỉ số.
    */
   public static void applyProc(PlayerSession session, World world, Effect effect) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      long now = System.currentTimeMillis();
      apply(d, effect, now); // phụ kiện kích hoạt: làm mới, không cộng dồn
      session.send(packet(d, now));
      ItemService.refreshLook(d);
      if (world != null) {
         world.broadcastStats(d);
      }
   }

   /** Op 51 sub 0: toàn bộ hiệu ứng còn hạn (client xoá danh sách cũ rồi vẽ lại). */
   public static Message packet(PlayerData d, long now) {
      Message msg = new Message(Opcode.BUFFS);
      msg.writeByte(0);
      Map<Integer, Long> buffs = d.buffs;
      boolean shield = d.gear != null && d.gear.has(GearCharges.Kind.SHIELD);
      int count = shield ? 1 : 0;
      for (long until : buffs.values()) {
         if (until > now) {
            count++;
         }
      }
      msg.writeByte(count);
      if (shield) { // đang đeo Khiên vỏ sò: icon cố định (-1 = không đếm giờ)
         msg.writeInt(OPTION_SHELL_SHIELD);
         msg.writeInt(-1);
      }
      for (Map.Entry<Integer, Long> e : buffs.entrySet()) {
         if (e.getValue() > now) {
            msg.writeInt(e.getKey());
            msg.writeInt((int) Math.min(Integer.MAX_VALUE, e.getValue() - now));
         }
      }
      return msg;
   }

   /**
    * Mỗi {@link #REGEN_INTERVAL_SECONDS} giây (luồng scheduler): người có hiệu ứng / đang thiếu HP / MP / phụ kiện vừa
    * bị trừ lượt -> xếp {@link #regen} vào session. Đọc hp / mp ở đây chỉ để lọc (có thể lệch 1 nhịp), luồng session tính
    * lại.
    */
   public void tick() {
      long now = System.currentTimeMillis();
      for (PlayerSession session : sessions.snapshot()) {
         PlayerData d = session.getPlayer();
         PlayerStats stats = d == null ? null : d.stats;
         if (d != null && d.inWorld && (!d.buffs.isEmpty() || d.pkPoints > 0 || d.spiritBadgeShown // sát khí / huy hiệu
               || (d.gear != null && d.gear.dirty()) || (stats != null && !d.isDead()
               && ((stats.hpRegen > 0 && d.hp < stats.maxHp) || (stats.mpRegen > 0 && d.mp < stats.maxMp))))) {
            session.inbox().execute(() -> regen(session, now), gamePool);
         }
      }
   }

   /**
    * Luồng session: chép lượt phụ kiện ({@link GearCharges#flush}), bỏ hiệu ứng hết hạn ({@link #dropExpired}), hồi HP /
    * MP = chỉ số "Hồi HP / MP mỗi 5 giây" (gốc + đồ + Nấm hương 1% HP / MP tối đa — đã tính trong {@link ItemStats}).
    */
   /** Người chơi giả (không nằm trong danh sách session của {@link #tick}): hồi HP / MP 5 s như người thật. Luồng session. */
   public void regenBot(PlayerSession session) {
      regen(session, System.currentTimeMillis());
   }

   private void regen(PlayerSession session, long now) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld) {
         return;
      }
      GearCharges.flush(session);
      dropExpired(session, world, now);
      com.tlnt.server.social.PkPoints.tick(session, world, d, now); // sát khí -1 mỗi 60 phút
      com.tlnt.server.badge.SpiritBadgeService.tick(session, d, now); // Huy hiệu Tinh Linh hết hạn -> tắt nút
      PlayerStats stats = d.stats;
      if (stats == null || d.isDead() || (d.hp >= stats.maxHp && d.mp >= stats.maxMp)) {
         return;
      }
      // Nấm hương đã nằm trong dòng Hồi HP / MP mỗi 5 s (ItemStats: + 1% HP / MP tối đa), không cộng riêng nữa.
      long hpAmount = Math.max(0, stats.hpRegen);
      long mpAmount = Math.max(0, stats.mpRegen);
      if ((hpAmount <= 0 || d.hp >= stats.maxHp) && (mpAmount <= 0 || d.mp >= stats.maxMp)) {
         return;
      }
      long hp = hpAmount > 0 ? d.healHp(hpAmount, stats.maxHp) : d.hp;
      long mp = mpAmount > 0 ? d.healMp(mpAmount, stats.maxMp) : d.mp;
      world.broadcastAround(session, ItemPackets.hpMp(d.getId(), hp, stats.maxHp, mp, stats.maxMp).toPacket());
   }

   /**
    * Luồng session: bỏ hiệu ứng đã hết hạn; có thì gửi lại thanh hiệu ứng, hiệu ứng có chỉ số thì tính lại + phát khối
    * chỉ số. Gọi mỗi 5 s ({@link #regen}) và đúng lúc hết của hiệu ứng ngắn (Mắt bão 5 s, Bùa linh mộc 15 s).
    */
   public static void dropExpired(PlayerSession session, World world, long now) {
      PlayerData d = session.getPlayer();
      if (d == null) {
         return;
      }
      Map<Integer, Long> buffs = d.buffs;
      boolean expired = false;
      boolean statsChanged = false;
      for (Map.Entry<Integer, Long> e : buffs.entrySet()) {
         if (e.getValue() <= now) {
            expired = true;
            Effect effect = BY_OPTION.get(e.getKey());
            statsChanged |= effect != null && effect.changesStats();
         }
      }
      if (!expired) {
         return;
      }
      Map<Integer, Long> next = new HashMap<>();
      buffs.forEach((k, v) -> {
         if (v > now) {
            next.put(k, v);
         }
      });
      d.buffs = next.isEmpty() ? Map.of() : next;
      session.send(packet(d, now));
      if (statsChanged) { // hết buff chỉ số: tính lại + phát khối chỉ số
         ItemService.refreshLook(d);
         if (world != null) {
            world.broadcastStats(d);
         }
      }
   }
}
