package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class ItemGridCloseClickListener extends ClickListener {
   public com.tlnt.ui.window.charinfo.ItemCountGridPanel.ConfirmButton closeButton;
   public com.tlnt.ui.window.charinfo.ItemCountGridPanel itemGridPanel;
   public ItemGridCloseClickListener(ItemCountGridPanel.ConfirmButton button, ItemCountGridPanel panel) {
      super();
      this.closeButton = button;
      this.itemGridPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendGuildKick();
   }
}
