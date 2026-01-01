package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class InspectPanelToggleClickListener extends ClickListener {
   public com.tlnt.ui.window.charinfo.EquipmentPaperdollPanel.PortraitButton paperdollPanelInner;
   public com.tlnt.ui.window.charinfo.EquipmentPaperdollPanel equipmentPaperdollPanel;
   public InspectPanelToggleClickListener(EquipmentPaperdollPanel.PortraitButton button, EquipmentPaperdollPanel panel) {
      super();
      this.paperdollPanelInner = button;
      this.equipmentPaperdollPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.paperdollPanelInner.outerPanel.equipInfoOverlay.setVisible(true);
   }
}
