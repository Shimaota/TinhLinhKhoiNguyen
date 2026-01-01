package com.tlnt.screen.login.tutorial;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.scene.LoginSceneObject;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.action.QuantityInputDialog;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.character.CharacterWindow;
import com.tlnt.ui.window.character.panel.CharacterBagTab;
import com.tlnt.ui.window.forge.ForgeWindow;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;
import com.tlnt.ui.window.reward.RewardPopup;
import com.tlnt.world.entity.npc.NpcEntity;

public final class TutorialWeaponForgeStep extends LoginSceneObject {

   public TutorialWeaponForgeStep(LoginScreen screen) {
      super(screen);
   }

   @Override
   public final void updateTarget() {
      CharacterWindow charWindow;
      TabbedWindow.TabButton bagTab = (charWindow = super.loginScreen.debugScrollPane.findDialog(CharacterWindow.class))
         .findTabButtonByPanelClass(CharacterBagTab.class);
      CharacterBagTab bagPanel = charWindow.findPanelByClass(CharacterBagTab.class);
      ItemInfoPopup itemPopup = super.loginScreen.debugScrollPane.findDialog(ItemInfoPopup.class);
      ForgeWindow forgeWindow = super.loginScreen.debugScrollPane.findDialog(ForgeWindow.class);
      NpcDialogPopupV2 npcDialog = super.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class);
      QuantityInputDialog quantityDialog = super.loginScreen.debugScrollPane.findDialog(QuantityInputDialog.class);
      if (GameState.inventory.equipmentSlots.get(0) != null) {
         if (charWindow.isVisible()) {
            if (!bagTab.isChecked()) {
               super.targetTable = bagTab;
               super.targetButton = bagTab;
            } else if (itemPopup.isVisible()) {
               super.targetTable = itemPopup;
               super.targetButton = itemPopup.findActionButton(ItemActionRegistry.USE_ITEM_ACTION);
            } else {
               super.targetTable = charWindow;
               super.targetButton = bagPanel.paperdollPanel.getEquipSlot(0);
            }
         } else {
            super.targetTable = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
            super.targetButton = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
         }
      } else {
         if (GameState.inventory.equipmentSlots.get(4) != null) {
            if (charWindow.isVisible()) {
               if (!bagTab.isChecked()) {
                  super.targetTable = bagTab;
                  super.targetButton = bagTab;
               } else if (itemPopup.isVisible()) {
                  super.targetTable = itemPopup;
                  super.targetButton = itemPopup.findActionButton(ItemActionRegistry.USE_ITEM_ACTION);
               } else {
                  super.targetTable = charWindow;
                  super.targetButton = bagPanel.paperdollPanel.getEquipSlot(4);
               }
            } else {
               super.targetTable = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
               super.targetButton = super.loginScreen.debugScrollPane.POOL2.targetPortrait;
            }
         } else if (!charWindow.isVisible()) {
            if (forgeWindow.isVisible()) {
               super.targetTable = forgeWindow;
               if (forgeWindow.OVERLAY_COLOR.getMaterialSlot2().getItem() == null) {
                  if (itemPopup.isVisible()) {
                     if (itemPopup.getCurrentItem().template.type.id == 3) {
                        super.targetTable = itemPopup;
                        super.targetButton = itemPopup.findActionButton(ItemActionRegistry.USE_ITEM_QUANTITY_ACTION);
                        return;
                     }
                  }

                  int[] materialTypeIds = new int[1];
                  materialTypeIds[0] = 3;
                  super.targetButton = forgeWindow.OVERLAY_COLOR.inventoryPanel.findSlotByItemId(materialTypeIds);
               } else if (forgeWindow.OVERLAY_COLOR.getTargetItemSlot().getItem() == null) {
                  if (quantityDialog.isVisible()) {
                     quantityDialog.setInputText("50");
                     super.targetTable = quantityDialog;
                     super.targetButton = quantityDialog.getConfirmButton();
                  } else {
                     if (itemPopup.isVisible()) {
                        if (itemPopup.getCurrentItem().template.type.id == 12) {
                           super.targetTable = itemPopup;
                           super.targetButton = itemPopup.findActionButton(ItemActionRegistry.USE_ITEM_QUANTITY_ACTION);
                           return;
                        }
                     }

                     int[] stoneTypeIds = new int[1];
                     stoneTypeIds[0] = 12;
                     super.targetButton = forgeWindow.OVERLAY_COLOR.inventoryPanel.findSlotByItemId(stoneTypeIds);
                  }
               } else {
                  super.targetButton = forgeWindow.OVERLAY_COLOR.getUpgradeButton();
               }
            } else {
               if (npcDialog.isVisible()) {
                  if (npcDialog.npcId == 2) {
                     super.targetTable = npcDialog;
                     super.targetButton = npcDialog.findChoiceButton("Cường hóa trang bị");
                     if (super.targetButton == null) {
                        super.targetButton = npcDialog.findChoiceButton("Nâng cấp trang bị");
                     }

                     return;
                  }
               }

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
   }

   @Override
   public final boolean isActive() {
      if (super.loginScreen.debugScrollPane.findDialog(RewardPopup.class).isVisible()) {
         return false;
      } else {
         if (GameState.tutorialState.questId == 6
            && GameState.tutorialState.currentStageIndex + 1 == 1) {
            return true;
         } else {
            return false;
         }
      }
   }

}
