package com.tlnt.screen.login.scene;

import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.screen.login.tutorial.TutorialActiveSkillAssignStep;
import com.tlnt.screen.login.tutorial.TutorialAutoPlaySettingsStep;
import com.tlnt.screen.login.tutorial.TutorialBagEquipStep;
import com.tlnt.screen.login.tutorial.TutorialEquipHpPotionStep;
import com.tlnt.screen.login.tutorial.TutorialEquipStaminaFoodStep;
import com.tlnt.screen.login.tutorial.TutorialEquipmentUpgradeStep;
import com.tlnt.screen.login.tutorial.TutorialMpPotionSlotStep;
import com.tlnt.screen.login.tutorial.TutorialPassiveSkillStep;
import com.tlnt.screen.login.tutorial.TutorialPharmacyNpcStep;
import com.tlnt.screen.login.tutorial.TutorialTreasureChestStep;
import com.tlnt.screen.login.tutorial.TutorialUpgradeNpcStep;
import com.tlnt.screen.login.tutorial.TutorialWeaponCraftStep;
import com.tlnt.screen.login.tutorial.TutorialWeaponForgeStep;

public final class LoginSceneController {
   public Array<LoginSceneObject> sceneObjects = new Array<>();
   public LoginSceneObject activeObject;

   public LoginSceneController(LoginScreen screen) {
      this.sceneObjects.add(new TutorialEquipHpPotionStep(screen));
      this.sceneObjects.add(new TutorialActiveSkillAssignStep(screen));
      this.sceneObjects.add(new TutorialAutoPlaySettingsStep(screen));
      this.sceneObjects.add(new TutorialMpPotionSlotStep(screen));
      this.sceneObjects.add(new TutorialUpgradeNpcStep(screen));
      this.sceneObjects.add(new TutorialTreasureChestStep(screen));
      this.sceneObjects.add(new TutorialEquipStaminaFoodStep(screen));
      this.sceneObjects.add(new TutorialPassiveSkillStep(screen));
      this.sceneObjects.add(new TutorialWeaponForgeStep(screen));
      this.sceneObjects.add(new TutorialEquipmentUpgradeStep(screen));
      this.sceneObjects.add(new TutorialPharmacyNpcStep(screen));
      this.sceneObjects.add(new TutorialWeaponCraftStep(screen));
      this.sceneObjects.add(new TutorialBagEquipStep(screen));
   }

   public final void update(float delta) {
      try {
         if (this.activeObject == null) {
            Array.ArrayIterator it = this.sceneObjects.iterator();

            while (it.hasNext()) {
               LoginSceneObject sceneObject;
               if ((sceneObject = (LoginSceneObject)it.next()).isActive()) {
                  this.activeObject = sceneObject;
                  break;
               }
            }
         }

         if (this.activeObject != null) {
            this.activeObject.clearTarget();
            this.activeObject.updateAnimation(delta);
            if (!this.activeObject.isActive()) {
               this.activeObject = null;
            }
         }
      } catch (Exception e) {
      }
   }

   public final void render(RenderContext ctx) {
      if (this.activeObject != null) {
         this.activeObject.render(ctx);
      }
   }
}
