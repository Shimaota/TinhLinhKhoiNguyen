package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class PopupMenuItemClickListener extends ClickListener {
   public com.tlnt.ui.window.action.ItemActionMenuPopup.ActionButton menuItem;
   public com.tlnt.ui.window.action.ItemActionMenuPopup contextMenuPopup;
   public PopupMenuItemClickListener(ItemActionMenuPopup.ActionButton menuButton, ItemActionMenuPopup popup) {
      super();
      this.menuItem = menuButton;
      this.contextMenuPopup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.menuItem.buttonSprite.hide();
      if (this.menuItem.action != null) {
         this.menuItem.action.execute();
      }
   }
}
