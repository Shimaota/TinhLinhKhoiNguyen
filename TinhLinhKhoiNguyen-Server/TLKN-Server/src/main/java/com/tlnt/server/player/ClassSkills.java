package com.tlnt.server.player;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.SkillPack;
import com.tlnt.server.item.WeaponClass;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Kỹ năng theo class. Cột {@code skill_template.class_id} = class sở hữu kỹ năng:
 * <pre>
 * class 1 Chưa vũ khí : 1 "Đánh Thường" (mọi nhân vật, luôn cấp 1)
 * class 2 Rìu         : 1 + 2..7
 * class 3 Cung tên    : 1 + 8..13
 * class 4 Ná thun     : 1 + 14..19
 * class 5 Giáo        : 1 + 20..25
 * class 6 Roi da      : 1 + 26, 42, 28, 29, 30, 31 (thứ tự bảng kỹ năng; 42 thay hẳn 27 — {@link #classSkills})
 * </pre>
 * Vào class (trang bị vũ khí đầu tiên) -> thêm mọi kỹ năng của class đó ở cấp 0 = "Chưa học" phía client (chờ nâng
 * cấp); kỹ năng đánh thường đầu tiên của class và kỹ năng 1 giữ cấp 1. Kỹ năng cấp 0 KHÔNG được đặt lên thanh kỹ
 * năng (client đọc levelData = null).
 */
public final class ClassSkills {

   /** "1. Đánh Thường". */
   public static final int BASIC_ATTACK = 1;
   /** Đòn đánh thường đầu tiên theo class (Rìu 2, Cung 8, Ná 14, Giáo 20, Roi 26). */
   private static final int[] CLASS_BASIC_ATTACK = {0, 0, 2, 8, 14, 20, 26};
   /** Số ô thanh kỹ năng (SkillSlotPopup "Ô kỹ năng số 1..6"); {@link PlayerData#skillBar} theo đúng vị trí ô. */
   public static final int BAR_SLOTS = 6;
   /** Ô trống trên thanh kỹ năng (client tra id -1 -> không có kỹ năng). */
   public static final int EMPTY = -1;
   /** Cấp của kỹ năng class mới nhận: 0 = chưa nâng cấp. */
   public static final byte NOT_UPGRADED = 0;

   private ClassSkills() {
   }

   /**
    * Cho danh sách kỹ năng khớp class hiện tại: có kỹ năng 1 (cấp ≥ 1), có đủ kỹ năng của class (thiếu thì thêm cấp 0,
    * đã có thì giữ cấp), bỏ kỹ năng của class khác (cả trên thanh kỹ năng). Gọi trên luồng session / lúc vào game.
    *
    * @return danh sách có đổi không (cần gửi lại opcode 21 sub 1)
    */
   public static boolean apply(PlayerData p) {
      if (p.skills == null) {
         p.skills = new ArrayList<>();
      }
      int[] own = classSkills(p.classId);
      boolean changed = false;
      boolean hasBasic = false;
      for (Iterator<SkillData> it = p.skills.iterator(); it.hasNext(); ) {
         SkillData s = it.next();
         if (s == null) {
            it.remove();
            changed = true;
            continue;
         }
         if (s.skillId == BASIC_ATTACK) {
            hasBasic = true;
            if (s.level < 1) {
               s.level = 1;
               changed = true;
            }
            continue;
         }
         if (indexOf(own, s.skillId) < 0) {
            it.remove(); // kỹ năng class khác / không còn dùng (vd 27 cũ của Roi)
            changed = true;
            continue;
         }
         if (s.skillId == classBasicAttack(p.classId) && s.level < 1) {
            s.level = 1;
            changed = true;
         }
      }
      if (!hasBasic) {
         p.skills.add(0, new SkillData(BASIC_ATTACK, (byte) 1));
         changed = true;
      }
      for (int id : own) {
         if (!learned(p, id) && GameDataCache.skillTemplate(id) != null) {
            p.skills.add(new SkillData(id, id == classBasicAttack(p.classId) ? (byte) 1 : NOT_UPGRADED));
            changed = true;
         }
      }
      // đúng thứ tự bảng kỹ năng: 1 rồi danh sách của class (Roi: 1, 26, 42, 28, 29, 30, 31)
      java.util.List<SkillData> sorted = new ArrayList<>(p.skills);
      sorted.sort(java.util.Comparator.comparingInt(sd -> sd.skillId == BASIC_ATTACK ? -1 : indexOf(own, sd.skillId)));
      if (!sorted.equals(p.skills)) {
         p.skills = sorted;
         changed = true;
      }
      normalizeBar(p);
      return changed;
   }

   /**
    * Kỹ năng của class theo ĐÚNG thứ tự hiện ở bảng kỹ năng (sau "1. Đánh Thường"). Roi: 26, 42, 28, 29, 30, 31 —
    * 42 Loạn Ảnh Phách thay hẳn 27 Song Ảnh Phách (27 không còn cấp / dùng).
    */
   static int[] classSkills(int classId) {
      return switch (classId) {
         case WeaponClass.AXE -> new int[] {2, 3, 4, 5, 6, 7};
         case WeaponClass.BOW -> new int[] {8, 9, 10, 11, 12, 13};
         case WeaponClass.SLINGSHOT -> new int[] {14, 15, 16, 17, 18, 19};
         case WeaponClass.SPEAR -> new int[] {20, 21, 22, 23, 24, 25};
         case WeaponClass.WHIP -> new int[] {26, 42, 28, 29, 30, 31};
         default -> new int[0];
      };
   }

   private static int indexOf(int[] list, int id) {
      for (int i = 0; i < list.length; i++) {
         if (list[i] == id) {
            return i;
         }
      }
      return -1;
   }

   private static int classBasicAttack(int classId) {
      return classId >= 0 && classId < CLASS_BASIC_ATTACK.length ? CLASS_BASIC_ATTACK[classId] : -1;
   }

   /**
    * Thanh kỹ năng đúng {@link #BAR_SLOTS} ô theo vị trí; ô chứa kỹ năng không còn học / cấp 0 -> trống. Trống hết thì
    * đặt "Đánh Thường" vào ô 1.
    */
   public static void normalizeBar(PlayerData p) {
      int[] bar = new int[BAR_SLOTS];
      java.util.Arrays.fill(bar, EMPTY);
      boolean any = false;
      if (p.skillBar != null) {
         for (int i = 0; i < Math.min(BAR_SLOTS, p.skillBar.length); i++) {
            int id = p.skillBar[i];
            if (id != EMPTY && level(p, id) > 0) {
               bar[i] = id;
               any = true;
            }
         }
      }
      if (!any) {
         bar[0] = BASIC_ATTACK;
      }
      p.skillBar = bar;
   }

   public static boolean onBar(PlayerData p, int skillId) {
      if (p.skillBar != null) {
         for (int id : p.skillBar) {
            if (id == skillId) {
               return true;
            }
         }
      }
      return false;
   }

   /** Class sở hữu kỹ năng ({@code skill_template.class_id}), -1 nếu không có mẫu. */
   public static int ownerClass(int skillId) {
      SkillPack.Skill t = GameDataCache.skillTemplate(skillId);
      return t != null ? t.classId() : -1;
   }

   private static boolean learned(PlayerData p, int skillId) {
      return level(p, skillId) >= 0;
   }

   /** Cấp đã học, -1 nếu chưa có. */
   private static int level(PlayerData p, int skillId) {
      for (SkillData s : p.skills) {
         if (s != null && s.skillId == skillId) {
            return s.level;
         }
      }
      return -1;
   }
}
