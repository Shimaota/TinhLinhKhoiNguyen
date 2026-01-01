package com.tlnt.ui.window.talent;

import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.CurrencyBarPanel;
import com.tlnt.ui.window.action.TabbedWindow;
import com.tlnt.ui.window.character.panel.MaskPanel;
import com.tlnt.ui.window.character.panel.PetPanel;
import com.tlnt.ui.window.talent.panel.CostumePanel;
import com.tlnt.ui.window.talent.panel.BackwearPanel;
import com.tlnt.ui.window.talent.panel.TitlePanel;

public final class TalentWindow extends TabbedWindow {

   public TalentWindow(RenderContext ctx) {
      super(ctx, WindowType.COLLECTION_POPUP);
      this.addTab("Ngoại trang", "icon/257", new CostumePanel(super.renderContext));
      this.addTab("Danh hiệu", "icon/258", new TitlePanel(super.renderContext));
      this.addTab("Mặt nạ", "icon/664", new MaskPanel(super.renderContext));
      this.addTab("Thú cưng", "icon/709", new PetPanel(super.renderContext));
      this.addTab("Đeo lưng", "icon/947", new BackwearPanel(super.renderContext));
      this.addActor(new CurrencyBarPanel(super.renderContext));
   }

}
