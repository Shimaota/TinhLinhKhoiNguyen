package com.tlnt.world.entity.mob;

import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.mob.MobCombatTargets;
import com.tlnt.model.mob.MobData;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.anim.mob.MobAnimDispatcher;
import com.tlnt.world.effect.BurnEffect;
import com.tlnt.world.effect.IceSpikeEffect;
import com.tlnt.world.effect.MobSkillEffect;
import com.tlnt.world.effect.MobSpawnEffect;
import com.tlnt.world.effect.RootEffect;
import com.tlnt.world.effect.StunEffect;
import com.tlnt.world.effect.TargetMarkerEffect;
import com.tlnt.world.effect.WeakenEffect;
import com.tlnt.world.effect.floating.FloatingEffectManager;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.mob.component.MobSprite;
import com.tlnt.world.entity.mob.component.MobWanderAI;
import com.tlnt.world.label.MobHealthBar;
import com.tlnt.world.label.SelectionMarker;
import com.tlnt.world.skeleton.AnimationStateDispatcher;
import com.tlnt.world.skeleton.GroundShadowRenderer;
import com.tlnt.world.skeleton.OverlayStackComponent;

public final class MobEntity extends WorldEntity {
   public MobData POOL2;
   public MobCombatTargets skeleton;
   public AnimationStateDispatcher<MobEntity> animStateDispatcher;
   public MobSprite POOL;
   public WeakenEffect owner;
   public RootEffect target;
   public IceSpikeEffect movementComponent;
   public BurnEffect target2;
   public StunEffect stunEffect;
   public MobSkillEffect skillEffect;
   public MobSpawnEffect spawnEffect;
   public TargetMarkerEffect targetMarkerEffect;
   public FloatingEffectManager floatingEffectManager;
   public MobWanderAI wanderAI;
   public OverlayStackComponent overlayStack;

   public MobEntity(LoginScreen screen, int id, float x, float y) {
      super(screen, 0, EntityType.MOB);
      this.POOL2 = new MobData();
      this.skeleton = new MobCombatTargets();
      this.teleportTo(0.0F, 0.0F);
      this.setRenderOrder(5500000.0F);
   }

   @Override
   public final void onAwake() {
      super.onAwake();
      this.wanderAI = new MobWanderAI(this);
      this.animStateDispatcher = new MobAnimDispatcher(this);
      this.POOL = new MobSprite(this);
      this.owner = new WeakenEffect(this);
      this.target = new RootEffect(this);
      this.movementComponent = new IceSpikeEffect(this);
      this.target2 = new BurnEffect(this);
      this.stunEffect = new StunEffect(this);
      this.skillEffect = new MobSkillEffect(this);
      this.spawnEffect = new MobSpawnEffect(this);
      this.targetMarkerEffect = new TargetMarkerEffect(this);
      this.floatingEffectManager = new FloatingEffectManager(this);
      new GroundShadowRenderer(this.getEntity());
      this.overlayStack = new OverlayStackComponent(this);
      this.overlayStack.addOverlay(new MobHealthBar(this));
      this.overlayStack.addOverlay(new SelectionMarker(this));
   }

   @Override
   public final void onStart() {
      this.setAnimationState(AnimationState.MOB_IDLE);
   }

   @Override
   public final boolean canChangeAnimationState(AnimationState current, AnimationState next) {
      switch (next) {
         case MOB_HURT:
            if (!current.interruptible) {
               return false;
            }
         default:
            return true;
      }
   }

   @Override
   public final void onHit(WorldEntity attacker, long currentHp, long damage, boolean isHeal) {
      if (!this.isDead()) {
         super.onHit(attacker, currentHp, damage, isHeal);
         this.POOL2.currentHp = currentHp;
         if (this.POOL2.currentHp < 0L) {
            this.POOL2.currentHp = 0L;
         }

         if (this.isAlive()) {
            if (damage > 0L) {
               if (this.wanderAI == null) {
                  return; // hit packet arrived before onAwake (mob spawned in the same frame): only update HP
               }
               this.wanderAI.startWaitPause();
               this.setAnimationState(AnimationState.MOB_HURT);
               return;
            }
         } else {
            this.setAnimationState(AnimationState.MOB_DIE);
            this.reset();
         }
      }
   }

   @Override
   public final boolean isDead() {
      if (this.POOL2.currentHp <= 0L) {
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean isAlive() {
      if (this.POOL2.currentHp > 0L) {
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final float getHpPercent() {
      return this.POOL2 == null ? 0.0F : (float)this.POOL2.currentHp / (float)this.POOL2.maxHp;
   }

   @Override
   public final void render(RenderContext ctx) {
   }

   @Override
   public final float getWidth() {
      return this.POOL != null ? this.POOL.getSkeletonWidth() : 0.0F;
   }

   @Override
   public final float getHeight() {
      return this.POOL != null ? this.POOL.getSkeletonHeight() : 0.0F;
   }

   @Override
   public final boolean containsPoint(Vector2 point) {
      if (this.POOL != null && this.POOL.containsPoint(point)) {
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final void reset() {
      if (this.owner != null) {
         this.owner.stopWeaken();
      }

      if (this.target != null) {
         this.target.stopRoot();
      }

      if (this.movementComponent != null) {
         this.movementComponent.stopIceSpike();
      }

      if (this.target2 != null) {
         this.target2.stopBurn();
      }

      if (this.stunEffect != null) {
         this.stunEffect.stopStun();
      }
   }

   @Override
   public final void dispose() {
      if (this.POOL2 != null) {
         MobData mobData = this.POOL2;
         this.POOL2.spawnPosition = null;
         mobData.template = null;
         this.POOL2 = null;
      }

      this.wanderAI = null;
      this.overlayStack = null;
   }
}
