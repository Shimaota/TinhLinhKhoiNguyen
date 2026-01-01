package com.tlnt.model.mob;

import com.badlogic.gdx.math.Vector2;

public final class MobData {
   public Vector2 spawnPosition = new Vector2();
   public MobTemplate template;
   public byte variantIndex;
   public int level;
   public long currentHp;
   public long maxHp;
}
