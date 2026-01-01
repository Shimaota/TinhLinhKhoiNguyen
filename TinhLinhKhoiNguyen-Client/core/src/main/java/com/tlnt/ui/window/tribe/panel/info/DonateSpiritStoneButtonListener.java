package com.tlnt.ui.window.tribe.panel.info;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.widget.TextField;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class DonateSpiritStoneButtonListener extends ClickListener {
   public com.tlnt.ui.window.tribe.panel.info.TribeInfoPanel panel;

   public DonateSpiritStoneButtonListener(TribeInfoPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GameMain.instance
         .getLoginScreen()
         .debugScrollPane
         .findDialog(QuantityInputDialog.class)
         .openWithPrompt(
            "Nhập lam thạch", new TextField.TextFieldFilter.DigitsOnlyFilter(), "", new DonateSpiritStoneInputCallback(this)
         );
   }

}
