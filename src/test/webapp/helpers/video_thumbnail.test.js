const assert = require("node:assert/strict");
const test = require("node:test");
const fs = require("node:fs/promises");
const os = require("node:os");
const path = require("node:path");
const { chromium } = require("playwright");
const { renderVideoTitle, TITLE_STYLES } = require("./video_thumbnail.js");

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
    for (const state of rendered) assert.deepEqual(state, { text: headline, children: 0, fits: true, widthFits: true, font: true,
      logoSize: [300, 60], logoSource: `data:image/svg+xml;base64,${logo.toString("base64")}` });
    assert.ok(!outputs[0].equals(outputs[1]) && !outputs[0].equals(outputs[2]));
    await renderVideoTitle(renderingBrowser, screenshot, "A long headline with several words and Unicode characters Ž Š Č. ".repeat(2), "clean");
    assert.ok(rendered.at(-1).fits && rendered.at(-1).widthFits);
    await assert.rejects(renderVideoTitle(browser, screenshot, "x\n".repeat(60)), /does not fit/);
    assert.equal(browser.contexts().length, 1);
  } finally { await browser.close(); }
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
    const original = await fs.readFile(output);
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
    await assert.rejects(helper.videoTitle("Default"), /does not fit/);
    assert.deepEqual(await fs.readFile(output), original);
    assert.deepEqual((await fs.readdir(directory)).sort(), ["example-title-clean.jpg", "example-title-glow.jpg"]);
  } finally {
    await browser?.close();
    await fs.rm(directory, { recursive: true, force: true });
    if (previousCodeceptjs === undefined) delete global.codeceptjs; else global.codeceptjs = previousCodeceptjs;
    if (previousText === undefined) delete process.env.VIDEO_TITLE_TEXT; else process.env.VIDEO_TITLE_TEXT = previousText;
    if (previousStyle === undefined) delete process.env.VIDEO_TITLE_STYLE; else process.env.VIDEO_TITLE_STYLE = previousStyle;
  }
});
