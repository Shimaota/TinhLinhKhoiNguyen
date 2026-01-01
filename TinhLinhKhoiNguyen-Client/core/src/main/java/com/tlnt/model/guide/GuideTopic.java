package com.tlnt.model.guide;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.tlnt.model.item.Item;

public final class GuideTopic {
   public GuideCategory category;
   public int id;
   public String name;
   public Item[] items;
   public String layoutJson;
   public Table contentTable;

   static {
      new GuideTopicPool();
   }
}
