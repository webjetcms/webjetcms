package sk.iway.iwcm.system.spring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sk.iway.iwcm.Constants;
import sk.iway.upload.DiskMultiPartRequestHandler;
import sk.iway.upload.UploadedFile;

class LegacyCommonsMultipartTomcatIntegrationTest {

    private static final byte[] FILE_PAYLOAD = new byte[] { 0, 1, 2, 3, (byte) 0xff, 42 };
    private static final String FORM_VALUE = "legacy-field-value";

    @TempDir
    Path tomcatBase;

    @Test
    void embeddedRegistrationLeavesMultipartBodyAvailableForLegacyCommonsParser() throws Exception {
        String boundary = "webjet-legacy-multipart-test-boundary";
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
        factory.setBaseDirectory(tomcatBase.toFile());
        ServletContext previousServletContext = Constants.getServletContext();
        WebServer server = null;

        try {
            server = factory.getWebServer(servletContext -> {
                Constants.setServletContext(servletContext);
                ServletRegistration.Dynamic servlet = servletContext.addServlet(
                    "legacyCommonsUploadProbe", new LegacyCommonsUploadProbeServlet());
                servlet.addMapping("/upload");
                MultipartConfigElement multipartConfig = new SpringBootStarter.ProductionServletConfiguration()
                    .multipleFileUploadServletRegistration()
                    .getMultipartConfig();
                if (multipartConfig != null) {
                    servlet.setMultipartConfig(multipartConfig);
                }

                FilterRegistration.Dynamic filter = servletContext.addFilter(
                    "eagerParameterFilter",
                    (request, response, chain) -> {
                        request.getParameterMap();
                        chain.doFilter(request, response);
                    });
                filter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/*");
            });
            server.start();
            HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getPort() + "/upload"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody(boundary)))
                    .build(),
                HttpResponse.BodyHandlers.ofByteArray());

            assertEquals(HttpServletResponse.SC_OK, response.statusCode(),
                () -> new String(response.body(), StandardCharsets.UTF_8));
            assertArrayEquals(FILE_PAYLOAD, response.body());
        } finally {
            try {
                if (server != null) {
                    server.destroy();
                }
            } finally {
                Constants.setServletContext(previousServletContext);
            }
        }
    }

    private static byte[] multipartBody(String boundary) {
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        body.writeBytes(("--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"description\"\r\n\r\n"
            + FORM_VALUE + "\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"file\"; filename=\"probe.txt\"\r\n"
            + "Content-Type: application/octet-stream\r\n\r\n")
            .getBytes(StandardCharsets.US_ASCII));
        body.writeBytes(FILE_PAYLOAD);
        body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));
        return body.toByteArray();
    }

    private static final class LegacyCommonsUploadProbeServlet extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response)
                throws ServletException, IOException {
            DiskMultiPartRequestHandler handler = new DiskMultiPartRequestHandler();
            boolean parsed = false;
            try {
                HttpServletRequest wrappedRequest = handler.handleRequest(request);
                parsed = true;
                UploadedFile file = handler.getFileElements().get("file");
                if (file == null || !FORM_VALUE.equals(wrappedRequest.getParameter("description"))) {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incomplete multipart request");
                    return;
                }
                response.setContentType("application/octet-stream");
                response.getOutputStream().write(file.getFileData());
            } catch (FileUploadException exception) {
                throw new ServletException("Commons FileUpload could not parse the request", exception);
            } finally {
                if (parsed) {
                    handler.rollback();
                }
            }
        }
    }

}
