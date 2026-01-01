package com.tlnt.world.entity.player.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class QuestCompleteEffect extends SpineSkeletonComponent {
   public PlayerEntity projectile2;

   public QuestCompleteEffect(PlayerEntity player) {
      super(player.getEntity());
      this.projectile2 = player;
      this.setRenderOrder(1.0E8F);
      String skeletonKey = "skeletons/vfxs/quest_complete";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig();
      this.requestLayerChange(skeletonKey, 0.25F, mixConfig);
      super.autoFlipEnabled = false;
      this.addAnimationListener(new QuestCompleteAnimListener(this));
   }

   public final void playEffect() {
      this.setEnabled(true);
      AudioManager.playSound("quest_complete", this.projectile2.getCenterPosition());
      this.setSkeletonOffset(0.0F, this.projectile2.getHeight() / 2.0F);
      String animName = "idle";
      this.playAnimation(animName, false, 1.0F);
   }

}
