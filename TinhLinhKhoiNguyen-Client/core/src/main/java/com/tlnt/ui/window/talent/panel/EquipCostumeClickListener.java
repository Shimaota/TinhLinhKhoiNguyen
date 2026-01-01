package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.DiscardItemCancelAction;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class EquipCostumeClickListener extends ClickListener {
   public com.tlnt.ui.window.talent.panel.CostumePanel.CostumeListPanel costumeListPanel;
   public EquipCostumeClickListener(CostumePanel.CostumeListPanel listPanel) {
      super();
      this.costumeListPanel = listPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton slotButton = this.costumeListPanel.selectedCostumeItem;
      Item item = this.costumeListPanel.selectedCostumeItem.getItem();
      DiscardItemCancelAction defaultAction = ItemActionRegistry.DISCARD_CANCEL_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[2];
      actions[0] = ItemActionRegistry.DISCARD_CANCEL_ACTION;
      actions[1] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(slotButton, item, defaultAction, actions);
   }
}
