package sk.iway.iwcm.io.s3;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;
import io.minio.credentials.IamAwsProvider;

/** Validates the selected SDK's IAM provider against local IMDSv2 responses, without accessing AWS. */
@Tag("s3-integration")
class S3IamProviderTest {
    /** The provider obtains an IMDSv2 token, loads a role and caches temporary credentials. */
    @Test
    void loadsTemporaryCredentialsFromMetadataService() throws Exception {
        HttpServer metadata = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicInteger requests = new AtomicInteger();
        metadata.createContext("/", exchange -> {
            requests.incrementAndGet();
            String path = exchange.getRequestURI().getPath();
            String body;
            int status = 200;
            if (path.equals("/latest/api/token") && "PUT".equals(exchange.getRequestMethod())) {
                body = "autotest-imds-token";
            } else if (!"autotest-imds-token".equals(exchange.getRequestHeaders().getFirst("X-aws-ec2-metadata-token"))) {
                status = 401;
                body = "token required";
            } else if (path.endsWith("/security-credentials/")) {
                body = "autotest-role";
            } else if (path.endsWith("/autotest-role")) {
                body = "{\"Code\":\"Success\",\"AccessKeyId\":\"autotest-access\",\"SecretAccessKey\":\"autotest-secret\","
                        + "\"Token\":\"autotest-session\",\"Expiration\":\"" + Instant.now().plusSeconds(3600) + "\"}";
            } else { status = 404; body = "unknown endpoint"; }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (var output = exchange.getResponseBody()) { output.write(bytes); }
        });
        metadata.start();
        try {
            IamAwsProvider provider = new IamAwsProvider("http://127.0.0.1:" + metadata.getAddress().getPort(), null);
            var credentials = provider.fetch();
            assertEquals("autotest-access", credentials.accessKey());
            assertEquals("autotest-session", credentials.sessionToken());
            assertEquals(3, requests.get());
            assertSame(credentials, provider.fetch());
            assertEquals(3, requests.get());
        } finally { metadata.stop(0); }
    }
}
