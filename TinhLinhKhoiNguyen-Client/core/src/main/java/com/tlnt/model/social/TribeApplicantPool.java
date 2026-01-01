package com.tlnt.model.social;

import com.badlogic.gdx.utils.Pool;

public final class TribeApplicantPool extends Pool<TribeApplicant> {

   @Override
   public TribeApplicant newObject() {
      return new com.tlnt.model.social.TribeApplicant();
   }
}
