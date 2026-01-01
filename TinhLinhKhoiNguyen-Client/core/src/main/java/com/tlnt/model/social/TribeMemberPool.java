package com.tlnt.model.social;

import com.badlogic.gdx.utils.Pool;

public final class TribeMemberPool extends Pool<TribeMember> {

   @Override
   public TribeMember newObject() {
      return new com.tlnt.model.social.TribeMember();
   }
}
