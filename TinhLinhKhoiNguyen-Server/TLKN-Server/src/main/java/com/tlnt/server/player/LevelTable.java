package com.tlnt.server.player;

import com.tlnt.server.db.Database;
import com.tlnt.server.util.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TreeMap;

/**
 * Bảng EXP lên cấp ({@code tlnt_data.level_exp}: {@code level}, {@code exp} = EXP cần ở cấp đó để lên cấp kế — cũng là
 * "EXP tối đa" client hiện ở thanh EXP). Cấp TỐI ĐA = cấp lớn nhất có trong bảng: tới đó thì không lên nữa, EXP dừng ở
 * {@code exp} của cấp đó. Muốn mở thêm cấp = thêm dòng. Nạp lúc khởi động + mỗi phút (pool blocking); bảng trống / lỗi
 * -> công thức cũ 100 × cấp² tới cấp {@link #FALLBACK_MAX_LEVEL}. Đọc không khoá (mảng bất biến, volatile).
 */
public final class LevelTable {

   /** Không có bảng: cấp tối đa cũ (cột player.level smallint). */
   static final int FALLBACK_MAX_LEVEL = 1000;

   /** exps[L] = EXP cần ở cấp L (index 0 bỏ). null = chưa có bảng. */
   private static volatile long[] exps;

   private LevelTable() {
   }

   /** EXP cần ở cấp {@code level} để lên cấp kế (cấp ≥ tối đa: giá trị của cấp tối đa). */
   public static long expAt(int level) {
      long[] t = exps;
      int lv = Math.max(1, level);
      if (t == null) {
         return 100L * lv * lv;
      }
      return t[Math.min(lv, t.length - 1)];
   }

   public static int maxLevel() {
      long[] t = exps;
      return t == null ? FALLBACK_MAX_LEVEL : t.length - 1;
   }

   /** Dùng bảng {@code levelToExp} (cấp liên tục từ 1; thiếu cấp giữa chừng -> chép cấp trước). Cho test / nạp SQL. */
   public static void set(java.util.SortedMap<Integer, Long> levelToExp) {
      if (levelToExp == null || levelToExp.isEmpty() || levelToExp.firstKey() != 1) {
         exps = null;
         return;
      }
      int max = levelToExp.lastKey();
      long[] t = new long[max + 1];
      for (int lv = 1; lv <= max; lv++) {
         Long v = levelToExp.get(lv);
         t[lv] = v != null ? v : t[lv - 1];
      }
      exps = t;
   }

   /** Nạp lại từ SQL (pool blocking / lúc khởi động). Lỗi -> giữ bảng cũ. */
   public static void reload(Database dataDb) {
      TreeMap<Integer, Long> map = new TreeMap<>();
      try (Connection conn = dataDb.getConnection();
            PreparedStatement ps = conn.prepareStatement("SELECT level, exp FROM level_exp ORDER BY level");
            ResultSet rs = ps.executeQuery()) {
         while (rs.next()) {
            map.put(rs.getInt(1), rs.getLong(2));
         }
      } catch (SQLException e) {
         Log.warn("Khong doc duoc level_exp (giu ban cu): %s", e.getMessage());
         return;
      }
      int before = maxLevel();
      set(map);
      if (before != maxLevel()) {
         Log.info("Bang EXP: cap toi da %d", maxLevel());
      }
   }
}
