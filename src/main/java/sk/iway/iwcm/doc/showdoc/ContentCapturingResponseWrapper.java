package sk.iway.iwcm.doc.showdoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import sk.iway.iwcm.Logger;

/**
 * Response wrapper that captures output for showdoc.do processing (like style to head move, CSP nonce etc).
 * Only captures body content - all headers, cookies, content-type etc. pass through to original response.
 */
public class ContentCapturingResponseWrapper extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private ServletOutputStream servletOutputStream;
    private PrintWriter printWriter;
    private boolean usingOutputStream = false;
    private boolean usingWriter = false;

    // Only redirect and error need special handling
    private String redirectLocation;
    private String errorMessage;
    private int errorCode;
    private boolean hasError = false;
    private String contentType;

    public ContentCapturingResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    // ---- Capture output only ----

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (usingWriter) {
            throw new IllegalStateException("getWriter() has already been called");
        }
        usingOutputStream = true;

        if (servletOutputStream == null) {
            servletOutputStream = new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    byteArrayOutputStream.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    byteArrayOutputStream.write(b, off, len);
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setWriteListener(WriteListener listener) {
                    // Not implemented for capturing
                }
            };
        }
        return servletOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (usingOutputStream) {
            throw new IllegalStateException("getOutputStream() has already been called");
        }
        usingWriter = true;

        if (printWriter == null) {
            String encoding = getCharacterEncoding();
            if (encoding == null) {
                encoding = StandardCharsets.UTF_8.name();
            }
            printWriter = new PrintWriter(new OutputStreamWriter(byteArrayOutputStream, encoding));
        }
        return printWriter;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (printWriter != null) {
            printWriter.flush();
        }
        // Don't call super - we don't want to flush to original response yet
    }

    @Override
    public void resetBuffer() {
        byteArrayOutputStream.reset();
    }

    // ---- Content-length is ignored - we set our own after processing ----

    @Override
    public void setContentLength(int len) {
        // Ignore - we will set the correct length after processing
    }

    @Override
    public void setContentLengthLong(long len) {
        // Ignore - we will set the correct length after processing
    }

    @Override
    public void setContentType(String type) {
        this.contentType = type;
        // Pass through content type to original response
        super.setContentType(type);
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    // ---- Capture redirect and error for special handling ----

    @Override
    public void sendError(int sc) throws IOException {
        this.errorCode = sc;
        this.hasError = true;
        // Don't call super - we handle it after processing
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        this.errorCode = sc;
        this.errorMessage = msg;
        this.hasError = true;
        // Don't call super - we handle it after processing
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        this.redirectLocation = location;
        // Don't call super - we handle it after processing
    }

    // ---- Getters for captured state ----

    public boolean hasError() {
        return hasError;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getRedirectLocation() {
        return redirectLocation;
    }

    public String getCapturedContent() {
        try {
            flushBuffer();
            String encoding = getCharacterEncoding();
            if (encoding == null) {
                encoding = StandardCharsets.UTF_8.name();
            }
            return byteArrayOutputStream.toString(encoding);
        } catch (IOException e) {
            Logger.error(ContentCapturingResponseWrapper.class, e);
            return byteArrayOutputStream.toString();
        }
    }
}
