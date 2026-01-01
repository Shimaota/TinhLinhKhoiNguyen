package com.tlnt.ui.window.character.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.EquipPetAction;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class PetEquipMenuClickListener extends ClickListener {
   public com.tlnt.ui.window.character.panel.MaskItemButtonPool itemPool;
   public com.tlnt.ui.hud.itemslot.ItemSlotButton itemButton;
   public PetEquipMenuClickListener(MaskItemButtonPool pool, ItemSlotButton button) {
      super();
      this.itemPool = pool;
      this.itemButton = button;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton anchor = this.itemButton;
      Item item = this.itemButton.getItem();
      EquipPetAction defaultAction = ItemActionRegistry.EQUIP_PET_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[3];
      actions[0] = ItemActionRegistry.EQUIP_PET_ACTION;
      actions[1] = ItemActionRegistry.UNEQUIP_PET_ACTION;
      actions[2] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(anchor, item, defaultAction, actions);
   }
}
