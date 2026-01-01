package com.tlnt.screen.login.tutorial;

import com.tlnt.core.GameState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.scene.LoginSceneObject;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.reward.RewardPopup;

public final class TutorialMpPotionSlotStep extends LoginSceneObject {
   public TutorialMpPotionSlotStep(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void updateTarget() {
      if (super.loginScreen.debugScrollPane.POOL2.isVisible()) {
         if (super.loginScreen.debugScrollPane.POOL2.mpPotionSlotButton.getItem() != null) {
            super.targetButton = super.loginScreen.debugScrollPane.POOL2.mpPotionSlotButton;
            super.targetTable = super.loginScreen.debugScrollPane.POOL2.mpPotionSlotButton;
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
         if (GameState.tutorialState.questId == 2) {
            int nextStage = GameState.tutorialState.currentStageIndex + 1;
            if (nextStage == 3) {
               return true;
            }
         }

         return false;
      }
   }
}
