package com.tlnt.ui.widget;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.tlnt.engine.render.RenderContext;

public class BaseTable extends Table {
   public RenderContext renderContext;

   public BaseTable(RenderContext renderContext) {
      this.renderContext = renderContext;
      this.top().left();
   }
}
