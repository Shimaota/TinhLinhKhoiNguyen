package com.tlnt.server.world;

import com.tlnt.server.linhmoc.LinhMocRules;
import com.tlnt.server.map.MapTemplate;

import java.util.ArrayList;

/**
 * Boss QUÁI Hắc thần Linh mộc của 1 khu Rừng Linh Mộc ({@code linhmoc.bossMob}, mặc định mob 49 trong
 * {@code map_template.mobs} map 29). HP / cấp / hệ số sát thương như mọi quái phó bản ({@code linhmoc.mobs}: HP ×15,
 * ST ×4,5 — {@link LinhMocRules}). Dùng chung vòng đánh của quái thường ({@code ZoneCombat.mobAttacks}: khoá 1 mục tiêu,
 * bỏ khi người đó chạy xa / chết / rời khu), khác 2 điểm: KHÔNG cần bị đánh trước — đang rảnh thì tự khoá người gần nhất
 * trong tầm ({@link #tick}); nhịp {@code linhmoc.bossAttackMs} (2 s) và mỗi nhịp ra chiêu theo vòng (tầm tính quanh điểm
 * sinh — server không biết chỗ thật vì client cho quái
 * đi lang thang {@code wander_range}):
 * <ul>
 *   <li>{@link #ATTACK} đánh thường: op 20 sub 1 vào mục tiêu — client tự chọn {@code skill1} (bắn đạn, ở xa)
 *       hoặc {@code skill2} (cận chiến), {@link #ATTACK_PERCENT}%.</li>
 *   <li>{@link #PUSH} {@code skill3}: op 20 sub 5 (quay về phía mục tiêu, rung mạnh) -> người PHÍA TRƯỚC mặt boss mất
 *       {@link #PUSH_PERCENT}% + bị ĐẨY VĂNG ra xa (op 12 sub 2, vận tốc ngang hướng ra ngoài).</li>
 *   <li>{@link #SLAM} {@code skill4}: op 20 sub 6 (đập xuống, rung nhẹ) -> mọi người quanh boss mất {@link #SLAM_PERCENT}%
 *       + bị HẤT TUNG tại chỗ.</li>
 * </ul>
 * Vòng chiêu {@link #ROTATION}. CHỈ luồng tick của khu đụng tới.
 *
 * <p>Boss thế giới Vệ thần Rừng xanh ({@link #world}) dùng chung lớp này: sát thương = Tấn công × % chiêu × giáp ±5%
 * ({@code ZoneCombat.worldMobBossHit}), lúc đập theo skeleton {@code mobs/Forest_guard} và chọn chiêu theo tình huống
 * ({@link #smartMove}) thay vì vòng cố định.
 */
final class LinhMocMobBoss {

   static final int ATTACK = 1;
   static final int PUSH = 3;
   static final int SLAM = 4;
   static final int[] ROTATION = {ATTACK, ATTACK, PUSH, ATTACK, SLAM};
   static final int ATTACK_PERCENT = 100;
   static final int PUSH_PERCENT = 150;
   static final int SLAM_PERCENT = 200;
   /**
    * Lúc đập xuống trong animation (skeleton Forest_Elite_lv3: skill3 dài 1,333 s, skill4 1,433 s; client rung màn hình ở
    * 50%) — đòn trúng + bay lúc này, không sớm hơn (trước đây 0,5 s: nhân vật bay trước khi boss đập).
    */
   static final long PUSH_HIT_MS = 667;
   static final long SLAM_HIT_MS = 717;
   /** Skill3: ước boss đứng cách mục tiêu bấy nhiêu px (phía sau theo hướng quay); đẩy người phía trước trong tầm này. */
   static final float BOSS_OFFSET_PX = 150f;
   static final float PUSH_FRONT_PX = 650f;
   /** Tầm chiêu diện rộng quanh người đang bị nhắm (đứng sát boss thật). */
   static final float AREA_X_PX = 500f;
   static final float AREA_Y_PX = 250f;
   /** Đẩy văng (Box2D client, m/s): ngang ra xa + nảy nhẹ; hất tung: nảy ~2 m tại chỗ. */
   static final float PUSH_X_MPS = 14f;
   static final float PUSH_Y_MPS = 6f;
   static final float KNOCK_UP_MPS = 14f;
   static final int KNOCK_MS = 600;
   /** Boss rảnh: bấy lâu xem lại 1 lần có ai bước vào tầm không. */
   static final long IDLE_SCAN_MS = 250;
   /**
    * Vệ thần Rừng xanh (skeleton {@code mobs/Forest_guard}): skill3 dài 1,333 s (client rung ở 50%), skill4 1,667 s có
    * sự kiện {@code damage} ở 0,933 s.
    */
   static final long GUARD_PUSH_HIT_MS = 667;
   static final long GUARD_SLAM_HIT_MS = 933;
   /** Chọn chiêu thông minh: số nhịp đánh tối thiểu giữa 2 lần đẩy / đập, và tối đa (đứng 1 mình vẫn bị). */
   static final int PUSH_MIN_GAP = 2;
   static final int PUSH_MAX_GAP = 4;
   static final int SLAM_MIN_GAP = 3;
   static final int SLAM_MAX_GAP = 6;

   /** Sát thương 1 đòn boss lên người (đã phát gói, xử lý bị hạ gục). @return HP người còn */
   interface Hit {
      long hit(Zone zone, int mobId, MapTemplate.MobInstance mob, WorldPlayer target, int percent, boolean attackAnim);
   }

   /** Id thực thể của boss trong {@link ZoneMobs} (= vị trí trong danh sách quái của map). */
   final int mobId;
   final long intervalMs;
   /** Sát thương từng người lên boss (người gây nhiều nhất nhận quà khi boss chết không do đòn của ai). */
   final DamageLedger damage = new DamageLedger();
   /** Người ra đòn kết liễu (nhận quà Rừng Linh Mộc); 0 = chưa / chết không do đòn của ai. */
   int lastHitId;
   private final Hit hit;
   private final long pushHitMs;
   private final long slamHitMs;
   /** true = chọn chiêu theo tình huống ({@link #smartMove}), false = vòng {@link #ROTATION}. */
   private final boolean smart;
   private long nextScanAt;
   private int rotation;
   /** Số nhịp đánh từ lần đẩy / đập gần nhất (chế độ thông minh). */
   private int sincePush;
   private int sinceSlam;

   LinhMocMobBoss(int mobId, long intervalMs) {
      this(mobId, intervalMs, ZoneCombat::linhMocBossHit, PUSH_HIT_MS, SLAM_HIT_MS, false);
   }

   private LinhMocMobBoss(int mobId, long intervalMs, Hit hit, long pushHitMs, long slamHitMs, boolean smart) {
      this.mobId = mobId;
      this.intervalMs = intervalMs;
      this.hit = hit;
      this.pushHitMs = pushHitMs;
      this.slamHitMs = slamHitMs;
      this.smart = smart;
   }

   /** Boss thế giới dạng quái (Vệ thần Rừng xanh): mỗi {@code intervalMs} 1 chiêu, đòn thường = {@code attack}. */
   static LinhMocMobBoss world(int mobId, long intervalMs, long attack) {
      return world(mobId, intervalMs, attack, 0);
   }

   /**
    * Như trên; {@code slamHitMs} = lúc skill4 (đập đất) trúng, 0 = {@link #GUARD_SLAM_HIT_MS} (Vệ thần Sấm sét: 717 —
    * animation 1,433 s, client rung ở 50%).
    */
   static LinhMocMobBoss world(int mobId, long intervalMs, long attack, long slamHitMs) {
      return new LinhMocMobBoss(mobId, intervalMs,
            (zone, id, mob, target, percent, anim) -> ZoneCombat.worldMobBossHit(zone, id, mob, target, attack, percent,
                  anim),
            GUARD_PUSH_HIT_MS, slamHitMs > 0 ? slamHitMs : GUARD_SLAM_HIT_MS, true);
   }

   /** Boss quái của khu (null = map không có con {@code linhmoc.bossMob}). */
   static LinhMocMobBoss find(ZoneMobs mobs) {
      int bossTemplate = LinhMocRules.config().bossMobId();
      for (int id = 0; id < mobs.size(); id++) {
         MapTemplate.MobInstance mob = mobs.alive(id);
         if (mob != null && mob.spawn().id() == bossTemplate) {
            return new LinhMocMobBoss(id, LinhMocRules.config().bossAttackMs());
         }
      }
      return null;
   }

   /**
    * Boss đang rảnh (chưa khoá ai): mỗi {@link #IDLE_SCAN_MS} tìm người gần nhất trong tầm (quanh điểm sinh + lang thang,
    * như quái thường) -> khoá làm mục tiêu ({@code ZoneMobs.provoke}). Đã khoá thì không làm gì — nhịp đánh, bỏ mục tiêu
    * (chạy xa / chết / rời khu) do vòng đánh chung của quái lo ({@code ZoneCombat.mobAttacks} -> {@link #attack}).
    */
   void tick(Zone zone, long now) {
      ZoneMobs mobs = zone.mobs();
      if (mobs.aggroTarget(mobId) != 0 || now < nextScanAt || !com.tlnt.server.combat.MobRules.mobAttack()) {
         return;
      }
      nextScanAt = now + IDLE_SCAN_MS;
      MapTemplate.MobInstance mob = mobs.alive(mobId);
      if (mob == null) {
         return;
      }
      lockNearest(zone, mob, now);
   }

   /**
    * Khoá người còn sống gần nhất trong tầm (thêm vào danh sách thù). Gọi khi boss rảnh ({@link #tick}) hoặc khi mọi người
    * đã đánh nó đều đang không cắn được ({@code ZoneCombat.mobAttacks}).
    */
   void lockNearest(Zone zone, MapTemplate.MobInstance mob, long now) {
      WorldPlayer nearest = null;
      float best = Float.MAX_VALUE;
      for (WorldPlayer p : zone.players()) {
         float d = Math.abs(p.x - mob.x()) + Math.abs(p.y - mob.y());
         if (valid(p) && ZoneCombat.inLeash(p, mob) && d < best) {
            nearest = p;
            best = d;
         }
      }
      if (nearest != null) {
         zone.mobs().provoke(mobId, nearest.id(), now);
      }
   }

   /** 1 nhịp đánh vào mục tiêu đang khoá: chiêu kế tiếp trong {@link #ROTATION}. Luồng tick. */
   void attack(Zone zone, MapTemplate.MobInstance mob, WorldPlayer target) {
      int move = smart ? smartMove(zone, mob, target) : ROTATION[rotation++ % ROTATION.length];
      switch (move) {
         case PUSH -> { // quay về phía mục tiêu, 0,667 s sau đập xuống: đẩy văng người PHÍA TRƯỚC mặt boss
            int dir = target.x < mob.x() ? -1 : 1;
            float bossX = target.x - dir * BOSS_OFFSET_PX; // ước chỗ boss: sát mục tiêu, phía sau theo hướng quay
            float bossY = target.y;
            zone.broadcast(WorldPackets.mobSkill3(mobId, dir).toPacket(), null);
            zone.schedule(pushHitMs, () -> push(zone, dir, bossX, bossY));
         }
         case SLAM -> { // 0,717 s sau đập xuống: hất tung mọi người xung quanh
            float cx = target.x;
            float cy = target.y;
            zone.broadcast(WorldPackets.mobSkill4(mobId).toPacket(), null);
            zone.schedule(slamHitMs, () -> slam(zone, cx, cy));
         }
         default -> hit.hit(zone, mobId, mob, target, ATTACK_PERCENT, true);
      }
   }

   /**
    * Chọn chiêu theo tình huống (boss thế giới): đông người quanh mục tiêu -> đập đất hất tung ({@link #SLAM}); đông người
    * phía trước -> đẩy văng ({@link #PUSH}); không thì đánh thường. Mỗi chiêu lớn cách nhau ít nhất / nhiều nhất bấy
    * nhiêu nhịp ({@link #SLAM_MIN_GAP}..{@link #SLAM_MAX_GAP}, {@link #PUSH_MIN_GAP}..{@link #PUSH_MAX_GAP}) — đứng 1
    * mình vẫn ăn chiêu lớn, đông người thì ăn sớm hơn.
    */
   int smartMove(Zone zone, MapTemplate.MobInstance mob, WorldPlayer target) {
      int around = 0;
      int front = 0;
      int dir = target.x < mob.x() ? -1 : 1;
      for (WorldPlayer p : zone.players()) {
         if (!valid(p) || Math.abs(p.y - target.y) > AREA_Y_PX) {
            continue;
         }
         if (Math.abs(p.x - target.x) <= AREA_X_PX) {
            around++;
         }
         float ahead = (p.x - (target.x - dir * BOSS_OFFSET_PX)) * dir;
         if (ahead >= -40f && ahead <= PUSH_FRONT_PX) {
            front++;
         }
      }
      sincePush++;
      sinceSlam++;
      int move = ATTACK;
      if (sinceSlam > SLAM_MAX_GAP || sinceSlam > SLAM_MIN_GAP && around >= 2) {
         move = SLAM;
      } else if (sincePush > PUSH_MAX_GAP || sincePush > PUSH_MIN_GAP && front >= 2) {
         move = PUSH;
      }
      if (move == SLAM) {
         sinceSlam = 0;
      } else if (move == PUSH) {
         sincePush = 0;
      }
      return move;
   }

   /**
    * Skill3 (đẩy): người PHÍA TRƯỚC mặt boss (hướng {@code dir} = hướng boss quay, gói op 20 sub 5) trong
    * {@link #PUSH_FRONT_PX} tính từ chỗ boss ({@code bossX}, ước sát mục tiêu) bị trúng + ĐẨY VĂNG ra xa theo hướng đó;
    * người đứng sau lưng boss không sao.
    */
   private void push(Zone zone, int dir, float bossX, float bossY) {
      MapTemplate.MobInstance mob = zone.mobs().alive(mobId);
      if (mob == null) { // boss chết giữa chiêu
         return;
      }
      for (WorldPlayer p : new ArrayList<>(zone.players())) {
         float ahead = (p.x - bossX) * dir; // > 0 = phía trước mặt boss
         if (!valid(p) || ahead < -40f || ahead > PUSH_FRONT_PX || Math.abs(p.y - bossY) > AREA_Y_PX) {
            continue;
         }
         long left = hit.hit(zone, mobId, mob, p, PUSH_PERCENT, false);
         if (left > 0) {
            ZoneCombat.knock(p, dir * PUSH_X_MPS, PUSH_Y_MPS, KNOCK_MS);
         }
      }
   }

   /** Skill4 (đập đất): mọi người quanh {@code (cx, cy)} (người bị nhắm — sát boss) bị trúng + HẤT TUNG ~2 m tại chỗ. */
   private void slam(Zone zone, float cx, float cy) {
      MapTemplate.MobInstance mob = zone.mobs().alive(mobId);
      if (mob == null) {
         return;
      }
      for (WorldPlayer p : new ArrayList<>(zone.players())) {
         if (!valid(p) || Math.abs(p.x - cx) > AREA_X_PX || Math.abs(p.y - cy) > AREA_Y_PX) {
            continue;
         }
         long left = hit.hit(zone, mobId, mob, p, SLAM_PERCENT, false);
         if (left > 0) {
            ZoneCombat.knock(p, 0, KNOCK_UP_MPS, KNOCK_MS);
         }
      }
   }

   private static boolean valid(WorldPlayer p) {
      return p != null && !p.removed && !p.locked && !p.data().isDead();
   }
}
