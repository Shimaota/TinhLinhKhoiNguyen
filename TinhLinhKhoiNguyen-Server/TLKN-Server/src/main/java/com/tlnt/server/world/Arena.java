package com.tlnt.server.world;

import com.tlnt.server.social.PkRules;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 1 khu đấu của Giải đấu Bộ lạc ở map {@link #MAP_ID} Thượng nguồn Eldarah ({@code tournament/TournamentService} tạo):
 * vòng loại (Anh hùng / Hợp chiến — tối đa 400 người, PK hỗn chiến, tự hồi sinh) hoặc 1 trận loại trực tiếp (1 vs 1 /
 * 5 vs 5, chết = ra khỏi trận). Mỗi khu đấu = 1 khu riêng của map ({@link GameMap#reserveArena}).
 *
 * <p>Luồng: dịch vụ đặt {@link #fighting} / đội / đồng hồ; luồng tick khu đọc + ghi sát thương / số lần bị hạ gục (map
 * đồng thời, dịch vụ đọc lúc chốt trận).
 *
 * <p>Võ đài (đo từ {@code map_template.colliders} map 22): mặt sàn y = {@link #FLOOR_Y} từ x {@link #LEFT_X} tới
 * {@link #RIGHT_X}; hai bên là vùng thấp (y = 0). Rơi xuống dưới {@link #FALL_Y} = bị hạ gục (lúc chưa cho đánh: kéo lại
 * lên đài).
 */
public final class Arena {

   /** Thượng nguồn Eldarah. */
   public static final int MAP_ID = 22;
   public static final float FLOOR_Y = 288f;
   public static final float LEFT_X = 1080f;
   public static final float RIGHT_X = 3080f;
   /** Thấp hơn mặt sàn bấy nhiêu = đã rơi khỏi võ đài (nhảy không bao giờ xuống dưới sàn). */
   public static final float FALL_Y = FLOOR_Y - 90f;
   /** Đứng cách mép võ đài tối thiểu (chỗ xuất phát / hồi sinh). */
   static final float EDGE_PX = 150f;
   /** Đòn cuối của người khác trong bấy lâu trước khi rơi đài = người đó hạ gục. */
   public static final long FALL_CREDIT_MS = 10_000;

   /** Id dòng đếm ngược op 67 (cạnh nút Thoát). */
   static final int TIMER_ID = 343;

   public static boolean isArenaMap(int mapId) {
      return mapId == MAP_ID;
   }

   public final long id;
   /** Số khu hiển thị. */
   public final int zoneNo;
   /** Vòng loại (hồi sinh tại chỗ sau {@link #reviveMs}, bị hạ gục quá {@link #maxDeaths} lần = loại). */
   public final boolean qualifier;
   /** Chỗ của khu (người thi đấu + dự phòng đăng nhập lại). */
   public final int slots;
   /** Cờ PK khi đang cho đánh: Thử thách (đánh mọi người) / Tổ đội (đánh mọi người trừ cùng đội). */
   public final byte fightFaction;
   public final int maxDeaths;
   public final long reviveMs;

   /** Đang cho đánh (hết 10 s chờ / đúng giờ vòng loại). Chưa cho đánh = không ai gây sát thương được. */
   volatile boolean fighting;
   /** Đã chốt (trận xong / vòng loại hết): không còn ai được giữ chỗ. */
   volatile boolean closed;
   /** Đồng hồ op 67 đang chạy: chữ + mốc bắt đầu / kết thúc (ms); chữ null = không có. */
   volatile String timerText;
   volatile long timerStart;
   volatile long timerEnd;

   /** id người -> đội (cùng đội không đánh nhau); không có = mỗi người 1 phe. */
   private final Map<Integer, Integer> teams = new ConcurrentHashMap<>();
   /** id người -> chỗ xuất phát (trận loại trực tiếp); không có = chỗ ngẫu nhiên trên đài. */
   private final Map<Integer, float[]> spots = new ConcurrentHashMap<>();
   /** Sát thương đã gây lên người khác trong khu (luồng tick ghi). */
   private final Map<Integer, Long> damage = new ConcurrentHashMap<>();
   /** Số lần bị hạ gục (luồng tick ghi). */
   private final Map<Integer, Integer> deaths = new ConcurrentHashMap<>();

   public Arena(long id, int zoneNo, boolean qualifier, int slots, byte fightFaction, int maxDeaths, long reviveMs) {
      this.id = id;
      this.zoneNo = zoneNo;
      this.qualifier = qualifier;
      this.slots = slots;
      this.fightFaction = fightFaction == PkRules.PARTY ? PkRules.PARTY : PkRules.CHALLENGE;
      this.maxDeaths = maxDeaths;
      this.reviveMs = reviveMs;
   }

   public boolean fighting() {
      return fighting;
   }

   public boolean closed() {
      return closed;
   }

   /** Cùng đội (Hợp chiến) — không đánh nhau. */
   public boolean sameTeam(int a, int b) {
      Integer ta = teams.get(a);
      return ta != null && ta.equals(teams.get(b));
   }

   public void setTeam(int playerId, int teamId) {
      teams.put(playerId, teamId);
   }

   public void setSpot(int playerId, float x, float y) {
      spots.put(playerId, new float[] {x, y});
   }

   /** Chỗ đứng của người này: chỗ đã đặt, không có thì ngẫu nhiên trên đài. */
   public float[] spotOf(int playerId) {
      float[] s = spots.get(playerId);
      return s != null ? s.clone() : randomSpot();
   }

   /** 1 chỗ ngẫu nhiên trên mặt võ đài (cách mép {@link #EDGE_PX}). */
   public static float[] randomSpot() {
      float x = (float) ThreadLocalRandom.current().nextDouble(LEFT_X + EDGE_PX, RIGHT_X - EDGE_PX);
      return new float[] {x, FLOOR_Y};
   }

   /**
    * Chỗ xuất phát của trận: phe trái / phải, {@code index} = thứ tự trong phe (5 vs 5 xếp cách nhau 90 px). Hai phe
    * cách nhau ~1000 px giữa đài.
    */
   public static float[] matchSpot(boolean left, int index) {
      float mid = (LEFT_X + RIGHT_X) / 2f;
      float x = left ? mid - 500f - index * 90f : mid + 500f + index * 90f;
      return new float[] {Math.max(LEFT_X + 60f, Math.min(RIGHT_X - 60f, x)), FLOOR_Y};
   }

   /** Cộng sát thương {@code playerId} gây ra (luồng tick khu; test). */
   public void addDamage(int playerId, long amount) {
      if (amount > 0) {
         damage.merge(playerId, amount, Long::sum);
      }
   }

   public long damageOf(int playerId) {
      return damage.getOrDefault(playerId, 0L);
   }

   /** +1 lần bị hạ gục. @return số lần sau khi cộng */
   int addDeath(int playerId) {
      return deaths.merge(playerId, 1, Integer::sum);
   }

   public int deathsOf(int playerId) {
      return deaths.getOrDefault(playerId, 0);
   }

   /** Vòng loại: bị hạ gục quá {@link #maxDeaths} lần (lần thứ maxDeaths + 1) = bị loại. */
   public boolean eliminated(int deathCount) {
      return qualifier && maxDeaths > 0 && deathCount > maxDeaths;
   }

   /** Cờ PK của người trong khu lúc này. */
   byte factionNow() {
      return fighting ? fightFaction : PkRules.PEACE;
   }

   /** CHỈ cho test (không có khu thật): bật / tắt cho đánh. */
   public void setFightingForTest(boolean value) {
      fighting = value;
   }

   /** Lệnh test: đặt số lần bị hạ gục. */
   public void setDeaths(int playerId, int count) {
      deaths.put(playerId, Math.max(0, count));
   }
}
