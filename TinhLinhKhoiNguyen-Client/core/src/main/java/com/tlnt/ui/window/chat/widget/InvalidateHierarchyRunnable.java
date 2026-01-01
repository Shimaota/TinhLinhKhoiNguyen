package com.tlnt.ui.window.chat.widget;

public final class InvalidateHierarchyRunnable implements Runnable {
   public com.tlnt.ui.window.chat.widget.ChatMessageRow chatMessageRow;
   public InvalidateHierarchyRunnable(ChatMessageRow chatMessageRow) {
      super();
      this.chatMessageRow = chatMessageRow;
   }

   @Override
   public final void run() {
      this.chatMessageRow.messageLabel.invalidateHierarchy();
   }
}
