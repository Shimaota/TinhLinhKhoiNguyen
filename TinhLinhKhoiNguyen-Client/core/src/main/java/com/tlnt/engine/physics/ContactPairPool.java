package com.tlnt.engine.physics;

import com.badlogic.gdx.utils.Pool;

public final class ContactPairPool extends Pool<ContactDispatcher.ContactPair> {
   public com.tlnt.engine.physics.ContactDispatcher dispatcher;
   public ContactPairPool(ContactDispatcher dispatcher) {
      super();
      this.dispatcher = dispatcher;
   }

   @Override
   public ContactDispatcher.ContactPair newObject() {
      return new com.tlnt.engine.physics.ContactDispatcher.ContactPair();
   }
}
