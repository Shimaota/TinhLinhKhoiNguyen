package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;

public final class ItemDescriptionPopupDismissListener extends InputListener {
   public com.badlogic.gdx.scenes.scene2d.Stage stage;
   public com.tlnt.ui.window.charinfo.InspectEquipmentPanel.ItemDescriptionPanel descriptionPanel;
   public ItemDescriptionPopupDismissListener(InspectEquipmentPanel.ItemDescriptionPanel panel, Stage stage) {
      super();
      this.descriptionPanel = panel;
      this.stage = stage;
   }

   @Override
   public final boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
      if (!this.descriptionPanel.isVisible()) {
         return false;
      } else {
         Actor hitActor;
         if ((hitActor = this.stage.hit(x, y, true)) == null || !hitActor.isDescendantOf(this.descriptionPanel)) {
            this.descriptionPanel.setVisible(false);
         }

         return false;
      }
   }
}
