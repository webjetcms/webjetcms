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
          page,
          _locateElement: (locator) => page.locator(locator).first()
        }
      }
    });
    videoHelper.videoCursorPage = page;
    videoHelper.videoCursorPosition = { x: 100, y: 750 };
    videoHelper.videoCursorRandom = () => 0.1;

    await videoHelper._moveCursorNaturally("#target", 1);

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
