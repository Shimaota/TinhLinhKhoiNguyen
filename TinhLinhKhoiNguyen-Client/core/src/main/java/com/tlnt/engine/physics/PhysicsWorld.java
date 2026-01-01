package com.tlnt.engine.physics;

import box2dLight.RayHandler;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.OrderedMap;
import com.tlnt.engine.physics.component.ComponentState;
import com.tlnt.engine.physics.options.Box2dPhysicsOptions;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.scene.GameObject;
import java.util.Comparator;

public final class PhysicsWorld implements Disposable {
   public final Box2dPhysicsOptions options = new Box2dPhysicsOptions();
   public final World box2dWorld;
   public final RayHandler rayHandler;
   public boolean paused;
   public final OrderedMap<Body, BodyComponent> bodyComponentMap = new OrderedMap<>();
   public final ContactDispatcher contactDispatcher;
   public final OrderedMap<Entity, Array<Component>> entityComponentsMap = new OrderedMap<>();
   public Array<Component> sortedRenderComponents = new Array<>();
   public boolean renderOrderDirty;
   public final Array<Entity> pendingAddEntities = new Array<>();
   public final Array<Entity> pendingRemoveEntities = new Array<>();
   public final Array<Component> pendingAddComponents = new Array<>();
   public final Array<Component> pendingRemoveComponents = new Array<>();
   public final Comparator<Component> updateOrderComparator = new ComponentOrderComparator(this);
   public final Comparator<Component> renderOrderComparator = new ComponentRenderComparator(this);
   public float accumulator = 0.0F;
   public final Array<Body> bodiesBuffer = new Array<>();

   public final World getBox2dWorld() {
      return this.box2dWorld;
   }

   public final RayHandler getRayHandler() {
      return this.rayHandler;
   }

   public PhysicsWorld() {
      this(new World(new Vector2(0.0F, 0.0F), true));
   }

   public PhysicsWorld(World world) {
      this.box2dWorld = world;
      this.rayHandler = new RayHandler(this.box2dWorld);
      this.rayHandler.setAmbientLight(new Color(0.0F, 0.0F, 0.0F, 0.2F));
      this.rayHandler.setBlurNum(55);
      world.setContactListener(this.contactDispatcher = new ContactDispatcher(this));
   }

   public final void update(float delta) {
      PhysicsWorld self = this;

      for (int i = 0; i < self.entityComponentsMap.orderedKeys().size; i++) {
         Entity entity;
         if ((entity = self.entityComponentsMap.orderedKeys().get(i)).isActive()) {
            Array components = self.entityComponentsMap.get(entity);

            for (int j = 0; j < components.size; j++) {
               Component component;
               if ((component = (Component)components.get(j)).getState() == ComponentState.ADDED) {
                  component.onStart();
                  component.setState(ComponentState.ACTIVE);
               }
            }
         }
      }

      self = this;

      for (int sortIndex = 0; sortIndex < self.entityComponentsMap.orderedKeys().size; sortIndex++) {
         Entity dirtyEntity;
         if ((dirtyEntity = self.entityComponentsMap.orderedKeys().get(sortIndex)).isComponentsDirty()) {
            self.entityComponentsMap.get(dirtyEntity).sort(self.updateOrderComparator);
         }
      }

      if (!this.paused) {
         this.box2dWorld.step(Math.min(0.033333335F, delta), this.options.getVelocityIterations(), this.options.getPositionIterations());
         self = this;
         float frameTime = Math.min(delta, this.options.getMaxFixedFrameTime());
         this.accumulator += frameTime;

         while (self.accumulator >= self.options.getTimeStep()) {
            float timeStep = self.options.getTimeStep();
            PhysicsWorld stepWorld = self;

            for (int stepEntityIndex = 0; stepEntityIndex < stepWorld.entityComponentsMap.orderedKeys().size; stepEntityIndex++) {
               Entity stepEntity;
               if ((stepEntity = stepWorld.entityComponentsMap.orderedKeys().get(stepEntityIndex)).isActive()) {
                  Array stepComponents = stepWorld.entityComponentsMap.get(stepEntity);

                  for (int stepComponentIndex = 0; stepComponentIndex < stepComponents.size; stepComponentIndex++) {
                     ((Component)stepComponents.get(stepComponentIndex)).isEnabled();
                  }
               }
            }

            self.contactDispatcher.dispatchBodyContacts();
            self.accumulator -= timeStep;
         }

         if (self.options.isInterpolateMovement()) {
            float remaining = self.accumulator;
            PhysicsWorld interpWorld = self;
            float alpha = remaining / self.options.getTimeStep();
            self.box2dWorld.getBodies(self.bodiesBuffer);

            for (int bodyIndex = 0; bodyIndex < interpWorld.bodiesBuffer.size; bodyIndex++) {
               Body body;
               if ((body = interpWorld.bodiesBuffer.get(bodyIndex)).getType() != BodyDef.BodyType.StaticBody) {
                  Vector2 pos = body.getPosition();
                  float bodyAngle = body.getAngle();
                  float interpX = pos.x * (1.0F - alpha) + pos.x * alpha;
                  frameTime = pos.y * (1.0F - alpha) + pos.y * alpha;
                  float interpAngle = bodyAngle * (1.0F - alpha) + bodyAngle * alpha;
                  body.setTransform(interpX, frameTime, interpAngle);
               }
            }
         }

         float updateDelta = delta;
         self = this;

         for (int updateIndex = 0; updateIndex < self.entityComponentsMap.orderedKeys().size; updateIndex++) {
            Entity updateEntity;
            if ((updateEntity = self.entityComponentsMap.orderedKeys().get(updateIndex)).isActive()) {
               updateEntity.updateVelocity(updateDelta);
               Array updateComponents = self.entityComponentsMap.get(updateEntity);

               for (int updateComponentIndex = 0; updateComponentIndex < updateComponents.size; updateComponentIndex++) {
                  Component updateComponent;
                  GameObject gameObject;
                  if ((updateComponent = (Component)updateComponents.get(updateComponentIndex)).isEnabled() && ((gameObject = updateComponent.getEntity().getGameObject()) == null || !gameObject.isDisposed())) {
                     updateComponent.update(updateDelta);
                  }
               }
            }
         }

         updateDelta = delta;
         self = this;

         for (int lateIndex = 0; lateIndex < self.entityComponentsMap.orderedKeys().size; lateIndex++) {
            Entity lateEntity;
            if ((lateEntity = self.entityComponentsMap.orderedKeys().get(lateIndex)).isActive()) {
               Array lateComponents = self.entityComponentsMap.get(lateEntity);

               for (int lateComponentIndex = 0; lateComponentIndex < lateComponents.size; lateComponentIndex++) {
                  Component lateComponent;
                  if ((lateComponent = (Component)lateComponents.get(lateComponentIndex)).isEnabled()) {
                     lateComponent.lateUpdate(updateDelta);
                  }
               }
            }
         }
      }

      self = this;
      if (this.renderOrderDirty) {
         this.renderOrderDirty = false;
         this.sortedRenderComponents.clear();

         for (int renderIndex = 0; renderIndex < self.entityComponentsMap.orderedKeys().size; renderIndex++) {
            Entity renderEntity = self.entityComponentsMap.orderedKeys().get(renderIndex);
            Array renderComponents = self.entityComponentsMap.get(renderEntity);

            for (int renderComponentIndex = 0; renderComponentIndex < renderComponents.size; renderComponentIndex++) {
               Component renderComponent = (Component)renderComponents.get(renderComponentIndex);
               self.sortedRenderComponents.add(renderComponent);
            }
         }

         self.sortedRenderComponents.sort(self.renderOrderComparator);
      }
   }

   public final void render(RenderContext ctx) {
      RenderContext renderCtx = ctx;
      PhysicsWorld self = this;

      for (int i = 0; i < self.sortedRenderComponents.size; i++) {
         Component component;
         GameObject gameObject;
         if ((component = self.sortedRenderComponents.get(i)).getEntity().isActive()
            && component.isEnabled()
            && component.isVisible()
            && ((gameObject = component.getEntity().getGameObject()) == null || !gameObject.isDisposed())) {
            component.render(renderCtx);
         }
      }
   }

   public final void renderDebug(RenderContext ctx) {
      RenderContext renderCtx = ctx;
      PhysicsWorld self = this;

      for (int i = 0; i < self.sortedRenderComponents.size; i++) {
         Component component;
         if ((component = self.sortedRenderComponents.get(i)).getEntity().isActive() && component.isDebugEnabled() && component.isEnabled()) {
            component.renderDebug(renderCtx);
         }
      }
   }

   public final void processQueues() {
      PhysicsWorld self = this;

      for (int addIndex = 0; addIndex < self.pendingAddEntities.size; addIndex++) {
         Entity addedEntity = self.pendingAddEntities.get(addIndex);
         self.entityComponentsMap.put(addedEntity, new Array<>());
         addedEntity.setEntityStateId(EntityState.ADDED);
         self.renderOrderDirty = true;
      }

      self.pendingAddEntities.clear();
      self = this;

      for (int addComponentIndex = 0; addComponentIndex < self.pendingAddComponents.size; addComponentIndex++) {
         Component addedComponent = self.pendingAddComponents.get(addComponentIndex);
         self.entityComponentsMap.get(addedComponent.getEntity()).add(addedComponent);
         addedComponent.onAdded();
         addedComponent.setState(ComponentState.ADDED);
         if (addedComponent.getEntity().isActive()) {
            addedComponent.onEnable();
         }

         self.renderOrderDirty = true;
         addedComponent.getEntity().markComponentsDirty();
      }

      self.pendingAddComponents.clear();
      self = this;

      for (int removeEntityIndex = 0; removeEntityIndex < self.pendingRemoveEntities.size; removeEntityIndex++) {
         Entity removedEntity = self.pendingRemoveEntities.get(removeEntityIndex);
         Array entityComponents = self.entityComponentsMap.get(removedEntity);

         for (int i = 0; i < entityComponents.size; i++) {
            self.removeComponent((Component)entityComponents.get(i));
         }

         removedEntity.setEntityStateId(EntityState.REMOVED);
         self.renderOrderDirty = true;
      }

      self.pendingRemoveEntities.clear();
      self = this;

      for (int removeComponentIndex = 0; removeComponentIndex < self.pendingRemoveComponents.size; removeComponentIndex++) {
         Component removedComponent;
         if ((removedComponent = self.pendingRemoveComponents.get(removeComponentIndex)).getEntity().isActive()) {
            removedComponent.onDisable();
         }

         removedComponent.onDestroy();
         removedComponent.setState(ComponentState.REMOVED);
         self.renderOrderDirty = true;
         removedComponent.getEntity().markComponentsDirty();
      }

      self.pendingRemoveComponents.clear();

      for (int entityIndex = this.entityComponentsMap.orderedKeys().size - 1; entityIndex >= 0; entityIndex--) {
         Entity entity = this.entityComponentsMap.orderedKeys().get(entityIndex);

         Array components;
         for (int componentIndex = (components = this.entityComponentsMap.get(entity)).size - 1; componentIndex >= 0; componentIndex--) {
            if (((Component)components.get(componentIndex)).getState() == ComponentState.REMOVED) {
               components.removeIndex(componentIndex);
            }
         }

         if (entity.getEntityStateId() == EntityState.REMOVED) {
            this.entityComponentsMap.removeIndex(entityIndex);
         }
      }
   }

   public final void addEntity(Entity entity) {
      entity.setEntityStateId(EntityState.PENDING_ADD);
      this.pendingAddEntities.add(entity);
   }

   public final void removeComponent(Component component) {
      if (component.getState() != ComponentState.PENDING_REMOVE && component.getState() != ComponentState.REMOVED) {
         component.setState(ComponentState.PENDING_REMOVE);
         this.pendingRemoveComponents.add(component);
      }
   }

   public final void removeEntity(Entity entity) {
      if (entity.getEntityStateId() != EntityState.PENDING_REMOVE && entity.getEntityStateId() != EntityState.REMOVED) {
         entity.setEntityStateId(EntityState.PENDING_REMOVE);
         Array components;
         if ((components = this.entityComponentsMap.get(entity)) != null) {
            for (int i = 0; i < components.size; i++) {
               this.removeComponent((Component)components.get(i));
            }
         }

         this.pendingRemoveEntities.add(entity);
      }
   }

   public final void markRenderOrderDirty() {
      this.renderOrderDirty = true;
   }

   public final Body createBody(BodyComponent bodyComponent, BodyDef bodyDef) {
      Body body = this.box2dWorld.createBody(bodyDef);
      this.bodyComponentMap.put(body, bodyComponent);
      return body;
   }

   public final Component getBodyComponent(Body body) {
      for (int i = 0; i < this.bodyComponentMap.orderedKeys().size; i++) {
         Body key;
         if ((key = this.bodyComponentMap.orderedKeys().get(i)).equals(body)) {
            return this.bodyComponentMap.get(key);
         }
      }

      return null;
   }

   @Override
   public final void dispose() {
      this.box2dWorld.dispose();
      this.rayHandler.dispose();
   }
}
