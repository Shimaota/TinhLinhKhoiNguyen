package com.tlnt.core.factory;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.player.PlayerEntity;

public final class PlayerFactory {
   public static PlayerEntity mainPlayer;
   public static final ObjectPool<PlayerEntity> PLAYER_POOL = new PlayerPool();

   public static PlayerEntity obtainPlayer(int id, float x, float y) {
      PlayerEntity player;
      (player = PLAYER_POOL.obtain()).setId(id);
      player.teleportTo(x, y);
      return player;
   }

   public static PlayerEntity obtainMainPlayer(int id, float x, float y) {
      if (mainPlayer == null) {
         mainPlayer = obtainPlayer(id, x, y);
      }

      mainPlayer.setId(id);
      mainPlayer.teleportTo(x, y);
      mainPlayer.setActive(true);
      return mainPlayer;
   }

   public static void freePlayer(PlayerEntity player) {
      PLAYER_POOL.free(player);
   }

   public static void deactivateMainPlayer() {
      mainPlayer.setActive(false);
   }
}
