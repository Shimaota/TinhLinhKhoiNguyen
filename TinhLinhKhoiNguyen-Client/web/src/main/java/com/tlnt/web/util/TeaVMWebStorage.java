package com.tlnt.web.util;

import org.teavm.jso.JSBody;

public final class TeaVMWebStorage {

   private TeaVMWebStorage() {
   }

   @JSBody(script =
      "if (typeof window.TLKN_CLEAR_DATA === 'function') {\n" +
      "  window.TLKN_CLEAR_DATA(true);\n" +
      "  return;\n" +
      "}\n" +
      "console.log('TeaVMWebStorage: Bat dau xoa du lieu web...');\n" +
      "var done = false;\n" +
      "function finishReload() {\n" +
      "  if (done) return;\n" +
      "  done = true;\n" +
      "  console.log('TeaVMWebStorage: Da xoa xong, dang reload trang...');\n" +
      "  window.location.reload();\n" +
      "}\n" +
      "setTimeout(finishReload, 2000);\n" +
      "try {\n" +
      "  if (window.indexedDB) {\n" +
      "    var req = window.indexedDB.open('db/assets');\n" +
      "    req.onsuccess = function(e) {\n" +
      "      var db = e.target.result;\n" +
      "      try {\n" +
      "        if (db.objectStoreNames.contains('FILE_DATA')) {\n" +
      "          var tx = db.transaction('FILE_DATA', 'readwrite');\n" +
      "          tx.objectStore('FILE_DATA').clear();\n" +
      "          tx.oncomplete = function() { db.close(); step2(); };\n" +
      "          tx.onerror = function() { db.close(); step2(); };\n" +
      "          return;\n" +
      "        }\n" +
      "      } catch(err) {}\n" +
      "      db.close();\n" +
      "      step2();\n" +
      "    };\n" +
      "    req.onerror = function() { step2(); };\n" +
      "  } else {\n" +
      "    step2();\n" +
      "  }\n" +
      "} catch(e) {\n" +
      "  step2();\n" +
      "}\n" +
      "function step2() {\n" +
      "  try {\n" +
      "    if (window.indexedDB && window.indexedDB.deleteDatabase) {\n" +
      "      window.indexedDB.deleteDatabase('db/assets');\n" +
      "    }\n" +
      "  } catch(e) {}\n" +
      "  try {\n" +
      "    if (window.indexedDB && window.indexedDB.databases) {\n" +
      "      window.indexedDB.databases().then(function(dbs) {\n" +
      "        if (dbs) {\n" +
      "          dbs.forEach(function(d) {\n" +
      "            if (d.name && (d.name === 'db/assets' || d.name.indexOf('asset') !== -1)) {\n" +
      "              try { window.indexedDB.deleteDatabase(d.name); } catch(err) {}\n" +
      "            }\n" +
      "          });\n" +
      "        }\n" +
      "      }).catch(function() {});\n" +
      "    }\n" +
      "  } catch(e) {}\n" +
      "  try {\n" +
      "    if (window.caches && window.caches.keys) {\n" +
      "      window.caches.keys().then(function(keys) {\n" +
      "        keys.forEach(function(k) { window.caches.delete(k); });\n" +
      "      }).catch(function() {});\n" +
      "    }\n" +
      "  } catch(e) {}\n" +
      "  try {\n" +
      "    if (window.localStorage) {\n" +
      "      var toRemove = [];\n" +
      "      for (var i = 0; i < window.localStorage.length; i++) {\n" +
      "        var k = window.localStorage.key(i);\n" +
      "        if (k && k.indexOf('account') === -1) {\n" +
      "          toRemove.push(k);\n" +
      "        }\n" +
      "      }\n" +
      "      for (var j = 0; j < toRemove.length; j++) {\n" +
      "        window.localStorage.removeItem(toRemove[j]);\n" +
      "      }\n" +
      "    }\n" +
      "  } catch(e) {}\n" +
      "  try { if (window.sessionStorage) window.sessionStorage.clear(); } catch(e) {}\n" +
      "  setTimeout(finishReload, 200);\n" +
      "}\n"
   )
   public static native void wipeDataAndReload();
}
