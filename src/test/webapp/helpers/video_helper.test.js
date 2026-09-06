const assert = require("node:assert/strict");
const test = require("node:test");
const { chromium } = require("playwright");

test("shows a two-second full-page slate and preserves editing focus inside an iframe", async () => {
  const previousCodeceptjs = global.codeceptjs;
  let browser;

  try {
    global.codeceptjs = require("codeceptjs");
    const VideoHelper = require("./video_helper.js");
    browser = await chromium.launch({ headless: true });
    const page = await browser.newPage({ viewport: { width: 1360, height: 765 } });
    await page.setContent('<iframe srcdoc="<input value=unchanged>"></iframe>');
    const frame = page.frameLocator("iframe");
    await frame.locator("input").focus();
    await frame.locator("input").evaluate(input => input.setSelectionRange(2, 5));

    const videoHelper = new VideoHelper({});
    Object.defineProperty(videoHelper, "helpers", {
      value: { Playwright: { page, context: frame } }
    });
    const title = "Shot 2: <em>literal text</em> & stable selection.";
    const startedAt = Date.now();
    const narration = "🎬".repeat(199) + "Z" + "THIS MUST BE TRUNCATED";
    const showing = videoHelper.videoTitle({ number: 2, title: title.replace("Shot 2: ", ""), narration });
    await Promise.all([
      showing,
      (async () => {
        const slate = page.locator("#wj-video-title-host div").first();
        await slate.waitFor({ state: "visible" });
        assert.equal(await slate.locator("div").first().textContent(), title);
        const excerpt = slate.locator("[data-video-narration]");
        assert.equal(await excerpt.textContent(), Array.from(narration).slice(0, 200).join(""));
        assert.ok(await excerpt.evaluate(element => parseFloat(getComputedStyle(element).fontSize) <
          parseFloat(getComputedStyle(element.previousElementSibling).fontSize)));
        assert.equal(await slate.locator("em").count(), 0);
        assert.deepEqual(await slate.boundingBox(), { x: 0, y: 0, width: 1360, height: 765 });
        assert.equal(await frame.locator("#wj-video-title-host").count(), 0);
      })()
    ]);
    assert.ok(Date.now() - startedAt >= 2000, "The slate must remain for the full presentation hold");
    assert.equal(await page.locator("#wj-video-title-host").count(), 0);
    assert.equal(videoHelper.helpers.Playwright.context, frame);
    assert.deepEqual(await frame.locator("input").evaluate(input => ({
      focused: document.activeElement === input,
      value: input.value,
      start: input.selectionStart,
      end: input.selectionEnd
    })), { focused: true, value: "unchanged", start: 2, end: 5 });
    await page.keyboard.type("NEXT");
    assert.equal(await frame.locator("input").inputValue(), "unNEXTnged");
  } finally {
    await browser?.close();
    if (previousCodeceptjs === undefined) {
      delete global.codeceptjs;
    } else {
      global.codeceptjs = previousCodeceptjs;
    }
  }
});

test("keeps synthetic cursor points inside the DOM viewport under browser zoom", async () => {
  const previousCodeceptjs = global.codeceptjs;
  let browser;

  try {
    global.codeceptjs = require("codeceptjs");
    const VideoHelper = require("./video_helper.js");

    browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({
      viewport: { width: 1920, height: 1080 }
    });
    const page = await context.newPage();
    const cdpSession = await context.newCDPSession(page);

    // Reproduce native zoom's smaller DOM coordinate space while preserving Playwright's viewport.
    await cdpSession.send("Emulation.setDeviceMetricsOverride", {
      width: 1360,
      height: 765,
      deviceScaleFactor: 1,
      mobile: false
    });
    await page.setContent(`
      <div id="target" style="position: fixed; right: 10px; bottom: 5px; width: 20px; height: 10px;"></div>
    `);

    const domViewport = await page.evaluate(() => ({
      width: window.innerWidth,
      height: window.innerHeight
    }));
    assert.deepEqual(page.viewportSize(), { width: 1920, height: 1080 });
    assert.deepEqual(domViewport, { width: 1360, height: 765 });

    const requestedMoves = [];
    const originalMouseMove = page.mouse.move.bind(page.mouse);
    page.mouse.move = async (x, y, options) => {
      requestedMoves.push({ x, y });
      return originalMouseMove(x, y, options);
    };

    const videoHelper = new VideoHelper({});
    Object.defineProperty(videoHelper, "helpers", {
      value: {
        Playwright: {
          page
        }
      }
    });
    videoHelper.videoCursorPage = page;
    videoHelper.videoCursorPosition = { x: 100, y: 750 };
    videoHelper.videoCursorRandom = () => 0.1;

    await videoHelper._moveCursorNaturally(page.locator("#target"), "#target", 1);

    assert.ok(requestedMoves.length > 1, "The cursor must request multiple movement points");
    const outsideMoves = requestedMoves.filter(({ x, y }) => {
      return x < 0 || x >= domViewport.width || y < 0 || y >= domViewport.height;
    });
    assert.deepEqual(
      outsideMoves,
      [],
      `Cursor points outside the DOM viewport: ${JSON.stringify(outsideMoves)}`
    );
  } finally {
    await browser?.close();
    if (previousCodeceptjs === undefined) {
      delete global.codeceptjs;
    } else {
      global.codeceptjs = previousCodeceptjs;
    }
  }
});

test("moves to and clicks the same visible target for a fuzzy locator", async () => {
  const previousCodeceptjs = global.codeceptjs;
  const previousCursorSetting = process.env.CODECEPT_VIDEO_CURSOR;
  const previousClickDelay = process.env.CODECEPT_VIDEO_CLICK_DELAY;
  let browser;

  try {
    process.env.CODECEPT_VIDEO_CURSOR = "true";
    process.env.CODECEPT_VIDEO_CLICK_DELAY = "0";
    global.codeceptjs = require("codeceptjs");
    const CodeceptPlaywright = require("codeceptjs/lib/helper/Playwright");
    const VideoHelper = require("./video_helper.js");

    browser = await chromium.launch({ headless: true });
    const page = await browser.newPage({ viewport: { width: 800, height: 600 } });
    await page.setContent(`
      <save id="css-decoy" style="position: fixed; left: 20px; top: 20px; width: 40px; height: 20px;">Save</save>
      <button id="hidden-save" style="display: none;">Save</button>
      <button id="visible-save" style="position: fixed; left: 600px; top: 400px; width: 100px; height: 40px;">Save</button>
      <script>
        window.clickedTargetId = null;
        document.addEventListener("click", (event) => {
          window.clickedTargetId = event.target.id;
        });
      </script>
    `);

    let locateClickableCalls = 0;
    let helperClickCalls = 0;
    const playwrightHelper = {
      page,
      options: { highlightElement: false },
      _getContext: async () => page,
      _locateElement: CodeceptPlaywright.prototype._locateElement,
      _locateClickable: async function(locator) {
        locateClickableCalls++;
        return CodeceptPlaywright.prototype._locateClickable.call(this, locator);
      },
      _waitForAction: async () => {},
      moveCursorTo: CodeceptPlaywright.prototype.moveCursorTo,
      click: async function(locator) {
        helperClickCalls++;
        return CodeceptPlaywright.prototype.click.call(this, locator);
      }
    };
    const videoHelper = new VideoHelper({});
    Object.defineProperty(videoHelper, "helpers", {
      value: { Playwright: playwrightHelper }
    });
    videoHelper.videoCursorPage = page;
    videoHelper.videoCursorPosition = { x: 400, y: 300 };
    videoHelper.videoCursorRandom = () => 0.1;

    const targetBox = await page.locator("#visible-save").boundingBox();
    await videoHelper.videoClick("Save", 0);

    assert.equal(await page.evaluate(() => window.clickedTargetId), "visible-save");
    assert.equal(locateClickableCalls, 1, "The fuzzy clickable target must be resolved exactly once");
    assert.equal(helperClickCalls, 0, "The original locator must not be resolved again for the click");
    assert.deepEqual(videoHelper.videoCursorPosition, {
      x: targetBox.x + targetBox.width / 2,
      y: targetBox.y + targetBox.height / 2
    });
  } finally {
    await browser?.close();
    if (previousCursorSetting === undefined) {
      delete process.env.CODECEPT_VIDEO_CURSOR;
    } else {
      process.env.CODECEPT_VIDEO_CURSOR = previousCursorSetting;
    }
    if (previousClickDelay === undefined) {
      delete process.env.CODECEPT_VIDEO_CLICK_DELAY;
    } else {
      process.env.CODECEPT_VIDEO_CLICK_DELAY = previousClickDelay;
    }
    if (previousCodeceptjs === undefined) {
      delete global.codeceptjs;
    } else {
      global.codeceptjs = previousCodeceptjs;
    }
  }
});

test("keeps one cursor when moving between the page and nested cross-origin scaled iframes", async () => {
  const previousCodeceptjs = global.codeceptjs;
  const previousCursorSetting = process.env.CODECEPT_VIDEO_CURSOR;
  let browser;
  try {
    process.env.CODECEPT_VIDEO_CURSOR = "true";
    global.codeceptjs = require("codeceptjs");
    const VideoHelper = require("./video_helper.js");
    browser = await chromium.launch({ headless: true });
    const context = await browser.newContext({ viewport: { width: 1000, height: 700 } });
    await context.route("**/*", route => {
      const host = new URL(route.request().url()).hostname;
      const html = host === "video.test"
        ? '<button style="position:fixed;left:20px;top:20px">Parent</button><iframe src="http://frame.test/" style="position:fixed;left:200px;top:150px;width:500px;height:350px;border:4px solid black;transform:scale(.8);transform-origin:0 0"></iframe>'
        : host === "frame.test"
          ? '<iframe src="http://nested.test/" style="position:fixed;left:80px;top:60px;width:250px;height:180px;border:6px solid black"></iframe>'
          : '<button style="position:fixed;left:20px;top:30px;width:100px;height:40px">Nested</button>';
      return route.fulfill({ contentType: "text/html", body: html });
    });
    const page = await context.newPage();
    const helper = new VideoHelper({});
    Object.defineProperty(helper, "helpers", { value: { Playwright: { page, browserContext: context } } });
    await helper._before({ title: "iframe cursor" });
    await page.goto("http://video.test/");
    await page.locator("button").click();
    const nestedButton = page.frameLocator("iframe").frameLocator("iframe").locator("button");
    await nestedButton.waitFor();
    const box = await nestedButton.boundingBox();
    await nestedButton.click();
    const expected = { x: box.x + box.width / 2, y: box.y + box.height / 2 };
    await page.waitForFunction(({ x, y }) => {
      const host = document.querySelector("#wj-video-cursor-host");
      return Math.abs(Number(host.dataset.cursorX) - x) < 1 &&
        Math.abs(Number(host.dataset.cursorY) - y) < 1 &&
        host.shadowRoot.firstElementChild.style.opacity === "1";
    }, expected);
    const hostCounts = await Promise.all(page.frames().map(frame => frame.locator("#wj-video-cursor-host").count()));
    assert.deepEqual(hostCounts, [1, 0, 0], "Only the top-level document may render a cursor");
    assert.ok(await page.locator("#wj-video-cursor-host span").evaluate(ring => ring.getAnimations().length > 0),
      "A click in a nested iframe must animate the single top-level cursor");
    await page.mouse.move(40, 40);
    await page.waitForFunction(() => document.querySelector("#wj-video-cursor-host").dataset.cursorX === "40");
    assert.equal(await page.locator("#wj-video-cursor-host").count(), 1);
  } finally {
    await browser?.close();
    if (previousCodeceptjs === undefined) delete global.codeceptjs;
    else global.codeceptjs = previousCodeceptjs;
    if (previousCursorSetting === undefined) delete process.env.CODECEPT_VIDEO_CURSOR;
    else process.env.CODECEPT_VIDEO_CURSOR = previousCursorSetting;
  }
});
