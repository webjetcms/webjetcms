# Headless CMS POC

## Overview

WebJET CMS Headless API provides a RESTful interface for consuming page content, navigation, and form submissions without requiring server-side rendering. This POC implementation exposes existing WebJET CMS content through a modern API while reusing the existing rendering pipeline for dynamic components.

## API Base URL

```
/rest/headless/v1
```

## Endpoints

### Page Retrieval

#### GET /rest/headless/v1/pages/by-path

Retrieves a page by its virtual path with content negotiation support.

**Query Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `path` | Yes | Virtual path/slug (e.g., `/about`, `/contact`) |
| `lng` | No | Language override (e.g., `en`, `sk`) |
| `preview` | No | Preview mode - requires admin session (`true`/`false`) |

**Content Negotiation:**

- `Accept: application/json` → Returns structured JSON (default)
- `Accept: text/html` → Returns server-rendered HTML body
- Missing/unsupported Accept → Defaults to JSON

**Response (JSON):**

```json
{
  "docId": 42,
  "title": "About Us",
  "virtualPath": "/about",
  "language": "en",
  "body": "<h1>About Us</h1><p>Content...</p>",
  "seo": {
    "metaTitle": "About Us | Company",
    "metaDescription": "Company about page",
    "metaKeywords": "company, about",
    "canonicalUrl": "https://example.com/about",
    "robots": "index, follow"
  }
}
```

**Response (HTML):**

Returns the server-rendered HTML body directly.

**Error Responses:**

| Status | Description |
|--------|-------------|
| 400 | Path parameter is required |
| 403 | Preview requested without admin session |
| 404 | Page not found |

**Example:**

```bash
# JSON response (default)
curl -H "Accept: application/json" \
  "https://example.com/rest/headless/v1/pages/by-path?path=/about"

# HTML response
curl -H "Accept: text/html" \
  "https://example.com/rest/headless/v1/pages/by-path?path=/about"

# Preview mode (requires admin session)
curl -H "Accept: application/json" \
  "https://example.com/rest/headless/v1/pages/by-path?path=/about&preview=true" \
  -b "JSESSIONID=abc123"
```

### Navigation

#### GET /rest/headless/v1/navigation

Retrieves the navigation tree starting from a root group or path.

**Query Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `rootPath` | Alt | Virtual path of the root group (e.g., `/`) |
| `rootGroupId` | Alt | Group ID to start from |
| `depth` | No | Maximum depth (default: 0 = unlimited) |
| `lng` | No | Language override |

**Response:**

```json
[
  {
    "docId": 1,
    "title": "Home",
    "virtualPath": "/",
    "language": "en",
    "level": 0,
    "hasChildren": true,
    "children": [
      {
        "docId": 5,
        "title": "About",
        "virtualPath": "/about",
        "level": 1,
        "hasChildren": false,
        "children": null
      }
    ]
  }
]
```

**Example:**

```bash
curl "https://example.com/rest/headless/v1/navigation?rootPath=/&depth=2"
```

### Preview (Admin Session Required)

#### GET /rest/headless/v1/preview/pages/by-id

Retrieves a page by document ID for preview (unpublished/draft content). Requires admin session.

**Query Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `docId` | Yes | Document ID |
| `lng` | No | Language override |

**Response:**

Same JSON structure as `/pages/by-path` endpoint.

**Error Responses:**

| Status | Description |
|--------|-------------|
| 403 | Admin session required |
| 404 | Document not found |

**Example:**

```bash
curl -H "Accept: application/json" \
  "https://example.com/rest/headless/v1/preview/pages/by-id?docId=42" \
  -b "JSESSIONID=admin_session_id"
```

### Forms Submission

#### POST /rest/headless/v1/actions/forms/submit

Submits a form through the headless API.

**Request Body:**

```json
{
  "formId": "contact-form",
  "formName": "Contact",
  "componentKey": "inquiry",
  "fields": {
    "name": "John Doe",
    "email": "john@example.com",
    "message": "Hello World"
  },
  "pagePath": "/contact",
  "locale": "en"
}
```

**Response:**

```json
{
  "success": true,
  "message": "Form submitted successfully."
}
```

**Validation Error Response:**

```json
{
  "success": false,
  "message": "Validation failed.",
  "fieldErrors": [
    {
      "field": "email",
      "message": "Invalid email address"
    }
  ]
}
```

**Example:**

```bash
curl -X POST \
  "https://example.com/rest/headless/v1/actions/forms/submit" \
  -H "Content-Type: application/json" \
  -d '{
    "formId": "contact-form",
    "fields": {
      "name": "John Doe",
      "email": "john@example.com"
    }
  }'
```

### Search

#### GET /rest/headless/v1/actions/search

Searches documents with pagination support.

**Query Parameters:**

| Parameter | Required | Description |
|-----------|----------|-------------|
| `q` | Yes | Search query |
| `page` | No | Page number (0-based, default: 0) |
| `size` | No | Page size (default: 20, max: 100) |
| `scope` | No | Scope filter |
| `lng` | No | Language filter |

**Response:**

```json
{
  "items": [
    {
      "docId": 42,
      "title": "About Us",
      "virtualPath": "/about",
      "language": "en",
      "perex": "perex-image.jpg",
      "snippet": "...This is a test document about search results..."
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

**Example:**

```bash
curl "https://example.com/rest/headless/v1/actions/search?q=about&page=0&size=10"
```

## Security

- **Public endpoints**: Page retrieval, navigation, forms submission, and search are publicly accessible with existing IP whitelist policy.
- **Preview endpoints**: Require valid admin session (cookie-based authentication).
- **Existing security model**: All existing WebJET CMS security (IP whitelist, admin session) is reused.

## Standards

- **OpenAPI 3.0**: Endpoints are documented via Swagger UI at `/swagger-ui/`.
- **Content negotiation**: HTTP `Accept` header controls response format (JSON/HTML).
- **REST naming**: Versioned namespace `/rest/headless/v1` prevents collisions with legacy endpoints.

## Astro Integration Example

```typescript
// src/lib/api.ts
const API_BASE = '/rest/headless/v1';

export async function getPage(path: string): Promise<any> {
  const response = await fetch(`${API_BASE}/pages/by-path?path=${path}`);
  return response.json();
}

export async function search(query: string, page: number = 0): Promise<any> {
  const response = await fetch(
    `${API_BASE}/actions/search?q=${query}&page=${page}`
  );
  return response.json();
}

export async function submitForm(formData: Record<string, string>): Promise<any> {
  const response = await fetch(`${API_BASE}/actions/forms/submit`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ formId: 'contact', fields: formData }),
  });
  return response.json();
}
```
