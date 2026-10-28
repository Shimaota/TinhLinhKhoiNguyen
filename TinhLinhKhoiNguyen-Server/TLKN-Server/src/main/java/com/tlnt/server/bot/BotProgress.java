package com.tlnt.server.bot;

import com.tlnt.server.quest.MainQuests;

/**
 * Tiến độ nhiệm vụ chính tuyến "hợp lý" của 1 bot theo cấp — bot không làm nhiệm vụ thật nhưng phải trông như đã làm tới
 * đó: chỉ đứng / up ở map người cùng cấp đã mở được ({@link MainQuests#requiredQuest}), chỉ có đồ nâng sao khi đã qua
 * nhiệm vụ lò rèn.
 *
 * <p>Mốc lấy từ chính chuỗi nhiệm vụ: các bước "Luyện tập đạt cấp N" — NV 6 cần cấp 5, NV 12 cấp 10, NV 22 cấp 35, NV 26
 * cấp 40, NV 30 cấp 45; giữa 2 mốc nội suy tuyến tính (người chơi làm đều tay). Nâng sao: NV 6 "Ấn Ký Nhật Nguyệt" mới
 * biết nâng; NV 17 đòi 3 sao, NV 21 đòi 6 sao -> trần sao theo đó.
 */
final class BotProgress {

   /** {cấp, nhiệm vụ đang làm} — nội suy giữa các mốc. */
   private static final int[][] MARKS = {{1, 1}, {3, 4}, {5, 6}, {10, 12}, {35, 22}, {40, 26}, {45, 30}, {50, 33}};
   /** Nhiệm vụ đầu tiên dạy nâng cấp trang bị (đá Mặt Trời / Mặt Trăng ở Thợ rèn Morgi). */
   static final int UPGRADE_QUEST = 6;

   private BotProgress() {
   }

   /** Nhiệm vụ chính tuyến người chơi cấp {@code level} thường đang làm. */
   static int questFor(int level) {
      if (level <= MARKS[0][0]) {
         return MARKS[0][1];
      }
      for (int i = 1; i < MARKS.length; i++) {
         int[] a = MARKS[i - 1];
         int[] b = MARKS[i];
         if (level <= b[0]) {
            return a[1] + (level - a[0]) * (b[1] - a[1]) / (b[0] - a[0]);
         }
      }
      return MARKS[MARKS.length - 1][1];
   }

   /** Người cấp {@code level} đã tới được map {@code mapId} chưa (theo nhiệm vụ). */
   static boolean canEnter(int level, int mapId) {
      return questFor(level) >= MainQuests.requiredQuest(mapId);
   }

   /** Số sao nâng cấp cao nhất trên 1 món ở cấp {@code level} (0 = chưa tới nhiệm vụ nâng cấp). */
   static int maxStars(int level) {
      int q = questFor(level);
      return q < UPGRADE_QUEST ? 0 : q < 17 ? 2 : q < 21 ? 4 : q < 25 ? 6 : 8;
   }
}
