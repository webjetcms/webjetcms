const assert = require("node:assert/strict");
const fs = require("node:fs/promises");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");
const { execFile } = require("node:child_process");
const { promisify } = require("node:util");
const { chromium } = require("playwright");
const { PNG } = require("pngjs");
const { installShotTiming, trimVideoSetup } = require("./video_shot_trim.js");

test("maps slate timestamps to encoded frames despite per-frame rounding and isolates tabs", async () => {
  class Recorder {
    async _launch() {}
    writeFrame(frame, timestamp) { this._lastFrameBuffer = frame; this._lastFrameTimestamp = timestamp; }
  }
  installShotTiming(Recorder);
  installShotTiming(Recorder);
  const first = new Recorder();
  const second = new Recorder();
  await first._launch({ outputFile: "/tmp/wj-timing-first.webm" });
  await second._launch({ outputFile: "/tmp/wj-timing-second.webm" });
  for (const time of [100, 100.01, 100.02, 100.03, 101]) first.writeFrame(Buffer.from("frame"), time);
  second.writeFrame(Buffer.from("frame"), 200);
  const starts = [];
  for (const [file, time] of [["first", 100.025], ["second", 200]]) {
    await trimVideoSetup(`/tmp/wj-timing-${file}.webm`, { startTime: time, endTime: time + 0.001 }, {
      execFile: async (command, args) => starts.push(args[args.indexOf("-ss") + 1])
    });
  }
  assert.deepEqual(starts, ["0.08", "0"], "Use the frame displayed at the slate marker, with actual 25-fps positions");
});

test("rejects a missing slate instead of publishing setup footage", async () => {
  await assert.rejects(trimVideoSetup("/tmp/wj-no-slate.webm", { startTime: 1, endTime: 3 }), /Cannot locate the shot slate/);
});

for (const type of ["auto", "manual", "head"]) test(`${type} retake starts on its slate, with three seconds before automatic actions`, async t => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), "wj-shot-trim-"));
  const previousCodeceptjs = global.codeceptjs;
  const previousShot = process.env.VIDEO_SHOT;
  let browser;
  t.after(async () => {
    await browser?.close();
    await fs.rm(root, { recursive: true, force: true });
    if (previousShot === undefined) delete process.env.VIDEO_SHOT;
    else process.env.VIDEO_SHOT = previousShot;
    if (previousCodeceptjs === undefined) delete global.codeceptjs;
    else global.codeceptjs = previousCodeceptjs;
  });
  process.env.VIDEO_SHOT = "sample";
  global.codeceptjs = require("codeceptjs");
  const coreRoot = path.dirname(require.resolve("playwright-core/package.json"));
  installShotTiming(require(path.join(coreRoot, "lib/server/chromium/videoRecorder.js")).VideoRecorder);
  const VideoHelper = require("./video_helper.js");
  const { recordVideoPlan } = require("./feature_video_plan.js");
  browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 320, height: 240 }, recordVideo: { dir: root, size: { width: 320, height: 240 } } });
  const page = await context.newPage();
  const playwright = { page };
  const helper = new VideoHelper({});
  Object.defineProperty(helper, "helpers", { value: { Playwright: playwright } });
  await recordVideoPlan({
    say: async () => {},
    videoTitle: helper.videoTitle.bind(helper),
    wait: seconds => new Promise(resolve => setTimeout(resolve, seconds * 1000))
  }, {
    plan: { shots: [{ id: "sample", type, durationSeconds: 1, title: "Sample", "text-sk": "",
      shot: async () => page.evaluate(() => document.body.style.background = "blue") }] },
    setup: async () => page.setContent('<style>body { margin: 0; background: lime; }</style>'),
    prepare: async () => page.evaluate(() => document.body.style.background = "red")
  });
  const video = page.video();
  await context.close();
  const trimmed = await trimVideoSetup(await video.path(), playwright.videoShotSlate);
  const { registry } = require(path.join(coreRoot, "lib/server/registry/index.js"));
  await promisify(execFile)(registry.findExecutable("ffmpeg").executablePathOrDie("javascript"), [
    "-v", "error", "-i", trimmed, "-vf", "crop=2:2:0:0", "-r", "10", path.join(root, "frame-%03d.png")
  ]);
  const pixels = [];
  for (const name of (await fs.readdir(root)).filter(name => name.endsWith(".png")).sort()) {
    pixels.push([...PNG.sync.read(await fs.readFile(path.join(root, name))).data.subarray(0, 3)]);
  }
  const red = pixel => pixel[0] > 200 && pixel[1] < 30 && pixel[2] < 30;
  const blue = pixel => pixel[2] > 200 && pixel[0] < 30 && pixel[1] < 30;
  assert.ok(pixels[0].every(value => value < 20), `First frame must show the black slate, without setup: ${pixels[0]}`);
  if (type !== "auto") {
    assert.ok(pixels.length >= 18, "Warning clips must retain the two-second slate");
    assert.ok(pixels.slice(0, 18).every(pixel => pixel.every(value => value < 20)));
    return;
  }
  const sceneStart = pixels.findIndex(red);
  const actionStart = pixels.findIndex(blue);
  assert.ok(sceneStart >= 18 && sceneStart <= 22, `Expected the complete two-second slate, got ${sceneStart / 10}s`);
  assert.ok(actionStart - sceneStart >= 29, "The clean scene must last three seconds before the action (one-frame tolerance)");
  assert.ok(pixels.slice(sceneStart, actionStart).every(red), "No setup or black slate may interrupt the transition hold");
});
