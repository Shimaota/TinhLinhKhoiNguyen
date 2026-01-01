package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameMain;
import com.tlnt.core.Settings;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.SettingsToggleButton;
import com.tlnt.ui.window.charinfo.Slider;
import com.tlnt.ui.window.settings.SettingsWindow;

public class SettingsMainPanel extends GamePanel {
   public SettingsWindow settingsWindow;
   public TextraLabel bgMusicLabel;
   public Slider bgMusicSlider;
   public TextraLabel soundEffectsLabel;
   public Slider soundEffectsSlider;
   public SettingsToggleButton hideOtherPlayersCheckbox;
   public SettingsToggleButton hideEffectsCheckbox;
   public SettingsToggleButton smallScreenCheckbox;
   public SettingsToggleButton fullscreenCheckbox;
   public TextraLabel secondaryPasswordNoteLabel;
   public SettingsButton settingsButton;

   public SettingsMainPanel(SettingsWindow window, RenderContext ctx) {
      super(ctx);
      this.settingsWindow = window;
      this.setFillParent(true);
      this.bgMusicLabel = new TextraLabel("[#8B4E23]Âm thanh nhạc nền", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 24));
      this.bgMusicLabel.setAlignment(8);
      this.bgMusicLabel.setSize(217.0F, 27.0F);
      this.bgMusicLabel.setPosition(268.0F, 530.0F);
      this.addActor(this.bgMusicLabel);
      this.bgMusicSlider = new Slider(super.renderContext);
      this.bgMusicSlider.setValue((float)AudioManager.getMusicVolumePercent());
      this.bgMusicSlider.setSize(528.0F, 24.0F);
      this.bgMusicSlider.setPosition(267.0F, 481.0F);
      this.addActor(this.bgMusicSlider);
      this.bgMusicSlider.addValueChangeListener(new MusicVolumeListener(this));
      String labelText = "[#8B4E23]Âm thanh hiệu ứng";
      this.soundEffectsLabel = new TextraLabel(labelText, FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 24));
      this.soundEffectsLabel.setAlignment(8);
      this.soundEffectsLabel.setSize(217.0F, 27.0F);
      this.soundEffectsLabel.setPosition(268.0F, 455.0F);
      this.addActor(this.soundEffectsLabel);
      this.soundEffectsSlider = new Slider(super.renderContext);
      this.soundEffectsSlider.setValue((float)AudioManager.getSoundVolumePercent());
      this.soundEffectsSlider.setSize(528.0F, 24.0F);
      this.soundEffectsSlider.setPosition(267.0F, 406.0F);
      this.addActor(this.soundEffectsSlider);
      this.soundEffectsSlider.addValueChangeListener(new SoundVolumeListener(this));
      this.hideOtherPlayersCheckbox = new SettingsToggleButton("Ẩn người chơi", super.renderContext);
      this.hideOtherPlayersCheckbox.setSize(215.0F, 55.0F);
      this.hideOtherPlayersCheckbox.setPosition(269.0F, 333.0F);
      this.addActor(this.hideOtherPlayersCheckbox);
      this.hideOtherPlayersCheckbox.addListener(new HideOtherPlayersCheckboxListener(this));
      this.hideEffectsCheckbox = new SettingsToggleButton("Ẩn hiệu ứng", super.renderContext);
      this.hideEffectsCheckbox.setSize(224.0F, 55.0F);
      this.hideEffectsCheckbox.setPosition(512.0F, 333.0F);
      this.addActor(this.hideEffectsCheckbox);
      this.hideEffectsCheckbox.addListener(new HideEffectsToggleListener(this));
      this.smallScreenCheckbox = new SettingsToggleButton("Màn hình nhỏ", super.renderContext);
      this.smallScreenCheckbox.setSize(217.0F, 55.0F);
      this.smallScreenCheckbox.setPosition(269.0F, 266.0F);
      this.addActor(this.smallScreenCheckbox);
      this.smallScreenCheckbox.addListener(new SmallScreenCheckboxListener(this));
      this.fullscreenCheckbox = new SettingsToggleButton("Full màn hình", super.renderContext);
      this.fullscreenCheckbox.setSize(217.0F, 55.0F);
      this.fullscreenCheckbox.setPosition(512.0F, 266.0F);
      this.addActor(this.fullscreenCheckbox);
      this.fullscreenCheckbox.addListener(new FullscreenToggleListener(this));
      this.secondaryPasswordNoteLabel = new TextraLabel(
         "[#8B4E23]Lưu ý: MK cấp 2 có thể dùng để lấy lại mật khẩu khi cần", FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 24)
      );
      this.secondaryPasswordNoteLabel.setAlignment(8);
      this.secondaryPasswordNoteLabel.setSize(568.0F, 25.0F);
      this.secondaryPasswordNoteLabel.setPosition(256.0F, 216.0F);
      this.addActor(this.secondaryPasswordNoteLabel);
      this.settingsButton = new SettingsButton(this, "Thiết lập", super.renderContext);
      this.settingsButton.setSize(169.0F, 63.0F);
      this.settingsButton.setPosition(279.0F, 124.0F);
      this.addActor(this.settingsButton);
      this.settingsButton.addListener(new SettingsButtonListener(this));
      SecPasswordButton secPasswordButton;
      (secPasswordButton = new SecPasswordButton(this, "MK cấp 2", super.renderContext)).setSize(169.0F, 63.0F);
      secPasswordButton.setPosition(456.0F, 124.0F);
      this.addActor(secPasswordButton);
      secPasswordButton.addListener(new SecPasswordButtonListener(this));
      LogoutButton logoutButton;
      (logoutButton = new LogoutButton(this, "Đăng xuất", super.renderContext)).setSize(169.0F, 63.0F);
      logoutButton.setPosition(633.0F, 124.0F);
      this.addActor(logoutButton);
      logoutButton.addListener(new LogoutButtonListener(this));
   }

   public void applyDisplaySettings() {
      if (Gdx.app.getType() == Application.ApplicationType.Desktop) {
         Settings.saveDisplaySettings(this.smallScreenCheckbox.isChecked(), this.fullscreenCheckbox.isChecked());
         GameMain.applyDisplayMode();
      }
   }

   @Override
   public final void refresh() {
      this.hideOtherPlayersCheckbox.setChecked(Settings.hideOtherPlayers);
      this.smallScreenCheckbox.setChecked(Settings.isSmallScreen());
      this.fullscreenCheckbox.setChecked(Settings.isFullscreen());
      this.hideEffectsCheckbox.setChecked(Settings.hideEffects);
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      super.renderContext
         .drawTextColorName(
            FontManager.PLAIN_FONT,
            AudioManager.getMusicVolumePercent() + "%",
            this.getX() + 824.0F,
            this.getY() + 484.0F,
            Anchor.BOTTOM_LEFT,
            24.0F,
            "#8B4E23"
         );
      RenderContext ctx = super.renderContext;
      BitmapFont font = FontManager.PLAIN_FONT;
      String volumeText = AudioManager.getSoundVolumePercent() + "%";
      float textX = this.getX() + 824.0F;
      float textY = this.getY() + 409.0F;
      ctx.drawTextColorName(font, volumeText, textX, textY, Anchor.BOTTOM_LEFT, 24.0F, "#8B4E23");
      super.draw(batch, parentAlpha);
   }

}
