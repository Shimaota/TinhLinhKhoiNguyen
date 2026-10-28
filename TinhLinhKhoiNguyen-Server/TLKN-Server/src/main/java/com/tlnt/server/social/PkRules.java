package com.tlnt.server.social;

import java.util.Set;

/**
 * Trạng thái chiến đấu (cờ PK — client {@code FactionStatus.FactionType}, gửi bằng byte = ordinal: khối chỉ số + op 42
 * sub 0) và luật ai được đánh ai, theo bảng hướng dẫn "Trạng thái chiến đấu" của game:
 *
 * <pre>
 * byte | client    | cờ (icon)            | đánh được
 * 0    | NEUTRAL   | Hoà bình  (ui/32)    | chỉ người cờ Thử thách (Hoà bình không đánh / không bị đánh bởi Hoà bình, Tổ đội,
 *      |           |                      | Bộ lạc)
 * 1    | FACTION_A | Tổ đội    (ui/33)    | mọi người trừ cùng tổ đội + người cờ Hoà bình
 * 2    | FACTION_B | Thử thách (ui/34)    | tất cả (sau này trừ người dùng Bùa hộ giáp); tên đỏ
 * 3    | FACTION_C | Bộ lạc    (ui/166, cờ tím) | mọi người trừ cùng bộ lạc + người cờ Hoà bình
 * </pre>
 *
 * Client ({@code AutoTargetController.canTargetPlayer}) chỉ tự chọn mục tiêu hẹp hơn (Tổ đội chỉ nhắm Tổ đội / Thử
 * thách, Bộ lạc chỉ nhắm Bộ lạc / Thử thách) -> server cho phép rộng hơn không sao: client không gửi đòn nào server
 * từ chối oan. {@code sameTribe} = cùng bộ lạc ({@code PlayerData.tribe}, client so {@code guildId} trong khối chỉ số).
 */
public final class PkRules {

   public static final byte PEACE = 0;
   public static final byte PARTY = 1;
   public static final byte CHALLENGE = 2;
   /** Cờ tím. */
   public static final byte TRIBE = 3;

   /**
    * Map cấm PK ({@code map_template.id}): 2 Làng ánh sáng Eldarah, 5 Nông trại. Không đổi được cờ ở đây; vào map này
    * thì tự về Hoà bình.
    */
   public static final Set<Integer> PEACE_MAP_IDS = Set.of(2, 5);

   /**
    * Điểm sát khí ({@code PlayerData.pkPoints}, hướng dẫn "Điểm sát khí"): cờ Thử thách hạ gục người khác (ngoài khu PK
    * riêng — Lôi vực; ngoài thách đấu) +1, tối đa {@link #MAX_PK_POINTS}; đạt {@link #PENALTY_POINTS} -> chỉ số chiến đấu
    * còn {@link #PENALTY_LEFT_PERCENT}%, đạt {@link #MAX_PK_POINTS} -> còn {@link #MAX_PENALTY_LEFT_PERCENT}% (người dùng
    * đổi "cấm đăng nhập 24 giờ" thành giảm 75%); mỗi {@link #DECAY_MS} không hạ ai thì -1 (tính cả lúc offline); Bùa
    * thanh tẩy về 0.
    */
   public static final int MAX_PK_POINTS = 50;
   public static final int PENALTY_POINTS = 20;
   public static final int PENALTY_LEFT_PERCENT = 50;
   public static final int MAX_PENALTY_LEFT_PERCENT = 25;
   public static final long DECAY_MS = 60 * 60_000L;

   /** Khoảng cách tối thiểu giữa 2 lần đổi cờ. */
   public static final long CHANGE_COOLDOWN_MS = 10_000;
   /** Bật Thử thách rồi phải giữ ít nhất chừng này mới được đổi (chống giết người xong đổi về Hoà bình ngay). */
   public static final long CHALLENGE_MIN_MS = 60_000;

   private PkRules() {
   }

   /** % chỉ số chiến đấu còn lại theo điểm sát khí: 100 / 50 / 25. */
   public static int statPercentLeft(int pkPoints) {
      return pkPoints >= MAX_PK_POINTS ? MAX_PENALTY_LEFT_PERCENT
            : pkPoints >= PENALTY_POINTS ? PENALTY_LEFT_PERCENT : 100;
   }

   /**
    * Giảm sát khí theo thời gian: mỗi {@link #DECAY_MS} kể từ {@code since} -1 điểm. @return {điểm mới, mốc mới}
    * (hết điểm -> mốc 0)
    */
   public static long[] decay(int points, long since, long now) {
      if (points <= 0) {
         return new long[] {0, 0};
      }
      if (since <= 0 || now < since) {
         return new long[] {points, now}; // chưa có mốc (dữ liệu cũ): bắt đầu đếm từ giờ
      }
      long steps = (now - since) / DECAY_MS;
      if (steps <= 0) {
         return new long[] {points, since};
      }
      int left = (int) Math.max(0, points - steps);
      return new long[] {left, left == 0 ? 0 : since + steps * DECAY_MS};
   }

   /**
    * Phạt sát khí: mọi chỉ số chiến đấu × {@link #statPercentLeft}% (trừ Tốc chạy, Thể lực, Thưởng vàng / EXP). Gọi cuối
    * {@code PlayerStats.of}.
    */
   public static void applyPenalty(com.tlnt.server.player.PlayerStats s, int pkPoints) {
      int p = statPercentLeft(pkPoints);
      if (p >= 100) {
         return;
      }
      s.maxHp = Math.max(1, s.maxHp * p / 100);
      s.maxMp = Math.max(1, s.maxMp * p / 100);
      s.attackPower = s.attackPower * p / 100;
      s.attackVsMob = s.attackVsMob * p / 100;
      s.critChance = s.critChance * p / 100;
      s.critDamagePercent = s.critDamagePercent * p / 100;
      s.armor = s.armor * p / 100;
      s.armorPen = s.armorPen * p / 100;
      s.evasion = s.evasion * p / 100;
      s.accuracy = s.accuracy * p / 100;
      s.hpRegen = s.hpRegen * p / 100;
      s.mpRegen = s.mpRegen * p / 100;
      s.hpLeechPercent = s.hpLeechPercent * p / 100;
      s.mpLeechPercent = s.mpLeechPercent * p / 100;
      s.hpLeechVsMobPercent = s.hpLeechVsMobPercent * p / 100;
      s.mpLeechVsMobPercent = s.mpLeechVsMobPercent * p / 100;
      s.reflectDamagePercent = s.reflectDamagePercent * p / 100;
      s.stunDamage = s.stunDamage * p / 100;
      s.weakenDamage = s.weakenDamage * p / 100;
      s.iceDamage = s.iceDamage * p / 100;
      s.burnDamage = s.burnDamage * p / 100;
      s.thunderDamage = s.thunderDamage * p / 100;
      s.critResistPercent = s.critResistPercent * p / 100;
      s.damageAmpPercent = s.damageAmpPercent * p / 100;
      s.damageAbsorbPercent = s.damageAbsorbPercent * p / 100;
      s.effectResistPercent = s.effectResistPercent * p / 100;
      s.damageResist = s.damageResist * p / 100;
   }

   public static boolean isValid(int faction) {
      return faction >= PEACE && faction <= TRIBE;
   }

   public static String name(int faction) {
      return switch (faction) {
         case PARTY -> "Tổ đội";
         case CHALLENGE -> "Thử thách";
         case TRIBE -> "Bộ lạc";
         default -> "Hoà bình";
      };
   }

   /**
    * Người chơi tự đổi cờ (menu biểu tượng PK, op 42 sub 0) từ {@code current} sang {@code wanted}.
    *
    * @param enteredCurrentAt lúc vào cờ hiện tại (ms), 0 = chưa từng đổi
    * @return null = được đổi; khác null = câu báo lỗi gửi client
    */
   public static String checkChange(byte current, int wanted, long enteredCurrentAt, long now) {
      return checkChange(current, wanted, enteredCurrentAt, now, -1);
   }

   /** Như trên, thêm luật map cấm PK ({@link #PEACE_MAP_IDS}). */
   public static String checkChange(byte current, int wanted, long enteredCurrentAt, long now, int mapId) {
      if (PEACE_MAP_IDS.contains(mapId)) {
         return "Không thể đổi cờ trong map này";
      }
      if (!isValid(wanted)) {
         return "Trạng thái chiến đấu không hợp lệ.";
      }
      long waitMs = enteredCurrentAt + CHANGE_COOLDOWN_MS - now;
      if (current == CHALLENGE) {
         waitMs = Math.max(waitMs, enteredCurrentAt + CHALLENGE_MIN_MS - now);
      }
      if (enteredCurrentAt > 0 && waitMs > 0) {
         return "Vui lòng đợi " + ((waitMs + 999) / 1000) + " giây nữa để đổi trạng thái chiến đấu.";
      }
      return null;
   }

   /**
    * {@code attacker} đánh được {@code target} không (theo cờ). Thách đấu (op 41) đang diễn ra giữa 2 người thì luôn
    * được, bất kể cờ.
    */
   public static boolean canAttack(byte attacker, byte target, boolean sameParty, boolean sameTribe, boolean dueling) {
      if (dueling) {
         return true;
      }
      return switch (attacker) {
         case PEACE -> target == CHALLENGE;
         case PARTY -> target != PEACE && !sameParty;
         case CHALLENGE -> true;
         case TRIBE -> target != PEACE && !sameTribe;
         default -> false;
      };
   }
}
