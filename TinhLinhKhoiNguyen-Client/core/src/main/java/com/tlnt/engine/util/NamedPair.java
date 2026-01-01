package com.tlnt.engine.util;

public final class NamedPair<A, B> {
   public String name;
   public A first;
   public B second;

   public NamedPair(String name) {
      this.name = name;
   }
}
