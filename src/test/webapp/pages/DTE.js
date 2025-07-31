const { I } = inject();
const DT = require("./DT");

/**
 * Functions for working with the DataTable EDITOR
 */

module.exports = {

          /**
      * Selects a value in the dropdown in the DataTable editor
      * @param {String} name - The name of the dropdown field
      * @param {String} text - The text of the option to select
      */
     selectOption(name, text) {
          I.click({ css: "div.modal-dialog div.DTE_Field_Name_" + name + " button.dropdown-toggle" });
          I.waitForElement(locate('div.dropdown-menu.show .dropdown-item').withText(text), 5);
          I.waitForVisible(locate('div.dropdown-menu.show .dropdown-item').withText(text), 5);
          I.click(locate('div.dropdown-menu.show .dropdown-item').withText(text));
          I.wait(0.3);
     },

     async selectOptionMulti(name, values) {
          //first unselect all options
          I.click({ css: "div.modal-dialog div.DTE_Field_Name_" + name + " button.dropdown-toggle" });
          var containerSelector = ".bs-container.dropdown.bootstrap-select.form-select";
          I.waitForElement(containerSelector, 5);
          let numOfElements = await I.grabNumberOfVisibleElements(containerSelector + " ul.dropdown-menu.inner.show li a.dropdown-item.selected");
          for (let i = 0; i < numOfElements; i++) {
               I.click(containerSelector + " ul.dropdown-menu.inner.show li a.dropdown-item.selected");
          }

          //then select wanted options
          for (let i = 0; i < values.length; i++) {
               I.click(locate(containerSelector + " ul.dropdown-menu.inner.show li a.dropdown-item").withText(values[i]));
               I.wait(0.3);
          }
     },

     /**
      * Waits for the loading indicator to disappear
      */
     waitForLoader() {
          let name = "div.DTE_Processing_Indicator";
          I.waitForInvisible(name, 200);
          I.wait(0.2);
     },

     /**
      * Saves the editor changes and waits for the result
      * @param {String} [name] - Optional modal name for the editor
      */
     save(name) {
          var prefixSelector = "div";
          if (typeof name != "undefined") prefixSelector = "#" + name + "_modal";

          //ideme cez slector, pretoze to moze byt Pridat (novy zaznam) alebo Ulozit (existujuci)
          I.click({ css: prefixSelector + ".DTED.show div.DTE_Footer.modal-footer button.btn.btn-primary" });
          this.waitForLoader(name);
          //cakaj na reload datatabulky, nevieme aka to je DT, takze radsej takto natvrdo
          //I.wait(3);
          DT.waitForLoader();
     },

     /**
      * Saves the editor changes using the bubble interface
      */
     saveBubble() {
          //ideme cez slector, pretoze to moze byt Pridat (novy zaznam) alebo Ulozit (existujuci)
          I.click({ css: "div.DTE.DTE_Bubble div.DTE_Form_Buttons button.btn.btn-primary" });
          this.waitForLoader();
          //cakaj na reload datatabulky, nevieme aka to je DT, takze radsej takto natvrdo
          I.wait(3);
     },

     /**
      * Cancels the editing process and closes the editor
      * @param {String} [name] - Optional modal name to target
      * @param {Boolean} [clickTopButton=false] - Determines which close button to click (top or footer)
      */
     cancel(name, clickTopButton=false) {
          var prefixSelector = "div";
          if (typeof name != "undefined" && name != null) prefixSelector = "#" + name + "_modal";

          if (clickTopButton===true) {
               //use X button on header for inner tables
               I.click({ css: prefixSelector + ".DTED.show div.DTE_Header button.btn-close-editor" });
          } else {
               I.click({ css: prefixSelector + ".DTED.show div.DTE_Footer.modal-footer button.btn-close-editor" });
          }

          if (typeof name == "undefined") { name = "datatableInit"; }
          I.waitForInvisible("#" + name + "_modal", 200);

          //wait for fade animation
          I.wait(0.5);
     },

     /**
      * Waits for the editor modal to be visible
      * @param {String} [name] - Optional modal name
      */
     waitForEditor(name) {
          if (typeof name == "undefined") { name = "datatableInit"; }
          I.waitForVisible("#" + name + "_modal", 200);
          I.wait(0.2);
     },

     /**
      * Waits for a specific modal to be visible
      * @param {String} id - The ID of the modal to wait for
      */
     waitForModal(id) {
          I.waitForVisible("#" + id);
          I.wait(0.2);
          if (id.endsWith("ImportModal")) I.wait(0.6);
     },

     /**
      * Waits for a specific modal to close
      * @param {String} id - The ID of the modal to wait for
      */
     waitForModalClose(id) {
          if (typeof id == "undefined") { id = "datatableInit_modal"; }
          I.waitForInvisible("#" + id);
          I.wait(0.2);
     },

     /**
      * Fills a specified field with a given value
      * @param {String} name - The name of the field to fill
      * @param {String} value - The value to fill in the field
      */
     fillField(name, value) {
          var value1 = value;
          if (value1.length>2) value1 = "**"; //just placeholder
          I.fillField("#DTE_Field_" + name, value1);

          I.fillField("#DTE_Field_" + name, value);
     },

     /**
      * Appends a value to a specified field
      * @param {String} name - The name of the field to append to
      * @param {String} value - The value to append
      */
     appendField(name, value) {
          //I.appendField is broken (eg in translation-keys.js, user-list.js I.pressKey('Home') scrolls window instead of cursor in field)
          I.executeScript(({name, value}) => {
                    var element = $("#DTE_Field_" + name);
                    element.val(element.val()+value);
               }, {name, value}
          );
          I.wait(0.5);
     },

     /**
      * Waits for the CKEditor to initialize
      */
     waitForCkeditor() {
          //pockaj na inicializaciu CK editora
          I.waitForElement("span.cke_toolbox");
          I.wait(2);
          I.waitForElement('#trEditor', 10);
     },

     /**
      * Fills value into the WYSIWYG editor Quill
      * @param {String} htmlCode - HTML code to set in the editor
      */
     async fillCkeditor(htmlCode) {
          this.waitForCkeditor();
          I.clickCss('#trEditor');
          I.pressKey('ArrowLeft');
          await I.executeScript(function (html) {
               window.ckEditorInstance.setData(html);
               }, htmlCode
          );
          I.wait(1);
     },

     /**
      * Fills value into the WYSIWYG editor Quill
      * @param {String} name - Name of the field to fill
      * @param {String} value - Value to fill in the field
      */
     fillQuill(name, value) {
          I.wait(1); //wait for Quill editor to initialize
          I.click(locate('p').inside("#DTE_Field_" + name));
          I.wait(0.3);
          I.pressKey(['CommandOrControl', 'A']);
          I.wait(0.3);
          I.pressKey('Backspace');
          I.wait(0.3);
          /*var i;
          for (i=0; i<value.length; i++) {
            I.pressKey(value.charAt(i));
          }*/
          //I.fillField(locate('p').inside("#DTE_Field_" + name), value);
          I.type(value, 50);
     },

     /**
      * Fills value into the WYSIWYG editor Cleditor
      * @param {String} parentSelector - Selector for the parent element
      * @param {String} value - Value to fill; WARNING: currently does not handle diacritics due to typing command
      */
    fillCleditor(parentSelector, value) {
     I.waitForElement(parentSelector+" div.cleditorMain", 10);
     I.click(parentSelector+" div.cleditorMain", null, { position: { x: 10, y: 30 } });
     I.wait(0.3);
     I.pressKey(['CommandOrControl', 'A']);
     I.wait(0.3);
     I.pressKey('Backspace');
     I.wait(0.3);
     I.type(value, 50);
   },

   /**
    * Clicks on checkbox 'div.custom-control.form-switch label' with text on Datatable Editor window
    * @param {String} text
    */
   clickSwitchLabel(text) {
     I.click(locate('div.modal.DTED.show div.custom-control.form-switch label').withText(text));
   },

   /**
    * Clicks on a switch element by its name
    * @param {String} name - The name of the switch to click
    */
   clickSwitch(name){
     I.clickCss('#DTE_Field_' + name);
   },

   /**
    * Verifies that a specified field contains a specific value
    * @param {String} name - The name of the field to check
    * @param {String} value - The expected value to verify
    */
   seeInField(name, value) {
     I.seeInField("#DTE_Field_" + name, value);
   }
}