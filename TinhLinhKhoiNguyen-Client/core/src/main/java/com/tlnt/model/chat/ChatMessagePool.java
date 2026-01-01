package com.tlnt.model.chat;

import com.badlogic.gdx.utils.Pool;

public final class ChatMessagePool extends Pool<ChatMessage> {

   @Override
   public ChatMessage newObject() {
      return new com.tlnt.model.chat.ChatMessage();
   }
}
