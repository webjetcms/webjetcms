import { defineMiddleware } from 'astro:middleware';

/**
 * Headless CMS routing middleware.
 *
 * WHY THIS EXISTS
 * ---------------
 * CMS content lives at arbitrary top-level paths (/o-nas/, /apps/galeria/, ...).
 * A naive implementation uses a root catch-all route (src/pages/[...slug].astro),
 * but that route also matches the URLs of dedicated pages (/, /news, /search, ...).
 *
 * Astro's dev router (astro/dist/core/routing/dev.js -> matchRoute) collects ALL
 * routes that match a URL and tries them in priority order. If a higher-priority
 * page (e.g. index.astro) fails to COMPILE, Astro records the error but falls
 * through to the next matching route. A root catch-all compiles fine, so it gets
 * served and the real compiler error is silently discarded. That is why a syntax
 * error in index.astro used to render [...slug].astro instead of showing the error.
 *
 * THE FIX
 * -------
 * The CMS catch-all is placed under /cms/ (src/pages/cms/[...slug].astro) so it can
 * never shadow a dedicated page. When a dedicated page is broken it has NO fallback
 * route, so Astro throws the real compiler error in the browser.
 *
 * This middleware forwards genuine CMS requests: if a request does not match any
 * dedicated page (Astro returns 404), it is rewritten to the CMS renderer, passing
 * the original pathname and query separately to the CMS renderer.
 */
export const onRequest = defineMiddleware(async (context, next) => {
  const { pathname, search } = context.url;

  // Never re-process the internal CMS route (avoids rewrite loops).
  if (pathname.startsWith('/cms/')) {
    return next();
  }

  const response = await next();

  // Only take over genuine "page not found" responses. Real errors (500) from a
  // broken dedicated page propagate untouched, so the developer sees them.
  if (response.status !== 404) {
    return response;
  }

  // Only treat page-like URLs as CMS content (ending with "/" or ".html", or root).
  const isPageLikePath =
    pathname === '/' || pathname.endsWith('/') || pathname.endsWith('.html');
  if (!isPageLikePath) {
    return response;
  }

  return context.rewrite(
    `/cms/render?path=${encodeURIComponent(pathname)}&query=${encodeURIComponent(search)}`
  );
});
