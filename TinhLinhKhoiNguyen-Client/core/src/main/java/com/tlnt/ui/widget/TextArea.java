package com.tlnt.ui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.Timer;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;

public class TextArea extends TextField {
   public IntArray backgroundPatch;
   public String backgroundPatch2;
   public int TMP_2;
   public int TMP_3;
   public int linesShowing;
   public float layout;
   public float prefRows;

   public TextArea(String text, RenderContext renderContext, BitmapFont font) {
      super(text, renderContext, font);
      if (super.inputListener != null) {
         this.removeListener(super.inputListener);
      }

      super.inputListener = this.createInputListener();
      this.addListener(super.inputListener);
      super.writeEnters = true;
      this.backgroundPatch = new IntArray();
      this.TMP_2 = 0;
      this.TMP_3 = 0;
      this.layout = -1.0F;
      this.linesShowing = 0;
      super.textHeight = super.font.getCapHeight() - super.font.getDescent();
   }

   @Override
   public final int letterUnderCursor(float x) {
      if (this.backgroundPatch.size <= 0) {
         return 0;
      } else if (this.TMP_2 << 1 >= this.backgroundPatch.size) {
         return super.text.length();
      } else {
         float[] positions = super.glyphPositions.items;
         int start = this.backgroundPatch.items[this.TMP_2 << 1];
         x += positions[start];
         int end = this.backgroundPatch.items[(this.TMP_2 << 1) + 1];
         start = start;

         while (start < end && !(positions[start] > x)) {
            start++;
         }

         if (start > 0 && positions[start] - x <= x - positions[start - 1]) {
            return start;
         } else {
            return Math.max(0, start - 1);
         }
      }
   }

   @Override
   public float getPrefHeight() {
      BitmapFont bitmapFont = super.font;
      return bitmapFont.getLineHeight() + 8.0F;
   }

   public final int getLines() {
      int endsWithNewline;
      int lineCount;
      label22: {
         label21: {
            lineCount = this.backgroundPatch.size / 2;
            if (super.text.length() != 0) {
               String str = super.text;
               int length = super.text.length();
               char lastChar = str.charAt(length - 1);
               if (lastChar == 10) {
                  break label21;
               }

               lastChar = super.text.charAt(super.text.length() - 1);
               if (lastChar == 13) {
                  break label21;
               }
            }

            endsWithNewline = 0;
            break label22;
         }

         endsWithNewline = 1;
      }

      if (endsWithNewline != 0) {
         endsWithNewline = 1;
      } else {
         endsWithNewline = 0;
      }

      return lineCount + endsWithNewline;
   }

   public final void moveCursorLine(int line) {
      if (line < 0) {
         this.TMP_2 = 0;
         super.cursor = 0;
         this.layout = -1.0F;
      } else if (line >= this.getLines()) {
         int lineCount = this.getLines();
         int lastLine = Math.max(0, lineCount - 1);
         super.cursor = super.text.length();
         if (line > this.getLines() || lastLine == this.TMP_2) {
            this.layout = -1.0F;
         }

         this.TMP_2 = lastLine;
      } else {
         if (line != this.TMP_2) {
            if (this.layout < 0.0F) {
               this.layout = this.backgroundPatch.size <= this.TMP_2 << 1
                  ? 0.0F
                  : super.glyphPositions.get(super.cursor)
                     - super.glyphPositions.get(this.backgroundPatch.get(this.TMP_2 << 1));
            }

            this.TMP_2 = line;
            TextArea area = this;
            int newCursor;
            if (this.TMP_2 << 1 >= this.backgroundPatch.size) {
               newCursor = super.text.length();
            } else {
               newCursor = this.backgroundPatch.get(this.TMP_2 << 1);
            }

            while (true) {
               area.cursor = newCursor;
               if (super.cursor >= super.text.length()) {
                  break;
               }

               int lineIndex = this.TMP_2 << 1;
               if (super.cursor > this.backgroundPatch.get(lineIndex + 1) - 1
                  || !(super.glyphPositions.get(super.cursor) - super.glyphPositions.get(this.backgroundPatch.get(this.TMP_2 << 1)) < this.layout)
                  )
                {
                  break;
               }

               area = this;
               newCursor = super.cursor + 1;
            }

            this.showCursor();
         }
      }
   }

   public final void updateCurrentLine() {
      int index;
      int lineIndex = index = this.calculateCurrentLineIndex(super.cursor);
      int line = lineIndex / 2;
      label24:
      if (index % 2 == 0
         || index + 1 >= this.backgroundPatch.size
         || super.cursor != this.backgroundPatch.items[index]
         || this.backgroundPatch.items[index + 1] != this.backgroundPatch.items[index]) {
         if (line >= this.backgroundPatch.size / 2 && super.text.length() != 0) {
            String str = super.text;
            int length = super.text.length();
            if (str.charAt(length - 1) != 10
               && super.text.charAt(super.text.length() - 1) != 13) {
               break label24;
            }
         }

         this.TMP_2 = line;
      }

      this.updateFirstLineShowing();
   }

   public final void showCursor() {
      this.updateCurrentLine();
      this.updateFirstLineShowing();
   }

   public void updateFirstLineShowing() {
      if (this.TMP_2 != this.TMP_3) {
         int value;
         if (this.TMP_2 >= this.TMP_3) {
            value = 1;
         } else {
            value = -1;
         }

         int step = value;

         while (true) {
            if (this.TMP_3 <= this.TMP_2) {
               value = this.TMP_3 + this.linesShowing;
               if (value - 1 >= this.TMP_2) {
                  break;
               }
            }

            this.TMP_3 += step;
         }
      }
   }

   public int calculateCurrentLineIndex(int cursor) {
      int index = 0;

      while (index < this.backgroundPatch.size && cursor > this.backgroundPatch.items[index]) {
         index++;
      }

      return index;
   }

   @Override
   public void sizeChanged() {
      this.backgroundPatch2 = null;
      BitmapFont bitmapFont = super.font;
      float availableHeight = this.getHeight() - 8.0F;
      this.linesShowing = (int)Math.floor((double)(availableHeight / bitmapFont.getLineHeight()));
      if (this.linesShowing <= 0) {
         this.linesShowing = 1;
      }
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      if (this.hasKeyboardFocus()) {
         if (!super.blinkTask.isScheduled()) {
            super.cursorOn = true;
            Timer.schedule(super.blinkTask, super.blinkTime, super.blinkTime);
         }
      } else {
         super.blinkTask.cancel();
         super.cursorOn = false;
      }

      ShapeDrawer drawer = super.renderContext.getShapeDrawer();
      float x = this.getX();
      float y = this.getY();
      float width = this.getWidth();
      float height = this.getHeight();
      boolean focused = this.hasKeyboardFocus();
      boolean disabled = this.isDisabled();
      float textX = x + 8.0F;
      float textWidth = width - 16.0F;
      this.drawBackground(x, y, width, height, focused, disabled, parentAlpha);
      String currentText = this.getText();
      BitmapFont bitmapFont = super.font;
      Color fontColor = disabled ? DISABLED_FONT_COLOR : FONT_COLOR;
      y = y + height - 4.0F;
      if (currentText.isEmpty() && this.getMessageText() != null && !focused) {
         fontColor = MESSAGE_FONT_COLOR;
         this.getMessageFont().setColor(fontColor);
         super.renderContext.getBatch().setColor(1.0F, 1.0F, 1.0F, parentAlpha * this.getColor().a);
         BitmapFont msgFont = this.getMessageFont();
         String messageText = this.getMessageText();
         int alignment = this.getAlignment();
         msgFont.draw(batch, messageText, textX, y, textWidth, alignment, true);
      } else {
         bitmapFont.setColor(fontColor);
         super.renderContext.getBatch().setColor(1.0F, 1.0F, 1.0F, parentAlpha * this.getColor().a);
         textWidth = textWidth;
         TextArea area = this;
         if (!super.text.equals(this.backgroundPatch2)) {
            this.backgroundPatch2 = super.text;
            BitmapFont layoutFont = super.font;
            this.backgroundPatch.clear();
            int lineStart = 0;
            int lastSpace = 0;
            Pool pool;
            GlyphLayout glyphLayout = (GlyphLayout)(pool = Pools.get(GlyphLayout.class)).obtain();

            for (int i = 0; i < area.text.length(); i++) {
               char c;
               char ch = c = area.text.charAt(i);
               if (ch != 13 && c != 10) {
                  lastSpace = area.continueCursor(i, 0) ? lastSpace : i;
                  glyphLayout.setText(layoutFont, area.text.subSequence(lineStart, i + 1));
                  if (glyphLayout.width > textWidth) {
                     if (lineStart >= lastSpace) {
                        lastSpace = i - 1;
                     }

                     area.backgroundPatch.add(lineStart);
                     area.backgroundPatch.add(lastSpace + 1);
                     lastSpace = lineStart = lastSpace + 1;
                  }
               } else {
                  area.backgroundPatch.add(lineStart);
                  area.backgroundPatch.add(i);
                  lineStart = i + 1;
               }
            }

            pool.free(glyphLayout);
            if (lineStart < area.text.length()) {
               area.backgroundPatch.add(lineStart);
               area.backgroundPatch.add(area.text.length());
            }

            area.showCursor();
         }

         if (super.hasSelection) {
            float selY = y;
            float selX = textX;
            ShapeDrawer selDrawer = drawer;
            TextArea selArea = this;
            int lineIdx = this.TMP_3 << 1;
            parentAlpha = 0.0F;
            int minIndex = Math.min(super.cursor, super.selectionStart);
            int maxIndex = Math.max(super.cursor, super.selectionStart);

            for (float lineOffset = -((height = bitmapFont.getLineHeight()) - super.textHeight) / 2.0F;
               lineIdx + 1 < selArea.backgroundPatch.size;
               lineIdx += 2
            ) {
               int lastLine = selArea.TMP_3 + selArea.linesShowing;
               if (lineIdx >= lastLine << 1) {
                  break;
               }

               int lineStart = selArea.backgroundPatch.get(lineIdx);
               int lineEnd = selArea.backgroundPatch.get(lineIdx + 1);
               if (((minIndex >= lineStart || maxIndex >= lineStart) && (minIndex <= lineEnd || maxIndex <= lineEnd) ? 0 : 1) == 0) {
                  int start = Math.max(lineStart, minIndex);
                  lineEnd = Math.min(lineEnd, maxIndex);
                  float selectionX = selArea.glyphPositions.get(start) - selArea.glyphPositions.get(lineStart);
                  float selectionWidth = selArea.glyphPositions.get(lineEnd) - selArea.glyphPositions.get(start);
                  float rectX = selX + selectionX;
                  float rectY = selY + lineOffset - height - parentAlpha;
                  selDrawer.drawFilledRectangle(rectX, rectY, Math.max(0.0F, selectionWidth), height, SELECTION_COLOR);
               }

               parentAlpha += height;
            }
         }

         parentAlpha = -(bitmapFont.getLineHeight() - super.textHeight) / 2.0F;
         x = 0.0F;
         int lineIdx = this.TMP_3 << 1;

         while (true) {
            int lastLine = this.TMP_3 + this.linesShowing;
            if (lineIdx >= lastLine << 1 || lineIdx >= this.backgroundPatch.size) {
               if (focused && !disabled && super.cursorOn) {
                  textWidth = 0.0F;
                  BitmapFont.BitmapFontData fontData = super.font.getData();
                  if (super.cursor < super.glyphPositions.size) {
                     if (this.TMP_2 << 1 < this.backgroundPatch.size) {
                        int lineStart = this.backgroundPatch.items[this.TMP_2 << 1];
                        float glyphOffset = 0.0F;
                        BitmapFont.Glyph glyph;
                        if ((glyph = fontData.getGlyph(super.displayText.charAt(lineStart))) != null) {
                           glyphOffset = glyph.fixedWidth ? 0.0F : (float)(-glyph.xoffset) * fontData.scaleX - fontData.padLeft;
                        }

                        textWidth = super.glyphPositions.get(super.cursor) - super.glyphPositions.get(lineStart) + glyphOffset;
                     }
                  }

                  height = textX + textWidth + fontData.cursorX;
                  float cursorBaseY = y + parentAlpha;
                  BitmapFont cursorFont = super.font;
                  lastLine = this.TMP_2 - this.TMP_3;
                  float cursorY = cursorBaseY + (float)(-(lastLine + 1)) * cursorFont.getLineHeight();
                  parentAlpha = bitmapFont.getLineHeight();
                  drawer.drawFilledRectangle(height, cursorY, 2.0F, parentAlpha, CURSOR_COLOR);
               }

               return;
            }

            float lineY = y + parentAlpha - x;
            int lineStartIdx = this.backgroundPatch.items[lineIdx];
            int lineEndIdx = this.backgroundPatch.items[lineIdx + 1];
            bitmapFont.draw(batch, super.displayText, textX, lineY, lineStartIdx, lineEndIdx, 0.0F, 8, false);
            x += bitmapFont.getLineHeight();
            lineIdx += 2;
         }
      }
   }

   @Override
   public final TextField.TextFieldClickListener createInputListener() {
      return new TextArea.TextAreaListener();
   }

   @Override
   public final void setSelection(int selectionStart, int selectionEnd) {
      super.setSelection(selectionStart, selectionEnd);
      this.updateCurrentLine();
   }

   @Override
   public final void moveCursor(boolean forward, boolean jump) {
      int dir;
      if (forward) {
         dir = 1;
      } else {
         dir = -1;
      }

      int step = dir;
      int index;
      if ((index = (this.TMP_2 << 1) + step) >= 0
         && index + 1 < this.backgroundPatch.size
         && this.backgroundPatch.items[index] == super.cursor
         && this.backgroundPatch.items[index + 1] == super.cursor) {
         this.TMP_2 += step;
         if (jump) {
            super.moveCursor(forward, jump);
         }

         this.showCursor();
      } else {
         super.moveCursor(forward, jump);
      }

      this.updateCurrentLine();
   }

   @Override
   public final boolean continueCursor(int index, int offset) {
      label20: {
         int line = this.calculateCurrentLineIndex(index + offset);
         if (super.continueCursor(index, offset)) {
            if (line < 0
               || line >= this.backgroundPatch.size - 2
               || this.backgroundPatch.items[line + 1] != index) {
               break label20;
            }

            int lineEnd = this.backgroundPatch.items[line + 1];
            if (lineEnd == this.backgroundPatch.items[line + 2]) {
               break label20;
            }
         }

         return false;
      }

      return true;
   }

   public class TextAreaListener extends TextField.TextFieldClickListener {
      @Override
      public final void setCursorPosition(float x, float y) {
         TextArea.this.layout = -1.0F;
         BitmapFont bitmapFont = TextArea.super.font;
         float height = TextArea.this.getHeight();
         x = Math.max(0.0F, x - 8.0F);
         TextArea.this.TMP_2 = (int)Math.floor((double)((height - y) / bitmapFont.getLineHeight())) + TextArea.this.TMP_3;
         TextArea area = TextArea.this;
         int line = TextArea.this.TMP_2;
         int lineCount = TextArea.this.getLines();
         area.TMP_2 = Math.max(0, Math.min(line, Math.max(0, lineCount - 1)));
         super.setCursorPosition(x + 8.0F, y);
         TextArea.this.updateCurrentLine();
      }

      @Override
      public final boolean keyDown(InputEvent event, int keycode) {
         boolean result = super.keyDown(event, keycode);
         if (!TextArea.this.hasKeyboardFocus()) {
            return result;
         } else {
            int shiftFlag;
            int repeat = 0;
            label41: {
               if (!Gdx.input.isKeyPressed(59)) {
                  if (!Gdx.input.isKeyPressed(60)) {
                     shiftFlag = 0;
                     break label41;
                  }
               }

               shiftFlag = 1;
            }

            int shift = shiftFlag;
            if (keycode == 20) {
               if (shift != 0) {
                  if (!TextArea.super.hasSelection) {
                     TextArea.super.selectionStart = TextArea.super.cursor;
                     TextArea.super.hasSelection = true;
                  }
               } else {
                  TextArea.this.clearSelection();
               }

               TextArea.this.moveCursorLine(TextArea.this.TMP_2 + 1);
               repeat = 1;
            } else {
               if (keycode == 19) {
                  if (shift != 0) {
                     if (!TextArea.super.hasSelection) {
                        TextArea.super.selectionStart = TextArea.super.cursor;
                        TextArea.super.hasSelection = true;
                     }
                  } else {
                     TextArea.this.clearSelection();
                  }

                  TextArea.this.moveCursorLine(TextArea.this.TMP_2 - 1);
                  repeat = 1;
               } else {
                  TextArea.this.layout = -1.0F;
               }
            }

            if (repeat != 0) {
               this.scheduleKeyRepeatTask(keycode);
            }

            TextArea.this.showCursor();
            return true;
         }
      }

      @Override
      public final boolean checkFocusTraversal(char character) {
         if (TextArea.super.focusTraversal && character == 9) {
            return true;
         } else {
            return false;
         }
      }

      @Override
      public final boolean keyTyped(InputEvent event, char character) {
         boolean result = super.keyTyped(event, character);
         TextArea.this.showCursor();
         return result;
      }

      @Override
      public final void goHome(boolean jump) {
         if (jump) {
            TextArea.super.cursor = 0;
         } else {
            if (TextArea.this.TMP_2 << 1 < TextArea.this.backgroundPatch.size) {
               TextArea.super.cursor = TextArea.this.backgroundPatch.get(TextArea.this.TMP_2 << 1);
            }
         }
      }

      @Override
      public final void goEnd(boolean jump) {
         if (!jump && TextArea.this.TMP_2 < TextArea.this.getLines()) {
            if ((TextArea.this.TMP_2 << 1) + 1 < TextArea.this.backgroundPatch.size) {
               TextArea.super.cursor = TextArea.this.backgroundPatch.get((TextArea.this.TMP_2 << 1) + 1);
            }
         } else {
            TextArea.super.cursor = TextArea.super.text.length();
         }
      }
   }
}
