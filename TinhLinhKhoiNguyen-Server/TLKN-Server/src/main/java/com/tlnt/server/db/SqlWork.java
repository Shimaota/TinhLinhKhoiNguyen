package com.tlnt.server.db;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 1 đoạn SQL chạy trên 1 kết nối đang mở transaction (luồng {@code db-writer}). Dùng để ghi CÙNG transaction với lượt
 * lưu nhân vật ({@code PlayerService.saveWith}): túi đồ + thư / kí gửi hoặc cùng được ghi, hoặc cùng không.
 */
@FunctionalInterface
public interface SqlWork {

   void run(Connection conn) throws SQLException;
}
