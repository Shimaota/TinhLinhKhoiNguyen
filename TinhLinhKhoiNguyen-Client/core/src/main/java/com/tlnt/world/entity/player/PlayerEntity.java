package com.tlnt.world.entity.player;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.tlnt.core.audio.CollisionFilters;
import com.tlnt.engine.input.InputHandler;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.BodyType;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.physics.collider.CapsuleCollider;
import com.tlnt.engine.physics.collider.PhysicsMaterial;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.player.PlayerCombatTargets;
import com.tlnt.model.player.PlayerStats;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.anim.player.PlayerAnimDispatcher;
import com.tlnt.world.effect.BurnEffect;
import com.tlnt.world.effect.IceSpikeEffect;
import com.tlnt.world.effect.MobSkillEffect;
import com.tlnt.world.effect.ReviveEffect;
import com.tlnt.world.effect.RootEffect;
import com.tlnt.world.effect.StunEffect;
import com.tlnt.world.effect.TargetMarkerEffect;
import com.tlnt.world.effect.WeakenEffect;
import com.tlnt.world.effect.floating.FloatingEffectManager;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.player.component.AutoTargetController;
import com.tlnt.world.entity.player.component.ClickMoveController;
import com.tlnt.world.entity.player.component.DashImpulseController;
import com.tlnt.world.entity.player.component.GlowAuraComponent;
import com.tlnt.world.entity.player.component.HarvestProgressBar;
import com.tlnt.world.entity.player.component.InteractionController;
import com.tlnt.world.entity.player.component.ItemPickupDisplay;
import com.tlnt.world.entity.player.component.OffscreenTargetArrow;
import com.tlnt.world.entity.player.component.PlayerMovementController;
import com.tlnt.world.entity.player.component.QuestCompleteEffect;
import com.tlnt.world.entity.player.component.TransparentBodyOverlay;
import com.tlnt.world.entity.player.component.WaypointContactListener;
import com.tlnt.world.entity.player.component.TitleAuraEffect;
import com.tlnt.world.entity.player.component.WeaponEquipEffect;
import com.tlnt.world.label.GuildNameLabel;
import com.tlnt.world.label.PlayerHealthBar;
import com.tlnt.world.label.PlayerNameLabel;
import com.tlnt.world.label.SelectionMarker;
import com.tlnt.world.label.TitleSkeletonLabel;
import com.tlnt.world.skeleton.AnimationStateDispatcher;
import com.tlnt.world.skeleton.GroundShadowRenderer;
import com.tlnt.world.skeleton.OverlayStackComponent;

public final class PlayerEntity extends WorldEntity {
   public static PlayerEntity instance;
   public boolean isLocalPlayer;
   public PlayerStats POOL2 = new PlayerStats();
   public PlayerCombatTargets skeleton = new PlayerCombatTargets();
   public BodyComponent POOL;
   public CapsuleCollider bodyCollider;
   public ClickMoveController owner;
   public DashImpulseController target;
   public AnimationStateDispatcher<PlayerEntity> animDispatcher;
   public TransparentBodyOverlay movementComponent;
   public WeaponEquipEffect target2;
   public WeaponEquipEffect secondaryWeaponEffect;
   public WeakenEffect weakenEffect;
   /** Sun / Moon Seal title halo ({@link #refreshTitleAura}). */
   public TitleAuraEffect titleAura;
   public RootEffect rootEffect;
   public IceSpikeEffect iceSpikeEffect;
   public BurnEffect burnEffect;
   public StunEffect stunEffect;
   public MobSkillEffect mobSkillEffect;
   public ReviveEffect reviveEffect;
   public TargetMarkerEffect targetMarkerEffect;
   public FloatingEffectManager floatingEffectManager;
   public ChatBubbleComponent chatBubble;
   public OverlayStackComponent overlayStack;
   public GroundShadowRenderer groundShadow;
   public ItemPickupDisplay itemPickupDisplay;
   public WaypointContactListener waypointListener;
   public PlayerMovementController movementController;
   public AutoTargetController autoTargetController;
   public InteractionController interactionController;
   public HarvestProgressBar harvestProgressBar;
   public OffscreenTargetArrow offscreenArrow;
   /** Op 63 sub 2: the server guides the character to the middle of this area (until the player moves on their own). */
   public boolean serverPathActive;
   public final com.badlogic.gdx.math.Vector2 serverPathTarget = new com.badlogic.gdx.math.Vector2();
   public final com.badlogic.gdx.math.Rectangle serverPathArea = new com.badlogic.gdx.math.Rectangle();
   private final com.badlogic.gdx.math.Vector2 serverPathFeet = new com.badlogic.gdx.math.Vector2();
   public QuestCompleteEffect questCompleteEffect;
   public boolean isStealthed;

   public static PlayerEntity getInstance() {
      return instance;
   }

   public static void setAsLocalPlayer(PlayerEntity player) {
      PlayerEntity localPlayer = instance = player;
      localPlayer.isLocalPlayer = true;
      if (localPlayer.POOL != null && localPlayer.POOL.getBody() != null) {
         localPlayer.POOL.getBody().setType(BodyDef.BodyType.DynamicBody);
         localPlayer.setSensorEnabled(false);
         localPlayer.POOL.getBody().setAwake(true);
      }
      if (instance.movementController == null) {
         instance.movementController = new PlayerMovementController(instance);
      }
      if (instance.movementController.physicsBody == null && instance.POOL != null) {
         instance.movementController.physicsBody = instance.POOL.getBody();
      }

      if (instance.waypointListener == null) {
         instance.waypointListener = new WaypointContactListener(instance);
      }

      if (instance.autoTargetController == null) {
         instance.autoTargetController = new AutoTargetController(instance);
      }

      if (instance.interactionController == null) {
         instance.interactionController = new InteractionController(instance);
      }

      if (instance.harvestProgressBar == null) {
         instance.harvestProgressBar = new HarvestProgressBar(instance);
      }

      if (instance.offscreenArrow == null) {
         instance.offscreenArrow = new OffscreenTargetArrow(instance);
      }

      if (instance.questCompleteEffect == null) {
         instance.questCompleteEffect = new QuestCompleteEffect(instance);
      }
   }

   public PlayerEntity(LoginScreen screen, int id, float x, float y) {
      super(screen, 0, EntityType.PLAYER);
      this.teleportTo(0.0F, 0.0F);
      this.setRenderOrder(6000000.0F);
   }

   @Override
   public final void onDisabled() {
      WeaponEquipEffect weaponEffect = this.target2;
      this.target2.setEnabled(false);
      weaponEffect = this.secondaryWeaponEffect;
      this.secondaryWeaponEffect.setEnabled(false);
   }

   @Override
   public final void onAwake() {
      super.onAwake();
      this.POOL = new BodyComponent(BodyType.STATIC, this.getEntity()).setInitialPosition(this.getPosition());
      this.bodyCollider = new CapsuleCollider(new Vector2(0.0F, 75.0F), 70.0F, 150.0F, this.getEntity());
      this.bodyCollider.setMaterial(new PhysicsMaterial(0.0F, 0.0F, 0.0F));
      this.bodyCollider.setFilterData(CollisionFilters.PLAYER);
      this.owner = new ClickMoveController(this);
      this.target = new DashImpulseController(this);
      this.animDispatcher = new PlayerAnimDispatcher(this);
      this.movementComponent = new TransparentBodyOverlay(this);
      this.target2 = new WeaponEquipEffect(this);
      this.secondaryWeaponEffect = new WeaponEquipEffect(this);
      this.weakenEffect = new WeakenEffect(this);
      this.titleAura = new TitleAuraEffect(this);
      this.rootEffect = new RootEffect(this);
      this.iceSpikeEffect = new IceSpikeEffect(this);
      this.burnEffect = new BurnEffect(this);
      this.stunEffect = new StunEffect(this);
      this.mobSkillEffect = new MobSkillEffect(this);
      this.reviveEffect = new ReviveEffect(this);
      this.targetMarkerEffect = new TargetMarkerEffect(this);
      this.floatingEffectManager = new FloatingEffectManager(this);
      this.chatBubble = new ChatBubbleComponent(this);
      this.overlayStack = new OverlayStackComponent(this);
      this.overlayStack.addOverlay(new PlayerNameLabel(this));
      this.overlayStack.addOverlay(new GuildNameLabel(this));
      this.overlayStack.addOverlay(new PlayerHealthBar(this));
      this.overlayStack.addOverlay(new SelectionMarker(this));
      this.overlayStack.addOverlay(new TitleSkeletonLabel(this));
      this.groundShadow = new GroundShadowRenderer(this.getEntity());
      this.itemPickupDisplay = new ItemPickupDisplay(this);
      new GlowAuraComponent(this.getEntity());
   }

   @Override
   public final void onStart() {
      if (this.getAnimationState() == AnimationState.DEFAULT) {
         this.setAnimationState(AnimationState.IDLE);
      }

      if (this.isLocalPlayer) {
         if (this.POOL != null && this.POOL.getBody() != null) {
            this.POOL.getBody().setType(BodyDef.BodyType.DynamicBody);
            this.POOL.getBody().setAwake(true);
         }
      }

      int sensorEnabled;
      if (!this.isLocalPlayer) {
         sensorEnabled = 1;
      } else {
         sensorEnabled = 0;
      }

      this.setSensorEnabled((sensorEnabled != 0));
   }

   @Override
   public final boolean canChangeAnimationState(AnimationState current, AnimationState next) {
      switch (next) {
         case HURT:
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
               this.setAnimationState(AnimationState.HURT);
               return;
            }
         } else {
            this.setAnimationState(AnimationState.DIE);
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

   /** Op 63 sub 2 (decompiled C3540 m16491): walk to the middle of the area (x, y, width, height) in world units. */
   public final void startServerPath(float x, float y, float width, float height) {
      this.serverPathTarget.set(x + width / 2.0F, y + height / 2.0F);
      this.serverPathArea.set(x, y, width, height);
      this.serverPathActive = true;
   }

   /** The player takes control: stop auto-pathing + stop following the server (decompiled C3555). */
   public final void stopAutoPath() {
      this.serverPathActive = false;
      if (this.offscreenArrow != null) {
         this.offscreenArrow.setAutoPathing(false);
      }
   }

   @Override
   public final void update(float delta) {
      // decompiled m16485: being guided by the server and not there yet (≥ 1.5) -> keep walking
      if (this.serverPathActive && this.movementController != null && !this.serverPathArea.contains(this.getPosition())
         && this.getPosition().dst(this.serverPathTarget) >= 1.5F) {
         this.serverPathFeet.set(this.serverPathTarget.x, this.serverPathArea.y + 0.1F); // foot of the area (see OffscreenTargetArrow)
         this.movementController.moveTowardTarget(this.serverPathFeet, null);
      }

      InputHandler.getInstance();
      if (InputHandler.isKeyJustPressed(39)) {
         ;
      }

      InputHandler.getInstance();
      InputHandler.isKeyJustPressed(41);
   }

   public final boolean isLocalPlayer() {
      return this.isLocalPlayer;
   }

   public final void setIsLocalPlayer(boolean isLocalPlayer) {
      this.isLocalPlayer = isLocalPlayer;
   }

   @Override
   public final void render(RenderContext ctx) {
   }

   public final String getCharacterName() {
      return this.POOL2 != null ? this.POOL2.characterName : "";
   }

   @Override
   public final float getWidth() {
      return this.movementComponent != null ? this.movementComponent.getSkeletonWidth() : 0.0F;
   }

   @Override
   public final float getHeight() {
      return this.movementComponent != null ? this.movementComponent.getSkeletonHeight() : 0.0F;
   }

   @Override
   public final boolean containsPoint(Vector2 point) {
      if (this.movementComponent != null && this.movementComponent.containsPoint(point)) {
         return true;
      } else {
         return false;
      }
   }

   public final boolean isMountClass() {
      if (this.POOL2 != null && this.POOL2.classId == 8) {
         return true;
      } else {
         return false;
      }
   }

   /** After reading the stats block (op 3 / 1 / 23): enable / disable the halo according to the title (decompiled C3873, end of readStats). */
   public final void refreshTitleAura() {
      if (this.titleAura != null) {
         this.titleAura.refresh();
      }
   }

   public final void setStealthed(boolean stealthed) {
      this.isStealthed = stealthed;
      if (!this.isLocalPlayer) {
         int visible;
         if (!this.isStealthed) {
            visible = 1;
         } else {
            visible = 0;
         }

         this.overlayStack.setVisible((visible != 0));
         if (!this.isStealthed) {
            visible = 1;
         } else {
            visible = 0;
         }

         this.movementComponent.setVisible((visible != 0));
         if (!this.isStealthed) {
            visible = 1;
         } else {
            visible = 0;
         }

         this.groundShadow.setVisible((visible != 0));
      } else {
         this.movementComponent.setTintAlpha(1.0F, 1.0F, 1.0F, this.isStealthed ? 0.2F : 1.0F);
      }
   }

   @Override
   public final void reset() {
      if (this.weakenEffect != null) {
         this.weakenEffect.stopWeaken();
      }

      if (this.titleAura != null) {
         this.titleAura.clear();
      }

      if (this.rootEffect != null) {
         this.rootEffect.stopRoot();
      }

      if (this.iceSpikeEffect != null) {
         this.iceSpikeEffect.stopIceSpike();
      }

      if (this.burnEffect != null) {
         this.burnEffect.stopBurn();
      }

      if (this.stunEffect != null) {
         this.stunEffect.stopStun();
      }

      if (this.target2 != null) {
         WeaponEquipEffect weaponEffect = this.target2;
         this.target2.setEnabled(false);
      }

      if (this.secondaryWeaponEffect != null) {
         WeaponEquipEffect secondaryEffect = this.secondaryWeaponEffect;
         this.secondaryWeaponEffect.setEnabled(false);
      }

      this.POOL2.isInParty = false;
   }

   @Override
   public final void dispose() {
   }
}
