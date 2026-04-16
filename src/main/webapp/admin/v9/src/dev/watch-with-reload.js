const path = require("path");
const { fork, spawn } = require("child_process");

const projectDir = path.resolve(__dirname, "../..");
const serverScript = path.resolve(__dirname, "dev-reload-server.js");
const port = String(process.env.ADMIN_V9_DEV_RELOAD_PORT || 35729);
const rspackCommand = process.platform === "win32" ? "rspack.cmd" : "rspack";

let isShuttingDown = false;

const serverProcess = fork(serverScript, [], {
    cwd: projectDir,
    env: {
        ...process.env,
        ADMIN_V9_DEV_RELOAD_PORT: port,
    },
    stdio: ["inherit", "inherit", "inherit", "ipc"],
});

const rspackProcess = spawn(rspackCommand, ["--config", "rspack.config.dev.js", "--watch"], {
    cwd: projectDir,
    env: {
        ...process.env,
        ADMIN_V9_DEV_RELOAD: "1",
        ADMIN_V9_DEV_RELOAD_PORT: port,
    },
    stdio: "inherit",
});

function terminateChild(child, signal) {
    if (!child || child.killed) return;
    child.kill(signal);
}

function shutdown(signal) {
    if (isShuttingDown) return;
    isShuttingDown = true;

    terminateChild(rspackProcess, signal || "SIGTERM");
    terminateChild(serverProcess, signal || "SIGTERM");
}

serverProcess.on("exit", (code) => {
    if (isShuttingDown) return;

    console.error("Live reload server stopped with exit code " + code + ".");
    shutdown("SIGTERM");
    process.exit(code || 1);
});

rspackProcess.on("exit", (code, signal) => {
    shutdown(signal || "SIGTERM");

    if (signal) {
        process.kill(process.pid, signal);
        return;
    }

    process.exit(code || 0);
});

process.on("SIGINT", () => shutdown("SIGINT"));
process.on("SIGTERM", () => shutdown("SIGTERM"));