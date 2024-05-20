function getDate(){
    let currentDate = new Date();
    let day = currentDate.getDate();
    let month = currentDate.getMonth() + 1;
    let year = currentDate.getFullYear();
    let formattedDate = `${day < 10 ? '0' + day : day}.${month < 10 ? '0' + month : month}.${year}`;
    return formattedDate
}

Feature('apps.podmienene-zobrazenie');


Scenario("Podmienené zobrazenie - test zobrazovania", ({ I }) => {
    I.amOnPage("/apps/podmienene-zobrazenie/");
    I.seeElement(locate("p").withText(("Demo component view, params:")));
    I.see('test1: Toto je test');
    I.see('stringField: Verzia pre PC');
    I.see('primitiveBooleanField: false');
    I.see('primitiveIntegerField: 0');
    I.see('primitiveDoubleField: 0.0');
    I.see('primitiveFloatField: 0.0');
    I.see('date:');
    I.see('dirSimple:');
    I.see('groupDetails:');
    I.see('docDetails:');
    I.see('groupDetailsList:');
    I.see('docDetailsList:');
    //time is little bit different between server and browser, test just the date
    I.see('current date: '+getDate());
});