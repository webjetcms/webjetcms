(function () {
  'use strict';

  const translations = {
    sk: {
      search: 'Vyhľadať', label: 'Hľadať v dokumentácii', alternative: 'alebo',
      placeholder: 'Zadajte otázku…', deepWiki: 'Opýtať sa DeepWiki',
      question: 'Vaša otázka', languageName: 'Slovenčina',
      directoryTitle: 'V adresári {directory} a podadresároch',
      allDirectoriesTitle: 'Celá dokumentácia',
      searchAllDirectories: 'vyhľadať vo všetkých adresároch',
      searchCurrentDirectory: 'vyhľadať v aktuálnom adresári {directory}',
      close: 'Zavrieť', closeHint: 'Esc na zatvorenie', loading: 'Vyhľadávam v dokumentácii…',
      loadingHint: 'Hľadám relevantné dokumenty. Môže to chvíľu trvať.',
      emptyHint: 'Skúste otázku preformulovať alebo použiť všeobecnejší výraz.',
      expandSearch: 'Prehľadať celú dokumentáciu',
      empty: 'Neboli nájdené žiadne výsledky.', results: 'Nájdené dokumenty',
      answer: 'Odpoveď AI', codeExample: 'Ukážka kódu', error: 'Vyhľadávanie sa nepodarilo. Skúste to znova.',
      loginRequired: 'Na vyhľadávanie sa prihláste.',
      forbidden: 'Nemáte oprávnenie na vyhľadávanie v tejto dokumentácii.',
      rateLimited: 'Dosiahli ste limit vyhľadávaní. Skúste to znova neskôr.',
      unavailable: 'Vyhľadávanie dokumentácie momentálne nie je dostupné.',
      open: 'Otvoriť dokument v novej karte'
    },
    cs: {
      search: 'Vyhledat', label: 'Hledat v dokumentaci', alternative: 'nebo',
      placeholder: 'Zadejte otázku…', deepWiki: 'Zeptat se DeepWiki',
      question: 'Vaše otázka', languageName: 'Čeština',
      directoryTitle: 'V adresáři {directory} a podadresářích',
      allDirectoriesTitle: 'Celá dokumentace',
      searchAllDirectories: 'vyhledat ve všech adresářích',
      searchCurrentDirectory: 'vyhledat v aktuálním adresáři {directory}',
      close: 'Zavřít', closeHint: 'Esc pro zavření', loading: 'Vyhledávám v dokumentaci…',
      loadingHint: 'Hledám relevantní dokumenty. Může to chvíli trvat.',
      emptyHint: 'Zkuste otázku přeformulovat nebo použít obecnější výraz.',
      expandSearch: 'Prohledat celou dokumentaci',
      empty: 'Nebyly nalezeny žádné výsledky.', results: 'Nalezené dokumenty',
      answer: 'Odpověď AI', codeExample: 'Ukázka kódu', error: 'Vyhledávání se nezdařilo. Zkuste to znovu.',
      loginRequired: 'Pro vyhledávání se přihlaste.',
      forbidden: 'Nemáte oprávnění k vyhledávání v této dokumentaci.',
      rateLimited: 'Dosáhli jste limitu vyhledávání. Zkuste to znovu později.',
      unavailable: 'Vyhledávání dokumentace momentálně není dostupné.',
      open: 'Otevřít dokument v nové kartě'
    },
    en: {
      search: 'Search', label: 'Search documentation', alternative: 'or',
      placeholder: 'Enter your question…', deepWiki: 'Ask DeepWiki',
      question: 'Your question', languageName: 'English',
      directoryTitle: 'In {directory} and its subdirectories',
      allDirectoriesTitle: 'All documentation',
      searchAllDirectories: 'search all directories',
      searchCurrentDirectory: 'search the current directory {directory}',
      close: 'Close', closeHint: 'Esc to close', loading: 'Searching documentation…',
      loadingHint: 'Looking for relevant documents. This may take a moment.',
      emptyHint: 'Try rephrasing your question or using a broader term.',
      expandSearch: 'Search all documentation',
      empty: 'No results found.', results: 'Documents found', answer: 'AI answer', codeExample: 'Code example',
      error: 'Search failed. Please try again.',
      loginRequired: 'Sign in to search documentation.',
      forbidden: 'You do not have permission to search this documentation.',
      rateLimited: 'Search limit reached. Please try again later.',
      unavailable: 'Documentation search is currently unavailable.',
      open: 'Open document in a new tab'
    }
  };

  /**
   * Renders preview formatting with Docsify's existing Markdown parser.
   * Escapes source HTML and keeps only the labels of links and images.
   *
   * @param {string} markdown - Markdown excerpt returned by the search endpoint.
   * @returns {string} HTML containing the formatted excerpt.
   */
  function renderSnippet(markdown) {
    const renderer = new window.marked.Renderer();
    renderer.html = function (text) {
      const escaped = document.createElement('span');
      escaped.textContent = text;
      return escaped.innerHTML;
    };
    renderer.text = text => text.replace(/</g, '&lt;');
    renderer.link = (href, title, text) => text;
    renderer.image = (href, title, text) => text;
    return window.marked(markdown, { renderer: renderer, headerIds: false, highlight: null });
  }

  /**
   * Appends a safe preview, moving heading context above the excerpt and removing duplicate leading titles.
   * Leaves the original snippet intact for section-link resolution.
   *
   * @param {HTMLElement} item - Result content receiving the preview.
   * @param {Object} result - Search result to display.
   * @param {string} result.title - Document title already shown in the result link.
   * @param {string|null} [result.snippet] - Markdown excerpt, optionally prefixed with heading context.
   * @param {string|null} [result.sourcePath] - Source path used as a fallback breadcrumb.
   * @param {string} language - Current documentation language.
   */
  function appendPreview(item, result, language) {
    const snippet = document.createElement('div');
    snippet.className = 'documentation-search-snippet';
    snippet.innerHTML = renderSnippet(result.snippet || '');
    const first = snippet.firstElementChild;
    const prefix = first ? first.textContent.trim() : '';
    const headings = [result.title];
    let breadcrumbText = '';
    if (first && first.tagName === 'P' && !/[\r\n]/.test(prefix) &&
        (prefix === result.title || prefix.startsWith(result.title + ' > '))) {
      first.remove();
      if (prefix !== result.title) {
        headings.push(...prefix.split(' > '));
        breadcrumbText = prefix.split(' > ').join(' / ');
      }
    }
    while (snippet.firstElementChild && /^H[1-6]$/.test(snippet.firstElementChild.tagName) &&
           headings.includes(snippet.firstElementChild.textContent.trim())) {
      snippet.firstElementChild.remove();
    }
    if (!breadcrumbText && typeof result.sourcePath === 'string') {
      const languageRoot = '/' + language + '/';
      const start = result.sourcePath.indexOf(languageRoot);
      if (start >= 0) breadcrumbText = result.sourcePath.substring(start + languageRoot.length).split('/').slice(0, -1).join(' / ');
    }
    if (breadcrumbText) {
      const breadcrumb = document.createElement('p');
      breadcrumb.className = 'documentation-search-breadcrumb';
      breadcrumb.textContent = breadcrumbText;
      item.appendChild(breadcrumb);
    }
    item.appendChild(snippet);
  }

  /**
   * Creates a localized search form and results dialog for a mounted Docsify viewer.
   * Requires the viewer's basePath global and $docsify configuration to be initialized.
   * Reads optional contextPath and endpoint overrides from $docsify.markdownSearch.
   */
  function initializeSearch() {
    const config = window.$docsify.markdownSearch || {};
    const adminPath = basePath.indexOf('/admin/docs/');
    const contextPath = (config.contextPath !== undefined ? config.contextPath :
      adminPath < 0 ? '' : basePath.substring(0, adminPath)).replace(/\/$/, '');
    const collectionPath = contextPath && basePath.startsWith(contextPath + '/') ? basePath.substring(contextPath.length) : basePath;
    const endpoint = config.endpoint || contextPath + '/rest/rag/markdown/search';
    let language = 'en';
    let labels = translations.en;
    let form, dialog, input, submit, dialogInput, dialogSubmit, status, answer, results, scopeLink;
    let searchCurrentDirectory = true;
    let activeRequest;

    /**
     * Updates the search language and document language from the current route.
     * Uses English labels for unsupported languages and refreshes existing controls.
     */
    function localize() {
      const route = window.location.pathname.substring(basePath.length - 1);
      language = (route.match(/^\/([a-z]{2,3}(?:-[a-z0-9]{2,6})?)(?:\/|$)/i) || [null, 'en'])[1].toLowerCase();
      labels = translations[language] || translations.en;
      document.documentElement.lang = language;
      if (!form) return;
      form.querySelector('[for="documentation-query"]').textContent = labels.label;
      form.querySelector('.deepwiki-badge > span').textContent = labels.alternative;
      form.querySelector('.deepwiki-badge a span').textContent = labels.deepWiki;
      input.placeholder = labels.placeholder;
      submit.setAttribute('aria-label', labels.search);
      dialog.setAttribute('aria-label', labels.label);
      dialogSubmit.setAttribute('aria-label', labels.search);
      dialog.querySelector('.documentation-search-query-label').textContent = labels.question;
      dialog.querySelector('.documentation-search-close').setAttribute('aria-label', labels.close);
      dialog.querySelector('.documentation-search-footer').textContent = labels.closeHint;
      answer.querySelector('h3 span').textContent = labels.answer;
    }

    /**
     * Resolves a result to a same-origin viewer URL.
     * Maps sourcePath under the current collection or file:/docs/ to a viewer route.
     * For source paths outside those roots, keeps the supplied same-origin URL.
     * Without sourcePath, validates and adapts the legacy URL route under basePath.
     * Derives the section ID from the last heading in the snippet's first-line breadcrumb.
     *
     * @param {Object} result - Search result returned by the backend.
     * @param {string|null} [result.url] - Backend viewer URL to validate when present.
     * @param {string|null} [result.sourcePath] - Markdown source path, including its configured root.
     * @param {string|null} [result.snippet] - Chunk excerpt beginning with its heading breadcrumb.
     * @returns {string|null} Absolute viewer URL, or null when validation fails or no usable route is available.
     */
    function resultUrl(result) {
      const value = result.url;
      try {
        const breadcrumb = typeof result.snippet === 'string' ? result.snippet.split(/[\r\n]/, 1)[0] : '';
        const heading = breadcrumb.includes(' > ') && !breadcrumb.endsWith('…') ? breadcrumb.split(' > ').pop() : '';
        const section = heading.trim().replace(/[A-Z]+/g, text => text.toLowerCase())
          .replace(/<[^>]+>/g, '').replace(/[\u2000-\u206F\u2E00-\u2E7F\\'!"#$%&()*+,./:;<=>?@[\]^`{|}~]/g, '')
          .replace(/\s/g, '-').replace(/-+/g, '-').replace(/^(\d)/, '_$1');
        /**
         * Converts legacy hash URLs to history routes and applies the derived section ID.
         *
         * @param {URL} target - Viewer URL to update in place.
         * @returns {string} Absolute history URL with the section ID when available.
         */
        const withSection = target => {
          if (target.hash.startsWith('#/')) {
            const route = new URL(target.hash.substring(1), window.location.origin);
            target.pathname += route.pathname.substring(1);
            target.search = route.search;
            target.hash = '';
          }
          if (section) target.searchParams.set('id', section);
          return target.href;
        };
        if (value != null && (typeof value !== 'string' || !value)) return null;
        const url = value == null ? null : new URL(value, window.location.origin);
        if (url && url.origin !== window.location.origin) return null;
        if (result.sourcePath != null) {
          const path = result.sourcePath;
          if (typeof path !== 'string' || !/\.md$/i.test(path)) return null;
          const viewerRoot = path.startsWith('file:/docs/') ? 'file:/docs/' : collectionPath;
          if (!path.startsWith(viewerRoot)) return url ? withSection(url) : null;
          const segments = path.substring(viewerRoot.length).split('/');
          // Validate before URL parsing so traversal is rejected instead of normalized away.
          if (segments.some(segment => !segment || segment === '.' || segment === '..' || /[\\\x00-\x1f\x7f]/.test(segment))) return null;
          const route = segments.join('/').replace(/\.md$/i, '').replace(/(^|\/)README$/, '$1')
            .split('/').map(encodeURIComponent).join('/');
          return withSection(new URL(basePath + route, window.location.origin));
        }
        if (!url || !url.pathname.startsWith(basePath)) return null;
        // Validate legacy document routes, including the hash, before adapting the router format.
        const documentPath = decodeURIComponent(url.pathname + (url.hash.startsWith('#/') ? url.hash.substring(2).split('?')[0] : ''));
        if (!documentPath.startsWith(contextPath + '/') || documentPath.split('/').some(segment => segment === '.' || segment === '..' || /[\\\x00-\x1f\x7f]/.test(segment))) return null;
        return withSection(url);
      } catch (error) {
        return null;
      }
    }

    form = document.createElement('form');
    form.className = 'documentation-search';
    form.setAttribute('role', 'search');
    form.innerHTML = '<label for="documentation-query"></label>' +
      '<div class="documentation-search-input"><input id="documentation-query" type="search" maxlength="1000" required autocomplete="off">' +
      '<button type="submit" class="documentation-search-submit"><i class="ti ti-search" aria-hidden="true"></i></button></div>' +
      '<div class="deepwiki-badge"><span></span>' +
      '<a href="https://deepwiki.com/webjetcms/webjetcms" target="_blank" rel="noopener noreferrer">' +
      '<span></span><i class="ti ti-arrow-up-right" aria-hidden="true"></i></a></div>';
    input = form.querySelector('input[type="search"]');
    submit = form.querySelector('button');
    const sidebar = document.querySelector('.sidebar');
    const heading = sidebar.querySelector('h1');
    sidebar.insertBefore(form, heading ? heading.nextSibling : sidebar.firstChild);

    dialog = document.createElement('dialog');
    dialog.className = 'documentation-search-dialog';
    dialog.innerHTML = '<div class="documentation-search-header">' +
      '<label for="documentation-dialog-query" class="documentation-search-query-label"></label>' +
      '<button type="button" class="documentation-search-close">' +
      '<i class="ti ti-x" aria-hidden="true"></i></button>' +
      '<form class="documentation-search-dialog-form" role="search">' +
      '<input id="documentation-dialog-query" type="text" enterkeyhint="search" maxlength="1000" required autocomplete="off" autofocus>' +
      '<button type="submit" class="documentation-search-dialog-submit"><i class="ti ti-search" aria-hidden="true"></i></button></form></div>' +
      '<div class="documentation-search-body"><p class="documentation-search-subtitle">' +
      '<i class="ti ti-folder-search" aria-hidden="true"></i><span class="documentation-search-scope-label"></span>' +
      '<a href="#" class="documentation-search-scope" hidden></a></p>' +
      '<section class="documentation-search-answer" hidden>' +
      '<h3><i class="ti ti-sparkles" aria-hidden="true"></i><span></span></h3><div></div></section>' +
      '<div class="documentation-search-status" role="status" aria-live="polite"></div>' +
      '<ol class="documentation-search-results" role="list"></ol>' +
      '<p class="documentation-search-footer"></p></div>';
    document.body.appendChild(dialog);
    dialogInput = dialog.querySelector('#documentation-dialog-query');
    dialogSubmit = dialog.querySelector('.documentation-search-dialog-submit');
    status = dialog.querySelector('.documentation-search-status');
    answer = dialog.querySelector('.documentation-search-answer');
    results = dialog.querySelector('ol');
    scopeLink = dialog.querySelector('.documentation-search-scope');
    scopeLink.addEventListener('click', function (event) {
      event.preventDefault();
      event.stopPropagation();
      runSearch(!searchCurrentDirectory, dialogInput.value);
    });
    dialog.querySelector('.documentation-search-close').addEventListener('click', function () { dialog.close(); });
    dialog.addEventListener('close', function () {
      if (activeRequest) activeRequest.abort();
      input.focus();
    });
    results.addEventListener('click', function (event) {
      // Docsify's history router otherwise handles same-origin links in the current tab.
      if (event.target.closest('a')) event.stopPropagation();
    });
    localize();

    form.addEventListener('submit', function (event) {
      event.preventDefault();
      runSearch(true, input.value);
    });
    dialog.querySelector('form').addEventListener('submit', function (event) {
      event.preventDefault();
      runSearch(searchCurrentDirectory, dialogInput.value);
    });

    /**
     * Displays search progress or an empty result with an action to broaden its scope.
     *
     * @param {boolean} loading - Whether a search is running.
     */
    function showSearchState(loading) {
      status.classList.add('documentation-search-state');
      status.innerHTML = '<i class="ti ti-' + (loading ? 'sparkles' : 'search') + '" aria-hidden="true"></i>' +
        '<div class="documentation-search-state-content"><strong></strong><p></p>' + (loading
          ? '<div class="documentation-search-progress" aria-hidden="true"></div>' +
            '<div class="documentation-search-skeleton" aria-hidden="true"><span></span><span></span><span></span></div>'
          : '<button type="button" class="documentation-search-expand" hidden></button>') + '</div>';
      status.querySelector('strong').textContent = loading ? labels.loading : labels.empty;
      status.querySelector('p').textContent = loading ? labels.loadingHint : labels.emptyHint;
      if (loading) return;
      const expand = status.querySelector('.documentation-search-expand');
      expand.textContent = labels.expandSearch;
      expand.hidden = !searchCurrentDirectory;
      if (searchCurrentDirectory) scopeLink.hidden = true;
      expand.addEventListener('click', function () { runSearch(false, dialogInput.value); });
    }

    /**
     * Opens a fresh result view for the current query in the selected scope.
     * Cancels previous requests so results cannot cross between the two views.
     * Ignores blank queries and omits the directory filter at the documentation or language root.
     *
     * @param {boolean} currentDirectoryOnly - Whether to include the current directory filter and all its descendants.
     * @param {string} value - Question entered in the sidebar or dialog.
     * @returns {Promise<void>} Resolves after rendering results or a localized error, or when skipped or aborted.
     */
    async function runSearch(currentDirectoryOnly, value) {
      const query = value.trim();
      if (!query) return;
      searchCurrentDirectory = currentDirectoryOnly;
      if (activeRequest) activeRequest.abort();
      const request = new AbortController();
      activeRequest = request;
      submit.disabled = true;
      dialogSubmit.disabled = true;
      dialogInput.disabled = true;
      results.replaceChildren();
      input.value = query;
      dialogInput.value = query;
      answer.hidden = true;
      answer.querySelector('div').textContent = '';
      showSearchState(true);
      if (!dialog.open) dialog.showModal();

      try {
        const route = window.location.pathname.substring(basePath.length - 1);
        const directory = decodeURIComponent(route.substring(0, route.lastIndexOf('/') + 1));
        const isRoot = !directory || directory === '/' || directory.toLowerCase() === '/' + language + '/';
        searchCurrentDirectory = currentDirectoryOnly && !isRoot;
        dialog.querySelector('.documentation-search-scope-label').textContent = (searchCurrentDirectory
          ? labels.directoryTitle.replace('{directory}', directory) : labels.allDirectoriesTitle) +
          ' · ' + (translations[language] ? labels.languageName : language);
        scopeLink.hidden = isRoot;
        scopeLink.textContent = searchCurrentDirectory
          ? labels.searchAllDirectories : labels.searchCurrentDirectory.replace('{directory}', directory);
        dialog.scrollTop = 0;
        dialog.querySelector('.documentation-search-header').scrollTop = 0;
        const url = new URL(endpoint, window.location.origin);
        url.search = new URLSearchParams({ query: query, language: language });
        if (searchCurrentDirectory) url.searchParams.set('directory', directory);
        const response = await fetch(url, { credentials: 'same-origin', signal: request.signal });
        if (!response.ok) {
          let message = labels.error;
          switch (response.status) {
            case 401:
              message = labels.loginRequired;
              break;
            case 403:
              message = labels.forbidden;
              break;
            case 429:
              message = labels.rateLimited;
              break;
            case 503:
              message = labels.unavailable;
              break;
          }
          throw new Error(message);
        }
        const contentType = (response.headers.get('content-type') || '').split(';')[0].trim().toLowerCase();
        if (contentType !== 'application/json' && !contentType.endsWith('+json')) {
          throw new Error(labels.unavailable);
        }
        const data = await response.json();
        if (!Array.isArray(data.results)) throw new Error(labels.error);
        if (request.signal.aborted) return;
        data.results.forEach(function (result) {
          const href = resultUrl(result);
          if (!href && result.url != null) return;
          const item = document.createElement('li');
          const link = document.createElement(href ? 'a' : 'span');
          if (href) {
            link.href = href;
            link.target = '_blank';
            link.rel = 'noopener noreferrer';
            link.title = labels.open;
          }
          link.textContent = result.title;
          if (href) {
            const icon = document.createElement('i');
            icon.className = 'ti ti-arrow-up-right';
            icon.setAttribute('aria-hidden', 'true');
            link.appendChild(icon);
          }
          item.innerHTML = '<i class="ti ti-file-text documentation-search-document-icon" aria-hidden="true"></i><div></div>';
          const content = item.querySelector('div');
          content.appendChild(link);
          appendPreview(content, result, language);
          results.appendChild(item);
        });
        if (typeof data.answer === 'string' && data.answer.trim()) {
          const content = answer.querySelector('div');
          const formatted = typeof data.answerHtml === 'string' && !!data.answerHtml.trim();
          content.classList.toggle('documentation-search-answer-formatted', formatted);
          if (formatted) {
            // The endpoint allows only formatting tags without attributes, links or images.
            content.innerHTML = data.answerHtml;
            content.querySelectorAll(':scope > ol').forEach(list => list.setAttribute('role', 'list'));
            content.querySelectorAll('pre').forEach(function (example) {
              const panel = document.createElement('div');
              panel.className = 'documentation-search-example';
              const title = document.createElement('strong');
              title.textContent = labels.codeExample;
              example.replaceWith(panel);
              panel.append(title, example);
            });
          } else content.textContent = data.answer;
          answer.hidden = false;
        }
        status.classList.remove('documentation-search-state');
        status.textContent = results.children.length ? labels.results + ' ' : labels.empty;
        if (results.children.length) {
          const count = document.createElement('span');
          count.className = 'documentation-search-count';
          count.textContent = results.children.length;
          status.appendChild(count);
        } else if (answer.hidden) showSearchState(false);
      } catch (error) {
        if (request.signal.aborted) return;
        const knownMessages = [labels.error, labels.loginRequired, labels.forbidden, labels.rateLimited, labels.unavailable];
        status.classList.remove('documentation-search-state');
        status.textContent = knownMessages.includes(error.message) ? error.message : labels.error;
      } finally {
        if (activeRequest === request) {
          activeRequest = null;
          submit.disabled = false;
          dialogSubmit.disabled = false;
          dialogInput.disabled = false;
        }
      }
    }

    document.addEventListener('documentation-rendered', function () {
      if (dialog && dialog.open) dialog.close();
      localize();
    });
  }

  // Async loading can finish before or after Docsify mounts.
  if (document.querySelector('.sidebar')) initializeSearch();
  else document.addEventListener('documentation-mounted', initializeSearch, { once: true });
})();
