package com.tlnt.model.chat;

import com.badlogic.gdx.utils.Array;
import com.tlnt.model.character.CharacterTemplate;

public final class PrivateConversation {
   public int partnerId;
   public String partnerName;
   public CharacterTemplate partnerClass;
   public int partnerLevel;
   public Array<ChatMessage> messages = new Array<>(50);
   public boolean hasUnread;

   public PrivateConversation(int partnerId, String partnerName, int partnerClassId, int partnerLevel) {
      this.partnerId = partnerId;
      this.partnerName = partnerName;
      this.partnerClass = CharacterTemplate.getById(partnerClassId);
      this.partnerLevel = partnerLevel;
   }
}
