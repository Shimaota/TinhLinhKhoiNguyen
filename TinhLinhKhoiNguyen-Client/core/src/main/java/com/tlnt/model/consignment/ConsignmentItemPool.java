package com.tlnt.model.consignment;

import com.badlogic.gdx.utils.Pool;

public final class ConsignmentItemPool extends Pool<ConsignmentItem> {

   @Override
   public ConsignmentItem newObject() {
      return new com.tlnt.model.consignment.ConsignmentItem();
   }
}
