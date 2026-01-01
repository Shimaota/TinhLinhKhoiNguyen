package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class InspectPortraitClickListener extends ClickListener {
   public com.tlnt.ui.window.charinfo.InspectEquipmentPanel inspectEquipmentPanel;
   public com.tlnt.ui.window.charinfo.InspectEquipmentPanel.PortraitButton portraitButton;
   public InspectPortraitClickListener(InspectEquipmentPanel.PortraitButton button, InspectEquipmentPanel panel) {
      super();
      this.portraitButton = button;
      this.inspectEquipmentPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.portraitButton.inspectEquipmentPanel.equipInfoOverlay.setVisible(true);
   }
}
