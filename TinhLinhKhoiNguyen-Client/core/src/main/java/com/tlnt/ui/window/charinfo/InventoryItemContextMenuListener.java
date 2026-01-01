package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.charinfo.InventorySlotButtonPool;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class InventoryItemContextMenuListener extends ClickListener {
   public com.tlnt.ui.window.charinfo.InventorySlotButtonPool slotButtonPool;
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemSlotButton;
   public InventoryItemContextMenuListener(InventorySlotButtonPool pool, ItemSlotButton button) {
      super();
      this.slotButtonPool = pool;
      this.itemSlotButton = button;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
       com.tlnt.model.item.Item item = this.itemSlotButton.getItem();
       if (item == null) {
           return;
       }
       switch (this.slotButtonPool.ownerPanel.inventoryType) {
           case InventoryGridPanel.EquipSlotType.SLOT_TYPE_1: {
               ItemInfoPopup.ActionConfig actionConfig = item.template.type.equippable ? (item.template.type.id == 19 ? ItemActionRegistry.DESTROY_ITEM_ACTION : (item.template.type.id == 20 ? ItemActionRegistry.EQUIP_SECOND_SLOT_ACTION : (item.template.type.id == 36 ? ItemActionRegistry.COMPOSITE_ITEM_ACTION : (item.template.type.id == 37 ? ItemActionRegistry.EQUIP_COSTUME_ACTION : (item.template.type.id == 48 ? ItemActionRegistry.EQUIP_BACKWEAR_ACTION : ItemActionRegistry.PICKUP_EQUIP_ACTION))))) : ItemActionRegistry.SPLIT_STACK_ACTION;
               Array<ItemInfoPopup.ActionConfig> array = new Array<ItemInfoPopup.ActionConfig>();
               array.add(actionConfig);
               if (this.itemSlotButton.getItem().quantity > 1) {
                   array.add(ItemActionRegistry.CRAFT_ITEM_ACTION);
               }
               array.add(ItemActionRegistry.BIND_SHORTCUT_ACTION);
               if (!item.locked) {
                   array.add(ItemActionRegistry.LOCK_ACTION);
               }
               array.add(ItemActionRegistry.SELL_ACTION);
               ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[array.size];
               for (int i = 0; i < actions.length; ++i) {
                   actions[i] = (ItemInfoPopup.ActionConfig)array.get(i);
               }
               GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class).showItemWithActions(this.itemSlotButton, this.itemSlotButton.getItem(), actionConfig, actions);
               return;
           }
           case InventoryGridPanel.EquipSlotType.SLOT_TYPE_2: {
               ItemInfoPopup.ActionConfig[] actionConfigArray = new ItemInfoPopup.ActionConfig[1];
               actionConfigArray[0] = ItemActionRegistry.DROP_ITEM_HANDLER;
               GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class).showItemWithActions(this.itemSlotButton, this.itemSlotButton.getItem(), ItemActionRegistry.DROP_ITEM_HANDLER, actionConfigArray);
               return;
           }
           case InventoryGridPanel.EquipSlotType.SLOT_TYPE_3: {
               ItemInfoPopup.ActionConfig[] actionConfigArray = new ItemInfoPopup.ActionConfig[1];
               actionConfigArray[0] = ItemActionRegistry.ADD_MAIL_ATTACHMENT_ACTION;
               GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class).showItemWithActions(this.itemSlotButton, this.itemSlotButton.getItem(), ItemActionRegistry.ADD_MAIL_ATTACHMENT_ACTION, actionConfigArray);
               return;
           }
           case InventoryGridPanel.EquipSlotType.SLOT_TYPE_5: {
               ItemInfoPopup.ActionConfig[] actionConfigArray = new ItemInfoPopup.ActionConfig[1];
               actionConfigArray[0] = ItemActionRegistry.USE_ITEM_QUANTITY_ACTION;
               GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class).showItemWithActions(this.itemSlotButton, this.itemSlotButton.getItem(), ItemActionRegistry.USE_ITEM_QUANTITY_ACTION, actionConfigArray);
               return;
           }
           case InventoryGridPanel.EquipSlotType.SLOT_TYPE_4: {
               ItemInfoPopup.ActionConfig[] actionConfigArray = new ItemInfoPopup.ActionConfig[1];
               actionConfigArray[0] = ItemActionRegistry.USE_ITEM_HANDLER;
               GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class).showItemWithActions(this.itemSlotButton, this.itemSlotButton.getItem(), ItemActionRegistry.USE_ITEM_HANDLER, actionConfigArray);
               return;
           }
           case InventoryGridPanel.EquipSlotType.SLOT_TYPE_6: {
               ItemInfoPopup.ActionConfig[] actionConfigArray = new ItemInfoPopup.ActionConfig[1];
               actionConfigArray[0] = ItemActionRegistry.ADD_TRADE_ITEM_ACTION;
               GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class).showItemWithActions(this.itemSlotButton, this.itemSlotButton.getItem(), ItemActionRegistry.ADD_TRADE_ITEM_ACTION, actionConfigArray);
           }
       }
   }
}
