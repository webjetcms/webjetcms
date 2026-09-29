const assert = require('node:assert/strict');
const http = require('node:http');
const test = require('node:test');
const { createPreview } = require('../serve-rag.cjs');

test('injects preview configuration and preserves proxy authentication and errors', async t => {
  /**
   * Starts a test server on an available loopback port and registers cleanup with the current test.
   *
   * @param {import('node:http').Server} server - Server to start and close after the test.
   * @returns {Promise<string>} HTTP origin of the listening server.
   */
  async function listen(server) {
    t.after(() => {
      server.closeAllConnections();
      return new Promise(resolve => server.close(resolve));
    });
    await new Promise((resolve, reject) => server.once('error', reject).listen(0, '127.0.0.1', resolve));
    return `http://127.0.0.1:${server.address().port}`;
  }

  let searchRequest;
  let docsCookie;
  const backendServer = http.createServer((req, res) => {
    searchRequest = { url: req.url, cookie: req.headers.cookie };
    res.writeHead(req.headers.cookie ? 200 : 403, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({ answer: 'AI answer' }));
  });
  const backend = await listen(backendServer);
  const docs = await listen(http.createServer((req, res) => {
    docsCookie = req.headers.cookie;
    res.writeHead(200, { 'Content-Type': 'text/html' });
    res.end('<script>window.$docsify = {};</script>');
  }));
  const url = await listen(createPreview({ backend: backend + '/cms', docs }));
  const headers = { Cookie: 'JSESSIONID=autotest' };

  const shell = await fetch(url, { headers });
  assert.match(await shell.text(), /markdownSearch: \{"endpoint":"\/cms\/rest\/rag\/markdown\/search"\}/);
  assert.equal(docsCookie, undefined);

  const query = '/cms/rest/rag/markdown/search?query=upload&language=sk';
  for (const scope of ['', '&directory=%2Fsk%2Fadmin%2F']) {
    const response = await fetch(url + query + scope, { headers });
    assert.equal(response.status, 200);
    assert.equal((await response.json()).answer, 'AI answer');
    assert.deepEqual(searchRequest, { url: query + scope, cookie: headers.Cookie });
  }
  assert.equal((await fetch(url + query)).status, 403);
  assert.equal((await fetch(url + query, { headers: { Origin: 'https://unrelated.example' } })).status, 403);
  assert.equal((await fetch(url + query, { method: 'POST' })).status, 405);
  backendServer.closeAllConnections();
  await new Promise(resolve => backendServer.close(resolve));
  assert.equal((await fetch(url + query)).status, 503);
});
