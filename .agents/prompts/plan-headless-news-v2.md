## Plan: Headless News REST and Astro Demo Pages

Deliver an implementation-ready specification for an external AI coding agent (qwen3.6) to extend HEADLESS mode with a news listing REST endpoint and two Astro demo variants.

### Goal
Add a headless news endpoint that accepts NewsActionBean-compatible parameters in a JSON request and returns:
1. A list of DocDetails items
2. A pagination object

Add two Astro example pages that consume this endpoint:
1. Server-side rendering page
2. Client-side rendering page

All documentation and outputs in English.

### Fixed Input Mapping from Existing Include
The new REST request must represent this include configuration exactly:
- groupIds = 24
- alsoSubGroups = false
- publishType = new
- order = date
- ascending = false
- paging = false
- pageSize = 10
- offset = 0
- perexNotRequired = false
- loadData = false
- checkDuplicity = false
- removeDefaultDocs = false
- perexGroup = empty
- perexGroupNot = empty

Canonical JSON payload values for demo and tests:
- groupIds: [24]
- alsoSubGroups: false
- publishType: new
- order: date
- ascending: false
- paging: false
- pageSize: 10
- offset: 0
- perexNotRequired: false
- loadData: false
- checkDuplicity: false
- removeDefaultDocs: false
- perexGroup: []
- perexGroupNot: []

### Locked Decisions
- Endpoint method: POST with JSON body only.
- Response item type: raw DocDetails serialization.
- Scope: core NewsActionBean parity first, advanced parity deferred.
- Security: existing REST IP whitelist enforcement.
- Client-side demo transport: browser -> Astro proxy -> CMS REST.

### Phase Plan

#### Phase 1 - REST Contract and DTOs
1. Define endpoint contract under headless namespace.
2. Define request DTO for NewsActionBean-like JSON parameters.
3. Define response DTO wrapper with:
   - items (DocDetails list)
   - pagination object with page, size, totalElements, totalPages
4. Define validation rules for malformed request and boundary values.

#### Phase 2 - Service and Query Translation
1. Add a dedicated headless news service.
2. Map request DTO fields to NewsQuery behavior using core NewsActionBean semantics.
3. Preserve ordering, publish filtering, group filtering, paging, and offset behavior.
4. Keep implementation additive and avoid changing existing page rendering flow.

#### Phase 3 - REST Controller and Security
1. Add headless news REST controller.
2. In every endpoint method, call IP whitelist check first using inherited REST base behavior.
3. Return deterministic 400 responses for request validation failures.
4. Return 200 responses with stable response schema.

#### Phase 4 - Astro API Integration
1. Extend existing Astro API client in [docs/examples/headless-astro/src/lib/api.ts](docs/examples/headless-astro/src/lib/api.ts) with a typed helper for news list POST requests.
2. Preserve cookie forwarding and Set-Cookie pass-through conventions already used by current Astro integration.

#### Phase 5 - Astro Demo Pages
1. Create server-side demo page under [docs/examples/headless-astro/src/pages](docs/examples/headless-astro/src/pages) using frontmatter fetch and server rendering.
2. Create client-side demo page under [docs/examples/headless-astro/src/pages](docs/examples/headless-astro/src/pages) with browser-rendered results.
3. For client-side demo, fetch through an Astro proxy endpoint to avoid CORS instability.
4. Add links from [docs/examples/headless-astro/src/pages/index.astro](docs/examples/headless-astro/src/pages/index.astro) to both demo pages.

#### Phase 6 - Tests and Verification
1. Add automated tests for:
   - valid request and expected response structure
   - invalid body handling
   - paging and ordering behavior
   - publishType behavior
   - IP whitelist rejection (401)
2. Add parity checks comparing representative output with current NewsActionBean behavior for the same parameter set.
3. Validate that existing headless endpoints and existing Astro pages continue to work unchanged.

### Core References
- [src/main/java/sk/iway/iwcm/components/news/NewsActionBean.java](src/main/java/sk/iway/iwcm/components/news/NewsActionBean.java)
- [src/main/java/sk/iway/iwcm/components/news/NewsQuery.java](src/main/java/sk/iway/iwcm/components/news/NewsQuery.java)
- [src/main/java/sk/iway/iwcm/headless/rest/HeadlessPageRestController.java](src/main/java/sk/iway/iwcm/headless/rest/HeadlessPageRestController.java)
- [src/main/java/sk/iway/iwcm/headless/rest/HeadlessActionsRestController.java](src/main/java/sk/iway/iwcm/headless/rest/HeadlessActionsRestController.java)
- [src/main/java/sk/iway/iwcm/rest/RestController.java](src/main/java/sk/iway/iwcm/rest/RestController.java)
- [docs/examples/headless-astro/src/lib/api.ts](docs/examples/headless-astro/src/lib/api.ts)
- [docs/examples/headless-astro/src/pages/index.astro](docs/examples/headless-astro/src/pages/index.astro)
- [docs/examples/headless-astro/src/pages/search.astro](docs/examples/headless-astro/src/pages/search.astro)
- [docs/examples/headless-astro/src/pages/[...slug].astro](docs/examples/headless-astro/src/pages/%5B...slug%5D.astro)

### Explicit Out of Scope for This Iteration
- Full advanced parity for all NewsActionBean edge behavior.
- New frontend design system or major visual redesign.
- Breaking changes to existing headless endpoints.
- Direct browser-to-CMS calls for client-side demo where CORS may block execution.

### Acceptance Criteria
1. Headless news endpoint accepts JSON body containing the mapped include parameters.
2. Endpoint returns DocDetails list and pagination object in one stable JSON response.
3. Endpoint enforces IP whitelist policy and returns 401 for disallowed IP.
4. Server-side Astro demo page renders news data from the endpoint.
5. Client-side Astro demo page renders news data via Astro proxy transport.
6. Demo uses the fixed parameter mapping defined in this document.
7. Existing headless and Astro functionality remains unaffected.

### Implementation Order for qwen3.6
1. Contract and DTOs
2. Service translation layer
3. REST controller
4. API client extension in Astro
5. Server-side demo page
6. Client-side demo page with proxy transport
7. Automated tests
8. Final doc sync
