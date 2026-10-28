package com.tlnt.server.world;

import com.tlnt.server.map.MapTemplate;

/**
 * 1 boss thế giới đang đứng trong 1 khu ({@link World#spawnWorldBoss}): hoặc boss QUÁI (Vệ thần Rừng xanh — 1 chỗ
 * {@link ZoneMobs#addManual} + AI {@link LinhMocMobBoss#world} ở {@code Zone.mobBoss}), hoặc boss người chơi giả (Tinh
 * Linh Rừng xanh — {@link AltarBoss} chế độ {@code smart} ở {@code Zone.boss}). Chết đường nào cũng tới
 * {@code ZoneCombat.worldBossCheck}: quà cho người gây nhiều sát thương nhất + báo {@link World.WorldBossSpawn#listener}.
 * CHỈ luồng tick của khu đụng tới.
 */
final class WorldBoss {

   final World.WorldBossSpawn request;
   /** Boss người chơi giả; null = boss quái. */
   final AltarBoss charBoss;
   /** Id quái trong {@link ZoneMobs} (boss quái); -1 = boss người chơi giả. */
   final int mobId;
   /** AI boss quái (= {@code Zone.mobBoss} lúc còn sống); null = boss người chơi giả. */
   final LinhMocMobBoss mobAi;
   /** Đã phát quà + báo chết (chỉ 1 lần). */
   boolean done;
   /** Người ra đòn kết liễu (nhận quà); 0 = chết không do đòn của ai (lệnh test) -> quà rơi tự do. */
   int lastHitId;
   String lastHitName = "";

   /** Ghi người ra đòn kết liễu. */
   void killedBy(WorldPlayer player) {
      if (lastHitId == 0 && player != null) {
         lastHitId = player.id();
         lastHitName = player.data().name;
      }
   }

   WorldBoss(World.WorldBossSpawn request, AltarBoss charBoss) {
      this.request = request;
      this.charBoss = charBoss;
      this.mobId = -1;
      this.mobAi = null;
   }

   WorldBoss(World.WorldBossSpawn request, int mobId, LinhMocMobBoss mobAi) {
      this.request = request;
      this.charBoss = null;
      this.mobId = mobId;
      this.mobAi = mobAi;
   }

   /** Sổ sát thương từng người lên boss. */
   DamageLedger ledger() {
      return charBoss != null ? charBoss.damageLedger : mobAi.damage;
   }

   boolean dead(Zone zone) {
      return charBoss != null ? charBoss.dead : zone.mobs().alive(mobId) == null;
   }

   /** Là boss quái có id {@code id}. */
   boolean isMob(int id) {
      return charBoss == null && mobId == id;
   }

   /** Vị trí (boss quái: điểm sinh — server không biết chỗ lang thang thật). */
   float x(Zone zone) {
      if (charBoss != null) {
         return charBoss.x;
      }
      MapTemplate.MobInstance mob = zone.mobs().instance(mobId);
      return mob == null ? 0 : mob.x();
   }

   float y(Zone zone) {
      if (charBoss != null) {
         return charBoss.y;
      }
      MapTemplate.MobInstance mob = zone.mobs().instance(mobId);
      return mob == null ? 0 : mob.y();
   }
}
