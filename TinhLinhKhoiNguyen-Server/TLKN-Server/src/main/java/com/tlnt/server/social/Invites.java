package com.tlnt.server.social;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lời mời đang chờ trả lời (giao dịch / thách đấu): mỗi người nhận giữ tối đa 1 lời mời mỗi loại, lời mời mới đè lời
 * cũ -> số phần tử không bao giờ quá số người online. Người mời ghi (luồng của họ), người nhận lấy ra (luồng của họ)
 * -> map đồng thời.
 */
public final class Invites {

   private record Invite(int fromId, long expiresAt) {
   }

   private final Map<Integer, Invite> byTarget = new ConcurrentHashMap<>();
   private final long ttlMs;

   public Invites(long ttlMs) {
      this.ttlMs = ttlMs;
   }

   public void put(int targetId, int fromId, long now) {
      byTarget.put(targetId, new Invite(fromId, now + ttlMs));
   }

   /** Lấy ra (dùng 1 lần) id người mời; 0 = không có hoặc đã hết hạn. */
   public int take(int targetId, long now) {
      Invite invite = byTarget.remove(targetId);
      return invite == null || now > invite.expiresAt() ? 0 : invite.fromId();
   }

   /** Người chơi thoát: bỏ lời mời gửi tới họ. */
   public void remove(int targetId) {
      byTarget.remove(targetId);
   }
}
