package com.tlnt.world.skeleton;

import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.thirdparty.spine.Event;
import com.tlnt.world.anim.AnimationBehavior;

public abstract class AnimationStateDispatcher<T extends GameObject> extends BaseComponent {
   public final T projectile;
   public AnimationBehavior<T> speed;
   public AnimationState active;
   /** {@code Entity.animationStateVersion} at the previous update. */
   private int seenVersion;

   public AnimationStateDispatcher(T gameObject) {
      super(gameObject.getEntity());
      this.projectile = (T)gameObject;
      this.seenVersion = gameObject.getEntity().animationStateVersion;
   }

   @Override
   public final void update(float delta) {
      AnimationState currentState = this.projectile.getAnimationState();
      int version = this.projectile.getEntity().animationStateVersion;
      // Same state as last time but it changed back and forth between 2 updates (e.g. attack -> IDLE -> attack): must re-enter.
      boolean retriggered = this.speed != null && this.active == currentState && version != this.seenVersion;
      this.seenVersion = version;
      if (this.active != currentState || this.speed == null || retriggered) {
         AnimationBehavior behavior = this.getAnimationBehavior(currentState);
         if (this.speed != behavior || retriggered) {
            if (this.speed != null) {
               this.speed.onExit(this.projectile);
            }

            this.speed = behavior;
            this.active = currentState;
            if (this.speed != null) {
               this.speed.onEnter(this.projectile);
            }
         }
      }

      if (this.speed != null) {
         this.speed.onUpdate(this.projectile, delta);
      }

      this.afterUpdate(delta);
   }

   /** After every update (subclasses check for stuck animations). */
   protected void afterUpdate(float delta) {
   }

   public final void dispatchEvent(String eventName) {
      if (this.speed != null) {
         this.speed.onAnimationEvent(this.projectile, eventName);
      }
   }

   public final void dispatchEventWithCallback(String eventName, Event event) {
      if (this.speed != null) {
         ;
      }
   }

   public abstract AnimationBehavior<T> getAnimationBehavior(AnimationState state);

   public final void resetState() {
      this.speed = null;
      this.active = null;
   }
}
