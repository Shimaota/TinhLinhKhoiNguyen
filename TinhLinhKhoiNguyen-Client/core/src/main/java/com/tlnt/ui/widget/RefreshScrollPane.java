package com.tlnt.ui.widget;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;

public final class RefreshScrollPane extends StyledScrollPane {
   public com.tlnt.ui.widget.ScrollPaneFactory.RefreshListener refreshListener;
   public boolean STYLE;

   public RefreshScrollPane(Actor actor, ScrollPaneFactory.RefreshListener refreshListener) {
      super(actor);
      this.refreshListener = refreshListener;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.getScrollY() == -50.0F && !this.STYLE) {
         this.STYLE = true;
      } else if (this.getScrollY() >= 0.0F && this.STYLE) {
         if (this.refreshListener != null) {
            this.refreshListener.onRefresh();
         }

         this.STYLE = false;
      }

      super.draw(batch, parentAlpha);
   }
}
