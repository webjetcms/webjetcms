const { I } = inject();
const DT = require("./DT");
const DTE = require("./DTE");
const Apps = require("./Apps");
const i18n = require("./i18n");

/**
 * Funkcie pre pracu s Dokumentami
 */

module.exports = {

  /**
   * Highlight element on page, by adding class 'screenshotHighlight'.
   * @param {*} selector
   */
  highlightElement(selector) {
    if (typeof selector != "undefined" && selector != null && selector != "") {
      I.say("Highlighting element");
      I.executeScript((selector) => {
          document.querySelector(selector).classList.add('screenshotHighlight');
      }, selector);
      I.wait(1);
    }
  },

  /**
   * Unhighlight element on page, by removing class 'screenshotHighlight'.
   * @param {*} selector
   */
  unhighlightElement(selector) {
    if (typeof selector != "undefined" && selector != null && selector != "") {
      I.say("un-Highlighting element");
      I.executeScript((selector) => {
          document.querySelector(selector).classList.remove('screenshotHighlight');
      }, selector);
      I.wait(1);
    }
  },

  screenshot(screenshotFilePath, width, height, selectorToHighlight) {
      this.screenshotElement(null, screenshotFilePath, width, height, selectorToHighlight);
  },

  screenshotElement(selector, screenshotFilePath, width, height, selectorToHighlight) {
      var windowResized = false;
      if (typeof width != "undefined" && width != null && typeof height != "undefined" && height != null) {
        I.say("resizing window");
        //60=vyska toolbaru okna
        I.resizeWindow(width, height + 60);
        windowResized = true;
      }

      var path = "../../../docs/" + I.getConfLng() + screenshotFilePath;
      //If platform is windows, edit path (or screens will not be saved)
      if(process.platform == "win32") {
        path = path.replace(/\//gi, '\\');
      }
      I.wait(2);
      this.highlightElement(selectorToHighlight);

      if (typeof selector != "undefined" && selector != null && selector != "") {
        I.saveElementScreenshot(selector, path);
      }
      else {
        I.saveScreenshot(path);
      }

      this.unhighlightElement(selectorToHighlight);

      I.say("windows resized=" + windowResized);
      if (windowResized) I.wjSetDefaultWindowSize();
  },

  screenshotAppEditor(docId, path, callback, width, height) {
      var windowResized = false;
      if (typeof width != "undefined" && width != null && typeof height != "undefined" && height != null) {
        I.say("resizing window");
        //60=vyska toolbaru okna
        I.resizeWindow(width, height + 60);
        windowResized = true;
      }

      if (typeof docId != "undefined" && docId != null && docId>0) {
        I.amOnPage("/admin/v9/webpages/web-pages-list/?docid="+docId);
        DTE.waitForEditor();
        I.wait(5);
      }

      I.switchTo('.cke_wysiwyg_frame.cke_reset');
      I.switchTo(locate("iframe.wj_component").first());
      I.click("a.inlineComponentButton.cke_button");

      I.wait(5);

      if (typeof callback == "function" && callback != null) {
        I.switchTo();
        I.switchTo(".cke_dialog_ui_iframe");
        I.switchTo("#editorComponent");
        callback(this, I, DT, DTE, Apps);
      }

      this.screenshot(path);
      I.switchTo();

      if (windowResized) I.wjSetDefaultWindowSize();
  },

  async compareScreenshotElement(selector, screenshotFileName, width, height, tolerance) {
      if (typeof tolerance == "undefined" || tolerance == null) tolerance = 1;

      //screenshot file name can't start with /
      if (screenshotFileName.indexOf("/")==0) screenshotFileName = screenshotFileName.substring(1);

      var windowResized = false;
      if (typeof width != "undefined" && width != null && typeof height != "undefined" && height != null) {
        I.resizeWindow(width, height);
        windowResized = true;
        I.wait(2);
      }

      let path = "../../../build/test/screenshots/actual/";
      //If platform is windows, edit path (or screens will not be saved)
      if(process.platform == "win32") {
        path = path.replace(/\//gi, '\\');
      }

      if (typeof selector != "undefined" && selector != null) I.saveElementScreenshot(selector, path + screenshotFileName);
      else I.saveScreenshot(path + screenshotFileName);

      if (windowResized) I.wjSetDefaultWindowSize();

      let result = await I.checkVisualDifferences(screenshotFileName, {
        tolerance: tolerance
      });

      I.say("Visual diff is: "+result.difference+"%");
  },

  switchDomain(domain) {
      I.clickCss("div.js-domain-toggler div.bootstrap-select button");
      I.wait(1);
      I.click(locate('.dropdown-item').withText(domain));
      I.waitForElement("#toast-container-webjet", 10);
      I.waitForElement(".toastr-buttons button.btn-primary", 10);
      I.waitForText(i18n.get("You need to reload the page to change the domain."), 10, ".toastr-message");
      I.wait(0.5);
      var btn = locate("#toast-container-webjet .toastr-buttons button.btn-primary").withText(i18n.get("Confirm"));
      I.waitForElement(btn);
      I.click(btn);
      I.say("Domain switched to: "+domain+" DO NOT FORGET TO LOGOUT AFTER THIS SCENARIO otherwise you will be logged in with wrong domain");
      DT.waitForLoader();
  },

  /**
   * Set configuration value
   * @param {*} name
   * @param {*} value
   */
  setConfigValue(name, value) {
    I.amOnPage("/admin/v9/settings/configuration/");
    I.clickCss("button.buttons-create");
    DTE.waitForEditor("configurationDatatable");
    I.fillField("#DTE_Field_name", name);
    I.fillField("#DTE_Field_value", value);
    DTE.save();

  },

  /**
   * reset editorType force mode to pageBuilder (default)
   */
  resetPageBuilderMode() {
    I.amOnPage("/admin/v9/webpages/web-pages-list/");
    I.executeScript(function () {
        window.WJ.setAdminSetting("editorTypeForced", "pageBuilder");
    });
  },

  /**
   * Close toastr notification
   */
  notifyClose() {
    I.forceClickCss("div.toast-container .toast-close-button");
  },

  /**
   * Check text on toast notify and close it
   * @param {String} text
   */
  notifyCheckAndClose(text) {
    I.see(text, "div.toast-container div.toast");
    this.notifyClose();
  },

  /**
   * Open editor_component dialog in ckeditor
   */
  editorComponentOpen() {
    I.switchTo();
    I.waitForElement(".cke_wysiwyg_frame.cke_reset", 10);
    I.wait(1);
    I.switchTo('.cke_wysiwyg_frame.cke_reset');
    I.wait(1);
    I.waitForElement("iframe.wj_component", 10);
    I.wait(3);
    I.clickCss("iframe.wj_component");
    I.wait(3);

    I.switchTo();
    I.switchTo(".cke_dialog_ui_iframe");
    I.switchTo("#editorComponent");
  },

  /**
   * Click on OK button in editor_component dialog in ckeditor
   */
  editorComponentOk() {
    I.switchTo();
    I.clickCss("a.cke_dialog_ui_button_ok");
    I.wait(6);
  },

  /**
   * Scroll to element
   * @param {*} selector
   */
  scrollTo(selector) {
    I.executeScript(({selector}) => {
      window.scrollbarMain.scrollIntoView($(selector)[0])
    }, {selector});
    I.wait(1);
  },

  /**
   * On iwcm.interway.sk host change domain. Before click on link grab current URL:
   * let currentUrl = await I.grabCurrentUrl();
   * @param {*} baseUrl
   */
  async fixLocalhostUrl(baseUrl) {
    let currentUrl = await I.grabCurrentUrl();
    I.say("Fixing localhost URL, baseUrl URL: "+baseUrl+", current URL: "+currentUrl);
    if(baseUrl.includes("iwcm.interway.sk") || baseUrl.includes("localhost")) {
        //get URL part after domain
        I.amOnPage(currentUrl.substring(currentUrl.indexOf("/", 10)));
    } else if(currentUrl.includes(I.getDefaultDomainName())) {
      //we moved server to tau20, so we need to change domain
      //get URL part after domain
      I.amOnPage(currentUrl.substring(currentUrl.indexOf("/", 10)));
    }
  },

  /**
   * Delete all cache objects to prevent spam protection and other issues
   */
  deleteAllCacheObjects() {
    I.relogin("admin");
    I.amOnPage("/admin/v9/settings/cache-objects/");
    I.clickCss("button.btn-delete-all");
    I.waitForElement("div.toast-message");
    I.clickCss("div.toast-message button.btn-primary");
    I.closeOtherTabs();
    I.logout();
  },

  /**
     * Adjusts the scroll position of a container based on the provided element selector.
     *
     * Note: Sometimes, the standard `I.scrollTo` function may not work correctly if the page uses custom
     * scrollbars (e.g., with CSS transformations or JavaScript-based scrolling). In such cases,
     * this function can be used as an alternative to manually scroll the custom scrollbar.
     *
     * @param {string} selector - The CSS selector for the element whose position is to be used for scrolling.
     * @param {number} [offsetAdjustment=0] - An optional offset to adjust the final scroll position.
     * @param {string} [scrollBarSelector='div.ly-content-wrapper div.scroll-content'] -
     * The CSS selector for the custom scrollbar container. Default is set to 'div.ly-content-wrapper div.scroll-content'.
     *
     * @throws {Error} If the element or scroll content container is not found.
     */
  async adjustScrollbar(selector, offsetAdjustment = 0) {
    const yOffset = await I.executeScript((selector) => {
        const element = document.querySelector(selector);
        if (element) {
            return element.offsetTop;
        } else {
            throw new Error(`Element not found for selector: ${selector}`);
        }
    }, selector);

    const adjustedOffset = yOffset - offsetAdjustment;

    await I.executeScript((adjustedOffset) => {
        const scrollContent = document.querySelector('div.ly-content-wrapper div.scroll-content');
        if (scrollContent) {
            scrollContent.style.transform = `translate3d(0px, -${adjustedOffset}px, 0px)`;
        } else {
            throw new Error('Scroll content container not found.');
        }
    }, adjustedOffset);
  },

  /**
   * Function that waits until the number of open tabs reaches the expected number.
   * @param {number} expectedTabs - The minimum number of tabs to wait for. The default value is 2.
   */
  async waitForTab(expectedTabs = 2){
    let retries = 0;
    const limit = 10;
    I.wait(1);
    let current = await I.grabNumberOfOpenTabs();
    while (current != expectedTabs) {
      I.say(`Waiting for tab to open..., current: ${current}, expected: ${expectedTabs}`);
      I.wait(1);
      retries++;
      if (retries > limit) {
        throw new Error(`Reached timeout (${limit}s) while waiting for open tab.`);
      }
      current = await I.grabNumberOfOpenTabs();
    }
  },

  async grabTagNameFrom(selector){
    const tagName = await I.executeScript((selector) => {
      let element = document.querySelector(selector);
      return element ? element.tagName.toLowerCase() : null;
    }, selector);
  return tagName;
  },

  /**
   * Returns true if PDF viewer is enabled, otherwise false.
   * In headless mode, PDF viewer is disabled in chromium.
   * @returns
   */
  isPdfViewerEnabled() {
    return "false"!==process.env.CODECEPT_SHOW;
  }
}