# Implement headless News query API in WebJET CMS - compact handsoff

## Mission
Implement a public HEADLESS News query API in WebJET CMS that reuses existing News component behavior and returns DocDetails directly with paginator data.

## Source Behavior to Reuse
- news-velocity.jsp
- NewsActionBean.java
- NewsApp.java
- NewsQuery.java

## Locked Decisions
1. Return DocDetails directly, not a custom item DTO.
2. Fix obvious legacy issues during implementation.
3. Include deduplication in REST v1.
4. Publish as public API.
5. Deliver two Astro demo pages:
6. news-sse.astro for server-side loading with pagination.
7. news-client-side.astro for client-side loading with interactive pagination.

## API Contract
1. Endpoint: POST /rest/headless/v1/news/query
2. Request: JSON payload compatible with NewsActionBean and NewsApp parameters.
3. Response:
4. items: array of DocDetails
5. paginator with first prev next last pages pagesAll totalCount totalPages
6. totalCount totalPages page pageSize offset
7. optional nextDuplicitySeedDocIds
8. Errors: 400 401 403 500

## Required Request Fields
1. groupIds
2. alsoSubGroups
3. subGroupsDepth
4. includeActualDoc
5. currentDocId
6. currentGroupId
7. searchAlsoProtectedPages
8. returnDocsWithAtributes
9. loadData
10. paging
11. page
12. pageSize
13. offset
14. publishType
15. order
16. ascending
17. docMode
18. perexNotRequired
19. perexCrop
20. perexGroup
21. perexGroupNot
22. tag
23. author
24. requestPerexGroupsName
25. requestPerexGroups
26. filter
27. search
28. checkDuplicity
29. duplicitySeedDocIds

## Implementation Structure
Create:
1. HeadlessNewsRestController
2. HeadlessNewsService
3. HeadlessNewsQueryRequest
4. HeadlessNewsQueryResponse
5. NewsPaginatorResponse
6. NewsPaginationItemResponse

Refactor shared logic into:
1. NewsQueryBuilderService in News component package.

## Mandatory Controller Rules
1. Extend sk.iway.iwcm.rest.RestController.
2. First line in endpoint method must call isIpAddressAllowed(request).
3. Validate payload and return 400 on invalid input.
4. Add OpenAPI annotations.

## Mandatory Service Rules
1. Apply filtering steps in same order as NewsActionBean news flow.
2. Use getNewsList or getNewsListWithAtributes based on request.
3. Apply perex crop when configured.
4. Build paginator equivalent to current NewsActionBean behavior.
5. Dedup must be stateless for REST:
6. consume duplicitySeedDocIds
7. exclude seed and mapped slave master IDs
8. return merged nextDuplicitySeedDocIds

## Legacy Fixes Required
1. Fix requestPerexGroupsName literal-check bug in perex group logic.
2. Guard fieldOperator length in filter parsing.
3. Fix inconsistent key usage in addCriteria static method.
4. Make totalPages computation safe when pageSize invalid or zero.
5. Remove hidden request-state coupling in REST dedup mode.

## Astro Demo Payload Mapping
Use this baseline mapping from include parameters:
1. groupIds [24]
2. alsoSubGroups false
3. publishType new
4. order date
5. ascending false
6. pageSize 10
7. offset 0
8. perexNotRequired false
9. loadData false
10. checkDuplicity false
11. docMode 0 from removeDefaultDocs false
12. perexGroup empty array
13. perexGroupNot empty array

## Astro Variant A
Page: news-sse.astro
1. Server-side fetch on each request.
2. Read page from query string, default 1.
3. Send paging true with page and pageSize 10.
4. Render list and pagination links as anchors.
5. Handle empty and error states.

## Astro Variant B
Page: news-client-side.astro
1. Render shell first.
2. Fetch page 1 on client load.
3. Client-side render list and paginator.
4. Pagination changes trigger fetch without full reload.
5. Show loading empty error states.

## Shared Astro Rules
1. Use one shared API helper for both pages.
2. Use HEADLESS_API_BASE_URL environment variable.
3. Keep cookie forwarding support.
4. Ensure REST allowlist permits Astro server access.

## Tests Required
Create:
1. HeadlessNewsRestControllerTest
2. HeadlessNewsServiceTest

Cover:
1. valid request returns items and paginator
2. DocDetails returned directly
3. paging behavior correct
4. includeActualDoc exclusion works
5. publishType variants work
6. perexGroup and perexGroupNot behavior works
7. filter and search parsing works
8. dedup chaining works
9. invalid request returns 400
10. allowlist restriction returns 401
11. legacy fixes are test-covered

## Documentation Required
1. Add English page for headless news query.
2. Update English docs navigation and public API sections.
3. Include endpoint contract, schemas, errors, dedup workflow, allowlist configuration, Astro examples.

## Non-Negotiable Compatibility
1. Do not break news-velocity.jsp.
2. Do not alter behavior of:
3. HeadlessPageRestController.java
4. HeadlessActionsRestController.java

## Done Criteria
1. Public endpoint implemented and documented.
2. Output is DocDetails plus paginator.
3. Legacy fixes implemented safely.
4. Stateless dedup works.
5. OpenAPI updated.
6. Both Astro demos implemented and working.
7. Automated tests passing.