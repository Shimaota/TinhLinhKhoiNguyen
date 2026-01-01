package com.tlnt.model.consignment;

import com.badlogic.gdx.utils.Pool;

public final class ConsignmentCategoryPool extends Pool<ConsignmentCategory> {

   @Override
   public ConsignmentCategory newObject() {
      return new com.tlnt.model.consignment.ConsignmentCategory();
   }
}
