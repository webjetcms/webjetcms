Feature('webpages.branch-p05');

if (process.env.WEBJET_P05_BROWSER === 'true') {
    const fs = require('fs');
    const crypto = require('crypto');
    const path = require('path');
    const read = file => Object.fromEntries(fs.readFileSync(file, 'utf8').trim().split('\n').filter(line => line && !line.startsWith('#')).map(line => { const i = line.indexOf('='); return [line.slice(0, i), line.slice(i + 1)]; }));
    const fixture = read('/tmp/webjet-p05-manual.properties');
    const credentials = read('/tmp/webjet-p05-login.properties');
    const evidence = path.resolve(process.env.WEBJET_P05_EVIDENCE || '../../../build/test-results/p05');
    const uploadSource = name => path.relative(process.cwd(), '/tmp/webjet-p05-ui/'+name);
    const file = name => `.elfinder-cwd-filename[title="${name}"]`;
    const hash = bytes => crypto.createHash('sha256').update(Buffer.from(bytes)).digest('hex');
    const mediaTable = 'datatableFieldDTE_Field_editorFields-media';
    const mediaModal = '#'+mediaTable+'_modal';

    Before(({ I }) => {
        I.switchTo();
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        I.wjSetDefaultWindowSize();
        I.fillField('username', credentials.login);
        I.fillField('password', secret(credentials.password));
        I.click('Prihlásiť sa');
        I.waitForEnabled('#branch-selection', 30);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?groupid=${fixture.groupId}`);
        I.waitForEnabled('#branch-selection', 20);
        I.selectOption('#branch-selection', fixture.branchA);
        I.waitForText('autotest P05 media/files A', 20, '#branch-active-label');
    });

    After(async ({ I }) => {
        fs.mkdirSync(evidence,{recursive:true});
        fs.writeFileSync(path.join(evidence,'browser-console.json'),JSON.stringify(await I.grabBrowserLogs(),null,2));
    });

    async function openFiles(I, DTE, requireProof = true) {
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${fixture.pageId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-perex-tab');
        I.click(locate('.DTE_Field_Name_perexImage').find('button.btn-outline-secondary').first());
        I.waitForElement('#modalIframeIframeElement', 15);
        I.switchTo('#modalIframeIframeElement');
        I.waitForVisible('#finder .elfinder-navbar', 30);
        I.retry({retries:30,minTimeout:100,maxTimeout:200}).seeTextEquals('Všetky súbory\n/\nimages\n/\nbranch-p05-autotest', '.elfinder-path');
        if (requireProof) I.waitForElement(file('proof.png'), 20);
    }

    async function assets(I) {
        return I.executeScript(async f => {
            const output = {};
            for (const [version,prefix] of [['A',`/admin/webpages/branch-preview/${f.branchA}`],['B',`/admin/webpages/branch-preview/${f.branchB}`],['LIVE','']]) {
                for (const [kind,url] of [['image',f.image],['thumb','/thumb'+f.image+'?w=16&h=16']]) {
                    const response = await fetch(prefix+url,{cache:'no-store'});
                    output[version+'-'+kind] = {status:response.status, cache:response.headers.get('cache-control'), bytes:Array.from(new Uint8Array(await response.arrayBuffer()))};
                }
            }
            return output;
        }, fixture);
    }

    Scenario('P05 modern upload replaces private bytes and keeps B and public thumbnails unchanged', async ({ I, DTE }) => {
        const before = await assets(I);
        for (const item of Object.values(before)) I.assertEqual(item.status, 200);
        await openFiles(I, DTE);
        I.waitForElement('#branch-upload-button:enabled', 20);
        // The file input is the existing Dropzone control, populated through the browser file chooser API.
        I.attachFile('input.dz-hidden-input', uploadSource('proof.png'));
        I.waitForVisible('.toast .btn-toast-overwrite', 20);
        I.clickCss('.toast .btn-toast-overwrite');
        I.waitForElement('.toast[data-upload-status="success"]', 30);
        I.switchTo();
        const after = await assets(I);
        I.assertEqual(hash(after['A-image'].bytes), hash(fs.readFileSync('/tmp/webjet-p05-ui/proof.png')));
        I.assertNotEqual(hash(before['A-thumb'].bytes), hash(after['A-thumb'].bytes));
        for (const name of ['B-image','B-thumb','LIVE-image','LIVE-thumb']) I.assertEqual(hash(before[name].bytes), hash(after[name].bytes));
        I.assertContain(after['A-image'].cache, 'no-store');
        fs.mkdirSync(evidence,{recursive:true});
        fs.writeFileSync(path.join(evidence,'ui-assets.json'),JSON.stringify({before,after},null,2));
        I.saveScreenshot('branch-p05-modern-overwrite.png');
    });

    Scenario('P05 elFinder retains an empty directory, uploads, renames, moves and deletes one file', async ({ I, DTE }) => {
        const folderName = 'autotest-empty-'+Date.now();
        await openFiles(I, DTE);
        I.clickCss('.elfinder-button-icon-mkdir');
        I.waitForElement('.elfinder-cwd-filename textarea, .elfinder-cwd-filename input[type=text]', 10);
        I.fillField('.elfinder-cwd-filename textarea, .elfinder-cwd-filename input[type=text]', folderName);
        I.pressKey('Enter');
        I.waitForElement(file(folderName), 20);
        I.switchTo();
        await openFiles(I, DTE);
        // Reopening the real file manager issues a new request and reloads the persisted marker.
        I.waitForElement(file(folderName), 20);
        I.doubleClick(file(folderName));
        I.waitForText(folderName, 20, '.elfinder-path');
        I.clickCss('.elfinder-button-icon-upload');
        I.waitForElement('.elfinder-dialog input[type=file]:not([directory])', 10);
        I.attachFile('.elfinder-dialog input[type=file]:not([directory])', uploadSource('ckeditor-autotest.png'));
        I.waitForElement(file('ckeditor-autotest.png'), 30);
        I.waitForInvisible('.elfinder-notify-upload', 30);
        I.click(file('ckeditor-autotest.png'));
        I.click('Nástroje', '.md-tabs');
        I.click(file('ckeditor-autotest.png'));
        I.pressKey('F2');
        I.waitForElement('.elfinder-cwd-filename textarea, .elfinder-cwd-filename input[type=text]', 10);
        I.fillField('.elfinder-cwd-filename textarea, .elfinder-cwd-filename input[type=text]', 'autotest-renamed.png');
        I.pressKey('Enter');
        I.waitForElement(file('autotest-renamed.png'), 20);
        I.click('Obrázky', '.md-tabs');
        I.click(file('autotest-renamed.png'));
        I.clickCss('.elfinder-button-icon-cut');
        I.clickCss('.elfinder-button-icon-up');
        I.waitForElement(file('proof.png'), 20);
        I.click('#finder .elfinder-cwd-wrapper', null, {position:{x:400,y:200}});
        I.clickCss('.elfinder-button-icon-paste');
        I.waitForVisible('.elfinder-confirm-accept', 10);
        I.clickCss('.elfinder-confirm-accept');
        I.waitForElement(file('autotest-renamed.png'), 20);
        I.click('Obrázky', '.md-tabs');
        I.click(file('autotest-renamed.png'));
        I.pressKey('Delete');
        I.waitForVisible('.elfinder-confirm-accept', 10);
        I.clickCss('.elfinder-confirm-accept');
        I.waitForInvisible(file('autotest-renamed.png'), 20);
        I.seeElement(file(folderName));
        I.saveScreenshot('branch-p05-elfinder-marker.png');
    });

    Scenario('P05 CKEditor real drop uploads WORK bytes and serializes final logical URLs', async ({ I, DTE }) => {
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${fixture.newPageId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-content-tab');
        DTE.waitForCkeditor();
        const bytes = Array.from(fs.readFileSync('/tmp/webjet-p05-ui/ckeditor-autotest.png'));
        I.executeScript(data => {
            const editor = Object.values(CKEDITOR.instances).find(instance => instance.status === 'ready' && instance.document);
            editor.focus();
            window.p05UploadSources = Array.from(editor.document.$.images).map(image => image.src);
            const transfer = new DataTransfer();
            transfer.items.add(new File([new Uint8Array(data)], 'ckeditor-autotest.png', {type:'image/png'}));
            editor.editable().$.dispatchEvent(new DragEvent('drop',{bubbles:true,cancelable:true,dataTransfer:transfer}));
        }, bytes);
        // CKEditor's upload widget finishes asynchronously inside its editable iframe.
        I.waitForFunction(() => Object.values(CKEDITOR.instances).some(editor => editor.document && Array.from(editor.document.$.images).some(image => image.src.includes('ckeditor-autotest') && !window.p05UploadSources.includes(image.src) && image.complete && image.naturalWidth>0)), 30);
        const uploaded = await I.executeScript(() => {
            const editor = Object.values(CKEDITOR.instances).find(instance => instance.document && Array.from(instance.document.$.images).some(image => image.src.includes('ckeditor-autotest')));
            return {html:editor.getData(),src:Array.from(editor.document.$.images).find(image => image.src.includes('ckeditor-autotest') && !window.p05UploadSources.includes(image.src)).getAttribute('src')};
        });
        I.assertContain(uploaded.src, `/admin/webpages/branch-preview/${fixture.branchA}/images/`);
        I.assertNotContain(uploaded.html, '/admin/webpages/branch-preview/');
        I.assertContain(uploaded.html, 'ckeditor-autotest.png');
        fs.writeFileSync(path.join(evidence,'ckeditor.json'),JSON.stringify(uploaded,null,2));
        I.saveScreenshot('branch-p05-ckeditor-upload.png');
        DTE.save();
        DTE.waitForModalClose();
    });

    Scenario('P05 old page media-only edit and WORK-only page media CRUD use final IDs', async ({ I, DT, DTE }) => {
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${fixture.pageId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-media-tab');
        I.waitForElement('#'+mediaTable+' tbody tr[id="'+fixture.mediaId+'"] a', 20);
        const currentTitle = await I.executeScript(id => datatableInnerTable_editorFields_media.rows().data().toArray().find(media => media.mediaId === Number(id)).mediaTitleSk, fixture.mediaId);
        I.click(currentTitle, '#'+mediaTable);
        DTE.waitForEditor(mediaTable);
        I.fillField('#DTE_Field_mediaTitleSk', 'autotest P05 WORK media-only');
        I.fillField('#DTE_Field_mediaSortOrder', '37');
        DTE.save(mediaTable);
        I.waitForText('autotest P05 WORK media-only', 20, '#'+mediaTable);
        DTE.cancel();
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchA}/p05-autotest/`);
        I.waitForText('autotest P05 WORK media-only', 20, '.p05-news');
        I.seeElement(`.p05-news-media[data-media="${fixture.mediaId}"]`);
        I.amOnPage('/p05-autotest/');
        I.waitForText('autotest P05 LIVE attachment', 20, '.p05-news');
        I.dontSee('autotest P05 WORK media-only');
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${fixture.newPageId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-media-tab');
        I.clickCss('#'+mediaTable+'_wrapper button.buttons-create');
        DTE.waitForEditor(mediaTable);
        I.fillField('#DTE_Field_mediaTitleSk', 'autotest P05 new attachment');
        const pdfName = 'autotest-new-'+Date.now()+'.pdf';
        fs.copyFileSync('/tmp/webjet-p05-ui/autotest-new.pdf', '/tmp/webjet-p05-ui/'+pdfName);
        I.fillField('#DTE_Field_mediaLink', '/files/branch-p05-autotest/'+pdfName);
        I.click(locate('.DTE_Field_Name_mediaLink').find('button.btn-outline-secondary').first());
        I.waitForElement('#modalIframeIframeElement', 15);
        I.switchTo('#modalIframeIframeElement');
        I.retry({retries:30,minTimeout:100,maxTimeout:200}).seeTextEquals('Všetky súbory\n/\nfiles\n/\nbranch-p05-autotest', '.elfinder-path');
        I.waitForElement('#branch-upload-button:enabled', 20);
        I.attachFile('input.dz-hidden-input', uploadSource(pdfName));
        I.waitForElement('.toast[data-upload-status="success"]', 30);
        I.waitForElement(file(pdfName), 20);
        I.click(file(pdfName));
        I.switchTo();
        I.click(locate('#modalIframe button.btn-primary').withText('Potvrdiť'));
        I.waitForInvisible('#modalIframe', 15);
        I.fillField('#DTE_Field_mediaSortOrder', '7');
        I.checkOption(`${mediaModal} input[type="checkbox"][value="${fixture.categoryId}"]`);
        DTE.save(mediaTable);
        I.waitForText('autotest P05 new attachment', 20, '#'+mediaTable);
        I.click('autotest P05 new attachment', '#'+mediaTable);
        DTE.waitForEditor(mediaTable);
        I.fillField('#DTE_Field_mediaTitleSk', 'autotest P05 new attachment edited');
        DTE.save(mediaTable);
        I.waitForText('autotest P05 new attachment edited', 20, '#'+mediaTable);
        const createdId = await I.executeScript(() => datatableInnerTable_editorFields_media.rows().data().toArray().find(media => media.mediaTitleSk === 'autotest P05 new attachment edited').mediaId);
        I.assertTrue(createdId > 0);
        fs.writeFileSync(path.join(evidence,'new-media.json'),JSON.stringify({id:createdId,owner:Number(fixture.newPageId)},null,2));
        I.saveScreenshot('branch-p05-media.png');
        DTE.cancel();
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchA}/p05-autotest/`);
        I.waitForText('autotest P05 new attachment edited', 20, `.p05-news li[data-id="${fixture.newPageId}"]`);
        I.seeElement(`.p05-news-media[data-media="${createdId}"]`);
        I.amOnPage(`/admin/v9/webpages/web-pages-list/?docid=${fixture.newPageId}`);
        DTE.waitForEditor();
        I.clickCss('#pills-dt-datatableInit-media-tab');
        I.waitForText('autotest P05 new attachment edited', 20, '#'+mediaTable);
        I.clickCss('#'+mediaTable+' tbody td.dt-select-td');
        I.clickCss('#'+mediaTable+'_wrapper button.buttons-remove');
        DTE.waitForEditor(mediaTable);
        DTE.save(mediaTable);
        I.waitForInvisible(locate('#'+mediaTable+' a').withText('autotest P05 new attachment edited'), 20);
        DTE.cancel();
    });

    Scenario('P05 working delete returns 404 while B and public originals stay unchanged', async ({ I, DTE }) => {
        await openFiles(I, DTE);
        I.click(file('proof.png'));
        I.pressKey('Delete');
        I.waitForVisible('.elfinder-confirm-accept', 10);
        I.clickCss('.elfinder-confirm-accept');
        I.waitForInvisible(file('proof.png'), 20);
        I.switchTo();
        const result = await assets(I);
        const previous = JSON.parse(fs.readFileSync(path.join(evidence,'ui-assets.json'),'utf8'));
        I.assertEqual(result['A-image'].status,404);
        I.assertEqual(result['A-thumb'].status,404);
        for (const name of ['B-image','B-thumb','LIVE-image','LIVE-thumb']) {
            I.assertEqual(result[name].status,200);
            I.assertEqual(hash(previous.before[name].bytes),hash(result[name].bytes));
        }
        fs.writeFileSync(path.join(evidence,'ui-delete.json'),JSON.stringify(result,null,2));
        I.saveScreenshot('branch-p05-working-delete.png');
    });
    Scenario('P05 server rejects gallery variants, unsupported commands and stale uploads', async ({ I }) => {
        const result = await I.executeScript(async ({f,bytes}) => {
            const call = async (path, body) => { const response=await fetch(path,{method:'POST',body}); const text=await response.text(); return {status:response.status,body:text.startsWith('{')?JSON.parse(text):text}; };
            const form = (folder,name) => { const data=new FormData();data.append('name',name);data.append('destinationFolder',folder);data.append('uploadType','image');data.append('writeDirectlyToDestination','true');data.append('dztotalfilesize',String(bytes.length));data.append('file',new File([new Uint8Array(bytes)],name,{type:'image/png'}));return data; };
            const output={};
            output.gallery=await call('/admin/upload/chunk',form('/images/gallery/autotest-p05/','autotest-denied.png'));
            output.variant=await call('/admin/upload/chunk',form('/images/gallery/autotest-p05/','s_autotest-denied.png'));
            output.planned=await call('/admin/upload/chunk',form('/files/protected/planned-changes/1/images/','autotest-denied.png'));
            const lookup = async url => (await call('/admin/elFinder/gethash.jsp',new URLSearchParams({url}))).body.hash;
            const root=await lookup('/images/branch-p05-autotest');
            const gallery=await lookup('/images/gallery');
            const legacy=new FormData();legacy.append('cmd','upload');legacy.append('target',gallery);legacy.append('volumes','images');legacy.append('upload[]',new File([new Uint8Array(bytes)],'o_autotest-denied.png',{type:'image/png'}));
            output.legacyGallery=await call('/admin/elfinder-connector/',legacy);
            for(const cmd of ['extract','archive','duplicate','put']) output[cmd]=await call('/admin/elfinder-connector/',new URLSearchParams({cmd,target:root,volumes:'images'}));
            output.directoryDelete=await call('/admin/elfinder-connector/',new URLSearchParams({cmd:'rm','targets[]':root,volumes:'images'}));
            output.directoryRename=await call('/admin/elfinder-connector/',new URLSearchParams({cmd:'rename',target:root,name:'autotest-denied',volumes:'images'}));
            output.copy=await call('/admin/elfinder-connector/',new URLSearchParams({cmd:'paste',cut:'0',dst:root,'targets[]':root,volumes:'images'}));
            const select = branchId => fetch('/admin/rest/branches/context',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({branchId:Number(branchId),expectedDomainName:f.domain||'iwcm.interway.sk'})});
            await select(f.branchB);
            output.stale=await call('/admin/upload/chunk',form('/images/branch-p05-autotest/','autotest-stale.png'));
            output.staleMedia=await call('/admin/rest/media/editor',new URLSearchParams({action:'remove'}));
            output.staleFinder=await call('/admin/elfinder-connector/',new URLSearchParams({cmd:'mkdir',target:root,name:'autotest-stale',volumes:'images'}));
            await select(f.branchA);
            return output;
        }, {f:fixture,bytes:Array.from(fs.readFileSync('/tmp/webjet-p05-ui/proof.png'))});
        I.assertFalse(result.gallery.body.success);I.assertFalse(result.variant.body.success);I.assertFalse(result.planned.body.success);
        for(const name of ['legacyGallery','extract','archive','duplicate','put','directoryDelete','directoryRename','copy']) I.assertTrue(Boolean(result[name].body.error),name);
        for(const name of ['stale','staleMedia','staleFinder']) I.assertEqual(result[name].status,409,name);
        fs.writeFileSync(path.join(evidence,'server-boundaries.json'),JSON.stringify(result,null,2));
        I.amOnPage('/logoff.do?forward=/admin/logon/');
        const anonymous=await I.executeScript(async id => (await fetch(`/admin/webpages/branch-preview/${id}/images/branch-p05-autotest/proof.png`)).status,fixture.branchA);
        I.assertEqual(anonymous,403);
    });

    Scenario('P05 restore the retained review image through the modern UI', async ({ I, DTE }) => {
        await openFiles(I, DTE, false);
        I.waitForElement('#branch-upload-button:enabled', 20);
        I.attachFile('input.dz-hidden-input', uploadSource('proof.png'));
        I.waitForElement('.toast[data-upload-status="success"]', 30);
        I.waitForElement(file('proof.png'), 20);
        I.switchTo();
        const result=await assets(I);
        I.assertEqual(result['A-image'].status,200);
        I.assertEqual(hash(result['A-image'].bytes),hash(fs.readFileSync('/tmp/webjet-p05-ui/proof.png')));
        I.assertEqual(result['B-image'].status,200);
        I.assertEqual(result['LIVE-image'].status,200);
        fs.writeFileSync(path.join(evidence,'review-assets.json'),JSON.stringify(result,null,2));
    });
    Scenario('P05 administrative preview prefix renders pages, links, images and private thumbnails', async ({ I }) => {
        const prefix = `/admin/webpages/branch-preview/${fixture.branchA}`;
        const link = await I.executeScript(async f => {
            const response = await fetch(`/admin/rest/branches/${f.branchA}/preview-link?docId=${f.pageId}`);
            if (!response.ok) throw new Error('Preview link: '+response.status);
            return response.json();
        }, fixture);
        I.assertContain(link.url, prefix+'/');
        I.amOnPage(prefix+'/p05-autotest/');
        I.waitForVisible('#branch-preview-status', 30);
        I.seeElement(`a[href^="${prefix}/"]`);
        const versions = await assets(I);
        for (const item of Object.values(versions)) I.assertEqual(item.status, 200);
        I.assertNotEqual(hash(versions['A-image'].bytes), hash(versions['LIVE-image'].bytes));
        I.assertEqual(hash(versions['B-image'].bytes), hash(versions['LIVE-image'].bytes));
        I.assertEqual(hash(versions['B-thumb'].bytes), hash(versions['LIVE-thumb'].bytes));
        I.assertContain(versions['A-image'].cache, 'no-store');
        const boundary = await I.executeScript(async f => {
            const base = `/admin/webpages/branch-preview/${f.branchA}`;
            const head = await fetch(base+f.image, {method:'HEAD',cache:'no-store'});
            const old = await fetch(`/__branch-preview/${f.branchA}`+f.image, {cache:'no-store'});
            return {head:head.status, old:old.status};
        }, fixture);
        I.assertEqual(boundary.head, 200);
        I.assertEqual(boundary.old, 404);
        fs.mkdirSync(evidence,{recursive:true});
        fs.writeFileSync(path.join(evidence,'admin-preview-urls.json'),JSON.stringify({link,versions,boundary},null,2));
        I.saveScreenshot('branch-p05-admin-preview.png');
        I.amOnPage(`/admin/webpages/branch-preview/${fixture.branchB}/p05-autotest/`);
        I.waitForVisible('#branch-preview-status', 30);
        I.seeInCurrentUrl(`/admin/webpages/branch-preview/${fixture.branchB}/`);
        I.logout();
        const guest = await I.executeScript(async f => {
            const response = await fetch(`/admin/webpages/branch-preview/${f.branchA}`+f.image);
            return {status:response.status, cache:response.headers.get('cache-control')};
        }, fixture);
        I.assertEqual(guest.status,403);
        I.assertContain(guest.cache,'no-store');
    });


}
