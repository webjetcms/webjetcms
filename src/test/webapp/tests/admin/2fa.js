Feature('admin.2fa');

const crypto = require('crypto');
const { PNG } = require('pngjs');
const jsQR = require('jsqr');

var randomLogin;
const passwordRequirementsErr_nonAdmin = 'Vaše heslo nespĺňa bezpečnostné nastavenia aplikácie';

function base32tohex(base32) {
	let base32chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';
	let bits = '';
	let hex = '';
	for (let i = 0; i < base32.length; i++) {
		let val = base32chars.indexOf(base32.charAt(i).toUpperCase());
		bits += (Array(5).fill(0).join('') + val.toString(2)).slice(-5);
	}
	for (let i = 0; i < bits.length - 3; i += 4) {
		let chunk = bits.substr(i, 4);
		hex = hex + parseInt(chunk, 2).toString(16);
	}
	return hex;
}

function getOTP(secretValue) {
	var mssg = Buffer.from((Array(16).fill(0).join('') + (Math.floor(Math.round(new Date().getTime() / 1000) / 30)).toString(16)).slice(-16), 'hex');
	var key = Buffer.from(base32tohex(secretValue), 'hex');
	var hmac = crypto.createHmac('sha1', key);
	hmac.setEncoding('hex');
	hmac.update(mssg);
	hmac.end();
	hmac = hmac.read();
	return ((parseInt((hmac.substr(parseInt(hmac.slice(-1), 16) * 2, 8)), 16) & 2147483647) + '').slice(-6);
}

Scenario('Testovanie dvojfaktorovej autentifikacie', async ({ I, DT, DTE }) =>{
    I.say('Prihlasenie sa ako admin');
    I.relogin('admin');
    I.amOnPage('/admin/v9/users/user-list/');

    I.say('Vytvorenie noveho testovacieho pouzivatela');
    DT.filterEquals('login', 'testerga');
    I.waitForText("Záznamy 1 až 1 z 1", 10, "#datatableInit_info");
    I.see("Google Authenticate", "div.dt-scroll-body td.required");
    I.clickCss("td.dt-select-td:first-child");
    I.clickCss('button.btn-duplicate');
    DTE.waitForEditor();
    randomLogin = 'testerga' + I.getRandomText();
    I.fillField('#DTE_Field_editorFields-login', randomLogin);
    I.fillField('#DTE_Field_password', secret(I.getDefaultPassword()));
    DTE.save(undefined, true);

    I.say('Prihlasenie sa do nového účtu');
    I.relogin(randomLogin, true, false);
    I.dontSee(passwordRequirementsErr_nonAdmin);

    I.say('Zapnutie dvojstupňového overovania');
    I.amOnPage("/admin/v9/");
    I.clickCss("#dropdownMenuUser");
    I.click(locate("li").withText("Dvojstupňové overovanie"));
    I.wait(1);
    I.switchToNextTab();
    I.waitForElement('#gauthCheckbox', 10);
    I.clickCss('#gauthCheckbox');

    I.say('Overenie QR kódu');
    I.seeElement('img[alt="Scan me!"]');

    I.wait(2);

    //
    I.say("Verify QR code is valid");

    const secretValue = await I.grabTextFrom('#secret');
    const qrCodeImageData = await I.grabAttributeFrom('img[alt="Scan me!"]', 'src');
    // Decode locally so the test does not depend on a public service or disclose the OTP secret.
    const png = PNG.sync.read(Buffer.from(qrCodeImageData.split(',')[1], 'base64'));
    const qrCode = jsQR(new Uint8ClampedArray(png.data), png.width, png.height);
    I.assertTrue(qrCode !== null, 'The displayed QR code must be readable');
    const otpUrl = qrCode && new URL(qrCode.data);
    I.assertTrue(otpUrl?.protocol === 'otpauth:' && otpUrl.hostname === 'totp', 'The QR code must configure TOTP');
    I.assertTrue(otpUrl?.searchParams.get('secret') === secretValue, 'The QR code must contain the displayed secret');

    I.clickCss("#btnOK");

    //
    I.say('Overenie dvojstupňového overovania');
    I.relogin(randomLogin, true, false);
    I.see('Pre váš účet je zapnuté dvojstupňové overovanie. Zadajte kód z aplikácie Authenticator:');

    //
    I.say('Prihlasujem sa s nesprávnym kódom');
    I.fillField('token', "1");
    I.clickCss('#login-submit');
    I.seeElement(locate('li').withText('Zadaný kód nie je správny.'));

    I.say('Prihlasujem sa s nesprávnym kódom');
    I.fillField('token', "5");
    I.clickCss('#login-submit');
    I.seeElement(locate('li').withText('Pre nesprávne zadané prihlasovacie údaje je prihlásenie na 10+ sekúnd zablokované'));

    //wait for block to expire, because we need to test correct code login
    I.wait(10);

    //
    I.say('Prihlasujem so správnym kódom');
    I.fillField('token', getOTP(secretValue));
    I.clickCss('#login-submit');

    //
    I.say("Maybe OTP is timeouted, so we need to put it again");
    let errorShown = await I.grabNumberOfVisibleElements("#login div.alert-danger");
    let failsafeCounter = 0;
    while (errorShown > 0 && failsafeCounter < 5) {
      I.say("Re-entering OTP, attempt " + (failsafeCounter + 1));
      I.wait(10);
      I.fillField('token', getOTP(secretValue));
      I.clickCss('#login-submit');
      failsafeCounter++;
      errorShown = await I.grabNumberOfVisibleElements("#login div.alert-danger");
    }

    I.dontSee("Zadaný kód nie je správny.");
    I.waitForElement(locate('h1.md-dashboard__greeting').withText('Vitajte späť, Tester Google Authenticate'), 10);

    I.logout();
});

function deleteCacheObjects(I) {
  I.relogin("admin");
  I.amOnPage("/admin/v9/settings/cache-objects/");
  I.clickCss("button.btn-delete-all");
  I.waitForElement("div.toast-message");
  I.clickCss("div.toast-message button.btn-primary");
  I.closeOtherTabs();
}

Scenario("Delete 2fa test user", ({ I, DT, DTE }) => {
    //
    I.relogin('admin')
    I.amOnPage('/admin/v9/users/user-list/');

    //
    I.say('Zmazanie testovacieho pouzivatela');
    DT.filterContains('login', randomLogin);
    I.clickCss("td.dt-select-td");
    I.click("button.buttons-remove");
    DTE.waitForEditor();
    I.click("Zmazať", "div.DTE_Action_Remove");
    DTE.waitForLoader();
    DTE.waitForModalClose('datatableInit_modal');

    //delete cache keys
    deleteCacheObjects(I);

    I.logout();
});

Scenario('overenie dropdown menu', ({ I }) => {
    I.relogin("admin");

    I.amOnPage("/admin/v9/");
    I.clickCss("#dropdownMenuUser");
    I.see("Profil", "ul.dropdown-menu.show");
    I.see("Dvojstupňové overovanie", "ul.dropdown-menu.show");
    I.see("Správa šifrovacích kľúčov");
    I.see("Odhlásenie", "ul.dropdown-menu.show");
});

Scenario('overenie vyzvy pre zadanie kodu', ({ I }) => {
    I.relogin("testerga", true, false);

    I.see("Pre váš účet je zapnuté dvojstupňové overovanie.");
    I.click("Odhlásiť sa");

    I.see("Prihlásiť sa");
});

Scenario('odhlasenie', ({ I }) => {
    I.logout();
});

Scenario('overenie nezobrazenie spravy sifrovacich klucov', ({ I }) => {
    I.relogin("tester2");
    I.amOnPage("/admin/v9/?removePerm=cmp_form");
    I.clickCss("#dropdownMenuUser");
    I.see("Profil", "ul.dropdown-menu.show");
    I.see("Dvojstupňové overovanie", "ul.dropdown-menu.show");
    I.dontSee("Správa šifrovacích kľúčov");
    I.see("Odhlásenie", "ul.dropdown-menu.show");
});

Scenario('odhlasenie2', ({ I }) => {
    I.logout();
});

Scenario('forced 2fa QR code scan at logon', ({ I }) => {
    //it's faked by mobile_device set to aaa value - faking isGoogleAuthRequiredForAdmin=true
    I.relogin("testerga2", true, false);

    I.waitForText("Pre využívanie dvojstupňového overenia si na váš telefón nainštalujte aplikáciu", 10);
    I.see("overenie zadaním kľúča: ");
    I.see("Zadajte zobrazený kód v aplikácii");
    I.seeElement("#qrImage img[alt='Scan me!']");

    I.click("Odhlásiť sa");

    I.see("Prihlásiť sa");
});

