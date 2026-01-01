package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class PopupMenuItemClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.PopupMenu popupMenu;
   public com.tlnt.ui.hud.widget.PopupMenu.PopupActionButton menuItemButton;
   public PopupMenuItemClickListener(PopupMenu.PopupActionButton menuItemButton, PopupMenu popupMenu) {
      super();
      this.menuItemButton = menuItemButton;
      this.popupMenu = popupMenu;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendGuildInvite(this.menuItemButton.actionType.ordinal());
      this.menuItemButton.popupMenu.toggle();
   }
}
