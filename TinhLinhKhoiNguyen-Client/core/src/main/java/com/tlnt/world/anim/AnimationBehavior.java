package com.tlnt.world.anim;

import com.tlnt.engine.scene.GameObject;

public abstract class AnimationBehavior<T extends GameObject> {
   public void onEnter(T entity) {
   }

   public void onUpdate(T entity, float delta) {
   }

   public void onExit(T entity) {
   }

   public void onAnimationEvent(T entity, String eventName) {
   }
}
