Feature('admin.2fa');

const http = require('http');
const crypto = require('crypto');

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

/**
 * Parse data:image/png URL to file data
 * @param {*} dataurl
 * @returns
 */
function dataURLtoFile(dataurl) {
  var arr = dataurl.split(','),
      mime = arr[0].match(/:(.*?);/)[1],
      bstr = atob(arr[arr.length - 1]),
      n = bstr.length,
      u8arr = new Uint8Array(n);
  while(n--){
      u8arr[n] = bstr.charCodeAt(n);
  }
  return u8arr;
}

/**
 * Make HTTP request to verify QR code
 * @param {*} qrCodeImageData
 * @returns
 */
function doRequest(qrCodeImageData) {
  return new Promise(function (resolve, reject) {
    const file = dataURLtoFile(qrCodeImageData);
    const options = {
      hostname: 'api.qrserver.com',
      port: 80,
      path: '/v1/read-qr-code/',
      method: 'POST',
      headers: {
        'Content-Type': 'multipart/form-data; boundary=boundary',
      },
    };
    const req = http.request(options, (res) => {
      let responseData = '';
      res.on('data', (chunk) => {
        responseData += chunk;
      });

      res.on('end', () => {
        resolve(responseData);
      });
    });
    req.write(
      `--boundary\r\nContent-Disposition: form-data; name="file"; filename="frame.png"\r\nContent-Type: image/png\r\n\r\n`
    );
    req.write(file);
    req.end(`\r\n--boundary--`);
    req.on('error', (error) => {
      console.log("Request error:");
      console.error(error);
      resolve(error);
    });
  });
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
    DTE.save();

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
    I.say("secret: "+secretValue);

    const qrCodeImageData = await I.grabAttributeFrom('img[alt="Scan me!"]', 'src');
    let responseData = await doRequest(qrCodeImageData);
    I.say("RESPONSE: "+responseData);
    const responseObj = JSON.parse(responseData);
    let qrCodeData = responseObj[0].symbol[0].data;
    I.assertContain(qrCodeData, secretValue, 'Pri testovaní zobrazenia QR kódu sa nenašiel kľúč v kóde');

    I.clickCss("#btnOK");

    //
    I.say('Overenie dvojstupňového overovania');
    I.relogin(randomLogin, true, false);
    I.see('Pre váš účet je zapnuté dvojstupňové overovanie. Zadajte kód z aplikácie Authenticator:');

    //
    I.say('Prihlasujem sa s nesprávnym kódom');
    I.fillField('token', 1);
    I.clickCss('#login-submit');
    I.seeElement(locate('li').withText('Zadaný kód nie je správny.'));

    //
    I.say('Prihlasujem so správnym kódom');
    I.fillField('token', getOTP(secretValue));
    I.clickCss('#login-submit');

    //
    I.say("Maybe OTP is timeouted, so we need to put it again");
    const errorShown = await I.grabNumberOfVisibleElements("#login div.alert-danger");
    if (errorShown > 0) {
      I.say("Re-entering OTP");
      I.fillField('token', getOTP(secretValue));
      I.clickCss('#login-submit');
    }

    I.dontSee("Zadaný kód nie je správny.");
    I.waitForElement(locate('h2').withText('Vitajte, Tester Google Authenticate'), 10);

    I.logout();
});

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

