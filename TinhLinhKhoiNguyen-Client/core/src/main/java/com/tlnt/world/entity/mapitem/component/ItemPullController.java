package com.tlnt.world.entity.mapitem.component;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.GameState;
import com.tlnt.core.factory.MapItemFactory;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.world.entity.mapitem.MapItemEntity;
import com.tlnt.world.entity.player.PlayerEntity;

public final class ItemPullController extends BaseComponent {
   public float projectile = 10.0F;
   public PlayerEntity speed;
   public MapItemEntity active;

   public ItemPullController(MapItemEntity mapItem) {
      super(mapItem.getEntity());
      this.active = mapItem;
   }

   public final void startPull(PlayerEntity player) {
      this.speed = player;
      this.active.setSensorEnabled(true);
      this.active.skeleton.getBody().setGravityScale(0.0F);
   }

   @Override
   public final void update(float delta) {
      if (this.speed != null) {
         Vector2 toPlayer;
         float distance = (toPlayer = this.speed.getCenterPosition().cpy().sub(this.active.getPosition())).len();
         if (toPlayer.nor().scl(this.projectile).scl(delta).len() > distance) {
            this.speed = null;
            this.active.setSensorEnabled(false);
            this.active.skeleton.getBody().setGravityScale(1.0F);
            GameState.mapItems.removeValue(this.active, true);
            MapItemFactory.freeMapItem(this.active);
         } else {
            this.active.setWorldPosition(this.active.getPosition().add(toPlayer));
         }
      }
   }
}
