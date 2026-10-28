package com.tlnt.server.item;

import com.tlnt.server.player.PlayerData;
import com.tlnt.server.session.PlayerSession;
import com.tlnt.server.session.SessionManager;

import java.util.concurrent.Executor;

/**
 * Chạy định kỳ ({@link #INTERVAL_SECONDS}) trên luồng scheduler: người nào đã tới {@link PlayerData#nextExpireAt}
 * thì xếp {@link ItemService#removeExpired} vào hàng đợi session của họ (túi đồ chỉ luồng session được sửa).
 * Mỗi lượt chỉ đọc 1 số volatile / người -> 10.000 người vẫn gần như không tốn gì.
 */
public final class ItemExpiryTask implements Runnable {

   public static final int INTERVAL_SECONDS = 10;

   private final SessionManager sessions;
   private final ItemService items;
   private final Executor gamePool;

   public ItemExpiryTask(SessionManager sessions, ItemService items, Executor gamePool) {
      this.sessions = sessions;
      this.items = items;
      this.gamePool = gamePool;
   }

   @Override
   public void run() {
      long now = System.currentTimeMillis();
      for (PlayerSession session : sessions.snapshot()) {
         PlayerData d = session.getPlayer();
         if (d == null || !d.inWorld || now < d.nextExpireAt) {
            continue;
         }
         d.nextExpireAt = Long.MAX_VALUE; // tránh xếp trùng; removeExpired tính lại
         session.inbox().execute(() -> items.removeExpired(session), gamePool);
      }
   }
}
