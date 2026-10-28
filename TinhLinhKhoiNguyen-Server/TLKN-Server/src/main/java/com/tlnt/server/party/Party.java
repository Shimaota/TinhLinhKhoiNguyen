package com.tlnt.server.party;

import java.util.ArrayList;
import java.util.List;

/**
 * 1 tổ đội (tối đa {@link PartyService#MAX_MEMBERS} người). Chỉ trong RAM, thoát game = rời tổ đội. Mọi field đổi
 * được chỉ đọc/ghi dưới khoá của {@link PartyService}; luồng khác chỉ so sánh object (cùng tổ đội) qua
 * {@code PlayerData.party}.
 */
public final class Party {

   /** 1 thành viên: id + lúc vào (thông tin hiển thị lấy trực tiếp từ nhân vật đang online). */
   record Member(int playerId, long joinedAt) {
   }

   /** 1 yêu cầu xin vào (tab "Chờ duyệt"), chụp tên / cấp / ảnh lúc xin. */
   record Request(int playerId, String name, int level, int templateId) {
   }

   private final int id;
   final String name;
   final long createdAt;
   /** Thành viên, trưởng nhóm luôn đứng đầu. */
   final List<Member> members = new ArrayList<>();
   final List<Request> requests = new ArrayList<>();
   boolean autoApprove;
   /** Loại vũ khí tổ đội đang có (bit theo {@code PartyService.FIVE_CLASSES}) — để hiện dòng sáng / mờ. */
   int bonusMask;

   Party(int id, String name, long createdAt) {
      this.id = id;
      this.name = name;
      this.createdAt = createdAt;
   }

   public int id() {
      return id;
   }

   int leaderId() {
      return members.isEmpty() ? 0 : members.get(0).playerId();
   }

   boolean isLeader(int playerId) {
      return leaderId() == playerId;
   }

   Member member(int playerId) {
      for (Member m : members) {
         if (m.playerId() == playerId) {
            return m;
         }
      }
      return null;
   }

   boolean full() {
      return members.size() >= PartyService.MAX_MEMBERS;
   }

   boolean removeRequest(int playerId) {
      return requests.removeIf(r -> r.playerId() == playerId);
   }
}
