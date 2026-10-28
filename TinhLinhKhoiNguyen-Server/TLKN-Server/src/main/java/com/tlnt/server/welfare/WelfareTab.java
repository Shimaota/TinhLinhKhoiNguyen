package com.tlnt.server.welfare;

/**
 * 3 tab phúc lợi (client {@code GiftcodeWindow}: mỗi "event" = 1 tab, tab "Giftcode" client tự thêm cuối). {@link #id}
 * = byte eventId gửi / nhận lại khi bấm "Nhận"; thứ tự enum = thứ tự tab.
 */
public enum WelfareTab {

   ONLINE(0, "Quà Online", "icon/259"),
   CHECKIN(1, "Điểm danh", "icon/261"),
   RECHARGE(2, "Quà Nạp", "icon/260");

   public final int id;
   public final String title;
   public final String icon;

   WelfareTab(int id, String title, String icon) {
      this.id = id;
      this.title = title;
      this.icon = icon;
   }

   public static WelfareTab of(int id) {
      for (WelfareTab tab : values()) {
         if (tab.id == id) {
            return tab;
         }
      }
      return null;
   }
}
