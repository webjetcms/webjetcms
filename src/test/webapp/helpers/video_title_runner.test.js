const assert = require("node:assert/strict");
const test = require("node:test");
const path = require("node:path");
const { runVideoTitle } = require("./video_title_runner.js");

test("isolates thumbnail generation and passes literal CLI overrides without a shell", () => {
  let invocation;
  const text = 'New editor & <glow> "title" $(literal)';
  const status = runVideoTitle(["video/308-pb-redesign.js", "--text", text, "--style", "bold"], {
    environment: { CODECEPT_SHOW: "false", VIDEO_SHOT: "outro", CODECEPT_URL: "http://local.test" },
    spawnSync: (command, args, options) => { invocation = { command, args, options }; return { status: 7 }; }
  });
  assert.equal(status, 7);
  assert.equal(invocation.command, process.execPath);
  assert.ok(invocation.args.includes("codecept.title.conf.js"));
  assert.ok(invocation.args.includes("autoLogin"));
  const grep = new RegExp(invocation.args[invocation.args.indexOf("--grep") + 1]);
  assert.ok(grep.test("YouTube thumbnail @title"));
  for (const tag of ["@video", "@audio", "@head", "@title-other"]) assert.ok(!grep.test(`Scenario ${tag}`));
  assert.equal(invocation.options.env.VIDEO_TITLE_TEXT, text);
  assert.equal(invocation.options.env.VIDEO_TITLE_STYLE, "bold");
  assert.equal(invocation.options.env.VIDEO_SHOT, "");
  assert.equal(invocation.options.env.CODECEPT_VIDEO_CURSOR, "false");
  assert.equal(invocation.options.env.CODECEPT_SHOW, "false");
  assert.equal(invocation.options.env.CODECEPT_URL, "http://local.test");
  assert.equal(invocation.options.shell, undefined);
  assert.ok(path.isAbsolute(invocation.args.at(-1)));
});

test("rejects invalid inputs before starting the browser and supports a dry run", () => {
  const errors = [];
  const options = { environment: {}, stderr: value => errors.push(value), spawnSync: () => assert.fail("Must not spawn") };
  for (const args of [[], ["../outside.js"], ["video/308-pb-redesign.js", "--style", "unknown"],
    ["video/308-pb-redesign.js", "--text", " "], ["video/308-pb-redesign.js", "--text", "x".repeat(161)],
    ["video/308-pb-redesign.js", "--unknown"]]) {
    assert.equal(runVideoTitle(args, options), 1);
  }
  assert.match(errors.join("\n"), /Choose glow, clean, bold/);
  assert.equal(runVideoTitle(["--help"], { ...options, stdout: () => {} }), 0);
  assert.equal(runVideoTitle(["video/308-pb-redesign.js", "--dry-run"], {
    ...options,
    spawnSync: (_command, args) => { assert.equal(args[1], "dry-run"); return { status: 0 }; }
  }), 0);
});
