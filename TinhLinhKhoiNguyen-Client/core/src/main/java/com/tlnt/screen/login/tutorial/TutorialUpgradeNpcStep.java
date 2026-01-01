package com.tlnt.screen.login.tutorial;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.scene.LoginSceneObject;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.reward.RewardPopup;
import com.tlnt.world.entity.npc.NpcEntity;

public final class TutorialUpgradeNpcStep extends LoginSceneObject {

   public TutorialUpgradeNpcStep(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void updateTarget() {
      NpcDialogPopupV2 npcDialog;
      if ((npcDialog = super.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class)).isVisible()) {
         if (npcDialog.npcId == 18) {
            super.targetTable = npcDialog;
            super.targetButton = npcDialog.findChoiceButton("Đồng ý");
            if (super.targetButton == null) {
               super.targetButton = npcDialog.findChoiceButton("Nâng cấp");
            }
         }
      } else {
         if (super.loginScreen.debugScrollPane.isHudActive()) {
            Array.ArrayIterator it = GameState.nearbyNpcs.iterator();

            while (it.hasNext()) {
               NpcEntity npc;
               if ((npc = (NpcEntity)it.next()).POOL2.template.id == 18) {
                  super.worldPosition.set(npc.getCenterPosition());
                  return;
               }
            }
         }
      }
   }

   @Override
   public final boolean isActive() {
      if (super.loginScreen.debugScrollPane.findDialog(RewardPopup.class).isVisible()) {
         return false;
      } else {
         if (GameState.tutorialState.questId == 4) {
            if (GameState.tutorialState.currentStageIndex + 1 == 2) {
               return true;
            }
         }

         return false;
      }
   }

}
