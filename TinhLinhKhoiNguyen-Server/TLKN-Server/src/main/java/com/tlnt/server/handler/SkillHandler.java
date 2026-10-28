package com.tlnt.server.handler;

import com.tlnt.server.combat.CombatService;
import com.tlnt.server.net.Message;
import com.tlnt.server.net.PacketHandler;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.skill.SkillService;

/**
 * Opcode 21 C→S: đánh / kỹ năng / tiềm năng. Tên method client SAI nghĩa:
 *
 * <pre>
 * sub | method client       | nút (nơi bấm)                                     | payload
 * 0   | sendSkillbarAction | Đánh khi đã chọn mục tiêu (tới đủ gần thì gửi)    | int skillId, byte loại mục tiêu
 *     |                    |                                                   | (0 quái, 1 người, -1 không), int id
 * 1   | sendSkillUse       | bấm kỹ năng trên thanh HUD (chọn kỹ năng)         | int skillId
 * 2   | sendAttack         | TalentPanel: nâng cấp tiềm năng (nhập số lần)     | int talentId, int số lần
 * 3   | sendDropItem       | TalentPanel: nâng cấp kỹ năng đang chọn           | int skillId
 * 4   | sendEquipItem      | SkillSlotPopup: gán kỹ năng vào "Ô kỹ năng số N"  | int skillId, byte ô (0..5)
 * </pre>
 */
public final class SkillHandler implements PacketHandler {

   private final SkillService skills;
   private final CombatService combat;

   public SkillHandler(SkillService skills, CombatService combat) {
      this.skills = skills;
      this.combat = combat;
   }

   @Override
   public void handle(PlayerSession session, Message message) {
      if (session.getPlayer() == null) {
         return;
      }
      switch (message.readByte()) {
         case 0 -> combat.useSkill(session, message.readInt(), message.readByte(), message.readInt());
         case 1 -> skills.select(session, message.readInt());
         case 2 -> skills.addTalent(session, message.readInt(), message.readInt());
         case 3 -> skills.upgradeSkill(session, message.readInt());
         case 4 -> skills.assign(session, message.readInt(), message.readByte());
         default -> {
         }
      }
   }
}
