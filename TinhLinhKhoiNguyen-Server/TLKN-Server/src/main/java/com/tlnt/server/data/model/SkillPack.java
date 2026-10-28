package com.tlnt.server.data.model;

import java.util.List;

/**
 * Nội dung file DataGame {@code skill} (xem {@code GameDataLoader.loadSkillData} phía client).
 */
public record SkillPack(
      int version,
      List<TalentPointCap> talentPointCaps,
      List<Talent> talents,
      List<Skill> skills) {

   public record TalentPointCap(int id, int requiredLevel, int[] caps) {
   }

   public record Talent(int id, String name, String icon, String description, int pointCost) {
   }

   /** {@code levels} đã sắp theo thứ tự trong file (level 1 -> n). */
   /**
    * {@code targetType} 1 = cần mục tiêu, 2 = tự dùng; {@code classId} = class sở hữu kỹ năng (cột {@code class_id}, trước
    * đây tên sai {@code unlock_level}); {@code manaCostType} 1 = MP cố định, 2 = % MP tối đa.
    */
   public record Skill(int id, String name, int targetType, int classId, int manaCostType,
         String description, String[] iconPaths, List<SkillLevel> levels) {
   }

   /**
    * 1 cấp kỹ năng. {@code goldCost} / {@code expCost} (cột {@code gold_cost} / {@code exp_cost}): vàng + kinh nghiệm
    * tiêu hao khi nâng LÊN cấp này ("Tiêu hao: X Vàng và Y Kinh nghiệm" ở TalentPanel) — trong file là 2 long sau
    * upgradeCost (decompile C3420 / C0169; trước 2026-10-13 bị đặt nhầm là 4 int extra1..4).
    */
   public record SkillLevel(int level, int rangeX, int rangeY, int manaCost, int cooldownMillis,
         int requiredLevel, int upgradeCost, long goldCost, long expCost, String effectDescription) {
   }
}
