# Parallel running of tests

In order to save test execution time, the tests can be [run in parallel](https://codecept.io/parallel/#parallel-execution-by-workers). Scripts are ready `parallel4,parallel6,parallel8,parallel12` for parallel execution in 4 to 12 browser windows at the same time.

<div class="video-container">
  <iframe width="560" height="315" src="https://www.youtube.com/embed/dkXVqNnZPWg" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowfullscreen></iframe>
</div>

## Use

For the sake of test continuity in a single scenario, tests are split between browser windows according to `Feature` (this is provided by the switch `--suites`), i.e. typically by file name. The scenarios in a single file are then executed sequentially in succession as in a standard run.

However, some tests cannot be executed in parallel, because they check the status by IP address and so on. An example is a check for a wrong password, which, if executed, will prevent logging in for the specified time. If this were to happen in a test running in parallel, it would not be possible to log in in other browser windows either, which would break the tests.

For this reason, a script is prepared `singlethread` which will only run tests marked with the `@singlethread` in the title of the script. At the same time, tests marked as such are excluded from parallel execution.

```javascript
Feature('admin.login');

Scenario('zle zadane heslo @singlethread', ({ I }) => {
    I.amOnPage('/admin/');
    I.fillField("username", "tester");
    ...
});
```

So for a complete test, you need to run the scripts in sequence:

```bash
npm run singlethread
npm run parallel8
```

## Implementation details

Individual scripts are defined in `package.json`:

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
