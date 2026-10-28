package com.tlnt.server.world;

import com.tlnt.server.map.MapTemplate;

/**
 * Thú cưng đi theo 1 người chơi. Từ 2026-10-02 MỖI CLIENT tự mô phỏng con thú (client {@code PetBrain}: trọng lực,
 * nhảy bậc, lẽo đẽo sau chủ, đi dạo) — server KHÔNG tính đường đi và KHÔNG phát op 5 cho thú cưng nữa. Server chỉ giữ
 * con nào đi với ai ({@link Zone#syncPet}) và chỗ hiện ra lúc gửi op 3: ngay sau lưng chủ, trên mặt đất dưới chân chủ.
 * CHỈ luồng tick của khu chứa chủ đụng tới.
 */
final class Pet {

   /** Đứng cách chủ bấy nhiêu khi hiện ra (client dùng cùng số khi đi theo). */
   static final float FOLLOW_STOP_PX = 130f;
   /** Bậc thấp hơn bấy nhiêu tính là cùng mặt đất. */
   static final float STEP_PX = 24f;
   /** Mặt đất thấp hơn chủ quá bấy nhiêu (chủ đang ở trên không) thì hiện ngang chủ. */
   static final float MAX_JUMP_PX = 650f;

   final int templateId;
   final String name;

   float x;
   float y;
   boolean facingLeft;

   Pet(int templateId, String name) {
      this.templateId = templateId;
      this.name = name;
   }

   /** Đặt ngay sau lưng chủ, trên mặt đất dưới chân chủ. */
   void placeBeside(WorldPlayer owner, MapTemplate map) {
      facingLeft = owner.facingLeft;
      x = map.clampX(owner.facingLeft ? owner.x + FOLLOW_STOP_PX : owner.x - FOLLOW_STOP_PX);
      float ground = surfaceBelow(map, x, owner.y + STEP_PX);
      y = Float.isNaN(ground) || owner.y - ground > MAX_JUMP_PX ? owner.y : ground;
   }

   /**
    * Mặt đứng cao nhất tại {@code x} không cao hơn {@code maxY}: mặt trên hộp va chạm (cả nền 1 chiều) hoặc cạnh trên
    * của dốc tam giác. NaN nếu không có gì bên dưới.
    */
   static float surfaceBelow(MapTemplate map, float x, float maxY) {
      float best = Float.NaN;
      for (MapTemplate.Collider c : map.colliders()) {
         float top;
         if (c.triangle()) {
            top = triangleTop(c.points(), x);
         } else if (x >= c.x() && x <= c.x() + c.w()) {
            top = c.y() + c.h();
         } else {
            continue;
         }
         if (!Float.isNaN(top) && top <= maxY && (Float.isNaN(best) || top > best)) {
            best = top;
         }
      }
      return best;
   }

   /** Độ cao cạnh trên của tam giác tại {@code x}, NaN nếu {@code x} nằm ngoài. */
   static float triangleTop(float[] p, float x) {
      float top = Float.NaN;
      for (int i = 0; i < 3; i++) {
         float x1 = p[i * 2];
         float y1 = p[i * 2 + 1];
         float x2 = p[(i + 1) % 3 * 2];
         float y2 = p[(i + 1) % 3 * 2 + 1];
         if (x1 == x2 || x < Math.min(x1, x2) || x > Math.max(x1, x2)) {
            continue;
         }
         float yy = y1 + (x - x1) * (y2 - y1) / (x2 - x1);
         if (Float.isNaN(top) || yy > top) {
            top = yy;
         }
      }
      return top;
   }
}
