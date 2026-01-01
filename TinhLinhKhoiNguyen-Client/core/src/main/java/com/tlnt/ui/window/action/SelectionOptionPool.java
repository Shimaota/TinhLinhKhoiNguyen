package com.tlnt.ui.window.action;

import com.badlogic.gdx.utils.Pool;

public final class SelectionOptionPool extends Pool<NpcSelectionPopup.OptionButton> {
   public com.tlnt.ui.window.action.NpcSelectionPopup parent;
   public SelectionOptionPool(NpcSelectionPopup popup) {
      super();
      this.parent = popup;
   }

   @Override
   public NpcSelectionPopup.OptionButton newObject() {
      return (this.parent).new OptionButton(this.parent.renderContext);
   }
}
