Feature('apps.app-weather');

function getDates(){
    let currentDate = new Date();
    let result = [];

    for (let i = 1; i < 9; i++) {
        let newDate = new Date(currentDate.getTime() + i * 24 * 60 * 60 * 1000);
        let newDay = newDate.getDate();
        let newMonth = newDate.getMonth() + 1;
        result.push(`${newDay}.${newMonth}`);
    }
    return result;
}

Scenario("Počasie - test zobrazovania", ({ I }) => {
    const MIN_INTEGER = -2147483648;
    const MAX_INTEGER = 2147483647;

    I.amOnPage("/apps/pocasie/");
    I.waitForElement(".app-weather");
    I.dontSee(MIN_INTEGER.toString(), ".pocStupne");
    I.dontSee(MAX_INTEGER.toString(), ".pocStupne");
    I.seeElement(locate("h1").withText(("Počasie")));
    I.see("Bratislava");
    for(let date of getDates()) {
        I.see(date);
    }
});