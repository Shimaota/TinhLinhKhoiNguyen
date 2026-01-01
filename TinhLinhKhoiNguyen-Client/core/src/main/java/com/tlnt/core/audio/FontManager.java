package com.tlnt.core.audio;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.engine.font.FontFactory;

public final class FontManager {
   public static final BitmapFont PLAIN_FONT;
   public static final BitmapFont DEFAULT_FONT;
   public static final BitmapFont WORLD_TEXT_FONT;
   public static final BitmapFont TEXT_FIELD_FONT;
   public static final BitmapFont TEXT_FIELD_MESSAGE_FONT;
   public static final BitmapFont EXP_GAIN_FONT;
   public static final BitmapFont DAMAGE_FONT;
   public static final BitmapFont CRIT_DAMAGE_FONT;
   public static final BitmapFont MISS_TEXT_FONT;

   static {
      WORLD_TEXT_FONT = FontFactory.generateFont( "Saira-Bold", "AÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬBCDĐEÈÉẺẼẸÊỀẾỂỄỆFGHIÌÍỈĨỊJKLMN\nOÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢPQRSTUÙÚỦŨỤŨƯỪỨỬỮỰVWXYÝỲỶỸỴZaàá\nảãạăằắẳẵặâầấẩẫậbcdđeèéẻẽẹêềếểễệfghiìíỉĩịjklmnoòó\nỏõọôồốổỗộơờớởỡợpqrstuùúủũụũưừứửữựvwxyýỳỷỹỵz012345\n6789~`!@#$%^&*()_-+=[{]}\\|;:'\",<.>/?" );
      CRIT_DAMAGE_FONT = FontFactory.loadBitmapFont("crit/crit");
      MISS_TEXT_FONT = FontFactory.loadBitmapFont("miss/miss");
      String fontName = "Saira-Bold";
      PLAIN_FONT = FontFactory.scaleFont(
         FontFactory.generateTtfFont(fontName, 0.0F, Color.WHITE, Color.BLACK, 0, 0, 0, 0, "AÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬBCDĐEÈÉẺẼẸÊỀẾỂỄỆFGHIÌÍỈĨỊJKLMN\nOÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢPQRSTUÙÚỦŨỤŨƯỪỨỬỮỰVWXYÝỲỶỸỴZaàá\nảãạăằắẳẵặâầấẩẫậbcdđeèéẻẽẹêềếểễệfghiìíỉĩịjklmnoòó\nỏõọôồốổỗộơờớởỡợpqrstuùúủũụũưừứửữựvwxyýỳỷỹỵz012345\n6789~`!@#$%^&*()_-+=[{]}\\|;:'\",<.>/?"), 24.0F
      );
      fontName = "Saira-Bold";
      DEFAULT_FONT = FontFactory.scaleFont(
         FontFactory.generateFontWithShadow(fontName, 0.0F, Color.WHITE, Color.BLACK, -2, 8, "AÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬBCDĐEÈÉẺẼẸÊỀẾỂỄỆFGHIÌÍỈĨỊJKLMN\nOÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢPQRSTUÙÚỦŨỤŨƯỪỨỬỮỰVWXYÝỲỶỸỴZaàá\nảãạăằắẳẵặâầấẩẫậbcdđeèéẻẽẹêềếểễệfghiìíỉĩịjklmnoòó\nỏõọôồốổỗộơờớởỡợpqrstuùúủũụũưừứửữựvwxyýỳỷỹỵz012345\n6789~`!@#$%^&*()_-+=[{]}\\|;:'\",<.>/?"), 24.0F
      );
      fontName = "Saira-Bold";
      TEXT_FIELD_FONT = FontFactory.scaleFont(
         FontFactory.generateFontWithShadow(
            fontName, 0.0F, Color.WHITE, Color.BLACK, -2, 8, "AÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬBCDĐEÈÉẺẼẸÊỀẾỂỄỆFGHIÌÍỈĨỊJKLMN\nOÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢPQRSTUÙÚỦŨỤŨƯỪỨỬỮỰVWXYÝỲỶỸỴZaàá\nảãạăằắẳẵặâầấẩẫậbcdđeèéẻẽẹêềếểễệfghiìíỉĩịjklmnoòó\nỏõọôồốổỗộơờớởỡợpqrstuùúủũụũưừứửữựvwxyýỳỷỹỵz012345\n6789~`!@#$%^&*()_-+=[{]}\\|;:'\",<.>/?"
         ),
         24.0F
      );
      fontName = "Saira-Bold";
      TEXT_FIELD_MESSAGE_FONT = FontFactory.scaleFont(
         FontFactory.generateFontWithShadow(fontName, 0.0F, Color.WHITE, Color.BLACK, 0, 0, "AÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬBCDĐEÈÉẺẼẸÊỀẾỂỄỆFGHIÌÍỈĨỊJKLMN\nOÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢPQRSTUÙÚỦŨỤŨƯỪỨỬỮỰVWXYÝỲỶỸỴZaàá\nảãạăằắẳẵặâầấẩẫậbcdđeèéẻẽẹêềếểễệfghiìíỉĩịjklmnoòó\nỏõọôồốổỗộơờớởỡợpqrstuùúủũụũưừứửữựvwxyýỳỷỹỵz012345\n6789~`!@#$%^&*()_-+=[{]}\\|;:'\",<.>/?"),
         24.0F
      );
      EXP_GAIN_FONT = FontFactory.loadBitmapFont("exp/exp");
      DAMAGE_FONT = FontFactory.loadBitmapFont("dame/dame");
   }

}
