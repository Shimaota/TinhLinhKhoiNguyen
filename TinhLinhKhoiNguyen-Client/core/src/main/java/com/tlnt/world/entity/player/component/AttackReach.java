package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.model.skill.SkillLevelData;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.player.PlayerEntity;

/**
 * Single-target attack reach, same model as the server ({@code ZoneCombat.inReach}): a BOX |dx| ≤ rangeX, |dy| ≤ rangeY (+ tolerance).
 * The client used to use straight-line distance against {@code SkillLevelData.getRange()} (≈ rangeX only) -> standing on a platform above
 * a mob, within the server's reach, the client still considered it "not there yet" and kept walking back and forth.
 */
final class AttackReach {

   /** The server allows 200 px of tolerance; the client uses 100 px to be sure to hit. */
   private static final float VERTICAL_SLACK = 1.0F;
   private static final float MIN_HORIZONTAL = 0.3F;

   private AttackReach() {
   }

   /** Horizontal reach (world units = px / 100). */
   static float horizontal(SkillLevelData level) {
      return Math.max(MIN_HORIZONTAL, level.rangeX / 100.0F);
   }

   /** Melee skills: Y reach ≤ 150 px (Axe / Spear / Whip / unarmed basic attack). */
   private static final int MELEE_MAX_RANGE_Y = 150;
   /** Melee + mob LOWER than the player: only attack when the offset is ≤ this (same floor) — the player must drop to the mob's floor. */
   private static final float MELEE_BELOW_LIMIT = 0.5F;

   /** Maximum height offset at which {@code player} can still hit {@code target}. */
   static float verticalLimit(Vector2 player, Vector2 target, SkillLevelData level) {
      float limit = level.rangeY / 100.0F + VERTICAL_SLACK;
      if (level.rangeY <= MELEE_MAX_RANGE_Y && target.y < player.y) {
         return Math.min(limit, MELEE_BELOW_LIMIT);
      }
      return limit;
   }

   /** Height offset beyond the Y reach (+ tolerance) — must change floors to attack. */
   static boolean tooHighOrLow(Vector2 player, Vector2 target, SkillLevelData level) {
      return Math.abs(target.y - player.y) > verticalLimit(player, target, level);
   }

   static boolean inReach(PlayerEntity player, WorldEntity target, SkillLevelData level) {
      Vector2 p = player.getPosition();
      Vector2 t = target.getPosition();
      return Math.abs(t.x - p.x) <= horizontal(level) && Math.abs(t.y - p.y) <= verticalLimit(p, t, level);
   }
}
