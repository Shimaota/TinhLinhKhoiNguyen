package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

/**
 * Direction arrow (op 63 — decompiled C3549) + "Auto-pathing": clicking the quest text on the HUD ({@code {TRIGGER=finding}})
 * makes the character walk to the middle of the arrow's target area ({@link #setAutoPathing}); moving manually / enabling auto-attack /
 * selecting a target / the arrow turning off stops it. The HUD shows "Tự tìm đường" while it is on.
 */
public final class OffscreenTargetArrow extends SpineSkeletonComponent {
   public final PlayerEntity projectile2;
   /** Auto-pathing towards {@link #getTargetCenter()}. */
   private boolean autoPathing;
   private final Vector2 walkTarget = new Vector2();
   private long lastEnterAt;
   public final Vector2 lastTemplate = new Vector2();
   public final Vector2 effectStartTime = new Vector2();
   public final Rectangle effectDuration = new Rectangle();
   public boolean isArrowVisible;

   public OffscreenTargetArrow(PlayerEntity player) {
      super(player.getEntity());
      this.projectile2 = player;
      this.setRenderOrder(1.0E8F);
      this.requestLayerChange("skeletons/vfxs/guide_arrow", 0.25F, new SkeletonLoader.AnimationMixConfig());
      this.playAnimation("idle", true, 1.5F);
      this.setScaleMultiplier(2.0F);
      super.autoFlipEnabled = false;
      this.setEnabled(false);
   }

   public final void setViewportBounds(float x, float y, float width, float height) {
      this.effectStartTime.set(x + width / 2.0F, y + height / 2.0F);
      this.effectDuration.set(x, y, width, height);
   }

   public final Vector2 getTargetCenter() {
      return this.effectStartTime;
   }

   public final boolean isArrowVisible() {
      return this.isArrowVisible;
   }

   /** Turns auto-pathing on / off (can only be turned on while the arrow is visible); turning it on disables auto-attack (decompiled m16524). */
   public final void setAutoPathing(boolean on) {
      if (!this.isEnabled()) {
         on = false;
      }

      this.autoPathing = on;
      if (on && this.projectile2.autoTargetController != null) {
         this.projectile2.autoTargetController.projectile = false;
      }
   }

   public final boolean isAutoPathing() {
      return this.autoPathing;
   }

   /** Auto-pathing to exactly the {@code waypoint} door (decompiled C3546: touching a click-to-enter door also enters automatically). */
   public final boolean isAutoPathingTo(com.tlnt.world.entity.decoration.WaypointEntity waypoint) {
      return this.autoPathing && this.isEnabled() && waypoint.containsVector(this.effectStartTime);
   }

   /** Standing inside a target area that is a door: send op 11 (500 ms throttle, not while auto-attacking). */
   private void enterWaypointAtTarget() {
      if (System.currentTimeMillis() - this.lastEnterAt < 500L
         || this.projectile2.autoTargetController != null && this.projectile2.autoTargetController.projectile) {
         return;
      }
      com.tlnt.screen.login.LoginScreen screen = com.tlnt.core.GameMain.instance == null ? null
         : com.tlnt.core.GameMain.instance.getLoginScreen();
      if (screen == null || screen.editorViewport == null || screen.editorViewport.owner == null) {
         return;
      }
      for (com.tlnt.world.entity.decoration.WaypointEntity waypoint : screen.editorViewport.owner) {
         if (waypoint.containsVector(this.effectStartTime)) {
            this.lastEnterAt = System.currentTimeMillis();
            com.tlnt.net.task.ServerPacketSender.getInstance().sendPlayerAction(waypoint.POOL2);
            return;
         }
      }
   }

   @Override
   public final void update(float delta) {
      if (this.autoPathing && !this.projectile2.serverPathActive && this.projectile2.movementController != null) {
         Vector2 pos = this.projectile2.getPosition();
         if (this.effectDuration.contains(pos) || pos.dst(this.effectStartTime) < 1.5F) {
            // reached the target area: stop; if the area is a door (including click-to-enter doors) -> enter automatically like decompiled C3546
            this.projectile2.movementController.stopMovement(false);
            enterWaypointAtTarget();
         } else {
            // walk to the FOOT of the target area (the area center is ≥ 1 unit above the foot -> it used to think it had not arrived and jumped forever)
            this.walkTarget.set(this.effectStartTime.x, this.effectDuration.y + 0.1F);
            this.projectile2.movementController.moveTowardTarget(this.walkTarget, null);
         }
      }

      Vector2 playerCenter = this.projectile2.getCenterPosition();
      Vector2 toTarget;
      float angleDeg = (toTarget = this.effectStartTime.cpy().sub(playerCenter)).angleDeg();
      this.setMixDuration(angleDeg);
      this.lastTemplate.set(playerCenter).add(toTarget.nor().scl(1.0F));
   }

   @Override
   public final void render(RenderContext ctx) {
      if (!this.effectDuration.contains(this.projectile2.getPosition())) {
         if (!(this.projectile2.getPosition().dst(this.effectStartTime) < 1.5F)) {
            super.render(ctx);
         }
      }
   }

   @Override
   public final void tickSkeleton(float delta) {
      if (super.active != null && super.speed != null) {
         super.active.update(delta);
         super.active.apply(super.speed);
         super.speed.update(delta);
         super.speed.setPosition(this.lastTemplate.x, this.lastTemplate.y);
         super.speed.updateWorldTransform(Skeleton.Physics.UPDATE);
         super.projectile.update(super.speed, true);
      }
   }

}
