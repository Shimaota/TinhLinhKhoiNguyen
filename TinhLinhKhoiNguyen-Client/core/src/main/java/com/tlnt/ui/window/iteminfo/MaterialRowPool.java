package com.tlnt.ui.window.iteminfo;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.window.iteminfo.effect.ItemSingleEffectRow;

public final class MaterialRowPool extends Pool<ItemSingleEffectRow> {
   public com.tlnt.ui.window.iteminfo.ItemInfoPopup popup;
   public MaterialRowPool(ItemInfoPopup popup) {
      super();
      this.popup = popup;
   }

   @Override
   public ItemSingleEffectRow newObject() {
      return new com.tlnt.ui.window.iteminfo.effect.ItemSingleEffectRow(this.popup.renderContext);
   }
}
