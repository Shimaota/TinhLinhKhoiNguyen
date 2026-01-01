package com.tlnt.engine.pool;

import com.badlogic.gdx.utils.Array;

public abstract class ObjectPool<T> {
   public int cacheEntry;
   public int peak;
   public final Array<T> freeObjects;

   public ObjectPool() {
      this(16, 2147483647);
   }

   public ObjectPool(int initialCapacity, int max) {
      this.freeObjects = new Array<>(false, 16);
      this.cacheEntry = 2147483647;
   }

   public abstract T newObject();

   public T obtain() {
      return this.freeObjects.size == 0 ? this.newObject() : this.freeObjects.pop();
   }

   public void free(T obj) {
      if (obj == null) {
         throw new IllegalArgumentException("object cannot be null.");
      } else {
         if (this.freeObjects.size < this.cacheEntry) {
            if (!this.freeObjects.contains((T)obj, true)) {
               this.freeObjects.add((T)obj);
               this.peak = Math.max(this.peak, this.freeObjects.size);
               resetObject(obj);
               return;
            }
         } else {
            resetObject(obj);
         }
      }
   }

   public static void resetObject(Object obj) {
      if (obj instanceof ObjectPool.Poolable) {
         ((ObjectPool.Poolable)obj).reset();
      }
   }

   public interface Poolable {
      void reset();
   }
}
