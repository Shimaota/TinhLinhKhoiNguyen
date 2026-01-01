package com.tlnt.engine.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.SkeletonRenderer;

public final class RenderContext {
   public static final ObjectMap<String, Color> COLOR_CACHE = new ObjectMap<>();
   public boolean debugMode;
   public final float worldUnitScale;
   public Batch batch;
   public final Viewport debugViewport;
   public final Viewport worldViewport;
   public final Viewport hudViewport;
   public final OrthographicCamera debugCamera;
   public final OrthographicCamera worldCamera;
   public final OrthographicCamera hudCamera;
   public ShapeDrawer shapeDrawer;
   public OrthographicCamera previousCamera;
   public OrthographicCamera currentCamera;
   public boolean hudCameraActive;
   public SkeletonRenderer skeletonRenderer;
   public ShaderProgram maskShader;
   public Rectangle viewBounds;
   public long lastFrameId;
   public static String vertexShaderSrc;
   public static String fragmentShaderSrc;
   public final Rectangle clipRect = new Rectangle();
   private final GlyphLayout textLayout = new GlyphLayout();
   private final Color textTint = new Color();
   public final Rectangle scissorRect = new Rectangle();

   public RenderContext(OrthographicCamera debugCamera, OrthographicCamera worldCamera, OrthographicCamera hudCamera, Viewport debugViewport, Viewport worldViewport, Viewport hudViewport) {
      this.debugCamera = debugCamera;
      this.debugViewport = debugViewport;
      this.worldCamera = worldCamera;
      this.hudCamera = hudCamera;
      this.worldViewport = worldViewport;
      this.hudViewport = hudViewport;
      this.viewBounds = new Rectangle();
      this.worldUnitScale = 0.01F;
   }

   public final void setBatch(Batch batch) {
      this.batch = batch;
   }

   public final void useWorldCamera() {
      this.useCamera(this.worldCamera);
   }

   public final void useDebugCamera() {
      this.useCamera(this.debugCamera);
   }

   public final void useHudCamera() {
      this.useCamera(this.hudCamera);
   }

   public final void usePreviousCamera() {
      this.useCamera(this.previousCamera);
   }

   public void useCamera(OrthographicCamera camera) {
      if (camera != null) {
         if (this.currentCamera != camera || this.lastFrameId != Gdx.graphics.getFrameId()) {
            this.previousCamera = this.currentCamera;
            this.currentCamera = camera;
            this.hudCameraActive = this.currentCamera.equals(this.hudCamera);
            this.lastFrameId = Gdx.graphics.getFrameId();
            if (this.currentCamera != null) {
               if (this.currentCamera.equals(this.worldCamera)) {
                  this.worldViewport.apply();
               } else if (this.currentCamera.equals(this.hudCamera)) {
                  this.hudViewport.apply();
               }
            }

            this.getBatch().setProjectionMatrix(camera.combined);
            float viewWidth = camera.viewportWidth * camera.zoom;
            float viewHeight = camera.viewportHeight * camera.zoom;
            float boundsWidth = viewWidth * Math.abs(camera.up.y) + viewHeight * Math.abs(camera.up.x);
            viewWidth = viewHeight * Math.abs(camera.up.y) + viewWidth * Math.abs(camera.up.x);
            this.viewBounds.set(camera.position.x - boundsWidth / 2.0F, camera.position.y - viewWidth / 2.0F, boundsWidth, viewWidth);
         }
      }
   }

   public final SkeletonRenderer getSkeletonRenderer() {
      if (this.skeletonRenderer == null) {
         this.skeletonRenderer = new SkeletonRenderer();
      }

      return this.skeletonRenderer;
   }

   public final void drawSkeleton(Skeleton skeleton) {
      Batch batch = this.getBatch();
      int blendSrc = batch.getBlendSrcFunc();
      int blendDst = batch.getBlendDstFunc();
      int blendSrcAlpha = batch.getBlendSrcFuncAlpha();
      int blendDstAlpha = batch.getBlendDstFuncAlpha();
      try {
         this.getSkeletonRenderer().draw(batch, skeleton);
      } finally {
         // Spine may switch blending to ADDITIVE for glow slots. Restore the previous state before
         // drawing the next map/sprite, otherwise every following layer is additively brightened and washed out.
         batch.setBlendFunctionSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha);
      }
   }

   public final void drawTextureFit(TextureAsset texture, float x, float y, float width, float height, Anchor anchor) {
      if (texture != null) {
         float texWidth = (float)texture.getWidth();
         float texHeight = (float)texture.getHeight();
         if (texWidth <= 0.0F || texHeight <= 0.0F) {
            this.drawTextureSized(texture, x, y, -1.0F, -1.0F, anchor);
         } else if (width <= 0.0F && height <= 0.0F) {
            this.drawTextureSized(texture, x, y, -1.0F, -1.0F, anchor);
         } else if (width > 0.0F && height <= 0.0F) {
            height = width * (texHeight / texWidth);
            this.drawTextureSized(texture, x, y, width, height, anchor);
         } else if (height > 0.0F && width <= 0.0F) {
            width = height * (texWidth / texHeight);
            this.drawTextureSized(texture, x, y, width, height, anchor);
         } else {
            float targetAspect = width / height;
            if ((texWidth = texWidth / texHeight) > targetAspect) {
               float fitHeight = width / texWidth;
               this.drawTextureSized(texture, x, y, width, fitHeight, anchor);
            } else {
               width = height * texWidth;
               this.drawTextureSized(texture, x, y, width, height, anchor);
            }
         }
      }
   }

   public final void drawTexture(TextureAsset texture, float x, float y, Anchor anchor) {
      this.drawTextureSized(texture, x, y, (float)texture.getWidth(), (float)texture.getHeight(), anchor);
   }

   public final void drawTextureScaled(TextureAsset texture, float x, float y, Anchor anchor, float scaleX, float scaleY) {
      float texWidth = (float)texture.getWidth();
      float texHeight = (float)texture.getHeight();
      this.drawTextureFull(
         texture,
         x,
         y,
         texWidth,
         texHeight,
         0.0F,
         scaleX,
         scaleY,
         false,
         false,
         anchor
      );
   }

   public final void drawTextureSized(TextureAsset texture, float x, float y, float width, float height, Anchor anchor) {
      this.drawTextureRotated(texture, x, y, width, height, 0.0F, anchor);
   }

   public final void drawTextureFlipped(TextureAsset texture, float x, float y, float width, float height, boolean flipX, boolean flipY, Anchor anchor) {
      this.drawTextureFull(texture, x, y, width, height, 0.0F, 1.0F, 1.0F, flipX, flipY, anchor);
   }

   public void drawTextureRotated(TextureAsset texture, float x, float y, float width, float height, float rotation, Anchor anchor) {
      this.drawTextureFull(texture, x, y, width, height, 0.0F, 1.0F, 1.0F, false, false, anchor);
   }

   public final void drawTextureRotatedFlipped(
      TextureAsset texture, float x, float y, float width, float height, float rotation, boolean flipX, boolean flipY, Anchor anchor
   ) {
      this.drawTextureFull(texture, x, y, width, height, rotation, 1.0F, 1.0F, flipX, flipY, anchor);
   }

   public void drawTextureFull(
      TextureAsset texture, float x, float y, float width, float height, float rotation, float scaleX, float scaleY, boolean flipX, boolean flipY, Anchor anchor
   ) {
      if (width == -1.0F) {
         width = (float)texture.getWidth();
      }

      if (height == -1.0F) {
         height = (float)texture.getHeight();
      }

      if (texture != null) {
         TextureRegion region;
         if ((region = texture.getRegion()) != null) {
            this.drawRegion(region, x, y, width, height, rotation, scaleX, scaleY, flipX, flipY, anchor);
         }
      }
   }

   public final void drawTextureCropped(TextureAsset texture, float x, float y, Anchor anchor, int cropWidth, int cropHeight, int width, int height) {
      if (texture != null) {
         TextureRegion region;
         if ((region = texture.getRegion()) != null) {
            cropWidth = Math.min(width, Math.max(0, region.getRegionWidth()));
            cropHeight = Math.min(height, Math.max(0, region.getRegionHeight()));
            if (cropWidth > 0 && cropHeight > 0) {
               width = region.getRegionX();
               height = region.getRegionY();
               TextureRegion cropped = new TextureRegion(region.getTexture(), width, height, cropWidth, cropHeight);
               float croppedHeight = (float)cropped.getRegionHeight();
               float croppedWidth = (float)cropped.getRegionWidth();
               float rotation = 0.0F;
               this.drawRegion(cropped, x, y, croppedWidth, croppedHeight, 0.0F, 1.0F, 1.0F, false, false, anchor);
            }
         }
      }
   }

   public void drawRegion(
      TextureRegion region, float x, float y, float width, float height, float rotation, float scaleX, float scaleY, boolean flipX, boolean flipY, Anchor anchor
   ) {
      if (region != null) {
         OrthographicCamera camera = this.currentCamera;
         if (this.currentCamera != null) {
            width *= scaleX;
            height *= scaleY;
            if (!this.hudCameraActive) {
               width /= 100.0F;
               height /= 100.0F;
            }

            scaleX = width / 2.0F;
            scaleY = height / 2.0F;
            switch (anchor) {
               case TOP_LEFT:
                  scaleX = 0.0F;
                  scaleY = height;
                  break;
               case TOP_CENTER:
                  scaleX = width / 2.0F;
                  scaleY = height;
                  break;
               case TOP_RIGHT:
                  scaleX = width;
                  scaleY = height;
                  break;
               case MIDDLE_LEFT:
                  scaleX = 0.0F;
                  scaleY = height / 2.0F;
                  break;
               case CENTER:
                  scaleX = width / 2.0F;
                  scaleY = height / 2.0F;
                  break;
               case MIDDLE_RIGHT:
                  scaleX = width;
                  scaleY = height / 2.0F;
                  break;
               case BOTTOM_LEFT:
                  scaleX = 0.0F;
                  scaleY = 0.0F;
                  break;
               case BOTTOM_CENTER:
                  scaleX = width / 2.0F;
                  scaleY = 0.0F;
                  break;
               case BOTTOM_RIGHT:
                  scaleX = width;
                  scaleY = 0.0F;
            }

            x -= scaleX;
            y -= scaleY;
            float[] corners = computeRotatedCorners(x, y, width, height, rotation, scaleX, scaleY);
            float viewLeft = camera.position.x - camera.viewportWidth * camera.zoom / 2.0F;
            float viewRight = camera.position.x + camera.viewportWidth * camera.zoom / 2.0F;
            float viewBottom = camera.position.y - camera.viewportHeight * camera.zoom / 2.0F;
            float viewTop = camera.position.y + camera.viewportHeight * camera.zoom / 2.0F;
            if (this.isQuadInView(corners, viewLeft, viewRight, viewBottom, viewTop)) {
               if (region.isFlipX() != flipX || region.isFlipY() != flipY) {
                  int needFlipX;
                  if (flipX != region.isFlipX()) {
                     needFlipX = 1;
                  } else {
                     needFlipX = 0;
                  }

                  int needFlipY;
                  if (flipY != region.isFlipY()) {
                     needFlipY = 1;
                  } else {
                     needFlipY = 0;
                  }

                  region.flip((needFlipX != 0), (needFlipY != 0));
               }

               this.getBatch().draw(region, x, y, scaleX, scaleY, width, height, 1.0F, 1.0F, rotation);
            }
         }
      }
   }

   public boolean isQuadInView(float[] quad, float left, float right, float bottom, float top) {
      float[] viewCorners = new float[8];
      viewCorners[0] = left;
      viewCorners[1] = bottom;
      viewCorners[2] = right;
      viewCorners[3] = bottom;
      viewCorners[4] = right;
      viewCorners[5] = top;
      viewCorners[6] = left;
      viewCorners[7] = top;
      float[] viewQuad = viewCorners;
      return this.polygonsOverlap(quad, viewQuad);
   }

   public boolean polygonsOverlap(float[] polyA, float[] polyB) {
      if (!hasSeparatingAxis(polyA, polyB) && !hasSeparatingAxis(polyB, polyA)) {
         return true;
      } else {
         return false;
      }
   }

   public static boolean hasSeparatingAxis(float[] polyA, float[] polyB) {
      int coordCount = polyA.length;
      int pointCount = coordCount / 2;

      for (int i = 0; i < pointCount; i++) {
         int next = (i + 1) % pointCount;
         float nextX = polyA[next << 1];
         float normalY = nextX - polyA[i << 1];
         int nextIdx = next << 1;
         float nextY = polyA[nextIdx + 1];
         int currIdx = i << 1;
         float normalX = -(nextY - polyA[currIdx + 1]);
         normalY = normalY;
         float minA = Float.MAX_VALUE;
         float maxA = Float.MIN_VALUE;

         for (int j = 0; j < polyA.length; j += 2) {
            float projX = polyA[j] * normalX;
            float projA = projX + polyA[j + 1] * normalY;
            minA = Math.min(minA, projA);
            maxA = Math.max(maxA, projA);
         }

         float minB = Float.MAX_VALUE;
         float maxB = Float.MIN_VALUE;

         for (int k = 0; k < polyB.length; k += 2) {
            float projB = polyB[k] * normalX + polyB[k + 1] * normalY;
            minB = Math.min(minB, projB);
            maxB = Math.max(maxB, projB);
         }

         if (maxA < minB || maxB < minA) {
            return true;
         }
      }

      return false;
   }

   public static float[] computeRotatedCorners(float x, float y, float width, float height, float rotation, float originX, float originY) {
      float[] result = new float[8];
      float[] unrotated = new float[8];
      unrotated[0] = x;
      unrotated[1] = y;
      unrotated[2] = x + width;
      unrotated[3] = y;
      unrotated[4] = x + width;
      unrotated[5] = y + height;
      unrotated[6] = x;
      unrotated[7] = y + height;
      float[] corners = unrotated;
      rotation = (float)Math.cos((double)(height = (float)Math.toRadians((double)rotation)));
      height = (float)Math.sin((double)height);
      x += originX;
      y += originY;
      int i = 0;

      while (true) {
         if (i >= 8) {
            return result;
         }

         originY = corners[i] - x;
         float dy = corners[i + 1] - y;
         result[i] = x + (originY * rotation - dy * height);
         result[i + 1] = y + originY * height + dy * rotation;
         i += 2;
      }
   }

   public final void drawLine(float x, float y, float width, float height, Color color) {
      this.getShapeDrawer().drawFilledRectangle(x, y, width, height, color);
   }

   public final void setBatchColorRgba(float r, float g, float b, float a) {
      this.getBatch().setColor(r, g, b, a);
   }

   public final void setBatchColor(Color color) {
      this.getBatch().setColor(color);
   }

   public final void resetBatchColor() {
      this.getBatch().setColor(Color.WHITE);
   }

   public final void drawCompositeSprite(CompositeSprite sprite, float x, float y, float width, float height, Anchor anchor) {
      if (sprite != null && sprite.startTexture != null && sprite.middleTexture != null && sprite.endTexture != null) {
         if (sprite.vertical) {
            float drawX = x;
            float drawY = y;
            switch (anchor) {
               case TOP_LEFT:
                  drawY = drawY - height;
                  break;
               case TOP_CENTER:
                  drawX = drawX - width / 2.0F;
                  drawY = drawY - height;
                  break;
               case TOP_RIGHT:
                  drawX = drawX - width;
                  drawY = drawY - height;
                  break;
               case MIDDLE_LEFT:
                  drawY = drawY - height / 2.0F;
                  break;
               case CENTER:
                  drawX = drawX - width / 2.0F;
                  drawY = drawY - height / 2.0F;
                  break;
               case MIDDLE_RIGHT:
                  drawX = drawX - width;
                  drawY = drawY - height / 2.0F;
               case BOTTOM_LEFT:
               default:
                  break;
               case BOTTOM_CENTER:
                  drawX = drawX - width / 2.0F;
                  break;
               case BOTTOM_RIGHT:
                  drawX = drawX - width;
            }

            float startSize = (float)sprite.startTexture.getWidth();
            float endSize = (float)sprite.startTexture.getHeight();
            float middleSize = (float)sprite.endTexture.getWidth();
            float endHeight = (float)sprite.endTexture.getHeight();
            float startScale = startSize == 0.0F ? 1.0F : width / startSize;
            float endScale = middleSize == 0.0F ? 1.0F : width / middleSize;
            startSize = endSize * startScale;
            endSize = endHeight * endScale;
            if ((middleSize = height - startSize - endSize) < 0.0F) {
               if ((endHeight = startSize + endSize) > 0.0F) {
                  middleSize = height / endHeight;
                  startSize *= middleSize;
                  endSize *= middleSize;
               } else {
                  startSize = 0.0F;
                  endSize = 0.0F;
               }

               middleSize = 0.0F;
            }

            Batch batch = this.getBatch();
            if (startSize > 0.0F) {
               batch.draw(sprite.startTexture.getRegion(), drawX, drawY + height - startSize, width, startSize);
            }

            if (middleSize > 0.0F) {
               batch.draw(sprite.middleTexture.getRegion(), drawX, drawY + endSize, width, middleSize);
            }

            if (endSize > 0.0F) {
               batch.draw(sprite.endTexture.getRegion(), drawX, drawY, width, endSize);
            }
         } else {
            float drawX = x;
            float drawY = y;
            switch (anchor) {
               case TOP_LEFT:
                  drawY = drawY - height;
                  break;
               case TOP_CENTER:
                  drawX = drawX - width / 2.0F;
                  drawY = drawY - height;
                  break;
               case TOP_RIGHT:
                  drawX = drawX - width;
                  drawY = drawY - height;
                  break;
               case MIDDLE_LEFT:
                  drawY = drawY - height / 2.0F;
                  break;
               case CENTER:
                  drawX = drawX - width / 2.0F;
                  drawY = drawY - height / 2.0F;
                  break;
               case MIDDLE_RIGHT:
                  drawX = drawX - width;
                  drawY = drawY - height / 2.0F;
               case BOTTOM_LEFT:
               default:
                  break;
               case BOTTOM_CENTER:
                  drawX = drawX - width / 2.0F;
                  break;
               case BOTTOM_RIGHT:
                  drawX = drawX - width;
            }

            float startSize = (float)sprite.startTexture.getWidth();
            float endSize = (float)sprite.startTexture.getHeight();
            float middleSize = (float)sprite.endTexture.getWidth();
            float endHeight = (float)sprite.endTexture.getHeight();
            float startScale = endSize == 0.0F ? 1.0F : height / endSize;
            float endScale = endHeight == 0.0F ? 1.0F : height / endHeight;
            startSize *= startScale;
            endSize = middleSize * endScale;
            if ((middleSize = width - startSize - endSize) < 0.0F) {
               if ((endHeight = startSize + endSize) > 0.0F) {
                  middleSize = width / endHeight;
                  startSize *= middleSize;
                  endSize *= middleSize;
               } else {
                  startSize = 0.0F;
                  endSize = 0.0F;
               }

               middleSize = 0.0F;
            }

            Batch batch = this.getBatch();
            if (startSize > 0.0F) {
               batch.draw(sprite.startTexture.getRegion(), drawX, drawY, startSize, height);
            }

            if (middleSize > 0.0F) {
               batch.draw(sprite.middleTexture.getRegion(), drawX + startSize, drawY, middleSize, height);
            }

            if (endSize > 0.0F) {
               batch.draw(sprite.endTexture.getRegion(), drawX + startSize + middleSize, drawY, endSize, height);
            }
         }
      }
   }

   public final void drawNinePatch(NinePatchAsset asset, float x, float y, float width, float height, Anchor anchor) {
      if (asset != null) {
         NinePatch ninePatch;
         if ((ninePatch = asset.getNinePatch()) != null) {
            float offsetX = 0.0F;
            float offsetY = 0.0F;
            switch (anchor) {
               case TOP_LEFT:
               case MIDDLE_LEFT:
               case BOTTOM_LEFT:
                  offsetX = 0.0F;
                  break;
               case TOP_CENTER:
               case CENTER:
               case BOTTOM_CENTER:
                  offsetX = -width / 2.0F;
                  break;
               case TOP_RIGHT:
               case MIDDLE_RIGHT:
               case BOTTOM_RIGHT:
                  offsetX = -width;
            }

            switch (anchor) {
               case TOP_LEFT:
               case TOP_CENTER:
               case TOP_RIGHT:
                  offsetY = -height;
                  break;
               case MIDDLE_LEFT:
               case CENTER:
               case MIDDLE_RIGHT:
                  offsetY = -height / 2.0F;
                  break;
               case BOTTOM_LEFT:
               case BOTTOM_CENTER:
               case BOTTOM_RIGHT:
                  offsetY = 0.0F;
            }

            ninePatch.draw(this.getBatch(), x + offsetX, y + offsetY, width, height);
         }
      }
   }

   public static Color parseColor(String colorStr) {
      if (colorStr != null && !colorStr.isEmpty()) {
         String normalized;
         if (colorStr.startsWith("#")) {
            normalized = colorStr.substring(1).toUpperCase();
         } else {
            normalized = colorStr.toUpperCase();
         }

         colorStr = normalized;
         Color color;
         if ((color = COLOR_CACHE.get(colorStr)) != null) {
            return color;
         } else {
            new Color();

            try {
               Color.valueOf(colorStr);
               color = Color.valueOf(colorStr);
            } catch (Exception e) {
               color = Color.WHITE;
            }

            COLOR_CACHE.put(colorStr, color);
            return color;
         }
      } else {
         return Color.WHITE;
      }
   }

   public final void drawTextColorName(BitmapFont font, String text, float x, float y, Anchor anchor, float scale, String colorName) {
      Color color = parseColor(colorName);
      this.drawTextRgba(font, text, x, y, anchor, scale, color.r, color.g, color.b, color.a);
   }

   public final void drawText(BitmapFont font, String text, float x, float y, Anchor anchor, float scale, Color color) {
      this.drawTextRgba(font, text, x, y, anchor, scale, color.r, color.g, color.b, color.a);
   }

   public final void drawTextRgba(
      BitmapFont font, String text, float x, float y, Anchor anchor, float scale, float r, float g, float b, float a
   ) {
      this.drawTextAligned(font, text, x, y, anchor, scale, r, g, b, a, anchor.halign);
   }

   public void drawTextAligned(
      BitmapFont font, String text, float x, float y, Anchor anchor, float scale, float r, float g, float b, float a, int halign
   ) {
      if (text != null) {
         FontFactory.scaleAndTintFont(font, scale, r, g, b, a);
         // Reuse 1 GlyphLayout + 1 Color (render thread only; font.draw is done with them immediately) — the original allocated new ones on EVERY
         // text draw (every character name / damage number / HUD text every frame) -> constant GC garbage, stutter on Android.
         GlyphLayout layout = this.textLayout;
         Color tint = this.textTint.set(r, g, b, a);
         layout.setText(font, text, tint, 0.0F, halign, false);
         float drawX = x;
         x = y;
         label50:
         switch (halign) {
            case 1:
               switch (anchor) {
                  case TOP_LEFT:
                     drawX = drawX + layout.width / 2.0F;
                  case TOP_CENTER:
                  default:
                     break label50;
                  case TOP_RIGHT:
                     drawX = drawX - layout.width / 2.0F;
                     break label50;
                  case MIDDLE_LEFT:
                     drawX = drawX + layout.width / 2.0F;
                     x = y + layout.height / 2.0F;
                     break label50;
                  case CENTER:
                     x = y + layout.height / 2.0F;
                     break label50;
                  case MIDDLE_RIGHT:
                     drawX = drawX - layout.width / 2.0F;
                     x = y + layout.height / 2.0F;
                     break label50;
                  case BOTTOM_LEFT:
                     drawX = drawX + layout.width / 2.0F;
                     x = y + layout.height;
                     break label50;
                  case BOTTOM_CENTER:
                     x = y + layout.height;
                     break label50;
                  case BOTTOM_RIGHT:
                     drawX = drawX - layout.width / 2.0F;
                     x = y + layout.height;
                     break label50;
               }
            case 8:
               switch (anchor) {
                  case TOP_LEFT:
                  default:
                     break label50;
                  case TOP_CENTER:
                     drawX = drawX - layout.width / 2.0F;
                     break label50;
                  case TOP_RIGHT:
                     drawX = drawX - layout.width;
                     break label50;
                  case MIDDLE_LEFT:
                     x = y + layout.height / 2.0F;
                     break label50;
                  case CENTER:
                     drawX = drawX - layout.width / 2.0F;
                     x = y + layout.height / 2.0F;
                     break label50;
                  case MIDDLE_RIGHT:
                     drawX = drawX - layout.width;
                     x = y + layout.height / 2.0F;
                     break label50;
                  case BOTTOM_LEFT:
                     x = y + layout.height;
                     break label50;
                  case BOTTOM_CENTER:
                     drawX = drawX - layout.width / 2.0F;
                     x = y + layout.height;
                     break label50;
                  case BOTTOM_RIGHT:
                     drawX = drawX - layout.width;
                     x = y + layout.height;
                     break label50;
               }
            case 16:
               label49: {
                  switch (anchor) {
                     case TOP_LEFT:
                        drawX = drawX + layout.width;
                        break label49;
                     case TOP_CENTER:
                        drawX = drawX + layout.width / 2.0F;
                     case TOP_RIGHT:
                     default:
                        break label49;
                     case MIDDLE_LEFT:
                        drawX = drawX + layout.width;
                        x = y + layout.height / 2.0F;
                        break label49;
                     case CENTER:
                        drawX = drawX + layout.width / 2.0F;
                        x = y + layout.height / 2.0F;
                        break label49;
                     case MIDDLE_RIGHT:
                        x = y + layout.height / 2.0F;
                        break label49;
                     case BOTTOM_LEFT:
                        drawX = drawX + layout.width;
                        break;
                     case BOTTOM_CENTER:
                        drawX = drawX + layout.width / 2.0F;
                     case BOTTOM_RIGHT:
                  }

                  x = y + layout.height;
               }
         }

         font.draw(this.getBatch(), layout, drawX, x);
      }
   }

   public final void drawRadialSprite(DeformableSprite sprite, float x, float y, float width, float height, Anchor anchor, float angle) {
      float offsetX = 0.0F;
      float offsetY = 0.0F;
      switch (anchor) {
         case TOP_LEFT:
         case MIDDLE_LEFT:
         case BOTTOM_LEFT:
            offsetX = 0.0F;
            break;
         case TOP_CENTER:
         case CENTER:
         case BOTTOM_CENTER:
            offsetX = -width / 2.0F;
            break;
         case TOP_RIGHT:
         case MIDDLE_RIGHT:
         case BOTTOM_RIGHT:
            offsetX = -width;
      }

      switch (anchor) {
         case TOP_LEFT:
         case TOP_CENTER:
         case TOP_RIGHT:
            offsetY = -height;
            break;
         case MIDDLE_LEFT:
         case CENTER:
         case MIDDLE_RIGHT:
            offsetY = -height / 2.0F;
            break;
         case BOTTOM_LEFT:
         case BOTTOM_CENTER:
         case BOTTOM_RIGHT:
            offsetY = 0.0F;
      }

      sprite.draw(this.getBatch(), x + offsetX, y + offsetY, width, height, angle);
   }

   public final void beginHudBatch() {
      this.useHudCamera();
      this.restartBatch();
   }

   public final void restartBatch() {
      if (this.getBatch().isDrawing()) {
         this.getBatch().end();
      }

      this.getBatch().begin();
   }

   public final void flushBatch() {
      if (this.getBatch().isDrawing()) {
         this.getBatch().flush();
      }
   }

   public final void endBatch() {
      this.getBatch().end();
   }

   public final Batch getBatch() {
      if (this.batch == null) {
         this.batch = new PolygonSpriteBatch();
      }

      return this.batch;
   }

   public final void unprojectWorld(Vector3 screenCoords) {
      if (this.worldCamera != null) {
         this.worldCamera.unproject(screenCoords);
      }
   }

   public final void beginStencilMask() {
      this.flushBatch();
      if (this.maskShader == null) {
         this.maskShader = new ShaderProgram(vertexShaderSrc, fragmentShaderSrc);
      }

      Gdx.gl.glEnable(2960);
      Gdx.gl.glStencilFunc(519, 1, 255);
      Gdx.gl.glStencilOp(7680, 7680, 7681);
      Gdx.gl.glStencilMask(255);
      Gdx.gl.glColorMask(false, false, false, false);
      this.getBatch().setShader(this.maskShader);
   }

   public final void beginMaskedRender() {
      this.flushBatch();
      this.getBatch().setShader(null);
      Gdx.gl.glStencilFunc(515, 1, 255);
      Gdx.gl.glColorMask(true, true, true, true);
   }

   public final void endStencilMask() {
      this.flushBatch();
      Gdx.gl.glDisable(2960);
      Gdx.gl.glClear(1024);
   }

   public final ShapeDrawer getShapeDrawer() {
      if (this.shapeDrawer == null) {
         this.shapeDrawer = new ShapeDrawer(this.getBatch(), new TextureRegion(createWhitePixelTexture()));
      }

      return this.shapeDrawer;
   }

   public static Texture createWhitePixelTexture() {
      Pixmap pixmap;
      (pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888)).setColor(1.0F, 1.0F, 1.0F, 1.0F);
      pixmap.drawPixel(0, 0);
      Texture texture = new Texture(pixmap);
      pixmap.dispose();
      return texture;
   }

   public final float getCurrentZoom() {
      return this.currentCamera.zoom;
   }

   public final Viewport getWorldViewport() {
      return this.worldViewport;
   }

   public final OrthographicCamera getWorldCamera() {
      return this.worldCamera;
   }

   public final OrthographicCamera getDebugCamera() {
      return this.debugCamera;
   }

   public final Viewport getDebugViewport() {
      return this.debugViewport;
   }

   public final boolean isDebugMode() {
      return this.debugMode;
   }

   public final OrthographicCamera getCurrentCamera() {
      return this.currentCamera;
   }

   public final void worldToHud(Vector3 coords) {
      if (this.currentCamera != null && this.hudCamera != null) {
         this.currentCamera.project(coords, 0.0F, 0.0F, (float)Gdx.graphics.getWidth(), (float)Gdx.graphics.getHeight());
         coords.y = (float)Gdx.graphics.getHeight() - coords.y;
         this.hudCamera.unproject(coords, 0.0F, 0.0F, (float)Gdx.graphics.getWidth(), (float)Gdx.graphics.getHeight());
      }
   }

   public final void screenToWorld(Vector3 screenCoords) {
      if (this.debugMode) {
         this.debugCamera.unproject(screenCoords);
      } else {
         this.worldCamera.unproject(screenCoords);
      }
   }

   public final boolean pushScissors(float x, float y, float width, float height) {
      this.clipRect.set(x, y, width, height);
      Rectangle clip = this.clipRect;
      this.flushBatch();
      ScissorStack.calculateScissors(this.currentCamera, this.getBatch().getTransformMatrix(), clip, this.scissorRect);
      return ScissorStack.pushScissors(this.scissorRect);
   }

   public final void popScissors() {
      this.flushBatch();
      ScissorStack.popScissors();
   }

   static {
      fragmentShaderSrc = "#ifdef GL_ES\nprecision mediump float;\n#endif\n\nvarying vec2 v_texCoords;\nuniform sampler2D u_texture;\n\nvoid main() {\n    vec4 texColor = texture2D(u_texture, v_texCoords);\n    if (texColor.a > 0.0) {\n        gl_FragColor = vec4(0.0, 0.0, 0.0, texColor.a);\n    } else {\n        discard;\n    }\n}";
      Pixmap pixmap;
      (pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888)).setColor(1.0F, 1.0F, 1.0F, 1.0F);
      pixmap.fill();
      pixmap.dispose();
      vertexShaderSrc = "attribute vec4 a_position;\nattribute vec2 a_texCoord0;\nuniform mat4 u_projTrans;\nvarying vec2 v_texCoords;\n\nvoid main() {\n    v_texCoords = a_texCoord0;\n    gl_Position = u_projTrans * a_position;\n}";
   }

}
