package com.tlnt.screen.login.tutorial;

import com.tlnt.core.GameState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.scene.LoginSceneObject;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.controls.ControlsWindow;
import com.tlnt.ui.window.controls.panel.AutoPlaySettingsPanel;
import com.tlnt.ui.window.reward.RewardPopup;
import com.tlnt.ui.window.settings.SettingsWindow;

public final class TutorialAutoPlaySettingsStep extends LoginSceneObject {
   public TutorialAutoPlaySettingsStep(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void updateTarget() {
      SettingsWindow settingsWindow = super.loginScreen.debugScrollPane.findDialog(SettingsWindow.class);
      ControlsWindow controlsWindow;
      AutoPlaySettingsPanel autoPanel = (controlsWindow = super.loginScreen.debugScrollPane.findDialog(ControlsWindow.class)).findPanelByClass(AutoPlaySettingsPanel.class);
      if (controlsWindow.isVisible()) {
         TabbedWindow.TabButton autoTab;
         if (!(autoTab = controlsWindow.findTabButtonByPanelClass(AutoPlaySettingsPanel.class)).isChecked()) {
            super.targetTable = autoTab;
            super.targetButton = autoTab;
         } else {
            super.targetTable = controlsWindow;
            if (!autoPanel.autoSkillSlot1Checkbox.isChecked()) {
               super.targetButton = autoPanel.autoSkillSlot1Checkbox;
            } else if (!autoPanel.autoSkillSlot2Checkbox.isChecked()) {
               super.targetButton = autoPanel.autoSkillSlot2Checkbox;
            } else if (!autoPanel.autoSkillSlot3Checkbox.isChecked()) {
               super.targetButton = autoPanel.autoSkillSlot3Checkbox;
            } else if (!autoPanel.autoSkillSlot4Checkbox.isChecked()) {
               super.targetButton = autoPanel.autoSkillSlot4Checkbox;
            } else if (!autoPanel.autoSkillSlot5Checkbox.isChecked()) {
               super.targetButton = autoPanel.autoSkillSlot5Checkbox;
            } else if (!autoPanel.autoSkillSlot6Checkbox.isChecked()) {
               super.targetButton = autoPanel.autoSkillSlot6Checkbox;
            } else if (!autoPanel.noAttackPlayersCheckbox.isChecked()) {
               super.targetButton = autoPanel.noAttackPlayersCheckbox;
            } else if (!autoPanel.autoPickupItemsCheckbox.isChecked()) {
               super.targetButton = autoPanel.autoPickupItemsCheckbox;
            } else if (!autoPanel.autoUseStaminaItemCheckbox.isChecked()) {
               super.targetButton = autoPanel.autoUseStaminaItemCheckbox;
            } else if (!autoPanel.autoUseHpItemCheckbox.isChecked()) {
               super.targetButton = autoPanel.autoUseHpItemCheckbox;
            } else if (!autoPanel.autoUseMpItemCheckbox.isChecked()) {
               super.targetButton = autoPanel.autoUseMpItemCheckbox;
            } else if (autoPanel.redAppleHpThresholdStepper.getValue() != 10.0F) {
               if (autoPanel.redAppleHpThresholdStepper.getValue() < 10.0F) {
                  super.targetButton = autoPanel.redAppleHpThresholdStepper.increaseButton;
               } else {
                  super.targetButton = autoPanel.redAppleHpThresholdStepper.decreaseButton;
               }
            } else {
               if (autoPanel.greenAppleMpThresholdStepper.getValue() != 10.0F) {
                  if (autoPanel.greenAppleMpThresholdStepper.getValue() < 10.0F) {
                     super.targetButton = autoPanel.greenAppleMpThresholdStepper.increaseButton;
                     return;
                  }

                  super.targetButton = autoPanel.greenAppleMpThresholdStepper.decreaseButton;
               }
            }
         }
      } else if (settingsWindow.isVisible()) {
         super.targetTable = settingsWindow.OVERLAY_COLOR.settingsButton;
         super.targetButton = settingsWindow.OVERLAY_COLOR.settingsButton;
      } else {
         super.targetTable = super.loginScreen.debugScrollPane.POOL2.menuButtonPanel.getRankButton(2, 3);
         super.targetButton = super.loginScreen
            .debugScrollPane
            .POOL2
            .menuButtonPanel
            .getRankButton(2, 3);
      }
   }

   @Override
   public final boolean isActive() {
      if (super.loginScreen.debugScrollPane.findDialog(RewardPopup.class).isVisible()) {
         return false;
      } else if (super.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class).isVisible()) {
         return false;
      } else {
         if (GameState.tutorialState.questId == 3
            && GameState.tutorialState.currentStageIndex + 1 == 1) {
            return true;
         } else {
            return false;
         }
      }
   }
}
