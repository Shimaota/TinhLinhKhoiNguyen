package com.tlnt.ui.window.handbook.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class HandbookPageButtonClickListener extends ClickListener {
   public com.tlnt.ui.window.handbook.panel.HandbookChapterPanel.PageListPanel pageListPanel;
   public com.tlnt.ui.window.handbook.panel.HandbookChapterPanel.PageListPanel.PageButton pageButton;
   public HandbookPageButtonClickListener(HandbookChapterPanel.PageListPanel.PageButton button, HandbookChapterPanel.PageListPanel listPanel) {
      super();
      this.pageButton = button;
      this.pageListPanel = listPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.pageButton.pageListPanel.handbookChapterPanel.pageContentPanel.displayPage(this.pageButton.page);
   }
}
