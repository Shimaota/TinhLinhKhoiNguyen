package com.tlnt.web.net;

import com.badlogic.gdx.Gdx;
import com.shimaota.a.a.IMessage;
import com.shimaota.a.a.Message;
import com.tlnt.core.ClientConfig;
import com.tlnt.core.GameMain;
import com.tlnt.core.Settings;
import com.tlnt.net.ConnectionErrorTask;
import com.tlnt.net.DisconnectHandler;
import com.tlnt.net.INetworkClient;
import com.tlnt.net.MessageHandler;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.screen.game.GameScreen;
import com.tlnt.ui.window.WindowType;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.events.MessageEvent;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;
import org.teavm.jso.websocket.WebSocket;

public class TeaVMWebSocketClient implements INetworkClient, WireCodec.FrameHandler {
   private WebSocket socket;
   private boolean connected;
   private boolean connecting;
   private int connectionId = -1;
   private volatile boolean intentionalDisconnect;
   private com.badlogic.gdx.utils.Timer.Task keepAliveTask;

   @Override
   public boolean isConnected() {
      return this.socket != null && this.connected;
   }

   @Override
   public boolean isConnecting() {
      return this.connecting;
   }

   @JSFunctor
   public interface TickerCallback extends JSObject {
      void tick();
   }

   @JSBody(params = { "callback" }, script =
       "try {\n" +
       "    if (window._tlkn_bg_worker) { window._tlkn_bg_worker.terminate(); }\n" +
       "    var code = \"setInterval(function() { postMessage('t'); }, 3000);\";\n" +
       "    var blob = new Blob([code], { type: 'application/javascript' });\n" +
       "    var worker = new Worker(URL.createObjectURL(blob));\n" +
       "    worker.onmessage = function() { callback(); };\n" +
       "    window._tlkn_bg_worker = worker;\n" +
       "} catch(e) {\n" +
       "    if (window._tlkn_bg_interval) clearInterval(window._tlkn_bg_interval);\n" +
       "    window._tlkn_bg_interval = setInterval(function() { callback(); }, 3000);\n" +
       "}"
   )
   private static native void initBackgroundWorker(TickerCallback callback);

   @JSBody(script =
       "try {\n" +
       "    if (window._tlkn_bg_worker) { window._tlkn_bg_worker.terminate(); window._tlkn_bg_worker = null; }\n" +
       "    if (window._tlkn_bg_interval) { clearInterval(window._tlkn_bg_interval); window._tlkn_bg_interval = null; }\n" +
       "} catch(e){}"
   )
   private static native void stopBackgroundWorker();

   @JSBody(params = { "client" }, script =
       "if (!window._tlkn_vis_init) {\n" +
       "    window._tlkn_vis_init = true;\n" +
       "    document.addEventListener('visibilitychange', function() {\n" +
       "        if (document.visibilityState === 'visible') {\n" +
       "            try {\n" +
       "                if (!client.isConnected() && !client.isConnecting()) {\n" +
       "                    console.log('Tab da hien thi lai, dang ket noi WebSocket...');\n" +
       "                    client.connectAndSetup();\n" +
       "                }\n" +
       "            } catch(e) {}\n" +
       "        }\n" +
       "    });\n" +
       "}"
   )
   private static native void initVisibilityListener(TeaVMWebSocketClient client);

   @JSBody(params = { "client", "delayMs" }, script =
       "setTimeout(function() {\n" +
       "    try {\n" +
       "        if (!client.isConnected() && !client.isConnecting()) {\n" +
       "            console.log('Tu dong ket noi lai WebSocket...');\n" +
       "            client.connectAndSetup();\n" +
       "        }\n" +
       "    } catch(e) {}\n" +
       "}, delayMs);"
   )
   private static native void scheduleReconnect(TeaVMWebSocketClient client, int delayMs);

   private boolean visibilityListenerInitialized;

   @JSBody(params = { "status", "url" }, script = "try { window.dispatchEvent(new CustomEvent('tlkn_net_status', { detail: { status: status, url: url } })); } catch(e){}")
   private static native void dispatchNetStatus(String status, String url);

   // Resolves the WebSocket URL automatically, allowing it to be changed WITHOUT REBUILDING:
   // 1. URL search param (?ws=... or ?wsUrl=... or ?ws_url=...)
   // 2. Global JS variable (window.TLKN_WS_URL or window.wsUrl — set in index.html)
   // 3. Meta tag (<meta name="tlkn-ws-url" content="...">)
   // 4. ClientConfig.SERVER_WS_URL (from client.properties: server.ws.url=wss://example.com/ws)
   // 5. Fallback: build scheme + host + "/ws" when configWsUrl is empty
   @JSBody(params = { "configWsUrl", "defaultHost" }, script =
       "try {\n" +
       "    var p = new URLSearchParams(window.location.search);\n" +
       "    var q = p.get('ws') || p.get('wsUrl') || p.get('ws_url') || p.get('url');\n" +
       "    if (q && q.trim()) return q.trim();\n" +
       "    if (window.TLKN_WS_URL && String(window.TLKN_WS_URL).trim()) return String(window.TLKN_WS_URL).trim();\n" +
       "    if (window.wsUrl && String(window.wsUrl).trim()) return String(window.wsUrl).trim();\n" +
       "    var m = document.querySelector('meta[name=\"tlkn-ws-url\"], meta[name=\"ws-url\"]');\n" +
       "    if (m && m.content && m.content.trim()) return m.content.trim();\n" +
       "    var qHost = (p.get('host') || '').trim();\n" +
       "    var qPort = (p.get('port') || '').trim();\n" +
       "    var base = (configWsUrl && configWsUrl.trim()) ? configWsUrl.trim() : '';\n" +
       "    var isHttps = window.location.protocol === 'https:';\n" +
       "    if (base) {\n" +
       "        if (base.charAt(0) === '/') {\n" +
       "            var proto = isHttps ? 'wss:' : 'ws:';\n" +
       "            var bHost = window.location.hostname || '127.0.0.1';\n" +
       "            var bPort = window.location.port ? (':' + window.location.port) : '';\n" +
       "            return proto + '//' + (qHost || bHost) + (qPort ? (':' + qPort) : bPort) + base;\n" +
       "        }\n" +
       "        try {\n" +
       "            var u = new URL(base);\n" +
       "            if (qHost) u.hostname = qHost;\n" +
       "            if (qPort) u.port = qPort;\n" +
       "            if (isHttps && u.protocol === 'ws:' && u.hostname !== 'localhost' && u.hostname !== '127.0.0.1') {\n" +
       "                u.protocol = 'wss:';\n" +
       "            }\n" +
       "            return u.href;\n" +
       "        } catch(e) {\n" +
       "            return base;\n" +
       "        }\n" +
       "    }\n" +
       "    var host = qHost || defaultHost || window.location.hostname || '127.0.0.1';\n" +
       "    if (!host || host === 'localhost') {\n" +
       "        if (window.location.hostname && window.location.hostname !== 'localhost') {\n" +
       "            host = window.location.hostname;\n" +
       "        } else {\n" +
       "            host = '127.0.0.1';\n" +
       "        }\n" +
       "    }\n" +
       "    var port = qPort || '7503';\n" +
       "    var scheme = isHttps ? 'wss://' : 'ws://';\n" +
       "    var portStr = (port === '80' || port === '443') ? '' : (':' + port);\n" +
       "    return scheme + host + portStr + '/ws';\n" +
       "} catch(e) {\n" +
       "    return 'ws://127.0.0.1:7503/ws';\n" +
       "}"
   )
   private static native String resolveWebSocketUrl(String configWsUrl, String defaultHost);

   @Override
   public synchronized void connectAndSetup() {
      if (this.connecting || this.isConnected()) {
         return;
      }
      if (!this.visibilityListenerInitialized) {
         this.visibilityListenerInitialized = true;
         initVisibilityListener(this);
      }

      this.connecting = true;
      try {
         String configWsUrl = ClientConfig.SERVER_WS_URL;
         String defaultHost = ClientConfig.SERVER_HOST;

         final String url = resolveWebSocketUrl(configWsUrl, defaultHost);
         System.out.println("Dang ket noi WebSocket toi: " + url);
         dispatchNetStatus("connecting", url);

         this.socket = WebSocket.create(url);
         this.socket.setBinaryType("arraybuffer");

         this.socket.addEventListener("open", new EventListener<Event>() {
            @Override
            public void handleEvent(Event event) {
               connecting = false;
               connected = true;
               System.out.println("WebSocket da ket noi thanh cong!");
               dispatchNetStatus("connected", url);

               startKeepAlive();
               ServerPacketSender.getInstance().sendHandshake();

               Gdx.app.postRunnable(() -> {
                  GameScreen gameScreen = GameMain.instance != null ? GameMain.instance.getGameScreen() : null;
                  if (gameScreen != null && gameScreen.active != null
                        && gameScreen.active.getActiveWindowType() == WindowType.DISCONNECTED_PANEL) {
                     gameScreen.active.showDialog(WindowType.LOGIN_PANEL);
                  }
               });
            }
         });

         this.socket.addEventListener("message", new EventListener<Event>() {
            @Override
            public void handleEvent(Event event) {
               MessageEvent msgEvent = (MessageEvent) event;
               Object rawData = msgEvent.getData();
               if (rawData instanceof ArrayBuffer) {
                  ArrayBuffer buffer = (ArrayBuffer) rawData;
                  Int8Array int8 = Int8Array.create(buffer);
                  byte[] bytes = new byte[int8.getLength()];
                  for (int i = 0; i < bytes.length; i++) {
                     bytes[i] = int8.get(i);
                  }
                  WireCodec.decodeFrames(bytes, TeaVMWebSocketClient.this);
               }
            }
         });

         this.socket.addEventListener("close", new EventListener<Event>() {
            @Override
            public void handleEvent(Event event) {
               System.out.println("WebSocket da dong ket noi");
               dispatchNetStatus("disconnected", url);
               onDisconnected();
            }
         });

         this.socket.addEventListener("error", new EventListener<Event>() {
            @Override
            public void handleEvent(Event event) {
               System.err.println("Loi ket noi WebSocket!");
               connecting = false;
               dispatchNetStatus("error", url);
               Gdx.app.postRunnable(new ConnectionErrorTask(TeaVMWebSocketClient.this));
            }
         });

      } catch (Throwable t) {
         connecting = false;
         System.err.println("Exception khi mo WebSocket: " + t.getMessage());
         dispatchNetStatus("error", "");
         ServerPacketSender.getInstance().connectToServer(2000);
         Gdx.app.postRunnable(new ConnectionErrorTask(this));
      }
   }

   private void startKeepAlive() {
      stopKeepAlive();
      initBackgroundWorker(() -> {
         if (isConnected()) {
            sendRawBytes(WireCodec.KEEP_ALIVE_FRAME);
         }
      });
      keepAliveTask = com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
         @Override
         public void run() {
            if (isConnected()) {
               sendRawBytes(WireCodec.KEEP_ALIVE_FRAME);
            }
         }
      }, 5f, 5f);
   }

   private void stopKeepAlive() {
      stopBackgroundWorker();
      if (keepAliveTask != null) {
         keepAliveTask.cancel();
         keepAliveTask = null;
      }
   }

   @Override
   public void onRegisterTcp(int connId) {
      this.connectionId = connId;
      System.out.println("Nhan RegisterTCP, connectionId = #" + connId);
   }

   @Override
   public void onKeepAlive() {
   }

   @Override
   public void onPing(int id, boolean isReply) {
      if (!isReply && this.isConnected()) {
         sendRawBytes(WireCodec.encodePing(id, true));
      }
   }

   @Override
   public void onDataPacket(byte cmd, byte[] payload) {
      MessageHandler.getInstance().receiveMessage(new Message(cmd, payload));
   }

   private void sendRawBytes(byte[] bytes) {
      if (this.socket == null || !this.connected) return;
      Int8Array int8 = Int8Array.create(bytes.length);
      for (int i = 0; i < bytes.length; i++) {
         int8.set(i, bytes[i]);
      }
      this.socket.send(int8.getBuffer());
   }

   @Override
   public void sendTcp(IMessage message) {
      if (this.isConnected()) {
         byte[] frame = WireCodec.encodeDataPacket(message.getOpcode(), message.getBytes());
         sendRawBytes(frame);
      }
   }

   @Override
   public void disconnect() {
      stopKeepAlive();
      if (this.socket != null) {
         this.connected = false;
         this.socket.close();
         this.socket = null;
      }
   }

   @Override
   public void switchServer() {
      if (this.isConnected()) {
         this.intentionalDisconnect = true;
         this.disconnect();
      } else {
         ServerPacketSender.getInstance().connectToServer(0);
      }
   }

   @Override
   public void onDisconnected() {
      this.connected = false;
      this.connecting = false;
      stopKeepAlive();
      if (this.socket != null) {
         try {
            this.socket.close();
         } catch (Throwable ignored) {
         }
         this.socket = null;
      }

      if (this.intentionalDisconnect) {
         this.intentionalDisconnect = false;
         ServerPacketSender.getInstance().connectToServer(0);
         return;
      }

      scheduleReconnect(this, 1500);
      ServerPacketSender.getInstance().connectToServer(1000);
      Gdx.app.postRunnable(new DisconnectHandler(this));
   }
}
