Feature('admin.welcome');

var random;

Before(({ I, login }) => {
    login('admin');
    I.amOnPage("/admin/v9/");
    I.waitForElement(".md-dashboard[data-loaded='true']", 20);
    random = I.getRandomText();
});

Scenario("Personal shortcuts replace the former bookmark section", ({ I }) => {
    I.seeElement('.md-dashboard__welcome .md-dashboard__shortcuts[aria-label="Vaše skratky"]');
    I.see("Upraviť skratky", ".md-dashboard__shortcut-actions");
    I.dontSeeElementInDOM(".md-dashboard__legacy");
    I.dontSeeElementInDOM("#webjet-overview-dashboard .bookmark");
    I.dontSeeElementInDOM("#bookmark_modal");
});

/** Supplies a real video stream while avoiding the browser's native sharing picker in tests. */
async function mockFeedbackCapture(I) {
    await I.executeScript(() => {
        window.feedbackCaptureState = { calls: [], tracks: [], overlaysHidden: [], error: null };
        Object.defineProperty(navigator, 'mediaDevices', { configurable: true, value: {
            getDisplayMedia: async options => {
                const state = window.feedbackCaptureState;
                state.calls.push(options);
                state.overlaysHidden.push(getComputedStyle(document.querySelector('#feedback_modal')).visibility === 'hidden'
                    && getComputedStyle(document.querySelector('.modal-backdrop')).visibility === 'hidden');
                if (state.hold) await new Promise(resolve => { state.resume = resolve; });
                if (state.error) throw new DOMException('Capture autotest', state.error);
                const canvas = document.createElement('canvas');
                canvas.width = 320;
                canvas.height = 180;
                const context = canvas.getContext('2d');
                context.fillStyle = '#ff0000';
                context.fillRect(0, 0, canvas.width, canvas.height);
                const stream = canvas.captureStream();
                state.tracks.push(...stream.getTracks());
                return stream;
            }
        } });
    });
}

Scenario("feedback draft and type selection", async ({ I }) => {
    I.clickCss(".md-dashboard__feedback");
    I.waitForVisible("#feedback_modal.show", 10);
    I.dontSeeElement("#feedback_modal .md-feedback__screenshot-row");
    I.waitForFunction(() => getComputedStyle(document.querySelector("#feedback_modal .modal-dialog")).transform === "none");
    I.seeElementInDOM("#feedback-screenshot[type=checkbox]");
    I.seeElement("#feedback_modal .md-feedback__send:disabled");
    I.seeElementInDOM("#feedback_modal #feedback-upload.dz-clickable");
    I.seeElementInDOM("#feedback_modal #upload-toastr-template .toast-error-message");
    I.saveScreenshot("feedback-empty.png");
    const draft = "Feedback draft autotest " + random;
    I.fillField("#feedback-group-text .ql-editor", draft);
    I.clickCss("#feedback_modal label[for='feedback-type-problem']");
    I.see(draft, "#feedback-group-text .ql-editor");
    I.seeAttributesOnElements("#feedback-group-text .ql-editor", { "data-placeholder": "Čo nefunguje? Popíšte, čo sa stalo a čo ste očakávali." });
    I.saveScreenshot("feedback-filled.png");
    I.clickCss("#feedback_modal .md-feedback__close");
    I.waitForInvisible("#feedback_modal", 10);
    I.clickCss(".md-dashboard__feedback");
    I.waitForVisible("#feedback_modal.show", 10);
    I.see(draft, "#feedback-group-text .ql-editor");
    I.seeCheckboxIsChecked("#feedback-type-problem");
    I.clickCss("#feedback_modal .md-feedback__cancel");
    I.waitForInvisible("#feedback_modal", 10);
    I.clickCss(".md-dashboard__feedback");
    I.waitForVisible("#feedback_modal.show", 10);
    I.dontSee(draft, "#feedback-group-text .ql-editor");
    I.seeCheckboxIsChecked("#feedback-type-idea");
    I.dontSeeElement("#feedback_modal .md-feedback__screenshot-row");
    I.clickCss("#feedback_modal .md-feedback__cancel");
});

Scenario("feedback sending, error, retry and success", async ({ I }) => {
    await mockFeedbackCapture(I);
    const requests = [];
    const uploads = [];
    let pending;
    await I.mockRoute("**/admin/upload/chunk", route => {
        uploads.push(route.request().postDataBuffer());
        return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ key: "feedback-upload-autotest-" + uploads.length, exists: false }) });
    });
    await I.mockRoute("**/admin/upload/skipkey", route => route.fulfill({ status: 200, contentType: "application/json", body: '{"success":true}' }));
    await I.mockRoute("**/admin/rest/feedback", route => {
        requests.push(new URLSearchParams(route.request().postData()));
        pending = route;
    });
    I.clickCss(".md-dashboard__feedback");
    I.waitForVisible("#feedback_modal.show", 10);
    I.clickCss("#feedback_modal label[for=feedback-screenshot]");
    I.waitForVisible("#feedback_modal .md-feedback__preview", 20);
    I.fillField("#feedback-group-text .ql-editor", "Formatted feedback autotest " + random);
    I.pressKey(["Command", "a"]);
    I.clickCss("#feedback-toolbar .ql-bold");
    I.attachFile("input.dz-hidden-input-feedback-upload", "tests/apps/gallery/gallery.png");
    I.waitForElement("#feedback_modal .md-feedback__send:not(:disabled)", 10);
    I.seeElement("#feedback-upload");
    I.clickCss("#feedback_modal .md-feedback__remove-file");
    I.waitForInvisible("#feedback_modal .md-feedback__remove-file", 10);
    I.attachFile("input.dz-hidden-input-feedback-upload", "tests/apps/gallery/gallery.png");
    I.waitForElement("#feedback_modal .md-feedback__send:not(:disabled)", 10);
    I.clickCss("#feedback_modal .md-feedback__send");
    I.waitForText("Odosiela sa…", 10, "#feedback_modal");
    I.seeElement("#feedback_modal fieldset:disabled");
    I.seeElement("#feedback_modal .md-feedback__close:disabled");
    // Held requests let the test inspect the pending UI without sending email.
    await I.usePlaywrightTo("reject the held feedback request", async () => {
        await pending.fulfill({ status: 500, body: "Sending failed" });
    });
    I.waitForText("Text aj prílohy zostali zachované", 10, "#feedback_modal");
    const errorVisible = await I.executeScript(() => {
        const error = document.querySelector("#feedback_modal .md-feedback__error").getBoundingClientRect();
        const body = document.querySelector("#feedback_modal .modal-body").getBoundingClientRect();
        return error.top >= body.top && error.bottom <= body.bottom;
    });
    I.assertTrue(errorVisible, "The sending error must be visible inside the scrollable body.");
    I.saveScreenshot("feedback-error.png");
    I.see("Formatted feedback autotest " + random, "#feedback-group-text .ql-editor");
    I.clickCss("#feedback_modal .md-feedback__send");
    I.waitForText("Odosiela sa…", 10, "#feedback_modal");
    await I.usePlaywrightTo("accept the held feedback request", async () => {
        await pending.fulfill({ status: 200, contentType: "text/plain", body: "OK" });
    });
    I.waitForText("Ďakujeme za spätnú väzbu", 10, "#feedback_modal");
    I.saveScreenshot("feedback-success.png");
    I.assertEqual(requests.length, 2, "Retry must send exactly one additional request.");
    I.assertEqual(requests[0].toString(), requests[1].toString(), "Retry must preserve text and attachment keys.");
    I.assertEqual(requests[0].getAll("data[fileKeys][]").length, 2, "Manual upload and screenshot must both be attached.");
    I.assertContain(requests[0].get("data[pageUrl]"), "/admin/v9/");
    I.assertContain(requests[0].get("data[text]"), "<strong>");
    I.assertEqual(requests[0].get("data[isHtml]"), "true");
    I.assertEqual(uploads.length, 3, "Retry must reuse the uploaded screenshot.");
    I.assertTrue(uploads.some(body => body.includes(Buffer.from("feedback-screenshot.png"))));
    I.clickCss("#feedback_modal .md-feedback__another");
    I.waitForVisible("#feedback-group-text .ql-editor", 10);
    I.dontSee("Formatted feedback autotest", "#feedback-group-text .ql-editor");
    I.seeElement("#feedback_modal .md-feedback__send:disabled");
    I.clickCss("#feedback_modal .md-feedback__cancel");
    await I.stopMockingRoute("**/admin/rest/feedback");
    await I.stopMockingRoute("**/admin/upload/chunk");
    await I.stopMockingRoute("**/admin/upload/skipkey");
});

Scenario("feedback responsive layout and shared Help entry point", async ({ I }) => {
    I.amOnPage("/admin/v9/search/");
    I.executeScript(() => WJ.showFeedbackDialog({ onBack: () => { document.body.dataset.feedbackBack = "true"; } }));
    I.waitForVisible("#feedback_modal.show", 10);
    I.fillField("#feedback-group-text .ql-editor", "Shared feedback autotest");
    I.clickCss("#feedback_modal .md-feedback__back");
    I.waitForInvisible("#feedback_modal", 10);
    I.seeAttributesOnElements("body", { "data-feedback-back": "true" });
    I.executeScript(() => WJ.showFeedbackDialog());
    I.waitForVisible("#feedback_modal.show", 10);
    I.see("Shared feedback autotest", "#feedback-group-text .ql-editor");
    for (const width of [1100, 760, 390]) {
        I.resizeWindow(width, 844);
        const fits = await I.executeScript(() => {
            const modal = document.querySelector("#feedback_modal .modal-dialog").getBoundingClientRect();
            return modal.left >= 0 && modal.right <= window.innerWidth && document.querySelector("#feedback_modal").scrollWidth <= window.innerWidth;
        });
        I.assertTrue(fits, "Feedback must fit the viewport at width " + width);
    }
    I.saveScreenshot("feedback-mobile.png");
    I.clickCss("#feedback_modal .md-feedback__cancel");
    I.resizeWindow(1280, 760);
});

Scenario("feedback native capture cancellation, failure and stream cleanup", async ({ I }) => {
    await mockFeedbackCapture(I);
    I.clickCss(".md-dashboard__feedback");
    I.waitForVisible("#feedback_modal.show", 10);
    I.see("Priložiť snímku tejto stránky", "#feedback_modal label[for=feedback-screenshot]");
    I.dontSeeCheckboxIsChecked("#feedback-screenshot");
    I.dontSeeElement("#feedback_modal .md-feedback__preview");
    I.fillField("#feedback-group-text .ql-editor", "Native capture autotest " + random);
    I.executeScript(() => { window.feedbackCaptureState.error = 'NotAllowedError'; });
    I.clickCss("#feedback_modal label[for=feedback-screenshot]");
    I.waitForVisible("#feedback-screenshot", 10);
    I.dontSeeCheckboxIsChecked("#feedback-screenshot");
    I.dontSeeElement("#feedback_modal .md-feedback__capture-error");
    I.seeElement("#feedback_modal .md-feedback__send:not(:disabled)");
    I.executeScript(() => { window.feedbackCaptureState.error = 'NotReadableError'; });
    I.clickCss("#feedback_modal label[for=feedback-screenshot]");
    I.waitForVisible("#feedback_modal .md-feedback__capture-error", 10);
    I.seeElement("#feedback_modal .md-feedback__send:not(:disabled)");
    I.executeScript(() => { window.feedbackCaptureState.error = null; });
    I.clickCss("#feedback_modal label[for=feedback-screenshot]");
    I.waitForVisible("#feedback_modal .md-feedback__preview", 10);
    const capture = await I.executeScript(() => {
        const state = window.feedbackCaptureState;
        const preview = document.querySelector('#feedback_modal .md-feedback__preview');
        const canvas = document.createElement('canvas');
        canvas.width = preview.naturalWidth;
        canvas.height = preview.naturalHeight;
        const context = canvas.getContext('2d');
        context.drawImage(preview, 0, 0);
        return { calls: state.calls, stopped: state.tracks.every(track => track.readyState === 'ended'),
            hidden: state.overlaysHidden.every(Boolean), width: canvas.width, height: canvas.height,
            pixel: Array.from(context.getImageData(10, 10, 1, 1).data), src: preview.src };
    });
    I.assertEqual(capture.calls.length, 3);
    I.assertFalse(capture.calls[2].audio, "Screenshot capture must not request audio.");
    I.assertEqual(capture.calls[2].video.displaySurface, 'browser');
    I.assertEqual(capture.calls[2].systemAudio, 'exclude');
    I.assertEqual(capture.calls[2].windowAudio, 'exclude');
    I.assertEqual(capture.calls[2].surfaceSwitching, 'exclude');
    I.assertTrue(capture.stopped, "Sharing tracks must stop after the frame has been captured.");
    I.assertTrue(capture.hidden, "Feedback and its backdrop must be hidden during capture.");
    I.assertEqual(capture.width, 320);
    I.assertEqual(capture.height, 180);
    // Video color conversion can round individual channels by a couple of levels.
    I.assertTrue(capture.pixel[0] >= 253 && capture.pixel[1] <= 2 && capture.pixel[2] <= 2 && capture.pixel[3] === 255,
        "The preview must contain pixels from the video stream.");
    I.seeCheckboxIsChecked("#feedback-screenshot");
    I.clickCss("#feedback_modal .md-feedback__close");
    I.waitForInvisible("#feedback_modal", 10);
    I.clickCss(".md-dashboard__feedback");
    I.waitForVisible("#feedback_modal.show", 10);
    I.seeCheckboxIsChecked("#feedback-screenshot");
    const preserved = await I.executeScript(() => document.querySelector('#feedback_modal .md-feedback__preview').src);
    I.assertEqual(preserved, capture.src, "Closing and reopening must preserve the attached screenshot.");
    const callsAfterReopen = await I.executeScript(() => window.feedbackCaptureState.calls.length);
    I.assertEqual(callsAfterReopen, 3, "Reopening must not trigger another sharing picker.");
    I.see("Native capture autotest " + random, "#feedback-group-text .ql-editor");
    I.waitForFunction(() => getComputedStyle(document.querySelector("#feedback_modal .modal-dialog")).transform === "none");
    I.saveScreenshot("feedback-native-capture.png");
    I.uncheckOption("#feedback-screenshot");
    I.dontSeeElement("#feedback_modal .md-feedback__preview");
    I.seeElement("#feedback_modal .md-feedback__send:not(:disabled)");
    I.clickCss("#feedback_modal .md-feedback__cancel");
});

for (const captureOutcome of ["cancel", "success"]) {
    Scenario("feedback preserves unfinished uploads during screenshot " + captureOutcome, async ({ I }) => {
        await mockFeedbackCapture(I);
        let releaseUploads;
        const uploadsReady = new Promise(resolve => { releaseUploads = resolve; });
        let uploadCount = 0;
        let submitted;
        await I.mockRoute("**/admin/upload/chunk", async route => {
            const key = "feedback-pending-autotest-" + ++uploadCount;
            await uploadsReady;
            return route.fulfill({ status: 200, contentType: "application/json", body: JSON.stringify({ key, exists: false }) });
        });
        await I.mockRoute("**/admin/upload/skipkey", route => route.fulfill({ status: 200, contentType: "application/json", body: '{"success":true}' }));
        await I.mockRoute("**/admin/rest/feedback", route => {
            submitted = new URLSearchParams(route.request().postData());
            return route.fulfill({ status: 200, contentType: "text/plain", body: "OK" });
        });
        I.clickCss(".md-dashboard__feedback");
        I.waitForVisible("#feedback_modal.show", 10);
        I.fillField("#feedback-group-text .ql-editor", "Pending attachments autotest " + random);
        I.executeScript(() => {
            const files = new DataTransfer();
            files.items.add(new File(['Active upload autotest'], 'feedback-active-autotest.txt', { type: 'text/plain' }));
            files.items.add(new File(['Queued upload autotest'], 'feedback-queued-autotest.txt', { type: 'text/plain' }));
            const input = document.querySelector('input.dz-hidden-input-feedback-upload');
            input.files = files.files;
            input.dispatchEvent(new Event('change', { bubbles: true }));
        });
        I.waitForFunction(() => {
            const files = document.querySelector('#feedback-upload').dropzone.files;
            return files.length === 2 && files[0].status === 'uploading' && files[1].status === 'queued';
        }, 10);
        I.executeScript(outcome => {
            window.feedbackCaptureState.hold = true;
            window.feedbackCaptureState.error = outcome === 'cancel' ? 'NotAllowedError' : null;
        }, captureOutcome);
        I.clickCss("#feedback_modal label[for=feedback-screenshot]");
        I.waitForFunction(() => typeof window.feedbackCaptureState.resume === 'function', 10);
        I.seeElementInDOM("#feedback_modal form[aria-busy=true]");
        I.seeElementInDOM("#feedback_modal fieldset:disabled");
        I.dontSeeElementInDOM("#feedback-upload.dz-clickable");
        const duringCapture = await I.executeScript(() => document.querySelector('#feedback-upload').dropzone.files.map(file => file.status));
        I.assertDeepEqual(duringCapture, ['uploading', 'queued'], "Capture must preserve both active and queued uploads.");
        I.executeScript(() => window.feedbackCaptureState.resume());
        I.waitForVisible("#feedback_modal fieldset:not(:disabled)", 10);
        if (captureOutcome === 'success') I.waitForVisible("#feedback_modal .md-feedback__preview", 10);
        else {
            I.dontSeeCheckboxIsChecked("#feedback-screenshot");
            I.dontSeeElement("#feedback_modal .md-feedback__capture-error");
        }
        I.seeElement("#feedback-upload.dz-clickable");
        I.seeElement("#feedback_modal .md-feedback__send:disabled");
        const afterCapture = await I.executeScript(() => document.querySelector('#feedback-upload').dropzone.files.map(file => file.status));
        await I.assertDeepEqual(afterCapture, ['uploading', 'queued'], "Finishing capture must preserve unfinished attachments.");
        releaseUploads();
        I.waitForFunction(() => document.querySelector('#feedback-upload').dropzone.files.every(file => file.status === 'success'), 10);
        I.waitForElement("#feedback_modal .md-feedback__send:not(:disabled)", 10);
        I.clickCss("#feedback_modal .md-feedback__send");
        await I.waitForText("Ďakujeme za spätnú väzbu", 10, "#feedback_modal");
        const expectedKeys = ['feedback-pending-autotest-1', 'feedback-pending-autotest-2'];
        if (captureOutcome === 'success') expectedKeys.push('feedback-pending-autotest-3');
        I.assertDeepEqual(submitted.getAll('data[fileKeys][]'), expectedKeys, "Sending must include both attachments and any captured screenshot.");
        I.clickCss("#feedback_modal .md-feedback__done");
        I.waitForInvisible("#feedback_modal", 10);
        await I.stopMockingRoute("**/admin/rest/feedback");
        await I.stopMockingRoute("**/admin/upload/chunk");
        await I.stopMockingRoute("**/admin/upload/skipkey");
    });
}

for (const discardAction of ["cancel", "remove", "replace"]) {
    Scenario("feedback screenshot uses temporary upload and is discarded on " + discardAction, async ({ I }) => {
        await mockFeedbackCapture(I);
        let uploaded;
        let discarded;
        await I.mockRoute("**/admin/upload/chunk", async route => {
            const response = await route.fetch();
            uploaded = await response.json();
            await route.fulfill({ response });
        });
        await I.mockRoute("**/admin/upload/skipkey", route => { discarded = route; });
        await I.mockRoute("**/admin/rest/feedback", route => route.fulfill({ status: 500, body: "Sending failed" }));
        I.clickCss(".md-dashboard__feedback");
        I.waitForVisible("#feedback_modal.show", 10);
        I.clickCss("#feedback_modal label[for=feedback-screenshot]");
        I.waitForVisible("#feedback_modal .md-feedback__preview", 20);
        I.fillField("#feedback-group-text .ql-editor", "Screenshot upload autotest " + random);
        I.clickCss("#feedback_modal .md-feedback__send");
        I.waitForText("Text aj prílohy zostali zachované", 10, "#feedback_modal");
        if (discardAction === "cancel") {
            I.clickCss("#feedback_modal .md-feedback__cancel");
            I.waitForInvisible("#feedback_modal", 10);
        } else if (discardAction === "remove") {
            I.uncheckOption("#feedback-screenshot");
            I.dontSeeElement("#feedback_modal .md-feedback__preview");
        } else {
            I.uncheckOption("#feedback-screenshot");
            I.clickCss("#feedback_modal label[for=feedback-screenshot]");
            I.waitForVisible("#feedback_modal .md-feedback__preview", 10);
            I.seeCheckboxIsChecked("#feedback-screenshot");
        }
        // Release the held request after checking the discard action's UI.
        const deleted = await I.usePlaywrightTo("discard the real temporary screenshot", async () => {
            const response = await discarded.fetch();
            const result = await response.json();
            await discarded.fulfill({ response });
            return result.success;
        });
        I.assertTrue(Boolean(uploaded.key), "Screenshot upload must return a temporary file key.");
        I.assertEqual(uploaded.name, "feedback-screenshot.png");
        I.assertTrue(deleted, "Discarding must delete the temporary screenshot.");
        if (discardAction !== "cancel") I.clickCss("#feedback_modal .md-feedback__cancel");
        await I.stopMockingRoute("**/admin/rest/feedback");
        await I.stopMockingRoute("**/admin/upload/chunk");
        await I.stopMockingRoute("**/admin/upload/skipkey");
    });
}

Scenario("update-2023-18.jsp", ({ I }) => {
    //verify JSP is able to compile without errors
    I.amOnPage("/admin/update/update-2023-18.jsp");
    I.waitForText("Upraviť kód v JSP súboroch pre aktuálny WebJET", 10);
});
