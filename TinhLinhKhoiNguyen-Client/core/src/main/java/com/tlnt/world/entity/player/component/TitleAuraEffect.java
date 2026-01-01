package com.tlnt.world.entity.player.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.model.title.TitleData;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

/**
 * Sun / Moon Seal title halo around the character (port of decompiled C3495): skeleton {@code skeletons/vfxs/aura_fx},
 * animation = the title's {@code texture_key} column ("sun" / "moon"), looping. Hidden when there is no title.
 */
public final class TitleAuraEffect extends SpineSkeletonComponent {
   private final PlayerEntity owner;
   private String playing;
   private String requested;

   public TitleAuraEffect(PlayerEntity owner) {
      super(owner.getEntity());
      this.owner = owner;
      this.setRenderOrder(owner.getRenderOrder());
      this.requestLayerChange("skeletons/vfxs/aura_fx", 0.25F, new SkeletonLoader.AnimationMixConfig(0.2F));
      this.setEnabled(false);
   }

   /** Called after every stats block read: enable / change / disable according to the current title. */
   public void refresh() {
      TitleData title = this.owner.POOL2 == null ? null : this.owner.POOL2.titleData;
      String anim = title == null || title.auraAnimation == null || title.auraAnimation.isEmpty() ? null : title.auraAnimation;
      if (anim == null) {
         this.playing = null;
         this.requested = null;
         this.pendingAnimation.clear();
         this.setEnabled(false);
         return;
      }
      this.requested = anim;
      this.setEnabled(true);
      this.startRequestedAnimation();
   }

   /** The skeleton loads asynchronously; keep the request until the animation can actually be played. */
   @Override
   public void lateUpdate(float delta) {
      super.lateUpdate(delta);
      this.startRequestedAnimation();
   }

   private void startRequestedAnimation() {
      if (this.requested != null && !this.requested.equals(this.playing)
            && !this.pendingLayerChange.pending && this.speed != null
            && this.playAnimation(this.requested, true, 1.0F)) {
         this.playing = this.requested;
      }
   }

   /** Object returned to the pool: turn off. */
   public void clear() {
      this.playing = null;
      this.requested = null;
      this.pendingAnimation.clear();
      this.setEnabled(false);
   }
}
