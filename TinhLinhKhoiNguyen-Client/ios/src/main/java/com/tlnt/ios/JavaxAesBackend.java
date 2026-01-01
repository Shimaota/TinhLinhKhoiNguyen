package com.tlnt.ios;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.tlnt.util.AesDecryptor;

/**
 * AES-CBC decryption with javax.crypto (RoboVM's built-in provider, already forceLinked in robovm.xml) instead of the pure-Java AesDecryptor
 * (written for the TeaVM web build, many times slower on RoboVM -> hitches on map change since atlases are decrypted on the render thread).
 */
final class JavaxAesBackend implements AesDecryptor.Backend {
    @Override
    public byte[] decryptAesCbc(byte[] ciphertext, byte[] key) throws Exception {
        // Cipher cannot be shared across threads (atlas / icon loading may happen on different threads) -> create a new one each time
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(ciphertext, 0, 16));
        return cipher.doFinal(ciphertext, 16, ciphertext.length - 16);
    }
}
