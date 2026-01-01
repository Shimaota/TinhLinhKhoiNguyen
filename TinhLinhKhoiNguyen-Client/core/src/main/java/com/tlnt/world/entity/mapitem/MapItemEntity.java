package com.tlnt.world.entity.mapitem;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.tlnt.core.audio.CollisionFilters;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.BodyType;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.physics.collider.CircleCollider;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.mapitem.MapItemData;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.util.GameUtils;
import com.tlnt.world.effect.TargetMarkerEffect;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.mapitem.component.ItemPullController;
import com.tlnt.world.label.SelectionMarker;
import com.tlnt.world.skeleton.OverlayStackComponent;

public final class MapItemEntity extends WorldEntity {
   public MapItemData POOL2 = new MapItemData();
   public BodyComponent skeleton;
   public CircleCollider owner;
   public ItemPullController POOL;
   public TargetMarkerEffect target;
   public OverlayStackComponent movementComponent;

   public MapItemEntity(LoginScreen screen, int id, float x, float y) {
      super(screen, 0, EntityType.ITEM_MAP);
      this.teleportTo(0.0F, 0.0F);
      this.setRenderOrder(8000000.0F);
   }

   @Override
   public final void onAwake() {
      super.onAwake();
      this.skeleton = new BodyComponent(BodyType.DYNAMIC, this.getEntity()).setInitialPosition(this.getPosition());
      this.owner = new CircleCollider(new Vector2(0.0F, 25.0F), 25.0F, this.getEntity());
      this.owner.setFilterData(CollisionFilters.MAP_ITEM);
      this.POOL = new ItemPullController(this);
      this.target = new TargetMarkerEffect(this);
      this.movementComponent = new OverlayStackComponent(this);
      this.movementComponent.addOverlay(new SelectionMarker(this));
   }

   @Override
   public final void render(RenderContext ctx) {
      if (this.POOL2.itemTemplate != null) {
         ctx.setBatchColor(Color.YELLOW);
         ctx.drawTextureFit(
            TextureAsset.obtain("ui/186"),
            this.getPosition().x,
            this.getPosition().y + this.getHeight() / 2.0F,
            this.getWidth() * 100.0F * GameUtils.oscillate(0.5F, 2.0F, 2.5F),
            this.getHeight() * 100.0F * GameUtils.oscillate(0.5F, 2.0F, 2.5F),
            Anchor.CENTER
         );
         ctx.resetBatchColor();
         ctx.drawTextureFit(
            TextureAsset.obtain(this.POOL2.itemTemplate.iconPath),
            this.getPosition().x,
            this.getPosition().y,
            this.getWidth() * 100.0F,
            this.getHeight() * 100.0F,
            Anchor.BOTTOM_CENTER
         );
      }
   }

   @Override
   public final float getWidth() {
      return this.owner.getRadius() * 2.0F;
   }

   @Override
   public final float getHeight() {
      return this.owner.getRadius() * 2.0F;
   }

   @Override
   public final boolean containsPoint(Vector2 point) {
      return super.containsPoint(point);
   }

   @Override
   public final void dispose() {
   }

}
