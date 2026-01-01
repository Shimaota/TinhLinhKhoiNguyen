package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.core.Settings;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.model.mob.MobTemplate;
import com.tlnt.model.player.FactionStatus;
import com.tlnt.model.skill.LearnedSkill;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.player.PlayerEntity;

public final class AutoTargetController extends BaseComponent {
   public PlayerEntity autoFlipEnabled;
   public boolean projectile = false;
   public boolean speed = true;
   public ObjectSet<MobTemplate> active = new ObjectSet<>();
   public Array<LearnedSkill> projectile2 = new Array<>();
   public long lastTemplate;
   /** Target that cannot be reached (stuck on terrain) -> time it may be selected again. */
   private final ObjectMap<WorldEntity, Long> unreachableUntil = new ObjectMap<>();
   private static final long UNREACHABLE_MS = 5_000L;

   /**
    * Target standing somewhere unreachable (e.g. a mob below solid ground with the player above): skipped for {@link #UNREACHABLE_MS}. Auto
    * used to reselect that same mob (or alternate between 2 mobs on either side) -> standing in place turning left / right forever.
    */
   public final void markUnreachable(WorldEntity target) {
      if (target != null && this.projectile) {
         this.unreachableUntil.put(target, System.currentTimeMillis() + UNREACHABLE_MS);
      }
   }

   private boolean isUnreachable(WorldEntity target) {
      Long until = this.unreachableUntil.get(target);
      if (until == null) {
         return false;
      }
      if (System.currentTimeMillis() >= until) {
         this.unreachableUntil.remove(target);
         return false;
      }
      return true;
   }

   public AutoTargetController(PlayerEntity player) {
      super(player.getEntity());
      this.autoFlipEnabled = player;
   }

   @Override
   public final void update(float delta) {
      if (this.autoFlipEnabled.isLocalPlayer()) {
         if (GameUtils.hasElapsed(this.lastTemplate, 100)) {
            this.lastTemplate = System.currentTimeMillis();
            AutoTargetController controller = this;
            if (this.projectile) {
               WorldEntity target = GameState.targetEntity;
               if (GameState.targetEntity != null) {
                  if (target.getEntityType() == EntityType.NPC) {
                     GameState.setTargetEntity(null);
                     target = null;
                  } else if (target.isDead()) {
                     target = null;
                  } else if (this.isUnreachable(target)) {
                     target = null;
                  } else if (target.getEntityType() != EntityType.PLAYER && target.getEntityType() != EntityType.MOB) {
                     target = null;
                  } else if (target.getEntityType() == EntityType.PLAYER && Settings.autoIgnorePlayersAndBosses) {
                     target = null;
                  } else if (target.getEntityType() == EntityType.MOB && !this.speed && !this.active.contains(((MobEntity)target).POOL2.template)) {
                     target = null;
                  }
               }

               if (target == null) {
                  float nearestDist = Float.MAX_VALUE;
                  Object nearest = null;

                  for (PlayerEntity otherPlayer : GameState.nearbyPlayers) {
                     float playerDist;
                     if (!Settings.autoIgnorePlayersAndBosses
                        && !otherPlayer.isLocalPlayer()
                        && !otherPlayer.isDead()
                        && !controller.isUnreachable(otherPlayer)
                        && controller.canTargetPlayer(otherPlayer)
                        && (playerDist = otherPlayer.distanceTo(PlayerEntity.getInstance())) < nearestDist) {
                        nearestDist = playerDist;
                        nearest = otherPlayer;
                     }
                  }

                  if (nearest == null) {
                     Array.ArrayIterator mobIterator = GameState.nearbyMobs.iterator();

                     while (mobIterator.hasNext()) {
                        MobEntity mob;
                        float mobDist;
                        if (!(mob = (MobEntity)mobIterator.next()).isDead()
                           && !controller.isUnreachable(mob)
                           && (controller.speed || controller.active.contains(mob.POOL2.template))
                           && (!Settings.autoAvoidEliteMobs || mob.POOL2.variantIndex == 0)
                           && (Settings.autoAttackRange == 0 // "Attack range" around the spot where auto-attack was enabled (decompiled C3541, Box2D units = px / 100)
                              || mob.getPosition().dst(Settings.autoAttackAnchor) <= Settings.autoAttackRange / 100.0F)
                           && (mobDist = mob.distanceTo(PlayerEntity.getInstance())) < nearestDist) {
                           nearestDist = mobDist;
                           nearest = mob;
                        }
                     }
                  }

                  if (nearest != null) {
                     GameState.setTargetEntity((WorldEntity)nearest);
                  }
               }

               if (!controller.canUseSkill(GameState.selectedSkill, false)) {
                  controller.projectile2.clear();

                  for (int i = 0;
                     i < GameMain.instance.getLoginScreen().debugScrollPane.POOL2.skillBar.skillButtons.length;
                     i++
                  ) {
                     if (Settings.autoSkillSlotEnabled[i]) {
                        LearnedSkill slotSkill = GameMain.instance.getLoginScreen().debugScrollPane.POOL2.skillBar.skillButtons[i].getSkill();
                        if (controller.canUseSkill(slotSkill, false)) {
                           controller.projectile2.add(slotSkill);
                        }
                     }
                  }

                  if (!controller.projectile2.isEmpty()) {
                     ServerPacketSender sender = ServerPacketSender.getInstance();
                     sender.sendSkillUse(
                        controller.projectile2.get(GameUtils.randomInt(0, controller.projectile2.size - 1)).skill.id
                     );
                  }
               }

               if (controller.autoFlipEnabled.interactionController != null) {
                  controller.autoFlipEnabled.interactionController.interact(false);
               }
            }
         }
      }
   }

   public final boolean canTargetPlayer(PlayerEntity other) {
      if (this.autoFlipEnabled.POOL2.isInParty && this.autoFlipEnabled.POOL2.partyLeaderId == other.getId()) {
         return true;
      } else {
         FactionStatus.FactionType myFaction = this.autoFlipEnabled.POOL2.factionStatus.faction;
         FactionStatus.FactionType otherFaction = other.POOL2.factionStatus.faction;
         boolean myIsEnemy = this.autoFlipEnabled.POOL2.isEnemy;
         boolean otherIsEnemy = other.POOL2.isEnemy;
         switch (myFaction) {
            case NEUTRAL:
               if (myIsEnemy && !other.isMountClass()) {
                  return false;
               } else {
                  switch (otherFaction) {
                     case FACTION_B:
                        return true;
                     default:
                        return false;
                  }
               }
            case FACTION_A:
               switch (otherFaction) {
                  case FACTION_A:
                     if (this.autoFlipEnabled.POOL2.partyId != -1 && this.autoFlipEnabled.POOL2.partyId == other.POOL2.partyId) {
                        return false;
                     }

                     return true;
                  case FACTION_B:
                     if (myIsEnemy) {
                        return false;
                     }

                     return true;
                  default:
                     return false;
               }
            case FACTION_C:
               switch (otherFaction) {
                  case FACTION_C:
                     if (this.autoFlipEnabled.POOL2.guildId != -1
                        && this.autoFlipEnabled.POOL2.guildId == other.POOL2.guildId) {
                        return false;
                     }

                     return true;
                  case FACTION_B:
                     if (myIsEnemy) {
                        return false;
                     }

                     return true;
                  default:
                     return false;
               }
            case FACTION_B:
               switch (otherFaction) {
                  case NEUTRAL:
                     if (otherIsEnemy) {
                        return false;
                     }

                     return true;
                  case FACTION_A:
                     if (otherIsEnemy) {
                        return false;
                     }

                     return true;
                  case FACTION_C:
                     if (otherIsEnemy) {
                        return false;
                     }

                     return true;
                  default:
                     return true;
               }
            default:
               return false;
         }
      }
   }

   public final boolean canUseSkill(LearnedSkill skill, boolean showMessage) {
      if (skill == null) {
         return false;
      } else if (isWeaponRequirementBlocked(skill)) {
         if (showMessage && !this.projectile) {
            GameMain.postFloatingMessage(getWeaponBlockedMessage(skill));
         }

         return false;
      } else if (PlayerEntity.getInstance().POOL2.activityPointsCurrent == 0) {
         if (showMessage && !this.projectile) {
            GameMain.postFloatingMessage("Thể thực đã cạn, hãy nghỉ ngơi để lấy lại sức!");
         }

         return false;
      } else if (!isSkillReady(skill)) {
         if (showMessage) {
            ;
         }

         return false;
      } else {
         long manaCost = 0L;
         switch (skill.skill.manaCostType) {
            case 1:
               manaCost = (long)skill.levelData.manaCost;
               break;
            case 2:
               manaCost = PlayerEntity.getInstance().POOL2.maxMp * (long)skill.levelData.manaCost / 100L;
         }

         if (PlayerEntity.getInstance().POOL2.currentMp < manaCost) {
            if (showMessage && !this.projectile) {
               GameMain.postFloatingMessage("Không đủ năng lượng để sử dụng chiêu thức này!");
            }

            return false;
         } else {
            return true;
         }
      }
   }

   public static boolean isWeaponRequirementBlocked(LearnedSkill skill) {
      if (skill.skill.id == 1) {
         if (PlayerEntity.getInstance().POOL2.weaponType != -1) {
            return true;
         }
      } else {
         if (PlayerEntity.getInstance().POOL2.weaponType == -1) {
            return true;
         }
      }

      return false;
   }

   public static String getWeaponBlockedMessage(LearnedSkill skill) {
      if (skill.skill.id == 1) {
         if (PlayerEntity.getInstance().POOL2.weaponType != -1) {
            return "Đang mang vũ khí! không thể sử dụng kỹ năng này";
         }
      } else {
         if (PlayerEntity.getInstance().POOL2.weaponType == -1) {
            return "Chưa mang vũ khí! không thể sử dụng kỹ năng này";
         }
      }

      return "";
   }

   public static boolean isSkillReady(LearnedSkill skill) {
      return GameUtils.hasElapsed(skill.lastCastTime, skill.levelData != null ? skill.levelData.cooldownMillis : 0);
   }

}
