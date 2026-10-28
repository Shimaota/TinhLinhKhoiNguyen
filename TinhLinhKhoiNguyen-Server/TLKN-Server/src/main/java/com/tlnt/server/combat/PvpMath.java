package com.tlnt.server.combat;

import com.tlnt.server.player.PlayerStats;

/**
 * Sát thương người đánh người / boss người chơi giả. Người đánh tính sát thương thô trên luồng của mình
 * ({@link CombatService}: CHỈ Tấn công, không cộng "Tấn công lên quái"; đánh thường đã nhân Khuếch đại tấn công); luồng
 * tick khu trừ theo Giáp của người bị đánh lúc trúng đòn:
 *
 * <pre>
 * sát thương = thô × (1 − min(75%, (Giáp − Xuyên giáp) / 10000)), tối thiểu 1     ({@link StatRules})
 * </pre>
 * Kháng sát thương / Suy yếu / Hấp thụ / Phản sát thương áp cho MỌI nguồn sát thương lên người ở {@code ZoneCombat}.
 */
public final class PvpMath {

   private PvpMath() {
   }

   public static long reduce(long raw, PlayerStats attacker, PlayerStats target) {
      return Math.max(1, StatRules.afterArmor(raw, target.armor, attacker.armorPen));
   }
}
