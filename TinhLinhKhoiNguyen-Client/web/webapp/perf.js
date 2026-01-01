// Real-device performance measurement: open the page with ?perf -> every 5 s frame stats are sent to server.js (/api/perf).
// script = the game's JS time per frame (logic + issuing draw calls), frame = interval between 2 frames.
(function () {
  if (!/[?&]perf\b/.test(location.search)) return;

  var frames = [], scripts = [], long = [], gl = { draw: 0, data: 0, sub: 0 };
  var lastFrame = 0, sessionStart = performance.now();

  var rawRaf = window.requestAnimationFrame.bind(window);
  window.requestAnimationFrame = function (cb) {
    return rawRaf(function (now) {
      if (lastFrame) {
        var dt = now - lastFrame;
        frames.push(dt);
        if (dt > 50) long.push(Math.round((now - sessionStart) / 100) / 10 + 's:' + Math.round(dt));
      }
      lastFrame = now;
      var t0 = performance.now();
      cb(now);
      scripts.push(performance.now() - t0);
    });
  };

  function hookGl(proto) {
    ['drawElements', 'drawArrays'].forEach(function (n) {
      var o = proto[n];
      proto[n] = function () { gl.draw++; return o.apply(this, arguments); };
    });
    [['bufferData', 'data'], ['bufferSubData', 'sub']].forEach(function (p) {
      var o = proto[p[0]];
      proto[p[0]] = function () { gl[p[1]]++; return o.apply(this, arguments); };
    });
  }
  if (window.WebGL2RenderingContext) hookGl(WebGL2RenderingContext.prototype);
  if (window.WebGLRenderingContext) hookGl(WebGLRenderingContext.prototype);

  function pct(a, p) {
    if (!a.length) return 0;
    var s = a.slice().sort(function (x, y) { return x - y; });
    return Math.round(s[Math.min(s.length - 1, Math.floor(s.length * p))] * 10) / 10;
  }

  function info() {
    var c = document.getElementById('canvas'), r = '';
    try {
      var ctx = c.getContext('webgl2') || c.getContext('webgl');
      var ext = ctx.getExtension('WEBGL_debug_renderer_info');
      r = ext ? ctx.getParameter(ext.UNMASKED_RENDERER_WEBGL) : ctx.getParameter(ctx.RENDERER);
    } catch (e) {}
    return { ua: navigator.userAgent, dpr: window.devicePixelRatio, canvas: c.width + 'x' + c.height, css: c.clientWidth + 'x' + c.clientHeight, gpu: r };
  }

  var sentInfo = false;
  setInterval(function () {
    var n = frames.length;
    if (!n) return;
    var sum = frames.reduce(function (a, b) { return a + b; }, 0);
    var report = {
      t: Math.round((performance.now() - sessionStart) / 1000),
      fps: Math.round(1000 * n / sum * 10) / 10,
      frame: { p50: pct(frames, 0.5), p95: pct(frames, 0.95), max: pct(frames, 1) },
      script: { p50: pct(scripts, 0.5), p95: pct(scripts, 0.95), max: pct(scripts, 1) },
      drawPerFrame: Math.round(gl.draw / n), bufferDataPerFrame: Math.round(gl.data / n), bufferSubDataPerFrame: Math.round(gl.sub / n),
      glfix: !/[?&]glfix=0\b/.test(location.search), dpr: window.devicePixelRatio,
      long: long.slice(0, 20)
    };
    if (!sentInfo) { report.info = info(); sentInfo = true; }
    frames = []; scripts = []; long = []; gl.draw = 0; gl.data = 0; gl.sub = 0;
    fetch('/api/perf', { method: 'POST', body: JSON.stringify(report) }).catch(function () {});
  }, 5000);
})();
