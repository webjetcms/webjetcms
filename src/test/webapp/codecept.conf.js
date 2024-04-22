const { setHeadlessWhen } = require('@codeceptjs/configure');
const { devices } = require('playwright');

// turn on headless mode when running with HEADLESS=true environment variable
// HEADLESS=true npx codecept run
setHeadlessWhen(process.env.HEADLESS);

var url = process.env.CODECEPT_URL || 'http://iwcm.interway.sk';
var codeceptShow = process.env.CODECEPT_SHOW;
var browser = process.env.CODECEPT_BROWSER || "chromium";
var restart = process.env.CODECEPT_RESTART || "context";
var autoDelayEnabled = "true" == process.env.CODECEPT_AUTODELAY;
var pressKeyDelay = 13;
if ("firefox"===browser) {
  autoDelayEnabled = true;
  pressKeyDelay = 30;
}

var showBrowser = true;
if (typeof codeceptShow != "undefined" && "false" == codeceptShow) {
  showBrowser = false;
}

console.log("url=", url);
console.log("showBrowser=", showBrowser);
console.log("browser=", browser);
console.log("restart=", restart);
console.log("autoDelayEnabled=", autoDelayEnabled);

exports.config = {
  tests: './tests/**/*.js',
  output: '../../../build/test',
  helpers: {
    Playwright: {
      url: url,
      show: showBrowser,
      browser: browser,
      waitForNavigation: "networkidle",
      waitForTimeout: 30000,
      pressKeyDelay: pressKeyDelay,
      timeout: 30000, //toto je max cas ako dlho moze trvat akcia, kedze mame pressKeyDelay 50ms tak musi byt dostatocne dlhe, aby cez fillField vedelo napisat text, inak timeoutne funkcia
      restart: restart,
      keepCookies: true,
      keepBrowserState: true
      /* este nefunguje, vid https://github.com/microsoft/playwright/pull/3526
      ,
      chromium: {
        showUserInput: true
      }
      */
      //,emulate: devices['iPhone 6']
    },
    CustomWebjetHelper: {
      require: './custom_helper.js'
    },
    ChaiWrapper: {
      //https://www.npmjs.com/package/codeceptjs-chai
      require: "codeceptjs-chai"
    },
    /*ResembleHelper: {
      require: "codeceptjs-resemblehelper",
      screenshotFolder: "./../../build/test",
      baseFolder: "./screenshots/base/",
      diffFolder: "./screenshots/diff/"
    }*/
    //https://github.com/stracker-phil/codeceptjs-pixelmatchhelper/wiki/Helper-Configuration
    PixelmatchHelper: {
      require: "codeceptjs-pixelmatchhelper",
      dirActual: "../../../build/test/screenshots/actual/",
      dirExpected: "./screenshots/base/",
      dirDiff: "../../../build/test/screenshots/diff/"
    },
    REST: {
      endpoint: url,
      defaultHeaders: {
        'x-auth-token': '*********'
      },
    },
    JSONResponse: {},
    FileSystem: {},
  },
  include: {
    I: './steps_file.js',
    Browser: './pages/Browser.js',
    DataTables: './pages/DataTables.js',
    Document: './pages/Document.js',
    DT: './pages/DT.js',
    DTE: './pages/DTE.js'
  },
  bootstrap: null,
  mocha: {
    //generovanie reportov do output adresara https://codecept.io/reports/#html
    reporterOptions: {
      reportDir: "../../../build/test/report"
    }
  },
  name: 'webapp',
  plugins: {
    retryFailedStep: {
      enabled: true
    },
    screenshotOnFail: {
      enabled: true
    },
    allure: {},
    pauseOnFail: {},
    autoLogin: {
      enabled: true,
      saveToFile: false,
      inject: 'login',
      users: {
        admin: {
          login: (I) => {
            I.say("logging in");
            //start in new fresh tab
            I.openNewTab();
            I.closeOtherTabs();
            I.amOnPage('/logoff.do?forward=/admin/logon/');
            //aby sme vzdy v kazdom scenari mali prednastavenu velkost okna
            I.wjSetDefaultWindowSize();
            //odosli prihlasenie
            I.fillField("username", "tester");
            I.fillField("password", secret("*********"));
            I.forceClick("Prihlásiť sa");
            I.waitForText("Tester Playwright", 30);
          },
          check: (I) => {
            I.say("checking logged user");
            I.see("Tester Playwright");
            //aby sme vzdy v kazdom scenari mali prednastavenu velkost okna
            I.wjSetDefaultWindowSize();
          },
          restore: (I) => {
            I.say("restoring logged user");
            I.amOnPage('/admin/');
            I.see("Tester Playwright");
          }
        }
      }
    },
    autoDelay: {
      enabled: autoDelayEnabled,
      methods: [
        "amOnPage",
        "click",
        "forceClick"
      ],
      delayBefore: 200,
      delayAfter: 300
    }
  },
  /*multiple: {
    parallel: {
        chunks: process.env.THREADS || 5,
        browsers: [{
            browser: 'chromium',
            windowSize: '1280x760',
        }],
    },
  }*/
}