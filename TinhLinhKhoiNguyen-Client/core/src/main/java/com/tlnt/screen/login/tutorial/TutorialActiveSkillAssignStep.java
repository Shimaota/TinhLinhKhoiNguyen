package com.tlnt.screen.login.tutorial;

import com.tlnt.core.GameState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.scene.LoginSceneObject;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.character.CharacterWindow;
import com.tlnt.ui.window.reward.RewardPopup;
import com.tlnt.ui.window.skillslot.SkillSlotPopup;
import com.tlnt.ui.window.talent.panel.TalentPanel;

public final class TutorialActiveSkillAssignStep extends LoginSceneObject {
   public TutorialActiveSkillAssignStep(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void updateTarget() {
      CharacterWindow charWindow = super.loginScreen.debugScrollPane.findDialog(CharacterWindow.class);
      SkillSlotPopup slotPopup = super.loginScreen.debugScrollPane.findDialog(SkillSlotPopup.class);
      TabbedWindow.TabButton talentTab = charWindow.findTabButtonByPanelClass(TalentPanel.class);
      TalentPanel talentPanel = charWindow.findPanelByClass(TalentPanel.class);
      if (slotPopup.isVisible()) {
         super.targetTable = slotPopup;
         super.targetButton = slotPopup.getSkillSlotButton(0);
      } else if (!charWindow.isVisible()) {
         super.targetTable = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
         super.targetButton = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
      } else if (!talentTab.isChecked()) {
         super.targetTable = talentTab;
         super.targetButton = talentTab;
      } else {
         TalentPanel.SkillButton skillNode;
         if (!(skillNode = talentPanel.getActiveSkillNode(1)).isChecked()) {
            super.targetTable = charWindow;
            super.targetButton = skillNode;
         } else {
            super.targetTable = charWindow;
            super.targetButton = talentPanel.getAssignSlotButton();
         }
      }
   }

   @Override
   public final boolean isActive() {
      if (super.loginScreen.debugScrollPane.findDialog(RewardPopup.class).isVisible()) {
         return false;
      } else if (super.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class).isVisible()) {
         return false;
      } else {
         return ((GameState.tutorialState.questId == 5
               && GameState.tutorialState.currentStageIndex + 1 == 6
            ? 1
            : 0) != 0);
      }
   }
}
