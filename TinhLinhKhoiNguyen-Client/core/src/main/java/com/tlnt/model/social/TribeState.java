package com.tlnt.model.social;

import com.badlogic.gdx.utils.Array;
import com.tlnt.model.item.Item;

public final class TribeState {
   public Tribe myTribe;
   public Array<String> notices = new Array<>();
   public Array<Item> warehouseItems = new Array<>();
   public Array<TribeSkill> skills = new Array<>();
   public Array<Tribe> tribeList = new Array<>();
}
