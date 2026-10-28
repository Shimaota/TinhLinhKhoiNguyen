package com.tlnt.server.account;

/**
 * 1 dòng bảng {@code account} nằm trong RAM khi đang online. Chỉ các field đổi được lúc chơi
 * (MK cấp 2, chế độ bảo vệ) là mutable; mọi thay đổi đi qua {@link AccountService}, ghi DB ngay.
 */
public final class Account {

   /** Vai trò ({@code account.role}): người chơi thường. */
   public static final int ROLE_PLAYER = 0;
   /** GM: dùng được lệnh test trong chat dù {@code chat.testCommands} tắt. */
   public static final int ROLE_GM = 1;
   /** Quản trị viên (như GM). */
   public static final int ROLE_ADMIN = 2;

   private final int id;
   private final String username;
   private final String passwordHash;
   private final String email;
   private final boolean banned;
   private final String banReason;
   private volatile String secondaryPasswordHash;
   private volatile boolean protectionEnabled;
   /** Admin panel đổi được lúc đang online. */
   private volatile int role;

   public Account(int id, String username, String passwordHash, String email, String secondaryPasswordHash,
         boolean protectionEnabled, boolean banned, String banReason) {
      this(id, username, passwordHash, email, secondaryPasswordHash, protectionEnabled, banned, banReason, ROLE_PLAYER);
   }

   public Account(int id, String username, String passwordHash, String email, String secondaryPasswordHash,
         boolean protectionEnabled, boolean banned, String banReason, int role) {
      this.role = role;
      this.id = id;
      this.username = username;
      this.passwordHash = passwordHash;
      this.email = email;
      this.secondaryPasswordHash = secondaryPasswordHash;
      this.protectionEnabled = protectionEnabled;
      this.banned = banned;
      this.banReason = banReason;
   }

   public int getId() {
      return id;
   }

   public String getUsername() {
      return username;
   }

   public String getPasswordHash() {
      return passwordHash;
   }

   public String getEmail() {
      return email;
   }

   public boolean isBanned() {
      return banned;
   }

   public String getBanReason() {
      return banReason;
   }

   public boolean hasSecondaryPassword() {
      return secondaryPasswordHash != null;
   }

   public String getSecondaryPasswordHash() {
      return secondaryPasswordHash;
   }

   void setSecondaryPasswordHash(String hash) {
      this.secondaryPasswordHash = hash;
   }

   /** "Tài khoản đang được bảo vệ" (client gọi là isSecondaryPasswordLocked). */
   public boolean isProtectionEnabled() {
      return protectionEnabled;
   }

   void setProtectionEnabled(boolean enabled) {
      this.protectionEnabled = enabled;
   }

   public int getRole() {
      return role;
   }

   /** GM / quản trị viên: được dùng lệnh test trong chat. */
   public boolean isStaff() {
      return role >= ROLE_GM;
   }

   void setRole(int role) {
      this.role = role;
   }
}
