package com.tlnt.server.tournament;

import com.tlnt.server.item.ItemService;
import com.tlnt.server.player.ItemData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Luật + hằng số Giải đấu Bộ lạc (không trạng thái, test được). Quyết định của người dùng: memory
 * {@code tribe-tournament-decisions}.
 */
public final class TournamentRules {

   private TournamentRules() {
   }

   /** Tối đa người đăng ký (giải hằng ngày) / người ở vòng loại (Anh hùng, Hợp chiến). */
   public static final int MAX_ENTRANTS = 400;
   /** Giải hằng ngày: đăng ký phút 0-25, bắt đầu phút 30. */
   public static final int REGISTER_MINUTES = 25;
   public static final int START_MINUTE = 30;
   /** Anh hùng / Hợp chiến: tập trung phút 0-15, vòng loại phút 15-25. */
   public static final int GATHER_MINUTES = 15;
   public static final int QUALIFY_MINUTES = 10;
   /** Vào vòng trong. */
   public static final int TOP = 16;

   /** Vào trận chờ 10 s (không gây sát thương), đánh 1 phút 30 giây. */
   public static final long WAIT_MS = 10_000;
   public static final long FIGHT_MS = 90_000;
   /** Về làng chờ vòng sau. */
   public static final long BREAK_MS = 60_000;
   /** Vòng loại: bị hạ gục quá 20 lần = loại, tự hồi sinh sau 3 giây. */
   public static final int QUALIFY_MAX_DEATHS = 20;
   public static final long QUALIFY_REVIVE_MS = 3_000;
   /** Tổ đội tối đa (5 vs 5). */
   public static final int TEAM_SIZE = 5;

   /** Điểm hoạt động (hướng dẫn "Điểm hoạt động"): thắng 1 vòng / vô địch. */
   public static final int ROUND_ACTIVITY = 5;
   public static final int CHAMPION_ACTIVITY = 50;

   public static final String NO_REVIVE_HERB = "Không thể dùng Cỏ hồi sinh ở đấu trường.";
   public static final String TIMER_WAIT = "Trận đấu bắt đầu sau";
   public static final String TIMER_FIGHT = "Giải đấu Bộ lạc";
   public static final String TIMER_GATHER = "Vòng loại bắt đầu sau";
   public static final String TIMER_QUALIFY = "Vòng loại";

   // ------------------------------------------------------------------ thông báo nhắc giờ

   /**
    * Phút (tính từ lúc mở) phát thông báo nhắc toàn server trong cửa sổ {@code windowMinutes} phút: 5 phút / lần, 5 phút
    * cuối 1 phút / lần. 25 phút -> 0, 5, 10, 15, 20, 21, 22, 23, 24; 15 phút -> 0, 5, 10, 11, 12, 13, 14.
    */
   public static List<Integer> reminderMinutes(int windowMinutes) {
      List<Integer> list = new ArrayList<>();
      int last5 = Math.max(0, windowMinutes - 5);
      for (int m = 0; m < last5; m += 5) {
         list.add(m);
      }
      for (int m = last5; m < windowMinutes; m++) {
         if (!list.contains(m)) {
            list.add(m);
         }
      }
      return list;
   }

   // ------------------------------------------------------------------ chia cặp

   /** 1 cặp; {@code b} null = được vào thẳng vòng sau (không thưởng). */
   public record Pair<T>(T a, T b) {
      public boolean bye() {
         return b == null;
      }
   }

   /** Số lượt miễn đấu vòng đầu của nhánh 16: đủ lên luỹ thừa 2 gần nhất (10 người -> 6, vòng sau còn 8). */
   public static int bracketByes(int n) {
      if (n <= 1) {
         return 0;
      }
      int size = Integer.highestOneBit(n - 1) << 1;
      return size - n;
   }

   /**
    * Chia cặp ngẫu nhiên. {@code seeded} = vòng đầu Anh hùng / Hợp chiến: {@code sides} đã xếp theo điểm vòng loại, người
    * điểm cao nhất được miễn đấu ({@link #bracketByes}); không thì lẻ 1 người -> 1 người ngẫu nhiên vào thẳng vòng sau.
    */
   public static <T> List<Pair<T>> pair(List<T> sides, boolean seeded, Random random) {
      List<T> rest = new ArrayList<>(sides);
      List<Pair<T>> pairs = new ArrayList<>();
      int byes = seeded ? bracketByes(rest.size()) : rest.size() % 2;
      for (int i = 0; i < byes && !rest.isEmpty(); i++) {
         T lucky = seeded ? rest.remove(0) : rest.remove(random.nextInt(rest.size()));
         pairs.add(new Pair<>(lucky, null));
      }
      Collections.shuffle(rest, random);
      for (int i = 0; i + 1 < rest.size(); i += 2) {
         pairs.add(new Pair<>(rest.get(i), rest.get(i + 1)));
      }
      if (rest.size() % 2 == 1) { // không xảy ra (đã tính miễn đấu), phòng hờ
         pairs.add(new Pair<>(rest.get(rest.size() - 1), null));
      }
      return pairs;
   }

   // ------------------------------------------------------------------ thắng thua khi hết giờ

   /**
    * Hết 1 phút 30 giây chưa ai bị hạ hết: bên gây nhiều sát thương hơn thắng; bằng nhau -> bên còn nhiều % HP hơn (phần
    * nghìn); vẫn bằng -> ngẫu nhiên. @return true = bên A thắng
    */
   public static boolean aWinsOnTime(long damageA, long damageB, int hpPermilleA, int hpPermilleB, Random random) {
      if (damageA != damageB) {
         return damageA > damageB;
      }
      if (hpPermilleA != hpPermilleB) {
         return hpPermilleA > hpPermilleB;
      }
      return random.nextBoolean();
   }

   /** % HP còn (phần nghìn) của cả phe: tổng HP / tổng HP tối đa. */
   public static int hpPermille(long hp, long maxHp) {
      return maxHp <= 0 ? 0 : (int) Math.max(0, Math.min(1000, hp * 1000 / maxHp));
   }

   // ------------------------------------------------------------------ phần thưởng (qua thư)

   /** Thưởng thắng 1 vòng (mọi giải, khoá): vàng 10-20k, Túi đá nguyên tố 1-2, Đá khởi nguyên 10-50, Cỏ hồi sinh 1-3, Bùa dịch chuyển. */
   public static List<ItemData> roundReward(Random random) {
      List<ItemData> items = new ArrayList<>();
      items.add(locked(ItemService.GOLD_TEMPLATE_ID, range(random, 10_000, 20_000)));
      items.add(locked(ELEMENT_BAG, range(random, 1, 2)));
      items.add(locked(ORIGIN_STONE, range(random, 10, 50)));
      items.add(locked(REVIVE_HERB, range(random, 1, 3)));
      items.add(locked(TELEPORT_CHARM, 1));
      return items;
   }

   /**
    * Thưởng vô địch (thêm vào thưởng vòng chung kết): vàng 10-50k, Túi đá nguyên tố 1-5, Đá khởi nguyên 10-150, Cỏ hồi sinh
    * 1-5, Bùa dịch chuyển, Ấn thủ lĩnh (giải hằng ngày spec không ghi khoá -> không khoá; Anh hùng / Hợp chiến khoá) +
    * danh hiệu 3 ngày: Anh hùng = Chiến thần (+150 Tấn công, +200 Giáp), Hợp chiến = Thống lĩnh (+250 Tấn công lên quái,
    * +2500 HP, +2500 MP).
    */
   public static List<ItemData> championReward(TournamentKind kind, Random random, long now) {
      List<ItemData> items = new ArrayList<>();
      items.add(locked(ItemService.GOLD_TEMPLATE_ID, range(random, 10_000, 50_000)));
      items.add(locked(ELEMENT_BAG, range(random, 1, 5)));
      items.add(locked(ORIGIN_STONE, range(random, 10, 150)));
      items.add(locked(REVIVE_HERB, range(random, 1, 5)));
      items.add(locked(TELEPORT_CHARM, 1));
      ItemData seal = ItemData.of(LEADER_SEAL, 1);
      seal.locked = !kind.daily();
      items.add(seal);
      switch (kind.format) {
         case HERO -> items.add(title(TITLE_WAR_GOD, now, new int[][] {{STAT_ATTACK, 150}, {STAT_ARMOR, 200}}));
         case TEAM -> items.add(title(TITLE_COMMANDER, now,
               new int[][] {{STAT_ATTACK_VS_MOB, 250}, {STAT_HP, 2500}, {STAT_MP, 2500}}));
         default -> {
         }
      }
      return items;
   }

   static final int ELEMENT_BAG = 269;
   static final int ORIGIN_STONE = 274;
   static final int REVIVE_HERB = 238;
   static final int TELEPORT_CHARM = 124;
   static final int LEADER_SEAL = 261;
   /** Danh hiệu Chiến thần (Anh hùng) / Thống lĩnh (Hợp chiến). */
   static final int TITLE_WAR_GOD = 211;
   static final int TITLE_COMMANDER = 207;
   static final int TITLE_DAYS = 3;
   /** stat_type: 3 HP, 5 MP, 7 Tấn công, 9 Tấn công lên quái, 15 Giáp. */
   static final int STAT_HP = 3;
   static final int STAT_MP = 5;
   static final int STAT_ATTACK = 7;
   static final int STAT_ATTACK_VS_MOB = 9;
   static final int STAT_ARMOR = 15;

   private static ItemData title(int templateId, long now, int[][] stats) {
      ItemData item = ItemData.of(templateId, 1);
      item.locked = true;
      item.createdTime = now;
      for (int[] s : stats) {
         item.stats.add(new ItemData.StatValue(s[0], new int[] {s[1]}));
      }
      item.expireTime = now + TITLE_DAYS * 86_400_000L;
      return item;
   }

   private static ItemData locked(int templateId, int quantity) {
      ItemData item = ItemData.of(templateId, quantity);
      item.locked = true;
      return item;
   }

   static int range(Random random, int min, int max) {
      return min + random.nextInt(max - min + 1);
   }
}
