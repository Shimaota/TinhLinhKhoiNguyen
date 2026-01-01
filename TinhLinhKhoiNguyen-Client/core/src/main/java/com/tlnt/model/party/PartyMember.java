package com.tlnt.model.party;

import com.tlnt.model.character.CharacterClass;
import com.tlnt.model.character.CharacterTemplate;

public final class PartyMember {
   public int playerId;
   public String name;
   public int level;
   public CharacterTemplate spirit;
   public CharacterClass charClass;
   public PartyMember.Role role;
   public long joinTime;

   public PartyMember(int playerId, String name, int level, int spiritId, int classId, PartyMember.Role role, long joinTime) {
      this.playerId = playerId;
      this.name = name;
      this.level = level;
      this.spirit = CharacterTemplate.getById(spiritId);
      this.charClass = CharacterClass.getById(classId);
      this.role = role;
      this.joinTime = joinTime;
   }

   public final void dispose() {
      this.name = null;
      this.role = null;
   }

   public static enum Role {
      LEADER("Trưởng nhóm"),
      MEMBER("Thành viên");

      public String label;

      Role(String label) {
         this.label = label;
      }

   }
}
