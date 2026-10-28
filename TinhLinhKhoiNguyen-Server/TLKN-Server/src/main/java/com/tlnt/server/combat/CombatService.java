package com.tlnt.server.combat;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.SkillEffect;
import com.tlnt.server.data.model.SkillPack;
import com.tlnt.server.item.Appearance;
import com.tlnt.server.item.GearCharges;
import com.tlnt.server.item.ItemBuffs;
import com.tlnt.server.item.ItemPackets;
import com.tlnt.server.net.Packets;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.WeaponClass;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;
import com.tlnt.server.player.SkillData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.skill.SkillService;
import com.tlnt.server.world.MobStatus;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;
import com.tlnt.server.world.ZoneCombat;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Người chơi dùng kỹ năng (op 21 sub 0) lên quái hoặc người chơi khác (loại mục tiêu 1 — PK / thách đấu; luật ai
 * đánh được ai kiểm tra ở luồng tick khu, {@code ZoneCombat.canHit}). MP + hồi chiêu tính 1 lần mỗi lần dùng, dù kỹ năng đánh nhiều đòn.
 * Chạy lane GAME, tuần tự theo session -> đọc/ghi HP, MP, hồi chiêu, buff của {@link PlayerData} an toàn như
 * ItemService (bình máu cũng sửa HP/MP trên luồng này).
 *
 * <p>Chia 2 bước theo luồng:
 * <ol>
 *   <li>Ở đây (luồng session): kỹ năng đã học + đã code, cầm đúng vũ khí, hết hồi chiêu, đủ MP -> trừ MP, bắt đầu
 *       hồi chiêu, tính sát thương từ chỉ số + {@code skill_effect}, hồi HP / hút HP / buff tốc chạy.</li>
 *   <li>{@link World} -> {@link ZoneCombat} (luồng tick khu): quái còn sống + đứng đủ gần -> trừ máu, phát cho cả
 *       khu, các bước sau hẹn giờ.</li>
 * </ol>
 * Client đã tự kiểm tra tầm / vũ khí / hồi chiêu / MP trước khi gửi, nên bị từ chối gần như chỉ khi lag hoặc hack.
 */
public final class CombatService {

   /** Cách server phát gói của 1 kỹ năng (theo nhánh skillId trong client {@code handleCombatAction}). */
   private enum Mechanic {
      /** Nhắm 1 quái: 1 hoặc nhiều gói "1 mục tiêu", có thể chảy máu / hiệu ứng / hút HP. Cần mục tiêu. */
      STRIKE(true),
      /** Đòn chính + quái phụ quanh nó, 1 gói. Cần mục tiêu. */
      SPLASH(true),
      /** Xoay chém quanh người: 1 animation nhiều vòng, mỗi vòng trúng mọi quái trong tầm 1 lần. Tự thân. */
      SPIN(false),
      /** Gồng rồi bổ 1 quái: gói bắt đầu + gói kết thúc có sát thương. Cần mục tiêu. */
      CHARGE(true),
      /** Vào thế / buff rồi kết thúc: 2 gói bool. Tự thân. */
      CHANNEL(false),
      /** Lướt + ảnh ảo tự đánh quái. Tự thân. */
      ILLUSION(false);

      final boolean needsTarget;

      Mechanic(boolean needsTarget) {
         this.needsTarget = needsTarget;
      }
   }

   /**
    * Mọi kỹ năng của 5 class (Rìu 2-7, Cung 8-13, Ná 14-19, Giáo 20-25, Roi 26-31) + 1 tay không. Chưa có: 42 Loạn
    * Ảnh Phách (không có dữ liệu cấp, client không có nhánh), class 7 (32-41, 43).
    */
   private static final Map<Integer, Mechanic> MECHANICS = Map.ofEntries(
         // tay không
         Map.entry(1, Mechanic.STRIKE),
         // Rìu
         Map.entry(2, Mechanic.STRIKE), Map.entry(3, Mechanic.STRIKE), Map.entry(4, Mechanic.CHANNEL),
         Map.entry(5, Mechanic.SPIN), Map.entry(6, Mechanic.STRIKE), Map.entry(7, Mechanic.CHARGE),
         // Cung
         Map.entry(8, Mechanic.STRIKE), Map.entry(9, Mechanic.STRIKE), Map.entry(10, Mechanic.CHANNEL),
         Map.entry(11, Mechanic.SPLASH), Map.entry(12, Mechanic.STRIKE), Map.entry(13, Mechanic.CHARGE),
         // Ná
         Map.entry(14, Mechanic.STRIKE), Map.entry(15, Mechanic.STRIKE), Map.entry(16, Mechanic.CHANNEL),
         Map.entry(17, Mechanic.SPLASH), Map.entry(18, Mechanic.STRIKE), Map.entry(19, Mechanic.CHARGE),
         // Giáo
         Map.entry(20, Mechanic.STRIKE), Map.entry(21, Mechanic.STRIKE), Map.entry(22, Mechanic.STRIKE),
         Map.entry(23, Mechanic.STRIKE), Map.entry(24, Mechanic.STRIKE), Map.entry(25, Mechanic.CHARGE),
         // Roi
         Map.entry(26, Mechanic.STRIKE), Map.entry(28, Mechanic.ILLUSION),
         Map.entry(29, Mechanic.STRIKE), Map.entry(30, Mechanic.STRIKE), Map.entry(31, Mechanic.CHARGE),
         Map.entry(42, Mechanic.SPIN)); // 42 Loạn Ảnh Phách (thay hẳn 27 — 27 không còn logic): 5 đòn quanh mình, gói đầu = op 21 case 42

   /**
    * STRIKE nhiều đòn gửi MỖI ĐÒN 1 GÓI (1 lần trừ máu + 1 số sát thương ở client) -> skillId -> ms giữa 2 gói.
    * Gói đầu gửi ngay (bắt đầu animation), các gói sau đều tới khi animation CÒN chạy (cùng trạng thái -> client không
    * phát lại chiêu; tới sau khi xong thì client phát lại cả chiêu).
    * <ul>
    *   <li>9 Bão Vũ Tiễn / 15 Tam Thạch Nỗ (client {@code BowSkill1Anim} / {@code SlingshotSkill1Anim}): mỗi gói thêm
    *       1 mục tiêu vào hàng đợi, animation {@code skill1_2} (0,3 s × tốc độ 2 = 0,15 s / vòng) bắn 1 phát mỗi vòng,
    *       hết hàng đợi thì dừng -> cách nhau 150 ms, gói luôn tới trước lúc cần (phát đầu sau {@code skill1_1}
    *       0,5 s / 1,3 ≈ 0,38 s).</li>
    *   <li>21 Vũ Mâu Kích (client {@code SpearSkill1Anim}): animation {@code spear/skill1_1} 0,7 s ->
    *       {@code skill1_2} 0,5 s -> {@code skill1_3} 0,43 s / 1,5 ≈ 1,49 s; mỗi gói hiện hiệu ứng trúng đòn khi
    *       đoạn đang chạy qua 30% -> cách nhau 250 ms: 5 cú ở 0 / 0,25 / 0,5 / 0,75 / 1,0 s, dư ~0,5 s.</li>
    * </ul>
    * STRIKE khác có {@code hits} > 1 nhưng không nằm đây: gửi 1 gói tổng.
    */
   private static final Map<Integer, Long> SEPARATE_HIT_INTERVAL_MS = Map.of(9, 150L, 15, 150L, 21, 250L);
   /**
    * Thời gian hiệu ứng khi {@code skill_effect.status_ms} không ghi. {@code status_power} là điểm tỉ lệ kích hoạt trên
    * thang 10.000 (1.000 = 10%); giá trị 0 dành cho hiệu ứng định sẵn như trói có thời lượng.
    */
   private static final Map<MobStatus, Integer> DEFAULT_STATUS_MS = Map.of(
         MobStatus.STUN, 1_500, MobStatus.ICE, 2_000, MobStatus.WEAKEN, 3_000, MobStatus.ROOT, 3_000);
   /**
    * SPIN (5 Cuồng Phong Trảm), số vòng = cột {@code hits} (5). Đếm từ skeleton nhân vật {@code axe/skill3}: bone gốc
    * lật scaleX 1 -> -1 -> 1 mỗi 0,133 s từ 0,233 s tới 0,9 s = 5 vòng; client chơi ở tốc độ 1,3 -> vòng bắt đầu
    * ≈ 0,18 s, mỗi vòng ≈ 0,103 s. Vòng 1 gửi ngay (bắt đầu animation), vòng 2 lúc 0,28 s, rồi mỗi 0,103 s.
    */
   private static final long SPIN_SECOND_REVOLUTION_MS = 282;
   private static final long SPIN_REVOLUTION_MS = 103;
   /**
    * CHARGE: thời gian gồng ("sau vài giây" — dữ liệu không ghi số), mọi chiêu cuối. Client chơi {@code skill5_1} rồi
    * lặp {@code skill5_2} tới khi nhận gói kết thúc.
    */
   private static final long CHARGE_MS = 2_000;
   /**
    * SPLASH chọn quái phụ quanh NGƯỜI BẮN trong tầm kỹ năng (cả phía sau): 11 Tán Diệt Tiễn. Còn lại (17 Xung Kích
    * Nỗ) chọn quanh quái chính trong {@link #SPLASH_RADIUS_PX}.
    */
   private static final Set<Integer> SPLASH_AROUND_ATTACKER = Set.of(11);
   /** SPLASH quanh quái chính: quái phụ trong bán kính này (px). */
   private static final int SPLASH_RADIUS_PX = 400;
   /** Chảy máu (6 Viễn Tâm Trảm "trong thoáng chốc" — dữ liệu không ghi thời gian): 3 nhịp, mỗi giây 1 nhịp. */
   private static final int BLEED_TICKS = 3;
   private static final long BLEED_INTERVAL_MS = 1_000;
   /**
    * ILLUSION (28): lướt 0,3 s với tốc độ đầu 20 đơn vị/giây (giảm dần về 0) ≈ 300 px; ảnh ảo quất bằng animation
    * đánh thường Roi (skill 26, tầm theo skill_level của nó) mỗi 0,6 s (hồi chiêu đánh thường Roi 0,55 s).
    */
   private static final float DASH_SECONDS = 0.3f;
   private static final float DASH_SPEED = 20f;
   private static final int ILLUSION_ATTACK_SKILL = 26;
   private static final long ILLUSION_ATTACK_INTERVAL_MS = 600;
   /**
    * Kỹ năng buff -> {@code option_info.id} hiện icon + thời gian còn lại trên thanh hiệu ứng (op 51): 10 Lá Chắn Tiễn
    * -> 23, 16 Phá Tốc Nỗ -> 24. Thời gian = {@code skill_effect.duration_ms} theo cấp.
    */
   private static final Map<Integer, Integer> BUFF_ICON_OPTION = Map.of(10, ItemBuffs.OPTION_SKILL_SHIELD, 16, 24);
   /** Sát thương dao động ±10% mỗi đòn. */
   private static final int DAMAGE_SPREAD_PERCENT = 10;
   /** Cho phép gói đánh tới sớm hơn hồi chiêu một chút (gói dồn lại do mạng). */
   private static final long COOLDOWN_TOLERANCE_MS = 100;
   /** {@code skill_template.mana_cost_type}: 1 = MP cố định, 2 = % MP tối đa (như client AutoTargetController). */
   private static final int MANA_FIXED = 1;
   private static final int MANA_PERCENT = 2;

   private final World world;
   /** Hẹn giờ hết buff (luồng scheduler chỉ xếp việc lại vào hàng đợi session, không đụng PlayerData). */
   private final ScheduledExecutorService scheduler;
   private final Executor gamePool;

   /** Thể lực = 0: không ra chiêu được (như game gốc). */
   static final String MSG_NO_STAMINA = "Thể lực đã cạn,Hãy nghỉ ngơi để lấy lại sức!";
   /** Client tự đánh liên tục -> báo tối đa 1 lần / bấy nhiêu ms. */
   private static final long STAMINA_NOTICE_INTERVAL_MS = 3000;

   public CombatService(World world, ScheduledExecutorService scheduler, Executor gamePool) {
      this.world = world;
      this.scheduler = scheduler;
      this.gamePool = gamePool;
   }

   /** Op 21 sub 0 ({@code sendSkillbarAction}): dùng kỹ năng {@code skillId} lên mục tiêu (tự thân: -1, -1). @return true = đã ra chiêu */
   public boolean useSkill(PlayerSession session, int skillId, int targetKind, int targetId) {
      PlayerData d = session.getPlayer();
      PlayerStats stats = d == null ? null : d.stats;
      Mechanic mechanic = MECHANICS.get(skillId);
      if (stats == null || !d.inWorld || mechanic == null || d.isDead()) {
         return false; // kỹ năng chưa code / đã bị hạ gục
      }
      boolean pvp = targetKind == WorldPackets.TARGET_PLAYER;
      if (mechanic.needsTarget && targetKind != WorldPackets.TARGET_MOB && !pvp) {
         return false;
      }
      if (pvp && targetId == d.getId()) {
         return false;
      }
      SkillData learned = SkillService.find(d, skillId);
      SkillPack.Skill template = GameDataCache.skillTemplate(skillId);
      SkillPack.SkillLevel level = learned == null || template == null ? null
            : SkillService.levelData(template, learned.level);
      SkillEffect effect = level == null ? null : GameDataCache.skillEffect(skillId, level.level());
      boolean dealsDamage = mechanic != Mechanic.CHANNEL && mechanic != Mechanic.ILLUSION;
      if (effect == null || (dealsDamage && effect.damagePercent() <= 0)) {
         return false; // chưa học (cấp 0) hoặc thiếu dữ liệu skill_level / skill_effect
      }
      if (heldWeaponClass(d) != template.classId()) {
         return false; // như client isWeaponRequirementBlocked: skill 1 = tay không, còn lại = vũ khí đúng class
      }
      long now = System.currentTimeMillis();
      if (now < d.frozenUntil || now < d.stunnedUntil) {
         return false; // bị đóng băng (Tim băng / Sát thương băng giá) / choáng: không thực hiện được thao tác nào
      }
      if (now - learned.lastUsedAt < level.cooldownMillis() - COOLDOWN_TOLERANCE_MS) {
         return false;
      }
      if (d.activityPoints < PlayerData.STAMINA_PER_SKILL) { // hết thể lực: không đánh được quái lẫn người
         if (now - d.staminaNoticeAt >= STAMINA_NOTICE_INTERVAL_MS) {
            d.staminaNoticeAt = now;
            session.send(Packets.notice(MSG_NO_STAMINA));
         }
         return false;
      }
      long manaCost = manaCost(template, level, stats);
      if (!d.spendMp(manaCost)) {
         return false;
      }
      d.spendStamina(PlayerData.STAMINA_PER_SKILL); // -1 mỗi lần ra chiêu / đánh
      session.send(WorldPackets.stamina(d.activityPoints, stats.maxStamina));

      learned.lastUsedAt = now;
      session.send(WorldPackets.skillCooldown(skillId, 0));
      if (stormEye(session, d, skillId, world)) { // Mắt bão Darguun: hết 5 s gỡ đúng lúc
         scheduler.schedule(() -> session.inbox().execute(
               () -> ItemBuffs.dropExpired(session, world, System.currentTimeMillis()), gamePool),
               ItemBuffs.STORM_PROC.durationMs() + 50, TimeUnit.MILLISECONDS);
      }
      switch (mechanic) {
         case STRIKE -> strike(session, stats, skillId, pvp, targetId, level, effect);
         case SPLASH -> {
            int percent = effect.damagePercent() * effect.hits();
            long mobBase = damage(stats, percent);
            long playerBase = pvpDamage(stats, percent);
            world.splash(session, new ZoneCombat.Splash(skillId, pvp, targetId, pvp ? playerBase : mobBase, mobBase,
                  playerBase, effect.aoeTargets(), effect.aoePercent(), SPLASH_AROUND_ATTACKER.contains(skillId),
                  SPLASH_RADIUS_PX, level.rangeX(), level.rangeY(), status(effect)));
         }
         case SPIN -> world.spin(session, new ZoneCombat.Spin(skillId, effect.hits(), SPIN_SECOND_REVOLUTION_MS,
               SPIN_REVOLUTION_MS, level.rangeX(), level.rangeY(), () -> damage(stats, effect.damagePercent()),
               () -> pvpDamage(stats, effect.damagePercent()), status(effect)));
         case CHARGE -> world.charge(session, new ZoneCombat.Charge(skillId, pvp, targetId, CHARGE_MS,
               (pvp ? pvpDamage(stats, effect.damagePercent()) : damage(stats, effect.damagePercent()))
                     + bonusDamage(stats, effect), level.rangeX(), level.rangeY(), status(effect)));
         case CHANNEL -> channel(session, d, stats, skillId, effect);
         case ILLUSION -> illusion(session, stats, skillId, effect);
      }
      sendHpMp(session, d);
      return true;
   }

   // ------------------------------------------------------------------ từng cơ chế

   private void strike(PlayerSession session, PlayerStats stats, int skillId, boolean pvp,
         int targetId, SkillPack.SkillLevel level, SkillEffect effect) {
      Long interval = SEPARATE_HIT_INTERVAL_MS.get(skillId);
      long[] hits = hits(stats, effect, interval != null, pvp);
      if (BASIC_ATTACKS.contains(skillId)) { // Khuếch đại tấn công: CHỈ đòn đánh thường, lên mọi mục tiêu
         amplify(hits, stats.damageAmpPercent);
      }
      ZoneCombat.Bleed bleed = effect.bleedPercent() > 0 ? new ZoneCombat.Bleed(
            pvp ? pvpDamage(stats, effect.bleedPercent()) : damage(stats, effect.bleedPercent()), BLEED_TICKS,
            BLEED_INTERVAL_MS) : null;
      // 22 Xuyên Tâm Kích: hút máu tính trên luồng khu theo sát thương đã gây ra (đòn trúng thật).
      world.strike(session, new ZoneCombat.Strike(skillId, pvp, targetId, hits, interval == null ? 0 : interval,
            bleed, status(effect), level.rangeX(), level.rangeY(), effect.lifestealPercent()));
   }

   /**
    * 4 Trụ Tái Tạo (hồi HP + giảm sát thương nhận), 10 Lá Chắn Tiễn (giảm 50-100% sát thương nhận + icon khiên),
    * 16 Phá Tốc Nỗ (tăng tốc chạy). Giảm sát thương = {@code skill_effect.damage_reduction_percent} trong
    * {@code duration_ms} ({@code ZoneCombat.incoming}); bị hạ gục thì mất ({@code ZoneCombat.onKilled}).
    */
   private void channel(PlayerSession session, PlayerData d, PlayerStats stats, int skillId, SkillEffect effect) {
      if (effect.damageReductionPercent() > 0 && effect.durationMs() > 0) { // 4 Trụ Tái Tạo / 10 Lá Chắn Tiễn
         d.skillShieldPercent = effect.damageReductionPercent(); // ZoneCombat.incoming: giảm sát thương nhận vào
         d.skillShieldUntil = System.currentTimeMillis() + effect.durationMs();
      }
      if (effect.healPercent() > 0) {
         heal(d, stats, stats.maxHp * effect.healPercent() / 100);
      }
      if (effect.moveSpeedPercent() > 0) {
         startMoveSpeedBuff(session, d, effect.moveSpeedPercent(), effect.durationMs());
      }
      world.channel(session, skillId, effect.durationMs());
      Integer option = BUFF_ICON_OPTION.get(skillId);
      if (option != null && effect.durationMs() > 0) {
         long now = System.currentTimeMillis();
         ItemBuffs.showTimed(d, option, now + effect.durationMs());
         session.send(ItemBuffs.packet(d, now));
      }
   }

   /** 28 Ảo Ảnh Phách: ảnh ảo đánh {@code illusion_percent}% Tấn công mỗi đòn trong {@code duration_ms}. */
   private void illusion(PlayerSession session, PlayerStats stats, int skillId, SkillEffect effect) {
      SkillPack.Skill attack = GameDataCache.skillTemplate(ILLUSION_ATTACK_SKILL);
      SkillPack.SkillLevel attackLevel = attack == null ? null : SkillService.levelData(attack, 1);
      int rangeX = attackLevel == null ? 0 : attackLevel.rangeX();
      int rangeY = attackLevel == null ? 0 : attackLevel.rangeY();
      world.illusion(session, new ZoneCombat.Illusion(skillId, DASH_SECONDS, DASH_SPEED, effect.durationMs(),
            ILLUSION_ATTACK_INTERVAL_MS, ILLUSION_ATTACK_SKILL, rangeX, rangeY,
            () -> damage(stats, effect.illusionPercent())));
   }

   // ------------------------------------------------------------------ Mắt bão Darguun

   /** Kỹ năng "Đánh Thường" (tay không + 5 class) — không kích hoạt Mắt bão Darguun. */
   static final Set<Integer> BASIC_ATTACKS = Set.of(1, 2, 8, 14, 20, 26);

   /**
    * Đeo Mắt bão Darguun còn lượt + vừa dùng kỹ năng {@code skillId} không phải đánh thường: -1 lượt, +2000 Né + 20% tốc
    * chạy 5 giây (option 26; dùng lại trong lúc còn hiệu ứng = tính lại 5 giây). Luồng session.
    *
    * @return true nếu vừa kích hoạt (nơi gọi hẹn giờ gỡ hiệu ứng)
    */
   static boolean stormEye(PlayerSession session, PlayerData d, int skillId, World world) {
      if (BASIC_ATTACKS.contains(skillId) || !d.gear.has(GearCharges.Kind.STORM)
            || GearCharges.use(session, d, GearCharges.Kind.STORM, 1) <= 0) {
         return false;
      }
      ItemBuffs.applyProc(session, world, ItemBuffs.STORM_PROC);
      return true;
   }

   // ------------------------------------------------------------------ buff tốc chạy

   /**
    * Cộng % tốc chạy (chỉ số thật, client chạy nhanh hơn), gửi lại khối chỉ số cho cả khu. Hết giờ: xếp việc tắt buff
    * vào hàng đợi session (PlayerData chỉ luồng session được sửa).
    */
   private void startMoveSpeedBuff(PlayerSession session, PlayerData d, int percent, long durationMs) {
      long until = System.currentTimeMillis() + durationMs;
      d.moveSpeedBuffPercent = percent;
      d.moveSpeedBuffUntil = until;
      refreshStats(d);
      scheduler.schedule(() -> session.inbox().execute(() -> endMoveSpeedBuff(session, until), gamePool),
            durationMs, TimeUnit.MILLISECONDS);
   }

   private void endMoveSpeedBuff(PlayerSession session, long until) {
      PlayerData d = session.getPlayer();
      if (d == null || !d.inWorld || d.moveSpeedBuffUntil != until) {
         return; // thoát game / đã dùng lại (buff mới tự hẹn giờ tắt)
      }
      d.moveSpeedBuffPercent = 0;
      refreshStats(d);
   }

   private void refreshStats(PlayerData d) {
      ItemService.refreshLook(d);
      world.broadcastStats(d); // op 23 sub 0: tốc chạy mới cho cả khu (kể cả chính mình)
   }

   // ------------------------------------------------------------------ tính số

   /**
    * Sát thương từng gói STRIKE: {@code separate} = mỗi đòn 1 gói (mỗi đòn dao động riêng), không thì 1 gói tổng
    * ({@code hits} × % mỗi đòn).
    */
   static long[] hits(PlayerStats stats, SkillEffect effect, boolean separate) {
      return hits(stats, effect, separate, false);
   }

   /** Như trên; {@code pvp} = đánh người (sát thương thô chỉ tính Tấn công, {@link #pvpDamage}). */
   static long[] hits(PlayerStats stats, SkillEffect effect, boolean separate, boolean pvp) {
      if (!separate) {
         int percent = effect.damagePercent() * effect.hits();
         return new long[] {pvp ? pvpDamage(stats, percent) : damage(stats, percent)};
      }
      long[] out = new long[effect.hits()];
      for (int i = 0; i < out.length; i++) {
         out[i] = pvp ? pvpDamage(stats, effect.damagePercent()) : damage(stats, effect.damagePercent());
      }
      return out;
   }

   /** Khuếch đại tấn công: mỗi gói × (100 + {@code percent}) / 100. */
   static void amplify(long[] hits, long percent) {
      if (percent <= 0) {
         return;
      }
      for (int i = 0; i < hits.length; i++) {
         hits[i] += hits[i] * percent / 100;
      }
   }

   /**
    * "Sát thương cộng thêm bằng X% HP / MP / Chính xác / Chí mạng / Né tránh" (chiêu cuối) — theo chỉ số TỐI ĐA của
    * người đánh. "Chí mạng" = điểm Chí mạng ({@link PlayerStats#critChance}), không phải Sát thương chí mạng.
    */
   static long bonusDamage(PlayerStats stats, SkillEffect effect) {
      if (effect.bonusStat() == null || effect.bonusPercent() <= 0) {
         return 0;
      }
      long stat = switch (effect.bonusStat()) {
         case "hp" -> stats.maxHp;
         case "mp" -> stats.maxMp;
         case "accuracy" -> stats.accuracy;
         case "crit" -> stats.critChance;
         case "evasion" -> stats.evasion;
         default -> 0;
      };
      return stat * effect.bonusPercent() / 100;
   }

   /** Hiệu ứng lên quái của kỹ năng; null nếu không có. Thời gian: {@code status_ms}, trống thì mặc định theo loại. */
   static ZoneCombat.Status status(SkillEffect effect) {
      MobStatus kind = MobStatus.of(effect.status());
      if (kind == null) {
         return null;
      }
      int ms = effect.statusMs() > 0 ? effect.statusMs() : DEFAULT_STATUS_MS.getOrDefault(kind, 0);
      return new ZoneCombat.Status(kind, ms, effect.statusPower());
   }

   /** (Tấn công + Tấn công quái) × {@code percent}%, dao động ±{@value #DAMAGE_SPREAD_PERCENT}%, tối thiểu 1. */
   static long damage(PlayerStats stats, int percent) {
      return roll((stats.attackPower + stats.attackVsMob) * percent / 100);
   }

   /**
    * Sát thương THÔ lên người chơi: Tấn công × {@code percent}% (không cộng "Tấn công lên quái"), dao động như trên.
    * Giáp / kháng của người bị đánh trừ sau, trên luồng tick khu ({@link PvpMath}).
    */
   static long pvpDamage(PlayerStats stats, int percent) {
      return roll(stats.attackPower * percent / 100);
   }

   private static long roll(long base) {
      int spread = ThreadLocalRandom.current().nextInt(-DAMAGE_SPREAD_PERCENT, DAMAGE_SPREAD_PERCENT + 1);
      return Math.max(1, base + base * spread / 100);
   }

   // ------------------------------------------------------------------ nội bộ

   /** Cộng {@code amount} HP, không vượt tối đa. */
   private static void heal(PlayerData d, PlayerStats stats, long amount) {
      d.healHp(amount, stats.maxHp);
   }

   /** HP + MP sau khi trừ MP / hồi HP (op 23 sub 3). Chỉ số đọc lại vì buff có thể vừa đổi {@link PlayerData#stats}. */
   private static void sendHpMp(PlayerSession session, PlayerData d) {
      PlayerStats stats = d.stats;
      session.send(ItemPackets.hpMp(d.getId(), d.hp, stats.maxHp, d.mp, stats.maxMp));
   }

   /**
    * Class của vũ khí đang cầm = class kỹ năng ({@code skill_template.class_id}) mới dùng được. Tay không / vũ
    * khí không có loại (Cần câu) = {@link WeaponClass#NONE} = class của skill 1 "Đánh Thường".
    */
   private static int heldWeaponClass(PlayerData d) {
      return WeaponClass.of(d.body.get(Appearance.SLOT_WEAPON));
   }

   private static long manaCost(SkillPack.Skill template, SkillPack.SkillLevel level, PlayerStats stats) {
      return switch (template.manaCostType()) {
         case MANA_FIXED -> level.manaCost();
         case MANA_PERCENT -> stats.maxMp * level.manaCost() / 100;
         default -> 0;
      };
   }
}
