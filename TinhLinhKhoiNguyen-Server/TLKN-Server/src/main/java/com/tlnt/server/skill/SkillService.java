package com.tlnt.server.skill;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.SkillPack;
import com.tlnt.server.item.ItemService;
import com.tlnt.server.item.WeaponClass;
import com.tlnt.server.net.Packets;
import com.tlnt.server.player.ClassSkills;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.SkillData;
import com.tlnt.server.quest.QuestEvents;
import com.tlnt.server.quest.StepKind;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.world.World;
import com.tlnt.server.world.WorldPackets;

import java.util.List;

/**
 * Bảng "Tiềm năng / Kỹ năng" (TalentPanel) + thanh kỹ năng HUD. Chạy lane GAME, tuần tự theo session (như ItemService).
 *
 * <ul>
 *   <li>Cộng tiềm năng: mỗi điểm tốn {@code talent.point_cost} điểm tiềm năng ({@link PlayerData#talentPoints}), tối
 *       đa theo {@code talent_point_cap} (mốc đầu tiên còn chưa vượt; cấp nhân vật phải ≥ {@code required_level}
 *       của mốc đó). Cộng chỉ số ({@link TalentStats}).</li>
 *   <li>Nâng kỹ năng: lên cấp kế tiếp trong {@code skill_level}: cấp nhân vật ≥ {@code required_level}, tốn
 *       {@code upgrade_cost} điểm tiềm năng + tiêu hao vàng (vàng khoá trước) và kinh nghiệm của cấp hiện tại
 *       ({@code skill_level.gold_cost} / {@code exp_cost}). Cấp 0 ("Chưa học") -> 1 =
 *       học.</li>
 *   <li>Gán kỹ năng: 6 ô thanh kỹ năng; kỹ năng phải học rồi (cấp ≥ 1).</li>
 * </ul>
 */
public final class SkillService {

   private final World world;

   public SkillService(World world) {
      this.world = world;
   }

   /** Op 21 sub 1 (bấm kỹ năng trên thanh HUD): chọn kỹ năng dùng cho đòn đánh. */
   public void select(PlayerSession session, int skillId) {
      PlayerData d = player(session);
      if (d == null || level(d, skillId) < 1 || !ClassSkills.onBar(d, skillId)) {
         return;
      }
      session.send(WorldPackets.selectedSkill(skillId));
   }

   /** Op 21 sub 2 (TalentPanel "Nâng cấp" tiềm năng, nhập số lần): cộng {@code times} điểm vào tiềm năng. */
   public void addTalent(PlayerSession session, int talentId, int times) {
      PlayerData d = player(session);
      SkillPack.Talent talent = d == null ? null : talent(talentId);
      if (talent == null || times <= 0) {
         return;
      }
      if (d.talents == null || d.talents.length < talentId) {
         d.talents = java.util.Arrays.copyOf(d.talents == null ? new int[0] : d.talents,
               GameDataCache.skill().talents().size());
      }
      int cost = Math.max(1, talent.pointCost());
      int added = 0;
      String stop = null;
      synchronized (d) { // điểm tiềm năng còn được luồng tick khu cộng khi lên cấp (Leveling.addExp, cùng khoá)
         while (added < times) {
            SkillPack.TalentPointCap cap = nextCap(d.talents[talentId - 1], talentId);
            if (cap == null) {
               stop = talent.name() + " đã đạt cấp độ tối đa.";
               break;
            }
            if (d.level < cap.requiredLevel()) {
               stop = "Cần đạt cấp " + cap.requiredLevel() + " để nâng tiếp " + talent.name() + ".";
               break;
            }
            if (d.talentPoints < cost) {
               stop = "Không đủ điểm tiềm năng.";
               break;
            }
            d.talentPoints -= cost;
            d.talents[talentId - 1]++;
            added++;
         }
      }
      if (added > 0) {
         session.send(Packets.notice(talent.name() + " " + TalentStats.bonusText(talentId, added)));
         refreshStats(d);
         QuestEvents.fire(session, StepKind.ADD_TALENT, 0);
      } else if (stop != null) {
         session.send(Packets.notice(stop));
      }
   }

   /** Op 21 sub 3 (TalentPanel nút nâng cấp kỹ năng): lên 1 cấp. */
   public void upgradeSkill(PlayerSession session, int skillId) {
      PlayerData d = player(session);
      SkillData learned = d == null ? null : find(d, skillId);
      SkillPack.Skill template = GameDataCache.skillTemplate(skillId);
      if (learned == null || template == null) {
         return;
      }
      int owner = template.classId();
      if (skillId != ClassSkills.BASIC_ATTACK && owner != d.classId && owner != WeaponClass.NONE) {
         session.send(Packets.notice("Kỹ năng này không thuộc class của bạn."));
         return;
      }
      SkillPack.SkillLevel next = levelData(template, learned.level + 1);
      if (next == null) {
         session.send(Packets.notice("Kỹ năng đã đạt cấp độ tối đa."));
         return;
      }
      synchronized (d) { // cùng khoá với Leveling.addExp (luồng tick khu cộng điểm khi lên cấp)
         if (d.level < next.requiredLevel()) {
            session.send(Packets.notice("Cần đạt cấp " + next.requiredLevel() + " để nâng kỹ năng này."));
            return;
         }
         if (d.talentPoints < next.upgradeCost()) {
            session.send(Packets.notice("Cần " + next.upgradeCost() + " điểm tiềm năng để nâng cấp."));
            return;
         }
         long gold = next.goldCost();
         long exp = next.expCost();
         if (d.gold + d.lockedGold < gold) {
            session.send(Packets.notice("Cần " + String.format(java.util.Locale.ROOT, "%,d", gold).replace(',', '.')
                  + " vàng để nâng cấp."));
            return;
         }
         if (d.exp < exp) {
            session.send(Packets.notice("Cần " + String.format(java.util.Locale.ROOT, "%,d", exp).replace(',', '.')
                  + " kinh nghiệm để nâng cấp."));
            return;
         }
         d.talentPoints -= next.upgradeCost();
         long fromLocked = Math.min(d.lockedGold, gold); // vàng khoá trước (như lò rèn)
         d.lockedGold -= fromLocked;
         d.gold -= gold - fromLocked;
         d.exp -= exp;
      }
      session.send(com.tlnt.server.item.ItemPackets.currency(d)); // op 22 sub 3
      learned.level = (byte) next.level();
      session.send(WorldPackets.learnedSkills(d.skills));
      session.send(WorldPackets.skillBar(d.skillBar)); // client tra lại object kỹ năng mới
      session.send(Packets.notice(template.name() + " đạt cấp " + next.level() + "."));
      // kỹ năng không đổi chỉ số: chỉ gửi điểm tiềm năng còn (op 23 sub 7) — không phát cả khối chỉ số cho khu
      session.send(com.tlnt.server.item.ItemPackets.talentPoints(d.talentPoints));
   }

   /** Op 21 sub 4 (SkillSlotPopup "Ô kỹ năng số N"): gán kỹ năng vào ô {@code slot} (0..5). */
   public void assign(PlayerSession session, int skillId, int slot) {
      PlayerData d = player(session);
      if (d == null || slot < 0 || slot >= ClassSkills.BAR_SLOTS) {
         return;
      }
      if (level(d, skillId) < 1) {
         session.send(Packets.notice("Kỹ năng chưa học, hãy nâng cấp trước."));
         return;
      }
      ClassSkills.normalizeBar(d);
      for (int i = 0; i < d.skillBar.length; i++) {
         if (d.skillBar[i] == skillId) {
            d.skillBar[i] = ClassSkills.EMPTY; // đã ở ô khác -> chuyển sang ô mới
         }
      }
      d.skillBar[slot] = skillId;
      session.send(WorldPackets.skillBar(d.skillBar));
      QuestEvents.fire(session, StepKind.ASSIGN_SKILL, 0);
   }

   /**
    * Bùa tẩy điểm (item 166): mọi tiềm năng về 0, kỹ năng class về cấp 0 ("Chưa học"), "Đánh Thường" về cấp 1, hoàn
    * lại TOÀN BỘ điểm tiềm năng đã tiêu ({@code talent.point_cost} × số điểm + {@code skill_level.upgrade_cost} các cấp
    * đã nâng); kỹ năng cấp 0 rời thanh kỹ năng. Luồng session; chưa gửi gói. @return số điểm hoàn lại (0 = chưa cộng /
    * nâng gì)
    */
   public static int resetAll(PlayerData d) {
      int refund;
      synchronized (d) { // điểm tiềm năng còn được luồng tick khu cộng khi lên cấp (Leveling.addExp, cùng khoá)
         refund = spent(d);
         if (d.skills != null) {
            for (SkillData s : d.skills) {
               if (s != null) {
                  s.level = (byte) baseLevel(s.skillId);
               }
            }
         }
         d.talents = new int[d.talents == null ? 0 : d.talents.length];
         d.talentPoints += refund;
      }
      ClassSkills.normalizeBar(d); // kỹ năng cấp 0 không được nằm trên thanh
      return refund;
   }

   /** Tổng điểm tiềm năng đã tiêu vào tiềm năng + nâng kỹ năng (số {@link #resetAll} sẽ hoàn). */
   public static int spent(PlayerData d) {
      int total = 0;
      int[] talents = d.talents == null ? new int[0] : d.talents;
      for (int i = 0; i < talents.length; i++) {
         SkillPack.Talent talent = talent(i + 1);
         total += talents[i] * Math.max(1, talent != null ? talent.pointCost() : 1);
      }
      if (d.skills != null) {
         for (SkillData s : d.skills) {
            SkillPack.Skill template = s == null ? null : GameDataCache.skillTemplate(s.skillId);
            for (int level = baseLevel(s == null ? 0 : s.skillId) + 1; template != null && level <= s.level; level++) {
               SkillPack.SkillLevel paid = levelData(template, level);
               total += paid != null ? paid.upgradeCost() : 0;
            }
         }
      }
      return total;
   }

   /** Cấp lúc mới nhận: "Đánh Thường" 1, kỹ năng class 0 (chưa học). */
   private static int baseLevel(int skillId) {
      return skillId == ClassSkills.BASIC_ATTACK || skillId == 2 || skillId == 8 || skillId == 14
            || skillId == 20 || skillId == 26 ? 1 : ClassSkills.NOT_UPGRADED;
   }

   // ------------------------------------------------------------------ nội bộ

   private void refreshStats(PlayerData d) {
      ItemService.refreshLook(d);
      world.broadcastStats(d); // op 23 sub 0: chỉ số + điểm tiềm năng + điểm từng tiềm năng
   }

   private static PlayerData player(PlayerSession session) {
      PlayerData d = session.getPlayer();
      return d != null && d.inWorld ? d : null;
   }

   private static SkillPack.Talent talent(int id) {
      for (SkillPack.Talent t : GameDataCache.skill().talents()) {
         if (t.id() == id) {
            return t;
         }
      }
      return null;
   }

   /** Như client {@code TalentPointCap.findNextCap}: mốc đầu tiên mà số điểm hiện có còn nhỏ hơn. */
   static SkillPack.TalentPointCap nextCap(int points, int talentId) {
      for (SkillPack.TalentPointCap cap : GameDataCache.skill().talentPointCaps()) {
         if (cap.caps() != null && talentId - 1 < cap.caps().length && points < cap.caps()[talentId - 1]) {
            return cap;
         }
      }
      return null;
   }

   /** Dòng {@code skill_level} đúng cấp {@code level}; null nếu không có (cấp 0 = chưa học). */
   public static SkillPack.SkillLevel levelData(SkillPack.Skill template, int level) {
      List<SkillPack.SkillLevel> levels = template.levels();
      if (levels != null) {
         for (SkillPack.SkillLevel l : levels) {
            if (l.level() == level) {
               return l;
            }
         }
      }
      return null;
   }

   /** Kỹ năng đã có trong danh sách của nhân vật (kể cả cấp 0); null nếu không có. */
   public static SkillData find(PlayerData d, int skillId) {
      if (d.skills != null) {
         for (SkillData s : d.skills) {
            if (s != null && s.skillId == skillId) {
               return s;
            }
         }
      }
      return null;
   }

   private static int level(PlayerData d, int skillId) {
      SkillData s = find(d, skillId);
      return s != null ? s.level : -1;
   }
}
