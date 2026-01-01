package com.tlnt.ui.window.iteminfo.effect;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.Item;
import com.tlnt.model.item.StatLine;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ImageActor;

public final class ItemEffectListRow extends BaseTable {
   public Pool<TextraLabel> labelPool = new MultiEffectLabelPool(this);
   public ImageActor itemIcon = new ImageActor(this.renderContext);
   public Table effectsTable;

   public ItemEffectListRow(RenderContext renderContext) {
      super(renderContext);
      this.add(this.itemIcon).size(40.0F, 40.0F).top().left().padTop(10.0F).space(10.0F);
      this.effectsTable = new Table().top().left();
      this.add(this.effectsTable).grow();
   }

   public final ItemEffectListRow bindItem(Item item) {
      this.itemIcon.setTexture(TextureAsset.obtain(item.template.iconPath));
      Array.ArrayIterator iter = this.effectsTable.getChildren().iterator();

      while (iter.hasNext()) {
         Actor child;
         if ((child = (Actor)iter.next()) instanceof TextraLabel) {
            this.labelPool.free((TextraLabel)child);
         }
      }

      this.effectsTable.clear();
      StatLine[] statLines = item.statLines;
      int count = item.statLines.length;

      for (int i = 0; i < count; i++) {
         StatLine statLine = statLines[i];
         TextraLabel label;
         (label = this.labelPool.obtain()).setText("[#8B4E23]" + statLine.formatText());
         this.effectsTable.add(label).growX().space(5.0F).row();
      }

      return this;
   }
}
