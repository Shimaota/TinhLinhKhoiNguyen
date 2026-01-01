package com.tlnt.ui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Disableable;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.badlogic.gdx.scenes.scene2d.utils.UIUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Clipboard;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.Null;
import com.badlogic.gdx.utils.Pools;
import com.badlogic.gdx.utils.Timer;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.thirdparty.shapedrawer.ShapeDrawer;
import com.tlnt.ui.window.chat.widget.EnterKeySubmitListener;

public class TextField extends Widget implements Disableable {
   public static final Vector2 backgroundPatch = new Vector2();
   public static final Vector2 TMP_2 = new Vector2();
   public static final Vector2 TMP_3 = new Vector2();
   public static float keyRepeatInitialTime = 0.4F;
   public static float keyRepeatTime = 0.1F;
   public String text;
   public int cursor;
   public int selectionStart;
   public boolean hasSelection;
   public boolean writeEnters;
   public GlyphLayout layout = new GlyphLayout();
   public final FloatArray glyphPositions = new FloatArray();
   public String backgroundPatch2;
   public CharSequence displayText;
   public Clipboard clipboard;
   public InputListener inputListener;
   @Null
   public TextField.TextFieldListener textFieldListener;
   @Null
   public TextField.TextFieldFilter filter;
   public TextField.DefaultOnscreenKeyboard onscreenKeyboard = new TextField.DefaultOnscreenKeyboard();
   public boolean focusTraversal;
   public boolean onlyFontChars;
   public boolean disabled;
   public int linesShowing;
   public float prefRows;
   public float selectionWidth;
   public String undoText;
   public long lastChangeTime;
   public boolean passwordMode;
   public StringBuilder passwordBuffer;
   public char passwordCharacter;
   public float fontOffset;
   public float textHeight;
   public float textOffset;
   public float renderOffset;
   public int visibleTextStart;
   public int visibleTextEnd;
   public int maxLength;
   public boolean focused;
   public boolean cursorOn;
   public float blinkTime;
   public final CursorBlinkTask blinkTask;
   public final TextField.KeyRepeatTask keyRepeatTask;
   public boolean programmaticChangeEvents;
   public RenderContext renderContext;
   public static Color backgroundColor = new Color(0.13F, 0.13F, 0.13F, 1.0F);
   public static Color borderColor = new Color(0.25F, 0.25F, 0.25F, 1.0F);
   public static Color focusedBorderColor = new Color(0.2F, 0.6F, 1.0F, 1.0F);
   public static final Color SELECTION_COLOR = new Color(0.2F, 0.6F, 1.0F, 0.35F);
   public static final Color CURSOR_COLOR = new Color(1.0F, 1.0F, 1.0F, 1.0F);
   public static final Color FONT_COLOR = Color.WHITE;
   public static final Color DISABLED_FONT_COLOR = Color.GRAY;
   public static final Color MESSAGE_FONT_COLOR = Color.LIGHT_GRAY;
   public BitmapFont font;
   public BitmapFont messageFont;

   public TextField(String text, RenderContext renderContext, BitmapFont font) {
      this.focusTraversal = true;
      this.onlyFontChars = true;
      this.linesShowing = 8;
      this.undoText = "";
      this.passwordCharacter = (char)149;
      this.blinkTime = 0.32F;
      this.blinkTask = new CursorBlinkTask(this);
      this.keyRepeatTask = new TextField.KeyRepeatTask();
      this.renderContext = renderContext;
      this.font = font;
      this.messageFont = font;
      this.clipboard = Gdx.app.getClipboard();
      this.initialize();
      this.setText(text);
      this.updateDisplayText();
      this.setSize(this.getPrefWidth(), this.getPrefHeight());
   }

   public TextField(String text, RenderContext renderContext, BitmapFont font, BitmapFont messageFont) {
      this.focusTraversal = true;
      this.onlyFontChars = true;
      this.linesShowing = 8;
      this.undoText = "";
      this.passwordCharacter = (char)149;
      this.blinkTime = 0.32F;
      this.blinkTask = new CursorBlinkTask(this);
      this.keyRepeatTask = new TextField.KeyRepeatTask();
      this.renderContext = renderContext;
      this.font = font;
      this.messageFont = messageFont;
      this.clipboard = Gdx.app.getClipboard();
      this.initialize();
      this.setText(text);
      this.updateDisplayText();
      this.setSize(this.getPrefWidth(), this.getPrefHeight());
   }

   public void initialize() {
      this.addListener(this.inputListener = this.createInputListener());
      // Focus lost (clicked outside, dialog closed, field removed from the stage) without moving to another field -> hide the soft keyboard.
      // Moving focus to another field with Tab / Next shows the keyboard again. PC: setOnscreenKeyboardVisible does nothing.
      this.addListener(new FocusListener() {
         @Override
         public void keyboardFocusChanged(FocusListener.FocusEvent event, Actor actor, boolean focused) {
            if (actor != TextField.this) {
               return;
            }

            if (focused) {
               TextField.this.onscreenKeyboard.show(true);
            } else if (!(event.getRelatedActor() instanceof TextField)) {
               TextField.this.onscreenKeyboard.show(false);
            }
         }
      });
   }

   public TextField.TextFieldClickListener createInputListener() {
      return new TextField.TextFieldClickListener();
   }

   public int letterUnderCursor(float x) {
      float localX;
      x = (localX = x - 8.0F) - this.textOffset;
      int count = this.glyphPositions.size;
      float[] positions = this.glyphPositions.items;
      int letter = count - 1;

      for (int i = 1; i < count; i++) {
         if (positions[i] > x) {
            float distToNext = positions[i] - x;
            letter = distToNext <= x - positions[i - 1] ? i : i - 1;
            break;
         }
      }

      // When the field is empty, glyphPositions are measured on the hint text (updateDisplayText), not the real text.
      return Math.min(letter, this.text.length());
   }

   public final boolean withinMaxLength(int size) {
      return true;
   }

   @Override
   public void draw(Batch batch, float parentAlpha) {
      if (this.hasKeyboardFocus()) {
         if (!this.blinkTask.isScheduled()) {
            this.cursorOn = true;
            Timer.schedule(this.blinkTask, this.blinkTime, this.blinkTime);
         }
      } else {
         this.blinkTask.cancel();
         this.cursorOn = false;
      }

      float x = this.getX();
      float y = this.getY();
      float width = this.getWidth();
      float height = this.getHeight();
      boolean focused = this.hasKeyboardFocus();
      boolean disabled = this.isDisabled();
      float textX = x + 8.0F;
      float textWidth = width - 16.0F;
      this.drawBackground(x, y, width, height, focused, disabled, parentAlpha);
      String currentText = this.text;
      String selectedText;
      if (this.hasSelection && !this.text.isEmpty()) {
         int selStart = Math.min(this.selectionStart, this.cursor);
         int selEnd = Math.max(this.selectionStart, this.cursor);
         selStart = Math.max(0, Math.min(selStart, this.text.length()));
         selEnd = Math.max(0, Math.min(selEnd, this.text.length()));
         selectedText = this.text.substring(selStart, selEnd);
      } else {
         selectedText = "";
      }

      int hasSelFlag;
      if (selectedText.length() > 0) {
         hasSelFlag = 1;
      } else {
         hasSelFlag = 0;
      }

      int hasSelectedText = hasSelFlag;
      Color fontColor = disabled ? DISABLED_FONT_COLOR : FONT_COLOR;
      if (currentText.isEmpty() && this.backgroundPatch2 != null && !focused) {
         fontColor = MESSAGE_FONT_COLOR;
         this.messageFont.setColor(fontColor);
      } else {
         this.font.setColor(fontColor);
      }

      this.renderContext.getBatch().setColor(1.0F, 1.0F, 1.0F, parentAlpha * this.getColor().a);
      TextField field = this;
      float visibleWidth = this.getWidth() - 16.0F;
      int glyphCount = this.glyphPositions.size;
      float[] positions = this.glyphPositions.items;
      this.cursor = MathUtils.clamp(this.cursor, 0, Math.min(glyphCount - 1, this.text.length()));
      float distance;
      if ((distance = positions[Math.max(0, this.cursor - 1)] + this.renderOffset) <= 0.0F) {
         this.renderOffset -= distance;
      } else {
         int lastIndex = glyphCount - 1;
         int index = Math.min(lastIndex, this.cursor + 1);
         float minX = positions[index] - visibleWidth;
         if (-this.renderOffset < minX) {
            this.renderOffset = -minX;
         }
      }

      float maxOffset = 0.0F;
      float fullWidth = positions[glyphCount - 1];

      for (int i = glyphCount - 2; i >= 0; i--) {
         float glyphX = positions[i];
         if (fullWidth - glyphX > visibleWidth) {
            break;
         }

         maxOffset = glyphX;
      }

      if (-this.renderOffset > maxOffset) {
         this.renderOffset = -maxOffset;
      }

      this.visibleTextStart = 0;
      parentAlpha = 0.0F;

      for (int i = 0; i < glyphCount; i++) {
         if (positions[i] >= -field.renderOffset) {
            field.visibleTextStart = i;
            parentAlpha = positions[i];
            break;
         }
      }

      int end = field.visibleTextStart + 1;
      maxOffset = visibleWidth - field.renderOffset;
      int endLimit = Math.min(field.displayText.length(), glyphCount);

      while (end <= endLimit && !(positions[end] > maxOffset)) {
         end++;
      }

      field.visibleTextEnd = Math.max(0, end - 1);
      if ((field.linesShowing & 8) == 0) {
         field.textOffset = visibleWidth - positions[field.visibleTextEnd] - field.fontOffset + parentAlpha;
         if ((field.linesShowing & 1) != 0) {
            field.textOffset = (float)Math.round(field.textOffset * 0.5F);
         }
      } else {
         field.textOffset = parentAlpha + field.renderOffset;
      }

      if (field.hasSelection) {
         int minIndex = Math.min(field.cursor, field.selectionStart);
         glyphCount = Math.max(field.cursor, field.selectionStart);
         distance = Math.max(positions[minIndex] - positions[field.visibleTextStart], -field.textOffset);
         maxOffset = Math.min(positions[glyphCount] - positions[field.visibleTextStart], visibleWidth - field.textOffset);
         field.prefRows = distance;
         field.selectionWidth = maxOffset - distance;
      }

      parentAlpha = y + (height + this.font.getCapHeight()) / 2.0F - this.font.getDescent();
      ShapeDrawer drawer = this.renderContext.getShapeDrawer();
      if (hasSelectedText != 0 && this.selectionWidth > 0.0F) {
         fullWidth = textX + this.textOffset + this.prefRows;
         drawer.drawFilledRectangle(fullWidth, y + 4.0F, this.selectionWidth, height - 8.0F, SELECTION_COLOR);
      }

      this.drawText(batch, this.font, textX, parentAlpha);
      ShapeDrawer cursorDrawer = this.renderContext.getShapeDrawer();
      if (focused && !disabled && this.cursorOn) {
         float cursorX = textX + this.textOffset;
         if (!this.text.isEmpty() && this.cursor < this.glyphPositions.size) {
            cursorX += this.glyphPositions.get(this.cursor);
         }

         distance = textX + textWidth - 2.0F;
         float clampedCursorX = Math.max(textX, Math.min(cursorX, distance));
         float cursorY = y + 4.0F;
         distance = height - 8.0F;
         cursorDrawer.drawFilledRectangle(clampedCursorX, cursorY, 2.0F, distance, CURSOR_COLOR);
      }
   }

   public void drawBackground(float x, float y, float width, float height, boolean focused, boolean disabled, float parentAlpha) {
      ShapeDrawer drawer;
      float rectX;
      float rectY;
      float rectWidth;
      float rectHeight;
      Color borderCol;
      label13: {
         ShapeDrawer shapeDrawer = this.renderContext.getShapeDrawer();
         if (disabled) {
            Color bgColor;
            (bgColor = new Color(backgroundColor)).a = 0.5F * parentAlpha;
            shapeDrawer.drawFilledRectangle(x, y, width, height, bgColor);
            drawer = shapeDrawer;
            rectX = x;
            rectY = y;
            rectWidth = width;
            rectHeight = height;
         } else {
            shapeDrawer.drawFilledRectangle(x, y, width, height, backgroundColor);
            drawer = shapeDrawer;
            rectX = x;
            rectY = y;
            rectWidth = width;
            rectHeight = height;
            if (focused) {
               borderCol = focusedBorderColor;
               break label13;
            }
         }

         borderCol = borderColor;
      }

      drawer.drawRectangle(rectX, rectY, rectWidth, rectHeight, borderCol, 2.0F);
   }

   public void drawText(Batch batch, BitmapFont font, float x, float y) {
      Object displayed = this.displayText == null ? "" : this.displayText;
      if (this.text.isEmpty() && this.backgroundPatch2 != null && !this.hasKeyboardFocus()) {
         this.messageFont.setColor(RenderContext.parseColor("#DBBA8B"));
         float messageX = x + this.textOffset;
         this.messageFont.draw(batch, this.backgroundPatch2, messageX, y, 0.0F, 8, false);
      } else {
         float textX = x + this.textOffset;
         font.draw(
            batch,
            (CharSequence)displayed,
            textX,
            y,
            this.visibleTextStart,
            this.visibleTextEnd,
            0.0F,
            8,
            false
         );
      }
   }

   public final void updateDisplayText() {
      String currentText = this.text;
      int textLength = this.text.length();
      StringBuilder buffer = new StringBuilder();

      for (int i = 0; i < textLength; i++) {
         char c = currentText.charAt(i);
         buffer.append(c);
      }

      String newDisplayText = buffer.toString();
      if (this.passwordMode && this.passwordCharacter != 0) {
         if (this.passwordBuffer == null) {
            this.passwordBuffer = new StringBuilder(newDisplayText.length());
         }

         if (this.passwordBuffer.length() > textLength) {
            this.passwordBuffer.setLength(textLength);
         } else {
            for (int i = this.passwordBuffer.length(); i < textLength; i++) {
               this.passwordBuffer.append(this.passwordCharacter);
            }
         }

         this.displayText = this.passwordBuffer;
      } else {
         this.displayText = newDisplayText;
      }

      String layoutText;
      if ((layoutText = this.displayText.toString()).isEmpty() && this.backgroundPatch2 != null) {
         layoutText = this.backgroundPatch2;
      }

      this.layout
         .setText(
            this.font,
            layoutText.replace((char)13, (char)32)
               .replace((char)10, (char)32)
         );
      this.glyphPositions.clear();
      float x = 0.0F;
      if (this.layout.runs.size > 0) {
         FloatArray xAdvances = this.layout.runs.first().xAdvances;
         this.fontOffset = xAdvances.first();
         int idx = 1;

         for (int advanceCount = xAdvances.size; idx < advanceCount; idx++) {
            this.glyphPositions.add(x);
            x += xAdvances.get(idx);
         }
      } else {
         this.fontOffset = 0.0F;
      }

      this.glyphPositions.add(x);
      this.visibleTextStart = Math.min(this.visibleTextStart, this.glyphPositions.size - 1);
      this.visibleTextEnd = MathUtils.clamp(this.visibleTextEnd, this.visibleTextStart, this.glyphPositions.size - 1);
      if (this.selectionStart > newDisplayText.length()) {
         this.selectionStart = textLength;
      }
   }

   public final void copy() {
      if (this.hasSelection && !this.passwordMode) {
         this.clipboard.setContents(this.text.substring(Math.min(this.cursor, this.selectionStart), Math.max(this.cursor, this.selectionStart)));
      }
   }

   public final void cut(boolean fireChangeEvent) {
      if (this.hasSelection && !this.passwordMode) {
         this.copy();
         this.cursor = this.delete(true);
         this.updateDisplayText();
      }
   }

   public final void paste(@Null String content, boolean fireChangeEvent) {
      if (content != null) {
         StringBuilder buffer = new StringBuilder();
         int textLength = this.text.length();
         if (this.hasSelection) {
            textLength -= Math.abs(this.cursor - this.selectionStart);
         }

         int i = 0;

         for (int contentLength = content.length(); i < contentLength; i++) {
            int c;
            label62: {
               c = textLength + buffer.length();
               c = content.charAt(i);
               if (this.writeEnters) {
                  if (c == 10) {
                     break label62;
                  }

                  if (c == 13) {
                     break label62;
                  }
               }

               if (c == 13) {
                  continue;
               }

               if (c == 10
                  || this.onlyFontChars && !this.font.getData().hasGlyph((char)c)
                  || this.filter != null && !this.filter.acceptChar((char)c)) {
                  continue;
               }
            }

            buffer.append((char)c);
         }

         content = buffer.toString();
         if (this.hasSelection) {
            this.cursor = this.delete(fireChangeEvent);
         }

         this.cursor = MathUtils.clamp(this.cursor, 0, this.text.length());
         if (fireChangeEvent) {
            this.changeText(this.text, insert(this.cursor, content, this.text));
         } else {
            this.text = insert(this.cursor, content, this.text);
         }

         this.updateDisplayText();
         this.cursor = this.cursor + content.length();
      }
   }

   public static String insert(int position, CharSequence text, String to) {
      if (to.length() == 0) {
         return text.toString();
      } else {
         position = MathUtils.clamp(position, 0, to.length());
         return to.substring(0, position) + text + to.substring(position, to.length());
      }
   }

   public final int delete(boolean fireChangeEvent) {
      int from = MathUtils.clamp(this.selectionStart, 0, this.text.length());
      int to = MathUtils.clamp(this.cursor, 0, this.text.length());
      int minIndex = Math.min(from, to);
      from = Math.max(from, to);
      String before;
      if (minIndex > 0) {
         before = this.text.substring(0, minIndex);
      } else {
         before = "";
      }

      String newText = before + (from < this.text.length() ? this.text.substring(from) : "");
      if (fireChangeEvent) {
         this.changeText(this.text, newText);
      } else {
         this.text = newText;
      }

      this.hasSelection = false;
      return minIndex;
   }

   public final void next(boolean up) {
      Stage stage;
      if ((stage = this.getStage()) != null) {
         TextField current = this;
         Vector2 currentCoords = this.getParent().localToStageCoordinates(TMP_2.set(this.getX(), this.getY()));
         Vector2 bestCoords = backgroundPatch;

         while (true) {
            TextField textField;
            if ((textField = current.findNextTextField(stage.getActors(), null, bestCoords, currentCoords, up)) == null) {
               if (up) {
                  currentCoords.set(-Float.MAX_VALUE, -Float.MAX_VALUE);
               } else {
                  currentCoords.set(Float.MAX_VALUE, Float.MAX_VALUE);
               }

               textField = current.findNextTextField(stage.getActors(), null, bestCoords, currentCoords, up);
            }

            if (textField == null) {
               Gdx.input.setOnscreenKeyboardVisible(false);
               return;
            }

            if (stage.setKeyboardFocus(textField)) {
               textField.setCursorPosition(textField.text.length());
               return;
            }

            current = textField;
            currentCoords.set(bestCoords);
         }
      }
   }

   @Null
   public TextField findNextTextField(Array<Actor> actors, @Null TextField best, Vector2 bestCoords, Vector2 currentCoords, boolean up) {
      int i = 0;

      for (int actorCount = actors.size; i < actorCount; i++) {
         Actor actor;
         if ((actor = (Actor)actors.get(i)) instanceof TextField) {
            TextField textField;
            if (actor != this && !(textField = (TextField)actor).isDisabled() && textField.focusTraversal && textField.ascendantsVisible()) {
               int flag;
               Vector2 actorCoords;
               label93: {
                  if ((actorCoords = actor.getParent().localToStageCoordinates(TMP_3.set(actor.getX(), actor.getY()))).y != currentCoords.y) {
                     if (actorCoords.y < currentCoords.y) {
                        flag = 1;
                     } else {
                        flag = 0;
                     }

                     if (((flag != 0) ^ up)) {
                        flag = 1;
                        break label93;
                     }
                  }

                  flag = 0;
               }

               int better = flag;
               if (actorCoords.y == currentCoords.y && ((actorCoords.x > currentCoords.x) ^ up)) {
                  flag = 1;
               } else {
                  flag = 0;
               }

               int sameLineBetter = flag;
               if (better != 0 || sameLineBetter != 0) {
                  label111: {
                     label75:
                     if (best != null) {
                        if (actorCoords.y != bestCoords.y) {
                           if (actorCoords.y > bestCoords.y) {
                              flag = 1;
                           } else {
                              flag = 0;
                           }

                           if (((flag != 0) ^ up)) {
                              break label75;
                           }
                        }

                        flag = 0;
                        break label111;
                     }

                     flag = 1;
                  }

                  better = flag;
                  if (flag == 0) {
                     label65: {
                        if (actorCoords.y == bestCoords.y) {
                           if (actorCoords.x < bestCoords.x) {
                              flag = 1;
                           } else {
                              flag = 0;
                           }

                           if (((flag != 0) ^ up)) {
                              flag = 1;
                              break label65;
                           }
                        }

                        flag = 0;
                     }

                     better = flag;
                  }

                  if (better != 0) {
                     best = (TextField)actor;
                     bestCoords.set(actorCoords);
                  }
               }
            }
         } else if (actor instanceof Group) {
            best = this.findNextTextField(((Group)actor).getChildren(), best, bestCoords, currentCoords, up);
         }
      }

      return best;
   }

   public final void setTextFieldListener(@Null EnterKeySubmitListener textFieldListener) {
      this.textFieldListener = textFieldListener;
   }

   public final void setTextFieldFilter(@Null TextField.TextFieldFilter filter) {
      this.filter = filter;
   }

   @Null
   public final String getMessageText() {
      return this.backgroundPatch2;
   }

   public final void setMessageText(@Null String backgroundPatch2) {
      this.backgroundPatch2 = backgroundPatch2;
      this.updateDisplayText();
   }

   public final BitmapFont getMessageFont() {
      return this.messageFont;
   }

   public final void setText(@Null String text) {
      if (text == null) {
         text = "";
      }

      if (!text.equals(this.text)) {
         this.hasSelection = false;
         this.text = "";
         this.paste(text, false);
         this.cursor = 0;
      }
   }

   public final String getText() {
      return this.text;
   }

   public final boolean changeText(String text, String newText) {
      if (newText.equals(text)) {
         return false;
      } else {
         this.text = newText;
         ChangeListener.ChangeEvent changeEvent = Pools.obtain(ChangeListener.ChangeEvent.class);
         boolean cancelled;
         if (cancelled = this.fire(changeEvent)) {
            this.text = text;
         }

         Pools.free(changeEvent);
         if (!cancelled) {
            return true;
         } else {
            return false;
         }
      }
   }

   public void setSelection(int selectionStart, int cursor) {
      if (selectionStart < 0) {
         throw new IllegalArgumentException("selectionStart must be >= 0");
      } else if (cursor < 0) {
         throw new IllegalArgumentException("selectionEnd must be >= 0");
      } else {
         selectionStart = Math.min(this.text.length(), selectionStart);
         if ((cursor = Math.min(this.text.length(), cursor)) == selectionStart) {
            this.hasSelection = false;
         } else {
            if (cursor < selectionStart) {
               int temp = cursor;
               cursor = selectionStart;
               selectionStart = temp;
            }

            this.hasSelection = true;
            this.selectionStart = selectionStart;
            this.cursor = cursor;
         }
      }
   }

   public final void selectAll() {
      this.setSelection(0, this.text.length());
   }

   public final void clearSelection() {
      this.hasSelection = false;
   }

   public final void setCursorPosition(int cursorPosition) {
      if (cursorPosition < 0) {
         throw new IllegalArgumentException("cursorPosition must be >= 0");
      } else {
         this.hasSelection = false;
         this.cursor = Math.min(cursorPosition, this.text.length());
      }
   }

   @Override
   public float getPrefWidth() {
      return 150.0F;
   }

   public final void setAlignment(int alignment) {
      this.linesShowing = 1;
   }

   public final int getAlignment() {
      return this.linesShowing;
   }

   public final void setPasswordMode(boolean passwordMode) {
      this.passwordMode = passwordMode;
      this.updateDisplayText();
   }

   public final void setPasswordCharacter(char passwordCharacter) {
      this.passwordCharacter = (char)42;
      if (this.passwordMode) {
         this.updateDisplayText();
      }
   }

   @Override
   public void setDisabled(boolean disabled) {
      this.disabled = disabled;
   }

   @Override
   public boolean isDisabled() {
      return this.disabled;
   }

   public void moveCursor(boolean forward, boolean jump) {
      int limit = forward ? this.text.length() : 0;
      int charOffset = forward ? 0 : -1;

      do {
         if (forward) {
            if ((this.cursor += 1) >= limit) {
               break;
            }
         } else {
            if ((this.cursor -= 1) <= limit) {
               break;
            }
         }
      } while (jump && this.continueCursor(this.cursor, charOffset));
   }

   public boolean continueCursor(int index, int offset) {
      return Character.isLetterOrDigit(this.text.charAt(index + offset));
   }

   public static class DefaultOnscreenKeyboard implements TextField.OnscreenKeyboard {
      @Override
      public final void show(boolean visible) {
         Gdx.input.setOnscreenKeyboardVisible(visible);
      }
   }

   public class KeyRepeatTask extends Timer.Task {
      public int keycode;

      @Override
      public final void run() {
         if (TextField.this.getStage() == null) {
            this.cancel();
         } else {
            TextField.this.inputListener.keyDown(null, this.keycode);
         }
      }
   }

   public interface OnscreenKeyboard {
      void show(boolean visible);
   }

   public class TextFieldClickListener extends ClickListener {

      @Override
      public void clicked(InputEvent event, float x, float y) {
         int tapCount;
         int count = tapCount = this.getTapCount();
         if (count == 1) {
            TextField.this.clearSelection();
         } else {
            if (tapCount == 2) {
               TextField.this.selectAll();
            }
         }
      }

      @Override
      public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
         if (!super.touchDown(event, x, y, pointer, button)) {
            return false;
         } else if (pointer == 0 && button != 0) {
            return false;
         } else if (TextField.this.disabled) {
            return true;
         } else {
            this.setCursorPosition(x, y);
            TextField.this.selectionStart = TextField.this.cursor;
            Stage stage;
            if ((stage = TextField.this.getStage()) != null) {
               stage.setKeyboardFocus(TextField.this);
            }

            TextField.this.onscreenKeyboard.show(true);
            TextField.this.hasSelection = true;
            return true;
         }
      }

      @Override
      public void touchDragged(InputEvent event, float x, float y, int pointer) {
         super.touchDragged(event, x, y, pointer);
         this.setCursorPosition(x, y);
      }

      @Override
      public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
         if (TextField.this.selectionStart == TextField.this.cursor) {
            TextField.this.hasSelection = false;
         }

         super.touchUp(event, x, y, pointer, button);
      }

      public void setCursorPosition(float x, float y) {
         TextField.this.cursor = TextField.this.letterUnderCursor(x);
         TextField.this.cursorOn = TextField.this.focused;
         TextField.this.blinkTask.cancel();
      }

      public void goHome(boolean jump) {
         TextField.this.cursor = 0;
      }

      public void goEnd(boolean jump) {
         TextField.this.cursor = TextField.this.text.length();
      }

      @Override
      public boolean keyDown(InputEvent event, int keycode) {
         if (TextField.this.disabled) {
            return false;
         } else {
            TextField.this.cursorOn = TextField.this.focused;
            TextField.this.blinkTask.cancel();
            if (!TextField.this.hasKeyboardFocus()) {
               return false;
            } else {
               int repeat = 0;
               boolean ctrl;
               int jumpFlag;
               if ((ctrl = UIUtils.ctrl()) && !TextField.this.passwordMode) {
                  jumpFlag = 1;
               } else {
                  jumpFlag = 0;
               }

               int jump = jumpFlag;
               int handled = 1;
               if (ctrl) {
                  switch (keycode) {
                     case 29:
                        TextField.this.selectAll();
                        return true;
                     case 31:
                     case 124:
                        TextField.this.copy();
                        return true;
                     case 50:
                        TextField field = TextField.this;
                        String clipboardText = TextField.this.clipboard.getContents();
                        field.paste(clipboardText, true);
                        repeat = 1;
                        break;
                     case 52:
                        TextField.this.cut(true);
                        return true;
                     case 54:
                        String oldText = TextField.this.text;
                        TextField.this.setText(TextField.this.undoText);
                        TextField.this.undoText = oldText;
                        TextField.this.updateDisplayText();
                        return true;
                     default:
                        handled = 0;
                  }
               }

               label56:
               if (UIUtils.shift()) {
                  switch (keycode) {
                     case 112:
                        TextField.this.cut(true);
                        break;
                     case 124:
                        TextField field = TextField.this;
                        String clipboardText = TextField.this.clipboard.getContents();
                        field.paste(clipboardText, true);
                  }

                  int oldCursor = TextField.this.cursor;
                  switch (keycode) {
                     case 3:
                        this.goHome((jump != 0));
                        handled = 1;
                        break;
                     case 21:
                        TextField.this.moveCursor(false, (jump != 0));
                        repeat = 1;
                        handled = 1;
                        break;
                     case 22:
                        TextField.this.moveCursor(true, (jump != 0));
                        repeat = 1;
                        handled = 1;
                        break;
                     case 123:
                        this.goEnd((jump != 0));
                        handled = 1;
                        break;
                     default:
                        break label56;
                  }

                  if (!TextField.this.hasSelection) {
                     TextField.this.selectionStart = oldCursor;
                     TextField.this.hasSelection = true;
                  }
               } else {
                  switch (keycode) {
                     case 3:
                        this.goHome((jump != 0));
                        TextField.this.clearSelection();
                        handled = 1;
                        break;
                     case 21:
                        TextField.this.moveCursor(false, (jump != 0));
                        TextField.this.clearSelection();
                        repeat = 1;
                        handled = 1;
                        break;
                     case 22:
                        TextField.this.moveCursor(true, (jump != 0));
                        TextField.this.clearSelection();
                        repeat = 1;
                        handled = 1;
                        break;
                     case 123:
                        this.goEnd((jump != 0));
                        TextField.this.clearSelection();
                        handled = 1;
                  }
               }

               TextField.this.cursor = MathUtils.clamp(TextField.this.cursor, 0, TextField.this.text.length());
               if (repeat != 0) {
                  this.scheduleKeyRepeatTask(keycode);
               }

               return (handled != 0);
            }
         }
      }

      public final void scheduleKeyRepeatTask(int keycode) {
         if (!TextField.this.keyRepeatTask.isScheduled() || TextField.this.keyRepeatTask.keycode != keycode) {
            TextField.this.keyRepeatTask.keycode = keycode;
            TextField.this.keyRepeatTask.cancel();
            Timer.schedule(TextField.this.keyRepeatTask, TextField.keyRepeatInitialTime, TextField.keyRepeatTime);
         }
      }

      @Override
      public boolean keyUp(InputEvent event, int keycode) {
         if (TextField.this.disabled) {
            return false;
         } else {
            TextField.this.keyRepeatTask.cancel();
            return true;
         }
      }

      public boolean checkFocusTraversal(char character) {
         if (TextField.this.focusTraversal) {
            if (character == 9
               || (character == 13 || character == 10) && (UIUtils.isAndroid || UIUtils.isIos)) {
               return true;
            }
         }

         return false;
      }

      @Override
      public boolean keyTyped(InputEvent event, char character) {
         if (TextField.this.disabled) {
            return false;
         } else if (character == 9) {
            TextField.this.next(UIUtils.shift());
            return true;
         } else {
            boolean ctrl = UIUtils.ctrl();
            switch (character) {
               case '\b':
                  if (ctrl && !TextField.this.hasSelection && TextField.this.cursor > 0) {
                     String prevText = TextField.this.text;
                     TextField.this.text = TextField.this.text.substring(TextField.this.cursor);
                     TextField.this.cursor = 0;
                     TextField.this.renderOffset = 0.0F;
                     TextField.this.updateDisplayText();
                     if (TextField.this.changeText(prevText, TextField.this.text)) {
                        long time;
                        if ((time = System.currentTimeMillis()) - 750L > TextField.this.lastChangeTime) {
                           TextField.this.undoText = prevText;
                        }

                        TextField.this.lastChangeTime = time;
                     }

                     return true;
                  }
               case '\n':
               case '\r':
                  break;
               default:
                  if (character < 32) {
                     return false;
                  }
            }

            if (!TextField.this.hasKeyboardFocus()) {
               return false;
            } else if (UIUtils.isMac && Gdx.input.isKeyPressed(63)) {
               return true;
            } else {
               if (this.checkFocusTraversal(character)) {
                  TextField.this.next(UIUtils.shift());
               } else {
                  int flag;
                  label149: {
                     if (character != 13) {
                        if (character != 10) {
                           flag = 0;
                           break label149;
                        }
                     }

                     flag = 1;
                  }

                  int enter = flag;
                  if (character == 127) {
                     flag = 1;
                  } else {
                     flag = 0;
                  }

                  int delete = flag;
                  if (character == 8) {
                     flag = 1;
                  } else {
                     flag = 0;
                  }

                  int backspace = flag;
                  int add = enter != 0
                     ? (TextField.this.writeEnters ? 1 : 0)
                     : (
                        TextField.this.onlyFontChars && !TextField.this.font.getData().hasGlyph(character)
                           ? 0
                           : 1
                     );
                  if (backspace == 0 && delete == 0) {
                     flag = 0;
                  } else {
                     flag = 1;
                  }

                  int remove = flag;
                  if (add != 0 || remove != 0) {
                     TextField.this.cursor = MathUtils.clamp(TextField.this.cursor, 0, TextField.this.text.length());
                     String oldText = TextField.this.text;
                     int oldCursor = TextField.this.cursor;
                     if (remove != 0) {
                        if (TextField.this.hasSelection) {
                           TextField.this.cursor = TextField.this.delete(false);
                        } else {
                           if (backspace != 0 && TextField.this.cursor > 0) {
                              TextField field = TextField.this;
                              String before = TextField.this.text.substring(0, TextField.this.cursor - 1);
                              String currentText = TextField.this.text;
                              int cursorPos = TextField.this.cursor;
                              TextField.this.cursor -= 1;
                              field.text = before + currentText.substring(cursorPos);
                              TextField.this.renderOffset = 0.0F;
                           }

                           if (delete != 0 && TextField.this.cursor < TextField.this.text.length()) {
                              TextField field = TextField.this;
                              String before = TextField.this.text.substring(0, TextField.this.cursor);
                              field.text = before + TextField.this.text.substring(TextField.this.cursor + 1);
                           }
                        }
                     }

                     if (add != 0 && remove == 0) {
                        if (enter == 0 && TextField.this.filter != null && !TextField.this.filter.acceptChar(character)) {
                           return true;
                        }

                        TextField field = TextField.this;
                        int textLength = TextField.this.text.length();
                        int selectionLength;
                        if (TextField.this.hasSelection) {
                           selectionLength = Math.abs(TextField.this.cursor - TextField.this.selectionStart);
                        } else {
                           selectionLength = 0;
                        }

                        if (!field.withinMaxLength(textLength - selectionLength)) {
                           return true;
                        }

                        if (TextField.this.hasSelection) {
                           TextField.this.cursor = TextField.this.delete(false);
                        }

                        String insertion;
                        if (enter != 0) {
                           insertion = "\n";
                        } else {
                           insertion = String.valueOf(character);
                        }

                        String insertText = insertion;
                        TextField target = TextField.this;
                        textLength = TextField.this.cursor;
                        TextField.this.cursor += 1;
                        target.text = TextField.insert(textLength, insertText, TextField.this.text);
                     }

                     if (TextField.this.changeText(oldText, TextField.this.text)) {
                        long time;
                        if ((time = System.currentTimeMillis()) - 750L > TextField.this.lastChangeTime) {
                           TextField.this.undoText = oldText;
                        }

                        TextField.this.lastChangeTime = time;
                        TextField.this.updateDisplayText();
                     } else if (!TextField.this.text.equals(oldText)) {
                        TextField.this.cursor = oldCursor;
                     }
                  }
               }

               if (TextField.this.textFieldListener != null) {
                  TextField.this.textFieldListener.keyTyped(character);
               }

               return true;
            }
         }
      }
   }

   public interface TextFieldFilter {
      boolean acceptChar(char c);

      public static class DigitsOnlyFilter implements TextField.TextFieldFilter {
         @Override
         public final boolean acceptChar(char c) {
            return Character.isDigit(c);
         }
      }
   }

   public interface TextFieldListener {
      void keyTyped(char character);
   }
}
