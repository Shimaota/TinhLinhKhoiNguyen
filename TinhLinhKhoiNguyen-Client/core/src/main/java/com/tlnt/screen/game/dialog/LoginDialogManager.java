package com.tlnt.screen.game.dialog;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.utils.Array;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.screen.game.GameScreen;
import com.tlnt.ui.dialog.CharacterCreateDialog;
import com.tlnt.ui.dialog.ConfirmDialog;
import com.tlnt.ui.dialog.LoadingDialog;
import com.tlnt.ui.dialog.LoginDialog;
import com.tlnt.ui.dialog.NoticeDialog;
import com.tlnt.ui.dialog.RegisterDialog;
import com.tlnt.ui.dialog.ServerListDialog;
import com.tlnt.ui.window.Dialog;
import com.tlnt.ui.window.UpdateListener;
import com.tlnt.ui.window.WindowType;

public final class LoginDialogManager extends GameObject {
   public Stack skeleton;
   public LoginDialog POOL;
   public ServerListDialog owner;
   public RegisterDialog target;
   public NoticeDialog movementComponent;
   public LoadingDialog POOL2;
   public ConfirmDialog target2;
   public CharacterCreateDialog characterCreateDialog;

   public LoginDialogManager(GameScreen screen) {
      super(screen, EntityType.CANVAS);
      RenderContext ctx = this.getScreen().getRenderContext();
      this.getScreen().getStage().addActor(this.skeleton = new Stack());
      this.skeleton.setFillParent(true);
      this.skeleton.add(this.POOL = new LoginDialog(ctx));
      this.skeleton.add(this.owner = new ServerListDialog(ctx));
      this.skeleton.add(this.target = new RegisterDialog(ctx));
      this.skeleton.add(this.movementComponent = new NoticeDialog(ctx));
      this.skeleton.add(this.POOL2 = new LoadingDialog(ctx));
      this.skeleton.add(this.target2 = new ConfirmDialog(ctx));
      this.skeleton.add(this.characterCreateDialog = new CharacterCreateDialog(ctx));
      this.showDialog(WindowType.LOGIN_PANEL);
   }

   public final WindowType getActiveWindowType() {
      Array.ArrayIterator it = this.skeleton.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof Dialog && child.isVisible()) {
            return ((Dialog)child).windowType;
         }
      }

      return WindowType.LOGIN_PANEL;
   }

   public final void showDialog(WindowType type) {
      Array.ArrayIterator it = this.skeleton.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof Dialog) {
            if (((Dialog)child).windowType == type) {
               if (!child.isVisible()) {
                  ((Dialog)child).show();
               }
            } else {
               ((Dialog)child).hide();
            }
         }
      }
   }

   @Override
   public final void update(float delta) {
      Array.ArrayIterator it = this.skeleton.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof UpdateListener) {
            ((UpdateListener)child).onUpdate(delta);
         }
      }
   }

   @Override
   public final void render(RenderContext ctx) {
      ctx.drawTextureSized(
         TextureAsset.obtain("ui/BG1"),
         0.0F,
         0.0F,
         (float)WorldConfig.VIEWPORT_WIDTH,
         (float)WorldConfig.VIEWPORT_HEIGHT,
         Anchor.CENTER
      );
   }

   @Override
   public final void dispose() {
   }

}
