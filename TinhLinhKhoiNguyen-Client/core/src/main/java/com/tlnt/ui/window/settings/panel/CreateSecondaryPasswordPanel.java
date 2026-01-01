package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.dialog.widget.TextInputRow;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.settings.SettingsWindow;

public class CreateSecondaryPasswordPanel extends GamePanel {
   public SettingsWindow settingsWindow;
   public TextInputRow currentPasswordField;
   public TextInputRow secondaryPasswordField;
   public TextInputRow confirmSecondaryPasswordField;
   public CreateSecondaryPasswordButton createButton;

   public CreateSecondaryPasswordPanel(SettingsWindow window, RenderContext ctx) {
      super(ctx);
      this.settingsWindow = window;
      this.setFillParent(true);
      this.currentPasswordField = new TextInputRow("Nhập MK hiện tại", super.renderContext);
      this.currentPasswordField.setSize(407.0F, 69.0F);
      this.currentPasswordField.setPosition(340.0F, 425.0F);
      this.addActor(this.currentPasswordField);
      this.currentPasswordField.setPasswordMode(true);
      this.secondaryPasswordField = new TextInputRow("Nhập MK cấp 2", super.renderContext);
      this.secondaryPasswordField.setSize(407.0F, 69.0F);
      this.secondaryPasswordField.setPosition(340.0F, 341.0F);
      this.addActor(this.secondaryPasswordField);
      this.secondaryPasswordField.setPasswordMode(true);
      this.confirmSecondaryPasswordField = new TextInputRow("Nhập lại MK cấp 2", super.renderContext);
      this.confirmSecondaryPasswordField.setSize(407.0F, 69.0F);
      this.confirmSecondaryPasswordField.setPosition(340.0F, 257.0F);
      this.addActor(this.confirmSecondaryPasswordField);
      this.confirmSecondaryPasswordField.setPasswordMode(true);
      this.createButton = new CreateSecondaryPasswordButton(this, "Tạo", super.renderContext);
      this.createButton.setSize(169.0F, 63.0F);
      this.createButton.setPosition(459.0F, 124.0F);
      this.addActor(this.createButton);
      this.createButton.addListener(new CreatePasswordSubmitListener(this));
   }

   public void submitCreateSecondaryPassword() {
      String currentPassword = this.currentPasswordField.getText();
      String secondaryPassword = this.secondaryPasswordField.getText();
      String confirmPassword = this.confirmSecondaryPasswordField.getText();
      ServerPacketSender.getInstance().sendRegister(currentPassword, secondaryPassword, confirmPassword);
   }

   @Override
   public final void refresh() {
      if (GameState.hasSecondaryPassword) {
         this.currentPasswordField.clearText();
         this.secondaryPasswordField.clearText();
         this.confirmSecondaryPasswordField.clearText();
         this.settingsWindow.showPanel(AccountProtectionPanel.class);
      }
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String text = "Tạo MK cấp 2";
      float textX = this.getX() + 543.0F;
      float textY = this.getY() + 513.0F;
      ctx.drawTextColorName(font, text, textX, textY, Anchor.BOTTOM_CENTER, 24.0F, "#8B4E23");
      ctx = super.renderContext;
      font = FontManager.PLAIN_FONT;
      text = "Lưu ý: MK cấp 2 có thể dùng để lấy lại mật khẩu khi cần";
      textX = this.getX() + 543.0F;
      textY = this.getY() + 204.0F;
      ctx.drawTextColorName(font, text, textX, textY, Anchor.BOTTOM_CENTER, 24.0F, "#8B4E23");
   }

}
