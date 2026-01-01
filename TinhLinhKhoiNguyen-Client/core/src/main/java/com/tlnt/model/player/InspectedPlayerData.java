package com.tlnt.model.player;

import com.badlogic.gdx.utils.Array;
import com.tlnt.model.item.Item;
import com.tlnt.model.item.StatLine;

public final class InspectedPlayerData {
   public PlayerStats playerStats = new PlayerStats();
   public Array<Item> equipmentSlots = new Array<>();
   public Array<Item> bagSlots1 = new Array<>();
   public Array<Item> bagSlots2 = new Array<>();
   public Array<Item> bagSlots3 = new Array<>();
   public Array<Item> bagSlots4 = new Array<>();
   /** Separate backwear collection, matching array f1262 in the decompiled character-inspect data. */
   public Array<Item> backpackSlots = new Array<>();
   public Array<StatLine> skillDescriptions1 = new Array<>();
   public Array<StatLine> skillDescriptions2 = new Array<>();
   public Array<StatLine> skillDescriptions3 = new Array<>();
   public Array<StatLine> skillDescriptions4 = new Array<>();
   public Array<StatLine> backpackSkillDescriptions = new Array<>();
   public Item selectedItem1;
   public Item selectedItem2;
   public Item selectedItem3;
   public Item selectedItem4;
   public Item selectedBackpackItem;

   public final void clearEquipmentSlots() {
      clearItemSlots(this.equipmentSlots);
   }

   public final void clearBagSlots1() {
      clearItemSlots(this.bagSlots1);
   }

   public final void clearBagSlots2() {
      clearItemSlots(this.bagSlots2);
   }

   public final void clearBagSlots3() {
      clearItemSlots(this.bagSlots3);
   }

   public final void clearBagSlots4() {
      clearItemSlots(this.bagSlots4);
   }

   public final void clearBackpackSlots() {
      clearItemSlots(this.backpackSlots);
      this.selectedBackpackItem = null;
   }

   public final void clearBackpackSkillDescriptions() {
      if (this.backpackSkillDescriptions != null && !this.backpackSkillDescriptions.isEmpty()) {
         Array.ArrayIterator<StatLine> it = this.backpackSkillDescriptions.iterator();
         while (it.hasNext()) {
            it.next().clear();
         }
         this.backpackSkillDescriptions.clear();
      }
   }

   public static void clearItemSlots(Array<Item> slots) {
      if (slots != null && !slots.isEmpty()) {
         Array.ArrayIterator it = slots.iterator();

         while (it.hasNext()) {
            Item item;
            if ((item = (Item)it.next()) != null) {
               item.clear();
            }
         }

         slots.clear();
      }
   }
}
