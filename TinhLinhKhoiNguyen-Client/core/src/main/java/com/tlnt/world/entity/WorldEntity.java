package com.tlnt.world.entity;

import com.tlnt.core.GameState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.BaseScreen;
import com.tlnt.world.entity.combattext.FloatingCombatText;
import com.tlnt.world.entity.player.PlayerEntity;

public abstract class WorldEntity extends GameObject implements ObjectPool.Poolable {

   public WorldEntity(BaseScreen screen, int id, EntityType entityType) {
      super(id, screen, entityType);
   }

   public void onHit(WorldEntity attacker, long currentHp, long damage, boolean isHeal) {
      if (attacker != null && shouldFaceVictim(attacker)) {
         attacker.faceTowards(this.getPosition());
      }

      if (damage != 0L) {
         FloatingCombatText.POOL2
            .obtain()
            .init("-" + damage, isHeal ? FloatingCombatText.CombatTextType.HEAL : FloatingCombatText.CombatTextType.CRITICAL, this.getCenterPosition());
      } else {
         FloatingCombatText combatText = FloatingCombatText.POOL2.obtain();
         String text;
         if (this.equals(PlayerEntity.getInstance())) {
            text = "né";
         } else {
            text = "hụt";
         }

         combatText.init(text, FloatingCombatText.CombatTextType.BONUS, this.getCenterPosition());
      }
   }

   /**
    * When the local player hits several targets (area skills) it only turns towards the SELECTED TARGET — it used to turn
    * towards each target hit, with mobs on both sides -> flipping left / right every hit, and the camera look-ahead jerked.
    * Targets at almost the same x do not flip it either (the sign of dx is just noise).
    */
   private boolean shouldFaceVictim(WorldEntity attacker) {
      if (attacker != PlayerEntity.getInstance()) {
         return true;
      }
      if (GameState.targetEntity != null && GameState.targetEntity != this && !GameState.targetEntity.isDead()) {
         return false;
      }
      return Math.abs(this.getPosition().x - attacker.getPosition().x) > 0.1F;
   }

   public boolean isDead() {
      return false;
   }

   public boolean isAlive() {
      return true;
   }

   public float getHpPercent() {
      return 0.0F;
   }

   @Override
   public void reset() {
   }

}
