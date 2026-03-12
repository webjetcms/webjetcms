/// <reference types='codeceptjs' />
type steps_file = typeof import('./steps_file.js');
type Browser = typeof import('./pages/Browser.js');
type DataTables = typeof import('./pages/DataTables.js');
type Document = typeof import('./pages/Document.js');
type DT = typeof import('./pages/DT.js');
type DTE = typeof import('./pages/DTE.js');
type TempMail = typeof import('./pages/TempMail.js');
type Apps = typeof import('./pages/Apps.js');
type i18n = typeof import('./pages/i18n.js');
type CustomWebjetHelper = import('./custom_helper.js');
type ChaiWrapper = import('codeceptjs-chai');
type PixelmatchHelper = import('codeceptjs-pixelmatchhelper');

declare namespace CodeceptJS {
  interface SupportObject { I: I, current: any, login: any, Browser: Browser, DataTables: DataTables, Document: Document, DT: DT, DTE: DTE, TempMail: TempMail, Apps: Apps, i18n: i18n }
  interface Methods extends Playwright, CustomWebjetHelper, ChaiWrapper, PixelmatchHelper, REST, JSONResponse, FileSystem {}
  interface I extends ReturnType<steps_file>, WithTranslation<Methods> {}
  namespace Translation {
    interface Actions {}
  }
}
