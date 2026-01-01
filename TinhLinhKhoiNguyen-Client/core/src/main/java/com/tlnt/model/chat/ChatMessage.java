package com.tlnt.model.chat;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.engine.render.TextureAsset;

public final class ChatMessage {
   public static Pool<ChatMessage> pool = new ChatMessagePool();
   public int senderId;
   public String senderName;
   public String text;
   public ChatMessage.ChatChannel channel;

   public static ChatMessage obtain(ChatMessage.ChatChannel channel, int senderId, String senderName, String text) {
      ChatMessage message;
      (message = pool.obtain()).channel = channel;
      message.senderId = senderId;
      message.senderName = senderName;
      message.text = text;
      return message;
   }

   public static void free(ChatMessage message) {
      pool.free(message);
   }

   public static enum ChatChannel {
      NEARBY("ui/191", "(Lân cận)"),
      WORLD("ui/187", "(Thế giới)"),
      SYSTEM("ui/192", "(Hệ thống)"),
      PARTY("ui/189", "(Tổ đội)"),
      TRIBE("ui/188", "(Bộ lạc)"),
      PRIVATE("ui/190", "(Riêng)");

      public TextureAsset icon;
      public String label;

      ChatChannel(String iconPath, String label) {
         this.icon = TextureAsset.obtain(iconPath);
         this.label = label;
      }

   }
}
