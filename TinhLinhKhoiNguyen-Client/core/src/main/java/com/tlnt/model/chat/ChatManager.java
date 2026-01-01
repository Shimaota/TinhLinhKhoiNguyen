package com.tlnt.model.chat;

import com.badlogic.gdx.utils.Array;

public final class ChatManager {
   public Array<ChatMessage> hudMessages = new Array<>(50);
   public boolean hudDirty;
   public Array<ChatMessage> generalMessages;
   public boolean generalDirty;
   public Array<ChatMessage> nearbyMessages;
   public boolean nearbyDirty;
   public Array<ChatMessage> worldMessages;
   public boolean worldDirty;
   public Array<ChatMessage> systemMessages;
   public boolean systemDirty;
   public Array<ChatMessage> partyMessages;
   public boolean partyDirty;
   public Array<ChatMessage> tribeMessages;
   public boolean tribeDirty;
   public Array<PrivateConversation> privateConversations;
   public boolean privateDirty;

   public ChatManager() {
      this.generalMessages = new Array<>(50);
      this.nearbyMessages = new Array<>(50);
      this.worldMessages = new Array<>(50);
      this.systemMessages = new Array<>(50);
      this.partyMessages = new Array<>(50);
      this.tribeMessages = new Array<>(50);
      this.privateConversations = new Array<>(10);
   }

   public void addHudMessage(ChatMessage message) {
      this.hudMessages.add(message);
      if (this.hudMessages.size > 50) {
         ChatMessage.free(this.hudMessages.removeIndex(0));
      }

      this.hudDirty = true;
   }

   public void addGeneralMessage(ChatMessage message) {
      this.generalMessages.add(message);
      if (this.generalMessages.size > 50) {
         ChatMessage.free(this.generalMessages.removeIndex(0));
      }

      this.generalDirty = true;
   }

   public final void addNearbyMessage(ChatMessage message) {
      this.nearbyMessages.add(message);
      if (this.nearbyMessages.size > 50) {
         ChatMessage.free(this.nearbyMessages.removeIndex(0));
      }

      this.nearbyDirty = true;
      this.addHudMessage(message);
      this.addGeneralMessage(message);
   }

   public final void addWorldMessage(ChatMessage message) {
      this.worldMessages.add(message);
      if (this.worldMessages.size > 50) {
         ChatMessage.free(this.worldMessages.removeIndex(0));
      }

      this.worldDirty = true;
      this.addHudMessage(message);
      this.addGeneralMessage(message);
   }

   public final void addSystemMessage(ChatMessage message) {
      this.systemMessages.add(message);
      if (this.systemMessages.size > 50) {
         ChatMessage.free(this.systemMessages.removeIndex(0));
      }

      this.systemDirty = true;
      this.addHudMessage(message);
      this.addGeneralMessage(message);
   }
}
