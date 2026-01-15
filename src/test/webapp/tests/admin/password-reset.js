Feature('admin.password-reset');

const assert = require('assert');

const passwordRequirementsErr_nonAdmin = 'Zadané heslo nespĺňa bezpečnostné nastavenia aplikácie';
const passwordRequirementsErr_admin = 'Pri spracovaní formuláru nastali chyby';
const passwordRetypeErr_nonAdmin = 'Nové heslo nebolo zopakované správne.';
const passwordRetypeErr_admin = 'Nové heslo a opakovanie nového hesla sa nezhodujú';

var randomLogin;
const withoutDigitPassword = 'password_P';
const withoutCapitalPassword = 'password_p123';
const tooShortPassword = 'paS1';
const user = 'user_slabeheslo';

function deleteCacheObjects(I) {
  I.relogin("admin");
  I.amOnPage("/admin/v9/settings/cache-objects/");
  I.clickCss("button.btn-delete-all");
  I.waitForElement("div.toast-message");
  I.clickCss("div.toast-message button.btn-primary");
  I.closeOtherTabs();
}

Scenario('delete cache objects to prevent logon form wrong password counting, 1 @singlethread', async ({ I }) => {
  deleteCacheObjects(I);
});


Scenario('forgotten password - customer zone - VIA login @singlethread', async ({ I, Document, TempMail }) => {
  I.logout();
  const randomPassword = 'password_Pľščťž' + I.getRandomText();

  I.say('Vyžiadanie zmeny hesla v zakaznickej zóne');
  I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona');
  setValueToRecoverPassword(I, user, false);

  await changePasswordEmail(I, Document, TempMail, 'webjetcmstest');

  await checkUsersSelectOptions(I, ['user_slabeheslo'], false); //only one login, because we recover using login

  changePasswordPlusTests(I, randomPassword, false);

  I.say('Prihlasujem sa so zmenenim heslom');
  I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona');
  I.fillField('input[name="username"]', 'user_slabeheslo');
  I.fillField('input[type="password"][name="password"]', randomPassword);
  I.clickCss('input[type="submit"].btn.btn-success.login-submit');
  I.waitForText("Táto sekcia a tento text sa zobrazí len prihlásenému používateľovi", 10, "p");
});

Scenario('forgotten password - administration - VIA login @singlethread', async ({ I, Document, TempMail }) => {
  I.logout();
  const randomPassword = 'password_Pľščť' + I.getRandomText();

  I.say('Wait 10 seconds for next login');
  I.wait(11);

  I.say('Vyžiadanie zmeny hesla v administracii');
  I.amOnPage('/admin');
  setValueToRecoverPassword(I, user, true);

  await changePasswordEmail(I, Document, TempMail, 'webjetcmstest');
  changePasswordPlusTests(I, randomPassword, true);

  I.amOnPage('/admin');
  I.fillField('#username', 'user_slabeheslo');
  I.fillField('#password', randomPassword);
  I.clickCss('button[name="login-submit"]');
  I.waitForText("Vitajte,", 15, ".overview__dashboard__title h2");
});

const emailName = 'samemail';
const email = emailName+TempMail.getTempMailDomain();
const users = ['sameA','sameB', 'sameC', 'sameD'];

Scenario('forgotten password - administration - VIA email @singlethread', async ({ I, Document, TempMail, DT}) => {
  I.logout();
  const randomPassword = 'password_Pľščť' + I.getRandomText();

  I.say('Wait 10 seconds for next login');
  I.wait(11);

  setValueToRecoverPassword(I, email);

  const lastId  = await checkAudit(I, DT);
  await changePasswordEmail(I, Document, TempMail, emailName);
  const changePasswordUrl = await I.grabCurrentUrl();

  I.closeOtherTabs();
  I.say('Verification that each of these users is displayed in the options on the page');
  await checkUsersSelectOptions(I, users, true);
  const selectedUserForPasswordChange = getRandomElement(users);

  I.say('Select random user');
  I.selectOption('#selectedLogin', selectedUserForPasswordChange);
  changePassword(I, randomPassword, 'Zmena hesla úspešne dokončená', true);

  I.amOnPage('/admin/logon/');
  I.say('Try to login');
  for (let user of users) {
    I.fillField('#username', user);
    I.fillField('#password', randomPassword);
    I.clickCss('button[name="login-submit"]');
    if (user === selectedUserForPasswordChange){
      I.waitForText("Vitajte,", 15, ".overview__dashboard__title h2");
      I.logout();
    }
    else
      I.waitForText("Zadané meno alebo heslo je nesprávne.", 10);

    I.say('Wait 10 seconds for next login');
    I.wait(11);
  }

  await checkAudit(I, DT, lastId);

  checkUrlDoesNotWork(I, changePasswordUrl, true);
});


Scenario('delete cache objects to prevent logon form wrong password counting, 2 @singlethread', async ({ I }) => {
  deleteCacheObjects(I);
});


Scenario('IF un-used email address by users is used, the email will not be sent @singlethread', async ({ I, TempMail }) => {
  const invalidEmailName = 'invalidwebjetcms';
  const invalidEmail = invalidEmailName+TempMail.getTempMailDomain();
  I.logout();
  setValueToRecoverPassword(I, invalidEmail);

  I.say('Otvorenie e-mailu pre zmenu hesla');
  await TempMail.login(invalidEmailName);
  if (!await TempMail.isInboxEmpty()){
    TempMail.openLatestEmail();
    I.dontSeeElement(locate('a').withText('Ak si chcete zmeniť heslo, kliknite sem do 30 minút.'));
    TempMail.closeEmail();
  }
  await TempMail.destroyInbox();
});

Scenario('Make users not-valid for password reset @singlethread', async ({I, DT, DTE }) => {
  I.relogin('admin');
  I.amOnPage('/admin/v9/users/user-list/');
  DT.filterContains("email", "samemail"+TempMail.getTempMailDomain());

  I.say("user sameA - will be un-approved");
  I.click("sameA");
  DTE.waitForEditor();
  I.uncheckOption("#DTE_Field_authorized_0");
  DTE.save();

  I.say("user sameB - will NOT BE admin");
  I.click("sameB");
  DTE.waitForEditor();
  I.clickCss("#pills-dt-datatableInit-rightsTab-tab");
  I.uncheckOption("#DTE_Field_admin_0");
  DTE.save();

  I.say("user sameC - will be after valid date");
  I.click("sameC");
  DTE.waitForEditor();
  I.fillField("#DTE_Field_allowLoginEnd", "01.01.2000");
  DTE.save();

  I.logout();
});

Scenario('forgotten password - user zone - invalid users option test - VIA email @singlethread', async ({I, Document, TempMail }) => {
  I.closeOtherTabs();
  Document.deleteAllCacheObjects();

  I.logout();
  I.say('Vyžiadanie zmeny hesla v zakaznickej zóne');
  I.amOnPage('/apps/prihlaseny-pouzivatel/zakaznicka-zona');

  I.say('Wait 10 seconds for next login');
  I.wait(11);
  setValueToRecoverPassword(I, email, false);

  // Do not close email tab - we will return there
  await changePasswordEmail(I, Document, TempMail, emailName, false);
  const changePasswordUrl = await I.grabCurrentUrl();

  await checkUsersSelectOptions(I, ['sameB', 'sameD'], false); //two one logins are VALID - must see even if not a admin

  I.say("Cancel change password action");
  I.closeCurrentTab();
  await cancelChangePassword(I, Document);

  checkUrlDoesNotWork(I, changePasswordUrl, false);
});


Scenario('delete cache objects to prevent logon form wrong password counting, 3 @singlethread', async ({ I }) => {
  deleteCacheObjects(I);
});


Scenario('forgotten password - administration - invalid users option test - VIA email @singlethread', async ({I, Document, TempMail }) => {
  await TempMail.login(emailName);
  await TempMail.destroyInbox();
  I.logout();
  I.amOnPage("/admin/logon/");

  I.say('Wait 10 seconds for next login');
  I.wait(11);
  setValueToRecoverPassword(I, email);

  // Do not close email tab - we will return there
  await changePasswordEmail(I, Document, TempMail, emailName, false);
  const changePasswordUrl = await I.grabCurrentUrl();
  I.say(`Url for password change: ${changePasswordUrl}`);

  await checkUsersSelectOptions(I, ['sameD'], true); //only one login is VALID

  I.say("Cancel change password action");
  I.closeCurrentTab();
  await cancelChangePassword(I, Document);

  checkUrlDoesNotWork(I, changePasswordUrl, true);
});

Scenario('Restoration of changes, approval of users @singlethread', async ({ I , DT, DTE, TempMail }) => {
  I.relogin('admin');
  I.amOnPage('/admin/v9/users/user-list/');
  DT.filterContains("email", "samemail"+TempMail.getTempMailDomain());

  I.say("user sameA");
  I.click("sameA");
  DTE.waitForEditor();
  I.checkOption("#DTE_Field_authorized_0");
  DTE.save();

  I.say("user sameB");
  I.click("sameB");
  DTE.waitForEditor();
  I.clickCss("#pills-dt-datatableInit-rightsTab-tab");
  I.checkOption("#DTE_Field_admin_0");
  DTE.save();

  I.say("user sameC");
  I.click("sameC");
  DTE.waitForEditor();
  I.fillField("#DTE_Field_allowLoginEnd", "");
  I.checkOption("#DTE_Field_authorized_0");
  DTE.save();

  I.logout();
});

Scenario('Verification of password reset completion and audit log removal @singlethread', async ({ I, Document, TempMail, DT }) => {
  I.logout();

  I.say('Wait 10 seconds for next login');
  I.wait(11);

  setValueToRecoverPassword(I, email);

  const lastId  = await checkAudit(I, DT);

  await TempMail.login(emailName);
  TempMail.openLatestEmail();

  await cancelChangePassword(I, Document);

  const changePasswordUrl = await I.grabCurrentUrl();

  I.switchToPreviousTab();
  I.closeOtherTabs();

  TempMail.closeEmail();
  //This switchTo is necessary, for unknown reason
  I.switchTo();
  await TempMail.destroyInbox();

  checkUrlDoesNotWork(I, changePasswordUrl, true);

  await checkAudit(I, DT, lastId);
});


Scenario('delete cache objects to prevent logon form wrong password counting, 4 @singlethread', async ({ I }) => {
  deleteCacheObjects(I);
});


async function checkAudit(I, DT, lastId = null){
  I.relogin('admin');
  I.amOnPage('/admin/v9/apps/audit-search/');
  if (lastId){
    I.say('Check if audit log was deleted');
    DT.filterId('id', lastId );
    const mails = ['Same A Mail', 'Same B Mail', 'Same C Mail', 'Same D Mail'];
    mails.forEach(mail => I.dontSee(mail));
  }
  else {
    I.say('Check last audit log');
    DT.filterSelect('logType', 'USER_CHANGE_PASSWORD');
    const lastId = await I.grabAttributeFrom('tbody tr:first-child', 'id');
    DT.filterId('id', lastId );
    I.see('Same D Mail');
    I.logout();
    return lastId;
  }
}

async function changePasswordEmail(I, Document, TempMail, emailName, closeAndDestroy = true) {
  I.say('Opening the email for password reset');
  await TempMail.login(emailName);
  TempMail.openLatestEmail();
  let openTabs = await I.grabNumberOfOpenTabs();
  I.click(locate('a').withText('Ak si chcete zmeniť heslo, kliknite sem do 30 minút.'));

  if(closeAndDestroy === true) {
    TempMail.closeEmail();
    await TempMail.destroyInbox();
  }

  await Document.waitForTab(openTabs+1);
  I.switchToNextTab();
  if(closeAndDestroy === true) {
    I.closeOtherTabs();
  }
}

function changePasswordPlusTests(I, randomPassword, isAdminSection) {

  let seeErrMsg = isAdminSection === true ? passwordRequirementsErr_admin : passwordRequirementsErr_nonAdmin;

  I.say('Testujem zmenu hesla na nedostatočné, bez číslice');
  changePassword(I, withoutDigitPassword, seeErrMsg, isAdminSection);
  I.say('Testujem zmena hesla na nedostatočné, bez velkeho pismena');
  changePassword(I, withoutCapitalPassword, seeErrMsg, isAdminSection);
  I.say('Testujem zmena hesla na nedostatočné, kratke heslo');
  changePassword(I, tooShortPassword, seeErrMsg, isAdminSection);
  I.say('Testujem zle zopakované heslo');
  changePassword(I, randomPassword, isAdminSection === true ? passwordRetypeErr_admin : passwordRetypeErr_nonAdmin, isAdminSection, randomPassword + "dummy");
  I.say('Dobre zmenene heslo');
  changePassword(I, randomPassword, "Zmena hesla úspešne dokončená", isAdminSection);
}

function changePassword(I, newPassword, msg, isAdminSection, secondPassword = null) {
  I.say('Changing password in form. Is admin section: ' + isAdminSection);
  I.fillField('input[type="password"][name="newPassword"]', newPassword);

  let retypePassword = isAdminSection === true ? 'input[type="password"][name="retypeNewPassword"]' : 'input[type="password"][name="retypePassword"]';

  if(secondPassword == null) {
    I.fillField(retypePassword, newPassword);
  } else {
    I.fillField(retypePassword, secondPassword);
  }

  I.clickCss( isAdminSection === true ? 'button#login-submit' : 'input[type="submit"].button.btn.btn-info' );
  I.waitForText(msg, 10);
  //this text is shown only after logon if the old password is not valid anymore
  I.dontSee("Vaše heslo nespĺňa bezpečnostné nastavenia aplikácie, alebo mu vypršala platnosť.");
}

function getRandomElement(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

function checkUrlDoesNotWork(I, changePasswordUrl, isAdminSection) {
  I.amOnPage(changePasswordUrl);
  I.see("Nedá sa zresetovať heslo. Skontrolujte, či adresa v prehliadači je totožná s adresou, ktorú ste dostali v emaily.");
  I.dontSeeElement('select#selectedLogin');
  I.dontSeeElement('input[type="password"][name="newPassword"]');
  I.dontSeeElement(isAdminSection === true ? 'input[type="password"][name="retypeNewPassword"]' : 'input[type="password"][name="retypePassword"]');
}

function setValueToRecoverPassword(I, valueToRecoverPassword, isAdminSection = true) {
  if(isAdminSection === true)
    I.clickCss('.btn.lost-password');
  else
    I.clickCss('a[href="#sendPassword"]');

  I.waitForVisible('input[name="loginName"]');
  I.fillField('input[name="loginName"]', valueToRecoverPassword);
  I.clickCss('button#register-submit-btn');
  I.see('Ak Vaše konto existuje heslo Vám bolo zaslané na e-mailovú adresu.');
}

async function checkUsersSelectOptions(I, users, isAdminSection) {
  let select = 'select#selectedLogin';

  I.waitForVisible(select, 10);
  const options = await I.grabTextFromAll(select + ' option');

  //For some reasons, if there is only 1 value, it is returned twice
  const uniqueOptions = [...new Set(options)];

  await uniqueOptions.sort();
  await users.sort();
  I.say("options: "+uniqueOptions);
  I.say("users: "+users);
  assert.deepStrictEqual(uniqueOptions, users, 'The options in the select do not match the expected values of users');
}

async function cancelChangePassword(I, Document) {
  I.waitForText('Ak ste nepožiadali o zmenu hesla môžete túto akciu zrušiť kliknutím sem.');
  let openTabs = await I.grabNumberOfOpenTabs();
  I.click(locate('a').withText('Ak ste nepožiadali o zmenu hesla môžete túto akciu zrušiť kliknutím sem.'));
  //wait for tab open
  await Document.waitForTab(openTabs+1);

  I.switchToNextTab();
  I.waitForText('Požiadavka na zmenu hesla bola zrušená.');
}