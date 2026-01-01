package com.tlnt.model.party;

import com.tlnt.model.character.CharacterTemplate;

public final class PartyJoinRequest {
   public int playerId;
   public String name;
   public int level;
   public CharacterTemplate spirit;

   public PartyJoinRequest(int playerId, String name, int level, CharacterTemplate spirit) {
      this.playerId = playerId;
      this.name = name;
      this.level = level;
      this.spirit = spirit;
   }
}
