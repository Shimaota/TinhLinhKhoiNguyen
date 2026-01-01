package com.tlnt.world.entity.npc.component;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.world.entity.npc.NpcEntity;
import com.tlnt.world.entity.player.PlayerEntity;

/**
 * {@code ui/230} talk button on the left, level with the NPC name — port of decompiled {@code C3536xbadcbe7}: shown only when the NPC template
 * has the flag set ({@code NpcTemplate.showsTalkButton}, column npc_type) and the player stands closer than {@link #SHOW_DISTANCE};
 * size 65 px (60 while pressed), pulsing ±1; press and release INSIDE the 0.65 box around the button = talk to the NPC (op 24 sub 0).
 * {@code LoginScreen.touchDown / touchUp} call {@link #touchDown} / {@link #touchUp}.
 */
public final class NpcTalkButton extends BaseComponent {
   private static final float SIZE = 65.0F;
   private static final float PRESSED_SIZE = 60.0F;
   private static final float HALF_HIT = 0.325F;
   private static final float SHOW_DISTANCE = 1.0F;
   private static final float ABOVE = 0.25F;

   private final NpcEntity npc;
   private final Rectangle hitBox = new Rectangle();
   private boolean pressed;
   private int pressedPointer = -1;
   private boolean shown;

   public NpcTalkButton(NpcEntity npc) {
      super(npc.getEntity());
      this.npc = npc;
      this.setRenderOrder(4500000.0F);
   }

   @Override
   public final void update(float delta) {
      PlayerEntity me = PlayerEntity.getInstance();
      this.shown = this.npc.POOL2 != null && this.npc.POOL2.template != null && this.npc.POOL2.template.showsTalkButton()
         && me != null && this.npc.distanceTo(me) < SHOW_DISTANCE;
      if (!this.shown) {
         this.pressed = false;
         this.pressedPointer = -1;
      }
   }

   private float centerX() {
      return this.npc.getPosition().x - this.npc.getWidth();
   }

   private float centerY() {
      return this.npc.getPosition().y + this.npc.getHeight() + ABOVE;
   }

   @Override
   public final void render(RenderContext ctx) {
      if (!this.shown) {
         return;
      }
      float size = (this.pressed ? PRESSED_SIZE : SIZE) + pulse();
      ctx.drawTextureSized(TextureAsset.obtain("ui/230"), this.centerX(), this.centerY(), size, size, Anchor.CENTER);
   }

   /** Triangle wave -1..1, 1 s period (decompiled C3849.m17729(1, -1, 1)). */
   private static float pulse() {
      float t = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F;
      return -1.0F + (2.0F - Math.abs((t * 4.0F) % 4.0F - 2.0F));
   }

   private boolean hit(Vector2 world) {
      this.hitBox.set(this.centerX() - HALF_HIT, this.centerY() - HALF_HIT, HALF_HIT * 2.0F, HALF_HIT * 2.0F);
      return this.hitBox.contains(world.x, world.y);
   }

   /** Touch down on the button: hold, wait for release. @return true = handled (do not select a target / walk there) */
   public final boolean touchDown(Vector2 world, int pointer) {
      if (!this.shown || !this.hit(world)) {
         return false;
      }
      this.pressedPointer = pointer;
      this.pressed = true;
      return true;
   }

   /** Release (same finger that pressed): talk if still inside the button. */
   public final boolean touchUp(Vector2 world, int pointer) {
      if (!this.shown || pointer != this.pressedPointer) {
         return false;
      }
      this.pressedPointer = -1;
      this.pressed = false;
      if (this.hit(world) && this.npc.POOL2.template != null) {
         ServerPacketSender.getInstance().sendUnequipItem(this.npc.getId(), this.npc.POOL2.template.id); // op 24 sub 0
      }
      return true;
   }
}
