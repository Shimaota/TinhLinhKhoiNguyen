package com.tlnt.world.skeleton;

import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.world.entity.WorldEntity;

public final class OverlayStackComponent extends BaseComponent {
   public WorldEntity projectile;
   public Vector3 speed = new Vector3();
   public Array<FloatingOverlay> active;
   public boolean autoFlipEnabled = false;

   public OverlayStackComponent(WorldEntity worldEntity) {
      super(worldEntity.getEntity());
      this.projectile = worldEntity;
      this.active = new Array<>();
      this.setRenderOrder(this.projectile.getRenderOrder() + 1.0F);
   }

   public final void addOverlay(FloatingOverlay overlay) {
      this.active.add(overlay);
   }

   @Override
   public final void update(float delta) {
      this.speed.set(this.projectile.getPosition(), 0.0F).add(0.0F, this.projectile.getHeight(), 0.0F);
      Array.ArrayIterator iterator = this.active.iterator();

      while (iterator.hasNext()) {
         ((FloatingOverlay)iterator.next()).refresh();
      }
   }

   @Override
   public final void render(RenderContext ctx) {
      ctx.worldToHud(this.speed);
      ctx.useHudCamera();
      Array.ArrayIterator iterator = this.active.iterator();

      while (iterator.hasNext()) {
         FloatingOverlay overlay;
         if ((overlay = (FloatingOverlay)iterator.next()).isVisible()) {
            overlay.render(ctx, this.speed.x, this.speed.y + overlay.size.y / 2.0F);
            this.speed.add(0.0F, overlay.size.y, 0.0F);
         }
      }

      ctx.usePreviousCamera();
   }

   @Override
   public final void onDestroy() {
      this.projectile = null;
      this.speed = null;
      Array.ArrayIterator iterator = this.active.iterator();

      while (iterator.hasNext()) {
         ((FloatingOverlay)iterator.next()).release();
      }

      this.active.clear();
      this.active = null;
   }
}
