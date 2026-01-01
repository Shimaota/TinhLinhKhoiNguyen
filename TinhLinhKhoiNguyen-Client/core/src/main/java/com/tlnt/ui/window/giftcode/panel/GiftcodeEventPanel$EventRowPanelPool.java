package com.tlnt.ui.window.giftcode.panel;

import com.badlogic.gdx.utils.Pool;

public final class GiftcodeEventPanel$EventRowPanelPool extends Pool<GiftcodeEventPanel.EventRowPanel> {
   public GiftcodeEventPanel panel;

   public GiftcodeEventPanel$EventRowPanelPool(GiftcodeEventPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public GiftcodeEventPanel.EventRowPanel newObject() {
      return (this.panel).new EventRowPanel(this.panel.renderContext);
   }
}
