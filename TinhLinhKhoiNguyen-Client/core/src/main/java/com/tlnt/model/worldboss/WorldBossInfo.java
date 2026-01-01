package com.tlnt.model.worldboss;

import com.badlogic.gdx.utils.Array;
import com.tlnt.model.item.Item;

/** One boss in the World Boss table (S→C op 100 sub 0 — server {@code worldboss/WorldBossList.listPacket}). */
public final class WorldBossInfo {
   public static final int STATE_WAITING = 0;
   public static final int STATE_ALIVE = 1;
   public static final int STATE_SPAWNING = 2;

   public int id;
   public String name = "";
   /** true = fake-player boss (chars/N + skin), false = mob boss (mob_template). */
   public boolean character;
   public int templateId;
   public String[] skins = new String[0];
   public int level;
   public long attack;
   public long hp;
   public long maxHp;
   public int state;
   /** Time (System.currentTimeMillis) the boss respawns, only when {@link #STATE_WAITING}. */
   public long respawnAt;
   /** Map the boss is currently on (only while alive — the server does not send the zone). */
   public String mapName = "";
   /** Maps it can spawn on, joined with ", ". */
   public String spawnMaps = "";
   public int respawnMinutes;
   public final Array<Loot> loot = new Array<>();

   /** One reward row: preview item + quantity (range) + drop %. */
   public static final class Loot {
      public final Item item;
      public final int minQty;
      public final int maxQty;
      public final float chance;

      public Loot(Item item, int minQty, int maxQty, float chance) {
         this.item = item;
         this.minQty = minQty;
         this.maxQty = maxQty;
         this.chance = chance;
      }
   }
}
