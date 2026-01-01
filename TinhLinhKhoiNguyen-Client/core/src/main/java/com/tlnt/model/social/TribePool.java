package com.tlnt.model.social;

import com.badlogic.gdx.utils.Pool;

public final class TribePool extends Pool<Tribe> {

   @Override
   public Tribe newObject() {
      return new com.tlnt.model.social.Tribe();
   }
}
