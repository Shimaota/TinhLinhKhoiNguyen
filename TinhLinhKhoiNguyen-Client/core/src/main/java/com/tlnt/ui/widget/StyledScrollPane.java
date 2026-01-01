package com.tlnt.ui.widget;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.tlnt.engine.render.TextureAsset;

public class StyledScrollPane extends ScrollPane {
   public static final ScrollPane.ScrollPaneStyle STYLE;

   public StyledScrollPane(Actor actor) {
      super(actor);
      this.addListener(new ScrollFocusListener(this));
   }

   static {
      ScrollPane.ScrollPaneStyle style = STYLE = new ScrollPane.ScrollPaneStyle();
      style.hScrollKnob = new TextureRegionDrawable(TextureAsset.obtain("skin/scrollpane").getRegion());
      STYLE.vScrollKnob = new TextureRegionDrawable(TextureAsset.obtain("skin/scrollpane").getRegion());
   }

}
