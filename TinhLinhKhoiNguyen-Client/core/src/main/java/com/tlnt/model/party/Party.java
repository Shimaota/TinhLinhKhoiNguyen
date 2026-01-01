package com.tlnt.model.party;

import com.badlogic.gdx.utils.Array;
import com.tlnt.model.character.CharacterTemplate;

public final class Party {
   public int id;
   public String name;
   public CharacterTemplate leaderSpirit;
   public long createdTime;
   public Array<PartyMember> members;
   public Array<PartyJoinRequest> joinRequests;
   public Array<PartyBuffEntry> buffs;
   public boolean joinDisabled;
   public boolean autoApprove;

   public Party(int id, String name, CharacterTemplate leaderSpirit, long createdTime) {
      this.id = id;
      this.name = name;
      this.leaderSpirit = leaderSpirit;
      this.createdTime = createdTime;
      this.members = new Array<>();
      this.joinRequests = new Array<>();
      this.buffs = new Array<>();
   }

   public final PartyMember.Role getMemberRole(int playerId) {
      Array.ArrayIterator it = this.members.iterator();

      while (it.hasNext()) {
         PartyMember member;
         if ((member = (PartyMember)it.next()).playerId == playerId) {
            return member.role;
         }
      }

      return PartyMember.Role.MEMBER;
   }

   public final void dispose() {
      this.name = null;
      Array.ArrayIterator it = this.members.iterator();

      while (it.hasNext()) {
         ((PartyMember)it.next()).dispose();
      }

      this.members.clear();
      this.members = null;
      this.joinRequests.clear();
      this.joinRequests = null;
      this.buffs.clear();
      this.buffs = null;
   }
}
