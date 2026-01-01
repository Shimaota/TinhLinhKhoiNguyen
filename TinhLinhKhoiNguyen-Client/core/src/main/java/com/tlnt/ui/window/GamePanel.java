package com.tlnt.ui.window;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;

public class GamePanel extends BaseTable implements UpdateListener, RenderListener {
   public GamePanel(RenderContext ctx) {
      super(ctx);
   }

   @Override
   public void setVisible(boolean visible) {
      boolean wasVisible = this.isVisible();
      if (visible) {
         super.setVisible(true);
         if (!wasVisible) {
            this.notifyShowRecursive(this);
         }

         ScrollPaneFactory.refreshVisibleActors(this);
      } else {
         super.setVisible(false);
         if (wasVisible) {
            this.notifyHideRecursive(this);
         }
      }
   }

   public void notifyShowRecursive(Actor actor) {
      if (actor.isVisible()) {
         if (actor instanceof Dialog) {
            ((Dialog)actor).onOpen();
         } else if (actor instanceof GamePanel) {
            ((GamePanel)actor).onShow();
         }

         if (actor instanceof Group) {
            for (Actor child : ((Group)actor).getChildren()) {
               this.notifyShowRecursive(child);
            }
         }
      }
   }

   public void notifyHideRecursive(Actor actor) {
      if (!actor.isVisible()) {
         if (actor instanceof Dialog) {
            ((Dialog)actor).onClose();
         } else if (actor instanceof GamePanel) {
            ;
         }

         if (actor instanceof Group) {
            for (Actor child : ((Group)actor).getChildren()) {
               this.notifyHideRecursive(child);
            }
         }
      }
   }

   public void onShow() {
   }

   @Override
   public void onUpdate(float delta) {
   }

   @Override
   public void onRender(float delta) {
   }

   public void refresh() {
   }
}
