package com.tlnt.ui.window.action;

import com.badlogic.gdx.utils.Pool;

public final class RegionButtonPool extends Pool<RegionInfoPopup.RegionEntryButton> {
   public com.tlnt.ui.window.action.RegionInfoPopup regionInfoPopup;
   public RegionButtonPool(RegionInfoPopup popup) {
      super();
      this.regionInfoPopup = popup;
   }

   @Override
   public RegionInfoPopup.RegionEntryButton newObject() {
      return (this.regionInfoPopup).new RegionEntryButton(this.regionInfoPopup.renderContext);
   }
}
