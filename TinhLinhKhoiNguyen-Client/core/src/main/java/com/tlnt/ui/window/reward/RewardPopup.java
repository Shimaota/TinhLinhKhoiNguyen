package com.tlnt.ui.window.reward;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Pool;
import com.github.tommyettinger.textra.TextraLabel;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.util.NamedPair;
import com.tlnt.model.item.Item;
import com.tlnt.thirdparty.spine.AnimationState;
import com.tlnt.thirdparty.spine.Skeleton;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.widget.ScrollPaneFactory;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.Popup;

public class RewardPopup extends Popup {
   public static final Color OVERLAY_COLOR = new Color(0.0F, 0.0F, 0.0F, 0.5F);
   public Pool<ItemSlotButton> rewardItemPool = new RewardItemPool(this);
   public AnimationState skeletonAnimation;
   public Skeleton skeletonRenderer;
   public TextraLabel headerLabel;
   public ScrollPane rewardScrollPane;
   public Table rewardItemTable;
   public RewardConfirmButton confirmButton;
   public Item[] rewardItems;
   public boolean itemsSpawned = true;

   public RewardPopup(RenderContext renderContext) {
      super(
         renderContext,
         WindowType.REWARD_ITEMS_POPUP,
         0.0F,
         0.0F,
         12,
         (float)WorldConfig.VIEWPORT_WIDTH,
         (float)WorldConfig.VIEWPORT_HEIGHT
      );
      NamedPair skeletonPair;
      if ((skeletonPair = SkeletonLoader.obtainSkeleton("ui/reward", 5.0F, new SkeletonLoader.AnimationMixConfig())) != null) {
         this.skeletonAnimation = (AnimationState)skeletonPair.first;
         this.skeletonRenderer = (Skeleton)skeletonPair.second;
         this.skeletonAnimation.addListener(new RewardAnimationListener(this));
      }

      String headerText = "[#8B4E23]Bạn vừa nhận được";
      this.headerLabel = new TextraLabel(headerText, FontFactory.getCachedTextraFont(FontManager.PLAIN_FONT, 30));
      this.headerLabel.setAlignment(2);
      float centerX = (float)(WorldConfig.VIEWPORT_WIDTH / 2);
      this.headerLabel
         .setPosition(
            centerX,
            (float)(WorldConfig.VIEWPORT_HEIGHT / 2 + 150),
            1
         );
      this.addActor(this.headerLabel);
      this.rewardScrollPane = ScrollPaneFactory.createScrollPane(this.rewardItemTable = new Table().center());
      this.rewardScrollPane.setSize(1000.0F, 116.0F);
      this.rewardScrollPane.setPosition(this.getWidth() / 2.0F, this.getHeight() / 2.0F, 1);
      this.addActor(this.rewardScrollPane);
      this.confirmButton = new RewardConfirmButton(this, "Đồng ý", super.renderContext);
      this.confirmButton.setSize(192.0F, 63.0F);
      centerX = (float)(WorldConfig.VIEWPORT_WIDTH / 2);
      this.confirmButton
         .setPosition(centerX, (float)(WorldConfig.VIEWPORT_HEIGHT / 2 - 100), 1);
      this.addActor(this.confirmButton);
      this.confirmButton.addListener(new ConfirmButtonClickListener(this));
   }

   public final void showRewards(Item[] items) {
      if (this.rewardItems != null) {
         Item[] oldItems = this.rewardItems;
         int count = this.rewardItems.length;

         for (int i = 0; i < count; i++) {
            Item oldItem;
            if ((oldItem = oldItems[i]) != null) {
               oldItem.clear();
            }
         }
      }

      for (int j = this.rewardItemTable.getChildren().size - 1; j >= 0; j--) {
         Actor child;
         if ((child = this.rewardItemTable.getChild(j)) instanceof ItemSlotButton) {
            this.rewardItemPool.free((ItemSlotButton)child);
         }
      }

      this.rewardItemTable.clear();
      this.headerLabel.setVisible(false);
      this.confirmButton.setVisible(false);
      this.clearActions();
      this.rewardItems = items;
      this.itemsSpawned = false;
      this.show();
      this.playSkeletonAnimation("appear", false);
      this.toFront();
      AudioManager.playSound("reward", null);
   }

   public void playSkeletonAnimation(String animationName, boolean loop) {
      if (this.skeletonAnimation != null) {
         this.skeletonAnimation.setAnimation(0, animationName, loop);
      }
   }

   @Override
   public final void onUpdate(float delta) {
      float animTime = this.skeletonAnimation.getCurrent(0).getAnimationTime();
      delta = animTime / this.skeletonAnimation.getCurrent(0).getAnimation().getDuration();
      if (!this.itemsSpawned) {
         String animName = this.skeletonAnimation.getCurrent(0).getAnimation().getName();
         if (animName.equals("appear") && delta >= 0.7F) {
            this.itemsSpawned = true;

            for (int i = 0; i < this.rewardItems.length; i++) {
               this.addAction(Actions.sequence(Actions.delay((float)i * 0.2F), Actions.run(new AddRewardItemTask(this, i))));
               int itemCount = this.rewardItems.length;
               if (i == itemCount - 1) {
                  this.addAction(Actions.sequence(Actions.delay((float)i * 0.2F + 0.25F), Actions.run(new ShowRewardItemsTask(this))));
               }
            }
         }
      }
   }

   @Override
   public void drawChildren(Batch batch, float parentAlpha) {
      super.drawChildren(batch, parentAlpha);
   }

   @Override
   public final void drawBackground() {
      super.renderContext.drawLine(this.getX(), this.getY(), this.getWidth(), this.getHeight(), OVERLAY_COLOR);
      if (this.skeletonRenderer != null) {
         this.skeletonAnimation.update(Gdx.graphics.getDeltaTime());
         this.skeletonAnimation.apply(this.skeletonRenderer);
         this.skeletonRenderer.update(Gdx.graphics.getDeltaTime());
         this.skeletonRenderer.setPosition(this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F);
         this.skeletonRenderer.setScale(25.0F, 25.0F);
         this.skeletonRenderer.updateWorldTransform(Skeleton.Physics.UPDATE);
      }

      if (this.skeletonRenderer != null) {
         super.renderContext.drawSkeleton(this.skeletonRenderer);
      }
   }

   @Override
   public final boolean isModal() {
      return false;
   }

}
