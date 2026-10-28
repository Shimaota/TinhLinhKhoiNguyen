package com.tlnt.server.account;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Semaphore;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash mật khẩu bằng PBKDF2-HMAC-SHA256 có sẵn trong JDK (không thêm thư viện). Chuỗi lưu DB:
 * {@code pbkdf2$<iterations>$<salt base64>$<hash base64>} — số vòng nằm trong chuỗi nên đổi {@link #ITERATIONS} vẫn kiểm
 * tra được hash cũ.
 *
 * <p>{@link #ITERATIONS} = 1 (chủ server chọn 2026-10-26): kiểm tra mật khẩu ~vài µs, 1.000 người đăng nhập cùng lúc sau
 * khởi động lại không phải xếp hàng (120.000 vòng trên VPS 2 nhân: 0,3-1,3 s / lượt). Mật khẩu vẫn không lưu dạng chữ
 * (salt riêng + SHA-256), nhưng lộ DB thì dò mật khẩu yếu nhanh hơn nhiều so với 120.000 vòng. Hash cũ: đăng nhập đúng 1
 * lần thì {@code AccountService} băm lại theo số vòng mới ({@link #needsRehash}); trong lúc chờ, tối đa (số nhân − 1) lượt
 * băm nặng chạy cùng lúc ({@link #SLOTS}) để chừa CPU cho gameplay.
 */
public final class PasswordHasher {

   private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
   static final int ITERATIONS = 1;
   private static final int SALT_BYTES = 16;
   private static final int HASH_BITS = 256;
   /** Từ bấy nhiêu vòng trở lên mới tính là băm nặng (phải xin {@link #SLOTS}). */
   private static final int HEAVY_ITERATIONS = 1_000;
   private static final SecureRandom RANDOM = new SecureRandom();
   private static final Semaphore SLOTS = new Semaphore(Math.max(1, Runtime.getRuntime().availableProcessors() - 1));

   private PasswordHasher() {
   }

   public static String hash(String password) {
      byte[] salt = new byte[SALT_BYTES];
      RANDOM.nextBytes(salt);
      byte[] hash = pbkdf2(password, salt, ITERATIONS);
      Base64.Encoder b64 = Base64.getEncoder();
      return "pbkdf2$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(hash);
   }

   public static boolean verify(String password, String stored) {
      if (password == null || stored == null) {
         return false;
      }
      String[] parts = stored.split("\\$");
      if (parts.length != 4 || !parts[0].equals("pbkdf2")) {
         return false;
      }
      Base64.Decoder b64 = Base64.getDecoder();
      byte[] salt = b64.decode(parts[2]);
      byte[] expected = b64.decode(parts[3]);
      byte[] actual = pbkdf2(password, salt, Integer.parseInt(parts[1]));
      return MessageDigest.isEqual(expected, actual); // so sánh thời gian hằng, chống dò theo thời gian
   }

   /** Hash lưu theo số vòng khác {@link #ITERATIONS} -> nên băm lại sau khi đã kiểm tra đúng mật khẩu. */
   public static boolean needsRehash(String stored) {
      if (stored == null) {
         return false;
      }
      String[] parts = stored.split("\\$");
      return parts.length == 4 && parts[0].equals("pbkdf2") && !parts[1].equals(Integer.toString(ITERATIONS));
   }

   private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
      boolean heavy = iterations >= HEAVY_ITERATIONS;
      if (heavy) {
         SLOTS.acquireUninterruptibly();
      }
      try {
         return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
      } catch (GeneralSecurityException e) {
         throw new IllegalStateException("JDK khong ho tro " + ALGORITHM, e);
      } finally {
         if (heavy) {
            SLOTS.release();
         }
         spec.clearPassword();
      }
   }
}
