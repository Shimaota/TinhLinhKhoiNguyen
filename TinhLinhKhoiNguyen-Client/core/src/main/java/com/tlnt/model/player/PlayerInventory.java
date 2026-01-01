package com.tlnt.model.player;

import com.badlogic.gdx.utils.Array;
import com.tlnt.model.item.Item;
import com.tlnt.model.item.StatLine;

public final class PlayerInventory {
   public long inventoryTimestamp1;
   public long inventoryTimestamp2;
   public long inventoryTimestamp3;
   public int inventoryItemCount;
   /** Total Lucky Coins (item 307) in the inventory — the counter on the Lucky Wheel tab (decompiled C0146.f1272). */
   public int luckyCoinCount;
   public int materialItemCount;
   public Array<Item> equipmentSlots = new Array<>();
   public Array<Item> weaponSlots = new Array<>();
   public Array<Item> armorSlots = new Array<>();
   public Array<Item> accessorySlots = new Array<>();
   public Array<Item> specialSlots = new Array<>();
   /** Dedicated backwear collection slots in the decompiled client. */
   public Array<Item> backpackSlots = new Array<>();
   public Array<Item> inventoryItems = new Array<>();
   public Array<Item> materialItems = new Array<>();
   public boolean inventoryFlag1;
   public boolean inventoryFlag2;
   public boolean inventoryFlag3;
   public boolean inventoryFlag4;
   public Array<StatLine> skillDescriptions1 = new Array<>();
   public Array<StatLine> skillDescriptions2 = new Array<>();
   public Array<StatLine> skillDescriptions3 = new Array<>();
   public Array<StatLine> skillDescriptions4 = new Array<>();
   public Array<StatLine> backpackSkillDescriptions = new Array<>();
   public Item selectedWeaponSlot;
   public Item selectedArmorSlot;
   public Item selectedAccessorySlot;
   public Item selectedSpecialSlot;
   public Item selectedBackpackSlot;
   public boolean hideBackpack;
   public Item quickSlot1;
   public Item quickSlot2;
   public Item quickSlot3;
   public Item quickSlot4;

   public final void clearEquipmentSlots() {
      clearItemSlots(this.equipmentSlots);
   }

   public final void clearWeaponSlots() {
      clearItemSlots(this.weaponSlots);
      this.selectedWeaponSlot = null;
   }

   public final void clearArmorSlots() {
      clearItemSlots(this.armorSlots);
      this.selectedArmorSlot = null;
   }

   public final void clearAccessorySlots() {
      clearItemSlots(this.accessorySlots);
      this.selectedAccessorySlot = null;
   }

   public final void clearSpecialSlots() {
      clearItemSlots(this.specialSlots);
      this.selectedSpecialSlot = null;
   }

   public final void clearBackpackSlots() {
      clearItemSlots(this.backpackSlots);
      this.selectedBackpackSlot = null;
   }

   public final void clearInventoryItems() {
      clearItemSlots(this.inventoryItems);
   }

   public final void clearMaterialItems() {
      clearItemSlots(this.materialItems);
   }

   public final void clearSkillDescriptions1() {
      if (this.skillDescriptions1 != null && !this.skillDescriptions1.isEmpty()) {
         Array.ArrayIterator it = this.skillDescriptions1.iterator();

         while (it.hasNext()) {
            ((StatLine)it.next()).clear();
         }

         this.skillDescriptions1.clear();
      }
   }

   public final void clearSkillDescriptions2() {
      if (this.skillDescriptions2 != null && !this.skillDescriptions2.isEmpty()) {
         Array.ArrayIterator it = this.skillDescriptions2.iterator();

         while (it.hasNext()) {
            ((StatLine)it.next()).clear();
         }

         this.skillDescriptions2.clear();
      }
   }

   public final void clearSkillDescriptions3() {
      if (this.skillDescriptions3 != null && !this.skillDescriptions3.isEmpty()) {
         Array.ArrayIterator it = this.skillDescriptions3.iterator();

         while (it.hasNext()) {
            ((StatLine)it.next()).clear();
         }

         this.skillDescriptions3.clear();
      }
   }

   public final void clearSkillDescriptions4() {
      if (this.skillDescriptions4 != null && !this.skillDescriptions4.isEmpty()) {
         Array.ArrayIterator it = this.skillDescriptions4.iterator();

         while (it.hasNext()) {
            ((StatLine)it.next()).clear();
         }

         this.skillDescriptions4.clear();
      }
   }

   public final void clearBackpackSkillDescriptions() {
      if (this.backpackSkillDescriptions != null && !this.backpackSkillDescriptions.isEmpty()) {
         Array.ArrayIterator it = this.backpackSkillDescriptions.iterator();
         while (it.hasNext()) {
            ((StatLine)it.next()).clear();
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
