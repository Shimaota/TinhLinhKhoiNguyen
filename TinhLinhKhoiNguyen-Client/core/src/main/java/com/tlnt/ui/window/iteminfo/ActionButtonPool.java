package com.tlnt.ui.window.iteminfo;

import com.badlogic.gdx.utils.Pool;

public final class ActionButtonPool extends Pool<ItemInfoPopup.ActionButton> {
   public com.tlnt.ui.window.iteminfo.ItemInfoPopup popup;
   public ActionButtonPool(ItemInfoPopup popup) {
      super();
      this.popup = popup;
   }

   @Override
   public ItemInfoPopup.ActionButton newObject() {
      return (this.popup).new ActionButton(this.popup.renderContext);
   }
}
