/**
 * Headless CMS API client for Astro integration.
 * Provides methods for page retrieval, navigation, search, and form submission.
 */

const API_BASE = 'http://cms.iway.sk/rest/headless/v1';

/**
 * Helper to create fetch options with cookies from Astro request.
 * When called server-side, extracts cookies from the HTTP request and forwards them.
 */
function createFetchOptions(request?: Request): RequestInit {
  const options: RequestInit = {};

  if (request) {
    const cookies = request.headers.get('cookie');
    if (cookies) {
      options.headers = { 'cookie': cookies };
    }
  }

  return options;
}

export interface PageResponse {
  docId: number;
  title: string;
  virtualPath: string;
  language: string;
  body: string;
  seo?: {
    metaTitle?: string;
    metaDescription?: string;
    metaKeywords?: string;
    canonicalUrl?: string;
    robots?: string;
  };
}

export interface NavigationItem {
  docId: number;
  title: string;
  virtualPath: string;
  language?: string;
  level: number;
  hasChildren: boolean;
  children?: NavigationItem[];
}

export interface SearchResults {
  items: Array<{
    docId: number;
    title: string;
    virtualPath: string;
    language?: string;
    perex?: string;
    snippet?: string;
  }>;
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FormResult {
  success: boolean;
  message: string;
  fieldErrors?: Array<{
    field: string;
    message: string;
  }>;
}

/**
 * Get a page by its virtual path with response headers (including Set-Cookie).
 * Pass the Astro request to forward cookies server-side.
 */
export async function getPageWithHeaders(
  path: string,
  lng?: string,
  request?: Request
): Promise<{ data: PageResponse; headers: Headers }> {
  const params = new URLSearchParams({ path });
  if (lng) params.set('lng', lng);

  const response = await fetch(
    `${API_BASE}/pages/by-path?${params}`,
    createFetchOptions(request)
  );
  if (!response.ok) {
    throw new Error(`Failed to fetch page: ${response.statusText}`);
  }

  return {
    data: await response.json(),
    headers: response.headers,
  };
}

/**
 * Get a page by its virtual path.
 * @deprecated Use getPageWithHeaders() in server-side code to handle cookies properly.
 */
export async function getPage(path: string, lng?: string): Promise<PageResponse> {
  const params = new URLSearchParams({ path });
  if (lng) params.set('lng', lng);

  const response = await fetch(`${API_BASE}/pages/by-path?${params}`);
  if (!response.ok) {
    throw new Error(`Failed to fetch page: ${response.statusText}`);
  }
  return response.json();
}

/**
 * Get the navigation tree with response headers (including Set-Cookie).
 * Pass the Astro request to forward cookies server-side.
 */
export async function getNavigationWithHeaders(
  request?: Request,
  rootPath?: string,
  rootGroupId?: string,
  depth?: number
): Promise<{ data: NavigationItem[]; headers: Headers }> {
  const params = new URLSearchParams();
  if (rootPath) params.set('rootPath', rootPath);
  if (rootGroupId) params.set('rootGroupId', rootGroupId);
  if (depth) params.set('depth', String(depth));

  const response = await fetch(
    `${API_BASE}/navigation?${params}`,
    createFetchOptions(request)
  );
  if (!response.ok) {
    return { data: [], headers: new Headers() };
  }

  return {
    data: await response.json(),
    headers: response.headers,
  };
}

/**
 * Get the navigation tree.
 * @deprecated Use getNavigationWithHeaders() in server-side code to handle cookies properly.
 */
export async function getNavigation(
  rootPath?: string,
  rootGroupId?: string,
  depth?: number
): Promise<NavigationItem[]> {
  const params = new URLSearchParams();
  if (rootPath) params.set('rootPath', rootPath);
  if (rootGroupId) params.set('rootGroupId', rootGroupId);
  if (depth) params.set('depth', String(depth));

  const response = await fetch(`${API_BASE}/navigation?${params}`);
  if (!response.ok) {
    return [];
  }
  return response.json();
}

/**
 * Search documents with response headers (including Set-Cookie).
 * Pass the Astro request to forward cookies server-side.
 */
export async function searchWithHeaders(
  query: string,
  request?: Request,
  page: number = 0,
  size: number = 20
): Promise<{ data: SearchResults; headers: Headers }> {
  const params = new URLSearchParams({ q: query, page: String(page), size: String(size) });

  const response = await fetch(
    `${API_BASE}/actions/search?${params}`,
    createFetchOptions(request)
  );
  if (!response.ok) {
    return {
      data: { items: [], page, size, totalElements: 0, totalPages: 0 },
      headers: new Headers(),
    };
  }

  return {
    data: await response.json(),
    headers: response.headers,
  };
}

/**
 * Search documents.
 * @param query  search query string
 * @param page   0-based page number (default 0)
 * @param size   results per page (default 20)
 * @deprecated Use searchWithHeaders() in server-side code to handle cookies properly.
 */
export async function search(
  query: string,
  page: number = 0,
  size: number = 20
): Promise<SearchResults> {
  const params = new URLSearchParams({ q: query, page: String(page), size: String(size) });

  const response = await fetch(`${API_BASE}/actions/search?${params}`);
  if (!response.ok) {
    return { items: [], page, size, totalElements: 0, totalPages: 0 };
  }
  return response.json();
}

/**
 * Submit a form with response headers (including Set-Cookie).
 * Pass the Astro request to forward cookies server-side.
 */
export async function submitFormWithHeaders(
  formId: string,
  fields: Record<string, string>,
  request?: Request
): Promise<{ data: FormResult; headers: Headers }> {
  const options = createFetchOptions(request);

  const response = await fetch(`${API_BASE}/actions/forms/submit`, {
    ...options,
    method: 'POST',
    headers: {
      ...options.headers,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ formId, fields }),
  });

  if (!response.ok) {
    return {
      data: { success: false, message: 'Form submission failed.' },
      headers: new Headers(),
    };
  }

  return {
    data: await response.json(),
    headers: response.headers,
  };
}

/**
 * Submit a form.
 * @deprecated Use submitFormWithHeaders() in server-side code to handle cookies properly.
 */
export async function submitForm(
  formId: string,
  fields: Record<string, string>
): Promise<FormResult> {
  const response = await fetch(`${API_BASE}/actions/forms/submit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ formId, fields }),
  });

  if (!response.ok) {
    return { success: false, message: 'Form submission failed.' };
  }
  return response.json();
}
