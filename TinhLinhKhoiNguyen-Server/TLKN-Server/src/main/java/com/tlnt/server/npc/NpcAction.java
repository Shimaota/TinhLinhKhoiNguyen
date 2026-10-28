package com.tlnt.server.npc;

/**
 * Việc xảy ra khi bấm 1 loại NPC ({@code npc_template.id}). Đăng ký trong {@link NpcService#registerDefaults} —
 * thêm NPC mới = 1 class / lambda + 1 dòng đăng ký.
 */
public interface NpcAction {

   /** Người chơi bấm vào NPC (đã kiểm tra đứng gần). */
   void onTalk(NpcContext ctx);

   /**
    * Người chơi bấm nút {@code option} của hộp thoại trang {@code page} ({@link NpcContext#say}) — đã kiểm tra đúng hộp
    * thoại đang mở + đứng gần. Mặc định: không làm gì (vd nút "Đóng").
    */
   default void onChoice(NpcContext ctx, int page, int option) {
   }
}
