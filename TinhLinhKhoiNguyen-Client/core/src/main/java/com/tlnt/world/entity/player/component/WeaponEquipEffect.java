package com.tlnt.world.entity.player.component;

import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.skeleton.SpineSkeletonComponent;

public final class WeaponEquipEffect extends SpineSkeletonComponent {
   public PlayerEntity projectile2;

   public WeaponEquipEffect(PlayerEntity player) {
      super(player.getEntity());
      this.projectile2 = player;
      this.setRenderOrder(6000000.0F);
      this.setEnabled(false);
   }

   public final void equipAxe() {
      String skeletonKey = "skeletons/vfxs/axe";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.0F);
      this.requestLayerChange(skeletonKey, 0.25F, mixConfig);
      String[] skins = new String[1];
      skins[0] = this.projectile2.POOL2.weaponTexture;
      this.setSkins(skins);
      this.setEnabled(true);
   }

   public final void equipBow() {
      this.requestLayerChange("skeletons/vfxs/bow", 0.25F, new SkeletonLoader.AnimationMixConfig(0.0F));
      String[] skins = new String[1];
      skins[0] = this.projectile2.POOL2.weaponTexture;
      this.setSkins(skins);
      this.setEnabled(true);
   }

   public final void equipWaterShield() {
      String skeletonKey = "skeletons/vfxs/water_shield";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.0F);
      this.requestLayerChange(skeletonKey, 0.25F, mixConfig);
      this.setEnabled(true);
   }

   public final void equipSlingshot() {
      String skeletonKey = "skeletons/vfxs/slingshot";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.0F);
      this.requestLayerChange(skeletonKey, 0.25F, mixConfig);
      String[] skins = new String[1];
      skins[0] = this.projectile2.POOL2.weaponTexture;
      this.setSkins(skins);
      this.setEnabled(true);
   }

   public final void equipSpear() {
      this.requestLayerChange("skeletons/vfxs/spear", 0.25F, new SkeletonLoader.AnimationMixConfig(0.0F));
      String[] skins = new String[1];
      skins[0] = this.projectile2.POOL2.weaponTexture;
      this.setSkins(skins);
      this.setEnabled(true);
   }

   public final void equipWhip() {
      String skeletonKey = "skeletons/vfxs/whip";
      SkeletonLoader.AnimationMixConfig mixConfig = new SkeletonLoader.AnimationMixConfig(0.0F);
      this.requestLayerChange(skeletonKey, 0.25F, mixConfig);
      String[] skins = new String[1];
      skins[0] = this.projectile2.POOL2.weaponTexture;
      this.setSkins(skins);
      this.setEnabled(true);
   }

}
