# Reports via Allure

During the automated test, a report is also generated in the format for [Allure](https://docs.qameta.io/allure/). They are published for [chromium](http://docs.webjetcms.sk/allure/chromium/) also for [firefox](http://docs.webjetcms.sk/allure/firefox/).

Generation of input data for the report is provided by the extension [codecept-allure](https://codecept.io/plugins/).

![](allure-overview.png)

The following data is displayed:
- `Overview` - basic overview of status and history
- `Categories` - displays a categorised list of errors, the categories are defined by [regular expressions](https://docs.qameta.io/allure/#_categories_2) returned error in the file `src/test/webapp/allure/categories.json`
- `Suites` - list of individual tests, showing the steps performed
- `Graphs` - charts of current and historical developments
- `Timeline` - time display of the execution of each test
- `Behaviors` - allows the tests to be divided into `Epic, Feature, Story`what WebJET CMS is not currently used
- `Packages` - tree representation of individual tests

## Running the report

The process of generating the report is more complicated because of the history preservation. This requires retrieving the previous report (for the `history`) before generating the report.

The whole process is in the script `npx-allure.sh`, which downloads the latest results from the documentation server before running the test and saves them to the documentation server after the test is run.

The script is used with parameters:
- `CODECEPT_BROWSER` - name of the browser used - `chromium` or `firefox` (default `chromium`)
- `CODECEPT_URL` - URL of the domain to be tested (default `demotest.webjetcms.sk`)
- `HOST_USER` - SSH user name to download history and save result
- `HOST_NAME` - server domain name for SSH connection history and result storage
- `HOST_DIR`
```sh
#spustenie s chrome a predvolenou domenou
npx-allure.sh

#spustenie s firefox a predvolenou domenou
npx-allure.sh firefox

#spustenie s firefoxom a domenou iwcm.interway.sk
npx-allure.sh firefox http://iwcm.interway.sk

#spustenie s chrome a domenou demo.webjetcms.sk
npx-allure.sh chromium http://demo.webjetcms.sk
```

## Technical information

`history``rsync`

`build/test/environment.properties``Overview``environment`.

`gitlab-ci.yml``rune2etest` a `rune2etestfirefox`.
