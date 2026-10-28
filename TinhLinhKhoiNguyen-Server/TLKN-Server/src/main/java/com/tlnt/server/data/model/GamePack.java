package com.tlnt.server.data.model;

import java.util.List;

/**
 * Nội dung file DataGame {@code game} (xem {@code GameDataLoader.loadGameData} phía client):
 * class nhân vật, template nhân vật, phí chợ, vật nuôi, cây thu hoạch, danh hiệu, huy hiệu/kỹ năng bộ lạc,
 * hướng dẫn.
 */
public record GamePack(
      int version,
      List<CharClass> classes,
      List<CharTemplate> templates,
      List<MarketFee> marketFees,
      List<Animal> animals,
      List<Plant> plants,
      List<Title> titles,
      List<TribeEmblem> tribeEmblems,
      List<TribeSkill> tribeSkills,
      List<GuideCategory> guideCategories) {

   public record CharClass(int id, String name) {
   }

   public record CharTemplate(int id, String name, String skeletonPath, String avatarIconPath,
         String infoTableJson, float[] statRatings, boolean selectable) {
   }

   /** Không có id trong file, định danh bằng thứ tự (giống {@code MarketFeeOption} client). */
   public record MarketFee(int index, String name, int percent) {
   }

   public record Animal(int id, String growingName, String matureName, String skeletonPath,
         String skinName, float scale, int growDurationMs) {
   }

   public record Plant(int id, String name, float offsetX, float offsetY, String skeletonName,
         String idleAnimation, float skeletonScale) {
   }

   /** {@code icon} = nút danh hiệu; {@code auraAnimation} (cột {@code aura_animation}, trước là texture_key) = hoạt ảnh aura_fx. */
   public record Title(int id, String name, String icon, String auraAnimation, String[] descriptionLines) {
   }

   public record TribeEmblem(int id, String name, String[] framePaths, int frameRate) {
   }

   public record TribeSkill(int id, String name, String icon, String description) {
   }

   public record GuideCategory(int id, String name, String iconPath, List<GuideTopic> topics) {
   }

   public record GuideTopic(int id, String name, String layoutJson) {
   }
}
