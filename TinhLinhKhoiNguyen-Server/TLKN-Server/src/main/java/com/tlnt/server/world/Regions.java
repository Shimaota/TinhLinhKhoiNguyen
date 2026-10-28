package com.tlnt.server.world;

import java.util.Set;

/**
 * Luật theo vùng đất (người dùng chốt 2026-10-11): quái map 9-20 CHỦ ĐỘNG tấn công người đứng gần (đi xa là thôi —
 * {@code ZoneCombat.mobAggro}); vào Avenna (9-11) / Frearh (15-17) từ map ngoài vùng thì báo 1 dòng; quái Frearh mỗi đòn
 * trúng có {@link #FREEZE_PER_MILLION} / 1.000.000 đóng băng người bị đánh {@link #FREEZE_MS}.
 */
public final class Regions {

   /** Map quái chủ động tấn công: Đầm lầy Avenna -> Bãi đá ngầm. */
   static final Set<Integer> AGGRESSIVE_MAPS = Set.of(9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20);
   static final Set<Integer> AVENNA = Set.of(9, 10, 11);
   static final Set<Integer> FREARH = Set.of(15, 16, 17);

   static final String AVENNA_NOTICE = "Bạn đang ở Avenna, ma thuật nơi đây sẽ rút cạn sinh lực của bạn. Hãy cẩn thận!";
   static final String FREARH_NOTICE = "Bạn đang ở Frearh, cái lạnh thấu xương nơi đây sẽ khiến bạn bị đóng băng. Hãy"
         + " cẩn thận!";

   /** Người đứng cách điểm sinh quái (cộng phạm vi lang thang) trong tầm này thì quái lao vào đánh (pixel). */
   static final float AGGRO_X_PX = 350f;
   static final float AGGRO_Y_PX = 250f;
   /** Quét người đứng gần mỗi bấy lâu / khu (rẻ: ≤ 30 người × vài chục quái). */
   static final long AGGRO_SCAN_MS = 500;

   /** Đóng băng: 0,1% mỗi đòn quái Frearh trúng (tính theo phần triệu). */
   static final int FREEZE_PER_MILLION = 1_000;
   static final int FREEZE_MS = 3_000;

   private Regions() {
   }

   static boolean aggressive(int mapId) {
      return AGGRESSIVE_MAPS.contains(mapId);
   }

   static boolean freezes(int mapId) {
      return FREARH.contains(mapId);
   }

   /** Dòng báo khi vào map {@code toMap} từ {@code fromMap} (đăng nhập: fromMap 0); null = không báo (cùng vùng / vùng khác). */
   static String enterNotice(int fromMap, int toMap) {
      if (AVENNA.contains(toMap) && !AVENNA.contains(fromMap)) {
         return AVENNA_NOTICE;
      }
      if (FREARH.contains(toMap) && !FREARH.contains(fromMap)) {
         return FREARH_NOTICE;
      }
      return null;
   }
}
