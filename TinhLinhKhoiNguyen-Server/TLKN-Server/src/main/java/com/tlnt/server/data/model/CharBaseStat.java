package com.tlnt.server.data.model;

/**
 * Chỉ số gốc của một nhân vật trong bảng {@code char_base_stat}.
 *
 * @param hpRegen / mpRegen HP / MP hồi mỗi 5 giây ({@code item/ItemBuffs.REGEN_INTERVAL_SECONDS})
 * @param damageAmpPercent khuếch đại sát thương (%)
 */
public record CharBaseStat(int templateId, long hp, long mp, long attack, long attackVsMob, long critDamagePercent,
      long armor, long moveSpeed, long hpRegen, long mpRegen, long damageAmpPercent, int stamina) {
}
