package com.tlnt.ui.window.character.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.GuildDepositAllGoldAction;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class PetItemUnequipClickListener extends ClickListener {
   public com.tlnt.ui.window.character.panel.PetPanel.PetEquipPanel petEquipPanel;
   public PetItemUnequipClickListener(PetPanel.PetEquipPanel panel) {
      super();
      this.petEquipPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton anchor = this.petEquipPanel.selectedItemButton;
      Item item = this.petEquipPanel.selectedItemButton.getItem();
      GuildDepositAllGoldAction defaultAction = ItemActionRegistry.WITHDRAW_GUILD_GOLD_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[2];
      actions[0] = ItemActionRegistry.WITHDRAW_GUILD_GOLD_ACTION;
      actions[1] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(anchor, item, defaultAction, actions);
   }
}
