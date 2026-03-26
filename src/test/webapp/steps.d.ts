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
type a11y = typeof import('./pages/a11y.js');
type CustomWebjetHelper = import('./helpers/custom_helper.js');
type ChaiWrapper = import('codeceptjs-chai');
type PixelmatchHelper = import('codeceptjs-pixelmatchhelper');
type A11yHelper = import('./helpers/a11yhelper.js');

declare namespace CodeceptJS {
  interface SupportObject { I: I, current: any, login: any, Browser: Browser, DataTables: DataTables, Document: Document, DT: DT, DTE: DTE, TempMail: TempMail, Apps: Apps, i18n: i18n, a11y: a11y }
  interface Methods extends Playwright, CustomWebjetHelper, ChaiWrapper, PixelmatchHelper, REST, JSONResponse, FileSystem, A11yHelper {}
  interface I extends ReturnType<steps_file>, WithTranslation<Methods> {}
  namespace Translation {
    interface Actions {}
  }
}
