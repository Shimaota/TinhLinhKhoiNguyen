package com.tlnt.world.entity.npc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.npc.NpcState;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.anim.npc.NpcAnimDispatcher;
import com.tlnt.world.effect.TargetMarkerEffect;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.npc.component.NpcSprite;
import com.tlnt.world.entity.player.ChatBubbleComponent;
import com.tlnt.world.label.NpcBuildingStatusLabel;
import com.tlnt.world.label.NpcReadyMarker;
import com.tlnt.world.label.SelectionMarker;
import com.tlnt.world.skeleton.AnimationStateDispatcher;
import com.tlnt.world.skeleton.GroundShadowRenderer;
import com.tlnt.world.skeleton.OverlayStackComponent;

public final class NpcEntity extends WorldEntity {
   public NpcState POOL2 = new NpcState();
   public AnimationStateDispatcher<NpcEntity> target;
   public NpcSprite skeleton;
   public TargetMarkerEffect movementComponent;
   public ChatBubbleComponent POOL;
   public OverlayStackComponent target2;
   public GroundShadowRenderer owner;
   /** ui/230 talk button (decompiled C3536). */
   public com.tlnt.world.entity.npc.component.NpcTalkButton talkButton;

   public NpcEntity(LoginScreen screen, int id, float x, float y) {
      super(screen, 0, EntityType.NPC);
      this.teleportTo(0.0F, 0.0F);
      this.setRenderOrder(4500000.0F);
   }

   @Override
   public final void onEnable() {
      Gdx.app.postRunnable(new NpcOnEnableRunnable(this));
   }

   @Override
   public final void onAwake() {
      super.onAwake();
      this.target = new NpcAnimDispatcher(this);
      this.skeleton = new NpcSprite(this);
      this.movementComponent = new TargetMarkerEffect(this);
      this.POOL = new ChatBubbleComponent(this);
      this.target2 = new OverlayStackComponent(this);
      this.target2.addOverlay(new NpcBuildingStatusLabel(this));
      this.target2.addOverlay(new SelectionMarker(this));
      this.target2.addOverlay(new NpcReadyMarker(this));
      this.owner = new GroundShadowRenderer(this.getEntity());
      this.talkButton = new com.tlnt.world.entity.npc.component.NpcTalkButton(this);
   }

   @Override
   public final void render(RenderContext ctx) {
   }

   @Override
   public final void onStart() {
      this.setAnimationState(AnimationState.NPC_IDLE);
   }

   public final String getTemplateName() {
      return this.POOL2 != null && this.POOL2.template != null ? this.POOL2.template.name : "";
   }

   @Override
   public final float getWidth() {
      return this.skeleton != null ? this.skeleton.getSkeletonWidth() : 0.0F;
   }

   @Override
   public final float getHeight() {
      return this.skeleton != null ? this.skeleton.getSkeletonHeight() : 0.0F;
   }

   @Override
   public final boolean containsPoint(Vector2 point) {
      if (this.skeleton != null && this.skeleton.containsPoint(point)) {
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final void dispose() {
      this.skeleton = null;
   }
}
