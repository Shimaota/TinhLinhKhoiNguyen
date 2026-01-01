package com.tlnt.ui.dialog.widget;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class TextInputRow extends BaseTable {
   public CompositeSprite focusedBorderSprite;
   public CompositeSprite unfocusedBorderSprite;
   public InputRowTextField textField;
   public InputVisibilityToggle visibilityToggle;

   public TextInputRow(String hint, RenderContext renderContext) {
      super(renderContext);
      String upPath = "ui/151";
      String downPath = "ui/152";
      String checkedPath = "ui/153";
      this.focusedBorderSprite = CompositeSprite.obtain(upPath, downPath, checkedPath, false);
      upPath = "ui/66";
      downPath = "ui/67";
      checkedPath = "ui/68";
      this.unfocusedBorderSprite = CompositeSprite.obtain(upPath, downPath, checkedPath, false);
      this.add(this.textField = new InputRowTextField(this, "", super.renderContext)).grow().pad(10.0F);
      this.textField.setMessageText(hint);
      this.visibilityToggle = new InputVisibilityToggle(this, super.renderContext);
      this.visibilityToggle.addListener(new InputVisibilityClickListener(this));
   }

   public final void setPasswordMode(boolean passwordMode) {
      this.clear();
      this.add(this.textField).grow().pad(10.0F);
      this.textField.setPasswordMode(true);
      this.textField.setPasswordCharacter((char)(10954 ^ '⫠'));
      this.add(this.visibilityToggle).size(47.0F, 34.0F).padRight(15.0F);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.textField.hasKeyboardFocus()) {
         super.renderContext.drawCompositeSprite(this.focusedBorderSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      } else {
         super.renderContext.drawCompositeSprite(this.unfocusedBorderSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      }

      super.draw(batch, parentAlpha);
   }

   public final String getText() {
      return this.textField.getText();
   }

   public final void clearText() {
      this.textField.setText("");
   }

   public final StyledTextField getTextField() {
      return this.textField;
   }

}
