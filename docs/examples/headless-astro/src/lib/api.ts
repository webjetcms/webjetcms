/**
 * Headless CMS API client for Astro integration.
 * Provides methods for page retrieval, navigation, search, and form submission.
 */

const API_BASE = 'http://iwcm.interway.sk/rest/headless/v1';

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
 * Get a page by its virtual path.
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
 * Get the navigation tree.
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
 * Search documents.
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
 * Submit a form.
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
