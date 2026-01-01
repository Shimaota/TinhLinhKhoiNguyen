package com.tlnt.model.social;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.model.character.CharacterTemplate;

public final class TribeMember {
   public static final Pool<TribeMember> POOL = new TribeMemberPool();
   public int playerId;
   public String name;
   public TribeMember.Role role;
   public int level;
   public CharacterTemplate spirit;
   public long contribution;

   public static enum Role {
      LEADER("Tù trưởng"),
      DEPUTY("Phó tù trưởng"),
      MEMBER("Thành viên");

      public String label;

      Role(String label) {
         this.label = label;
      }

   }
}
