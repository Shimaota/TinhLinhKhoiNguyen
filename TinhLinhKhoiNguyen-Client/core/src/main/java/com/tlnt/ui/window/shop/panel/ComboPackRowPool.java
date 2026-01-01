package com.tlnt.ui.window.shop.panel;

import com.badlogic.gdx.utils.Pool;

public final class ComboPackRowPool extends Pool<ComboPackPanel.ComboPackRow> {
   public com.tlnt.ui.window.shop.panel.ComboPackPanel comboPackPanel;
   public ComboPackRowPool(ComboPackPanel panel) {
      super();
      this.comboPackPanel = panel;
   }

   @Override
   public ComboPackPanel.ComboPackRow newObject() {
      return (this.comboPackPanel).new ComboPackRow(this.comboPackPanel.renderContext);
   }
}
