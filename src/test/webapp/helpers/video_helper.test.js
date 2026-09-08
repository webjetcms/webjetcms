const assert = require("node:assert/strict");
const test = require("node:test");
const { chromium } = require("playwright");

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
