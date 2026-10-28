package com.tlnt.server.event;

/**
 * Trạng thái sự kiện của 1 nhân vật (cột JSON {@code player.event_state}). Trung thu: ngày (epoch day) đã nhận quà điểm danh ở
 * Nguyệt linh thần. Luồng session của chủ.
 */
public final class EventState {

   @com.google.gson.annotations.SerializedName("ci")
   public int checkInDay;
   /** Lần ném cà rốt gần nhất (hồi 10 s) — chỉ RAM. */
   public transient long lastCarrotAt;

   public boolean isEmpty() {
      return checkInDay == 0;
   }
}
