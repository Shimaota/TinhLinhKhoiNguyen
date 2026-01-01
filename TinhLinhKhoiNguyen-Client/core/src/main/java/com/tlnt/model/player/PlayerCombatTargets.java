package com.tlnt.model.player;

import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.WorldEntity;

public final class PlayerCombatTargets {
   public WorldEntity skill6AttackTarget;
   public WorldEntity skill8AttackTarget;
   public Array<GameObject> skill9HitTargets = new Array<>();
   public WorldEntity skill11MainTarget;
   public Array<GameObject> skill11AoeTargets = new Array<>();
   public WorldEntity skill12AttackTarget;
   public WorldEntity skill13AttackTarget;
   public WorldEntity skill30AttackTarget;
   public WorldEntity skill14AttackTarget;
   public Array<GameObject> skill15AoeTargets = new Array<>();
   public WorldEntity skill17AttackTarget;
   public WorldEntity skill18AttackTarget;
   public WorldEntity skill19AttackTarget;
   public WorldEntity skill25AttackTarget;
   public boolean skill7PierceFlag;
   public boolean skill26Flag;
   public boolean skill29Flag;
   public boolean skill23Flag;
   public boolean skill3AoeFlag;
   public boolean skill28Flag;
   public WorldEntity currentAttackTarget;
   public Array<WorldEntity> aoeHitTargets = new Array<>();
}
