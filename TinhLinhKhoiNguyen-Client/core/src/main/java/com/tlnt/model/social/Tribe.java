package com.tlnt.model.social;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.model.item.StatLine;

public final class Tribe implements Pool.Poolable {
   public static final Pool<Tribe> POOL = new TribePool();
   public int id;
   public String name;
   public String slogan;
   public String leaderName;
   public int maxMembers;
   public int level;
   public TribeEmblem emblem;
   public long emblemExpireTime;
   public Array<TribeMember> members = new Array<>();
   public Array<TribeApplicant> applicants = new Array<>();
   public long fund;
   public long resourcePoints;
   public long exp;
   public long expMax;
   public StatLine[] emblemStatLines = new StatLine[0];

   public final TribeMember.Role getMemberRole(int playerId) {
      Array.ArrayIterator it = this.members.iterator();

      while (it.hasNext()) {
         TribeMember member;
         if ((member = (TribeMember)it.next()).playerId == playerId) {
            return member.role;
         }
      }

      return TribeMember.Role.MEMBER;
   }

   public final boolean isLeader(int playerId) {
      if (this.getMemberRole(playerId) == TribeMember.Role.LEADER) {
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final void reset() {
      TribeMember.POOL.freeAll(this.members);
      this.members.clear();
      TribeApplicant.POOL.freeAll(this.applicants);
      this.applicants.clear();
      this.emblemStatLines = new StatLine[0];
   }
}
