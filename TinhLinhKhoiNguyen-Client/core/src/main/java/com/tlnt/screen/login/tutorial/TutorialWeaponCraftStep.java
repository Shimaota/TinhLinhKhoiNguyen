package com.tlnt.screen.login.tutorial;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.scene.LoginSceneObject;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.marketsell.MarketSellWindow;
import com.tlnt.ui.window.reward.RewardPopup;
import com.tlnt.ui.window.weaponcraft.WeaponCraftWindow;
import com.tlnt.world.entity.npc.NpcEntity;

public final class TutorialWeaponCraftStep extends LoginSceneObject {

   public TutorialWeaponCraftStep(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void updateTarget() {
      WeaponCraftWindow craftWindow = super.loginScreen.debugScrollPane.findDialog(WeaponCraftWindow.class);
      NpcDialogPopupV2 npcDialog = super.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class);
      if (!craftWindow.isVisible()) {
         if (npcDialog.isVisible() && npcDialog.npcId == 2) {
            super.targetTable = npcDialog;
            super.targetButton = npcDialog.findChoiceButton("Chế tác");
            if (super.targetButton == null) {
               super.targetButton = npcDialog.findChoiceButton("Rèn vũ khí");
            }
         } else {
            Array.ArrayIterator it = GameState.nearbyNpcs.iterator();

            while (it.hasNext()) {
               NpcEntity npc;
               if ((npc = (NpcEntity)it.next()).POOL2.template.id == 2) {
                  super.worldPosition.set(npc.getCenterPosition());
                  return;
               }
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
         if (GameState.tutorialState.questId == 5) {
            if (GameState.tutorialState.currentStageIndex + 1 == 4) {
               return true;
            }
         }

         return false;
      }
   }

}
