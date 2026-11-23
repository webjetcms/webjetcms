package sk.iway.iwcm;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.ServletContext;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockServletContext;

class FileToolsDownloadFileTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 10})
    void downloadsCompleteFileWithinRedirectLimit(int redirectCount, @TempDir Path root) throws Exception {
        byte[] expectedContent = "Downloaded content after redirects.\n".getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requests = new AtomicInteger();
        String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/download/", exchange -> {
            requests.incrementAndGet();
            int step = Integer.parseInt(exchange.getRequestURI().getPath().substring("/download/".length()));
            try {
                if (step < redirectCount) {
                    exchange.getResponseHeaders().set("Location", baseUrl + "/download/" + (step + 1));
                    // Exercise manual redirects rather than HttpURLConnection's automatic redirects.
                    exchange.sendResponseHeaders(308, -1);
                } else {
                    exchange.sendResponseHeaders(200, expectedContent.length);
                    exchange.getResponseBody().write(expectedContent);
                }
            } finally {
                exchange.close();
            }
        });
        ServletContext originalContext = Constants.getServletContext();
        server.start();
        try {
            Constants.setServletContext(new MockServletContext() {
                @Override
                public String getRealPath(String path) {
                    return root.resolve(path.substring(1)).toString();
                }
            });

            assertTrue(FileTools.downloadFile(baseUrl + "/download/0", "/downloads/result.txt", null, 0, 1));
            assertEquals(redirectCount + 1, requests.get(), "Each redirect and the final download need one request");
            assertArrayEquals(expectedContent, Files.readAllBytes(root.resolve("downloads/result.txt")));
        } finally {
            Constants.setServletContext(originalContext);
            server.stop(0);
        }
    }

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
