# AGENTS: Pages, folders and templates

[Index](AGENTS.md) · [Working guide](CONTRIBUTING.md)

This area contains website content, folder structure, public rendering and the caches used to resolve pages. The editor save and approval workflow is described in [Editor](AGENTS-editor.md).

Paths below are relative to the WebJET core repository. Customer projects usually consume these classes from their CMS dependency; check that dependency's version before using an API or extending its behavior.

## Models and persistence

| Model / entry point | Actual responsibility |
|---|---|
| `src/main/java/sk/iway/iwcm/doc/DocBasic.java` | JPA `@MappedSuperclass` shared by pages and history, including HTML `data`, searchable `dataAsc`, metadata and editor annotations. It is not a lightweight list DTO. |
| `src/main/java/sk/iway/iwcm/doc/DocDetails.java` | Entity for `documents`; its page ID is distinct from a history record ID. Uses the audit entity listener. |
| `src/main/java/sk/iway/iwcm/doc/DocHistory.java` | Entity for `documents_history`, with its own `history_id`, page `docId`, approval fields, `actual` and scheduled-publication flag `publicable`. |
| `src/main/java/sk/iway/iwcm/doc/DocDetailsRepository.java`, `src/main/java/sk/iway/iwcm/doc/DocHistoryRepository.java` | Spring Data access to these two tables. Saving directly does not run the full editor workflow. |
| `src/main/java/sk/iway/iwcm/doc/GroupDetails.java`, `src/main/java/sk/iway/iwcm/doc/GroupsDB.java` | Folder metadata and cached/JDBC access: parent, domain, default page, template, access settings and URL path. `GroupDetails` is not a JPA entity. |
| `src/main/java/sk/iway/iwcm/doc/TemplateDetails.java`, `src/main/java/sk/iway/iwcm/doc/TemplatesDB.java` | Template metadata and lookup/cache layer. Templates are not represented by a generic JPA repository here. |
| `src/main/java/sk/iway/iwcm/doc/TemplatesGroupBean.java`, `src/main/java/sk/iway/iwcm/doc/TemplatesGroupDB.java` | Template groups, shared presentation settings and their persistence. These are separate from website folders. |

`DocDB.getInstance().getDoc(docId)` loads full content. `getBasicDocDetails(docId, false)` reads cached metadata and can return `null`; it still returns `DocDetails`. Do not save a metadata-only object as complete page content. See `src/main/java/sk/iway/iwcm/doc/DocDB.java` and the unloaded-content guard in `src/main/java/sk/iway/iwcm/editor/service/EditorService.java`.

## Content lifecycle

- Normal editor saves use `EditorFacade`, which coordinates `EditorService`, media, attributes and multigroup copies. Direct SQL or repository saves bypass that coordination.
- `src/main/java/sk/iway/iwcm/doc/DocPublishService.java` handles **timed publication and unpublication**. It publishes eligible history records into `documents` and disables pages after their end date. It is not the general draft/approval service.
- `DocDB.getInstance()` invokes publication checks; the publisher refreshes its pending records through the document cache lifecycle. Preserve this integration when changing scheduled publication.
- `src/main/java/sk/iway/iwcm/doc/GroupPublisher.java` handles scheduled folder changes in `groups_scheduler`. The editor also uses this table for pending folder approvals; inspect `src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerDto.java` and `src/main/java/sk/iway/iwcm/editor/approve/GroupsApproveRestController.java` before changing it.
- History and audit are different concerns. History carries editable versions and approval/publication metadata; audit records changes. `editorDisableHistory` can disable history writing, so do not assume every save always produces a new version.

## Folders, URLs and permissions

- Pages are scoped through their folder hierarchy and domain. Do not apply a generic `domainId` column assumption to `documents` or `groups`.
- Use `DocDB.getDocLink(...)` and existing folder URL helpers for links; consider domain, external links, default pages and context paths. For URL lookup, inspect `DocDB.getDocIdFromURL(virtualPath, domain)` rather than assuming paths are globally unique.
- Cached `DocDB`/`GroupsDB` lookups do not replace authorization. Editor permissions are enforced by the editor services; public visibility and password-protected access are handled during rendering.
- `src/main/java/sk/iway/iwcm/doc/GroupsTreeService.java` builds and filters admin jsTree nodes by permissions, domain and tree mode. `src/main/java/sk/iway/iwcm/doc/GroupsTreeRestController.java` exposes `/admin/rest/groups/tree`. Preserve the distinct System, Trash and selector modes when modifying the tree.
- `src/main/java/sk/iway/iwcm/doc/NavbarService.java` and the `Navbar*` implementations produce breadcrumbs. Multigroup copies, structure cloning and language mirroring are separate features; start in `src/main/java/sk/iway/iwcm/editor/service/MultigroupService.java`, `src/main/java/sk/iway/iwcm/doc/clone_structure/` or `src/main/java/sk/iway/iwcm/doc/mirroring/` respectively.

## Public rendering and templates

- Start in `src/main/java/sk/iway/iwcm/doc/ShowDoc.java` for page loading, access checks, request attributes and template dispatch. It publishes `WebjetEvent<ShowDocBean>` events. `ShowDocListener` is an unused compatibility shell, not the place to add rendering behavior.
- JSP and Thymeleaf templates coexist. `src/main/java/sk/iway/iwcm/doc/showdoc/ThymeleafShowdocController.java` renders the template selected by `ShowDoc`, exposing `ninja`, `docDetails`, `docDetailsOriginal`, `groupDetails`, `tempDetails` and `templatesGroupDetails` to the model.
- Template administration lives in `src/main/java/sk/iway/iwcm/components/templates/` and `src/main/java/sk/iway/iwcm/components/template_groups/`. `TemplatesController` uses `/admin/rest/templates/temps-list` and the `menuTemplates` permission.
- Website templates are under `src/main/webapp/templates/`. Resolve the target project's actual template group, installation name and template engine before editing a layout.
- See `docs/en/frontend/templates/README.md`, `docs/en/frontend/templates/templates.md` and `docs/en/frontend/templates/template-groups.md` for authoring guidance; verify details against the target code/version.

## Cache and field changes

- Use existing save paths so caches are refreshed along with persistence. Relevant APIs include `DocDB.getInstance().updateInternalCaches(docId)`, `GroupsDB.getInstance(true)` and `TemplatesDB.getInstance(true)`; a full refresh is not interchangeable with a page update.
- Do not mutate cached collections or reuse a cached page as an isolated edit copy. Follow the current service's copy/reload behavior.
- A page field may affect `DocBasic`, both database tables, editor annotations/`DocEditorFields`, MapStruct mapping in `src/main/java/sk/iway/iwcm/editor/rest/DocDetailsToDocHistoryMapper.java`, history DTOs and rendering. Check which layers actually need the field.
- Folder/template fields also have JDBC persistence. Inspect `GroupsDB`, `TemplatesDB`, `DataAccessHelper` and the relevant editor service; adding a Java property alone does not persist it.

## Verification starting points

Existing browser coverage lives in `src/test/webapp/tests/webpages/`: `publishing.js`, `webpage-history.js`, `approve-logic.js`, `group-approve-logic.js`, `multigroup.js`, `mirroring.js`, `virtual-path.js` and `webpage-perms.js`. Select scenarios matching the change, including scheduled/history behavior and restricted users when relevant. Customer projects must use their own available fixtures and test environment.
