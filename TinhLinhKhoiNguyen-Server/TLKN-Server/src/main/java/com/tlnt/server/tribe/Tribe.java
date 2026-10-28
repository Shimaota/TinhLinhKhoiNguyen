package com.tlnt.server.tribe;

import com.tlnt.server.player.ItemData;

import java.util.ArrayList;
import java.util.List;

/**
 * 1 bộ lạc trong RAM (bản chuẩn). CHỈ sửa dưới khoá của {@link TribeService}. Mỗi field = 1 cột bảng {@code tribe}
 * (danh sách = cột JSON riêng, {@link TribeRepository}); {@link #dirty} không lưu.
 */
final class Tribe {

   int id;
   String name;
   String slogan = "";
   int level;
   /** Điểm cống hiến trong cấp hiện tại (đủ {@link TribeRules#need} thì lên cấp, trừ đi). */
   long exp;
   /** Ngân sách: vàng / lam thạch. */
   long gold;
   long diamond;
   int emblemId = TribeRules.DEFAULT_EMBLEM;
   /** Hết hạn biểu tượng (epoch ms), -1 = vĩnh viễn (Cờ lệnh mặc định). */
   long emblemExpireAt = -1;
   /** Dòng chỉ số biểu tượng đang hiệu lực: cặp (stat_type, giá trị) — chép từ món biểu tượng lúc "Sử dụng". */
   int[][] emblemLines = new int[0][];
   /** {@code tribe_skill.id} theo thứ tự ô. */
   List<Integer> skills = new ArrayList<>();
   /** Tù trưởng luôn đứng đầu. */
   List<Member> members = new ArrayList<>();
   /** Đơn xin vào, cũ trước. */
   List<Applicant> applicants = new ArrayList<>();
   /** Tab "Thông báo", cũ trước. */
   List<String> notices = new ArrayList<>();
   long createdAt;
   /** Người giữ danh hiệu (id, 0 = chưa ai): Công thần / Tài chủ / Kim chủ — đổi khi có người VƯỢT hẳn. */
   int topPoints;
   int topDiamond;
   int topGold;

   /** Rương bộ lạc (ô = vị trí trong danh sách). */
   transient List<ItemData> items = new ArrayList<>();
   /** Có thay đổi chưa ghi DB (cộng điểm cống hiến — ghi gom theo chu kỳ). */
   transient boolean dirty;

   /** 1 thành viên. Tên / cấp / mẫu nhân vật chụp lúc online gần nhất (người offline vẫn hiện trong danh sách). */
   static final class Member {
      int id;
      String name;
      int level;
      int tpl;
      int role = TribeRules.ROLE_MEMBER;
      /** Điểm cống hiến cá nhân (Công thần) + đã góp vàng / lam thạch (Kim chủ / Tài chủ) — tính từ lúc vào. */
      long points;
      long gold;
      long diamond;
      long joinedAt;

      Member() {
      }

      Member(int id, String name, int level, int tpl, int role, long joinedAt) {
         this.id = id;
         this.name = name;
         this.level = level;
         this.tpl = tpl;
         this.role = role;
         this.joinedAt = joinedAt;
      }
   }

   /** 1 đơn xin vào. */
   record Applicant(int id, String name, int level, int tpl, long at) {
   }

   Member member(int playerId) {
      for (Member m : members) {
         if (m.id == playerId) {
            return m;
         }
      }
      return null;
   }

   Member leader() {
      for (Member m : members) {
         if (m.role == TribeRules.ROLE_LEADER) {
            return m;
         }
      }
      return members.isEmpty() ? null : members.get(0);
   }

   Applicant applicant(int playerId) {
      for (Applicant a : applicants) {
         if (a.id() == playerId) {
            return a;
         }
      }
      return null;
   }

   boolean removeApplicant(int playerId) {
      return applicants.removeIf(a -> a.id() == playerId);
   }

   int maxMembers() {
      return TribeRules.maxMembers(level);
   }

   boolean full() {
      return members.size() >= maxMembers();
   }

   int count(int role) {
      int n = 0;
      for (Member m : members) {
         if (m.role == role) {
            n++;
         }
      }
      return n;
   }

   boolean hasSkill(int skillId) {
      return skills.contains(skillId);
   }

   int skillMask() {
      int mask = 0;
      for (int id : skills) {
         if (id > 0 && id < 31) {
            mask |= 1 << id;
         }
      }
      return mask;
   }
}
