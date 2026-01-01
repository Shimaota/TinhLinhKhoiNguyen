package com.tlnt.model.ranking;

import com.tlnt.model.character.CharacterTemplate;

public final class RankEntry {
   public int rank;
   public int playerId;
   public String playerName;
   public String value;
   public CharacterTemplate characterClass;

   public RankEntry(int rank, int playerId, String playerName, String value, int classId) {
      this.rank = rank;
      this.playerId = playerId;
      this.playerName = playerName;
      this.value = value;
      this.characterClass = CharacterTemplate.getById(classId);
   }
}
