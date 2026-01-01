package com.tlnt.engine.input;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.tlnt.core.GameMain;
import com.tlnt.core.Settings;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.hud.widget.HudSkillButton1;
import com.tlnt.ui.hud.widget.HudSkillButton3;
import com.tlnt.ui.hud.widget.MpPotionSlotButton;
import com.tlnt.ui.window.WindowType;

public final class ShortcutAssignInputHandler extends InputAdapter {
   @Override
   public final boolean touchDown(int screenX, int screenY, int pointer, int button) {
      if (Settings.assigningStaminaFoodSlot) {
         Stage stage = GameMain.instance.getLoginScreen().getStage();
         Vector2 stageCoords = new Vector2((float)screenX, (float)screenY);
         stage.screenToStageCoordinates(stageCoords);
         Actor hitActor;
         if ((hitActor = stage.hit(stageCoords.x, stageCoords.y, true)) instanceof HudSkillButton1) {
            HudSkillButton1 skillButton;
            (skillButton = (HudSkillButton1)hitActor).activateSkill();
         }

         Settings.assigningStaminaFoodSlot = false;
         GameMain.instance.getLoginScreen().debugScrollPane.showWindow(WindowType.GENERIC_POPUP);
         return true;
      } else if (Settings.assigningHpPotionSlot) {
         Stage hpStage = GameMain.instance.getLoginScreen().getStage();
         Vector2 hpStageCoords = new Vector2((float)screenX, (float)screenY);
         hpStage.screenToStageCoordinates(hpStageCoords);
         Actor hpHitActor;
         if ((hpHitActor = hpStage.hit(hpStageCoords.x, hpStageCoords.y, true)) instanceof HudSkillButton3) {
            HudSkillButton3.swapSkillFromInventory();
         }

         Settings.assigningHpPotionSlot = false;
         GameMain.instance.getLoginScreen().debugScrollPane.showWindow(WindowType.GENERIC_POPUP);
         return true;
      } else if (Settings.assigningMpPotionSlot) {
         Stage mpStage = GameMain.instance.getLoginScreen().getStage();
         Vector2 mpStageCoords = new Vector2((float)screenX, (float)screenY);
         mpStage.screenToStageCoordinates(mpStageCoords);
         Actor mpHitActor;
         if ((mpHitActor = mpStage.hit(mpStageCoords.x, mpStageCoords.y, true)) instanceof MpPotionSlotButton) {
            MpPotionSlotButton.swapItemToSlot();
         }

         Settings.assigningMpPotionSlot = false;
         GameMain.instance.getLoginScreen().debugScrollPane.showWindow(WindowType.GENERIC_POPUP);
         return true;
      } else {
         return super.touchDown(screenX, screenY, pointer, button);
      }
   }

   @Override
   public final boolean keyDown(int keycode) {
      if (Settings.isCapturingKeybind) {
         Settings.keybindings[Settings.pendingKeybindIndex] = keycode;
         Settings.isCapturingKeybind = false;
         ServerPacketSender.getInstance().sendGameSettings();
         return true;
      } else {
         return super.keyDown(keycode);
      }
   }
}
