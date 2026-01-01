package com.tlnt.ui.window.action;

import com.tlnt.core.GameMain;
import com.tlnt.core.Settings;
import com.tlnt.model.item.Item;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class BindShortcutAction extends ItemInfoPopup.ActionConfig {
   public BindShortcutAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (item != null) {
         switch (item.template.type.id) {
            case 21:
            case 24:
               Settings.assigningHpPotionSlot = true;
               break;
            case 22:
            case 25:
               Settings.assigningMpPotionSlot = true;
               break;
            case 23:
            default:
               Settings.assigningStaminaFoodSlot = true;
         }

         Settings.pendingQuickSlotItemId = item.slotIndex;
         GameMain.instance.getLoginScreen().debugScrollPane.closeAllDialogs();
      }
   }
}
