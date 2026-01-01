package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.GameState;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.model.skill.LearnedSkill;
import com.tlnt.model.skill.SkillLevelData;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.world.entity.decoration.WaypointEntity;
import com.tlnt.world.entity.mapitem.MapItemEntity;
import com.tlnt.world.entity.plant.PlantEntity;
import com.tlnt.world.entity.npc.NpcEntity;
import com.tlnt.world.entity.player.PlayerEntity;

public final class InteractionController extends BaseComponent {
   public PlayerEntity projectile;
   public WorldEntity speed;
   public MapItemEntity active;
   public NpcEntity autoFlipNpc;
   public AnimalEntity projectile2;
   public PlantEntity targetPlant;
   public WaypointEntity waypointTarget;
   public final MoveToTargetAndAttackCallback attackCallback = new MoveToTargetAndAttackCallback(this);
   public final MoveToItemCallback moveToItemCallback = new MoveToItemCallback(this);
   public final MoveToNpcCallback moveToNpcCallback = new MoveToNpcCallback(this);
   public final MoveToAnimalCallback moveToAnimalCallback = new MoveToAnimalCallback(this);
   public final MoveToPlantCallback moveToPlantCallback = new MoveToPlantCallback(this);
   /** Stand slightly inside the horizontal reach: once there, it is surely in reach even if the mob sways. */
   private static final float APPROACH_FACTOR = 0.8F;
   /** If the horizontal offset exceeds this, recompute the approach side (the target has moved fully to the other side). */
   private static final float SIDE_LOCK_DISTANCE = 0.3F;
   /** Target on another floor: stand this far away horizontally (after falling / jumping it is in reach). */
   private static final float CLOSE_STAND = 0.4F;
   private final Vector2 approachPoint = new Vector2();
   private WorldEntity approachTarget;
   private float approachSide = -1.0F;

   public InteractionController(PlayerEntity player) {
      super(player.getEntity());
      this.projectile = player;
   }

   /**
    * Spot to stand on to attack {@code target}: BESIDE the target, {@link #APPROACH_FACTOR} × attack reach away, on the side the player is
    * standing (like the original: {@code target + range × (player.x < target.x ? -1 : 1)}). It used to walk straight into the mob's center ->
    * standing on top of the mob, dx changed sign with every sway of the mob -> the character kept turning left / right, and the camera
    * (look-ahead in the facing direction) jerked along. The approach side is REMEMBERED per target so it does not flip when standing close.
    */
   private Vector2 approachPoint(WorldEntity target, SkillLevelData level) {
      Vector2 targetPos = target.getPosition();
      Vector2 me = this.projectile.getPosition();
      float dx = me.x - targetPos.x;
      // Height offset beyond the Y reach (mob below the platform / on the platform): must get CLOSER horizontally (to the platform edge to drop down,
      // or under the platform to jump up). It used to treat "close enough horizontally" as done -> standing still on the platform, considered stuck after 1 s,
      // auto switched to another mob on the other side -> turning left / right without moving.
      boolean verticalOk = !AttackReach.tooHighOrLow(me, targetPos, level);
      float reachX = verticalOk ? AttackReach.horizontal(level) * APPROACH_FACTOR : CLOSE_STAND;
      if (Math.abs(dx) <= reachX) {
         // Close enough horizontally: do NOT walk back to the side spot (walking out and back = flipping), only jumping / falling remains.
         return this.approachPoint.set(me.x, targetPos.y);
      }
      if (target != this.approachTarget || Math.abs(dx) > SIDE_LOCK_DISTANCE) {
         this.approachTarget = target;
         this.approachSide = dx != 0.0F ? Math.signum(dx) : (this.projectile.getDirection() == Direction.LEFT ? 1.0F : -1.0F);
      }
      return this.approachPoint.set(targetPos.x + this.approachSide * reachX, targetPos.y);
   }

   public final void setWaypointTarget(WaypointEntity waypoint) {
      this.waypointTarget = waypoint;
   }

   public final void interact(boolean useWaypoint) {
      if (useWaypoint && this.waypointTarget != null) {
         ServerPacketSender.getInstance().sendPlayerAction(this.waypointTarget.POOL2);
      } else {
         WorldEntity target = GameState.targetEntity;
         if (GameState.targetEntity != null) {
            if (target.getEntityType() == EntityType.ITEM_MAP) {
               MapItemEntity mapItem = (MapItemEntity)target;
               if (mapItem != null) {
                  this.active = mapItem;
                  if (this.projectile.distanceTo(mapItem) > 1.0F) {
                     this.projectile.movementController.moveTowardTarget(mapItem.getPosition(), this.moveToItemCallback);
                     return;
                  }

                  this.moveToItemCallback.execute();
               }

               return;
            }

            if (target.getEntityType() == EntityType.NPC) {
               NpcEntity npc = (NpcEntity)target;
               if (npc != null) {
                  if (npc.POOL2.template.facesPlayerOnInteract) {
                     npc.faceTowards(PlayerEntity.getInstance().getPosition());
                  }

                  this.autoFlipNpc = npc;
                  if (this.projectile.distanceTo(npc) > 1.0F) {
                     this.projectile.movementController.moveTowardTarget(npc.getPosition(), this.moveToNpcCallback);
                     return;
                  }

                  this.moveToNpcCallback.execute();
               }

               return;
            }

            if (target.getEntityType() == EntityType.ANIMAL) {
               AnimalEntity animal = (AnimalEntity)target;
               if (animal != null) {
                  this.projectile2 = animal;
                  if (this.projectile.distanceTo(this.projectile2) > 1.0F) {
                     this.projectile.movementController.moveTowardTarget(this.projectile2.getPosition(), this.moveToAnimalCallback);
                     return;
                  }

                  this.moveToAnimalCallback.execute();
               }

               return;
            }

            if (target.getEntityType() == EntityType.PLANT) {
               PlantEntity plant = (PlantEntity)target;
               if (plant != null) {
                  this.targetPlant = plant;
                  if (this.projectile.distanceTo(this.targetPlant) > 1.0F) {
                     this.projectile.movementController.moveTowardTarget(this.targetPlant.getPosition(), this.moveToPlantCallback);
                     return;
                  }

                  this.moveToPlantCallback.execute();
               }

               return;
            }
         }

         LearnedSkill selectedSkill = GameState.selectedSkill;
         if (this.projectile.getAnimationState().allowsInteraction) {
            switch (selectedSkill.skill.targetType) {
               case 1:
                  if (target == null || target.isDead()) {
                     return;
                  }

                  if (target != null && !target.isDead()) {
                     this.speed = target;
                     SkillLevelData level = GameState.selectedSkill.levelData;
                     if (AttackReach.inReach(this.projectile, target, level)) {
                        this.attackCallback.execute();
                        return;
                     }

                     // Removed: "using the Spirit Badge (appleEventState, op 45) + auto + mob -> setWorldPosition straight
                     // to the mob" (not present in decompiled C3545). The server's speed-hack protection pulled the player back every 0.1 s -> the screen
                     // jerked and distant mobs were never reached. Now it walks there like a player / boss.
                     this.projectile.movementController.moveTowardTarget(this.approachPoint(target, level), this.attackCallback);
                  }
                  break;
               case 2:
                  if (!this.projectile.autoTargetController.canUseSkill(selectedSkill, true)) {
                     return;
                  }

                  ServerPacketSender sender = ServerPacketSender.getInstance();
                  sender.sendSkillbarAction(selectedSkill.skill.id, -1, -1);
            }
         }
      }
   }
}
