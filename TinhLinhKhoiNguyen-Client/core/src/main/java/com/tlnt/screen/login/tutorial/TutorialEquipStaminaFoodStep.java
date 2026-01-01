package com.tlnt.screen.login.tutorial;

import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.scene.LoginSceneObject;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.character.CharacterWindow;
import com.tlnt.ui.window.character.panel.CharacterBagTab;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;
import com.tlnt.ui.window.reward.RewardPopup;

public final class TutorialEquipStaminaFoodStep extends LoginSceneObject {
   public TutorialEquipStaminaFoodStep(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void updateTarget() {
      CharacterWindow charWindow;
      TabbedWindow.TabButton bagTab = (charWindow = super.loginScreen.debugScrollPane.findDialog(CharacterWindow.class))
         .findTabButtonByPanelClass(CharacterBagTab.class);
      CharacterBagTab bagPanel = charWindow.findPanelByClass(CharacterBagTab.class);
      ItemInfoPopup itemPopup = super.loginScreen.debugScrollPane.findDialog(ItemInfoPopup.class);
      if (!charWindow.isVisible()) {
         if (super.loginScreen.debugScrollPane.isHudActive()) {
            super.targetTable = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
            super.targetButton = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
         }
      } else if (!bagTab.isChecked()) {
         super.targetTable = bagTab;
         super.targetButton = bagTab;
      } else if (itemPopup.isVisible()) {
         super.targetTable = itemPopup;
         Item item;
         if ((item = itemPopup.getCurrentItem()) != null) {
            if (item.template.type.id == 3) {
               super.targetButton = itemPopup.findActionButton(ItemActionRegistry.PICKUP_EQUIP_ACTION);
            }
         }
      } else {
         super.targetTable = charWindow;
         int[] itemTypeIds = new int[1];
         itemTypeIds[0] = 3;
         super.targetButton = bagPanel.inventoryGridPanel.findSlotByItemId(itemTypeIds);
      }
   }

   @Override
   public final boolean isActive() {
      if (super.loginScreen.debugScrollPane.findDialog(RewardPopup.class).isVisible()) {
         return false;
      } else if (super.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class).isVisible()) {
         return false;
      } else if (GameState.tutorialState.questId == 3
         && GameState.tutorialState.currentStageIndex + 1 == 4) {
         return true;
      } else {
         return false;
      }
   }
}
