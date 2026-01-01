package com.tlnt.net;

import com.tlnt.world.entity.player.pet.PetTerrain;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Queue;
import com.shimaota.a.a.IMessage;
import com.shimaota.a.a.Message;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.core.Settings;
import com.tlnt.core.audio.AudioManager;
import com.tlnt.core.factory.AmbientFishFactory;
import com.tlnt.core.factory.PlayerFactory;
import com.tlnt.engine.physics.AnimationState;
import com.tlnt.model.character.CharacterTemplate;
import com.tlnt.model.chat.ChatManager;
import com.tlnt.model.chat.ChatMessage;
import com.tlnt.model.chat.PrivateConversation;
import com.tlnt.model.friend.MapTile;
import com.tlnt.model.friendentry.FriendEntry;
import com.tlnt.model.friendentry.FriendInviteEntry;
import com.tlnt.model.item.StatLine;
import com.tlnt.model.item.StatType;
import com.tlnt.model.option.OptionInfo;
import com.tlnt.model.party.Party;
import com.tlnt.model.party.PartyBuffEntry;
import com.tlnt.model.party.PartyJoinRequest;
import com.tlnt.model.party.PartyMember;
import com.tlnt.model.player.FactionStatus;
import com.tlnt.model.ranking.RankEntry;
import com.tlnt.model.ranking.RankingCategory;
import com.tlnt.model.server.ServerInfo;
import com.tlnt.model.worldmap.RegionInfo;
import com.tlnt.net.task.PacketFragment;
import com.tlnt.net.task.ServerPacketHandler;
import com.tlnt.ui.hud.widget.SkeletonActor;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.action.DeathRespawnPopup;
import com.tlnt.ui.window.action.InvitationConfirmPopup;
import com.tlnt.ui.window.action.RegionInfoPopup;
import com.tlnt.ui.window.countdown.CountdownPopup;
import com.tlnt.ui.window.giftcode.GiftcodeEventConfirmDialog;
import com.tlnt.util.GameUtils;
import com.tlnt.world.effect.ReviveEffect;
import com.tlnt.world.entity.decoration.WaypointEntity;
import com.tlnt.world.entity.decoration.WorldMapEntity;
import com.tlnt.world.entity.decoration.tilemap.TileMapRenderer;
import com.tlnt.world.entity.fish.AmbientFish;
import com.tlnt.world.entity.mob.MobEntity;
import com.tlnt.world.entity.player.PlayerEntity;
import com.tlnt.world.groundeffect.EarthSpikeEffect;
import com.tlnt.world.groundeffect.LightningStrikeEffect;
import com.tlnt.world.groundeffect.TeleportOutEffect;

public final class MessageHandler {
   public static MessageHandler INSTANCE;
   public final ServerPacketHandler packetHandler = ServerPacketHandler.getInstance();
   public final ObjectMap<Integer, PacketFragment> fragmentMap = new ObjectMap<>();
   public final Queue<IMessage> messageQueue = new Queue<>();

   public static MessageHandler getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new MessageHandler();
      }

      return INSTANCE;
   }

   public final void receiveFragment(FragmentPacket packet) {
      PacketFragment fragment;
      if ((fragment = this.fragmentMap.get(packet.packetId)) == null) {
         fragment = new PacketFragment();
         this.fragmentMap.put(packet.packetId, fragment);
         fragment.packetId = packet.packetId;
         fragment.data = new byte[packet.totalSize];
      }

      throw new NullPointerException();
   }

   public final void receiveMessage(Message message) {
      if (this.packetHandler.loginScreen == null) {
         this.packetHandler.loginScreen = GameMain.instance.getLoginScreen();
      }

      if (message.getOpcode() == 29) {
         try {
            ServerPacketHandler.handlePingResponse(message);
         } catch (Exception e) {
         }
      } else {
         Gdx.app.postRunnable(new HandleMessageTask(this, message));
      }
   }

   public void dispatchMessage(IMessage msg) {
      try {
         label633:
         switch (msg.getOpcode()) {
            case 1:
               ServerPacketHandler handlerAuth = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     GameState.hasSecondaryPassword = msg.readBoolean();
                     GameState.isSecondaryPasswordLocked = msg.readBoolean();
                     handlerAuth.loginScreen.debugScrollPane.refreshHudState();
                  default:
                     break label633;
               }
            case 2:
               this.packetHandler.handlePlayerUpdate(msg);
               break;
            case 3:
               this.packetHandler.handlePlayerSpawn(msg);
               break;
            case 4:
               PlayerEntity leavingPlayer;
               if ((leavingPlayer = GameState.findPlayerById(msg.readInt())) != null) {
                  TeleportOutEffect.POOL2.obtain().playOnPlayer(leavingPlayer);
                  GameState.nearbyPlayers.removeValue(leavingPlayer, true);
                  if (!leavingPlayer.isLocalPlayer()) {
                     PlayerFactory.freePlayer(leavingPlayer);
                  }
               }
               break;
            case 5:
               PlayerEntity movingPlayer = GameState.findPlayerById(msg.readInt());
               switch (msg.readByte()) {
                  case 0:
                     if (movingPlayer != null && !movingPlayer.isLocalPlayer()) {
                        float moveX = msg.readFloat() / 100.0F;
                        float moveY = msg.readFloat() / 100.0F;
                        if (movingPlayer.owner != null) {
                           movingPlayer.owner.setClickTarget(moveX, moveY);
                        } else {
                           movingPlayer.teleportTo(moveX, moveY);
                        }
                     }
                  default:
                     break label633;
               }
            case 6:
               IMessage msgMap = msg;
               ServerPacketHandler handlerMap = this.packetHandler;
               GameMain.instance.showMapLoadingScreen();
               GameState.clearWorldEntities(false);
               WorldMapEntity worldMap;
               (worldMap = handlerMap.loginScreen.editorViewport).setId(msg.readInt());
               worldMap.POOL2 = msg.readString();
               boolean showExit = msg.readBoolean();
               handlerMap.loginScreen.debugScrollPane.POOL2.exitButton.setVisible(showExit);
               worldMap.skeleton = msg.readInt();
               worldMap.POOL.speed = msg.readInt();
               worldMap.POOL.active = msg.readInt();
               worldMap.POOL.autoFlipEnabled = msg.readInt();
               worldMap.POOL.projectile2 = msg.readInt();
               int colliderCount = msg.readInt();
               int solidColliderCount = msg.readInt();
               PetTerrain.CURRENT.reset(0.0F); // the pet walks on exactly these boxes / slopes (PetBrain)

               for (int colliderIndex = 0; colliderIndex < colliderCount; colliderIndex++) {
                  int isTriggerFlag;
                  if (colliderIndex >= solidColliderCount) {
                     isTriggerFlag = 1;
                  } else {
                     isTriggerFlag = 0;
                  }

                  int isTrigger = isTriggerFlag;
                  switch (msgMap.readByte()) {
                     case 0:
                        float boxX = msgMap.readFloat();
                        float boxY = msgMap.readFloat();
                        float boxW = msgMap.readFloat();
                        float boxH = msgMap.readFloat();
                        worldMap.addBoxCollider(boxX, boxY, boxW, boxH, (isTrigger != 0));
                        PetTerrain.CURRENT.add(new PetTerrain.Collider(boxX, boxY, boxW, boxH, isTrigger != 0));
                        break;
                     case 1:
                        float[] polygonPoints = new float[6];

                        for (int pointIndex = 0; pointIndex < polygonPoints.length; pointIndex++) {
                           polygonPoints[pointIndex] = msgMap.readFloat();
                        }

                        worldMap.addPolygonCollider(polygonPoints, (isTrigger != 0));
                        PetTerrain.CURRENT.add(PetTerrain.Collider.triangle(polygonPoints, isTrigger != 0));
                  }
               }

               byte waypointCount = msgMap.readByte();

               for (int waypointIndex = 0; waypointIndex < waypointCount; waypointIndex++) {
                  String waypointName = msgMap.readString();
                  float waypointX = msgMap.readFloat();
                  float waypointY = msgMap.readFloat();
                  float waypointW = msgMap.readFloat();
                  float waypointH = msgMap.readFloat();
                  boolean waypointEnabled = msgMap.readBoolean();
                  worldMap.owner.add(new WaypointEntity(handlerMap.loginScreen, waypointIndex, waypointName, waypointX, waypointY, waypointW, waypointH, waypointEnabled));
               }

               AudioManager.playMusic(msgMap.readString());
               break;
            case 7:
               IMessage msgTiles = msg;
               ServerPacketHandler handlerTiles = this.packetHandler;
               WorldMapEntity tileWorldMap = this.packetHandler.loginScreen.editorViewport;
               byte layerCount = msg.readByte();

               for (int layerIndex = 0; layerIndex < layerCount; layerIndex++) {
                  tileWorldMap.POOL.addTileLayer();

                  for (int rowIndex = 0; rowIndex < tileWorldMap.POOL.active; rowIndex++) {

                     for (int colIndex = 0; colIndex < tileWorldMap.POOL.speed; colIndex++) {
                        TileMapRenderer tileRenderer = tileWorldMap.POOL;
                        MapTile tile = MapTile.getById(msgTiles.readInt());
                        TileMapRenderer renderer = tileRenderer;
                        if (!tileRenderer.projectile.isEmpty()) {
                           renderer.projectile.get(renderer.projectile.size - 1).add(tile);
                        }
                     }
                  }
               }
            case 8:
            case 9:
            case 11:
            case 58:
            default:
               break;
            case 10:
               ServerPacketHandler handlerLogout = this.packetHandler;
               this.packetHandler.loginScreen.debugScrollPane.closeAllDialogs();
               GameMain.instance.showLoginScreen();
               GameState.loginScreenTimestamp = System.currentTimeMillis();
               break;
            case 12:
               ServerPacketHandler handlerTeleport = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     PlayerEntity localPlayer = PlayerEntity.getInstance();
                     float teleportX = msg.readFloat() / 100.0F;
                     float teleportY = msg.readFloat() / 100.0F;
                     if (localPlayer != null) {
                        localPlayer.teleportTo(teleportX, teleportY);
                        handlerTeleport.loginScreen.box2dDebugRenderer.snapToPosition(teleportX, teleportY);
                     }
                     break label633;
                  case 1:
                     PlayerEntity teleportedPlayer = GameState.findPlayerById(msg.readInt());
                     float targetX = msg.readFloat() / 100.0F;
                     float targetY = msg.readFloat() / 100.0F;
                     if (teleportedPlayer != null) {
                        teleportedPlayer.teleportTo(targetX, targetY);
                        if (teleportedPlayer.isLocalPlayer()) {
                           Body playerBody = teleportedPlayer.POOL.getBody();
                           playerBody.setAwake(true);
                        }
                     }
                     break label633;
                  case 2:
                     float velocityX = msg.readFloat() / 100.0F;
                     float velocityY = msg.readFloat() / 100.0F;
                     int velocityDuration = msg.readInt();
                     PlayerEntity.getInstance().movementController.applyServerVelocity(velocityX, velocityY, velocityDuration);
                  default:
                     break label633;
               }
            case 13:
               this.packetHandler.handleMapDecorations(msg);
               break;
            case 14:
               ServerPacketHandler handlerNotice = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     String systemText;
                     GameMain.postFloatingMessage(systemText = msg.readString());
                     GameState.chatManager.addSystemMessage(ChatMessage.obtain(ChatMessage.ChatChannel.SYSTEM, -1, "Hệ thống", systemText));
                     break label633;
                  case 1:
                     String tickerText = msg.readString();
                     handlerNotice.loginScreen.debugScrollPane.POOL2.messageTicker.enqueueMessage(tickerText);
                     GameState.chatManager
                        .addSystemMessage(ChatMessage.obtain(ChatMessage.ChatChannel.SYSTEM, -1, "Hệ thống", tickerText));
                  default:
                     break label633;
               }
            case 15:
               this.packetHandler.handleLoadGameData(msg);
               break;
            case 16:
               this.packetHandler.handleLoadMapData(msg);
               break;
            case 17:
               this.packetHandler.handleLoadItemData(msg);
               break;
            case 18:
               this.packetHandler.handleLoadSkillData(msg);
               break;
            case 19:
               this.packetHandler.handleCheckResources(msg);
               break;
            case 20:
               this.packetHandler.handleMobUpdate(msg);
               break;
            case 21:
               this.packetHandler.handleCombatAction(msg);
               break;
            case 22:
               this.packetHandler.handleInventory(msg);
               break;
            case 23:
               this.packetHandler.handlePlayerStats(msg);
               break;
            case 24:
               this.packetHandler.handleNpcUpdate(msg);
               break;
            case 25:
               IMessage msgZone = msg;
               ServerPacketHandler handlerZone = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     RegionInfo[] regions = new RegionInfo[msg.readInt()];

                     for (int regionIndex = 0; regionIndex < regions.length; regionIndex++) {
                        regions[regionIndex] = new RegionInfo(msgZone.readInt(), msgZone.readByte(), msgZone.readByte(), msgZone.readByte(), msgZone.readByte());
                     }

                     handlerZone.loginScreen.debugScrollPane.findDialog(RegionInfoPopup.class).updateRegionList(regions);
                     handlerZone.loginScreen.debugScrollPane.showWindow(WindowType.ZONE_POPUP);
                     break label633;
                  case 1:
                     int fishZoneCount = msg.readInt();

                     for (int zoneIndex = 0; zoneIndex < fishZoneCount; zoneIndex++) {
                        float zoneX = msgZone.readFloat() / 100.0F;
                        float zoneY = msgZone.readFloat() / 100.0F;
                        float zoneW = msgZone.readFloat() / 100.0F;
                        float zoneH = msgZone.readFloat() / 100.0F;
                        Rectangle zoneRect = new Rectangle(zoneX, zoneY, zoneW, zoneH);
                        int fishCount = msgZone.readInt();

                        for (int fishIndex = 0; fishIndex < fishCount; fishIndex++) {
                           AmbientFish fish;
                           (fish = AmbientFishFactory.obtainFish(
                                 zoneIndex, GameUtils.randomFloat(zoneX, zoneX + zoneW), GameUtils.randomFloat(zoneY, zoneY + zoneH)
                              ))
                              .POOL2 = zoneRect;
                           GameState.ambientFish.add(fish);
                        }
                     }
                     break label633;
                  default:
                     break label633;
               }
            case 26:
               ServerPacketHandler handlerChat = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     int nearbySenderId = msg.readInt();
                     String nearbyText = msg.readString();
                     PlayerEntity nearbySender;
                     if ((nearbySender = GameState.findPlayerById(nearbySenderId)) != null) {
                        if (nearbySender.chatBubble != null) { // created in onAwake: players who just appeared do not have it yet
                           nearbySender.chatBubble.showChatMessage(nearbyText);
                        }
                        ChatMessage nearbyMessage = ChatMessage.obtain(ChatMessage.ChatChannel.NEARBY, nearbySenderId, nearbySender.getCharacterName(), nearbyText);
                        GameState.chatManager.addNearbyMessage(nearbyMessage);
                        handlerChat.loginScreen.debugScrollPane.refreshHudState();
                     }
                     break label633;
                  case 1:
                     int worldSenderId = msg.readInt();
                     String worldSenderName = msg.readString();
                     String worldText = msg.readString();
                     ChatMessage worldMessage = ChatMessage.obtain(ChatMessage.ChatChannel.WORLD, worldSenderId, worldSenderName, worldText);
                     GameState.chatManager.addWorldMessage(worldMessage);
                     handlerChat.loginScreen.debugScrollPane.POOL2.messageTicker.enqueueMessage(worldText);
                     handlerChat.loginScreen.debugScrollPane.refreshHudState();
                     break label633;
                  case 2:
                     int partySenderId = msg.readInt();
                     String partySenderName = msg.readString();
                     String partyText = msg.readString();
                     ChatMessage partyMessage = ChatMessage.obtain(ChatMessage.ChatChannel.PARTY, partySenderId, partySenderName, partyText);
                     ChatManager partyChat = GameState.chatManager;
                     GameState.chatManager.partyMessages.add(partyMessage);
                     if (partyChat.partyMessages.size > 50) {
                        ChatMessage.free(partyChat.partyMessages.removeIndex(0));
                     }

                     partyChat.partyDirty = true;
                     handlerChat.loginScreen.debugScrollPane.refreshHudState();
                     break label633;
                  case 3:
                     int tribeSenderId = msg.readInt();
                     String tribeSenderName = msg.readString();
                     String tribeText = msg.readString();
                     ChatMessage tribeMessage = ChatMessage.obtain(ChatMessage.ChatChannel.TRIBE, tribeSenderId, tribeSenderName, tribeText);
                     ChatManager tribeChat = GameState.chatManager;
                     GameState.chatManager.tribeMessages.add(tribeMessage);
                     if (tribeChat.tribeMessages.size > 50) {
                        ChatMessage.free(tribeChat.tribeMessages.removeIndex(0));
                     }

                     tribeChat.tribeDirty = true;
                     handlerChat.loginScreen.debugScrollPane.refreshHudState();
                     break label633;
                  case 4:
                     int partnerId = msg.readInt();
                     String partnerName = msg.readString();
                     byte partnerClassId = msg.readByte();
                     int partnerLevel = msg.readInt();
                     int privateSenderId = msg.readInt();
                     String privateSenderName = msg.readString();
                     String privateText = msg.readString();
                     int lookupPartnerId = partnerId;
                     ChatManager privateChat = GameState.chatManager;
                     Array.ArrayIterator conversationIt = GameState.chatManager.privateConversations.iterator();

                     PrivateConversation foundConversation;
                     while (true) {
                        if (!conversationIt.hasNext()) {
                           foundConversation = null;
                           break;
                        }

                        PrivateConversation candidateConversation;
                        if ((candidateConversation = (PrivateConversation)conversationIt.next()).partnerId == lookupPartnerId) {
                           foundConversation = candidateConversation;
                           break;
                        }
                     }

                     PrivateConversation conversation = foundConversation;
                     if (foundConversation == null) {
                        conversation = new PrivateConversation(partnerId, partnerName, partnerClassId, partnerLevel);
                        privateChat = GameState.chatManager;
                        GameState.chatManager.privateConversations.add(conversation);
                        privateChat.privateDirty = true;
                     }

                     ChatMessage privateMessage = ChatMessage.obtain(ChatMessage.ChatChannel.PRIVATE, privateSenderId, privateSenderName, privateText);
                     conversation.messages.add(privateMessage);
                     conversation.hasUnread = true;
                     handlerChat.loginScreen.debugScrollPane.refreshHudState();
                  default:
                     break label633;
               }
            case 27:
               switch (msg.readByte()) {
                  case 0:
                     GameState.skill1CooldownStart = System.currentTimeMillis() - msg.readLong();
                     GameState.skill1CooldownDuration = msg.readInt();
                     break label633;
                  case 1:
                     GameState.skill2CooldownStart = System.currentTimeMillis() - msg.readLong();
                     GameState.skill2CooldownDuration = msg.readInt();
                  default:
                     break label633;
               }
            case 28:
               ServerPacketHandler handlerRevive = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     DeathRespawnPopup deathPopup;
                     if ((deathPopup = handlerRevive.loginScreen.debugScrollPane.findDialog(DeathRespawnPopup.class)) != null) {
                        deathPopup.setMessage(msg.readString());
                     }
                     break label633;
                  case 1:
                     PlayerEntity revivedPlayer;
                     if ((revivedPlayer = GameState.findPlayerById(msg.readInt())) != null) {
                        revivedPlayer.setAnimationState(AnimationState.IDLE);
                        ReviveEffect reviveEffect = revivedPlayer.reviveEffect;
                        String reviveAnimation = "revive";
                        revivedPlayer.reviveEffect.playAnimation(reviveAnimation, false, 1.0F);
                        reviveEffect.setEnabled(true);
                        AudioManager.playSound("revive", revivedPlayer.getCenterPosition());
                     }
                  default:
                     break label633;
               }
            case 29:
               ServerPacketHandler.handlePingResponse(msg);
               break;
            case 30:
               this.packetHandler.handleMapItemUpdate(msg);
               break;
            case 31:
               IMessage msgParty = msg;
               ServerPacketHandler handlerParty = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     int partyId;
                     int partyIdCheck = partyId = msg.readInt();
                     if (partyIdCheck == -1) {
                        if (GameState.currentParty != null) {
                           GameState.currentParty.dispose();
                           GameState.currentParty = null;
                        }
                     } else {
                        String partyName = msg.readString();
                        byte partyByte = (byte)(msg.readBoolean() ? 1 : 0);
                        CharacterTemplate leaderSpirit = CharacterTemplate.getById(msg.readInt());
                        long partyCreatedTime = msg.readLong();
                        if (GameState.currentParty == null) {
                           GameState.currentParty = new Party(partyId, partyName, leaderSpirit, partyCreatedTime);
                        } else {
                           GameState.currentParty.id = partyId;
                           GameState.currentParty.name = partyName;
                           GameState.currentParty.autoApprove = (partyByte != 0);
                           GameState.currentParty.leaderSpirit = leaderSpirit;
                           GameState.currentParty.createdTime = partyCreatedTime;
                           GameState.currentParty.members.clear();
                           GameState.currentParty.joinRequests.clear();
                           GameState.currentParty.buffs.clear();
                        }
                        byte memberCount = msg.readByte();

                        for (int memberIndex = 0; memberIndex < memberCount; memberIndex++) {
                           int memberId = msgParty.readInt();
                           String memberName = msgParty.readString();
                           short memberLevel = msgParty.readShort();
                           byte memberSpiritId = msgParty.readByte();
                           partyByte = msgParty.readByte();
                           PartyMember.Role memberRole = PartyMember.Role.values()[msgParty.readByte()];
                           long memberJoinTime = msgParty.readLong();
                           GameState.currentParty.members.add(new PartyMember(memberId, memberName, memberLevel,
                                 memberSpiritId, partyByte, memberRole, memberJoinTime));
                        }

                        byte requestCount = msgParty.readByte();

                        for (int requestIndex = 0; requestIndex < requestCount; requestIndex++) {
                           int requesterId = msgParty.readInt();
                           String requesterName = msgParty.readString();
                           short requesterLevel = msgParty.readShort();
                           CharacterTemplate requesterSpirit = CharacterTemplate.getById(msgParty.readInt());
                           GameState.currentParty.joinRequests.add(new PartyJoinRequest(requesterId, requesterName,
                                 requesterLevel, requesterSpirit));
                        }

                        byte buffCount = msgParty.readByte();

                        for (int buffIndex = 0; buffIndex < buffCount; buffIndex++) {
                           boolean buffActive = msgParty.readBoolean();
                           String buffText = msgParty.readString();
                           GameState.currentParty.buffs.add(new PartyBuffEntry(buffActive, buffText));
                        }
                     }

                     handlerParty.loginScreen.debugScrollPane.refreshHudState();
                     break label633;
                  case 1:
                     Array.ArrayIterator partyIt = GameState.nearbyParties.iterator();

                     while (partyIt.hasNext()) {
                        ((Party)partyIt.next()).dispose();
                     }

                     GameState.nearbyParties.clear();
                     byte nearbyPartyCount = msg.readByte();

                     for (int nearbyPartyIndex = 0; nearbyPartyIndex < nearbyPartyCount; nearbyPartyIndex++) {
                        int nearbyPartyId = msgParty.readInt();
                        String nearbyPartyName = msgParty.readString();
                        CharacterTemplate nearbyPartySpirit = CharacterTemplate.getById(msgParty.readInt());
                        long nearbyPartyCreated = msgParty.readLong();
                        boolean nearbyJoinDisabled = msgParty.readBoolean();
                        Party nearbyParty;
                        (nearbyParty = new Party(nearbyPartyId, nearbyPartyName, nearbyPartySpirit, nearbyPartyCreated)).joinDisabled = nearbyJoinDisabled;
                        GameState.nearbyParties.add(nearbyParty);
                     }

                     handlerParty.loginScreen.debugScrollPane.refreshHudState();
                  default:
                     break label633;
               }
            case 32:
               ServerPacketHandler handlerInvite = this.packetHandler;
               byte inviteType = msg.readByte();
               boolean isInvitation = msg.readBoolean();
               String inviterName = msg.readString();
               String inviteText = msg.readString();
               if (isInvitation) {
                  handlerInvite.loginScreen.debugScrollPane.findDialog(InvitationConfirmPopup.class).setInvitationData(inviteType, inviterName, inviteText);
                  handlerInvite.loginScreen.debugScrollPane.showWindow(WindowType.CONFIRM_POPUP);
               } else {
                  handlerInvite.loginScreen.debugScrollPane.findDialog(GiftcodeEventConfirmDialog.class).showConfirm(inviteType, inviteText);
                  handlerInvite.loginScreen.debugScrollPane.showWindow(WindowType.SMALL_CONFIRM_POPUP);
               }
               break;
            case 33:
               this.packetHandler.handleBagUpdate(msg);
               break;
            case 34:
               IMessage msgFriends = msg;
               ServerPacketHandler handlerFriends = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     FriendEntry.POOL.freeAll(GameState.friends);
                     GameState.friends.clear();
                     int friendCount = msg.readInt();

                     for (int friendIndex = 0; friendIndex < friendCount; friendIndex++) {
                        int friendId = msgFriends.readInt();
                        String friendName = msgFriends.readString();
                        String friendAvatar = msgFriends.readString();
                        short friendLevel = msgFriends.readShort();
                        boolean friendOnline = msgFriends.readBoolean();
                        Array friendList = GameState.friends;
                        FriendEntry friend;
                        (friend = FriendEntry.POOL.obtain()).index = friendIndex;
                        friend.playerId = friendId;
                        friend.name = friendName;
                        friend.avatarIcon = friendAvatar;
                        friend.level = friendLevel;
                        friend.online = friendOnline;
                        friendList.add(friend);
                     }

                     handlerFriends.loginScreen.debugScrollPane.refreshHudState();
                     break label633;
                  case 1:
                     FriendInviteEntry.POOL.freeAll(GameState.friendInvites);
                     GameState.friendInvites.clear();
                     int inviteCount = msg.readInt();

                     for (int inviteIndex = 0; inviteIndex < inviteCount; inviteIndex++) {
                        int inviterId = msgFriends.readInt();
                        String inviterPlayerName = msgFriends.readString();
                        String inviterAvatar = msgFriends.readString();
                        short inviterLevel = msgFriends.readShort();
                        boolean inviterOnline = msgFriends.readBoolean();
                        Array inviteList = GameState.friendInvites;
                        FriendInviteEntry friendInvite;
                        (friendInvite = FriendInviteEntry.POOL.obtain()).index = inviteIndex;
                        friendInvite.playerId = inviterId;
                        friendInvite.name = inviterPlayerName;
                        friendInvite.avatarIcon = inviterAvatar;
                        friendInvite.level = inviterLevel;
                        friendInvite.online = inviterOnline;
                        inviteList.add(friendInvite);
                     }

                     handlerFriends.loginScreen.debugScrollPane.refreshHudState();
                  default:
                     break label633;
               }
            case 35:
               switch (msg.readByte()) {
                  case 0:
                     int rootPlayerId = msg.readInt();
                     boolean rootActive = msg.readBoolean();
                     int rootDuration = msg.readInt();
                     PlayerEntity rootedPlayer;
                     if ((rootedPlayer = GameState.findPlayerById(rootPlayerId)) != null) {
                        if (rootActive) {
                           rootedPlayer.rootEffect.startRoot(rootDuration);
                        } else {
                           rootedPlayer.rootEffect.stopRoot();
                        }
                     }
                     break label633;
                  case 1:
                     int rootMobId = msg.readInt();
                     boolean mobRootActive = msg.readBoolean();
                     int mobRootDuration = msg.readInt();
                     MobEntity rootedMob;
                     if ((rootedMob = GameState.findMobById(rootMobId)) != null) {
                        if (mobRootActive) {
                           rootedMob.target.startRoot(mobRootDuration);
                        } else {
                           rootedMob.target.stopRoot();
                        }
                     }
                     break label633;
                  case 2:
                     int burnPlayerId = msg.readInt();
                     boolean burnActive = msg.readBoolean();
                     int burnDuration = msg.readInt();
                     PlayerEntity burnedPlayer;
                     if ((burnedPlayer = GameState.findPlayerById(burnPlayerId)) != null) {
                        if (burnActive) {
                           burnedPlayer.burnEffect.startBurn(burnDuration);
                        } else {
                           burnedPlayer.burnEffect.stopBurn();
                        }
                     }
                     break label633;
                  case 3:
                     int burnMobId = msg.readInt();
                     boolean mobBurnActive = msg.readBoolean();
                     int mobBurnDuration = msg.readInt();
                     MobEntity burnedMob;
                     if ((burnedMob = GameState.findMobById(burnMobId)) != null) {
                        if (mobBurnActive) {
                           burnedMob.target2.startBurn(mobBurnDuration);
                        } else {
                           burnedMob.target2.stopBurn();
                        }
                     }
                     break label633;
                  case 4:
                     int weakenPlayerId = msg.readInt();
                     boolean weakenActive = msg.readBoolean();
                     int weakenDuration = msg.readInt();
                     PlayerEntity weakenedPlayer;
                     if ((weakenedPlayer = GameState.findPlayerById(weakenPlayerId)) != null) {
                        if (weakenActive) {
                           weakenedPlayer.weakenEffect.startWeaken(weakenDuration);
                        } else {
                           weakenedPlayer.weakenEffect.stopWeaken();
                        }
                     }
                     break label633;
                  case 5:
                     int weakenMobId = msg.readInt();
                     boolean mobWeakenActive = msg.readBoolean();
                     int mobWeakenDuration = msg.readInt();
                     MobEntity weakenedMob;
                     if ((weakenedMob = GameState.findMobById(weakenMobId)) != null) {
                        if (mobWeakenActive) {
                           weakenedMob.owner.startWeaken(mobWeakenDuration);
                        } else {
                           weakenedMob.owner.stopWeaken();
                        }
                     }
                     break label633;
                  case 6:
                     int stunPlayerId = msg.readInt();
                     boolean stunActive = msg.readBoolean();
                     int stunDuration = msg.readInt();
                     PlayerEntity stunnedPlayer;
                     if ((stunnedPlayer = GameState.findPlayerById(stunPlayerId)) != null) {
                        if (stunActive) {
                           stunnedPlayer.stunEffect.startStun(stunDuration);
                        } else {
                           stunnedPlayer.stunEffect.stopStun();
                        }
                     }
                     break label633;
                  case 7:
                     int stunMobId = msg.readInt();
                     boolean mobStunActive = msg.readBoolean();
                     int mobStunDuration = msg.readInt();
                     MobEntity stunnedMob;
                     if ((stunnedMob = GameState.findMobById(stunMobId)) != null) {
                        if (mobStunActive) {
                           stunnedMob.stunEffect.startStun(mobStunDuration);
                        } else {
                           stunnedMob.stunEffect.stopStun();
                        }
                     }
                     break label633;
                  case 8:
                     int icePlayerId = msg.readInt();
                     boolean iceActive = msg.readBoolean();
                     int iceDuration = msg.readInt();
                     PlayerEntity icedPlayer;
                     if ((icedPlayer = GameState.findPlayerById(icePlayerId)) != null) {
                        if (iceActive) {
                           icedPlayer.iceSpikeEffect.startIceSpike(iceDuration);
                        } else {
                           icedPlayer.iceSpikeEffect.stopIceSpike();
                        }
                     }
                     break label633;
                  case 9:
                     int iceMobId = msg.readInt();
                     boolean mobIceActive = msg.readBoolean();
                     int mobIceDuration = msg.readInt();
                     MobEntity icedMob;
                     if ((icedMob = GameState.findMobById(iceMobId)) != null) {
                        if (mobIceActive) {
                           icedMob.movementComponent.startIceSpike(mobIceDuration);
                        } else {
                           icedMob.movementComponent.stopIceSpike();
                        }
                     }
                     break label633;
                  case 10:
                     PlayerEntity lightningPlayer;
                     if ((lightningPlayer = GameState.findPlayerById(msg.readInt())) != null) {
                        LightningStrikeEffect.POOL2.obtain().playAt(lightningPlayer.getPosition().x, lightningPlayer.getPosition().y);
                     }
                     break label633;
                  case 11:
                     MobEntity lightningMob;
                     if ((lightningMob = GameState.findMobById(msg.readInt())) != null) {
                        LightningStrikeEffect.POOL2.obtain().playAt(lightningMob.getPosition().x, lightningMob.getPosition().y);
                     }
                     break label633;
                  case 12:
                     PlayerEntity earthSpikePlayer;
                     if ((earthSpikePlayer = GameState.findPlayerById(msg.readInt())) != null) {
                        EarthSpikeEffect.POOL2.obtain().playAt(earthSpikePlayer.getPosition().x, earthSpikePlayer.getPosition().y);
                     }
                     break label633;
                  case 13:
                     MobEntity earthSpikeMob;
                     if ((earthSpikeMob = GameState.findMobById(msg.readInt())) != null) {
                        EarthSpikeEffect.POOL2.obtain().playAt(earthSpikeMob.getPosition().x, earthSpikeMob.getPosition().y);
                     }
                  default:
                     break label633;
               }
            case 36:
               this.packetHandler.handleTradeUpdate(msg);
               break;
            case 37:
               this.packetHandler.handlePetUpdate(msg);
               break;
            case 38:
               GameMain.instance.getGameScreen().active.showDialog(WindowType.CREATE_PLAYER_PANEL);
               break;
            case 39:
               IMessage msgRank = msg;
               ServerPacketHandler handlerRank = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     byte categoryCount = msg.readByte();
                     if (GameState.rankingCategories.size != categoryCount) {
                        GameState.rankingCategories.clear();

                        for (int newCategoryIndex = 0; newCategoryIndex < categoryCount; newCategoryIndex++) {
                           int categoryId = msgRank.readInt();
                           String categoryName = msgRank.readString();
                           String categoryDescription = msgRank.readString();
                           GameState.rankingCategories.add(new RankingCategory(categoryId, categoryName, categoryDescription));
                        }
                     } else {
                        for (int categoryIndex = 0; categoryIndex < categoryCount; categoryIndex++) {
                           GameState.rankingCategories.get(categoryIndex).categoryId = msgRank.readInt();
                           GameState.rankingCategories.get(categoryIndex).name = msgRank.readString();
                           GameState.rankingCategories.get(categoryIndex).description = msgRank.readString();
                        }
                     }

                     handlerRank.loginScreen.debugScrollPane.refreshHudState();
                     break label633;
                  case 1:
                     int rankCategoryId = msg.readInt();
                     RankingCategory rankCategory = null;
                     Array.ArrayIterator categoryIt = GameState.rankingCategories.iterator();

                     while (categoryIt.hasNext()) {
                        RankingCategory candidateCategory;
                        if ((candidateCategory = (RankingCategory)categoryIt.next()).categoryId == rankCategoryId) {
                           rankCategory = candidateCategory;
                           break;
                        }
                     }

                     if (rankCategory != null) {
                        rankCategory.entries.clear();
                        int rankEntryCount = msg.readInt();

                        for (int rankEntryIndex = 0; rankEntryIndex < rankEntryCount; rankEntryIndex++) {
                           int rank = msgRank.readInt();
                           int rankPlayerId = msgRank.readInt();
                           String rankPlayerName = msgRank.readString();
                           String rankValue = msgRank.readString();
                           byte rankClassId = msgRank.readByte();
                           rankCategory.entries.add(new RankEntry(rank, rankPlayerId, rankPlayerName, rankValue, rankClassId));
                        }

                        int myRank = msgRank.readInt();
                        int myPlayerId = msgRank.readInt();
                        String myPlayerName = msgRank.readString();
                        String myRankValue = msgRank.readString();
                        byte myClassId = msgRank.readByte();
                        GameState.myRankEntry = new RankEntry(myRank, myPlayerId, myPlayerName, myRankValue, myClassId);
                        handlerRank.loginScreen.debugScrollPane.refreshHudState();
                     }
                  default:
                     break label633;
               }
            case 40:
               this.packetHandler.handleFriendOrGuildList(msg);
               break;
            case 41:
               ServerPacketHandler handlerEvent = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     PlayerEntity partyStatePlayer;
                     if ((partyStatePlayer = GameState.findPlayerById(msg.readInt())) != null) {
                        partyStatePlayer.POOL2.isInParty = msg.readBoolean();
                        if (partyStatePlayer.POOL2.isInParty) {
                           partyStatePlayer.POOL2.partyLeaderId = msg.readInt();
                        }
                     }
                  case 1:
                  case 2:
                  default:
                     break label633;
                  case 3:
                     byte eventAnimationIndex = msg.readByte();
                     SkeletonActor eventActor = handlerEvent.loginScreen.debugScrollPane.POOL2.eventSkeletonActor;
                     switch (eventAnimationIndex) {
                        case 0:
                           eventActor.playAnimation("Start", false);
                           break label633;
                        case 1:
                           eventActor.playAnimation("1", false);
                           break label633;
                        case 2:
                           String eventAnimationName = "2";
                           eventActor.playAnimation(eventAnimationName, false);
                           break label633;
                        case 3:
                           eventActor.playAnimation("3", false);
                        default:
                           break label633;
                     }
               }
            case 42:
               switch (msg.readByte()) {
                  case 0:
                     int factionPlayerId = msg.readInt();
                     byte factionType = msg.readByte();
                     PlayerEntity factionPlayer;
                     if ((factionPlayer = GameState.findPlayerById(factionPlayerId)) != null) {
                        factionPlayer.POOL2.factionStatus.faction = FactionStatus.FactionType.values()[factionType];
                     }
                  default:
                     break label633;
               }
            case 43:
               IMessage msgServers = msg;
               GameState.serverList.clear();
               byte serverCount = msg.readByte();

               for (int serverIndex = 0; serverIndex < serverCount; serverIndex++) {
                  String serverName = msgServers.readString();
                  String serverHost = msgServers.readString();
                  int serverPort = msgServers.readInt();
                  GameState.serverList.add(new ServerInfo(serverName, serverHost, serverPort));
                  Settings.saveServerEntry(serverIndex, serverName, serverHost, serverPort);
               }

               Settings.saveServerCount(serverCount);
               break;
            case 44:
               this.packetHandler.handlePartyUpdate(msg);
               break;
            case 45:
               IMessage msgCountdown = msg;
               ServerPacketHandler handlerCountdown = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     GameState.appleEventState.active = msg.readBoolean();
                     if (GameState.appleEventState.active) {
                        GameState.appleEventState.expiryTime = System.currentTimeMillis() + msg.readLong();
                     }
                     break label633;
                  case 1:
                     String[] countdownOptions = new String[msg.readByte()];

                     for (int optionIndex = 0; optionIndex < countdownOptions.length; optionIndex++) {
                        countdownOptions[optionIndex] = msgCountdown.readString();
                     }

                     handlerCountdown.loginScreen.debugScrollPane.findDialog(CountdownPopup.class).setCountdownOptions(countdownOptions);
                  default:
                     break label633;
               }
            case 46:
               IMessage msgSettings = msg;

               for (int slotIndex = 0; slotIndex < Settings.autoSkillSlotEnabled.length; slotIndex++) {
                  Settings.autoSkillSlotEnabled[slotIndex] = msgSettings.readBoolean();
               }

               Settings.autoIgnorePlayersAndBosses = msgSettings.readBoolean();
               Settings.autoAvoidEliteMobs = msgSettings.readBoolean();
               Settings.autoRevive = msgSettings.readBoolean();
               Settings.autoPickupItems = msgSettings.readBoolean();
               Settings.autoUseStaminaItems = msgSettings.readBoolean();
               Settings.autoUseHpItems = msgSettings.readBoolean();
               Settings.autoUseMpItems = msgSettings.readBoolean();
               Settings.autoHarvestApples = msgSettings.readBoolean();
               Settings.autoHpThresholdPercent = msgSettings.readByte();
               Settings.autoMpThresholdPercent = msgSettings.readByte();
               GameState.inventory.inventoryFlag1 = msgSettings.readBoolean();
               GameState.inventory.inventoryFlag2 = msgSettings.readBoolean();
               GameState.inventory.inventoryFlag3 = msgSettings.readBoolean();
               GameState.inventory.inventoryFlag4 = msgSettings.readBoolean();
               GameState.inventory.hideBackpack = msgSettings.readBoolean(); // 5th flag "Hide backwear" (decompiled C3873)

               for (int keybindIndex = 0; keybindIndex < Settings.keybindings.length; keybindIndex++) {
                  Settings.keybindings[keybindIndex] = msgSettings.readInt();
               }

               Settings.hideOtherPlayers = msgSettings.readBoolean();
               // "Attack range" (decompiled C3873 reads an int after the 2 %HP / %MP bytes; this server appends it at the end — old servers do not send it)
               Settings.autoAttackRange = msgSettings instanceof Message && ((Message)msgSettings).inputStream != null
                     && ((Message)msgSettings).inputStream.available() >= 4 ? msgSettings.readInt() : 0;
               break;
            case 47:
               this.packetHandler.handleQuestUpdate(msg);
               break;
            case 48:
               IMessage msgStats1 = msg;
               ServerPacketHandler handlerStats1 = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     GameState.inventory.clearSkillDescriptions1();
                     int weaponStatCount = msg.readInt();

                     for (int weaponStatIndex = 0; weaponStatIndex < weaponStatCount; weaponStatIndex++) {
                        int weaponStatTypeId = msgStats1.readInt();
                        Integer[] weaponStatValues = new Integer[msgStats1.readByte()];

                        for (int weaponValueIndex = 0; weaponValueIndex < weaponStatValues.length; weaponValueIndex++) {
                           weaponStatValues[weaponValueIndex] = msgStats1.readInt();
                        }

                        GameState.inventory.skillDescriptions1.add(new StatLine(StatType.getById(weaponStatTypeId), weaponStatValues, null));
                     }

                     StatLine.mergeStatLines(GameState.inventory.skillDescriptions1);
                     handlerStats1.loginScreen.debugScrollPane.refreshHudState();
                     break label633;
                  case 1:
                     int weaponSlotIndex = msg.readInt();
                     GameState.inventory.selectedWeaponSlot = weaponSlotIndex != -1
                        ? GameState.inventory.weaponSlots.get(weaponSlotIndex)
                        : null;
                     handlerStats1.loginScreen.debugScrollPane.refreshHudState();
                  default:
                     break label633;
               }
            case 49:
               switch (msg.readByte()) {
                  case 0:
                     GameState.captchaTexture.applyTextureBytes(msg.readBytes());
                  default:
                     break label633;
               }
            case 50:
               IMessage msgStats2 = msg;
               ServerPacketHandler handlerStats2 = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     GameState.inventory.clearSkillDescriptions2();
                     int armorStatCount = msg.readInt();

                     for (int armorStatIndex = 0; armorStatIndex < armorStatCount; armorStatIndex++) {
                        int armorStatTypeId = msgStats2.readInt();
                        Integer[] armorStatValues = new Integer[msgStats2.readByte()];

                        for (int armorValueIndex = 0; armorValueIndex < armorStatValues.length; armorValueIndex++) {
                           armorStatValues[armorValueIndex] = msgStats2.readInt();
                        }

                        GameState.inventory.skillDescriptions2.add(new StatLine(StatType.getById(armorStatTypeId), armorStatValues, null));
                     }

                     StatLine.mergeStatLines(GameState.inventory.skillDescriptions2);
                     handlerStats2.loginScreen.debugScrollPane.refreshHudState();
                     break label633;
                  case 1:
                     int armorSlotIndex = msg.readInt();
                     GameState.inventory.selectedArmorSlot = armorSlotIndex != -1 ? GameState.inventory.armorSlots.get(armorSlotIndex) : null;
                     handlerStats2.loginScreen.debugScrollPane.refreshHudState();
                  default:
                     break label633;
               }
            case 51:
               IMessage msgBuffs = msg;
               ServerPacketHandler handlerBuffs = this.packetHandler;
               if (PlayerEntity.getInstance() != null) {
                  switch (msg.readByte()) {
                     case 0:
                        GameState.activeBuffs.clear();
                        byte activeBuffCount = msg.readByte();

                        for (int activeBuffIndex = 0; activeBuffIndex < activeBuffCount; activeBuffIndex++) {
                           int buffTemplateId = msgBuffs.readInt();
                           long buffStartTime = System.currentTimeMillis();
                           int buffDuration = msgBuffs.readInt();
                           GameState.activeBuffs.add(new OptionInfo(buffTemplateId, buffStartTime, buffDuration));
                        }

                        handlerBuffs.loginScreen.debugScrollPane.refreshHudState();
                  }
               }
               break;
            case 52:
               this.packetHandler.handleAnimalUpdate(msg);
               break;
            case 53:
               ServerPacketHandler handlerBox = this.packetHandler;
               switch (msg.readByte()) {
                  case 0:
                     handlerBox.loginScreen.debugScrollPane.showWindow(WindowType.BOX_POPUP);
                     break label633;
                  case 2:
                     // Spirit Badge > "Offline Training" (decompiled C3873 m18132): Settings window, auto-attack tab
                     handlerBox.loginScreen.debugScrollPane.showWindow(WindowType.SETTING_2_POPUP);
                     handlerBox.loginScreen.debugScrollPane
                        .findDialog(com.tlnt.ui.window.controls.ControlsWindow.class)
                        .selectTabByIndex(1);
                     break label633;
                  default:
                     break label633;
               }
            case 54:
               this.packetHandler.handlePlantInteraction(msg);
               break;
            case 55:
               this.packetHandler.handleShopUpdate(msg);
               break;
            case 56:
               this.packetHandler.handleTradeWindow(msg);
               break;
            case 57:
               this.packetHandler.handleGatherAction(msg);
               break;
            case 59:
               ServerPacketHandler.handleDecryptKeys(msg);
               break;
            case 60:
               this.packetHandler.handleGuildSkillTree(msg);
               break;
            case 61:
               this.packetHandler.handleSkillSlots(msg);
               break;
            case 62:
               this.packetHandler.handleLeaderboard(msg);
               break;
            case 63:
               ServerPacketHandler.handleOffscreenTarget(msg);
               break;
            case 64:
               this.packetHandler.handleEquippedSkills(msg);
               break;
            case 65:
               this.packetHandler.handleRewardPopup(msg);
               break;
            case 66:
               this.packetHandler.handlePassiveSkills(msg);
               break;
            case 67:
               ServerPacketHandler.handleChatMessage(msg);
               break;
            case 68:
               this.packetHandler.handleDungeonUpdate(msg);
               break;
            case 69:
               this.packetHandler.handleAuctionUpdate(msg);
               break;
            case 70:
               this.packetHandler.handleShowDialog(msg);
               break;
            case 71:
               this.packetHandler.handleCraftBook(msg);
               break;
            case 73:
               this.packetHandler.handleWeeklyTop(msg);
                break;
            case 72: // recharge (decompiled C3873 m18088): 0 package list, 1 str link -> open the browser
               switch (msg.readByte()) {
                  case 0:
                     GameState.rechargePacks.clear();
                     int packCount = msg.readInt();

                     for (int i = 0; i < packCount; i++) {
                        int packId = msg.readInt();
                        String packName = msg.readString();
                        String packIcon = msg.readString();
                        int packPrice = msg.readInt();
                        String packDesc = msg.readString();
                        GameState.rechargePacks.add(new com.tlnt.ui.window.recharge.RechargeWindow.Pack(packId, packName, packIcon, packPrice, packDesc));
                     }

                     this.packetHandler.loginScreen.debugScrollPane.refreshHudState();
                     break;
                  case 1:
                     com.badlogic.gdx.Gdx.net.openURI(msg.readString());
                     break;
                  default:
                     break;
               }
               break;
            case 75: // standalone ranking (decompiled C3873 m18111): column name, byte n, n × (id, name, character byte, value)
               this.packetHandler.handleOtherRank(msg);
               break;
            case 100: // NEW: World Boss table (sub 0 list, sub 1 enable auto-pathing)
               this.packetHandler.handleWorldBoss(msg);
               break;
            case 79: // sound at a player (decompiled m18107): sub 0 int id, str sound name
               if (msg.readByte() == 0) {
                  int soundPlayerId = msg.readInt();
                  String soundName = msg.readString();
                  PlayerEntity soundPlayer = GameState.findPlayerById(soundPlayerId);
                  if (soundPlayer != null) {
                     AudioManager.playSound(soundName, soundPlayer.getCenterPosition());
                  }
               }
               break;
            case 80: // floating object (decompiled m18077): sub 0 int id, int map_effect template, float x, y, x mark (÷100), float speed
               if (msg.readByte() == 0) {
                  int floatId = msg.readInt();
                  int floatTemplate = msg.readInt();
                  float floatX = msg.readFloat() / 100.0F;
                  float floatY = msg.readFloat() / 100.0F;
                  float floatEndX = msg.readFloat() / 100.0F;
                  float floatSpeed = msg.readFloat();
                  com.tlnt.world.entity.floataway.FloatAwayEffect.spawn(floatId, floatTemplate, floatX, floatY, floatEndX, floatSpeed);
               }
               break;
            case 81: // map decorations (decompiled m18092): 0 list, 1 add one (int id, str image, float x, y), 2 remove (int id)
               switch (msg.readByte()) {
                  case 0:
                     int propCount = msg.readInt();

                     for (int i = 0; i < propCount; i++) {
                        int propId = msg.readInt();
                        String propTexture = msg.readString();
                        float propX = msg.readFloat() / 100.0F;
                        float propY = msg.readFloat() / 100.0F;
                        com.tlnt.world.entity.mapprop.MapPropEntity.spawn(propId, propTexture, propX, propY);
                     }
                     break;
                  case 1:
                     int addPropId = msg.readInt();
                     String addPropTexture = msg.readString();
                     float addPropX = msg.readFloat() / 100.0F;
                     float addPropY = msg.readFloat() / 100.0F;
                     com.tlnt.world.entity.mapprop.MapPropEntity.spawn(addPropId, addPropTexture, addPropX, addPropY);
                     break;
                  case 2:
                     com.tlnt.world.entity.mapprop.MapPropEntity.remove(msg.readInt());
                     break;
                  default:
                     break;
               }
               break;
            case 82: // batched positions of several players (decompiled m18084): byte n, n × (int id, float x, float y)
               byte batchCount = msg.readByte();

               for (int i = 0; i < batchCount; i++) {
                  PlayerEntity batchPlayer = GameState.findPlayerById(msg.readInt());
                  float batchX = msg.readFloat() / 100.0F;
                  float batchY = msg.readFloat() / 100.0F;
                  if (batchPlayer != null && !batchPlayer.isLocalPlayer()) {
                     if (batchPlayer.owner != null) {
                        batchPlayer.owner.setClickTarget(batchX, batchY);
                     } else {
                        batchPlayer.teleportTo(batchX, batchY);
                     }
                  }
               }
               break;
             case 78:
                this.packetHandler.handleBackwearSkills(msg);
         }

         msg.close();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

}
