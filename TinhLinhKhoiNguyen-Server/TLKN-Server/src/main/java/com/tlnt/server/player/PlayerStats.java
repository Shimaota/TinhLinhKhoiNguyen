package com.tlnt.server.player;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.CharBaseStat;
import com.tlnt.server.item.ItemStats;
import com.tlnt.server.util.Log;

/**
 * Bộ chỉ số chiến đấu. Chỉ số cuối của người chơi KHÔNG lưu DB mà tính lại từ nguồn ({@link #of}):
 * <pre>
 *   chỉ số gốc theo nhân vật (char_base_stat) + chỉ số đồ đang dùng (ItemStats)
 * </pre>
 * Tính lại mỗi khi nguồn đổi (vào game, lên cấp, mặc/tháo đồ...) rồi gửi client (opcode 2/3/23).
 * Thứ tự field khớp đoạn cuối {@code ServerPacketHandler.readPlayerStats}. Đơn vị = giá trị client hiển thị:
 * {@link #critDamagePercent} 80 = 80%, {@link #moveSpeed} 450 = 450 px/giây.
 */
public final class PlayerStats {

   /** Dùng khi nhân vật chưa có dòng trong char_base_stat — vẫn đi lại được, tránh kẹt người chơi. */
   private static final CharBaseStat MISSING = new CharBaseStat(0, 1, 1, 0, 0, 0, 0, 450, 0, 0, 0, 1000);

   public long maxHp;
   public long maxMp;
   public long attackPower;
   public long attackVsMob;
   public long critChance;
   public long critDamagePercent;
   public long armor;
   public long armorPen;
   public long evasion;
   public long accuracy;
   public long moveSpeed;
   public long hpRegen;
   public long mpRegen;
   public long hpLeechPercent;
   public long mpLeechPercent;
   public long hpLeechVsMobPercent;
   public long mpLeechVsMobPercent;
   public long reflectDamagePercent;
   public long stunDamage;
   public long weakenDamage;
   public long iceDamage;
   public long burnDamage;
   public long thunderDamage;
   public long critResistPercent;
   public long damageAmpPercent;
   public long damageAbsorbPercent;
   public long effectResistPercent;
   /** Dòng 71 "Giảm X% Thời gian đóng băng" (ngoại trang 191...) — chỉ server dùng, không gửi client. */
   public long freezeReducePercent;
   /** Dòng 70 "Chống lạnh": không bị cái lạnh Frearh đóng băng — chỉ server dùng, không gửi client. */
   public boolean coldResist;
   public long damageResist;
   public long goldBonusPercent;
   public long expBonusPercent;
   /** +% sát thương lên quái (Thuốc Sức mạnh) — chỉ server dùng, không gửi client. */
   public long mobDamagePercent;
   /** -% sát thương nhận vào từ quái (Thuốc Cứng cáp) — chỉ server dùng. */
   public long mobDamageTakenReducePercent;
   /** Thể lực tối đa (opcode 23 sub 8). */
   public int maxStamina;

   /**
    * Chỉ số cuối của nhân vật: gốc theo nhân vật + dòng chỉ số của đồ đang dùng ({@link ItemStats}) + buff kỹ năng
    * còn hạn. Đọc túi đồ nên chỉ gọi trên luồng session (hoặc lúc vào game, trước khi vào khu).
    */
   public static PlayerStats of(PlayerData player) {
      player.titleId = com.tlnt.server.forge.SetTitles.compute(player); // bộ Nhật / Nguyệt ấn (gửi trong khối chỉ số)
      int template = Gender.charTemplateId(player.gender);
      PlayerStats s = base(template);
      com.tlnt.server.skill.TalentStats.apply(s, player); // điểm tiềm năng: cộng vào chỉ số gốc
      ItemStats.apply(s, player, base(template)); // đồ / bộ sưu tập / buff + % riêng của nhân vật (Gender.bonusPercent)
      if (player.moveSpeedBuffPercent > 0 && System.currentTimeMillis() < player.moveSpeedBuffUntil) {
         s.moveSpeed += s.moveSpeed * player.moveSpeedBuffPercent / 100; // buff kỹ năng (16 Phá Tốc Nỗ)
      }
      if (System.currentTimeMillis() < player.weakenedUntil) { // Suy yếu: chậm 50%
         s.moveSpeed -= s.moveSpeed * com.tlnt.server.combat.StatRules.WEAKEN_SLOW_PERCENT / 100;
      }
      if (System.currentTimeMillis() < player.bossWeakenAttackUntil && player.bossWeakenAttackPercent > 0) {
         long reduce = Math.min(100, player.bossWeakenAttackPercent);
         s.attackPower -= s.attackPower * reduce / 100;
         s.attackVsMob -= s.attackVsMob * reduce / 100;
      }
      com.tlnt.server.party.PartyService.applyBonus(s, player.partyBonusMask); // Ngũ Linh Liên Vũ (tổ đội)
      com.tlnt.server.altar.AltarRules.applySeal(s, player); // Ấn linh (chỉ trong Tế đàn Thiên tích)
      com.tlnt.server.thunder.ThunderRules.applySeal(s, player); // Ấn linh Lôi vực Sấm sét (chỉ trong map đó)
      com.tlnt.server.social.PkRules.applyPenalty(s, player.pkPoints); // sát khí ≥ 20: -50%, ≥ 50: -75% (sau cùng)
      return s;
   }

   /** Chỉ số gốc của nhân vật; cấp độ hiện chưa làm thay đổi chỉ số. */
   public static PlayerStats base(int templateId) {
      CharBaseStat b = GameDataCache.charBaseStat(templateId);
      if (b == null) {
         Log.warn("Nhan vat template %d chua co dong trong char_base_stat, dung chi so tam", templateId);
         b = MISSING;
      }
      PlayerStats s = new PlayerStats();
      s.maxHp = b.hp();
      s.maxMp = b.mp();
      s.attackPower = b.attack();
      s.attackVsMob = b.attackVsMob();
      s.critDamagePercent = b.critDamagePercent();
      s.armor = b.armor();
      s.moveSpeed = b.moveSpeed();
      s.hpRegen = b.hpRegen();
      s.mpRegen = b.mpRegen();
      s.damageAmpPercent = b.damageAmpPercent();
      s.maxStamina = b.stamina();
      return s;
   }

   /** EXP cần để lên cấp kế tiếp — bảng {@code tlnt_data.level_exp} ({@link LevelTable}). */
   public static long expToNextLevel(int level) {
      return LevelTable.expAt(level);
   }
}
