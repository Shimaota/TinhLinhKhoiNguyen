package com.tlnt.net.task;

import com.tlnt.ui.window.action.QuantityInputDialog;

public final class ChatDialogCallback implements QuantityInputDialog.InputCallback {
   public byte channel;
   public com.tlnt.net.task.ServerPacketHandler handler;
   public ChatDialogCallback(ServerPacketHandler handler, byte channel) {
      super();
      this.handler = handler;
      this.channel = channel;
   }

   @Override
   public final void onInputConfirmed(String text) {
      ServerPacketSender.getInstance().sendChannelChat(this.channel, text);
   }
}
