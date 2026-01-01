package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.utils.Pool;

public final class TribeListEntryRowPool extends Pool<TribeListEntry.TribeInfoRow> {
   public com.tlnt.ui.window.tribe.panel.TribeListEntry tribeListEntry;
   public TribeListEntryRowPool(TribeListEntry tribeListEntry) {
      super();
      this.tribeListEntry = tribeListEntry;
   }

   @Override
   public TribeListEntry.TribeInfoRow newObject() {
      return (this.tribeListEntry).new TribeInfoRow(this.tribeListEntry.renderContext);
   }
}
