package com.tlnt.ui.window.tribe.panel.info;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.widget.TextField;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class DonateGoldButtonListener extends ClickListener {
   public com.tlnt.ui.window.tribe.panel.info.TribeInfoPanel panel;

   public DonateGoldButtonListener(TribeInfoPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      QuantityInputDialog dialog = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(QuantityInputDialog.class);
      dialog.openWithPrompt("Nhập vàng", new TextField.TextFieldFilter.DigitsOnlyFilter(), "", new DonateGoldInputCallback(this));
   }

}
