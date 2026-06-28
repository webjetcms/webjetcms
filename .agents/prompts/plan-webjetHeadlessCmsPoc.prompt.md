## Plan: WebJET Headless CMS POC Final Specification (v1)

This document defines an implementation-ready POC specification for exposing WebJET CMS in headless mode, while reusing existing rendering internals and keeping backward compatibility with current page delivery.

## 1. Objective

Deliver a first functional headless interface for WebJET CMS that:
- exposes rendered page content through REST,
- supports modern frontend consumption,
- keeps dynamic WebJET apps server-rendered for compatibility,
- provides limited write actions needed for realistic POC integration,
- is safe to implement incrementally without breaking current ShowDoc/JSP/Thymeleaf behavior.

## 2. Scope

Included in POC:
- Page retrieval by URL path/slug.
- Navigation/list endpoint for frontend routing support.
- SEO metadata in response payload.
- Server-side rendering of dynamic applications included in page content.
- Preview access for unpublished/draft content via admin session.
- Accept-header content negotiation (JSON or HTML response).
- Action endpoints for:
  - forms submit,
  - search query.
- OpenAPI documentation exposure for new endpoints.
- Astro demonstration integration specification.

Out of scope in POC:
- Signed preview tokens for external preview links.
- Full generic action framework for every existing WebJET app.
- Component-level structured JSON model replacing HTML.
- GraphQL layer.
- Public write operations beyond forms/search.

## 3. Standards and Compatibility

Standards:
- OpenAPI 3.0 for endpoint discoverability and contract publication.
- HTTP content negotiation via Accept header.
- REST resource naming with explicit versioning.

Compatibility principles:
- Existing ShowDoc behavior remains default for non-headless requests.
- Existing public endpoint /rest/documents remains unchanged.
- Existing component rendering pipeline is reused (not duplicated).
- Existing security model (IP whitelist/admin session) is reused in POC.

## 4. API Namespace and Versioning

Recommended namespace:
- /rest/headless/v1

Rationale:
- Prevents collisions with legacy /rest/documents.
- Enables additive evolution for V2 without breaking consumers.
- Makes roadmap task isolation and rollout safer.

## 5. Content Negotiation Contract

Behavior by Accept header:
- Accept: application/json
  - Returns structured JSON page payload including rendered HTML body.
- Accept: text/html
  - Returns server-rendered HTML body only.
- Missing/unsupported Accept
  - Default to application/json for API ergonomics in headless consumers.
  - Optionally return 406 for strict mode (deferred to V2; POC uses JSON fallback).

Response headers:
- Content-Type according to selected representation.
- Optional cache hints (if available from existing logic).

## 6. Endpoint Specification (POC)

### 6.1 Get Page by Path

Endpoint:
- GET /rest/headless/v1/pages/by-path

Query parameters:
- path (required): normalized virtual path/slug.
- lng (optional): language override when applicable.
- preview (optional): ignored unless admin session is valid.

Security:
- Public read with existing IP whitelist policy.
- If preview=true then admin session required; otherwise return 403.

Responses:
- 200 application/json: full PageResponse object.
- 200 text/html: rendered HTML body.
- 404 if document not found or unavailable.
- 401 if IP not allowed.
- 403 if preview requested without admin session.

### 6.2 Get Navigation/List

Endpoint:
- GET /rest/headless/v1/navigation

Query parameters:
- rootPath or rootGroupId (one required).
- depth (optional, default safe value).
- lng (optional).

Security:
- Public read with existing IP whitelist policy.

Responses:
- 200 application/json: navigation tree/list payload.
- 401/404 as applicable.

### 6.3 Preview Page (Admin Session)

Endpoint:
- GET /rest/headless/v1/preview/pages/by-id

Query parameters:
- docId (required).

Security:
- Admin session only.

Responses:
- 200 application/json or text/html.
- 403 if no admin session.
- 404 if doc does not exist.

### 6.4 Forms Submit Action

Endpoint:
- POST /rest/headless/v1/actions/forms/submit

Request body:
- Form identification (form id/name/component key).
- Input fields payload.
- Optional context (page path, locale, anti-spam fields as required by existing logic).

Security:
- Public (with existing anti-spam/validation rules).
- IP whitelist follows public REST policy unless explicitly exempted by product decision.

Responses:
- 200 success envelope with action result.
- 400 validation errors with field-level details.
- 422 semantic form processing errors (optional in POC; 400 acceptable fallback).

### 6.5 Search Action

Endpoint:
- GET /rest/headless/v1/actions/search

Query parameters:
- q (required).
- page, size (optional).
- scope filters (optional, if available from existing search services).

Security:
- Public read with IP whitelist policy.

Responses:
- 200 search result envelope.
- 400 for invalid query.

## 7. JSON Contracts

### 7.1 PageResponse

Top-level fields:
- version: API version string.
- generatedAt: ISO timestamp.
- request:
  - path,
  - locale,
  - preview flag,
  - representation.
- page:
  - docId,
  - groupId,
  - title,
  - url,
  - language,
  - published/available flags.
- seo:
  - title,
  - description,
  - canonicalUrl,
  - robots,
  - openGraph map.
- content:
  - html (server-rendered body after include/component processing),
  - headerHtml (optional),
  - footerHtml (optional),
  - additional template areas if enabled.
- actions:
  - forms endpoints metadata,
  - search endpoint metadata,
  - action capability flags.
- diagnostics (optional, disabled by default in production):
  - cache info,
  - render timings,
  - rendered component names.

### 7.2 NavigationResponse

Fields:
- root descriptor,
- items array,
- each item: title, path, docId/groupId, children.

### 7.3 ActionResponse

Fields:
- success boolean,
- message code/text,
- data object,
- fieldErrors array (for forms),
- redirect optional.

### 7.4 ErrorResponse

Fields:
- timestamp,
- status,
- error,
- message,
- path,
- requestId/correlationId (if available).

## 8. Dynamic Applications in Headless Mode

POC strategy:
- Keep server-side execution for dynamic apps.
- Reuse existing include processing pipeline.
- Return resulting HTML to frontend.
- Expose explicit action endpoints for interactions that need roundtrip.

Implications:
- Maximum compatibility with existing components.
- Faster POC delivery.
- Frontend remains decoupled while still consuming trusted rendered output.

Known limitation:
- Not all app interactions are normalized in V1; forms and search are first-class.

## 9. Rendering and Integration Architecture

Primary hook recommendation:
- Add headless response path near the end of ShowDoc execute flow before final template forward.

Reasoning:
- Request attributes are already populated.
- Component execution is already completed through current mechanisms.
- Existing cache/device/redirect behavior remains in effect.
- Minimal disruption to existing delivery path.

Key reuse points:
- ShowDoc request preparation and lifecycle.
- WebjetComponentParser for Spring/Thymeleaf component includes.
- WriteTag behavior assumptions for include-generated output.
- Existing security and OpenAPI infrastructure.

## 10. Security Model (POC)

Public read endpoints:
- Protected by existing IP whitelist pattern used in public REST controllers.

Preview endpoints:
- Admin session required.
- No signed preview token in V1.

Write actions in scope:
- Forms submit and search only.
- Validation and sanitization must reuse existing service rules.

CORS/CSRF notes:
- Follow current platform behavior and constraints.
- Document exact requirements per endpoint when implementation is started.

## 11. Caching, Device and Redirect Semantics

Caching:
- Preserve existing component/page caching logic where applicable.
- Expose cache behavior only as optional diagnostics in response.

Device filtering:
- Preserve existing device-based include logic.
- Response must reflect actual resolved device context.

Redirects:
- If component/page flow sets redirect semantics, map to HTTP redirect or explicit action response rule.
- In JSON mode, prefer explicit redirect field unless HTTP redirect is required by behavior contract.

## 12. Astro Demo Specification

Goal:
- Provide a practical demonstration that WebJET headless POC can drive a modern frontend.

Demo requirements:
- Route rendering from headless page endpoint.
- Navigation rendering from navigation endpoint.
- One preview workflow (admin session based).
- One form submit workflow.
- One search workflow.
- Basic SEO mapping from API response to Astro page metadata.

Suggested approach:
- Build Astro routes that call /rest/headless/v1 endpoints.
- Render server-side HTML content in page layout.
- Keep frontend logic thin; rely on API contracts from this document.

## 13. OpenAPI and Documentation Deliverables

Implementation documentation outputs:
- OpenAPI docs for all new headless endpoints.
- Developer guide page in English:
  - concepts,
  - endpoint catalog,
  - request/response examples,
  - auth rules,
  - preview behavior,
  - Astro quickstart.

Documentation placement:
- Extend existing Spring/public REST documentation sections.
- Add dedicated headless chapter under English docs.

## 14. Backward Compatibility Requirements

Mandatory compatibility checks:
- Non-headless requests continue to follow current ShowDoc forward behavior.
- Existing /rest/documents consumers remain unaffected.
- Existing components render unchanged in legacy mode.

Non-goals for POC:
- No forced migration of existing frontend templates.
- No breaking API changes in old endpoints.

## 15. Acceptance Criteria

Functional:
- Page by path endpoint works in JSON and HTML mode.
- Navigation endpoint returns valid tree/list.
- SEO metadata is present for rendered pages.
- Dynamic apps in page content are rendered server-side.
- Preview endpoint works only with admin session.
- Forms submit and search actions return valid responses.

Non-functional:
- New endpoints visible in OpenAPI docs.
- Legacy page delivery path is unaffected.
- Error handling is consistent and documented.

Demo:
- Astro demo successfully renders at least one real page from headless API.
- Astro demo includes preview, form submit, and search examples.

## 16. Implementation Plan for AI Agent (Execution-Oriented)

Phase A: Contracts
1. Introduce endpoint namespace and controller skeletons.
2. Define DTOs for PageResponse, NavigationResponse, ActionResponse, ErrorResponse.
3. Add OpenAPI annotations and verify docs generation.

Phase B: Read Flow
4. Implement page-by-path read flow using existing ShowDoc/render internals.
5. Implement content negotiation JSON/HTML.
6. Implement navigation/list endpoint.

Phase C: Preview
7. Implement preview endpoint with admin-session gate.

Phase D: Actions
8. Implement forms submit endpoint reusing existing form processing logic.
9. Implement search endpoint reusing existing search services.

Phase E: Validation
10. Add integration tests/manual test scripts for all acceptance criteria.
11. Verify backward compatibility with current non-headless requests.

Phase F: Demo and Docs
12. Produce Astro demo integration.
13. Publish English developer documentation for headless POC.

## 17. Risks and Mitigations

Risk: hidden coupling to JSP/template output details.
Mitigation: keep server-rendered HTML model in V1 and avoid aggressive restructuring.

Risk: inconsistent behavior between JSON and HTML modes.
Mitigation: single render pipeline, dual serialization only at response layer.

Risk: preview misuse.
Mitigation: strict admin-session check in POC and explicit audit logging where available.

Risk: action endpoint drift.
Mitigation: limit V1 actions to forms/search and document the boundary clearly.

## 18. Post-POC Roadmap (V2 Candidates)

- Signed preview tokens for external frontend preview links.
- Expanded action endpoint catalog for additional dynamic apps.
- Optional structured component tree alongside HTML.
- Optional CDN-friendly cache invalidation hooks.
- Optional frontend SDK/client generation from OpenAPI.

## Decisions (Locked for POC)

- Document language: English.
- Output negotiation: Accept header, JSON + HTML support.
- Public read security: IP whitelist pattern.
- Preview security: admin session only.
- Dynamic apps: server-rendered HTML + separate action endpoints.
- Included write actions: forms submit and search.
- Demo framework: Astro.

## 19. Implementation Mode Addendum

This section is optimized for direct execution by an implementation AI agent.

Rules for implementation mode:
- Preserve current non-headless behavior by default.
- Reuse existing services and render pipeline before introducing new abstractions.
- Keep endpoint contracts stable once examples below are implemented.
- Use additive changes only; avoid breaking or renaming existing APIs.
- Place all newly introduced headless implementation classes under base package `sk.iway.iwcm.headless`.
- Allowed subpackages for new code: `dto`, `rest`, `service`, `jpa` (if persistence is later needed).

## 20. Concrete API Examples (Normative for POC)

### 20.1 GET Page by Path (JSON)

Request:
```http
GET /rest/headless/v1/pages/by-path?path=/en/home/&lng=en HTTP/1.1
Accept: application/json
```

Response 200:
```json
{
  "version": "v1",
  "generatedAt": "2026-06-26T12:00:00Z",
  "request": {
    "path": "/en/home/",
    "locale": "en",
    "preview": false,
    "representation": "application/json"
  },
  "page": {
    "docId": 12345,
    "groupId": 210,
    "title": "Home",
    "url": "/en/home/",
    "language": "en",
    "published": true,
    "available": true
  },
  "seo": {
    "title": "Home | Example",
    "description": "Example homepage",
    "canonicalUrl": "https://example.com/en/home/",
    "robots": "index,follow",
    "openGraph": {
      "og:title": "Home",
      "og:type": "website",
      "og:url": "https://example.com/en/home/"
    }
  },
  "content": {
    "html": "<main><h1>Home</h1><div class=\"app\">...</div></main>",
    "headerHtml": "<header>...</header>",
    "footerHtml": "<footer>...</footer>"
  },
  "actions": {
    "forms": {
      "submitUrl": "/rest/headless/v1/actions/forms/submit",
      "method": "POST"
    },
    "search": {
      "queryUrl": "/rest/headless/v1/actions/search",
      "method": "GET"
    }
  }
}
```

Response 401:
```json
{
  "timestamp": "2026-06-26T12:00:00Z",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Not allowed access",
  "path": "/rest/headless/v1/pages/by-path"
}
```

Response 404:
```json
{
  "timestamp": "2026-06-26T12:00:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Document not found",
  "path": "/rest/headless/v1/pages/by-path"
}
```

### 20.2 GET Page by Path (HTML)

Request:
```http
GET /rest/headless/v1/pages/by-path?path=/en/home/&lng=en HTTP/1.1
Accept: text/html
```

Response 200:
```html
<main><h1>Home</h1><div class="app">...</div></main>
```

### 20.3 GET Navigation

Request:
```http
GET /rest/headless/v1/navigation?rootPath=/en/&depth=2&lng=en HTTP/1.1
Accept: application/json
```

Response 200:
```json
{
  "root": {
    "path": "/en/",
    "title": "English"
  },
  "items": [
    {
      "title": "Home",
      "path": "/en/home/",
      "docId": 12345,
      "groupId": 210,
      "children": []
    },
    {
      "title": "Blog",
      "path": "/en/blog/",
      "docId": 12346,
      "groupId": 211,
      "children": [
        {
          "title": "Post 1",
          "path": "/en/blog/post-1/",
          "docId": 12347,
          "groupId": 212,
          "children": []
        }
      ]
    }
  ]
}
```

### 20.4 GET Preview by Doc ID (Admin Session)

Request:
```http
GET /rest/headless/v1/preview/pages/by-id?docId=12345 HTTP/1.1
Accept: application/json
Cookie: JSESSIONID=...
```

Response 200:
- Same schema as PageResponse.

Response 403:
```json
{
  "timestamp": "2026-06-26T12:00:00Z",
  "status": 403,
  "error": "FORBIDDEN",
  "message": "Preview requires admin session",
  "path": "/rest/headless/v1/preview/pages/by-id"
}
```

### 20.5 POST Forms Submit

Request:
```http
POST /rest/headless/v1/actions/forms/submit HTTP/1.1
Content-Type: application/json
Accept: application/json
```

```json
{
  "form": {
    "id": "contactForm",
    "componentKey": "!INCLUDE(ContactFormComponent,formId=contactForm)!"
  },
  "context": {
    "path": "/en/contact/",
    "locale": "en"
  },
  "fields": {
    "name": "John Doe",
    "email": "john@example.com",
    "message": "Hello"
  }
}
```

Response 200:
```json
{
  "success": true,
  "message": "Form submitted",
  "data": {
    "submissionId": "f-20260626-0001"
  },
  "fieldErrors": [],
  "redirect": null
}
```

Response 400:
```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "fieldErrors": [
    {
      "name": "email",
      "status": "Invalid email format"
    }
  ],
  "redirect": null
}
```

### 20.6 GET Search Action

Request:
```http
GET /rest/headless/v1/actions/search?q=webjet&page=0&size=10 HTTP/1.1
Accept: application/json
```

Response 200:
```json
{
  "success": true,
  "message": "OK",
  "data": {
    "query": "webjet",
    "page": 0,
    "size": 10,
    "totalElements": 2,
    "items": [
      {
        "title": "WebJET Intro",
        "path": "/en/blog/webjet-intro/",
        "snippet": "..."
      },
      {
        "title": "WebJET Headless",
        "path": "/en/blog/webjet-headless/",
        "snippet": "..."
      }
    ]
  },
  "fieldErrors": [],
  "redirect": null
}
```

## 21. Definition of Done (Strict Checklist)

All items below must be satisfied:

Contract and API:
- New endpoints implemented under /rest/headless/v1.
- OpenAPI JSON contains all new endpoints and schemas.
- JSON and HTML representations work for page endpoint.
- Error responses follow ErrorResponse or ActionResponse contracts.

Security:
- Public read endpoints enforce existing IP whitelist policy.
- Preview endpoint enforces admin session.
- Forms/search endpoints follow existing validation/sanitization expectations.

Behavior:
- Rendered page HTML includes resolved dynamic components.
- Device filtering behavior is preserved.
- Redirect behavior is explicitly handled for JSON/HTML modes.
- Existing /rest/documents behavior is unchanged.
- Existing non-headless ShowDoc rendering is unchanged.

Tests and validation:
- Manual verification script exists for all POC endpoints.
- At least one automated test covers JSON and HTML negotiation for page endpoint.
- At least one automated test covers preview authorization failure.
- At least one automated test covers forms validation error shape.
- At least one automated test covers search success shape.

Documentation and demo:
- Headless developer doc page added in English.
- Endpoint examples in docs match actual implementation.
- Astro demo can render one real page, navigation, preview, form submit, search.

## 22. AI-Agent Task Breakdown with File Targets

### Task 1: Create headless response DTOs

Goal:
- Introduce canonical response types for headless APIs.

Files to create:
- src/main/java/sk/iway/iwcm/headless/dto/PageResponse.java
- src/main/java/sk/iway/iwcm/headless/dto/NavigationResponse.java
- src/main/java/sk/iway/iwcm/headless/dto/ActionResponse.java
- src/main/java/sk/iway/iwcm/headless/dto/ErrorResponse.java

Files to update:
- src/main/java/sk/iway/iwcm/system/spring/openapi/OpenApiRestController.java (only if schema visibility requires adjustments)

Validation:
- Project compiles.
- DTOs visible in OpenAPI models after endpoints are added.

### Task 2: Implement page read endpoint with content negotiation

Goal:
- Implement GET /rest/headless/v1/pages/by-path supporting JSON and HTML.

Files to create:
- src/main/java/sk/iway/iwcm/headless/rest/HeadlessPageRestController.java
- src/main/java/sk/iway/iwcm/headless/service/HeadlessPageService.java

Files to update:
- src/main/java/sk/iway/iwcm/doc/ShowDoc.java (minimal additive hook if needed)
- src/main/java/sk/iway/iwcm/system/spring/webjet_component/WebjetComponentParser.java (only if required for extracted headless flow)

Validation:
- Accept application/json returns PageResponse.
- Accept text/html returns HTML.
- 401 and 404 behavior matches examples.

### Task 3: Implement navigation endpoint

Goal:
- Implement GET /rest/headless/v1/navigation.

Files to create:
- src/main/java/sk/iway/iwcm/headless/service/HeadlessNavigationService.java

Files to update:
- src/main/java/sk/iway/iwcm/headless/rest/HeadlessPageRestController.java

Validation:
- Root + children structure returned.
- Depth filter applied.

### Task 4: Implement preview endpoint (admin session)

Goal:
- Implement GET /rest/headless/v1/preview/pages/by-id with strict admin-session authorization.

Files to update:
- src/main/java/sk/iway/iwcm/headless/rest/HeadlessPageRestController.java
- src/main/java/sk/iway/iwcm/system/spring/services/WebjetSecurityService.java (only if helper method extension is needed)

Validation:
- 200 with admin session.
- 403 without admin session.

### Task 5: Implement forms action endpoint

Goal:
- Implement POST /rest/headless/v1/actions/forms/submit using existing form processing logic.

Files to create:
- src/main/java/sk/iway/iwcm/headless/rest/HeadlessActionsRestController.java
- src/main/java/sk/iway/iwcm/headless/service/HeadlessFormActionService.java

Validation:
- 200 success envelope.
- 400 with fieldErrors for validation failures.

### Task 6: Implement search action endpoint

Goal:
- Implement GET /rest/headless/v1/actions/search using existing search services.

Files to create:
- src/main/java/sk/iway/iwcm/headless/service/HeadlessSearchService.java

Files to update:
- src/main/java/sk/iway/iwcm/headless/rest/HeadlessActionsRestController.java
- src/main/java/sk/iway/iwcm/headless/service/HeadlessSearchService.java (reuse existing query/filter logic when possible)

Validation:
- Returns paged result envelope.
- 400 for invalid query.

### Task 7: Add security and error-handling wiring

Goal:
- Ensure endpoint-level behavior aligns with POC rules.

Files to update:
- src/main/java/sk/iway/iwcm/headless/rest/HeadlessPageRestController.java
- src/main/java/sk/iway/iwcm/headless/rest/HeadlessActionsRestController.java
- src/main/java/sk/iway/iwcm/system/spring/BaseSpringConfig.java (only if matcher updates are needed)

Validation:
- Public read and preview auth behaviors match contract.
- Error response consistency verified.

### Task 8: Add tests

Goal:
- Cover contract-critical behavior.

Files to create:
- src/test/java/sk/iway/iwcm/headless/rest/HeadlessPageRestControllerTest.java
- src/test/java/sk/iway/iwcm/headless/rest/HeadlessActionsRestControllerTest.java

Validation:
- Tests for content negotiation, preview authorization, forms validation, search success.

### Task 9: Add docs

Goal:
- Publish implementation and integration guide.

Files to create:
- docs/en/custom-apps/spring/headless-cms-poc.md

Files to update:
- docs/en/custom-apps/_sidebar.md
- docs/en/custom-apps/spring/public-services.md
- docs/en/custom-apps/spring/api-auth.md

Validation:
- Docs match endpoint behavior and examples.

### Task 10: Add Astro demo

Goal:
- Demonstrate usage of headless endpoints.

Recommended location options:
- docs/examples/headless-astro/ (if documentation examples are preferred)
- test/headless-astro-demo/ (if test/demo separation is preferred)

Minimum files:
- package.json
- src/pages/[...slug].astro
- src/pages/search.astro
- src/pages/preview/[docId].astro
- src/lib/api.ts

Validation:
- Demo routes render API content.
- Search and form submit roundtrip works.

## 23. Manual Verification Script

Execute in order:
1. GET page JSON with Accept application/json.
2. GET page HTML with Accept text/html.
3. GET navigation with depth filter.
4. GET preview without admin session and verify 403.
5. GET preview with admin session and verify 200.
6. POST forms submit with invalid data and verify fieldErrors.
7. POST forms submit with valid data and verify success.
8. GET search with valid query and verify paged data.
9. GET unknown page path and verify 404 envelope.
10. Verify legacy /rest/documents endpoint still behaves unchanged.

## 24. Prompt Usage for Implementation Agent

When used as implementation prompt:
- Execute tasks in section 22 in order.
- Do not skip strict checks from section 21.
- Treat section 20 examples as contract source for first implementation.
- Keep changes minimal and additive to reduce regression risk.

## 25. Frontend (Astro) Session and Cookie Management

When consuming headless endpoints from a decoupled frontend (e.g., Astro), session management requires special handling:

### Problem
- Backend runs on domain A (e.g., `cms.iway.sk`), frontend on domain B (e.g., `iwcm.interway.sk`).
- Cookies from backend have domain scope set to backend domain only.
- Frontend cannot store or directly access backend cookies due to browser SameSite/CORS policies.
- Session persistence across multiple API calls fails.

### Solution Pattern: Transparent Cookie Forwarding

**Client-side (browser):**
- Makes HTTP requests to frontend server (domain B) normally; no special cookie handling needed.

**Frontend server-side (Astro/Node):**

1. **Outbound API calls** - Forward incoming request cookies to backend:
   ```typescript
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
   ```

2. **API wrapper functions** - Return both data and response headers:
   ```typescript
   export async function getPage(
     path: string,
     request?: Request
   ): Promise<{ data: PageResponse; headers: Headers }> {
     const response = await fetch(`${API_BASE}/pages/by-path?path=${path}`,
       createFetchOptions(request)
     );
     return { data: await response.json(), headers: response.headers };
   }
   ```

3. **In Astro components** - Extract `Set-Cookie` from API response and append to frontend response:
   ```astro
   ---
   const result = await getPage(path, Astro.request);
   const setCookie = result.headers.get('set-cookie');
   if (setCookie) {
     Astro.response.headers.append('Set-Cookie', setCookie);
   }
   ---
   ```

**Flow:**
1. Browser sends request to `iwcm.interway.sk` (frontend).
2. Frontend extracts `Cookie` header from browser request.
3. Frontend forwards cookie in fetch to `cms.iway.sk` (backend).
4. Backend processes request with session cookie, returns updated `Set-Cookie`.
5. Frontend extracts `Set-Cookie` from backend response.
6. Frontend appends `Set-Cookie` to browser response.
7. Browser stores cookie scoped to backend domain (as directed).
8. On next request, browser sends both frontend + backend cookies.
9. Frontend extracts backend cookies and forwards them to backend again.

**Key Requirements:**
- Backend `Set-Cookie` must **not** have `SameSite=Strict` (use `Lax` or `None` with `Secure`).
- Backend `Set-Cookie` domain scope must match actual backend domain (not frontend domain).
- Frontend must always pass through `Set-Cookie` headers in responses.
- All API wrapper functions must expose response headers, not just data.

**Implementation Pattern for API Modules:**
- Provide only `getPage()` style functions (new: `getPage()` → `{ data, headers }`).
- Always require `request` parameter in server-side functions to enable cookie forwarding.

This pattern enables seamless multi-domain session persistence without requiring the frontend to manage tokens or cookies explicitly.
