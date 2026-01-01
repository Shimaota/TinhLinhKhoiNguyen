package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class InventorySortButtonClickListener extends ClickListener {
   public InventoryGridPanel.SortButton button;
   public com.tlnt.ui.window.charinfo.InventoryGridPanel inventoryGridPanel;
   public InventorySortButtonClickListener(InventoryGridPanel.SortButton button, InventoryGridPanel panel) {
      super();
      this.button = button;
      this.inventoryGridPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendGuildLeave();
   }
}
