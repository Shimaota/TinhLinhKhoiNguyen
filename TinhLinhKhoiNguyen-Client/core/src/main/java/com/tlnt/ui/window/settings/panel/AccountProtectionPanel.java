package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.settings.SettingsWindow;

public class AccountProtectionPanel extends GamePanel {
   public SettingsWindow settingsWindow;
   public AccountProtectionActionButton unlockButton;
   public LockAccountButton lockButton;
   public ChangeSecPasswordButton changePasswordButton;
   public AccountProtectionActionButton deletePasswordButton;

   public AccountProtectionPanel(SettingsWindow window, RenderContext ctx) {
      super(ctx);
      this.settingsWindow = window;
      this.setFillParent(true);
      this.unlockButton = new AccountProtectionActionButton(this, "Mở khóa", super.renderContext);
      this.unlockButton.setSize(169.0F, 63.0F);
      this.unlockButton.setPosition(371.0F, 418.0F);
      this.addActor(this.unlockButton);
      this.unlockButton.addListener(new UnlockAccountListener(this));
      this.lockButton = new LockAccountButton(this, "Khóa", super.renderContext);
      this.lockButton.setSize(169.0F, 63.0F);
      this.lockButton.setPosition(371.0F, 418.0F);
      this.addActor(this.lockButton);
      this.lockButton.addListener(new LockAccountListener(this));
      this.changePasswordButton = new ChangeSecPasswordButton(this, "Đổi MK cấp 2", super.renderContext);
      this.changePasswordButton.setSize(169.0F, 63.0F);
      this.changePasswordButton.setPosition(548.0F, 418.0F);
      this.addActor(this.changePasswordButton);
      this.changePasswordButton.addListener(new ChangePasswordNavListener(this));
      this.deletePasswordButton = new AccountProtectionActionButton(this, "Xóa MK cấp 2", super.renderContext);
      this.deletePasswordButton.setSize(169.0F, 63.0F);
      this.deletePasswordButton.setPosition(458.0F, 347.0F);
      this.addActor(this.deletePasswordButton);
      this.deletePasswordButton.addListener(new DeleteSecondaryPasswordListener(this));
   }

   @Override
   public final void refresh() {
      if (!GameState.hasSecondaryPassword && this.isVisible()) { // just did "Delete secondary password" -> back to the create-password panel
         this.settingsWindow.showPanel(CreateSecondaryPasswordPanel.class);
         return;
      }
      this.unlockButton.setVisible(GameState.isSecondaryPasswordLocked);
      int unlockedFlag;
      if (!GameState.isSecondaryPasswordLocked) {
         unlockedFlag = 1;
      } else {
         unlockedFlag = 0;
      }

      this.lockButton.setVisible((unlockedFlag != 0));
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      super.draw(batch, parentAlpha);
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String text;
      if (GameState.isSecondaryPasswordLocked) {
         text = "Tài khoản đang được bảo vệ";
      } else {
         text = "Tài khoản đang tắt bảo vệ";
      }

      float textX = this.getX() + 543.0F;
      float textY = this.getY() + 507.0F;
      ctx.drawTextColorName(font, text, textX, textY, Anchor.BOTTOM_CENTER, 24.0F, "#8B4E23");
      ctx = super.renderContext;
      font = FontManager.PLAIN_FONT;
      text = "Lưu ý: MK cấp 2 có thể dùng để lấy lại mật khẩu khi cần";
      textX = this.getX() + 543.0F;
      textY = this.getY() + 300.0F; // below the "Delete secondary password" button (y 347)
      ctx.drawTextColorName(font, text, textX, textY, Anchor.BOTTOM_CENTER, 24.0F, "#8B4E23");
   }

}
