package com.tlnt.core;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.core.factory.AmbientFishFactory;
import com.tlnt.core.factory.AnimalFactory;
import com.tlnt.core.factory.MapItemFactory;
import com.tlnt.core.factory.MobFactory;
import com.tlnt.core.factory.PlantFactory;
import com.tlnt.core.factory.NpcFactory;
import com.tlnt.core.factory.PlayerFactory;
import com.tlnt.engine.physics.BodyComponent;
import com.tlnt.engine.physics.EntityType;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.UtilityServiceState;
import com.tlnt.model.achievement.AchievementList;
import com.tlnt.model.announcement.AnnouncementList;
import com.tlnt.model.chat.ChatManager;
import com.tlnt.model.combopack.ComboPackCatalog;
import com.tlnt.model.consignment.ConsignmentShopData;
import com.tlnt.model.farm.AnimalBarnState;
import com.tlnt.model.forge.ForgeState;
import com.tlnt.model.friendentry.FriendEntry;
import com.tlnt.model.friendentry.FriendInviteEntry;
import com.tlnt.model.giftcode.GiftcodeEvent;
import com.tlnt.model.giftcode.GiftcodeEventMilestone;
import com.tlnt.model.giftcode.GiftcodeState;
import com.tlnt.model.item.Item;
import com.tlnt.model.luckywheel.LuckyWheelData;
import com.tlnt.model.mail.Mailbox;
import com.tlnt.model.market.MarketFeeOption;
import com.tlnt.model.market.MarketListing;
import com.tlnt.model.option.OptionInfo;
import com.tlnt.model.party.Party;
import com.tlnt.model.player.InspectedPlayerData;
import com.tlnt.model.player.PlayerInventory;
import com.tlnt.model.quest.MainQuestState;
import com.tlnt.model.quest.daily.DailyActivityState;
import com.tlnt.model.ranking.RankEntry;
import com.tlnt.model.ranking.RankingCategory;
import com.tlnt.model.server.ServerInfo;
import com.tlnt.model.skill.LearnedSkill;
import com.tlnt.model.social.Tribe;
import com.tlnt.model.social.TribeState;
import com.tlnt.model.trade.TradeState;
import com.tlnt.model.weeklytop.WeeklyTopEvent;
import com.tlnt.world.effect.TargetMarkerEffect;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.world.entity.fish.AmbientFish;
import com.tlnt.world.entity.mapitem.MapItemEntity;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.plant.PlantEntity;
import com.tlnt.world.entity.npc.NpcEntity;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.projectile.axe.AxeProjectileEntity;
import com.tlnt.world.projectile.bow.BowArrowProjectile;
import com.tlnt.world.projectile.mobskill.MobSkillProjectile;
import com.tlnt.world.projectile.slingshot.SlingshotProjectile;
import com.tlnt.world.projectile.spear.SpearProjectileEntity;

public final class GameState {
   public static int pingMs;
   public static long loginScreenTimestamp;
   public static Array<ServerInfo> serverList = new Array<>();
   public static WorldEntity targetEntity;
   public static boolean hasSecondaryPassword;
   public static boolean isSecondaryPasswordLocked;
   public static PlayerInventory inventory = new PlayerInventory();
   public static Array<LearnedSkill> learnedSkills = new Array<>(5);
   public static Array<LearnedSkill> skillBarSkills;
   public static LearnedSkill selectedSkill;
   public static Party currentParty;
   public static Array<Party> nearbyParties = new Array<>();
   public static GiftcodeState giftcodeData = new GiftcodeState();
   public static Array<FriendEntry> friends = new Array<>();
   public static Array<FriendInviteEntry> friendInvites = new Array<>();
   public static TribeState tribeData = new TribeState();
   public static MainQuestState tutorialState = new MainQuestState();
   public static DailyActivityState dailyQuestData = new DailyActivityState();
   public static Array<OptionInfo> activeBuffs = new Array<>();
   public static AnnouncementList announcements = new AnnouncementList();
   public static InspectedPlayerData wardrobeData = new InspectedPlayerData();
   public static Array<PlayerEntity> nearbyPlayers = new Array<>();
   public static Array<MobEntity> nearbyMobs = new Array<>();
   public static Array<NpcEntity> nearbyNpcs = new Array<>(5);
   public static Array<MapItemEntity> mapItems = new Array<>();
   public static Array<AnimalEntity> nearbyAnimals = new Array<>();
   public static Array<PlantEntity> nearbyPlants = new Array<>();
   public static AnimalBarnState buildingStatus = new AnimalBarnState();
   public static Array<AmbientFish> ambientFish = new Array<>();
   public static AchievementList achievementData = new AchievementList();
   public static LuckyWheelData luckyWheelData = new LuckyWheelData();
   public static ChatManager chatManager = new ChatManager();
   public static Item marketSellItem;
   public static MarketFeeOption selectedMarketCategory;
   public static Array<MarketListing> marketListings = new Array<>();
   public static Array<MarketListing> myMarketListings = new Array<>();
   public static Array<RankingCategory> rankingCategories = new Array<>();
   public static RankEntry myRankEntry;
   /** Recharge packages (op 72 sub 0). */
   public static Array<com.tlnt.ui.window.recharge.RechargeWindow.Pack> rechargePacks = new Array<>();
   /** World boss table (op 100 sub 0); {@link #worldBossVersion} increments on every update so the table knows to rebuild. */
   public static Array<com.tlnt.model.worldboss.WorldBossInfo> worldBosses = new Array<>();
   public static int worldBossVersion;
   /** Standalone op 75 ranking (categoryId -1: not re-requested, no row for the local player). */
   public static RankingCategory otherRanking = new RankingCategory(-1, "", "");
   public static Array<WeeklyTopEvent> weeklyTopEvents = new Array<>();
   public static Mailbox mailbox = new Mailbox();
   public static TextureAsset captchaTexture = new TextureAsset();
   public static ForgeState forgeData = new ForgeState();
   public static ConsignmentShopData shopData = new ConsignmentShopData();
   public static ComboPackCatalog comboPackData = new ComboPackCatalog();
   public static TradeState tradeSession = new TradeState();
   public static UtilityServiceState appleEventState = new UtilityServiceState();
   public static long skill1CooldownStart;
   public static int skill1CooldownDuration = 1;
   public static long skill2CooldownStart;
   public static int skill2CooldownDuration = 1;
   public static byte[] textureDecryptKey;
   public static byte[] atlasDecryptKey;

   public static void setTargetEntity(WorldEntity entity) {
      TargetMarkerEffect marker;
      if (entity != null && (marker = entity.getEntity().findComponent(TargetMarkerEffect.class, false)) != null) {
         marker.showTargetMarker();
      }

      targetEntity = entity;
      if (PlayerEntity.getInstance() != null && PlayerEntity.getInstance().offscreenArrow != null) {
         PlayerEntity.getInstance().offscreenArrow.setAutoPathing(false);
      }
   }

   public static void validateTargetEntity() {
      if (targetEntity != null) {
         if (!targetEntity.isActive()) {
            targetEntity = null;
            return;
         }

         if (targetEntity.getEntityType() == EntityType.PLAYER && ((PlayerEntity)targetEntity).isStealthed) {
            targetEntity = null;
         }
      }
   }

   public static MapItemEntity findMapItemById(int id) {
      Array.ArrayIterator it = mapItems.iterator();

      while (it.hasNext()) {
         MapItemEntity mapItem;
         if ((mapItem = (MapItemEntity)it.next()).getId() == id) {
            return mapItem;
         }
      }

      return null;
   }

   public static PlayerEntity findPlayerById(int id) {
      if (PlayerEntity.getInstance() != null && PlayerEntity.getInstance().getId() == id) {
         return PlayerEntity.getInstance();
      }

      // Index loop: no allocation, does not touch Array's shared iterator (calling it nested inside another iteration
      // = GdxRuntimeException "#iterator() cannot be used nested").
      Object[] items = nearbyPlayers.items;
      for (int i = 0, n = nearbyPlayers.size; i < n; i++) {
         PlayerEntity e = (PlayerEntity)items[i];
         if (e.getId() == id) {
            return e;
         }
      }

      return null;
   }

   public static MobEntity findMobById(int id) {
      // Index loop: no allocation, does not touch Array's shared iterator (calling it nested inside another iteration
      // = GdxRuntimeException "#iterator() cannot be used nested").
      Object[] items = nearbyMobs.items;
      for (int i = 0, n = nearbyMobs.size; i < n; i++) {
         MobEntity e = (MobEntity)items[i];
         if (e.getId() == id) {
            return e;
         }
      }

      return null;
   }

   public static NpcEntity findNpcById(int id) {
      // Index loop: no allocation, does not touch Array's shared iterator (calling it nested inside another iteration
      // = GdxRuntimeException "#iterator() cannot be used nested").
      Object[] items = nearbyNpcs.items;
      for (int i = 0, n = nearbyNpcs.size; i < n; i++) {
         NpcEntity e = (NpcEntity)items[i];
         if (e.getId() == id) {
            return e;
         }
      }

      return null;
   }

   public static AnimalEntity findAnimalById(int id) {
      // Index loop: no allocation, does not touch Array's shared iterator (calling it nested inside another iteration
      // = GdxRuntimeException "#iterator() cannot be used nested").
      Object[] items = nearbyAnimals.items;
      for (int i = 0, n = nearbyAnimals.size; i < n; i++) {
         AnimalEntity e = (AnimalEntity)items[i];
         if (e.getId() == id) {
            return e;
         }
      }

      return null;
   }

   public static PlantEntity findPlantById(int id) {
      // Index loop: no allocation, does not touch Array's shared iterator (calling it nested inside another iteration
      // = GdxRuntimeException "#iterator() cannot be used nested").
      Object[] items = nearbyPlants.items;
      for (int i = 0, n = nearbyPlants.size; i < n; i++) {
         PlantEntity e = (PlantEntity)items[i];
         if (e.getId() == id) {
            return e;
         }
      }

      return null;
   }

   public static LearnedSkill getSkillById(int skillId) {
      Array.ArrayIterator it = learnedSkills.iterator();

      while (it.hasNext()) {
         LearnedSkill learned;
         if ((learned = (LearnedSkill)it.next()).skill.id == skillId) {
            return learned;
         }
      }

      return null;
   }

   public static void clearWorldEntities(boolean fullReset) {
      targetEntity = null;
      Array.ArrayIterator it = ambientFish.iterator();

      while (it.hasNext()) {
         AmbientFishFactory.freeFish((AmbientFish)it.next());
      }

      ambientFish.clear();
      com.tlnt.world.entity.floataway.FloatAwayEffect.clearAll(); // op 80 floating objects
      com.tlnt.world.entity.mapprop.MapPropEntity.clearAll(); // op 81 decorations
      it = nearbyMobs.iterator();

      while (it.hasNext()) {
         MobFactory.freeMob((MobEntity)it.next());
      }

      nearbyMobs.clear();
      it = nearbyNpcs.iterator();

      while (it.hasNext()) {
         NpcFactory.freeNpc((NpcEntity)it.next());
      }

      nearbyNpcs.clear();
      it = mapItems.iterator();

      while (it.hasNext()) {
         MapItemFactory.freeMapItem((MapItemEntity)it.next());
      }

      mapItems.clear();
      it = nearbyAnimals.iterator();

      while (it.hasNext()) {
         AnimalFactory.freeAnimal((AnimalEntity)it.next());
      }

      nearbyAnimals.clear();
      it = nearbyPlants.iterator();

      while (it.hasNext()) {
         PlantFactory.freePlant((PlantEntity)it.next());
      }

      nearbyPlants.clear();

      for (int i = nearbyPlayers.size - 1; i >= 0; i--) {
         PlayerEntity player;
         if ((player = nearbyPlayers.get(i)).isLocalPlayer() && !fullReset) {
            BodyComponent bodyComponent;
            Body body;
            if ((bodyComponent = player.getEntity().getBodyComponent()) != null && (body = bodyComponent.getBody()) != null) {
               body.setLinearVelocity(Vector2.Zero);
            }
         } else {
            if (player.isLocalPlayer()) {
               PlayerFactory.deactivateMainPlayer();
            } else {
               PlayerFactory.freePlayer(player);
            }

            nearbyPlayers.removeIndex(i);
         }
      }

      it = nearbyParties.iterator();

      while (it.hasNext()) {
         ((Party)it.next()).dispose();
      }

      nearbyParties.clear();
      GameMain.instance.getLoginScreen().editorViewport.clearMap();
      AxeProjectileEntity.releaseAll();
      BowArrowProjectile.clearAll();
      SpearProjectileEntity.clearAll();
      MobSkillProjectile.clearAll();
      SlingshotProjectile.recycleAll();
      AudioManager.stopAllLoopedSounds();
      if (fullReset) {
         if (PlayerEntity.getInstance() != null) {
            PlayerEntity.getInstance().autoTargetController.projectile = false;
            PlayerEntity.getInstance().harvestProgressBar.stopProgress();
            GameMain.instance.getLoginScreen().debugScrollPane.POOL2.entityHpBarPanel.setVisible(false);
         }

         activeBuffs.clear();
         forgeData.reset();
         PlayerInventory inv = inventory;
         inventory.inventoryTimestamp1 = 0L;
         inv.inventoryTimestamp2 = 0L;
         inv.inventoryTimestamp3 = 0L;
         inv.clearEquipmentSlots();
         inv.clearWeaponSlots();
         inv.clearArmorSlots();
         inv.clearAccessorySlots();
         inv.clearInventoryItems();
         inv.clearMaterialItems();
         inv.clearSkillDescriptions1();
         inv.clearSkillDescriptions2();
         inv.clearSkillDescriptions3();
         shopData.clearCategories();
         AnnouncementList announcementList = announcements;
         announcements.announcements.clear();
         if (currentParty != null) {
            currentParty.dispose();
            currentParty = null;
         }

         GiftcodeState giftcodes = giftcodeData;
         it = giftcodeData.events.iterator();

         while (it.hasNext()) {
            GiftcodeEvent event;
            (event = (GiftcodeEvent)it.next()).title = null;
            if (event.milestones != null) {

               for (int j = 0; j < event.milestones.length; j++) {
                  if (event.milestones[j] != null) {
                     GiftcodeEventMilestone milestone;
                     (milestone = event.milestones[j]).description = null;
                     if (milestone.rewardItems != null) {

                        for (int k = 0; k < milestone.rewardItems.length; k++) {
                           if (milestone.rewardItems[k] != null) {
                              milestone.rewardItems[k].clear();
                           }

                           milestone.rewardItems[k] = null;
                        }

                        milestone.rewardItems = null;
                     }
                  }

                  event.milestones[j] = null;
               }

               event.milestones = null;
            }
         }

         giftcodes.events.clear();
         giftcodes.giftcodeListText = null;
         FriendEntry.POOL.freeAll(friends);
         friends.clear();
         FriendInviteEntry.POOL.freeAll(friendInvites);
         friendInvites.clear();
         marketSellItem = null;
         if (tribeData.myTribe != null) {
            Tribe.POOL.free(tribeData.myTribe);
            tribeData.myTribe = null;
         }

         skill1CooldownStart = 0L;
         skill2CooldownStart = 0L;
      }
   }

   static {
      skillBarSkills = new Array<>(5);
   }
}
