package com.tlnt.model.guide;

import com.badlogic.gdx.utils.Pool;

public final class GuideTopicPool extends Pool<GuideTopic> {

   @Override
   public GuideTopic newObject() {
      return new com.tlnt.model.guide.GuideTopic();
   }
}
