package com.tlnt.ui.widget;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.tlnt.engine.render.RenderContext;

public class BaseActor extends Actor {
   public RenderContext renderContext;

   public BaseActor(RenderContext renderContext) {
      this.renderContext = renderContext;
   }
}
