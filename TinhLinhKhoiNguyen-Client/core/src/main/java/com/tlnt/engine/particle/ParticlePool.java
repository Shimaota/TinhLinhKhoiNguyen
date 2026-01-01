package com.tlnt.engine.particle;

import com.badlogic.gdx.utils.Pool;

public final class ParticlePool extends Pool<Particle> {
   public com.tlnt.engine.particle.ParticleEmitter emitter;
   public ParticlePool(ParticleEmitter emitter) {
      super();
      this.emitter = emitter;
   }

   @Override
   public Particle newObject() {
      return new com.tlnt.engine.particle.Particle(this.emitter);
   }
}
