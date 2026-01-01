package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;

public final class SetBonusOutsideClickListener extends InputListener {
   public com.badlogic.gdx.scenes.scene2d.Stage stage;
   public com.tlnt.ui.window.charinfo.EquipmentPaperdollPanel.ItemDescriptionPanel setBonusPanel;
   public SetBonusOutsideClickListener(EquipmentPaperdollPanel.ItemDescriptionPanel panel, Stage stage) {
      super();
      this.setBonusPanel = panel;
      this.stage = stage;
   }

   @Override
   public final boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
      if (!this.setBonusPanel.isVisible()) {
         return false;
      } else {
         Actor hitActor;
         if ((hitActor = this.stage.hit(x, y, true)) == null || !hitActor.isDescendantOf(this.setBonusPanel)) {
            this.setBonusPanel.setVisible(false);
         }

         return false;
      }
   }
}
