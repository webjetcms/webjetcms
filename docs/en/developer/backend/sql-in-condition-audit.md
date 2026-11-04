# SQL IN condition audit

Date: 2026-11-03. Scope: queries involving `documents`, `documents_history`, `groups`, and `groups_scheduler` in application Java and JSP source.

Implementation status: the dynamic native SQL and 16 Criteria sites identified below have been migrated to the shared helpers. This includes joined perex/attribute filters and `groups_approve` cleanup reached by folder deletion. Fixed lists, subqueries, and repository collection parameters already handled by the inspected provider remain unchanged. No risky dynamic list was found in direct `groups_scheduler` operations.

The finding tables describe the pre-migration source at commit `58a616ba11cdadada397f7fac739a9d37f422d30`; their line numbers refer to that baseline and may shift after migration. They are an inventory of the original risks, not outstanding work. Remaining scope limits and separate defects are explicitly listed below. Verification covers generated SQL without a live database connection.

## Reading the findings

- **High priority**: an ordinary content operation can generate an unbounded list from a page history, subtree, or database result.
- **Conditional**: the source permits a large list, but the trigger is an unusually large configuration or explicit selection.
- **ORM classification**: a database-free probe of the cached EclipseLink 2.7.16 Oracle platform distinguishes oversized Criteria lists from bound collection parameters that the provider already splits. See the ORM section for evidence and deployment limits. An SQL string from `DB.getInCondition` cannot directly replace a Criteria predicate or repository parameter.
- A count of returned rows is not an IN-list limit. Equality predicates and `IN (SELECT ...)` subqueries do not have the same 1000-literal issue.

## Document history and core document queries

| Priority | Operation and SQL sites | Table / column | Input shape and trigger | Migration note |
| --- | --- | --- | --- | --- |
| High | `EditorDB.saveEditorForm`: [1649](../../../../src/webjet8/java/sk/iway/iwcm/editor/EditorDB.java#L1649), [1662](../../../../src/webjet8/java/sk/iway/iwcm/editor/EditorDB.java#L1662), [1704](../../../../src/webjet8/java/sk/iway/iwcm/editor/EditorDB.java#L1704) | `documents_history.history_id` | Two `StringBuilder` CSV lists populated from SELECT results at [1610](../../../../src/webjet8/java/sk/iway/iwcm/editor/EditorDB.java#L1610) and [1674](../../../../src/webjet8/java/sk/iway/iwcm/editor/EditorDB.java#L1674). The first covers older unpublished revisions for the author; the second covers all earlier revisions of the page. Neither SELECT caps the number of IDs. | Closest counterpart to the original HistoryDB bug. Updating draft metadata, deleting obsolete drafts, and clearing the actual flag can fail when their respective lists exceed 1000. Preserve the author/approval predicates and existing bind positions. Existing CSV overload is sufficient. |
| High | `DocDB.getDocPerex`, folder filtering: [1965](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L1965), [1981](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L1981), [2005](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L2005); matching count at [2165](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L2165) | `documents.group_id` | A supplied CSV can expand to every descendant at [1518](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L1518). MSSQL, Oracle and the other-database branches each build an unsplit list. | The root-level optimization uses equality for a single root in the first three levels; those branches are not affected. Deeper roots, multiple roots, direct list calls and configurations bypassing that optimization retain the IN risk. Fix both results and count. Pagination does not bound the list. |
| High | `DocDB.getDocPerex` and `getDocPerexCountInGroups`: [1810](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L1810), [2122](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L2122) | `documents.d.doc_id NOT IN` | Numeric CSV built from master/slave duplicate mappings across all pages in the chosen groups at [1565](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L1565), [1629](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L1629). | Requires exclusion support. NOT IN chunks must be combined with AND, or the complete positive condition must be negated. Both result and count need identical semantics. |
| High | `DocDB.getDateTimeCreatedString`: [3808](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L3808) | `documents.group_id` | `List<GroupDetails>` from a complete tree becomes CSV at [3788](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L3788). | Even an aggregate returning one timestamp can fail when the selected tree exceeds 1000 groups. |
| High | `EditorDB.fixRenamedVirtualPath` and `replaceUrl`: [1976](../../../../src/webjet8/java/sk/iway/iwcm/editor/EditorDB.java#L1976), [2093](../../../../src/webjet8/java/sk/iway/iwcm/editor/EditorDB.java#L2093) | `documents.group_id` | CSV from `getSubgroupsIds(domain)` when multi-domain support is enabled. Used by an external-link UPDATE and a data-search SELECT. | More than 1000 groups in the domain, even when renaming a single page. Modern EditorService has separate JPA paths; migrating these legacy JDBC methods does not alter those. |
| High | `SpecSearch.addSpecSearchByUserEditable`: [312](../../../../src/main/java/sk/iway/iwcm/system/datatable/SpecSearch.java#L312) | `documents.group_id` | `int[] expandedGroupIds` from editable roots and their children at [306](../../../../src/main/java/sk/iway/iwcm/system/datatable/SpecSearch.java#L306), joined to CSV. | Permission-derived subtree can exceed 1000. An int[] overload avoids the join. The subsequent Criteria IN at [317](../../../../src/main/java/sk/iway/iwcm/system/datatable/SpecSearch.java#L317) currently targets forum through its caller, outside the four-table scope; the preliminary documents lookup remains in scope. |
| Conditional | `DocDB.getDocumentsByIds`: [1178](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L1178) | `documents.doc_id` | `List<Integer>` supplied by a public method; no upper bound. Joined with `Tools.join`, and empty lists currently become the SQL token `null`. | Use a numeric collection overload directly. No in-repository caller was found, so deployment usage is unconfirmed. Do not send the legacy literal string `"null"` through the strict numeric CSV parser; empty collections should generate `(1=0)`. |
| Conditional | New mirroring service, document selections: [600](../../../../src/main/java/sk/iway/iwcm/doc/mirroring/rest/MirroringService.java#L600), [637](../../../../src/main/java/sk/iway/iwcm/doc/mirroring/rest/MirroringService.java#L637) | `documents.doc_id` | `List<Integer>` differences between previous and new selections at [318](../../../../src/main/java/sk/iway/iwcm/doc/mirroring/rest/MirroringService.java#L318), converted to CSV by `listIdsToString`. | More than 1000 documents added or removed in one mapping update. Collection overload is the natural input. |

The existing POC at [HistoryDB:108](../../../../src/main/java/sk/iway/iwcm/doc/HistoryDB.java#L108) already reuses one generated condition across all three SQL construction sites. It is not an outstanding finding.

An additional joined-table predicate exists at [DocDB:1436](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L1436): `p.perex_group_id IN (...)` when `perexGroupUseJoin` is enabled. More than 1000 selected perex groups can fail the documents query even though the IN column belongs to `perex_group_doc`. Preserve the name-to-ID normalization contract during migration: the current loop resolves `pid` but appends `gn` at [1433](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocDB.java#L1433), so raw input names are not suitable for the numeric helper.

## Group and subtree workflows

`GroupsDB.getSubgroupsIds(int)` returns the root ID followed by every descendant ID as CSV; it has no size cap ([source](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L2957)). The tree overload used by these operations passes `maxDepth=null` and does not limit the number of descendants ([source](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L637)). Thus one selected root can already expand beyond 1000 IDs.

| Priority | Operation and SQL sites | Table / column | Input shape and trigger | Migration note |
| --- | --- | --- | --- | --- |
| High | `GroupsDB.deleteGroup`: [2805](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L2805), [2812](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L2812), [2856](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L2856) | `documents.group_id`, `groups.group_id` | `List<GroupDetails>` from `getGroupsTree` becomes a `StringBuilder` CSV. Permanent deletion or moving a subtree with over 1000 groups to trash. | Three in-scope SQL sites. Use the same complete IN condition for the whole operation. Related `groups_approve` cleanup at [2737](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L2737) uses the same list but is outside the requested table scope; it remains a workflow dependency. |
| High | `GroupsService.recoverGroupFromTrash`: [102](../../../../src/main/java/sk/iway/iwcm/editor/service/GroupsService.java#L102), [135](../../../../src/main/java/sk/iway/iwcm/editor/service/GroupsService.java#L135), [136](../../../../src/main/java/sk/iway/iwcm/editor/service/GroupsService.java#L136) | `documents.group_id`, `documents.doc_id` | Subtree CSV first; then `List<Integer> availableTrue` / `availableFalse`. A subtree can have over 1000 groups; even one group can have over 1000 pages to restore. | Three direct SQL sites. A collection overload avoids joining the page IDs only to parse them again. There is also an ORM history query between these statements; see below. |
| High | `GroupsDB.changeDomain`: [3553](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L3553), [3558](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L3558), [3559](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L3559) | `groups.group_id`, `documents.group_id` | `String groupIds` from the whole domain-root subtree at [3527](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L3527). | Domain changes and replacement of links/content can exceed the limit on large sites. Three statements share one ID set. |
| High | `GroupsDB.setLngToSubGroups`: [3900](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L3900) | `groups.group_id` | CSV from `getSubgroupsIds(parentGroupId)`. | Propagating language to a large subtree. Existing CSV method is sufficient. |
| High | `GroupsDB.setInternalAndMenuTypeToSubgroups`: [3935](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L3935) | `groups.group_id` | CSV from `getSubgroupsIds(parentGroupId)`. | Propagating menu/internal settings to a large subtree. |
| High | `GroupsDB.setPermissionToSubgroups`: [3968](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L3968) | `groups.group_id` | CSV from `getSubgroupsIds(parentGroupId)`. | Propagating permissions to a large subtree. |
| High | `GroupsDB.setTemplateToSubgroups` / `setTemplateToSubpages`: [4002](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L4002), [4031](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L4031) | `groups.group_id`, `documents.group_id` | Same subtree CSV shape. | Two statements in the template propagation workflow. |
| High | `WebpagesService.setAttributeToSubgroups`: [487](../../../../src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java#L487) | `groups.group_id` | CSV from `getSubgroupsIds` at [482](../../../../src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java#L482). | Shared implementation for several admin actions, including menu, navbar, sitemap, internal flag, permissions and new-page template; callers are at [GroupsRestController:376](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java#L376). Fixing this one SQL site covers multiple settings. |
| High | `GroupsRestController` forced domain change: [440](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java#L440), [446](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java#L446) | `groups.group_id` | Two CSV lists: the selected subtree at [435](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java#L435), plus an optional local System subtree at [293](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupsRestController.java#L293). | Both statements need replacement. |
| High | `WebpagesService.addSpecSearch`, default-page filter: [1184](../../../../src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java#L1184) | `groups.group_id` | `List<Integer> groupIds` expanded from a request filter such as `123*`, then joined to CSV. | Direct SQL runs before a second Criteria IN over the returned `default_doc_id` values at [1186](../../../../src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java#L1186). Fixing just the SQL does not settle the Criteria path. |
| High | New `doc.mirroring.rest.MirroringService.removeGroupsFromSync`: [835](../../../../src/main/java/sk/iway/iwcm/doc/mirroring/rest/MirroringService.java#L835), [838](../../../../src/main/java/sk/iway/iwcm/doc/mirroring/rest/MirroringService.java#L838) | `groups.group_id`, `documents.group_id` | `List<Integer> groupsTreeIds` collected from the expanded tree at [826](../../../../src/main/java/sk/iway/iwcm/doc/mirroring/rest/MirroringService.java#L826), then joined to CSV. | A small explicit selection can expand to thousands of descendants. Collection overload is appropriate. |
| Conditional | New `doc.mirroring.rest.MirroringService.addGroupToSync`: [875](../../../../src/main/java/sk/iway/iwcm/doc/mirroring/rest/MirroringService.java#L875) | `groups.group_id` | Filtered `List<Integer>` supplied to `addGroupToSync`; `listIdsToString` only joins it. | More than 1000 groups added to the same synchronization mapping. |
| High | Legacy `components.structuremirroring.MirroringService.clearSyncId`: [304](../../../../src/webjet8/java/sk/iway/iwcm/components/structuremirroring/MirroringService.java#L304), [305](../../../../src/webjet8/java/sk/iway/iwcm/components/structuremirroring/MirroringService.java#L305) | `groups.group_id`, `documents.group_id` | Full subtree `List<GroupDetails>` becomes `StringBuilder` CSV at [294](../../../../src/webjet8/java/sk/iway/iwcm/components/structuremirroring/MirroringService.java#L294). | The `rootGroupId == 0` branch uses unrestricted UPDATE without IN; only the subtree branch has this issue. |
| Conditional | Legacy `components.structuremirroring.MirroringService.checkRootGroupsConfig`: [264](../../../../src/webjet8/java/sk/iway/iwcm/components/structuremirroring/MirroringService.java#L264), [270](../../../../src/webjet8/java/sk/iway/iwcm/components/structuremirroring/MirroringService.java#L270) | `groups.group_id`, `documents.doc_id` | A single configuration line becomes `int[] mapping` at [236](../../../../src/webjet8/java/sk/iway/iwcm/components/structuremirroring/MirroringService.java#L236), then two CSV builders. | Requires over 1000 valid groups/default pages in one configuration line; unlike subtree operations it does not automatically expand descendants. |
| Conditional | `EditorService` trash repair: [1549](../../../../src/main/java/sk/iway/iwcm/editor/service/EditorService.java#L1549) | `groups.group_id` | `List<Integer>` from all rows where `group_id=parent_group_id`, converted to CSV at [1536](../../../../src/main/java/sk/iway/iwcm/editor/service/EditorService.java#L1536). | Requires over 1000 malformed groups. The statement also has an existing unbound `parent_group_id=?`; that separate issue is not fixed by splitting IN. |

The table above identifies **26 direct SQL construction sites** in group-related workflows, including their effects on `documents`. It is a count of expressions, not 26 independent user operations.

## Public queries, shared filters and JSPs

Static inspection of the source at 2026-11-03. These are SQL-generation risks, not claims that a particular installation currently contains more than 1000 values. Rows count logical paths, not every SQL execution; several paths generate both a result query and a count query. The inventory describes the pre-migration builders; migrated queries are verified without database connections.

### Confirmed unbounded literal or JDBC-placeholder lists

| Priority | SQL construction sites | Table / column | Source and growth trigger | Required handling |
|---|---|---|---|---|
| High | [NewsQuery.java:331](../../../../src/webjet8/java/sk/iway/iwcm/components/news/NewsQuery.java#L331), `:349` | `documents`, any `FieldEnum` column including `d.group_id` and `d.doc_id` | `List<Object>` emitted as one `?` per value, with no chunking. `NewsActionBean.java:450-475` expands selected groups and complete subtrees into `List<Integer>`; `:482` selects default document IDs; `:485` excludes default document IDs. `:253-329` builds duplicate-document exclusions, including master/slave mappings, and `:337-345` retains them in the request. A broad tree or many duplicate pages exceeds 1000 even if output is paged. | Parameterized `IN` and `NOT IN` builder, preserving argument order and values. This is raw JDBC, not an ORM expression that a provider might rewrite. Both data and count use the same builder (`NewsQuery.java:527`, `:706`). |
| High | [SearchAction.java:650](../../../../src/webjet8/java/sk/iway/iwcm/doc/SearchAction.java#L650), `:651` | `documents.d.doc_id NOT IN` | Numeric CSV `StringBuffer`; master/slave duplicate IDs accumulate from all cached documents in selected subtrees (`:140-148`, `:159-210`). There is no list-size bound. Applied to both results and total count. | Numeric exclusion helper. Split exclusions must use `AND`, or use `NOT` around the entire grouped positive condition. |
| High | [replaceall.jsp:90](../../../../src/main/webapp/admin/replaceall.jsp#L90), `:249` | `documents.group_id`, `groups.group_id` | Numeric CSV `String`; `getGroupIds` expands every supplied root with `getGroupsTree(..., true, true)` (`:19-45`). Search-and-replace across a large subtree grows past 1000. | Existing CSV overload fits after expansion. Both document and folder paths need migration. Preserve surrounding predicate grouping; the folder query already has separate `LIKE ... OR ...` terms (`:242`). |
| Medium | [DocFacade.java:56-69](../../../../src/webjet8/java/sk/iway/iwcm/doc/DocFacade.java#L56) | `documents.group_id` | `List<GroupDetails>` produced from options; recursive mode calls `GroupsDB.getRecursiveGroupsSqlIn` (`:97-109`) and then appends accessible group IDs directly. No cap. | Project `GroupDetails::getGroupId` into a numeric collection rather than making the DB helper aware of domain objects. |
| Medium | [AtrDB.java:526](../../../../src/webjet8/java/sk/iway/iwcm/doc/AtrDB.java#L526) | `documents.d.group_id` | Numeric CSV `String dirs`; `includeSub=true` builds all IDs from `getGroupsTree(dirId, true, false)` (`:468-484`). Single directory is safe, expanded trees are unbounded. | Existing CSV overload. |
| Medium | [news_calendar-ajax_utf-8.jsp:83](../../../../src/main/webapp/components/news-calendar/news_calendar-ajax_utf-8.jsp#L83) | `documents.group_id` | Numeric CSV `String groupIds`; `expandGroupIds=true` builds a CSV from each subtree (`:28-72`). No cap. | Existing CSV overload after legacy input normalization. |
| Medium | [KalendarUdalostiDB.java:107](../../../../src/webjet8/java/sk/iway/iwcm/calendar/KalendarUdalostiDB.java#L107) | `documents.d.group_id` | Caller-provided `String groupIds`, normalizing `+` to `,` (`:89`); no list cap. Note that this method's `includeSubdirectories` logic runs after SQL retrieval (`:306-316`), so it does not itself expand this SQL list. | Existing CSV overload after normalization. |
| Medium | [OfflineService.java:131](../../../../src/webjet8/java/sk/iway/iwcm/components/offline/OfflineService.java#L131) | `documents.group_id` | Numeric `StringBuilder searchGroups` accumulated from all non-internal groups in the selected tree (`:104-122`). No cap; all-site mode avoids this predicate (`:128-129`). | Existing CSV overload, retaining the existing all-site/no-group fallback. |
| Lower likelihood | [SearchAction.java:613](../../../../src/webjet8/java/sk/iway/iwcm/doc/SearchAction.java#L613), `:614` | `documents.root_group_l1` | `List<Integer> rootGroupIds` from every root group matching a domain (`:597-608`). No hard cap, but requires more than 1000 top-level roots for the domain. Both result and count paths are affected. | Numeric collection overload. |
| Lower likelihood | [AdminTools.java:305](../../../../src/webjet8/java/sk/iway/iwcm/common/AdminTools.java#L305), `:317` (two predicates) | `documents.d.root_group_l1`, `d.group_id NOT IN`, `d.root_group_l2 NOT IN` | `StringBuilder` CSV lists: all roots of a selected domain (`:292-302`), and all matching trash groups (`:308-314`). No hard cap; requires unusually many roots/trash folders. | CSV positive and exclusion helpers. Existing maximum output `size` does not cap filter-list size. |
| Lower likelihood | [CloudToolsForCore.java:757](../../../../src/webjet8/java/sk/iway/iwcm/common/CloudToolsForCore.java#L757) | `documents.root_group_l1` | `GroupsDB.expandRootGroupL1(rootGroupId)` produces CSV of all roots for the domain ([GroupsDB.java:4222-4234](../../../../src/main/java/sk/iway/iwcm/doc/GroupsDB.java#L4222)). No cap, but requires more than 1000 root groups. | Existing CSV overload. |

### Joined-table predicates in queries that read documents

These also cause a documents query to fail, although the IN column belongs to a joined table outside the four requested tables. They should be tracked separately if the scope means columns strictly belonging to the four tables.

| Site | Input and risk | Handling |
|---|---|---|
| [RelatedPagesDB.java:327](../../../../src/webjet8/java/sk/iway/iwcm/doc/RelatedPagesDB.java#L327) | `p.perex_group_id IN (...)` in a `documents` + `perex_group_doc` query. The public `getRelatedPagesByGroups` method accepts CSV and passes the entire list to the join branch (`:52`, `:98-101`); filtering positive numeric tokens (`:312-320`) does not cap count. More than 1000 selected perex groups is sufficient. `relatedPagesMaxSize` limits output, not predicate length. | Existing numeric CSV helper works. Lower likelihood than subtree expansion, but a real unbounded builder. |
| [AtrDB.java:510-515](../../../../src/webjet8/java/sk/iway/iwcm/doc/AtrDB.java#L510) | `dad.atr_group IN ('...', '...')` while reading documents and attributes. The `String group` argument is a comma-separated list of text group names, and no size cap exists. | Use a parameterized collection overload if migrated; the integer-ID CSV helper must not be applied to quoted text. This is additional evidence for placeholders, not a reason to build a SQL string-literal parser. |

### Safe, unrelated, or conditional cases

- `UnusedFilesCleaner.disable` already flushes every 100 indices ([UnusedFilesCleaner.java:101-115](../../../../src/webjet8/java/sk/iway/iwcm/filebrowser/UnusedFilesCleaner.java#L101)): the first batch contains one ID and subsequent batches at most 100. Its `documents` update at `:124` is safe for the 1000-item problem. Avoid flagging it solely from the SQL text.
- The JSP exclusions in `components/news/news.jsp:119`, `news-json.jsp:107`, `news_only_changed.jsp:105`, `newsGalleryFirst.jsp:108`, `components/forum/mb_list.jsp:103`, and basket product JSPs each exclude one `actualDocId`. They cannot exceed 1000 from this clause.
- [scratchpad.jsp:359](../../../../src/main/webapp/scratchpad.jsp#L359) and `:367` contain fixed three-ID lists and are not dynamic production builders.
- `KalendarUdalostiDB.java:118` is `IN (?)`, one expression. Binding a comma-separated string to one placeholder does not create an N-item list; it is not this limit issue. Do not mechanically treat it as a chunkable values list.
- `NewsQuery.java:597` has another dynamic ID list, but its query only reads `doc_atr` and `doc_atr_def`, outside the requested table scope. Track in a future wider audit; it may remain a separate failure after fixing the main news query.
- `AtrDB.java:93` is a text-group list in an attributes-only query, outside scope. The similar `:510` query is listed above because it reads `documents`.
- `SearchAction.java:130-137` restricts selected root paths using `file_name LIKE ... OR ...`; it is not an `IN` list. Arbitrary `searchWhereSql`/`where_sql` extension strings cannot be proven safe by this inspection, because their contents are supplied by callers/custom code.

## ORM findings: Criteria literals differ from bound collection parameters

The configured provider is EclipseLink ([V9JpaDBConfig:102](../../../../src/main/java/sk/iway/webjet/v9/V9JpaDBConfig.java#L102), [BaseJpaDBConfig:57](../../../../src/main/java/sk/iway/iwcm/system/spring/BaseJpaDBConfig.java#L57)). Both configurations select the Oracle target database when `DB_TYPE` is Oracle ([V9JpaDBConfig:163](../../../../src/main/java/sk/iway/webjet/v9/V9JpaDBConfig.java#L163), [BaseJpaDBConfig:67](../../../../src/main/java/sk/iway/iwcm/system/spring/BaseJpaDBConfig.java#L67)). [build.gradle:199](../../../../build.gradle#L199) requests `org.eclipse.persistence:eclipselink:2.7.+`; this audit inspected the locally cached **2.7.16** binary and matching sources. Spring Data JPA is **2.6.1** ([build.gradle:46](../../../../build.gradle#L46), [237](../../../../build.gradle#L237)). These observations do not establish which version/configuration is deployed at the client.

A database-free SQL-generation probe used the actual EclipseLink 2.7.16 jar, `OraclePlatform`, parameter binding and a minimal `documents.doc_id` mapping. It prepared and translated real EclipseLink SQL calls without connecting to any database. With **1001 IDs**:

| Construction | Generated SQL shape | Classification |
|---|---|---|
| JPA Criteria `ExpressionImpl.in(Collection)` | One `IN` containing 1001 `?` placeholders | Confirmed oversized generated list; vulnerable to Oracle's 1000-item limit |
| Criteria `.in(Collection).not()` | `NOT (column IN (...1001 placeholders...))` | Same risk |
| Bound collection parameter | Two `IN` lists joined by `OR` | Provider splits automatically on the inspected Oracle platform |
| Bound collection parameter with `NOT IN` | Two `NOT IN` lists joined by `AND` | Provider splits automatically |

All four shape assertions passed, including an independent rerun. Ongoing regression coverage is in `JpaToolsInConditionTest`, listed in the verification section. This tests SQL generation, not live Oracle execution.

Primary dependency-source evidence (from locally cached source JARs):

- `org.eclipse.persistence:eclipselink:2.7.16`, `org/eclipse/persistence/platform/database/OraclePlatform.java:1277-1279`: `getINClauseLimit()` returns 1000.
- Same artifact, `internal/databaseaccess/DatasourceCall.java:1338-1342,1392-1419`: collection-parameter expansion consults that limit, joins positive chunks with `OR` and negative chunks with `AND`.
- Same artifact, `internal/jpa/querydef/InImpl.java:54-57` and `ExpressionImpl.java:79-82,129-132`: the Criteria collection/varargs forms create literal collection expressions.
- Same artifact, `internal/expressions/ExpressionSQLPrinter.java:218-240`: a literal collection is emitted as a single list without consulting the platform IN limit.
- `org.springframework.data:spring-data-jpa:2.6.1`, `repository/query/StringQuery.java:621-655`: `InParameterBinding.prepare` converts arrays, including primitive `int[]`, to a `Collection`. Therefore array arguments in the repository `@Query(... IN :parameter)` methods below also take the bound-collection path.
- Same artifact, `repository/support/SimpleJpaRepository.java:408-432,929-933`: `findAllById` uses a `Collection` `ParameterExpression`, not a literal Criteria list.

### Criteria sites that require attention

The following application sites build literal list predicates, so the existing SQL-string helper cannot be directly inserted. None has a local 1000-item cap. The grouping below distinguishes plausible high-volume trees/history from less common large root/trash/user sets.

| Source | Table / predicate | Input and growth trigger | Priority |
|---|---|---|---|
| [WebpagesService.java:708](../../../../src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java#L708) (`getGroupIdCondition`) | `documents.group_id IN` | `List<Integer>` from `getGroupsTree(..., true, true)`; recursive page listing over >1000 folders | High |
| [WebpagesService.java:1176](../../../../src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java#L1176) (`addSpecSearch`) | `documents.group_id IN` | `List<Integer>` expanded from selected group IDs and `*` subtree selections | High |
| [WebpagesService.java:1186](../../../../src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java#L1186) | `documents.doc_id IN` | `List<Integer> defaultDocIds` from selected folders. The raw `groups` query at 1184 is a separate earlier failure point | High |
| `src/main/java/sk/iway/iwcm/search/SearchService.java:92,97` | `documents.group_id IN` | `Integer[]` of all descendants of the selected search root | High |
| `src/main/java/sk/iway/iwcm/search/SearchService.java:93,101` | `documents.doc_id IN` | `Integer[]` from user's explicitly editable pages | Medium |
| [BlogService.java:79](../../../../src/main/java/sk/iway/iwcm/components/blog/rest/BlogService.java#L79) | `documents.group_id IN` | `List<Integer>` containing one blogger's entire subtree or all bloggers' subtrees | High |
| [WebApproveRestController.java:143](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebApproveRestController.java#L143) | `documents_history.group_id IN` | `List<Integer> groupIdsInDomain` collected from pending history records; duplicates are not deduplicated, so >1000 pending rows can exceed the limit even with fewer distinct folders | High |
| [WebApproveRestController.java:161](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebApproveRestController.java#L161) | `documents_history.group_id` negated `IN` | `List<Integer> groupIdsInTrash`, also collected from pending history; duplicates can increase the count | High |
| [WebApproveRestController.java:165](../../../../src/main/java/sk/iway/iwcm/editor/rest/WebApproveRestController.java#L165) | `documents_history.history_id` negated `IN` | `List<Integer> skipHistoryId` for removed/trashed current pages | High |
| [WebpagesService.java:665](../../../../src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java#L665) | `documents.root_group_l1 IN` | `List<Integer>` of root folders sharing the current domain | Lower-frequency |
| `src/main/java/sk/iway/iwcm/editor/service/WebpagesService.java:676,679` | `documents.group_id` / `root_group_l2` negated `IN` | `List<Integer>` of trash folders across all groups | Lower-frequency |
| [SearchService.java:78](../../../../src/main/java/sk/iway/iwcm/search/SearchService.java#L78) | `documents.root_group_l1 IN` | `Integer[]` from configured/root domain groups | Lower-frequency |
| [SpecSearch.java:159](../../../../src/main/java/sk/iway/iwcm/system/datatable/SpecSearch.java#L159), called by `editor/service/WebpagesService.java:1116` | `documents.author_id IN` | `List<Integer>` of users matching a name/email search; >1000 matching users | Medium |

Other `SpecSearch` `.in(...)` occurrences require correct scope attribution: `addSpecSearchDocFullPath` at 282 currently has callers in media/forum, whose outer tables are outside this audit; its preliminary `documents` lookup has no IN. `addSpecSearchByUserEditable` has a confirmed raw `documents.group_id IN` at 312, but its final Criteria predicate at 317 currently targets forum through `DocForumRestController.java:138`, not `documents`. Do not count those outer Criteria predicates as extra affected document-table sites.

### Repository candidates not confirmed failures with this provider

These lists may grow large but are **bound collection parameters**, which the inspected EclipseLink Oracle path splits. Avoid automatically replacing them with native SQL. Confirm generated SQL against the client's exact EclipseLink version and binding configuration; retain provider-level regression coverage when upgrading dependencies.

| Repository source | Input | Representative source / caller |
|---|---|---|
| [DocHistoryRepository.java:27-28](../../../../src/main/java/sk/iway/iwcm/doc/DocHistoryRepository.java#L27) `deleteHistoryOnPublish` | `List<Integer>` history IDs | `EditorService.java:663`: old history of one document |
| Same file `:42-43` `updateActualHistory` | `List<Integer>` history IDs | `EditorService.java:670`: old history of one document |
| Same file `:47-48` `updateActual` | `List<Integer>` history IDs | `DomainIdUpdateService.java:454-455`: all previous versions of a migrated document |
| Same file `:71-72` `findByDocIdInActual` | `List<Integer>` document IDs | `GroupsService.java:102-105`: documents of a recovered subtree; even one folder may contain >1000 pages |
| Same file `:78-79` `updateActual` | `int[]` history IDs, converted by Spring Data | `DocPublishService.java:110-114`: all versions of a scheduled publication |
| [DocDetailsRepository.java:29-30](../../../../src/main/java/sk/iway/iwcm/doc/DocDetailsRepository.java#L29) `findAllByGroupIdIn` | `int[]` folder IDs, converted by Spring Data | `LinkCheckService.java:228`, `BlogService.java:35`; `Pageable.ofSize(500)` does not bound the input folder count |
| Same file `:49-50` `updateRedirect` | `int[]` folder IDs, converted by Spring Data | `EditorService.java:901`: all subgroups of a domain/root |
| Same file `:55-56` `findByDataLikeAndGroupIdIn` | `int[]` folder IDs, converted by Spring Data | `EditorService.java:945`: link replacement in subtree |
| Same file `:89-90` `deleteByDocIdIn` | `List<Long>` IDs | `EditorService.java:1519-1525`: multilingual/multigroup slave documents |
| [DatatableRestControllerV2.java:1711](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java#L1711) `findAllById` | `List<Long> requestedIds` | Generic **row reorder**, not ordinary listing/export; affects whichever entity controller inherits it, including scheduler if exposed. Spring Data uses a bound Collection parameter |

Names ending in `In` do not necessarily indicate a list: `DocHistoryRepository.getPublicableByDocIdIn` and `getHisotryIdsByDocIdIn` at 85-90 accept a single `Integer` and use equality, so they are not IN-list defects.


## groups_scheduler

No table-specific dynamic literal IN list was found in the inspected scheduler Java/JSP sources. These paths therefore do not currently need the CSV SQL helper:

- `GroupPublisher.deleteRecord` and `markAsPublished` use `schedule_id = ?` ([121](../../../../src/webjet8/java/sk/iway/iwcm/doc/GroupPublisher.java#L121), [126](../../../../src/webjet8/java/sk/iway/iwcm/doc/GroupPublisher.java#L126)).
- Scheduled publication selects IDs by date and then processes each ID separately ([141](../../../../src/webjet8/java/sk/iway/iwcm/doc/GroupPublisher.java#L141), [147](../../../../src/webjet8/java/sk/iway/iwcm/doc/GroupPublisher.java#L147)). Having over 1000 scheduled rows is not itself an IN-list problem here.
- Group history listing uses `group_id = ?` ([GroupPublisher:250](../../../../src/webjet8/java/sk/iway/iwcm/doc/GroupPublisher.java#L250)); one revision uses `schedule_id = ?` ([294](../../../../src/webjet8/java/sk/iway/iwcm/doc/GroupPublisher.java#L294)).
- `GroupSchedulerDtoRepository` declares single-group predicates ([12](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerDtoRepository.java#L12), [14](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerDtoRepository.java#L14), [17](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerDtoRepository.java#L17)). The normal controller listing invokes these paginated queries ([GroupSchedulerRestController:35](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerRestController.java#L35)).
- Synchronization reads scheduler rows using a date predicate ([SyncDirAction:1819](../../../../src/webjet8/java/sk/iway/iwcm/stripes/SyncDirAction.java#L1819)); rollback uses scalar IDs and a scalar subquery ([rollback_documents.jsp:54](../../../../src/main/webapp/components/sync/rollback_documents.jsp#L54), [112](../../../../src/main/webapp/components/sync/rollback_documents.jsp#L112)).

One indirect path was also checked: `GroupSchedulerRestController` inherits the generic `/row-reorder` endpoint, which calls `repo.findAllById(requestedIds)` for a `List<Long>` ([DatatableRestControllerV2:1695](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java#L1695), [1711](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java#L1711)). It rejects empty/null/duplicate IDs but has no count cap; the database lookup precedes column/reordering checks at [1730](../../../../src/main/java/sk/iway/iwcm/system/datatable/DatatableRestControllerV2.java#L1730). This does not imply the scheduler UI normally uses row reordering. The inspected Spring Data/EclipseLink path binds a collection and splits it automatically (see ORM evidence); this is not a confirmed unsplit-list defect in the inspected version. The scheduler entity maps its ID to `groups_scheduler.schedule_id` at [GroupSchedulerDto:17](../../../../src/main/java/sk/iway/iwcm/editor/rest/GroupSchedulerDto.java#L17).


## Implemented interface and shared implementation

The existing two-argument String method is retained. All four input forms use one private SQL renderer:

```java
String getInCondition(String columnName, String numericIds);
String getInCondition(String columnName, Collection<? extends Number> numericIds);
String getInCondition(String columnName, int[] numericIds);
String getInCondition(String columnName, Collection<?> values, List<Object> parameters);
```

| Input | Concrete reason |
| --- | --- |
| Numeric CSV | Existing HistoryDB/EditorDB and many subtree helpers already produce CSV. Preserve the strict integer-ID contract. StringBuilder callers can continue using `toString()`. |
| Numeric collection | `List<Integer>` in trash recovery and mirroring, root/default-page sets, and other already-typed inputs. Accepting `Collection` also covers sets without extra overloads. Validate integer values without lossy `Number.longValue()` conversion: reject fractional or out-of-range numbers. |
| `int[]` | `SpecSearch` subtree expansion and configuration parsing already return primitive arrays. This overload boxes IDs and delegates without a CSV round trip. |
| Collection plus parameter list | NewsQuery uses raw JDBC placeholders and `List<Object>`. Preserve the original value objects and parameter order; do not coerce them to integers. The caller keeps its existing binder. This also accommodates text values in joined attribute filters without parsing SQL string literals. |

Each overload also has a `getNotInCondition` counterpart. Public overloads adapt their inputs and delegate to **one private membership renderer** responsible for:

1. Validating the trusted column's syntax and resolving the effective `dbInClauseMaxItems` / Oracle cap.
2. Iterating values and opening/closing chunks.
3. Rendering a numeric literal or a `?` and appending its bound value.
4. Applying the correct operator and connector: `IN` chunks with `OR`, `NOT IN` chunks with `AND`, with outer parentheses in both cases.
5. Returning false for empty positive membership and true for empty exclusion. Bound lists retain null values and SQL NULL semantics; numeric ID inputs reject null items.

This gives one chunking/SQL-formatting implementation, not four copied loops. There is no current need for domain-object overloads, a quoted-SQL parser, `Object...`, or primitive `long[]`; map domain objects to IDs at the caller, and add long[] only when a concrete caller needs it. `Collection<Long>` is already covered. Adding unrelated overload types makes bare `getInCondition("id", null)` source calls ambiguous; callers should pass a typed value or an explicitly empty collection.

Criteria uses a separate representation in `JpaTools`:

```java
<T> Predicate in(Expression<T> field, Collection<? extends T> values, CriteriaBuilder builder);
<T> Predicate notIn(Expression<T> field, Collection<? extends T> values, CriteriaBuilder builder);
```

Both methods share one Criteria implementation and call `DB.getInClauseMaxItems()` for the same runtime policy as SQL: default 1000 on all engines, capped at 1000 on Oracle, with non-positive settings falling back to 1000. Criteria exclusion negates the complete positive predicate. SearchService adapts its `Integer[]` inputs with `Arrays.asList`.

`JpaTools.removePredicateWithName` recognizes positive OR groups containing only predicates for the requested field, so the blog can replace its default `groupId` filter after chunking. Exclusions, conjunctions and mixed-field groups remain intact. Bound repository methods keep their current representation.

Usage examples:

```java
String where = DB.getInCondition("d.group_id", groupIds);
String exclusions = DB.getNotInCondition("d.doc_id", duplicateIds);
List<Object> parameters = new ArrayList<>();
String names = DB.getInCondition("dad.atr_group", groupNames, parameters);
Predicate groups = JpaTools.in(root.get("groupId"), groupIds, builder);
```

Use trusted column names; identifier validation does not authorize a column or replace domain checks. Numeric inputs accept signed long integers, rejecting fractions, null items and overflow. A null/empty collection or blank numeric CSV is false for positive membership and true for exclusion. Existing callers that omit empty filters retain their guards. The bound overload appends values after existing parameters in placeholder order; the caller remains responsible for binding them. Bare `null` calls require a typed variable or cast because of the overloads.

## Migration coverage and compatibility checks

- History publishing in EditorDB; folder deletion/restoration, domain changes, inherited settings and both mirroring implementations.
- NewsQuery, DocDB and SearchAction result/count pairs; public helpers and the two audited JSPs; joined perex and attribute filters.
- All 16 Criteria expressions in WebpagesService, WebApproveRestController, SearchService, BlogService and SpecSearch. Repository queries already split by EclipseLink remain unchanged.
- `groups_scheduler` required no change. The separate unbound `parent_group_id=?` in EditorService trash repair is recorded in the baseline inventory and remains outside this IN-list fix.

Specific compatibility constraints:

- Keep `DB.getOnlyNumbersIn(ids, true)` where it currently filters groups by the active cloud domain ([DB:1066](../../../../src/webjet8/java/sk/iway/iwcm/DB.java#L1066)). `getInCondition` only builds a predicate; it does not replace permission/domain filtering.
- Preserve existing caller semantics for empty inputs: an omitted filter means something different from `(1=0)`. All-site fallbacks, intentional `-1` sentinels, `"null"` SQL literals and legacy `+`/semicolon/trailing-comma normalization need attention at the caller.
- Preserve one SQL statement for each existing bulk operation where possible. Splitting the predicate retains global sorting, counting and paging; splitting execution into independent queries can change those results and transaction behavior.
- Bound placeholders solve injection/typing concerns but do not lower the total number of statement parameters. Splitting the predicate addresses Oracle's per-list limit, not every database's total statement/resource limits.
- Deletion also reaches `groups_approve`, so that dependent statement was migrated. NewsQuery attribute loading and the attributes-only AtrDB query remain outside this four-table scope and need a future wider audit; their lists can still exceed 1000.
- Regression tests exercise actual caller SQL plus helper behavior: 0/1/999/1000/1001/2000/2001 IDs, positive/exclusion membership combined with AND/OR, bind order, custom limits, empty lists, validation, and preservation of domain filters. The Criteria tests translate real EclipseLink queries using its Oracle platform; live Oracle execution still requires an integration environment.

## Scope and verification limits

The search covered production Java/JSP/SQL/XML under `src/main` and `src/webjet8`, using table names, identifier lists, `.in(...)`, JPQL `IN :parameter`, repository methods and the generic news criteria builder. Dynamic candidates were traced to their list producers and obvious bounds; fixed singleton lists, the existing 100-item file-cleaner batches and the already-migrated HistoryDB were excluded from outstanding findings. Caller-provided SQL fragments and external customer modules are not statically enumerable here. The inventories are grouped by logical operation; repeated result/count/vendor branches are shown explicitly, not treated as independent incidents.

Verification is implemented in the following tests:

- `DBInConditionTest`: numeric adapters, exclusions, placeholders, ordering, invalid input, empty inputs and configuration limits across the supported engines.
- `HistoryDBTest`: published/unpublished history with 1001 IDs in the Oracle and shared non-Oracle SQL branches.
- `NewsQueryInConditionTest`: data/count queries, inclusion/exclusion, surrounding filters and JDBC argument order.
- `DocumentTreeInConditionTest`: one representative inherited menu-settings update over a 1001-folder subtree, original bind positions, active cloud domain filtering and an empty authorized set.
- `JpaToolsInConditionTest`: actual CriteriaQuery translation for 1/999/1000/1001/2000/2001 values, complete negation and grouping with other AND/OR filters, empty-set composition, null items, blog filter replacement and provider expansion of bound collections.

Run with the repository's Gradle test task and the relevant class filters. These tests require no database credentials or connection. Live Oracle publishing, subtree operations and paging remain integration smoke checks for the deployment environment; the inspected dependency version does not establish the client's deployed provider version.
