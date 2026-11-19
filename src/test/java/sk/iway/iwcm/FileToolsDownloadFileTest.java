package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

class FileToolsDownloadFileTest {

    @Test
    void redirectCycleStopsAfterTenRedirects() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requests = new AtomicInteger();
        String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/redirect";
        server.createContext("/redirect", exchange -> {
            // Stop a broken implementation without hanging the test.
            int status = requests.incrementAndGet() <= 11 ? 308 : 500;
            exchange.getResponseHeaders().set("Location", url);
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        try {
            assertFalse(FileTools.downloadFile(url, "/unused-redirect-test", null, 0, 1));
            assertEquals(11, requests.get(), "The initial request may be followed by at most ten redirects");
        } finally {
            server.stop(0);
        }
    }
}
