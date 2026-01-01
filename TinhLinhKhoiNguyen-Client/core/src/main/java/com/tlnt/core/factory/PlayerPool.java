package com.tlnt.core.factory;

import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.core.GameMain;
import com.tlnt.screen.login.LoginScreen;

public final class PlayerPool extends ObjectPool<PlayerEntity> {
   @Override
   public PlayerEntity newObject() {
      return new PlayerEntity(com.tlnt.core.GameMain.instance.getLoginScreen(), 0, 0.0F, 0.0F);
   }

   @Override
   public final void free(PlayerEntity obj) {
      PlayerEntity player = (PlayerEntity)obj;
      player.setIsLocalPlayer(false);
      player.setActive(false);
      super.free(player);
   }

   @Override
   public final PlayerEntity obtain() {
      PlayerEntity player;
      (player = (PlayerEntity)super.obtain()).setActive(true);
      return player;
   }
}
