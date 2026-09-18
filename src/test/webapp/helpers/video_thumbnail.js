const fs = require("node:fs/promises");
const path = require("node:path");

const TITLE_STYLES = ["glow", "clean", "bold"];
const FONT_PATH = path.resolve(__dirname, "../../../main/webapp/admin/v9/src/fonts/asap/bold/asap-v15-latin-ext_latin-700.woff2");
const LOGO_PATH = path.resolve(__dirname, "../../../main/webapp/admin/v9/src/images/logo-cms.svg");

/** Validates thumbnail input before opening the renderer or replacing an artifact. */
function validateVideoTitle(text, style = "glow") {
  if (typeof text !== "string" || !text.trim()) throw new Error("Video title must be a non-empty string.");
  if (Array.from(text).length > 160) throw new Error("Video title must contain at most 160 characters; use a short headline.");
  if (!TITLE_STYLES.includes(style)) throw new Error(`Unknown video title style: ${style}. Choose ${TITLE_STYLES.join(", ")}.`);
}

/**
 * Renders a 1920 x 1080 JPEG from a scene screenshot in an isolated browser context.
 * Supersampling smooths rotated screenshot edges before the final JPEG encoding.
 * Text is inserted literally, including explicit line breaks; the source page is never restyled.
 * @param {import('playwright').Browser} browser Browser used only to create a temporary render context
 * @param {Buffer} screenshot PNG screenshot of the prepared scene
 * @param {string} text Short headline
 * @param {string} [style] One of glow, clean or bold
 * @returns {Promise<Buffer>} JPEG smaller than 2 MB
 */
async function renderVideoTitle(browser, screenshot, text, style = "glow") {
  validateVideoTitle(text, style);
  const [font, logo] = await Promise.all([fs.readFile(FONT_PATH), fs.readFile(LOGO_PATH)]);
  const context = await browser.newContext({ viewport: { width: 1280, height: 720 }, deviceScaleFactor: 3 });
  try {
    const page = await context.newPage();
    await page.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>
      @font-face { font-family: Asap; src: url(data:font/woff2;base64,${font.toString("base64")}); font-weight: 700; }
      * { box-sizing: border-box; }
      body { margin: 0; width: 1280px; height: 720px; overflow: hidden; font-family: Asap, sans-serif; color: white; background: #080d21; }
      main { position: relative; width: 100%; height: 100%; overflow: hidden; }
      .glow { background: radial-gradient(ellipse at 65% 75%, #0063fb66, transparent 60%), radial-gradient(ellipse at 95% 10%, #8a3eff66, transparent 55%); }
      .brand { position: absolute; top: 46px; left: 58px; }
      .brand img { display: block; width: 260px; height: 52px; }
      .headline { position: absolute; top: 160px; left: 58px; width: 505px; height: 455px; display: flex; align-items: center; z-index: 2; }
      h1 { margin: 0; width: 100%; font-size: 100px; line-height: 1.04; font-weight: 700; letter-spacing: -3px; white-space: pre-line; overflow-wrap: anywhere; }
      .glow h1 { text-shadow: 0 0 6px #ffffff40, 0 0 28px #0063fb99, 0 0 65px #8a3eff88, 0 4px 3px #080d21; }
      .scene { position: absolute; left: 608px; top: 172px; width: 790px; height: 464px; border: 2px solid #97baff88; border-radius: 16px; overflow: hidden; transform: rotate(-4deg); background: #f3f3f6; box-shadow: 0 35px 70px #0008, 0 0 60px #0063fb44; }
      .scene img { width: 100%; height: 100%; object-fit: cover; object-position: left top; display: block; }
      .accent { position: absolute; left: 60px; bottom: 54px; width: 92px; height: 7px; border-radius: 6px; background: #0063fb; }
      .clean { background: #f3f3f6; color: #13151b; }
      .clean .brand { background: #13151b; padding: 12px 16px; margin: -12px -16px; border-radius: 10px; }
      .clean h1 { color: #0054d5; }
      .clean .scene { transform: none; border-color: #dddfe6; box-shadow: 0 26px 60px #13151b25; }
      .bold { background: #0063fb; }
      .bold h1 { text-transform: uppercase; text-shadow: 5px 6px 0 #13151b; }
      .bold .accent { background: #fabd00; }
      .bold .scene { border: 5px solid #fff; box-shadow: 12px 14px 0 #13151b; transform: rotate(-6deg); }
    </style></head><body><main class="${style}">
      <div class="brand"><img alt="WebJET CMS" src="data:image/svg+xml;base64,${logo.toString("base64")}"></div><div class="headline"><h1></h1></div>
      <div class="scene"><img alt="Video scene"></div><div class="accent"></div>
    </main></body></html>`);
    await page.locator("h1").evaluate((heading, value) => { heading.textContent = value; }, text.trim());
    await page.locator(".scene img").evaluate(async (img, source) => {
      img.src = source;
      await img.decode();
    }, `data:image/png;base64,${screenshot.toString("base64")}`);
    await page.evaluate(async () => {
      await document.fonts.ready;
      await document.querySelector(".brand img").decode();
      const heading = document.querySelector("h1");
      let size = 100;
      while ((heading.offsetHeight > heading.parentElement.clientHeight || heading.scrollWidth > heading.clientWidth) && size > 36) {
        heading.style.fontSize = `${--size}px`;
      }
      if (heading.offsetHeight > heading.parentElement.clientHeight || heading.scrollWidth > heading.clientWidth) {
        throw new Error("Video title does not fit. Shorten the headline or remove line breaks.");
      }
    });
    // Render at twice the output resolution, then filter down once without intermediate JPEG loss.
    const png = await page.screenshot({ type: "png", animations: "disabled" });
    const encoded = await page.evaluate(async source => {
      const image = new Image();
      image.src = source;
      await image.decode();
      const canvas = document.createElement("canvas");
      canvas.width = 1920;
      canvas.height = 1080;
      const context = canvas.getContext("2d");
      context.imageSmoothingEnabled = true;
      context.imageSmoothingQuality = "high";
      context.drawImage(image, 0, 0, canvas.width, canvas.height);
      return canvas.toDataURL("image/jpeg", 0.92).split(",")[1];
    }, `data:image/png;base64,${png.toString("base64")}`);
    const jpeg = Buffer.from(encoded, "base64");
    if (jpeg.length >= 2 * 1024 * 1024) throw new Error("Video thumbnail exceeds 2 MB.");
    return jpeg;
  } finally {
    await context.close();
  }
}

module.exports = { TITLE_STYLES, validateVideoTitle, renderVideoTitle };
