package com.tlnt.ui.window.action;

import com.badlogic.gdx.utils.Pool;

public final class ContextMenuButtonPool extends Pool<ItemActionMenuPopup.ActionButton> {
   public com.tlnt.ui.window.action.ItemActionMenuPopup itemActionMenuPopup;
   public ContextMenuButtonPool(ItemActionMenuPopup popup) {
      super();
      this.itemActionMenuPopup = popup;
   }

   @Override
   public ItemActionMenuPopup.ActionButton newObject() {
      return (this.itemActionMenuPopup).new ActionButton(this.itemActionMenuPopup.renderContext);
   }
}
