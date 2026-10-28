package com.tlnt.server.data.model;

/**
 * 1 dòng bảng {@code server_list}: máy chủ hiện trong danh sách chọn server của client
 * (xem {@code MessageHandler case 43} / {@code ServerInfo} phía client).
 */
public record ServerEntry(int id, String name, String host, int port) {
}
