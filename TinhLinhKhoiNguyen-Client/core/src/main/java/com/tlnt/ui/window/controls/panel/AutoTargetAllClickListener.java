package com.tlnt.ui.window.controls.panel;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.tlnt.world.entity.player.PlayerEntity;

public final class AutoTargetAllClickListener extends ClickListener {
   public com.tlnt.ui.window.controls.panel.AutoPlaySettingsPanel panel;
   public AutoTargetAllClickListener(AutoPlaySettingsPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      PlayerEntity.getInstance().autoTargetController.speed = this.panel.allTargetsCheckbox.isChecked();
      if (PlayerEntity.getInstance().autoTargetController.speed) {
         Array.ArrayIterator childIter = this.panel.targetFilterTable.getChildren().iterator();

         while (childIter.hasNext()) {
            Actor child;
            if ((child = (Actor)childIter.next()) instanceof AutoPlaySettingsPanel.TargetFilterCheckbox) {
               ((AutoPlaySettingsPanel.TargetFilterCheckbox)child).setChecked(false);
            }
         }
      }
   }
}
