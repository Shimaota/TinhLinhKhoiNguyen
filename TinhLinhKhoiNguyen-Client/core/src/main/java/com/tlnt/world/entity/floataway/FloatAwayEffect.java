package com.tlnt.world.entity.floataway;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameMain;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.pool.ObjectPool;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.model.mapeffect.MapEffectTemplate;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.decoration.MapDecorationObject;
import com.tlnt.world.entity.decoration.WorldMapEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

/**
 * Floating-away object (op 80 — port of decompiled C3524 / C0063 / C0064 / C3427), e.g. a lotus lantern = {@code map_effect} template 1:
 * the {@link MapEffectTemplate} skeleton (scale 0.175, looping "animation") drifts horizontally at {@code speed} world units
 * per second, bobbing sin(t × 0.5) × 0.15; once past the x mark {@code endX} it disappears. Everything is cleared on map change ({@link #clearAll}).
 *
 * <p>Components are created RIGHT in the constructor (the GameObject's onAwake runs one tick late — the first drop used to have no
 * skeleton and showed nothing). Draw layer: after all water tiles; trees and decorations on higher layers can still cover the lantern.
 */
public final class FloatAwayEffect extends GameObject {
   public static final Array<FloatAwayEffect> ACTIVE = new Array<>();
   private static final ObjectPool<FloatAwayEffect> POOL = new ObjectPool<FloatAwayEffect>() {
      @Override
      public FloatAwayEffect newObject() {
         return new FloatAwayEffect(GameMain.instance.getLoginScreen());
      }
   };
   /** Default render mark if the map has no recognized water tile. */
   private static final float FRONT_LAYER_ORDER = 9000000.0F;

   final SpineSkeletonComponent skeleton;
   final Drift drift;
   /** Skeleton being loaded (reloaded when the template changes). */
   private String loadedPath;
   /** Placement finished (decompiled f13037): only then is the disappear mark checked. */
   boolean ready;

   public FloatAwayEffect(LoginScreen screen) {
      super(screen);
      this.skeleton = new SpineSkeletonComponent(this.getEntity());
      this.drift = new Drift(this);
   }

   /** Op 80 sub 0: entity id, template id, position (world units), x mark where it disappears, drift speed. */
   public static void spawn(int id, int templateId, float x, float y, float endX, float speed) {
      MapEffectTemplate template = MapEffectTemplate.getById(templateId);
      if (template == null) {
         return;
      }
      FloatAwayEffect effect = POOL.obtain();
      effect.setActive(true);
      effect.setId(id);
      effect.teleportTo(x, y);
      if (!template.skeletonPath.equals(effect.loadedPath)) {
         effect.skeleton.requestLayerChange(template.skeletonPath, 0.175F, new SkeletonLoader.AnimationMixConfig());
         effect.loadedPath = template.skeletonPath;
      }
      effect.skeleton.playAnimation("animation", true, 1.0F);
      effect.skeleton.setRenderOrder(renderOrderAboveWater());
      effect.skeleton.setEnabled(true);
      effect.drift.start(speed, endX);
      effect.ready = true;
      ACTIVE.add(effect);
   }

   /** Orders the lantern after the water tile with the highest render order on the map, regardless of the tile's layer. */
   static float renderOrderAboveWater() {
      float fallbackOrder = FRONT_LAYER_ORDER + 1.0F;
      LoginScreen screen = GameMain.instance == null ? null : GameMain.instance.getLoginScreen();
      WorldMapEntity map = screen == null ? null : screen.editorViewport;
      if (map == null || map.target == null) {
         return fallbackOrder;
      }

      float lastFrontLayerOrder = FRONT_LAYER_ORDER;
      float lastWaterLayerOrder = Float.NEGATIVE_INFINITY;
      for (MapDecorationObject decoration : map.target) {
         if (decoration.getRenderOrder() >= FRONT_LAYER_ORDER) {
            lastFrontLayerOrder = Math.max(lastFrontLayerOrder, decoration.getRenderOrder());
         }
         if (decoration.skeleton != MapDecorationObject.DecorationType.ANIMATED_TILE
               || decoration.owner == null
               || (decoration.owner.id != 3 && decoration.owner.id != 4)) {
            continue;
         }
         lastWaterLayerOrder = Math.max(lastWaterLayerOrder, decoration.getRenderOrder());
      }
      if (lastWaterLayerOrder != Float.NEGATIVE_INFINITY) {
         return lastWaterLayerOrder + 1.0F;
      }
      // No recognizable water animation: avoid the lantern being covered by decorations in front.
      return Math.max(fallbackOrder, lastFrontLayerOrder + 1.0F);
   }

   static void free(FloatAwayEffect effect) {
      ACTIVE.removeValue(effect, true);
      effect.ready = false;
      effect.skeleton.setEnabled(false);
      effect.setActive(false);
      POOL.free(effect);
   }

   /** Map change: drop everything. */
   public static void clearAll() {
      while (ACTIVE.size > 0) {
         free(ACTIVE.peek());
      }
   }

   @Override
   public final void dispose() {
   }

   /** Horizontal drift + bobbing (decompiled C0064). */
   static final class Drift extends BaseComponent {
      private final FloatAwayEffect effect;
      private float speed = 0.5F;
      private float endX;
      private float time;
      private float lastBob;

      Drift(FloatAwayEffect effect) {
         super(effect.getEntity());
         this.effect = effect;
      }

      void start(float speed, float endX) {
         this.speed = speed;
         this.endX = endX;
         this.time = 0.0F;
         this.lastBob = 0.0F;
      }

      @Override
      public final void update(float delta) {
         if (!this.effect.ready) {
            return;
         }
         this.time += delta;
         float bob = (float)Math.sin(this.time * 0.5F) * 0.15F;
         float dy = bob - this.lastBob;
         this.lastBob = bob;
         this.effect.setWorldPosition(this.effect.getPosition().add(this.speed * delta, dy));
         if (this.effect.getPosition().x >= this.endX) {
            FloatAwayEffect.free(this.effect);
         }
      }
   }
}
