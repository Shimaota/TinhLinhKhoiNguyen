package com.tlnt.ui.dialog;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.thirdparty.spine.utils.SpineSkeletonActor;
import com.tlnt.ui.dialog.widget.TextInputRow;
import com.tlnt.ui.window.WindowType;

public final class RegisterDialog extends SlideInDialog {
   public TextInputRow usernameInput;
   public TextInputRow passwordInput;
   public TextInputRow confirmPasswordInput;
   public TextInputRow emailInput;
   public TextInputRow captchaInput;

   public RegisterDialog(RenderContext renderContext) {
      super(renderContext, WindowType.REGIS_PANEL);
      RegisterTitlePanel titlePanel;
      (titlePanel = new RegisterTitlePanel(this, super.renderContext)).setSize(1009.0F, 809.0F);
      float posX = (float)WorldConfig.VIEWPORT_WIDTH / 2.1F;
      float posY = (float)WorldConfig.VIEWPORT_HEIGHT / 2.25F;
      titlePanel.setPosition(posX, posY, 1);
      this.addActor(titlePanel);
      SpineSkeletonActor logoActor;
      (logoActor = new SpineSkeletonActor()).setRenderer(super.renderContext.getSkeletonRenderer());
      NamedPair skeletonPair = SkeletonLoader.obtainSkeleton("ui/logo", 100.0F, new SkeletonLoader.AnimationMixConfig());
      logoActor.setAnimationState((AnimationState)skeletonPair.first);
      logoActor.setSkeleton((Skeleton)skeletonPair.second);
      AnimationState animState = logoActor.getAnimationState();
      String animationName = "animation";
      animState.setAnimation(0, animationName, true);
      logoActor.setPosition(titlePanel.getWidth() / 1.85F, titlePanel.getHeight() - 50.0F, 2);
      titlePanel.addActor(logoActor);
      this.usernameInput = new TextInputRow("Tài khoản", super.renderContext);
      this.usernameInput.setSize(714.0F, 69.0F);
      this.usernameInput.setPosition(186.0F, 560.0F);
      titlePanel.addActor(this.usernameInput);
      this.passwordInput = new TextInputRow("Mật khẩu", super.renderContext);
      this.passwordInput.setPasswordMode(true);
      this.passwordInput.setSize(714.0F, 69.0F);
      this.passwordInput.setPosition(186.0F, 476.0F);
      titlePanel.addActor(this.passwordInput);
      this.confirmPasswordInput = new TextInputRow("Nhập lại mật khẩu", super.renderContext);
      this.confirmPasswordInput.setPasswordMode(true);
      this.confirmPasswordInput.setSize(714.0F, 69.0F);
      this.confirmPasswordInput.setPosition(186.0F, 392.0F);
      titlePanel.addActor(this.confirmPasswordInput);
      this.emailInput = new TextInputRow("Email", super.renderContext);
      this.emailInput.setSize(714.0F, 69.0F);
      this.emailInput.setPosition(186.0F, 308.0F);
      titlePanel.addActor(this.emailInput);
      this.captchaInput = new TextInputRow("Captcha", super.renderContext);
      this.captchaInput.setSize(465.0F, 69.0F);
      this.captchaInput.setPosition(186.0F, 224.0F);
      titlePanel.addActor(this.captchaInput);
      CaptchaButton captchaButton;
      (captchaButton = new CaptchaButton(this, super.renderContext)).setSize(234.0F, 69.0F);
      captchaButton.setPosition(666.0F, 224.0F);
      titlePanel.addActor(captchaButton);
      captchaButton.addListener(new RefreshCaptchaClickListener(this));
      RegisterDialogTextButton loginButton;
      (loginButton = new RegisterDialogTextButton(this, "Đăng nhập", super.renderContext)).setSize(169.0F, 63.0F);
      loginButton.setPosition(370.0F, 142.0F);
      titlePanel.addActor(loginButton);
      loginButton.addListener(new SwitchToLoginClickListener(this));
      RegisterDialogButton registerButton;
      (registerButton = new RegisterDialogButton(this, "Đăng ký", super.renderContext)).setSize(169.0F, 63.0F);
      registerButton.setPosition(547.0F, 142.0F);
      titlePanel.addActor(registerButton);
      registerButton.addListener(new RegisterCaptchaRefreshListener(this));
   }

   @Override
   public final void onOpen() {
      ServerPacketSender.getInstance().sendCreateCharacter();
   }

   public void submitRegister() {
      String username = this.usernameInput.getText();
      String password = this.passwordInput.getText();
      String confirmPassword = this.confirmPasswordInput.getText();
      String email = this.emailInput.getText();
      String captcha = this.captchaInput.getText();
      ServerPacketSender.getInstance().sendSocialLogin(username, password, confirmPassword, email, captcha);
   }

}
