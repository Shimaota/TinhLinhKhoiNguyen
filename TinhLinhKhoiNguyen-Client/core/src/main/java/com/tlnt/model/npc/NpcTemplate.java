package com.tlnt.model.npc;

import com.badlogic.gdx.utils.ObjectMap;

public final class NpcTemplate {
   public static ObjectMap<Integer, NpcTemplate> ALL_TEMPLATES = new ObjectMap<>();
   public int id;
   public String name;
   public String overheadText;
   public boolean facesPlayerOnInteract;
   public int type;
   public String skeletonPath;
   public String idleAnimation;
   public float skeletonScale;

   /**
    * Shows the {@code ui/230} talk button next to the name ({@code NpcTalkButton}). The data file's "type" byte is, in the decompiled code,
    * the 2nd boolean of the NPC template (C0054.f676) — 1 for regular NPCs, 0 for the Apple Tree / Storage Chest.
    */
   public boolean showsTalkButton() {
      return this.type != 0;
   }

   public static NpcTemplate getTemplate(int id) {
      return ALL_TEMPLATES.get(id);
   }

   public static void addTemplate(NpcTemplate template) {
      ALL_TEMPLATES.put(template.id, template);
   }

   public static void clearTemplates() {
      ALL_TEMPLATES.clear();
   }

   public NpcTemplate(int id, String name, String overheadText, boolean facesPlayerOnInteract, int type, String skeletonPath, String idleAnimation, float skeletonScale) {
      this.id = id;
      this.name = name;
      this.overheadText = overheadText;
      this.facesPlayerOnInteract = facesPlayerOnInteract;
      this.type = type;
      this.skeletonPath = skeletonPath;
      this.idleAnimation = idleAnimation;
      this.skeletonScale = skeletonScale;
   }
}
