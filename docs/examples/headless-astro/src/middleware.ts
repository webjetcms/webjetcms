export const onRequest = async (context: any, next: any) => {
  const path = context.url.pathname;
  // Skip known static routes
  if (path === '/' || path.startsWith('/search') || path.startsWith('/preview')) {
    return next();
  }
  // Redirect all other paths to index with path as query param
  return context.redirect(`/?path=${encodeURIComponent(path)}`);
};
