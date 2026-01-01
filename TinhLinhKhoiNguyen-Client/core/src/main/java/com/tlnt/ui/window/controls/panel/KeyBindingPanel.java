package com.tlnt.ui.window.controls.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.GamePanel;
import com.tlnt.ui.window.charinfo.SectionHeaderLabel;

public final class KeyBindingPanel extends GamePanel {
   public Table bindingsTable;

   public KeyBindingPanel(RenderContext ctx) {
      super(ctx);
      this.add(new SectionHeaderLabel("Thiết lập Phím tắt bàn phím", super.renderContext)).height(34.0F).growX().space(20.0F).row();
      this.bindingsTable = new Table().top();
      ScrollPane scrollPane = ScrollPaneFactory.createScrollPane(this.bindingsTable);
      this.add(scrollPane).grow();
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow("Di chuyển lên", 0, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      String actionName = "Di chuyển sang trái";
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow(actionName, 1, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      this.bindingsTable.row();
      actionName = "Di chuyển xuống";
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow(actionName, 2, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      actionName = "Di chuyển sang phải";
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow(actionName, 3, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      this.bindingsTable.row();
      actionName = "Ô đánh số 1";
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow(actionName, 4, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow("Đánh cơ bản", 5, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      this.bindingsTable.row();
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow("Ô đánh số 2", 6, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      actionName = "Đổi mục tiêu";
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow(actionName, 7, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      this.bindingsTable.row();
      actionName = "Ô đánh số 3";
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow(actionName, 8, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow("Bật/Tắt Tự động đánh", 9, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      this.bindingsTable.row();
      actionName = "Ô đánh số 4";
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow(actionName, 10, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow("Chat", 11, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      this.bindingsTable.row();
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow("Ô đánh số 5", 12, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      actionName = "Sử dụng vật phẩm hồi HP";
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow(actionName, 13, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      this.bindingsTable.row();
      actionName = "Ô đánh số 6";
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow(actionName, 14, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
      this.bindingsTable
         .add(new KeyBindingPanel.KeyBindingRow("Sử dụng vật phẩm hồi MP", 15, super.renderContext))
         .size(495.0F, 60.0F)
         .space(10.0F)
         .spaceLeft(157.0F)
         .spaceRight(157.0F);
   }

   public class KeyBindingRow extends BaseTable {
      public String actionName;
      public KeyBindingButton keyBindButton;
      public int keyBindIndex;

      public KeyBindingRow(String actionName, int keyBindIndex, RenderContext ctx) {
         super(ctx);
         this.actionName = actionName;
         this.keyBindIndex = keyBindIndex;
         this.keyBindButton = new KeyBindingButton(this, "", super.renderContext);
         this.add().grow();
         this.add(this.keyBindButton).size(165.0F, 50.0F);
         this.keyBindButton.addListener(new KeyCaptureStartListener(this));
      }

      @Override
      public final void draw(Batch batch, float parentAlpha) {
         RenderContext ctx = super.renderContext;
         BitmapFont font = FontManager.PLAIN_FONT;
         String text = this.actionName;
         float textX = this.getX();
         float textY = this.getY() + this.getHeight() / 2.0F;
         ctx.drawTextColorName(font, text, textX, textY, Anchor.MIDDLE_LEFT, 24.0F, "#8B4E23");
         super.draw(batch, parentAlpha);
      }
   }
}
