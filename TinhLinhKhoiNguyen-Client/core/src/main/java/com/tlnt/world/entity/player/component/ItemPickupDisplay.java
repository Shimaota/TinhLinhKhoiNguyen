package com.tlnt.world.entity.player.component;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.engine.physics.BaseComponent;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.item.ItemTemplate;
import com.tlnt.util.GameUtils;
import com.tlnt.world.entity.player.PlayerEntity;

public final class ItemPickupDisplay extends BaseComponent {
   public Pool<ItemPickupDisplay.PickupEntry> projectile = new ItemPickupEntryPool(this);
   public Array<ItemPickupDisplay.PickupEntry> speed = new Array<>();
   public PlayerEntity active;

   public ItemPickupDisplay(PlayerEntity player) {
      super(player.getEntity());
      this.active = player;
      this.setRenderOrder(1.0E8F);
   }

   public final void showItemPickup(int itemId) {
      this.speed
         .add(
            this.projectile
               .obtain()
               .init(itemId, this.active.getPosition().x, this.active.getPosition().y + this.active.getHeight(), GameUtils.randomFloat(1.5F, 2.5F))
         );
   }

   @Override
   public final void update(float delta) {
      for (int i = this.speed.size - 1; i >= 0; i--) {
         ItemPickupDisplay.PickupEntry entry;
         (entry = this.speed.get(i)).update(delta);
         if (entry.finished) {
            this.speed.removeIndex(i);
            this.projectile.free(entry);
         }
      }
   }

   @Override
   public final void render(RenderContext ctx) {
      RenderContext renderContext = ctx;

      for (ItemPickupDisplay.PickupEntry entry : this.speed) {
         renderContext.setBatchColorRgba(1.0F, 1.0F, 1.0F, entry.alpha);
         renderContext.drawTexture(TextureAsset.obtain(entry.itemDef.iconPath), entry.position.x, entry.position.y, Anchor.CENTER);
         renderContext.resetBatchColor();
      }
   }

   public class PickupEntry {
      public ItemTemplate itemDef;
      public Vector2 position = new Vector2();
      public float startX;
      public float startY;
      public float targetY;
      public float elapsed;
      public float duration;
      public float alpha;
      public boolean finished;

      public final ItemPickupDisplay.PickupEntry init(int itemId, float x, float y, float durationSec) {
         this.itemDef = ItemTemplate.getById(itemId);
         this.position.set(x, y);
         this.startX = x;
         this.startY = y;
         this.targetY = y + GameUtils.randomFloat(1.0F, 1.5F);
         this.elapsed = 0.0F;
         this.duration = durationSec;
         this.alpha = 1.0F;
         this.finished = false;
         return this;
      }

      public final void update(float delta) {
         if (!this.finished) {
            this.elapsed += delta;
            float wobbleX = (float)Math.sin((double)(delta = Math.min(this.elapsed / this.duration, 1.0F)) * Math.PI * 5.0) * 0.01F;
            this.position.x = this.startX + wobbleX;
            this.position.y = Interpolation.fastSlow.apply(this.startY, this.targetY, delta);
            if (delta >= 0.7F) {
               this.alpha = Interpolation.fastSlow.apply(1.0F, 0.0F, (delta - 0.7F) / 0.3F);
            }

            if (delta >= 1.0F) {
               this.finished = true;
            }
         }
      }
   }
}
