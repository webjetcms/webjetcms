Feature('apps.app-date');

function getDate(){
    let days = ["Nedeľa", "Pondelok", "Utorok", "Streda", "Štvrtok", "Piatok", "Sobota"];
    let currentDate = new Date();
    let nameOfDay = (days[currentDate.getDay()]);
    let day = currentDate.getDate();
    let month = currentDate.getMonth() + 1;
    let year = currentDate.getFullYear();

    if (day < 10) {
        day = '0' + day;
    }
    if (month < 10) {
        month = '0' + month;
    }

    let formattedDate = `${nameOfDay} ${day}.${month}.${year}`;
    return formattedDate;
}

Scenario("Dátum - test zobrazenia", ({ I }) => {
    I.amOnPage("/apps/datum/");
    I.waitForElement(".dateapp");
    I.seeElement(locate("h1").withText("Dátum"));
    I.see(getDate());
});