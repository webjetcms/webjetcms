const test = require("node:test");
const assert = require("node:assert/strict");

const utilities = import("../../../main/webapp/admin/v9/npm_packages/webjetdatatables/jsoneditor-utils.mjs");

test("JSON editor accepts nested objects and handles optional whitespace", async () => {
    const {validateJsonObject} = await utilities;
    assert.deepEqual(validateJsonObject('{"items":[null,true,1,{"name":"text"}]}'), {valid: true});
    assert.deepEqual(validateJsonObject(" \n\t "), {valid: true});
    assert.deepEqual(validateJsonObject(" \n\t ", true), {valid: false, error: "required"});
});

test("JSON editor rejects every non-object root", async () => {
    const {validateJsonObject} = await utilities;
    for (const text of ["null", "[]", "true", "false", "42", '"text"']) {
        assert.deepEqual(validateJsonObject(text), {valid: false, error: "object"});
    }
});

test("JSON editor rejects incomplete and concatenated objects", async () => {
    const {validateJsonObject} = await utilities;
    for (const text of ['{"a":1,}', '{"a":}', '{} {}', '{} trailing', '{"a":NaN}', '{"a":undefined}']) {
        const result = validateJsonObject(text);
        assert.equal(result.valid, false, text);
        assert.equal(result.error, "invalid", text);
    }
});

test("Browser and server share the supported extended object syntax", async () => {
    const {validateJsonObject, formatJsonObject} = await utilities;
    const fixtures = require("../../resources/sk/iway/iwcm/components/customfields/jsoneditor-syntax.json");
    for (const text of fixtures.valid) {
        assert.equal(validateJsonObject(text).valid, true, text);
        const formatted = formatJsonObject(text);
        assert.equal(validateJsonObject(formatted).valid, true, formatted);
        assert.equal(formatJsonObject(formatted), formatted, "Formatting must remain stable: " + text);
    }
    for (const text of fixtures.invalid) {
        assert.equal(validateJsonObject(text).valid, false, text);
        assert.equal(formatJsonObject(text), text, "Invalid input must remain untouched");
    }
    assert.equal(Object.prototype.polluted, undefined, "Parsing must not execute object definitions");
});

test("Formatting preserves apostrophes, comments and their closing-brace boundaries", async () => {
    const {formatJsonObject} = await utilities;
    const source = "{title:'test',data-toggle:'tooltip',id:9007199254740993,action:{content:'{Question?}' // keep } braces and  spaces  \n}}";
    assert.equal(formatJsonObject(source), "{\n  title: 'test',\n  data-toggle: 'tooltip',\n  id: 9007199254740993,\n  action: {\n    content: '{Question?}' // keep } braces and  spaces  \n  }\n}");
});

test("Syntax errors report the original position after comments and single-quoted strings", async () => {
    const {validateJsonObject} = await utilities;
    assert.deepEqual(validateJsonObject("{\n // comment\n title: 'test',\n data-toggle: ]\n}"), {valid: false, error: "invalid", line: 4, column: 15});
});

test("Formatting keeps trailing comments beside the property they describe", async () => {
    const {formatJsonObject, validateJsonObject} = await utilities;
    const source = `{
    'user' : {
        'id' : '{444555678}', //”uuId”
        'clientId' : '{44AAA4555678}',//CookieId
        'loggedId' : '{444555678}',
        'snId' : '{11133487}',// SSO_OUSER_ID || user?code
        'missionsAvailable' : '{2}',
        'missionsCompleted' : '{8}',
        'paymentType' : '{prepaid}',//prepaid/postpaid
        'geoDefault' : '{1}', //only for geo offers like FTTH geo offer
        'geoActual' : '{2}', //only for geo offers like FTTH geo offer
        'role' : '{customerAdmin}'
    }
}`;
    const expected = `{
  'user': {
    'id': '{444555678}', //”uuId”
    'clientId': '{44AAA4555678}', //CookieId
    'loggedId': '{444555678}',
    'snId': '{11133487}', // SSO_OUSER_ID || user?code
    'missionsAvailable': '{2}',
    'missionsCompleted': '{8}',
    'paymentType': '{prepaid}', //prepaid/postpaid
    'geoDefault': '{1}', //only for geo offers like FTTH geo offer
    'geoActual': '{2}', //only for geo offers like FTTH geo offer
    'role': '{customerAdmin}'
  }
}`;
    for (const newline of ["\n", "\r\n", "\r"]) {
        const formatted = formatJsonObject(source.replaceAll("\n", newline));
        assert.equal(formatted, expected);
        assert.equal(validateJsonObject(formatted).valid, true);
        assert.equal(formatJsonObject(formatted), formatted, "Repeated formatting must retain comment placement");
    }
});

test("Formatting distinguishes standalone comments from inline comments in nested containers", async () => {
    const {formatJsonObject, validateJsonObject} = await utilities;
    const source = "{ // object\nitems:[ // array\n1,/* first */ // trailing\n// second\n2\n// array end\n], // items\nlast:{} // last\n}\n// footer";
    const expected = "{ // object\n  items: [ // array\n    1, /* first */ // trailing\n    // second\n    2\n    // array end\n  ], // items\n  last: {} // last\n}\n// footer";
    assert.equal(formatJsonObject(source), expected);
    assert.equal(validateJsonObject(expected).valid, true);
    assert.equal(formatJsonObject(expected), expected);
});

test("JSON formatting preserves numeric spelling, duplicate keys and order", async () => {
    const {formatJsonObject} = await utilities;
    const text = '{"id":9007199254740993,"id":1.2300e+42,"negative":-0,"empty":{},"items":[1,[]]}';
    assert.equal(formatJsonObject(text), '{\n  "id": 9007199254740993,\n  "id": 1.2300e+42,\n  "negative": -0,\n  "empty": {},\n  "items": [\n    1,\n    []\n  ]\n}');
});

test("JSON formatting preserves string escapes, whitespace and HTML as literal text", async () => {
    const {formatJsonObject} = await utilities;
    const value = String.raw`"  </textarea>&quot; 😀 \u0061 \n \" \\ , : {} []  "`;
    const text = '{\n "text" : ' + value + ' , "next" : true\n}';
    assert.equal(formatJsonObject(text), '{\n  "text": ' + value + ',\n  "next": true\n}');
    assert.equal(formatJsonObject(formatJsonObject(text)), formatJsonObject(text));
});

test("JSON formatting leaves empty, invalid and non-object values untouched", async () => {
    const {formatJsonObject} = await utilities;
    for (const text of [" \n", '{"a":}', "null", "[]", '"text"']) assert.equal(formatJsonObject(text), text);
});
