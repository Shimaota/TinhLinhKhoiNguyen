package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class AutoApproveToggle extends ShapeButton {
   public com.tlnt.ui.window.party.panel.PartyMembersPanel partyMembersPanel;

   public AutoApproveToggle(PartyMembersPanel partyMembersPanel, RenderContext renderContext) {
      super(renderContext);
      this.partyMembersPanel = partyMembersPanel;
   }

   @Override
   public final void drawBackground() {
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/63"),
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            Anchor.BOTTOM_LEFT
         );
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      if (this.isChecked()) {
         super.renderContext
            .drawTextureSized(
               TextureAsset.obtain("ui/64"),
               this.getX() + this.getWidth() / 2.0F,
               this.getY() + this.getHeight() / 2.0F,
               40.0F,
               40.0F,
               Anchor.CENTER
            );
      }

      super.renderContext.resetBatchColor();
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Tự động duyệt thành viên tổ đội",
            this.getX() + this.getWidth() + 12.0F,
            this.getY() + this.getHeight() / 2.0F,
            Anchor.MIDDLE_LEFT,
            22.0F,
            Color.WHITE
         );
   }

}
