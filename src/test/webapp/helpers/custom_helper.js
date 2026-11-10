const { Helper } = codeceptjs;

class CustomWebjetHelper extends Helper {

  _before() {
    // CodeceptJS 3.6 retains failed named sessions after their contexts are closed.
    const playwright = this.helpers['Playwright'];
    playwright.activeSessionName = '';
    playwright.sessionPages = {};
    // Keep only failure metadata so blank pages can be diagnosed without logging headers or form data.
    this._diagnostics = [];
    if (!this._diagnosticContexts) this._diagnosticContexts = new WeakSet();
    if (!this._diagnosticPages) this._diagnosticPages = new WeakSet();
    const observe = page => {
      if (this._diagnosticPages.has(page)) return;
      this._diagnosticPages.add(page);
      const record = item => {
        this._diagnostics.push(item);
        if (this._diagnostics.length > 30) this._diagnostics.shift();
      };
      const urlPath = value => {
        try {
          const url = new URL(value);
          return ['http:', 'https:'].includes(url.protocol) ? url.origin + url.pathname.replace(/;jsessionid=[^/;]*/gi, '') : null;
        } catch {
          return null;
        }
      };
      page.on('requestfailed', request => {
        const url = urlPath(request.url());
        if (url) record({ url, failure: request.failure()?.errorText });
      });
      page.on('response', response => {
        const url = urlPath(response.url());
        if (url && response.status() >= 400) record({ url, status: response.status() });
      });
      page.on('pageerror', error => {
        let message = error.message.replace(/https?:\/\/[^\s)]+/g, urlPath);
        for (const [name, value] of Object.entries(process.env)) {
          if (value && /PASSWORD|TOKEN|SECRET|API_KEY/.test(name)) message = message.split(value).join('<redacted>');
        }
        record({ error: message });
      });
    };
    if (playwright.page) observe(playwright.page);
    if (playwright.browserContext && !this._diagnosticContexts.has(playwright.browserContext)) {
      this._diagnosticContexts.add(playwright.browserContext);
      playwright.browserContext.on('page', observe);
    }
  }

  /**
   * Returns a cookie matching the current page URL, including HttpOnly session cookies.
   * @param {string} name - cookie name
   * @returns {Promise<object>} matching cookie
   * @throws {Error} when the current page has no matching cookie
   */
  async grabCookieForCurrentUrl(name) {
    const { browserContext, page } = this.helpers['Playwright'];
    const cookies = await browserContext.cookies(page.url());
    const cookie = cookies.find(cookie => cookie.name === name);
    if (!cookie) throw new Error(`Cookie "${name}" is missing for the current page URL`);
    return cookie;
  }

  /**
   * Metoda vrati stlpce vo formate .json
   * @param {string} dataTable objekt pre datatabulku
   */
  async getDataTableColumns(dataTableName) {
    const { page } = this.helpers['Playwright'];
    const window = await page.evaluateHandle(() => window);
    const table = await window.getProperty(dataTableName);
    const DATA = await table.getProperty('DATA');
    const columns = await DATA.getProperty('columns');
    const columnsToJson = await columns.jsonValue();
    try {
      if (columnsToJson) return columnsToJson;
    } catch(err) {
      console.log(err);
    }
  }

  async getDataTableId(dataTableName) {
    const { page } = this.helpers['Playwright'];
    const window = await page.evaluateHandle(() => window);
    const table = await window.getProperty(dataTableName);
    const DATA = await table.getProperty('DATA');
    let id = await DATA.getProperty("id");
    /*id = "xxx"+id;
    //nevedel som z jshandle objektu ziskat priamo value, takze takto skaredo
    const prefix = "xxxJSHandle@";
    let i = id.indexOf(prefix);
    console.log("id="+id+" i="+i);
    if (i==0) id = id.substring(prefix.length);
    console.log("datatable "+dataTableName+" id="+id);*/
    return id;
  }

  /**
   * Metoda otestuje ci existuje na stranke element pokial ano klikne naneho.
   * @param {string} selector html element ktory sa ma testovat
   */
  async clickIfVisible(selector) {
    const helper = this.helpers['Playwright'];
    const numVisible = await helper.grabNumberOfVisibleElements(selector);

    try {
      if (numVisible) {
        await helper.click(selector);
        return true;
      } else {
        console.log(`Skipping operation, element ${selector} is not visible.`);
        return false;
      }
    } catch (err) {
      console.log(err);
    }
  }

  /**
   * Metoda na kontrolu inputov ci su spristupnene alebo nie
   * @param {string} selector HTML element na ktorom sa robi kontrola
   */
  async verifyDisabled(selector) {
    const helper = this.helpers['Playwright'];
    try {
      const isDisabled = await helper.grabAttributeFrom(selector, 'disabled');
      return isDisabled;
    } catch (err) {
        console.log(err);
    }
  }

  /**
   * Ziska pocet zaznamov v datatabulke
   * POZOR: mozno kvoli bugu to dropne session (cookies)
   */
  async getTotalRows() {
    const I = this.helpers['Playwright'];
    var text = await I.grabTextFrom("div.dt-info");
    if (text.indexOf("(")>=0) {
      //Záznamy 1 až 6 z 6 (vyfiltrované spomedzi 25 záznamov)
      text = text.substring(0, text.indexOf("(")-1).trim();
    }
    //console.log("TEXT=", text);
    var i = text.lastIndexOf(" ");
    var count = parseInt(text.substring(i+1).replace(",", ""));
    //console.log("count=", count);
    return count;
  }

  async _failed() {
    const I = this.helpers['Playwright'];
    console.log('Browser failure diagnostics: ' + JSON.stringify(this._diagnostics || []));
    try {
      if (I.page && !I.page.isClosed()) console.log('Browser initialization state: ' + JSON.stringify(await I.page.evaluate(() => ({
        path: location.pathname.replace(/;jsessionid=[^/;]*/gi, ''), state: document.readyState,
        mainContentLength: document.querySelector('#main-content')?.innerHTML.length,
        tables: [...document.querySelectorAll('table')].map(table => ({ id: table.id, initialized: Boolean(table.closest('.dt-container')) })),
        domReadyAvailable: typeof window.domReady !== 'undefined',
        translationTimestamp: window.webjetTranslationService?.storage.getStorageItem('lastmodified'),
        serverTranslationTimestamp: window.propertiesLastModified
      }))));
    } catch {
      console.log('Browser initialization state unavailable');
    }
    const logs = await I.grabBrowserLogs();
    //const errors = logs.map(l => ({ type: l.type(), text: l.text() })).filter(l => l.type === 'error');
    const length = logs.length;
    //print last 10 logs, because they are accumulating
    let start = length - 10;
    if (start < 0) start = 0;
    for (let i=start; i<length; i++) {
      let log = logs[i];
      console.log(log.type()+": "+log.text());
    }
  }
}

module.exports = CustomWebjetHelper;
