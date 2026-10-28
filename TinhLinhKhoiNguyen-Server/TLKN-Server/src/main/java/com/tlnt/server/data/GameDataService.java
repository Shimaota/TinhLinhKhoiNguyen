package com.tlnt.server.data;

import com.tlnt.server.data.sql.GameDataRepository;
import com.tlnt.server.util.Log;

import java.sql.SQLException;

/**
 * Nạp dữ liệu tĩnh (game/map/item/skill) từ SQL lên {@link GameDataCache} — logic server chỉ đọc
 * cache, không query SQL. Client vẫn tự tải file DataGame qua HTTP như cũ.
 */
public final class GameDataService {

   private final GameDataRepository repository;

   public GameDataService(GameDataRepository repository) {
      this.repository = repository;
   }

   /**
    * Chỉ nạp lại 2 bảng nhỏ admin sửa được lúc chạy: {@code server_list} (danh sách máy chủ op 43) và
    * {@code item_appearance} (ngoại hình item — áp dụng từ lần mặc / vào game sau). Lỗi -> giữ bản cũ.
    */
   public void reloadSmall() {
      try {
         GameDataCache.setServers(repository.loadServerList());
         GameDataCache.setItemAppearances(repository.loadItemAppearances());
      } catch (SQLException e) {
         Log.warn("Khong nap lai duoc server_list / item_appearance: %s", e.getMessage());
      }
   }

   /** Đọc lại toàn bộ SQL -> cache. Gọi được lúc server đang chạy (vd lệnh admin sau khi sửa SQL). */
   public void reloadCache() throws SQLException {
      long start = System.currentTimeMillis();
      GameDataCache.set(repository.loadGame(), repository.loadMap(), repository.loadItem(), repository.loadSkill());
      GameDataCache.setServers(repository.loadServerList());
      GameDataCache.setCharBaseStats(repository.loadCharBaseStats());
      GameDataCache.setItemAppearances(repository.loadItemAppearances());
      GameDataCache.setSkillEffects(repository.loadSkillEffects());
      Log.info("Da nap cache du lieu tu SQL trong %d ms: %s", System.currentTimeMillis() - start,
            GameDataCache.summary());
   }
}
