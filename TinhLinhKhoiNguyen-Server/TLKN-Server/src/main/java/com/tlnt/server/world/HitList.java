package com.tlnt.server.world;

/**
 * Danh sách mục tiêu trúng 1 đòn diện rộng (id, HP còn, sát thương, chí mạng) — quái hoặc người chơi. Chỉ luồng tick khu.
 */
final class HitList {

   final int[] ids;
   final long[] hpLeft;
   final long[] damage;
   /** Đòn chí mạng (client hiện chữ chí mạng). */
   final boolean[] crit;
   int count;

   HitList(int capacity) {
      ids = new int[capacity];
      hpLeft = new long[capacity];
      damage = new long[capacity];
      crit = new boolean[capacity];
   }

   boolean full() {
      return count >= ids.length;
   }

   void add(int id, long hp, long dmg) {
      add(id, hp, dmg, false);
   }

   void add(int id, long hp, long dmg, boolean isCrit) {
      ids[count] = id;
      hpLeft[count] = hp;
      damage[count] = dmg;
      crit[count] = isCrit;
      count++;
   }

   static final HitList EMPTY = new HitList(0);
}
