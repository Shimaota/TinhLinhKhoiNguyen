package com.tlnt.ui.window.wardrobe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.DestroyItemAction;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class MaskEquipSlotClickListener extends ClickListener {
   public com.tlnt.ui.window.wardrobe.panel.MaskPanel.EquipGridPanel equipGridPanel;
   public MaskEquipSlotClickListener(MaskPanel.EquipGridPanel panel) {
      super();
      this.equipGridPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton anchor = this.equipGridPanel.equippedItemSlot;
      Item item = this.equipGridPanel.equippedItemSlot.getItem();
      DestroyItemAction defaultAction = ItemActionRegistry.DESTROY_ITEM_ACTION_2;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[2];
      actions[0] = ItemActionRegistry.DESTROY_ITEM_ACTION_2;
      actions[1] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(anchor, item, defaultAction, actions);
   }
}
