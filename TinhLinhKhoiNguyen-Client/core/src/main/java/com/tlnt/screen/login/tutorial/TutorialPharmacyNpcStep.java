package com.tlnt.screen.login.tutorial;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.scene.LoginSceneObject;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.marketsell.MarketSellWindow;
import com.tlnt.ui.window.reward.RewardPopup;
import com.tlnt.world.entity.npc.NpcEntity;

public final class TutorialPharmacyNpcStep extends LoginSceneObject {

   public TutorialPharmacyNpcStep(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void updateTarget() {
      NpcDialogPopupV2 npcDialog;
      if ((npcDialog = super.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class)).isVisible() && npcDialog.npcId == 3) {
         super.targetTable = npcDialog;
         super.targetButton = npcDialog.findChoiceButton("Cửa hàng dược phẩm");
      } else {
         Array.ArrayIterator it = GameState.nearbyNpcs.iterator();

         while (it.hasNext()) {
            NpcEntity npc;
            if ((npc = (NpcEntity)it.next()).POOL2.template.id == 3) {
               super.worldPosition.set(npc.getCenterPosition());
               return;
            }
         }
      }
   }

   @Override
   public final boolean isActive() {
      if (super.loginScreen.debugScrollPane.findDialog(MarketSellWindow.class).isVisible()) {
         return false;
      } else if (super.loginScreen.debugScrollPane.findDialog(RewardPopup.class).isVisible()) {
         return false;
      } else {
         if (GameState.tutorialState.questId == 7) {
            int nextStage = GameState.tutorialState.currentStageIndex + 1;
            if (nextStage == 2) {
               return true;
            }
         }

         return false;
      }
   }

}
