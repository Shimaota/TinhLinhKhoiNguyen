package com.tlnt.screen.login.tutorial;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.scene.LoginSceneObject;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.chest.ChestWindow;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;
import com.tlnt.ui.window.reward.RewardPopup;
import com.tlnt.world.entity.npc.NpcEntity;

public final class TutorialTreasureChestStep extends LoginSceneObject {
   public TutorialTreasureChestStep(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void updateTarget() {
      ChestWindow chestWindow = super.loginScreen.debugScrollPane.findDialog(ChestWindow.class);
      ItemInfoPopup itemPopup;
      if ((itemPopup = super.loginScreen.debugScrollPane.findDialog(ItemInfoPopup.class)).isVisible()) {
         super.targetTable = itemPopup;
         super.targetButton = itemPopup.findActionButton(ItemActionRegistry.UNEQUIP_ACTION);
      } else if (chestWindow.isVisible()) {
         super.targetTable = chestWindow;
         super.targetButton = chestWindow.OVERLAY_COLOR.itemCountGrid.getItemSlot(0);
      } else {
         if (super.loginScreen.debugScrollPane.isHudActive()) {
            Array.ArrayIterator it = GameState.nearbyNpcs.iterator();

            while (it.hasNext()) {
               NpcEntity npc;
               if ((npc = (NpcEntity)it.next()).POOL2.template.id == 19) {
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
      } else if (super.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class).isVisible()) {
         return false;
      } else {
         if (GameState.tutorialState.questId == 3) {
            int nextStage = GameState.tutorialState.currentStageIndex + 1;
            if (nextStage == 3) {
               return true;
            }
         }

         return false;
      }
   }
}
