package com.tlnt.server.db;

import com.tlnt.server.config.ServerConfig;
import com.tlnt.server.util.Log;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Connection pool MySQL/MariaDB (HikariCP). Server mở 2 pool: {@code tlnt-data} (dữ liệu tĩnh, chỉ đọc lúc khởi
 * động rồi nằm trong cache RAM) và {@code tlnt-player} (tài khoản + nhân vật: đăng nhập, tạo nhân vật, lưu).
 * Luồng game KHÔNG bao giờ gọi SQL — chỉ pool blocking và luồng ghi {@code db-writer}.
 */
public final class Database implements AutoCloseable {

   private final HikariDataSource dataSource;

   public Database(ServerConfig config) {
      this(config.getDbUrl(), config.getDbUser(), config.getDbPassword(), config.getDbPoolSize(), "tlnt-data");
   }

   public Database(ServerConfig config, boolean playerDatabase) {
      this(playerDatabase ? config.getPlayerDbUrl() : config.getDbUrl(), config.getDbUser(), config.getDbPassword(),
            playerDatabase ? config.getPlayerDbPoolSize() : config.getDbPoolSize(),
            playerDatabase ? "tlnt-player" : "tlnt-data");
   }

   private Database(String jdbcUrl, String username, String password, int poolSize, String poolName) {
      HikariConfig hikari = new HikariConfig();
      hikari.setJdbcUrl(jdbcUrl);
      hikari.setUsername(username);
      hikari.setPassword(password);
      hikari.setMaximumPoolSize(poolSize);
      hikari.setMinimumIdle(1);
      hikari.setPoolName(poolName);
      // Gộp batch INSERT thành 1 câu multi-row -> import hàng nghìn dòng nhanh hơn nhiều.
      hikari.addDataSourceProperty("rewriteBatchedStatements", "true");
      hikari.addDataSourceProperty("cachePrepStmts", "true");
      hikari.addDataSourceProperty("useServerPrepStmts", "false");
      this.dataSource = new HikariDataSource(hikari);
      Log.info("Da ket noi SQL: %s", jdbcUrl);
   }

   /** Admin panel: {đang dùng, rảnh, tổng, đang chờ} của pool. */
   public int[] poolStats() {
      var mx = dataSource.getHikariPoolMXBean();
      return mx == null ? new int[4] : new int[] {mx.getActiveConnections(), mx.getIdleConnections(),
            mx.getTotalConnections(), mx.getThreadsAwaitingConnection()};
   }

   public Connection getConnection() throws SQLException {
      return dataSource.getConnection();
   }


   @Override
   public void close() {
      dataSource.close();
   }
}
