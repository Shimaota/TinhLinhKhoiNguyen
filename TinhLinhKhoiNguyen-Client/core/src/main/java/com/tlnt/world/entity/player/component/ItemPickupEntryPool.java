package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.utils.Pool;

public final class ItemPickupEntryPool extends Pool<ItemPickupDisplay.PickupEntry> {
   public com.tlnt.world.entity.player.component.ItemPickupDisplay itemPickupDisplay;
   public ItemPickupEntryPool(ItemPickupDisplay itemPickupDisplay) {
      super();
      this.itemPickupDisplay = itemPickupDisplay;
   }

   @Override
   public ItemPickupDisplay.PickupEntry newObject() {
      return (this.itemPickupDisplay).new PickupEntry();
   }
}
