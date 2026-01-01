package com.tlnt.model.social;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.model.character.CharacterTemplate;

public final class TribeApplicant {
   public static final Pool<TribeApplicant> POOL = new TribeApplicantPool();
   public int playerId;
   public String name;
   public int level;
   public CharacterTemplate spirit;
}
