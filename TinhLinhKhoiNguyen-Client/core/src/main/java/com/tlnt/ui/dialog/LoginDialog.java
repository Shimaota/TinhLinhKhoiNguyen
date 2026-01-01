package com.tlnt.ui.dialog;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.core.Settings;
import com.tlnt.core.GameMain;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.model.account.SavedAccount;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.utils.SpineSkeletonActor;
import com.tlnt.ui.dialog.widget.TextInputRow;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.widget.ShapeButton;
import com.tlnt.ui.window.WindowType;

public final class LoginDialog extends SlideInDialog {
   public Pool<LoginDialog.SavedAccountRow> savedAccountRowPool = new SavedAccountRowPool(this);
   public LoginTitlePanel titlePanel = new LoginTitlePanel(this, this.renderContext);
   public Group contentGroup;
   public LoginDialog.SavedAccountToggleButton loginTabButton;
   public LoginDialog.SavedAccountToggleButton registerTabButton;
   public Table loginFormTable;
   public Table savedAccountsTable;
   public TextInputRow usernameInput;
   public TextInputRow passwordInput;
   public Table savedAccountsList;

   public LoginDialog(RenderContext renderContext) {
      super(renderContext, WindowType.LOGIN_PANEL);
      this.titlePanel.setSize(1009.0F, 643.0F);
      float posX = (float)WorldConfig.VIEWPORT_WIDTH / 2.1F;
      float posY = (float)WorldConfig.VIEWPORT_HEIGHT / 2.0F;
      this.titlePanel.setPosition(posX, posY, 1);
      this.addActor(this.titlePanel);
      SpineSkeletonActor logoActor;
      (logoActor = new SpineSkeletonActor()).setRenderer(super.renderContext.getSkeletonRenderer());
      NamedPair skeletonPair = SkeletonLoader.obtainSkeleton("ui/logo", 100.0F, new SkeletonLoader.AnimationMixConfig());
      logoActor.setAnimationState((AnimationState)skeletonPair.first);
      logoActor.setSkeleton((Skeleton)skeletonPair.second);
      AnimationState animState = logoActor.getAnimationState();
      String animationName = "animation";
      animState.setAnimation(0, animationName, true);
      posX = this.titlePanel.getWidth() / 1.85F;
      posY = this.titlePanel.getHeight() - 50.0F;
      logoActor.setPosition(posX, posY, 2);
      this.titlePanel.addActor(logoActor);
      this.contentGroup = new Group();
      this.titlePanel.add(this.contentGroup).grow();
      ButtonGroup tabGroup = new ButtonGroup();
      String tabLabel = "Đăng nhập";
      this.loginTabButton = new LoginDialog.SavedAccountToggleButton(super.renderContext, tabLabel, "icon/717");
      this.loginTabButton.setSize(184.0F, 80.0F);
      this.loginTabButton.setPosition(-46.0F, 385.0F);
      this.titlePanel.addActor(this.loginTabButton);
      this.loginTabButton.addListener(new TogglePasswordVisibilityListener(this));
      tabLabel = "Tài khoản";
      this.registerTabButton = new LoginDialog.SavedAccountToggleButton(super.renderContext, tabLabel, "icon/718");
      this.registerTabButton.setSize(184.0F, 80.0F);
      this.registerTabButton.setPosition(-46.0F, 290.0F);
      this.titlePanel.addActor(this.registerTabButton);
      this.registerTabButton.addListener(new ShowSavedAccountsClickListener(this));
      Button[] tabButtons = new Button[2];
      tabButtons[0] = this.loginTabButton;
      tabButtons[1] = this.registerTabButton;
      tabGroup.add(tabButtons);
      this.loginFormTable = new Table();
      this.loginFormTable.setFillParent(true);
      this.contentGroup.addActor(this.loginFormTable);
      this.usernameInput = new TextInputRow("Tài khoản", super.renderContext);
      this.usernameInput.setSize(714.0F, 69.0F);
      this.usernameInput.setPosition(186.0F, 395.0F);
      this.loginFormTable.addActor(this.usernameInput);
      this.usernameInput.getTextField().addListener(new LoginUsernameKeyListener(this));
      if (!Settings.savedAccounts.isEmpty()) {
         this.usernameInput.getTextField().setText(Settings.savedAccounts.first().username);
      }

      this.passwordInput = new TextInputRow("Mật khẩu", super.renderContext);
      this.passwordInput.setSize(714.0F, 69.0F);
      this.passwordInput.setPosition(186.0F, 316.0F);
      this.loginFormTable.addActor(this.passwordInput);
      this.passwordInput.setPasswordMode(true);
      this.passwordInput.getTextField().addListener(new LoginPasswordKeyListener(this));
      if (!Settings.savedAccounts.isEmpty()) {
         this.passwordInput.getTextField().setText(Settings.savedAccounts.first().password);
      }

      SavedAccountButton serverButton;
      (serverButton = new SavedAccountButton(this, super.renderContext)).setSize(714.0F, 69.0F);
      serverButton.setPosition(186.0F, 237.0F);
      this.loginFormTable.addActor(serverButton);
      serverButton.addListener(new OpenServerListClickListener(this));
      LoginDialogButton registerButton;
      (registerButton = new LoginDialogButton(this, "Đăng ký", super.renderContext)).setSize(169.0F, 63.0F);
      registerButton.setPosition(370.0F, 142.0F);
      this.loginFormTable.addActor(registerButton);
      registerButton.addListener(new OpenRegisterClickListener(this));
      LoginDialogTextButton loginButton;
      (loginButton = new LoginDialogTextButton(this, "Đăng nhập", super.renderContext)).setSize(169.0F, 63.0F);
      loginButton.setPosition(547.0F, 142.0F);
      this.loginFormTable.addActor(loginButton);
      loginButton.addListener(new LoginButtonClickListener(this));
      SaveInfoTextButton saveInfoButton;
      (saveInfoButton = new SaveInfoTextButton(this, "Lưu thông tin", super.renderContext)).setSize(138.0F, 25.0F);
      saveInfoButton.setPosition(750.0F, 207.0F);
      this.loginFormTable.addActor(saveInfoButton);
      saveInfoButton.addListener(new SaveAccountClickListener(this));
      this.savedAccountsTable = new Table();
      this.savedAccountsTable.setFillParent(true);
      this.contentGroup.addActor(this.savedAccountsTable);
      this.savedAccountsTable.setVisible(false);
      ScrollPane scrollPane;
      (scrollPane = ScrollPaneFactory.createScrollPane(this.savedAccountsList = new Table().top())).setSize(714.0F, 303.0F);
      scrollPane.setPosition(186.0F, 161.0F);
      this.savedAccountsTable.addActor(scrollPane);
      LoginDialogTextButton2 resetDataButton;
      (resetDataButton = new LoginDialogTextButton2(this, "Xóa dữ liệu", super.renderContext)).setSize(150.0F, 50.0F);
      // Phone & Web: rounded corners + home indicator / browser navigation bar -> move the buttons higher and make them bigger for easier tapping
      boolean isMobileOrWeb = Gdx.app.getType() == Application.ApplicationType.iOS
            || Gdx.app.getType() == Application.ApplicationType.Android
            || Gdx.app.getType() == Application.ApplicationType.WebGL;
      posX = (float)WorldConfig.VIEWPORT_WIDTH - (isMobileOrWeb ? 40.0F : 20.0F);
      float posYReset = isMobileOrWeb ? 55.0F : 20.0F;
      resetDataButton.setPosition(posX, posYReset, 20);
      this.addActor(resetDataButton);
      resetDataButton.addListener(new LoginResetDataListener(this));
   }

   @Override
   public final void onClose2() {
      Array.ArrayIterator it = this.savedAccountsList.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof LoginDialog.SavedAccountRow) {
            this.savedAccountRowPool.free((LoginDialog.SavedAccountRow)child);
         }
      }

      this.savedAccountsList.clear();

      for (SavedAccount account : Settings.savedAccounts) {
         this.savedAccountsList.add(this.savedAccountRowPool.obtain().setAccount(account)).height(69.0F).growX().space(10.0F).row();
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);
   }

   public void submitLogin() {
      if (ServerPacketSender.getInstance().networkClient == null || !ServerPacketSender.getInstance().networkClient.isConnected()) {
         if (ServerPacketSender.getInstance().networkClient != null && ServerPacketSender.getInstance().networkClient.isConnecting()) {
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
               @Override
               public void run() {
                  if (ServerPacketSender.getInstance().networkClient != null && ServerPacketSender.getInstance().networkClient.isConnected()) {
                     submitLogin();
                  } else if (GameMain.instance != null && GameMain.instance.getGameScreen() != null
                        && GameMain.instance.getGameScreen().active != null) {
                     GameMain.instance.getGameScreen().active.showDialog(WindowType.DISCONNECTED_PANEL);
                  }
               }
            }, 0.4F);
            return;
         }

         if (GameMain.instance != null && GameMain.instance.getGameScreen() != null
               && GameMain.instance.getGameScreen().active != null) {
            GameMain.instance.getGameScreen().active.showDialog(WindowType.DISCONNECTED_PANEL);
         }
         return;
      }

      String username = this.usernameInput.getText();
      String password = this.passwordInput.getText();
      ServerPacketSender.getInstance().sendLogin(username, password);
   }

   public void saveAccountAndRefresh() {
      Settings.saveAccount(this.usernameInput.getText(), this.passwordInput.getText());
      this.onClose2();
   }

   public void selectSavedAccount(SavedAccount savedAccount) {
      if (savedAccount != null) {
         this.usernameInput.getTextField().setText(savedAccount.username);
         this.passwordInput.getTextField().setText(savedAccount.password);
         this.loginTabButton.setChecked(true);
         this.loginFormTable.setVisible(true);
         this.savedAccountsTable.setVisible(false);
         this.submitLogin();
      }
   }

   public class AccountNameButton extends ShapeButton {

      public final LoginDialog loginDialog = LoginDialog.this;
      public CompositeSprite buttonSprite;
      public SavedAccount account;

      public AccountNameButton(RenderContext renderContext) {
         super(renderContext);
         String upPath = "ui/66";
         this.buttonSprite = CompositeSprite.obtain(
            upPath, "ui/67", "ui/68", false
         );
         this.addListener(new LoadSavedAccountClickListener(this, LoginDialog.this));
      }

      public final LoginDialog.AccountNameButton setAccount(SavedAccount account) {
         this.account = account;
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawCompositeSprite(this.buttonSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               this.account.username,
               this.getX() + 18.0F,
               this.getY() + this.getHeight() / 2.0F + 3.0F,
               Anchor.MIDDLE_LEFT,
               22.0F,
               Color.WHITE
            );
         super.renderContext.resetBatchColor();
      }
   }

   public class DeleteSavedAccountButton extends ShapeButton {

      public final LoginDialog loginDialog = LoginDialog.this;
      public TextureAsset buttonTexture;
      public SavedAccount accountInfo;

      public DeleteSavedAccountButton(RenderContext renderContext) {
         super(renderContext);
         this.buttonTexture = TextureAsset.obtain("ui/115");
         this.addListener(new RemoveSavedAccountListener(this, LoginDialog.this));
      }

      public final LoginDialog.DeleteSavedAccountButton setAccountData(SavedAccount accountInfo) {
         this.accountInfo = accountInfo;
         return this;
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawTextureSized(this.buttonTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext
            .drawText(
               FontManager.DEFAULT_FONT,
               "Xóa",
               this.getX() + this.getWidth() / 2.1F,
               this.getY() + this.getHeight() / 1.8F,
               Anchor.CENTER,
               22.0F,
               Color.WHITE
            );
         super.renderContext.resetBatchColor();
      }
   }

   public class SavedAccountRow extends BaseTable {
      public LoginDialog.AccountNameButton accountNameButton = LoginDialog.this.new AccountNameButton(this.renderContext);
      public LoginDialog.DeleteSavedAccountButton deleteButton;

      public SavedAccountRow(RenderContext renderContext) {
         super(renderContext);
         this.add(this.accountNameButton).grow().space(10.0F);
         this.deleteButton = LoginDialog.this.new DeleteSavedAccountButton(super.renderContext);
         this.add(this.deleteButton).size(129.0F, 63.0F).space(10.0F);
      }

      public final LoginDialog.SavedAccountRow setAccount(SavedAccount savedAccount) {
         this.accountNameButton.setAccount(savedAccount);
         this.deleteButton.setAccountData(savedAccount);
         return this;
      }
   }

   public class SavedAccountToggleButton extends ShapeButton {
      public TextureAsset uncheckedTexture;
      public TextureAsset checkedTexture;
      public String accountNameText;
      public TextureAsset accountIconTexture;

      public SavedAccountToggleButton(RenderContext renderContext, String accountNameText, String iconPath) {
         super(renderContext);
         this.uncheckedTexture = TextureAsset.obtain("ui/74");
         this.checkedTexture = TextureAsset.obtain("ui/75");
         this.accountNameText = accountNameText;
         this.accountIconTexture = TextureAsset.obtain(iconPath);
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         if (this.isPressed()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext
            .drawTexture(this.isChecked() ? this.checkedTexture : this.uncheckedTexture, this.getX() + this.getWidth(), this.getY(), Anchor.BOTTOM_RIGHT);
         if (this.isChecked()) {
            super.renderContext
               .drawText(
                  FontManager.PLAIN_FONT,
                  this.accountNameText,
                  this.getX() + this.getWidth() / 2.0F,
                  this.getY() + this.getHeight() / 2.0F,
                  Anchor.CENTER,
                  30.0F,
                  SavedAccountToggleButtonStatics.CHECKED_TEXT_COLOR
               );
         } else {
            super.renderContext.drawTexture(this.accountIconTexture, this.getX() + 140.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER);
         }

         super.renderContext.resetBatchColor();
      }
   }

   public static final class SavedAccountToggleButtonStatics {
      public static final Color CHECKED_TEXT_COLOR = new Color(0.54509807F, 0.30588236F, 0.13725491F, 1.0F);

   }
}
