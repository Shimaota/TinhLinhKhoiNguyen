// Safari (iOS) / Metal: writing to a buffer the GPU is still using (an earlier draw in the same frame) makes Safari wait for the GPU or
// copy the whole buffer, ~1 ms per draw call. libGDX (PolygonSpriteBatch) re-uploads the same buffer for every draw call ->
// iPhone 15 Pro Max gets only 6-8 FPS in game. Measured with bench.html on Safari iOS (150 draws/frame):
//   bufferData per draw 6 FPS | bufferSubData overwrite 8 FPS | appending into one large buffer 6-15 FPS | one buffer per draw 35 FPS
// (Safari tracks "in use" per whole buffer, not per range, so only separate buffers avoid the wait.)
//
// Approach: every dynamic buffer (not STATIC_DRAW) the game creates becomes a "logical" buffer backed by a ring of real buffers. Every time
// the game re-uploads data from the start (bufferData, or bufferSubData at offset 0) we switch to the next real buffer the GPU has finished
// with (≥ 3 frames ago, creating more if needed), then re-point the VAO's vertex attributes / index buffer to it.
// Disable for comparison: ?glfix=0
(function () {
  if (/[?&]glfix=0\b/.test(location.search) || !window.WebGL2RenderingContext) return;

  var ARRAY_BUFFER = 0x8892, ELEMENT_ARRAY_BUFFER = 0x8893, STATIC_DRAW = 0x88E4, DYNAMIC_DRAW = 0x88E8;
  var FRAMES_IN_FLIGHT = 3;
  var P = WebGL2RenderingContext.prototype;
  var o = {};
  ['bindBuffer', 'bufferData', 'bufferSubData', 'vertexAttribPointer', 'bindVertexArray', 'deleteBuffer',
    'deleteVertexArray', 'createBuffer'].forEach(function (n) { o[n] = P[n]; });

  var frame = 0;
  (function tick() { frame++; requestAnimationFrame(tick); })();

  // Logical buffer -> { list: [{buf, size, used}], cur, usage }
  var rings = new WeakMap();
  // Logical buffers ever allocated with a usage other than STATIC_DRAW
  var dynamic = new WeakSet();
  // Per context: bound VAO, logical buffer at ARRAY_BUFFER, per-VAO state (vertex attributes + logical index buffer)
  var states = new WeakMap();

  function state(gl) {
    var s = states.get(gl);
    if (!s) {
      s = { vao: null, array: null, vaos: new Map() };
      states.set(gl, s);
    }
    return s;
  }
  function vaoState(s) {
    var v = s.vaos.get(s.vao);
    if (!v) { v = { attribs: {}, element: null }; s.vaos.set(s.vao, v); }
    return v;
  }
  function physical(logical) {
    var r = logical && rings.get(logical);
    return r ? r.list[r.cur].buf : logical;
  }
  function boundLogical(gl, target) {
    var s = state(gl);
    return target === ARRAY_BUFFER ? s.array : target === ELEMENT_ARRAY_BUFFER ? vaoState(s).element : null;
  }

  // Re-point the current VAO's vertex attributes that use this logical buffer to the real buffer bound at ARRAY_BUFFER
  function repointAttribs(gl, logical) {
    var attribs = vaoState(state(gl)).attribs;
    for (var i in attribs) {
      var a = attribs[i];
      if (a.buffer === logical) o.vertexAttribPointer.call(gl, +i, a.size, a.type, a.normalized, a.stride, a.offset);
    }
  }

  // Switch the logical buffer to the next free real buffer with room for `bytes`; bind it (and re-point the VAO).
  function rotate(gl, target, logical, bytes) {
    var r = rings.get(logical);
    if (!r) {
      r = { list: [{ buf: logical, size: 0, used: -FRAMES_IN_FLIGHT }], cur: 0, usage: DYNAMIC_DRAW };
      rings.set(logical, r);
    }
    r.list[r.cur].used = frame;
    var next = (r.cur + 1) % r.list.length;
    if (next === r.cur || frame - r.list[next].used < FRAMES_IN_FLIGHT) {
      // The GPU may still be using the next buffer: insert a new buffer into the ring
      next = r.cur + 1;
      r.list.splice(next, 0, { buf: o.createBuffer.call(gl), size: 0, used: -FRAMES_IN_FLIGHT });
    }
    r.cur = next;
    var entry = r.list[next];
    entry.used = frame;
    o.bindBuffer.call(gl, target, entry.buf);
    if (entry.size < bytes) {
      entry.size = Math.max(bytes, entry.size * 2, 1024);
      o.bufferData.call(gl, target, entry.size, r.usage);
    }
    if (target === ARRAY_BUFFER) repointAttribs(gl, logical);
  }

  function upload(gl, target, logical, data, srcOffset, count) {
    rotate(gl, target, logical, count * (data.BYTES_PER_ELEMENT || 1));
    o.bufferSubData.call(gl, target, 0, data, srcOffset, count);
  }

  // Delete the extra real buffers of a logical buffer (keeps the logical buffer)
  function release(gl, logical) {
    var r = rings.get(logical);
    if (!r) return;
    for (var i = 0; i < r.list.length; i++) if (r.list[i].buf !== logical) o.deleteBuffer.call(gl, r.list[i].buf);
    rings.delete(logical);
  }

  P.bindBuffer = function (target, buffer) {
    var s = state(this);
    if (target === ARRAY_BUFFER) s.array = buffer;
    else if (target === ELEMENT_ARRAY_BUFFER) vaoState(s).element = buffer;
    return o.bindBuffer.call(this, target, physical(buffer));
  };

  P.bindVertexArray = function (vao) {
    state(this).vao = vao;
    return o.bindVertexArray.call(this, vao);
  };

  P.vertexAttribPointer = function (index, size, type, normalized, stride, offset) {
    var s = state(this);
    vaoState(s).attribs[index] = { size: size, type: type, normalized: normalized, stride: stride, offset: offset, buffer: s.array };
    return o.vertexAttribPointer.apply(this, arguments);
  };

  P.deleteBuffer = function (buffer) {
    if (buffer) release(this, buffer);
    return o.deleteBuffer.call(this, buffer);
  };

  P.deleteVertexArray = function (vao) {
    state(this).vaos.delete(vao);
    return o.deleteVertexArray.call(this, vao);
  };

  P.bufferData = function (target, data, usage, srcOffset, length) {
    var logical = boundLogical(this, target);
    if (!logical) return o.bufferData.apply(this, arguments);
    var r = rings.get(logical);
    if (usage === STATIC_DRAW) {
      // Became a static buffer: drop the ring, reuse the original buffer
      if (r) {
        release(this, logical);
        o.bindBuffer.call(this, target, logical);
        if (target === ARRAY_BUFFER) repointAttribs(this, logical);
      }
      dynamic.delete(logical);
      return o.bufferData.apply(this, arguments);
    }
    dynamic.add(logical);
    if (r) r.usage = usage;
    if (typeof data === 'number' || !data || !ArrayBuffer.isView(data)) {
      // Allocation only (libGDX creating a Mesh): a real buffer with enough room is allocated when data is uploaded
      return r ? undefined : o.bufferData.apply(this, arguments);
    }
    var offset = srcOffset | 0;
    upload(this, target, logical, data, offset, arguments.length > 4 && length ? length : data.length - offset);
  };

  P.bufferSubData = function (target, dstOffset, data, srcOffset, length) {
    var logical = boundLogical(this, target);
    // Only switch buffers when a dynamic buffer is rewritten from the start; writes in the middle go to the current real buffer
    if (!logical || dstOffset !== 0 || !dynamic.has(logical) || !data || !ArrayBuffer.isView(data)) {
      return o.bufferSubData.apply(this, arguments);
    }
    var offset = srcOffset | 0;
    upload(this, target, logical, data, offset, arguments.length > 4 && length ? length : data.length - offset);
  };
})();
