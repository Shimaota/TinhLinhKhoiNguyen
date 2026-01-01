package com.tlnt.engine.font;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.ObjectMap;
import com.github.tommyettinger.textra.Font;

public final class FontFactory {
   public static final ObjectMap<String, Font> fontCache = new ObjectMap<>();

   public static BitmapFont loadBitmapFont(String fontName) {
      FileHandle fontFile = Gdx.files.internal("fonts/" + fontName + ".fnt");
      BitmapFont font;
      (font = new BitmapFont(fontFile, false))
         .getRegion()
         .getTexture()
         .setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
      return font;
   }

   public static BitmapFont generateFont(String fontName, String characters) {
      int shadowOffsetX = 0;
      int shadowOffsetY = 0;
      return generateTtfFont(
         fontName, 0.0F, Color.BLACK, Color.BLACK, shadowOffsetX, shadowOffsetY, 0, 0, characters
      );
   }

   public static BitmapFont generateFontWithShadow(String fontName, float borderWidth, Color borderColor, Color shadowColor, int shadowOffsetX, int shadowOffsetY, String characters) {
      return generateTtfFont(fontName, 0.0F, borderColor, shadowColor, shadowOffsetX, shadowOffsetY, 0, 0, characters);
   }

   public static BitmapFont generateTtfFont(String fontName, float borderWidth, Color borderColor, Color shadowColor, int shadowOffsetX, int shadowOffsetY, int spaceX, int spaceY, String characters) {
      FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/" + fontName + ".ttf"));
      FreeTypeFontGenerator.FreeTypeFontParameter parameter;
      (parameter = new FreeTypeFontGenerator.FreeTypeFontParameter()).characters = characters;
      parameter.size = 50;
      parameter.color = Color.WHITE;
      parameter.minFilter = Texture.TextureFilter.MipMapLinearLinear;
      parameter.magFilter = Texture.TextureFilter.Linear;
      parameter.genMipMaps = true;
      parameter.borderWidth = borderWidth;
      parameter.borderColor = borderColor;
      parameter.shadowColor = shadowColor;
      parameter.shadowOffsetX = shadowOffsetX;
      parameter.shadowOffsetY = shadowOffsetY;
      parameter.spaceX = 0;
      parameter.spaceY = 0;
      parameter.kerning = true;
      return generator.generateFont(parameter);
   }

   public static BitmapFont scaleFont(BitmapFont font, float scale) {
      return scaleAndTintFont(font, scale, Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, Color.WHITE.a);
   }

   public static BitmapFont scaleAndTintFont(BitmapFont font, float scale, float r, float g, float b, float a) {
      if (font == null) {
         return null;
      } else {
         scale *= 2.0F;
         font.getData().setScale(scale / 100.0F);
         font.setColor(r, g, b, a);
         return font;
      }
   }

   public static BitmapFont scaleFontWithColor(BitmapFont font, float scale, Color color) {
      return scaleAndTintFont(font, scale, color.r, color.g, color.b, color.a);
   }

   public static Font getCachedTextraFont(BitmapFont bitmapFont, int size) {
      Color color = Color.WHITE;
      String key = bitmapFont.hashCode() + "_" + size;
      Font font;
      if ((font = fontCache.get(key)) == null) {
         scaleFontWithColor(bitmapFont, (float)size, color);
         font = new Font(bitmapFont);
         fontCache.put(key, font);
      }

      return font;
   }

   public static Font createTextraFontWithLineHeight(BitmapFont bitmapFont, int size, float lineHeight) {
      scaleFontWithColor(bitmapFont, (float)size, Color.WHITE);
      return new Font(bitmapFont).adjustLineHeight(lineHeight);
   }
}
