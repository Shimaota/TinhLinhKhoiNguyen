package com.tlnt.ui.window;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;

public class Dialog extends BaseTable implements UpdateListener, RenderListener {
   public final WindowType windowType;

   public Dialog(RenderContext ctx, WindowType windowType) {
      super(ctx);
      this.windowType = windowType;
      this.setTouchable(Touchable.enabled);
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

   // Hidden windows do not need act: ~30 prebuilt windows (~1,100 Groups) were acted every frame, the mobile web build lagged.
   // show() always calls setVisible(true) before adding actions, so no action is pending while hidden.
   @Override
   public void act(float delta) {
      if (this.isVisible()) {
         super.act(delta);
      }
   }

   public void notifyShowRecursive(Actor actor) {
      if (actor.isVisible()) {
         if (actor instanceof Group) {
            for (Actor child : ((Group)actor).getChildren()) {
               this.notifyShowRecursive(child);
            }
         }

         if (actor instanceof Dialog) {
            ((Dialog)actor).onOpen();
         } else {
            if (actor instanceof GamePanel) {
               ((GamePanel)actor).onShow();
            }
         }
      }
   }

   public void notifyHideRecursive(Actor actor) {
      if (!actor.isVisible()) {
         if (actor instanceof Group) {
            for (Actor child : ((Group)actor).getChildren()) {
               this.notifyHideRecursive(child);
            }
         }

         if (actor instanceof Dialog) {
            ((Dialog)actor).onClose();
         } else {
            if (actor instanceof GamePanel) {
               ;
            }
         }
      }
   }

   public void show() {
      this.setVisible(true);
   }

   public void hide() {
      this.setVisible(false);
   }

   public void onOpen() {
   }

   public void onClose() {
   }

   @Override
   public void onUpdate(float delta) {
   }

   @Override
   public void onRender(float delta) {
   }

   public void onClose2() {
   }
}
