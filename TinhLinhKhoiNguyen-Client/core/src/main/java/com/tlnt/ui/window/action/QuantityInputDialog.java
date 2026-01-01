package com.tlnt.ui.window.action;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameMain;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.GameTextField;
import com.tlnt.ui.widget.TextField;
import com.tlnt.ui.window.WindowType;

public class QuantityInputDialog extends Popup {
   public TextureAsset backgroundTexture;
   public TextraLabel promptLabel;
   public Color textColor;
   public InputDialogConfirmButton confirmButton;
   public QuantityInputDialog.InputField inputField;
   public QuantityInputDialog.InputCallback inputCallback;

   public QuantityInputDialog(RenderContext renderContext) {
      super(renderContext, WindowType.INPUT_POPUP, (float)WorldConfig.VIEWPORT_WIDTH / 2.0F, (float)WorldConfig.VIEWPORT_HEIGHT / 4.0F, 1, 538.0F, 340.0F);
      this.backgroundTexture = TextureAsset.obtain("ui/130");
      this.textColor = new Color(0.54509807F, 0.30588236F, 0.13725491F, 1.0F);
      this.promptLabel = new TextraLabel("", FontFactory.createTextraFontWithLineHeight(FontManager.PLAIN_FONT, 30, 1.5F));
      this.promptLabel.setAlignment(2);
      this.promptLabel.setSize(468.0F, 82.0F);
      this.promptLabel.setPosition(35.0F, 225.0F);
      this.addActor(this.promptLabel);
      Table inputTable;
      (inputTable = new QuantityInputBackground(this, super.renderContext).pad(10.0F))
         .add(this.inputField = new QuantityInputDialog.InputField("", super.renderContext))
         .grow();
      inputTable.setSize(297.0F, 60.0F);
      inputTable.setPosition(121.0F, 160.0F);
      this.addActor(inputTable);
      this.inputField.addListener(new QuantityInputEnterListener(this));
      this.confirmButton = new InputDialogConfirmButton(this, "Đồng ý", super.renderContext);
      this.confirmButton.setSize(169.0F, 63.0F);
      this.confirmButton.setPosition(97.0F, 79.0F);
      this.addActor(this.confirmButton);
      this.confirmButton.addListener(new ConfirmInputClickListener(this));
      DialogButton closeButton;
      (closeButton = new DialogButton(this, "Đóng", super.renderContext)).setSize(169.0F, 63.0F);
      closeButton.setPosition(274.0F, 79.0F);
      this.addActor(closeButton);
      closeButton.addListener(new CloseInputDialogClickListener(this));
   }

   public final Button getConfirmButton() {
      return this.confirmButton;
   }

   public final void setInputText(String text) {
      this.inputField.setText(text);
      this.inputField.setCursorPosition(text.length());
   }

   public final void openWithPrompt(String prompt, TextField.TextFieldFilter.DigitsOnlyFilter filter, String initialText, QuantityInputDialog.InputCallback callback) {
      this.openWithPrompt(prompt, false, filter, initialText, callback);
   }

   /** Like decompiled C3656.m16748 (String, boolean passwordMode, ...): the secondary password input shows * instead of characters. */
   public final void openWithPrompt(String prompt, boolean passwordMode, TextField.TextFieldFilter.DigitsOnlyFilter filter, String initialText, QuantityInputDialog.InputCallback callback) {
      this.inputField.setPasswordCharacter('*');
      this.inputField.setPasswordMode(passwordMode);
      this.promptLabel.setText("[#8B4E23]" + prompt);
      this.inputCallback = callback;
      this.inputField.setText(initialText);
      this.inputField.setCursorPosition(this.inputField.getText().length());
      this.inputField.setTextFieldFilter(filter);
      this.getStage().setKeyboardFocus(this.inputField);
      GameMain.instance.getLoginScreen().debugScrollPane.showWindow(super.windowType);
   }

   public void confirmInput() {
      if (this.inputCallback != null) {
         this.inputCallback.onInputConfirmed(this.inputField.getText());
      }

      this.hide();
   }

   @Override
   public final boolean isDismissOnOutsideClick() {
      return true;
   }

   @Override
   public final void drawBackground() {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
   }

   public interface InputCallback {
      void onInputConfirmed(String text);
   }

   public class InputField extends GameTextField {
      public InputField(String text, RenderContext renderContext) {
         super(text, renderContext, FontFactory.scaleFont(FontManager.TEXT_FIELD_FONT, 22.0F));
      }

      @Override
      public final void drawBackground(float x, float y, float width, float height, boolean focused, boolean disabled, float parentAlpha) {
      }

      @Override
      public final void drawText(Batch batch, BitmapFont font, float x, float y) {
         super.drawText(batch, font, x, y - 5.0F);
      }
   }
}
