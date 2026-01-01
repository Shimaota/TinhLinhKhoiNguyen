package com.tlnt.net;

import com.shimaota.a.a.IMessage;

public final class HandleMessageTask implements Runnable {
   public com.shimaota.a.a.IMessage message;
   public com.tlnt.net.MessageHandler messageHandler;
   public HandleMessageTask(MessageHandler messageHandler, IMessage message) {
      super();
      this.messageHandler = messageHandler;
      this.message = message;
   }

   @Override
   public final void run() {
      MessageHandler.getInstance().dispatchMessage(this.message);
   }
}
