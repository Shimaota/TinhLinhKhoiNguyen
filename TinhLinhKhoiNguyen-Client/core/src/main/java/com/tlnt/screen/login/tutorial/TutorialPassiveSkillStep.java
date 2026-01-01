package com.tlnt.screen.login.tutorial;

import com.tlnt.core.GameState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.scene.LoginSceneObject;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.action.QuantityInputDialog;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.character.CharacterWindow;
import com.tlnt.ui.window.reward.RewardPopup;
import com.tlnt.ui.window.talent.panel.TalentPanel;

public final class TutorialPassiveSkillStep extends LoginSceneObject {

   public TutorialPassiveSkillStep(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void updateTarget() {
      CharacterWindow charWindow;
      TabbedWindow.TabButton talentTab = (charWindow = super.loginScreen.debugScrollPane.findDialog(CharacterWindow.class)).findTabButtonByPanelClass(TalentPanel.class);
      TalentPanel talentPanel;
      TalentPanel panel = talentPanel = charWindow.findPanelByClass(TalentPanel.class);
      TalentPanel.TalentTypeButton passiveNode = panel.getPassiveSkillNode(5);
      QuantityInputDialog quantityDialog;
      if ((quantityDialog = super.loginScreen.debugScrollPane.findDialog(QuantityInputDialog.class)).isVisible()) {
         quantityDialog.setInputText("1");
         super.targetTable = quantityDialog;
         super.targetButton = quantityDialog.getConfirmButton();
      } else if (!charWindow.isVisible()) {
         super.targetTable = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
         super.targetButton = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
      } else if (!talentTab.isChecked()) {
         super.targetTable = talentTab;
         super.targetButton = talentTab;
      } else if (!passiveNode.isChecked()) {
         super.targetTable = charWindow;
         super.targetButton = talentPanel.getPassiveSkillNode(5);
      } else {
         super.targetTable = charWindow;
         super.targetButton = talentPanel.getEnterPointsButton();
      }
   }

   @Override
   public final boolean isActive() {
      if (super.loginScreen.debugScrollPane.findDialog(RewardPopup.class).isVisible()) {
         return false;
      } else if (super.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class).isVisible()) {
         return false;
      } else {
         if (GameState.tutorialState.questId == 3) {
            int nextStage = GameState.tutorialState.currentStageIndex + 1;
            if (nextStage == 5) {
               return true;
            }
         }

         return false;
      }
   }

}
