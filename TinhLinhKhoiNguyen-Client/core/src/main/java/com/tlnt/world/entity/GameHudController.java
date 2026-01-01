package com.tlnt.world.entity;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameState;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.model.player.MoveSpeedTable;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.ui.hud.FadeOverlay;
import com.tlnt.ui.hud.GameHud;
import com.tlnt.ui.hud.WindowManager;
import com.tlnt.ui.hud.widget.MountButton;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.Dialog;
import com.tlnt.ui.window.RenderListener;
import com.tlnt.ui.window.UpdateListener;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.Popup;
import com.tlnt.world.entity.player.PlayerEntity;

public final class GameHudController extends GameObject {
   public boolean POOL;
   public GameHud POOL2;
   public WindowManager skeleton;

   public GameHudController(LoginScreen screen) {
      super(screen, EntityType.CANVAS);
      RenderContext renderContext = this.getScreen().getRenderContext();
      Group rootGroup;
      (rootGroup = new Group()).setSize((float)WorldConfig.VIEWPORT_WIDTH, (float)WorldConfig.VIEWPORT_HEIGHT);
      this.getScreen().getStage().addActor(rootGroup);
      MountButton mountButton = new MountButton(renderContext);
      rootGroup.addActor(mountButton);
      rootGroup.addActor(this.POOL2 = new GameHud(renderContext));
      this.POOL2.setFillParent(true);
      rootGroup.addActor(this.skeleton = new WindowManager(renderContext));
      this.skeleton.setFillParent(true);
      rootGroup.addActor(new FadeOverlay(renderContext));
   }

   public final void closeAllDialogs() {
      WindowManager windowManager = this.skeleton;
      Array.ArrayIterator iter = this.skeleton.getChildren().iterator();

      while (iter.hasNext()) {
         Actor child;
         if ((child = (Actor)iter.next()) instanceof Dialog) {
            ((Dialog)child).hide();
         }
      }
   }

   public final void showWindow(WindowType windowType) {
      this.skeleton.showDialog(windowType);
   }

   public final void closeWindowByType(WindowType windowType) {
      WindowType type = windowType;
      WindowManager windowManager = this.skeleton;
      Array.ArrayIterator iter = this.skeleton.getChildren().iterator();

      while (iter.hasNext()) {
         Actor child;
         Dialog dialog;
         if ((child = (Actor)iter.next()) instanceof Dialog && (dialog = (Dialog)child).windowType == type) {
            dialog.hide();
         }
      }
   }

   public final <T extends Dialog> T findDialog(Class<T> dialogClass) {
      return this.skeleton.findDialog(dialogClass);
   }

   public final void refreshHudState() {
      GameHud hud = this.POOL2;
      this.POOL2.hudSkillButton3.setSkill(GameState.inventory.quickSlot1);
      hud.mpPotionSlotButton.setItem(GameState.inventory.quickSlot2);
      hud.hudSkillButton1.setSkill(GameState.inventory.quickSlot3);
      PlayerEntity localPlayer = PlayerEntity.getInstance();
      if (localPlayer == null) {
         // Not in a map yet (e.g. just logged in / creating a character): there is no per-character HUD to update.
         ScrollPaneFactory.refreshVisibleActors(this.skeleton);
         return;
      }

      int movementMode = localPlayer.POOL2.movementMode;
      hud.pickupButton
         .setVisible(
            ((movementMode != MoveSpeedTable.SPEED_MOUNT && movementMode != MoveSpeedTable.SPEED_NORMAL
               ? 0
               : 1) != 0)
         );
      hud.avatarButton.setVisible(((movementMode == MoveSpeedTable.SPEED_FAST ? 1 : 0) != 0));
      int skillBarVisible;
      if (movementMode != MoveSpeedTable.SPEED_FAST) {
         skillBarVisible = 1;
      } else {
         skillBarVisible = 0;
      }

      hud.skillBar.setVisible((skillBarVisible != 0));
      ScrollPaneFactory.refreshVisibleActors(this.POOL2);
      ScrollPaneFactory.refreshVisibleActors(this.skeleton);
   }

   public void updateActorsRecursive(Actor actor, float delta) {
      if (actor instanceof UpdateListener && actor.isVisible()) {
         ((UpdateListener)actor).onUpdate(delta);
      }

      if (actor instanceof RenderListener) {
         ((RenderListener)actor).onRender(delta);
      }

      // Hidden Group: ITS OWN listeners still run above (popups reappear on death / key press...), but we do not descend into
      // the children — hundreds of hidden windows used to be traversed every frame (JFR: 23% of the main thread). Children lag only 1 frame when the parent shows again.
      if (actor instanceof Group && actor.isVisible()) {
         // Index loop over a snapshot (begin/end): the whole UI tree (hidden windows included) is traversed EVERY frame — the iterator took more
         // than half of this method's time (JFR 2026-10-19: method = 27% of the main thread). The snapshot stays safe when callbacks add / remove children.
         com.badlogic.gdx.utils.SnapshotArray<Actor> children = ((Group)actor).getChildren();
         Actor[] items = children.begin();
         try {
            for (int i = 0, n = children.size; i < n; i++) {
               this.updateActorsRecursive(items[i], delta);
            }
         } finally {
            children.end();
         }
      }
   }

   @Override
   public final void update(float delta) {
      this.POOL = true;
      if (PlayerEntity.getInstance() != null && PlayerEntity.getInstance().POOL2.isAlive) {
         this.POOL = false;
      } else {
         Array.ArrayIterator iter = this.skeleton.getChildren().iterator();

         while (iter.hasNext()) {
            Actor child;
            if ((child = (Actor)iter.next()).isVisible() && child instanceof Popup && ((Popup)child).isModal()) {
               this.POOL = false;
               break;
            }
         }
      }

      if (this.POOL2.isVisible() != this.POOL) {
         this.POOL2.setVisible(this.POOL);
      }

      this.updateActorsRecursive(this.POOL2, delta);
      this.updateActorsRecursive(this.skeleton, delta);
   }

   public final boolean isHudActive() {
      return this.POOL;
   }

   @Override
   public final void dispose() {
   }
}
