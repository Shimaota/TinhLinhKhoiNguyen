package com.tlnt.server.social;

/**
 * 1 người trong danh sách bạn bè (cột {@code player.friends}, JSON). Tên / ảnh đại diện / cấp là bản chụp lúc thêm,
 * cập nhật lại mỗi lần gửi danh sách mà người đó đang online -> bạn offline vẫn hiện đủ thông tin, không cần SQL.
 */
public final class FriendEntry {

   public int id;
   public String name;
   public String avatar;
   public int level;

   public FriendEntry() {
   }

   public FriendEntry(int id, String name, String avatar, int level) {
      this.id = id;
      this.name = name;
      this.avatar = avatar;
      this.level = level;
   }
}
