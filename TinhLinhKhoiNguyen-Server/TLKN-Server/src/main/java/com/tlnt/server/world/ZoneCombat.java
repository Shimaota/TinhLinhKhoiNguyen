package com.tlnt.server.world;

import com.tlnt.server.altar.AltarRules;
import com.tlnt.server.combat.MobRules;
import com.tlnt.server.combat.PvpMath;
import com.tlnt.server.combat.StatRules;
import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.MapPack;
import com.tlnt.server.gather.GatherConfig;
import com.tlnt.server.gather.GatherService;
import com.tlnt.server.item.GearCharges;
import com.tlnt.server.item.ItemBuffs;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.linhmoc.LinhMocRules;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.map.MapTemplate;
import com.tlnt.server.net.Packets;
import com.tlnt.server.party.Party;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.Leveling;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.QuestService;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.social.PkRules;
import com.tlnt.server.thunder.ThunderRules;
import com.tlnt.server.thunder.ThunderRun;
import com.tlnt.server.tribe.TribeRules;
import com.tlnt.server.tribe.TribeTag;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/**
 * Đòn đánh của người chơi lên quái VÀ người chơi khác (PK / thách đấu), chạy trên LUỒNG TICK của khu (gọi qua
 * {@link World}, sau {@code zone.submit}). {@code CombatService} (luồng session) đã kiểm tra kỹ năng / vũ khí / hồi
 * chiêu / MP và tính sát thương thô; ở đây kiểm tra mục tiêu còn sống + đứng đủ gần + (người chơi) được phép đánh theo
 * {@link PkRules}, trừ máu, phát gói cho cả khu (kể cả người đánh: client chỉ chơi animation đánh khi nhận gói). Các bước
 * sau hẹn giờ bằng {@link Zone#schedule}; mỗi bước tự kiểm tra lại.
 *
 * <p>Vị trí quái: server không biết vị trí thật (client tự cho đi lang thang quanh điểm sinh trong
 * {@code mob_template.wander_range}) -> đòn nhắm 1 mục tiêu cho phép rộng (tầm + cả phạm vi lang thang + độ lệch mạng),
 * đòn diện rộng chọn chặt hơn (tầm + nửa phạm vi lang thang) để không trúng quái ở quá xa. Vị trí người chơi server
 * biết (op 5) -> chỉ cộng độ lệch mạng.
 *
 * <p>Người chơi: sát thương thô chỉ tính Tấn công, trừ giáp / kháng của người bị đánh ({@link PvpMath}); hút HP / MP
 * theo chỉ số. HP về 0 = bị hạ gục (hộp "Kiệt sức", chờ hồi sinh op 28); đang thách đấu nhau thì HP dừng ở 1 = thua.
 */
public final class ZoneCombat {

   /** Đòn 1 mục tiêu: độ lệch khoảng cách cho phép (trễ mạng giữa vị trí client và server). */
   private static final float HIT_SLACK_PX = 200f;
   /** Đòn diện rộng lên người chơi: độ lệch khoảng cách cho phép. */
   private static final float AREA_SLACK_PX = 100f;
   /** Đòn diện rộng: số mục tiêu tối đa mỗi loại mỗi gói (client đọc số lượng bằng byte). */
   private static final int MAX_AREA_TARGETS = 20;
   /** Id ảnh ảo: số âm, không bao giờ trùng id người chơi thật (id DB > 0). */
   private static final AtomicInteger NEXT_ILLUSION_ID = new AtomicInteger();

   private ZoneCombat() {
   }

   /**
    * Đòn nhắm 1 mục tiêu ({@code targetPlayer}: người chơi, không thì quái). {@code hits} = sát thương thô từng gói:
    * 1 phần tử = 1 gói; nhiều phần tử (skill 9 bắn 5 mũi, 21 đâm 5 cú) = gói đầu gửi ngay, các gói sau cách nhau
    * {@code hitIntervalMs}. {@code bleed} / {@code status} null = không có (hiệu ứng lên quái + boss Tế đàn, không lên người).
    */
   public record Strike(int skillId, boolean targetPlayer, int targetId, long[] hits, long hitIntervalMs, Bleed bleed,
         Status status, int rangeX, int rangeY, int lifestealPercent) {
   }

   /** Chảy máu sau đòn chính: {@code ticks} lần, mỗi {@code intervalMs} mất {@code damagePerTick}. */
   public record Bleed(long damagePerTick, int ticks, long intervalMs) {
   }

   /** Hiệu ứng lên quái sau đòn đầu tiên (op 35). {@code durationMs} bỏ qua với hiệu ứng tức thì (sét). */
   public record Status(MobStatus kind, int durationMs, long chancePoints) {
   }

   /**
    * Đòn chính lên 1 mục tiêu ({@code damage}) + tối đa {@code extraTargets} mục tiêu phụ mỗi loại, mất
    * {@code extraPercent}% của {@code mobExtraBase} (quái) / {@code playerExtraBase} (người). Mục tiêu phụ quanh:
    * <ul>
    *   <li>{@code aroundAttacker} = true: NGƯỜI BẮN, trong tầm kỹ năng ({@code rangeX} × {@code rangeY}), cả phía sau
    *       (11 Tán Diệt Tiễn).</li>
    *   <li>false: MỤC TIÊU CHÍNH, trong {@code radiusPx} (17 Xung Kích Nỗ).</li>
    * </ul>
    */
   public record Splash(int skillId, boolean targetPlayer, int targetId, long damage, long mobExtraBase,
         long playerExtraBase, int extraTargets, int extraPercent, boolean aroundAttacker, int radiusPx, int rangeX,
         int rangeY, Status status) {
   }

   /**
    * Xoay chém quanh người (5 Cuồng Phong Trảm): 1 lần dùng = 1 animation xoay {@code revolutions} vòng; MỖI vòng trúng
    * mọi quái / người được đánh trong tầm 1 lần ({@code damageRoll} quái, {@code pvpRoll} người). Vòng 2.. tới lúc
    * {@code secondRevolutionMs} + (k - 2) × {@code revolutionMs} sau khi bắt đầu (bám nhịp animation).
    */
   public record Spin(int skillId, int revolutions, long secondRevolutionMs, long revolutionMs, int rangeX,
         int rangeY, LongSupplier damageRoll, LongSupplier pvpRoll, Status status) {
   }

   /** Gồng {@code chargeMs} rồi bổ 1 mục tiêu (chiêu cuối): gói bắt đầu ngay, gói kết thúc + sát thương sau. */
   public record Charge(int skillId, boolean targetPlayer, int targetId, long chargeMs, long damage, int rangeX,
         int rangeY, Status status) {
   }

   /**
    * 28 Ảo Ảnh Phách: người chơi lướt ({@code dashSeconds}, {@code dashSpeed}); ảnh ảo (người chơi giả, id âm) đứng
    * lại chỗ cũ {@code durationMs}, mỗi {@code attackIntervalMs} quất quái gần nhất trong tầm bằng animation của
    * {@code attackSkillId} (đánh thường Roi), sát thương = {@code damageRoll}. Ảnh ảo không đánh người.
    */
   public record Illusion(int skillId, float dashSeconds, float dashSpeed, long durationMs, long attackIntervalMs,
         int attackSkillId, int attackRangeX, int attackRangeY, LongSupplier damageRoll) {
   }

   // ------------------------------------------------------------------ đòn nhắm 1 mục tiêu

   static void strike(Zone zone, WorldPlayer player, Strike s) {
      if (s.hits().length == 0) {
         return;
      }
      breakStealth(zone, player); // 28 Ảo Ảnh Phách: tự ra đòn -> hiện hình
      if (s.targetPlayer()) {
         strikePlayer(zone, player, s);
         return;
      }
      MapTemplate.MobInstance mob = zone.mobs().alive(s.targetId());
      if (mob == null || !inReach(player, mob, s.rangeX(), s.rangeY())) {
         return; // quái vừa chết / đánh từ xa (lag hoặc hack): bỏ qua, MP đã trừ như client
      }
      hitMob(zone, player, s, s.hits()[0]);
      for (int i = 1; i < s.hits().length; i++) {
         long damage = s.hits()[i];
         zone.schedule(i * s.hitIntervalMs(), () -> {
            if (player.zone == zone) { // rời khu / thoát giữa chừng -> bỏ các đòn còn lại
               hitMob(zone, player, s, damage);
            }
         });
      }
      Bleed bleed = s.bleed();
      if (bleed != null && bleed.damagePerTick() > 0) {
         long start = (s.hits().length - 1) * s.hitIntervalMs();
         for (int t = 1; t <= bleed.ticks(); t++) {
            zone.schedule(start + t * bleed.intervalMs(), () -> bleedTick(zone, player, s.targetId(),
                  bleed.damagePerTick()));
         }
      }
   }

   private static void strikePlayer(Zone zone, WorldPlayer player, Strike s) {
      AltarBoss boss = zone.bossById(s.targetId());
      if (boss != null) {
         strikeBoss(zone, player, boss, s);
         return;
      }
      WorldPlayer target = zone.find(s.targetId());
      if (!canHit(zone, player, target) || !inReach(player, target, s.rangeX(), s.rangeY())) {
         return;
      }
      hitPlayer(zone, player, target, s, s.hits()[0]);
      for (int i = 1; i < s.hits().length; i++) {
         long damage = s.hits()[i];
         zone.schedule(i * s.hitIntervalMs(), () -> {
            if (canHit(zone, player, target)) {
               hitPlayer(zone, player, target, s, damage);
            }
         });
      }
      Bleed bleed = s.bleed();
      if (bleed != null && bleed.damagePerTick() > 0) {
         long start = (s.hits().length - 1) * s.hitIntervalMs();
         for (int t = 1; t <= bleed.ticks(); t++) {
            zone.schedule(start + t * bleed.intervalMs(), () -> {
               if (canHit(zone, player, target)) {
                  PvpHit hit = hurt(player, target, bleed.damagePerTick(), false);
                  zone.broadcast(WorldPackets.playerDamaged(target.id(), hit.damage(), hit.hpLeft()), null);
                  afterHit(zone, player, target, hit);
               }
            });
         }
      }
   }

   /** 1 gói đòn 1 mục tiêu lên quái từ ảnh ảo (id âm, không hút máu; EXP cho chủ nếu còn ở khu). */
   private static void hitOnce(Zone zone, WorldPlayer owner, int attackerId, int skillId, int mobId, long damage) {
      MapTemplate.MobInstance mob = zone.mobs().alive(mobId);
      if (mob == null || zone.mobs().isGather(mobId)) {
         return;
      }
      using(owner, skillId);
      damage = vsMob(zone, owner, mobId, damage); // giáp quái / Người rơm: tối đa 1 HP / đòn
      boolean crit = owner.lastCrit;
      long now = System.currentTimeMillis();
      long before = zone.mobs().hp(mobId);
      long left = zone.mobs().damage(mobId, damage, now);
      recordBossDamage(zone, owner, mobId, Math.max(0, before - left)); // ảnh ảo: tính cho chủ
      if (owner.zone == zone) {
         gainMobExp(zone, owner, mob, Math.max(0, before - left));
         provoke(zone, owner, mobId, left, now);
         if (before > 0 && left <= 0) {
            onMobKilled(zone, owner, mob);
         }
      } else if (before > 0 && left <= 0) {
         checkAltarCleared(zone);
      }
      zone.broadcast(WorldPackets.singleTargetHit(attackerId, skillId, WorldPackets.TARGET_MOB, mobId, left, damage, crit)
            .toPacket(), null);
   }

   /** 1 gói đòn 1 mục tiêu lên quái của người chơi. Quái đã chết thì bỏ; chết -> ZoneMobs hẹn hồi sinh. */
   private static void hitMob(Zone zone, WorldPlayer player, Strike s, long damage) {
      int mobId = s.targetId();
      if (zone.mobs().alive(mobId) == null) {
         return;
      }
      using(player, s.skillId());
      long shown = vsMob(zone, player, mobId, damage);
      boolean crit = player.lastCrit;
      long left = damageMob(zone, player, mobId, shown, System.currentTimeMillis(), s.status());
      if (left > 0 && s.status() != null && s.status().kind() == MobStatus.ROOT) {
         zone.broadcast(WorldPackets.mobStatus(MobStatus.ROOT, mobId, s.status().durationMs()).toPacket(), null);
      }
      zone.broadcast(WorldPackets.singleTargetHit(player.id(), s.skillId(), WorldPackets.TARGET_MOB, mobId, left, shown,
            crit)
            .toPacket(), null);
      skillLifesteal(player, shown, s.lifestealPercent());
   }

   /** 1 gói đòn 1 mục tiêu lên người chơi. */
   private static void hitPlayer(Zone zone, WorldPlayer attacker, WorldPlayer target, Strike s, long raw) {
      using(attacker, s.skillId());
      PvpHit hit = hurt(attacker, target, raw, s.status());
      zone.broadcast(WorldPackets.singleTargetHit(attacker.id(), s.skillId(), WorldPackets.TARGET_PLAYER, target.id(),
            hit.hpLeft(), hit.damage(), attacker.lastCrit).toPacket(), null);
      afterHit(zone, attacker, target, hit);
      skillLifesteal(attacker, hit.damage(), s.lifestealPercent());
   }

   /**
    * Hút máu của kỹ năng (22 Xuyên Tâm Kích): hồi % sát thương ĐÃ GÂY RA, chỉ khi đòn trúng thật (trước đây tính trên
    * luồng session theo sát thương dự kiến -> đánh hụt / id quái giả vẫn hồi máu).
    */
   private static void skillLifesteal(WorldPlayer player, long dealt, int percent) {
      if (percent <= 0 || dealt <= 0) {
         return;
      }
      PlayerData d = player.data();
      PlayerStats s = player.stats();
      if (d.isDead() || d.hp >= s.maxHp) {
         return;
      }
      d.healHp(dealt * percent / 100, s.maxHp);
      player.send(ItemPackets.hpMp(player.id(), d.hp, s.maxHp, d.mp, s.maxMp));
   }

   /**
    * Chảy máu / thiêu đốt nằm trên quái: người đánh rời khu vẫn tiếp tục. Quái chết vì nó -> người ra chiêu
    * ({@code player}) là người KẾT LIỄU (quà boss, nhiệm vụ, đồ Tinh anh) nếu còn ở khu.
    */
   static void bleedTick(Zone zone, WorldPlayer player, int mobId, long damage) {
      MapTemplate.MobInstance mob = zone.mobs().alive(mobId);
      if (mob == null || zone.mobs().isGather(mobId)) { // quái săn bắt: chỉ đòn trúng tính
         return;
      }
      damage = dealt(zone, mobId, damage); // Người rơm: mỗi lần chảy máu tối đa 1 HP
      long left = zone.mobs().damage(mobId, damage, System.currentTimeMillis());
      zone.broadcast(WorldPackets.mobDamaged(mobId, damage, left).toPacket(), null);
      if (left <= 0) {
         if (player.zone == zone && !player.removed) {
            onMobKilled(zone, player, mob); // người ra chiêu kết liễu
         } else {
            checkAltarCleared(zone); // đã rời khu: không ai kết liễu
         }
      }
   }

   // ------------------------------------------------------------------ đòn chính + mục tiêu phụ

   static void splash(Zone zone, WorldPlayer player, Splash s) {
      breakStealth(zone, player); // 28 Ảo Ảnh Phách: tự ra đòn -> hiện hình
      using(player, s.skillId());
      ZoneMobs mobs = zone.mobs();
      long now = System.currentTimeMillis();
      int mainKind;
      long mainLeft;
      long mainDamage;
      float cx;
      float cy;
      WorldPlayer mainTarget = null;
      PvpHit mainHit = null;
      boolean mainCrit;
      AltarBoss boss = s.targetPlayer() ? zone.bossById(s.targetId()) : null;
      if (boss != null) {
         if (!nearBoss(player, boss, s.rangeX(), s.rangeY())) {
            return;
         }
         long[] r = hitBoss(zone, player, boss, s.damage(), true, s.status());
         mainCrit = player.lastCrit;
         mainKind = WorldPackets.TARGET_PLAYER;
         mainDamage = r[0];
         mainLeft = r[1];
         cx = boss.x;
         cy = boss.y;
      } else if (s.targetPlayer()) {
         mainTarget = zone.find(s.targetId());
         if (!canHit(zone, player, mainTarget) || !inReach(player, mainTarget, s.rangeX(), s.rangeY())) {
            return;
         }
         mainHit = hurt(player, mainTarget, s.damage(), s.status());
         mainCrit = player.lastCrit;
         mainKind = WorldPackets.TARGET_PLAYER;
         mainLeft = mainHit.hpLeft();
         mainDamage = mainHit.damage();
         cx = mainTarget.x;
         cy = mainTarget.y;
      } else {
         MapTemplate.MobInstance main = mobs.alive(s.targetId());
         if (main == null || !inReach(player, main, s.rangeX(), s.rangeY())) {
            return;
         }
         mainKind = WorldPackets.TARGET_MOB;
         mainDamage = vsMob(zone, player, s.targetId(), s.damage());
         mainCrit = player.lastCrit;
         mainLeft = damageMob(zone, player, s.targetId(), mainDamage, now, s.status());
         cx = main.x();
         cy = main.y();
      }
      int max = Math.min(s.extraTargets(), MAX_AREA_TARGETS);
      HitList mobHits = new HitList(max);
      long mobExtra = Math.max(1, s.mobExtraBase() * s.extraPercent() / 100);
      for (int k = 0; k < mobs.slots() && !mobHits.full(); k++) {
         int id = mobs.idAt(k);
         MapTemplate.MobInstance mob = !s.targetPlayer() && id == s.targetId() ? null : mobs.alive(id);
         boolean inside = mob != null && (s.aroundAttacker()
               ? inArea(player.x, player.y, mob, s.rangeX(), s.rangeY())
               : Math.abs(mob.x() - cx) <= s.radiusPx() && Math.abs(mob.y() - cy) <= s.radiusPx());
         if (inside) {
            long shown = vsMob(zone, player, id, mobExtra);
            boolean crit = player.lastCrit;
            long left = damageMob(zone, player, id, shown, now, s.status());
            mobHits.add(id, left, shown, crit);
         }
      }
      HitList playerHits = new HitList(max);
      List<WorldPlayer> hurtPlayers = new ArrayList<>();
      List<PvpHit> results = new ArrayList<>();
      long playerExtra = Math.max(1, s.playerExtraBase() * s.extraPercent() / 100);
      for (WorldPlayer other : zone.players()) {
         if (playerHits.full()) {
            break;
         }
         if (other == mainTarget || !canHit(zone, player, other)) {
            continue;
         }
         boolean inside = s.aroundAttacker()
               ? inArea(player.x, player.y, other, s.rangeX(), s.rangeY())
               : Math.abs(other.x - cx) <= s.radiusPx() && Math.abs(other.y - cy) <= s.radiusPx();
         if (inside) {
            PvpHit hit = hurt(player, other, playerExtra, s.status());
            playerHits.add(other.id(), hit.hpLeft(), hit.damage(), player.lastCrit);
            hurtPlayers.add(other);
            results.add(hit);
         }
      }
      zone.broadcast(WorldPackets.splashHit(player.id(), s.skillId(), mainKind, s.targetId(), mainLeft, mainDamage,
            mainCrit, mobHits, playerHits).toPacket(), null);
      if (boss != null) {
         checkAltarCleared(zone);
      }
      if (mainHit != null) {
         afterHit(zone, player, mainTarget, mainHit);
      }
      for (int i = 0; i < hurtPlayers.size(); i++) {
         afterHit(zone, player, hurtPlayers.get(i), results.get(i));
      }
   }

   // ------------------------------------------------------------------ xoay chém diện rộng

   /**
    * Vòng 1 = gói op 21 sub 0 danh sách quái + người: bắt đầu animation + hiệu ứng trúng (client chỉ hiện hiệu ứng lúc
    * animation BẮT ĐẦU). Vòng 2.. = op 20 sub 3 từng quái / op 2 sub 3 từng người, KHÔNG gửi thêm gói op 21 trong cùng
    * animation: client dồn mục tiêu vào {@code aoeHitTargets} tới lần dùng sau mới xoá -> hiệu ứng trúng sai.
    */
   static void spin(Zone zone, WorldPlayer player, Spin s) {
      breakStealth(zone, player); // 28 Ảo Ảnh Phách: tự ra đòn -> hiện hình
      firstRevolution(zone, player, s);
      for (int k = 2; k <= s.revolutions(); k++) {
         zone.schedule(s.secondRevolutionMs() + (k - 2) * s.revolutionMs(), () -> {
            if (player.zone == zone && !player.data().isDead()) {
               nextRevolution(zone, player, s);
            }
         });
      }
   }

   /** Gửi cả khi không trúng gì (client vẫn phải chơi animation xoay). */
   private static void firstRevolution(Zone zone, WorldPlayer player, Spin s) {
      using(player, s.skillId());
      ZoneMobs mobs = zone.mobs();
      HitList mobHits = new HitList(MAX_AREA_TARGETS);
      long now = System.currentTimeMillis();
      for (int k = 0; k < mobs.slots() && !mobHits.full(); k++) {
         int id = mobs.idAt(k);
         MapTemplate.MobInstance mob = mobs.alive(id);
         if (mob != null && inArea(player.x, player.y, mob, s.rangeX(), s.rangeY())) {
            long shown = vsMob(zone, player, id, s.damageRoll().getAsLong());
            boolean crit = player.lastCrit;
            long left = damageMob(zone, player, id, shown, now, s.status());
            mobHits.add(id, left, shown, crit);
         }
      }
      HitList playerHits = new HitList(MAX_AREA_TARGETS);
      List<WorldPlayer> hurtPlayers = new ArrayList<>();
      List<PvpHit> results = new ArrayList<>();
      for (WorldPlayer other : zone.players()) {
         if (playerHits.full()) {
            break;
         }
         if (canHit(zone, player, other) && inArea(player.x, player.y, other, s.rangeX(), s.rangeY())) {
            PvpHit hit = hurt(player, other, s.pvpRoll().getAsLong(), s.status());
            playerHits.add(other.id(), hit.hpLeft(), hit.damage(), player.lastCrit);
            hurtPlayers.add(other);
            results.add(hit);
         }
      }
      AltarBoss boss = zone.boss;
      if (boss != null && !boss.dead && !playerHits.full() && nearBoss(player, boss, s.rangeX(), s.rangeY())) {
         long[] r = hitBoss(zone, player, boss, s.pvpRoll().getAsLong(), true, s.status());
         playerHits.add(boss.id, r[1], r[0], player.lastCrit);
      }
      zone.broadcast(WorldPackets.areaHit(player.id(), s.skillId(), mobHits, playerHits).toPacket(), null);
      checkAltarCleared(zone);
      for (int i = 0; i < hurtPlayers.size(); i++) {
         afterHit(zone, player, hurtPlayers.get(i), results.get(i));
      }
   }

   private static void nextRevolution(Zone zone, WorldPlayer player, Spin s) {
      using(player, s.skillId());
      ZoneMobs mobs = zone.mobs();
      long now = System.currentTimeMillis();
      int hit = 0;
      for (int k = 0; k < mobs.slots() && hit < MAX_AREA_TARGETS; k++) {
         int id = mobs.idAt(k);
         MapTemplate.MobInstance mob = mobs.alive(id);
         if (mob != null && inArea(player.x, player.y, mob, s.rangeX(), s.rangeY())) {
            long shown = vsMob(zone, player, id, s.damageRoll().getAsLong());
            long left = damageMob(zone, player, id, shown, now, s.status());
            zone.broadcast(WorldPackets.mobDamaged(id, shown, left).toPacket(), null);
            hit++;
         }
      }
      List<WorldPlayer> targets = new ArrayList<>();
      for (WorldPlayer other : zone.players()) {
         if (targets.size() < MAX_AREA_TARGETS && canHit(zone, player, other)
               && inArea(player.x, player.y, other, s.rangeX(), s.rangeY())) {
            targets.add(other);
         }
      }
      for (WorldPlayer other : targets) {
         PvpHit result = hurt(player, other, s.pvpRoll().getAsLong(), s.status());
         zone.broadcast(WorldPackets.playerDamaged(other.id(), result.damage(), result.hpLeft()), null);
         afterHit(zone, player, other, result);
      }
      AltarBoss boss = zone.boss;
      if (boss != null && !boss.dead && nearBoss(player, boss, s.rangeX(), s.rangeY())) {
         long[] r = hitBoss(zone, player, boss, s.pvpRoll().getAsLong(), true, s.status());
         zone.broadcast(WorldPackets.playerDamaged(boss.id, r[0], r[1]), null);
         checkAltarCleared(zone);
      }
   }

   // ------------------------------------------------------------------ gồng rồi bổ / vào thế

   static void charge(Zone zone, WorldPlayer player, Charge c) {
      breakStealth(zone, player); // 28 Ảo Ảnh Phách: tự ra đòn -> hiện hình
      zone.broadcast(WorldPackets.skillPhase(player.id(), c.skillId(), true).toPacket(), null);
      zone.schedule(c.chargeMs(), () -> {
         if (player.zone != zone) {
            return;
         }
         using(player, c.skillId());
         // LUÔN gửi gói kết thúc (client lặp tư thế gồng tới khi nhận). Mục tiêu chết / chạy xa: sát thương 0 ("hụt").
         AltarBoss boss = c.targetPlayer() ? zone.bossById(c.targetId()) : null;
         if (boss != null) {
            long[] r = !player.data().isDead() && nearBoss(player, boss, c.rangeX(), c.rangeY())
                  ? hitBoss(zone, player, boss, c.damage(), true, c.status()) : new long[] {0, boss.hp};
            zone.broadcast(WorldPackets.chargedHit(player.id(), c.skillId(), WorldPackets.TARGET_PLAYER, c.targetId(),
                  r[1], r[0], r[0] > 0 && player.lastCrit).toPacket(), null);
            checkAltarCleared(zone);
            return;
         }
         if (c.targetPlayer()) {
            WorldPlayer target = zone.find(c.targetId());
            if (canHit(zone, player, target) && inReach(player, target, c.rangeX(), c.rangeY())) {
               PvpHit hit = hurt(player, target, c.damage(), c.status());
               zone.broadcast(WorldPackets.chargedHit(player.id(), c.skillId(), WorldPackets.TARGET_PLAYER,
                     c.targetId(), hit.hpLeft(), hit.damage(), player.lastCrit).toPacket(), null);
               afterHit(zone, player, target, hit);
            } else {
               zone.broadcast(WorldPackets.chargedHit(player.id(), c.skillId(), WorldPackets.TARGET_PLAYER,
                     c.targetId(), target == null ? 0 : target.data().hp, 0, false).toPacket(), null);
            }
            return;
         }
         MapTemplate.MobInstance mob = zone.mobs().alive(c.targetId());
         long raw = mob != null && !player.data().isDead() && inReach(player, mob, c.rangeX(), c.rangeY())
               ? c.damage() : 0;
         long damage = raw > 0 ? vsMob(zone, player, c.targetId(), raw) : 0;
         boolean crit = raw > 0 && player.lastCrit;
         long left = raw > 0 ? damageMob(zone, player, c.targetId(), damage, System.currentTimeMillis(), c.status())
               : zone.mobs().hp(c.targetId());
         zone.broadcast(WorldPackets.chargedHit(player.id(), c.skillId(), WorldPackets.TARGET_MOB, c.targetId(), left,
               damage, crit).toPacket(), null);
      });
   }

   /** Vào thế / buff {@code durationMs} (4, 10, 16): gói bắt đầu ngay, gói kết thúc sau. */
   static void channel(Zone zone, WorldPlayer player, int skillId, long durationMs) {
      zone.broadcast(WorldPackets.skillPhase(player.id(), skillId, true).toPacket(), null);
      zone.schedule(durationMs, () -> {
         if (player.zone == zone) {
            zone.broadcast(WorldPackets.skillPhase(player.id(), skillId, false).toPacket(), null);
         }
      });
   }

   // ------------------------------------------------------------------ ảnh ảo

   /**
    * 28 Ảo Ảnh Phách: để lại ảnh ảo ở chỗ đứng, chủ lướt về phía trước rồi TÀNG HÌNH trong {@code durationMs} (op 2 sub 2
    * — người khác không thấy / không chọn được, quái thôi đuổi; bản thân mờ đi). Ảnh ảo quất quái quanh nó suốt thời gian
    * đó. Chủ tự ra đòn đánh ({@link #breakStealth}) hoặc hết giờ / rời khu -> hiện hình + ảnh ảo biến mất. Người vào khu
    * SAU lúc ảnh ảo xuất hiện không thấy ảnh ảo (chỉ vài giây, chấp nhận) nhưng vẫn không thấy chủ đang tàng hình.
    */
   static void illusion(Zone zone, WorldPlayer player, Illusion il) {
      breakStealth(zone, player); // dùng lại khi đang tàng hình: ảnh ảo cũ biến mất
      int illusionId = NEXT_ILLUSION_ID.decrementAndGet();
      float x = player.x;
      float y = player.y;
      zone.broadcast(WorldPackets.illusionSpawn(player, illusionId, x, y), null);
      zone.broadcast(WorldPackets.dash(player.id(), il.skillId(), il.dashSeconds(), il.dashSpeed()).toPacket(), null);
      player.illusionId = illusionId;
      zone.broadcast(WorldPackets.stealth(player.id(), true), null);
      for (long t = il.attackIntervalMs(); t < il.durationMs(); t += il.attackIntervalMs()) {
         zone.schedule(t, () -> illusionAttack(zone, player, illusionId, x, y, il));
      }
      zone.schedule(il.durationMs(), () -> {
         if (player.illusionId == illusionId && player.zone == zone) {
            breakStealth(zone, player);
         } else if (player.illusionId == illusionId) { // đã rời khu: chỉ gỡ ảnh ảo ở khu cũ
            player.illusionId = 0;
            zone.broadcast(WorldPackets.playerLeave(illusionId), null);
         }
      });
   }

   /** Đang tàng hình (28 Ảo Ảnh Phách). Luồng tick khu. */
   static boolean stealthed(WorldPlayer p) {
      return p != null && p.illusionId != 0;
   }

   /** Hiện hình: gỡ ảnh ảo + op 2 sub 2 false (không tàng hình thì thôi). Luồng tick khu. */
   static void breakStealth(Zone zone, WorldPlayer player) {
      int illusionId = player.illusionId;
      if (illusionId == 0) {
         return;
      }
      player.illusionId = 0;
      zone.broadcast(WorldPackets.playerLeave(illusionId), null);
      zone.broadcast(WorldPackets.stealth(player.id(), false), null);
   }

   /** Ảnh ảo quất quái gần nhất trong tầm (không có quái -> đứng yên). Ảnh ảo đã mất (chủ hiện hình) thì thôi. */
   private static void illusionAttack(Zone zone, WorldPlayer owner, int illusionId, float x, float y, Illusion il) {
      if (owner.illusionId != illusionId) {
         return;
      }
      ZoneMobs mobs = zone.mobs();
      int nearest = -1;
      float best = Float.MAX_VALUE;
      for (int id = 0; id < mobs.size(); id++) {
         MapTemplate.MobInstance mob = mobs.alive(id);
         if (mob != null && inArea(x, y, mob, il.attackRangeX(), il.attackRangeY())) {
            float d = Math.abs(mob.x() - x) + Math.abs(mob.y() - y);
            if (d < best) {
               best = d;
               nearest = id;
            }
         }
      }
      if (nearest >= 0) {
         hitOnce(zone, owner, illusionId, il.attackSkillId(), nearest, il.damageRoll().getAsLong());
      }
   }

   // ------------------------------------------------------------------ người đánh người

   /** Kết quả 1 đòn lên người: sát thương sau giảm trừ, HP còn, có phải trong thách đấu không. */
   record PvpHit(long damage, long hpLeft, boolean duel) {
   }

   /**
    * {@code attacker} đánh được {@code target} lúc này không: cùng khu, cả 2 còn sống, mục tiêu không khoá nhân vật
    * (ngắm cảnh), đúng luật trạng thái PK hoặc đang thách đấu nhau.
    */
   static boolean canHit(Zone zone, WorldPlayer attacker, WorldPlayer target) {
      if (target == null || target == attacker || target.removed || target.locked || target.zone != zone
            || attacker.zone != zone || stealthed(target)) {
         return false;
      }
      PlayerData a = attacker.data();
      PlayerData t = target.data();
      if (a.isDead() || t.isDead()) {
         return false;
      }
      Arena arena = zone.arena;
      if (arena != null) { // Giải đấu Bộ lạc: chỉ khi giải cho đánh, không đánh đồng đội (Hợp chiến); bỏ qua cờ / Bùa hộ giáp
         return arena.fighting && !arena.sameTeam(attacker.id(), target.id());
      }
      boolean dueling = attacker.dueling(target);
      long now = System.currentTimeMillis();
      if (!dueling && !zone.map().isThunder() && (ItemBuffs.active(a, ItemBuffs.OPTION_PK_SHIELD, now)
            || ItemBuffs.active(t, ItemBuffs.OPTION_PK_SHIELD, now))) {
         return false; // Bùa hộ giáp: không bị đánh / không đánh được người khác (thách đấu là tự đồng ý -> vẫn được)
      }
      Party party = a.party;
      boolean sameParty = party != null && party == t.party;
      TribeTag tribe = a.tribe;
      TribeTag otherTribe = t.tribe;
      boolean sameTribe = tribe != null && otherTribe != null && tribe.id() == otherTribe.id();
      return PkRules.canAttack(a.faction, t.faction, sameParty, sameTribe, dueling);
   }

   /** Trừ máu người chơi (không phát gói). Thách đấu: HP không xuống dưới 1. Kèm hút HP / MP cho người đánh. */
   static PvpHit hurt(WorldPlayer attacker, WorldPlayer target, long raw) {
      return hurt(attacker, target, raw, true);
   }

   static PvpHit hurt(WorldPlayer attacker, WorldPlayer target, long raw, Status skillStatus) {
      return hurt(attacker, target, raw, true, skillStatus);
   }

   /**
    * Như trên; {@code canCrit} false = chảy máu (không né, không chí mạng, không phản, không kích hoạt hiệu ứng). Đòn thường:
    * né theo (Né − Chính xác) / 10000 (tối đa 75%, sát thương 0 -> client hiện "né" / "hụt"), chí mạng trừ Kháng chí mạng,
    * giáp ({@link PvpMath}), rồi {@link #incoming}; sau đó Hấp thụ, hiệu ứng từ chỉ số, Phản sát thương.
    */
   static PvpHit hurt(WorldPlayer attacker, WorldPlayer target, long raw, boolean canCrit) {
      return hurt(attacker, target, raw, canCrit, null);
   }

   private static PvpHit hurt(WorldPlayer attacker, WorldPlayer target, long raw, boolean canCrit, Status skillStatus) {
      Zone zone = attacker.zone;
      attacker.lastCrit = false;
      PlayerData t = target.data();
      boolean duel = attacker.dueling(target);
      if (canCrit && StatRules.dodged(target.stats().evasion, attacker.stats().accuracy,
            ThreadLocalRandom.current().nextInt(StatRules.SCALE))) {
         return new PvpHit(0, t.hp, duel); // né
      }
      if (canCrit) { // Nanh Hổ răng kiếm / chí mạng (chảy máu không chí mạng)
         raw = critical(zone, attacker, targetKey(null, KEY_PLAYER, target.id(), target.lives), raw,
               target.stats().critResistPercent);
      }
      long damage = incoming(target, PvpMath.reduce(raw, attacker.stats(), target.stats()), false);
      long before = t.hp;
      long left = t.damageHp(damage, duel ? 1 : 0);
      long taken = Math.max(0, before - left);
      if (t.bot && !attacker.data().bot) {
         World.BotHurtListener l = World.botHurt;
         if (l != null) {
            l.attacked(target.id(), attacker.id()); // bot bị PK: BotService quyết đánh trả / chạy
         }
      }
      target.invalidateSpawnPacket(); // gói xuất hiện có HP
      leech(attacker, taken, false);
      absorb(target, taken);
      if (left > (duel ? 1 : 0) && damage > 0 && zone != null) {
         procOnPlayer(zone, attacker, target); // Tim băng / Nhẫn u mộc
         if (canCrit) {
            statProcsOnPlayer(zone, attacker, target, skillStatus); // cộng điểm từ skill với chỉ số cùng loại
         }
      }
      if (canCrit && zone != null) {
         reflectToPlayer(zone, target, attacker, taken);
      }
      return new PvpHit(damage, left, duel);
   }

   /** Sau khi đã phát gói trúng đòn: thua thách đấu (HP về 1) hoặc bị hạ gục (HP về 0). */
   private static void afterHit(Zone zone, WorldPlayer attacker, WorldPlayer target, PvpHit hit) {
      Arena arena = zone.arena;
      if (arena != null && hit.damage() > 0) { // Giải đấu: sát thương gây ra (hết giờ so ai gây nhiều hơn) + đòn cuối
         arena.addDamage(attacker.id(), hit.damage());
         target.arenaHitBy = attacker.id();
         target.arenaHitAt = System.currentTimeMillis();
      }
      if (hit.duel()) {
         if (hit.hpLeft() <= 1) {
            World.duelWon(zone, attacker, target);
         }
         return;
      }
      if (hit.hpLeft() <= 0) {
         target.clearPendingPosition();
         died(target, "Bạn đã bị " + attacker.data().name + " hạ gục.");
         sendStamina(target); // bị hạ gục -50 thể lực (PlayerData.damageHp)
         attacker.send(Packets.notice("Bạn đã hạ gục " + target.data().name + "."));
         if (arena != null) { // Giải đấu: không tính điểm PK, giải tính điểm / thắng thua
            arenaKilled(zone, attacker, target);
         } else if (zone.map().isThunder()) { // Lôi vực: không tính điểm PK, thưởng điểm cống hiến / hoạt động
            World.ThunderHooks hooks = zone.map().world().thunderHooks();
            if (hooks != null) {
               hooks.playerKilled(attacker, target);
            }
         } else {
            pkKill(attacker, target);
         }
         com.tlnt.server.daily.DailyEvents.add(attacker, com.tlnt.server.daily.DailyKind.PVP, 0, 1); // NV hằng ngày "Đánh bại người khác"
         if (target.duelOpponentId != 0) {
            World.endDuel(zone, target, "Thách đấu bị huỷ.");
         }
      }
   }

   /**
    * Điểm sát khí khi 1 người hạ gục người khác (ngoài thách đấu, ngoài Lôi vực): người đánh đang cờ Thử thách -> +1
    * (tối đa {@link PkRules#MAX_PK_POINTS}), mốc giảm theo thời gian tính lại từ lúc này; chạm mức phạt (20 / 50) -> tính
    * lại chỉ số trên luồng session ({@code social/PkPoints}). Người bị hạ gục không bị phạt gì.
    */
   static void pkKill(WorldPlayer attacker, WorldPlayer target) {
      PlayerData a = attacker.data();
      if (a.faction != PkRules.CHALLENGE) {
         return;
      }
      int before;
      int points;
      synchronized (a) {
         before = a.pkPoints;
         points = a.pkPoints = Math.min(PkRules.MAX_PK_POINTS, a.pkPoints + 1);
         a.pkPointsAt = System.currentTimeMillis();
      }
      attacker.send(Packets.notice("Điểm sát khí: " + points + "/" + PkRules.MAX_PK_POINTS + "."));
      Zone zone = attacker.zone;
      World world = zone == null ? null : zone.map().world();
      if (world != null && PkRules.statPercentLeft(before) != PkRules.statPercentLeft(points)) {
         PlayerSession s = attacker.session();
         GearCharges.post(s, () -> {
            if (s.getPlayer() == a) {
               com.tlnt.server.social.PkPoints.added(s, world, a, before, points);
            }
         });
      }
   }

   /**
    * Quái (mob_template.id) mỗi đòn trúng chỉ mất tối đa 1 HP, mọi kỹ năng / chỉ số: 33 Người rơm (bù nhìn tập đánh ở
    * Làng ánh sáng Eldarah, 10 HP = 10 đòn — HP đặt trong {@code map_template.mobs}).
    */
   static final java.util.Set<Integer> ONE_DAMAGE_MOBS = java.util.Set.of(33);

   /** Quái này mỗi đòn chỉ mất 1 HP (quái săn bắt hoặc {@link #ONE_DAMAGE_MOBS}). */
   private static boolean oneDamage(Zone zone, int mobId) {
      if (zone.mobs().isGather(mobId)) {
         return true;
      }
      MapTemplate.MobInstance mob = zone.mobs().alive(mobId);
      return mob != null && ONE_DAMAGE_MOBS.contains(mob.spawn().id());
   }

   /** Sát thương hiện lên client không có người đánh (chảy máu): quái săn bắt / Người rơm luôn tối đa 1. */
   private static long dealt(Zone zone, int mobId, long damage) {
      return oneDamage(zone, mobId) ? Math.min(1, damage) : damage;
   }

   /**
    * Sát thương THẬT của {@code player} lên quái từ sát thương thô (đã có % kỹ năng + dao động): giáp quái theo cấp
    * trừ Xuyên giáp, người cao hơn quái ≥ 10 cấp chỉ 20% — TRỪ phó bản / Lôi vực ({@link GameMap#noLevelGapPenalty})
    * ({@link MobRules#playerHitsMob}). Quái săn bắt / Người rơm: 1.
    */
   private static long vsMob(Zone zone, WorldPlayer player, int mobId, long raw) {
      player.lastCrit = false;
      if (oneDamage(zone, mobId)) {
         return Math.min(1, raw);
      }
      MapTemplate.MobInstance mob = zone.mobs().alive(mobId);
      if (mob == null || raw <= 0) {
         return raw;
      }
      raw = critical(zone, player, targetKey(zone, KEY_MOB, mobId, zone.mobs().deaths(mobId)), raw); // Nanh Hổ / chí mạng
      long damage = MobRules.playerHitsMob(raw, player.data().level, mob.spawn().level(), player.stats().armorPen,
            !zone.map().noLevelGapPenalty()); // phó bản / Lôi vực: không giảm 20% theo chênh cấp
      if (zone.mobs().weakened(mobId, System.currentTimeMillis())) {
         damage = StatRules.weakened(damage); // Suy yếu: +20%
      }
      return boostVsMob(player, damage); // Thuốc Sức mạnh: + % trên sát thương đã gây lên quái
   }

   /** Quái thường còn sống bị {@code player} đánh -> quay sang đánh trả (quái săn bắt / Người rơm không đánh). */
   private static void provoke(Zone zone, WorldPlayer player, int mobId, long hpLeft, long now) {
      if (MobRules.mobAttack() && hpLeft > 0 && !oneDamage(zone, mobId) && !player.data().isDead()) {
         zone.mobs().provoke(mobId, player.id(), now); // vào danh sách cắn: quái cắn lần lượt mọi người đã đánh nó
      }
   }

   /** Trừ máu quái ({@code damage} = sát thương thật, qua {@link #vsMob}) + hút HP / MP + EXP. @return HP quái còn */
   private static long damageMob(Zone zone, WorldPlayer player, int mobId, long damage, long now) {
      return damageMob(zone, player, mobId, damage, now, null);
   }

   private static long damageMob(Zone zone, WorldPlayer player, int mobId, long damage, long now, Status skillStatus) {
      ZoneMobs.Gather gather = zone.mobs().gather(mobId);
      if (gather != null) {
         return hitGather(zone, player, gather, now);
      }
      if (oneDamage(zone, mobId)) {
         damage = Math.min(1, damage); // Người rơm: tối đa 1 HP / đòn
      }
      MapTemplate.MobInstance mob = zone.mobs().alive(mobId);
      long before = zone.mobs().hp(mobId);
      long left = zone.mobs().damage(mobId, damage, now);
      long dealt = Math.max(0, before - Math.max(0, left));
      recordBossDamage(zone, player, mobId, dealt); // boss quái Rừng Linh Mộc: quà cho người gây nhiều nhất
      leech(player, dealt, true);
      if (mob != null) {
         gainMobExp(zone, player, mob, dealt);
         provoke(zone, player, mobId, left, now);
         if (before > 0 && left <= 0) {
            onMobKilled(zone, player, mob); // đòn kết liễu
         } else if (left > 0 && dealt > 0 && !oneDamage(zone, mobId)) {
            procOnMob(zone, player, mobId); // Tim băng / Nhẫn u mộc
            statProcsOnMob(zone, player, mobId, mob, skillStatus); // cộng điểm từ skill với chỉ số cùng loại
         }
      }
      return left;
   }

   /**
    * Quái {@code mob} vừa bị {@code player} kết liễu: bước đánh quái (thường + Tinh anh) tính cho người kết liễu + đồng
    * đội CÙNG KHU; vật phẩm nhiệm vụ rơi RIÊNG cho người kết liễu (chỉ họ thấy / nhặt — bước nhặt đồ phải tự làm).
    */
   private static void onMobKilled(Zone zone, WorldPlayer killer, MapTemplate.MobInstance mob) {
      if (isWorldMobBoss(zone, mob.id())) { // boss thế giới dạng quái: người kết liễu nhận quà (worldBossCheck)
         for (WorldBoss wb : zone.worldBosses) {
            if (wb.isMob(mob.id())) {
               wb.killedBy(killer);
            }
         }
         checkAltarCleared(zone);
         return;
      }
      int mobTemplateId = mob.spawn().id();
      int variant = mob.spawn().variant();
      WorldPlayer player = killer; // mọi quái (cả Tinh anh / Thủ lĩnh): công thuộc người kết liễu
      QuestEvents.mobKilled(player, mobTemplateId, variant);
      com.tlnt.server.daily.DailyEvents.mobKilled(player, mobTemplateId, variant); // NV hằng ngày: kết liễu + đồng đội cùng khu
      com.tlnt.server.achievement.AchievementEvents.mobKilled(player, variant); // thành tựu Tinh anh / Thủ lĩnh: người kết liễu
      if (variant == MobRules.VARIANT_ELITE || variant == MobRules.VARIANT_BOSS) { // điểm hoạt động: người kết liễu
         // Bảng Chiến binh (+ tuần): Tinh anh 5, Thủ lĩnh 10 điểm cho người kết liễu
         com.tlnt.server.ranking.RankingService.eliteKilled(player.data(), variant == MobRules.VARIANT_BOSS);
         com.tlnt.server.player.ActivityPoints.post(player.session(), variant == MobRules.VARIANT_ELITE
               ? com.tlnt.server.player.ActivityPoints.ELITE : com.tlnt.server.player.ActivityPoints.LEADER);
      }
      Party party = player.data().party;
      if (party != null) {
         for (WorldPlayer other : zone.players()) {
            if (other != player && other.data().party == party) {
               QuestEvents.mobKilled(other, mobTemplateId, variant);
               com.tlnt.server.daily.DailyEvents.mobKilled(other, mobTemplateId, variant);
            }
         }
      }
      ItemData drop = QuestEvents.questDrop(player, mobTemplateId, variant);
      if (drop != null && QuestService.atStep(player.data(), StepKind.COLLECT_ELITE)) {
         // 13.1 Nanh Hổ răng kiếm: 100%, cả khu thấy nhưng chỉ người làm nhiệm vụ nhặt được
         World.dropOwnerOnly(zone, mob.x(), mob.y(), player, drop);
      } else if (drop != null) { // nhặt đồ nhiệm vụ (5.2 / 5.3): rơi RIÊNG (chỉ người kết liễu thấy / nhặt)
         World.dropPersonal(zone, mob.x(), mob.y(), player, drop);
      } else if (player.data().level - mob.spawn().level() < MobRules.ELITE_NO_DROP_GAP) {
         // người khác: đồ quý của Tinh anh / Thủ lĩnh rơi theo tỉ lệ — RIÊNG người kết liễu (chỉ họ thấy / nhặt);
         // cao hơn siêu quái ≥ 15 cấp: không rơi gì
         int rare = MobRules.eliteDrop(mobTemplateId, variant, ThreadLocalRandom.current().nextInt(100));
         if (rare > 0) {
            ItemData item = ItemData.of(rare, 1);
            item.locked = true;
            World.dropPersonal(zone, mob.x(), mob.y(), player, item);
         }
      }
      for (ItemData item : MobRules.eliteLoot(variant, player.data().level, mob.spawn().level(),
            ThreadLocalRandom.current())) { // quà siêu quái: sách tiềm năng, đá khởi nguyên, đá nguyên tố (khoá, riêng)
         World.dropPersonal(zone, mob.x(), mob.y(), player, item);
      }
      if (variant == MobRules.VARIANT_ELITE || variant == MobRules.VARIANT_BOSS) { // Trung thu: nguyên liệu bánh (riêng)
         for (ItemData item : com.tlnt.server.event.MidAutumnService.eliteMaterials(variant == MobRules.VARIANT_BOSS,
               ThreadLocalRandom.current())) {
            World.dropPersonal(zone, mob.x(), mob.y(), player, item);
         }
      }
      if (zone.linhMoc != null) {
         if (zone.mobBoss != null && zone.mobBoss.mobId == mob.id() && zone.mobBoss.lastHitId == 0) {
            zone.mobBoss.lastHitId = killer.id(); // Hắc thần: người kết liễu nhận quà
         }
         linhMocMobLoot(zone, killer, mob); // Rừng Linh Mộc: vàng khoá / Quả Linh Mộc
      }
      checkAltarCleared(zone);
   }

   /**
    * Mã ({@code world_boss.id}) của boss thế giới trong khu: người chơi giả {@code boss}, hoặc quái {@code mobId}; 0 = không
    * phải boss thế giới.
    */
   static int worldBossKey(Zone zone, AltarBoss boss, int mobId) {
      for (WorldBoss wb : zone.worldBosses) {
         if (boss != null ? wb.charBoss == boss : wb.isMob(mobId)) {
            return wb.request.key();
         }
      }
      return 0;
   }

   /**
    * Boss vừa chết: mọi người ĐÃ GÂY SÁT THƯƠNG lên nó ({@code ledger}) còn ở khu mà đang ở bước
    * {@link StepKind#COLLECT_BOSS} (16.2 Bùa linh mộc) -> 1 món rơi RIÊNG mỗi người (chỉ họ thấy / nhặt).
    */
   static void bossQuestDrops(Zone zone, DamageLedger ledger, float x, float y) {
      for (int id : ledger.players()) {
         WorldPlayer p = zone.find(id);
         ItemData item = p == null ? null : QuestEvents.bossDrop(p);
         if (item != null) {
            World.dropPersonal(zone, x, y + 50, p, item);
         }
      }
   }

   /** Quái {@code mobId} là boss thế giới dạng quái (Vệ thần Rừng xanh) đang đứng trong khu. */
   static boolean isWorldMobBoss(Zone zone, int mobId) {
      for (WorldBoss wb : zone.worldBosses) {
         if (wb.isMob(mobId)) {
            return true;
         }
      }
      return false;
   }

   // ------------------------------------------------------------------ boss thế giới

   /** Quà boss thế giới nằm dưới đất bấy lâu (chỉ người kết liễu nhặt được) rồi mới biến mất. */
   static final long WORLD_BOSS_LOOT_MS = 180_000;

   /**
    * Boss thế giới của khu vừa chết (mọi đường chết đều qua {@link #checkAltarCleared}): gỡ khỏi khu, phát quà 1 lần.
    * Người KẾT LIỄU ({@link WorldBoss#lastHitId}) còn ở khu -> quà rơi xuống đất, cả khu thấy nhưng CHỈ người đó nhặt được
    * ({@link #WORLD_BOSS_LOOT_MS}); không có người kết liễu (chảy máu, lệnh test) / người đó đã rời khu -> quà rơi tự do, ai
    * nhặt cũng được. Báo {@code listener} (thông báo toàn server + hẹn con sau). Luồng tick.
    */
   static void worldBossCheck(Zone zone) {
      List<WorldBoss> list = zone.worldBosses;
      for (int i = list.size() - 1; i >= 0; i--) {
         WorldBoss wb = list.get(i);
         if (wb.done || !wb.dead(zone)) {
            continue;
         }
         wb.done = true;
         list.remove(i);
         if (wb.mobAi != null && zone.mobBoss == wb.mobAi) {
            zone.mobBoss = null;
         }
         DamageLedger ledger = wb.ledger();
         if (wb.charBoss == null || wb.charBoss.thief == null) { // Cáo không phải boss nhiệm vụ
            bossQuestDrops(zone, ledger, wb.x(zone), wb.y(zone));
         }
         int killerId = wb.lastHitId; // người KẾT LIỄU nhận quà
         WorldPlayer killer = killerId == 0 ? null : zone.find(killerId);
         com.tlnt.server.achievement.AchievementEvents.worldBossKilled(killer, wb.request.key()); // thành tựu Vệ thần / Tinh linh
         List<ItemData> loot = wb.request.loot().get();
         float x = wb.x(zone);
         float y = wb.y(zone) + 50;
         if (wb.charBoss != null && wb.charBoss.thief != null) { // mini boss Cáo: không thông báo gì
            World.dropItems(zone, x, y, killerId, loot); // vàng khoá: người kết liễu ưu tiên, sau đó ai cũng nhặt
            if (killer != null) {
               com.tlnt.server.daily.DailyEvents.add(killer, com.tlnt.server.daily.DailyKind.FOX, 0, 1);
               com.tlnt.server.player.ActivityPoints.post(killer.session(), com.tlnt.server.player.ActivityPoints.FOX);
            }
         } else if (killer != null) {
            com.tlnt.server.player.ActivityPoints.post(killer.session(), wb.charBoss != null
                  ? com.tlnt.server.player.ActivityPoints.SPIRIT_BOSS : com.tlnt.server.player.ActivityPoints.GUARDIAN_BOSS); // Vệ thần 25 / Tinh linh 50
            World.dropOwnerOnly(zone, x, y, killerId, loot, WORLD_BOSS_LOOT_MS);
            killer.send(Packets.notice("Bạn đã kết liễu " + wb.request.name()
                  + "! Phần thưởng rơi dưới đất, chỉ bạn nhặt được."));
         } else {
            World.dropItems(zone, x, y, 0, loot); // không có người kết liễu ở khu: ai nhặt cũng được
         }
         try {
            wb.request.listener().killed(killerId, wb.lastHitName);
         } catch (RuntimeException e) {
            com.tlnt.server.util.Log.error("Loi bao boss the gioi chet", e);
         }
      }
   }

   /**
    * Boss thế giới dạng quái ({@link LinhMocMobBoss#world}) đánh {@code target} {@code percent}% của {@code attack}:
    * × 2200/(2200+Giáp) ±5% ({@link MobRules#tableHitsPlayer}), rồi bất tử / Thuốc Cứng cáp / Khiên vỏ sò. {@code attackAnim}
    * = op 20 sub 1 (client tự chọn skill1 / skill2), false = chỉ trừ máu (op 2 sub 3). @return HP còn
    */
   static long worldMobBossHit(Zone zone, int mobId, MapTemplate.MobInstance mob, WorldPlayer target, long attack,
         int percent, boolean attackAnim) {
      if (dodgesMob(target)) {
         return applyMobHit(zone, mobId, mob, target, 0, attackAnim); // né
      }
      int spread = ThreadLocalRandom.current().nextInt(-MobRules.MOB_DAMAGE_SPREAD_PERCENT,
            MobRules.MOB_DAMAGE_SPREAD_PERCENT + 1);
      long damage = MobRules.tableHitsPlayer(attack * percent / 100, target.stats().armor, spread);
      return applyMobHit(zone, mobId, mob, target, incoming(target, damage, true), attackAnim);
   }

   /**
    * Rễ cây của boss đốt {@code target}: {@code permille} phần nghìn HP tối đa (tối thiểu 1; bất tử / lá chắn / Khiên vỏ sò
    * vẫn có tác dụng), op 2 sub 3. Đang miễn nhiễm (Túi sữa dê — đã gỡ trói) thì thôi.
    */
   static long bossBurnsPlayer(Zone zone, AltarBoss boss, WorldPlayer target, int permille) {
      PlayerData d = target.data();
      if (ItemBuffs.active(d, ItemBuffs.OPTION_IMMUNE, System.currentTimeMillis())) {
         return 0;
      }
      long damage = incoming(target, Math.max(1, target.stats().maxHp * permille / 1000), false);
      long before = d.hp;
      long left = d.damageHp(damage, 0);
      target.invalidateSpawnPacket();
      zone.broadcast(WorldPackets.playerDamaged(target.id(), Math.max(0, before - left), Math.max(0, left)), null);
      absorb(target, Math.max(0, before - left));
      afterBossHit(zone, boss, target, left);
      return Math.max(0, before - left);
   }

   /** Skill boss 43 hút phần trăm HP hiện tại của mục tiêu; áp dụng khiên/kháng và trả lượng HP thật sự mất. */
   static long bossDrainsPlayer(Zone zone, AltarBoss boss, WorldPlayer target, int percent) {
      PlayerData d = target.data();
      if (boss.dead || target.removed || target.zone != zone || d.isDead() || percent <= 0) {
         return 0;
      }
      long raw = Math.max(1, d.hp * Math.min(100, percent) / 100);
      long before = d.hp;
      long left = d.damageHp(incoming(target, raw, false), 0);
      long drained = Math.max(0, before - left);
      if (drained > 0) {
         target.invalidateSpawnPacket();
         zone.broadcast(WorldPackets.playerDamaged(target.id(), drained, Math.max(0, left)), null);
         absorb(target, drained);
         afterBossHit(zone, boss, target, left);
      }
      return drained;
   }

   /** Lệnh test: hạ quái thường của khu (op 20 sub 3 cả khu), chừa {@code keep} con; hết quái thì như vừa diệt hết. */
   static void testKillAll(Zone zone, int keep) {
      ZoneMobs mobs = zone.mobs();
      long now = System.currentTimeMillis();
      for (int id = 0; id < mobs.size() && zone.altarAlive() > keep; id++) {
         long hp = mobs.hp(id);
         if (hp > 0) {
            mobs.damage(id, hp, now);
            zone.broadcast(WorldPackets.mobDamaged(id, hp, 0).toPacket(), null);
         }
      }
      AltarBoss boss = zone.boss;
      if (boss != null && !boss.dead && zone.altarAlive() > keep) {
         long hp = boss.hp;
         boss.damage(zone, hp);
         zone.broadcast(WorldPackets.playerDamaged(boss.id, hp, 0), null);
         boss.dropLoot(zone, zone.ownerId());
      }
      checkAltarCleared(zone);
   }

   /**
    * Tế đàn Thiên tích: con vừa chết là con cuối của map -> báo chủ khu (mở đường sang map sau / xong lượt). Gọi sau MỌI
    * cách quái chết (đòn, chảy máu, ảnh ảo).
    */
   private static void checkAltarCleared(Zone zone) {
      if (!zone.worldBosses.isEmpty()) {
         worldBossCheck(zone); // boss thế giới vừa chết?
      }
      if (zone.linhMoc != null) { // Rừng Linh Mộc: báo lượt (mở cửa map sau / xét hoàn thành) — 1 lần / khu
         linhMocBossRewards(zone);
         if (!zone.linhMocClearedSent && zone.linhMocCleared()) {
            zone.linhMocClearedSent = true;
            World.LinhMocHooks hooks = zone.map().world().linhMocHooks();
            if (hooks != null) {
               hooks.cleared(zone.linhMoc, zone.map().template().id());
            }
         }
         return;
      }
      if (!zone.map().isAltar() || !zone.altarCleared()) {
         return;
      }
      WorldPlayer owner = zone.find(zone.ownerId());
      World.AltarHooks hooks = zone.map().world().altarHooks();
      if (owner != null && hooks != null) {
         hooks.cleared(owner, zone.map().template().id());
      }
   }

   // ------------------------------------------------------------------ quà Rừng Linh Mộc

   /** {@code player} vừa gây {@code dealt} sát thương lên quái {@code mobId}: là boss quái của khu thì ghi sổ. */
   private static void recordBossDamage(Zone zone, WorldPlayer player, int mobId, long dealt) {
      LinhMocMobBoss boss = zone.mobBoss;
      if (boss != null && boss.mobId == mobId) {
         boss.damage.add(player.id(), player.data().name, dealt);
         int key = dealt > 0 ? worldBossKey(zone, null, mobId) : 0;
         if (key != 0) {
            QuestEvents.worldBossHit(player, key); // NV 16.1: đếm đòn trúng Vệ thần Rừng xanh
            com.tlnt.server.daily.DailyEvents.add(player, com.tlnt.server.daily.DailyKind.BOSS_HIT, key, 1); // NV hằng ngày "Chạm trán"
         }
         if (dealt > 0) {
            QuestEvents.bossHit(player, zone.map().template().id()); // NV 15.2: đếm đòn trúng Hắc thần (map 29)
         }
      }
   }

   /** Quái thường Rừng Linh Mộc bị hạ: tung {@code linhmoc.mobLoot} (vàng khoá, Quả Linh Mộc), người kết liễu ưu tiên. */
   private static void linhMocMobLoot(Zone zone, WorldPlayer killer, MapTemplate.MobInstance mob) {
      if (zone.mobBoss != null && zone.mobBoss.mobId == mob.id()) {
         return; // boss: quà riêng (linhMocBossRewards)
      }
      List<ItemData> items = LinhMocRules.rollLoot(LinhMocRules.config().mobLoot(), zone.linhMoc.mobLevel,
            ThreadLocalRandom.current());
      if (!items.isEmpty()) {
         World.dropItems(zone, mob.x(), mob.y(), killer.id(), items);
      }
   }

   /** Boss vừa chết (đường nào cũng tới đây qua {@link #checkAltarCleared}): phát quà 1 lần. */
   private static void linhMocBossRewards(Zone zone) {
      LinhMocMobBoss mobBoss = zone.mobBoss;
      if (mobBoss != null && !mobBoss.damage.rewarded && zone.mobs().alive(mobBoss.mobId) == null) {
         mobBoss.damage.rewarded = true;
         MapTemplate.MobInstance mob = zone.mobs().instance(mobBoss.mobId);
         bossQuestDrops(zone, mobBoss.damage, mob.x(), mob.y());
         rewardLinhMocBoss(zone, mob.x(), mob.y(), mobBoss.lastHitId, mobName(mob),
               LinhMocRules.config().blackGodLoot());
      }
      AltarBoss spirit = zone.boss;
      if (spirit != null && spirit.dead && !spirit.damageLedger.rewarded) {
         spirit.damageLedger.rewarded = true;
         bossQuestDrops(zone, spirit.damageLedger, spirit.x, spirit.y);
         rewardLinhMocBoss(zone, spirit.x, spirit.y, spirit.lastHitId, spirit.spec.name(),
               LinhMocRules.config().spiritLoot());
      }
   }

   /**
    * Quà boss Rừng Linh Mộc rơi XUỐNG ĐẤT quanh xác, cả khu thấy; người KẾT LIỄU ({@code topId}) được ưu tiên nhặt
    * {@code ZoneDrops.OWNER_MS} (15 s), sau đó ai cũng nhặt được ({@code topId} 0 = chết không do đòn của ai -> ai nhặt cũng
    * được). Mọi thành viên của lượt đang ở khu nhận điểm hoạt động ({@code LinhMocService}). Luồng tick.
    */
   private static void rewardLinhMocBoss(Zone zone, float x, float y, int topId, String bossName,
         List<com.tlnt.server.linhmoc.LinhMocConfig.Loot> table) {
      World.dropItems(zone, x, y + 50, topId,
            LinhMocRules.rollLoot(table, zone.linhMoc.mobLevel, ThreadLocalRandom.current()));
      List<Integer> present = new ArrayList<>();
      for (WorldPlayer p : zone.players()) {
         if (zone.linhMoc.memberIds.contains(p.id())) {
            present.add(p.id());
         }
      }
      World.LinhMocHooks hooks = zone.map().world().linhMocHooks();
      if (hooks != null) {
         hooks.bossKilled(zone.linhMoc, topId, bossName, present);
      }
   }

   // ------------------------------------------------------------------ boss Tế đàn (AltarBoss)

   /** Đòn nhắm 1 mục tiêu (+ các gói sau + chảy máu) vào boss. */
   private static void strikeBoss(Zone zone, WorldPlayer player, AltarBoss boss, Strike s) {
      if (!nearBoss(player, boss, s.rangeX(), s.rangeY())) {
         return;
      }
      long dealt = hitBossPacket(zone, player, boss, s.skillId(), s.hits()[0], s.status());
      skillLifesteal(player, dealt, s.lifestealPercent()); // 22 Xuyên Tâm Kích
      for (int i = 1; i < s.hits().length; i++) {
         long damage = s.hits()[i];
         zone.schedule(i * s.hitIntervalMs(), () -> {
            if (player.zone == zone && !boss.dead && !player.data().isDead()) {
               skillLifesteal(player,
                     hitBossPacket(zone, player, boss, s.skillId(), damage, s.status()), s.lifestealPercent());
            }
         });
      }
      Bleed bleed = s.bleed();
      if (bleed != null && bleed.damagePerTick() > 0) {
         long start = (s.hits().length - 1) * s.hitIntervalMs();
         for (int t = 1; t <= bleed.ticks(); t++) {
            zone.schedule(start + t * bleed.intervalMs(), () -> {
               if (player.zone == zone && !boss.dead) {
                  long[] r = hitBoss(zone, player, boss, bleed.damagePerTick(), false);
                  zone.broadcast(WorldPackets.playerDamaged(boss.id, r[0], r[1]), null);
                  checkAltarCleared(zone);
               }
            });
         }
      }
   }

   /** @return sát thương đã gây (0 = hụt). */
   private static long hitBossPacket(Zone zone, WorldPlayer player, AltarBoss boss, int skillId, long raw) {
      return hitBossPacket(zone, player, boss, skillId, raw, null);
   }

   private static long hitBossPacket(Zone zone, WorldPlayer player, AltarBoss boss, int skillId, long raw,
         Status skillStatus) {
      using(player, skillId);
      long[] r = hitBoss(zone, player, boss, raw, true, skillStatus);
      if (skillStatus != null && skillStatus.kind() == MobStatus.ROOT && r[0] > 0 && !boss.dead) {
         boss.applyStatus(zone, MobStatus.ROOT, skillStatus.durationMs(), System.currentTimeMillis());
      }
      zone.broadcast(WorldPackets.singleTargetHit(player.id(), skillId, WorldPackets.TARGET_PLAYER, boss.id, r[1], r[0],
            r[0] > 0 && player.lastCrit)
            .toPacket(), null);
      checkAltarCleared(zone);
      return r[0];
   }

   /**
    * 1 đòn người lên boss (sát thương thô kiểu đánh người — {@code CombatService.pvpDamage}): né theo (Né boss − Chính
    * xác), giảm theo {@link PvpMath} với chỉ số boss; hút HP / MP như đánh QUÁI (cả "Hút HP / MP quái" + Ấn linh), KHÔNG có
    * EXP (boss Tế đàn không cho EXP — thưởng = điểm hoạt động + quà rơi), boss quay sang đuổi + chat. Không phát gói. @return {sát thương, HP boss còn}
    */
   private static long[] hitBoss(Zone zone, WorldPlayer player, AltarBoss boss, long raw) {
      return hitBoss(zone, player, boss, raw, true);
   }

   /** Như trên; {@code canCrit} false = chảy máu (không chí mạng). */
   private static long[] hitBoss(Zone zone, WorldPlayer player, AltarBoss boss, long raw, boolean canCrit) {
      return hitBoss(zone, player, boss, raw, canCrit, null);
   }

   private static long[] hitBoss(Zone zone, WorldPlayer player, AltarBoss boss, long raw, boolean canCrit,
         Status skillStatus) {
      player.lastCrit = false;
      if (boss.dead) {
         return new long[] {0, 0};
      }
      long now = System.currentTimeMillis();
      boss.onHurt(zone, player, now);
      if (raw <= 0 || boss.dodges(player.stats().accuracy)) {
         return new long[] {0, boss.hp};
      }
      if (canCrit) { // Nanh Hổ răng kiếm / chí mạng (chảy máu không chí mạng)
         raw = critical(zone, player, targetKey(zone, KEY_BOSS, boss.id, 0), raw);
      }
      long damage = PvpMath.reduce(raw, player.stats(), boss.stats);
      if (boss.smart && !zone.map().noLevelGapPenalty()
            && player.data().level - boss.spec.level() >= MobRules.LOW_LEVEL_GAP) {
         // boss thế giới như quái: người cao hơn boss ≥ 10 cấp chỉ gây 20% (Lôi vực: không)
         damage = Math.max(1, damage * MobRules.LOW_LEVEL_EXP_DAMAGE_PERCENT / 100);
      }
      if (boss.weakened(now)) {
         damage = StatRules.weakened(damage); // Suy yếu: +20%
      }
      damage = boostVsMob(player, damage); // boss như quái: Thuốc Sức mạnh
      if (boss.thief != null) {
         damage = Math.min(damage, FoxThief.MAX_DAMAGE); // Tiểu Tinh linh Cáo: tối đa 10 HP / đòn (cả chữ hiện)
      }
      long before = boss.hp;
      long left = applyBossDamage(zone, player, boss, damage);
      long dealt = before - left;
      leech(player, dealt, true); // boss tính như quái: Hút HP / MP quái (đồ, Ngũ Linh, Ấn linh)
      if (left > 0 && dealt > 0) {
         MobStatus status = rollStatus(player); // Tim băng / Nhẫn u mộc
         if (status != null) {
            boss.applyStatus(zone, status, statusMs(status), now);
         }
         if (canCrit) {
            statProcsOnBoss(zone, player, boss, now, skillStatus); // cộng điểm từ skill với chỉ số cùng loại
         }
      }
      return new long[] {damage, left};
   }

   /**
    * Trừ {@code damage} (đã tính xong) vào boss + sổ sát thương, đếm nhiệm vụ, quà / người kết liễu. Dùng chung cho đòn
    * đánh, chảy máu, thiêu đốt, sấm sét, phản sát thương. Không phát gói. @return HP boss còn
    */
   private static long applyBossDamage(Zone zone, WorldPlayer player, AltarBoss boss, long damage) {
      if (System.currentTimeMillis() < boss.shieldUntil) { // khiên nước Tinh linh Biển cả
         int reduction = BossSkillData.damageReductionPercent(AltarBoss.SHIELD, AltarBoss.SHIELD_REDUCE_PERCENT);
         damage = damage * (100 - Math.min(100, Math.max(0, reduction))) / 100;
      }
      if (boss.thief != null) {
         damage = Math.min(damage, FoxThief.MAX_DAMAGE); // Tiểu Tinh linh Cáo: tối đa 10 HP / đòn (cả thiêu đốt, sấm sét, phản)
      }
      long before = boss.hp;
      long left = boss.damage(zone, damage);
      long dealt = before - left;
      boss.damageLedger.add(player.id(), player.data().name, dealt); // quà cho người gây nhiều sát thương nhất
      if (dealt > 0) {
         QuestEvents.bossHit(player, zone.map().template().id()); // NV 10.2: đếm đòn trúng boss
         int key = worldBossKey(zone, boss, -1);
         if (key != 0) {
            QuestEvents.worldBossHit(player, key); // NV 17.2: đếm đòn trúng Tinh Linh Rừng xanh
            com.tlnt.server.daily.DailyEvents.add(player, com.tlnt.server.daily.DailyKind.BOSS_HIT, key, 1);
         }
      }
      if (left == 0 && before > 0 && !boss.spec.drops().isEmpty()) {
         boss.dropLoot(zone, player.id());
      }
      if (left == 0 && before > 0) {
         if (boss.lastHitId == 0) {
            boss.lastHitId = player.id(); // người kết liễu (quà Tinh linh Thần mộc)
         }
         for (WorldBoss wb : zone.worldBosses) { // boss thế giới: người kết liễu nhận quà
            if (wb.charBoss == boss) {
               wb.killedBy(player);
            }
         }
      }
      if (left == 0 && before > 0 && zone.map().isAltar()) { // Rừng Linh Mộc: boss chết -> checkAltarCleared xét dọn map / hoàn thành
         World.AltarHooks hooks = zone.map().world().altarHooks();
         if (hooks != null) {
            hooks.bossKilled(player); // +điểm hoạt động (luồng session)
         }
      }
      return left;
   }

   /** Người đứng trong tầm đánh boss (+ độ lệch mạng; boss do server điều khiển nên không cộng phạm vi lang thang). */
   private static boolean nearBoss(WorldPlayer player, AltarBoss boss, int rangeX, int rangeY) {
      return Math.abs(player.x - boss.x) <= rangeX + HIT_SLACK_PX && Math.abs(player.y - boss.y) <= rangeY + HIT_SLACK_PX;
   }

   /** Boss đánh trúng {@code target} {@code percent}% (bảng cấp, giáp, chí mạng): trừ HP, ghi {@code boss.lastDamage}. */
   static long bossHitsPlayer(Zone zone, AltarBoss boss, WorldPlayer target, int percent) {
      PlayerData d = target.data();
      if (boss.missedBy(target.stats().evasion)) { // sát thương 0: client người bị đánh hiện "né", người khác "hụt"
         boss.lastDamage = 0;
         return d.hp;
      }
      long damage = incoming(target, boss.rollDamage(percent, target.stats().armor,
            target.stats().critResistPercent), true);
      long before = d.hp;
      long left = d.damageHp(damage, 0);
      boss.lastDamage = Math.max(0, before - left); // đòn kết liễu: chỉ đúng phần HP còn
      target.invalidateSpawnPacket();
      absorb(target, boss.lastDamage);
      long back = StatRules.reflected(boss.lastDamage, target.stats().reflectDamagePercent);
      if (back > 0) { // Phản sát thương: tick sau (đang trong lượt ra chiêu của boss)
         zone.schedule(1, () -> {
            if (!boss.dead && target.zone == zone) {
               dotBoss(zone, target, boss, back);
            }
         });
      }
      return left;
   }

   /** Sau gói trúng đòn boss: HP về 0 = bị hạ gục (hộp "Kiệt sức"). */
   static void afterBossHit(Zone zone, AltarBoss boss, WorldPlayer target, long left) {
      if (left > 0) {
         return;
      }
      target.clearPendingPosition();
      died(target, "Bạn đã bị " + boss.spec.name() + " hạ gục.");
      sendStamina(target);
   }

   // ------------------------------------------------------------------ quái đánh trả

   /**
    * Quái bị đánh thì đánh trả: nhớ mọi người đã đánh nó ({@code ZoneMobs.provoke}), mỗi nhịp cắn 1 người — người ĐÁNH
    * GẦN ĐÂY NHẤT ({@code ZoneMobs.pickTarget}). Đòn đầu sau {@link MobRules#MOB_FIRST_ATTACK_MS}, rồi mỗi
    * {@link MobRules#MOB_ATTACK_INTERVAL_MS} (game gốc: 3,5 s; boss Hắc thần 2 s + vòng chiêu). Gọi mỗi tick của khu.
    *
    * <p>Quái CHỈ quên danh sách khi nó chết. Người đang rời khu / thoát / bị hạ gục / chạy xa khỏi vùng quái chỉ bị bỏ
    * qua lúc đó (quay lại vùng quái là bị cắn tiếp); không ai cắn được thì quái đứng chờ. Vùng quái
    * ({@link #LEASH_X_PX} / {@link #LEASH_Y_PX} + phạm vi lang thang — server không biết vị trí thật của quái, client
    * tự cho đi lang thang quanh điểm sinh). Sát thương: {@link MobRules#mobHitsPlayer} (cấp + biến thể quái, Giáp người),
    * không quá HP còn; HP về 0 = bị hạ gục (hộp "Kiệt sức").
    */
   static void mobAttacks(Zone zone, long now) {
      ZoneMobs mobs = zone.mobs();
      if (!mobs.anyAggro()) {
         return;
      }
      for (int id = 0; id < mobs.size(); id++) {
         if (mobs.aggroTarget(id) == 0 || now < mobs.nextAttackAt(id)) {
            continue;
         }
         MapTemplate.MobInstance mob = mobs.alive(id);
         if (mob == null) {
            mobs.calm(id);
            continue;
         }
         if (mobs.disabled(id, now)) { // choáng / đóng băng: chờ hết rồi mới đánh tiếp
            mobs.scheduleNextAttack(id, mobs.disabledUntil(id));
            continue;
         }
         // Cắn người ĐÁNH GẦN ĐÂY NHẤT còn hợp lệ; người chạy xa / chết / rời khu chỉ bị bỏ qua lần này (quái nhớ tới khi chết)
         int targetId = mobs.pickTarget(id, playerId -> {
            WorldPlayer p = zone.find(playerId);
            return p != null && !p.removed && p.zone == zone && !p.data().isDead() && !stealthed(p) && inLeash(p, mob);
         });
         WorldPlayer target = targetId == 0 ? null : zone.find(targetId);
         if (target == null && mobs.nearbyAggro(id)) { // chủ động tấn công mà người đã đi xa: thôi đánh, quên luôn
            mobs.calm(id);
            continue;
         }
         if (target == null) { // lúc này không cắn được ai: xem lại sau ít lâu (boss Hắc thần: khoá người đứng gần)
            mobs.scheduleNextAttack(id, now + RECHECK_MS);
            if (zone.mobBoss != null && zone.mobBoss.mobId == id) {
               zone.mobBoss.lockNearest(zone, mob, now);
            }
            continue;
         }
         LinhMocMobBoss boss = zone.mobBoss;
         if (boss != null && boss.mobId == id) { // boss quái Hắc thần: nhịp riêng + vòng chiêu
            mobs.scheduleNextAttack(id, now + boss.intervalMs);
            boss.attack(zone, mob, target);
            continue;
         }
         mobs.scheduleNextAttack(id, now + MobRules.MOB_ATTACK_INTERVAL_MS);
         mobHitsPlayer(zone, id, mob, target);
      }
   }

   /**
    * Map quái chủ động tấn công ({@link Regions#aggressive}): mỗi {@link Regions#AGGRO_SCAN_MS} quái thường đang rảnh (không
    * phải boss thế giới / quái săn bắt / boss Hắc thần, không bị choáng) thấy người còn sống đứng gần điểm sinh (cộng phạm vi
    * lang thang) thì khoá người gần nhất ({@code ZoneMobs.provokeNearby}) — đòn đầu sau {@link MobRules#MOB_FIRST_ATTACK_MS}
    * như bị đánh. Người đi ra khỏi vùng quái ({@link #inLeash}) thì quái thôi ({@link #mobAttacks}). Luồng tick khu.
    */
   static void mobAggro(Zone zone, long now) {
      if (!MobRules.mobAttack() || zone.players().isEmpty()) {
         return;
      }
      ZoneMobs mobs = zone.mobs();
      for (int id = 0; id < mobs.size(); id++) {
         if (mobs.aggroTarget(id) != 0 || mobs.isManual(id) || mobs.isGather(id) || mobs.disabled(id, now)
               || (zone.mobBoss != null && zone.mobBoss.mobId == id)) {
            continue;
         }
         MapTemplate.MobInstance mob = mobs.alive(id);
         if (mob == null) {
            continue;
         }
         float wander = wanderRange(mob);
         WorldPlayer nearest = null;
         float best = Float.MAX_VALUE;
         for (WorldPlayer p : zone.players()) {
            if (p.removed || p.locked || p.zone != zone || p.data().isDead() || p.data().bot) {
               continue; // bot: quái chỉ đánh trả khi bot đánh nó (bot đứng làng không bị quái lao vào)
            }
            float dx = Math.abs(p.x - mob.x());
            float dy = Math.abs(p.y - mob.y());
            if (dx <= Regions.AGGRO_X_PX + wander && dy <= Regions.AGGRO_Y_PX + wander / 2 && dx + dy < best) {
               nearest = p;
               best = dx + dy;
            }
         }
         if (nearest != null) {
            mobs.provokeNearby(id, nearest.id(), now);
         }
      }
   }

   /** Người chơi còn trong vùng quái đuổi theo được không. */
   static boolean inLeash(WorldPlayer player, MapTemplate.MobInstance mob) {
      float wander = wanderRange(mob);
      return Math.abs(player.x - mob.x()) <= LEASH_X_PX + wander && Math.abs(player.y - mob.y()) <= LEASH_Y_PX + wander / 2;
   }

   /** Quái đuổi / bắn được tới đây quanh điểm sinh (cộng phạm vi lang thang); xa hơn thì quái bỏ cuộc. */
   private static final float LEASH_X_PX = 900f;
   private static final float LEASH_Y_PX = 500f;
   /** Quái nhớ người nhưng lúc này không cắn được ai (chạy xa / chết / rời khu): bấy lâu xem lại. */
   private static final long RECHECK_MS = 500;

   private static void mobHitsPlayer(Zone zone, int mobId, MapTemplate.MobInstance mob, WorldPlayer target) {
      if (dodgesMob(target)) {
         applyMobHit(zone, mobId, mob, target, 0, true); // né: sát thương 0
         return;
      }
      int spread = ThreadLocalRandom.current().nextInt(-MobRules.MOB_DAMAGE_SPREAD_PERCENT,
            MobRules.MOB_DAMAGE_SPREAD_PERCENT + 1);
      long damage;
      if (zone.linhMoc != null && LinhMocRules.hasMob(mob.spawn().id())) {
         // Rừng Linh Mộc: hệ số ST × u × giáp × rand(0.95, 1.05) × (1 − giảm ST thuốc), double tới bước cuối
         damage = LinhMocRules.mobHitsPlayer(mob.spawn().id(), zone.linhMoc.unit, target.stats().armor,
               target.stats().mobDamageTakenReducePercent, ThreadLocalRandom.current().nextDouble(0.95, 1.05));
         damage = incoming(target, damage, false); // bất tử / Khiên vỏ sò (thuốc đã tính ở trên)
      } else {
         damage = zone.map().isAltar() // Cận vệ Thiên tích: sát thương gốc theo bảng cấp
               ? MobRules.tableHitsPlayer(AltarRules.baseDamage(mob.spawn().level()), target.stats().armor, spread)
               : MobRules.mobHitsPlayer(mob.spawn().level(), mob.spawn().variant(), target.stats().armor, spread);
         damage = incoming(target, damage, true); // bất tử / Thuốc Cứng cáp / Khiên vỏ sò
      }
      long left = applyMobHit(zone, mobId, mob, target, damage, true);
      if (left > 0 && Regions.freezes(zone.map().template().id()) && !target.stats().coldResist
            && ThreadLocalRandom.current().nextInt(1_000_000) < Regions.FREEZE_PER_MILLION) {
         bossStatusPlayer(zone, target, MobStatus.ICE, Regions.FREEZE_MS); // Frearh: cái lạnh đóng băng (0,1% / đòn)
      }
   }

   /**
    * Boss quái Rừng Linh Mộc ({@link LinhMocMobBoss}) đánh {@code target} {@code percent}% đòn thường (hệ số ST × u ×
    * giáp × rand × thuốc, rồi bất tử / Khiên vỏ sò). {@code attackAnim} = op 20 sub 1 (client chơi skill1 / skill2),
    * false = chỉ trừ máu (op 2 sub 3 — animation skill3 / skill4 đã phát). @return HP còn
    */
   static long linhMocBossHit(Zone zone, int mobId, MapTemplate.MobInstance mob, WorldPlayer target, int percent,
         boolean attackAnim) {
      if (dodgesMob(target)) {
         return applyMobHit(zone, mobId, mob, target, 0, attackAnim); // né
      }
      long damage = LinhMocRules.mobHitsPlayer(mob.spawn().id(), zone.linhMoc.unit * percent / 100.0,
            target.stats().armor, target.stats().mobDamageTakenReducePercent,
            ThreadLocalRandom.current().nextDouble(0.95, 1.05));
      return applyMobHit(zone, mobId, mob, target, incoming(target, damage, false), attackAnim);
   }

   /** Trừ HP người bị quái đánh + phát gói (op 20 sub 1 có animation / op 2 sub 3 chỉ mất máu) + xử lý bị hạ gục. */
   private static long applyMobHit(Zone zone, int mobId, MapTemplate.MobInstance mob, WorldPlayer target, long damage,
         boolean attackAnim) {
      PlayerData d = target.data();
      long before = d.hp;
      long left = d.damageHp(damage, 0);
      long shown = Math.max(0, before - left); // đòn kết liễu: chỉ đúng phần HP còn (như game gốc)
      target.invalidateSpawnPacket(); // gói xuất hiện có HP
      zone.broadcast(attackAnim ? WorldPackets.mobAttack(mobId, target.id(), left, shown).toPacket()
            : WorldPackets.playerDamaged(target.id(), shown, Math.max(0, left)), null);
      absorb(target, shown);
      long back = StatRules.reflected(shown, target.stats().reflectDamagePercent);
      if (back > 0) { // Phản sát thương: tick sau (đang trong vòng quái đánh) — người phản kết liễu thì được tính
         int life = zone.mobs().deaths(mobId);
         zone.schedule(1, () -> {
            if (zone.mobs().deaths(mobId) == life && zone.mobs().alive(mobId) != null) {
               bleedTick(zone, target, mobId, back);
            }
         });
      }
      if (left <= 0) {
         target.clearPendingPosition();
         died(target, "Bạn đã bị " + mobName(mob) + " hạ gục.");
         sendStamina(target);
         if (target.duelOpponentId != 0) {
            World.endDuel(zone, target, "Thách đấu bị huỷ.");
         }
      }
      return left;
   }

   /** Thanh thể lực (op 23 sub 8) sau khi đổi — bị hạ gục mất {@link PlayerData#STAMINA_DEATH_LOSS}. */
   private static void sendStamina(WorldPlayer player) {
      player.send(WorldPackets.stamina(player.data().activityPoints, player.stats().maxStamina));
   }

   private static String mobName(MapTemplate.MobInstance mob) {
      MapPack.Mob template = GameDataCache.mob(mob.spawn().id());
      return template == null ? "quái" : template.name();
   }

   /**
    * EXP theo sát thương thật của 1 đòn ({@link Leveling#mobHitExp}): gửi op 23 sub 4 (client cộng + chữ "+N") cho
    * người đánh; lên cấp -> tính lại chỉ số, phát op 23 sub 5 + khối chỉ số cho cả khu (đang ở luồng tick khu).
    */
   private static void gainMobExp(Zone zone, WorldPlayer player, MapTemplate.MobInstance mob, long dealt) {
      if (isWorldMobBoss(zone, mob.id())) { // boss thế giới (game gốc Vệ thần Rừng xanh): 1 EXP mỗi đòn, kể cả có +EXP%
         if (dealt > 0 && !player.data().isDead()) {
            grantExp(zone, player, 1);
         }
         return;
      }
      gainExp(zone, player, mob.spawn().level(), mob.spawn().variant(), dealt);
   }

   /** EXP 1 đòn lên quái cấp {@code mobLevel}, biến thể {@code variant}. */
   private static void gainExp(Zone zone, WorldPlayer player, int mobLevel, int variant, long dealt) {
      PlayerData d = player.data();
      if (d.isDead()) {
         return;
      }
      long exp = zone.linhMoc != null // Rừng Linh Mộc: floor(sát thương × exp.dungeon × cấp) × (1 + EXP%), không chia
            ? LinhMocRules.hitExp(dealt, mobLevel, player.stats().expBonusPercent, MobRules.expRate())
            : Leveling.mobHitExp(dealt, d.level, mobLevel, variant, player.stats().expBonusPercent,
                  !zone.map().noLevelGapPenalty());
      if (exp <= 0) {
         return;
      }
      grantExp(zone, player, exp);
   }

   /** Cộng {@code exp} (op 23 sub 4); lên cấp -> tính lại chỉ số + phát cho cả khu. */
   private static void grantExp(Zone zone, WorldPlayer player, long exp) {
      PlayerData d = player.data();
      int gained = Leveling.addExp(d, exp);
      player.send(ItemPackets.expGain(exp));
      if (gained > 0) {
         ItemService.refreshLook(d); // chỉ số gốc theo cấp
         zone.broadcast(ItemPackets.levelUp(d.getId(), d.level, d.exp, PlayerStats.expToNextLevel(d.level))
               .toPacket(), null);
         player.invalidateSpawnPacket();
         zone.broadcast(WorldPackets.playerStats(player), null);
         zone.sendPartyFlags(player);
         zone.syncPet(player);
         QuestEvents.levelChanged(player); // bước "Đạt cấp"
         com.tlnt.server.achievement.AchievementEvents.add(player, com.tlnt.server.achievement.AchievementType.LEVEL, d.level);
      }
   }

   /**
    * 1 đòn trúng quái săn bắt: -1 HP; {@code sting} > 0 (tổ ong) -> người đánh mất {@code sting}% HP tối đa (có thể
    * kiệt sức). Phá xong -> đồ RƠI xuống đất ({@link World#dropItems}), người phá được ưu tiên nhặt (không hút máu).
    */
   private static long hitGather(Zone zone, WorldPlayer player, ZoneMobs.Gather gather, long now) {
      GatherConfig.Entry entry = gather.entry;
      long left = zone.mobs().damage(gather.mob.id(), 1, now);
      PlayerData d = player.data();
      long sting = entry.stingPercent() > 0 && !d.isDead()
            ? incoming(player, Math.max(1, player.stats().maxHp * entry.stingPercent() / 100), true) : 0;
      if (sting > 0) {
         long hpLeft = d.damageHp(sting, 0);
         player.invalidateSpawnPacket();
         zone.broadcast(WorldPackets.playerDamaged(player.id(), sting, hpLeft), null);
         if (hpLeft <= 0) {
            player.clearPendingPosition();
            died(player, "Bạn bị ong đốt đến kiệt sức.");
            sendStamina(player);
         }
      }
      if (left <= 0) {
         if (entry.stingPercent() > 0) { // tổ ong (quả dừa không đốt)
            com.tlnt.server.achievement.AchievementEvents.add(player, com.tlnt.server.achievement.AchievementType.BEEHIVE, 1);
         }
         com.tlnt.server.player.ActivityPoints.post(player.session(), entry.stingPercent() > 0
               ? com.tlnt.server.player.ActivityPoints.BEEHIVE : com.tlnt.server.player.ActivityPoints.COCONUT); // phá tổ ong / quả dừa
         List<ItemData> items = GatherService.roll(entry, now, ThreadLocalRandom.current().nextInt(100));
         World.dropItems(zone, gather.mob.x(), gather.mob.y(), player.id(), items);
      }
      return left;
   }

   /**
    * Hút HP / MP: % "Hút HP / MP" (chỉ khi đánh NGƯỜI) hoặc % "Hút HP / MP quái" (chỉ khi đánh QUÁI / boss) của sát thương gây ra. HP / MP
    * ghi có khoá ({@link PlayerData#healHp}) vì luồng session cũng sửa.
    */
   private static void leech(WorldPlayer player, long dealt, boolean vsMob) {
      PlayerStats s = player.stats();
      // Hút HP / MP = CHỈ khi đánh người; Hút HP / MP quái = CHỈ khi đánh quái (boss Tế đàn tính như quái). Không cộng chéo.
      long hpPercent = vsMob ? s.hpLeechVsMobPercent : s.hpLeechPercent;
      long mpPercent = vsMob ? s.mpLeechVsMobPercent : s.mpLeechPercent;
      if (dealt <= 0 || (hpPercent <= 0 && mpPercent <= 0)) {
         return;
      }
      PlayerData d = player.data();
      if (d.isDead() || (d.hp >= s.maxHp && d.mp >= s.maxMp)) {
         return;
      }
      d.healHp(dealt * hpPercent / 100, s.maxHp);
      d.healMp(dealt * mpPercent / 100, s.maxMp);
      player.send(ItemPackets.hpMp(player.id(), d.hp, s.maxHp, d.mp, s.maxMp));
   }

   // ------------------------------------------------------------------ hiệu ứng vật phẩm / phụ kiện

   /** Loại mục tiêu trong key "đòn đầu tiên" (Nanh Hổ răng kiếm). */
   private static final int KEY_MOB = 1;
   private static final int KEY_PLAYER = 2;
   private static final int KEY_BOSS = 3;
   /** Giảm sát thương nhận vào từ quái tối đa (Thuốc Cứng cáp). */
   static final int MAX_MOB_TAKEN_REDUCE_PERCENT = 90;

   /**
    * Key 1 mục tiêu cho "đòn đầu tiên": loại + (map, khu với quái / boss) + id + lượt sống (quái chết hồi sinh / người hồi
    * sinh = mục tiêu mới). Người chơi không gắn khu (đổi khu vẫn là cùng mục tiêu).
    */
   static long targetKey(Zone zone, int kind, int id, int life) {
      long h = kind;
      h = h * 1_000_003L + (zone == null ? 0 : zone.map().template().id());
      h = h * 1_000_003L + (zone == null ? 0 : zone.id());
      h = h * 1_000_003L + id;
      return h * 1_000_003L + life;
   }

   /**
    * Thuốc Sức mạnh "Tăng X% Sức tấn công lên quái": KHÔNG phải chỉ số — đòn đánh lên quái / boss gây bao nhiêu (đã trừ
    * giáp, chênh cấp) thì cộng thêm X% chính số đó: {@code damage} × (100 + X) / 100. Vd đòn 338 + 35% = 456.
    */
   static long boostVsMob(WorldPlayer player, long damage) {
      long percent = player.stats().mobDamagePercent;
      return percent > 0 && damage > 0 ? damage + damage * percent / 100 : damage;
   }

   /**
    * Chí mạng cho 1 đòn của {@code player} (luồng tick): Nanh Hổ răng kiếm (đòn đầu lên mục tiêu, chắc chắn — {@link #fang})
    * hoặc tỉ lệ = chỉ số Chí mạng / 10000 (750 -> 7,5%, như boss: 2000 = 20%), sát thương × (100 + Sát thương chí mạng% −
    * Kháng sát thương chí mạng% của mục tiêu) / 100 (gốc 80-85% -> ×1,8). Ghi {@code player.lastCrit} cho gói trúng đòn
    * (client hiện chữ chí mạng).
    */
   static long critical(Zone zone, WorldPlayer player, long key, long raw) {
      return critical(zone, player, key, raw, 0);
   }

   /** Như trên; {@code critResist} = Kháng sát thương chí mạng (%) của người bị đánh (quái / boss: 0). */
   static long critical(Zone zone, WorldPlayer player, long key, long raw, long critResist) {
      player.lastCrit = false;
      if (raw <= 0) {
         return raw;
      }
      raw = TribeRules.bloodRage(player.data(), player.stats(), raw); // kỹ năng bộ lạc 1: HP dưới 50% -> +10%
      if (NO_CRIT_SKILLS.contains(player.hitSkillId)) {
         return raw; // chiêu cuối: không chí mạng (cả Nanh Hổ — không tốn lượt)
      }
      long fanged = fang(zone, player, key, raw, critResist);
      if (fanged != raw) {
         player.lastCrit = true;
         return fanged;
      }
      PlayerStats s = player.stats();
      if (s.critChance > 0 && ThreadLocalRandom.current().nextInt(CRIT_SCALE) < s.critChance) {
         player.lastCrit = true;
         long critPercent = s.critDamagePercent + TribeRules.weakPointCrit(player.data(), s); // bộ lạc 9: HP trên 50%
         return raw + raw * StatRules.critBonus(critPercent, critResist) / 100;
      }
      return raw;
   }

   /**
    * Chiêu cuối của 5 hệ — KHÔNG BAO GIỜ chí mạng: 7 Thịnh Nộ Trảm, 13 Tuyệt Diệt Tiễn, 19 Thần Phong Nỗ, 25 Mâu Thần Kích,
    * 31 Kình Lực Phách.
    */
   static final java.util.Set<Integer> NO_CRIT_SKILLS = java.util.Set.of(7, 13, 19, 25, 31);

   /** Ghi kỹ năng của đòn sắp tính (mọi lối vào đòn đánh gọi trước khi tính sát thương — {@link #critical} đọc). */
   static void using(WorldPlayer player, int skillId) {
      player.hitSkillId = skillId;
   }

   /** Thang tỉ lệ chí mạng: chỉ số Chí mạng / {@value} (phần vạn). */
   static final int CRIT_SCALE = 10_000;

   /**
    * Nanh Hổ răng kiếm: đòn ĐẦU TIÊN lên mục tiêu {@code key} chắc chắn chí mạng = thô × (100 + Sát thương chí mạng% +
    * {@link GearCharges#FANG_CRIT_BONUS}) / 100, mỗi lần -1 lượt. Người đánh phải đang ở đúng khu (ảnh ảo đánh sau khi chủ
    * đã đi khu khác thì thôi — tập mục tiêu của người chơi chỉ luồng khu đang chứa họ được đụng).
    */
   static long fang(Zone zone, WorldPlayer player, long key, long raw) {
      return fang(zone, player, key, raw, 0);
   }

   static long fang(Zone zone, WorldPlayer player, long key, long raw, long critResist) {
      if (zone == null || player.zone != zone || raw <= 0 || !player.data().gear.has(GearCharges.Kind.FANG)
            || !player.firstHit(key) || GearCharges.use(player, GearCharges.Kind.FANG, 1) <= 0) {
         return raw;
      }
      long crit = Math.max(0, player.stats().critDamagePercent) + GearCharges.FANG_CRIT_BONUS;
      return raw + raw * StatRules.critBonus(crit, critResist) / 100;
   }

   /**
    * Sát thương {@code target} THỰC nhận (sau giáp): bất tử (Bùa linh mộc) = 0; Suy yếu +20%; từ quái giảm theo Thuốc Cứng cáp
    * (tối đa {@value #MAX_MOB_TAKEN_REDUCE_PERCENT}%); Khiên vỏ sò chặn {@link GearCharges#SHIELD_PERCENT}% phần còn lại,
    * phần chặn được trừ vào độ bền (độ bền còn ít hơn thì chỉ chặn bằng đó); Kháng sát thương trừ thẳng (mọi nguồn).
    */
   static long incoming(WorldPlayer target, long damage, boolean fromMob) {
      if (damage <= 0) {
         return 0;
      }
      PlayerData d = target.data();
      if (System.currentTimeMillis() < d.invulnerableUntil) {
         return 0;
      }
      if (System.currentTimeMillis() < d.weakenedUntil) { // Suy yếu: nhận thêm 20% từ mọi nguồn
         damage = StatRules.weakened(damage);
      }
      if (d.skillShieldPercent > 0 && System.currentTimeMillis() < d.skillShieldUntil) { // Lá Chắn Tiễn / Trụ Tái Tạo
         damage -= damage * Math.min(100, d.skillShieldPercent) / 100;
         if (damage <= 0) {
            return 0; // giảm 100%: không mất máu (client hiện "né")
         }
      }
      if (fromMob) {
         long reduce = Math.min(MAX_MOB_TAKEN_REDUCE_PERCENT, Math.max(0, target.stats().mobDamageTakenReducePercent));
         damage -= damage * reduce / 100;
      }
      damage = TribeRules.endurance(d, target.stats(), damage); // kỹ năng bộ lạc 10: -2% mỗi 10% HP đã mất
      damage = StatRules.afterResist(damage, target.stats().damageResist); // Kháng sát thương: trừ thẳng, còn ≥ 1
      if (d.gear.has(GearCharges.Kind.SHIELD)) {
         long block = damage * GearCharges.SHIELD_PERCENT / 100;
         damage -= GearCharges.use(target, GearCharges.Kind.SHIELD, block);
      }
      return Math.max(1, damage);
   }

   // ------------------------------------------------------------------ chỉ số: hấp thụ / phản / né / hiệu ứng

   /** Hấp thụ sát thương: vừa nhận {@code taken} -> hồi X% vào HP và X% vào MP (tối đa 80%). */
   static void absorb(WorldPlayer target, long taken) {
      PlayerStats s = target.stats();
      long heal = StatRules.absorbed(taken, s.damageAbsorbPercent);
      PlayerData d = target.data();
      if (heal <= 0 || d.isDead()) {
         return;
      }
      d.healHp(heal, s.maxHp);
      d.healMp(heal, s.maxMp);
      target.send(ItemPackets.hpMp(target.id(), d.hp, s.maxHp, d.mp, s.maxMp));
   }

   /** Người nhận đòn né đòn quái: Né / 10000 (quái không có Chính xác), tối đa 75%. */
   private static boolean dodgesMob(WorldPlayer target) {
      return StatRules.dodged(target.stats().evasion, 0, ThreadLocalRandom.current().nextInt(StatRules.SCALE));
   }

   /** Phản sát thương PvP: {@code victim} dội X% sát thương vừa nhận lên {@code attacker} (tick sau, không dội tiếp). */
   private static void reflectToPlayer(Zone zone, WorldPlayer victim, WorldPlayer attacker, long taken) {
      long back = StatRules.reflected(taken, victim.stats().reflectDamagePercent);
      if (back > 0) {
         zone.schedule(1, () -> dotPlayer(zone, victim, attacker, back));
      }
   }

   /**
    * Sát thương "thẳng" của {@code source} lên người {@code target} (thiêu đốt, sấm sét, phản sát thương): không né / chí
    * mạng / hiệu ứng / phản, vẫn qua {@link #incoming} + Hấp thụ; HP về 0 -> {@code source} hạ gục (thách đấu: dừng ở 1).
    */
   static void dotPlayer(Zone zone, WorldPlayer source, WorldPlayer target, long raw) {
      PlayerData t = target.data();
      if (raw <= 0 || target.removed || target.zone != zone || t.isDead()) {
         return;
      }
      boolean duel = source.dueling(target);
      long damage = incoming(target, raw, false);
      long before = t.hp;
      long left = t.damageHp(damage, duel ? 1 : 0);
      long taken = Math.max(0, before - left);
      target.invalidateSpawnPacket();
      zone.broadcast(WorldPackets.playerDamaged(target.id(), taken, Math.max(0, left)), null);
      absorb(target, taken);
      afterHit(zone, source, target, new PvpHit(taken, left, duel));
   }

   /** Sát thương thẳng lên boss người chơi giả (thiêu đốt, sấm sét, phản): op 2 sub 3 + xét boss chết. */
   private static void dotBoss(Zone zone, WorldPlayer player, AltarBoss boss, long damage) {
      if (boss.dead || damage <= 0) {
         return;
      }
      long before = boss.hp;
      long left = applyBossDamage(zone, player, boss, damage);
      zone.broadcast(WorldPackets.playerDamaged(boss.id, before - left, left), null);
      checkAltarCleared(zone);
   }

   private static boolean roll(long points) {
      return StatRules.procs(points, ThreadLocalRandom.current().nextInt(StatRules.SCALE));
   }

   /** Cộng điểm status của skill với chỉ số cùng loại, rồi tung đúng một lần trên thang 10.000. */
   private static long combinedStatusPoints(PlayerStats stats, MobStatus kind, Status skillStatus) {
      long points = switch (kind) {
         case STUN -> stats.stunDamage;
         case WEAKEN -> stats.weakenDamage;
         case ICE -> stats.iceDamage;
         case BURN -> stats.burnDamage;
         case LIGHTNING -> stats.thunderDamage;
         default -> 0;
      };
      if (skillStatus != null && skillStatus.kind() == kind) {
         points += Math.max(0, skillStatus.chancePoints());
      }
      return Math.min(StatRules.SCALE, Math.max(0, points));
   }

   /**
    * Hiệu ứng từ chỉ số của {@code player} sau 1 đòn trúng quái còn sống: choáng / băng (không đánh trả) + suy yếu (+20%
    * sát thương nhận) {@value StatRules#STATUS_MS_MOB} ms (cả boss dạng quái), thiêu đốt 5 s (boss dạng quái 3 s), sấm sét.
    * Đang dính thì không dính lại. Op 35 bản quái cho cả khu.
    */
   private static void statProcsOnMob(Zone zone, WorldPlayer player, int mobId, MapTemplate.MobInstance mob,
         Status skillStatus) {
      PlayerStats s = player.stats();
      if (s.stunDamage <= 0 && s.iceDamage <= 0 && s.weakenDamage <= 0 && s.burnDamage <= 0 && s.thunderDamage <= 0
            && skillStatus == null) {
         return;
      }
      ZoneMobs.Effects e = zone.mobs().effects(mobId);
      if (e == null) {
         return;
      }
      long now = System.currentTimeMillis();
      int ms = StatRules.STATUS_MS_MOB;
      if (now >= e.disabledUntil && roll(combinedStatusPoints(s, MobStatus.STUN, skillStatus))) {
         e.disabledUntil = now + ms;
         zone.broadcast(WorldPackets.mobStatus(MobStatus.STUN, mobId, ms).toPacket(), null);
      } else if (now >= e.disabledUntil && roll(combinedStatusPoints(s, MobStatus.ICE, skillStatus))) {
         e.disabledUntil = now + ms;
         zone.broadcast(WorldPackets.mobStatus(MobStatus.ICE, mobId, ms).toPacket(), null);
      }
      if (now >= e.weakenedUntil && roll(combinedStatusPoints(s, MobStatus.WEAKEN, skillStatus))) {
         e.weakenedUntil = now + ms;
         zone.broadcast(WorldPackets.mobStatus(MobStatus.WEAKEN, mobId, ms).toPacket(), null);
      }
      if (now >= e.burningUntil && roll(combinedStatusPoints(s, MobStatus.BURN, skillStatus))) {
         boolean boss = isWorldMobBoss(zone, mobId) || (zone.mobBoss != null && zone.mobBoss.mobId == mobId);
         int burnMs = boss ? StatRules.BURN_MS_BOSS_OR_PLAYER : StatRules.BURN_MS_MOB;
         e.burningUntil = now + burnMs;
         zone.broadcast(WorldPackets.mobStatus(MobStatus.BURN, mobId, burnMs).toPacket(), null);
         long tick = StatRules.burnPerTick(s.attackPower);
         int life = zone.mobs().deaths(mobId);
         for (int t = 1; t <= burnMs / StatRules.BURN_TICK_MS; t++) {
            zone.schedule((long) t * StatRules.BURN_TICK_MS, () -> {
               if (zone.mobs().deaths(mobId) == life) { // quái chết rồi hồi sinh: thôi
                  bleedTick(zone, player, mobId, tick);
               }
            });
         }
      }
      if (roll(combinedStatusPoints(s, MobStatus.LIGHTNING, skillStatus))) { // 1 đòn thường qua giáp, không chí mạng
         zone.broadcast(WorldPackets.mobStatus(MobStatus.LIGHTNING, mobId, 0).toPacket(), null);
         long damage = MobRules.playerHitsMob(s.attackPower + s.attackVsMob, player.data().level, mob.spawn().level(),
               s.armorPen, !zone.map().noLevelGapPenalty());
         if (zone.mobs().weakened(mobId, now)) {
            damage = StatRules.weakened(damage);
         }
         long thunder = boostVsMob(player, damage);
         int life = zone.mobs().deaths(mobId);
         zone.schedule(1, () -> {
            if (zone.mobs().deaths(mobId) == life) {
               bleedTick(zone, player, mobId, thunder);
            }
         });
      }
   }

   /**
    * Hiệu ứng từ chỉ số của {@code attacker} sau 1 đòn trúng người còn sống: choáng / băng (không ra chiêu, client khoá
    * di chuyển), suy yếu (chậm 20% + nhận thêm 20%), thiêu đốt — {@value StatRules#STATUS_MS_PLAYER} ms trừ Kháng hiệu ứng
    * của người bị đánh; sấm sét. Bất tử / Túi sữa dê (miễn nhiễm) thì không dính. Đang dính thì không dính lại.
    */
   private static void statProcsOnPlayer(Zone zone, WorldPlayer attacker, WorldPlayer target, Status skillStatus) {
      PlayerStats s = attacker.stats();
      if (s.stunDamage <= 0 && s.iceDamage <= 0 && s.weakenDamage <= 0 && s.burnDamage <= 0 && s.thunderDamage <= 0
            && skillStatus == null) {
         return;
      }
      PlayerData t = target.data();
      long now = System.currentTimeMillis();
      if (t.isDead() || now < t.invulnerableUntil || ItemBuffs.active(t, ItemBuffs.OPTION_IMMUNE, now)) {
         return;
      }
      long resist = target.stats().effectResistPercent;
      int ms = StatRules.afterEffectResist(StatRules.STATUS_MS_PLAYER, resist);
      boolean locked = now < t.stunnedUntil || now < t.frozenUntil;
      if (ms > 0 && !locked && roll(combinedStatusPoints(s, MobStatus.STUN, skillStatus))) {
         t.stunnedUntil = now + ms;
         zone.broadcast(WorldPackets.playerStatus(MobStatus.STUN, target.id(), ms).toPacket(), null);
         debuffIcon(target, ItemBuffs.OPTION_STUN, now + ms);
      } else if (ms > 0 && !locked && roll(combinedStatusPoints(s, MobStatus.ICE, skillStatus))) {
         int iceMs = iceMs(target, ms);
         if (iceMs > 0) {
            t.frozenUntil = now + iceMs;
            zone.broadcast(WorldPackets.playerStatus(MobStatus.ICE, target.id(), iceMs).toPacket(), null);
            debuffIcon(target, ItemBuffs.OPTION_FROZEN, now + iceMs);
         }
      }
      if (ms > 0 && now >= t.weakenedUntil && roll(combinedStatusPoints(s, MobStatus.WEAKEN, skillStatus))) {
         weakenPlayer(zone, target, ms, now);
      }
      int burnMs = StatRules.afterEffectResist(StatRules.BURN_MS_BOSS_OR_PLAYER, resist);
      if (burnMs >= StatRules.BURN_TICK_MS && now >= t.burningUntil
            && roll(combinedStatusPoints(s, MobStatus.BURN, skillStatus))) {
         long until = now + burnMs;
         t.burningUntil = until;
         zone.broadcast(WorldPackets.playerStatus(MobStatus.BURN, target.id(), burnMs).toPacket(), null);
         debuffIcon(target, ItemBuffs.OPTION_BURN, until);
         for (int k = 1; k <= burnMs / StatRules.BURN_TICK_MS; k++) {
            zone.schedule((long) k * StatRules.BURN_TICK_MS, () -> {
               if (t.burningUntil == until) { // Túi sữa dê đã gỡ (về 0) / hồi sinh: thôi
                  burnTickPlayer(zone, attacker, target);
               }
            });
         }
      }
      if (roll(combinedStatusPoints(s, MobStatus.LIGHTNING, skillStatus))) { // đòn thường qua giáp, không chí mạng
         zone.broadcast(WorldPackets.playerStatus(MobStatus.LIGHTNING, target.id(), 0).toPacket(), null);
         long thunder = PvpMath.reduce(s.attackPower, s, target.stats());
         zone.schedule(1, () -> dotPlayer(zone, attacker, target, thunder));
      }
   }

   /**
    * Hiệu ứng từ chỉ số lên boss người chơi giả: choáng / băng / suy yếu {@value StatRules#STATUS_MS_PLAYER} ms
    * ({@link AltarBoss#applyStatus}: đứng im + ngắt chiêu / đánh yếu đi; suy yếu còn nhận thêm 20%), thiêu đốt 3 s,
    * sấm sét. Đang dính thì không dính lại.
    */
   private static void statProcsOnBoss(Zone zone, WorldPlayer player, AltarBoss boss, long now, Status skillStatus) {
      PlayerStats s = player.stats();
      if (s.stunDamage <= 0 && s.iceDamage <= 0 && s.weakenDamage <= 0 && s.burnDamage <= 0 && s.thunderDamage <= 0
            && skillStatus == null) {
         return;
      }
      int ms = StatRules.STATUS_MS_PLAYER;
      if (!boss.disabledAt(now) && roll(combinedStatusPoints(s, MobStatus.STUN, skillStatus))) {
         boss.applyStatus(zone, MobStatus.STUN, ms, now);
      } else if (!boss.disabledAt(now) && roll(combinedStatusPoints(s, MobStatus.ICE, skillStatus))) {
         boss.applyStatus(zone, MobStatus.ICE, ms, now);
      }
      if (!boss.weakened(now) && roll(combinedStatusPoints(s, MobStatus.WEAKEN, skillStatus))) {
         boss.applyStatus(zone, MobStatus.WEAKEN, ms, now);
      }
      if (now >= boss.burningUntil && roll(combinedStatusPoints(s, MobStatus.BURN, skillStatus))) {
         int burnMs = StatRules.BURN_MS_BOSS_OR_PLAYER;
         boss.burningUntil = now + burnMs;
         boss.applyStatus(zone, MobStatus.BURN, burnMs, now);
         long tick = StatRules.burnPerTick(s.attackPower);
         for (int k = 1; k <= burnMs / StatRules.BURN_TICK_MS; k++) {
            zone.schedule((long) k * StatRules.BURN_TICK_MS, () -> dotBoss(zone, player, boss, tick));
         }
      }
      if (roll(combinedStatusPoints(s, MobStatus.LIGHTNING, skillStatus))) {
         boss.applyStatus(zone, MobStatus.LIGHTNING, 0, now);
         long thunder = PvpMath.reduce(s.attackPower, s, boss.stats);
         long damage = boss.weakened(now) ? StatRules.weakened(thunder) : thunder;
         zone.schedule(1, () -> dotBoss(zone, player, boss, damage));
      }
   }

   /** Tốc chạy đổi (suy yếu bắt đầu / hết): tính lại chỉ số trên luồng session + phát khối chỉ số cho cả khu. */
   private static void refreshSpeed(WorldPlayer target) {
      PlayerSession s = target.session();
      World w = GearCharges.world();
      GearCharges.post(s, () -> {
         PlayerData d = s.getPlayer();
         if (d != null && d.inWorld && w != null) {
            ItemService.refreshLook(d);
            w.broadcastStats(d);
         }
      });
   }

   /** Tim băng / Nhẫn u mộc: tung xúc xắc cho 1 đòn trúng, trúng thì -1 lượt. @return hiệu ứng, null = không */
   static MobStatus rollStatus(WorldPlayer player) {
      GearCharges.State g = player.data().gear;
      ThreadLocalRandom rnd = ThreadLocalRandom.current();
      if (g.has(GearCharges.Kind.ICE) && rnd.nextInt(100) < GearCharges.ICE_CHANCE
            && GearCharges.use(player, GearCharges.Kind.ICE, 1) > 0) {
         return MobStatus.ICE;
      }
      if (g.has(GearCharges.Kind.ROOT) && rnd.nextInt(100) < GearCharges.ROOT_CHANCE
            && GearCharges.use(player, GearCharges.Kind.ROOT, 1) > 0) {
         return MobStatus.ROOT;
      }
      return null;
   }

   static int statusMs(MobStatus status) {
      return status == MobStatus.ICE ? GearCharges.ICE_MS : GearCharges.ROOT_MS;
   }

   /** Đòn trúng quái còn sống: đóng băng / trói (op 35 bản quái — client dừng quái). */
   private static void procOnMob(Zone zone, WorldPlayer player, int mobId) {
      if (player.zone != zone) {
         return;
      }
      MobStatus status = rollStatus(player);
      if (status != null) {
         zone.broadcast(WorldPackets.mobStatus(status, mobId, statusMs(status)).toPacket(), null);
      }
   }

   /**
    * Đòn trúng người còn sống: đóng băng (không ra chiêu được — {@link PlayerData#frozenUntil}, client khoá di chuyển) /
    * trói (client khoá di chuyển) + icon option 8 / 21 trên thanh hiệu ứng của người bị dính. Bất tử / Túi sữa dê (miễn
    * nhiễm) thì không dính, không tốn lượt.
    */
   private static void procOnPlayer(Zone zone, WorldPlayer attacker, WorldPlayer target) {
      PlayerData t = target.data();
      long now = System.currentTimeMillis();
      if (t.isDead() || now < t.invulnerableUntil || ItemBuffs.active(t, ItemBuffs.OPTION_IMMUNE, now)) {
         return;
      }
      MobStatus status = rollStatus(attacker);
      if (status == null) {
         return;
      }
      int ms = status == MobStatus.ICE ? iceMs(target, statusMs(status)) : statusMs(status);
      if (ms <= 0) {
         return;
      }
      long until = now + ms;
      if (status == MobStatus.ICE) {
         t.frozenUntil = until;
      }
      zone.broadcast(WorldPackets.playerStatus(status, target.id(), ms).toPacket(), null);
      debuffIcon(target, status == MobStatus.ICE ? ItemBuffs.OPTION_FROZEN : ItemBuffs.OPTION_ROOTED, until);
   }

   /**
    * {@code target} vừa bị hạ gục ({@code text} = dòng 1 hộp "Kiệt sức"): gửi hộp + {@link #onKilled}. Ở Lôi vực Sấm sét:
    * đếm lần bị hạ gục của lượt, hẹn {@link ThunderRules#REVIVE_MS} sau hồi sinh tại chỗ — quá
    * {@link ThunderRules#MAX_DEATHS} lần thì bị loại về làng; dòng 2 của hộp nói điều đó. Luồng tick.
    */
   static void died(WorldPlayer target, String text) {
      Zone zone = target.zone;
      String hint = zone == null ? null : zone.map().isThunder() ? thunderDeath(zone, target)
            : zone.arena != null ? arenaDeath(zone, target) : null;
      target.send(hint == null ? WorldPackets.deathMessage(text) : WorldPackets.deathMessage(text, hint));
      onKilled(target); // mất Lá Chắn Tiễn + gỡ icon
   }

   static final String THUNDER_REVIVE_HINT = "Tự động hồi sinh sau 5 giây";
   static final String THUNDER_OUT_HINT = "Bạn đã bị hạ gục quá " + ThunderRules.MAX_DEATHS
         + " lần, sau 5 giây sẽ bị đưa về làng";
   static final String THUNDER_OUT_NOTICE = "Bạn đã bị hạ gục quá " + ThunderRules.MAX_DEATHS
         + " lần và bị loại khỏi Lôi vực Sấm sét.";

   static final String ARENA_MATCH_HINT = "Bạn đã bị hạ gục, chờ trận đấu kết thúc";

   /**
    * Khu đấu Giải đấu Bộ lạc: +1 lần bị hạ gục. Trận loại trực tiếp: nằm chờ giải chốt trận (không hồi sinh). Vòng loại: hẹn
    * {@link Arena#reviveMs} sau hồi sinh đầy HP / MP ở chỗ ngẫu nhiên trên đài — quá {@link Arena#maxDeaths} lần thì về
    * làng (rời khu = bị loại, {@link World.ArenaHooks#left}). @return dòng 2 hộp "Kiệt sức"
    */
   private static String arenaDeath(Zone zone, WorldPlayer target) {
      Arena arena = zone.arena;
      int count = arena.addDeath(target.id());
      if (!arena.qualifier) {
         return ARENA_MATCH_HINT;
      }
      boolean out = arena.eliminated(count);
      long seconds = Math.max(1, arena.reviveMs / 1000);
      zone.schedule(arena.reviveMs, () -> {
         if (target.zone != zone || target.removed || !target.data().isDead()) {
            return; // đã rời khu (vòng loại hết / thoát game)
         }
         if (out) {
            zone.map().world().exitArena(zone, target, "Bạn đã bị hạ gục quá " + arena.maxDeaths
                  + " lần và bị loại khỏi giải đấu.");
         } else {
            float[] spot = Arena.randomSpot();
            zone.placeAt(target, spot[0], spot[1]);
            World.reviveInPlace(zone, target);
         }
      });
      return out ? "Bạn đã bị hạ gục quá " + arena.maxDeaths + " lần, sau " + seconds + " giây sẽ bị đưa về làng"
            : "Tự động hồi sinh sau " + seconds + " giây";
   }

   /** Khu đấu: {@code victim} vừa bị hạ gục ({@code killer} null = rơi đài không ai đánh) — báo giải. Luồng tick. */
   private static void arenaKilled(Zone zone, WorldPlayer killer, WorldPlayer victim) {
      World world = zone.map().world();
      World.ArenaHooks hooks = world == null ? null : world.arenaHooks();
      if (hooks != null) {
         hooks.killed(zone.arena, killer, victim);
      }
   }

   /**
    * Khu đấu: {@code p} vừa xuống dưới {@link Arena#FALL_Y} (vùng thấp hai bên võ đài). Đã bị hạ gục -> đặt xác lại lên
    * đài; chưa cho đánh -> bị hạ gục, chỉ còn "Về làng" ({@link #arenaFallWaiting}); đang đánh -> bị hạ gục (người đánh
    * trúng cuối trong {@link Arena#FALL_CREDIT_MS} được tính hạ gục).
    * @return true = đã đặt lại vị trí (không phát vị trí vừa báo)
    */
   static boolean arenaFall(Zone zone, WorldPlayer p, long now) {
      Arena arena = zone.arena;
      PlayerData d = p.data();
      if (d.isDead()) { // xác rơi xuống: đặt lại lên đài
         float[] spot = arena.spotOf(p.id());
         zone.placeAt(p, spot[0], spot[1]);
         return true;
      }
      if (!arena.fighting) { // chưa cho đánh mà nhảy xuống đài: bị hạ gục, không tự hồi sinh — chỉ còn "Về làng"
         arenaFallWaiting(zone, p);
         return false;
      }
      long before = d.hp;
      long left = d.damageHp(before, 0);
      p.invalidateSpawnPacket();
      zone.broadcast(WorldPackets.playerDamaged(p.id(), Math.max(0, before - left), Math.max(0, left)), null);
      if (left > 0) {
         return false;
      }
      p.clearPendingPosition();
      WorldPlayer killer = p.arenaHitBy != 0 && now - p.arenaHitAt <= Arena.FALL_CREDIT_MS ? zone.find(p.arenaHitBy)
            : null;
      if (killer != null && killer.data().isDead()) {
         killer = null;
      }
      died(p, killer != null ? "Bạn đã bị " + killer.data().name + " đánh rơi khỏi võ đài." : "Bạn đã rơi khỏi võ đài.");
      sendStamina(p);
      if (killer != null) {
         killer.send(Packets.notice("Bạn đã hạ gục " + d.name + "."));
      }
      arenaKilled(zone, killer, p);
      return false;
   }

   static final String ARENA_FALL_WAIT_HINT = "Bấm Về làng để rời đấu trường";

   /**
    * Rơi khỏi đài lúc CHƯA cho đánh (10 s chờ trận / chờ giờ vòng loại): hạ gục ngay, KHÔNG tính người hạ gục, KHÔNG tự
    * hồi sinh (vòng loại cũng vậy) — hộp Kiệt sức chỉ còn đường "Về làng" (Cỏ hồi sinh bị chặn ở khu đấu). Trận loại trực
    * tiếp: tính như bị hạ (ra khỏi trận); vòng loại: không mất lượt, về làng trước giờ đánh thì vào lại được.
    */
   private static void arenaFallWaiting(Zone zone, WorldPlayer p) {
      PlayerData d = p.data();
      long before = d.hp;
      long left = d.damageHp(before, 0);
      p.invalidateSpawnPacket();
      zone.broadcast(WorldPackets.playerDamaged(p.id(), Math.max(0, before - left), Math.max(0, left)), null);
      if (left > 0) {
         return;
      }
      p.clearPendingPosition();
      p.send(WorldPackets.deathMessage("Bạn đã rơi khỏi võ đài.", ARENA_FALL_WAIT_HINT));
      onKilled(p); // mất Lá Chắn Tiễn + gỡ icon
      sendStamina(p);
      if (!zone.arena.qualifier) {
         arenaKilled(zone, null, p);
      }
   }

   /** Lôi vực: +1 lần bị hạ gục, hẹn hồi sinh tại chỗ / bị loại. @return dòng 2 hộp "Kiệt sức" */
   private static String thunderDeath(Zone zone, WorldPlayer target) {
      ThunderRun run = target.data().thunderRun;
      boolean out = false;
      if (run != null) {
         run.deaths++;
         out = run.eliminated();
      }
      boolean eliminated = out;
      zone.schedule(ThunderRules.REVIVE_MS, () -> {
         if (target.zone != zone || target.removed || !target.data().isDead()) {
            return; // đã bấm "Về làng" / thoát game
         }
         if (eliminated) {
            zone.map().world().exitThunder(zone, target, THUNDER_OUT_NOTICE);
         } else {
            World.reviveInPlace(zone, target);
         }
      });
      return out ? THUNDER_OUT_HINT : THUNDER_REVIVE_HINT;
   }

   /**
    * {@code target} vừa bị hạ gục: mất giảm sát thương của kỹ năng (Lá Chắn Tiễn / Trụ Tái Tạo) + gỡ icon Lá Chắn Tiễn trên
    * thanh hiệu ứng (luồng session). Luồng tick.
    */
   static void onKilled(WorldPlayer target) {
      PlayerData d = target.data();
      d.burningUntil = 0; // hết cháy (tick còn hẹn tự dừng)
      d.stunnedUntil = 0;
      d.frozenUntil = 0;
      d.skillShieldUntil = 0;
      d.skillShieldPercent = 0;
      PlayerSession s = target.session();
      GearCharges.post(s, () -> { // gỡ icon Lá Chắn Tiễn + choáng / băng / thiêu đốt (suy yếu tự hết theo giờ)
         PlayerData pd = s.getPlayer();
         if (pd != null && pd.inWorld && ItemBuffs.remove(pd, java.util.Set.of(ItemBuffs.OPTION_SKILL_SHIELD,
               ItemBuffs.OPTION_STUN, ItemBuffs.OPTION_FROZEN, ItemBuffs.OPTION_BURN))) {
            s.send(ItemBuffs.packet(pd, System.currentTimeMillis()));
         }
      });
   }

   /**
    * Đẩy / hất tung {@code p} (op 12 sub 2: vận tốc m/s, client tự bay + rơi theo trọng lực, khoá điều khiển {@code ms}).
    * Cho người đó miễn kiểm tra di chuyển {@code ms} + {@link #KNOCK_FALL_MS} — không thì bộ chống hack tốc độ
    * (ngang tối đa 450 px) kéo nhân vật về chỗ cũ. Luồng tick.
    */
   static void knock(WorldPlayer p, float vx, float vy, int ms) {
      p.knockedUntil = System.currentTimeMillis() + ms + KNOCK_FALL_MS;
      com.tlnt.server.session.PlayerSession s = p.session();
      Zone zone = p.zone;
      if (zone != null && s != null && (s.isBot() || s.isOfflineTraining())) {
         // không có client tự bay (bot / treo offline): server mô phỏng cú văng (trước đây chiêu không tác dụng lên bot)
         zone.knockNoClient(p, vx * PX_PER_M, vy * PX_PER_M);
         return;
      }
      p.send(WorldPackets.knockback(vx, vy, ms));
   }

   /** Vận tốc op 12 sub 2 tính theo m / s Box2D của client; 1 m = 100 px. */
   static final float PX_PER_M = 100f;

   /** Sau khi hết thời gian bị đẩy, nhân vật còn rơi xuống — miễn kiểm tra thêm bấy lâu. */
   static final long KNOCK_FALL_MS = 1_500;

   /**
    * Trói {@code target} {@code ms} ms (boss Tinh linh Thần mộc): op 35 sub 0 cả khu thấy (client {@code rootEffect} khoá
    * di chuyển của chính người đó) + icon hiệu ứng (luồng session). Bất tử / miễn khống chế thì thôi. Luồng tick.
    */
   /**
    * Chiêu boss gây hiệu ứng lên người (Tinh linh boss thế giới): choáng / đóng băng = không ra chiêu, client khoá di chuyển
    * (op 35); suy yếu = chậm 20% + nhận thêm 20%. Thời gian trừ Kháng hiệu ứng; bất tử / Túi sữa dê miễn; đang choáng / băng
    * thì không chồng. Luồng tick.
    */
   /** Thời gian đóng băng sau "Giảm X% Thời gian đóng băng" (dòng 71, tối đa 100%). */
   static int iceMs(WorldPlayer target, int ms) {
      long p = Math.min(100, Math.max(0, target.stats().freezeReducePercent));
      return (int) (ms * (100 - p) / 100);
   }

   static void bossStatusPlayer(Zone zone, WorldPlayer target, MobStatus status, int baseMs) {
      bossStatusPlayer(zone, target, status, baseMs, 0);
   }

   static void bossStatusPlayer(Zone zone, WorldPlayer target, MobStatus status, int baseMs,
         int weakenAttackPercent) {
      PlayerData t = target.data();
      long now = System.currentTimeMillis();
      if (t.isDead() || now < t.invulnerableUntil || ItemBuffs.active(t, ItemBuffs.OPTION_IMMUNE, now)) {
         return;
      }
      int ms = StatRules.afterEffectResist(baseMs, target.stats().effectResistPercent);
      if (status == MobStatus.ICE) {
         ms = iceMs(target, ms);
      }
      if (ms <= 0) {
         return;
      }
      if (status == MobStatus.WEAKEN) {
         if (weakenAttackPercent > 0) {
            if (now >= t.bossWeakenAttackUntil) {
               t.bossWeakenAttackPercent = 0;
            }
            t.bossWeakenAttackPercent = Math.max(t.bossWeakenAttackPercent,
                  Math.min(100, weakenAttackPercent));
            t.bossWeakenAttackUntil = Math.max(t.bossWeakenAttackUntil, now + ms);
         }
         weakenPlayer(zone, target, ms, now);
         return;
      }
      if (now < t.stunnedUntil || now < t.frozenUntil) {
         return;
      }
      if (status == MobStatus.STUN) {
         t.stunnedUntil = now + ms;
      } else {
         t.frozenUntil = now + ms;
      }
      zone.broadcast(WorldPackets.playerStatus(status, target.id(), ms).toPacket(), null);
      debuffIcon(target, status == MobStatus.STUN ? ItemBuffs.OPTION_STUN : ItemBuffs.OPTION_FROZEN, now + ms);
   }

   /**
    * Suy yếu người chơi {@code ms} (option_info 6): chậm {@link StatRules#WEAKEN_SLOW_PERCENT}% (tính lại khối chỉ số lúc
    * bắt đầu + lúc hết) + nhận thêm {@link StatRules#WEAKEN_TAKEN_PERCENT}% sát thương ({@link #incoming}). Luồng tick.
    */
   private static void weakenPlayer(Zone zone, WorldPlayer target, int ms, long now) {
      PlayerData t = target.data();
      t.weakenedUntil = Math.max(t.weakenedUntil, now + ms);
      zone.broadcast(WorldPackets.playerStatus(MobStatus.WEAKEN, target.id(), ms).toPacket(), null);
      debuffIcon(target, ItemBuffs.OPTION_WEAKEN, t.weakenedUntil);
      refreshSpeed(target);
      zone.schedule(ms + 50L, () -> refreshSpeed(target)); // hết suy yếu: tốc chạy cũ
   }

   /**
    * 1 nhịp Thiêu đốt lên người chơi (option_info 7): mất thẳng 0,5% HP tối đa ({@link StatRules#burnPlayerPerTick}) — không
    * qua giáp / Suy yếu / khiên; bất tử thì không mất. HP về 0 -> {@code source} hạ gục (thách đấu: dừng ở 1). Luồng tick.
    */
   static void burnTickPlayer(Zone zone, WorldPlayer source, WorldPlayer target) {
      PlayerData t = target.data();
      if (target.removed || target.zone != zone || t.isDead() || t.stats == null
            || System.currentTimeMillis() < t.invulnerableUntil) {
         return;
      }
      boolean duel = source.dueling(target);
      long before = t.hp;
      long left = t.damageHp(StatRules.burnPlayerPerTick(t.stats.maxHp), duel ? 1 : 0);
      long taken = Math.max(0, before - left);
      target.invalidateSpawnPacket();
      zone.broadcast(WorldPackets.playerDamaged(target.id(), taken, Math.max(0, left)), null);
      afterHit(zone, source, target, new PvpHit(taken, left, duel));
   }

   /** Icon hiệu ứng bất lợi (option_info 6 / 7 / 8 / 9 / 21) trên thanh hiệu ứng của người bị dính, tới {@code until}. */
   static void debuffIcon(WorldPlayer target, int option, long until) {
      PlayerSession s = target.session();
      GearCharges.post(s, () -> {
         PlayerData d = s.getPlayer();
         if (d != null && d.inWorld && !d.isDead()
               && !ItemBuffs.active(d, ItemBuffs.OPTION_IMMUNE, System.currentTimeMillis())) {
            ItemBuffs.showTimed(d, option, until);
            s.send(ItemBuffs.packet(d, System.currentTimeMillis()));
         }
      });
   }

   static boolean rootPlayer(Zone zone, WorldPlayer target, int ms) {
      PlayerData t = target.data();
      long now = System.currentTimeMillis();
      if (t.isDead() || now < t.invulnerableUntil || ItemBuffs.active(t, ItemBuffs.OPTION_IMMUNE, now)) {
         return false;
      }
      long until = now + ms;
      zone.broadcast(WorldPackets.playerStatus(MobStatus.ROOT, target.id(), ms).toPacket(), null);
      PlayerSession s = target.session();
      GearCharges.post(s, () -> {
         PlayerData d = s.getPlayer();
         if (d != null && d.inWorld && !ItemBuffs.active(d, ItemBuffs.OPTION_IMMUNE, System.currentTimeMillis())) {
            ItemBuffs.showTimed(d, ItemBuffs.OPTION_ROOTED, until);
            s.send(ItemBuffs.packet(d, System.currentTimeMillis()));
         }
      });
      return true;
   }

   /**
    * Vừa hồi sinh (luồng tick khu, {@code World.revive}): đeo Bùa linh mộc còn lượt -> -1 lượt, bất tử
    * {@link GearCharges#CHARM_INVULNERABLE_MS} + 10% Khuếch đại tấn công 15 giây (option 25, luồng session).
    */
   static void onRevive(WorldPlayer player) {
      PlayerData d = player.data();
      if (!d.gear.has(GearCharges.Kind.CHARM) || GearCharges.use(player, GearCharges.Kind.CHARM, 1) <= 0) {
         return;
      }
      d.invulnerableUntil = System.currentTimeMillis() + GearCharges.CHARM_INVULNERABLE_MS;
      PlayerSession s = player.session();
      World w = GearCharges.world();
      GearCharges.post(s, () -> ItemBuffs.applyProc(s, w, ItemBuffs.CHARM_PROC));
      Zone zone = player.zone;
      if (zone != null) { // hết 15 s: gỡ đúng lúc (không chờ nhịp 5 s)
         zone.schedule(ItemBuffs.CHARM_PROC.durationMs() + 50,
               () -> GearCharges.post(s, () -> ItemBuffs.dropExpired(s, w, System.currentTimeMillis())));
      }
   }

   // ------------------------------------------------------------------ khoảng cách

   /** Đòn nhắm 1 quái: tầm + cả phạm vi lang thang + độ lệch mạng. */
   private static boolean inReach(WorldPlayer player, MapTemplate.MobInstance mob, int rangeX, int rangeY) {
      float wander = wanderRange(mob);
      return Math.abs(player.x - mob.x()) <= rangeX + wander + HIT_SLACK_PX
            && Math.abs(player.y - mob.y()) <= rangeY + wander / 2 + HIT_SLACK_PX;
   }

   /** Đòn nhắm 1 người: tầm + độ lệch mạng. */
   private static boolean inReach(WorldPlayer player, WorldPlayer target, int rangeX, int rangeY) {
      return Math.abs(player.x - target.x) <= rangeX + HIT_SLACK_PX
            && Math.abs(player.y - target.y) <= rangeY + HIT_SLACK_PX;
   }

   /** Đòn diện rộng quanh điểm ({@code x}, {@code y}): tầm + nửa phạm vi lang thang (không cộng độ lệch). */
   private static boolean inArea(float x, float y, MapTemplate.MobInstance mob, int rangeX, int rangeY) {
      float wander = wanderRange(mob);
      return Math.abs(x - mob.x()) <= rangeX + wander / 2 && Math.abs(y - mob.y()) <= rangeY + wander / 4;
   }

   /** Đòn diện rộng lên người quanh điểm ({@code x}, {@code y}). */
   private static boolean inArea(float x, float y, WorldPlayer other, int rangeX, int rangeY) {
      return Math.abs(x - other.x) <= rangeX + AREA_SLACK_PX && Math.abs(y - other.y) <= rangeY + AREA_SLACK_PX;
   }

   private static float wanderRange(MapTemplate.MobInstance mob) {
      MapPack.Mob template = GameDataCache.mob(mob.spawn().id());
      return template == null ? 0 : template.wanderRange();
   }
}
