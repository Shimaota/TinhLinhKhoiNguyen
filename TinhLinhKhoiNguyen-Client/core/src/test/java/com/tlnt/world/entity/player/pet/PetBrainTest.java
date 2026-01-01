package com.tlnt.world.entity.player.pet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pets (client-side simulation): follow the owner on the ground, slower than the owner, jump up steps, do not fly when the owner jumps,
 * wander while the owner stands still. Ported from the server's {@code PetTest}.
 */
class PetBrainTest {

   static final float FLOOR = 100f;
   static final long TICK = 33;

   /** Long ground y 0..100, a 150 px step at x 2000..2400 (top at 250), 2 floating one-way platforms. */
   static PetTerrain map() {
      PetTerrain map = new PetTerrain();
      map.reset(6000);
      map.add(new PetTerrain.Collider(0, 0, 6000, FLOOR, false));
      map.add(new PetTerrain.Collider(2000, FLOOR, 400, 150, false));
      map.add(new PetTerrain.Collider(3000, 450, 800, 20, true)); // top at 470
      map.add(new PetTerrain.Collider(4500, 880, 1000, 20, true)); // top at 900 (too high to jump)
      return map;
   }

   static PetBrain.Owner owner(float x, float y) {
      PetBrain.Owner o = new PetBrain.Owner();
      o.x = x;
      o.y = y;
      return o;
   }

   static PetBrain pet(PetBrain.Owner at, PetTerrain map) {
      PetBrain pet = new PetBrain();
      pet.placeBeside(at, map);
      return pet;
   }

   @Test
   void followsOnGroundSlowerThanOwner() {
      PetTerrain map = map();
      PetBrain.Owner owner = owner(500, FLOOR);
      PetBrain pet = pet(owner, map);
      long now = 1_000;
      float maxStep = 0;
      for (int i = 0; i < 60; i++) { // owner runs right at 500 px/s, sometimes jumping 200 high
         now += TICK;
         owner.x += 500 * TICK / 1000f;
         owner.y = i % 20 < 8 ? FLOOR + 200 : FLOOR;
         owner.lastMoveAt = now;
         float before = pet.x;
         pet.update(owner, map, now, TICK / 1000f);
         maxStep = Math.max(maxStep, Math.abs(pet.x - before));
         assertEquals(FLOOR, pet.y, 0.01f, "chủ nhảy thì thú vẫn dưới đất");
      }
      assertTrue(pet.x > 600, "có chạy theo: " + pet.x);
      assertTrue(owner.x - pet.x > PetBrain.FOLLOW_STOP_PX, "chậm hơn chủ");
      assertTrue(maxStep <= 500 * TICK / 1000f + 1, "không nhanh hơn chủ: bước lớn nhất " + maxStep);
   }

   /** When the owner walks steadily the pet runs continuously with gradual speed changes — no run - stop - run. */
   @Test
   void noStopAndGoWhileOwnerWalks() {
      PetTerrain map = map();
      PetBrain.Owner owner = owner(500, FLOOR);
      PetBrain pet = pet(owner, map);
      long now = 1_000;
      int stops = 0;
      float lastStep = -1;
      float maxChange = 0;
      for (int i = 0; i < 200; i++) { // khung 16 ms (60 Hz)
         now += 16;
         owner.x += 400 * 16 / 1000f;
         owner.lastMoveAt = now;
         float before = pet.x;
         pet.update(owner, map, now, 0.016f);
         float step = pet.x - before;
         if (i > 40) {
            if (step == 0) {
               stops++;
            }
            if (lastStep >= 0) {
               maxChange = Math.max(maxChange, Math.abs(step - lastStep));
            }
            lastStep = step;
         }
      }
      assertEquals(0, stops, "không dừng giữa chừng");
      assertTrue(maxChange < 2f, "tốc độ đổi dần: " + maxChange);
      assertTrue(owner.x - pet.x > PetBrain.FOLLOW_STOP_PX && owner.x - pet.x < 300, "lẽo đẽo phía sau: "
            + (owner.x - pet.x));
   }

   @Test
   void jumpsOntoStep() {
      PetTerrain map = map();
      PetBrain.Owner owner = owner(2200, FLOOR + 150);
      PetBrain pet = pet(owner(1700, FLOOR), map);
      long now = 1_000;
      float highest = FLOOR;
      for (int i = 0; i < 150; i++) {
         now += TICK;
         pet.update(owner, map, now, TICK / 1000f);
         highest = Math.max(highest, pet.y);
      }
      assertTrue(highest > FLOOR + 150, "có nhảy lên");
      assertEquals(FLOOR + 150, pet.y, 0.01f, "đứng trên bậc cạnh chủ");
      assertTrue(Math.abs(owner.x - pet.x) < PetBrain.FOLLOW_START_PX, "tới cạnh chủ: " + pet.x);
   }

   @Test
   void climbsToOwnerOnHighPlatformFromFarAway() {
      PetTerrain map = map();
      PetBrain.Owner owner = owner(3700, 470);
      PetBrain pet = pet(owner(2900, FLOOR), map);
      long now = 1_000;
      for (int i = 0; i < 400; i++) {
         now += 18;
         assertTrue(pet.update(owner, map, now, 0.018f) != PetBrain.WARPED, "nhảy lên được, không cần hiện lại");
      }
      assertEquals(470f, pet.y, 0.01f, "đứng trên nền của chủ");
      assertTrue(Math.abs(owner.x - pet.x) < PetBrain.FOLLOW_START_PX, "tới cạnh chủ: " + pet.x);
   }

   @Test
   void dropsThroughOneWayPlatformWhenOwnerBelow() {
      PetTerrain map = map();
      PetBrain.Owner owner = owner(3500, FLOOR);
      PetBrain pet = pet(owner(3400, 470), map);
      assertEquals(470f, pet.y, 0.01f);
      long now = 1_000;
      for (int i = 0; i < 150; i++) {
         now += 18;
         assertTrue(pet.update(owner, map, now, 0.018f) != PetBrain.WARPED);
      }
      assertEquals(FLOOR, pet.y, 0.01f, "đã xuống đất cùng chủ");
   }

   @Test
   void walksOffSolidStepWhenOwnerBelow() {
      PetTerrain map = map();
      PetBrain.Owner owner = owner(2480, FLOOR);
      PetBrain pet = pet(owner(2350, FLOOR + 150), map);
      assertEquals(FLOOR + 150, pet.y, 0.01f);
      long now = 1_000;
      for (int i = 0; i < 150; i++) {
         now += 18;
         assertTrue(pet.update(owner, map, now, 0.018f) != PetBrain.WARPED);
      }
      assertEquals(FLOOR, pet.y, 0.01f);
   }

   /** Owner walks left then turns right: the pet does not run in front of the owner — it waits for the owner to pass, then follows behind. */
   @Test
   void staysBehindWhenOwnerTurnsAround() {
      PetTerrain map = map();
      PetBrain.Owner owner = owner(1500, FLOOR);
      PetBrain pet = pet(owner, map);
      long now = 1_000;
      for (int i = 0; i < 80; i++) {
         now += 18;
         owner.x -= 400 * 18 / 1000f;
         owner.lastMoveAt = now;
         pet.update(owner, map, now, 0.018f);
      }
      assertTrue(pet.x > owner.x, "đang theo sau (bên phải) khi chủ đi trái");
      for (int i = 0; i < 200; i++) {
         now += 18;
         owner.x += 400 * 18 / 1000f;
         owner.lastMoveAt = now;
         float before = pet.x;
         boolean ahead = pet.x > owner.x;
         pet.update(owner, map, now, 0.018f);
         if (i > 15 && ahead) {
            assertEquals(before, pet.x, 0.001f, "đứng chờ chủ đi qua, bước " + i);
         }
         if (i > 15) {
            assertTrue(pet.x <= before + 0.001f || pet.x < owner.x, "không chạy trước mặt chủ, bước " + i);
         }
      }
      assertTrue(pet.x < owner.x - PetBrain.FOLLOW_STOP_PX * 0.5f, "đã ở sau lưng: " + (owner.x - pet.x));
   }

   /** Owner shuffling slightly nearby: the pet stays still. */
   @Test
   void ignoresSmallOwnerShuffles() {
      PetTerrain map = map();
      PetBrain.Owner owner = owner(1000, FLOOR);
      PetBrain pet = pet(owner, map);
      long now = 1_000;
      int moves = 0;
      for (int i = 0; i < 100; i++) {
         now += 18;
         owner.x = 1000 + (i % 20 < 10 ? 30 : -30);
         owner.lastMoveAt = now;
         if (pet.update(owner, map, now, 0.018f) == PetBrain.MOVED) {
            moves++;
         }
      }
      assertEquals(0, moves);
   }

   @Test
   void warpsWhenPlatformTooHigh() {
      PetTerrain map = map();
      PetBrain.Owner owner = owner(5000, 900);
      PetBrain pet = pet(owner(4600, FLOOR), map);
      long now = 1_000;
      boolean warped = false;
      for (int i = 0; i < 200 && !warped; i++) {
         now += 18;
         warped = pet.update(owner, map, now, 0.018f) == PetBrain.WARPED;
      }
      assertTrue(warped);
      assertEquals(900f, pet.y, 0.01f);
   }

   @Test
   void wandersAroundIdleOwnerWithoutFalling() {
      PetTerrain map = map();
      PetBrain.Owner owner = owner(1000, FLOOR);
      owner.lastMoveAt = 0;
      PetBrain pet = pet(owner, map);
      long now = 10_000;
      float min = pet.x;
      float max = pet.x;
      int legs = 0;
      float legStart = pet.x;
      float lastStep = 0;
      float lastDir = 0;
      int turns = 0;
      float shortest = Float.MAX_VALUE;
      float longest = 0;
      long stoppedAt = 0;
      float fullStep = PetBrain.WANDER_SPEED * 0.018f;
      for (int i = 0; i < 5_600; i++) { // ~100 seconds
         now += 18;
         float before = pet.x;
         pet.update(owner, map, now, 0.018f);
         float step = pet.x - before;
         if (step != 0 && lastStep == 0) {
            legStart = before;
            if (Math.signum(step) != lastDir) {
               turns++;
            }
            lastDir = Math.signum(step);
            if (stoppedAt != 0) {
               assertTrue(now - stoppedAt >= PetBrain.WANDER_PAUSE_MIN_MS - 18, "nghỉ đủ lâu: " + (now - stoppedAt));
            }
         }
         if (step != 0 && lastStep != 0) {
            assertEquals(fullStep, Math.abs(lastStep), 0.01f, "tốc đều");
         }
         if (step == 0 && lastStep != 0) {
            float len = Math.abs(pet.x - legStart);
            assertTrue(len >= PetBrain.WANDER_MIN_LEG_PX - 1, "lượt quá ngắn: " + len);
            legs++;
            shortest = Math.min(shortest, len);
            longest = Math.max(longest, len);
            stoppedAt = now;
         }
         lastStep = step;
         min = Math.min(min, pet.x);
         max = Math.max(max, pet.x);
         assertEquals(FLOOR, pet.y, 0.01f);
      }
      assertTrue(legs >= 4 && legs <= 25, "thỉnh thoảng mới đi: " + legs + " lượt / 100 s");
      assertTrue(turns >= legs / 2, "phần lớn là chạy qua chạy lại: " + turns + " / " + legs);
      assertTrue(shortest < longest * 0.6f, "dừng ở điểm ngẫu nhiên: " + shortest + ".." + longest);
      assertTrue(min >= owner.x - PetBrain.WANDER_RANGE_PX - 1 && max <= owner.x + PetBrain.WANDER_RANGE_PX + 1);
   }

   @Test
   void warpsBesideOwnerWhenFarAway() {
      PetTerrain map = map();
      PetBrain.Owner owner = owner(500, FLOOR);
      PetBrain pet = pet(owner, map);
      owner.x = 4000;
      assertEquals(PetBrain.WARPED, pet.update(owner, map, 5_000, 0.018f));
      assertTrue(Math.abs(pet.x - owner.x) <= PetBrain.FOLLOW_STOP_PX + 1);
      assertEquals(FLOOR, pet.y, 0.01f);
   }

   @Test
   void slopeSurface() {
      assertEquals(150f, PetBrain.triangleTop(new float[] {0, 100, 100, 200, 100, 100}, 50), 0.01f);
   }
}
