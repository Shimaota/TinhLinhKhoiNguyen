package com.tlnt.ui.widget;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.engine.render.RenderContext;

public class GameTextField extends TextField {
   public GameTextField(String text, RenderContext renderContext, BitmapFont font) {
      super(text, renderContext, font);
   }

   public GameTextField(String text, RenderContext renderContext, BitmapFont font, BitmapFont messageFont) {
      super(text, renderContext, font, messageFont);
   }
}
