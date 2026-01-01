package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.dialog.widget.TextInputRow;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.settings.SettingsWindow;

public class ChangeSecondaryPasswordPanel extends GamePanel {
   public SettingsWindow settingsWindow;
   public TextInputRow currentPasswordField;
   public TextInputRow newPasswordField;
   public TextInputRow confirmNewPasswordField;
   public ChangeSecondaryPasswordButton changeButton;

   public ChangeSecondaryPasswordPanel(SettingsWindow window, RenderContext ctx) {
      super(ctx);
      this.settingsWindow = window;
      this.setFillParent(true);
      this.currentPasswordField = new TextInputRow("Nhập MK cấp 2 hiện tại", super.renderContext);
      this.currentPasswordField.setSize(407.0F, 69.0F);
      this.currentPasswordField.setPosition(340.0F, 425.0F);
      this.addActor(this.currentPasswordField);
      this.currentPasswordField.setPasswordMode(true);
      this.newPasswordField = new TextInputRow("Nhập MK cấp 2 mới", super.renderContext);
      this.newPasswordField.setSize(407.0F, 69.0F);
      this.newPasswordField.setPosition(340.0F, 341.0F);
      this.addActor(this.newPasswordField);
      this.newPasswordField.setPasswordMode(true);
      this.confirmNewPasswordField = new TextInputRow("Nhập lại MK cấp 2 mới", super.renderContext);
      this.confirmNewPasswordField.setSize(407.0F, 69.0F);
      this.confirmNewPasswordField.setPosition(340.0F, 257.0F);
      this.addActor(this.confirmNewPasswordField);
      this.confirmNewPasswordField.setPasswordMode(true);
      this.changeButton = new ChangeSecondaryPasswordButton(this, "Đổi", super.renderContext);
      this.changeButton.setSize(169.0F, 63.0F);
      this.changeButton.setPosition(459.0F, 124.0F);
      this.addActor(this.changeButton);
      this.changeButton.addListener(new ChangePasswordSubmitListener(this));
   }

   public void submitChangePassword() {
      String currentPassword = this.currentPasswordField.getText();
      String newPassword = this.newPasswordField.getText();
      String confirmPassword = this.confirmNewPasswordField.getText();
      ServerPacketSender.getInstance().sendGuestLogin(currentPassword, newPassword, confirmPassword);
      this.currentPasswordField.getTextField().setText("");
      this.newPasswordField.getTextField().setText("");
      this.confirmNewPasswordField.getTextField().setText("");
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT,
            "Đổi MK cấp 2",
            this.getX() + 543.0F,
            this.getY() + 513.0F,
            Anchor.BOTTOM_CENTER,
            24.0F,
            "#8B4E23"
         );
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String text = "Lưu ý: MK cấp 2 có thể dùng để lấy lại mật khẩu khi cần";
      float textX = this.getX() + 543.0F;
      float textY = this.getY() + 204.0F;
      ctx.drawTextColorName(font, text, textX, textY, Anchor.BOTTOM_CENTER, 24.0F, "#8B4E23");
   }

}
