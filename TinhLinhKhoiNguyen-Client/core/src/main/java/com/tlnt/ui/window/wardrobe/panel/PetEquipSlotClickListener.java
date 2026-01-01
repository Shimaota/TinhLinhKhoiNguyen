package com.tlnt.ui.window.wardrobe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.GuildDepositAllGoldAction;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class PetEquipSlotClickListener extends ClickListener {
   public com.tlnt.ui.window.wardrobe.panel.PetPanel.EquipGridPanel equipGridPanel;
   public PetEquipSlotClickListener(PetPanel.EquipGridPanel panel) {
      super();
      this.equipGridPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton anchor = this.equipGridPanel.equippedItemSlot;
      Item item = this.equipGridPanel.equippedItemSlot.getItem();
      GuildDepositAllGoldAction defaultAction = ItemActionRegistry.WITHDRAW_GUILD_GOLD_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[2];
      actions[0] = ItemActionRegistry.WITHDRAW_GUILD_GOLD_ACTION;
      actions[1] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(anchor, item, defaultAction, actions);
   }
}
