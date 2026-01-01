package com.tlnt.web;

import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.tlnt.core.GameMain;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.util.FileStorage;
import com.tlnt.util.HttpClient;
import com.tlnt.web.net.TeaVMHttpHandler;
import com.tlnt.web.net.TeaVMWebSocketClient;
import com.tlnt.web.util.TeaVMWebStorage;
import org.teavm.jso.JSBody;

public class WebLauncher {
   public static void main(String[] args) {
      // 1. WebSocket network client for the Web environment
      ServerPacketSender.setNetworkClientFactory(TeaVMWebSocketClient::new);

      // 2. HttpHandler using the Web Fetch API & coroutines for the Web environment
      HttpClient.setCustomHandler(new TeaVMHttpHandler());

      // 3. Web-specific "Clear data & reload page" action
      FileStorage.setWebWipeAction(TeaVMWebStorage::wipeDataAndReload);

      // 4. WebGL canvas configuration (width=0, height=0 to auto-fit 100% of the window)
      WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
      config.width = 0;
      config.height = 0;
      config.usePhysicalPixels = true;
      config.stencil = true;
      // Phones: devicePixelRatio 3 + MSAA = drawing ~9x the pixels -> GPU overloaded, very laggy.
      // Cap the pixel ratio and disable antialiasing (2D game, practically no visible difference).
      boolean mobile = isMobile();
      double forced = forcedPixelRatio();
      if (forced > 0) {
         capPixelRatio(forced);
      } else if (mobile) {
         capPixelRatio(MOBILE_MAX_PIXEL_RATIO);
      }
      config.antialiasing = !mobile;

      // 5. Start GameMain from core
      new WebApplication(new GameMain(), config);
   }

   // 1.5 looks blurry on iPhone (a 3x screen upscaled 2x); 2 still draws only ~44% of the pixels of 3x.
   private static final double MOBILE_MAX_PIXEL_RATIO = 2.0;

   @JSBody(script = "return /Android|iPhone|iPad|iPod/i.test(navigator.userAgent)"
         + " || (navigator.maxTouchPoints > 1 && /Macintosh/.test(navigator.userAgent));")
   private static native boolean isMobile();

   // Testing on real devices: ?dpr=1 / ?dpr=3 ... overrides the cap (0 = no override).
   @JSBody(script = "var m = /[?&]dpr=([0-9.]+)/.exec(location.search); return m ? parseFloat(m[1]) || 0 : 0;")
   private static native double forcedPixelRatio();

   // WebGraphics reads the global devicePixelRatio variable to compute the canvas size.
   @JSBody(params = "max", script = "if ((window.devicePixelRatio || 1) <= max) return;"
         + "Object.defineProperty(window, 'devicePixelRatio', {get: function() { return max; }, configurable: true});")
   private static native void capPixelRatio(double max);
}
