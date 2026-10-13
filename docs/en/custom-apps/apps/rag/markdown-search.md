# Searching in Markdown documentation

Semantic search and RAG can also work with Markdown files deployed in a web application or stored in a separate folder on the server. WebJET CMS documentation and client documentation have separate roots. The Markdown index is common to all domains. The search respects the access rights of the current user and the selected language. By default, Docsify searches the current directory and all its subdirectories across the accessible configured roots. The directory restriction can be disabled in the dialog.

## Deploying Docsify

Copy the Docsify shell from the `docs` folder into the extracted WAR application: `index.jsp`, `index.html`, `style.css`, and `assets/`. The shell uses the original history routing; nested placement requires the configuration modification described below. Add language folders with content to the same root, for example:

```text
/admin/docs/webjetcms/
    index.jsp
    index.html
    style.css
    assets/
    sk/
        README.md
        _sidebar.md
        _navbar.md
        redactor/...
    en/...
/admin/docs/orange/
    index.jsp
    index.html
    style.css
    assets/
    sk/...
```

The original shell is set for public documentation under `/latest/`. In this deployment, open for example `/latest/sk/` ; navigating from the documentation root selects the browser language (`sk`, `cs`, otherwise `en`). The search field has Slovak, Czech and English texts; for other language codes, it will use the English translation.

For nested deployments, such as `/admin/docs/webjetcms/`, it is not enough to simply copy the files. In the copy of `index.html`, set `basePath`, `<base href>` and the paths to static files according to the location of the documentation. Keep `routerMode: 'history'` and set the rewrite of document addresses, such as `/admin/docs/webjetcms/sk/redactor/`, to the corresponding `index.html` on the server. The welcome-file `index.jsp` itself does not replace this service. Existing public addresses `/latest/...` and versioned addresses are still served by the original `404.jsp`. For a separate documentation domain, the REST service must be available on the same origin; a static server alone or Tomcat without a WebJET backend does not provide search.

The original configuration uses the root `/` for `127.0.0.1`, `localhost`, `iwcm.interway.sk`, and `docs.interway.sk`. The links `/sk/...`, `/en/...`, and `/cs/...` remain at these addresses. The server must return `index.html` when opening a document address, for example by using a fallback in `docsify serve`. The existing `404.jsp` does not provide this handling for unversioned addresses. For a different host or location, adjust the shell configuration; the path is not automatically determined from the opened address.

The shell continues to load Docsify libraries and their plugins from existing CDNs. You don't need to copy `node_modules`, tests, or documentation generation tools into the WAR.

## Configuration

Use the same [vector database and AI provider](semantic-search/README.md) as for web search. Set documentation roots and file path mappings globally, without domain restrictions:

| Variable | Example | Meaning |
| --- | --- | --- |
| `ragSemanticSearchEnabled` | `true` | Turns on semantic search. |
| `ragMarkdownFolders` | `/admin/docs/client,file:/docs/sk/admin/users` | A global list of documentation roots for all domains, separated by commas or newlines. Defaults to empty. The `file:` prefix indicates a folder on the server; `file:/docs` is the documentation alias set via `symlinkTranslate`. |
| `ragMarkdownSearchRequireLogin` | `true` | By default, it requires a logged in user; it does not have to be an administrator. A value of `false` will allow anonymous searching even in the configured `file:` and `/admin/` roots. |
| `symlinkTranslate` | `/docs/\|/srv/documentation/webjetcms/` | Global mapping of the `/docs/` alias to the absolute documentation folder. Required for `file:/docs` and its subfolders. |
| `ragAnswerAllowed` | `true` | Enables automatic generation of RAG answers when searching. If set to `false`, only found documents will be displayed. Requires configured answer assistant. |

Roots without the prefix `file:` are paths in the expanded web application **without the application context path**: for URL `/cms/admin/docs/webjetcms` set to `/admin/docs/webjetcms`. Only a subfolder can be indexed, for example `/admin/docs/webjetcms/sk/admin/users`. The backend for links finds the nearest parent Docsify `index.html` or `index.jsp` ; if missing, it uses the indexed root. Paths `..`, encoded web paths, and `WEB-INF` /`META-INF` are not allowed.

For server files, use an absolute path prefixed with `file:`, such as `file:/srv/manuals`. The form `file:///srv/manuals` normalizes to `file:/srv/manuals` ; relative paths and `file://server/...` are not supported. The path `/Users/.../docs` alone indicates the web path. The WebJET process must have read access to the folder; symbolic links in its contents are not processed.

Alias ​​`file:/docs` and its subfolders require global mapping `/docs/` in `symlinkTranslate`. Use the mapping for the given environment and keep the other mappings on separate lines:

```text
/docs/|/srv/documentation/webjetcms/
```

Locally, the target can point to the `docs` folder of your project, for example `/home/developer/webjetcms/docs/`. This must be an existing absolute folder. The alias `file:/docs/sk/admin/users` then points to its subfolder `sk/admin/users` ; the path remains limited to the mapped root. Without a mapping, the alias fails with an error. The location is not derived from the working directory or the project structure. The logical path remains in the configuration, queue, and chunks, for example `file:/docs/sk/admin/users/README.md`. Changing the physical location of the same documentation does not require new indexes. A separate `sourceRoot` is not saved.

**Divide your documentation into language folders**, such as `sk`, `en`, or `cs`. The language is determined for each file from the nearest parent folder with a recognized name in the entire path, including the configured root. The languages ​​are determined by the global variable `languages` ; `sk`, `en`, and `cs` are always supported. The name `sk-manual` is not considered a language. Files without a language folder are skipped during indexing.

At root `file:/docs/sk/admin/users`, the direct file `README.md` is given the language `sk`. At root `file:/docs`, all language subfolders are processed. Direct `.md` files are also indexed; adjacent parts of the documentation are not searched. The language is not selected in the dialog and is not stored in the queue, only in chunks.

`ragMarkdownSearchRequireLogin` of the current domain requires any logged in user by default; a value of `false` will also allow anonymous searches in all configured roots, including `/admin/` and `file:`. Blocked paths and protection of individual web files by user groups are still respected. Login checks are performed before calling embeds and generating an AI response.

The `/rest/rag/markdown/search` API is outside the admin path. Setting up a login does not change the protection of the Docsify browser itself, files, or index management. For regular users or anonymous access, deploy content to an accessible web path, such as `/docs/webjetcms/`, or to a separate documentation domain with a WebJET backend on the same origin. Files under `/admin/` are still subject to admin protection.

Indexing `file:` itself will not make the content available over HTTP. The results contain the title, snippet, and full path of `sourcePath`, but `url` is `null`. Docsify for `file:/docs/...` creates a link to the current browser, which must serve the contents of the mapped folder `/docs/`. Other file roots display the title without a link.

Markdown uses existing text splitting, hybrid search, result similarity, and RAG response settings. Chunks and vectors of type `MARKDOWN` are stored with `domainId = 0` ; source identity is also calculated with domain `0`. Web pages remain of type `DOCUMENT` with their domain ID.

The queue will keep the positive ID of the domain that created the request. Its `RAG-EMB-INDEX` assistant, provider configuration, and consumption statistics will be used in processing. The search uses the `RAG-EMB-SEARCH` assistant and the current domain's consumption, as well as the RAG response. You can edit assistants in the regular list of AI assistants for the respective domain.

The search assistant must use the same provider and model as the stored embeddings. Domains with the same combination share existing vectors. For a different combination, a corresponding index must be created; the dimension must correspond to the common vector database.

After setting the shell for nested deployment, the search context is derived from its `basePath`: for `/cms/admin/docs/...`, `/cms` is used. For a different location, for example `/cms/manual/`, add the following to `window.$docsify`:

```javascript
markdownSearch: {
    contextPath: '/cms'
}
```

The plugin also allows you to set `endpoint` ; if `basePath` is correct, it does not need to be entered under `/admin/docs/`. This search setting does not replace the configuration of the shell and server address rewriting. The search roots are determined exclusively by the backend according to `ragMarkdownFolders` and access rights. The browser sends the language of the current Docsify page and an optional restriction to the current directory according to the selected link in the dialog.

### Docsify local preview

In the `docs` folder, start the static server:

```sh
npm run docs
```

The preview of `http://127.0.0.1:3000` does not provide a REST service to WebJET. It may return HTML instead of JSON for a search request; the plugin will then display a message stating that the search is unavailable.

For both search and AI responses, set the global mapping `/docs/` in `symlinkTranslate` to the absolute path to the folder `docs` provided by the static server. Leave the static server running and in the second terminal in the folder `docs` start the local proxy server. If WebJET is available on `http://iwcm.interway.sk`, use:

```sh
npm run docs:rag -- --backend http://iwcm.interway.sk
```

Open `http://iwcm.interway.sk:3001/sk/`. If `ragMarkdownSearchRequireLogin=true`, first log in via the website login page at `http://iwcm.interway.sk` in the same browser and profile; a regular user account is sufficient. Administrators can also use `/admin/`. The same hostname will allow the existing login cookie to be used on a different port. With a value of `false`, no login is required. The built-in VS Code browser has a separate session and will not take over the login from the regular browser.

For the default production REST service `https://docs.webjetcms.sk`, just:

```sh
npm run docs:rag
```

Open `http://127.0.0.1:3001/sk/`. The documentation will be loaded from a local static server and the search will use `https://docs.webjetcms.sk/rest/rag/markdown/search`. The local preview does not share the production domain's login cookies, so this method requires anonymous search to be enabled on the backend (`ragMarkdownSearchRequireLogin=false`).

You can also open the preview via a custom hostname pointing to `127.0.0.1`, for example `http://docs.interway.sk:3001/sk/`. The REST call destination is specified by the `--backend` parameter with a default value of `https://docs.webjetcms.sk`. The `--docs` parameter has a default value of `http://127.0.0.1:3000` and `--port` has a default value of `3001`.

The proxy listens only on the local interface, displays a preview from a static server, and sends search requests to WebJET. In the HTML preview, it sets the documentation root to `/`, so even if you use a different backend host name, you use the history address `/sk/...`. The documentation root is not specified at startup. New searches in a subdirectory are limited to the current page directory and its subdirectories. For example, from the address `http://iwcm.interway.sk:3001/sk/admin/README`, `directory=/sk/admin/` is sent. The **search in all directories** link in the dialog will repeat the question without this filter. In the language root, for example `/sk/`, the filter is not sent and the link is not displayed. Documentation files, CORS settings, or login authentication are not changed.

## Planned indexation

In **Settings → Automated Tasks** add a task:

```text
sk.iway.iwcm.rag.service.MarkdownIndexCronTask
```

Set the desired time, for example daily at 02:00: minute `0`, hour `2`, other time fields `*`. You can perform the first index population by manually running this task. This task scans the files directly. The existing `RagIndexCronTask` also handles manual requests for indexing and removal of Markdown documents from the web page queue.

The first optional parameter of the task is the configured documentation root, the second is the name of the domain whose assistant and statistics will be used. Separate the parameters with `|`. An empty first parameter means all globally configured roots. The language is determined from the path for each file, so one task can process multiple languages. For a local folder and a specific domain, use:

```text
file:/docs/sk/admin/users|www.example.sk
```

To process all roots with the same domain, use:

```text
|www.example.sk
```

In MultiWeb/cloud mode, the domain name is mandatory and must belong to an existing domain. The task itself does not have a domain context, so the consumption cannot be derived from the user who created it. In single domain mode, the second parameter can be omitted; the domain `1` is used. The root `file:/docs/sk/admin/users` itself or empty parameters thus remain valid only in single domain mode. The folder must be specified exactly in the global `ragMarkdownFolders`.

For existing jobs, remove the older language parameter and set the parameters in the order `koreň|doména`. For example, change `sk|file:/docs/sk/admin/users` or `sk||file:/docs/sk/admin/users` to `file:/docs/sk/admin/users|www.example.sk`. If the job only had the parameter `sk`, set `|www.example.sk` to process all roots. Replace the example with the name of the actual domain whose consumption you want to record.

At each startup:

1. Searches the selected root or configured roots, including their subfolders. Infers the language from the path of each file. Processes `.md`, skips files without a language folder, navigation files (`_sidebar.md`, `_navbar.md`, `_coverpage.md`, `_footer.md`, `_404.md`, `404.md`), hidden folders, and `node_modules`. Does not follow symbolic links in the content.
2. Compares the SHA-256 of the content and the current text portion with the stored index. Does not create new embeddings if the finished document is unchanged. Changing the provider, model, dimension, or text partition will trigger new processing.
3. When changing a file, it reuses unchanged embeddings based on the hashes of individual parts. It preserves headings, tables, code examples, and all HTML comments, including comments outside of code examples. It omits YAML front matter.
4. After successfully reading the entire root, the index of no longer existing files and older indexes of files without a recognized language folder will be removed. Renaming will be treated as a new file and deleting the original. The empty file will be removed from the index.

If the provider fails before saving, the original index remains. Incomplete or failed parts are retried on the next run. If the root is missing or cannot be read, its original records are not bulk deleted. Removing the root from the configuration immediately disables its search; it does not delete the saved data itself. After changing the dimension and deleting the embeddings, run the job again.

For multiple application nodes, schedule the task on one node. Concurrent executions within a single process are blocked. You can find errors in the server log under classes `MarkdownIndexCronTask` and `MarkdownIndexService`.

## Index management in administration

In **Settings → Semantic Index → ​​Markdown Documents**, select the configured root or subfolder and set the inclusion of subfolders. The dialog will take this range, display the counts, and queue the indexing or removal to be processed by the `RagIndexCronTask` task. Indexing only counts files with a recognized language; re-indexing also updates the language of the chunks and can reuse unchanged embeddings. Removal includes all indexes in the selected range, regardless of language, as well as records of files that have already been removed. For details, see [Semantic Index Management Guide](../../../redactor/apps/semantic-search/embedding-chunks.md).

During deployment, database updates `autoupdate-webjet9.xml` must occur, which will extend the queue identifiers to `BIGINT` and add source file information. The language is not stored in the queue and remains part of the chunks. Access to the new card and its REST services is controlled by the existing `embeddingChunks` right.

Both the queue and chunks use only the full logical path `sourcePath`, such as `file:/docs/sk/admin/users/README.md` ; they do not store a separate `sourceRoot`. When processing the queue, the root is verified against `ragMarkdownFolders` and the document identifier. The `sourcePath` field allows 1537 characters. Existing Markdown requests and chunks with relative paths are not automatically converted; remove them before re-indexing.

Queued requests are replaced for the selected shared resource regardless of the domain that created them. The new request will retain the current user's domain. Deleting it will also cancel any pending indexing of the same resource from another domain.

When migrating from an older version:

- Requeue pending Markdown requests with `domainId = 0` or with a source identity calculated with a non-null domain. The domain in the queue must indicate the owner of the consumption, the source identity is calculated with the domain `0`.
- Common chunks and vectors with `domainId = 0` remain valid. Markdown chunks with non-zero domain are not used; re-index the documentation and then remove the old chunks. Changing `domainId` via SQL alone is not enough.
- Verify language folders. Older indexing requests without a recognized language will remain in the queue with an error; either edit the path and requeue them or delete the requests. A successful scheduled index will also remove older chunks without a language folder.

## REST search

```http
GET /rest/rag/markdown/search?query=How%20to%20edit%20a%20page&language=en
```

The `query` and `language` parameters are required. All accessible roots configured via `ragMarkdownFolders` in the selected language are searched, or limited by the `directory` parameter. The documentation root is not sent in the request. `query` can be 1 to 2000 characters. The AI ​​response generation is automatically controlled by the `ragAnswerAllowed` configuration variable; the `answer` request parameter is not used. The API returns a maximum of 10 documents:

```json
{
  "results": [
    {
      "title": "Page editor",
      "url": "/admin/docs/webjetcms/#/en/redactor/webpages/",
      "sourcePath": "/admin/docs/webjetcms/en/redactor/webpages/README.md",
      "snippet": "The page editor allows...",
      "score": 0.82
    }
  ],
  "answer": "Open the page editor..."
}
```

The optional parameter `directory` limits the search to the current page directory, including all subdirectories. It contains the language and both leading and trailing slashes, for example `/sk/admin/`:

```http
GET /rest/rag/markdown/search?query=users&language=sk&directory=%2Fsk%2Fadmin%2F
```

The backend translates the directory into a path in each accessible root. For a root of `file:/docs`, it searches under `file:/docs/sk/admin/` ; for a root of `/admin/docs/webjetcms`, it searches under `/admin/docs/webjetcms/sk/admin/`. If only a deeper root is configured, such as `file:/docs/sk/admin/users`, only that root is searched. Neighboring directories are not included. The filter is applied before sorting and limiting results in both vector and full-text search, and also limits the context of the AI ​​response. Without `directory`, the directory is not restricted. The language in the path must match the parameter `language` ; relative paths and `..` segments are not allowed.

`answer` is plain text, or `null` if `ragAnswerAllowed` is disabled or the response could not be constructed from the available context. The `answerHtml` field preserves the safe formatting of the response: paragraphs, highlighting, lists, tables, code samples, and expandable details. It does not contain attributes, links, images, or executable content. RAG gets only accessible parts from the common index for accessible roots and the selected language, including the name and source link. Response generation uses the assistant and the current domain settings. Results are not cached in the browser (`Cache-Control: no-store`). Invalid parameters return HTTP 400. If login is required and the user is not logged in, the API returns HTTP 401. Access to all configured roots is blocked returns HTTP 403.

Search uses the common [SPAM protection](../../../install/config/spam-protection.md) of the search application. The `spamProtectionTimeout-search` variable determines the minimum interval between requests (default 10 seconds) and `spamProtectionHourlyLimit-search` the number of requests per hour (default 200). The limits are calculated per IP address within a single application instance and are shared with regular search. They also apply to logged-in users. When the limit is exceeded, the API returns HTTP 429 before calling embeddings or generating an AI response. Docsify displays a message about the limit; it does not automatically retry the request.

Docsify displays a custom search box instead of Google Custom Search without an AI answer toggle. Results and AI answer enabled via `ragAnswerAllowed` are displayed in a dialog; clicking on the result name opens the corresponding page in a new tab and keeps the dialog open. For `file:/docs/...` resources, the full path `sourcePath` is converted to a link to the current documentation browser, even if only a subfolder is indexed. Other file roots without a public address are displayed as text. The dialog can also be closed with the Escape key.

The preview of the found part of the document preserves Markdown formatting: headings, bold, italics, lists, quotes, and code snippets. The `snippet` field contains up to 350 characters of the original Markdown text with line breaks preserved and, when truncated, the `…` character. The browser renders it with the existing Docsify parser; the headings are compact in size in the preview. The repeated name at the beginning of the preview is omitted. If the preview begins with the name and path to the section, the path is displayed separately below the link, and the matching opening headings are not repeated. The link to the relevant section is preserved. The HTML from the source is displayed as text, and only their text description is preserved for links and images. Changing the display does not require re-indexing.

The dialog header contains a question in an editable field. After editing, you can start a new search by pressing Enter or the magnifying glass icon at the end of the field, without closing the dialog. The current scope and language are preserved, and the question is also updated in the sidebar. Below the field are the search scope and language. The AI ​​answer is in a light purple tab above the **Documents Found** list with the number of results displayed. It preserves the assistant's formatting: procedures have highlighted step numbers, fields can be in a compact table, and code samples are in separate panels. The assistant can insert longer optional examples into the drop-down details. If `answerHtml` is missing, the original text is displayed. The new default assistant uses numbered steps, highlighted field names, tables with field descriptions, and short code samples for procedures. Existing assistants retain saved instructions; the same style can be set by editing them. Documents are in separate tabs with an icon, preview, and path to the section or source directory. If the answer is not available, the AI ​​tab is not displayed. The question remains visible during loading, with empty results, and on error. When scrolling through the results, the question header and close button remain in place.

A new search in a subdirectory will be limited to the current page directory and all its subdirectories: `/latest/sk/admin/README` and `/latest/sk/admin/` will send `/sk/admin/`. The **search in all directories** link will repeat the same query without `directory` ; the opposite link will reset the directory filter. In the root directory of a language, for example `/latest/sk/` or `/latest/sk/README`, **All documentation · Slovak** will be displayed, the `directory` filter will not be sent, and the link to expand the scope will not be displayed. The search remains limited to the selected language and accessible roots. Switching the scope will preserve the query and language, replace the results and the AI ​​answer, and will not change the open page. The links will work even with empty results.

### Server search cache

The backend stores the searched chunks and the AI ​​answer in `Cache` for 30 minutes. For the cache key, it lowercases the question and removes the diacritics, so that for example `Ďalšia úloha` and `dalsia uloha` use the same record. Search and answer generation get the original question. Records are separated by domain, language, resulting search paths, and application context path. Reusing a record does not extend its validity.

If the search does not return any chunks, the result is not cached. This means that the next request will repeat the search even after a temporary outage of the embedding provider or vector database.

If a saved chunk no longer exists in the database after re-indexing or deletion, the backend discards the saved results and response and reruns the search. This ensures that updated documents are available even if the same query is repeated before 30 minutes have passed.

With each request, access to resources and the current value `ragAnswerAllowed` are re-verified. When AI responses are disabled, the saved response is not returned either. When enabled, the missing response is generated from the saved accessible chunks without repeating the search. If the list of accessible chunks changes, the previous response is not used. Unsuccessful response generation is not saved and the next request can repeat it. SPAM protection also applies when using a cache; the browser continues to not save responses.

## Verifying changes

Backend regression tests:

```sh
./gradlew test --tests 'sk.iway.iwcm.rag.*'
```

Interface test with real Docsify and simulated REST response as well as local proxy server test:

```sh
node --test docs/tests/*.test.cjs
```

Tests require Node.js 20 or later and the `docs/` and `src/test/webapp/` dependencies installed, including the Playwright browser. When deploying, also verify actual indexing, embedding provider, and RAG responses on the target database.

Run the new tab and queued dialogs test against local WebJET after loading the current Java classes and translations:

```sh
cd src/test/webapp
npm run all tests/rag/markdown-embedding-chunks.js
```

The test uses simulated Markdown queue and table responses, so it doesn't send requests to the AI ​​provider. It validates column metadata and access control in the running application.
