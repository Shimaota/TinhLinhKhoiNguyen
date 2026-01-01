package com.tlnt.ui.window.wardrobe;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.wardrobe.panel.CostumePanel;
import com.tlnt.ui.window.wardrobe.panel.BackwearPanel;
import com.tlnt.ui.window.wardrobe.panel.MaskPanel;
import com.tlnt.ui.window.wardrobe.panel.PetPanel;
import com.tlnt.ui.window.wardrobe.panel.TitlePanel;
import com.tlnt.ui.window.wardrobe.panel.WardrobeCharacterTab;

public final class WardrobeWindow extends TabbedWindow {

   public WardrobeWindow(RenderContext renderContext) {
      super(renderContext, WindowType.INFO_PLAYER_POPUP);
      this.addTab("Nhân vật", "icon/252", new WardrobeCharacterTab(super.renderContext));
      this.addTab("Ngoại trang", "icon/257", new CostumePanel(super.renderContext));
      String tabTitle = "Danh hiệu";
      this.addTab(tabTitle, "icon/258", new TitlePanel(super.renderContext));
      this.addTab("Mặt nạ", "icon/664", new MaskPanel(super.renderContext));
      this.addTab("Thú cưng", "icon/709", new PetPanel(super.renderContext));
      this.addTab("Đeo lưng", "icon/947", new BackwearPanel(super.renderContext));
   }

}
