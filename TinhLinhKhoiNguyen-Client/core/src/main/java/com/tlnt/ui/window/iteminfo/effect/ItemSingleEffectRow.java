package com.tlnt.ui.window.iteminfo.effect;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.ElementStone;
import com.tlnt.ui.widget.BaseTable;
import com.tlnt.ui.widget.ImageActor;

public final class ItemSingleEffectRow extends BaseTable {
   public Pool<TextraLabel> labelPool = new SingleEffectLabelPool(this);
   public ImageActor itemIcon = new ImageActor(this.renderContext);
   public Table effectTable;

   public ItemSingleEffectRow(RenderContext renderContext) {
      super(renderContext);
      this.add(this.itemIcon).size(40.0F, 40.0F).top().left().padTop(10.0F).space(10.0F);
      this.effectTable = new Table().top().left();
      this.add(this.effectTable).grow();
   }

   public final ItemSingleEffectRow bindComponent(ElementStone stone) {
      this.itemIcon.setTexture(TextureAsset.obtain(stone.stoneTemplate.iconPath));
      Array.ArrayIterator iter = this.effectTable.getChildren().iterator();

      while (iter.hasNext()) {
         Actor child;
         if ((child = (Actor)iter.next()) instanceof TextraLabel) {
            this.labelPool.free((TextraLabel)child);
         }
      }

      this.effectTable.clear();
      TextraLabel label;
      (label = this.labelPool.obtain()).setText("[#8B4E23]" + stone.statLine.formatText());
      this.effectTable.add(label).growX().space(5.0F).row();
      return this;
   }
}
