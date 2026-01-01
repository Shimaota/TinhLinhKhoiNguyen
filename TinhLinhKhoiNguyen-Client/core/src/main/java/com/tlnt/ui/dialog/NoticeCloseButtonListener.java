package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.WindowType;

public final class NoticeCloseButtonListener extends ClickListener {
   public com.tlnt.ui.dialog.NoticeDialog noticeDialog;
   public NoticeCloseButtonListener(NoticeDialog noticeDialog) {
      super();
      this.noticeDialog = noticeDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      com.tlnt.net.task.ServerPacketSender.getInstance().connectToServer(0);
      GameMain.instance.getGameScreen().active.showDialog(WindowType.LOGIN_PANEL);
   }
}
