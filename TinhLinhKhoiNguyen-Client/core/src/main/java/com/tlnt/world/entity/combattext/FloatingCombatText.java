package com.tlnt.world.entity.combattext;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.badlogic.gdx.math.Interpolation;
import com.tlnt.core.audio.FontManager;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.combattext.FloatingCombatTextPool;

public final class FloatingCombatText extends GameObject {
   public static final Pool<FloatingCombatText> POOL2 = new FloatingCombatTextPool();
   public int skeleton = FloatingCombatText.CombatTextType.NORMAL;
   public String POOL;
   public BitmapFont owner;
   public float target;
   public float movementComponent;
   public Color target2 = Color.WHITE;
   public final Vector2 startPosition = new Vector2();
   public final Vector2 targetPosition = new Vector2();
   public final Vector3 worldPosition = new Vector3();
   public byte animPhase;
   public float elapsed;
   public float phaseDuration;

   public FloatingCombatText(LoginScreen screen) {
      super(screen, EntityType.DEFAULT);
      this.setRenderOrder(1.0E7F);
   }

   public final void init(String text, int type, Vector2 position) {
       this.setRenderOrder(1.0E7f + (float)System.currentTimeMillis() / 1000.0f % 100.0f);
       this.POOL = text;
       this.skeleton = type;
       this.startPosition.set(position);
       this.animPhase = (byte)0;
       this.elapsed = 0.0f;
       switch (this.skeleton) {
           case CombatTextType.MISS: {
               this.owner = FontManager.EXP_GAIN_FONT;
               this.targetPosition.set(this.startPosition).add(0.0f, GameUtils.randomFloat(1.0f, 1.0f));
               this.phaseDuration = 0.8f;
               this.target = 1.0f;
               this.movementComponent = 1.0f;
               return;
           }
           case CombatTextType.CRITICAL: {
               this.owner = FontManager.DAMAGE_FONT;
               this.targetPosition.set(this.startPosition).add(0.0f, GameUtils.randomFloat(1.0f, 1.3f));
               this.phaseDuration = 0.5f;
               this.target = 1.0f;
               this.movementComponent = 1.0f;
               return;
           }
           case CombatTextType.HEAL: {
               this.owner = FontManager.CRIT_DAMAGE_FONT;
               this.targetPosition.set(this.startPosition).add(0.0f, GameUtils.randomFloat(1.0f, 1.3f));
               this.phaseDuration = 0.5f;
               this.target = 1.5f;
               this.movementComponent = 1.0f;
               return;
           }
           case CombatTextType.BONUS: {
               this.owner = FontManager.MISS_TEXT_FONT;
               this.targetPosition.set(this.startPosition).add(0.0f, GameUtils.randomFloat(0.7f, 1.0f));
               this.phaseDuration = 0.5f;
               this.target = 1.0f;
               this.movementComponent = 1.0f;
           }
       }
   }

   @Override
   public final void update(float delta) {
       this.elapsed += delta;
       block0 : switch (this.skeleton) {
           case CombatTextType.MISS: {
               switch (this.animPhase) {
                   case 0: {
                       float progressMiss;
                       delta = this.elapsed / this.phaseDuration;
                       if (delta >= 1.0f) {
                           this.elapsed = 0.0f;
                           this.phaseDuration = 0.5f;
                           this.animPhase = (byte)(this.animPhase + 1);
                           break;
                       }
                       this.target = Interpolation.fastSlow.apply(0.7f, 1.2f, delta);
                       this.worldPosition.set(Interpolation.fastSlow.apply(this.startPosition.x, this.targetPosition.x, delta), Interpolation.fastSlow.apply(this.startPosition.y, this.targetPosition.y, delta), 0.0f);
                       break block0;
                   }
                   case 1: {
                       delta = this.elapsed / this.phaseDuration;
                       this.worldPosition.set(this.targetPosition, 0.0f);
                       if (delta >= 1.0f) {
                           this.returnToPool();
                           break block0;
                       }
                       this.target = Interpolation.fastSlow.apply(1.2f, 0.7f, delta);
                       this.movementComponent = Interpolation.fastSlow.apply(1.0f, 0.0f, delta);
                   }
               }
               return;
           }
           case CombatTextType.HEAL: {
               switch (this.animPhase) {
                   case 0: {
                       float progressHeal;
                       delta = this.elapsed / this.phaseDuration;
                       if (delta >= 1.0f) {
                           this.elapsed = 0.0f;
                           this.phaseDuration = 0.5f;
                           this.animPhase = (byte)(this.animPhase + 1);
                           break;
                       }
                       this.target = Interpolation.fastSlow.apply(1.0f, 2.2f, delta);
                       this.worldPosition.set(Interpolation.fastSlow.apply(this.startPosition.x, this.targetPosition.x, delta), Interpolation.fastSlow.apply(this.startPosition.y, this.targetPosition.y, delta), 0.0f);
                       break block0;
                   }
                   case 1: {
                       delta = this.elapsed / this.phaseDuration;
                       this.worldPosition.set(this.targetPosition, 0.0f);
                       if (delta >= 1.0f) {
                           this.returnToPool();
                           break block0;
                       }
                       this.target = Interpolation.fastSlow.apply(2.2f, 1.0f, delta);
                       this.movementComponent = Interpolation.fastSlow.apply(1.0f, 0.0f, delta);
                   }
               }
               return;
           }
           case CombatTextType.CRITICAL: {
               switch (this.animPhase) {
                   case 0: {
                       float progressCrit;
                       delta = this.elapsed / this.phaseDuration;
                       if (delta >= 1.0f) {
                           this.elapsed = 0.0f;
                           this.phaseDuration = 0.5f;
                           this.animPhase = (byte)(this.animPhase + 1);
                           break;
                       }
                       this.target = Interpolation.fastSlow.apply(1.0f, 2.0f, delta);
                       this.worldPosition.set(Interpolation.fastSlow.apply(this.startPosition.x, this.targetPosition.x, delta), Interpolation.fastSlow.apply(this.startPosition.y, this.targetPosition.y, delta), 0.0f);
                       break block0;
                   }
                   case 1: {
                       delta = this.elapsed / this.phaseDuration;
                       this.worldPosition.set(this.targetPosition, 0.0f);
                       if (delta >= 1.0f) {
                           this.returnToPool();
                           break block0;
                       }
                       this.target = Interpolation.fastSlow.apply(2.0f, 1.0f, delta);
                       this.movementComponent = Interpolation.fastSlow.apply(1.0f, 0.0f, delta);
                   }
               }
               return;
           }
           case CombatTextType.BONUS: {
               switch (this.animPhase) {
                   case 0: {
                       float progressBonus;
                       delta = this.elapsed / this.phaseDuration;
                       if (delta >= 1.0f) {
                           this.elapsed = 0.0f;
                           this.phaseDuration = 0.5f;
                           this.animPhase = (byte)(this.animPhase + 1);
                           break block0;
                       }
                       this.target = Interpolation.fastSlow.apply(0.5f, 1.2f, delta);
                       this.worldPosition.set(Interpolation.fastSlow.apply(this.startPosition.x, this.targetPosition.x, delta), Interpolation.fastSlow.apply(this.startPosition.y, this.targetPosition.y, delta), 0.0f);
                       return;
                   }
                   case 1: {
                       delta = this.elapsed / this.phaseDuration;
                       this.worldPosition.set(this.targetPosition, 0.0f);
                       if (delta >= 1.0f) {
                           this.returnToPool();
                           return;
                       }
                       this.target = Interpolation.fastSlow.apply(1.2f, 0.5f, delta);
                       this.movementComponent = Interpolation.fastSlow.apply(1.0f, 0.0f, delta);
                   }
               }
           }
       }
   }

   @Override
   public final void render(RenderContext ctx) {
      ctx.worldToHud(this.worldPosition);
      ctx.useHudCamera();
      ctx.drawTextRgba(
         this.owner,
         this.POOL,
         this.worldPosition.x,
         this.worldPosition.y,
         Anchor.BOTTOM_CENTER,
         32.0F * this.target,
         this.target2.r,
         this.target2.g,
         this.target2.b,
         this.movementComponent
      );
      ctx.usePreviousCamera();
   }

   public void returnToPool() {
      this.setActive(false);
      POOL2.free(this);
   }

   @Override
   public final void dispose() {
   }

   public static final class CombatTextType {
      public static final int NORMAL = 1;
      public static final int MISS = 2;
      public static final int CRITICAL = 3;
      public static final int HEAL = 4;
      public static final int BONUS = 5;
      public static final int[] VALUES;

      public static int[] values() {
         return (int[])VALUES.clone();
      }

      static {
         int[] values = new int[5];
         values[0] = NORMAL;
         values[1] = MISS;
         values[2] = CRITICAL;
         values[3] = HEAL;
         values[4] = BONUS;
         VALUES = values;
      }
   }
}
