# Reports via Allure

During the automated test, a report is also generated in the format for [Allure](https://docs.qameta.io/allure/). They are published for [chromium](http://docs.webjetcms.sk/allure/chromium/) and [firefox](http://docs.webjetcms.sk/allure/firefox/).

The generation of input data for the report is provided by the [codecept-allure](https://codecept.io/plugins/) extension.

![](allure-overview.png)

The following data is displayed:

- ```Overview``` - ​​basic status and history overview
- ```Categories``` - ​​displays a categorized list of errors, categories are defined according to [regular expressions](https://docs.qameta.io/allure/#_categories_2) of the returned error in the file ```src/test/webapp/allure/categories.json```
- ```Suites``` - ​​list of individual tests, showing individual steps performed
- ```Graphs``` - ​​graphs of current and historical development
- ```Timeline``` - ​​time display of individual test execution
- ```Behaviors``` - ​​allows for the division of tests into ```Epic, Feature, Story```, which is not currently used by WebJET CMS
- ```Packages``` - ​​tree representation of individual tests

## Running a report

The report generation process is more complicated due to history maintenance. This requires retrieving the previous report (folder ```history```) before generating the report.

The entire process is in the script ```npx-allure.sh```, which downloads the latest results from the documentation server before running the test and saves them to the documentation server after the test is performed.

The script is used with the parameters:

- ```CODECEPT_BROWSER``` - ​​name of the browser used - ```chromium``` or ```firefox``` (default ```chromium```)
- ```CODECEPT_URL``` - ​​URL of the domain to be tested (default ```demotest.webjetcms.sk```)
- ```HOST_USER``` - ​​SSH user name for downloading history and saving the result
- ```HOST_NAME``` - ​​server domain name for SSH connection history and result storage
- ```HOST_DIR``` - ​​folder with the report on the server, the name of the browser used is added to the folder name

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

As written above, to preserve history, it is necessary to obtain the folder ```history``` from the previous version before generating the report. This is ensured in the script by using ```rsync``` from the documentation page.

Additionally, a file ```build/test/environment.properties``` is generated at startup with the name of the browser used and the domain used. This is displayed on the ```Overview``` tab in the ```environment``` section.

Running tests and generating reports is provided using CI-CD in the ```gitlab-ci.yml``` file by running the gradle tasks ```rune2etest``` and ```rune2etestfirefox```.