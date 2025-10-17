Feature('admin.2fa');

const http = require('http');
const crypto = require('crypto');

var randomLogin;
const passwordRequirementsErr_nonAdmin = 'Vaše heslo nespĺňa bezpečnostné nastavenia aplikácie';

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

Scenario('Testovanie dvojfaktorovej autentifikacie', async ({ I, DT, DTE, Document, i18n }) =>{
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

    Document.screenshot("/redactor/admin/2fa_part_1.png", 1280, 450, "#dropdownMenuUser");

    I.clickCss("#dropdownMenuUser");
    I.waitForVisible( locate("a.dropdown-item").withText(i18n.get("Two-Step Verification")), 10);

    let path = '/admin/2factorauth.jsp';
    Document.screenshot("/redactor/admin/2fa_part_2.png", 1280, 450, 'a[onclick*="WJ.openPopupDialog(\'' + path + '\')"]');

    I.amOnPage(path);
    I.waitForElement('#gauthCheckbox', 10);

    Document.screenshot("/redactor/admin/2fa_part_3.png", 1280, 450);

    I.clickCss('#gauthCheckbox');
    I.seeElement('img[alt="Scan me!"]');

    Document.screenshot("/redactor/admin/2fa_part_4.png", 1280, 600);

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

    I.say("Go to login for image");
    I.amOnPage('/logoff.do?forward=/admin/logon/');
    I.fillField("username", randomLogin);
    I.fillField("password", secret("123456789"));

    Document.screenshot("/redactor/admin/2fa_part_5.png", 1280, 600);


    I.say('Overenie dvojstupňového overovania');
    I.relogin(randomLogin, true, false);
    I.see('Authenticator:');

    I.fillField('token', secret("548376"));

    Document.screenshot("/redactor/admin/2fa_part_6.png", 1280, 600);

    I.clickCss('#login-submit');

    Document.screenshot("/redactor/admin/2fa_part_7.png", 1280, 600);
});

Scenario("Delete 2fa test user", ({ I, DT, DTE, i18n }) => {
    //
    I.relogin('admin')
    I.amOnPage('/admin/v9/users/user-list/');

    //
    I.say('Zmazanie testovacieho pouzivatela');
    DT.filterContains('login', randomLogin);
    I.clickCss("td.dt-select-td");
    I.click("button.buttons-remove");
    DTE.waitForEditor();
    I.click(i18n.get("Delete"), "div.DTE_Action_Remove");
    DTE.waitForLoader();
    DTE.waitForModalClose('datatableInit_modal');

    I.logout();
});