package com.tlnt.ui.window.mail.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.DeselectItemAction;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class AttachItemClickListener extends ClickListener {
   public com.tlnt.ui.window.mail.panel.ComposeAttachmentPool composeItemPool;
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemSlot;
   public AttachItemClickListener(ComposeAttachmentPool composeItemPool, ItemSlotButton itemSlot) {
      super();
      this.composeItemPool = composeItemPool;
      this.itemSlot = itemSlot;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton slot = this.itemSlot;
      Item item = this.itemSlot.getItem();
      DeselectItemAction primaryAction = ItemActionRegistry.DESELECT_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
      actions[0] = ItemActionRegistry.DESELECT_ACTION;
      popup.showItemWithActions(slot, item, primaryAction, actions);
   }
}
