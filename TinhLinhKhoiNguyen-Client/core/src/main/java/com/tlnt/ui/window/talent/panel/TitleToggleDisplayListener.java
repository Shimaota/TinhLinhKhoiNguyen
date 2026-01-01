package com.tlnt.ui.window.talent.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class TitleToggleDisplayListener extends ClickListener {
   public com.tlnt.ui.window.talent.panel.TitlePanel.TitleDetailPanel titleDetailPanel;
   public TitleToggleDisplayListener(TitlePanel.TitleDetailPanel detailPanel) {
      super();
      this.titleDetailPanel = detailPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendToggleAutoPickup(this.titleDetailPanel.autoEquipCheckbox.isChecked());
   }
}
