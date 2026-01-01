package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class UnlockAccountListener extends ClickListener {
   public com.tlnt.ui.window.settings.panel.AccountProtectionPanel protectionPanel;

   public UnlockAccountListener(AccountProtectionPanel panel) {
      super();
      this.protectionPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GameMain.instance
         .getLoginScreen()
         .debugScrollPane
         .findDialog(QuantityInputDialog.class)
         .openWithPrompt("Nhập mật khẩu cấp 2", true, null, "", new SecondaryPasswordInputCallback(this));
   }

}
