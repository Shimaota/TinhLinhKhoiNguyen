package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.action.QuantityInputDialog;

/** "Delete secondary password" button (decompiled C3760, op 1 sub 6): asks for the secondary password and sends it — the server refuses if wrong / locked. */
public final class DeleteSecondaryPasswordListener extends ClickListener {
   public AccountProtectionPanel protectionPanel;

   public DeleteSecondaryPasswordListener(AccountProtectionPanel panel) {
      super();
      this.protectionPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GameMain.instance
         .getLoginScreen()
         .debugScrollPane
         .findDialog(QuantityInputDialog.class)
         .openWithPrompt("Nhập mật khẩu cấp 2 để xóa", true, null, "",
            input -> ServerPacketSender.getInstance().sendDeleteSecondaryPassword(input));
   }
}
