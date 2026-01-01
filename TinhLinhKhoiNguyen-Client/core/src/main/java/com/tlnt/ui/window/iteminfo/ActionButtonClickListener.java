package com.tlnt.ui.window.iteminfo;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class ActionButtonClickListener extends ClickListener {
   public ItemInfoPopup.ActionButton button;
   public com.tlnt.ui.window.iteminfo.ItemInfoPopup popup;
   public ActionButtonClickListener(ItemInfoPopup.ActionButton button, ItemInfoPopup popup) {
      super();
      this.button = button;
      this.popup = popup;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.button.actionConfig != null && this.button.popup.itemSlot.getItem() != null) {
         this.button.actionConfig.onItemAction(this.button.popup.itemSlot.getItem());
      }

      this.button.popup.hide();
   }
}
