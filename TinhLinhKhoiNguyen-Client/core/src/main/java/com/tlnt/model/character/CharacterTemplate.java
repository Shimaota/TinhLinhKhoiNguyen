package com.tlnt.model.character;

import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.OrderedMap;
import com.tlnt.util.JsonUiBuilder;

public final class CharacterTemplate {
   public static final ObjectMap<Integer, CharacterTemplate> REGISTRY = new OrderedMap<>();
   public int id;
   public String name;
   public String skeletonPath;
   public String avatarIconPath;
   public String infoTableJson;
   public float[] statRatings;
   public boolean selectable;
   public JsonUiBuilder uiBuilder = new JsonUiBuilder();
   public Table infoTable;

   public static void clearAll() {
      REGISTRY.clear();
   }

   public static void register(CharacterTemplate template) {
      REGISTRY.put(template.id, template);
   }

   public static CharacterTemplate getById(int id) {
      return REGISTRY.get(id);
   }

   public static Array<CharacterTemplate> getSelectable() {
      Array result = new Array();
      Array.ArrayIterator it = REGISTRY.values().toArray().iterator();

      while (it.hasNext()) {
         CharacterTemplate template;
         if ((template = (CharacterTemplate)it.next()).selectable) {
            result.add(template);
         }
      }

      return result;
   }

   public CharacterTemplate(int id, String name, String skeletonPath, String avatarIconPath, String infoTableJson, float[] statRatings, boolean selectable) {
      this.id = id;
      this.name = name;
      this.skeletonPath = skeletonPath;
      this.avatarIconPath = avatarIconPath;
      this.infoTableJson = infoTableJson;
      this.statRatings = statRatings;
      this.selectable = selectable;
   }
}
