package com.tlnt.net.task;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.shimaota.a.a.Message;
import com.tlnt.core.GameDataLoader;
import com.tlnt.core.GameState;
import com.tlnt.core.Settings;
import com.tlnt.model.item.Item;
import com.tlnt.net.INetworkClient;
import java.util.Locale;

public final class ServerPacketSender implements Disposable {
   public interface NetworkClientFactory {
      INetworkClient createClient();
   }

   private static NetworkClientFactory clientFactory;

   public static void setNetworkClientFactory(NetworkClientFactory factory) {
      clientFactory = factory;
   }

   public static INetworkClient createDefaultClient() {
      if (clientFactory != null) {
         return clientFactory.createClient();
      }
      try {
         Class<?> clazz = Class.forName("com.tlnt.net.KryoNetworkClient");
         return (INetworkClient) clazz.getDeclaredConstructor().newInstance();
      } catch (Throwable t) {
         System.err.println("Default KryoNetworkClient not available: " + t);
         return null;
      }
   }

   public static ServerPacketSender INSTANCE;
   public INetworkClient networkClient;

   public static ServerPacketSender getInstance() {
      if (INSTANCE == null) {
         INSTANCE = new ServerPacketSender();
      }

      return INSTANCE;
   }

   public ServerPacketSender() {
      this.networkClient = createDefaultClient();
      this.connectToServer(0);
   }

   public final void connectToServer(int delayMillis) {
      if (this.networkClient != null && !this.networkClient.isConnected()) {
         if (delayMillis <= 0) {
            this.networkClient.connectAndSetup();
         } else {
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
               @Override
               public void run() {
                  if (networkClient != null && !networkClient.isConnected()) {
                     networkClient.connectAndSetup();
                  }
               }
            }, delayMillis / 1000.0f);
         }
      }
   }

   public final void sendHandshake() {
      Message msg = new Message(0);

      try {
         msg.writeString(com.tlnt.core.ClientConfig.VERSION);
         msg.writeString(Gdx.app.getType().name());
         msg.writeString(Locale.getDefault().getLanguage() + "|" + Locale.getDefault().getCountry());
         msg.writeInt(GameDataLoader.gameDataVersion);
         msg.writeInt(GameDataLoader.mapDataVersion);
         msg.writeInt(GameDataLoader.itemDataVersion);
         msg.writeInt(GameDataLoader.skillDataVersion);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPlayerMove(float x, float y) {
      Message msg = new Message(5);

      try {
         msg.writeByte(0);
         msg.writeFloat(x * 100.0F);
         msg.writeFloat(y * 100.0F);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPlayerAction(int waypointId) {
      Message msg = new Message(11);

      try {
         msg.writeByte(waypointId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSkillbarAction(int skillId, int targetKind, int targetId) {
      Message msg = new Message(21);

      try {
         msg.writeByte(0);
         msg.writeInt(skillId);
         msg.writeByte(targetKind);
         msg.writeInt(targetId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSkillUse(int skillId) {
      Message msg = new Message(21);

      try {
         msg.writeByte(1);
         msg.writeInt(skillId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendAttack(int skillId, int points) {
      Message msg = new Message(21);

      try {
         msg.writeByte(2);
         msg.writeInt(skillId);
         msg.writeInt(points);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendDropItem(int skillId) {
      Message msg = new Message(21);

      try {
         msg.writeByte(3);
         msg.writeInt(skillId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendEquipItem(int skillId, int slotIndex) {
      Message msg = new Message(21);

      try {
         msg.writeByte(4);
         msg.writeInt(skillId);
         msg.writeByte(slotIndex);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendUnequipItem(int targetId, int templateId) {
      Message msg = new Message(24);

      try {
         msg.writeByte(0);
         msg.writeByte(targetId);
         msg.writeInt(templateId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendMoveItem(int npcType, int npcId, int optionIndex) {
      Message msg = new Message(24);

      try {
         msg.writeByte(1);
         msg.writeByte(npcType);
         msg.writeInt(npcId);
         msg.writeByte(optionIndex);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestInventory() {
      Message msg = new Message(25);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSplitItem(int regionId) {
      Message msg = new Message(25);

      try {
         msg.writeByte(1);
         msg.writeInt(regionId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSellItem(int mapId, int targetMapId) {
      Message msg = new Message(25);

      try {
         msg.writeByte(2);
         msg.writeInt(mapId);
         msg.writeInt(targetMapId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestShop() {
      Message msg = new Message(25);

      try {
         msg.writeByte(3);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendChat(String text) {
      Message msg = new Message(26);

      try {
         msg.writeByte(0);
         msg.writeString(text);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPrivateChat(String text) {
      Message msg = new Message(26);

      try {
         msg.writeByte(1);
         msg.writeString(text);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendGuildChat(String text) {
      Message msg = new Message(26);

      try {
         msg.writeByte(2);
         msg.writeString(text);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPartyChat(String text) {
      Message msg = new Message(26);

      try {
         msg.writeByte(3);
         msg.writeString(text);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendWhisper(String recipientName, String text) {
      Message msg = new Message(26);

      try {
         msg.writeByte(4);
         msg.writeString(recipientName);
         msg.writeString(text);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPickupItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(0);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendUseItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(1);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendCraftItem(int itemSlot, int quantity) {
      Message msg = new Message(27);

      try {
         msg.writeByte(2);
         msg.writeInt(itemSlot);
         msg.writeInt(quantity);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestCraft(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(3);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRepairItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(4);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendIdentifyItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(5);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendDestroyItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(6);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendLockItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(7);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSwapItems(int itemId, int quickSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(8);
         msg.writeInt(itemId);
         msg.writeByte(quickSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendDepositItem(int quickSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(9);
         msg.writeByte(quickSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendWithdrawItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(10);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendStorageAction(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(11);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendEnchantItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(12);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRefineItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(13);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSocketItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(14);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendExtractSocket(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(15);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendCompositeItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(16);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendDecompositeItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(17);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendTransmuteItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(18);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendInheritItem(int itemSlot) {
      Message msg = new Message(27);

      try {
         msg.writeByte(19);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public void trySendMessage(Message msg) {
      if (this.networkClient.isConnected()) {
         this.networkClient.sendTcp(msg);
      }
   }

   public final void sendRequestQuest() {
      Message msg = new Message(28);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestDungeon() {
      Message msg = new Message(28);

      try {
         msg.writeByte(1);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPing() {
      Message msg = new Message(29);

      try {
         msg.writeLong(System.currentTimeMillis());
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendQuestAction(int targetId) {
      Message msg = new Message(30);

      try {
         msg.writeByte(0);
         msg.writeInt(targetId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendEmote(byte typeId) {
      Message msg = new Message(32);

      try {
         msg.writeByte(typeId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendChannelChat(byte channel, String text) {
      Message msg = new Message(70);

      try {
         msg.writeByte(channel);
         msg.writeString(text);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendOpenStorage() {
      Message msg = new Message(31);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestLeaderboard() {
      Message msg = new Message(31);

      try {
         msg.writeByte(1);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendLeaderboardAction(int targetId) {
      Message msg = new Message(31);

      try {
         msg.writeByte(2);
         msg.writeInt(targetId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestGuild() {
      Message msg = new Message(31);

      try {
         msg.writeByte(3);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestParty() {
      Message msg = new Message(31);

      try {
         msg.writeByte(4);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPartyAction(int partyId) {
      Message msg = new Message(31);

      try {
         msg.writeByte(5);
         msg.writeInt(partyId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPartyInvite(int playerId) {
      Message msg = new Message(31);

      try {
         msg.writeByte(6);
         msg.writeInt(playerId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestFriends() {
      Message msg = new Message(31);

      try {
         msg.writeByte(7);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendFriendAction(int itemId) {
      Message msg = new Message(31);

      try {
         msg.writeByte(8);
         msg.writeInt(itemId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendGuildAction(int itemId) {
      Message msg = new Message(31);

      try {
         msg.writeByte(9);
         msg.writeInt(itemId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendNpcInteract(int eventId, int eventDataId) {
      Message msg = new Message(33);

      try {
         msg.writeByte(0);
         msg.writeByte(eventId);
         msg.writeByte(eventDataId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendDialogResponse(String code) {
      Message msg = new Message(33);

      try {
         msg.writeByte(1);
         msg.writeString(code);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestMap() {
      Message msg = new Message(34);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestSkills() {
      Message msg = new Message(34);

      try {
         msg.writeByte(1);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSkillAction(int itemId) {
      Message msg = new Message(34);

      try {
         msg.writeByte(2);
         msg.writeInt(itemId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendLearnSkill(int inviteIndex) {
      Message msg = new Message(34);

      try {
         msg.writeByte(3);
         msg.writeInt(inviteIndex);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendUpgradeSkill(int inviteIndex) {
      Message msg = new Message(34);

      try {
         msg.writeByte(4);
         msg.writeInt(inviteIndex);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendResetSkills(int playerId) {
      Message msg = new Message(34);

      try {
         msg.writeByte(5);
         msg.writeInt(playerId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestStats() {
      Message msg = new Message(36);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendBuyItem(int itemSlot, int quantity, int price, int categoryId) {
      Message msg = new Message(36);

      try {
         msg.writeByte(1);
         msg.writeInt(itemSlot);
         msg.writeInt(quantity);
         msg.writeInt(price);
         msg.writeByte(categoryId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestTrade(int itemSlot) {
      Message msg = new Message(36);

      try {
         msg.writeByte(2);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendTradeAction(int itemSlot) {
      Message msg = new Message(36);

      try {
         msg.writeByte(3);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendConfirmTrade(int itemSlot) {
      Message msg = new Message(36);

      try {
         msg.writeByte(4);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendMarketSearch(String query) {
      Message msg = new Message(37);

      try {
         msg.writeByte(0);
         msg.writeString(query);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendMarketBuy(String tribeName) {
      Message msg = new Message(37);

      try {
         msg.writeByte(1);
         msg.writeString(tribeName);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendMarketSell(int tribeId) {
      Message msg = new Message(37);

      try {
         msg.writeByte(2);
         msg.writeInt(tribeId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendMarketCancel(int playerId) {
      Message msg = new Message(37);

      try {
         msg.writeByte(3);
         msg.writeInt(playerId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendMarketAction(int playerId) {
      Message msg = new Message(37);

      try {
         msg.writeByte(4);
         msg.writeInt(playerId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestMarket() {
      Message msg = new Message(37);

      try {
         msg.writeByte(5);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestAuction() {
      Message msg = new Message(37);

      try {
         msg.writeByte(6);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendAuctionBid(int amount) {
      Message msg = new Message(37);

      try {
         msg.writeByte(7);
         msg.writeInt(amount);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendAuctionAction(int amount) {
      Message msg = new Message(37);

      try {
         msg.writeByte(8);
         msg.writeInt(amount);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestPet() {
      Message msg = new Message(37);

      try {
         msg.writeByte(9);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPetAction(int skillId) {
      Message msg = new Message(37);

      try {
         msg.writeByte(10);
         msg.writeInt(skillId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPetFeed(int itemId) {
      Message msg = new Message(37);

      try {
         msg.writeByte(11);
         msg.writeInt(itemId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPetEquip(int itemId) {
      Message msg = new Message(37);

      try {
         msg.writeByte(12);
         msg.writeInt(itemId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPetSkill(int itemId) {
      Message msg = new Message(37);

      try {
         msg.writeByte(13);
         msg.writeInt(itemId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPetRename(int itemId) {
      Message msg = new Message(37);

      try {
         msg.writeByte(14);
         msg.writeInt(itemId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPetRelease(int playerId) {
      Message msg = new Message(37);

      try {
         msg.writeByte(15);
         msg.writeInt(playerId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSetTitle(String text) {
      Message msg = new Message(37);

      try {
         msg.writeByte(16);
         msg.writeString(text);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendEncryptedPacket(String name, int classId) {
      Message msg = new Message(38);

      try {
         msg.writeString(name);
         msg.writeByte(classId);
         this.networkClient.sendTcp(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestRanking(int categoryId) {
      Message msg = new Message(39);

      try {
         msg.writeByte(0);
         msg.writeInt(categoryId);
         this.networkClient.sendTcp(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   /** Requests the leaderboard of a Weekly Top event (opcode 73, sub 0, int eventId). */
   public final void sendRequestWeeklyTop(int eventId) {
      Message msg = new Message(73);

      try {
         msg.writeByte(0);
         msg.writeInt(eventId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendMultiItemAction(int actionType, int[] ids) {
      if (ids.length != 0) {
         Message msg = new Message(40);

         try {
            msg.writeByte(4);
            msg.writeByte(actionType);
            msg.writeByte(ids.length);

            for (int i = 0; i < ids.length; i++) {
               msg.writeInt(ids[i]);
            }

            this.networkClient.sendTcp(msg);
            msg.close();
         } catch (Exception e) {
         }
      }
   }

   public final void sendSortInventory(int mailId) {
      Message msg = new Message(40);

      try {
         msg.writeByte(3);
         msg.writeInt(mailId);
         this.networkClient.sendTcp(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestSettings() {
      Message msg = new Message(40);

      try {
         msg.writeByte(5);
         this.networkClient.sendTcp(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRankingAction(int mailId) {
      Message msg = new Message(40);

      try {
         msg.writeByte(0);
         msg.writeInt(mailId);
         this.networkClient.sendTcp(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendEquipmentSet(String recipient, String subject, String body, Array<Item> attachments) {
      Message msg = new Message(40);

      try {
         msg.writeByte(1);
         msg.writeString(recipient);
         msg.writeString(subject);
         msg.writeString(body);
         msg.writeByte(attachments.size);

         for (Item item : attachments) {
            msg.writeInt(item.slotIndex);
            msg.writeInt(item.template.id);
            msg.writeInt(item.quantity);
         }

         this.networkClient.sendTcp(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPartySkill(int itemId) {
      Message msg = new Message(41);

      try {
         msg.writeByte(0);
         msg.writeInt(itemId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendGuildInvite(int actionType) {
      Message msg = new Message(42);

      try {
         msg.writeByte(0);
         msg.writeByte(actionType);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendGuildLeave() {
      Message msg = new Message(22);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendGuildKick() {
      Message msg = new Message(22);

      try {
         msg.writeByte(1);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPvpAction(int itemSlot, int quantity) {
      Message msg = new Message(44);

      try {
         msg.writeByte(GameState.forgeData.activeMode);
         msg.writeByte(0);
         msg.writeInt(itemSlot);
         msg.writeInt(quantity);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPvpChallenge(int itemSlot) {
      Message msg = new Message(44);

      try {
         msg.writeByte(GameState.forgeData.activeMode);
         msg.writeByte(1);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   /** Op 52 sub 1 ("Grow fast" button on an animal, decompiled C3872 m18011): int animal id. */
   public final void sendAnimalGrowFast(int animalId) {
      Message msg = new Message(52);

      try {
         msg.writeByte(1);
         msg.writeInt(animalId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   /** Op 72 sub 0 (Recharge window, decompiled C3872 m17976): package {@code packId} clicked. */
   public final void sendRechargePack(int packId) {
      Message msg = new Message(72);

      try {
         msg.writeByte(0);
         msg.writeInt(packId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPvpAccept(int quantity) {
      Message msg = new Message(44);

      try {
         msg.writeByte(GameState.forgeData.activeMode);
         msg.writeByte(2);
         msg.writeInt(quantity);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPvpDecline(int itemSlot) {
      Message msg = new Message(44);

      try {
         msg.writeByte(GameState.forgeData.activeMode);
         msg.writeByte(3);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPvpCancel(int mode) {
      Message msg = new Message(44);

      try {
         msg.writeByte(GameState.forgeData.activeMode);
         msg.writeByte(3);
         msg.writeByte(mode);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestDuel() {
      Message msg = new Message(45);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendDuelAction(int optionIndex) {
      Message msg = new Message(45);

      try {
         msg.writeByte(1);
         msg.writeByte(optionIndex);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendGameSettings() {
      Message msg = new Message(46);

      try {
         boolean[] autoSlots = Settings.autoSkillSlotEnabled;
         int count = Settings.autoSkillSlotEnabled.length;

         for (int i = 0; i < count; i++) {
            Boolean enabled = autoSlots[i];
            msg.writeBoolean(enabled);
         }

         msg.writeBoolean(Settings.autoIgnorePlayersAndBosses);
         msg.writeBoolean(Settings.autoAvoidEliteMobs);
         msg.writeBoolean(Settings.autoRevive);
         msg.writeBoolean(Settings.autoPickupItems);
         msg.writeBoolean(Settings.autoUseStaminaItems);
         msg.writeBoolean(Settings.autoUseHpItems);
         msg.writeBoolean(Settings.autoUseMpItems);
         msg.writeBoolean(Settings.autoHarvestApples);
         msg.writeByte(Settings.autoHpThresholdPercent);
         msg.writeByte(Settings.autoMpThresholdPercent);
         int[] keys = Settings.keybindings;
         count = Settings.keybindings.length;

         for (int j = 0; j < count; j++) {
            int keycode = keys[j];
            msg.writeInt(keycode);
         }

         msg.writeBoolean(Settings.hideOtherPlayers);
         msg.writeInt(Settings.autoAttackRange); // "Attack range" — appended at the END of the packet (old servers ignore it)
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendLogin(String username, String password) {
      Message msg = new Message(1);

      try {
         msg.writeByte(0);
         msg.writeString(username);
         msg.writeString(password);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRegister(String currentPassword, String secondaryPassword, String confirmPassword) {
      Message msg = new Message(1);

      try {
         msg.writeByte(1);
         msg.writeString(currentPassword);
         msg.writeString(secondaryPassword);
         msg.writeString(confirmPassword);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendGuestLogin(String currentPassword, String newPassword, String confirmPassword) {
      Message msg = new Message(1);

      try {
         msg.writeByte(2);
         msg.writeString(currentPassword);
         msg.writeString(newPassword);
         msg.writeString(confirmPassword);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSocialLogin(String username, String password, String confirmPassword, String email, String captcha) {
      Message msg = new Message(1);

      try {
         msg.writeByte(3);
         msg.writeString(username);
         msg.writeString(password);
         msg.writeString(confirmPassword);
         msg.writeString(email);
         msg.writeString(captcha);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendForgotPassword() {
      Message msg = new Message(1);

      try {
         msg.writeByte(4);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendVerifyCode(String code) {
      Message msg = new Message(1);

      try {
         msg.writeByte(5);
         msg.writeString(code);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   /** Op 1 sub 6 "Delete secondary password" (decompiled C3872.m18028 has no payload; this build also sends the secondary password for the server to verify). */
   public final void sendDeleteSecondaryPassword(String secondaryPassword) {
      Message msg = new Message(1);

      try {
         msg.writeByte(6);
         msg.writeString(secondaryPassword);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   /** Op 77 sub 0 "Offline Training" (decompiled C3872.m18051): byte n, n × int mob_template (0 = every mob type). */
   /**
    * Op 100 (NEW — World Boss table): sub 0 request the list, sub 1 int id "Teleport" (sapphire), sub 2 int id "Find
    * path" (gold). The server asks for confirmation before charging.
    */
   public final void sendWorldBoss(int sub, int bossId) {
      Message msg = new Message(100);

      try {
         msg.writeByte(sub);
         if (sub != 0) {
            msg.writeInt(bossId);
         }
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendOfflineTraining(int[] mobTemplateIds) {
      Message msg = new Message(77);

      try {
         msg.writeByte(0);
         msg.writeByte(mobTemplateIds.length);
         for (int id : mobTemplateIds) {
            msg.writeInt(id);
         }
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendToggleAutoAttack(boolean enabled) {
      Message msg = new Message(48);

      try {
         msg.writeByte(0);
         msg.writeBoolean(enabled);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSelectCharacter(int itemSlot) {
      Message msg = new Message(48);

      try {
         msg.writeByte(1);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendDeleteCharacter() {
      Message msg = new Message(48);

      try {
         msg.writeByte(2);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendCreateCharacter() {
      Message msg = new Message(49);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendToggleAutoPickup(boolean enabled) {
      Message msg = new Message(50);

      try {
         msg.writeByte(0);
         msg.writeBoolean(enabled);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendAutoPickupFilter(int itemSlot) {
      Message msg = new Message(50);

      try {
         msg.writeByte(1);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestAutoPickup() {
      Message msg = new Message(50);

      try {
         msg.writeByte(2);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendToggleSetting(boolean enabled) {
      Message msg = new Message(64);

      try {
         msg.writeByte(0);
         msg.writeBoolean(enabled);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendClaimReward(int itemSlot) {
      Message msg = new Message(64);

      try {
         msg.writeByte(1);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestDailyTask() {
      Message msg = new Message(64);

      try {
         msg.writeByte(2);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendTogglePvp(boolean enabled) {
      Message msg = new Message(66);

      try {
         msg.writeByte(0);
         msg.writeBoolean(enabled);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   /** Opcode 78 sub 0, same payload as C3872.m18033x47512caa in the decompiled client. */
   public final void sendToggleBackwearHidden(boolean hidden) {
      Message msg = new Message(78);
      try {
         msg.writeByte(0);
         msg.writeBoolean(hidden);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   /**
    * Opcode 78 sub 1 int index: "Use" an item from the backwear list (C3872.m17941 — decompiled C3655.f13741 "Use").
    * The earlier port mistakenly sent opcode 50 (= titles).
    */
   public final void sendUseBackwear(int index) {
      Message msg = new Message(78);
      try {
         msg.writeByte(1);
         msg.writeInt(index);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   /** Opcode 78 sub 1 int -1: "Unequip" the backwear item in use (decompiled C3655.f13739 sends m17941(-1)). */
   public final void sendUnequipBackwear() {
      this.sendUseBackwear(-1);
   }

   /** Opcode 27 sub 20 int bag slot: "Equip" a type-48 item from the bag -> stores it in the backwear collection (C3872.m18047). */
   public final void sendPutBackwear(int bagSlot) {
      Message msg = new Message(27);
      try {
         msg.writeByte(20);
         msg.writeInt(bagSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   /** Opcode 27 sub 21 int index: "Take out" a backwear item back to the bag (C3872.m17906 — decompiled C3655.f13717). */
   public final void sendTakeBackwear(int index) {
      Message msg = new Message(27);
      try {
         msg.writeByte(21);
         msg.writeInt(index);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   /** Opcode 78 sub 2, same payload as C3872.m18038x6e593564 in the decompiled client. */
   public final void sendExpandBackwearSlots() {
      Message msg = new Message(78);
      try {
         msg.writeByte(2);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendGuildDepositGold(int itemSlot) {
      Message msg = new Message(66);

      try {
         msg.writeByte(1);
         msg.writeInt(itemSlot);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestGuildVault() {
      Message msg = new Message(66);

      try {
         msg.writeByte(2);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendGuildVaultAction(int npcId) {
      Message msg = new Message(54);

      try {
         msg.writeByte(0);
         msg.writeInt(npcId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendEnhanceEquipment(int shopId, int categoryIndex, int itemSlot, int quantity) {
      Message msg = new Message(55);

      try {
         msg.writeByte(0);
         msg.writeInt(shopId);
         msg.writeInt(categoryIndex);
         msg.writeInt(itemSlot);
         msg.writeInt(quantity);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestEnhance() {
      Message msg = new Message(55);

      try {
         msg.writeByte(1);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestSocialList() {
      Message msg = new Message(68);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSocialAction(int catalogId, int packIndex) {
      Message msg = new Message(68);

      try {
         msg.writeByte(1);
         msg.writeInt(catalogId);
         msg.writeInt(packIndex);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendFishingAction(int itemId) {
      Message msg = new Message(56);

      try {
         msg.writeByte(0);
         msg.writeInt(itemId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendFishingResult(int coins, int[][] entries) {
      Message msg = new Message(56);

      try {
         msg.writeByte(1);
         msg.writeInt(coins);
         msg.writeByte(entries.length);
         int[][] entryArray = entries;
         int count = entries.length;

         for (int i = 0; i < count; i++) {
            int[] entry = entryArray[i];
            msg.writeInt(entry[0]);
            msg.writeInt(entry[1]);
         }

         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendStopFishing() {
      Message msg = new Message(56);

      try {
         msg.writeByte(2);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestFishing() {
      Message msg = new Message(56);

      try {
         msg.writeByte(3);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendHarvestAction(int targetId) {
      Message msg = new Message(52);

      try {
         msg.writeByte(0);
         msg.writeInt(targetId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestHarvest() {
      Message msg = new Message(57);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendHarvestItem(int targetId) {
      Message msg = new Message(2);

      try {
         msg.writeByte(0);
         msg.writeInt(targetId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendToggleMount(boolean enabled) {
      Message msg = new Message(2);

      try {
         msg.writeByte(1);
         msg.writeBoolean(enabled);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendSelectMount(String text) {
      Message msg = new Message(58);

      try {
         msg.writeByte(0);
         msg.writeString(text);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendMountAction(int recipeIndex) {
      Message msg = new Message(60);

      try {
         msg.writeByte(0);
         msg.writeByte(recipeIndex);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendMountEquip(int achievementId) {
      Message msg = new Message(62);

      try {
         msg.writeByte(0);
         msg.writeInt(achievementId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestMount() {
      Message msg = new Message(69);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPetSummon(int milestoneId) {
      Message msg = new Message(69);

      try {
         msg.writeByte(1);
         msg.writeInt(milestoneId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPetDismiss(int questIndex) {
      Message msg = new Message(69);

      try {
         msg.writeByte(2);
         msg.writeInt(questIndex);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPetInteract(int questIndex) {
      Message msg = new Message(69);

      try {
         msg.writeByte(3);
         msg.writeInt(questIndex);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendPetStay() {
      Message msg = new Message(69);

      try {
         msg.writeByte(4);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendRequestNotification() {
      Message msg = new Message(71);

      try {
         msg.writeByte(0);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   public final void sendNotificationAction(int actionId) {
      Message msg = new Message(71);

      try {
         msg.writeByte(1);
         msg.writeByte(actionId);
         this.trySendMessage(msg);
         msg.close();
      } catch (Exception e) {
      }
   }

   @Override
   public final void dispose() {
      if (this.networkClient.isConnected()) {
         this.networkClient.disconnect();
         this.networkClient = null;
      }
   }

   public final void disconnect() {
      this.networkClient.disconnect();
   }

   public final void switchServer() {
      this.networkClient.switchServer();
   }

}
