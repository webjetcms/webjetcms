const { Helper } = codeceptjs;

class CustomWebjetHelper extends Helper {

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
        return helper.click(selector) && true;
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
    var text = await I.grabTextFrom("div.dataTables_info");
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
