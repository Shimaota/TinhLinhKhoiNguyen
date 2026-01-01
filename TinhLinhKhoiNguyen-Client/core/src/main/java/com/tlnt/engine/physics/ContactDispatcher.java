package com.tlnt.engine.physics;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public final class ContactDispatcher implements ContactListener {
   public final PhysicsWorld world;
   public final Array<ContactDispatcher.ContactPair> activeContacts = new Array<>();
   public final Pool<ContactDispatcher.ContactPair> pairPool = new ContactPairPool(this);

   public ContactDispatcher(PhysicsWorld world) {
      this.world = world;
   }

   @Override
   public final void beginContact(Contact contact) {
      Component componentA = this.world.getBodyComponent(contact.getFixtureA().getBody());
      Component componentB = this.world.getBodyComponent(contact.getFixtureB().getBody());
      if (componentA != null) {
         Array componentsA = this.world.entityComponentsMap.get(componentA.getEntity());

         for (int i = 0; i < componentsA.size; i++) {
            Component component = (Component)componentsA.get(i);
            contact.getFixtureB().getUserData();
         }
      }

      if (componentB != null) {
         Array componentsB = this.world.entityComponentsMap.get(componentB.getEntity());

         for (int j = 0; j < componentsB.size; j++) {
            Component other = (Component)componentsB.get(j);
            contact.getFixtureA().getUserData();
         }
      }

      if (componentA != null && componentB != null) {
         this.activeContacts.add(this.pairPool.obtain().setBodies((BodyComponent)componentA, (BodyComponent)componentB));
      }
   }

   @Override
   public final void endContact(Contact contact) {
      Component componentA = this.world.getBodyComponent(contact.getFixtureA().getBody());
      Component componentB = this.world.getBodyComponent(contact.getFixtureB().getBody());
      Array components;
      if (componentA != null && (components = this.world.entityComponentsMap.get(componentA.getEntity())) != null) {

         for (int i = 0; i < components.size; i++) {
            ((Component)components.get(i)).onContactEnd(componentB, contact, contact.getFixtureB().getUserData());
         }
      }

      if (componentB != null && (components = this.world.entityComponentsMap.get(componentB.getEntity())) != null) {
         for (int j = 0; j < components.size; j++) {
            ((Component)components.get(j)).onContactEnd(componentA, contact, contact.getFixtureA().getUserData());
         }
      }

      for (int pairIndex = this.activeContacts.size - 1; pairIndex >= 0; pairIndex--) {
         if (this.activeContacts.get(pairIndex).matches((BodyComponent)componentA, (BodyComponent)componentB)) {
            this.pairPool.free(this.activeContacts.removeIndex(pairIndex));
            return;
         }
      }
   }

   public final void dispatchBodyContacts() {

      for (int i = 0; i < this.activeContacts.size; i++) {
         ContactDispatcher.ContactPair pair = this.activeContacts.get(i);
         Array componentsA = this.world.entityComponentsMap.get(pair.getBodyA().getEntity());

         for (int j = 0; j < componentsA.size; j++) {
            ((Component)componentsA.get(j)).onBodyContact(pair.getBodyB());
         }

         Array componentsB = this.world.entityComponentsMap.get(pair.getBodyB().getEntity());

         for (int k = 0; k < componentsB.size; k++) {
            ((Component)componentsB.get(k)).onBodyContact(pair.getBodyA());
         }
      }
   }

   @Override
   public final void preSolve(Contact contact, Manifold oldManifold) {
      Component componentA = this.world.getBodyComponent(contact.getFixtureA().getBody());
      Component componentB = this.world.getBodyComponent(contact.getFixtureB().getBody());
      if (componentA != null) {
         Array componentsA = this.world.entityComponentsMap.get(componentA.getEntity());
         int i = 0;

         while (i < componentsA.size && !((Component)componentsA.get(i)).onContactPreSolve(componentB, contact, oldManifold, contact.getFixtureB().getUserData())) {
            i++;
         }
      }

      if (componentB != null) {
         Array componentsB = this.world.entityComponentsMap.get(componentB.getEntity());
         int j = 0;

         while (j < componentsB.size && !((Component)componentsB.get(j)).onContactPreSolve(componentA, contact, oldManifold, contact.getFixtureA().getUserData())) {
            j++;
         }
      }
   }

   @Override
   public final void postSolve(Contact contact, ContactImpulse impulse) {
      Component componentA = this.world.getBodyComponent(contact.getFixtureA().getBody());
      Component componentB = this.world.getBodyComponent(contact.getFixtureB().getBody());
      if (componentA != null) {
         Array componentsA = this.world.entityComponentsMap.get(componentA.getEntity());
         int i = 0;

         while (i < componentsA.size && !((Component)componentsA.get(i)).onContactPostSolve(componentB, contact, impulse)) {
            i++;
         }
      }

      if (componentB != null) {
         Array componentsB = this.world.entityComponentsMap.get(componentB.getEntity());
         int j = 0;

         while (j < componentsB.size && !((Component)componentsB.get(j)).onContactPostSolve(componentA, contact, impulse)) {
            j++;
         }
      }
   }

   public final void removeContactsForBody(BodyComponent bodyComponent) {
      for (int i = this.activeContacts.size - 1; i >= 0; i--) {
         if (this.activeContacts.get(i).involves(bodyComponent)) {
            this.pairPool.free(this.activeContacts.removeIndex(i));
         }
      }
   }

   public static class ContactPair {
      public BodyComponent bodyA;
      public BodyComponent bodyB;

      public final BodyComponent getBodyA() {
         return this.bodyA;
      }

      public final BodyComponent getBodyB() {
         return this.bodyB;
      }

      public final ContactDispatcher.ContactPair setBodies(BodyComponent bodyA, BodyComponent bodyB) {
         this.bodyA = bodyA;
         this.bodyB = bodyB;
         return this;
      }

      public final boolean matches(BodyComponent bodyA, BodyComponent bodyB) {
         if (this.bodyA != bodyA || this.bodyB != bodyB) {
            int swappedA;
            if (this.bodyA == bodyB) {
               swappedA = 1;
            } else {
               swappedA = 0;
            }

            int swappedB;
            if (this.bodyB == bodyA) {
               swappedB = 1;
            } else {
               swappedB = 0;
            }

            if ((swappedA & swappedB) == 0) {
               return false;
            }
         }

         return true;
      }

      public final boolean involves(BodyComponent body) {
         return ((this.bodyA != body && this.bodyB != body ? 0 : 1) != 0);
      }
   }
}
