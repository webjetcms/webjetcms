const http = require('node:http');
const https = require('node:https');
const { parseArgs } = require('node:util');

/**
 * Proxies the local Docsify preview and its search endpoint without changing backend authentication.
 * Sets the preview root and search configuration in HTML and forwards cookies only to the backend search endpoint.
 * Accepts GET and HEAD requests on any hostname and rejects cross-origin requests.
 *
 * @param {Object} options - Local preview configuration.
 * @param {string} options.backend - HTTP or HTTPS backend URL, optionally including its application context path.
 * @param {string} [options.docs="http://127.0.0.1:3000"] - HTTP Docsify development server URL.
 * @returns {import('node:http').Server} Configured server; the caller must start listening.
 * @throws {Error} If a server uses an unsupported protocol or either URL contains credentials.
 */
function createPreview({ backend, docs = 'http://127.0.0.1:3000' }) {
  const webjet = new URL(backend);
  const preview = new URL(docs);
  if (!['http:', 'https:'].includes(webjet.protocol) || preview.protocol !== 'http:') {
    throw new Error('The backend requires HTTP or HTTPS; the Docsify development server requires HTTP.');
  }
  if (webjet.username || webjet.password || preview.username || preview.password) {
    throw new Error('Provide server URLs without credentials.');
  }
  const searchPath = webjet.pathname.replace(/\/$/, '') + '/rest/rag/markdown/search';
  const config = JSON.stringify({ endpoint: searchPath }).replace(/</g, '\\u003c');
  return http.createServer((req, res) => {
    const origin = `http://${req.headers.host}`;
    let url;
    try {
      url = new URL(req.url, origin);
    } catch {
      res.writeHead(400);
      return res.end();
    }
    if ((req.headers.origin && req.headers.origin !== origin)
        || req.headers['sec-fetch-site'] === 'cross-site') {
      res.writeHead(403);
      return res.end();
    }
    if (req.method !== 'GET' && req.method !== 'HEAD') {
      res.writeHead(405, { Allow: 'GET, HEAD' });
      return res.end();
    }
    const search = url.pathname === searchPath;
    const target = search ? webjet : preview;
    const headers = { host: target.host, accept: req.headers.accept || '*/*', 'accept-encoding': 'identity' };
    for (const name of ['user-agent', 'referer']) {
      if (req.headers[name]) headers[name] = req.headers[name];
    }
    if (search && req.headers.cookie) headers.cookie = req.headers.cookie;
    const transport = target.protocol === 'https:' ? https : http;
    const upstream = transport.request({
      hostname: target.hostname, port: target.port || (target.protocol === 'https:' ? 443 : 80), method: req.method,
      path: (search ? '' : target.pathname.replace(/\/$/, '')) + url.pathname + url.search,
      headers
    }, response => {
      response.on('error', () => res.destroy());
      const responseHeaders = { ...response.headers, 'cache-control': 'no-store' };
      const html = !search && (response.headers['content-type'] || '').startsWith('text/html');
      if (!html || req.method === 'HEAD') {
        res.writeHead(response.statusCode, responseHeaders);
        response.pipe(res);
        return;
      }
      delete responseHeaders['content-length'];
      delete responseHeaders.etag;
      let body = '';
      response.setEncoding('utf8');
      response.on('data', chunk => { body += chunk; });
      response.on('end', () => {
        res.writeHead(response.statusCode, responseHeaders);
        res.end(body.replace('window.$docsify = {', `basePath = '/'; window.$docsify = { markdownSearch: ${config},`));
      });
    });
    upstream.setTimeout(120000, () => upstream.destroy(new Error('Upstream request timed out')));
    upstream.on('error', () => {
      if (res.headersSent) return res.destroy();
      res.writeHead(503, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
      res.end(JSON.stringify({ error: 'The local documentation or WebJET server is unavailable.' }));
    });
    res.on('close', () => { if (!res.writableEnded) upstream.destroy(); });
    upstream.end();
  });
}

if (require.main === module) {
  const { values } = parseArgs({ options: {
    backend: { type: 'string', default: 'https://docs.webjetcms.sk' },
    docs: { type: 'string' }, port: { type: 'string' }
  } });
  const server = createPreview(values);
  server.on('error', error => { console.error(error.message); process.exitCode = 1; });
  server.listen(Number(values.port || 3001), '127.0.0.1', () => {
    const backend = new URL(values.backend);
    console.log(`Preview: http://127.0.0.1:${server.address().port}/sk/`);
    if (backend.protocol === 'http:') {
      console.log(`For a backend requiring login, use http://${backend.hostname}:${server.address().port}/sk/ with the backend hostname resolving to loopback and sign in at ${backend.href} in the same browser.`);
    }
    console.log('The loopback preview does not share production login cookies; the backend must allow anonymous search.');
  });
}

module.exports = { createPreview };
