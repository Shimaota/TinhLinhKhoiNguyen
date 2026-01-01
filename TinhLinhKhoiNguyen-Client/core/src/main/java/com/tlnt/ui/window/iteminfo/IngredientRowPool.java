package com.tlnt.ui.window.iteminfo;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.window.iteminfo.effect.ItemEffectListRow;

public final class IngredientRowPool extends Pool<ItemEffectListRow> {
   public com.tlnt.ui.window.iteminfo.ItemInfoPopup popup;
   public IngredientRowPool(ItemInfoPopup popup) {
      super();
      this.popup = popup;
   }

   @Override
   public ItemEffectListRow newObject() {
      return new com.tlnt.ui.window.iteminfo.effect.ItemEffectListRow(this.popup.renderContext);
   }
}
