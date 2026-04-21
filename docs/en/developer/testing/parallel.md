# Running tests in parallel

To save time executing tests, they can be [run in parallel](https://codecept.io/parallel/#parallel-execution-by-workers). Scripts ```parallel4,parallel6,parallel8,parallel12``` are prepared for parallel execution in 4 to 12 browser windows at once.

<div class="video-container">
    <iframe width="560" height="315" src="https://www.youtube.com/embed/dkXVqNnZPWg" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
</div>

## Use

Due to the continuity of tests in one scenario, tests are divided between individual browser windows according to ```Feature``` (this is ensured by the ```--suites``` switch), i.e. typically by file name. Scenarios in one file are then executed sequentially as in a standard launch.

However, some tests cannot be run in parallel, because they check the status by IP address, etc. An example is a check for a wrong password, which if executed will prevent login for a specified time. If this happens in a test run in parallel, you will not be able to log in in other browser windows, which will ruin the tests.

For this reason, a script ```singlethread``` has been prepared that will run only the tests marked with the ```@singlethread``` tag in the scenario name. At the same time, tests marked in this way are excluded from parallel execution.

```javascript
Feature('admin.login');

Scenario('zle zadane heslo @singlethread', ({ I }) => {
    I.amOnPage('/admin/');
    I.fillField("username", "tester");
    ...
});
```

So for a complete test it is necessary to run the scripts one after the other:

```bash
npm run singlethread
npm run parallel8
```

## Implementation details

Individual scripts are defined in ```package.json```:

```json
{
    "scripts": {
        "parallel4": "CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_AUTODELAY='true' codeceptjs run-workers --suites 4 -p allure --grep '(?=.*)^(?!.*@singlethread)'",
        "parallel6": "CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_AUTODELAY='true' codeceptjs run-workers --suites 6 -p allure --grep '(?=.*)^(?!.*@singlethread)'",
        "parallel8": "CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_AUTODELAY='true' codeceptjs run-workers --suites 8 -p allure --grep '(?=.*)^(?!.*@singlethread)'",
        "parallel12": "CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_AUTODELAY='true' codeceptjs run-workers --suites 12 -p allure --grep '(?=.*)^(?!.*@singlethread)'",
        "singlethread": "CODECEPT_RESTART='session' CODECEPT_SHOW=false CODECEPT_AUTODELAY='true' codeceptjs run -p allure --grep '@singlethread'",
    }
}
```