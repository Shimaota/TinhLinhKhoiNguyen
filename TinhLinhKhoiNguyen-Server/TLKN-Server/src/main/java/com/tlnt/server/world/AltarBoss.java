package com.tlnt.server.world;

import com.tlnt.server.altar.AltarRules;
import com.tlnt.server.combat.MobRules;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerStats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Boss của 1 khu Tế đàn Thiên tích ({@link AltarRules.BossSpec}): người chơi giả do SERVER điều khiển (op 3 loại thực
 * thể 8, cờ Thử thách -> client mọi người chọn làm mục tiêu được như người chơi; người đánh nó = đòn nhắm người chơi,
 * {@code ZoneCombat} chuyển sang {@link #id}). Xuất hiện ở chỗ NPC {@code npcId} của map (0 = chỗ con quái cuối map —
 * xa nhất về bên phải), không hồi
 * sinh; chết thì rơi quà khoá ({@code BossSpec.drops}) cho người kết liễu.
 *
 * <p>AI ({@link #tick}): mở màn tìm người gần nhất TRONG CẢ KHU (hoặc người vừa đánh nó); sau MỖI chiêu chọn lại mục
 * tiêu theo điểm ({@link #score}: lâu chưa bị nhắm + vừa đánh boss − khoảng cách) -> rải đòn cho nhiều người, không dồn
 * 1 người, không chạy loạn sang đầu kia map. Đi tới bằng {@link BossMover}
 * (chạy, nhảy bậc / tường có trọng lực, rơi xuống tầng dưới; kẹt lâu mới nhảy vọt cứu kẹt), tới gần thì ra
 * chiêu theo vòng {@code BossSpec.rotation} (id kỹ năng op 21):
 * <ul>
 *   <li>1 (đánh thường, client tự xen kẽ {@code attack_1 / attack_2}) / 32 ({@code stick/skill1}): đánh 1 mục tiêu.</li>
 *   <li>skill2 (33, {@code stick/skill2}, byte hướng): chém mọi người phía trước.</li>
 *   <li>skill3 (34, {@code stick/skill3}, rung màn hình): dậm đất, mọi người quanh boss bị HẤT TUNG (op 12 sub 2).</li>
 *   <li>skill9 (40: byte 0 bắt đầu {@code skill9_1/2}, bool true + mục tiêu = 1 lông vũ, byte 1 kết thúc {@code skill9_3}):
 *       bắn 50 lông vũ, mỗi chiếc gây 500% sát thương.</li>
 *   <li>skill4 (35, {@code stick/skill4}, Tinh linh Thần mộc — Rừng Linh Mộc): rễ cây trói 3 người, hút máu 15 giây.</li>
 * </ul>
 * Sát thương = {@code BossSpec.hitDamage} × % chiêu × 2200/(2200+Giáp) ±5%; chí mạng (tỉ lệ phần vạn) cộng thêm
 * sát thương chí mạng / 1000 (180 = +18%).
 *
 * <p><b>Boss thế giới</b> ({@link #smart}, vd Tinh Linh Rừng xanh — {@code worldboss/}): CHỦ ĐỘNG tìm tới người chơi
 * trong cả khu (như boss Tế đàn — ưu tiên người gần / vừa đánh nó, {@link #score}); chọn chiêu theo tình huống ({@link #chooseSmart}: đông
 * người quanh -> dậm đất, đông người phía trước -> chém, người chạy xa / đông người trong tầm -> trói); sau chiêu hay chạy
 * VÒNG sang phía bên kia mục tiêu ({@link BossMover#crossTo}); khu không còn ai thì chạy qua chạy lại quanh chỗ xuất hiện
 * ({@link #patrol}). Trói {@link #rootMs} / {@link #rootTargets} người, rễ đốt {@link #rootBurnPermille} phần nghìn HP
 * tối đa mỗi giây; Xuyên giáp trừ vào Giáp người bị đánh ({@link #pierceArmor}).
 * CHỈ luồng tick của khu đụng tới.
 */
final class AltarBoss {

   /** Id thực thể: âm, ngoài vùng thú cưng (≤ MIN/2) + hộ tống (≤ MIN/4) + ảnh ảo (-1, -2...). */
   private static final int ID_BASE = Integer.MIN_VALUE / 8;

   /** Tầm ra chiêu cận chiến (skill1 / 2 / 3) — rộng hơn chỗ dừng {@link BossMover#STOP_PX}. */
   static final float MELEE_X_PX = 230f;
   static final float MELEE_Y_PX = 160f;
   /** Tầm bắn lông vũ (skill9) — xa hơn thì vẫn đuổi. */
   static final float RANGED_X_PX = 900f;
   static final float RANGED_Y_PX = 500f;
   static final float SKILL2_FRONT_PX = 450f;
   static final float SKILL2_Y_PX = 200f;
   static final float SKILL3_RADIUS_PX = 500f;
   static final float SKILL3_Y_PX = 250f;
   /** Thời gian ra chiêu (đứng yên) + lúc trúng đòn trong animation. */
   /** Đánh thường (Zyren / Vaelor): animation ngắn + nghỉ ngắn -> ~0,75 s / đòn. */
   static final long ATTACK_CAST_MS = 500;
   static final long ATTACK_GAP_MS = 250;
   static final long SKILL1_CAST_MS = 1_000;
   static final long SKILL2_CAST_MS = 1_200;
   static final long SKILL2_HIT_MS = 500;
   static final long SKILL3_CAST_MS = 1_500;
   /** Lúc dậm đất trong animation (client rung màn hình ở 40% animation). */
   static final long SKILL3_HIT_MS = 500;
   /** Hất tung TẠI CHỖ (Box2D client: trọng lực 49 m/s², 1 m = 100 px): nảy cao ~2 m (v = √(2·49·2)), không văng ngang. */
   static final float KNOCK_UP_MPS = 14f;
   static final int KNOCK_MS = 600;
   /** Lông vũ bắn liên tục: chiếc đầu sau bấy lâu, rồi cách nhau rất ngắn. */
   static final long FEATHER_FIRST_MS = 300;
   static final long FEATHER_INTERVAL_MS = 100;
   /** Không cộng thêm thời gian nghỉ sau animation; từng skill tự dùng cooldown của nó. */
   static final long SKILL_GAP_MS = 0;
   /** Suy yếu: boss gây ít hơn bấy nhiêu % trong thời gian hiệu ứng. */
   static final int WEAKEN_PERCENT = 20;
   /** Bị đánh: chat tối đa 1 lần / bấy nhiêu ms. */
   static final long HURT_CHAT_MS = 3_000;
   /** Chết xong bấy lâu thì gỡ khỏi client (op 4) — kịp chơi animation ngã. */
   static final long CORPSE_MS = 3_000;
   /** Chọn mục tiêu ({@link #score}): tối đa bấy nhiêu ms "chưa bị nhắm" được tính điểm, điểm cộng cho người vừa đánh
    *  boss (nhớ bấy lâu), và bao nhiêu px khoảng cách thì trừ 1 điểm. */
   static final long SPREAD_CAP_MS = 10_000;
   static final float ATTACKER_BONUS = 3f;
   static final long ATTACKER_MEMORY_MS = 10_000;
   static final float DISTANCE_PX_PER_POINT = 400f;

   /** Boss cuối map (npcId 0) mà map không có quái: cách mép phải bấy nhiêu (trước cửa sang map sau). */
   static final float MAP_END_MARGIN_PX = 450f;
   static final int SKILL2 = AltarRules.MOVE_SKILL2;
   static final int SKILL3 = AltarRules.MOVE_SKILL3;
   static final int SKILL9 = AltarRules.MOVE_FEATHERS;
   static final int ROOT = AltarRules.MOVE_ROOT;
   static final int BOLT = AltarRules.MOVE_BOLT;
   static final int WEAKEN = AltarRules.MOVE_WEAKEN;
   static final int SHIELD = AltarRules.MOVE_SHIELD;
   static final int STUN = AltarRules.MOVE_STUN;
   static final int FREEZE = AltarRules.MOVE_FREEZE;
   static final int SKILL11 = AltarRules.MOVE_SKILL11;
   /** Chiêu đặc biệt thay trói (thứ tự ưu tiên khi spec có nhiều). */
   static final int[] SPECIALS = {BOLT, WEAKEN, SHIELD, STUN, FREEZE, SKILL11};
   /** Fallback số mục tiêu 36 / 38; skill 39 không giới hạn; thời gian trừ Kháng hiệu ứng. */
   static final int SPECIAL_TARGETS = 3;
   static final long SPECIAL_COOLDOWN_MS = 15_000;
   static final int WEAKEN_STATUS_MS = 15_000;
   static final int STUN_STATUS_MS = 15_000;
   static final int FREEZE_STATUS_MS = 15_000;
   /** 37 khiên nước: giảm sát thương theo skill_effect trong {@link #SHIELD_MS}. */
   static final int SHIELD_REDUCE_PERCENT = 75;
   static final long SHIELD_MS = 15_000;
   static final long SHIELD_COOLDOWN_MS = 25_000;
   static final long SPECIAL_CAST_MS = 1_367;
   /** Lúc chiêu 43 {@code stick/skill11} trúng (sự kiện skill_up, chars/24). */
   static final long SKILL11_HIT_MS = 533;
   /** Tia sét (41): hồi chiêu; đứng sát mục tiêu thì chỉ bấy nhiêu % lần chọn chiêu dùng sét. */
   static final long BOLT_COOLDOWN_MS = 4_000;
   static final int MELEE_BOLT_PERCENT = 30;
   static final long BOLT_CAST_MS = 633;
   /** Trói (35): lúc rễ cây mọc trong animation {@code stick/skill4} + thời gian đứng ra chiêu. */
   static final long ROOT_HIT_MS = 600;
   static final long ROOT_CAST_MS = 1_500;
   /** Hồi chiêu trói: thời gian trói ({@link AltarRules#ROOT_MS}) + 10 s — hết trói rồi mới có thể bị trói lại. */
   static final long ROOT_COOLDOWN_MS = AltarRules.ROOT_MS + 10_000;
   /** Hồi chiêu trói = thời gian trói + bấy nhiêu. */
   static final long ROOT_REST_MS = ROOT_COOLDOWN_MS - AltarRules.ROOT_MS;

   // ---- boss thế giới ({@link #smart})
   /** Hồi chiêu chém (skill2) / dậm đất (skill3) khi chọn chiêu theo tình huống. */
   static final long SMART_SKILL2_CD_MS = 4_000;
   static final long SMART_SKILL3_CD_MS = 6_000;
   /** Đứng 1 mình vẫn có lúc ăn chiêu lớn: % mỗi lần chọn (khi hết hồi). */
   static final int SOLO_SKILL3_PERCENT = 25;
   static final int SOLO_SKILL2_PERCENT = 35;
   /** Ra xong chiêu: % chạy vòng sang phía bên kia mục tiêu (chạy qua chạy lại). */
   static final int CROSS_PERCENT = 35;
   /** Đi tuần quanh chỗ xuất hiện: xa tối đa, tốc = tốc chạy × hệ số, nghỉ giữa 2 lượt, bỏ lượt nếu kẹt bấy lâu. */
   static final float PATROL_PX = 350f;
   static final float PATROL_SPEED = 0.5f;
   static final long PATROL_PAUSE_MIN_MS = 1_500;
   static final long PATROL_PAUSE_MAX_MS = 3_500;
   static final long PATROL_GIVE_UP_MS = 5_000;
   /** Hết người để đánh mà đang cách chỗ xuất hiện quá bấy nhiêu: nhảy vọt về. */
   static final float HOME_LEAP_PX = 1_200f;
   /** Rễ cây đốt máu người bị trói mỗi bấy lâu. */
   static final long ROOT_BURN_INTERVAL_MS = 1_000;

   final AltarRules.BossSpec spec;
   final int id;
   final PlayerStats stats;
   /** Sát thương từng người lên boss (người gây nhiều nhất nhận quà khi boss chết không do đòn của ai). */
   final DamageLedger damageLedger = new DamageLedger();
   /** Người ra đòn kết liễu (nhận quà Rừng Linh Mộc); 0 = chưa / chết không do đòn của ai. */
   int lastHitId;
   float x;
   float y;
   long hp;
   boolean dead;
   /** Đã gỡ khỏi client (sau {@link #CORPSE_MS}). */
   boolean gone;

   /** Người boss đang đuổi / đánh, 0 = không ai. */
   private int targetId;
   /** Ra xong 1 chiêu: lần chọn mục tiêu kế tiếp tính lại điểm (rải đòn cho nhiều người). */
   private boolean retargetAfterCast;
   /** Lần cuối boss nhắm / bị đánh bởi từng người (chọn mục tiêu). */
   private final java.util.Map<Integer, Long> lastTargetedAt = new java.util.HashMap<>();
   private final java.util.Map<Integer, Long> lastHurtBy = new java.util.HashMap<>();
   private final BossMover mover;
   private long busyUntil;
   private long nextSkillAt;
   private int rotation;
   private long lastHurtChatAt;
   /** Lần ra chiêu trói (35) gần nhất. */
   private long lastRootAt = Long.MIN_VALUE / 2;
   private long lastTickAt;
   private long nextSendAt;
   /** Choáng / băng tới lúc này: không đi, không ra chiêu, đòn đang tung dở bị ngắt. */
   private long disabledUntil;
   /** Trói tới lúc này: không đi (vẫn đánh được người đứng gần). */
   private long rootedUntil;
   /** Suy yếu tới lúc này: sát thương −{@link #WEAKEN_PERCENT}%. */
   private long weakenedUntil;
   /** Đang bị thiêu đốt tới (chỉ số Sát thương thiêu đốt của người đánh). Luồng tick. */
   long burningUntil;

   /** Mini boss Tiểu Tinh linh Cáo (không đánh ai, trộm vàng khoá — {@link #thiefTick}); null = boss thường. */
   FoxThief thief;
   /** Ngọc thố Trung thu (không đánh ai, không chết, ăn cà rốt — {@link #rabbitTick}); null = không phải thỏ. */
   Rabbit rabbit;

   /** Boss thế giới: chọn chiêu theo tình huống, chạy qua chạy lại, chỉ đuổi người ở gần, rảnh thì đi tuần. */
   boolean smart;
   /** Chỗ xuất hiện (đi tuần quanh đây). */
   float homeX;
   float homeY;
   /** Chiêu trói (35): fallback khi skill_effect chưa có dòng; SQL thắng các giá trị này. */
   int rootMs = 15_000;
   int rootTargets = 3;
   /** Cấu hình riêng của boss thế giới; skill_effect.status_power là fallback chung nếu bằng 0. */
   int rootBurnPermille;
   /** Xuyên giáp của boss trừ vào Giáp người bị đánh (game gốc: Tinh Linh Rừng xanh 750 xuyên hết Giáp 342). */
   boolean pierceArmor;
   private long skill2ReadyAt;
   private long skill3ReadyAt;
   private long boltReadyAt;
   private long specialReadyAt;
   /** Hồi chiêu từng skill lấy trực tiếp từ skill_level; không chứa đòn thường (id 1). */
   private final Map<Integer, Long> skillReadyAt = new HashMap<>();
   /** Khiên nước (37) tới lúc này: nhận −{@link #SHIELD_REDUCE_PERCENT}% sát thương ({@code ZoneCombat.applyBossDamage}). */
   long shieldUntil;
   private float patrolX = Float.NaN;
   private float patrolY = Float.NaN;
   /** Cáo đang trốn: chỗ chạy tới (NaN = chọn lại) + lúc chọn lại. */
   private float escapeX = Float.NaN;
   private float escapeY = Float.NaN;
   private long escapeRepickAt;
   private long patrolPauseUntil;
   private long patrolGiveUpAt;

   AltarBoss(AltarRules.BossSpec spec, MapTemplate map) {
      this.spec = spec;
      this.id = ID_BASE - spec.mapId();
      this.stats = spec.stats();
      this.hp = spec.hp();
      float sx = map.spawnX();
      float sy = map.spawnY();
      if (spec.npcId() == 0) { // chỗ con quái cuối map (x lớn nhất)
         MapTemplate.MobSpawn last = null;
         for (MapTemplate.MobSpawn mob : map.mobs()) {
            if (last == null || mob.x() > last.x()) {
               last = mob;
            }
         }
         if (last != null) {
            sx = last.x();
            sy = last.y();
         } else {
            sx = map.clampX(map.width() - MAP_END_MARGIN_PX);
            float ground = Pet.surfaceBelow(map, sx, map.height());
            sy = Float.isNaN(ground) ? sy : ground;
         }
      }
      for (MapTemplate.NpcSpawn npc : map.npcs()) {
         if (spec.npcId() != 0 && npc.id() == spec.npcId()) {
            sx = npc.x();
            sy = npc.y();
            break;
         }
      }
      this.x = sx;
      this.y = sy;
      this.mover = new BossMover(sx, sy);
   }

   /** Boss thế giới: đứng ở (x, y) với id riêng ({@code World.spawnWorldBoss}). */
   AltarBoss(AltarRules.BossSpec spec, int id, float x, float y) {
      this.spec = spec;
      this.id = id;
      this.stats = spec.stats();
      this.hp = spec.hp();
      this.x = x;
      this.y = y;
      this.homeX = x;
      this.homeY = y;
      this.mover = new BossMover(x, y);
   }

   /** Id thực thể của boss thế giới thứ {@code seq} — mỗi lần xuất hiện 1 id mới, không trùng boss Tế đàn / phó bản. */
   static int worldId(int seq) {
      return ID_BASE - 10_000 - (seq & 0xFFFFF);
   }

   /** Tạo boss cho khu Tế đàn của map này (null = map không có boss). */
   static AltarBoss create(MapTemplate map) {
      AltarRules.BossSpec spec = AltarRules.boss(map.id());
      return spec == null ? null : new AltarBoss(spec, map);
   }

   boolean alive() {
      return !dead;
   }

   // ------------------------------------------------------------------ bị đánh

   /** Người chơi đánh trúng: boss quay sang đuổi người này nếu đang rảnh; chat "Á đau" (giãn cách). Luồng tick. */
   void onHurt(Zone zone, WorldPlayer attacker, long now) {
      if (rabbit != null) {
         return; // Ngọc thố: không phản ứng, không đuổi ai
      }
      if (thief != null) {
         if (thief.detectedAt == 0) {
            zone.broadcast(WorldPackets.chat(id, FoxThief.DETECTED_CHAT), null);
         }
         thief.hit(now); // bị phát hiện: bỏ trộm, chạy trốn, đếm giờ đổi map
         return;
      }
      lastHurtBy.put(attacker.id(), now); // được ưu tiên khi chọn mục tiêu kế tiếp
      if (targetId == 0) {
         targetId = attacker.id();
      }
      if (spec.hurtChat() != null && now - lastHurtChatAt >= HURT_CHAT_MS) {
         lastHurtChatAt = now;
         zone.broadcast(WorldPackets.chat(id, spec.hurtChat()), null);
      }
   }

   /** Trừ máu (đã giảm trừ); @return HP còn. Chết thì hẹn gỡ khỏi client. */
   long damage(Zone zone, long amount) {
      if (dead) {
         return 0;
      }
      if (rabbit != null) {
         return hp; // Ngọc thố: hiện thanh HP nhưng không thể bị giết
      }
      hp = Math.max(0, hp - Math.max(0, amount));
      if (hp == 0) {
         dead = true;
         targetId = 0;
         zone.schedule(CORPSE_MS, () -> {
            gone = true;
            zone.broadcast(WorldPackets.playerLeave(id), null);
         });
      }
      return hp;
   }

   /** Hồi HP boss, không vượt HP tối đa; @return lượng HP thật sự hồi. */
   long heal(long amount) {
      if (dead || amount <= 0 || hp >= spec.hp()) {
         return 0;
      }
      long healed = Math.min(amount, spec.hp() - hp);
      hp += healed;
      return healed;
   }

   /**
    * Boss vừa bị hạ: rơi quà khoá xuống đất quanh xác, {@code killerId} được ưu tiên nhặt (người khác 15 s sau). Mỗi
    * {@link AltarRules.BossDrop} = {@code piles} món riêng, mỗi món số lượng random [min, max].
    */
   void dropLoot(Zone zone, int killerId) {
      ThreadLocalRandom r = ThreadLocalRandom.current();
      List<com.tlnt.server.player.ItemData> items = new ArrayList<>();
      for (AltarRules.BossDrop drop : spec.drops()) {
         for (int i = 0; i < drop.piles(); i++) {
            com.tlnt.server.player.ItemData item = com.tlnt.server.player.ItemData.of(drop.templateId(),
                  r.nextInt(drop.min(), drop.max() + 1));
            item.locked = true;
            items.add(item);
         }
      }
      java.util.Collections.shuffle(items, r);
      World.dropItems(zone, x, y + 50, killerId, items);
   }

   /**
    * Hiệu ứng từ kỹ năng người chơi (op 35 bản người chơi cho cả khu thấy): choáng / băng = đứng im + ngắt chiêu, trói =
    * không đi được, suy yếu = đánh yếu đi, sét = chỉ hình ảnh. Luồng tick.
    */
   void applyStatus(Zone zone, MobStatus kind, int durationMs, long now) {
      zone.broadcast(WorldPackets.playerStatus(kind, id, durationMs).toPacket(), null);
      long until = now + Math.max(0, durationMs);
      switch (kind) {
         case STUN, ICE -> {
            disabledUntil = Math.max(disabledUntil, until);
            busyUntil = Math.min(busyUntil, now); // chiêu đang ra bị ngắt
            nextSkillAt = Math.max(nextSkillAt, until);
         }
         case ROOT -> rootedUntil = Math.max(rootedUntil, until);
         case WEAKEN -> weakenedUntil = Math.max(weakenedUntil, until);
         default -> {
         }
      }
   }

   /** Đang suy yếu (nhận thêm 20% sát thương — chỉ số Sát thương suy yếu / kỹ năng). */
   boolean weakened(long now) {
      return now < weakenedUntil;
   }

   /** Đang choáng / đóng băng (hiệu ứng mới cùng loại không chồng). */
   boolean disabledAt(long now) {
      return now < disabledUntil;
   }

   private boolean disabled() {
      return System.currentTimeMillis() < disabledUntil;
   }

   /** Đòn của boss bị người có Né {@code evasion} (phần vạn) né: (Né − Chính xác boss) / 10000. */
   boolean missedBy(long evasion) {
      long chance = com.tlnt.server.combat.StatRules.dodgePermyriad(evasion, spec.accuracy()); // tối đa 75%
      return chance > 0 && ThreadLocalRandom.current().nextInt(10_000) < chance;
   }

   /**
    * Né đòn của người có {@code accuracy} (phần vạn): (Né boss − Chính xác) / 10000. Sát thương 0 -> client hiện "hụt"
    * trên boss ({@code WorldEntity.onHit}).
    */
   boolean dodges(long accuracy) {
      long chance = com.tlnt.server.combat.StatRules.dodgePermyriad(spec.evasion(), accuracy); // tối đa 75%
      return chance > 0 && ThreadLocalRandom.current().nextInt(10_000) < chance;
   }

   // ------------------------------------------------------------------ AI

   void tick(Zone zone, long now) {
      if (dead) {
         return;
      }
      float dt = lastTickAt == 0 ? 0 : Math.min(0.1f, (now - lastTickAt) / 1000f);
      lastTickAt = now;
      if (thief != null) {
         thiefTick(zone, now, dt);
         return;
      }
      if (rabbit != null) {
         rabbitTick(zone, now, dt);
         return;
      }
      WorldPlayer target = pickTarget(zone, now);
      if (target == null && smart && now >= busyUntil && now >= disabledUntil && now >= rootedUntil) {
         patrol(zone, now, dt); // không ai ở gần: chạy qua chạy lại quanh chỗ xuất hiện
         return;
      }
      if (target == null || now < busyUntil || now < disabledUntil) { // không ai / đang ra chiêu: đứng yên (đang trên không thì rơi xuống)
         mover.idle(zone.map().template(), now, dt);
         sendMove(zone, now);
         return;
      }
      if (!mover.onGround()) { // đang nhảy / rơi: chưa ra chiêu được
         chase(zone, target, now, dt);
         return;
      }
      boolean inMelee = withinRange(target, AltarRules.MOVE_SKILL1, MELEE_X_PX, MELEE_Y_PX);
      int skill;
      if (smart) {
         skill = mover.crossing() ? 0 : chooseSmart(zone, target, now, inMelee); // đang chạy vòng: tới nơi mới đánh
      } else {
         int[] moves = spec.rotation();
         skill = moves[rotation % moves.length];
         if (skill == ROOT && now - lastRootAt < rootCooldownMs()) {
            skill = AltarRules.MOVE_SKILL1; // trói còn hồi (trói 10 s, vòng chiêu ~7,5 s): đánh thường thay, không trói liên tục
         }
      }
      if (skill >= AltarRules.MOVE_SKILL1 && skill <= BOLT && !skillCooldownReady(skill, now)) {
         skill = AltarRules.MOVE_ATTACK;
      } else if (skill == SKILL11 && !skillCooldownReady(skill, now)) {
         skill = AltarRules.MOVE_ATTACK;
      }
      boolean inRange = skill == SHIELD || withinRange(target, skill, defaultRangeX(skill), defaultRangeY(skill));
      if (skill != 0 && inRange && now >= nextSkillAt) {
         cast(zone, skill, target, now);
         rotation++;
         if (smart) {
            afterSmartCast(skill, target, now);
         }
         return;
      }
      // Dùng tầm của chính chiêu đã chọn. Dùng tầm melee ở đây làm boss đứng im khi skill
      // đang chọn ngoài tầm (ví dụ skill3 tầm 300 nhưng skill32 tầm 350).
      if (shouldMoveTowardTarget(inRange, mover.crossing()) && dt > 0) {
         if (now < rootedUntil) { // bị trói: đứng im (đang trên không thì vẫn rơi)
            mover.idle(zone.map().template(), now, dt);
            sendMove(zone, now);
         } else {
            chase(zone, target, now, dt);
         }
      }
   }

   static boolean shouldMoveTowardTarget(boolean inSelectedSkillRange, boolean crossing) {
      return !inSelectedSkillRange || crossing;
   }

   /** Người đang đuổi còn hợp lệ thì giữ; không thì người sống gần nhất trong cả khu (boss tự tìm tới). */
   /**
    * Mục tiêu để đuổi / đánh: giữ người đang nhắm tới khi ra xong 1 chiêu ({@link #retargetAfterCast}); xong chiêu hoặc
    * người đó không còn hợp lệ thì chọn lại theo điểm ({@link #score}) — boss rải đòn cho nhiều người thay vì dồn 1 người.
    */
   private WorldPlayer pickTarget(Zone zone, long now) {
      WorldPlayer current = targetId == 0 ? null : zone.find(targetId);
      if (!retargetAfterCast && valid(current)) {
         return current;
      }
      retargetAfterCast = false;
      WorldPlayer best = null;
      float bestScore = -Float.MAX_VALUE;
      for (WorldPlayer p : zone.players()) {
         if (!valid(p)) {
            continue;
         }
         float s = score(p, now);
         if (s > bestScore) {
            best = p;
            bestScore = s;
         }
      }
      targetId = best == null ? 0 : best.id();
      return best;
   }

   /**
    * Điểm chọn mục tiêu (cao = bị đánh tiếp): + số giây chưa bị boss nhắm (tối đa {@link #SPREAD_CAP_MS}) — người vừa
    * bị đánh được "nghỉ"; + {@link #ATTACKER_BONUS} nếu vừa gây sát thương lên boss trong {@link #ATTACKER_MEMORY_MS};
    * − khoảng cách / {@link #DISTANCE_PX_PER_POINT} — không chạy loạn sang đầu kia map khi bên cạnh còn người.
    */
   /** Ghi lúc boss nhắm {@code playerId} (ra chiêu vào người đó). */
   void markTargeted(int playerId, long now) {
      lastTargetedAt.put(playerId, now);
   }

   float score(WorldPlayer p, long now) {
      long since = now - lastTargetedAt.getOrDefault(p.id(), 0L);
      float s = Math.min(SPREAD_CAP_MS, Math.max(0, since)) / 1000f;
      if (now - lastHurtBy.getOrDefault(p.id(), Long.MIN_VALUE / 2) <= ATTACKER_MEMORY_MS) {
         s += ATTACKER_BONUS;
      }
      return s - (Math.abs(p.x - x) + Math.abs(p.y - y)) / DISTANCE_PX_PER_POINT;
   }

   private static boolean valid(WorldPlayer p) {
      return p != null && !p.removed && !p.locked && !p.data().isDead() && !ZoneCombat.stealthed(p);
   }

   /** Boss có chiêu tia sét (41) trong bảng % của spec — dùng thay chiêu trói. */
   boolean hasBolt() {
      return special() == BOLT;
   }

   /** Chiêu đặc biệt thay trói ({@link #SPECIALS}) có trong bảng % của spec; 0 = không có (dùng trói). */
   int special() {
      if (spec.percents() == null) {
         return 0;
      }
      for (int s : SPECIALS) {
         if (spec.percents().containsKey(s)) {
            return s;
         }
      }
      return 0;
   }

   /** Hồi chiêu trói: trói xong + {@link #ROOT_REST_MS} mới trói lại. */
   long rootCooldownMs() {
      return BossSkillData.cooldownMillis(ROOT, rootDurationMs() + ROOT_REST_MS);
   }

   private int rootDurationMs() {
      return BossSkillData.statusDurationMs(ROOT, rootMs);
   }

   private int rootTargetCount() {
      return BossSkillData.targetCount(ROOT, rootTargets);
   }

   private static float defaultRangeX(int skill) {
      return switch (skill) {
         case SKILL2 -> SKILL2_FRONT_PX;
         case SKILL3 -> SKILL3_RADIUS_PX;
         case SKILL9, ROOT, BOLT, WEAKEN, SHIELD, STUN, FREEZE, SKILL11 -> RANGED_X_PX;
         default -> MELEE_X_PX;
      };
   }

   private static float defaultRangeY(int skill) {
      return switch (skill) {
         case SKILL2 -> SKILL2_Y_PX;
         case SKILL3 -> SKILL3_Y_PX;
         case SKILL9, ROOT, BOLT, WEAKEN, SHIELD, STUN, FREEZE, SKILL11 -> RANGED_Y_PX;
         default -> MELEE_Y_PX;
      };
   }

   private static float rangeX(int skill, float fallback) {
      return BossSkillData.rangeX(skill, fallback);
   }

   private static float rangeY(int skill, float fallback) {
      return BossSkillData.rangeY(skill, fallback);
   }

   private boolean withinRange(WorldPlayer player, int skill, float fallbackX, float fallbackY) {
      return Math.abs(player.x - x) <= rangeX(skill, fallbackX)
            && Math.abs(player.y - y) <= rangeY(skill, fallbackY);
   }

   private boolean skillCooldownReady(int skill, long now) {
      return now >= skillReadyAt.getOrDefault(skill, 0L);
   }

   private void recordSkillCooldown(int skill, long now) {
      if (BossSkillData.hasCooldown(skill)) {
         skillReadyAt.put(skill, now + BossSkillData.cooldownMillis(skill, 0));
      }
   }

   /**
    * Chọn chiêu theo tình huống (boss thế giới), mục tiêu {@code target}:
    * <ol>
    *   <li>Trói hết hồi + (mục tiêu đứng ngoài tầm cận chiến — chạy / đánh xa — hoặc ≥ 3 người trong tầm bắn) -> trói.</li>
    *   <li>Dậm đất hết hồi + (≥ 2 người quanh boss, hoặc {@link #SOLO_SKILL3_PERCENT}%) -> dậm đất hất tung.</li>
    *   <li>Chém hết hồi + (≥ 2 người phía trước, hoặc {@link #SOLO_SKILL2_PERCENT}%) -> chém phía trước.</li>
    *   <li>Không thì đánh thường (skill1).</li>
    * </ol>
    */
   int chooseSmart(Zone zone, WorldPlayer target, long now, boolean inMelee) {
      int dir = target.x < x ? -1 : 1;
      int around = 0;
      int front = 0;
      int ranged = 0;
      int special = special();
      for (WorldPlayer p : zone.players()) {
         if (!valid(p)) {
            continue;
         }
         float dx = p.x - x;
         float dy = Math.abs(p.y - y);
         if (withinRange(p, SKILL3, SKILL3_RADIUS_PX, SKILL3_Y_PX)) {
            around++;
         }
         if ((Math.signum(dx) == dir || Math.abs(dx) < 40f)
               && Math.abs(dx) <= rangeX(SKILL2, SKILL2_FRONT_PX)
               && dy <= rangeY(SKILL2, SKILL2_Y_PX)) {
            front++;
         }
         int rangeSkill = special == 0 ? ROOT : special;
         if (withinRange(p, rangeSkill, defaultRangeX(rangeSkill), defaultRangeY(rangeSkill))) {
            ranged++;
         }
      }
      ThreadLocalRandom r = ThreadLocalRandom.current();
      if (special == BOLT) { // Tinh linh Sấm sét: tia sét thay chiêu trói — bắn người đứng xa, thỉnh thoảng cả người đứng sát
         if (now >= boltReadyAt && (!inMelee || r.nextInt(100) < MELEE_BOLT_PERCENT)) {
            return BOLT;
         }
      } else if (special == SHIELD) { // Tinh linh Biển cả: dựng khiên nước khi hết hồi
         if (now >= specialReadyAt) {
            return SHIELD;
         }
      } else if (special == SKILL11) { // Cổ thần Dạ xoa: chiêu tối thượng diện rộng — hết hồi là ra (kể cả đứng sát)
         if (now >= specialReadyAt) {
            return SKILL11;
         }
      } else if (special != 0) { // Đầm lầy / Phong thạch / Băng giá: như trói — người đứng xa hoặc ≥ 2 người trong tầm
         if (now >= specialReadyAt && (!inMelee || ranged >= 2)) {
            return special;
         }
      } else if (now - lastRootAt >= rootCooldownMs() && (!inMelee || ranged >= 3)) {
         return ROOT;
      }
      if (now >= skill3ReadyAt && (around >= 2 || r.nextInt(100) < SOLO_SKILL3_PERCENT)) {
         return SKILL3;
      }
      if (now >= skill2ReadyAt && (front >= 2 || r.nextInt(100) < SOLO_SKILL2_PERCENT)) {
         return SKILL2;
      }
      return AltarRules.MOVE_SKILL1;
   }

   /** Sau 1 chiêu (boss thế giới): tính hồi chiêu, có lúc chạy vòng sang phía bên kia mục tiêu. */
   private void afterSmartCast(int skill, WorldPlayer target, long now) {
      if (skill == SKILL2) {
         skill2ReadyAt = now + BossSkillData.cooldownMillis(SKILL2, SMART_SKILL2_CD_MS);
      } else if (skill == SKILL3) {
         skill3ReadyAt = now + BossSkillData.cooldownMillis(SKILL3, SMART_SKILL3_CD_MS);
      } else if (skill == BOLT) {
         boltReadyAt = now + BossSkillData.cooldownMillis(BOLT, BOLT_COOLDOWN_MS);
      } else if (skill == SHIELD) {
         specialReadyAt = now + BossSkillData.cooldownMillis(SHIELD, SHIELD_COOLDOWN_MS);
      } else if (skill == WEAKEN || skill == STUN || skill == FREEZE || skill == SKILL11) {
         specialReadyAt = now + BossSkillData.cooldownMillis(skill, SPECIAL_COOLDOWN_MS);
      }
      if (skill != ROOT && ThreadLocalRandom.current().nextInt(100) < CROSS_PERCENT) {
         mover.crossTo(x < target.x ? 1 : -1);
      }
   }

   /**
    * Không ai ở gần (boss thế giới): xa chỗ xuất hiện quá {@link #HOME_LEAP_PX} thì nhảy vọt về; không thì chạy qua chạy
    * lại tới 1 điểm ngẫu nhiên trong ±{@link #PATROL_PX} quanh chỗ xuất hiện, nghỉ 1 lúc rồi đi tiếp.
    */
   private void patrol(Zone zone, long now, float dt) {
      MapTemplate map = zone.map().template();
      if (Float.isNaN(patrolX)) {
         patrolX = mover.x;
         patrolY = mover.y;
      }
      boolean arrived = mover.arrived(patrolX, patrolY, 10f) || patrolGiveUpAt != 0 && now >= patrolGiveUpAt;
      if (arrived) {
         if (patrolPauseUntil == 0) {
            patrolPauseUntil = now + (rabbit != null
                  ? ThreadLocalRandom.current().nextLong(Rabbit.WANDER_PAUSE_MIN_MS, Rabbit.WANDER_PAUSE_MAX_MS + 1)
                  : ThreadLocalRandom.current().nextLong(PATROL_PAUSE_MIN_MS, PATROL_PAUSE_MAX_MS + 1));
         } else if (now >= patrolPauseUntil) {
            patrolPauseUntil = 0;
            pickPatrolSpot(map, now);
         }
         mover.idle(map, now, dt);
      } else {
         float speed = rabbit != null ? Rabbit.RUN_SPEED
               : stats.moveSpeed * (roams() ? FOX_ROAM_SPEED : PATROL_SPEED);
         mover.walkTo(map, patrolX, patrolY, speed, now, dt);
      }
      sendMove(zone, now);
   }

   /**
    * Lượt đi tuần kế: Cáo -> chỗ ngẫu nhiên KHẮP map (đi bộ tới được); boss thế giới -> ±{@link #PATROL_PX} quanh chỗ
    * xuất hiện (đi xa quá {@link #HOME_LEAP_PX} thì chạy bộ về chỗ xuất hiện — không nhảy vọt bay qua).
    */
   private void pickPatrolSpot(MapTemplate map, long now) {
      ThreadLocalRandom r = ThreadLocalRandom.current();
      float[] spot = null;
      if (rabbit != null) { // dạo như thú cưng: đi 1 đoạn ngắn sang trái / phải rồi nghỉ
         float len = (float) r.nextDouble(Rabbit.WANDER_MIN_PX, Rabbit.WANDER_MAX_PX);
         float px = map.clampX(mover.x + (r.nextBoolean() ? len : -len));
         spot = new float[] {px, BossMover.groundUnder(map, px, mover.y)};
      } else if (roams()) {
         spot = mover.randomSpot(map, r);
      } else if (Math.abs(mover.x - homeX) + Math.abs(mover.y - homeY) > HOME_LEAP_PX) {
         spot = new float[] {homeX, homeY};
      } else {
         float px = map.clampX(homeX + (float) r.nextDouble(-PATROL_PX, PATROL_PX));
         spot = new float[] {px, BossMover.groundUnder(map, px, homeY)};
      }
      if (spot == null) {
         spot = new float[] {mover.x, mover.y};
      }
      patrolX = spot[0];
      patrolY = spot[1];
      patrolGiveUpAt = now + (roams() ? FOX_ROAM_GIVE_UP_MS : PATROL_GIVE_UP_MS);
   }

   /** Cáo / Ngọc thố đi dạo KHẮP map (boss thế giới chỉ quanh chỗ xuất hiện). */
   private boolean roams() {
      return thief != null || rabbit != null;
   }

   // ------------------------------------------------------------------ Ngọc thố ({@link Rabbit})

   /**
    * AI Ngọc thố: không đánh ai. Có củ cà rốt dưới đất -> chạy tới (theo thứ tự ném), tới nơi đứng ăn
    * {@link Rabbit#EAT_MS} (chat "Măm măm") rồi rơi quà riêng cho người ném + điểm sự kiện; đủ {@link Rabbit#MAX_EATEN}
    * củ thì {@link Rabbit#LEAVE_DELAY_MS} sau chạy sang map khác. Không có củ nào -> chạy qua chạy lại khắp map.
    */
   private void rabbitTick(Zone zone, long now, float dt) {
      Rabbit r = rabbit;
      MapTemplate map = zone.map().template();
      if (r.leaving) {
         mover.idle(map, now, dt);
         sendMove(zone, now);
         return;
      }
      if (r.eating != null) { // đang ăn: đứng yên tới khi ăn xong
         mover.idle(map, now, dt);
         sendMove(zone, now);
         if (now >= r.eatUntil) {
            finishEating(zone, r, now);
         }
         return;
      }
      Rabbit.Carrot c = r.next(now, carrotId -> zone.broadcast(WorldPackets.mapPropRemove(carrotId), null));
      if (c == null) {
         patrol(zone, now, dt);
         return;
      }
      if (Math.abs(c.x() - x) <= Rabbit.EAT_REACH_PX && Math.abs(c.y() - y) <= Rabbit.EAT_REACH_PX * 3) {
         r.carrots.pollFirst();
         r.eating = c;
         r.eatUntil = now + Rabbit.EAT_MS;
         zone.broadcast(WorldPackets.mapPropRemove(c.id()), null); // thỏ ăn: gỡ củ
         zone.broadcast(WorldPackets.chat(id, Rabbit.EAT_CHAT), null);
         mover.idle(map, now, dt);
         sendMove(zone, now);
         return;
      }
      patrolX = Float.NaN; // ăn xong đi dạo tiếp từ chỗ đứng
      patrolPauseUntil = 0;
      mover.walkTo(map, c.x(), c.y(), Rabbit.RUN_SPEED, now, dt);
      sendMove(zone, now);
   }

   /** Ăn xong 1 củ: quà riêng + điểm cho người ném (nếu còn trong khu), đủ củ thì hẹn rời map. */
   private void finishEating(Zone zone, Rabbit r, long now) {
      Rabbit.Carrot c = r.eating;
      r.eating = null;
      r.eaten++;
      WorldPlayer owner = null;
      for (WorldPlayer p : zone.players()) {
         if (p.id() == c.ownerId()) {
            owner = p;
         }
      }
      if (owner != null) {
         for (ItemData item : com.tlnt.server.event.MidAutumnService.rabbitRewards(ThreadLocalRandom.current())) {
            World.dropPersonal(zone, x, y, owner, item);
         }
         com.tlnt.server.event.MidAutumnService.rabbitFed(owner.session(), owner.data());
      }
      if (r.eaten >= Rabbit.MAX_EATEN) {
         r.leaving = true;
         clearCarrots(zone); // củ chưa ăn: mất
         zone.schedule(Rabbit.LEAVE_DELAY_MS, () -> {
            if (!gone) {
               World.rabbitLeave(zone, this);
            }
         });
      }
   }

   /** Gỡ mọi củ cà rốt còn nằm đất của thỏ này khỏi client (thỏ bỏ đi / bị gỡ). Luồng tick khu. */
   void clearCarrots(Zone zone) {
      if (rabbit == null) {
         return;
      }
      for (Rabbit.Carrot c : rabbit.carrots) {
         zone.broadcast(WorldPackets.mapPropRemove(c.id()), null);
      }
      rabbit.carrots.clear();
   }

   /** Cáo đi dạo khắp map: tốc = tốc chạy × hệ số; bỏ lượt nếu bấy lâu chưa tới. */
   static final float FOX_ROAM_SPEED = 0.6f;
   static final long FOX_ROAM_GIVE_UP_MS = 25_000;

   // ------------------------------------------------------------------ Tiểu Tinh linh Cáo ({@link FoxThief})

   /**
    * AI cáo trộm: không đánh ai. Choáng / băng = đứng im. Tới giờ đổi map ({@link FoxThief#relocateDue}) -> rời khu
    * ({@link World#foxLeave}). Bị phát hiện (đã bị đánh) -> chạy bộ tới chỗ xa người gần nhất (đi bộ tới được,
    * ưu tiên phía bên kia). Không thì tìm người đứng quanh 1 chỗ ({@link FoxThief#track}) còn vàng khoá, đuổi tới,
    * đứng cạnh mỗi {@link FoxThief#STEAL_INTERVAL_MS} trộm 1%; không ai thì đi dạo KHẮP map.
    */
   private void thiefTick(Zone zone, long now, float dt) {
      FoxThief t = thief;
      MapTemplate map = zone.map().template();
      java.util.Set<Integer> present = new java.util.HashSet<>();
      for (WorldPlayer p : zone.players()) {
         present.add(p.id());
         t.track(p.id(), p.x, p.y, now);
      }
      t.forgetExcept(present);
      if (t.relocateDue(now)) {
         World.foxLeave(zone, this);
         return;
      }
      if (now < disabledUntil) { // choáng / đóng băng
         mover.idle(map, now, dt);
         sendMove(zone, now);
         return;
      }
      if (t.detectedAt > 0) {
         flee(zone, map, now, dt);
         return;
      }
      if (t.full()) { // đủ 1 triệu: thôi trộm, chỉ đi tuần (bị đánh thì trốn + 2 phút đổi map)
         patrol(zone, now, dt);
         return;
      }
      WorldPlayer target = t.targetId == 0 ? null : zone.find(t.targetId);
      if (!stealable(target)) {
         int before = t.targetId;
         target = pickStill(zone, now);
         t.targetId = target == null ? 0 : target.id();
         t.nextStealAt = now + FoxThief.STEAL_INTERVAL_MS; // tới nơi đứng cạnh 10 s mới trộm lần đầu
         if (target != null && t.targetId != before) {
            zone.broadcast(WorldPackets.chat(id, FoxThief.TARGET_CHAT), null); // bắt đầu tới tìm người này
         }
      }
      if (target == null) {
         patrol(zone, now, dt);
         return;
      }
      boolean near = Math.abs(target.x - x) <= FoxThief.STEAL_X_PX && Math.abs(target.y - y) <= FoxThief.STEAL_Y_PX;
      if (!near) {
         chase(zone, target, now, dt); // mục tiêu đi chỗ khác: đuổi theo trộm tiếp
         return;
      }
      mover.idle(map, now, dt);
      sendMove(zone, now);
      if (now >= t.nextStealAt) {
         t.nextStealAt = now + FoxThief.STEAL_INTERVAL_MS;
         steal(zone, target);
      }
   }

   /** Còn trộm được của người này: còn sống, không ngắm cảnh, còn vàng khoá, chưa bị trộm đủ 100k. */
   private boolean stealable(WorldPlayer p) {
      return valid(p) && p.data().lockedGold > 0 && !thief.done(p.id());
   }

   /** Người đứng quanh 1 chỗ đủ lâu, trộm được, gần cáo nhất. */
   private WorldPlayer pickStill(Zone zone, long now) {
      WorldPlayer best = null;
      float bestDist = Float.MAX_VALUE;
      for (WorldPlayer p : zone.players()) {
         if (!stealable(p) || !thief.track(p.id(), p.x, p.y, now)) {
            continue;
         }
         float d = Math.abs(p.x - x) + Math.abs(p.y - y);
         if (d < bestDist) {
            best = p;
            bestDist = d;
         }
      }
      return best;
   }

   /**
    * Trộm 1% vàng khoá của {@code target} (dưới khoá nhân vật — cùng khoá với thư / shop / lưu), báo người đó + gửi lại
    * tiền. Đủ 100k của người này -> đổi mục tiêu; vừa đủ 1 triệu -> báo thế giới 1 lần + bỏ sang map khác.
    */
   private void steal(Zone zone, WorldPlayer target) {
      FoxThief t = thief;
      com.tlnt.server.player.PlayerData d = target.data();
      long amount;
      synchronized (d) {
         amount = t.stealAmount(d.lockedGold, t.stolenFrom.getOrDefault(target.id(), 0L));
         d.lockedGold -= amount;
      }
      if (amount <= 0) {
         t.targetId = 0;
         return;
      }
      boolean reachedMax = t.record(target.id(), amount);
      zone.broadcast(WorldPackets.chat(id, FoxThief.STEAL_CHAT), null);
      target.send(com.tlnt.server.item.ItemPackets.currency(d));
      target.send(com.tlnt.server.net.Packets.notice(spec.name() + " vừa trộm " + String.format(java.util.Locale.GERMANY,
            "%,d", amount) + " vàng khoá của bạn!"));
      com.tlnt.server.util.Log.debug("Cao #%d trom %d vang khoa cua '%s' (tong %d)", t.slot, amount, d.name, t.total);
      if (t.done(target.id())) {
         t.targetId = 0;
      }
      if (reachedMax && !t.announced) {
         t.announced = true;
         t.leaveNow = true; // không trộm nữa, bỏ sang map khác
         zone.map().world().broadcastTicker(FoxThief.ANNOUNCE); // CHỈ chữ chạy thế giới, không báo map / khu
      }
   }

   /** Bị phát hiện: chạy bộ (nhảy / rơi theo địa hình) tới chỗ xa người gần nhất, chọn lại chỗ trốn mỗi vài giây. */
   private void flee(Zone zone, MapTemplate map, long now, float dt) {
      if (now < restUntil) { // đang đứng thở: đứng yên, đánh được
         mover.idle(map, now, dt);
         sendMove(zone, now);
         return;
      }
      WorldPlayer nearest = null;
      float best = Float.MAX_VALUE;
      for (WorldPlayer p : zone.players()) {
         if (!valid(p)) {
            continue;
         }
         float d = Math.abs(p.x - x) + Math.abs(p.y - y);
         if (d < best) {
            best = d;
            nearest = p;
         }
      }
      if (nearest == null || best > FoxThief.FLEE_TRIGGER_PX) {
         runSince = 0;
         patrol(zone, now, dt);
         return;
      }
      if (runSince == 0) {
         runSince = now;
      } else if (now - runSince >= FoxThief.RUN_MS) { // chạy mệt: đứng thở
         runSince = 0;
         restUntil = now + FoxThief.REST_MS;
         zone.broadcast(WorldPackets.chat(id, FoxThief.TIRED_CHAT), null);
         mover.idle(map, now, dt);
         sendMove(zone, now);
         return;
      }
      if (Float.isNaN(escapeX) || now >= escapeRepickAt || mover.arrived(escapeX, escapeY, 30f)) {
         float[] spot = mover.escapeSpot(map, nearest.x, nearest.y, ThreadLocalRandom.current());
         escapeX = spot == null ? x : spot[0];
         escapeY = spot == null ? y : spot[1];
         escapeRepickAt = now + ESCAPE_REPICK_MS;
      }
      mover.walkTo(map, escapeX, escapeY, stats.moveSpeed, now, dt); // chạy bộ / nhảy theo địa hình, không bay
      sendMove(zone, now);
   }

   /** Đang trốn: chọn lại chỗ chạy tới mỗi bấy lâu (người đuổi đổi chỗ). */
   static final long ESCAPE_REPICK_MS = 2_500;
   /** Cáo đang trốn: bắt đầu chạy liền mạch lúc này (0 = không chạy) + đứng thở tới lúc này. */
   private long runSince;
   private long restUntil;

   private void chase(Zone zone, WorldPlayer target, long now, float dt) {
      mover.update(target, zone.map().template(), stats.moveSpeed, now, dt);
      sendMove(zone, now);
   }

   /** Phát vị trí mới (op 5) tối đa mỗi nhịp phát di chuyển của khu. */
   private void sendMove(Zone zone, long now) {
      x = mover.x;
      y = mover.y;
      if (mover.dirty && now >= nextSendAt) {
         mover.dirty = false;
         nextSendAt = now + Zone.moveBroadcastMs;
         zone.broadcast(WorldPackets.entityMove(id, x, y), null);
      }
   }

   // ------------------------------------------------------------------ chiêu

   private void cast(Zone zone, int skill, WorldPlayer target, long now) {
      markTargeted(target.id(), now); // người này vừa bị nhắm: chiêu sau ưu tiên người khác
      retargetAfterCast = true;
      recordSkillCooldown(skill, now);
      if (mover.dirty) { // vị trí cuối trước khi đứng ra chiêu
         mover.dirty = false;
         zone.broadcast(WorldPackets.entityMove(id, x, y), null);
      }
      switch (skill) {
         case AltarRules.MOVE_ATTACK, AltarRules.MOVE_SKILL1, BOLT -> { // 1 mục tiêu (đánh thường / skill1 / tia sét)
            int percent = BossSkillData.damagePercent(skill, spec.percent(skill));
            long left = ZoneCombat.bossHitsPlayer(zone, this, target, percent);
            zone.broadcast(WorldPackets.singleTargetHit(id, skill, WorldPackets.TARGET_PLAYER, target.id(),
                  Math.max(0, left), lastDamage, lastDamage > 0 && lastCrit).toPacket(), null);
            ZoneCombat.afterBossHit(zone, this, target, left);
            if (skill == AltarRules.MOVE_ATTACK) {
               setBusy(now, ATTACK_CAST_MS, ATTACK_GAP_MS);
            } else if (skill == BOLT) {
               setBusy(now, spec.castMs(BOLT, BOLT_CAST_MS));
            } else {
               setBusy(now, spec.castMs(skill, SKILL1_CAST_MS));
            }
         }
         case AltarRules.MOVE_SKILL2 -> {
            int dir = target.x < x ? -1 : 1;
            zone.broadcast(WorldPackets.bossFacing(id, SKILL2, dir).toPacket(), null);
            zone.schedule(spec.hitMs(SKILL2, SKILL2_HIT_MS),
                  () -> area(zone, BossSkillData.damagePercent(SKILL2, spec.percent(SKILL2)), dir,
                  rangeX(SKILL2, SKILL2_FRONT_PX),
                  rangeY(SKILL2, SKILL2_Y_PX)));
            setBusy(now, spec.castMs(SKILL2, SKILL2_CAST_MS));
         }
         case AltarRules.MOVE_SKILL3 -> {
            zone.broadcast(WorldPackets.bossSkill(id, SKILL3).toPacket(), null);
            zone.schedule(spec.hitMs(SKILL3, SKILL3_HIT_MS), () -> {
               for (WorldPlayer p : area(zone, BossSkillData.damagePercent(SKILL3, spec.percent(SKILL3)), 0,
                     rangeX(SKILL3, SKILL3_RADIUS_PX),
                     rangeY(SKILL3, SKILL3_Y_PX))) {
                  if (!p.data().isDead()) { // nảy lên tại chỗ: client tự rơi xuống theo trọng lực
                     ZoneCombat.knock(p, 0, KNOCK_UP_MPS, KNOCK_MS);
                  }
               }
            });
            setBusy(now, spec.castMs(SKILL3, SKILL3_CAST_MS));
         }
         case WEAKEN, STUN, FREEZE -> { // 36 / 38 / 39: giới hạn và tác dụng theo skill_effect
            zone.broadcast(WorldPackets.bossSkill(id, skill).toPacket(), null);
            zone.schedule(spec.hitMs(skill, 600), () -> specialArea(zone, skill));
            setBusy(now, spec.castMs(skill, SPECIAL_CAST_MS));
         }
         case SKILL11 -> { // 43: hút máu tối đa 5 mục tiêu ngẫu nhiên rồi hồi boss
            zone.broadcast(WorldPackets.bossSkill(id, SKILL11).toPacket(), null); // client case 43: không payload
            zone.schedule(spec.hitMs(SKILL11, SKILL11_HIT_MS), () -> drainArea(zone));
            setBusy(now, spec.castMs(SKILL11, SPECIAL_CAST_MS));
         }
         case SHIELD -> { // 37 stick/skill6: khiên nước — giảm sát thương nhận vào, hết giờ vỡ (bool false)
            zone.broadcast(WorldPackets.bossToggle(id, SHIELD, true).toPacket(), null);
            long shieldMs = BossSkillData.durationMs(SHIELD, (int) SHIELD_MS);
            long until = now + shieldMs;
            shieldUntil = until;
            zone.schedule(shieldMs, () -> {
               if (!dead && shieldUntil == until) {
                  zone.broadcast(WorldPackets.bossToggle(id, SHIELD, false).toPacket(), null);
               }
            });
            setBusy(now, spec.castMs(SHIELD, SPECIAL_CAST_MS));
         }
         case ROOT -> { // 35 stick/skill4: rễ cây trói tối đa 3 người ngẫu nhiên quanh boss
            lastRootAt = now;
            zone.broadcast(WorldPackets.bossSkill(id, ROOT).toPacket(), null);
            zone.schedule(spec.hitMs(ROOT, ROOT_HIT_MS), () -> roots(zone));
            setBusy(now, spec.castMs(ROOT, ROOT_CAST_MS));
         }
         default -> { // 40: lông vũ
            zone.broadcast(WorldPackets.bossPhase(id, SKILL9, 0).toPacket(), null);
            int featherCount = BossSkillData.hits(SKILL9, AltarRules.BOSS_FEATHERS);
            long t = FEATHER_FIRST_MS;
            for (int i = 0; i < featherCount; i++, t += FEATHER_INTERVAL_MS) {
               zone.schedule(t, () -> feather(zone));
            }
            zone.schedule(t, () -> {
               if (!dead) {
                  zone.broadcast(WorldPackets.bossPhase(id, SKILL9, 1).toPacket(), null);
               }
            });
            setBusy(now, t + 400);
         }
      }
   }

   private void setBusy(long now, long castMs) {
      setBusy(now, castMs, spec.gap(SKILL_GAP_MS));
   }

   private void setBusy(long now, long castMs, long gapMs) {
      busyUntil = now + castMs;
      nextSkillAt = busyUntil + gapMs;
   }

   /** Sát thương của đòn vừa tính ({@link ZoneCombat#bossHitsPlayer}) — để ghi vào gói chiêu. */
   long lastDamage;
   /** Đòn vừa tính là chí mạng (client hiện chữ chí mạng ở gói 1 mục tiêu / lông vũ). */
   boolean lastCrit;

   /**
    * Đòn diện rộng: {@code dir} = -1 / 1 chỉ phía trước (skill2), 0 = quanh boss (skill3). Mỗi người trúng 1 gói op 2
    * sub 3 (mất máu không animation người đánh — animation chiêu đã chơi).
    */
   private List<WorldPlayer> area(Zone zone, int percent, int dir, float rangeX, float rangeY) {
      if (dead || disabled()) { // bị choáng / băng giữa chiêu: đòn bị ngắt
         return List.of();
      }
      List<WorldPlayer> hit = new ArrayList<>();
      for (WorldPlayer p : zone.players()) {
         float dx = p.x - x;
         boolean inFront = dir == 0 || Math.signum(dx) == dir || Math.abs(dx) < 40f;
         if (valid(p) && inFront && Math.abs(dx) <= rangeX && Math.abs(p.y - y) <= rangeY) {
            hit.add(p);
         }
      }
      List<WorldPlayer> landed = new ArrayList<>();
      for (WorldPlayer p : hit) {
         long left = ZoneCombat.bossHitsPlayer(zone, this, p, percent);
         zone.broadcast(WorldPackets.playerDamaged(p.id(), lastDamage, Math.max(0, left)), null);
         ZoneCombat.afterBossHit(zone, this, p, left);
         if (lastDamage > 0) {
            landed.add(p); // né thì không bị hất
         }
      }
      return landed;
   }

   /**
    * Chiêu đặc biệt 36 / 38 / 39: lấy giới hạn mục tiêu, sát thương và thời lượng từ skill_effect rồi áp dụng trạng thái.
    */
   private void specialArea(Zone zone, int skill) {
      if (dead || disabled()) {
         return;
      }
      List<WorldPlayer> inRange = new ArrayList<>();
      for (WorldPlayer p : zone.players()) {
         if (valid(p) && withinRange(p, skill, defaultRangeX(skill), defaultRangeY(skill))) {
            inRange.add(p);
         }
      }
      java.util.Collections.shuffle(inRange, ThreadLocalRandom.current());
      int percent = BossSkillData.damagePercent(skill, spec.percent(skill));
      MobStatus status = skill == WEAKEN ? MobStatus.WEAKEN : skill == STUN ? MobStatus.STUN : MobStatus.ICE;
      int defaultMs = skill == WEAKEN ? WEAKEN_STATUS_MS : skill == STUN ? STUN_STATUS_MS : FREEZE_STATUS_MS;
      int ms = BossSkillData.statusDurationMs(skill, defaultMs);
      int targetCount = BossSkillData.targetCount(skill, SPECIAL_TARGETS);
      int end = targetCount == 0 ? inRange.size() : Math.min(targetCount, inRange.size());
      for (WorldPlayer p : inRange.subList(0, end)) {
         if (percent > 0) {
            long left = ZoneCombat.bossHitsPlayer(zone, this, p, percent);
            zone.broadcast(WorldPackets.playerDamaged(p.id(), lastDamage, Math.max(0, left)), null);
            ZoneCombat.afterBossHit(zone, this, p, left);
            if (lastDamage <= 0) {
               continue; // né
            }
         }
         if (!p.data().isDead()) {
            if (skill == STUN) {
               zone.broadcast(WorldPackets.playerStatus(MobStatus.EARTH_SPIKE, p.id(), 0).toPacket(), null);
            }
            ZoneCombat.bossStatusPlayer(zone, p, status, ms,
                  skill == WEAKEN ? BossSkillData.statusPower(skill, 50) : 0);
         }
      }
   }

   /**
    * Trói: số mục tiêu, thời gian, sát thương hút và tỉ lệ hồi máu đọc từ {@code skill_effect}; nếu chưa có migration thì
    * dùng fallback khớp mô tả boss.
    */
   private void roots(Zone zone) {
      if (dead || disabled()) {
         return;
      }
      List<WorldPlayer> inRange = new ArrayList<>();
      for (WorldPlayer p : zone.players()) {
         if (valid(p) && withinRange(p, ROOT, RANGED_X_PX, RANGED_Y_PX)) {
            inRange.add(p);
         }
      }
      java.util.Collections.shuffle(inRange, ThreadLocalRandom.current());
      int percent = BossSkillData.damagePercent(ROOT, spec.percent(ROOT));
      int count = rootTargetCount();
      int duration = rootDurationMs();
      int burnPermille = BossSkillData.statusPower(ROOT, rootBurnPermille > 0 ? rootBurnPermille : 10);
      int lifestealPercent = BossSkillData.lifestealPercent(ROOT, 100);
      int end = count == 0 ? inRange.size() : Math.min(count, inRange.size());
      for (WorldPlayer p : inRange.subList(0, end)) {
         if (percent > 0) {
            long left = ZoneCombat.bossHitsPlayer(zone, this, p, percent);
            zone.broadcast(WorldPackets.playerDamaged(p.id(), lastDamage, Math.max(0, left)), null);
            ZoneCombat.afterBossHit(zone, this, p, left);
         }
         if (!p.data().isDead() && ZoneCombat.rootPlayer(zone, p, duration) && burnPermille > 0) {
            burn(zone, p, duration, burnPermille, lifestealPercent);
         }
      }
   }

   /**
    * Rễ cây đốt người bị trói: mỗi {@link #ROOT_BURN_INTERVAL_MS} mất {@link #rootBurnPermille} phần nghìn HP tối đa
    * (game gốc: Tinh Linh Rừng xanh 39 / giây với HP 3936 = 1%) tới hết thời gian trói; boss chết / người rời khu / chết
    * thì thôi.
    */
   private void burn(Zone zone, WorldPlayer p, int durationMs, int permille, int lifestealPercent) {
      for (long t = ROOT_BURN_INTERVAL_MS; t <= durationMs; t += ROOT_BURN_INTERVAL_MS) {
         zone.schedule(t, () -> {
            if (!dead && p.zone == zone && valid(p)) {
               long drained = ZoneCombat.bossBurnsPlayer(zone, this, p, permille);
               healFromDrain(zone, drained, lifestealPercent);
            }
         });
      }
   }

   /** Skill 43 hút % HP hiện tại của tối đa 5 người được chọn ngẫu nhiên. */
   private void drainArea(Zone zone) {
      if (dead || disabled()) {
         return;
      }
      List<WorldPlayer> inRange = new ArrayList<>();
      for (WorldPlayer p : zone.players()) {
         if (valid(p) && withinRange(p, SKILL11, defaultRangeX(SKILL11), defaultRangeY(SKILL11))) {
            inRange.add(p);
         }
      }
      java.util.Collections.shuffle(inRange, ThreadLocalRandom.current());
      int count = BossSkillData.targetCount(SKILL11, 5);
      int end = count == 0 ? inRange.size() : Math.min(count, inRange.size());
      int drainPercent = BossSkillData.aoePercent(SKILL11, 20);
      int healPercent = BossSkillData.lifestealPercent(SKILL11, 100);
      for (WorldPlayer p : inRange.subList(0, end)) {
         long drained = ZoneCombat.bossDrainsPlayer(zone, this, p, drainPercent);
         healFromDrain(zone, drained, healPercent);
      }
   }

   private void healFromDrain(Zone zone, long drained, int healPercent) {
      if (drained <= 0 || healPercent <= 0) {
         return;
      }
      long healed = heal(drained * Math.min(100, healPercent) / 100);
      if (healed > 0) {
         zone.broadcast(WorldPackets.playerHp(id, hp), null);
      }
   }

   /** 1 lông vũ: trúng 1 người ngẫu nhiên trong tầm bắn. */
   private void feather(Zone zone) {
      if (dead || disabled()) {
         return;
      }
      List<WorldPlayer> inRange = new ArrayList<>();
      for (WorldPlayer p : zone.players()) {
         if (valid(p) && withinRange(p, SKILL9, RANGED_X_PX, RANGED_Y_PX)) {
            inRange.add(p);
         }
      }
      if (inRange.isEmpty()) {
         return;
      }
      WorldPlayer p = inRange.get(ThreadLocalRandom.current().nextInt(inRange.size()));
      long left = ZoneCombat.bossHitsPlayer(zone, this, p,
            BossSkillData.damagePercent(SKILL9, spec.percent(SKILL9)));
      zone.broadcast(WorldPackets.bossFeather(id, SKILL9, p.id(), Math.max(0, left), lastDamage,
            lastDamage > 0 && lastCrit).toPacket(), null);
      ZoneCombat.afterBossHit(zone, this, p, left);
   }

   /** Sát thương gốc 1 đòn (chưa giáp / dao động / chí mạng): đòn thường của boss × % chiêu. */
   long baseDamage(int percent) {
      return spec.hitDamage() * percent / 100;
   }

   /** Sát thương 1 đòn boss lên người có {@code armor}: {@link #baseDamage}, giáp, ±5%, chí mạng. */
   long rollDamage(int percent, long armor) {
      return rollDamage(percent, armor, 0);
   }

   /** Như trên; {@code critResistPercent} = Kháng sát thương chí mạng (%) của người bị đánh, trừ vào phần cộng chí mạng. */
   long rollDamage(int percent, long armor, long critResistPercent) {
      ThreadLocalRandom r = ThreadLocalRandom.current();
      int spread = r.nextInt(-MobRules.MOB_DAMAGE_SPREAD_PERCENT, MobRules.MOB_DAMAGE_SPREAD_PERCENT + 1);
      long base = baseDamage(percent);
      if (System.currentTimeMillis() < weakenedUntil) {
         base = base * (100 - WEAKEN_PERCENT) / 100;
      }
      if (pierceArmor) {
         armor = Math.max(0, armor - spec.armorPen()); // Xuyên giáp trừ thẳng vào Giáp
      }
      long damage = MobRules.tableHitsPlayer(base, armor, spread);
      lastCrit = r.nextInt(10_000) < spec.critChance();
      if (lastCrit) {
         long bonus = Math.max(0, spec.critDamagePercent() - Math.max(0, critResistPercent) * 10); // phần nghìn: 180 = +18%
         damage += damage * bonus / 1_000;
      }
      return Math.max(1, damage);
   }
}
