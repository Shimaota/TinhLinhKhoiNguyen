package com.tlnt.model.weeklytop;

import com.badlogic.gdx.utils.Array;
import com.tlnt.model.item.Item;

/** Weekly event, rank rewards and leaderboard sent by the server. */
public final class WeeklyTopEvent {
   public int id;
   public String name;
   public long endTime;
   public final Array<Item> firstPlaceRewards = new Array<>();
   public final Array<Item> secondPlaceRewards = new Array<>();
   public final Array<Item> thirdPlaceRewards = new Array<>();
   public final Array<WeeklyTopEntry> entries = new Array<>();
   public WeeklyTopEntry myEntry;

   public WeeklyTopEvent(int id, String name, long endTime) {
      this.id = id;
      this.name = name == null ? "" : name;
      this.endTime = endTime;
   }

   public void clearRewards() {
      clearItems(firstPlaceRewards);
      clearItems(secondPlaceRewards);
      clearItems(thirdPlaceRewards);
   }

   private static void clearItems(Array<Item> items) {
      for (Item item : items) {
         if (item != null) {
            item.clear();
         }
      }
      items.clear();
   }
}
