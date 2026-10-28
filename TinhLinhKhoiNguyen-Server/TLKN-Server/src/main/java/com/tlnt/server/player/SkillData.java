package com.tlnt.server.player;

/**
 * Kỹ năng đã học (opcode 21 sub 1 phía client: {@code LearnedSkill(skillId, level, now - elapsed)}).
 * Lưu thời điểm dùng gần nhất để hồi chiêu còn đúng sau khi đăng nhập lại.
 */
public final class SkillData {

   public int skillId;
   public byte level;
   public long lastUsedAt;

   public SkillData() {
   }

   public SkillData(int skillId, byte level) {
      this.skillId = skillId;
      this.level = level;
   }
}
