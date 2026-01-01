package com.tlnt.screen.login.tutorial;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.scene.LoginSceneObject;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.action.QuantityInputDialog;
import com.tlnt.ui.window.forge.ForgeWindow;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;
import com.tlnt.ui.window.reward.RewardPopup;
import com.tlnt.world.entity.npc.NpcEntity;

public final class TutorialEquipmentUpgradeStep extends LoginSceneObject {

   public TutorialEquipmentUpgradeStep(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void updateTarget() {
      ItemInfoPopup itemPopup = super.loginScreen.debugScrollPane.findDialog(ItemInfoPopup.class);
      ForgeWindow forgeWindow = super.loginScreen.debugScrollPane.findDialog(ForgeWindow.class);
      NpcDialogPopupV2 npcDialog = super.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class);
      QuantityInputDialog quantityDialog = super.loginScreen.debugScrollPane.findDialog(QuantityInputDialog.class);
      if (forgeWindow.isVisible()) {
         super.targetTable = forgeWindow;
         if (forgeWindow.OVERLAY_COLOR.getMaterialSlot2().getItem() != null
            && forgeWindow.OVERLAY_COLOR.getMaterialSlot2().getItem().template.type.id == 7) {
            if (forgeWindow.OVERLAY_COLOR.getTargetItemSlot().getItem() != null) {
               super.targetButton = forgeWindow.OVERLAY_COLOR.getUpgradeButton();
            } else if (quantityDialog.isVisible()) {
               quantityDialog.setInputText("50");
               super.targetTable = quantityDialog;
               super.targetButton = quantityDialog.getConfirmButton();
            } else if (itemPopup.isVisible() && itemPopup.getCurrentItem().template.type.id == 12) {
               super.targetTable = itemPopup;
               super.targetButton = itemPopup.findActionButton(ItemActionRegistry.USE_ITEM_QUANTITY_ACTION);
            } else {
               int[] stoneTypeIds = new int[1];
               stoneTypeIds[0] = 12;
               super.targetButton = forgeWindow.OVERLAY_COLOR.inventoryPanel.findSlotByItemId(stoneTypeIds);
            }
         } else {
            if (itemPopup.isVisible()) {
               if (itemPopup.getCurrentItem().template.type.id == 7) {
                  super.targetTable = itemPopup;
                  super.targetButton = itemPopup.findActionButton(ItemActionRegistry.USE_ITEM_QUANTITY_ACTION);
                  return;
               }
            }

            int[] bagTypeIds = new int[1];
            bagTypeIds[0] = 7;
            super.targetButton = forgeWindow.OVERLAY_COLOR.inventoryPanel.findSlotByItemId(bagTypeIds);
         }
      } else if (npcDialog.isVisible() && npcDialog.npcId == 2) {
         super.targetTable = npcDialog;
         super.targetButton = npcDialog.findChoiceButton("Cường hóa trang bị");
         if (super.targetButton == null) {
            super.targetButton = npcDialog.findChoiceButton("Nâng cấp trang bị");
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

   @Override
   public final boolean isActive() {
      if (super.loginScreen.debugScrollPane.findDialog(RewardPopup.class).isVisible()) {
         return false;
      } else {
         if (GameState.tutorialState.questId == 6
            && GameState.tutorialState.currentStageIndex + 1 == 2) {
            return true;
         } else {
            return false;
         }
      }
   }

}
