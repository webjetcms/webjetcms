const assert = require("node:assert/strict");
const test = require("node:test");
const fs = require("node:fs/promises");
const os = require("node:os");
const path = require("node:path");
const { chromium } = require("playwright");
const { renderVideoTitle, validateVideoTitle, TITLE_STYLES } = require("./video_thumbnail.js");

test("rejects invalid explicit font sizes", () => {
  for (const fontSize of [0, -1, NaN, Infinity, "50", null]) {
    assert.throws(() => validateVideoTitle("Title", "glow", fontSize), /font size must be a positive finite number/);
  }
});

test("renders all styles as uploadable JPEGs with literal, fitted Unicode text and cleans up contexts", async () => {
  const browser = await chromium.launch({ headless: true });
  try {
    const source = await browser.newPage();
    await source.setContent('<body style="background:#0063fb"><h1>Editor screenshot</h1></body>');
    const screenshot = await source.screenshot();
    const headline = 'Page Builder\nŽltý kôň & <b>literal</b>';
    const rendered = [];
    const renderingBrowser = { newContext: async options => {
      const context = await browser.newContext(options);
      const close = context.close.bind(context);
      context.close = async () => {
        rendered.push(await context.pages()[0].evaluate(() => {
          const h = document.querySelector("h1");
          const logo = document.querySelector(".brand img");
          return { text: h.textContent, children: h.children.length, fits: h.offsetHeight <= h.parentElement.clientHeight,
            widthFits: h.scrollWidth <= h.clientWidth, font: document.fonts.check('700 100px Asap'),
            fontSize: parseFloat(getComputedStyle(h).fontSize),
            scene: document.querySelector(".scene").getBoundingClientRect().toJSON(),
            logoSize: [logo.naturalWidth, logo.naturalHeight], logoSource: logo.src };
        }));
        await close();
      };
      return context;
    } };
    const outputs = [];
    for (const style of TITLE_STYLES) {
      const jpeg = await renderVideoTitle(renderingBrowser, screenshot, headline, style);
      outputs.push(jpeg);
      assert.ok(jpeg.length < 2 * 1024 * 1024);
      const size = await source.evaluate(async base64 => {
        const image = new Image();
        image.src = `data:image/jpeg;base64,${base64}`;
        await image.decode();
        return [image.naturalWidth, image.naturalHeight];
      }, jpeg.toString("base64"));
      assert.deepEqual(size, [1920, 1080]);
      assert.equal(browser.contexts().length, 1);
    }
    const logo = await fs.readFile(path.resolve(__dirname, "../../../main/webapp/admin/v9/src/images/logo-cms.svg"));
    const originalScene = rendered[0].scene;
    for (const { fontSize, scene, ...state } of rendered) {
      assert.ok(fontSize >= 36 && fontSize <= 100);
      assert.deepEqual(state, { text: headline, children: 0, fits: true, widthFits: true, font: true,
        logoSize: [300, 60], logoSource: `data:image/svg+xml;base64,${logo.toString("base64")}` });
    }
    assert.ok(!outputs[0].equals(outputs[1]) && !outputs[0].equals(outputs[2]));
    await renderVideoTitle(renderingBrowser, screenshot, "A long headline with several words and Unicode characters Ž Š Č. ".repeat(2), "clean");
    assert.ok(rendered.at(-1).fits && rendered.at(-1).widthFits);
    assert.ok(rendered.at(-1).fontSize < 100);
    for (const fontSize of [32, 50, 120]) {
      await renderVideoTitle(renderingBrowser, screenshot, "Title", "glow", fontSize);
      assert.equal(rendered.at(-1).fontSize, fontSize, "Explicit sizes must be preserved, including outside the automatic range");
      assert.deepEqual(rendered.at(-1).scene, originalScene, "Changing the font size must preserve the screenshot position and dimensions");
    }
    await renderVideoTitle(renderingBrowser, screenshot, "Title\nTitle\nTitle\nTitle", "glow", 120);
    assert.equal(rendered.at(-1).fontSize, 120);
    assert.equal(rendered.at(-1).fits, false, "Explicit sizes may overflow the headline area");
    assert.deepEqual(rendered.at(-1).scene, originalScene);
    await assert.rejects(renderVideoTitle(browser, screenshot, "x\n".repeat(60)), /does not fit/);
    assert.equal(browser.contexts().length, 1);
  } finally { await browser.close(); }
});

test("keeps the thumbnail at its original scale under native Chromium recording zoom", async () => {
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "wj-title-zoom-"));
  let context;
  try {
    await fs.mkdir(path.join(directory, "Default"));
    await fs.writeFile(path.join(directory, "Default", "Preferences"), JSON.stringify({
      partition: { default_zoom_level: { x: Math.log(24 / 17) / Math.log(1.2) } }
    }));
    // The full Chromium binary honors profile zoom; the default headless shell does not.
    const launch = options => chromium.launchPersistentContext(directory, {
      headless: true, executablePath: chromium.executablePath(), ...options
    });
    context = await launch({ viewport: { width: 1280, height: 720 }, deviceScaleFactor: 3 });
    const source = context.pages()[0];
    await source.setContent("<body>Prepared scene</body>");
    assert.ok(await source.evaluate(() => innerWidth < 1280), "The source profile must reproduce native recording zoom");
    const screenshot = await source.screenshot();
    await context.close();
    let layout;
    await renderVideoTitle({ newContext: async options => {
      context = await launch(options);
      const close = context.close.bind(context);
      context.close = async () => {
        layout = await context.pages().at(-1).evaluate(() => {
          const scene = document.querySelector(".scene");
          const logo = document.querySelector(".brand img");
          return { width: innerWidth, height: innerHeight, pixelRatio: devicePixelRatio,
            sceneLeft: getComputedStyle(scene).left, sceneWidth: scene.offsetWidth,
            logoWidth: logo.getBoundingClientRect().width,
            fontSize: getComputedStyle(document.querySelector("h1")).fontSize };
        });
        await close();
      };
      return context;
    } }, screenshot, "Title", "glow", 30);
    context = undefined;
    assert.deepEqual(layout, { width: 1280, height: 720, pixelRatio: 3,
      sceneLeft: "608px", sceneWidth: 790, logoWidth: 260, fontSize: "30px" });
  } finally {
    await context?.close();
    await fs.rm(directory, { recursive: true, force: true });
  }
});

test("saves independent style artifacts, preserves the editor and retains the previous file on failure", async () => {
  const previousCodeceptjs = global.codeceptjs;
  const previousText = process.env.VIDEO_TITLE_TEXT;
  const previousStyle = process.env.VIDEO_TITLE_STYLE;
  delete process.env.VIDEO_TITLE_TEXT;
  delete process.env.VIDEO_TITLE_STYLE;
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), "wj-title-"));
  let browser;
  try {
    global.codeceptjs = require("codeceptjs");
    const VideoHelper = require("./video_helper.js");
    browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();
    await page.setContent('<iframe srcdoc="<input value=unchanged>"></iframe>');
    const frame = page.frameLocator("iframe");
    await frame.locator("input").focus();
    await frame.locator("input").evaluate(input => input.setSelectionRange(2, 5));
    const helper = new VideoHelper({ titleMode: true, featureVideoDirectory: directory });
    const playwright = { page, context: frame };
    Object.defineProperty(helper, "helpers", { value: { Playwright: playwright } });
    const scenario = { file: "/video/example.js" };
    helper._test(scenario);
    const output = await helper.videoTitle("Page Builder\nNew experience");
    assert.equal(path.basename(output), "example-title-glow.jpg");
    assert.equal(scenario.artifacts["title-glow"], output);
    const automatic = await fs.readFile(output);
    helper.config.titleMode = false;
    await helper.videoTitle("Page Builder\nNew experience", 50);
    const original = await fs.readFile(output);
    assert.notDeepEqual(original, automatic, "The numeric second argument must change the rendered thumbnail outside title mode");
    const bold = await helper.videoTitle("Page Builder\nNew experience", 50, "bold");
    assert.equal(path.basename(bold), "example-title-bold.jpg");
    assert.equal(browser.contexts().length, 1);
    assert.equal(playwright.context, frame);
    assert.deepEqual(await frame.locator("input").evaluate(input => ({ focus: document.activeElement === input,
      value: input.value, start: input.selectionStart, end: input.selectionEnd })),
    { focus: true, value: "unchanged", start: 2, end: 5 });
    process.env.VIDEO_TITLE_STYLE = "clean";
    process.env.VIDEO_TITLE_TEXT = "Override text";
    const clean = await helper.videoTitle("Default", "bold");
    assert.equal(path.basename(clean), "example-title-clean.jpg");
    assert.deepEqual(await fs.readFile(output), original);
    process.env.VIDEO_TITLE_STYLE = "glow";
    process.env.VIDEO_TITLE_TEXT = "x\n".repeat(60);
    await assert.rejects(helper.videoTitle("Default", "glow"), /does not fit/);
    await assert.rejects(helper.videoTitle("Default", 0), /font size must be a positive finite number/);
    assert.deepEqual(await fs.readFile(output), original);
    assert.deepEqual((await fs.readdir(directory)).sort(), ["example-title-bold.jpg", "example-title-clean.jpg", "example-title-glow.jpg"]);
  } finally {
    await browser?.close();
    await fs.rm(directory, { recursive: true, force: true });
    if (previousCodeceptjs === undefined) delete global.codeceptjs; else global.codeceptjs = previousCodeceptjs;
    if (previousText === undefined) delete process.env.VIDEO_TITLE_TEXT; else process.env.VIDEO_TITLE_TEXT = previousText;
    if (previousStyle === undefined) delete process.env.VIDEO_TITLE_STYLE; else process.env.VIDEO_TITLE_STYLE = previousStyle;
  }
});
