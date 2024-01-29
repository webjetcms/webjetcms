const { I } = inject();
const DT = require("./DT");

/**
 * Funkcie pre pracu s Datatable EDITOR
 */

module.exports = {

     //vybere hodnotu v selecte v DT editore
     selectOption(name, text) {
          I.click({ css: "div.modal-dialog div.DTE_Field_Name_" + name + " button.dropdown-toggle" });
          I.waitForElement(locate('div.dropdown-menu.show .dropdown-item').withText(text), 5);
          I.click(locate('div.dropdown-menu.show .dropdown-item').withText(text));
          I.wait(0.3);
     },

     waitForLoader() {
          let name = "div.DTE_Processing_Indicator";
          I.waitForInvisible(name, 200);
          I.wait(0.2);
     },

     //ulozi editor a pocka na vysledok
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

     saveBubble() {
          //ideme cez slector, pretoze to moze byt Pridat (novy zaznam) alebo Ulozit (existujuci)
          I.click({ css: "div.DTE.DTE_Bubble div.DTE_Form_Buttons button.btn.btn-primary" });
          this.waitForLoader();
          //cakaj na reload datatabulky, nevieme aka to je DT, takze radsej takto natvrdo
          I.wait(3);
     },

     cancel(name, clickTopButton=false) {
          var prefixSelector = "div";
          if (typeof name != "undefined" && name != null) prefixSelector = "#" + name + "_modal";

          if (clickTopButton===true) {
               //use X button on header for inner tables
               I.click({ css: prefixSelector + ".DTED.show div.DTE_Header button.btn-close-editor" });
          } else {
               I.click({ css: prefixSelector + ".DTED.show div.DTE_Footer.modal-footer button.btn-close-editor" });
          }
          this.waitForLoader(name);
          //wait for fade animation
          I.wait(0.5);
     },

     waitForEditor(name) {
          if (typeof name == "undefined") { name = "datatableInit"; }
          I.waitForVisible("#" + name + "_modal", 200);
          I.wait(0.2);
     },

     waitForModal(id) {
          I.waitForVisible("#" + id);
          I.wait(0.2);
          if (id.endsWith("ImportModal")) I.wait(0.6);
     },

     waitForModalClose(id) {
          I.waitForInvisible("#" + id);
          I.wait(0.2);
     },

     fillField(name, value) {
          var value1 = value;
          if (value1.length>2) value1 = "**"; //just placeholder
          I.fillField("#DTE_Field_" + name, value1);

          I.fillField("#DTE_Field_" + name, value);
     },

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
      * Vyplni hodnotu do WYSIWYG editora Quill
      * @param {*} htmlCode - html kod, ktory sa ma do editora nastavit
      */
     async fillCkeditor(htmlCode) {
          //pockaj na inicializaciu CK editora
          I.waitForElement("span.cke_toolbox");
          I.wait(2);
          I.waitForElement('#trEditor', 10);
          I.click('#trEditor');
          I.pressKey('ArrowLeft');
          await I.executeScript(function (html) {
               window.ckEditorInstance.setData(html);
               }, htmlCode
          );
          I.wait(1);
     },

     /**
      * Vyplni hodnotu do WYSIWYG editora Quill
      * @param {*} name
      * @param {*} value
      */
     fillQuill(name, value) {
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
     * Vyplni hodnotu do WYSIWYG editora Cleditor
     * @param {*} parentSelector - selector parent elementu
     * @param {*} value - hodnota na vyplnenie, POZOR, nevie to zatial diakritiku z dovodu pouzitia type prikazu
     */
    fillCleditor(parentSelector, value) {
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
}