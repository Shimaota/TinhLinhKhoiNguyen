package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.GameState;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.model.party.PartyBuffEntry;
import com.tlnt.ui.window.GamePanel;

public final class PartyBuffInfoPanel extends GamePanel {
   public Pool<TextraLabel> labelPool = new BuffLabelPool(this);
   public NinePatchAsset background;
   public TextraLabel titleLabel;
   public TextraLabel descriptionLabel;
   public Table buffListTable;

   public PartyBuffInfoPanel(RenderContext renderContext) {
      super(renderContext);
      this.background = NinePatchAsset.obtain("ui/100", 50);
      String labelText = "[yellow]Ngũ linh liên vũ";
      this.add(this.titleLabel = new TextraLabel(labelText, FontFactory.getCachedTextraFont(FontManager.DEFAULT_FONT, 30)))
         .height(40.0F)
         .growX()
         .space(10.0F)
         .row();
      this.titleLabel.setAlignment(2);
      labelText = "Các thành viên trong tổi đội sẽ chia sẻ kinh nghiệm và vật phẩm nhận được khi đánh quái đồng thời kích hoạt hiệu ứng Ngũ linh Liên vũ khi có đầy đủ các thành viên sở hữu 5 loại vũ khí khác nhau trong tổ đội";
      this.add(
            this.descriptionLabel = new TextraLabel(
               labelText, FontFactory.createTextraFontWithLineHeight(FontManager.DEFAULT_FONT, 22, 2.5F)
            )
         )
         .growX()
         .padLeft(16.0F)
         .padRight(16.0F)
         .space(10.0F)
         .row();
      this.descriptionLabel.setAlignment(10);
      this.descriptionLabel.setWrap(true);
      this.add(this.buffListTable = new Table().top().left()).growX().padLeft(30.0F).padRight(30.0F).row();
   }

   @Override
   public final void refresh() {
      Array.ArrayIterator it = this.buffListTable.getChildren().iterator();

      while (it.hasNext()) {
         Actor child;
         if ((child = (Actor)it.next()) instanceof TextraLabel) {
            this.labelPool.free((TextraLabel)child);
         }
      }

      this.buffListTable.clear();
      if (GameState.currentParty != null) {
         for (PartyBuffEntry buff : GameState.currentParty.buffs) {
            TextraLabel label;
            TextraLabel pooledLabel = label = this.labelPool.obtain();
            BitmapFont font = buff.active ? FontManager.DEFAULT_FONT : FontManager.PLAIN_FONT;
            pooledLabel.setFont(FontFactory.createTextraFontWithLineHeight(font, 22, 3.0F));
            label.setText(buff.text);
            this.buffListTable.add(label).growX().row();
         }
      }
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.background, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
