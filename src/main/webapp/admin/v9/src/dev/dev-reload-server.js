const http = require("http");

const port = Number(process.env.ADMIN_V9_DEV_RELOAD_PORT || 35729);
const host = "127.0.0.1";
const clients = new Set();
let reloadVersion = 0;

function readRequestBody(request) {
    return new Promise((resolve, reject) => {
        let body = "";

        request.on("data", (chunk) => {
            body += chunk;
        });

        request.on("end", () => {
            resolve(body);
        });

        request.on("error", reject);
    });
}

function writeSseEvent(response, eventName, payload) {
    response.write("event: " + eventName + "\n");
    response.write("data: " + JSON.stringify(payload) + "\n\n");
}

function closeAllClients() {
    clients.forEach((response) => {
        response.end();
    });
    clients.clear();
}

const server = http.createServer((request, response) => {
    // this is dev server only, running on 127.0.0.1 allow all origins
    response.setHeader("Access-Control-Allow-Origin", "*");
    response.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
    response.setHeader("Access-Control-Allow-Headers", "Content-Type");

    if (request.method === "OPTIONS") {
        response.writeHead(204);
        response.end();
        return;
    }

    if (request.method === "GET" && request.url === "/health") {
        response.writeHead(200, { "Content-Type": "application/json" });
        response.end(JSON.stringify({ ok: true, clients: clients.size, version: reloadVersion }));
        return;
    }

    if (request.method === "GET" && request.url === "/events") {
        response.writeHead(200, {
            "Content-Type": "text/event-stream",
            "Cache-Control": "no-cache, no-transform",
            Connection: "keep-alive",
            "X-Accel-Buffering": "no",
        });

        response.write("retry: 1000\n\n");
        writeSseEvent(response, "connected", { version: reloadVersion });
        clients.add(response);

        request.on("close", () => {
            clients.delete(response);
        });
        return;
    }

    if (request.method === "POST" && request.url === "/reload") {
        readRequestBody(request).then((body) => {
            let payload = {};

            if (body.length > 0) {
                try {
                    payload = JSON.parse(body);
                } catch (error) {
                    response.writeHead(400, { "Content-Type": "application/json" });
                    response.end(JSON.stringify({ error: "Invalid JSON payload" }));
                    return;
                }
            }

            reloadVersion += 1;

            clients.forEach((client) => {
                writeSseEvent(client, "reload", {
                    kind: payload.kind === "css" ? "css" : "full",
                    version: reloadVersion,
                    updatedAt: Date.now(),
                });
            });

            response.writeHead(204);
            response.end();
        }).catch(() => {
            response.writeHead(500, { "Content-Type": "application/json" });
            response.end(JSON.stringify({ error: "Failed to read request body" }));
        });
        return;
    }

    response.writeHead(404, { "Content-Type": "application/json" });
    response.end(JSON.stringify({ error: "Not found" }));
});

server.on("error", (error) => {
    if (error.code === "EADDRINUSE") {
        console.error("Live reload server port " + port + " is already in use.");
    } else {
        console.error(error);
    }
    process.exit(1);
});

server.listen(port, host, () => {
    console.log("Live reload server listening on http://" + host + ":" + port);
});

function shutdown() {
    closeAllClients();
    server.close(() => {
        process.exit(0);
    });
}

process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);
