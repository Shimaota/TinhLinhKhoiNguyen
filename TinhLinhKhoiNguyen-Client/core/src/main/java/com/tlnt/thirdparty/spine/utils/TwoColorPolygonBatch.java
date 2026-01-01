package com.tlnt.thirdparty.spine.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.g2d.PolygonBatch;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Null;

public final class TwoColorPolygonBatch implements PolygonBatch {
   public final Mesh mesh;
   public final float[] vertices;
   public final short[] triangles;
   public final Matrix4 transformMatrix = new Matrix4();
   public final Matrix4 projectionMatrix = new Matrix4();
   public final Matrix4 combinedMatrix = new Matrix4();
   public boolean blendingDisabled;
   public ShaderProgram defaultShader;
   public boolean ownsShader;
   public ShaderProgram shader;
   public int vertexIndex;
   public int triangleIndex;
   @Null
   public Texture lastTexture;
   public float invTexWidth = 0.0F;
   public float invTexHeight = 0.0F;
   public boolean drawing;
   public int blendSrcFunc = 770;
   public int blendDstFunc;
   public int blendSrcFuncAlpha;
   public int blendDstFuncAlpha;
   public boolean premultipliedAlpha;
   public final Color lightColor;
   public final Color darkColor;
   public float lightPacked;
   public float darkPacked;
   public int totalRenderCalls;

   public TwoColorPolygonBatch() {
      this(2000);
   }

   public TwoColorPolygonBatch(int size) {
      this(2000, 4000, null);
   }

   public TwoColorPolygonBatch(int maxVertices, int maxTriangles, @Null ShaderProgram defaultShader) {
      this.blendDstFunc = 771;
      this.blendSrcFuncAlpha = 770;
      this.blendDstFuncAlpha = 771;
      this.lightColor = new Color(1.0F, 1.0F, 1.0F, 1.0F);
      this.darkColor = new Color(0.0F, 0.0F, 0.0F, 1.0F);
      this.lightPacked = Color.WHITE.toFloatBits();
      this.darkPacked = Color.BLACK.toFloatBits();
      this.totalRenderCalls = 0;
      if (maxVertices > 32767) {
         throw new IllegalArgumentException("Can't have more than 32767 vertices per batch: " + maxTriangles);
      } else {
         Mesh.VertexDataType vertexDataType = Mesh.VertexDataType.VertexArray;
         if (Gdx.gl30 != null) {
            vertexDataType = Mesh.VertexDataType.VertexBufferObjectWithVAO;
         }

         int maxIndices = maxTriangles * 3;
         VertexAttribute[] attributes = new VertexAttribute[4];
         attributes[0] = new VertexAttribute(1, 2, "a_position");
         attributes[1] = new VertexAttribute(4, 4, "a_light");
         attributes[2] = new VertexAttribute(4, 4, "a_dark");
         attributes[3] = new VertexAttribute(16, 2, "a_texCoord0");
         this.mesh = new Mesh(vertexDataType, false, maxVertices, maxIndices, attributes);
         this.vertices = new float[maxVertices * 6];
         this.triangles = new short[maxTriangles * 3];
         this.ownsShader = true;
         if (this.ownsShader) {
            String vertexShader = "attribute vec4 a_position;\nattribute vec4 a_light;\nattribute vec4 a_dark;\nattribute vec2 a_texCoord0;\nuniform mat4 u_projTrans;\nvarying vec4 v_light;\nvarying vec4 v_dark;\nvarying vec2 v_texCoords;\n\nvoid main()\n{\n  v_light = a_light;\n  v_light.a = v_light.a * (255.0/254.0);\n  v_dark = a_dark;\n  v_texCoords = a_texCoord0;\n  gl_Position = u_projTrans * a_position;\n}\n";
            String fragmentShader = "#ifdef GL_ES\n#define LOWP lowp\nprecision mediump float;\n#else\n#define LOWP \n#endif\nvarying LOWP vec4 v_light;\nvarying LOWP vec4 v_dark;\nuniform float u_pma;\nvarying vec2 v_texCoords;\nuniform sampler2D u_texture;\nvoid main()\n{\n  vec4 texColor = texture2D(u_texture, v_texCoords);\n  gl_FragColor.a = texColor.a * v_light.a;\n  gl_FragColor.rgb = ((texColor.a - 1.0) * u_pma + 1.0 - texColor.rgb) * v_dark.rgb + texColor.rgb * v_light.rgb;\n}";
            ShaderProgram program;
            if (!(program = new ShaderProgram(vertexShader, fragmentShader)).isCompiled()) {
               throw new IllegalArgumentException("Error compiling shader: " + program.getLog());
            }

            defaultShader = program;
         }

         this.defaultShader = defaultShader;
         this.shader = defaultShader;
         this.projectionMatrix.setToOrtho2D(0.0F, 0.0F, (float)Gdx.graphics.getWidth(), (float)Gdx.graphics.getHeight());
      }
   }

   @Override
   public final void begin() {
      if (this.drawing) {
         throw new IllegalStateException("end must be called before begin.");
      } else {
         Gdx.gl.glDepthMask(false);
         this.shader.bind();
         this.setupMatrices();
         this.drawing = true;
      }
   }

   @Override
   public final void end() {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before end.");
      } else {
         if (this.vertexIndex > 0) {
            this.flush();
         }

         Gdx.gl.glDepthMask(true);
         if (!this.blendingDisabled) {
            Gdx.gl.glDisable(3042);
         }

         this.lastTexture = null;
         this.drawing = false;
      }
   }

   @Override
   public final void setColor(Color tint) {
      this.lightColor.set(tint);
      this.lightPacked = tint.toFloatBits();
   }

   @Override
   public final void setColor(float r, float g, float b, float a) {
      this.lightColor.set(r, g, b, a);
      this.lightPacked = this.lightColor.toFloatBits();
   }

   @Override
   public final void setPackedColor(float packedColor) {
      Color.abgr8888ToColor(this.lightColor, packedColor);
      this.lightPacked = packedColor;
   }

   @Override
   public final Color getColor() {
      return this.lightColor;
   }

   @Override
   public final float getPackedColor() {
      return this.lightPacked;
   }

   @Override
   public final void draw(PolygonRegion region, float x, float y) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         short[] regionTriangles;
         int regionTrianglesLength = (regionTriangles = region.getTriangles()).length;
         float[] regionVertices;
         int regionVerticesLength = (regionVertices = region.getVertices()).length;
         Texture texture;
         if ((texture = region.getRegion().getTexture()) != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            label34: {
               if (this.triangleIndex + regionTrianglesLength <= triangles.length) {
                  if (this.vertexIndex + regionVerticesLength * 6 / 2 <= this.vertices.length) {
                     break label34;
                  }
               }

               this.flush();
            }
         }

         int triangleIndex = this.triangleIndex;
         int vertexIndex = this.vertexIndex;
         int startVertex = this.vertexIndex / 6;

         for (int i = 0; i < regionTrianglesLength; i++) {
            triangles[triangleIndex++] = (short)(regionTriangles[i] + startVertex);
         }

         this.triangleIndex = triangleIndex;
         float[] vertices = this.vertices;
         float light = this.lightPacked;
         float dark = this.darkPacked;
         float[] textureCoords = region.getTextureCoords();

         for (int ii = 0; ii < regionVerticesLength; ii += 2) {
            vertices[vertexIndex++] = regionVertices[ii] + x;
            int index = vertexIndex++;
            vertices[index] = regionVertices[ii + 1] + y;
            vertices[vertexIndex++] = light;
            vertices[vertexIndex++] = dark;
            vertices[vertexIndex++] = textureCoords[ii];
            vertices[vertexIndex++] = textureCoords[ii + 1];
         }

         this.vertexIndex = vertexIndex;
      }
   }

   @Override
   public final void draw(PolygonRegion region, float x, float y, float width, float height) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         short[] regionTriangles;
         int regionTrianglesLength = (regionTriangles = region.getTriangles()).length;
         float[] regionVertices;
         int regionVerticesLength = (regionVertices = region.getVertices()).length;
         TextureRegion textureRegion;
         Texture texture;
         if ((texture = (textureRegion = region.getRegion()).getTexture()) != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            label34: {
               if (this.triangleIndex + regionTrianglesLength <= triangles.length) {
                  int vertexFloats = regionVerticesLength * 6;
                  if (this.vertexIndex + vertexFloats / 2 <= this.vertices.length) {
                     break label34;
                  }
               }

               this.flush();
            }
         }

         regionTrianglesLength = this.triangleIndex;
         int vertexIndex = this.vertexIndex;
         int startVertex = this.vertexIndex / 6;
         int i = 0;

         for (int n = regionTriangles.length; i < n; i++) {
            triangles[regionTrianglesLength++] = (short)(regionTriangles[i] + startVertex);
         }

         this.triangleIndex = regionTrianglesLength;
         float[] vertices = this.vertices;
         float light = this.lightPacked;
         float dark = this.darkPacked;
         float[] textureCoords = region.getTextureCoords();
         width /= (float)textureRegion.getRegionWidth();
         height /= (float)textureRegion.getRegionHeight();

         for (int ii = 0; ii < regionVerticesLength; ii += 2) {
            vertices[vertexIndex++] = regionVertices[ii] * width + x;
            int index = vertexIndex++;
            vertices[index] = regionVertices[ii + 1] * height + y;
            vertices[vertexIndex++] = light;
            vertices[vertexIndex++] = dark;
            vertices[vertexIndex++] = textureCoords[ii];
            index = vertexIndex++;
            vertices[index] = textureCoords[ii + 1];
         }

         this.vertexIndex = vertexIndex;
      }
   }

   @Override
   public final void draw(PolygonRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         short[] regionTriangles;
         int regionTrianglesLength = (regionTriangles = region.getTriangles()).length;
         float[] regionVertices;
         int regionVerticesLength = (regionVertices = region.getVertices()).length;
         TextureRegion textureRegion;
         Texture texture;
         if ((texture = (textureRegion = region.getRegion()).getTexture()) != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            label34: {
               if (this.triangleIndex + regionTrianglesLength <= triangles.length) {
                  if (this.vertexIndex + regionVerticesLength * 6 / 2 <= this.vertices.length) {
                     break label34;
                  }
               }

               this.flush();
            }
         }

         int triangleIndex = this.triangleIndex;
         int vertexIndex = this.vertexIndex;
         int startVertex = this.vertexIndex / 6;

         for (int i = 0; i < regionTrianglesLength; i++) {
            triangles[triangleIndex++] = (short)(regionTriangles[i] + startVertex);
         }

         this.triangleIndex = triangleIndex;
         float[] vertices = this.vertices;
         float light = this.lightPacked;
         float dark = this.darkPacked;
         float[] textureCoords = region.getTextureCoords();
         x += originX;
         y += originY;
         width /= (float)textureRegion.getRegionWidth();
         height /= (float)textureRegion.getRegionHeight();
         float cos = SpineUtils.cosDeg(rotation);
         rotation = SpineUtils.sinDeg(rotation);

         for (int ii = 0; ii < regionVerticesLength; ii += 2) {
            float fx = (regionVertices[ii] * width - originX) * scaleX;
            float fy = (regionVertices[ii + 1] * height - originY) * scaleY;
            vertices[vertexIndex++] = cos * fx - rotation * fy + x;
            vertices[vertexIndex++] = rotation * fx + cos * fy + y;
            vertices[vertexIndex++] = light;
            vertices[vertexIndex++] = dark;
            vertices[vertexIndex++] = textureCoords[ii];
            int index = vertexIndex++;
            vertices[index] = textureCoords[ii + 1];
         }

         this.vertexIndex = vertexIndex;
      }
   }

   @Override
   public final void draw(
      Texture texture,
      float x,
      float y,
      float originX,
      float originY,
      float width,
      float height,
      float scaleX,
      float scaleY,
      float rotation,
      int srcX,
      int srcY,
      int srcWidth,
      int srcHeight,
      boolean flipX,
      boolean flipY
   ) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            label37: {
               if (this.triangleIndex + 6 <= triangles.length) {
                  if (this.vertexIndex + 24 <= vertices.length) {
                     break label37;
                  }
               }

               this.flush();
            }
         }

         int triangleIndex = this.triangleIndex;
         int startVertex = this.vertexIndex / 6;
         triangles[triangleIndex++] = (short)startVertex;
         triangles[triangleIndex++] = (short)(startVertex + 1);
         triangles[triangleIndex++] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 3);
         triangles[triangleIndex++] = (short)startVertex;
         this.triangleIndex = triangleIndex;
         float worldOriginX = x + originX;
         x = y + originY;
         y = -originX;
         float fy = -originY;
         originX = width - originX;
         originY = height - originY;
         if (scaleX != 1.0F || scaleY != 1.0F) {
            y *= scaleX;
            fy *= scaleY;
            originX *= scaleX;
            originY *= scaleY;
         }

         if (rotation != 0.0F) {
            float cos = SpineUtils.cosDeg(rotation);
            float sin = SpineUtils.sinDeg(rotation);
            rotation = cos * y - sin * fy;
            width = sin * y + cos * fy;
            height = cos * y - sin * originY;
            y = sin * y + cos * originY;
            scaleX = cos * originX - sin * originY;
            originY = sin * originX + cos * originY;
            originX = rotation + (scaleX - height);
            scaleY = originY - (y - width);
         } else {
            rotation = y;
            width = fy;
            height = y;
            y = originY;
            scaleX = originX;
            originY = originY;
            originX = originX;
            scaleY = fy;
         }

         rotation += worldOriginX;
         width += x;
         height += worldOriginX;
         y += x;
         scaleX += worldOriginX;
         originY += x;
         originX += worldOriginX;
         scaleY += x;
         float u = (float)srcX * this.invTexWidth;
         float v = (float)(srcY + srcHeight) * this.invTexHeight;
         float u2 = (float)(srcX + srcWidth) * this.invTexWidth;
         x = (float)srcY * this.invTexHeight;
         if (flipX) {
            float tmpU = u;
            u = u2;
            u2 = tmpU;
         }

         if (flipY) {
            float tmpV = v;
            v = x;
            x = tmpV;
         }

         float light = this.lightPacked;
         float dark = this.darkPacked;
         srcWidth = this.vertexIndex;
         vertices[srcWidth++] = rotation;
         vertices[srcWidth++] = width;
         vertices[srcWidth++] = light;
         vertices[srcWidth++] = dark;
         vertices[srcWidth++] = u;
         vertices[srcWidth++] = v;
         vertices[srcWidth++] = height;
         vertices[srcWidth++] = y;
         vertices[srcWidth++] = light;
         vertices[srcWidth++] = dark;
         vertices[srcWidth++] = u;
         vertices[srcWidth++] = x;
         vertices[srcWidth++] = scaleX;
         vertices[srcWidth++] = originY;
         vertices[srcWidth++] = light;
         vertices[srcWidth++] = dark;
         vertices[srcWidth++] = u2;
         vertices[srcWidth++] = x;
         vertices[srcWidth++] = originX;
         vertices[srcWidth++] = scaleY;
         vertices[srcWidth++] = light;
         vertices[srcWidth++] = dark;
         vertices[srcWidth++] = u2;
         vertices[srcWidth++] = v;
         this.vertexIndex = srcWidth;
      }
   }

   @Override
   public final void draw(Texture texture, float x, float y, float width, float height, int srcX, int srcY, int srcWidth, int srcHeight, boolean flipX, boolean flipY) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.triangleIndex + 6 > triangles.length || this.vertexIndex + 24 > vertices.length) {
            this.flush();
         }

         int triangleIndex = this.triangleIndex;
         int startVertex = this.vertexIndex / 6;
         triangles[triangleIndex++] = (short)startVertex;
         triangles[triangleIndex++] = (short)(startVertex + 1);
         triangles[triangleIndex++] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 3);
         triangles[triangleIndex++] = (short)startVertex;
         this.triangleIndex = triangleIndex;
         float u = (float)srcX * this.invTexWidth;
         float v = (float)(srcY + srcHeight) * this.invTexHeight;
         float u2 = (float)(srcX + srcWidth) * this.invTexWidth;
         float v2 = (float)srcY * this.invTexHeight;
         width = x + width;
         height = y + height;
         if (flipX) {
            float tmpU = u;
            u = u2;
            u2 = tmpU;
         }

         if (flipY) {
            float tmpV = v;
            v = v2;
            v2 = tmpV;
         }

         float light = this.lightPacked;
         float dark = this.darkPacked;
         int vertexIndex = this.vertexIndex;
         vertices[vertexIndex++] = x;
         vertices[vertexIndex++] = y;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u;
         vertices[vertexIndex++] = v;
         vertices[vertexIndex++] = x;
         vertices[vertexIndex++] = height;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u;
         vertices[vertexIndex++] = v2;
         vertices[vertexIndex++] = width;
         vertices[vertexIndex++] = height;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u2;
         vertices[vertexIndex++] = v2;
         vertices[vertexIndex++] = width;
         vertices[vertexIndex++] = y;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u2;
         vertices[vertexIndex++] = v;
         this.vertexIndex = vertexIndex;
      }
   }

   @Override
   public final void draw(Texture texture, float x, float y, int srcX, int srcY, int srcWidth, int srcHeight) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            if (this.triangleIndex + 6 > triangles.length || this.vertexIndex + 24 > vertices.length) {
               this.flush();
            }
         }

         int triangleIndex = this.triangleIndex;
         int startVertex = this.vertexIndex / 6;
         triangles[triangleIndex++] = (short)startVertex;
         int index = triangleIndex++;
         triangles[index] = (short)(startVertex + 1);
         triangles[triangleIndex++] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 3);
         triangles[triangleIndex++] = (short)startVertex;
         this.triangleIndex = triangleIndex;
         float u = (float)srcX * this.invTexWidth;
         float v = (float)(srcY + srcHeight) * this.invTexHeight;
         float u2 = (float)(srcX + srcWidth) * this.invTexWidth;
         float v2 = (float)srcY * this.invTexHeight;
         float fx2 = x + (float)srcWidth;
         float fy2 = y + (float)srcHeight;
         float light = this.lightPacked;
         float dark = this.darkPacked;
         int vertexIndex = this.vertexIndex;
         vertices[vertexIndex++] = x;
         vertices[vertexIndex++] = y;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u;
         vertices[vertexIndex++] = v;
         vertices[vertexIndex++] = x;
         vertices[vertexIndex++] = fy2;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u;
         vertices[vertexIndex++] = v2;
         vertices[vertexIndex++] = fx2;
         vertices[vertexIndex++] = fy2;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u2;
         vertices[vertexIndex++] = v2;
         vertices[vertexIndex++] = fx2;
         vertices[vertexIndex++] = y;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u2;
         vertices[vertexIndex++] = v;
         this.vertexIndex = vertexIndex;
      }
   }

   @Override
   public final void draw(Texture texture, float x, float y, float width, float height, float u, float v, float u2, float v2) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.triangleIndex + 6 > triangles.length
            || this.vertexIndex + 24 > vertices.length) {
            this.flush();
         }

         int triangleIndex = this.triangleIndex;
         int startVertex = this.vertexIndex / 6;
         triangles[triangleIndex++] = (short)startVertex;
         triangles[triangleIndex++] = (short)(startVertex + 1);
         triangles[triangleIndex++] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 3);
         triangles[triangleIndex++] = (short)startVertex;
         this.triangleIndex = triangleIndex;
         float fx2 = x + width;
         width = y + height;
         height = this.lightPacked;
         float dark = this.darkPacked;
         startVertex = this.vertexIndex;
         vertices[startVertex++] = x;
         vertices[startVertex++] = y;
         vertices[startVertex++] = height;
         vertices[startVertex++] = dark;
         vertices[startVertex++] = u;
         vertices[startVertex++] = v;
         vertices[startVertex++] = x;
         vertices[startVertex++] = width;
         vertices[startVertex++] = height;
         vertices[startVertex++] = dark;
         vertices[startVertex++] = u;
         vertices[startVertex++] = v2;
         vertices[startVertex++] = fx2;
         vertices[startVertex++] = width;
         vertices[startVertex++] = height;
         vertices[startVertex++] = dark;
         vertices[startVertex++] = u2;
         vertices[startVertex++] = v2;
         vertices[startVertex++] = fx2;
         vertices[startVertex++] = y;
         vertices[startVertex++] = height;
         vertices[startVertex++] = dark;
         vertices[startVertex++] = u2;
         vertices[startVertex++] = v;
         this.vertexIndex = startVertex;
      }
   }

   @Override
   public final void draw(Texture texture, float x, float y) {
      this.draw(texture, x, y, (float)texture.getWidth(), (float)texture.getHeight());
   }

   @Override
   public final void draw(Texture texture, float x, float y, float width, float height) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            label16: {
               if (this.triangleIndex + 6 <= triangles.length) {
                  if (this.vertexIndex + 24 <= vertices.length) {
                     break label16;
                  }
               }

               this.flush();
            }
         }

         int triangleIndex = this.triangleIndex;
         int startVertex = this.vertexIndex / 6;
         triangles[triangleIndex++] = (short)startVertex;
         int index = triangleIndex++;
         triangles[index] = (short)(startVertex + 1);
         index = triangleIndex++;
         triangles[index] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 3);
         triangles[triangleIndex++] = (short)startVertex;
         this.triangleIndex = triangleIndex;
         float fx2 = x + width;
         width = y + height;
         height = this.lightPacked;
         float dark = this.darkPacked;
         startVertex = this.vertexIndex;
         vertices[startVertex++] = x;
         vertices[startVertex++] = y;
         vertices[startVertex++] = height;
         vertices[startVertex++] = dark;
         vertices[startVertex++] = 0.0F;
         vertices[startVertex++] = 1.0F;
         vertices[startVertex++] = x;
         vertices[startVertex++] = width;
         vertices[startVertex++] = height;
         vertices[startVertex++] = dark;
         vertices[startVertex++] = 0.0F;
         vertices[startVertex++] = 0.0F;
         vertices[startVertex++] = fx2;
         vertices[startVertex++] = width;
         vertices[startVertex++] = height;
         vertices[startVertex++] = dark;
         vertices[startVertex++] = 1.0F;
         vertices[startVertex++] = 0.0F;
         vertices[startVertex++] = fx2;
         vertices[startVertex++] = y;
         vertices[startVertex++] = height;
         vertices[startVertex++] = dark;
         vertices[startVertex++] = 1.0F;
         vertices[startVertex++] = 1.0F;
         this.vertexIndex = startVertex;
      }
   }

   public final void drawTwoColor(Texture texture, float[] polygonVertices, int verticesOffset, int verticesCount, short[] polygonTriangles, int trianglesOffset, int trianglesCount) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.triangleIndex + trianglesCount > triangles.length || this.vertexIndex + verticesCount > vertices.length) {
            this.flush();
         }

         int triangleIndex = this.triangleIndex;
         int vertexIndex = this.vertexIndex;
         int startVertex = this.vertexIndex / 6;
         int i = 0;

         for (int n = trianglesCount + 0; i < n; i++) {
            triangles[triangleIndex++] = (short)(polygonTriangles[i] + startVertex);
         }

         this.triangleIndex = triangleIndex;
         SpineUtils.arraycopy(polygonVertices, 0, vertices, vertexIndex, verticesCount);
         this.vertexIndex += verticesCount;
      }
   }

   @Override
   public final void draw(Texture texture, float[] polygonVertices, int verticesOffset, int verticesCount, short[] polygonTriangles, int trianglesOffset, int trianglesCount) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            label34: {
               if (this.triangleIndex + trianglesCount <= triangles.length) {
                  int vertexCount = verticesCount / 5;
                  if (this.vertexIndex + vertexCount * 6 <= vertices.length) {
                     break label34;
                  }
               }

               this.flush();
            }
         }

         int triangleIndex = this.triangleIndex;
         int startVertex = this.vertexIndex;
         startVertex = this.vertexIndex / 6;
         int vertexIndex = trianglesOffset;

         for (int n = trianglesOffset + trianglesCount; vertexIndex < n; vertexIndex++) {
            triangles[triangleIndex++] = (short)(polygonTriangles[vertexIndex] + startVertex);
         }

         this.triangleIndex = triangleIndex;
         vertexIndex = this.vertexIndex;
         trianglesOffset = verticesOffset;

         for (int nn = verticesOffset + verticesCount; trianglesOffset < nn; trianglesOffset += 5) {
            vertices[vertexIndex++] = polygonVertices[trianglesOffset];
            int index = vertexIndex++;
            vertices[index] = polygonVertices[trianglesOffset + 1];
            index = vertexIndex++;
            vertices[index] = polygonVertices[trianglesOffset + 2];
            vertices[vertexIndex++] = 0.0F;
            index = vertexIndex++;
            vertices[index] = polygonVertices[trianglesOffset + 3];
            index = vertexIndex++;
            vertices[index] = polygonVertices[trianglesOffset + 4];
         }

         this.vertexIndex = vertexIndex;
      }
   }

   @Override
   public final void draw(Texture texture, float[] spriteVertices, int offset, int count) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         int spriteCount = count / 20;
         int triangleCount = spriteCount * 6;
         if (texture != this.lastTexture) {
            this.switchTexture(texture);
         } else if (this.triangleIndex + triangleCount > triangles.length || this.vertexIndex + count / 5 * 6 > vertices.length) {
            this.flush();
         }

         int vertexIndex = this.vertexIndex;
         int triangleIndex = this.triangleIndex;
         short vertex = (short)(vertexIndex / 6);

         for (int n = triangleIndex + triangleCount; triangleIndex < n; vertex = (short)(vertex + 4)) {
            triangles[triangleIndex] = vertex;
            triangles[triangleIndex + 1] = (short)(vertex + 1);
            int tIndex = triangleIndex + 2;
            triangles[tIndex] = (short)(vertex + 2);
            triangles[triangleIndex + 3] = (short)(vertex + 2);
            triangles[triangleIndex + 4] = (short)(vertex + 3);
            triangles[triangleIndex + 5] = vertex;
            triangleIndex += 6;
         }

         this.triangleIndex = triangleIndex;
         triangleCount = this.vertexIndex;
         int i = offset;

         for (int nn = offset + count; i < nn; i += 5) {
            vertices[triangleCount++] = spriteVertices[i];
            int index = triangleCount++;
            vertices[index] = spriteVertices[i + 1];
            index = triangleCount++;
            vertices[index] = spriteVertices[i + 2];
            vertices[triangleCount++] = 0.0F;
            vertices[triangleCount++] = spriteVertices[i + 3];
            vertices[triangleCount++] = spriteVertices[i + 4];
         }

         this.vertexIndex = triangleCount;
      }
   }

   @Override
   public final void draw(TextureRegion region, float x, float y) {
      this.draw(region, x, y, (float)region.getRegionWidth(), (float)region.getRegionHeight());
   }

   @Override
   public final void draw(TextureRegion region, float x, float y, float width, float height) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         Texture texture;
         if ((texture = region.getTexture()) != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            label16: {
               if (this.triangleIndex + 6 <= triangles.length) {
                  if (this.vertexIndex + 24 <= vertices.length) {
                     break label16;
                  }
               }

               this.flush();
            }
         }

         int triangleIndex = this.triangleIndex;
         int startVertex = this.vertexIndex / 6;
         triangles[triangleIndex++] = (short)startVertex;
         triangles[triangleIndex++] = (short)(startVertex + 1);
         int index = triangleIndex++;
         triangles[index] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 2);
         index = triangleIndex++;
         triangles[index] = (short)(startVertex + 3);
         triangles[triangleIndex++] = (short)startVertex;
         this.triangleIndex = triangleIndex;
         width = x + width;
         height = y + height;
         float u = region.getU();
         float v = region.getV2();
         float u2 = region.getU2();
         float v2 = region.getV();
         float light = this.lightPacked;
         float dark = this.darkPacked;
         int vertexIndex = this.vertexIndex;
         vertices[vertexIndex++] = x;
         vertices[vertexIndex++] = y;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u;
         vertices[vertexIndex++] = v;
         vertices[vertexIndex++] = x;
         vertices[vertexIndex++] = height;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u;
         vertices[vertexIndex++] = v2;
         vertices[vertexIndex++] = width;
         vertices[vertexIndex++] = height;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u2;
         vertices[vertexIndex++] = v2;
         vertices[vertexIndex++] = width;
         vertices[vertexIndex++] = y;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u2;
         vertices[vertexIndex++] = v;
         this.vertexIndex = vertexIndex;
      }
   }

   @Override
   public final void draw(TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         Texture texture;
         if ((texture = region.getTexture()) != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            label29: {
               if (this.triangleIndex + 6 <= triangles.length) {
                  if (this.vertexIndex + 24 <= vertices.length) {
                     break label29;
                  }
               }

               this.flush();
            }
         }

         int triangleIndex = this.triangleIndex;
         int startVertex = this.vertexIndex / 6;
         triangles[triangleIndex++] = (short)startVertex;
         int index = triangleIndex++;
         triangles[index] = (short)(startVertex + 1);
         index = triangleIndex++;
         triangles[index] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 2);
         index = triangleIndex++;
         triangles[index] = (short)(startVertex + 3);
         triangles[triangleIndex++] = (short)startVertex;
         this.triangleIndex = triangleIndex;
         x += originX;
         y += originY;
         float fx = -originX;
         float fy = -originY;
         originX = width - originX;
         originY = height - originY;
         if (scaleX != 1.0F || scaleY != 1.0F) {
            fx *= scaleX;
            fy *= scaleY;
            originX *= scaleX;
            originY *= scaleY;
         }

         float y4;
         if (rotation != 0.0F) {
            float cos = SpineUtils.cosDeg(rotation);
            float sin = SpineUtils.sinDeg(rotation);
            rotation = cos * fx - sin * fy;
            width = sin * fx + cos * fy;
            height = cos * fx - sin * originY;
            scaleX = sin * fx + cos * originY;
            scaleY = cos * originX - sin * originY;
            originY = sin * originX + cos * originY;
            originX = rotation + (scaleY - height);
            y4 = originY - (scaleX - width);
         } else {
            rotation = fx;
            width = fy;
            height = fx;
            scaleX = originY;
            scaleY = originX;
            originY = originY;
            originX = originX;
            y4 = fy;
         }

         rotation += x;
         width += y;
         height += x;
         scaleX += y;
         scaleY += x;
         originY += y;
         originX += x;
         float y4World = y4 + y;
         float u = region.getU();
         float v = region.getV2();
         x = region.getU2();
         float v2 = region.getV();
         y = this.lightPacked;
         float dark = this.darkPacked;
         int vertexIndex = this.vertexIndex;
         vertices[vertexIndex++] = rotation;
         vertices[vertexIndex++] = width;
         vertices[vertexIndex++] = y;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u;
         vertices[vertexIndex++] = v;
         vertices[vertexIndex++] = height;
         vertices[vertexIndex++] = scaleX;
         vertices[vertexIndex++] = y;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u;
         vertices[vertexIndex++] = v2;
         vertices[vertexIndex++] = scaleY;
         vertices[vertexIndex++] = originY;
         vertices[vertexIndex++] = y;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = x;
         vertices[vertexIndex++] = v2;
         vertices[vertexIndex++] = originX;
         vertices[vertexIndex++] = y4World;
         vertices[vertexIndex++] = y;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = x;
         vertices[vertexIndex++] = v;
         this.vertexIndex = vertexIndex;
      }
   }

   @Override
   public final void draw(
      TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation, boolean clockwise
   ) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         Texture texture;
         if ((texture = region.getTexture()) != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            label34: {
               if (this.triangleIndex + 6 <= triangles.length) {
                  if (this.vertexIndex + 24 <= vertices.length) {
                     break label34;
                  }
               }

               this.flush();
            }
         }

         int triangleIndex = this.triangleIndex;
         int startVertex = this.vertexIndex / 6;
         triangles[triangleIndex++] = (short)startVertex;
         triangles[triangleIndex++] = (short)(startVertex + 1);
         int index = triangleIndex++;
         triangles[index] = (short)(startVertex + 2);
         index = triangleIndex++;
         triangles[index] = (short)(startVertex + 2);
         triangles[triangleIndex++] = (short)(startVertex + 3);
         triangles[triangleIndex++] = (short)startVertex;
         this.triangleIndex = triangleIndex;
         x += originX;
         y += originY;
         float fx = -originX;
         float fy = -originY;
         originX = width - originX;
         originY = height - originY;
         if (scaleX != 1.0F || scaleY != 1.0F) {
            fx *= scaleX;
            fy *= scaleY;
            originX *= scaleX;
            originY *= scaleY;
         }

         float y4;
         if (rotation != 0.0F) {
            float cos = SpineUtils.cosDeg(rotation);
            float sin = SpineUtils.sinDeg(rotation);
            rotation = cos * fx - sin * fy;
            width = sin * fx + cos * fy;
            height = cos * fx - sin * originY;
            scaleX = sin * fx + cos * originY;
            scaleY = cos * originX - sin * originY;
            originY = sin * originX + cos * originY;
            originX = rotation + (scaleY - height);
            y4 = originY - (scaleX - width);
         } else {
            rotation = fx;
            width = fy;
            height = fx;
            scaleX = originY;
            scaleY = originX;
            originY = originY;
            originX = originX;
            y4 = fy;
         }

         rotation += x;
         width += y;
         height += x;
         scaleX += y;
         scaleY += x;
         originY += y;
         originX += x;
         float y4World = y4 + y;
         float v3;
         float u4;
         float v4;
         float u3;
         float u1;
         float v1;
         if (clockwise) {
            u1 = region.getU2();
            v1 = region.getV2();
            x = region.getU();
            y = region.getV2();
            u3 = region.getU();
            v3 = region.getV();
            u4 = region.getU2();
            v4 = region.getV();
         } else {
            u1 = region.getU();
            v1 = region.getV();
            x = region.getU2();
            y = region.getV();
            u3 = region.getU2();
            v3 = region.getV2();
            u4 = region.getU();
            v4 = region.getV2();
         }

         float light = this.lightPacked;
         float dark = this.darkPacked;
         int vertexIndex = this.vertexIndex;
         vertices[vertexIndex++] = rotation;
         vertices[vertexIndex++] = width;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u1;
         vertices[vertexIndex++] = v1;
         vertices[vertexIndex++] = height;
         vertices[vertexIndex++] = scaleX;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = x;
         vertices[vertexIndex++] = y;
         vertices[vertexIndex++] = scaleY;
         vertices[vertexIndex++] = originY;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u3;
         vertices[vertexIndex++] = v3;
         vertices[vertexIndex++] = originX;
         vertices[vertexIndex++] = y4World;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u4;
         vertices[vertexIndex++] = v4;
         this.vertexIndex = vertexIndex;
      }
   }

   @Override
   public final void draw(TextureRegion region, float width, float height, Affine2 transform) {
      if (!this.drawing) {
         throw new IllegalStateException("begin must be called before draw.");
      } else {
         short[] triangles = this.triangles;
         float[] vertices = this.vertices;
         Texture texture;
         if ((texture = region.getTexture()) != this.lastTexture) {
            this.switchTexture(texture);
         } else {
            label16: {
               if (this.triangleIndex + 6 <= triangles.length) {
                  if (this.vertexIndex + 24 <= vertices.length) {
                     break label16;
                  }
               }

               this.flush();
            }
         }

         int triangleIndex = this.triangleIndex;
         int startVertex = this.vertexIndex / 6;
         triangles[triangleIndex++] = (short)startVertex;
         triangles[triangleIndex++] = (short)(startVertex + 1);
         int index = triangleIndex++;
         triangles[index] = (short)(startVertex + 2);
         index = triangleIndex++;
         triangles[index] = (short)(startVertex + 2);
         index = triangleIndex++;
         triangles[index] = (short)(startVertex + 3);
         triangles[triangleIndex++] = (short)startVertex;
         this.triangleIndex = triangleIndex;
         float x1 = transform.m02;
         float y1 = transform.m12;
         float x2 = transform.m01 * height + transform.m02;
         float y2 = transform.m11 * height + transform.m12;
         float x3 = transform.m00 * width + transform.m01 * height + transform.m02;
         height = transform.m10 * width + transform.m11 * height + transform.m12;
         float x4 = transform.m00 * width + transform.m02;
         width = transform.m10 * width + transform.m12;
         float u = region.getU();
         float v = region.getV2();
         float u2 = region.getU2();
         float v2 = region.getV();
         float light = this.lightPacked;
         float dark = this.darkPacked;
         int vertexIndex = this.vertexIndex;
         vertices[vertexIndex++] = x1;
         vertices[vertexIndex++] = y1;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u;
         vertices[vertexIndex++] = v;
         vertices[vertexIndex++] = x2;
         vertices[vertexIndex++] = y2;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u;
         vertices[vertexIndex++] = v2;
         vertices[vertexIndex++] = x3;
         vertices[vertexIndex++] = height;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u2;
         vertices[vertexIndex++] = v2;
         vertices[vertexIndex++] = x4;
         vertices[vertexIndex++] = width;
         vertices[vertexIndex++] = light;
         vertices[vertexIndex++] = dark;
         vertices[vertexIndex++] = u2;
         vertices[vertexIndex++] = v;
         this.vertexIndex = vertexIndex;
      }
   }

   @Override
   public final void flush() {
      if (this.vertexIndex != 0) {
         this.totalRenderCalls += 1;
         Texture texture = this.lastTexture;
         this.lastTexture.bind();
         Mesh mesh = this.mesh;
         this.mesh.setVertices(this.vertices, 0, this.vertexIndex);
         mesh.setIndices(this.triangles, 0, this.triangleIndex);
         if (this.blendingDisabled) {
            Gdx.gl.glDisable(3042);
         } else {
            Gdx.gl.glEnable(3042);
            if (this.blendSrcFunc != -1) {
               Gdx.gl.glBlendFuncSeparate(this.blendSrcFunc, this.blendDstFunc, this.blendSrcFuncAlpha, this.blendDstFuncAlpha);
            }
         }

         mesh.render(this.shader, 4, 0, this.triangleIndex);
         this.vertexIndex = 0;
         this.triangleIndex = 0;
      }
   }

   @Override
   public final void disableBlending() {
      this.flush();
      this.blendingDisabled = true;
   }

   @Override
   public final void enableBlending() {
      this.flush();
      this.blendingDisabled = false;
   }

   @Override
   public final void dispose() {
      this.mesh.dispose();
      if (this.ownsShader) {
         this.defaultShader.dispose();
      }
   }

   @Override
   public final Matrix4 getProjectionMatrix() {
      return this.projectionMatrix;
   }

   @Override
   public final Matrix4 getTransformMatrix() {
      return this.transformMatrix;
   }

   @Override
   public final void setProjectionMatrix(Matrix4 projection) {
      if (this.drawing) {
         this.flush();
      }

      this.projectionMatrix.set(projection);
      if (this.drawing) {
         this.setupMatrices();
      }
   }

   @Override
   public final void setTransformMatrix(Matrix4 transform) {
      if (this.drawing) {
         this.flush();
      }

      this.transformMatrix.set(transform);
      if (this.drawing) {
         this.setupMatrices();
      }
   }

   public final void setPremultipliedAlpha(boolean premultipliedAlpha) {
      if (this.premultipliedAlpha != premultipliedAlpha) {
         if (this.drawing) {
            this.flush();
         }

         this.premultipliedAlpha = premultipliedAlpha;
         if (this.drawing) {
            this.setupMatrices();
         }
      }
   }

   public void setupMatrices() {
      this.combinedMatrix.set(this.projectionMatrix).mul(this.transformMatrix);
      this.shader.setUniformf("u_pma", this.premultipliedAlpha ? 1.0F : 0.0F);
      this.shader.setUniformMatrix("u_projTrans", this.combinedMatrix);
      String uniformName = "u_texture";
      this.shader.setUniformi(uniformName, 0);
   }

   public void switchTexture(Texture texture) {
      this.flush();
      this.lastTexture = texture;
      this.invTexWidth = 1.0F / (float)texture.getWidth();
      this.invTexHeight = 1.0F / (float)texture.getHeight();
   }

   @Override
   public final void setShader(@Null ShaderProgram newShader) {
      if (newShader == null) {
         newShader = this.defaultShader;
      }

      if (this.shader != newShader) {
         if (this.drawing) {
            this.flush();
         }

         this.shader = newShader;
         if (this.drawing) {
            this.shader.bind();
            this.setupMatrices();
         }
      }
   }

   @Override
   public final ShaderProgram getShader() {
      return this.shader;
   }

   @Override
   public final boolean isBlendingEnabled() {
      if (!this.blendingDisabled) {
         return true;
      } else {
         return false;
      }
   }

   @Override
   public final boolean isDrawing() {
      return this.drawing;
   }

   @Override
   public final void setBlendFunction(int srcFunc, int dstFunc) {
      this.setBlendFunctionSeparate(srcFunc, dstFunc, srcFunc, dstFunc);
   }

   @Override
   public final void setBlendFunctionSeparate(int srcFuncColor, int dstFuncColor, int srcFuncAlpha, int dstFuncAlpha) {
      if (this.blendSrcFunc != srcFuncColor || this.blendDstFunc != dstFuncColor || this.blendSrcFuncAlpha != srcFuncAlpha || this.blendDstFuncAlpha != dstFuncAlpha) {
         this.flush();
         this.blendSrcFunc = srcFuncColor;
         this.blendDstFunc = dstFuncColor;
         this.blendSrcFuncAlpha = srcFuncAlpha;
         this.blendDstFuncAlpha = dstFuncAlpha;
      }
   }

   @Override
   public final int getBlendSrcFunc() {
      return this.blendSrcFunc;
   }

   @Override
   public final int getBlendDstFunc() {
      return this.blendDstFunc;
   }

   @Override
   public final int getBlendSrcFuncAlpha() {
      return this.blendSrcFuncAlpha;
   }

   @Override
   public final int getBlendDstFuncAlpha() {
      return this.blendDstFuncAlpha;
   }

}
