package com.tlnt.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;

/**
 * Dropdown select — port of the decompiled {@code com.puzzle.core.game.ui.components.C3275}: one button shows the selected item; clicking it
 * opens a list of buttons right BELOW the box (a transparent full-screen overlay, clicking outside = close), picking an item -> calls the listener.
 */
public class DropdownSelect<T> extends Group {

   public interface SelectionListener<T> {
      void onSelected(T item);
   }

   private final RenderContext renderContext;
   private final OptionButton headerButton;
   private T[] items;
   private int selectedIndex = -1;
   private Group popup;
   private SelectionListener<T> listener;

   public DropdownSelect(RenderContext renderContext) {
      this.renderContext = renderContext;
      this.headerButton = new OptionButton("...", renderContext);
      this.headerButton.addListener(new ClickListener() {
         @Override
         public void clicked(InputEvent event, float x, float y) {
            DropdownSelect.this.togglePopup();
         }
      });
      this.addActor(this.headerButton);
   }

   public final void setSelectionListener(SelectionListener<T> listener) {
      this.listener = listener;
   }

   @SafeVarargs
   public final void setItems(T... items) {
      this.items = items;
      if (items.length > 0 && this.selectedIndex == -1) {
         this.setSelectedIndex(0);
      }
   }

   public final T getSelected() {
      return this.items == null || this.selectedIndex < 0 || this.selectedIndex >= this.items.length ? null : this.items[this.selectedIndex];
   }

   public final void setSelected(T item) {
      if (this.items == null) {
         return;
      }

      for (int i = 0; i < this.items.length; i++) {
         if (this.items[i].equals(item)) {
            this.setSelectedIndex(i);
            return;
         }
      }
   }

   public final void setSelectedIndex(int index) {
      if (this.items != null && index >= 0 && index < this.items.length) {
         this.selectedIndex = index;
         this.headerButton.setText(this.items[index].toString());
      }
   }

   void togglePopup() {
      if (this.popup != null && this.popup.getParent() != null) {
         this.closePopup();
         return;
      }

      Stage stage = this.getStage();
      if (stage == null || this.items == null) {
         return;
      }

      this.popup = new Group();
      Vector2 origin = this.localToStageCoordinates(new Vector2(0.0F, 0.0F));
      Table table = new Table();
      table.top().left();

      for (int i = 0; i < this.items.length; i++) {
         final int index = i;
         OptionButton option = new OptionButton(this.items[i].toString(), this.renderContext);
         option.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
               DropdownSelect.this.setSelectedIndex(index);
               if (DropdownSelect.this.listener != null) {
                  DropdownSelect.this.listener.onSelected(DropdownSelect.this.items[index]);
               }

               DropdownSelect.this.closePopup();
            }
         });
         table.add(option).width(this.getWidth()).height(63.0F).row();
      }

      table.pack();
      table.setPosition(origin.x, origin.y - table.getHeight());
      Button outside = new Button(new Button.ButtonStyle());
      outside.setSize(stage.getWidth(), stage.getHeight());
      outside.addListener(new ClickListener() {
         @Override
         public void clicked(InputEvent event, float x, float y) {
            DropdownSelect.this.closePopup();
         }
      });
      this.popup.addActor(outside);
      this.popup.addActor(table);
      stage.addActor(this.popup);
   }

   final void closePopup() {
      if (this.popup != null) {
         this.popup.remove();
         this.popup = null;
      }
   }

   @Override
   public void setSize(float width, float height) {
      super.setSize(width, height);
      this.headerButton.setSize(width, height);
   }

   @Override
   protected void setStage(Stage stage) {
      if (stage == null) {
         this.closePopup(); // the window holding the box was closed -> remove the open list
      }

      super.setStage(stage);
   }

   /** ui/10-12 button with white text (like the C3275 inner class). */
   static final class OptionButton extends TextButton {
      private final CompositeSprite sprite = CompositeSprite.obtain("ui/10", "ui/11", "ui/12", false);

      OptionButton(String text, RenderContext ctx) {
         super(text, ctx);
      }

      @Override
      public void drawBackground() {
         if (this.isPressed() || this.isOver()) {
            super.renderContext.setBatchColor(Color.GRAY);
         }

         super.renderContext.drawCompositeSprite(this.sprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
         super.renderContext.resetBatchColor();
      }

      @Override
      public void drawLabel() {
         super.renderContext
            .drawText(FontManager.DEFAULT_FONT, super.text, this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 1.8F, Anchor.CENTER, 24.0F, Color.WHITE);
      }
   }
}
