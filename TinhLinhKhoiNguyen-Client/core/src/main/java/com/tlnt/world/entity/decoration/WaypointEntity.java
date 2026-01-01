package com.tlnt.world.entity.decoration;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.tlnt.core.audio.CollisionFilters;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.BodyType;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.physics.collider.BoxCollider;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.world.entity.player.PlayerEntity;

public final class WaypointEntity extends GameObject {
   public GlyphLayout POOL;
   public float owner = 30.0F;
   public BitmapFont target = FontFactory.scaleFont(FontManager.WORLD_TEXT_FONT, this.owner);
   public int POOL2;
   public String movementComponent;
   public float target2;
   public float colliderHeight;
   public boolean skeleton;
   public BodyComponent body;
   public BoxCollider collider;
   public final Rectangle bounds = new Rectangle();
   public final Vector3 unprojectTemp = new Vector3();
   public final float[] renderData;
   public boolean pressed;
   public int touchPointer;
   public NinePatchAsset backgroundPatch;

   public WaypointEntity(LoginScreen screen, int waypointId, String label, float x, float y, float width, float height, boolean enabledFlag) {
      super(screen, EntityType.WAYPOINT);
      this.renderData = new float[5];
      this.touchPointer = -1;
      String patchName = "ui/13";
      this.backgroundPatch = NinePatchAsset.obtain(patchName, 20);
      this.POOL2 = waypointId;
      this.movementComponent = label;
      this.target2 = width;
      this.colliderHeight = height;
      this.skeleton = enabledFlag;
      this.teleportTo((x + width / 2.0F) / 100.0F, (y + height / 2.0F) / 100.0F);
      this.POOL = new GlyphLayout(this.target, this.movementComponent);
      this.setRenderOrder(2000000.0F);
   }

   @Override
   public final void onAwake() {
      this.body = new BodyComponent(BodyType.STATIC, this.getEntity()).setInitialPosition(this.getPosition());
      this.collider = new BoxCollider(this.target2 / 2.0F, this.colliderHeight / 2.0F, Vector2.Zero, this.getEntity());
      this.collider.setFilterData(CollisionFilters.WAYPOINT);
      this.collider.setSensor(true);
   }

   public final boolean onTouchDown(int screenX, int screenY, int pointer) {
      this.unprojectTemp.set((float)screenX, (float)screenY, 0.0F);
      this.getScreen().getUiViewport().unproject(this.unprojectTemp);
      if (this.bounds.contains(this.unprojectTemp.x, this.unprojectTemp.y)) {
         this.touchPointer = pointer;
         this.pressed = true;
         return true;
      } else {
         return false;
      }
   }

   public final boolean onTouchUp(int screenX, int screenY, int pointer) {
      if (pointer != this.touchPointer) {
         return false;
      } else {
         this.touchPointer = -1;
         this.pressed = false;
         this.unprojectTemp.set((float)screenX, (float)screenY, 0.0F);
         this.getScreen().getUiViewport().unproject(this.unprojectTemp);
         if (this.bounds.contains(this.unprojectTemp.x, this.unprojectTemp.y) && PlayerEntity.getInstance() != null) {
            PlayerEntity.getInstance().movementController.setMoveTarget(this.getPosition().x, this.getPosition().y, new WaypointArrivalCallback(this));
         }

         return true;
      }
   }

   @Override
   public final void render(RenderContext ctx) {
      float[] data = this.renderData;
      Rectangle rect = this.bounds;
      int cond;
      if (PlayerEntity.getInstance() != null && !(this.getPosition().dst(PlayerEntity.getInstance().getPosition()) > 5.0F)) {
         ctx.useWorldCamera();
         float zoom = ctx.getCurrentZoom();
         float boxW = this.POOL.width / zoom;
         float boxH = this.POOL.height / zoom;
         float pad = 30.0F / zoom;
         float labelX = 300.0F / zoom;
         boxW = Math.max(boxW + pad * 2.0F, labelX);
         boxH += pad * 2.0F;
         pad = ((LoginScreen)this.getScreen()).editorViewport.getMapWidth();
         labelX = ((LoginScreen)this.getScreen()).editorViewport.getMapHeight();
         Vector3 topPos = new Vector3(0.0F, 0.0F, 0.0F);
         Vector3 bottomRight = new Vector3(pad / 100.0F, labelX / 100.0F, 0.0F);
         ctx.worldToHud(topPos);
         ctx.worldToHud(bottomRight);
         labelX = Math.min(topPos.x, bottomRight.x);
         float maxX = Math.max(topPos.x, bottomRight.x);
         float minY = Math.min(topPos.y, bottomRight.y);
         pad = Math.max(topPos.y, bottomRight.y);
         topPos = new Vector3(this.getPosition().x, this.getPosition().y + this.body.getBoundsRect().height / 2.0F, 0.0F);
         Vector3 bottomPos = new Vector3(this.getPosition().x, this.getPosition().y - this.body.getBoundsRect().height / 2.0F, 0.0F);
         ctx.worldToHud(topPos);
         ctx.worldToHud(bottomPos);
         labelX = labelX + boxW / 2.0F + 10.0F;
         maxX = maxX - boxW / 2.0F - 10.0F;
         labelX = MathUtils.clamp(topPos.x, labelX, maxX);
         if (topPos.y + boxH + 10.0F <= pad) {
            cond = 1;
         } else {
            cond = 0;
         }

         int fitsAbove = cond;
         int fitsBelow = bottomPos.y - boxH - 10.0F >= minY ? 1 : 0;
         float labelY;
         if (fitsAbove == 0 && fitsBelow != 0) {
            maxX = minY + boxH + 10.0F;
            pad -= 10.0F;
            labelY = MathUtils.clamp(bottomPos.y, maxX, pad) - boxH / 2.0F;
         } else {
            maxX = minY + 10.0F;
            pad = pad - boxH - 10.0F;
            labelY = MathUtils.clamp(topPos.y, maxX, pad) + boxH / 2.0F;
         }

         rect.set(labelX - boxW / 2.0F, labelY - boxH / 2.0F, boxW, boxH);
         data[0] = labelX;
         data[1] = labelY;
         data[2] = boxW;
         data[3] = boxH;
         data[4] = zoom;
         ctx.usePreviousCamera();
         cond = 1;
      } else {
         cond = 0;
      }

      if (cond != 0) {
         float drawX = this.renderData[0];
         float drawY = this.renderData[1];
         float drawW = this.renderData[2];
         float drawH = this.renderData[3];
         float drawZoom = this.renderData[4];
         ctx.useHudCamera();
         if (this.pressed) {
            drawW -= 5.0F;
            drawH -= 5.0F;
         }

         ctx.drawNinePatch(this.backgroundPatch, drawX, drawY, drawW, drawH, Anchor.CENTER);
         ctx.drawText(FontManager.PLAIN_FONT, this.movementComponent, drawX, drawY, Anchor.CENTER, this.owner / drawZoom, Color.WHITE);
         ctx.usePreviousCamera();
      }
   }

   public final boolean containsPoint(float x, float y) {
      return ((this.body == null ? 0 : (this.body.getBoundsRect().contains(x, y) ? 1 : 0)) != 0);
   }

   public final boolean containsVector(Vector2 point) {
      return this.containsPoint(point.x, point.y);
   }

   @Override
   public final void dispose() {
      this.movementComponent = null;
      this.body = null;
      this.collider = null;
   }

}
