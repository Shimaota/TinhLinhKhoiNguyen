package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class PopupMenuCloseClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.PlayerIconPopupButton weaponSlotButton;
   public com.tlnt.ui.hud.widget.PopupMenu popupMenu;
   public PopupMenuCloseClickListener(PlayerIconPopupButton weaponSlotButton, PopupMenu popupMenu) {
      super();
      this.weaponSlotButton = weaponSlotButton;
      this.popupMenu = popupMenu;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.popupMenu.toggle();
   }
}
