const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const test = require("node:test");
const { runVideoShots } = require("./video_shots_runner.js");

function fixture(t) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "wj-video-shots-"));
  fs.mkdirSync(path.join(root, "video"));
  fs.writeFileSync(path.join(root, "video", "example.js"), `
    throw new Error("Preflight must never execute scenario code");
    const videoPlan = { language: "en", shots: [
      { id: "first", type: "auto", durationSeconds: 1, title: "First", "text-en": "First", shot: async () => {} },
      { id: "card", type: "manual", durationSeconds: 1, title: "Card", "text-en": "Card" },
      { id: "presenter", type: "head", durationSeconds: 1, title: "Presenter", "text-en": "Presenter" },
      { id: "last", type: "auto", durationSeconds: 1, title: "Last", "text-en": "Last", shot: async () => {} }
    ] };
  `);
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  return root;
}

test("runs every shot in plan order through video, overriding inherited selection and retaining recording settings", t => {
  const root = fixture(t);
  const calls = [];
  const environment = { VIDEO_SHOT: "last", CODECEPT_VIDEO_WIDTH: "1280", CODECEPT_URL: "https://test.example" };
  const status = runVideoShots(["video/example.js"], {
    webappRoot: root, environment, stdout() {}, stderr: assert.fail,
    spawnSync(command, args, options) {
      calls.push(options.env.VIDEO_SHOT);
      assert.equal(command, "npm");
      assert.deepEqual(args, ["run", "video", "--", fs.realpathSync(path.join(root, "video", "example.js"))]);
      assert.equal(options.cwd, root);
      assert.equal(options.stdio, "inherit");
      assert.equal(options.env.CODECEPT_VIDEO_WIDTH, "1280");
      assert.equal(options.env.CODECEPT_URL, environment.CODECEPT_URL);
      return { status: 0 };
    }
  });
  assert.equal(status, 0);
  assert.deepEqual(calls, ["first", "card", "presenter", "last"]);
  assert.equal(environment.VIDEO_SHOT, "last");
});

test("continues after shot failures and reports a failing exit code with all failed IDs", t => {
  const calls = [];
  const errors = [];
  const status = runVideoShots(["video/example.js"], {
    webappRoot: fixture(t), stdout() {}, stderr: message => errors.push(message),
    spawnSync(command, args, options) {
      calls.push(options.env.VIDEO_SHOT);
      return { status: ["first", "presenter"].includes(options.env.VIDEO_SHOT) ? 1 : 0 };
    }
  });
  assert.equal(status, 1);
  assert.equal(calls.length, 4);
  assert.match(errors[0], /Failed shots: first, presenter/);
});

test("stops on interrupted or unavailable child processes", t => {
  const root = fixture(t);
  for (const result of [{ signal: "SIGINT" }, { error: new Error("Cannot start npm") }]) {
    let calls = 0;
    const errors = [];
    assert.equal(runVideoShots(["video/example.js"], {
      webappRoot: root, stdout() {}, stderr: message => errors.push(message),
      spawnSync() { calls++; return result; }
    }), 1);
    assert.equal(calls, 1);
    assert.match(errors[0], /SIGINT|Cannot start npm/);
  }
});

test("validates arguments, paths, metadata and all automatic callbacks before starting a browser", t => {
  const root = fixture(t);
  const options = { webappRoot: root, stdout() {}, stderr() {}, spawnSync: assert.fail };
  assert.equal(runVideoShots(["--help"], options), 0);
  for (const args of [[], ["video/example.js", "extra"], ["../outside.js"], ["video/missing.js"]]) {
    assert.equal(runVideoShots(args, options), 1);
  }
  const file = path.join(root, "video", "example.js");
  const source = fs.readFileSync(file, "utf8");
  for (const invalid of [source.replace('id: "last"', 'id: "first"'),
    source.replaceAll(', shot: async () => {}', ''), source.replace('"text-en": "Last"', '"text-sk": "Last"')]) {
    fs.writeFileSync(file, invalid);
    assert.equal(runVideoShots(["video/example.js"], options), 1);
  }
});
