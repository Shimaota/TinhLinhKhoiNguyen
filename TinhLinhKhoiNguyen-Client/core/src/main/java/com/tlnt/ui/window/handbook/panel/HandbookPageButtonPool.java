package com.tlnt.ui.window.handbook.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;

public final class HandbookPageButtonPool extends Pool<HandbookChapterPanel.PageListPanel.PageButton> {
   public com.tlnt.ui.window.handbook.panel.HandbookChapterPanel.PageListPanel pageListPanel;
   public ButtonGroup<Button> buttonGroup;

   public HandbookPageButtonPool(HandbookChapterPanel.PageListPanel listPanel) {
      super();
      this.pageListPanel = listPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final HandbookChapterPanel.PageListPanel.PageButton newObject() {
      HandbookChapterPanel.PageListPanel.PageButton pageButton = this.pageListPanel.new PageButton(this.pageListPanel.renderContext);
      this.buttonGroup.add(pageButton);
      return pageButton;
   }
}
