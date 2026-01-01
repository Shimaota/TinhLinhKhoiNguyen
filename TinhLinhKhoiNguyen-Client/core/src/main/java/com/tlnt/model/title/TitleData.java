package com.tlnt.model.title;

import com.badlogic.gdx.utils.ObjectMap;

public final class TitleData {
   public static final ObjectMap<Integer, TitleData> BY_ID = new ObjectMap<>();
   public int id;
   public String name;
   /** Title icon (column {@code icon}, e.g. "icon/225") — the title button in the equipment window (decompiled C0018.f66). */
   public String textureKey;
   /** {@code skeletons/vfxs/aura_fx} halo animation around the character (column {@code texture_key}: "sun" / "moon"). */
   public String auraAnimation;
   public String[] descriptionLines;

   public static void clearAll() {
      BY_ID.clear();
   }

   public static void register(TitleData title) {
      BY_ID.put(title.id, title);
   }

   public static TitleData getById(int id) {
      return BY_ID.get(id);
   }

   public TitleData(int id, String name, String textureKey, String auraAnimation, String[] descriptionLines) {
      this.id = id;
      this.name = name;
      this.textureKey = textureKey;
      this.auraAnimation = auraAnimation;
      this.descriptionLines = descriptionLines;
   }
}
