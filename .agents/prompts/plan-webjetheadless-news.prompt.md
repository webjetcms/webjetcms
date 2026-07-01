# Implement headless News query API in WebJET CMS

## 1. Objective
Implement a public headless REST API for News listing that reuses existing News component logic from:
- news-velocity.jsp
- NewsActionBean.java
- NewsApp.java
- NewsQuery.java

The API must accept NewsActionBean-compatible parameters as JSON and return:
1. A list of DocDetails objects directly.
2. A pagination object equivalent to current NewsActionBean paginator behavior.

## 2. Locked Decisions
1. Return DocDetails directly, not a custom NewsDocItemDto.
2. Fix obvious legacy issues during implementation.
3. Include deduplication logic in REST v1.
4. Publish this as a public API.
5. Provide two Astro demo variants:
6. Server-side page: news-sse.astro
7. Client-side page: news-client-side.astro

## 3. Public API Contract

### Endpoint
1. POST /rest/headless/v1/news/query

### Request
1. Content-Type: application/json
2. Body: one JSON object mapping NewsActionBean and NewsApp parameters.

### Response
1. Content-Type: application/json
2. Fields:
3. items: array of DocDetails
4. paginator: object with paging metadata and navigation entries
5. totalCount, totalPages, page, pageSize, offset
6. optional requestMeta for debugging and integration traceability

### Error Responses
1. 400 invalid request
2. 401 IP not allowed
3. 403 forbidden, when applicable
4. 500 unexpected server error

## 4. Request JSON Fields
Use one request DTO, for example HeadlessNewsQueryRequest, with these fields:

1. groupIds: integer array
2. alsoSubGroups: boolean
3. subGroupsDepth: integer
4. includeActualDoc: boolean
5. currentDocId: integer
6. currentGroupId: integer
7. searchAlsoProtectedPages: boolean
8. returnDocsWithAtributes: boolean
9. loadData: boolean
10. paging: boolean
11. page: integer
12. pageSize: integer
13. offset: integer
14. publishType: enum-like string NEW OLD ALL NEXT VALID
15. order: enum-like string matching NewsQuery.OrderEnum
16. ascending: boolean
17. docMode: integer
18. perexNotRequired: boolean
19. perexCrop: integer
20. perexGroup: integer array
21. perexGroupNot: integer array
22. tag: string
23. author: string
24. requestPerexGroupsName: string
25. requestPerexGroups: integer array
26. filter: map of string to string array
27. search: map of string to string array
28. checkDuplicity: boolean
29. duplicitySeedDocIds: integer array

Notes:
1. requestPerexGroups is the REST-native input and should be primary.
2. requestPerexGroupsName remains only for compatibility fallback.

## 5. Response JSON Fields
Use one response DTO, for example HeadlessNewsQueryResponse:

1. items: List of DocDetails
2. paginator: NewsPaginatorResponse
3. totalCount: integer
4. totalPages: integer
5. page: integer
6. pageSize: integer
7. offset: integer
8. optional nextDuplicitySeedDocIds for stateless dedup chaining

Paginator should mirror current getPaginator semantics from NewsActionBean:
1. first
2. prev
3. next
4. last
5. pages
6. pagesAll
7. totalCount
8. totalPages

Pagination item shape:
1. label
2. pageNumber
3. url
4. active
5. actual
6. first
7. last

## 6. Implementation Architecture

### New classes
1. HeadlessNewsRestController
2. HeadlessNewsService
3. HeadlessNewsQueryRequest
4. HeadlessNewsQueryResponse
5. NewsPaginatorResponse
6. NewsPaginationItemResponse

### Shared logic refactor target
1. NewsQueryBuilderService under the News component package.

Purpose:
1. Avoid duplicated query business logic.
2. Keep JSP and REST behavior aligned.

## 7. Controller Requirements
In HeadlessNewsRestController:

1. Extend sk.iway.iwcm.rest.RestController.
2. Call isIpAddressAllowed(request) as the first line in endpoint method.
3. Validate request payload.
4. Delegate query execution to HeadlessNewsService.
5. Return DocDetails plus paginator in one JSON response.
6. Add OpenAPI annotations for public API visibility.

## 8. Service Logic Requirements
HeadlessNewsService must:

1. Build NewsQuery through shared query builder logic.
2. Apply filters in the same order as NewsActionBean news method.
3. Execute getNewsList or getNewsListWithAtributes based on request.
4. Apply perex crop when configured.
5. Build paginator compatible with NewsActionBean behavior.
6. Implement stateless dedup:
7. seed from duplicitySeedDocIds
8. append current result docIds
9. return merged set as nextDuplicitySeedDocIds

## 9. Legacy Issues to Fix
Fix these issues while preserving compatibility intent:

1. In addPerexGroups, wrong literal check for requestPerexGroupsName.
2. In addFilter, guard fieldOperator length before index access.
3. In addCriteria static method, inconsistent request attribute key usage.
4. In paginator math, make totalPages safe when pageSize is zero or invalid.
5. In dedup behavior, remove hidden request-state dependency for REST mode.

## 10. Deduplication Design for REST
Because REST is stateless:

1. Accept duplicitySeedDocIds in request.
2. Exclude those IDs and mapped slave or master IDs using DocDB mappings.
3. Return merged dedup IDs as nextDuplicitySeedDocIds.
4. Keep behavior aligned with checkDuplicity semantics.

## 11. Testing Plan

### New tests
1. HeadlessNewsRestControllerTest
2. HeadlessNewsServiceTest

### Required test cases
1. Valid minimal request returns 200 with items and paginator.
2. Response contains DocDetails fields directly.
3. paging true with page and pageSize computes paginator correctly.
4. includeActualDoc false excludes currentDocId.
5. publishType NEW VALID NEXT OLD behavior is correct.
6. perexGroup include and perexGroupNot exclude behavior is correct.
7. filter and search map parsing is validated.
8. checkDuplicity true respects duplicitySeedDocIds.
9. Invalid enum or malformed filter returns 400.
10. IP restriction returns 401.
11. Legacy issue fixes are covered by tests.

## 12. OpenAPI and Public Documentation

### OpenAPI
1. Add endpoint and schema annotations to appear in generated OpenAPI docs.

### Documentation
1. Add an English page for headless news query.
2. Update existing public services and API auth docs in English navigation.
3. Include:
4. endpoint overview
5. full request schema
6. response schema with DocDetails list
7. dedup chaining workflow
8. error matrix
9. security and IP allowlist setup
10. compatibility notes with legacy News component

## 13. Backward Compatibility Rules
1. Do not break JSP include flow in news-velocity.jsp.
2. Do not change behavior of existing headless page and actions endpoints:
3. HeadlessPageRestController.java
4. HeadlessActionsRestController.java
5. Keep all changes additive.

## 14. Delivery Phases
1. Phase A: DTOs and endpoint skeleton.
2. Phase B: shared query builder extraction.
3. Phase C: service implementation with DocDetails direct output.
4. Phase D: stateless dedup model.
5. Phase E: tests.
6. Phase F: OpenAPI and English documentation.
7. Phase G: Astro demo pages with both rendering modes.

## 15. Definition of Done
1. Public endpoint works with JSON request and returns DocDetails list plus paginator.
2. Query semantics match NewsActionBean behavior for equivalent inputs.
3. Dedup works in stateless chain mode.
4. Listed legacy issues are fixed and test-covered.
5. OpenAPI includes endpoint and schemas.
6. English docs are published and linked.
7. Both Astro demo variants work and are documented.

## 16. Astro Demo Baseline Payload
Use this fixed payload mapped from your include parameters:

{
  "groupIds": [24],
  "alsoSubGroups": false,
  "publishType": "new",
  "order": "date",
  "ascending": false,
  "paging": false,
  "pageSize": 10,
  "offset": 0,
  "perexNotRequired": false,
  "loadData": false,
  "checkDuplicity": false,
  "docMode": 0,
  "perexGroup": [],
  "perexGroupNot": []
}

Mapping notes:
1. removeDefaultDocs false maps to docMode 0.
2. Empty perexGroup and perexGroupNot must be empty arrays.
3. page can be omitted when paging is false.

## 17. Astro Variant A: Server-Side Page (news-sse.astro)

### Goal
Create news-sse.astro with server-side loading and server-rendered pagination.

### Behavior
1. Read page query param, default to 1.
2. Call POST /rest/headless/v1/news/query with paging true and page value.
3. Render items on server.
4. Render pagination links as normal anchor links with page query parameter.
5. Use pageSize 10.

### Acceptance Criteria
1. news-sse.astro renders first page by default.
2. news-sse.astro?page=2 renders second page.
3. Pagination links are based on returned paginator.
4. Empty and error states are visible.

## 18. Astro Variant B: Client-Side Page (news-client-side.astro)

### Goal
Create news-client-side.astro with browser-side loading and interactive pagination.

### Behavior
1. Render shell HTML first.
2. Fetch page 1 on client load.
3. Render items and pagination in browser.
4. Clicking page buttons fetches selected page without full page reload.
5. Show loading, empty, and error states.
6. Use paging true and pageSize 10.

### Acceptance Criteria
1. Initial load triggers client fetch automatically.
2. Pagination updates list without navigation reload.
3. UX works on desktop and mobile.
4. Errors are clearly visible in UI and console.

## 19. Shared Astro Integration Rules
1. Keep one shared API helper module for both pages.
2. Add HEADLESS_API_BASE_URL environment variable.
3. Keep cookie forwarding support in helper for future protected scenarios.
4. Ensure REST IP allowlist permits Astro server and, if needed, browser-origin traffic policy.

## 20. Manual Verification Script
1. Call REST endpoint with baseline payload and verify list output.
2. Call with paging true and page 2 and verify paginator consistency.
3. Verify publishType and ordering effects.
4. Verify dedup chaining with duplicitySeedDocIds.
5. Open news-sse.astro and validate server-side pagination.
6. Open news-client-side.astro and validate client-side pagination.
7. Validate 401 behavior when request origin is outside allowlist.
8. Confirm legacy News JSP rendering still works on existing pages.
