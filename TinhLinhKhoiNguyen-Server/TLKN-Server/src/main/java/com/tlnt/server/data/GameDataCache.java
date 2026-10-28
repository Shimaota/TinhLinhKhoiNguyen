package com.tlnt.server.data;

import com.tlnt.server.data.model.CharBaseStat;
import com.tlnt.server.data.model.GamePack;
import com.tlnt.server.data.model.ItemAppearance;
import com.tlnt.server.data.model.ItemPack;
import com.tlnt.server.data.model.MapPack;
import com.tlnt.server.data.model.ServerEntry;
import com.tlnt.server.data.model.SkillEffect;
import com.tlnt.server.data.model.SkillPack;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Toàn bộ dữ liệu tĩnh (game/map/item/skill) nạp từ SQL vào RAM cho logic server dùng.
 *
 * Mỗi lần {@link #set} tạo 1 snapshot bất biến rồi thay tham chiếu {@code volatile} — thread
 * worker đọc không cần lock, reload nóng (admin sửa SQL rồi gọi reload) không làm packet đang xử lý
 * thấy dữ liệu nửa cũ nửa mới.
 */
public final class GameDataCache {

   private static volatile Snapshot snapshot = Snapshot.EMPTY;
   private static volatile List<ServerEntry> servers = List.of();
   private static volatile Map<Integer, CharBaseStat> charBaseStats = Map.of();
   private static volatile Map<Integer, ItemAppearance> itemAppearances = Map.of();
   private static volatile Map<Long, SkillEffect> skillEffects = Map.of();

   private GameDataCache() {
   }

   static void set(GamePack game, MapPack map, ItemPack item, SkillPack skill) {
      snapshot = new Snapshot(game, map, item, skill);
   }

   static void setServers(List<ServerEntry> list) {
      servers = List.copyOf(list);
   }

   static void setCharBaseStats(List<CharBaseStat> list) {
      charBaseStats = Snapshot.index(list, CharBaseStat::templateId);
   }

   static void setItemAppearances(List<ItemAppearance> list) {
      itemAppearances = Snapshot.index(list, ItemAppearance::itemTemplateId);
   }

   static void setSkillEffects(List<SkillEffect> list) {
      Map<Long, SkillEffect> map = new HashMap<>();
      for (SkillEffect e : list) {
         map.put(e.key(), e);
      }
      skillEffects = Collections.unmodifiableMap(map);
   }

   /** Con số của kỹ năng {@code skillId} ở cấp {@code level} (bảng {@code skill_effect}), null nếu chưa khai báo. */
   public static SkillEffect skillEffect(int skillId, int level) {
      return skillEffects.get(SkillEffect.key(skillId, level));
   }

   /** Ngoại hình khi dùng item (bảng {@code item_appearance}), null nếu item không đổi ngoại hình. */
   public static ItemAppearance itemAppearance(int itemTemplateId) {
      return itemAppearances.get(itemTemplateId);
   }

   /** Chỉ số gốc của nhân vật (bảng {@code char_base_stat}), null nếu chưa khai báo. */
   public static CharBaseStat charBaseStat(int templateId) {
      return charBaseStats.get(templateId);
   }

   /** Danh sách máy chủ (bảng {@code server_list}, chỉ dòng enabled) gửi cho client qua opcode 43. */
   public static List<ServerEntry> servers() {
      return servers;
   }

   public static GamePack game() {
      return snapshot.game;
   }

   public static MapPack map() {
      return snapshot.map;
   }

   public static ItemPack item() {
      return snapshot.item;
   }

   public static SkillPack skill() {
      return snapshot.skill;
   }

   // ------------------------------------------------------------------ lookup theo id (null nếu không có)

   public static GamePack.CharClass charClass(int id) {
      return snapshot.charClasses.get(id);
   }

   public static GamePack.CharTemplate charTemplate(int id) {
      return snapshot.charTemplates.get(id);
   }

   public static GamePack.Plant plant(int id) {
      return snapshot.plants.get(id);
   }

   public static GamePack.Animal animal(int id) {
      return snapshot.animals.get(id);
   }

   public static GamePack.Title title(int id) {
      return snapshot.titles.get(id);
   }

   public static MapPack.Mob mob(int id) {
      return snapshot.mobs.get(id);
   }

   public static MapPack.Npc npc(int id) {
      return snapshot.npcs.get(id);
   }

   public static MapPack.WorldMapPage worldMapPage(int id) {
      return snapshot.worldMapPages.get(id);
   }

   public static ItemPack.ItemType itemType(int id) {
      return snapshot.itemTypes.get(id);
   }

   public static ItemPack.ItemTemplate itemTemplate(int id) {
      return snapshot.itemTemplates.get(id);
   }

   public static ItemPack.OptionInfo option(int id) {
      return snapshot.options.get(id);
   }

   public static SkillPack.Skill skillTemplate(int id) {
      return snapshot.skills.get(id);
   }

   /** @return level của skill (theo field {@code level}, không phải index), null nếu không có. */
   public static SkillPack.SkillLevel skillLevel(int skillId, int level) {
      SkillPack.Skill s = snapshot.skills.get(skillId);
      if (s == null) {
         return null;
      }
      for (SkillPack.SkillLevel l : s.levels()) {
         if (l.level() == level) {
            return l;
         }
      }
      return null;
   }

   public static SkillPack.Talent talent(int id) {
      return snapshot.talents.get(id);
   }

   public static String summary() {
      Snapshot s = snapshot;
      return String.format("game v%d (class=%d, template=%d, plant=%d, title=%d) | map v%d (tileset=%d, mob=%d,"
                  + " npc=%d, page=%d) | item v%d (type=%d, template=%d, option=%d) | skill v%d (skill=%d, talent=%d)",
            s.game.version(), s.game.classes().size(), s.game.templates().size(), s.game.plants().size(),
            s.game.titles().size(), s.map.version(), s.map.tilesets().size(), s.map.mobs().size(),
            s.map.npcs().size(), s.map.worldMapPages().size(), s.item.version(), s.item.itemTypes().size(),
            s.item.itemTemplates().size(), s.item.options().size(), s.skill.version(), s.skill.skills().size(),
            s.skill.talents().size()) + " | server=" + servers.size() + " | char_base_stat=" + charBaseStats.size()
            + " | item_appearance=" + itemAppearances.size() + " | skill_effect=" + skillEffects.size();
   }

   private static final class Snapshot {

      static final Snapshot EMPTY = new Snapshot(
            new GamePack(-1, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                  List.of()),
            new MapPack(-1, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of()),
            new ItemPack(-1, List.of(), List.of(), List.of(), List.of()),
            new SkillPack(-1, List.of(), List.of(), List.of()));

      final GamePack game;
      final MapPack map;
      final ItemPack item;
      final SkillPack skill;

      final Map<Integer, GamePack.CharClass> charClasses;
      final Map<Integer, GamePack.CharTemplate> charTemplates;
      final Map<Integer, GamePack.Plant> plants;
      final Map<Integer, GamePack.Animal> animals;
      final Map<Integer, GamePack.Title> titles;
      final Map<Integer, MapPack.Mob> mobs;
      final Map<Integer, MapPack.Npc> npcs;
      final Map<Integer, MapPack.WorldMapPage> worldMapPages;
      final Map<Integer, ItemPack.ItemType> itemTypes;
      final Map<Integer, ItemPack.ItemTemplate> itemTemplates;
      final Map<Integer, ItemPack.OptionInfo> options;
      final Map<Integer, SkillPack.Skill> skills;
      final Map<Integer, SkillPack.Talent> talents;

      Snapshot(GamePack game, MapPack map, ItemPack item, SkillPack skill) {
         this.game = game;
         this.map = map;
         this.item = item;
         this.skill = skill;
         charClasses = index(game.classes(), GamePack.CharClass::id);
         charTemplates = index(game.templates(), GamePack.CharTemplate::id);
         plants = index(game.plants(), GamePack.Plant::id);
         animals = index(game.animals(), GamePack.Animal::id);
         titles = index(game.titles(), GamePack.Title::id);
         mobs = index(map.mobs(), MapPack.Mob::id);
         npcs = index(map.npcs(), MapPack.Npc::id);
         worldMapPages = index(map.worldMapPages(), MapPack.WorldMapPage::id);
         itemTypes = index(item.itemTypes(), ItemPack.ItemType::id);
         itemTemplates = index(item.itemTemplates(), ItemPack.ItemTemplate::id);
         options = index(item.options(), ItemPack.OptionInfo::id);
         skills = index(skill.skills(), SkillPack.Skill::id);
         talents = index(skill.talents(), SkillPack.Talent::id);
      }

      static <T> Map<Integer, T> index(List<T> list, Function<T, Integer> id) {
         Map<Integer, T> map = new HashMap<>(Math.max(16, list.size() * 2));
         for (T value : list) {
            map.put(id.apply(value), value);
         }
         return Collections.unmodifiableMap(map);
      }
   }
}
