package com.tlnt.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import com.tlnt.engine.particle.ParticleEffect;
import com.tlnt.model.character.CharacterClass;
import com.tlnt.model.character.CharacterTemplate;
import com.tlnt.model.download.DownloadEntry;
import com.tlnt.model.farm.AnimalType;
import com.tlnt.model.friend.MapDecorationAnimation;
import com.tlnt.model.friend.MapTile;
import com.tlnt.model.friend.Tileset;
import com.tlnt.model.guide.GuideCategory;
import com.tlnt.model.guide.GuideTopic;
import com.tlnt.model.item.ItemTemplate;
import com.tlnt.model.item.ItemType;
import com.tlnt.model.item.StatType;
import com.tlnt.model.market.MarketFeeOption;
import com.tlnt.model.mob.MobTemplate;
import com.tlnt.model.plant.PlantTemplate;
import com.tlnt.model.option.OptionInfoTemplate;
import com.tlnt.model.npc.NpcTemplate;
import com.tlnt.model.skill.SkillData;
import com.tlnt.model.skill.SkillLevelData;
import com.tlnt.model.skill.TalentData;
import com.tlnt.model.skill.TalentPointCap;
import com.tlnt.model.social.TribeEmblem;
import com.tlnt.model.social.TribeSkill;
import com.tlnt.model.title.TitleData;
import com.tlnt.model.worldmap.WorldMapPage;
import com.tlnt.ui.window.WindowType;
import com.tlnt.util.FileStorage;
import com.tlnt.util.HttpClient;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

public final class GameDataLoader {
   public static int gameDataVersion = -1;
   public static int mapDataVersion;
   public static int itemDataVersion;
   public static int skillDataVersion;
   public static String resourcesVersion;

   public static void loadAllDataFromCache() {
      loadGameData(FileStorage.readFile("version/game"), false);
      byte[] cached = FileStorage.readFile("version/map");
      loadMapData(cached, false);
      loadItemData(FileStorage.readFile("version/item"), false);
      cached = FileStorage.readFile("version/skill");
      loadSkillData(cached, false);

      try {
         resourcesVersion = FileStorage.getExternalFile("version/resources").readString();
      } catch (Exception e) {
         resourcesVersion = "";
      }
   }

   public static void loadGameData(byte[] data, boolean persist) {
      CharacterClass.clearAll();
      CharacterTemplate.clearAll();
      MarketFeeOption.clearAll();
      AnimalType.clearAll();
      PlantTemplate.clearTemplates();
      TitleData.clearAll();
      TribeEmblem.clearAll();
      TribeSkill.clearAll();
      GuideCategory.clearAll();

      try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
         gameDataVersion = in.readInt();
         int count = in.readByte();

         for (int classIndex = 0; classIndex < count; classIndex++) {
            int classId = in.readInt();
            String className = in.readUTF();
            CharacterClass.register(new CharacterClass(classId, className));
         }

         byte templateCount = in.readByte();

         for (int templateIndex = 0; templateIndex < templateCount; templateIndex++) {
            int templateId = in.readInt();
            String templateName = in.readUTF();
            String skeletonPath = in.readUTF();
            String avatarIconPath = in.readUTF();
            String infoTableJson = in.readUTF();
            float[] statRatings = new float[in.readByte()];

            for (int ratingIndex = 0; ratingIndex < statRatings.length; ratingIndex++) {
               statRatings[ratingIndex] = in.readFloat();
            }

            boolean selectable = in.readBoolean();
            CharacterTemplate.register(new CharacterTemplate(templateId, templateName, skeletonPath, avatarIconPath, infoTableJson, statRatings, selectable));
         }

         int tmpInt = in.readByte();

         for (int feeIndex = 0; feeIndex < tmpInt; feeIndex++) {
            String feeName = in.readUTF();
            int feePercent = in.readInt();
            MarketFeeOption.register(new MarketFeeOption(feeIndex, feeName, feePercent));
         }

         int animalCount = in.readInt();

         for (int animalIndex = 0; animalIndex < animalCount; animalIndex++) {
            int animalId = in.readInt();
            String growingName = in.readUTF();
            String matureName = in.readUTF();
            String animalSkeletonPath = in.readUTF();
            String skinName = in.readUTF();
            float animalScale = in.readFloat();
            tmpInt = in.readInt();
            AnimalType.register(new AnimalType(animalId, growingName, matureName, animalSkeletonPath, skinName, animalScale, tmpInt));
         }

         int plantCount = in.readInt();

         for (int plantIndex = 0; plantIndex < plantCount; plantIndex++) {
            int plantId = in.readInt();
            String plantName = in.readUTF();
            float offsetX = in.readFloat();
            float offsetY = in.readFloat();
            String plantSkeletonName = in.readUTF();
            String idleAnimation = in.readUTF();
            float plantSkeletonScale = in.readFloat();
            PlantTemplate.registerTemplate(new PlantTemplate(plantId, plantName, offsetX, offsetY, plantSkeletonName, idleAnimation, plantSkeletonScale));
         }

         int titleCount = in.readInt();

         for (int titleIndex = 0; titleIndex < titleCount; titleIndex++) {
            int titleId = in.readInt();
            String titleName = in.readUTF();
            String iconKey = in.readUTF(); // title button icon (decompiled C3420: string 2)
            String auraAnimation = in.readUTF(); // aura_fx halo animation (string 3)
            String[] descriptionLines = new String[in.readByte()];

            for (int lineIndex = 0; lineIndex < descriptionLines.length; lineIndex++) {
               descriptionLines[lineIndex] = in.readUTF();
            }

            TitleData.register(new TitleData(titleId, titleName, iconKey, auraAnimation, descriptionLines));
         }

         int emblemCount = in.readInt();

         for (int emblemIndex = 0; emblemIndex < emblemCount; emblemIndex++) {
            int emblemId = in.readInt();
            String emblemName = in.readUTF();
            String[] framePaths = new String[in.readByte()];

            for (int frameIndex = 0; frameIndex < framePaths.length; frameIndex++) {
               framePaths[frameIndex] = in.readUTF();
            }

            tmpInt = in.readInt();
            TribeEmblem.register(new TribeEmblem(emblemId, emblemName, framePaths, tmpInt));
         }

         int tribeSkillCount = in.readInt();

         for (int tribeSkillIndex = 0; tribeSkillIndex < tribeSkillCount; tribeSkillIndex++) {
            int tribeSkillId = in.readInt();
            String tribeSkillName = in.readUTF();
            String tribeSkillIcon = in.readUTF();
            String tribeSkillDescription = in.readUTF();
            TribeSkill.register(new TribeSkill(tribeSkillId, tribeSkillName, tribeSkillIcon, tribeSkillDescription));
         }

         int categoryCount = in.readInt();

         for (int categoryIndex = 0; categoryIndex < categoryCount; categoryIndex++) {
            GuideCategory category;
            (category = new GuideCategory()).id = in.readByte();
            category.name = in.readUTF();
            category.iconPath = in.readUTF();
            byte topicCount = in.readByte();

            for (int topicIndex = 0; topicIndex < topicCount; topicIndex++) {
               GuideTopic topic;
               (topic = new GuideTopic()).category = category;
               topic.id = in.readByte();
               topic.name = in.readUTF();
               topic.layoutJson = in.readUTF();
               category.topics.add(topic);
            }

            GuideCategory.register(category);
            GuideCategory.dirty = true;
         }

         if (persist) {
            FileStorage.writeFile("version/game", data);
         }
      } catch (Exception e) {
         gameDataVersion = -1;
         logLoadError("game", data, e);
      }

      System.out.println("version game " + gameDataVersion);
   }

   public static void loadMapData(byte[] data, boolean persist) {
      Tileset.clearAll();
      MapTile.clearAll();
      MapDecorationAnimation.clearAll();
      ParticleEffect.clearRegistry();
      MobTemplate.clearAll();
      NpcTemplate.clearTemplates();
      WorldMapPage.clearPages();

      try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
         mapDataVersion = in.readInt();
         int tilesetCount = in.readInt();

         for (int tilesetIndex = 0; tilesetIndex < tilesetCount; tilesetIndex++) {
            int tilesetId = in.readInt();
            String textureName = in.readUTF();
            int tileWidth = in.readInt();
            int tileHeight = in.readInt();
            Tileset tileset;
            Tileset.register(tileset = new Tileset(tilesetId, textureName, tileWidth, tileHeight));
            tileset.tiles = new MapTile[in.readInt()];

            for (int tileIndex = 0; tileIndex < tileset.tiles.length; tileIndex++) {
               int tileId = in.readInt();
               byte tileType = in.readByte();
               MapTile tile = new MapTile(tileset, tileId, tileType);
               tileset.tiles[tileIndex] = tile;
               MapTile.register(tile);
               switch (tileType) {
                  case 0:
                     tile.srcX = in.readInt();
                     tile.srcY = in.readInt();
                     break;
                  case 1:
                     tile.animFrames = new MapTile.AnimFrame[in.readInt()];

                     for (int frameIndex = 0; frameIndex < tile.animFrames.length; frameIndex++) {
                        tileId = in.readInt();
                        int frameDuration = in.readInt();
                        tile.animFrames[frameIndex] = new MapTile.AnimFrame(tileId, frameDuration);
                     }
               }
            }
         }

         int decorationCount = in.readInt();

         for (int decorationIndex = 0; decorationIndex < decorationCount; decorationIndex++) {
            int decorationId = in.readInt();
            String decorationName = in.readUTF();
            short framesPerSecond = in.readShort();
            byte anchorId = in.readByte();
            String[] frameNames = new String[in.readByte()];

            for (int nameIndex = 0; nameIndex < frameNames.length; nameIndex++) {
               frameNames[nameIndex] = in.readUTF();
            }

            MapDecorationAnimation.register(new MapDecorationAnimation(decorationId, decorationName, framesPerSecond, anchorId, frameNames));
         }

         int particleCount = in.readInt();

         for (int particleIndex = 0; particleIndex < particleCount; particleIndex++) {
            ParticleEffect particle;
            (particle = new ParticleEffect()).loadFromJson(in.readUTF());
            ParticleEffect.register(particle);
         }

         int mobCount = in.readInt();

         for (int mobIndex = 0; mobIndex < mobCount; mobIndex++) {
            int mobId = in.readInt();
            String mobName = in.readUTF();
            byte wanderType = in.readByte();
            int wanderRange = in.readInt();
            String mobSkeletonPath = in.readUTF();
            float mobScale = in.readFloat();
            MobTemplate.MobVariant[] variants = new MobTemplate.MobVariant[in.readByte()];

            for (int variantIndex = 0; variantIndex < variants.length; variantIndex++) {
               String variantName = in.readUTF();
               String variantSkin = in.readUTF();
               float variantSpeed = in.readFloat();
               boolean showHealthBar = in.readBoolean();
               variants[variantIndex] = new MobTemplate.MobVariant(variantName, variantSkin, variantSpeed, showHealthBar);
            }

            MobTemplate.register(new MobTemplate(mobId, mobName, wanderType, wanderRange, mobSkeletonPath, mobScale, variants));
         }

         int npcCount = in.readInt();

         for (int npcIndex = 0; npcIndex < npcCount; npcIndex++) {
            int npcId = in.readInt();
            String npcName = in.readUTF();
            String overheadText = in.readUTF();
            boolean facesPlayer = in.readBoolean();
            byte npcType = in.readByte();
            in.readByte(); // extra: present in the file but not used by NpcTemplate yet
            String npcSkeletonPath = in.readUTF();
            String npcIdleAnimation = in.readUTF();
            float npcScale = in.readFloat();
            NpcTemplate.addTemplate(new NpcTemplate(npcId, npcName, overheadText, facesPlayer, npcType, npcSkeletonPath, npcIdleAnimation, npcScale));
         }

         int pageCount = in.readInt();

         for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
            int pageId = in.readInt();
            String pageName = in.readUTF();
            String mapImagePath = in.readUTF();
            WorldMapPage.MapMarker[] markers = new WorldMapPage.MapMarker[in.readByte()];

            for (int markerIndex = 0; markerIndex < markers.length; markerIndex++) {
               int markerMapId = in.readInt();
               String markerName = in.readUTF();
               float markerX = in.readFloat();
               float markerY = in.readFloat();
               markers[markerIndex] = new WorldMapPage.MapMarker(markerMapId, markerName, markerX, markerY);
            }

            WorldMapPage.addPage(new WorldMapPage(pageId, pageName, mapImagePath, markers));
         }

         // trailing section (v0.2.5 — decompiled C3420): op 80 floating object templates (map_effect table); skipped for old files
         com.tlnt.model.mapeffect.MapEffectTemplate.clearAll();
         if (in.available() > 0) {
            int effectCount = in.readInt();

            for (int effectIndex = 0; effectIndex < effectCount; effectIndex++) {
               int effectId = in.readInt();
               String effectName = in.readUTF();
               String effectSkeleton = in.readUTF();
               com.tlnt.model.mapeffect.MapEffectTemplate.register(
                  new com.tlnt.model.mapeffect.MapEffectTemplate(effectId, effectName, effectSkeleton)
               );
            }
         }

         if (persist) {
            FileStorage.writeFile("version/map", data);
         }
      } catch (Exception e) {
         mapDataVersion = -1;
         logLoadError("map", data, e);
      }
   }

   public static void loadItemData(byte[] data, boolean persist) {
      try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
         itemDataVersion = in.readInt();
         int typeCount = in.readInt();

         for (int typeIndex = 0; typeIndex < typeCount; typeIndex++) {
            int typeId = in.readInt();
            String typeName = in.readUTF();
            int equipSlot = in.readInt();
            boolean equippable = in.readBoolean();
            ItemType.register(new ItemType(typeId, typeName, equipSlot, equippable));
         }

         int templateCount = in.readInt();

         for (int templateIndex = 0; templateIndex < templateCount; templateIndex++) {
            int templateId = in.readInt();
            int itemTypeId = in.readInt();
            int classId = in.readInt();
            String itemName = in.readUTF();
            String description = in.readUTF();
            String iconPath = in.readUTF();
            String previewImagePath = in.readUTF();
            int requiredLevel = in.readInt();
            String emblemPath = in.readUTF();
            ItemTemplate.register(new ItemTemplate(templateId, itemTypeId, classId, itemName, description, iconPath, previewImagePath, requiredLevel, emblemPath));
         }

         int statTypeCount = in.readInt();

         for (int statIndex = 0; statIndex < statTypeCount; statIndex++) {
            int statTypeId = in.readInt();
            String formatPattern = in.readUTF();
            StatType.register(new StatType(statTypeId, formatPattern));
         }

         int optionCount = in.readInt();

         for (int optionIndex = 0; optionIndex < optionCount; optionIndex++) {
            int optionId = in.readInt();
            String optionName = in.readUTF();
            String optionIcon = in.readUTF();
            String optionDescription = in.readUTF();
            OptionInfoTemplate.register(new OptionInfoTemplate(optionId, optionName, optionIcon, optionDescription));
         }

         if (persist) {
            FileStorage.writeFile("version/item", data);
         }
      } catch (Exception e) {
         itemDataVersion = -1;
         logLoadError("item", data, e);
      }
   }

   public static void loadSkillData(byte[] data, boolean persist) {
      TalentPointCap.clearAll();
      TalentData.clearAll();
      SkillData.clearAll();

      try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
         skillDataVersion = in.readInt();
         int tmpInt = in.readByte();

         for (int capIndex = 0; capIndex < tmpInt; capIndex++) {
            int capId = in.readInt();
            int requiredLevel = in.readInt();
            int[] talentPointCaps = new int[in.readByte()];

            for (int capValueIndex = 0; capValueIndex < talentPointCaps.length; capValueIndex++) {
               talentPointCaps[capValueIndex] = in.readInt();
            }

            TalentPointCap.register(new TalentPointCap(capId, requiredLevel, talentPointCaps));
         }

         byte tmpByte = in.readByte();

         for (int talentIndex = 0; talentIndex < tmpByte; talentIndex++) {
            int talentId = in.readInt();
            String talentName = in.readUTF();
            String talentIcon = in.readUTF();
            String talentDescription = in.readUTF();
            int pointCost = in.readInt();
            TalentData.register(new TalentData(talentId, talentName, talentIcon, talentDescription, pointCost));
         }

         int skillCount = in.readInt();

         for (int skillIndex = 0; skillIndex < skillCount; skillIndex++) {
            int skillId = in.readInt();
            String skillName = in.readUTF();
            tmpInt = in.readByte();
            int tmpInt2 = in.readByte();
            tmpByte = in.readByte();
            String skillDescription = in.readUTF();
            String[] iconPaths = new String[in.readByte()];

            for (int iconIndex = 0; iconIndex < iconPaths.length; iconIndex++) {
               iconPaths[iconIndex] = in.readUTF();
            }

            SkillData skill;
            SkillData.register(skill = new SkillData(skillId, skillName, tmpInt, tmpInt2, tmpByte, skillDescription, iconPaths));
            tmpInt = in.readInt();
            SkillLevelData prevLevel = null;

            for (int levelIndex = 0; levelIndex < tmpInt; levelIndex++) {
               int level = in.readInt();
               tmpInt2 = in.readInt();
               int rangeY = in.readInt();
               int manaCost = in.readInt();
               int cooldownMillis = in.readInt();
               int levelRequired = in.readInt();
               int upgradeCost = in.readInt();

               long goldCost = in.readLong(); // decompiled C3420: 2 longs = gold + experience cost
               long expCost = in.readLong();

               String effectDescription = in.readUTF();
               SkillLevelData levelData = new SkillLevelData(skill, level, tmpInt2, rangeY, manaCost, cooldownMillis, levelRequired, upgradeCost, goldCost, expCost, effectDescription);
               skill.levels.add(levelData);
               if (prevLevel != null) {
                  prevLevel.nextLevel = levelData;
                  levelData.prevLevel = prevLevel;
               }

               prevLevel = levelData;
            }
         }

         if (persist) {
            FileStorage.writeFile("version/skill", data);
         }
      } catch (Exception e) {
         skillDataVersion = -1;
         logLoadError("skill", data, e);
      }
   }

   public static void checkAndDownloadResources(String baseUrl, String versionInfo) {
      versionInfo = HttpClient.fetchString(versionInfo, null);
      System.out.println(versionInfo);
      String localVersion = resourcesVersion;
      Array remoteParts = new Array<>(versionInfo.split(";"));
      Array<String> localParts = new Array<>(localVersion.split(";"));
      Array changed = new Array();
      Array selected;
      if (remoteParts.size < localParts.size) {
         Array.ArrayIterator it = remoteParts.iterator();

         while (it.hasNext()) {
            if (!(localVersion = (String)it.next()).isEmpty()) {
               changed.add(localVersion);
            }
         }

         selected = changed;
      } else {
         ObjectSet localSet = new ObjectSet();

         for (String part : localParts) {
            localSet.add(part);
         }

         Array.ArrayIterator remoteIt = remoteParts.iterator();

         while (remoteIt.hasNext()) {
            String remotePart;
            if (!(remotePart = (String)remoteIt.next()).isEmpty() && !localSet.contains(remotePart)) {
               changed.add(remotePart);
            }
         }

         selected = changed;
      }

      Array changedParts = selected;
      Queue downloadQueue = new Queue();
      int totalSize = 0;
      Array.ArrayIterator partIt = changedParts.iterator();

      while (partIt.hasNext()) {
         String changedPart;
         if (!(changedPart = (String)partIt.next()).isEmpty()) {
            String partUrl = (baseUrl != null && baseUrl.endsWith("/") ? baseUrl : (baseUrl == null ? "" : baseUrl + "/"))
               + (changedPart.startsWith("/") ? changedPart.substring(1) : changedPart);
            String fileList = HttpClient.fetchString(partUrl, null);
            String[] fileEntries;
            int entryCount = (fileEntries = fileList.split(";")).length;

            for (int i = 0; i < entryCount; i++) {
               String fileEntry;
               if (!(fileEntry = fileEntries[i]).isEmpty()) {
                  String[] fields = fileEntry.split(",");
                  String fileName = fields[0];
                  DownloadEntry download = new DownloadEntry(baseUrl, fileName, Integer.parseInt(fields[1]));
                  downloadQueue.addLast(download);
                  totalSize += download.fileSize;
               }
            }
         }
      }

      if (totalSize != 0) {
         GameMain.instance.getGameScreen().active.POOL2.startLoading(downloadQueue, totalSize, versionInfo);
         // This runs on the network thread: open the dialog on the game thread (scene2d is not thread-safe).
         Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
               GameMain.instance.getGameScreen().active.showDialog(WindowType.DOWNLOAD_RESOURCES_PANEL);
            }
         });
      }
   }

   private static void logLoadError(String part, byte[] data, Exception e) {
      // FileStorage.readFile returns an empty array when there is no cache file yet -> not an error, wait for the server to send the download link.
      if (data == null || data.length == 0) {
         System.out.println("Chua co du lieu '" + part + "' (cache trong hoac tai that bai)");
      } else {
         System.out.println("Loi doc du lieu '" + part + "' (" + data.length + " bytes), khong luu cache:");
         e.printStackTrace();
      }
   }

   static {
      mapDataVersion = -1;
      itemDataVersion = -1;
      skillDataVersion = -1;
   }

}
