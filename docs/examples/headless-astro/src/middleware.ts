const API_BASE = 'http://iwcm.interway.sk/rest/headless/v1';

export const onRequest = async (context: any, next: any) => {
  const path = context.url.pathname;
  
  // Skip known static routes (search, preview, index)
  if (path === '/' || path.startsWith('/search') || path.startsWith('/preview')) {
    return next();
  }

  // Fetch page from headless API
  let page: any = null;
  let nav: any[] = [];
  let error = '';

  try {
    const pageResp = await fetch(`${API_BASE}/pages/by-path?path=${encodeURIComponent(path)}`);
    if (pageResp.ok) {
      page = await pageResp.json();
    } else {
      error = `Page not found: ${pageResp.statusText}`;
    }
  } catch (e: any) {
    error = e.message || 'Failed to fetch page';
  }

  // Fetch navigation scoped to current path
  try {
    const navParams = new URLSearchParams();
    navParams.set('rootPath', path);
    navParams.set('depth', '2');
    const navResp = await fetch(`${API_BASE}/navigation?${navParams}`);
    if (navResp.ok) {
      nav = await navResp.json();
    }
  } catch {
    // Navigation failure is non-fatal
  }

  // Build HTML response
  const navHtml = nav.map((item: any) => 
    `<a href="${item.virtualPath}">${item.title}</a>`
  ).join('\n');
  
  const childrenHtml = nav.flatMap((item: any) => item.children || []).map((child: any) => 
    `<a href="${child.virtualPath}">  ${child.title}</a>`
  ).join('\n');

  const contentHtml = error 
    ? `<div class="error"><strong>Error:</strong> ${error}</div>`
    : page 
      ? `<div class="content"><h1>${page.title}</h1><p>Path: ${page.virtualPath} | Language: ${page.language}</p><div innerHTML:raw="${page.body}"></div></div>`
      : `<p>Enter a path: <code>/:path</code></p>`;

  const html = `<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>${page?.title || 'Headless CMS Demo'}</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 900px; margin: 0 auto; padding: 20px; }
    nav { background: #f5f5f5; padding: 15px; border-radius: 8px; margin-bottom: 20px; }
    nav a { display: block; padding: 5px 0; color: #0066cc; text-decoration: none; }
    nav a:hover { text-decoration: underline; }
    .content { background: #fafafa; padding: 20px; border-radius: 8px; }
    .error { color: #d32f2f; background: #ffebee; padding: 15px; border-radius: 8px; }
    h1 { color: #333; }
  </style>
</head>
<body>
  <h1>WebJET CMS Headless Demo</h1>
  <nav><h3>Navigation</h3>${navHtml}${childrenHtml}</nav>
  ${contentHtml}
</body>
</html>`;

  return new Response(html, {
    status: error ? 404 : 200,
    headers: { 'Content-Type': 'text/html; charset=utf-8' },
  });
};
