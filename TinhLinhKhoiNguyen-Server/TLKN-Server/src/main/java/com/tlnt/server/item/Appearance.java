package com.tlnt.server.item;

import com.tlnt.server.data.GameDataCache;
import com.tlnt.server.data.model.ItemAppearance;
import com.tlnt.server.player.Gender;
import com.tlnt.server.player.ItemData;
import com.tlnt.server.player.PlayerData;
import com.tlnt.server.player.PlayerSettings;

import java.util.ArrayList;
import java.util.List;

/**
 * Ngoại hình người khác nhìn thấy, tính từ đồ đang dùng (bất biến, công bố qua {@link PlayerData#appearance}).
 * Ghi vào khối chỉ số ({@code WorldPackets.writeStats}) — client đọc lại skeleton/skin/danh hiệu mỗi lần nhận khối này.
 * Mọi tên skin / skeleton nằm ở bảng {@code tlnt_data.item_appearance} (lấy từ DataGame/skeletons).
 *
 * <p>Client ghép các skin theo thứ tự gửi ({@code Skin.addSkin}: skin sau ghi đè attachment cùng slot của skin
 * trước), skin không có trong skeleton thì bỏ qua:
 * <ol>
 *   <li>Thân: áo đang mặc (ô 0) -> {@code skin/lv1..5} (cả bộ thân, THAY skin mặc định, không cộng dồn — skin/lv1 có
 *       slot mà lv2 không có); không mặc áo -> {@link #BODY_SKIN}.</li>
 *   <li>Vũ khí (ô 4): {@code axe/lvN}, {@code spear/lvN}, {@code whip/lvN}, {@code bow/lvN}, {@code slingshot/lvN}.
 *       Số N cũng là {@link #weaponLevel} (client ghép "lv" + N làm skin đạn / hiệu ứng chiêu).</li>
 *   <li>Đeo lưng (loại 48): {@code backpack/lvN}.</li>
 *   <li>Mặt nạ (36) không ẩn: {@code mask/lvN} — chỉ đổi slot đầu (mask), đặt cuối để đè lên thân.</li>
 * </ol>
 * Ngoại trang (19) đang dùng + không ẩn: {@code char_template_id} trong {@code item_appearance} chọn skeleton và icon
 * từ {@code char_template}. Áo không hiện trên skeleton ngoại trang, còn vũ khí / đeo lưng / mặt nạ vẫn hiện.
 * Danh hiệu (20) không ẩn: {@code titleKey} = {@code skeletons/vfxs/...} (hiệu ứng trên đầu).
 * Thú cưng (37) đang dùng + không ẩn: {@code char_template_id} của món -> 1 con thú đi theo chủ ({@code world/Zone}).
 * Item chưa có dòng trong {@code item_appearance} thì vẫn dùng được (có chỉ số), chỉ không đổi hình.
 *
 * @param weaponLevel cấp vũ khí đang cầm (1..5), -1 = tay không / vũ khí chưa có skin.
 * @param petTemplateId mẫu char_template thú cưng đi theo, 0 = không có
 * @param petName tên hiện trên đầu thú cưng (tên item)
 */
public record Appearance(int templateId, String[] skins, String titleKey, int weaponLevel, int petTemplateId,
      String petName) {

   /** Skin thân mặc định, giống bảng tạo nhân vật ({@code CharacterCreateDialog}: "skin/lv1"). */
   public static final String BODY_SKIN = "skin/lv1";
   /** {@code item_type.equip_slot} của áo / vũ khí = ô trong {@link PlayerData#body}. */
   public static final int SLOT_ARMOR = 0;
   public static final int SLOT_WEAPON = 4;
   public static final int NO_WEAPON = -1;

   public static Appearance of(PlayerData d) {
      boolean[] hidden = d.settings != null ? d.settings.inventoryFlags : new boolean[PlayerSettings.INVENTORY_FLAGS];

      // Ngoại trang chọn template riêng để tra skeleton / avatar; nếu không có thì dùng template gốc theo giới tính.
      int templateId = Gender.charTemplateId(d.gender);
      ItemAppearance costume = visual(d.costumes.getSelected(), isHidden(hidden, PlayerSettings.HIDE_COSTUME));
      if (costume != null && costume.charTemplateId() > 0) {
         templateId = costume.charTemplateId();
      }

      List<String> skins = new ArrayList<>(4);
      String armor = skinKey(d.body.get(SLOT_ARMOR));
      skins.add(armor != null ? armor : BODY_SKIN);
      String weapon = skinKey(d.body.get(SLOT_WEAPON));
      if (weapon != null) {
         skins.add(weapon);
      }
      String back = isHidden(hidden, PlayerSettings.HIDE_BACK) ? null : skinKey(d.back());
      if (back != null) {
         skins.add(back);
      }
      if (!isHidden(hidden, PlayerSettings.HIDE_MASK)) {
         String mask = skinKey(d.masks.getSelected());
         if (mask != null) {
            skins.add(mask);
         }
      }

      ItemAppearance title = visual(d.titles.getSelected(), isHidden(hidden, PlayerSettings.HIDE_TITLE));
      String titleKey = title != null && title.titleKey() != null ? title.titleKey() : "";

      int petTemplateId = 0;
      String petName = "";
      ItemData pet = isHidden(hidden, PlayerSettings.HIDE_PET) ? null : d.pets.getSelected();
      ItemAppearance petLook = visual(pet, false);
      if (petLook != null && petLook.charTemplateId() > 0
            && GameDataCache.charTemplate(petLook.charTemplateId()) != null) {
         petTemplateId = petLook.charTemplateId();
         petName = pet.template() != null ? pet.template().name() : "";
      }

      return new Appearance(templateId, skins.toArray(new String[0]), titleKey, levelOf(weapon), petTemplateId,
            petName);
   }

   /** "axe/lv3" -> 3; không có đuôi "/lvN" -> {@link #NO_WEAPON}. */
   static int levelOf(String skinKey) {
      if (skinKey == null) {
         return NO_WEAPON;
      }
      int at = skinKey.lastIndexOf("/lv");
      if (at < 0) {
         return NO_WEAPON;
      }
      try {
         int level = Integer.parseInt(skinKey.substring(at + 3));
         return level >= 0 && level <= Byte.MAX_VALUE ? level : NO_WEAPON;
      } catch (NumberFormatException e) {
         return NO_WEAPON;
      }
   }

   private static String skinKey(ItemData item) {
      ItemAppearance a = visual(item, false);
      return a != null ? a.skinKey() : null;
   }

   private static ItemAppearance visual(ItemData item, boolean hidden) {
      return item == null || hidden ? null : GameDataCache.itemAppearance(item.templateId);
   }

   private static boolean isHidden(boolean[] flags, int index) {
      return flags != null && index < flags.length && flags[index];
   }
}
