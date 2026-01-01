package com.tlnt.engine.particle;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;

public final class Particle implements Pool.Poolable {
   public ParticleEmitter emitter;
   public TextureAsset texture;
   public Vector2 position = new Vector2();
   public Vector2 size = new Vector2();
   public float rotation;
   public Color color = new Color();
   public float lifePercent;
   public float age;
   public float lifeDuration;
   public float[] trailXs;
   public float[] trailYs;
   public int trailCapacity;
   public int trailHead;
   public int trailCount;
   public float trailStepTimer;
   public float trailAlpha = 1.0F;
   public Mesh trailMesh;
   public float[] trailVertices;
   public short[] trailIndices;
   public final Vector2 tmpDirection = new Vector2();
   public float sizeXLow;
   public float sizeXHigh;
   public float sizeYLow;
   public float sizeYHigh;
   public float rotationLow;
   public float rotationHigh;
   public float velocityLow;
   public float velocityHigh;
   public float angleLow;
   public float angleHigh;
   public float gravityLow;
   public float gravityHigh;
   public float windLow;
   public float windHigh;
   public boolean attached;
   public int trailMaxPointsLow;
   public int trailMaxPointsHigh;
   public float trailStepTimeLow;
   public float trailStepTimeHigh;
   public float trailAlphaLow;
   public float trailAlphaHigh;
   public float bloomIntensityLow;
   public float bloomIntensityHigh;
   public float bloomSizeMultLow;
   public float bloomSizeMultHigh;

   public Particle(ParticleEmitter emitter) {
      this.emitter = emitter;
   }

   public final void update(float delta) {
      Particle self = this;
      if (!this.emitter.trailEnabled) {
         this.trailCount = 0;
         this.trailHead = 0;
         this.trailStepTimer = 0.0F;
      } else {
         int maxPointsLow = this.trailMaxPointsLow;
         int maxPointsHigh = this.trailMaxPointsHigh;
         int maxPoints;
         if ((maxPoints = Math.round(BezierUtil.evalCurve(this.lifePercent, this.emitter.trailMaxPointsCurve, (float)maxPointsLow, (float)maxPointsHigh))) < 0) {
            maxPoints = 0;
         }

         if ((maxPoints = maxPoints) <= 0) {
            this.trailCount = 0;
            this.trailHead = 0;
         } else {
            if (this.trailCapacity != maxPoints || this.trailXs == null || this.trailYs == null) {
               if (this.trailMesh != null) {
                  this.trailMesh.dispose();
                  this.trailMesh = null;
               }

               this.trailCapacity = maxPoints;
               this.trailXs = new float[maxPoints];
               this.trailYs = new float[maxPoints];
               this.trailHead = 0;
               this.trailCount = 0;
               this.trailStepTimer = 0.0F;
               maxPointsLow = maxPoints + 1 << 1;
               this.trailVertices = new float[maxPointsLow * 5];
               this.trailIndices = new short[maxPoints * 6];
               maxPointsHigh = 0;

               for (int i = 0; i < maxPoints; i++) {
                  short base = (short)(i << 1);
                  self.trailIndices[maxPointsHigh++] = base;
                  self.trailIndices[maxPointsHigh++] = (short)(base + 1);
                  int idx = maxPointsHigh++;
                  self.trailIndices[idx] = (short)(base + 2);
                  idx = maxPointsHigh++;
                  self.trailIndices[idx] = (short)(base + 2);
                  self.trailIndices[maxPointsHigh++] = (short)(base + 1);
                  self.trailIndices[maxPointsHigh++] = (short)(base + 3);
               }

               int indexCount = self.trailIndices.length;
               VertexAttribute[] attributes = new VertexAttribute[3];
               attributes[0] = new VertexAttribute(1, 2, "a_position");
               attributes[1] = new VertexAttribute(4, 4, "a_color");
               attributes[2] = new VertexAttribute(16, 2, "a_texCoord0");
               self.trailMesh = new Mesh(true, maxPointsLow, indexCount, attributes);
               self.trailMesh.setIndices(self.trailIndices);
            }

            float stepTimeLow = self.trailStepTimeLow;
            float stepTimeHigh = self.trailStepTimeHigh;
            float stepTime;
            if ((stepTime = BezierUtil.evalCurve(self.lifePercent, self.emitter.trailStepTimeCurve, stepTimeLow, stepTimeHigh)) <= 0.0F) {
               stepTime = 1.0E-4F;
            }

            float stepInterval = stepTime;
            self.trailStepTimer += delta;

            while (self.trailStepTimer >= stepInterval) {
               self.trailStepTimer -= stepInterval;
               self.trailXs[self.trailHead] = self.position.x;
               self.trailYs[self.trailHead] = self.position.y;
               self.trailHead = (self.trailHead + 1) % self.trailCapacity;
               if (self.trailCount < self.trailCapacity) {
                  self.trailCount += 1;
               }
            }

            float alphaLow = self.trailAlphaLow;
            float alphaHigh = self.trailAlphaHigh;
            float alpha;
            if ((alpha = BezierUtil.evalCurve(self.lifePercent, self.emitter.trailAlphaCurve, alphaLow, alphaHigh)) < 0.0F) {
               alpha = 0.0F;
            }

            if (alpha > 1.0F) {
               alpha = 1.0F;
            }

            self.trailAlpha = alpha;
         }
      }

      this.age += delta;
      this.lifePercent = Math.min(1.0F, this.age / this.lifeDuration);
      float sizeX = BezierUtil.evalCurve(this.lifePercent, this.emitter.sizeXCurve, this.sizeXLow, this.sizeXHigh);
      float angle = BezierUtil.evalCurve(this.lifePercent, this.emitter.sizeYCurve, this.sizeYLow, this.sizeYHigh);
      this.size.set(sizeX, angle);
      float velocity = BezierUtil.evalCurve(this.lifePercent, this.emitter.velocityCurve, this.velocityLow, this.velocityHigh);
      angle = BezierUtil.evalCurve(this.lifePercent, this.emitter.angleCurve, this.angleLow, this.angleHigh);
      float gravity = BezierUtil.evalCurve(this.lifePercent, this.emitter.gravityCurve, this.gravityLow, this.gravityHigh);
      float dx = BezierUtil.evalCurve(this.lifePercent, this.emitter.windCurve, this.windLow, this.windHigh);
      float distance;
      dx = (distance = velocity * delta) * MathUtils.cosDeg(angle) + dx * delta;
      delta = distance * MathUtils.sinDeg(angle) + gravity * delta;
      this.position.add(dx, delta);
      float rotationValue = BezierUtil.evalCurve(this.lifePercent, this.emitter.rotationCurve, this.rotationLow, this.rotationHigh);
      if (this.emitter.aligned) {
         delta = (float)Math.atan2((double)delta, (double)dx) * (180.0F / (float)Math.PI);
         this.rotation = delta + rotationValue;
      } else {
         this.rotation = rotationValue;
      }
   }

   /** Default PolygonSpriteBatch capacity (2000 vertices x 5 floats). */
   private static final int MAX_BATCHED_TRAIL_FLOATS = 2000 * 5;

   // Draw trails through the shared PolygonSpriteBatch (same vertex format x, y, color, u, v) instead of a dedicated Mesh:
   // each particle with a trail cost 1 flush + 1 draw call (~120 per frame on maps with falling leaves), the mobile web build lagged.
   private boolean drawTrailBatched(Batch batch) {
      int vertexFloats = (this.trailCount + 1 << 1) * 5;
      if (!(batch instanceof PolygonSpriteBatch) || !batch.isDrawing() || vertexFloats > MAX_BATCHED_TRAIL_FLOATS) {
         return false;
      }

      ((PolygonSpriteBatch)batch)
         .draw(this.texture.getRegion().getTexture(), this.trailVertices, 0, vertexFloats, this.trailIndices, 0, this.trailCount * 6);
      return true;
   }

   public final void draw(RenderContext ctx, float scale) {
      if (this.emitter.spriteMode == ParticleEmitter.SpriteMode.ANIMATED && this.emitter.imagePaths.size > 0 && this.emitter.animationFps > 0) {
         float frameDuration = 1.0F / (float)this.emitter.animationFps;
         int frameIndex = (int)(this.age / frameDuration) % this.emitter.imagePaths.size;
         this.texture = TextureAsset.obtain(this.emitter.imagePaths.get(frameIndex));
      }

      Color out = this.color;
      Array tintPoints = this.emitter.tintPoints;
      float t = this.lifePercent;
      if (out == null) {
         throw new IllegalArgumentException("out Color must not be null");
      } else {
         if (tintPoints != null && tintPoints.size != 0) {
            if (tintPoints.size == 1) {
               out.set(((TintPoint)tintPoints.first()).color);
            } else {
               t = MathUtils.clamp(t, 0.0F, 1.0F);
               TintPoint firstPoint = (TintPoint)tintPoints.first();
               TintPoint lastPoint = (TintPoint)tintPoints.peek();
               if (t <= firstPoint.position) {
                  out.set(firstPoint.color);
               } else if (t >= lastPoint.position) {
                  out.set(lastPoint.color);
               } else {
                  int lo = 0;
                  int hi = tintPoints.size - 1;

                  while (true) {
                     if (lo + 1 >= hi) {
                        TintPoint lower = (TintPoint)tintPoints.get(lo);
                        TintPoint upper;
                        float span;
                        float ratio = (span = (upper = (TintPoint)tintPoints.get(hi)).position - lower.position) == 0.0F
                           ? 0.0F
                           : (t - lower.position) / span;
                        out.r = lower.color.r + (upper.color.r - lower.color.r) * ratio;
                        out.g = lower.color.g + (upper.color.g - lower.color.g) * ratio;
                        out.b = lower.color.b + (upper.color.b - lower.color.b) * ratio;
                        out.a = lower.color.a + (upper.color.a - lower.color.a) * ratio;
                        break;
                     }

                     int mid = lo + hi >>> 1;
                     if (t < ((TintPoint)tintPoints.get(mid)).position) {
                        hi = mid;
                     } else {
                        lo = mid;
                     }
                  }
               }
            }
         } else {
            out.set(Color.WHITE);
         }

         float transparency = BezierUtil.evalCurve(this.lifePercent, this.emitter.transparencyCurve, 0.0F, 1.0F);
         float offsetX = this.emitter.effect.getRenderOffset().x;
         float offsetY = this.emitter.effect.getRenderOffset().y;
         Batch batch;
         int srcFunc = (batch = ctx.getBatch()).getBlendSrcFunc();
         int dstFunc = batch.getBlendDstFunc();
         if (this.emitter.trailEnabled && this.trailCount > 1 && this.trailCapacity > 0 && this.texture != null && this.trailMesh != null) {
            if (this.emitter.bloomEnabled) {
               t = BezierUtil.evalCurve(this.lifePercent, this.emitter.bloomIntensityCurve, this.bloomIntensityLow, this.bloomIntensityHigh);
               float bloomSizeMult = BezierUtil.evalCurve(this.lifePercent, this.emitter.bloomSizeMultiplierCurve, this.bloomSizeMultLow, this.bloomSizeMultHigh);
               if (t > 0.0F && bloomSizeMult > 1.0F) {
                  float bloomOffsetY = offsetY;
                  float bloomOffsetX = offsetX;
                  Particle bloomSelf = this;
                  if (this.trailCount >= 2 && this.trailVertices != null) {
                     int bloomPointCount = this.trailCount;
                     float bloomR = this.color.r;
                     float bloomG = this.color.g;
                     float bloomB = this.color.b;
                     float bloomA = this.color.a * transparency * this.trailAlpha * t * 0.8F;
                     float bloomHalfWidth;
                     if ((bloomHalfWidth = Math.max(this.size.y, this.size.x) * 0.5F * bloomSizeMult) <= 0.0F) {
                        bloomHalfWidth = 1.0F;
                     }

                     TextureRegion bloomRegion;
                     float bloomU = (bloomRegion = this.texture.getRegion()).getU();
                     float bloomV2 = bloomRegion.getV2();
                     float bloomU2 = bloomRegion.getU2();
                     float bloomV = bloomRegion.getV();
                     int bloomVertexIndex = 0;
                     float bloomPrevDirX = 0.0F;
                     float bloomPrevDirY = 1.0F;

                     for (int i = 0; i <= bloomPointCount; i++) {
                        float px;
                        float py;
                        if (i < bloomPointCount) {
                           int ringIndex;
                           if ((ringIndex = (bloomSelf.trailHead - bloomPointCount + i) % bloomSelf.trailCapacity) < 0) {
                              ringIndex += bloomSelf.trailCapacity;
                           }

                           px = bloomSelf.trailXs[ringIndex] + bloomOffsetX;
                           py = bloomSelf.trailYs[ringIndex] + bloomOffsetY;
                        } else {
                           px = bloomSelf.position.x + bloomOffsetX;
                           py = bloomSelf.position.y + bloomOffsetY;
                        }

                        if (i < bloomPointCount) {
                           float nextX;
                           float nextY;
                           if (i + 1 < bloomPointCount) {
                              int nextRingIndex;
                              if ((nextRingIndex = (bloomSelf.trailHead - bloomPointCount + i + 1) % bloomSelf.trailCapacity) < 0) {
                                 nextRingIndex += bloomSelf.trailCapacity;
                              }

                              nextX = bloomSelf.trailXs[nextRingIndex] + bloomOffsetX;
                              nextY = bloomSelf.trailYs[nextRingIndex] + bloomOffsetY;
                           } else {
                              nextX = bloomSelf.position.x + bloomOffsetX;
                              nextY = bloomSelf.position.y + bloomOffsetY;
                           }

                           bloomSelf.tmpDirection.set(nextX - px, nextY - py);
                        } else {
                           bloomSelf.tmpDirection.set(bloomPrevDirX, bloomPrevDirY);
                        }

                        float dirY;
                        float dirX;
                        if (bloomSelf.tmpDirection.len() >= 1.0E-6F) {
                           bloomSelf.tmpDirection.nor();
                           dirX = bloomSelf.tmpDirection.x;
                           dirY = bloomSelf.tmpDirection.y;
                           bloomPrevDirX = dirX;
                           bloomPrevDirY = dirY;
                        } else {
                           dirX = bloomPrevDirX;
                           dirY = bloomPrevDirY;
                        }

                        float normalX = -dirY;
                        float progress;
                        float bloomWidth = (progress = (float)i / (float)bloomPointCount) * progress;
                        bloomWidth = bloomHalfWidth * (1.0F - bloomWidth * 0.7F);
                        float alpha = bloomA * (0.3F + 0.7F * progress);
                        float colorBits = Color.toFloatBits(bloomR, bloomG, bloomB, alpha);
                        float u = bloomU + (bloomU2 - bloomU) * progress;
                        bloomSelf.trailVertices[bloomVertexIndex++] = px - normalX * bloomWidth;
                        bloomSelf.trailVertices[bloomVertexIndex++] = py - dirX * bloomWidth;
                        bloomSelf.trailVertices[bloomVertexIndex++] = colorBits;
                        bloomSelf.trailVertices[bloomVertexIndex++] = u;
                        bloomSelf.trailVertices[bloomVertexIndex++] = bloomV2;
                        bloomSelf.trailVertices[bloomVertexIndex++] = px + normalX * bloomWidth;
                        bloomSelf.trailVertices[bloomVertexIndex++] = py + dirX * bloomWidth;
                        bloomSelf.trailVertices[bloomVertexIndex++] = colorBits;
                        bloomSelf.trailVertices[bloomVertexIndex++] = u;
                        bloomSelf.trailVertices[bloomVertexIndex++] = bloomV;
                     }

                     bloomSelf.trailMesh
                        .setVertices(
                           bloomSelf.trailVertices,
                           0,
                           (bloomPointCount + 1 << 1) * 5
                        );
                  }

                  // The Mesh version below changes blending after the flush, so additive blending never took effect: batch drawing keeps the current blend.
                  if (!this.drawTrailBatched(batch)) {
                     if (batch.isDrawing()) {
                        batch.flush();
                     }

                     this.texture.getRegion().getTexture().bind();
                     ShaderProgram bloomShader = batch.getShader();
                     batch.setBlendFunction(770, 1);
                     int bloomIndexCount = this.trailCount * 6;
                     this.trailMesh.render(bloomShader, 4, 0, bloomIndexCount);
                     batch.setBlendFunction(srcFunc, dstFunc);
                  }
               }
            }

            float trailOffsetY = offsetY;
            float trailOffsetX = offsetX;
            Particle self = this;
            if (this.trailCount >= 2 && this.trailVertices != null) {
               int pointCount = this.trailCount;
               float r = this.color.r * transparency;
               float g = this.color.g * transparency;
               float b = this.color.b * transparency;
               float a = this.color.a * transparency * this.trailAlpha;
               float trailHalfWidth;
               if ((trailHalfWidth = Math.max(this.size.y, this.size.x) * 0.5F) <= 0.0F) {
                  trailHalfWidth = 1.0F;
               }

               TextureRegion region;
               float regionU = (region = this.texture.getRegion()).getU();
               float regionV2 = region.getV2();
               float regionU2 = region.getU2();
               float regionV = region.getV();
               int vertexIndex = 0;
               float prevDirX = 0.0F;
               float prevDirY = 1.0F;

               for (int j = 0; j <= pointCount; j++) {
                  float x;
                  float y;
                  if (j < pointCount) {
                     int pointIndex;
                     if ((pointIndex = (self.trailHead - pointCount + j) % self.trailCapacity) < 0) {
                        pointIndex += self.trailCapacity;
                     }

                     x = self.trailXs[pointIndex] + trailOffsetX;
                     y = self.trailYs[pointIndex] + trailOffsetY;
                  } else {
                     x = self.position.x + trailOffsetX;
                     y = self.position.y + trailOffsetY;
                  }

                  if (j < pointCount) {
                     float nextPointX;
                     float nextPointY;
                     if (j + 1 < pointCount) {
                        int headOffset = self.trailHead - pointCount + j;
                        int nextPointIndex;
                        if ((nextPointIndex = (headOffset + 1) % self.trailCapacity) < 0) {
                           nextPointIndex += self.trailCapacity;
                        }

                        nextPointX = self.trailXs[nextPointIndex] + trailOffsetX;
                        nextPointY = self.trailYs[nextPointIndex] + trailOffsetY;
                     } else {
                        nextPointX = self.position.x + trailOffsetX;
                        nextPointY = self.position.y + trailOffsetY;
                     }

                     self.tmpDirection.set(nextPointX - x, nextPointY - y);
                  } else {
                     self.tmpDirection.set(prevDirX, prevDirY);
                  }

                  float trailDirX;
                  float trailDirY;
                  if (self.tmpDirection.len() >= 1.0E-6F) {
                     self.tmpDirection.nor();
                     trailDirX = self.tmpDirection.x;
                     trailDirY = self.tmpDirection.y;
                     prevDirX = trailDirX;
                     prevDirY = trailDirY;
                  } else {
                     trailDirX = prevDirX;
                     trailDirY = prevDirY;
                  }

                  float trailNormalX = -trailDirY;
                  float trailProgress;
                  float progressSq = (trailProgress = (float)j / (float)pointCount) * trailProgress;
                  float trailWidth = trailHalfWidth * (1.0F - progressSq * 0.7F);
                  float trailColor = a * trailProgress;
                  trailColor = Color.toFloatBits(r, g, b, trailColor);
                  float trailU = regionU + (regionU2 - regionU) * trailProgress;
                  self.trailVertices[vertexIndex++] = x - trailNormalX * trailWidth;
                  self.trailVertices[vertexIndex++] = y - trailDirX * trailWidth;
                  self.trailVertices[vertexIndex++] = trailColor;
                  self.trailVertices[vertexIndex++] = trailU;
                  self.trailVertices[vertexIndex++] = regionV2;
                  self.trailVertices[vertexIndex++] = x + trailNormalX * trailWidth;
                  self.trailVertices[vertexIndex++] = y + trailDirX * trailWidth;
                  self.trailVertices[vertexIndex++] = trailColor;
                  self.trailVertices[vertexIndex++] = trailU;
                  self.trailVertices[vertexIndex++] = regionV;
               }

               int vertexPairs = pointCount + 1;
               self.trailMesh
                  .setVertices(self.trailVertices, 0, (vertexPairs << 1) * 5);
            }

            if (!this.drawTrailBatched(batch)) {
               if (batch.isDrawing()) {
                  batch.flush();
               }

               this.texture.getRegion().getTexture().bind();
               ShaderProgram shader = batch.getShader();
               int indexCount = this.trailCount * 6;
               this.trailMesh.render(shader, 4, 0, indexCount);
            }
         }

         int drawSprite;
         label161: {
            if (this.emitter.trailEnabled) {
               if (this.trailCount > 2) {
                  drawSprite = 0;
                  break label161;
               }
            }

            drawSprite = 1;
         }

         int spriteBloom = drawSprite;
         if (this.emitter.bloomEnabled && spriteBloom != 0) {
            float bloomIntensity = BezierUtil.evalCurve(this.lifePercent, this.emitter.bloomIntensityCurve, this.bloomIntensityLow, this.bloomIntensityHigh);
            t = BezierUtil.evalCurve(this.lifePercent, this.emitter.bloomSizeMultiplierCurve, this.bloomSizeMultLow, this.bloomSizeMultHigh);
            if (bloomIntensity > 0.0F && t > 1.0F) {
               batch.setBlendFunction(770, 1);
               float bloomAlpha = this.color.a * transparency * bloomIntensity;
               ctx.setBatchColorRgba(this.color.r, this.color.g, this.color.b, bloomAlpha);
               ctx.drawTextureRotatedFlipped(
                  this.texture,
                  this.position.x + offsetX,
                  this.position.y + offsetY,
                  this.size.x * t * scale,
                  this.size.y * t * scale,
                  this.rotation,
                  false,
                  false,
                  Anchor.CENTER
               );
               batch.setBlendFunction(srcFunc, dstFunc);
               ctx.resetBatchColor();
            }
         }

         if (this.emitter.trailEnabled && this.trailCount > 1) {
            drawSprite = 0;
         } else {
            drawSprite = 1;
         }

         if (drawSprite != 0) {
            ctx.setBatchColorRgba(this.color.r * transparency, this.color.g * transparency, this.color.b * transparency, this.color.a * transparency);
            float drawX = this.position.x + offsetX;
            float drawY = this.position.y + offsetY;
            float drawWidth = this.size.x * scale;
            float drawHeight = this.size.y * scale;
            ctx.drawTextureRotatedFlipped(
               this.texture,
               drawX,
               drawY,
               drawWidth,
               drawHeight,
               this.rotation,
               false,
               false,
               Anchor.CENTER
            );
            ctx.resetBatchColor();
         }
      }
   }

   public final void translate(Vector2 offset) {
      this.position.add(offset);
      if (this.trailCount > 0 && this.trailCapacity > 0) {

         for (int i = 0; i < this.trailCount; i++) {
            int index;
            if ((index = (this.trailHead - this.trailCount + i) % this.trailCapacity) < 0) {
               index += this.trailCapacity;
            }

            this.trailXs[index] = this.trailXs[index] + offset.x;
            this.trailYs[index] = this.trailYs[index] + offset.y;
         }
      }
   }

   @Override
   public final void reset() {
      this.position.set(Vector2.Zero);
      this.size.set(Vector2.Zero);
      this.age = 0.0F;
      this.rotation = 0.0F;
      this.trailHead = 0;
      this.trailCount = 0;
      this.trailStepTimer = 0.0F;
      this.trailAlpha = 1.0F;
   }

}
