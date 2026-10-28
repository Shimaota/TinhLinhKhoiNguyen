package com.tlnt.server.tribe;

import java.util.function.Function;

/**
 * Khoá {@code tribe.*} trong {@code server.properties} (mặc định ở đây; số nào spec ghi "tính sau" / "cần chốt" đều nằm
 * ở đây để sửa không cần build lại).
 *
 * @param joinAfterQuest phải làm XONG nhiệm vụ chính tuyến này mới được xin vào / nhận lời mời (tạo thì không cần)
 * @param rejoinHours rời / bị đuổi -> bấy nhiêu giờ sau mới vào được bộ lạc khác (giải tán thì không tính)
 * @param applyHours đơn xin vào tự huỷ sau bấy nhiêu giờ
 * @param maxApplicants số đơn tối đa 1 bộ lạc giữ (đầy thì bỏ đơn cũ nhất)
 * @param maxApplicationsPerPlayer 1 người xin vào tối đa bấy nhiêu bộ lạc cùng lúc
 * @param maxDeputies số Phó tù trưởng tối đa
 * @param createItem {@code item_template.id} Cờ lệnh Bộ lạc (trừ 1 khi tạo)
 * @param nameMax tên bộ lạc tối đa bấy nhiêu ký tự (có dấu)
 * @param sloganMax khẩu hiệu tối đa bấy nhiêu ký tự
 * @param openSkillGold vàng quỹ / lần "Mở kỹ năng"
 * @param changeSkillGold vàng quỹ / lần "Đổi kỹ năng"
 * @param emblemDays biểu tượng dùng từ kho có hiệu lực bấy nhiêu ngày
 * @param shopId {@code tlnt_data.shop.id} của Cửa hàng Bộ lạc (trả bằng lam thạch quỹ, hàng vào kho)
 * @param warehouseSlots số ô Rương bộ lạc
 * @param nearbyPx "đứng cạnh" (kỹ năng 2, 4) = cùng khu và cách nhau không quá bấy nhiêu px
 * @param enduranceMaxPercent kỹ năng 10 giảm sát thương nhận vào tối đa bấy nhiêu %
 * @param listMax tab "Bộ lạc" (tìm kiếm) hiện tối đa bấy nhiêu bộ lạc
 * @param pointsLinhMoc điểm cống hiến mỗi lần vào lượt MỚI Rừng Linh Mộc
 */
public record TribeConfig(int joinAfterQuest, int rejoinHours, int applyHours, int maxApplicants,
      int maxApplicationsPerPlayer, int maxDeputies, int createItem, int nameMax, int sloganMax, long openSkillGold,
      long changeSkillGold, int emblemDays, int shopId, int warehouseSlots, int nearbyPx, int enduranceMaxPercent,
      int listMax, int pointsLinhMoc) {

   public static final TribeConfig DEFAULTS = parse(key -> null);

   public static TribeConfig parse(Function<String, String> source) {
      Function<String, String> get = key -> {
         String v = source.apply("tribe." + key);
         return v == null || v.isBlank() ? null : v.trim();
      };
      return new TribeConfig(
            num(get, "joinAfterQuest", 10),
            num(get, "rejoinHours", 12),
            num(get, "applyHours", 24),
            num(get, "maxApplicants", 50),
            num(get, "maxApplicationsPerPlayer", 5),
            num(get, "maxDeputies", 2),
            num(get, "createItem", 168),
            num(get, "nameMax", 12),
            num(get, "sloganMax", 60),
            lng(get, "openSkillGold", 50_000_000L),
            lng(get, "changeSkillGold", 25_000_000L),
            num(get, "emblemDays", 7),
            num(get, "shopId", 7),
            num(get, "warehouseSlots", 50),
            num(get, "nearbyPx", 300),
            num(get, "enduranceMaxPercent", 18),
            num(get, "listMax", 20),
            num(get, "pointsLinhMoc", 1));
   }

   private static int num(Function<String, String> get, String key, int def) {
      String v = get.apply(key);
      return v == null ? def : Integer.parseInt(v);
   }

   private static long lng(Function<String, String> get, String key, long def) {
      String v = get.apply(key);
      return v == null ? def : Long.parseLong(v);
   }
}
