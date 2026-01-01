package com.tlnt.ui.window.controls.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.world.entity.player.PlayerEntity;

public final class MobTypeAutoTargetToggleListener extends ClickListener {
   public com.tlnt.ui.window.controls.panel.AutoPlaySettingsPanel panel;
   public com.tlnt.ui.window.controls.panel.AutoPlaySettingsPanel.TargetFilterCheckbox targetFilterCheckbox;
   public MobTypeAutoTargetToggleListener(AutoPlaySettingsPanel.TargetFilterCheckbox checkbox, AutoPlaySettingsPanel panel) {
      super();
      this.targetFilterCheckbox = checkbox;
      this.panel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.targetFilterCheckbox.checkIconTexture != null) {
         if (this.targetFilterCheckbox.isChecked()) {
            PlayerEntity.getInstance().autoTargetController.active.add(this.targetFilterCheckbox.checkIconTexture);
            PlayerEntity.getInstance().autoTargetController.speed = false;
            this.targetFilterCheckbox.buttonSprite.allTargetsCheckbox.setChecked(false);
         } else {
            PlayerEntity.getInstance().autoTargetController.active.remove(this.targetFilterCheckbox.checkIconTexture);
         }
      }
   }
}
