const { setHeadlessWhen } = require('@codeceptjs/configure');
const { devices } = require('playwright');

// turn on headless mode when running with HEADLESS=true environment variable
// HEADLESS=true npx codecept run
setHeadlessWhen(process.env.HEADLESS);

let url = process.env.CODECEPT_URL || "https://"+process.env.CODECEPT_DEFAULT_DOMAIN_NAME;
let codeceptShow = process.env.CODECEPT_SHOW;
let browser = process.env.CODECEPT_BROWSER || "chromium";
let restart = process.env.CODECEPT_RESTART || "context";
let autoDelayEnabled = "true" == process.env.CODECEPT_AUTODELAY;

let language = process.env.CODECEPT_LNG || "sk"; //default sk

let loginButtonText = "Prihlásiť sa";
//Select language if not default
if ("sk" != language) {
  //Different language detected, selecting language
  if("en" == language) {
    loginButtonText = "Login";
  } else if("cs" == language) {
    loginButtonText = "Přihlásit se";
  }
}

let pressKeyDelay = 13;
if ("firefox"===browser) {
  autoDelayEnabled = true;
  pressKeyDelay = 30;
}

let showBrowser = true;
if (typeof codeceptShow != "undefined" && "false" == codeceptShow) {
  showBrowser = false;
}

console.log("url=", url);
console.log("showBrowser=", showBrowser);
console.log("browser=", browser);
console.log("restart=", restart);
console.log("autoDelayEnabled=", autoDelayEnabled);

exports.config = {
  tests: './tests/admin/**/*.js',
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
      keepBrowserState: true,
      /* este nefunguje, vid https://github.com/microsoft/playwright/pull/3526
      ,
      chromium: {
        showUserInput: true
      }
      */
      //,emulate: devices['iPhone 6']
      chromium: {
        args: ['--lang=sk']
      }
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
        'x-auth-token': process.env.CODECEPT_DEFAULT_AUTH_TOKEN
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
    DTE: './pages/DTE.js',
    TempMail: './pages/TempMail.js',
    Apps: './pages/Apps.js',
    i18n: './pages/i18n.js',
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
      enabled: true,
      fullPageScreenshots: true
    },
    allure: {
      enabled: true,
      require: "allure-codeceptjs",
      resultsDir: "../../../build/test/allure-results",
    },
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
            I.switchTo();

            //Select language if not default
            if ("sk" != language) {
              //Different language detected, selecting language
              if("en" == language) {
                I.selectOption("language", "English");
              } else if("cs" == language) {
                I.selectOption("language", "Česky");
              }
            }

            //odosli prihlasenie
            I.fillField("username", "tester");
            I.fillField("password", secret(I.getDefaultPassword()));
            I.forceClick(loginButtonText);
            I.waitForText("Tester Playwright", 30, "button.js-profile-toggler");
          },
          check: (I) => {
            I.say("checking logged user");
            I.see("Tester Playwright", "button.js-profile-toggler");
            //aby sme vzdy v kazdom scenari mali prednastavenu velkost okna
            I.wjSetDefaultWindowSize();
          },
          restore: (I) => {
            I.say("restoring logged user");
            I.amOnPage('/admin/');
            I.see("Tester Playwright", "button.js-profile-toggler");
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