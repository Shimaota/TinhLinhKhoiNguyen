const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 8080;
const ROOT = path.join(__dirname, 'build', 'dist', 'js', 'webapp');

const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.wav': 'audio/wav',
  '.ogg': 'audio/ogg',
  '.mp3': 'audio/mpeg',
  '.wasm': 'application/wasm',
  '.atlas': 'text/plain; charset=UTF-8',
  '.skel': 'application/octet-stream',
  '.fnt': 'text/plain; charset=UTF-8',
  '.glsl': 'text/plain; charset=UTF-8'
};

const server = http.createServer((req, res) => {
  // Normalize and parse URL path
  const parsedUrl = new URL(req.url, `http://${req.headers.host}`);
  let reqPath = decodeURIComponent(parsedUrl.pathname);
  if (reqPath === '/' || reqPath === '') {
    reqPath = '/index.html';
  }

  // Handle CORS preflight
  if (req.method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': '*'
    });
    res.end();
    return;
  }

  // Performance reports from webapp/perf.js (open the game with ?perf) -> console + perf.log
  if (reqPath === '/api/perf' && req.method === 'POST') {
    let body = '';
    req.on('data', (chunk) => { if (body.length < 65536) body += chunk; });
    req.on('end', () => {
      const line = new Date().toISOString() + ' ' + req.socket.remoteAddress + ' ' + body;
      console.log('[perf] ' + line);
      fs.appendFile(path.join(__dirname, 'perf.log'), line + '\n', () => {});
      res.writeHead(204);
      res.end();
    });
    return;
  }

  const filePath = path.join(ROOT, reqPath);

  // Security: prevent path traversal outside ROOT
  if (!filePath.startsWith(ROOT)) {
    res.writeHead(403, { 'Content-Type': 'text/plain' });
    res.end('403 Forbidden');
    return;
  }

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      // Fallback: if not found, check if index.html
      const indexPath = path.join(ROOT, 'index.html');
      fs.readFile(indexPath, (err2, content) => {
        if (err2) {
          res.writeHead(404, { 'Content-Type': 'text/plain' });
          res.end('404 Not Found - Have you run gradle build?');
        } else {
          res.writeHead(200, { 'Content-Type': 'text/html; charset=UTF-8' });
          res.end(content);
        }
      });
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    // CORS and No-Cache headers for local testing
    const headers = {
      'Content-Type': contentType,
      'Access-Control-Allow-Origin': '*',
      'Cache-Control': 'no-cache, no-store, must-revalidate',
      'Pragma': 'no-cache',
      'Expires': '0'
    };

    res.writeHead(200, headers);
    const readStream = fs.createReadStream(filePath);
    readStream.pipe(res);
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`=======================================================`);
  console.log(`  Tinh Linh Khoi Nguyen - Web Client Server`);
  console.log(`  Dang phuc vu tai: http://localhost:${PORT}`);
  console.log(`  Thu muc phan phoi: ${ROOT}`);
  console.log(`=======================================================`);
});
