package com.tlnt.screen.login.tutorial;

import com.tlnt.core.GameState;
import com.tlnt.core.Settings;
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

public final class TutorialEquipHpPotionStep extends LoginSceneObject {
   public TutorialEquipHpPotionStep(LoginScreen screen) {
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
         if (!Settings.assigningHpPotionSlot && !Settings.assigningMpPotionSlot && !Settings.assigningStaminaFoodSlot) {
            super.targetTable = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
            super.targetButton = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
         }
      } else if (!bagTab.isChecked()) {
         super.targetTable = bagTab;
         super.targetButton = bagTab;
      } else if (GameState.tutorialState.currentProgress == 0) {
         if (itemPopup.isVisible()) {
            super.targetTable = itemPopup;
            Item hpItem;
            if ((hpItem = itemPopup.getCurrentItem()) != null && hpItem.template.type.id == 21) {
               super.targetButton = itemPopup.findActionButton(ItemActionRegistry.BIND_SHORTCUT_ACTION);
            }
         } else {
            super.targetTable = charWindow;
            int[] hpTypeIds = new int[1];
            hpTypeIds[0] = 21;
            super.targetButton = bagPanel.inventoryGridPanel.findSlotByItemId(hpTypeIds);
         }
      } else if (itemPopup.isVisible()) {
         super.targetTable = itemPopup;
         Item mpItem;
         if ((mpItem = itemPopup.getCurrentItem()) != null) {
            if (mpItem.template.type.id == 22) {
               super.targetButton = itemPopup.findActionButton(ItemActionRegistry.BIND_SHORTCUT_ACTION);
            }
         }
      } else {
         super.targetTable = charWindow;
         int[] mpTypeIds = new int[1];
         mpTypeIds[0] = 22;
         super.targetButton = bagPanel.inventoryGridPanel.findSlotByItemId(mpTypeIds);
      }
   }

   @Override
   public final boolean isActive() {
      if (super.loginScreen.debugScrollPane.findDialog(RewardPopup.class).isVisible()) {
         return false;
      } else if (super.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class).isVisible()) {
         return false;
      } else if (GameState.tutorialState.questId == 2
         && GameState.tutorialState.currentStageIndex + 1 == 1) {
         return true;
      } else {
         return false;
      }
   }
}
