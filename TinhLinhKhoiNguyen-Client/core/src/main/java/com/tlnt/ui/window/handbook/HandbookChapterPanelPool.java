package com.tlnt.ui.window.handbook;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.window.handbook.panel.HandbookChapterPanel;

public final class HandbookChapterPanelPool extends Pool<HandbookChapterPanel> {
   public com.tlnt.ui.window.handbook.HandbookWindow handbookWindow;
   public HandbookChapterPanelPool(HandbookWindow window) {
      super();
      this.handbookWindow = window;
   }

   @Override
   public HandbookChapterPanel newObject() {
      return new com.tlnt.ui.window.handbook.panel.HandbookChapterPanel(this.handbookWindow.renderContext);
   }
}
