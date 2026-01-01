package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.DetachSpiritualStoneAction;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class EquipTitleClickListener extends ClickListener {
   public com.tlnt.ui.window.talent.panel.TitlePanel.TitleListPanel titleListPanel;
   public EquipTitleClickListener(TitlePanel.TitleListPanel listPanel) {
      super();
      this.titleListPanel = listPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton slotButton = this.titleListPanel.equippedTitleButton;
      Item item = this.titleListPanel.equippedTitleButton.getItem();
      DetachSpiritualStoneAction defaultAction = ItemActionRegistry.DETACH_SPIRITUAL_STONE_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[2];
      actions[0] = ItemActionRegistry.DETACH_SPIRITUAL_STONE_ACTION;
      actions[1] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(slotButton, item, defaultAction, actions);
   }
}
