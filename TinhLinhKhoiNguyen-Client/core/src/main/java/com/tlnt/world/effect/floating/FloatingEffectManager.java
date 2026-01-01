package com.tlnt.world.effect.floating;

import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.world.entity.WorldEntity;

public final class FloatingEffectManager extends BaseComponent {
   public GameObject projectile;
   public Array<PooledFloatingEffect> speed = new Array<>();

   public FloatingEffectManager(WorldEntity owner) {
      super(owner.getEntity());
      this.projectile = owner;
      this.setRenderOrder(this.projectile.getRenderOrder());
   }

   public final void addEffect(PooledFloatingEffect effect) {
      this.speed.add(effect);
   }

   @Override
   public final void update(float delta) {

      for (int i = this.speed.size - 1; i >= 0; i--) {
         PooledFloatingEffect effect;
         (effect = this.speed.get(i)).update(delta);
         if (!effect.isAlive) {
            this.speed.removeValue(effect, true);
            // Effects never return to the pool (the original left it to the GC) -> give the skeleton back to SkeletonLoader, otherwise the pair in
            // use is held forever (2026-10-19: 2536 live Skeletons) and effect atlases are never freed.
            if (effect.skeletonPair != null) {
               com.tlnt.core.asset.SkeletonLoader.releaseSkeleton(effect.skeletonPair);
               effect.skeletonPair = null;
               effect.skeleton = null;
               effect.animationState = null;
            }
         }
      }
   }

   @Override
   public final void render(RenderContext ctx) {
      Array.ArrayIterator it = this.speed.iterator();

      while (it.hasNext()) {
         ((PooledFloatingEffect)it.next())
            .render(ctx, this.projectile.getPosition().x, this.projectile.getPosition().y + this.projectile.getHeight() / 2.0F);
      }
   }
}
