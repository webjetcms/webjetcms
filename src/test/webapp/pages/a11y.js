const { I } = inject();
const allure = require("allure-js-commons");

module.exports = {

    async check() {
        await allure.tags("accessibility");
        let violations = await I.runA11yCheck();

        let errorMessage = "";

        if (violations.length > 0) {

            await allure.severity("minor");
            await allure.label("my custom label", "value");
            await allure.epic("Accesibility");

            //iterate over violations and create simple text error message
            for (const violation of violations) {
                errorMessage += `- ${violation.impact.toUpperCase()}: ${violation.id} - ${violation.description}\n`;

                await allure.tags("a11y-"+violation.id);
            }
        }

        I.assertEqual(
            violations.length,
            0,
            `${violations.length} accessibility violation${
                violations.length === 1 ? '' : 's'
            } ${violations.length === 1 ? 'was' : 'were'} detected:\n\n${errorMessage}`,
        );
    }

}