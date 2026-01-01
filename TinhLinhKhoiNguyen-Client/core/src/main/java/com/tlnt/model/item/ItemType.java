package com.tlnt.model.item;

import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;

public final class ItemType {
   public static ObjectMap<Integer, ItemType> typesById = new ObjectMap<>();
   public static ObjectMap<Integer, ItemType> typesByEquipSlot = new OrderedMap<>();
   public int id;
   public String name;
   public int equipSlot;
   public boolean equippable;

   public static ItemType getById(int id) {
      return typesById.get(id);
   }

   public static ItemType getByEquipSlot(int equipSlot) {
      return typesByEquipSlot.get(equipSlot);
   }

   public static void register(ItemType itemType) {
      typesById.put(itemType.id, itemType);
      typesByEquipSlot.put(itemType.equipSlot, itemType);
   }

   public ItemType(int id, String name, int equipSlot, boolean equippable) {
      this.id = id;
      this.name = name;
      this.equipSlot = equipSlot;
      this.equippable = equippable;
   }
}
