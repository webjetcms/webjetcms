const path = require("node:path");
const { spawn } = require("node:child_process");

const projectDir = path.resolve(__dirname, "../..");
const rspackCommand = process.platform === "win32" ? "rspack.cmd" : "rspack";
const rspackArgs = ["--config", "rspack.config.prod.js", ...process.argv.slice(2)];

const child = spawn(rspackCommand, rspackArgs, {
    cwd: projectDir,
    env: {
        ...process.env,
        ADMIN_V9_BUNDLE_ANALYZE: "1",
    },
    stdio: "inherit",
});

child.on("exit", (code, signal) => {
    if (signal) {
        process.kill(process.pid, signal);
        return;
    }

    process.exit(code || 0);
});
