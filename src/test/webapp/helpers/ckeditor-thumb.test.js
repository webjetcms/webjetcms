const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');
const acorn = require('acorn');

const adminDirectory = path.resolve(__dirname, '../../../main/webapp/admin/v9');
const editorSource = fs.readFileSync(path.join(adminDirectory, 'src/js/datatables-ckeditor.js'), 'utf8');
const thumbSource = editorSource.slice(editorSource.indexOf('// Thumb tab - configure'), editorSource.indexOf('//console.log("dialogDefinition=", dialogDefinition);'));
const webjetSource = fs.readFileSync(path.join(adminDirectory, 'src/js/webjet.js'), 'utf8');

/** Extracts production functions without changing their implementation. */
function functionsFromSource(source, names) {
    const result = [];
    function visit(node) {
        if (!node || typeof node !== 'object') return;
        if (node.type === 'FunctionDeclaration' && names.includes(node.id.name)) result.push(source.slice(node.start, node.end));
        for (const value of Object.values(node)) {
            if (Array.isArray(value)) value.forEach(visit);
            else if (value && typeof value === 'object') visit(value);
        }
    }
    visit(acorn.parse(source, {ecmaVersion: 'latest', sourceType: 'module'}));
    return result.join('\n');
}

const urlFunctions = functionsFromSource(webjetSource, ['urlAddParam', 'urlUpdateParam', 'urlRemoveParam', 'urlGetParam']);
const translations = {thumbIp0: 'Maximum size', thumbIp1: 'Fixed Width', thumbIp4: 'Fixed Width and Height Filled with Color - Centered', thumbIp5: 'Centered with Aspect Ratio - Scaled'};

/** Runs the actual thumbnail tab definition and callbacks with dialog field values. */
function createFixture(sizes, mode = 'strict') {
    let definition;
    const context = vm.createContext({
        URLSearchParams,
        ev: {editor: {config: {thumbServletAllowedSizeMode: mode, thumbServletAllowedSizes: sizes}}},
        that: {translate: key => translations[key] || key},
        dialogDefinition: {addContents: value => { definition = value; }}
    });
    vm.runInContext(urlFunctions + '\nconst WJ = {urlAddParam, urlUpdateParam, urlRemoveParam, urlGetParam, translate: key => key};\n' + thumbSource, context);
    const fields = {};
    const dialog = {getContentElement: (tab, id) => fields[tab + '.' + id]};
    function addField(tab, id, initialValue, config = {}) {
        fields[tab + '.' + id] = {
            value: initialValue,
            visible: true,
            getValue() { return this.value; },
            setValue(value, silent = false) {
                this.value = value;
                if (!silent && config.onChange) config.onChange.call(this);
            },
            getDialog: () => dialog,
            getElement() { return {hide: () => { this.visible = false; }, show: () => { this.visible = true; }}; },
            validate() { return config.validate ? config.validate.call(this) : true; },
            setup(attributes, type = 1) { config.setup.call(this, type, {getAttribute: key => attributes[key]}); }
        };
    }
    addField('info', 'txtUrl', '/images/autotest-thumb.jpg?v=123');
    addField('info', 'txtWidth', '');
    addField('info', 'txtHeight', '');
    addField('advanced', 'txtGenClass', 'img-fluid fixedSize-160-160-5');
    for (const field of definition.elements) addField('thumb', field.id, field.default, field);
    return {context, definition, dialog, fields, select: fields['thumb.thumbAllowedSize'], commit: () => context.generateThumbClass(dialog)};
}

test('strict mode offers one numerically sorted select with readable labels and no duplicate or malformed entries', () => {
    const fixture = createFixture('416x276ip5\n159x159ip1, 413x275\r\n180x180ip5,159x159ip1,bad,0x200,20x20ip5');
    assert.deepEqual(Array.from(fixture.definition.elements, field => field.id), ['thumbAllowedSize']);
    assert.deepEqual(Array.from(fixture.definition.elements[0].items, item => item[1]), ['', '20x20ip5', '159x159ip1', '180x180ip5', '413x275', '416x276ip5']);
    assert.equal(fixture.definition.elements[0].items[2][0], '159 x 159 (1 - Fixed Width)');
    assert.equal(fixture.definition.elements[0].items[4][0], '413 x 275 (0 - Maximum size)');
});

test('selection updates the URL and commit persists every allowed parameter without changing unrelated classes', () => {
    const fixture = createFixture('730x401ip5ncff00ffq90,413x275,380x200ip4');
    fixture.select.setValue('730x401ip5ncff00ffq90');
    assert.equal(fixture.fields['advanced.txtGenClass'].getValue(), 'img-fluid fixedSize-160-160-5', 'Only commit should change the CSS class');
    fixture.commit();
    assert.equal(fixture.fields['advanced.txtGenClass'].getValue(), 'img-fluid fixedSize-730-401-5-ff00ff-true-q90');
    let url = new URL(fixture.fields['info.txtUrl'].getValue(), 'https://example.test');
    assert.equal(url.pathname, '/thumb/images/autotest-thumb.jpg');
    assert.deepEqual(Object.fromEntries(url.searchParams), {v: '123', w: '730', h: '401', ip: '5', c: 'ff00ff', q: '90', noip: 'true'});
    assert.equal(fixture.context.findAllowedThumbSize(fixture.fields['advanced.txtGenClass'].getValue(), url.href), '730x401ip5ncff00ffq90');

    fixture.select.setValue('413x275');
    fixture.commit();
    assert.equal(fixture.fields['advanced.txtGenClass'].getValue(), 'img-fluid fixedSize-413-275-0');
    url = new URL(fixture.fields['info.txtUrl'].getValue(), 'https://example.test');
    assert.deepEqual(Object.fromEntries(url.searchParams), {v: '123', w: '413', h: '275', ip: '0'});

    fixture.select.setValue('380x200ip4');
    fixture.commit();
    assert.equal(fixture.fields['advanced.txtGenClass'].getValue(), 'img-fluid fixedSize-380-200-4');
    assert.equal(new URL(fixture.fields['info.txtUrl'].getValue(), 'https://example.test').searchParams.has('c'), false, 'An omitted color must not inherit a previous selection');
});

test('setup restores allowed images from classes or URLs and normalizes fixed-width and fixed-height cache dimensions', () => {
    const fixture = createFixture('159x159ip1,180x180ip2,413x275,730x401ip5ncff00ffq90');
    const cases = [
        [{class: 'img-fluid fixedSize-159-0-1', src: '/images/autotest.jpg'}, '159x159ip1'],
        [{class: 'fixedSize-0-180-2', src: '/images/autotest.jpg'}, '180x180ip2'],
        [{class: 'fixedSize-413-275-0', src: '/thumb/images/autotest.jpg?w=413&h=275&ip=0&c=ffffff'}, '413x275'],
        [{src: '/thumb/images/autotest.jpg?w=413&h=275'}, '413x275'],
        [{class: 'fixedSize-730-401-5-ff00ff-true-q90', src: '/images/autotest.jpg'}, '730x401ip5ncff00ffq90'],
        [{class: 'fixedSize-99-99-5', src: '/images/autotest.jpg'}, '']
    ];
    for (const [attributes, expected] of cases) {
        fixture.select.setup(attributes);
        assert.equal(fixture.select.getValue(), expected, JSON.stringify(attributes));
    }
    fixture.select.setup(cases[0][0]);
    fixture.select.setup({href: '/autotest-link'}, 2);
    assert.equal(fixture.select.getValue(), '159x159ip1', 'Link setup must not replace image settings');
});

test('strict mode requires a configured option and never commits a missing or arbitrary value', () => {
    for (const config of ['', '159x159ip1']) {
        const fixture = createFixture(config);
        for (const value of ['', '999x999ip5']) {
            fixture.select.setValue(value);
            assert.equal(fixture.select.validate(), 'editor.image.allowedSizeRequired.js');
            fixture.commit();
            assert.equal(fixture.fields['info.txtUrl'].getValue(), '/images/autotest-thumb.jpg?v=123');
            assert.equal(fixture.fields['advanced.txtGenClass'].getValue(), 'img-fluid fixedSize-160-160-5');
        }
    }
});

test('other modes keep the original fields and mode zero visibility', () => {
    for (const mode of ['allow', 'learn', 'check', 'deny', '']) {
        const fixture = createFixture('159x159ip1', mode);
        assert.deepEqual(Array.from(fixture.definition.elements, field => field.id), ['thumbIpMode', 'thumbWidth', 'thumbHeight', 'thumbBackgroundColor', 'thumbNoIp']);
        fixture.fields['thumb.thumbIpMode'].setValue('0');
        assert.equal(fixture.fields['thumb.thumbWidth'].visible, true);
        assert.equal(fixture.fields['thumb.thumbHeight'].visible, true);
        assert.equal(fixture.fields['thumb.thumbBackgroundColor'].visible, false);
        assert.equal(fixture.fields['thumb.thumbNoIp'].visible, false);
        fixture.fields['thumb.thumbWidth'].setValue('300');
        fixture.fields['thumb.thumbHeight'].setValue('200');
        fixture.commit();
        assert.equal(fixture.fields['advanced.txtGenClass'].getValue(), 'img-fluid fixedSize-300-200-0');
    }
});

test('replacing an image keeps quality from fixedSize and removes parameters from a previous size', () => {
    const fixture = createFixture('730x401ip5ncff00ffq90,413x275');
    const pug = fs.readFileSync(path.join(adminDirectory, 'views/pages/files/wj_image.pug'), 'utf8');
    const selectLinkSource = pug.slice(pug.indexOf('function selectLink('), pug.indexOf('function copyAltToTitle('));
    const input = {value: ''};
    fixture.context.window = {parent: {CKEDITOR: {dialog: {getCurrent: () => fixture.dialog}}}};
    fixture.context.GetE = () => input;
    fixture.context.updateValuesToCk = () => {};
    vm.runInContext(selectLinkSource, fixture.context);
    fixture.select.setValue('730x401ip5ncff00ffq90');
    fixture.commit();
    fixture.context.selectLink('/images/autotest-replacement.jpg', 456);
    let url = new URL(input.value, 'https://example.test');
    assert.equal(url.pathname, '/thumb/images/autotest-replacement.jpg');
    assert.deepEqual(Object.fromEntries(url.searchParams), {w: '730', h: '401', ip: '5', c: 'ff00ff', noip: 'true', q: '90', v: '456'});

    fixture.select.setValue('413x275');
    fixture.commit();
    fixture.context.selectLink('/thumb/images/autotest-replacement.jpg?w=730&h=401&ip=5&c=ff00ff&noip=true&q=90');
    url = new URL(input.value, 'https://example.test');
    assert.deepEqual(Object.fromEntries(url.searchParams), {w: '413', h: '275', ip: '0'});
});

test('CKEditor renders the strict select, blocks an unlisted size and restores the committed selection', {timeout: 30000}, async t => {
    const {chromium} = require('playwright');
    const browser = await chromium.launch({headless: true});
    t.after(() => browser.close());
    const page = await browser.newPage();
    const webappDirectory = path.resolve(adminDirectory, '../..');
    const ckeditorDirectory = path.join(webappDirectory, 'admin/skins/webjet8/ckeditor/dist');
    const imageBranch = editorSource.slice(editorSource.indexOf("if ( dialogName == 'image')"), editorSource.indexOf("else if ( dialogName == 'link')"));
    const pageErrors = [];
    page.on('pageerror', error => pageErrors.push(error.message));
    // Serve repository assets through intercepted requests; no running CMS or network is needed.
    await page.route('**/*', async route => {
        const pathname = new URL(route.request().url()).pathname;
        if (pathname === '/autotest-thumb') {
            return route.fulfill({contentType: 'text/html', body: '<textarea id="autotest-editor"><img id="autotest-image" src="/images/autotest.jpg" class="img-fluid fixedSize-99-99-5"></textarea>'});
        }
        if (pathname === '/admin/v9/files/wj_image/') {
            return route.fulfill({contentType: 'text/html', body: '<script>function refreshValuesFromCk() {}</script>'});
        }
        if (pathname.startsWith('/images/') || pathname.startsWith('/thumb/')) {
            return route.fulfill({contentType: 'image/png', body: Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl6LxkAAAAASUVORK5CYII=', 'base64')});
        }
        const filename = path.join(webappDirectory, pathname);
        if (fs.existsSync(filename) && fs.statSync(filename).isFile()) {
            const contentType = filename.endsWith('.js') ? 'text/javascript' : filename.endsWith('.css') ? 'text/css' : 'application/octet-stream';
            return route.fulfill({contentType, body: fs.readFileSync(filename)});
        }
        return route.fulfill({status: 404, body: ''});
    });
    await page.goto('http://iwcm.interway.sk/autotest-thumb');
    await page.evaluate(() => { window.CKEDITOR_BASEPATH = '/admin/skins/webjet8/ckeditor/dist/'; });
    await page.addScriptTag({path: path.join(adminDirectory, 'node_modules/jquery/dist/jquery.js')});
    await page.addScriptTag({path: path.join(ckeditorDirectory, 'ckeditor.js')});
    await page.addScriptTag({content: urlFunctions + '\nwindow.WJ = {urlAddParam, urlUpdateParam, urlRemoveParam, urlGetParam, translate: key => key};'});
    await page.evaluate(imageBranch => {
        CKEDITOR.on('dialogDefinition', function(ev) {
            var dialogName = ev.data.name;
            var dialogDefinition = ev.data.definition;
            var options = {constants: {}, lang: {}};
            var that = {ckEditorObject: CKEDITOR, ckEditorInstance: ev.editor, translate: key => ev.editor.lang.webjetadmin[key]};
            var imageAlignClasses = ['pull-left image-left', 'pull-right image-right'];
            eval(imageBranch);
        });
        window.autotestEditor = CKEDITOR.replace('autotest-editor', {
            customConfig: '',
            language: 'en',
            skin: 'moono',
            plugins: 'image,dialog,dialogui,toolbar,wysiwygarea,basicstyles,link',
            toolbar: [['Image']],
            allowedContent: true,
            thumbServletAllowedSizeMode: 'strict',
            thumbServletAllowedSizes: '730x401ip5ncff00ffq90\n413x275\n159x159ip1'
        });
    }, imageBranch);
    await page.waitForFunction(() => window.autotestEditor.status === 'ready');
    async function openImage() {
        await page.evaluate(() => {
            autotestEditor.getSelection().selectElement(autotestEditor.document.getById('autotest-image'));
            autotestEditor.openDialog('image');
        });
        await page.waitForFunction(() => CKEDITOR.dialog.getCurrent()?.getElement().isVisible());
        await page.locator('.cke_dialog_tab').filter({hasText: 'Thumbnail'}).click();
    }
    await openImage();
    const select = page.locator('.cke_dialog select:visible');
    await select.waitFor({state: 'visible'});
    assert.equal(await select.count(), 1);
    assert.equal(await page.locator('.cke_dialog input:visible').count(), 0);
    assert.deepEqual(await select.locator('option').evaluateAll(options => options.map(option => option.value)), ['', '159x159ip1', '413x275', '730x401ip5ncff00ffq90']);
    assert.equal(await select.inputValue(), '');
    const alertPromise = page.waitForEvent('dialog');
    const clickPromise = page.locator('.cke_dialog_ui_button_ok').click();
    const alert = await alertPromise;
    assert.equal(alert.message(), 'editor.image.allowedSizeRequired.js');
    await alert.accept();
    await clickPromise;
    assert.equal(await page.evaluate(() => CKEDITOR.dialog.getCurrent().getElement().isVisible()), true);

    await select.selectOption('730x401ip5ncff00ffq90');
    await page.locator('.cke_dialog_ui_button_ok').click();
    await page.waitForFunction(() => !CKEDITOR.dialog.getCurrent()?.getElement().isVisible());
    const result = await page.evaluate(() => {
        const image = autotestEditor.document.getById('autotest-image');
        return {className: image.getAttribute('class'), src: image.getAttribute('src')};
    });
    assert.equal(result.className, 'img-fluid fixedSize-730-401-5-ff00ff-true-q90');
    assert.equal(new URL(result.src, 'http://iwcm.interway.sk').searchParams.get('q'), '90');
    await openImage();
    assert.equal(await select.inputValue(), '730x401ip5ncff00ffq90');
    await select.selectOption('413x275');
    await page.locator('.cke_dialog_ui_button_ok').click();
    await page.waitForFunction(() => !CKEDITOR.dialog.getCurrent()?.getElement().isVisible());
    await openImage();
    assert.equal(await select.inputValue(), '413x275');
    assert.deepEqual(pageErrors, []);
});
