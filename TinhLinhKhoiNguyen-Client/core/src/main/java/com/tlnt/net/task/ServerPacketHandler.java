package com.tlnt.net.task;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.shimaota.a.a.IMessage;
import com.shimaota.a.a.Message;
import com.tlnt.core.GameState;
import com.tlnt.core.TaskScheduler;
import com.tlnt.core.asset.SkeletonLoader;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.core.factory.AnimalFactory;
import com.tlnt.core.factory.MapItemFactory;
import com.tlnt.core.factory.MobFactory;
import com.tlnt.core.factory.PlantFactory;
import com.tlnt.core.factory.NpcFactory;
import com.tlnt.core.factory.PlayerFactory;
import com.tlnt.engine.particle.ParticleEffect;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.engine.physics.Direction;
import com.tlnt.engine.scene.GameObject;
import com.tlnt.model.achievement.Achievement;
import com.tlnt.model.character.CharacterClass;
import com.tlnt.model.character.CharacterTemplate;
import com.tlnt.model.combopack.ComboPack;
import com.tlnt.model.combopack.ComboPackCatalog;
import com.tlnt.model.consignment.ConsignmentCategory;
import com.tlnt.model.consignment.ConsignmentItem;
import com.tlnt.model.consignment.ConsignmentShopData;
import com.tlnt.model.farm.AnimalBarnState;
import com.tlnt.model.farm.AnimalType;
import com.tlnt.model.giftcode.GiftcodeEvent;
import com.tlnt.model.giftcode.GiftcodeEventMilestone;
import com.tlnt.model.guide.GuideCategory;
import com.tlnt.model.guide.GuideTopic;
import com.tlnt.model.item.ElementStone;
import com.tlnt.model.item.Item;
import com.tlnt.model.item.ItemTemplate;
import com.tlnt.model.item.StatLine;
import com.tlnt.model.item.StatType;
import com.tlnt.model.luckywheel.LuckyWheelData;
import com.tlnt.model.mail.MailMessage;
import com.tlnt.model.market.MarketListing;
import com.tlnt.model.mob.MobData;
import com.tlnt.model.mob.MobTemplate;
import com.tlnt.model.plant.PlantTemplate;
import com.tlnt.model.plant.PlantTemplateRef;
import com.tlnt.model.npc.NpcState;
import com.tlnt.model.npc.NpcTemplate;
import com.tlnt.model.player.FactionStatus;
import com.tlnt.model.player.InspectedPlayerData;
import com.tlnt.model.player.MoveSpeedTable;
import com.tlnt.model.player.PlayerInventory;
import com.tlnt.model.player.PlayerStats;
import com.tlnt.model.quest.MainQuestStage;
import com.tlnt.model.quest.MainQuestState;
import com.tlnt.model.quest.daily.DailyActivityState;
import com.tlnt.model.quest.daily.DailyMilestone;
import com.tlnt.model.quest.daily.DailyQuestTask;
import com.tlnt.model.skill.LearnedSkill;
import com.tlnt.model.skill.SkillLevelData;
import com.tlnt.model.social.Tribe;
import com.tlnt.model.social.TribeApplicant;
import com.tlnt.model.social.TribeEmblem;
import com.tlnt.model.social.TribeMember;
import com.tlnt.model.social.TribeSkill;
import com.tlnt.model.title.TitleData;
import com.tlnt.model.trade.TradeState;
import com.tlnt.model.weeklytop.WeeklyTopEntry;
import com.tlnt.model.weeklytop.WeeklyTopEvent;
import com.tlnt.screen.login.LoginScreen;
import com.tlnt.ui.hud.widget.EntityHpBarPanel;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.NpcDialogPopupV2;
import com.tlnt.ui.window.action.NpcSelectionPopup;
import com.tlnt.ui.window.action.QuantityInputDialog;
import com.tlnt.ui.window.market.MarketWindow;
import com.tlnt.ui.window.reward.RewardPopup;
import com.tlnt.ui.window.weaponcraft.WeaponCraftWindow;
import com.tlnt.util.GameUtils;
import com.tlnt.world.effect.floating.PooledFloatingEffect;
import com.tlnt.world.groundeffect.SpearProjectileEffect;
import com.tlnt.world.entity.WorldEntity;
import com.tlnt.world.entity.animal.AnimalEntity;
import com.tlnt.world.entity.combattext.FloatingCombatText;
import com.tlnt.world.entity.decoration.MapDecorationObject;
import com.tlnt.world.entity.mapitem.MapItemEntity;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.plant.PlantEntity;
import com.tlnt.world.entity.npc.NpcEntity;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.entity.player.component.WeaponEquipEffect;
import com.tlnt.world.projectile.feather.FeatherProjectileEntity;
import java.util.concurrent.TimeUnit;

public final class ServerPacketHandler {
   public static ServerPacketHandler INSTANCE;
   public LoginScreen loginScreen;

   public static ServerPacketHandler getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ServerPacketHandler();
      }

      return INSTANCE;
   }

   public final void handlePlayerUpdate(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            int localPlayerId = msg.readInt();
            float spawnX = msg.readFloat() / 100.0F;
            float spawnY = msg.readFloat() / 100.0F;
            PlayerEntity localPlayer;
            readPlayerStats((localPlayer = PlayerFactory.obtainMainPlayer(localPlayerId, spawnX, spawnY)).POOL2, msg);
            localPlayer.refreshTitleAura();
            PlayerEntity.setAsLocalPlayer(localPlayer);
            if (localPlayer.isAlive()) {
               localPlayer.setAnimationState(AnimationState.IDLE);
            } else {
               localPlayer.setAnimationState(AnimationState.DIE);
            }

            GameState.nearbyPlayers.add(localPlayer);
            this.loginScreen.box2dDebugRenderer.setFollowTarget(localPlayer);
            return;
         case 1:
            PlayerEntity skinPlayer;
            if ((skinPlayer = GameState.findPlayerById(msg.readInt())) == null) {
               return;
            }

            String[] skinKeys = new String[msg.readByte()];

            for (int i = 0; i < skinKeys.length; i++) {
               skinKeys[i] = msg.readString();
            }

            skinPlayer.POOL2.equipmentSkinKeys = skinKeys;
            skinPlayer.POOL2.isTransparent = true; // reload skeleton + skin on the next tick (TransparentBodyOverlay)
            return;
         case 2:
            PlayerEntity stealthPlayer;
            if ((stealthPlayer = GameState.findPlayerById(msg.readInt())) == null) {
               return;
            }

            boolean stealthed = msg.readBoolean();
            stealthPlayer.setStealthed(stealthed);
            return;
         case 3:
            int hitPlayerId = msg.readInt();
            long damage = msg.readLong();
            long currentHp = msg.readLong();
            PlayerEntity hitPlayer;
            if ((hitPlayer = GameState.findPlayerById(hitPlayerId)) == null) {
               return;
            }

            hitPlayer.onHit(null, currentHp, damage, false);
            return;
         case 4:
            PlayerEntity.getInstance().POOL2.movementMode = MoveSpeedTable.getSpeedValues()[msg.readByte()];
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 5:
            GameState.wardrobeData.clearBackpackSlots();
            GameState.wardrobeData.clearBackpackSkillDescriptions();
            readPlayerStats(GameState.wardrobeData.playerStats, msg);
            GameState.wardrobeData.clearEquipmentSlots();
            int intValue = msg.readInt();

            for (int i = 0; i < intValue; i++) {
               GameState.wardrobeData.equipmentSlots.add(this.readItemFromMessage(i, null, msg));
            }

            GameState.wardrobeData.clearBagSlots1();
            int bagCount = msg.readInt();

            for (int i = 0; i < bagCount; i++) {
               GameState.wardrobeData.bagSlots1.add(this.readItemFromMessage(i, null, msg));
            }

            int selectedIndex = msg.readInt();
            GameState.wardrobeData.selectedItem1 = selectedIndex != -1 ? GameState.wardrobeData.bagSlots1.get(selectedIndex) : null;
            InspectedPlayerData wardrobe1 = GameState.wardrobeData;
            if (GameState.wardrobeData.skillDescriptions1 != null && !wardrobe1.skillDescriptions1.isEmpty()) {
               Array.ArrayIterator statIt1 = wardrobe1.skillDescriptions1.iterator();

               while (statIt1.hasNext()) {
                  ((StatLine)statIt1.next()).clear();
               }

               wardrobe1.skillDescriptions1.clear();
            }

            int statLineCount = msg.readInt();

            for (int i = 0; i < statLineCount; i++) {
               intValue = msg.readInt();
               Integer[] statValues1 = new Integer[msg.readByte()];

               for (int j = 0; j < statValues1.length; j++) {
                  statValues1[j] = msg.readInt();
               }

               GameState.wardrobeData.skillDescriptions1.add(new StatLine(StatType.getById(intValue), statValues1, null));
            }

            StatLine.mergeStatLines(GameState.wardrobeData.skillDescriptions1);
            GameState.wardrobeData.clearBagSlots2();
            int bagCount2 = msg.readInt();

            for (int i = 0; i < bagCount2; i++) {
               GameState.wardrobeData.bagSlots2.add(this.readItemFromMessage(i, null, msg));
            }

            intValue = msg.readInt();
            GameState.wardrobeData.selectedItem2 = intValue != -1 ? GameState.wardrobeData.bagSlots2.get(intValue) : null;
            InspectedPlayerData wardrobe2 = GameState.wardrobeData;
            if (GameState.wardrobeData.skillDescriptions2 != null && !wardrobe2.skillDescriptions2.isEmpty()) {
               Array.ArrayIterator statIt2 = wardrobe2.skillDescriptions2.iterator();

               while (statIt2.hasNext()) {
                  ((StatLine)statIt2.next()).clear();
               }

               wardrobe2.skillDescriptions2.clear();
            }

            int statLineCount2 = msg.readInt();

            for (int i = 0; i < statLineCount2; i++) {
               intValue = msg.readInt();
               Integer[] statValues2 = new Integer[msg.readByte()];

               for (int j = 0; j < statValues2.length; j++) {
                  statValues2[j] = msg.readInt();
               }

               GameState.wardrobeData.skillDescriptions2.add(new StatLine(StatType.getById(intValue), statValues2, null));
            }

            StatLine.mergeStatLines(GameState.wardrobeData.skillDescriptions2);
            GameState.wardrobeData.clearBagSlots3();
            bagCount = msg.readInt();

            for (int i = 0; i < bagCount; i++) {
               GameState.wardrobeData.bagSlots3.add(this.readItemFromMessage(i, null, msg));
            }

            intValue = msg.readInt();
            GameState.wardrobeData.selectedItem3 = intValue != -1 ? GameState.wardrobeData.bagSlots3.get(intValue) : null;
            InspectedPlayerData wardrobe3 = GameState.wardrobeData;
            if (GameState.wardrobeData.skillDescriptions3 != null && !wardrobe3.skillDescriptions3.isEmpty()) {
               Array.ArrayIterator statIt3 = wardrobe3.skillDescriptions3.iterator();

               while (statIt3.hasNext()) {
                  ((StatLine)statIt3.next()).clear();
               }

               wardrobe3.skillDescriptions3.clear();
            }

            selectedIndex = msg.readInt();

            for (int i = 0; i < selectedIndex; i++) {
               intValue = msg.readInt();
               Integer[] statValues3 = new Integer[msg.readByte()];

               for (int j = 0; j < statValues3.length; j++) {
                  statValues3[j] = msg.readInt();
               }

               GameState.wardrobeData.skillDescriptions3.add(new StatLine(StatType.getById(intValue), statValues3, null));
            }

            StatLine.mergeStatLines(GameState.wardrobeData.skillDescriptions3);
            GameState.wardrobeData.clearBagSlots4();
            statLineCount = msg.readInt();

            for (int i = 0; i < statLineCount; i++) {
               GameState.wardrobeData.bagSlots4.add(this.readItemFromMessage(i, null, msg));
            }

            intValue = msg.readInt();
            GameState.wardrobeData.selectedItem4 = intValue != -1 ? GameState.wardrobeData.bagSlots4.get(intValue) : null;
            InspectedPlayerData wardrobe4 = GameState.wardrobeData;
            if (GameState.wardrobeData.skillDescriptions4 != null && !wardrobe4.skillDescriptions4.isEmpty()) {
               Array.ArrayIterator statIt4 = wardrobe4.skillDescriptions4.iterator();

               while (statIt4.hasNext()) {
                  ((StatLine)statIt4.next()).clear();
               }

               wardrobe4.skillDescriptions4.clear();
            }

            statLineCount2 = msg.readInt();

            for (int i = 0; i < statLineCount2; i++) {
               intValue = msg.readInt();
               Integer[] statValues4 = new Integer[msg.readByte()];

               for (int j = 0; j < statValues4.length; j++) {
                  statValues4[j] = msg.readInt();
               }

               GameState.wardrobeData.skillDescriptions4.add(new StatLine(StatType.getById(intValue), statValues4, null));
            }

            StatLine.mergeStatLines(GameState.wardrobeData.skillDescriptions4);
            this.readOptionalBackwearInfo(msg);
            this.loginScreen.debugScrollPane.showWindow(WindowType.INFO_PLAYER_POPUP);
            return;
         case 6:
            int alivePlayerId = msg.readInt();
            boolean alive = msg.readBoolean();
            PlayerEntity alivePlayer;
            if ((alivePlayer = GameState.findPlayerById(alivePlayerId)) == null) {
               return;
            }

            alivePlayer.POOL2.isAlive = alive;
            if (alivePlayer.POOL2.isAlive && alivePlayer.isLocalPlayer()) {
               this.loginScreen.cameraTarget.set(alivePlayer.getPosition().cpy().add(0.0F, alivePlayer.getHeight() / 2.0F));
            }

            return;
         case 7:
            int renamedPlayerId = msg.readInt();
            String newName = msg.readString();
            PlayerEntity renamedPlayer;
            if ((renamedPlayer = GameState.findPlayerById(renamedPlayerId)) == null) {
               return;
            } else {
               renamedPlayer.POOL2.characterName = newName;
            }
            return;
         case 8:
            int healedPlayerId = msg.readInt();
            long healedHp = msg.readLong();
            PlayerEntity healedPlayer;
            if ((healedPlayer = GameState.findPlayerById(healedPlayerId)) != null) {
               healedPlayer.POOL2.currentHp = Math.max(0L, healedHp);
            }
      }
   }

   public final void handleFriendOrGuildList(IMessage msg) {
      if (PlayerEntity.getInstance() != null) {
         switch (msg.readByte()) {
            case 0:
               Array.ArrayIterator inboxIt = GameState.mailbox.inboxMails.iterator();

               while (inboxIt.hasNext()) {
                  ((MailMessage)inboxIt.next()).clear();
               }

               GameState.mailbox.inboxMails.clear();
               byte inboxCount = msg.readByte();

               for (int i = 0; i < inboxCount; i++) {
                  int mailId = msg.readInt();
                  String senderName = msg.readString();
                  String receiverName = msg.readString();
                  String subject = msg.readString();
                  String content = msg.readString();
                  long sendTime = msg.readLong();
                  boolean isRead = msg.readBoolean();
                  MailMessage mail;
                  MailMessage newMail = mail = new MailMessage(mailId, senderName, receiverName, subject, content, sendTime, isRead);
                  newMail.boxType = 0;
                  mail.attachmentClaimed = msg.readBoolean();
                  byte attachmentCount = msg.readByte();

                  for (int j = 0; j < attachmentCount; j++) {
                     mail.attachments.add(this.readItemFromMessage(j, null, msg));
                  }

                  GameState.mailbox.inboxMails.add(mail);
               }

               this.loginScreen.debugScrollPane.refreshHudState();
               return;
            case 1:
               Array.ArrayIterator sentIt = GameState.mailbox.sentMails.iterator();

               while (sentIt.hasNext()) {
                  ((MailMessage)sentIt.next()).clear();
               }

               GameState.mailbox.sentMails.clear();
               byte sentCount = msg.readByte();

               for (int i = 0; i < sentCount; i++) {
                  int sentMailId = msg.readInt();
                  String sentSender = msg.readString();
                  String sentReceiver = msg.readString();
                  String sentSubject = msg.readString();
                  String sentContent = msg.readString();
                  long sentTime = msg.readLong();
                  MailMessage sentMail;
                  (sentMail = new MailMessage(sentMailId, sentSender, sentReceiver, sentSubject, sentContent, sentTime, true)).boxType = 1;
                  byte sentAttachmentCount = msg.readByte();

                  for (int j = 0; j < sentAttachmentCount; j++) {
                     sentMail.attachments.add(this.readItemFromMessage(j, null, msg));
                  }

                  GameState.mailbox.sentMails.add(sentMail);
               }

               this.loginScreen.debugScrollPane.refreshHudState();
         }
      }
   }

   public final void handlePlayerSpawn(IMessage msg) {
      int playerId = msg.readInt();
      float x = msg.readFloat() / 100.0F;
      float y = msg.readFloat() / 100.0F;
      boolean facingLeft = msg.readBoolean();
      if (PlayerEntity.getInstance() == null || PlayerEntity.getInstance().getId() != playerId) {
         PlayerEntity player;
         (player = PlayerFactory.obtainPlayer(playerId, x, y)).setDirection(facingLeft ? Direction.LEFT : Direction.RIGHT);
         readPlayerStats(player.POOL2, msg);
         player.refreshTitleAura();
         if (player.isAlive()) {
            player.setAnimationState(AnimationState.IDLE);
         } else {
            player.setAnimationState(AnimationState.DIE);
         }

         GameState.nearbyPlayers.add(player);
      }
   }

   public final void handleMapItemUpdate(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            int itemCount = msg.readInt();

            for (int i = 0; i < itemCount; i++) {
               int mapItemId = msg.readInt();
               int templateId = msg.readInt();
               float itemX = msg.readFloat() / 100.0F;
               float itemY = msg.readFloat() / 100.0F;
               MapItemEntity mapItem;
               (mapItem = MapItemFactory.obtainMapItem(mapItemId, itemX, itemY)).POOL2.itemTemplate = ItemTemplate.getById(templateId);
               GameState.mapItems.add(mapItem);
            }

            return;
         case 1:
            int singleItemId = msg.readInt();
            int singleTemplateId = msg.readInt();
            float singleX = msg.readFloat() / 100.0F;
            float singleY = msg.readFloat() / 100.0F;
            MapItemEntity singleMapItem;
            (singleMapItem = MapItemFactory.obtainMapItem(singleItemId, singleX, singleY)).POOL2.itemTemplate = ItemTemplate.getById(singleTemplateId);
            GameState.mapItems.add(singleMapItem);
            return;
         case 2:
            MapItemEntity removedItem;
            if ((removedItem = GameState.findMapItemById(msg.readInt())) == null) {
               return;
            }

            MapItemFactory.freeMapItem(removedItem);
            GameState.mapItems.removeValue(removedItem, true);
            return;
         case 3:
            int pickerPlayerId = msg.readInt();
            int pickedItemId = msg.readInt();
            PlayerEntity picker = GameState.findPlayerById(pickerPlayerId);
            MapItemEntity pickedItem = GameState.findMapItemById(pickedItemId);
            if (picker == null || pickedItem == null) {
               return;
            } else {
               if (pickedItem.POOL != null) {
                  pickedItem.POOL.startPull(picker);
               } else {
                  Gdx.app.postRunnable(new PickupItemTask(this, pickedItem, picker));
               }

               AudioManager.playSound("pick", picker.getCenterPosition());
            }
      }
   }

   public final void handleMapDecorations(IMessage msg) {
      this.parseMapDecorationsLayer(msg, 0.0F, 1.0F);
      this.parseMapDecorationsLayer(msg, 1000000.0F, 1.0F);
      // Reserve one render-order slot between front-layer objects so runtime effects can
      // be placed between water and the trees that follow it in the map's decoration list.
      this.parseMapDecorationsLayer(msg, 9000000.0F, 2.0F);
   }

   public void parseMapDecorationsLayer(IMessage msg, float baseRenderOrder, float renderOrderStep) {
      int decorationCount = msg.readInt();

      for (int i = 0; i < decorationCount; i++) {
         float renderOrder = baseRenderOrder + (float)i * renderOrderStep;
         switch (msg.readByte()) {
            case 0:
               String texturePath = msg.readString();
               float texX = msg.readFloat() / 100.0F;
               float texY = msg.readFloat() / 100.0F;
               float texWidth = msg.readFloat();
               float texHeight = msg.readFloat();
               float texAnchorX = msg.readFloat();
               float texAnchorY = msg.readFloat();
               boolean texFlipX = msg.readBoolean();
               boolean texFlipY = msg.readBoolean();
               boolean texTiledX = msg.readBoolean();
               boolean texTiledY = msg.readBoolean();
               float texR = msg.readFloat();
               float texG = msg.readFloat();
               float texB = msg.readFloat();
               float texA = msg.readFloat();
               this.loginScreen
                  .editorViewport
                  .target
                  .add(
                     MapDecorationObject.POOL2
                        .obtain()
                        .setupTexture(
                           texturePath, texX, texY, texWidth, texHeight, texAnchorX, texAnchorY, texFlipX, texFlipY, texTiledX, texTiledY, texR, texG, texB, texA, renderOrder
                        )
                  );
               break;
            case 1:
               int animationId = msg.readInt();
               float tileX = msg.readFloat() / 100.0F;
               float tileY = msg.readFloat() / 100.0F;
               float tileWidth = msg.readFloat();
               float tileHeight = msg.readFloat();
               float tileAnchorX = msg.readFloat();
               float tileAnchorY = msg.readFloat();
               boolean tileFlipX = msg.readBoolean();
               boolean tileFlipY = msg.readBoolean();
               boolean tileTiledX = msg.readBoolean();
               boolean tileTiledY = msg.readBoolean();
               float tileR = msg.readFloat();
               float tileG = msg.readFloat();
               float tileB = msg.readFloat();
               float tileA = msg.readFloat();
               this.loginScreen
                  .editorViewport
                  .target
                  .add(
                     MapDecorationObject.POOL2
                        .obtain()
                        .setupAnimatedTile(
                           animationId, tileX, tileY, tileWidth, tileHeight, tileAnchorX, tileAnchorY, tileFlipX, tileFlipY, tileTiledX, tileTiledY, tileR, tileG, tileB, tileA, renderOrder
                        )
                  );
               break;
            case 2:
               ParticleEffect[] effects = new ParticleEffect[msg.readByte()];

               for (int j = 0; j < effects.length; j++) {
                  effects[j] = new ParticleEffect(ParticleEffect.getById(msg.readInt()));
                  effects[j].setPosition(msg.readFloat() / 100.0F, msg.readFloat() / 100.0F);
                  effects[j].setScale(msg.readFloat() / 100.0F);
                  effects[j].setFlipX(msg.readBoolean());
                  effects[j].setParallax(msg.readFloat(), msg.readFloat());
               }

               this.loginScreen.editorViewport.target.add(MapDecorationObject.POOL2.obtain().setupParticle(effects, renderOrder));
               break;
            case 3:
               String skeletonName = msg.readString();
               float skelScale = msg.readFloat();
               String animName = msg.readString();
               float skelScaleX = msg.readFloat();
               float skelScaleY = msg.readFloat();
               boolean loop = msg.readBoolean();
               float timeScale = msg.readFloat();
               byte skinCount;
               String[] skinNames = new String[skinCount = msg.readByte()];

               for (int j = 0; j < skinCount; j++) {
                  skinNames[j] = msg.readString();
               }

               float skelX = msg.readFloat() / 100.0F;
               float skelY = msg.readFloat() / 100.0F;
               float skelAnchorX = msg.readFloat();
               float skelAnchorY = msg.readFloat();
               this.loginScreen
                  .editorViewport
                  .target
                  .add(
                     MapDecorationObject.POOL2
                        .obtain()
                        .setupSkeleton(skeletonName, skelScale, animName, skelScaleX, skelScaleY, loop, timeScale, skinNames, skelX, skelY, skelAnchorX, skelAnchorY, renderOrder)
                  );
         }
      }
   }

   public final void handleLoadGameData(IMessage msg) {
      String url = msg.readString();
      System.out.println("handleLoadGameData URL: " + url);
      TaskScheduler.schedule(new LoadGameDataTask(this, url), 0L, TimeUnit.MILLISECONDS);
   }

   public final void handleLoadMapData(IMessage msg) {
      String url = msg.readString();
      System.out.println("handleLoadMapData URL: " + url);
      TaskScheduler.schedule(new LoadMapDataTask(this, url), 0L, TimeUnit.MILLISECONDS);
   }

   public final void handleLoadItemData(IMessage msg) {
      String url = msg.readString();
      System.out.println("handleLoadItemData URL: " + url);
      TaskScheduler.schedule(new LoadItemDataTask(this, url), 0L, TimeUnit.MILLISECONDS);
   }

   public final void handleLoadSkillData(IMessage msg) {
      String url = msg.readString();
      System.out.println("handleLoadSkillData URL: " + url);
      TaskScheduler.schedule(new LoadSkillDataTask(this, url), 0L, TimeUnit.MILLISECONDS);
   }

   public final void handleCheckResources(IMessage msg) {
      String baseUrl = msg.readString();
      String version = msg.readString();
      System.out.println("handleCheckResources: baseUrl = " + baseUrl + ", version = " + version);
      TaskScheduler.schedule(new CheckResourceUpdatesTask(this, baseUrl, version), 0L, TimeUnit.MILLISECONDS);
   }

   public final void handleNpcUpdate(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            byte npcCount = msg.readByte();

            for (int i = 0; i < npcCount; i++) {
               int npcId = msg.readInt();
               float npcX = msg.readFloat() / 100.0F;
               float npcY = msg.readFloat() / 100.0F;
               NpcEntity npc;
               NpcState npcState = (npc = NpcFactory.obtainNpc(npcId, npcX, npcY)).POOL2;
               NpcTemplate npcTemplate = NpcTemplate.getTemplate(msg.readInt());
               boolean shadowEnabled = msg.readBoolean();
               byte direction = msg.readByte();
               if (npcState != null) {
                  npcState.template = npcTemplate;
                  npcState.shadowEnabled = shadowEnabled;
                  npcState.direction = direction;
               }

               GameState.nearbyNpcs.add(npc);
            }

            return;
         case 1:
            byte npcType = msg.readByte();
            int npcId = msg.readInt();
            byte useDialogV2 = msg.readByte();
            String npcName = msg.readString();
            String portraitPath = msg.readString();
            String dialogText = msg.readString();
            String[] choices = new String[msg.readByte()];

            for (int j = 0; j < choices.length; j++) {
               choices[j] = msg.readString();
            }

            if (useDialogV2 != 0) {
               this.loginScreen.debugScrollPane.findDialog(NpcDialogPopupV2.class).showNpcDialog(npcType, npcId, npcName, portraitPath, dialogText, choices);
               this.loginScreen.debugScrollPane.showWindow(WindowType.MENU_NPC_POPUP_V2);
               return;
            }

            this.loginScreen.debugScrollPane.findDialog(NpcSelectionPopup.class).showWithOptions(npcType, npcId, npcName, dialogText, choices);
            this.loginScreen.debugScrollPane.showWindow(WindowType.MENU_NPC_POPUP);
            break;
         case 2:
            int chatNpcId = msg.readInt();
            String chatText = msg.readString();
            NpcEntity chatNpc;
            if ((chatNpc = GameState.findNpcById(chatNpcId)) == null) {
               return;
            }

            chatNpc.POOL.showChatMessage(chatText);
      }
   }

   public final void handleMobUpdate(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            byte mobCount = msg.readByte();

            for (int i = 0; i < mobCount; i++) {
               int mobId = msg.readInt();
               float mobX = msg.readFloat() / 100.0F;
               float mobY = msg.readFloat() / 100.0F;
               MobEntity mob;
               readMobStats((mob = MobFactory.obtainMob(mobId, mobX, mobY)).POOL2, msg);
               if (mob.isDead()) {
                  mob.setActive(false);
               }

               GameState.nearbyMobs.add(mob);
            }

            return;
         case 1:
            int attackerMobId = msg.readInt();
            int targetPlayerId = msg.readInt();
            long targetCurrentHp = msg.readLong();
            long damage = msg.readLong();
            boolean isHeal = msg.readBoolean();
            MobEntity attackerMob = GameState.findMobById(attackerMobId);
            PlayerEntity targetPlayer;
            if ((targetPlayer = GameState.findPlayerById(targetPlayerId)) != null) {
               targetPlayer.onHit(attackerMob, targetCurrentHp, damage, isHeal);
            }

            if (attackerMob == null || targetPlayer == null) {
               return;
            }

            if (!(attackerMob.distanceTo(targetPlayer) >= 1.5F)) {
               attackerMob.setAnimationState(AnimationState.MOB_ATTACK_2);
               attackerMob.skeleton.meleeAttackTarget = targetPlayer;
               attackerMob.faceTowards(targetPlayer.getPosition());
               return;
            }

            attackerMob.setAnimationState(AnimationState.MOB_ATTACK_1);
            attackerMob.faceTowards(targetPlayer.getPosition());
            attackerMob.skeleton.rangedAttackTarget = targetPlayer;
            break;
         case 2:
            MobEntity respawnedMob;
            if ((respawnedMob = GameState.findMobById(msg.readInt())) == null) {
               return;
            }

            readMobStats(respawnedMob.POOL2, msg);
            respawnedMob.setActive(true);
            respawnedMob.setWorldPosition(respawnedMob.POOL2.spawnPosition);
            if (respawnedMob.spawnEffect != null) { // revive packet arrived before onAwake (mob added in the same frame)
               respawnedMob.spawnEffect.playSpawnAnimation();
            }
            return;
         case 3:
            int hitMobId = msg.readInt();
            long hitDamage = msg.readLong();
            long hitCurrentHp = msg.readLong();
            MobEntity hitMob;
            if ((hitMob = GameState.findMobById(hitMobId)) == null) {
               return;
            }

            hitMob.onHit(null, hitCurrentHp, hitDamage, false);
            return;
         case 4:
            int spawnMobId = msg.readInt();
            float spawnX = msg.readFloat() / 100.0F;
            float spawnY = msg.readFloat() / 100.0F;
            MobEntity spawnedMob;
            readMobStats((spawnedMob = MobFactory.obtainMob(spawnMobId, spawnX, spawnY)).POOL2, msg);
            if (spawnedMob.isDead()) {
               spawnedMob.setActive(false);
            }

            GameState.nearbyMobs.add(spawnedMob);
            return;
         case 5:
            int skillMobId = msg.readInt();
            byte skillDirection = msg.readByte();
            MobEntity skillMob;
            if ((skillMob = GameState.findMobById(skillMobId)) != null) {
               skillMob.setDirection(skillDirection < 0 ? Direction.LEFT : Direction.RIGHT);
               skillMob.setAnimationState(AnimationState.MOB_SKILL_3);
            }

            return;
         case 6:
            MobEntity skill4Mob;
            if ((skill4Mob = GameState.findMobById(msg.readInt())) != null) {
               skill4Mob.setAnimationState(AnimationState.MOB_SKILL_4);
            }

            return;
         case 7:
            MobEntity deactivatedMob;
            if ((deactivatedMob = GameState.findMobById(msg.readInt())) != null) {
               deactivatedMob.setActive(false);
            }
      }
   }

   public final void handleCombatAction(IMessage msg) {
      label493:
      switch (msg.readByte()) {
         case 0:
            PlayerEntity player;
            if ((player = GameState.findPlayerById((msg = msg).readInt())) == null) {
               return;
            }

            switch (msg.readInt()) {
               case 1:
                  byte meleeTargetType = msg.readByte();
                  int meleeTargetId = msg.readInt();
                  long meleeHp = msg.readLong();
                  long meleeDamage = msg.readLong();
                  boolean meleeIsHeal = msg.readBoolean();
                  Object meleeTarget;
                  if ((meleeTarget = meleeTargetType == 0 ? GameState.findMobById(meleeTargetId) : GameState.findPlayerById(meleeTargetId)) != null) {
                     ((WorldEntity)meleeTarget).onHit(player, meleeHp, meleeDamage, meleeIsHeal);
                  }

                  player.setAnimationState(AnimationState.MELEE_NORMAL_ATTACK);
                  player.skeleton.currentAttackTarget = (WorldEntity)meleeTarget;
                  return;
               case 2:
                  byte axeTargetType = msg.readByte();
                  int axeTargetId = msg.readInt();
                  long axeHp = msg.readLong();
                  long axeDamage = msg.readLong();
                  boolean axeIsHeal = msg.readBoolean();
                  Object axeTarget;
                  if ((axeTarget = axeTargetType == 0 ? GameState.findMobById(axeTargetId) : GameState.findPlayerById(axeTargetId)) != null) {
                     ((WorldEntity)axeTarget).onHit(player, axeHp, axeDamage, axeIsHeal);
                  }

                  player.setAnimationState(AnimationState.AXE_NORMAL_ATTACK);
                  player.skeleton.currentAttackTarget = (WorldEntity)axeTarget;
                  return;
               case 3:
                  byte axeS1TargetType = msg.readByte();
                  int axeS1TargetId = msg.readInt();
                  long axeS1Hp = msg.readLong();
                  long axeS1Damage = msg.readLong();
                  boolean axeS1IsHeal = msg.readBoolean();
                  Object axeS1Target;
                  if ((axeS1Target = axeS1TargetType == 0 ? GameState.findMobById(axeS1TargetId) : GameState.findPlayerById(axeS1TargetId)) != null) {
                     ((WorldEntity)axeS1Target).onHit(player, axeS1Hp, axeS1Damage, axeS1IsHeal);
                  }

                  player.setAnimationState(AnimationState.AXE_SKILL_1);
                  player.skeleton.skill3AoeFlag = true;
                  player.skeleton.currentAttackTarget = (WorldEntity)axeS1Target;
                  return;
               case 4:
                  boolean axeS2Start = msg.readBoolean();
                  player.setAnimationState(axeS2Start ? AnimationState.AXE_SKILL_2_START : AnimationState.AXE_SKILL_2_END);
                  return;
               case 5:
                  byte axeS3MobCount = msg.readByte();

                  for (int i = 0; i < axeS3MobCount; i++) {
                     int axeS3MobId = msg.readInt();
                     long axeS3MobHp = msg.readLong();
                     long axeS3MobDamage = msg.readLong();
                     boolean axeS3MobIsHeal = msg.readBoolean();
                     MobEntity axeS3Mob;
                     if ((axeS3Mob = GameState.findMobById(axeS3MobId)) != null) {
                        axeS3Mob.onHit(player, axeS3MobHp, axeS3MobDamage, axeS3MobIsHeal);
                     }

                     player.skeleton.aoeHitTargets.add(axeS3Mob);
                  }

                  byte axeS3PlayerCount = msg.readByte();

                  for (int i = 0; i < axeS3PlayerCount; i++) {
                     int axeS3PlayerId = msg.readInt();
                     long axeS3PlayerHp = msg.readLong();
                     long axeS3PlayerDamage = msg.readLong();
                     boolean axeS3PlayerIsHeal = msg.readBoolean();
                     PlayerEntity axeS3Player;
                     if ((axeS3Player = GameState.findPlayerById(axeS3PlayerId)) != null) {
                        axeS3Player.onHit(player, axeS3PlayerHp, axeS3PlayerDamage, axeS3PlayerIsHeal);
                     }

                     player.skeleton.aoeHitTargets.add(axeS3Player);
                  }

                  player.setAnimationState(AnimationState.AXE_SKILL_3);
                  return;
               case 6:
                  byte axeS4TargetType = msg.readByte();
                  int axeS4TargetId = msg.readInt();
                  long axeS4Hp = msg.readLong();
                  long axeS4Damage = msg.readLong();
                  boolean axeS4IsHeal = msg.readBoolean();
                  Object axeS4Target;
                  if ((axeS4Target = axeS4TargetType == 0 ? GameState.findMobById(axeS4TargetId) : GameState.findPlayerById(axeS4TargetId)) != null) {
                     ((WorldEntity)axeS4Target).onHit(player, axeS4Hp, axeS4Damage, axeS4IsHeal);
                  }

                  player.skeleton.skill6AttackTarget = (WorldEntity)axeS4Target;
                  player.setAnimationState(AnimationState.AXE_SKILL_4);
                  return;
               case 7:
                  if (msg.readBoolean()) {
                     player.setAnimationState(AnimationState.AXE_SKILL_5_START);
                     return;
                  }

                  byte axeS5TargetType = msg.readByte();
                  int axeS5TargetId = msg.readInt();
                  long axeS5Hp = msg.readLong();
                  long axeS5Damage = msg.readLong();
                  boolean axeS5IsHeal = msg.readBoolean();
                  Object axeS5Target;
                  if ((axeS5Target = axeS5TargetType == 0 ? GameState.findMobById(axeS5TargetId) : GameState.findPlayerById(axeS5TargetId)) != null) {
                     ((WorldEntity)axeS5Target).onHit(player, axeS5Hp, axeS5Damage, axeS5IsHeal);
                  }

                  player.setAnimationState(AnimationState.AXE_SKILL_5_END);
                  player.skeleton.skill7PierceFlag = true;
                  player.skeleton.currentAttackTarget = (WorldEntity)axeS5Target;
                  return;
               case 8:
                  byte bowTargetType = msg.readByte();
                  int bowTargetId = msg.readInt();
                  long bowHp = msg.readLong();
                  long bowDamage = msg.readLong();
                  boolean bowIsHeal = msg.readBoolean();
                  Object bowTarget;
                  if ((bowTarget = bowTargetType == 0 ? GameState.findMobById(bowTargetId) : GameState.findPlayerById(bowTargetId)) != null) {
                     ((WorldEntity)bowTarget).onHit(player, bowHp, bowDamage, bowIsHeal);
                  }

                  player.skeleton.skill8AttackTarget = (WorldEntity)bowTarget;
                  player.setAnimationState(AnimationState.BOW_NORMAL_ATTACK);
                  return;
               case 9:
                  byte bowS1TargetType = msg.readByte();
                  int bowS1TargetId = msg.readInt();
                  long bowS1Hp = msg.readLong();
                  long bowS1Damage = msg.readLong();
                  boolean bowS1IsHeal = msg.readBoolean();
                  Object bowS1Target;
                  if ((bowS1Target = bowS1TargetType == 0 ? GameState.findMobById(bowS1TargetId) : GameState.findPlayerById(bowS1TargetId)) != null) {
                     ((WorldEntity)bowS1Target).onHit(player, bowS1Hp, bowS1Damage, bowS1IsHeal);
                  }

                  player.skeleton.skill9HitTargets.add((GameObject)bowS1Target);
                  player.setAnimationState(AnimationState.BOW_SKILL_1);
                  return;
               case 10:
                  if (msg.readBoolean()) {
                     player.setAnimationState(AnimationState.BOW_SKILL_2);
                     return;
                  }

                  player.secondaryWeaponEffect.equipBow();
                  player.secondaryWeaponEffect.setEnabled(true);
                  String bowS2AnimName = "skill2_3";
                  player.secondaryWeaponEffect.playAnimation(bowS2AnimName, false, 1.0F);
                  return;
               case 11:
                  byte bowS3TargetType = msg.readByte();
                  int bowS3TargetId = msg.readInt();
                  long bowS3Hp = msg.readLong();
                  long bowS3Damage = msg.readLong();
                  boolean bowS3IsHeal = msg.readBoolean();
                  Object bowS3Target;
                  if ((bowS3Target = bowS3TargetType == 0 ? GameState.findMobById(bowS3TargetId) : GameState.findPlayerById(bowS3TargetId)) != null) {
                     ((WorldEntity)bowS3Target).onHit(player, bowS3Hp, bowS3Damage, bowS3IsHeal);
                  }

                  player.skeleton.skill11MainTarget = (WorldEntity)bowS3Target;
                  byte bowS3MobCount = msg.readByte();

                  for (int i = 0; i < bowS3MobCount; i++) {
                     bowS3TargetId = msg.readInt();
                     bowS3Hp = msg.readLong();
                     bowS3Damage = msg.readLong();
                     bowS3IsHeal = msg.readBoolean();
                     MobEntity bowS3Mob;
                     if ((bowS3Mob = GameState.findMobById(bowS3TargetId)) != null) {
                        bowS3Mob.onHit(player, bowS3Hp, bowS3Damage, bowS3IsHeal);
                        player.skeleton.skill11AoeTargets.add(bowS3Mob);
                     }
                  }

                  bowS3TargetType = msg.readByte();

                  for (int i = 0; i < bowS3TargetType; i++) {
                     int bowS3PlayerId = msg.readInt();
                     bowS3Hp = msg.readLong();
                     bowS3Damage = msg.readLong();
                     bowS3IsHeal = msg.readBoolean();
                     PlayerEntity bowS3Player;
                     if ((bowS3Player = GameState.findPlayerById(bowS3PlayerId)) != null) {
                        bowS3Player.onHit(player, bowS3Hp, bowS3Damage, bowS3IsHeal);
                        player.skeleton.skill11AoeTargets.add(bowS3Player);
                     }
                  }

                  player.setAnimationState(AnimationState.BOW_SKILL_3);
                  return;
               case 12:
                  byte bowS4TargetType = msg.readByte();
                  int bowS4TargetId = msg.readInt();
                  long bowS4Hp = msg.readLong();
                  long bowS4Damage = msg.readLong();
                  boolean bowS4IsHeal = msg.readBoolean();
                  Object bowS4Target;
                  if ((bowS4Target = bowS4TargetType == 0 ? GameState.findMobById(bowS4TargetId) : GameState.findPlayerById(bowS4TargetId)) != null) {
                     ((WorldEntity)bowS4Target).onHit(player, bowS4Hp, bowS4Damage, bowS4IsHeal);
                  }

                  player.skeleton.skill12AttackTarget = (WorldEntity)bowS4Target;
                  player.setAnimationState(AnimationState.BOW_SKILL_4);
                  return;
               case 13:
                  if (msg.readBoolean()) {
                     player.setAnimationState(AnimationState.BOW_SKILL_5_START);
                     return;
                  }

                  byte bowS5TargetType = msg.readByte();
                  int bowS5TargetId = msg.readInt();
                  long bowS5Hp = msg.readLong();
                  long bowS5Damage = msg.readLong();
                  boolean bowS5IsHeal = msg.readBoolean();
                  Object bowS5Target;
                  if ((bowS5Target = bowS5TargetType == 0 ? GameState.findMobById(bowS5TargetId) : GameState.findPlayerById(bowS5TargetId)) != null) {
                     ((WorldEntity)bowS5Target).onHit(player, bowS5Hp, bowS5Damage, bowS5IsHeal);
                  }

                  player.skeleton.skill13AttackTarget = (WorldEntity)bowS5Target;
                  player.setAnimationState(AnimationState.BOW_SKILL_5_END);
                  return;
               case 14:
                  byte slingTargetType = msg.readByte();
                  int slingTargetId = msg.readInt();
                  long slingHp = msg.readLong();
                  long slingDamage = msg.readLong();
                  boolean slingIsHeal = msg.readBoolean();
                  Object slingTarget;
                  if ((slingTarget = slingTargetType == 0 ? GameState.findMobById(slingTargetId) : GameState.findPlayerById(slingTargetId)) != null) {
                     ((WorldEntity)slingTarget).onHit(player, slingHp, slingDamage, slingIsHeal);
                  }

                  player.skeleton.skill14AttackTarget = (WorldEntity)slingTarget;
                  player.setAnimationState(AnimationState.SLINGSHOT_NORMAL_ATTACK);
                  return;
               case 15:
                  byte slingS1TargetType = msg.readByte();
                  int slingS1TargetId = msg.readInt();
                  long slingS1Hp = msg.readLong();
                  long slingS1Damage = msg.readLong();
                  boolean slingS1IsHeal = msg.readBoolean();
                  Object slingS1Target;
                  if ((slingS1Target = slingS1TargetType == 0 ? GameState.findMobById(slingS1TargetId) : GameState.findPlayerById(slingS1TargetId)) != null) {
                     ((WorldEntity)slingS1Target).onHit(player, slingS1Hp, slingS1Damage, slingS1IsHeal);
                  }

                  player.skeleton.skill15AoeTargets.add((GameObject)slingS1Target);
                  player.setAnimationState(AnimationState.SLINGSHOT_SKILL_1);
                  return;
               case 16:
                  if (msg.readBoolean()) {
                     player.setAnimationState(AnimationState.SLINGSHOT_SKILL_2);
                     return;
                  }

                  WeaponEquipEffect slingS2Effect = player.secondaryWeaponEffect;
                  player.secondaryWeaponEffect.setEnabled(false);
                  return;
               case 17:
                  byte slingS3TargetType = msg.readByte();
                  int slingS3TargetId = msg.readInt();
                  long slingS3Hp = msg.readLong();
                  long slingS3Damage = msg.readLong();
                  boolean slingS3IsHeal = msg.readBoolean();
                  Object slingS3Target;
                  if ((slingS3Target = slingS3TargetType == 0 ? GameState.findMobById(slingS3TargetId) : GameState.findPlayerById(slingS3TargetId)) != null) {
                     ((WorldEntity)slingS3Target).onHit(player, slingS3Hp, slingS3Damage, slingS3IsHeal);
                  }

                  player.skeleton.skill17AttackTarget = (WorldEntity)slingS3Target;
                  byte slingS3MobCount = msg.readByte();

                  for (int i = 0; i < slingS3MobCount; i++) {
                     slingS3TargetId = msg.readInt();
                     slingS3Hp = msg.readLong();
                     slingS3Damage = msg.readLong();
                     slingS3IsHeal = msg.readBoolean();
                     MobEntity slingS3Mob;
                     if ((slingS3Mob = GameState.findMobById(slingS3TargetId)) != null) {
                        slingS3Mob.onHit(player, slingS3Hp, slingS3Damage, slingS3IsHeal);
                     }
                  }

                  slingS3TargetType = msg.readByte();

                  for (int i = 0; i < slingS3TargetType; i++) {
                     int slingS3PlayerId = msg.readInt();
                     slingS3Hp = msg.readLong();
                     slingS3Damage = msg.readLong();
                     slingS3IsHeal = msg.readBoolean();
                     PlayerEntity slingS3Player;
                     if ((slingS3Player = GameState.findPlayerById(slingS3PlayerId)) != null) {
                        slingS3Player.onHit(player, slingS3Hp, slingS3Damage, slingS3IsHeal);
                     }
                  }

                  player.setAnimationState(AnimationState.SLINGSHOT_SKILL_3);
                  return;
               case 18:
                  byte slingS4TargetType = msg.readByte();
                  int slingS4TargetId = msg.readInt();
                  long slingS4Hp = msg.readLong();
                  long slingS4Damage = msg.readLong();
                  boolean slingS4IsHeal = msg.readBoolean();
                  Object slingS4Target;
                  if ((slingS4Target = slingS4TargetType == 0 ? GameState.findMobById(slingS4TargetId) : GameState.findPlayerById(slingS4TargetId)) != null) {
                     ((WorldEntity)slingS4Target).onHit(player, slingS4Hp, slingS4Damage, slingS4IsHeal);
                  }

                  player.skeleton.skill18AttackTarget = (WorldEntity)slingS4Target;
                  player.setAnimationState(AnimationState.SLINGSHOT_SKILL_4);
                  return;
               case 19:
                  if (msg.readBoolean()) {
                     player.setAnimationState(AnimationState.SLINGSHOT_SKILL_5_START);
                     return;
                  }

                  byte slingS5TargetType = msg.readByte();
                  int slingS5TargetId = msg.readInt();
                  long slingS5Hp = msg.readLong();
                  long slingS5Damage = msg.readLong();
                  boolean slingS5IsHeal = msg.readBoolean();
                  Object slingS5Target;
                  if ((slingS5Target = slingS5TargetType == 0 ? GameState.findMobById(slingS5TargetId) : GameState.findPlayerById(slingS5TargetId)) != null) {
                     ((WorldEntity)slingS5Target).onHit(player, slingS5Hp, slingS5Damage, slingS5IsHeal);
                  }

                  player.skeleton.skill19AttackTarget = (WorldEntity)slingS5Target;
                  player.setAnimationState(AnimationState.SLINGSHOT_SKILL_5_END);
                  return;
               case 20:
                  byte spearTargetType = msg.readByte();
                  int spearTargetId = msg.readInt();
                  long spearHp = msg.readLong();
                  long spearDamage = msg.readLong();
                  boolean spearIsHeal = msg.readBoolean();
                  Object spearTarget;
                  if ((spearTarget = spearTargetType == 0 ? GameState.findMobById(spearTargetId) : GameState.findPlayerById(spearTargetId)) != null) {
                     ((WorldEntity)spearTarget).onHit(player, spearHp, spearDamage, spearIsHeal);
                  }

                  player.setAnimationState(AnimationState.SPEAR_NORMAL_ATTACK);
                  player.skeleton.currentAttackTarget = (WorldEntity)spearTarget;
                  return;
               case 21:
                  byte spearS1TargetType = msg.readByte();
                  int spearS1TargetId = msg.readInt();
                  long spearS1Hp = msg.readLong();
                  long spearS1Damage = msg.readLong();
                  boolean spearS1IsHeal = msg.readBoolean();
                  Object spearS1Target;
                  if ((spearS1Target = spearS1TargetType == 0 ? GameState.findMobById(spearS1TargetId) : GameState.findPlayerById(spearS1TargetId)) != null) {
                     ((WorldEntity)spearS1Target).onHit(player, spearS1Hp, spearS1Damage, spearS1IsHeal);
                  }

                  player.setAnimationState(AnimationState.SPEAR_SKILL_1);
                  player.skeleton.currentAttackTarget = (WorldEntity)spearS1Target;
                  return;
               case 22:
                  byte spearS2TargetType = msg.readByte();
                  int spearS2TargetId = msg.readInt();
                  long spearS2Hp = msg.readLong();
                  long spearS2Damage = msg.readLong();
                  boolean spearS2IsHeal = msg.readBoolean();
                  Object spearS2Target;
                  if ((spearS2Target = spearS2TargetType == 0 ? GameState.findMobById(spearS2TargetId) : GameState.findPlayerById(spearS2TargetId)) != null) {
                     ((WorldEntity)spearS2Target).onHit(player, spearS2Hp, spearS2Damage, spearS2IsHeal);
                  }

                  player.setAnimationState(AnimationState.SPEAR_SKILL_2);
                  player.skeleton.currentAttackTarget = (WorldEntity)spearS2Target;
                  return;
               case 23:
                  byte spearS3TargetType = msg.readByte();
                  int spearS3TargetId = msg.readInt();
                  long spearS3Hp = msg.readLong();
                  long spearS3Damage = msg.readLong();
                  boolean spearS3IsHeal = msg.readBoolean();
                  Object spearS3Target;
                  if ((spearS3Target = spearS3TargetType == 0 ? GameState.findMobById(spearS3TargetId) : GameState.findPlayerById(spearS3TargetId)) != null) {
                     ((WorldEntity)spearS3Target).onHit(player, spearS3Hp, spearS3Damage, spearS3IsHeal);
                  }

                  player.setAnimationState(AnimationState.SPEAR_SKILL_3);
                  player.skeleton.skill23Flag = true;
                  player.skeleton.currentAttackTarget = (WorldEntity)spearS3Target;
                  return;
               case 24:
                  byte spearS4TargetType = msg.readByte();
                  int spearS4TargetId = msg.readInt();
                  long spearS4Hp = msg.readLong();
                  long spearS4Damage = msg.readLong();
                  boolean spearS4IsHeal = msg.readBoolean();
                  Object spearS4Target;
                  if ((spearS4Target = spearS4TargetType == 0 ? GameState.findMobById(spearS4TargetId) : GameState.findPlayerById(spearS4TargetId)) != null) {
                     ((WorldEntity)spearS4Target).onHit(player, spearS4Hp, spearS4Damage, spearS4IsHeal);
                     SpearProjectileEffect.POOL2
                        .obtain()
                        .playAt(
                           ((GameObject)spearS4Target).getPosition().x,
                           ((GameObject)spearS4Target).getPosition().y,
                           player.POOL2.weaponTexture,
                           ((GameObject)spearS4Target).getRenderOrder() + 1.0F
                        );
                  }

                  player.setAnimationState(AnimationState.SPEAR_SKILL_4);
                  player.skeleton.currentAttackTarget = (WorldEntity)spearS4Target;
                  return;
               case 25:
                  if (msg.readBoolean()) {
                     player.setAnimationState(AnimationState.SPEAR_SKILL_5_START);
                     return;
                  }

                  byte spearS5TargetType = msg.readByte();
                  int spearS5TargetId = msg.readInt();
                  long spearS5Hp = msg.readLong();
                  long spearS5Damage = msg.readLong();
                  boolean spearS5IsHeal = msg.readBoolean();
                  Object spearS5Target;
                  if ((spearS5Target = spearS5TargetType == 0 ? GameState.findMobById(spearS5TargetId) : GameState.findPlayerById(spearS5TargetId)) != null) {
                     ((WorldEntity)spearS5Target).onHit(player, spearS5Hp, spearS5Damage, spearS5IsHeal);
                  }

                  player.skeleton.skill25AttackTarget = (WorldEntity)spearS5Target;
                  player.setAnimationState(AnimationState.SPEAR_SKILL_5_END);
                  return;
               case 26:
                  byte whipTargetType = msg.readByte();
                  int whipTargetId = msg.readInt();
                  long whipHp = msg.readLong();
                  long whipDamage = msg.readLong();
                  boolean whipIsHeal = msg.readBoolean();
                  Object whipTarget;
                  if ((whipTarget = whipTargetType == 0 ? GameState.findMobById(whipTargetId) : GameState.findPlayerById(whipTargetId)) != null) {
                     ((WorldEntity)whipTarget).onHit(player, whipHp, whipDamage, whipIsHeal);
                  }

                  player.setAnimationState(AnimationState.WHIP_NORMAL_ATTACK);
                  player.skeleton.currentAttackTarget = (WorldEntity)whipTarget;
                  return;
               case 27:
                  byte whipS1TargetType = msg.readByte();
                  int whipS1TargetId = msg.readInt();
                  long whipS1Hp = msg.readLong();
                  long whipS1Damage = msg.readLong();
                  boolean whipS1IsHeal = msg.readBoolean();
                  Object whipS1Target;
                  if ((whipS1Target = whipS1TargetType == 0 ? GameState.findMobById(whipS1TargetId) : GameState.findPlayerById(whipS1TargetId)) != null) {
                     ((WorldEntity)whipS1Target).onHit(player, whipS1Hp, whipS1Damage, whipS1IsHeal);
                  }

                  player.setAnimationState(AnimationState.WHIP_SKILL_1);
                  player.skeleton.currentAttackTarget = (WorldEntity)whipS1Target;
                  return;
               case 28:
                  float dashDuration = msg.readFloat();
                  float dashSpeed = msg.readFloat();
                  player.target.startDash(dashDuration, dashSpeed);
                  player.setAnimationState(AnimationState.WHIP_SKILL_2);
                  return;
               case 29:
                  byte whipS3TargetType = msg.readByte();
                  int whipS3TargetId = msg.readInt();
                  long whipS3Hp = msg.readLong();
                  long whipS3Damage = msg.readLong();
                  boolean whipS3IsHeal = msg.readBoolean();
                  Object whipS3Target;
                  if ((whipS3Target = whipS3TargetType == 0 ? GameState.findMobById(whipS3TargetId) : GameState.findPlayerById(whipS3TargetId)) != null) {
                     ((WorldEntity)whipS3Target).onHit(player, whipS3Hp, whipS3Damage, whipS3IsHeal);
                  }

                  player.setAnimationState(AnimationState.WHIP_SKILL_3);
                  player.skeleton.skill26Flag = true;
                  player.skeleton.currentAttackTarget = (WorldEntity)whipS3Target;
                  return;
               case 30:
                  byte whipS4TargetType = msg.readByte();
                  int whipS4TargetId = msg.readInt();
                  long whipS4Hp = msg.readLong();
                  long whipS4Damage = msg.readLong();
                  boolean whipS4IsHeal = msg.readBoolean();
                  Object whipS4Target;
                  if ((whipS4Target = whipS4TargetType == 0 ? GameState.findMobById(whipS4TargetId) : GameState.findPlayerById(whipS4TargetId)) != null) {
                     ((WorldEntity)whipS4Target).onHit(player, whipS4Hp, whipS4Damage, whipS4IsHeal);
                  }

                  player.setAnimationState(AnimationState.WHIP_SKILL_4);
                  player.skeleton.currentAttackTarget = (WorldEntity)whipS4Target;
                  return;
               case 31:
                  if (msg.readBoolean()) {
                     player.setAnimationState(AnimationState.WHIP_SKILL_5_START);
                     return;
                  }

                  byte whipS5TargetType = msg.readByte();
                  int whipS5TargetId = msg.readInt();
                  long whipS5Hp = msg.readLong();
                  long whipS5Damage = msg.readLong();
                  boolean whipS5IsHeal = msg.readBoolean();
                  Object whipS5Target;
                  if ((whipS5Target = whipS5TargetType == 0 ? GameState.findMobById(whipS5TargetId) : GameState.findPlayerById(whipS5TargetId)) != null) {
                     ((WorldEntity)whipS5Target).onHit(player, whipS5Hp, whipS5Damage, whipS5IsHeal);
                  }

                  player.setAnimationState(AnimationState.WHIP_SKILL_5_END);
                  player.skeleton.skill29Flag = true;
                  player.skeleton.currentAttackTarget = (WorldEntity)whipS5Target;
                  return;
               case 32:
                  byte stickS1TargetType = msg.readByte();
                  int stickS1TargetId = msg.readInt();
                  long stickS1Hp = msg.readLong();
                  long stickS1Damage = msg.readLong();
                  boolean stickS1IsHeal = msg.readBoolean();
                  Object stickS1Target;
                  if ((stickS1Target = stickS1TargetType == 0 ? GameState.findMobById(stickS1TargetId) : GameState.findPlayerById(stickS1TargetId)) != null) {
                     ((WorldEntity)stickS1Target).onHit(player, stickS1Hp, stickS1Damage, stickS1IsHeal);
                  }

                  player.setAnimationState(AnimationState.STICK_SKILL_1);
                  return;
               case 33:
                  byte stickS2Direction = msg.readByte();
                  player.setDirection(stickS2Direction == -1 ? Direction.LEFT : Direction.RIGHT);
                  player.setAnimationState(AnimationState.STICK_SKILL_2);
                  return;
               case 34:
                  player.setAnimationState(AnimationState.STICK_SKILL_3);
                  player.skeleton.skill28Flag = true;
                  return;
               case 35:
                  player.setAnimationState(AnimationState.STICK_SKILL_4);
                  return;
               case 36:
                  player.setAnimationState(AnimationState.STICK_SKILL_5);
                  return;
               case 37:
                  if (msg.readBoolean()) {
                     player.setAnimationState(AnimationState.STICK_SKILL_6);
                     return;
                  }

                  player.secondaryWeaponEffect.equipWaterShield();
                  player.secondaryWeaponEffect.setEnabled(true);
                  player.secondaryWeaponEffect
                     .playAnimation("skill6_3", false, 1.0F);
                  return;
               case 38:
                  player.setAnimationState(AnimationState.STICK_SKILL_7);
                  return;
               case 39:
                  player.setAnimationState(AnimationState.STICK_SKILL_8);
                  return;
               case 40:
                  if (msg.readBoolean()) {
                     byte stickS9TargetType = msg.readByte();
                     int stickS9TargetId = msg.readInt();
                     long stickS9Hp = msg.readLong();
                     long stickS9Damage = msg.readLong();
                     boolean stickS9IsHeal = msg.readBoolean();
                     Object stickS9Target;
                     if ((stickS9Target = stickS9TargetType == 0 ? GameState.findMobById(stickS9TargetId) : GameState.findPlayerById(stickS9TargetId)) != null) {
                        ((WorldEntity)stickS9Target).onHit(null, stickS9Hp, stickS9Damage, stickS9IsHeal);
                        String featherAnimName = "project";
                        FeatherProjectileEntity feather;
                        (feather = FeatherProjectileEntity.spawn(
                                 player.getCenterPosition().x
                                    + GameUtils.randomFloat(-player.getWidth() / 2.0F, player.getWidth() / 2.0F) * player.getDirection().vector.x,
                                 player.getCenterPosition().y + player.getHeight() / 2.0F
                              )
                              .setTarget((WorldEntity)stickS9Target))
                           .POOL2
                           .playAnimation(featherAnimName, true, 1.5F);
                        AudioManager.playSound("mob_attack_1", feather.getCenterPosition());
                     }

                     return;
                  }

                  byte stickS9Phase = msg.readByte();
                  player.setAnimationState(stickS9Phase == 0 ? AnimationState.STICK_SKILL_9_START : AnimationState.STICK_SKILL_9_END);
                  return;
               case 41:
                  byte stickS10TargetType = msg.readByte();
                  int stickS10TargetId = msg.readInt();
                  long stickS10Hp = msg.readLong();
                  long stickS10Damage = msg.readLong();
                  boolean stickS10IsHeal = msg.readBoolean();
                  Object stickS10Target;
                  if ((stickS10Target = stickS10TargetType == 0 ? GameState.findMobById(stickS10TargetId) : GameState.findPlayerById(stickS10TargetId)) != null) {
                     ((WorldEntity)stickS10Target).onHit(player, stickS10Hp, stickS10Damage, stickS10IsHeal);
                  }

                  player.skeleton.skill30AttackTarget = (WorldEntity)stickS10Target;
                  player.setAnimationState(AnimationState.STICK_SKILL_10);
                  return;
               case 42: // whip/skill6 (decompiled C3873 case 42): byte n mobs × (id, hp, dmg, crit), byte m players × (...)
                  byte whipS6MobCount = msg.readByte();

                  for (int i = 0; i < whipS6MobCount; i++) {
                     int whipS6MobId = msg.readInt();
                     long whipS6MobHp = msg.readLong();
                     long whipS6MobDamage = msg.readLong();
                     boolean whipS6MobCrit = msg.readBoolean();
                     MobEntity whipS6Mob;
                     if ((whipS6Mob = GameState.findMobById(whipS6MobId)) != null) {
                        whipS6Mob.onHit(player, whipS6MobHp, whipS6MobDamage, whipS6MobCrit);
                     }

                     player.skeleton.aoeHitTargets.add(whipS6Mob);
                  }

                  byte whipS6PlayerCount = msg.readByte();

                  for (int i = 0; i < whipS6PlayerCount; i++) {
                     int whipS6PlayerId = msg.readInt();
                     long whipS6PlayerHp = msg.readLong();
                     long whipS6PlayerDamage = msg.readLong();
                     boolean whipS6PlayerCrit = msg.readBoolean();
                     PlayerEntity whipS6Player;
                     if ((whipS6Player = GameState.findPlayerById(whipS6PlayerId)) != null) {
                        whipS6Player.onHit(player, whipS6PlayerHp, whipS6PlayerDamage, whipS6PlayerCrit);
                     }

                     player.skeleton.aoeHitTargets.add(whipS6Player);
                  }

                  player.setAnimationState(AnimationState.WHIP_SKILL_6);
                  return;
               case 43: // stick/skill11 (Cổ thần Dạ xoa) — decompiled C3873 case 43: no payload
                  player.setAnimationState(AnimationState.STICK_SKILL_11);
                  return;
               default:
                  return;
            }
         case 1:
            GameState.learnedSkills.clear();
            byte learnedSkillCount = msg.readByte();

            for (int i = 0; i < learnedSkillCount; i++) {
               GameState.learnedSkills.add(new LearnedSkill(msg.readInt(), msg.readByte(), System.currentTimeMillis() - msg.readLong()));
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 2:
            GameState.skillBarSkills.clear();
            byte skillBarCount = msg.readByte();

            for (int i = 0; i < skillBarCount; i++) {
               int skillBarSkillId = msg.readInt();
               GameState.skillBarSkills.add(GameState.getSkillById(skillBarSkillId));
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 3:
            GameState.selectedSkill = GameState.getSkillById(msg.readInt());
            break;
         case 4:
            LearnedSkill cooldownSkill;
            if ((cooldownSkill = GameState.getSkillById(msg.readInt())) == null) {
               return;
            }

            cooldownSkill.lastCastTime = System.currentTimeMillis() - msg.readLong();
            return;
         case 5:
            int levelSkillId = msg.readInt();
            byte skillLevel = msg.readByte();
            LearnedSkill leveledSkill;
            if ((leveledSkill = GameState.getSkillById(levelSkillId)) == null) {
               return;
            }

            byte targetLevel = skillLevel;
            if (skillLevel == 0) {
               leveledSkill.levelData = null;
            } else {
               Array.ArrayIterator levelIt = leveledSkill.skill.levels.iterator();

               while (levelIt.hasNext()) {
                  SkillLevelData levelData;
                  if ((levelData = (SkillLevelData)levelIt.next()).level == targetLevel) {
                     leveledSkill.levelData = levelData;
                     break label493;
                  }
               }
            }
            break;
         default:
            return;
      }

      this.loginScreen.debugScrollPane.refreshHudState();
   }

   public final void handlePlayerStats(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            PlayerEntity statsPlayer;
            if ((statsPlayer = GameState.findPlayerById(msg.readInt())) == null) {
               return;
            }

            readPlayerStats(statsPlayer.POOL2, msg);
            statsPlayer.refreshTitleAura();
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            PlayerEntity hpPlayer;
            if ((hpPlayer = GameState.findPlayerById(msg.readInt())) != null && hpPlayer.POOL2 != null) {
               hpPlayer.POOL2.currentHp = msg.readLong();
               hpPlayer.POOL2.maxHp = msg.readLong();
               return;
            }

            return;
         case 2:
            PlayerEntity mpPlayer;
            if ((mpPlayer = GameState.findPlayerById(msg.readInt())) != null && mpPlayer.POOL2 != null) {
               mpPlayer.POOL2.currentMp = msg.readLong();
               mpPlayer.POOL2.maxMp = msg.readLong();
               return;
            }

            return;
         case 3:
            PlayerEntity hpMpPlayer;
            if ((hpMpPlayer = GameState.findPlayerById(msg.readInt())) != null && hpMpPlayer.POOL2 != null) {
               hpMpPlayer.POOL2.currentHp = msg.readLong();
               hpMpPlayer.POOL2.maxHp = msg.readLong();
               hpMpPlayer.POOL2.currentMp = msg.readLong();
               hpMpPlayer.POOL2.maxMp = msg.readLong();
               return;
            }

            return;
         case 4:
            long expGain = msg.readLong();
            PlayerEntity.getInstance().POOL2.currentExp += expGain;
            FloatingCombatText.POOL2.obtain().init("+" + expGain, FloatingCombatText.CombatTextType.MISS, PlayerEntity.getInstance().getCenterPosition());
            return;
         case 5:
            int levelUpPlayerId = msg.readInt();
            short newLevel = msg.readShort();
            long currentExp = msg.readLong();
            long maxExp = msg.readLong();
            PlayerEntity levelUpPlayer;
            if ((levelUpPlayer = GameState.findPlayerById(levelUpPlayerId)) == null) {
               return;
            }

            levelUpPlayer.POOL2.level = newLevel;
            levelUpPlayer.POOL2.currentExp = currentExp;
            levelUpPlayer.POOL2.maxExp = maxExp;
            AudioManager.playSound("level_up", levelUpPlayer.getCenterPosition());
            PooledFloatingEffect levelUpEffect;
            PooledFloatingEffect newEffect = levelUpEffect = PooledFloatingEffect.POOL.obtain();
            newEffect.requestSkeleton("skeletons/vfxs/level_up", 0.25F, new SkeletonLoader.AnimationMixConfig());
            String levelUpAnimName = "level_up";
            levelUpEffect.tryPlayAnimation(levelUpAnimName, false, 1.0F);
            levelUpEffect.setPositionOffset(0.0F, -levelUpPlayer.getHeight() / 2.0F);
            levelUpPlayer.floatingEffectManager.addEffect(levelUpEffect);
            return;
         case 6:
            int[] talentPoints = new int[msg.readByte()];

            for (int i = 0; i < talentPoints.length; i++) {
               talentPoints[i] = msg.readInt();
            }

            PlayerEntity.getInstance().POOL2.talentPointsArray = talentPoints;
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 7:
            PlayerEntity.getInstance().POOL2.availableTalentPoints = msg.readInt();
            return;
         case 8:
            PlayerEntity.getInstance().POOL2.activityPointsCurrent = msg.readInt();
            PlayerEntity.getInstance().POOL2.activityPointsMax = msg.readInt();
            return;
         case 9:
            PlayerEntity.getInstance().POOL2.activityPoints = msg.readInt();
      }
   }

   public static void handlePingResponse(IMessage msg) {
      GameState.pingMs = (int)(System.currentTimeMillis() - msg.readLong());
   }

   public final void handleInventory(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            GameState.inventory.clearEquipmentSlots();
            int equipmentCount = msg.readInt();

            for (int i = 0; i < equipmentCount; i++) {
               GameState.inventory.equipmentSlots.add(this.readItemFromMessage(i, null, msg));
            }
            break;
         case 1:
            GameState.inventory.clearInventoryItems();
            GameState.inventory.inventoryItemCount = 0;
            GameState.inventory.luckyCoinCount = 0;
            int inventoryCount = msg.readInt();

            for (int i = 0; i < inventoryCount; i++) {
               Item inventoryItem = this.readItemFromMessage(i, null, msg);
               GameState.inventory.inventoryItems.add(inventoryItem);
               if (inventoryItem != null) {
                  GameState.inventory.inventoryItemCount += 1;
                  if (inventoryItem.template != null && inventoryItem.template.id == 307) { // Lucky Coin
                     GameState.inventory.luckyCoinCount += inventoryItem.quantity;
                  }
               }
            }
            break;
         case 2:
            GameState.inventory.clearMaterialItems();
            GameState.inventory.materialItemCount = 0;
            int materialCount = msg.readInt();

            for (int i = 0; i < materialCount; i++) {
               Item materialItem = this.readItemFromMessage(i, null, msg);
               GameState.inventory.materialItems.add(materialItem);
               if (materialItem != null) {
                  GameState.inventory.materialItemCount += 1;
               }
            }
            break;
         case 3:
            GameState.inventory.inventoryTimestamp1 = msg.readLong();
            GameState.inventory.inventoryTimestamp2 = msg.readLong();
            GameState.inventory.inventoryTimestamp3 = msg.readLong();
            break;
         case 4:
            GameState.inventory.clearWeaponSlots();
            int weaponCount = msg.readInt();

            for (int i = 0; i < weaponCount; i++) {
               GameState.inventory.weaponSlots.add(this.readItemFromMessage(i, null, msg));
            }

            int selectedWeaponIndex = msg.readInt();
            GameState.inventory.selectedWeaponSlot = selectedWeaponIndex != -1 ? GameState.inventory.weaponSlots.get(selectedWeaponIndex) : null;
            break;
         case 5:
            int quickIndex1 = msg.readInt();
            int quickIndex2 = msg.readInt();
            int quickIndex3 = msg.readInt();
            int quickIndex4 = msg.readInt();
            GameState.inventory.quickSlot1 = quickIndex1 != -1 ? GameState.inventory.inventoryItems.get(quickIndex1) : null;
            GameState.inventory.quickSlot2 = quickIndex2 != -1 ? GameState.inventory.inventoryItems.get(quickIndex2) : null;
            GameState.inventory.quickSlot3 = quickIndex3 != -1 ? GameState.inventory.inventoryItems.get(quickIndex3) : null;
            GameState.inventory.quickSlot4 = quickIndex4 != -1 ? GameState.inventory.inventoryItems.get(quickIndex4) : null;
            break;
         case 6:
            GameState.inventory.clearArmorSlots();
            int armorCount = msg.readInt();

            for (int i = 0; i < armorCount; i++) {
               GameState.inventory.armorSlots.add(this.readItemFromMessage(i, null, msg));
            }

            int selectedArmorIndex = msg.readInt();
            GameState.inventory.selectedArmorSlot = selectedArmorIndex != -1 ? GameState.inventory.armorSlots.get(selectedArmorIndex) : null;
            break;
         case 7:
            byte slotType = msg.readByte();
            int slotIndex = msg.readInt();
            Item updatedItem = null;
            switch (slotType) {
               case 0:
                  Item equipmentItem;
                  label174: {
                     PlayerInventory equipInventory = GameState.inventory;
                     if (slotIndex >= 0) {
                        if (slotIndex <= equipInventory.equipmentSlots.size - 1) {
                           equipmentItem = equipInventory.equipmentSlots.get(slotIndex);
                           break label174;
                        }
                     }

                     equipmentItem = null;
                  }

                  updatedItem = equipmentItem;
                  break;
               case 1:
                  PlayerInventory bagInventory = GameState.inventory;
                  updatedItem = slotIndex >= 0 && slotIndex <= bagInventory.inventoryItems.size - 1 ? bagInventory.inventoryItems.get(slotIndex) : null;
                  break;
               case 2:
                  Item materialSlotItem;
                  label161: {
                     PlayerInventory materialInventory = GameState.inventory;
                     if (slotIndex >= 0) {
                        if (slotIndex <= materialInventory.materialItems.size - 1) {
                           materialSlotItem = materialInventory.materialItems.get(slotIndex);
                           break label161;
                        }
                     }

                     materialSlotItem = null;
                  }

                  updatedItem = materialSlotItem;
                  break;
               case 3:
                  PlayerInventory weaponInventory = GameState.inventory;
                  updatedItem = slotIndex >= 0 && slotIndex <= weaponInventory.weaponSlots.size - 1 ? weaponInventory.weaponSlots.get(slotIndex) : null;
                  break;
               case 4:
                  Item armorItem;
                  label151: {
                     PlayerInventory armorInventory = GameState.inventory;
                     if (slotIndex >= 0) {
                        if (slotIndex <= armorInventory.armorSlots.size - 1) {
                           armorItem = armorInventory.armorSlots.get(slotIndex);
                           break label151;
                        }
                     }

                     armorItem = null;
                  }

                  updatedItem = armorItem;
                  break;
               case 5:
                  Item accessoryItem;
                  label146: {
                     PlayerInventory accessoryInventory = GameState.inventory;
                     if (slotIndex >= 0) {
                        if (slotIndex <= accessoryInventory.accessorySlots.size - 1) {
                           accessoryItem = accessoryInventory.accessorySlots.get(slotIndex);
                           break label146;
                        }
                     }

                     accessoryItem = null;
                  }

                  updatedItem = accessoryItem;
                  break;
               case 6:
                  PlayerInventory specialInventory = GameState.inventory;
                  updatedItem = slotIndex >= 0 && slotIndex <= specialInventory.specialSlots.size - 1 ? specialInventory.specialSlots.get(slotIndex) : null;
                  break;
               case 7:
                  PlayerInventory backpackInventory = GameState.inventory;
                  updatedItem = slotIndex >= 0 && slotIndex <= backpackInventory.backpackSlots.size - 1
                     ? backpackInventory.backpackSlots.get(slotIndex)
                     : null;
            }

            if (updatedItem == null) {
               return;
            }

            this.readItemFromMessage(slotIndex, updatedItem, msg);
            break;
         case 8:
            GameState.inventory.clearAccessorySlots();
            int accessoryCount = msg.readInt();

            for (int i = 0; i < accessoryCount; i++) {
               GameState.inventory.accessorySlots.add(this.readItemFromMessage(i, null, msg));
            }

            int selectedAccessoryIndex = msg.readInt();
            GameState.inventory.selectedAccessorySlot = selectedAccessoryIndex != -1 ? GameState.inventory.accessorySlots.get(selectedAccessoryIndex) : null;
            break;
         case 9:
            GameState.inventory.clearSpecialSlots();
            int specialCount = msg.readInt();

            for (int i = 0; i < specialCount; i++) {
               GameState.inventory.specialSlots.add(this.readItemFromMessage(i, null, msg));
            }

            int selectedSpecialIndex = msg.readInt();
            GameState.inventory.selectedSpecialSlot = selectedSpecialIndex != -1 ? GameState.inventory.specialSlots.get(selectedSpecialIndex) : null;
            break;
         case 10:
            GameState.inventory.clearBackpackSlots();
            int backpackCount = msg.readInt();
            for (int i = 0; i < backpackCount; i++) {
               GameState.inventory.backpackSlots.add(this.readItemFromMessage(i, null, msg));
            }
            int selectedBackpackIndex = msg.readInt();
            GameState.inventory.selectedBackpackSlot = selectedBackpackIndex >= 0
               && selectedBackpackIndex < GameState.inventory.backpackSlots.size
               ? GameState.inventory.backpackSlots.get(selectedBackpackIndex)
               : null;
      }

      this.loginScreen.debugScrollPane.refreshHudState();
   }

   /**
    * The decompiled version appends the backwear list and stats to the end of the character-inspect packet.
    * Stays compatible with the current server's old packet, which ends right after the pet stats.
    */
   private void readOptionalBackwearInfo(IMessage msg) {
      if (!(msg instanceof Message) || ((Message)msg).inputStream == null || ((Message)msg).inputStream.available() == 0) {
         return;
      }

      InspectedPlayerData data = GameState.wardrobeData;
      data.clearBackpackSlots();
      int itemCount = msg.readInt();
      for (int i = 0; i < itemCount; i++) {
         data.backpackSlots.add(this.readItemFromMessage(i, null, msg));
      }

      int selectedIndex = msg.readInt();
      data.selectedBackpackItem = selectedIndex >= 0 && selectedIndex < data.backpackSlots.size
         ? data.backpackSlots.get(selectedIndex)
         : null;

      data.clearBackpackSkillDescriptions();
      int statCount = msg.readInt();
      for (int i = 0; i < statCount; i++) {
         int statTypeId = msg.readInt();
         Integer[] values = new Integer[msg.readByte()];
         for (int j = 0; j < values.length; j++) {
            values[j] = msg.readInt();
         }
         data.backpackSkillDescriptions.add(new StatLine(StatType.getById(statTypeId), values, null));
      }
      StatLine.mergeStatLines(data.backpackSkillDescriptions);
   }

   public Item readItemFromMessage(int slotIndex, Item item, IMessage msg) {
      int idValue;
      if ((idValue = msg.readInt()) == -1) {
         return null;
      } else {
         if (item == null) {
            item = new Item(slotIndex, idValue, null, 0, false, 0L, 0L);
         }

         item.clear();
         item.slotIndex = slotIndex;
         item.template = ItemTemplate.getById(idValue);
         item.starGrade = Item.StarGrade.getValues()[msg.readByte()];
         item.starCount = msg.readByte();
         item.quantity = msg.readInt();
         item.locked = msg.readBoolean();
         item.createdTime = msg.readLong();
         item.expireTime = msg.readLong();
         item.statLines = new StatLine[msg.readByte()];

         for (int i = 0; i < item.statLines.length; i++) {
            idValue = msg.readInt();
            if (!msg.readBoolean()) {
               Integer[] statValues = new Integer[msg.readByte()];

               for (int j = 0; j < statValues.length; j++) {
                  statValues[j] = msg.readInt();
               }

               item.statLines[i] = new StatLine(StatType.getById(idValue), statValues, null);
            } else {
               Integer[][] statRanges = new Integer[msg.readByte()][2];

               for (int j = 0; j < statRanges.length; j++) {
                  Integer[] statRange = statRanges[j];
                  statRange[0] = msg.readInt();
                  statRanges[j][1] = msg.readInt();
               }

               item.statLines[i] = new StatLine(StatType.getById(idValue), null, statRanges);
            }
         }

         item.soulCharms = new Item[msg.readByte()];

         for (int i = 0; i < item.soulCharms.length; i++) {
            item.soulCharms[i] = this.readItemFromMessage(i, null, msg);
         }

         item.elementStones = new ElementStone[msg.readByte()];

         for (int i = 0; i < item.elementStones.length; i++) {
            int stoneTemplateId = idValue = msg.readInt();
            if (stoneTemplateId != -1) {
               int stoneLevelOrStatId = msg.readByte();
               item.elementStones[i] = new ElementStone(idValue, (byte)stoneLevelOrStatId);
               stoneLevelOrStatId = msg.readInt();
               Integer[] stoneStatValues = new Integer[msg.readByte()];

               for (int j = 0; j < stoneStatValues.length; j++) {
                  stoneStatValues[j] = msg.readInt();
               }

               item.elementStones[i].statLine = new StatLine(StatType.getById(stoneLevelOrStatId), stoneStatValues, null);
            }
         }

         return item;
      }
   }

   public static void readMobStats(MobData stats, IMessage msg) {
      MobTemplate template = MobTemplate.getById(msg.readInt());
      byte variantIndex = msg.readByte();
      int level = msg.readInt();
      long currentHp = msg.readLong();
      long maxHp = msg.readLong();
      if (stats != null) {
         stats.template = template;
         stats.variantIndex = variantIndex;
         stats.level = level;
         stats.currentHp = currentHp;
         stats.maxHp = maxHp;
      }
   }

   public static void readPlayerStats(PlayerStats stats, IMessage msg) {
      if (stats != null) {
         stats.classId = msg.readByte();
         stats.characterName = msg.readString();
         stats.partyId = msg.readInt();
         stats.guildId = msg.readInt();
         stats.guildName = msg.readString();
         stats.guildBadgeTexture = msg.readString();
         stats.avatarTexture = msg.readString();
         stats.classData = CharacterClass.getById(msg.readByte());
         stats.jobData = CharacterTemplate.getById(msg.readByte());
         stats.showHealthBar = msg.readBoolean();
         stats.factionStatus.faction = FactionStatus.FactionType.values()[msg.readByte()];
         stats.isEnemy = msg.readBoolean();
         stats.isAlive = msg.readBoolean();
         stats.isInParty = msg.readBoolean();
         stats.partyLeaderId = stats.isInParty ? msg.readInt() : -1;
         stats.skeletonKey = msg.readString();
         byte skinCount = msg.readByte();
         stats.equipmentSkinKeys = new String[skinCount];

         for (int i = 0; i < skinCount; i++) {
            stats.equipmentSkinKeys[i] = msg.readString();
         }

         stats.titleData = TitleData.getById(msg.readInt());
         stats.titleKey = msg.readString();
         stats.weaponType = msg.readByte();
         stats.weaponTexture = "lv" + stats.weaponType;
         stats.level = msg.readShort();
         stats.currentExp = msg.readLong();
         stats.maxExp = msg.readLong();
         stats.availableTalentPoints = msg.readInt();
         byte talentCount = msg.readByte();
         stats.talentPointsArray = new int[talentCount];

         for (int i = 0; i < talentCount; i++) {
            stats.talentPointsArray[i] = msg.readInt();
         }

         stats.currentHp = msg.readLong();
         stats.maxHp = msg.readLong();
         stats.currentMp = msg.readLong();
         stats.maxMp = msg.readLong();
         stats.attackPower = msg.readLong();
         stats.attackVsMob = msg.readLong();
         stats.critChance = msg.readLong();
         stats.critDamagePercent = msg.readLong();
         stats.armor = msg.readLong();
         stats.armorPen = msg.readLong();
         stats.evasion = msg.readLong();
         stats.accuracy = msg.readLong();
         stats.moveSpeed = msg.readLong();
         stats.hpRegen = msg.readLong();
         stats.mpRegen = msg.readLong();
         stats.hpLeechPercent = msg.readLong();
         stats.mpLeechPercent = msg.readLong();
         stats.hpLeechVsMobPercent = msg.readLong();
         stats.mpLeechVsMobPercent = msg.readLong();
         stats.reflectDamagePercent = msg.readLong();
         stats.stunDamage = msg.readLong();
         stats.weakenDamage = msg.readLong();
         stats.iceDamage = msg.readLong();
         stats.burnDamage = msg.readLong();
         stats.thunderDamage = msg.readLong();
         stats.critResistPercent = msg.readLong();
         stats.damageAmpPercent = msg.readLong();
         stats.damageAbsorbPercent = msg.readLong();
         stats.effectResistPercent = msg.readLong();
         stats.damageResist = msg.readLong();
         stats.goldBonusPercent = msg.readLong();
         stats.expBonusPercent = msg.readLong();
         stats.isTransparent = true;
      }
   }

   /**
    * Op 100 (NEW — World Boss table, server {@code worldboss/WorldBossList}): sub 0 boss list, sub 2 open the window (Utilities button), sub 1 enable
    * "Auto-pathing" along the op 63 arrow just received ("Find path" button).
    */
   public final void handleWorldBoss(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            Array<com.tlnt.model.worldboss.WorldBossInfo> list = new Array<>();
            int count = msg.readByte() & 0xff;
            long now = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
               com.tlnt.model.worldboss.WorldBossInfo boss = new com.tlnt.model.worldboss.WorldBossInfo();
               boss.id = msg.readInt();
               boss.name = msg.readString();
               boss.character = msg.readByte() == 1;
               boss.templateId = msg.readInt();
               boss.skins = new String[msg.readByte() & 0xff];
               for (int k = 0; k < boss.skins.length; k++) {
                  boss.skins[k] = msg.readString();
               }
               boss.level = msg.readShort();
               boss.attack = msg.readLong();
               boss.hp = msg.readLong();
               boss.maxHp = msg.readLong();
               boss.state = msg.readByte();
               boss.respawnAt = now + msg.readInt() * 1000L;
               boss.mapName = msg.readString();
               boss.spawnMaps = msg.readString();
               boss.respawnMinutes = msg.readInt();
               int lootCount = msg.readByte() & 0xff;
               for (int k = 0; k < lootCount; k++) {
                  com.tlnt.model.item.Item item = this.readItemFromMessage(k, null, msg);
                  int minQty = msg.readInt();
                  int maxQty = msg.readInt();
                  float chance = msg.readFloat();
                  if (item != null) {
                     boss.loot.add(new com.tlnt.model.worldboss.WorldBossInfo.Loot(item, minQty, maxQty, chance));
                  }
               }
               list.add(boss);
            }
            GameState.worldBosses = list;
            GameState.worldBossVersion++;
            this.loginScreen.debugScrollPane.refreshHudState();
            break;
         case 1:
            com.tlnt.world.entity.player.PlayerEntity player = com.tlnt.world.entity.player.PlayerEntity.getInstance();
            if (player != null && player.offscreenArrow != null) {
               player.serverPathActive = false;
               player.offscreenArrow.setAutoPathing(true);
            }
            break;
         case 2: // "World Boss" button in the Utilities window
            com.tlnt.ui.window.worldboss.WorldBossWindow.open();
            break;
         default:
            break;
      }
   }

   /** Op 75 (decompiled C3873 m18111): opens {@code OtherRankWindow} with the column name + list. */
   public final void handleOtherRank(IMessage msg) {
      com.tlnt.model.ranking.RankingCategory ranking = GameState.otherRanking;
      ranking.entries.clear();
      ranking.name = msg.readString();
      byte count = msg.readByte();

      for (int i = 0; i < count; i++) {
         int playerId = msg.readInt();
         String playerName = msg.readString();
         byte templateId = msg.readByte();
         String value = msg.readString();
         ranking.entries.add(new com.tlnt.model.ranking.RankEntry(i + 1, playerId, playerName, value, templateId));
      }

      this.loginScreen.debugScrollPane.showWindow(com.tlnt.ui.window.WindowType.OTHER_RANK_POPUP);
      this.loginScreen.debugScrollPane.refreshHudState();
   }

   public final void handleCraftBook(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            LuckyWheelData wheelData = GameState.luckyWheelData;
            Array.ArrayIterator rewardIt = GameState.luckyWheelData.rewardItems.iterator();

            while (rewardIt.hasNext()) {
               ((Item)rewardIt.next()).clear();
            }

            wheelData.rewardItems.clear();
            byte rewardCount = msg.readByte();

            for (int i = 0; i < rewardCount; i++) {
               GameState.luckyWheelData.rewardItems.add(this.readItemFromMessage(i, null, msg));
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            GameState.luckyWheelData.winningSlotIndex = msg.readByte();
            this.loginScreen.debugScrollPane.refreshHudState();
      }
   }

   public final void handleShowDialog(IMessage msg) {
      byte channel = msg.readByte();
      String prompt = msg.readString();
      String initialText = msg.readString();
      this.loginScreen.debugScrollPane.findDialog(QuantityInputDialog.class).openWithPrompt(prompt, null, initialText, new ChatDialogCallback(this, channel));
   }

   public final void handleBagUpdate(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            byte eventCount = msg.readByte();

            for (int i = 0; i < eventCount; i++) {
               byte eventOrMilestoneId = msg.readByte();
               String titleOrDescription = msg.readString();
               String iconPath = msg.readString();
               GiftcodeEvent event;
               (event = new GiftcodeEvent(eventOrMilestoneId, titleOrDescription, iconPath)).milestones = new GiftcodeEventMilestone[msg.readByte()];

               for (int j = 0; j < event.milestones.length; j++) {
                  eventOrMilestoneId = msg.readByte();
                  titleOrDescription = msg.readString();
                  byte claimStatus = msg.readByte();
                  event.milestones[j] = new GiftcodeEventMilestone(event, eventOrMilestoneId, titleOrDescription, claimStatus);
                  event.milestones[j].rewardItems = new Item[msg.readByte()];

                  for (int k = 0; k < event.milestones[j].rewardItems.length; k++) {
                     event.milestones[j].rewardItems[k] = this.readItemFromMessage(k, null, msg);
                  }
               }

               GameState.giftcodeData.events.add(event);
            }

            GameState.giftcodeData.updateClaimableFlags();
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            byte targetEventId = msg.readByte();
            byte targetMilestoneId = msg.readByte();
            Array.ArrayIterator eventIt = GameState.giftcodeData.events.iterator();

            label50:
            while (eventIt.hasNext()) {
               GiftcodeEvent matchedEvent;
               if ((matchedEvent = (GiftcodeEvent)eventIt.next()).id == targetEventId) {
                  GiftcodeEventMilestone[] milestones = matchedEvent.milestones;
                  int milestoneCount = matchedEvent.milestones.length;

                  for (int i = 0; i < milestoneCount; i++) {
                     GiftcodeEventMilestone milestone;
                     if ((milestone = milestones[i]).id == targetMilestoneId) {
                        milestone.description = msg.readString();
                        milestone.claimStatus = msg.readByte();
                        milestone.rewardItems = new Item[msg.readByte()];

                        for (int j = 0; j < milestone.rewardItems.length; j++) {
                           milestone.rewardItems[j] = this.readItemFromMessage(j, null, msg);
                        }
                        break label50;
                     }
                  }
                  break;
               }
            }

            GameState.giftcodeData.updateClaimableFlags();
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 2:
            GameState.giftcodeData.giftcodeListText = msg.readString();
            this.loginScreen.debugScrollPane.refreshHudState();
      }
   }

   public final void handleGatherAction(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            PlayerEntity castPlayer;
            if ((castPlayer = GameState.findPlayerById(msg.readInt())) == null) {
               return;
            }

            castPlayer.setAnimationState(AnimationState.CAST_LINE_FISHING);
            return;
         case 1:
            int hookPlayerId = msg.readInt();
            int[] pickedItemIds = new int[msg.readByte()];

            for (int i = 0; i < pickedItemIds.length; i++) {
               pickedItemIds[i] = msg.readInt();
            }

            PlayerEntity hookPlayer;
            if ((hookPlayer = GameState.findPlayerById(hookPlayerId)) == null) {
               return;
            } else {
               int[] itemIdsCopy = pickedItemIds;
               int itemIdCount = pickedItemIds.length;

               for (int j = 0; j < itemIdCount; j++) {
                  int itemId = itemIdsCopy[j];
                  hookPlayer.itemPickupDisplay.showItemPickup(itemId);
               }

               if (hookPlayer.isLocalPlayer()) {
                  EntityHpBarPanel hpBarPanel = this.loginScreen.debugScrollPane.POOL2.entityHpBarPanel;
                  this.loginScreen.debugScrollPane.POOL2.entityHpBarPanel.setVisible(false);
               }

               hookPlayer.setAnimationState(AnimationState.SET_HOOK_FISHING);
               return;
            }
         case 2:
            long entityId = System.currentTimeMillis() - (long)msg.readInt();
            float leftStart = msg.readFloat();
            float leftEnd = msg.readFloat();
            float rightStart = msg.readFloat();
            float rightEnd = msg.readFloat();
            int animPeriod = msg.readInt();
            this.loginScreen.debugScrollPane.POOL2.entityHpBarPanel.showForEntity(entityId, animPeriod, leftStart, leftEnd, rightStart, rightEnd);
            return;
         case 3:
            PlayerEntity idlePlayer;
            if ((idlePlayer = GameState.findPlayerById(msg.readInt())) == null) {
               return;
            } else {
               if (idlePlayer.isLocalPlayer()) {
                  EntityHpBarPanel hpBarPanel2 = this.loginScreen.debugScrollPane.POOL2.entityHpBarPanel;
                  this.loginScreen.debugScrollPane.POOL2.entityHpBarPanel.setVisible(false);
               }

               idlePlayer.setAnimationState(AnimationState.IDLE);
            }
      }
   }

   public final void handleTradeWindow(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            TradeState trade = GameState.tradeSession;
            Array.ArrayIterator offerIt = GameState.tradeSession.myOfferItems.iterator();

            while (offerIt.hasNext()) {
               ((Item)offerIt.next()).clear();
            }

            trade.myOfferItems.clear();
            trade.myOfferCoins = 0;
            trade.myOfferLocked = false;
            trade.myAccepted = false;
            trade.partnerName = "";
            offerIt = trade.partnerOfferItems.iterator();

            while (offerIt.hasNext()) {
               ((Item)offerIt.next()).clear();
            }

            trade.partnerOfferItems.clear();
            trade.partnerOfferCoins = 0;
            trade.partnerOfferLocked = false;
            trade.partnerAccepted = false;
            GameState.tradeSession.partnerName = msg.readString();
            GameState.tradeSession.partnerClass = CharacterTemplate.getById(msg.readInt());
            GameState.tradeSession.partnerLevel = msg.readInt();
            this.loginScreen.debugScrollPane.showWindow(WindowType.TRANSACTION_POPUP);
            return;
         case 1:
            GameState.tradeSession.partnerOfferLocked = true;
            GameState.tradeSession.partnerOfferCoins = msg.readInt();
            byte partnerItemCount = msg.readByte();

            for (int i = 0; i < partnerItemCount; i++) {
               GameState.tradeSession.partnerOfferItems.add(this.readItemFromMessage(i, null, msg));
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 2:
            GameState.tradeSession.partnerAccepted = true;
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 3:
            this.loginScreen.debugScrollPane.closeWindowByType(WindowType.TRANSACTION_POPUP);
      }
   }

   public final void handleAuctionUpdate(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            DailyActivityState daily = GameState.dailyQuestData;
            DailyActivityState dailyForClear = GameState.dailyQuestData;
            Array.ArrayIterator milestoneIt = GameState.dailyQuestData.milestones.iterator();

            while (milestoneIt.hasNext()) {
               DailyMilestone oldMilestone;
               if ((oldMilestone = (DailyMilestone)milestoneIt.next()).rewardItem != null) {
                  oldMilestone.rewardItem.clear();
               }

               DailyMilestone.POOL.free(oldMilestone);
            }

            dailyForClear.milestones.clear();
            byte milestoneCount = msg.readByte();

            for (int i = 0; i < milestoneCount; i++) {
               DailyMilestone milestone;
               (milestone = DailyMilestone.POOL.obtain()).milestoneId = msg.readInt();
               milestone.requiredPoints = msg.readInt();
               milestone.rewardItem = this.readItemFromMessage(i, null, msg);
               milestone.claimed = msg.readBoolean();
               daily.milestones.add(milestone);
            }

            daily.currentDiligencePoints = msg.readInt();
            daily.remainingTime = msg.readLong();
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            int claimedMilestoneId = msg.readInt();
            boolean claimed = msg.readBoolean();
            Array.ArrayIterator claimIt = GameState.dailyQuestData.milestones.iterator();

            while (claimIt.hasNext()) {
               DailyMilestone claimMilestone;
               if ((claimMilestone = (DailyMilestone)claimIt.next()).milestoneId == claimedMilestoneId) {
                  claimMilestone.claimed = claimed;
                  return;
               }
            }

            return;
         case 2:
            GameState.dailyQuestData.currentDiligencePoints = msg.readInt();
            return;
         case 3:
            DailyActivityState dailyTasks = GameState.dailyQuestData;
            GameState.dailyQuestData.hasNewlyCompletedTasks = false;
            Array.ArrayIterator taskIt = dailyTasks.questTasks.iterator();

            while (taskIt.hasNext()) {
               DailyQuestTask oldTask;
               Array.ArrayIterator rewardIt = (oldTask = (DailyQuestTask)taskIt.next()).rewards.iterator();

               while (rewardIt.hasNext()) {
                  ((Item)rewardIt.next()).clear();
               }

               oldTask.rewards.clear();
               DailyQuestTask.POOL.free(oldTask);
            }

            dailyTasks.questTasks.clear();
            byte taskCount = msg.readByte();

            for (int i = 0; i < taskCount; i++) {
               DailyQuestTask task;
               (task = DailyQuestTask.POOL.obtain()).questIndex = i;
               task.questName = msg.readString();
               task.questDescription = msg.readString();
               task.targetProgress = msg.readInt();
               task.currentProgress = msg.readInt();
               byte rewardCount = msg.readByte();

               for (int j = 0; j < rewardCount; j++) {
                  task.rewards.add(this.readItemFromMessage(j, null, msg));
               }

               task.rewardClaimed = msg.readBoolean();
               dailyTasks.questTasks.add(task);
               if (task.currentProgress >= task.targetProgress && !task.rewardClaimed) {
                  dailyTasks.hasNewlyCompletedTasks = true;
               }
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 4:
            int questIndex = msg.readInt();
            int currentProgress = msg.readInt();
            boolean rewardClaimed = msg.readBoolean();
            Array.ArrayIterator progressIt = GameState.dailyQuestData.questTasks.iterator();

            while (progressIt.hasNext()) {
               DailyQuestTask progressTask;
               if ((progressTask = (DailyQuestTask)progressIt.next()).questIndex == questIndex) {
                  progressTask.currentProgress = currentProgress;
                  progressTask.rewardClaimed = rewardClaimed;
               }
            }
      }
   }

   public final void handleDungeonUpdate(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            ComboPackCatalog catalog = GameState.comboPackData;
            ComboPackCatalog catalogForClear = GameState.comboPackData;
            Array.ArrayIterator packIt = GameState.comboPackData.packs.iterator();

            while (packIt.hasNext()) {
               ComboPack oldPack;
               Array.ArrayIterator packItemIt = (oldPack = (ComboPack)packIt.next()).items.iterator();

               while (packItemIt.hasNext()) {
                  ((Item)packItemIt.next()).clear();
               }

               oldPack.items.clear();
               ComboPack.pool.free(oldPack);
            }

            catalogForClear.packs.clear();
            catalog.id = msg.readInt();
            catalog.name = msg.readString();
            byte packCount = msg.readByte();

            for (int i = 0; i < packCount; i++) {
               ComboPack pack;
               (pack = ComboPack.pool.obtain()).catalog = catalog;
               pack.purchasePending = false;
               pack.purchaseRequestedAt = 0L;
               pack.index = i;
               pack.name = msg.readString();
               byte packItemCount = msg.readByte();

               for (int j = 0; j < packItemCount; j++) {
                  pack.items.add(this.readItemFromMessage(j, null, msg));
               }

               pack.priceItem = ItemTemplate.getById(msg.readInt());
               pack.price = msg.readInt();
               pack.purchased = msg.readBoolean();
               catalog.packs.add(pack);
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            int purchasedPackIndex = msg.readInt();
            boolean purchased = msg.readBoolean();
            Array.ArrayIterator purchaseIt = GameState.comboPackData.packs.iterator();

            while (purchaseIt.hasNext()) {
               ComboPack purchasePack;
               if ((purchasePack = (ComboPack)purchaseIt.next()).index == purchasedPackIndex) {
                  purchasePack.purchased = purchased;
                  purchasePack.purchasePending = false;
                  purchasePack.purchaseRequestedAt = 0L;
                  return;
               }
            }
      }
   }

   /** Reads Weekly Top data using the reference client's opcode 73 protocol. */
   public final void handleWeeklyTop(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            for (WeeklyTopEvent oldEvent : GameState.weeklyTopEvents) {
               oldEvent.clearRewards();
               oldEvent.entries.clear();
            }
            GameState.weeklyTopEvents.clear();
            int eventCount = msg.readByte() & 0xff;
            for (int i = 0; i < eventCount; i++) {
               WeeklyTopEvent event = new WeeklyTopEvent(msg.readInt(), msg.readString(),
                     System.currentTimeMillis() + msg.readLong());
               readWeeklyRewards(event.firstPlaceRewards, msg);
               readWeeklyRewards(event.secondPlaceRewards, msg);
               readWeeklyRewards(event.thirdPlaceRewards, msg);
               GameState.weeklyTopEvents.add(event);
            }
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            int eventId = msg.readInt();
            int entryCount = msg.readInt();
            if (entryCount < 0 || entryCount > 1000) {
               throw new IllegalArgumentException("So dong Top tuan khong hop le: " + entryCount);
            }
            Array<WeeklyTopEntry> entries = new Array<>(true, entryCount);
            for (int i = 0; i < entryCount; i++) {
               entries.add(readWeeklyTopEntry(msg));
            }
            WeeklyTopEntry myEntry = readWeeklyTopEntry(msg);
            for (WeeklyTopEvent event : GameState.weeklyTopEvents) {
               if (event.id == eventId) {
                  event.entries.clear();
                  event.entries.addAll(entries);
                  event.myEntry = myEntry;
                  break;
               }
            }
            this.loginScreen.debugScrollPane.refreshHudState();
      }
   }

   private void readWeeklyRewards(Array<Item> destination, IMessage msg) {
      int count = msg.readByte() & 0xff;
      for (int i = 0; i < count; i++) {
         destination.add(this.readItemFromMessage(i, null, msg));
      }
   }

   private WeeklyTopEntry readWeeklyTopEntry(IMessage msg) {
      return new WeeklyTopEntry(msg.readInt(), msg.readInt(), msg.readString(), msg.readString());
   }

   public final void handleShopUpdate(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            ConsignmentShopData shop = GameState.shopData;
            GameState.shopData.clearCategories();
            shop.shopId = msg.readInt();
            shop.shopName = msg.readString();
            shop.shopType = msg.readByte();
            byte categoryCount = msg.readByte();

            for (int i = 0; i < categoryCount; i++) {
               ConsignmentCategory category;
               (category = ConsignmentCategory.pool.obtain()).shopData = shop;
               category.index = i;
               category.tabName = msg.readString();
               category.tabNameAlt = msg.readString();
               int categoryItemCount = msg.readInt();

               for (int j = 0; j < categoryItemCount; j++) {
                  ConsignmentItem shopItem;
                  (shopItem = ConsignmentItem.pool.obtain()).category = category;
                  shopItem.slot = j;
                  shopItem.itemData = this.readItemFromMessage(j, null, msg);
                  shopItem.itemConfig = ItemTemplate.getById(msg.readInt());
                  shopItem.quantity = msg.readInt();
                  category.items.add(shopItem);
               }

               shop.categories.add(category);
            }

            shop.dirty = true;
            if (shop.shopType == 1) {
               this.loginScreen.debugScrollPane.showWindow(WindowType.SHOP_POPUP);
               return;
            } else {
               this.loginScreen.debugScrollPane.refreshHudState();
            }
      }
   }

   public final void handleTradeUpdate(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            MarketListing.POOL.freeAll(GameState.marketListings);
            GameState.marketListings.clear();
            GameState.myMarketListings.clear();
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            int listingCount = msg.readInt();

            for (int i = 0; i < listingCount; i++) {
               MarketListing listing;
               (listing = MarketListing.POOL.obtain()).listingId = msg.readInt();
               listing.sellerId = msg.readInt();
               listing.sellerName = msg.readString();
               listing.price = msg.readInt();
               listing.item = this.readItemFromMessage(listing.listingId, null, msg);
               listing.expireTime = System.currentTimeMillis() + msg.readLong();
               GameState.marketListings.add(listing);
               if (listing.sellerId == PlayerEntity.getInstance().getId()) {
                  GameState.myMarketListings.add(listing);
               }
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 2:
            GameState.marketSellItem = null;
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 3:
            this.loginScreen.debugScrollPane.showWindow(WindowType.SHOP_CONSIGNMENT_POPUP);
            MarketWindow marketWindow = this.loginScreen.debugScrollPane.findDialog(MarketWindow.class);
            marketWindow.selectTabByIndex(1);
      }
   }

   public final void handlePetUpdate(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            Tribe.POOL.freeAll(GameState.tribeData.tribeList);
            GameState.tribeData.tribeList.clear();
            byte tribeCount = msg.readByte();

            for (int i = 0; i < tribeCount; i++) {
               GameState.tribeData.tribeList.add(readPetFromMessage(msg));
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            if (GameState.tribeData.myTribe != null) {
               Tribe.POOL.free(GameState.tribeData.myTribe);
               GameState.tribeData.myTribe = null;
            }

            if (msg.readBoolean()) {
               GameState.tribeData.myTribe = readPetFromMessage(msg);
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 2:
            GameState.tribeData.notices.clear();
            byte noticeCount = msg.readByte();

            for (int i = 0; i < noticeCount; i++) {
               GameState.tribeData.notices.add(msg.readString());
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 3:
            GameState.tribeData.warehouseItems.clear();
            int warehouseCount = msg.readInt();

            for (int i = 0; i < warehouseCount; i++) {
               GameState.tribeData.warehouseItems.add(this.readItemFromMessage(i, null, msg));
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 4:
            GameState.tribeData.skills.clear();
            int skillCount = msg.readInt();

            for (int i = 0; i < skillCount; i++) {
               GameState.tribeData.skills.add(TribeSkill.getById(msg.readInt()));
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 5:
            int statLineCount = msg.readInt();
            StatLine[] emblemStatLines = new StatLine[statLineCount];

            for (int i = 0; i < statLineCount; i++) {
               int statTypeId = msg.readInt();
               if (!msg.readBoolean()) {
                  Integer[] values = new Integer[msg.readByte()];
                  for (int j = 0; j < values.length; j++) {
                     values[j] = msg.readInt();
                  }
                  emblemStatLines[i] = new StatLine(StatType.getById(statTypeId), values, null);
               } else {
                  Integer[][] ranges = new Integer[msg.readByte()][2];
                  for (int j = 0; j < ranges.length; j++) {
                     ranges[j][0] = msg.readInt();
                     ranges[j][1] = msg.readInt();
                  }
                  emblemStatLines[i] = new StatLine(StatType.getById(statTypeId), null, ranges);
               }
            }

            if (GameState.tribeData.myTribe != null) {
               GameState.tribeData.myTribe.emblemStatLines = emblemStatLines;
               this.loginScreen.debugScrollPane.refreshHudState();
            }
      }
   }

   public static Tribe readPetFromMessage(IMessage msg) {
      Tribe tribe;
      (tribe = Tribe.POOL.obtain()).id = msg.readInt();
      tribe.name = msg.readString();
      tribe.slogan = msg.readString();
      tribe.maxMembers = msg.readByte();
      tribe.level = msg.readByte();
      tribe.emblem = TribeEmblem.getById(msg.readInt());
      tribe.emblemExpireTime = msg.readLong();
      tribe.fund = msg.readLong();
      tribe.resourcePoints = msg.readLong();
      tribe.exp = msg.readLong();
      tribe.expMax = msg.readLong();
      byte memberCount = msg.readByte();

      for (int i = 0; i < memberCount; i++) {
         TribeMember member;
         (member = TribeMember.POOL.obtain()).playerId = msg.readInt();
         member.name = msg.readString();
         member.level = msg.readInt();
         member.role = TribeMember.Role.values()[msg.readByte()];
         member.spirit = CharacterTemplate.getById(msg.readByte());
         member.contribution = msg.readLong();
         if (member.role == TribeMember.Role.LEADER) {
            tribe.leaderName = member.name;
         }

         tribe.members.add(member);
      }

      byte applicantCount = msg.readByte();

      for (int i = 0; i < applicantCount; i++) {
         TribeApplicant applicant;
         (applicant = TribeApplicant.POOL.obtain()).playerId = msg.readInt();
         applicant.name = msg.readString();
         applicant.level = msg.readInt();
         applicant.spirit = CharacterTemplate.getById(msg.readByte());
         tribe.applicants.add(applicant);
      }

      return tribe;
   }

   public final void handleRewardPopup(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            Item[] rewards = new Item[msg.readInt()];

            for (int i = 0; i < rewards.length; i++) {
               rewards[i] = this.readItemFromMessage(i, null, msg);
            }

            this.loginScreen.debugScrollPane.findDialog(RewardPopup.class).showRewards(rewards);
      }
   }

   public final void handleEquippedSkills(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            GameState.inventory.clearSkillDescriptions3();
            int statLineCount = msg.readInt();

            for (int i = 0; i < statLineCount; i++) {
               int statTypeId = msg.readInt();
               Integer[] statValues = new Integer[msg.readByte()];

               for (int j = 0; j < statValues.length; j++) {
                  statValues[j] = msg.readInt();
               }

               GameState.inventory.skillDescriptions3.add(new StatLine(StatType.getById(statTypeId), statValues, null));
            }

            StatLine.mergeStatLines(GameState.inventory.skillDescriptions3);
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            int selectedAccessoryIndex = msg.readInt();
            GameState.inventory.selectedAccessorySlot = selectedAccessoryIndex != -1 ? GameState.inventory.accessorySlots.get(selectedAccessoryIndex) : null;
            this.loginScreen.debugScrollPane.refreshHudState();
      }
   }

   public static void handleChatMessage(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            int announcementId = msg.readInt();
            String text = msg.readString();
            long startTime = System.currentTimeMillis() - (long)msg.readInt();
            int durationMs = msg.readInt();
            GameState.announcements.addOrUpdateAnnouncement(announcementId, text, startTime, durationMs);
            return;
         case 1:
            int removedAnnouncementId = msg.readInt();
            GameState.announcements.removeAnnouncement(removedAnnouncementId);
      }
   }

   public final void handlePassiveSkills(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            GameState.inventory.clearSkillDescriptions4();
            int statLineCount = msg.readInt();

            for (int i = 0; i < statLineCount; i++) {
               int statTypeId = msg.readInt();
               Integer[] statValues = new Integer[msg.readByte()];

               for (int j = 0; j < statValues.length; j++) {
                  statValues[j] = msg.readInt();
               }

               GameState.inventory.skillDescriptions4.add(new StatLine(StatType.getById(statTypeId), statValues, null));
            }

            StatLine.mergeStatLines(GameState.inventory.skillDescriptions4);
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            int selectedSpecialIndex = msg.readInt();
            GameState.inventory.selectedSpecialSlot = selectedSpecialIndex != -1 ? GameState.inventory.specialSlots.get(selectedSpecialIndex) : null;
            this.loginScreen.debugScrollPane.refreshHudState();
      }
   }

   /** Opcode 78: backwear stats / backwear item changes in the decompiled format. */
   public final void handleBackwearSkills(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            GameState.inventory.clearBackpackSkillDescriptions();
            int statCount = msg.readInt();
            for (int i = 0; i < statCount; i++) {
               int statTypeId = msg.readInt();
               Integer[] values = new Integer[msg.readByte()];
               for (int j = 0; j < values.length; j++) {
                  values[j] = msg.readInt();
               }
               GameState.inventory.backpackSkillDescriptions.add(new StatLine(StatType.getById(statTypeId), values, null));
            }
            StatLine.mergeStatLines(GameState.inventory.backpackSkillDescriptions);
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            int selectedIndex = msg.readInt();
            GameState.inventory.selectedBackpackSlot = selectedIndex >= 0
               && selectedIndex < GameState.inventory.backpackSlots.size
               ? GameState.inventory.backpackSlots.get(selectedIndex)
               : null;
            this.loginScreen.debugScrollPane.refreshHudState();
      }
   }

   public final void handleQuestUpdate(IMessage msg) {
      byte subTypeOrStageCount = msg.readByte();
      MainQuestState quest = GameState.tutorialState;
      switch (subTypeOrStageCount) {
         case 0:
            Array.ArrayIterator it = quest.stages.iterator();

            while (it.hasNext()) {
               MainQuestStage stage;
               (stage = (MainQuestStage)it.next()).stageName = null;
               stage.stageDescription = null;
            }

            quest.stages.clear();
            it = quest.rewardItems.iterator();

            while (it.hasNext()) {
               ((Item)it.next()).clear();
            }

            quest.rewardItems.clear();
            quest.questId = msg.readInt();
            quest.questTitle = msg.readString();
            quest.questDescription = msg.readString();
            subTypeOrStageCount = msg.readByte();

            for (int i = 0; i < subTypeOrStageCount; i++) {
               String stageName = msg.readString();
               String stageDescription = msg.readString();
               int requiredCount = msg.readInt();
               quest.stages.add(new MainQuestStage(i, stageName, stageDescription, requiredCount));
            }

            byte rewardCount = msg.readByte();

            for (int i = 0; i < rewardCount; i++) {
               quest.rewardItems.add(this.readItemFromMessage(i, null, msg));
            }

            quest.currentStageIndex = msg.readByte();
            quest.currentProgress = msg.readInt();
            quest.targetObjectId = msg.readInt();
            quest.targetMapId = msg.readInt();
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 1:
            quest.currentStageIndex = msg.readByte();
            quest.currentProgress = msg.readInt();
            quest.targetObjectId = msg.readInt();
            quest.targetMapId = msg.readInt();
            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 2:
            if (PlayerEntity.getInstance() != null) {
               PlayerEntity.getInstance().questCompleteEffect.playEffect();
            }
      }
   }

   public final void handlePartyUpdate(IMessage msg) {
      byte action = msg.readByte();
      byte forgeMode = msg.readByte();
      switch (action) {
         case 0:
            GameState.forgeData.reset();
            GameState.forgeData.activeMode = forgeMode;
            this.loginScreen.debugScrollPane.showWindow(WindowType.COMBINE_POPUP);
            return;
         case 1:
            switch (forgeMode) {
               case 0:
                  GameState.forgeData.upgradeState.materialItem1 = this.readItemFromMessage(0, null, msg);
                  GameState.forgeData.upgradeState.materialItem2 = this.readItemFromMessage(1, null, msg);
                  GameState.forgeData.upgradeState.equipmentItem = this.readItemFromMessage(2, null, msg);
                  GameState.forgeData.upgradeState.resultItem = this.readItemFromMessage(3, null, msg);
                  GameState.forgeData.upgradeState.infoText = msg.readString();
                  break;
               case 1:
                  GameState.forgeData.soulState.soulLevel = msg.readByte();
                  GameState.forgeData.soulState.equipmentItem = this.readItemFromMessage(0, null, msg);
                  GameState.forgeData.soulState.materialItem1 = this.readItemFromMessage(1, null, msg);
                  GameState.forgeData.soulState.materialItem2 = this.readItemFromMessage(2, null, msg);
                  GameState.forgeData.soulState.infoText = msg.readString();
                  break;
               case 2:
                  GameState.forgeData.elementState.elementLevel = msg.readByte();
                  GameState.forgeData.elementState.equipmentItem = this.readItemFromMessage(0, null, msg);
                  GameState.forgeData.elementState.materialItem1 = this.readItemFromMessage(1, null, msg);
                  GameState.forgeData.elementState.materialItem2 = this.readItemFromMessage(2, null, msg);
                  GameState.forgeData.elementState.infoText = msg.readString();
                  break;
               case 3:
                  GameState.forgeData.brewState.materialItem = this.readItemFromMessage(0, null, msg);
                  GameState.forgeData.brewState.mainItem = this.readItemFromMessage(1, null, msg);
                  GameState.forgeData.brewState.resultItem = this.readItemFromMessage(2, null, msg);
                  GameState.forgeData.brewState.infoText = msg.readString();
                  break;
               case 4:
                  GameState.forgeData.craftState.materialItem1 = this.readItemFromMessage(0, null, msg);
                  GameState.forgeData.craftState.materialItem2 = this.readItemFromMessage(1, null, msg);
                  GameState.forgeData.craftState.mainItem = this.readItemFromMessage(2, null, msg);
                  GameState.forgeData.craftState.resultItem = this.readItemFromMessage(3, null, msg);
                  GameState.forgeData.craftState.infoText = msg.readString();
                  break;
               case 5:
                  GameState.forgeData.refineState.inputItem = this.readItemFromMessage(0, null, msg);
                  GameState.forgeData.refineState.resultItem = this.readItemFromMessage(1, null, msg);
                  GameState.forgeData.refineState.refineOption = msg.readByte();
                  GameState.forgeData.refineState.infoText = msg.readString();
                  break;
               case 6:
                  GameState.forgeData.transmuteState.sourceItem = this.readItemFromMessage(0, null, msg);
                  GameState.forgeData.transmuteState.targetItem = this.readItemFromMessage(1, null, msg);
                  GameState.forgeData.transmuteState.charmItem = this.readItemFromMessage(2, null, msg);
                  GameState.forgeData.transmuteState.infoText = msg.readString();
            }

            this.loginScreen.debugScrollPane.refreshHudState();
            return;
         case 2:
            switch (forgeMode) {
               case 0:
                  GameState.forgeData.upgradeState.resultStatus = msg.readByte();
                  break;
               case 1:
                  GameState.forgeData.soulState.resultStatus = msg.readByte();
                  break;
               case 2:
                  GameState.forgeData.elementState.resultStatus = msg.readByte();
                  break;
               case 3:
                  GameState.forgeData.brewState.resultStatus = msg.readByte();
                  break;
               case 4:
                  GameState.forgeData.craftState.resultStatus = msg.readByte();
                  break;
               case 5:
                  GameState.forgeData.refineState.resultStatus = msg.readByte();
                  break;
               case 6:
                  GameState.forgeData.transmuteState.resultStatus = msg.readByte();
            }

            this.loginScreen.debugScrollPane.refreshHudState();
      }
   }

   public final void handlePlantInteraction(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            int plantCount = msg.readInt();

            for (int i = 0; i < plantCount; i++) {
               int plantId = msg.readInt();
               float plantX = msg.readFloat() / 100.0F;
               float plantY = msg.readFloat() / 100.0F;
               PlantEntity plant;
               readPlantStats((plant = PlantFactory.obtainPlant(plantId, plantX, plantY)).POOL2, msg);
               GameState.nearbyPlants.add(plant);
            }

            return;
         case 1:
            int singlePlantId = msg.readInt();
            float singlePlantX = msg.readFloat() / 100.0F;
            float singlePlantY = msg.readFloat() / 100.0F;
            PlantEntity singlePlant;
            readPlantStats((singlePlant = PlantFactory.obtainPlant(singlePlantId, singlePlantX, singlePlantY)).POOL2, msg);
            GameState.nearbyPlants.add(singlePlant);
            return;
         case 2:
            PlayerEntity.getInstance().harvestProgressBar.startProgress(System.currentTimeMillis() - (long)msg.readInt(), msg.readInt());
            return;
         case 3:
            int pickerPlayerId = msg.readInt();
            int pickedPlantId = msg.readInt();
            PlayerEntity picker = GameState.findPlayerById(pickerPlayerId);
            PlantEntity pickedPlant = GameState.findPlantById(pickedPlantId);
            if (picker != null) {
               if (pickedPlant != null) {
                  picker.faceTowards(pickedPlant.getPosition());
               }

               picker.setAnimationState(AnimationState.PICK);
            }

            if (pickedPlant != null) {
               PlantFactory.freePlant(pickedPlant);
            }

            return;
         case 4:
            PlayerEntity.getInstance().harvestProgressBar.stopProgress();
      }
   }

   public static void readPlantStats(PlantTemplateRef ref, IMessage msg) {
      int templateId = msg.readInt();
      ref.template = PlantTemplate.getTemplateById(templateId);
   }

   public final void handleAnimalUpdate(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            byte animalCount = msg.readByte();

            for (int i = 0; i < animalCount; i++) {
               AnimalEntity animal = readAnimalFromMessage(msg);
               GameState.nearbyAnimals.add(animal);
            }

            return;
         case 1:
            AnimalEntity singleAnimal = readAnimalFromMessage(msg);
            // same id already exists (e.g. right after "Grow fast"): replace the old one
            for (int i = GameState.nearbyAnimals.size - 1; i >= 0; i--) {
               AnimalEntity old = GameState.nearbyAnimals.get(i);
               if (old != singleAnimal && old.getId() == singleAnimal.getId()) {
                  GameState.nearbyAnimals.removeIndex(i);
                  com.tlnt.core.factory.AnimalFactory.freeAnimal(old);
               }
            }
            GameState.nearbyAnimals.add(singleAnimal);
            return;
         case 2:
            AnimalBarnState barn;
            (barn = GameState.buildingStatus).level = msg.readInt();
            barn.upgrading = msg.readBoolean();
            if (!barn.upgrading) {
               barn.productionStartTime = System.currentTimeMillis() - msg.readLong();
               barn.maxHarvestCount = msg.readInt();
               barn.productionIntervalMs = msg.readInt();
               return;
            }

            barn.upgradeStartTime = System.currentTimeMillis() - msg.readLong();
            barn.upgradeDurationMs = msg.readLong();
            break;
         case 3:
            AnimalEntity harvestAnimal;
            if ((harvestAnimal = GameState.findAnimalById(msg.readInt())) != null) {
               harvestAnimal.POOL.projectile = false;
            }

            PlayerEntity.getInstance().harvestProgressBar.startProgress(System.currentTimeMillis() - (long)msg.readInt(), msg.readInt());
            return;
         case 4:
            int pickerPlayerId = msg.readInt();
            int pickedAnimalId = msg.readInt();
            PlayerEntity picker = GameState.findPlayerById(pickerPlayerId);
            AnimalEntity pickedAnimal = GameState.findAnimalById(pickedAnimalId);
            if (picker != null) {
               if (pickedAnimal != null) {
                  picker.faceTowards(pickedAnimal.getPosition());
               }

               picker.setAnimationState(AnimationState.PICK);
            }

            if (pickedAnimal != null) {
               pickedAnimal.setAnimationState(AnimationState.ANIMAL_DIE);
            }

            return;
         case 5:
            PlayerEntity.getInstance().harvestProgressBar.stopProgress();
      }
   }

   public static AnimalEntity readAnimalFromMessage(IMessage msg) {
      int animalId = msg.readInt();
      float x = msg.readFloat() / 100.0F;
      float y = msg.readFloat() / 100.0F;
      AnimalEntity animal = AnimalFactory.obtainAnimal(animalId, x, y);
      AnimalType animalType = AnimalType.getById(msg.readInt());
      int remainingGrowTime = msg.readInt();
      animal.POOL2.animalType = animalType;
      animal.POOL2.remainingGrowTime = remainingGrowTime;
      animal.POOL2.growStartTime = System.currentTimeMillis() + (long)animal.POOL2.remainingGrowTime - (long)animal.POOL2.animalType.growDurationMs;
      return animal;
   }

   public static void handleDecryptKeys(IMessage msg) {
      byte keyLength = msg.readByte();
      if (GameState.textureDecryptKey == null || GameState.textureDecryptKey.length != keyLength) {
         GameState.textureDecryptKey = new byte[keyLength];
      }

      for (int i = 0; i < keyLength; i++) {
         GameState.textureDecryptKey[i] = msg.readByte();
      }

      keyLength = msg.readByte();
      if (GameState.atlasDecryptKey == null || GameState.atlasDecryptKey.length != keyLength) {
         GameState.atlasDecryptKey = new byte[keyLength];
      }

      for (int j = 0; j < keyLength; j++) {
         GameState.atlasDecryptKey[j] = msg.readByte();
      }
   }

   public final void handleGuildSkillTree(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            Item[] recipes = new Item[msg.readByte()];

            for (int i = 0; i < recipes.length; i++) {
               recipes[i] = this.readItemFromMessage(i, null, msg);
            }

            this.loginScreen.debugScrollPane.skeleton.findDialog(WeaponCraftWindow.class).setRecipes(recipes);
            return;
         case 1:
            msg.readByte();
            this.loginScreen.debugScrollPane.skeleton.findDialog(WeaponCraftWindow.class).hide();
      }
   }

   public static void handleOffscreenTarget(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            if (PlayerEntity.getInstance() != null) {
               PlayerEntity.getInstance().offscreenArrow.setAutoPathing(false); // arrow off: no target anymore
               PlayerEntity.getInstance().offscreenArrow.setEnabled(false);
               return;
            }
            break;
         case 1:
            if (PlayerEntity.getInstance() != null) {
               float x = msg.readFloat() / 100.0F;
               float y = msg.readFloat() / 100.0F;
               float width = msg.readFloat() / 100.0F;
               float height = msg.readFloat() / 100.0F;
               PlayerEntity.getInstance().offscreenArrow.setViewportBounds(x, y, width, height);
               PlayerEntity.getInstance().offscreenArrow.setEnabled(true);
            }
            break;
         case 2: // decompiled m18108: the server guides the character to the middle of an area (x, y, width, height)
            if (PlayerEntity.getInstance() != null) {
               float pathX = msg.readFloat() / 100.0F;
               float pathY = msg.readFloat() / 100.0F;
               float pathWidth = msg.readFloat() / 100.0F;
               float pathHeight = msg.readFloat() / 100.0F;
               PlayerEntity.getInstance().startServerPath(pathX, pathY, pathWidth, pathHeight);
            }
      }
   }

   public final void handleLeaderboard(IMessage msg) {
      switch (msg.readByte()) {
         case 0:
            GameState.achievementData.achievements.clear();
            return;
         case 1:
            byte achievementCount = msg.readByte();

            for (int i = 0; i < achievementCount; i++) {
               int achievementId = msg.readInt();
               String title = msg.readString();
               String description = msg.readString();
               int targetCount = msg.readInt();
               Item[] rewardItems = new Item[msg.readByte()];

               for (int j = 0; j < rewardItems.length; j++) {
                  rewardItems[j] = this.readItemFromMessage(j, null, msg);
               }

               int progress = msg.readInt();
               byte status = msg.readByte();
               GameState.achievementData.achievements.add(new Achievement(achievementId, title, description, targetCount, rewardItems, progress, status));
            }

            GameState.achievementData.updateClaimableFlag();
            return;
         case 2:
            int updatedId = msg.readInt();
            int updatedProgress = msg.readInt();
            byte updatedStatus = msg.readByte();
            Achievement updated = null;
            Array.ArrayIterator achievementIt = GameState.achievementData.achievements.iterator();

            while (achievementIt.hasNext()) {
               Achievement candidate;
               if ((candidate = (Achievement)achievementIt.next()).id == updatedId) {
                  updated = candidate;
                  break;
               }
            }

            if (updated != null) {
               updated.progress = updatedProgress;
               updated.status = updatedStatus;
               GameState.achievementData.updateClaimableFlag();
               this.loginScreen.debugScrollPane.refreshHudState();
            }
      }
   }

   public final void handleSkillSlots(IMessage msg) {
      byte categoryCount = msg.readByte();

      for (int i = 0; i < categoryCount; i++) {
         GuideCategory category = GuideCategory.getOrCreate(msg.readByte());
         byte topicCount = msg.readByte();

         for (int j = 0; j < topicCount; j++) {
            byte topicId = msg.readByte();
            byte targetTopicId = topicId;
            Array.ArrayIterator topicIt = category.topics.iterator();

            GuideTopic foundTopic;
            while (true) {
               if (!topicIt.hasNext()) {
                  foundTopic = null;
                  break;
               }

               GuideTopic candidateTopic;
               if ((candidateTopic = (GuideTopic)topicIt.next()).id == targetTopicId) {
                  foundTopic = candidateTopic;
                  break;
               }
            }

            GuideTopic topic = foundTopic;
            foundTopic.items = new Item[msg.readByte()];

            for (int k = 0; k < topic.items.length; k++) {
               topic.items[k] = this.readItemFromMessage(k, null, msg);
            }
         }
      }

      this.loginScreen.debugScrollPane.refreshHudState();
   }

}
