package com.tlnt.server.world;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.SkillEffect;
import com.tlnt.server.data.model.SkillPack;

/** Metadata phạm vi / hồi chiêu lấy từ skill_level, tác dụng lấy từ skill_effect. */
final class BossSkillData {

   private BossSkillData() {
   }

   static float rangeX(int skillId, float fallback) {
      SkillPack.SkillLevel level = level(skillId);
      return level == null || level.rangeX() <= 0 ? fallback : level.rangeX();
   }

   static float rangeY(int skillId, float fallback) {
      SkillPack.SkillLevel level = level(skillId);
      return level == null || level.rangeY() <= 0 ? fallback : level.rangeY();
   }

   static long cooldownMillis(int skillId, long fallback) {
      SkillPack.SkillLevel level = level(skillId);
      return level == null || level.cooldownMillis() <= 0 ? fallback : level.cooldownMillis();
   }

   static boolean hasCooldown(int skillId) {
      SkillPack.SkillLevel level = level(skillId);
      return level != null && level.cooldownMillis() > 0;
   }

   static SkillEffect effect(int skillId) {
      return GameDataCache.skillEffect(skillId, 1);
   }

   /** skill_effect is authoritative when present; zero is meaningful (for example, a pure debuff). */
   static int damagePercent(int skillId, int fallback) {
      SkillEffect effect = effect(skillId);
      if (effect != null) {
         return effect.damagePercent();
      }
      return switch (skillId) {
         case 35, 36, 37, 39, 43 -> 0; // khống chế / buff / hút máu dùng thông số riêng, không lấy % đòn cũ
         case 40 -> 500;
         case 41 -> 1_000;
         default -> fallback;
      };
   }

   /** Zero target cap means all targets in range. */
   static int targetCount(int skillId, int fallback) {
      SkillEffect effect = effect(skillId);
      if (effect != null) {
         return Math.max(0, effect.aoeTargets());
      }
      return switch (skillId) {
         case 35, 36, 38 -> 3;
         case 39 -> 0;
         case 43 -> 5;
         default -> fallback;
      };
   }

   static int statusDurationMs(int skillId, int fallback) {
      SkillEffect effect = effect(skillId);
      if (effect == null) {
         return switch (skillId) {
            case 35, 36, 38, 39 -> 15_000;
            default -> fallback;
         };
      }
      if (effect.statusMs() > 0) {
         return effect.statusMs();
      }
      return effect.durationMs() > 0 ? effect.durationMs() : fallback;
   }

   static int durationMs(int skillId, int fallback) {
      SkillEffect effect = effect(skillId);
      if (effect == null) {
         return skillId == 37 ? 15_000 : fallback;
      }
      if (effect.durationMs() > 0) {
         return effect.durationMs();
      }
      return effect.statusMs() > 0 ? effect.statusMs() : fallback;
   }

   static int statusPower(int skillId, int fallback) {
      SkillEffect effect = effect(skillId);
      if (effect != null) {
         return effect.statusPower();
      }
      return switch (skillId) {
         case 35 -> 10; // 1% HP tối đa mỗi giây; theo cấu hình rễ boss thế giới hiện có
         case 36 -> 50;
         default -> fallback;
      };
   }

   static int aoePercent(int skillId, int fallback) {
      SkillEffect effect = effect(skillId);
      return effect == null || effect.aoePercent() <= 0 ? fallback : effect.aoePercent();
   }

   static int lifestealPercent(int skillId, int fallback) {
      SkillEffect effect = effect(skillId);
      return effect == null ? (skillId == 35 || skillId == 43 ? 100 : fallback) : effect.lifestealPercent();
   }

   static int damageReductionPercent(int skillId, int fallback) {
      SkillEffect effect = effect(skillId);
      return effect == null ? (skillId == 37 ? 75 : fallback) : effect.damageReductionPercent();
   }

   static int hits(int skillId, int fallback) {
      SkillEffect effect = effect(skillId);
      return effect == null ? (skillId == 40 ? 50 : fallback) : Math.max(1, effect.hits());
   }

   private static SkillPack.SkillLevel level(int skillId) {
      return GameDataCache.skillLevel(skillId, 1);
   }
}
