package com.tlnt.engine.physics.gizmo;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.input.InputHandler;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.physics.Entity;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;

public final class TransformGizmo extends BaseComponent {
   public final Vector2 projectile = new Vector2(0.0F, 1.0F);
   public final Vector2 speed = new Vector2(1.0F, 0.0F);
   public String active;
   public Vector3 autoFlipEnabled = new Vector3();
   public int projectile2 = GizmoAxis.NONE;

   public TransformGizmo(Entity entity, String label) {
      super(entity);
      this.active = label;
   }

   @Override
   public final void update(float delta) {
   }

   @Override
   public final void render(RenderContext ctx) {
      if (ctx.isDebugMode()) {
         Vector2 pointerPos = InputHandler.getInstance().getPointerWorldPos();
         Vector2 entityPos = this.getEntity().getPosition();
         ScreenViewport viewport = (ScreenViewport)ctx.getDebugViewport();
         float unitScale = ctx.getCurrentZoom() * viewport.getUnitsPerPixel();
         ShapeDrawer shapeDrawer = ctx.getShapeDrawer();
         float size = 20.0F * unitScale;
         int hoverFlag;
         if (pointerPos.x >= entityPos.x && pointerPos.x <= entityPos.x + size && pointerPos.y >= entityPos.y && pointerPos.y <= entityPos.y + size) {
            hoverFlag = 1;
         } else {
            hoverFlag = 0;
         }

         int centerHovered = hoverFlag;
         shapeDrawer.drawFilledRectangle(entityPos.x, entityPos.y, size, size, centerHovered != 0 ? Color.WHITE : Color.RED);
         size = 1.0F * ctx.getDebugCamera().zoom;
         Vector2 yTip;
         Vector2[] yArrow = computeArrowheadPoints(yTip = new Vector2(entityPos).add(this.projectile.x * size, this.projectile.y * size), this.projectile, unitScale);
         Vector2 arrowB = yArrow[0];
         Vector2 arrowC = yArrow[1];
         boolean yHovered;
         Color yColor = (yHovered = this.isPointInTriangle(pointerPos, arrowB, arrowC, yArrow[2])) ? Color.WHITE : Color.GREEN;
         shapeDrawer.drawLine(entityPos.x, entityPos.y, yTip.x, yTip.y, unitScale, yColor, yColor);
         Vector2 arrowA = yArrow[0];
         arrowB = yArrow[1];
         shapeDrawer.drawFilledTriangle(arrowA, arrowB, yArrow[2], yColor);
         Vector2 xTip;
         Vector2[] xArrow = computeArrowheadPoints(xTip = new Vector2(entityPos).add(this.speed.x * size, this.speed.y * size), this.speed, unitScale);
         arrowB = xArrow[0];
         boolean xHovered;
         Color xColor = (xHovered = this.isPointInTriangle(pointerPos, arrowB, xArrow[1], xArrow[2]))
            ? Color.WHITE
            : Color.BLUE;
         shapeDrawer.drawLine(entityPos.x, entityPos.y, xTip.x, xTip.y, unitScale, xColor, xColor);
         shapeDrawer.drawFilledTriangle(xArrow[0], xArrow[1], xArrow[2], xColor);
         if (centerHovered != 0) {
            this.projectile2 = GizmoAxis.AXIS_XY;
         } else if (xHovered) {
            this.projectile2 = GizmoAxis.AXIS_X;
         } else if (yHovered) {
            this.projectile2 = GizmoAxis.AXIS_Y;
         } else {
            this.projectile2 = GizmoAxis.NONE;
         }

         this.autoFlipEnabled.set(this.getEntity().getPosition().x, this.getEntity().getPosition().y - 25.0F * unitScale, 0.0F);
         ctx.worldToHud(this.autoFlipEnabled);
         ctx.useHudCamera();
         ctx.drawText(FontManager.WORLD_TEXT_FONT, this.active, this.autoFlipEnabled.x, this.autoFlipEnabled.y, Anchor.CENTER, 24.0F, Color.WHITE);
         ctx.useDebugCamera();
      }
   }

   public static Vector2[] computeArrowheadPoints(Vector2 tip, Vector2 direction, float size) {
      size = 16.0F * size;
      Vector2 perpendicular = new Vector2(-direction.y, direction.x).nor().scl(size);
      direction = new Vector2(direction).nor().scl(-size);
      Vector2 tipCopy = new Vector2(tip);
      Vector2 leftPoint = new Vector2(tip).add(direction).add(perpendicular);
      tip = new Vector2(tip).add(direction).sub(perpendicular);
      Vector2[] points = new Vector2[3];
      points[0] = tipCopy;
      points[1] = leftPoint;
      points[2] = tip;
      return points;
   }

   public boolean isPointInTriangle(Vector2 point, Vector2 a, Vector2 b, Vector2 c) {
      float d1 = crossProductZ(point, a, b);
      float d2 = crossProductZ(point, b, c);
      float d3 = crossProductZ(point, c, a);
      int flag;
      if (!(d1 < 0.0F) && !(d2 < 0.0F) && !(d3 < 0.0F)) {
         flag = 0;
      } else {
         flag = 1;
      }

      int hasNegative = flag;
      if (!(d1 > 0.0F) && !(d2 > 0.0F) && !(d3 > 0.0F)) {
         flag = 0;
      } else {
         flag = 1;
      }

      int hasPositive = flag;
      if (hasNegative != 0 && hasPositive != 0) {
         return false;
      } else {
         return true;
      }
   }

   public static float crossProductZ(Vector2 a, Vector2 b, Vector2 c) {
      return (a.x - c.x) * (b.y - c.y) - (b.x - c.x) * (a.y - c.y);
   }
}
