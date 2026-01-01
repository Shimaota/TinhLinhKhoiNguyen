package com.tlnt.ui.window.action;

import com.badlogic.gdx.utils.Pool;

public final class NpcDialogChoicePool extends Pool<NpcDialogPopupV2.NpcChoiceButton> {
   public com.tlnt.ui.window.action.NpcDialogPopupV2 ownerDialog;
   public NpcDialogChoicePool(NpcDialogPopupV2 popup) {
      super();
      this.ownerDialog = popup;
   }

   @Override
   public NpcDialogPopupV2.NpcChoiceButton newObject() {
      return (this.ownerDialog).new NpcChoiceButton("", this.ownerDialog.renderContext);
   }
}
