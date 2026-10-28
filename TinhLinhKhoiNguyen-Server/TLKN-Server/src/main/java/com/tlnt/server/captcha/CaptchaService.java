package com.tlnt.server.captcha;

import com.tlnt.server.net.Packets;
import com.tlnt.server.session.PlayerSession;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import javax.imageio.ImageIO;

/**
 * Captcha cho form đăng ký (RegisterDialog: ô "Captcha" + nút ảnh bấm để đổi). Mã lưu trong
 * {@link PlayerSession}, ảnh PNG gửi qua opcode 49 sub 0 ({@code GameState.captchaTexture}).
 * Mã dùng 1 lần, hết hạn sau {@link #TTL_MS}.
 */
public final class CaptchaService {

   private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // bỏ O/0, I/1 dễ nhầm
   private static final int LENGTH = 5;
   private static final int WIDTH = 224;
   private static final int HEIGHT = 60;
   private static final long TTL_MS = 2 * 60_000L;
   /** Chặn client bấm đổi captcha liên tục (mỗi lần là 1 lần vẽ ảnh). */
   private static final long MIN_INTERVAL_MS = 1_000L;

   private final SecureRandom random = new SecureRandom();

   /** Client xin captcha (mở form đăng ký / bấm vào ảnh). Bỏ qua nếu xin quá dồn dập. */
   public void issue(PlayerSession session) {
      long now = System.currentTimeMillis();
      if (now - session.getLastCaptchaRequestAt() < MIN_INTERVAL_MS) {
         return;
      }
      session.setLastCaptchaRequestAt(now);
      reissue(session);
   }

   /** Server chủ động cấp mã mới (mã cũ vừa bị dùng), không giới hạn tần suất. */
   public void reissue(PlayerSession session) {
      String code = randomCode();
      session.setCaptcha(code, System.currentTimeMillis() + TTL_MS);
      session.send(Packets.captcha(render(code)));
   }

   private String randomCode() {
      char[] chars = new char[LENGTH];
      for (int i = 0; i < LENGTH; i++) {
         chars[i] = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
      }
      return new String(chars);
   }

   private byte[] render(String code) {
      BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      try {
         g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         g.setColor(new Color(245, 235, 215));
         g.fillRect(0, 0, WIDTH, HEIGHT);

         // Nhiễu nền: chấm + đường cong.
         for (int i = 0; i < 120; i++) {
            g.setColor(randomColor(150, 220));
            g.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
         }
         g.setStroke(new BasicStroke(1.5f));
         for (int i = 0; i < 4; i++) {
            g.setColor(randomColor(100, 180));
            g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
         }

         // Từng ký tự xoay/lệch ngẫu nhiên.
         g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 38));
         int step = WIDTH / (LENGTH + 1);
         for (int i = 0; i < LENGTH; i++) {
            AffineTransform saved = g.getTransform();
            int x = step / 2 + i * step + random.nextInt(8);
            int y = HEIGHT / 2 + 14 + random.nextInt(8) - 4;
            g.rotate(Math.toRadians(random.nextInt(50) - 25), x, y);
            g.setColor(randomColor(20, 110));
            g.drawString(String.valueOf(code.charAt(i)), x, y);
            g.setTransform(saved);
         }
      } finally {
         g.dispose();
      }

      try (ByteArrayOutputStream out = new ByteArrayOutputStream(4096)) {
         ImageIO.write(image, "png", out);
         return out.toByteArray();
      } catch (IOException e) {
         throw new UncheckedIOException(e);
      }
   }

   private Color randomColor(int min, int max) {
      int range = max - min;
      return new Color(min + random.nextInt(range), min + random.nextInt(range), min + random.nextInt(range));
   }
}
