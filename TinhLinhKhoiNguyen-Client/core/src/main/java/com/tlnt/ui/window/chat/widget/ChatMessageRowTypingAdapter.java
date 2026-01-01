package com.tlnt.ui.window.chat.widget;

import com.github.tommyettinger.textra.TypingAdapter;
import com.tlnt.net.task.ServerPacketSender;

public final class ChatMessageRowTypingAdapter extends TypingAdapter {
   public com.tlnt.ui.window.chat.widget.ChatMessageRow chatMessageRow;
   public ChatMessageRowTypingAdapter(ChatMessageRow chatMessageRow) {
      super();
      this.chatMessageRow = chatMessageRow;
   }

   @Override
   public final void event(String event) {
      ServerPacketSender.getInstance().sendSelectMount(event);
   }
}
