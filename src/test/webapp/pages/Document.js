const { I } = inject();
const DT = require("./DT");
const DTE = require("./DTE");
const Apps = require("./Apps");

/**
 * Funkcie pre pracu s Dokumentami
 */

module.exports = {

  screenshot(screenshotFilePath, width, height) {
      this.screenshotElement(null, screenshotFilePath, width, height);
  },

  screenshotElement(selector, screenshotFilePath, width, height) {
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
      if (typeof selector != "undefined" && selector != null && selector != "") I.saveElementScreenshot(selector, path);
      else I.saveScreenshot(path);

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
      I.switchTo("iframe.wj_component");
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
      I.waitForText("Pre zmenu domény je potrebné znovu načítať stránku", 10, ".toastr-message");
      I.wait(0.5);
      var btn = locate("#toast-container-webjet .toastr-buttons button.btn-primary").withText("Potvrdiť");
      I.waitForElement(btn);
      I.click(btn);
      I.say("Domain switched to: "+domain+" DO NOT FORGET TO LOGOUT AFTER THIS SCENARIO otherwise you will be logged in with wrong domain");
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
   * @param {*} currentUrl
   */
  async fixLocalhostUrl(currentUrl) {
    if(currentUrl.includes("iwcm.interway.sk") || currentUrl.includes("localhost")) {
        currentUrl = await I.grabCurrentUrl();
        //get URL part after domain
        I.amOnPage(currentUrl.substring(currentUrl.indexOf("/", 10)));
    }
  },
}