(function () {
  'use strict';

  const translations = {
    sk: {
      search: 'Vyhľadať', label: 'Hľadať v dokumentácii',
      title: 'Výsledky vyhľadávania',
      directoryTitle: 'V adresári {directory} a podadresároch',
      allDirectoriesTitle: 'Vo všetkých adresároch',
      searchAllDirectories: 'vyhľadať vo všetkých adresároch',
      searchCurrentDirectory: 'vyhľadať v aktuálnom adresári {directory}',
      close: 'Zavrieť', loading: 'Vyhľadávam…',
      empty: 'Neboli nájdené žiadne výsledky.', results: 'Počet výsledkov:',
      answer: 'Odpoveď AI', error: 'Vyhľadávanie sa nepodarilo. Skúste to znova.',
      loginRequired: 'Na vyhľadávanie sa prihláste.',
      forbidden: 'Nemáte oprávnenie na vyhľadávanie v tejto dokumentácii.',
      unavailable: 'Vyhľadávanie dokumentácie momentálne nie je dostupné.',
      open: 'Otvoriť dokument v novej karte'
    },
    cs: {
      search: 'Vyhledat', label: 'Hledat v dokumentaci',
      title: 'Výsledky vyhledávání',
      directoryTitle: 'V adresáři {directory} a podadresářích',
      allDirectoriesTitle: 'Ve všech adresářích',
      searchAllDirectories: 'vyhledat ve všech adresářích',
      searchCurrentDirectory: 'vyhledat v aktuálním adresáři {directory}',
      close: 'Zavřít', loading: 'Vyhledávám…',
      empty: 'Nebyly nalezeny žádné výsledky.', results: 'Počet výsledků:',
      answer: 'Odpověď AI', error: 'Vyhledávání se nezdařilo. Zkuste to znovu.',
      loginRequired: 'Pro vyhledávání se přihlaste.',
      forbidden: 'Nemáte oprávnění k vyhledávání v této dokumentaci.',
      unavailable: 'Vyhledávání dokumentace momentálně není dostupné.',
      open: 'Otevřít dokument v nové kartě'
    },
    en: {
      search: 'Search', label: 'Search documentation',
      title: 'Search results',
      directoryTitle: 'In {directory} and its subdirectories',
      allDirectoriesTitle: 'In all directories',
      searchAllDirectories: 'search all directories',
      searchCurrentDirectory: 'search the current directory {directory}',
      close: 'Close', loading: 'Searching…',
      empty: 'No results found.', results: 'Results:', answer: 'AI answer',
      error: 'Search failed. Please try again.',
      loginRequired: 'Sign in to search documentation.',
      forbidden: 'You do not have permission to search this documentation.',
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
   * Registers a localized search form and results dialog with the Docsify lifecycle.
   * Requires the viewer's basePath and useHashRouter globals to be initialized.
   *
   * @param {Object} hook - Docsify lifecycle registration API.
   * @param {function(function(): void): number} hook.mounted - Registers the form creation callback and returns the hook count.
   * @param {function(function(): void): number} hook.doneEach - Registers the route update callback and returns the hook count.
   * @param {Object} vm - Docsify instance.
   * @param {Object} vm.config - Viewer configuration.
   * @param {Object} [vm.config.markdownSearch] - Search configuration overrides.
   * @param {string} [vm.config.markdownSearch.contextPath] - Backend context path; inferred from the admin viewer path when omitted.
   * @param {string} [vm.config.markdownSearch.endpoint] - Search endpoint; defaults to the context path plus /rest/rag/markdown/search.
   */
  function searchPlugin(hook, vm) {
    const config = vm.config.markdownSearch || {};
    const adminPath = basePath.indexOf('/admin/docs/');
    const contextPath = (config.contextPath !== undefined ? config.contextPath :
      adminPath < 0 ? '' : basePath.substring(0, adminPath)).replace(/\/$/, '');
    const collectionPath = contextPath && basePath.startsWith(contextPath + '/') ? basePath.substring(contextPath.length) : basePath;
    const endpoint = config.endpoint || contextPath + '/rest/rag/markdown/search';
    let language = 'en';
    let labels = translations.en;
    let form, dialog, input, submit, status, answer, results, scopeLink;
    let searchCurrentDirectory = true;
    let activeRequest;

    /**
     * Updates the search language and document language from the current route.
     * Uses English labels for unsupported languages and refreshes existing controls.
     */
    function localize() {
      const route = useHashRouter ? window.location.hash.substring(1) : window.location.pathname.substring(basePath.length - 1);
      language = (route.match(/^\/([a-z]{2,3}(?:-[a-z0-9]{2,6})?)(?:\/|$)/i) || [null, 'en'])[1].toLowerCase();
      labels = translations[language] || translations.en;
      document.documentElement.lang = language;
      if (!form) return;
      form.querySelector('[for="documentation-query"]').textContent = labels.label;
      input.placeholder = labels.label;
      submit.textContent = labels.search;
      dialog.querySelector('h2').textContent = labels.title;
      dialog.querySelector('.documentation-search-close').textContent = labels.close;
      answer.querySelector('h3').textContent = labels.answer;
    }

    /**
     * Resolves a result to a same-origin viewer URL.
     * Uses sourcePath when present, otherwise validates and adapts the legacy URL route.
     * Derives the section ID from the last heading in the snippet's first-line breadcrumb.
     *
     * @param {Object} result - Search result returned by the backend.
     * @param {string|null} [result.url] - Backend viewer URL to validate when present.
     * @param {string|null} [result.sourcePath] - Markdown source path, including its configured root.
     * @param {string|null} [result.snippet] - Chunk excerpt beginning with its heading breadcrumb.
     * @returns {string|null} Absolute viewer URL, or null for missing, malformed, or out-of-scope paths.
     */
    function resultUrl(result) {
      const value = result.url;
      try {
        const breadcrumb = typeof result.snippet === 'string' ? result.snippet.split(/[\r\n]/, 1)[0] : '';
        const heading = breadcrumb.includes(' > ') && !breadcrumb.endsWith('…') ? breadcrumb.split(' > ').pop() : '';
        const section = heading.trim().replace(/[A-Z]+/g, text => text.toLowerCase())
          .replace(/<[^>]+>/g, '').replace(/[\u2000-\u206F\u2E00-\u2E7F\\'!"#$%&()*+,./:;<=>?@[\]^`{|}~]/g, '')
          .replace(/\s/g, '-').replace(/-+/g, '-').replace(/^(\d)/, '_$1');
        const withSection = target => {
          if (section) {
            if (target.hash.startsWith('#/')) {
              const route = new URL(target.hash.substring(1), window.location.origin);
              route.searchParams.set('id', section);
              target.hash = route.pathname + route.search;
            } else target.searchParams.set('id', section);
          }
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
          return withSection(new URL(basePath + (useHashRouter ? '#/' : '') + route, window.location.origin));
        }
        if (!url || !url.pathname.startsWith(basePath)) return null;
        // Validate legacy document routes, including the hash, before adapting the router format.
        const documentPath = decodeURIComponent(url.pathname + (url.hash.startsWith('#/') ? url.hash.substring(2).split('?')[0] : ''));
        if (!documentPath.startsWith(contextPath + '/') || documentPath.split('/').some(segment => segment === '.' || segment === '..' || /[\\\x00-\x1f\x7f]/.test(segment))) return null;
        if (!useHashRouter && url.hash.startsWith('#/')) {
          const route = new URL(url.hash.substring(1), window.location.origin);
          url.pathname += route.pathname.substring(1);
          url.search = route.search;
          url.hash = '';
        }
        return withSection(url);
      } catch (error) {
        return null;
      }
    }

    hook.mounted(function () {
      form = document.createElement('form');
      form.className = 'documentation-search';
      form.setAttribute('role', 'search');
      form.innerHTML = '<label for="documentation-query"></label>' +
        '<div class="documentation-search-input"><input id="documentation-query" type="search" maxlength="1000" required autocomplete="off">' +
        '<button type="submit"></button></div>';
      input = form.querySelector('input[type="search"]');
      submit = form.querySelector('button');
      const sidebar = document.querySelector('.sidebar');
      const heading = sidebar.querySelector('h1');
      sidebar.insertBefore(form, heading ? heading.nextSibling : sidebar.firstChild);

      dialog = document.createElement('dialog');
      dialog.className = 'documentation-search-dialog';
      dialog.setAttribute('aria-labelledby', 'documentation-search-title');
      dialog.innerHTML = '<div class="documentation-search-header"><h2 id="documentation-search-title"></h2>' +
        '<button type="button" class="documentation-search-close" autofocus></button></div>' +
        '<h3 class="documentation-search-subtitle"><span></span>, ' +
        '<a href="#" class="documentation-search-scope"></a></h3>' +
        '<p class="documentation-search-status" role="status" aria-live="polite"></p>' +
        '<section class="documentation-search-answer" hidden><h3></h3><div></div></section>' +
        '<ol class="documentation-search-results"></ol>';
      document.body.appendChild(dialog);
      status = dialog.querySelector('.documentation-search-status');
      answer = dialog.querySelector('.documentation-search-answer');
      results = dialog.querySelector('ol');
      scopeLink = dialog.querySelector('.documentation-search-scope');
      scopeLink.addEventListener('click', function (event) {
        event.preventDefault();
        event.stopPropagation();
        runSearch(!searchCurrentDirectory);
      });
      dialog.querySelector('button').addEventListener('click', function () { dialog.close(); });
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
        runSearch(true);
      });

      /**
       * Opens a fresh result view for the current query in the selected scope.
       * Cancels previous requests so results cannot cross between the two views.
       *
       * @param {boolean} currentDirectoryOnly - Whether to include the current directory filter and all its descendants.
       * @returns {Promise<void>} Resolves after rendering results or a localized error.
       */
      async function runSearch(currentDirectoryOnly) {
        const query = input.value.trim();
        if (!query) return;
        searchCurrentDirectory = currentDirectoryOnly;
        if (activeRequest) activeRequest.abort();
        const request = new AbortController();
        activeRequest = request;
        submit.disabled = true;
        results.replaceChildren();
        answer.hidden = true;
        answer.querySelector('div').textContent = '';
        status.textContent = labels.loading;
        if (!dialog.open) dialog.showModal();

        try {
          const route = (useHashRouter ? window.location.hash.substring(1) :
            window.location.pathname.substring(basePath.length - 1)).split(/[?#]/)[0];
          const directory = decodeURIComponent(route.substring(0, route.lastIndexOf('/') + 1));
          dialog.querySelector('.documentation-search-subtitle span').textContent = currentDirectoryOnly
            ? labels.directoryTitle.replace('{directory}', directory) : labels.allDirectoriesTitle;
          scopeLink.textContent = currentDirectoryOnly
            ? labels.searchAllDirectories : labels.searchCurrentDirectory.replace('{directory}', directory);
          dialog.scrollTop = 0;
          const url = new URL(endpoint, window.location.origin);
          url.search = new URLSearchParams({ query: query, language: language });
          if (currentDirectoryOnly) url.searchParams.set('directory', directory);
          const response = await fetch(url, { credentials: 'same-origin', signal: request.signal });
          if (!response.ok) {
            const message = response.status === 401 ? labels.loginRequired : response.status === 403 ? labels.forbidden :
              response.status === 503 ? labels.unavailable : labels.error;
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
            const snippet = document.createElement('div');
            snippet.className = 'documentation-search-snippet';
            snippet.innerHTML = renderSnippet(result.snippet || '');
            item.append(link, snippet);
            results.appendChild(item);
          });
          if (typeof data.answer === 'string' && data.answer.trim()) {
            answer.querySelector('div').textContent = data.answer;
            answer.hidden = false;
          }
          status.textContent = results.children.length ? labels.results + ' ' + results.children.length : labels.empty;
        } catch (error) {
          if (request.signal.aborted) return;
          const knownMessages = [labels.error, labels.loginRequired, labels.forbidden, labels.unavailable];
          status.textContent = knownMessages.includes(error.message) ? error.message : labels.error;
        } finally {
          if (activeRequest === request) {
            activeRequest = null;
            submit.disabled = false;
          }
        }
      }
    });

    hook.doneEach(function () {
      if (dialog && dialog.open) dialog.close();
      localize();
    });
  }

  window.$docsify.plugins = [].concat(window.$docsify.plugins || [], searchPlugin);
})();
