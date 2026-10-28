package com.tlnt.server.bot;

import com.tlnt.server.player.PlayerService;

import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Tên người chơi giả — trộn kiểu đặt tên hay gặp trong game Việt: kiếm hiệp ("Hắc Long", "TiểuPhong"), tên thật + số
 * ("Tuấn2k5", "linhnhi99"), dễ thương ("MèoMập", "BéNa"), tiếng Anh ("ShadowKing"). Luôn qua đúng luật tạo nhân vật
 * ({@link PlayerService#normalizeCharacterName}: 5-16 ký tự chữ / số / khoảng trắng). Không trạng thái — chỉ hàm thuần.
 */
final class BotNames {

   private static final String[] HERO_A = {"Hắc", "Bạch", "Tiểu", "Đại", "Long", "Hổ", "Phong", "Vân", "Thiên", "Ma",
         "Kiếm", "Huyết", "Băng", "Hỏa", "Lôi", "Dạ", "Ngạo", "Cuồng", "Sát", "Tà", "Thần", "Mộng", "Lãng", "Phiêu",
         "Tuyết", "Nguyệt", "Nhật", "Tinh", "Hoàng", "Vô", "Độc", "Ảo", "Ám", "Thánh", "Bá"};
   private static final String[] HERO_B = {"Long", "Hổ", "Phong", "Vân", "Thiên", "Kiếm", "Ma", "Thần", "Sát", "Vương",
         "Đế", "Tử", "Hiệp", "Khách", "Nhi", "Lang", "Ảnh", "Linh", "Tuyết", "Nguyệt", "Hồn", "Ưng", "Điệp", "Quân",
         "Bảo", "Tâm", "Kiệt", "Phượng", "Hà", "Mị"};
   private static final String[] REAL = {"Tuấn", "Hùng", "Minh", "Nam", "Linh", "Trang", "Huy", "Khoa", "Vy", "Ngọc",
         "Phúc", "Bảo", "Duy", "Quân", "Thảo", "My", "Long", "Đạt", "Tú", "Hiếu", "Thắng", "Toàn", "Khánh", "Vinh",
         "Sơn", "Trung", "Hải", "Lâm", "Nhung", "Hương", "Yến", "Quỳnh", "Phương", "Hân", "An", "Kiên", "Lộc", "Tài",
         "Thịnh", "Nghĩa"};
   private static final String[] REAL_TAIL = {"Pro", "Vip", "Noob", "Ka", "Kute", "Dz", "Bg", "Zz", "TV", "GM", "Kid"};
   private static final String[] CUTE = {"MèoMập", "CáMập", "GàCon", "HeoCon", "ThỏNgọc", "BéNa", "BéHeo", "Gấu",
         "SóiCon", "CáoNhỏ", "RồngCon", "MèoMun", "BắpNgô", "KẹoNgọt", "Sushi", "MítTơ", "BánhBao", "Cún", "Xoài"};
   private static final String[] EN_A = {"Shadow", "Dark", "Dragon", "Ghost", "Silent", "Night", "Storm", "Fire",
         "Ice", "Lucky", "Crazy", "Mr", "Super", "Black", "Red", "King"};
   private static final String[] EN_B = {"King", "Killer", "Hunter", "Wolf", "Ninja", "Sniper", "Angel", "Devil", "Lord",
         "Boy", "Girl", "Master", "Rider", "Zero", "Fox", "Blade"};
   private static final String[] JP = {"Kenji", "Akira", "Yuki", "Ryu", "Sakura", "Hiro", "Naruto", "Itachi", "Zoro",
         "Luffy", "Kaito", "Rin"};

   private BotNames() {
   }

   /**
    * 1 tên mới hợp lệ, chưa ai dùng ({@code taken} nhận tên chữ thường). Thử nhiều lần rồi mới thêm số đuôi.
    * @return null nếu thử mãi vẫn trùng (gần như không xảy ra)
    */
   static String next(Random rnd, Predicate<String> taken) {
      for (int attempt = 0; attempt < 40; attempt++) {
         String name = PlayerService.normalizeCharacterName(raw(rnd, attempt >= 20));
         if (name != null && !taken.test(key(name))) {
            return name;
         }
      }
      return null;
   }

   /** Khoá so trùng tên (chữ thường, như cột {@code player.name} collation không phân biệt hoa thường). */
   static String key(String name) {
      return name.toLowerCase(Locale.ROOT);
   }

   private static String raw(Random rnd, boolean forceNumber) {
      String base = switch (rnd.nextInt(100) / 10) {
         case 0, 1, 2 -> pick(rnd, HERO_A) + (rnd.nextInt(3) == 0 ? " " : "") + pick(rnd, HERO_B);
         case 3, 4 -> realName(rnd);
         case 5 -> pick(rnd, CUTE) + (rnd.nextBoolean() ? pick(rnd, new String[] {"", "Xinh", "Ú", "Lười", "Đen"}) : "");
         case 6 -> pick(rnd, EN_A) + pick(rnd, EN_B);
         case 7 -> pick(rnd, JP) + pick(rnd, new String[] {"Kun", "Chan", "Sama", "VN", "2k", "Pro"});
         case 8 -> stripMarks(realName(rnd)).toLowerCase(Locale.ROOT); // gõ không dấu: "tuanpro", "linhnhi"
         default -> pick(rnd, HERO_A) + pick(rnd, REAL);
      };
      if (forceNumber || rnd.nextInt(100) < 30) {
         base += number(rnd);
      }
      if (base.codePointCount(0, base.length()) < 5) {
         base += number(rnd);
      }
      if (base.codePointCount(0, base.length()) > 16) {
         base = base.substring(0, base.offsetByCodePoints(0, 16)).trim();
      }
      return base;
   }

   private static String realName(Random rnd) {
      String first = pick(rnd, REAL);
      return switch (rnd.nextInt(4)) {
         case 0 -> first + pick(rnd, REAL_TAIL);
         case 1 -> first + pick(rnd, REAL);
         case 2 -> pick(rnd, new String[] {"Anh", "Em", "Bé", "Chị", "Ông", "Lão"}) + first;
         default -> first + pick(rnd, HERO_B);
      };
   }

   private static String number(Random rnd) {
      return switch (rnd.nextInt(5)) {
         case 0 -> String.valueOf(rnd.nextInt(90) + 10);
         case 1 -> "2k" + (rnd.nextInt(12) + 1);
         case 2 -> String.valueOf(1990 + rnd.nextInt(20));
         case 3 -> String.valueOf(rnd.nextInt(9) + 1);
         default -> String.valueOf(rnd.nextInt(900) + 100);
      };
   }

   /** Bỏ dấu tiếng Việt (đ -> d). */
   static String stripMarks(String s) {
      String n = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
      return n.replace('đ', 'd').replace('Đ', 'D');
   }

   private static String pick(Random rnd, String[] list) {
      return list[rnd.nextInt(list.length)];
   }

   /** Tên test: có trong tập không (chữ thường). */
   static Predicate<String> in(Set<String> names) {
      return names::contains;
   }
}
