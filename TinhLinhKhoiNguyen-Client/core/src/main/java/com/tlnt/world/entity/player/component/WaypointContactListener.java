package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.physics.box2d.Contact;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.Component;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.decoration.WaypointEntity;
import com.tlnt.world.entity.player.PlayerEntity;

public final class WaypointContactListener extends BaseComponent {
   public PlayerEntity projectile;
   public long speed;

   public WaypointContactListener(PlayerEntity player) {
      super(player.getEntity());
      this.projectile = player;
   }

   @Override
   public final void onBodyContact(BodyComponent other) {
      if (this.projectile.isLocalPlayer()) {
         if (other.getEntity().getEntityType() == EntityType.WAYPOINT) {
            WaypointEntity waypoint;
            // decompiled C3546: walk-through doors, or click-to-enter doors while auto-pathing / being guided by the server to exactly this door
            // -> enter automatically (not while auto-attacking); otherwise just select the door as the target
            if (!(waypoint = (WaypointEntity)other.getEntity().getGameObject()).skeleton
               || this.projectile.offscreenArrow.isAutoPathingTo(waypoint)
               || this.projectile.serverPathActive && waypoint.containsVector(this.projectile.serverPathTarget)) {
               if (GameUtils.hasElapsed(this.speed, 500) && !this.projectile.autoTargetController.projectile) {
                  this.speed = System.currentTimeMillis();
                  ServerPacketSender.getInstance().sendPlayerAction(((WaypointEntity)other.getEntity().getGameObject()).POOL2);
                  return;
               }
            } else {
               this.projectile.interactionController.setWaypointTarget(waypoint);
            }
         }
      }
   }

   @Override
   public final void onContactEnd(Component other, Contact contact, Object userData) {
      if (this.projectile.isLocalPlayer()) {
         if (other.getEntity().getEntityType() == EntityType.WAYPOINT) {
            this.projectile.interactionController.setWaypointTarget(null);
         }
      }
   }
}
