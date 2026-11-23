# AGENTS: Page editor and approval

[Index](AGENTS.md) · [Working guide](CONTRIBUTING.md)

The editor combines the admin DataTable, CKEditor, PageBuilder and request-scoped backend services. It owns page editing, approval, media associations and multigroup coordination. See [Doc](AGENTS-doc.md) for content models, public rendering and timed publication.

All source paths are core-root-relative. In a customer project, verify the installed CMS version and existing extension points instead of copying current-core internals into the application.

## Request flow and source map

| Entry point | Role |
|---|---|
| `src/main/java/sk/iway/iwcm/editor/rest/WebpagesRestController.java` | `/admin/rest/web-pages`, guarded by `menuWebpages`; extends `WebpagesDatatable`. |
| `src/main/java/sk/iway/iwcm/editor/rest/WebpagesDatatable.java` | DataTable CRUD/actions, response metadata and notifications. Calls the facade for saves and deletion. |
| `src/main/java/sk/iway/iwcm/editor/facade/EditorFacade.java` | Request-scoped entry point for `getDocForEditor(docId, historyId, groupId)` and `save(DocDetails)`. Coordinates editor fields, master/slave pages, attributes, media and template cache refreshes. |
| `src/main/java/sk/iway/iwcm/editor/service/EditorService.java` | Request-scoped persistence/workflow implementation. Checks permissions, prepares URLs/content, saves documents/history and invokes approval. Its class documentation directs normal callers to `EditorFacade`. |
| `src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java` | Page list/detail preparation, filtering and the `DATA_NOT_LOADED` sentinel for content not loaded into list rows. |
| `src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java` | `/admin/rest/groups`; folder editing, validation and approval/scheduling integration. |
| `src/main/java/sk/iway/iwcm/editor/service/GroupsService.java` | Folder operations and System/Trash helpers. |
| `src/main/java/sk/iway/iwcm/editor/service/MultigroupService.java` | Master/slave page mapping and synchronization across folders. |
| `src/main/java/sk/iway/iwcm/editor/service/MediaService.java` | Page-to-media associations, temporary media for new pages and link updates; not the general thumbnail/image service. |
| `src/main/java/sk/iway/iwcm/editor/service/LinkCheckService.java` | Link-check workflow; exposed by `src/main/java/sk/iway/iwcm/editor/rest/LinkCheckRestController.java`. |

Legacy entry points such as `src/main/java/sk/iway/iwcm/editor/SaveDocAction.java` and `src/main/java/sk/iway/iwcm/editor/EditorDB.java` remain in use. Inspect call sites before removing or bypassing one; a `.struts` route can also be handled by a modern Spring controller.

## Save, approval and publication

1. Load an editable page through `EditorFacade.getDocForEditor(...)`. It handles history selection, new-page defaults and multigroup master resolution.
2. Keep `DocEditorFields` consistent with `DocDetails`; the facade converts editor fields back to persistent fields before saving. Do not submit a list row containing `DATA_NOT_LOADED` as page HTML.
3. The facade delegates to `EditorService.saveEditedDoc(...)`. The service validates access, normalizes content/URLs and decides whether the change can reach the public document or remains a history/work version.
4. Approval is in `src/main/java/sk/iway/iwcm/editor/approve/ApproveService.java`. It loads folder approvers, supports approval levels and self-approval, and handles notifications/actions. It is not under `editor/service`.
5. The facade then performs related updates and returns the saved editor representation. `WebpagesDatatable` propagates notification and reload information to the UI.

`editorFields.requestPublish`, page `available`, history `actual`/`publicable`, approval fields and publication dates have different meanings. Avoid treating them as a single status enum or toggling them to implement a save. Timed publication is handled by `src/main/java/sk/iway/iwcm/doc/DocPublishService.java`.

Folder approval and deletion also have workflows: `src/main/java/sk/iway/iwcm/editor/approve/GroupsApproveRestController.java`, `src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerRestController.java` and `GroupSchedulerDto` use scheduled/pending folder data. Test page and folder deletion/recovery separately from ordinary updates.

## Permissions, scope and notifications

- A controller's `menuWebpages` check does not replace per-page checks. `EditorService.checkPermissions(...)` also evaluates page operations and `isPageEditable(...)`; folder operations use folder permissions. Preserve checks for the target folder when moving or copying content.
- The facade, editor service and approval service hold request-specific user/state. Do not reuse them as static singletons or carry their mutable state between requests.
- Editor locking is an advisory presence/notification mechanism in `src/main/java/sk/iway/iwcm/system/datatable/editorlocking/EditorLockingRestController.java` and `src/main/webapp/admin/v9/npm_packages/webjetdatatables/datatables-wjfunctions.js`. It does not provide a transaction lock around `EditorFacade.save(...)`.
- Follow existing `NotifyBean` handling and DataTable error responses. `src/main/java/sk/iway/iwcm/editor/service/NotifyService.java` is a small notification-list base class, not a global message queue.
- For JSON payloads, use the existing Jackson DTO serialization. HTML content needs the appropriate content validation and output escaping; a JSON escaping helper is not an HTML sanitizer.
- Page/group domain ownership comes from the content hierarchy. Derive URLs and destination permissions from the actual page/group, including when the administrator has selected another domain.

## Frontend and extension points

- Page list/editor glue: `src/main/webapp/admin/v9/src/js/pages/web-pages-list/` and `src/main/webapp/admin/v9/views/pages/webpages/web-pages-list.pug`.
- CKEditor integration: `src/main/webapp/admin/v9/src/js/datatables-ckeditor.js`; the bundled CKEditor 4 distribution/configuration is in `src/main/webapp/admin/skins/webjet8/ckeditor/`.
- PageBuilder: `src/main/webapp/admin/webpages/page-builder/`; read `docs/en/frontend/page-builder/blocks.md` and the `wj-page-builder` skill when editing blocks.
- AppStore editor: `src/main/java/sk/iway/iwcm/editor/rest/ComponentsService.java`, `src/main/java/sk/iway/iwcm/editor/rest/ComponentListener.java` and `src/main/java/sk/iway/iwcm/editor/appstore/`. See [Components](AGENTS-components.md) for app authoring.
- Save events: `EditorService` publishes `WebjetEvent<DocDetails>` for save/delete/recover phases; `src/main/java/sk/iway/iwcm/editor/rest/SaveGroupListener.java` shows a folder-save listener. `src/main/java/sk/iway/iwcm/editor/rest/WebPagesListener.java` instead prepares the initial admin page model through `ThymeleafEvent`. Use the event contract matching the extension; subclassing `ApproveService` alone does not replace the injected service.
- AI editor integration lives in `src/main/webapp/admin/v9/npm_packages/webjetdatatables/editor-ai.js` and `src/main/java/sk/iway/iwcm/components/ai/`. Its DTO/provider contract is separate from the page save workflow; see [Components](AGENTS-components.md).

## Verification starting points

- Backend examples: `src/test/java/sk/iway/iwcm/editor/service/EditorServiceTest.java`, `src/test/java/sk/iway/iwcm/editor/service/WebpagesUrlTest.java` and `src/test/java/sk/iway/iwcm/editor/approve/GroupsApproveRestControllerTest.java`.
- Browser scenarios: `src/test/webapp/tests/webpages/` covers content, permissions, history, approval, publishing, media, multigroup and PageBuilder; `src/test/webapp/tests/admin/editor-locking.js` covers presence warnings.
- For workflow changes, check restricted users, work-version save versus publish, approval, future publication and the public page after saving. Run only the relevant available scenarios using the project's established database and fixtures.
