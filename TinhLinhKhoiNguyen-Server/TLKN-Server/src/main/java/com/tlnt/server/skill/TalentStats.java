package com.tlnt.server.skill;

import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerStats;

/**
 * Chỉ số mỗi điểm tiềm năng (bảng {@code talent}, số lấy từ dòng cuối mô tả mỗi tiềm năng). {@code d.talents[id-1]}
 * = số điểm đã cộng (client: {@code talentPointsArray[id - 1]}).
 * <pre>
 * id 1 Sinh lực   +10 HP        id 4 Tập trung +1 Chính xác
 * id 2 Trí lực    +10 MP        id 5 Bạo kích  +1 Chí mạng
 * id 3 Nhanh nhẹn +1 Né tránh   id 6 Sức mạnh  +1 Tấn công
 * </pre>
 * Cộng vào chỉ số GỐC (trước dòng % của đồ), gọi trong {@link PlayerStats#of}.
 */
public final class TalentStats {

   public static final int VITALITY = 1;
   public static final int INTELLECT = 2;
   public static final int AGILITY = 3;
   public static final int FOCUS = 4;
   public static final int CRITICAL = 5;
   public static final int STRENGTH = 6;

   private TalentStats() {
   }

   public static void apply(PlayerStats s, PlayerData d) {
      s.maxHp += 10L * points(d, VITALITY);
      s.maxMp += 10L * points(d, INTELLECT);
      s.evasion += points(d, AGILITY);
      s.accuracy += points(d, FOCUS);
      s.critChance += points(d, CRITICAL);
      s.attackPower += points(d, STRENGTH);
   }

   /** Tên chỉ số được cộng, dùng cho thông báo. */
   public static String bonusText(int talentId, int points) {
      return switch (talentId) {
         case VITALITY -> "+" + 10L * points + " HP";
         case INTELLECT -> "+" + 10L * points + " MP";
         case AGILITY -> "+" + points + " Né tránh";
         case FOCUS -> "+" + points + " Chính xác";
         case CRITICAL -> "+" + points + " Chí mạng";
         case STRENGTH -> "+" + points + " Tấn công";
         default -> "";
      };
   }

   static int points(PlayerData d, int talentId) {
      return d.talents != null && talentId >= 1 && talentId <= d.talents.length ? d.talents[talentId - 1] : 0;
   }
}
