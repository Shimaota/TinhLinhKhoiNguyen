package com.tlnt.model.trade;

import com.badlogic.gdx.utils.Array;
import com.tlnt.model.character.CharacterTemplate;
import com.tlnt.model.item.Item;

public final class TradeState {
   public Array<Item> myOfferItems = new Array<>();
   public int myOfferCoins;
   public boolean myOfferLocked;
   public boolean myAccepted;
   public String partnerName = "";
   public CharacterTemplate partnerClass;
   public int partnerLevel;
   public Array<Item> partnerOfferItems = new Array<>();
   public int partnerOfferCoins;
   public boolean partnerOfferLocked;
   public boolean partnerAccepted;
}
