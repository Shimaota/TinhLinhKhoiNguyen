package com.tlnt.ui.window.worldboss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.model.character.CharacterTemplate;
import com.tlnt.model.mob.MobTemplate;
import com.tlnt.model.worldboss.WorldBossInfo;
import com.tlnt.thirdparty.spine.Animation;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.SkeletonData;
import com.tlnt.thirdparty.spine.Skin;
import com.tlnt.ui.widget.BaseActor;
import com.tlnt.util.GameUtils;

/**
 * Static boss image ("idle" animation) in the World Boss table. Mob boss = skeleton + variant-0 skin of
 * {@link MobTemplate}; fake-player boss = {@link CharacterTemplate} skeleton + skin sent by the server. Scales itself to fit the frame
 * using the REAL bounds of the idle pose (measured over every attachment vertex, weapons included), clipped to the frame.
 */
final class BossPreview extends BaseActor {
   private String path;
   private float loadScale;
   private String[] skins = new String[0];
   private long lastAttempt;
   private NamedPair<AnimationState, Skeleton> pair;
   private float fitScale;
   private float offsetX;
   private float offsetY;
   /** Character draw scale in the Info window ({@code CharacterModelActor.skeletonScale}, skeleton loaded at 0.25). */
   static final float CHAR_SCALE = 150.0F;
   /** Fake-player boss (aligned on the body, not including the weapon). */
   private boolean character;

   BossPreview(RenderContext ctx) {
      super(ctx);
   }

   /** Changes the boss (same boss -> kept as is, no reload). */
   void show(WorldBossInfo boss) {
      String newPath;
      float newScale;
      String[] newSkins;
      if (boss == null) {
         newPath = null;
         newScale = 0.0F;
         newSkins = new String[0];
      } else if (boss.character) {
         CharacterTemplate template = CharacterTemplate.getById(boss.templateId);
         newPath = template != null ? template.skeletonPath : "chars/" + boss.templateId;
         newScale = 0.25F;
         newSkins = boss.skins;
      } else {
         MobTemplate template = MobTemplate.getById(boss.templateId);
         newPath = template != null ? template.skeletonPath : null;
         newScale = template != null ? template.scale : 0.25F;
         newSkins = template != null && template.variants != null && template.variants.length > 0
            ? new String[] {template.variants[0].skinName} : new String[0];
      }
      if (newPath != null && newPath.equals(this.path) && newScale == this.loadScale
         && java.util.Arrays.equals(newSkins, this.skins)) {
         return;
      }
      this.release();
      this.character = boss != null && boss.character;
      this.path = newPath;
      this.loadScale = newScale;
      this.skins = newSkins;
      this.lastAttempt = 0L;
   }

   void release() {
      if (this.pair != null) {
         SkeletonLoader.releaseSkeleton(this.pair);
         this.pair = null;
      }
   }

   /** Skeletons load asynchronously (encrypted external files): retry every 100 ms until available. */
   private void tryLoad() {
      if (this.path == null || this.pair != null || !GameUtils.hasElapsed(this.lastAttempt, 100)) {
         return;
      }
      this.lastAttempt = System.currentTimeMillis();
      NamedPair<AnimationState, Skeleton> loaded = SkeletonLoader.obtainSkeleton(this.path, this.loadScale,
         new SkeletonLoader.AnimationMixConfig(0.2F));
      if (loaded == null) {
         return;
      }
      this.pair = loaded;
      Skeleton skeleton = loaded.second;
      SkeletonData data = skeleton.getData();
      Skin combined = new Skin("");
      for (String name : this.skins) {
         Skin skin = data.findSkin(name);
         if (skin != null) {
            combined.addSkin(skin);
         }
      }
      skeleton.setSkin(combined);
      skeleton.setSlotsToSetupPose();
      Animation idle = data.findAnimation("idle");
      if (idle == null && data.animations.size > 0) {
         idle = data.animations.first();
      }
      if (idle != null) {
         loaded.first.setAnimation(0, idle.getName(), true);
      }
      this.computeFit(loaded.first, skeleton);
   }

   /** Real bounds (idle pose at 0 s, scale 1): minX, minY, maxX, maxY. */
   private static float[] measure(AnimationState state, Skeleton skeleton) {
      state.apply(skeleton);
      skeleton.setScale(1.0F, 1.0F);
      skeleton.setPosition(0.0F, 0.0F);
      skeleton.updateWorldTransform(Skeleton.Physics.NONE);
      float[] box = {Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
      float[] verts = new float[64];
      for (com.tlnt.thirdparty.spine.Slot slot : skeleton.drawOrder) {
         com.tlnt.thirdparty.spine.attachment.Attachment a = slot.attachment;
         if (a == null || slot.color.a <= 0.01F) {
            continue;
         }
         int n;
         if (a instanceof com.tlnt.thirdparty.spine.attachment.RegionAttachment) {
            n = 8;
            ((com.tlnt.thirdparty.spine.attachment.RegionAttachment)a).computeWorldVertices(slot, verts, 0, 2);
         } else if (a instanceof com.tlnt.thirdparty.spine.attachment.MeshAttachment) {
            com.tlnt.thirdparty.spine.attachment.MeshAttachment mesh = (com.tlnt.thirdparty.spine.attachment.MeshAttachment)a;
            n = mesh.getWorldVerticesLength();
            if (verts.length < n) {
               verts = new float[n];
            }
            mesh.computeWorldVertices(slot, 0, n, verts, 0, 2);
         } else {
            continue;
         }
         for (int i = 0; i + 1 < n; i += 2) {
            box[0] = Math.min(box[0], verts[i]);
            box[1] = Math.min(box[1], verts[i + 1]);
            box[2] = Math.max(box[2], verts[i]);
            box[3] = Math.max(box[3], verts[i + 1]);
         }
      }
      return box[0] > box[2] ? null : box;
   }

   /** Scales the image to fit the frame (mob boss: centered on the real bounds; char boss: centered on the body), feet on the frame bottom. */
   private void computeFit(AnimationState state, Skeleton skeleton) {
      float[] box = measure(state, skeleton);
      if (box == null || box[2] - box[0] <= 0.0F || box[3] - box[1] <= 0.0F) {
         this.fitScale = 1.0F;
         this.offsetX = 0.0F;
         this.offsetY = 0.0F;
         return;
      }
      float w = box[2] - box[0];
      float h = box[3] - box[1];
      if (this.character) {
         // fake-player boss: like the character Info window (CharacterModelActor) — the skeleton origin is the feet, placed at the bottom center
         // of the frame, scale 150 (smaller if too tall); long weapons sticking out are clipped at the frame edge, the body stays centered
         this.fitScale = Math.min(CHAR_SCALE, this.getHeight() * 0.86F / Math.max(1.0F, box[3]));
         this.offsetX = 0.0F;
         this.offsetY = 0.0F;
         return;
      } else {
         this.fitScale = Math.min(this.getWidth() * 0.86F / w, this.getHeight() * 0.86F / h);
         this.offsetX = (box[0] + w / 2.0F) * this.fitScale;
      }
      this.offsetY = box[1] * this.fitScale;
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      this.tryLoad();
      float cx = this.getX() + this.getWidth() / 2.0F;
      float floorY = this.getY() + this.getHeight() * 0.07F;
      TextureAsset shadow = TextureAsset.obtain("shadow");
      super.renderContext.drawTextureSized(shadow, cx, floorY, this.getWidth() * 0.55F, this.getHeight() * 0.07F, Anchor.CENTER);
      if (this.pair == null) {
         return;
      }
      float delta = Gdx.graphics.getDeltaTime();
      AnimationState state = this.pair.first;
      Skeleton skeleton = this.pair.second;
      state.update(delta);
      state.apply(skeleton);
      skeleton.update(delta);
      skeleton.setScale(this.fitScale, this.fitScale);
      skeleton.setPosition(cx - this.offsetX, floorY - this.offsetY);
      skeleton.updateWorldTransform(Skeleton.Physics.NONE);
      skeleton.setColor(com.badlogic.gdx.graphics.Color.WHITE);
      // idle may sway outside the measured bounds: clip exactly to the frame so it does not spill onto the neighboring text
      boolean clipped = super.renderContext.pushScissors(this.getX(), this.getY(), this.getWidth(), this.getHeight()); // draw coordinates (same space as the batch)
      super.renderContext.drawSkeleton(skeleton);
      if (clipped) {
         super.renderContext.popScissors();
      }
   }
}
