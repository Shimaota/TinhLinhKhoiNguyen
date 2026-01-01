package com.tlnt.util;

import java.util.Arrays;

/**
 * Pure-Java AES-CBC decryption with PKCS5Padding (no javax.crypto dependency, for TeaVM / WebAssembly compatibility).
 */
public final class AesDecryptor {

   private static final byte[] S_BOX = {
      (byte)0x63, (byte)0x7c, (byte)0x77, (byte)0x7b, (byte)0xf2, (byte)0x6b, (byte)0x6f, (byte)0xc5,
      (byte)0x30, (byte)0x01, (byte)0x67, (byte)0x2b, (byte)0xfe, (byte)0xd7, (byte)0xab, (byte)0x76,
      (byte)0xca, (byte)0x82, (byte)0xc9, (byte)0x7d, (byte)0xfa, (byte)0x59, (byte)0x47, (byte)0xf0,
      (byte)0xad, (byte)0xd4, (byte)0xa2, (byte)0xaf, (byte)0x9c, (byte)0xa4, (byte)0x72, (byte)0xc0,
      (byte)0xb7, (byte)0xfd, (byte)0x93, (byte)0x26, (byte)0x36, (byte)0x3f, (byte)0xf7, (byte)0xcc,
      (byte)0x34, (byte)0xa5, (byte)0xe5, (byte)0xf1, (byte)0x71, (byte)0xd8, (byte)0x31, (byte)0x15,
      (byte)0x04, (byte)0xc7, (byte)0x23, (byte)0xc3, (byte)0x18, (byte)0x96, (byte)0x05, (byte)0x9a,
      (byte)0x07, (byte)0x12, (byte)0x80, (byte)0xe2, (byte)0xeb, (byte)0x27, (byte)0xb2, (byte)0x75,
      (byte)0x09, (byte)0x83, (byte)0x2c, (byte)0x1a, (byte)0x1b, (byte)0x6e, (byte)0x5a, (byte)0xa0,
      (byte)0x52, (byte)0x3b, (byte)0xd6, (byte)0xb3, (byte)0x29, (byte)0xe3, (byte)0x2f, (byte)0x84,
      (byte)0x53, (byte)0xd1, (byte)0x00, (byte)0xed, (byte)0x20, (byte)0xfc, (byte)0xb1, (byte)0x5b,
      (byte)0x6a, (byte)0xcb, (byte)0xbe, (byte)0x39, (byte)0x4a, (byte)0x4c, (byte)0x58, (byte)0xcf,
      (byte)0xd0, (byte)0xef, (byte)0xaa, (byte)0xfb, (byte)0x43, (byte)0x4d, (byte)0x33, (byte)0x85,
      (byte)0x45, (byte)0xf9, (byte)0x02, (byte)0x7f, (byte)0x50, (byte)0x3c, (byte)0x9f, (byte)0xa8,
      (byte)0x51, (byte)0xa3, (byte)0x40, (byte)0x8f, (byte)0x92, (byte)0x9d, (byte)0x38, (byte)0xf5,
      (byte)0xbc, (byte)0xb6, (byte)0xda, (byte)0x21, (byte)0x10, (byte)0xff, (byte)0xf3, (byte)0xd2,
      (byte)0xcd, (byte)0x0c, (byte)0x13, (byte)0xec, (byte)0x5f, (byte)0x97, (byte)0x44, (byte)0x17,
      (byte)0xc4, (byte)0xa7, (byte)0x7e, (byte)0x3d, (byte)0x64, (byte)0x5d, (byte)0x19, (byte)0x73,
      (byte)0x60, (byte)0x81, (byte)0x4f, (byte)0xdc, (byte)0x22, (byte)0x2a, (byte)0x90, (byte)0x88,
      (byte)0x46, (byte)0xee, (byte)0xb8, (byte)0x14, (byte)0xde, (byte)0x5e, (byte)0x0b, (byte)0xdb,
      (byte)0xe0, (byte)0x32, (byte)0x3a, (byte)0x0a, (byte)0x49, (byte)0x06, (byte)0x24, (byte)0x5c,
      (byte)0xc2, (byte)0xd3, (byte)0xac, (byte)0x62, (byte)0x91, (byte)0x95, (byte)0xe4, (byte)0x79,
      (byte)0xe7, (byte)0xc8, (byte)0x37, (byte)0x6d, (byte)0x8d, (byte)0xd5, (byte)0x4e, (byte)0xa9,
      (byte)0x6c, (byte)0x56, (byte)0xf4, (byte)0xea, (byte)0x65, (byte)0x7a, (byte)0xae, (byte)0x08,
      (byte)0xba, (byte)0x78, (byte)0x25, (byte)0x2e, (byte)0x1c, (byte)0xa6, (byte)0xb4, (byte)0xc6,
      (byte)0xe8, (byte)0xdd, (byte)0x74, (byte)0x1f, (byte)0x4b, (byte)0xbd, (byte)0x8b, (byte)0x8a,
      (byte)0x70, (byte)0x3e, (byte)0xb5, (byte)0x66, (byte)0x48, (byte)0x03, (byte)0xf6, (byte)0x0e,
      (byte)0x61, (byte)0x35, (byte)0x57, (byte)0xb9, (byte)0x86, (byte)0xc1, (byte)0x1d, (byte)0x9e,
      (byte)0xe1, (byte)0xf8, (byte)0x98, (byte)0x11, (byte)0x69, (byte)0xd9, (byte)0x8e, (byte)0x94,
      (byte)0x9b, (byte)0x1e, (byte)0x87, (byte)0xe9, (byte)0xce, (byte)0x55, (byte)0x28, (byte)0xdf,
      (byte)0x8c, (byte)0xa1, (byte)0x89, (byte)0x0d, (byte)0xbf, (byte)0xe6, (byte)0x42, (byte)0x68,
      (byte)0x41, (byte)0x99, (byte)0x2d, (byte)0x0f, (byte)0xb0, (byte)0x54, (byte)0xbb, (byte)0x16
   };

   private static final byte[] INV_S_BOX = new byte[256];
   private static final int[] RCON = {
      0x00, 0x01, 0x02, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x1b, 0x36
   };

   static {
      for (int i = 0; i < 256; i++) {
         INV_S_BOX[S_BOX[i] & 0xFF] = (byte) i;
      }
   }

   /** AES-CBC + PKCS5 decryption (the first 16 bytes are the IV), same format as {@link #decryptAesCbc}. */
   public interface Backend {
      byte[] decryptAesCbc(byte[] ciphertext, byte[] key) throws Exception;
   }

   /**
    * Fast platform decryptor (e.g. javax.crypto, set by the iOS launcher), null = use the pure-Java version below. The pure-Java
    * version is ~10-30x slower: on iPhone (RoboVM, no JIT) decrypting atlases on every map change made the game hitch.
    */
   public static volatile Backend backend;

   private AesDecryptor() {
   }

   public static byte[] decryptAesCbc(byte[] ciphertext, byte[] key) {
      if (key == null || (key.length != 16 && key.length != 24 && key.length != 32)) {
         throw new IllegalArgumentException("Invalid AES key: " + (key == null ? "null" : key.length + " bytes"));
      }
      if (ciphertext == null || ciphertext.length < 32 || (ciphertext.length - 16) % 16 != 0) {
         throw new IllegalArgumentException("Invalid ciphertext length: " + (ciphertext == null ? 0 : ciphertext.length));
      }

      Backend fast = backend;
      if (fast != null) {
         try {
            return fast.decryptAesCbc(ciphertext, key);
         } catch (Exception e) {
            // Wrong key / corrupt data: let the pure-Java version below throw as before (the caller catches and handles it).
         }
      }

      byte[] iv = new byte[16];
      System.arraycopy(ciphertext, 0, iv, 0, 16);

      int encryptedLen = ciphertext.length - 16;
      int numRounds = key.length == 32 ? 14 : (key.length == 24 ? 12 : 10);
      int[] roundKeys = keyExpansion(key, numRounds);

      byte[] decrypted = new byte[encryptedLen];
      byte[] prevBlock = iv;
      byte[] block = new byte[16];

      for (int offset = 16; offset < ciphertext.length; offset += 16) {
         System.arraycopy(ciphertext, offset, block, 0, 16);
         byte[] outBlock = invCipher(block, roundKeys, numRounds);
         for (int i = 0; i < 16; i++) {
            decrypted[offset - 16 + i] = (byte) (outBlock[i] ^ prevBlock[i]);
         }
         prevBlock = block;
         block = new byte[16];
      }

      // Strip PKCS5 / PKCS7 padding
      int padLen = decrypted[decrypted.length - 1] & 0xFF;
      if (padLen < 1 || padLen > 16) {
         throw new RuntimeException("Invalid PKCS5 padding value: " + padLen);
      }
      for (int i = decrypted.length - padLen; i < decrypted.length; i++) {
         if ((decrypted[i] & 0xFF) != padLen) {
            throw new RuntimeException("Corrupted PKCS5 padding");
         }
      }

      return Arrays.copyOf(decrypted, decrypted.length - padLen);
   }

   private static byte[] invCipher(byte[] block, int[] roundKeys, int numRounds) {
      byte[] state = Arrays.copyOf(block, 16);

      addRoundKey(state, roundKeys, numRounds * 4);

      for (int round = numRounds - 1; round > 0; round--) {
         invShiftRows(state);
         invSubBytes(state);
         addRoundKey(state, roundKeys, round * 4);
         invMixColumns(state);
      }

      invShiftRows(state);
      invSubBytes(state);
      addRoundKey(state, roundKeys, 0);

      return state;
   }

   private static void invSubBytes(byte[] state) {
      for (int i = 0; i < 16; i++) {
         state[i] = INV_S_BOX[state[i] & 0xFF];
      }
   }

   private static void invShiftRows(byte[] s) {
      byte t;
      // Row 1: shift right 1 (s1, s5, s9, s13)
      t = s[13]; s[13] = s[9]; s[9] = s[5]; s[5] = s[1]; s[1] = t;
      // Row 2: shift right 2 (s2, s6, s10, s14)
      t = s[2]; s[2] = s[10]; s[10] = t;
      t = s[6]; s[6] = s[14]; s[14] = t;
      // Row 3: shift right 3 = shift left 1 (s3, s7, s11, s15)
      t = s[3]; s[3] = s[7]; s[7] = s[11]; s[11] = s[15]; s[15] = t;
   }

   private static int xtime(int x) {
      return ((x << 1) ^ (((x >> 7) & 1) * 0x11b)) & 0xFF;
   }

   private static int mul(int x, int y) {
      int m2 = xtime(x);
      int m4 = xtime(m2);
      int m8 = xtime(m4);
      if (y == 0x09) return m8 ^ x;
      if (y == 0x0b) return m8 ^ m2 ^ x;
      if (y == 0x0d) return m8 ^ m4 ^ x;
      if (y == 0x0e) return m8 ^ m4 ^ m2;
      return 0;
   }

   private static void invMixColumns(byte[] s) {
      for (int c = 0; c < 4; c++) {
         int i = c * 4;
         int a0 = s[i] & 0xFF;
         int a1 = s[i + 1] & 0xFF;
         int a2 = s[i + 2] & 0xFF;
         int a3 = s[i + 3] & 0xFF;

         s[i]     = (byte) (mul(a0, 0x0e) ^ mul(a1, 0x0b) ^ mul(a2, 0x0d) ^ mul(a3, 0x09));
         s[i + 1] = (byte) (mul(a0, 0x09) ^ mul(a1, 0x0e) ^ mul(a2, 0x0b) ^ mul(a3, 0x0d));
         s[i + 2] = (byte) (mul(a0, 0x0d) ^ mul(a1, 0x09) ^ mul(a2, 0x0e) ^ mul(a3, 0x0b));
         s[i + 3] = (byte) (mul(a0, 0x0b) ^ mul(a1, 0x0d) ^ mul(a2, 0x09) ^ mul(a3, 0x0e));
      }
   }

   private static void addRoundKey(byte[] state, int[] roundKeys, int keyOffset) {
      for (int i = 0; i < 4; i++) {
         int k = roundKeys[keyOffset + i];
         state[i * 4]     ^= (byte) (k >>> 24);
         state[i * 4 + 1] ^= (byte) (k >>> 16);
         state[i * 4 + 2] ^= (byte) (k >>> 8);
         state[i * 4 + 3] ^= (byte) k;
      }
   }

   private static int[] keyExpansion(byte[] key, int numRounds) {
      int nk = key.length / 4;
      int totalWords = 4 * (numRounds + 1);
      int[] w = new int[totalWords];

      for (int i = 0; i < nk; i++) {
         w[i] = ((key[4 * i] & 0xFF) << 24)
              | ((key[4 * i + 1] & 0xFF) << 16)
              | ((key[4 * i + 2] & 0xFF) << 8)
              | (key[4 * i + 3] & 0xFF);
      }

      for (int i = nk; i < totalWords; i++) {
         int temp = w[i - 1];
         if (i % nk == 0) {
            temp = subWord(rotWord(temp)) ^ (RCON[i / nk] << 24);
         } else if (nk > 6 && i % nk == 4) {
            temp = subWord(temp);
         }
         w[i] = w[i - nk] ^ temp;
      }

      return w;
   }

   private static int rotWord(int w) {
      return (w << 8) | (w >>> 24);
   }

   private static int subWord(int w) {
      return ((S_BOX[(w >>> 24) & 0xFF] & 0xFF) << 24)
           | ((S_BOX[(w >>> 16) & 0xFF] & 0xFF) << 16)
           | ((S_BOX[(w >>> 8) & 0xFF] & 0xFF) << 8)
           | (S_BOX[w & 0xFF] & 0xFF);
   }
}
