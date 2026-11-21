package sk.iway.iwcm.branch;

import java.io.*;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.mock.web.MockHttpServletResponse;

/** Rewrites HTML and CSS and streams other preview content with private response headers. */
public final class BranchPreviewResponseWrapper extends HttpServletResponseWrapper {
    // Keep only a short prefix when a renderer starts writing before setting its content type.
    private static final int UNTYPED_BUFFER_LIMIT = 8192;
    private static final OutputStream DISCARD = OutputStream.nullOutputStream();
    private static final Set<String> RESERVED_HEADERS = Set.of(
        "cache-control", "pragma", "expires", "x-robots-tag", "etag", "last-modified", "content-length");

    private final ByteArrayOutputStream buffered = new ByteArrayOutputStream();
    private final HttpServletRequest request;
    private final HttpServletResponse original;
    private final boolean head;
    private PrintWriter writer;
    private ServletOutputStream streaming;
    private boolean discarding;

    private final ServletOutputStream output = new ServletOutputStream() {
        @Override public boolean isReady() { return true; }
        @Override public void setWriteListener(WriteListener listener) { throw new UnsupportedOperationException("Synchronous preview only"); }
        @Override public void write(int value) throws IOException { destination(1).write(value); }
        @Override public void write(byte[] data, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, data.length);
            if (length > 0) destination(length).write(data, offset, length);
        }
        @Override public void flush() throws IOException {
            if (streaming != null && !discarding) streaming.flush();
        }
    };

    public BranchPreviewResponseWrapper(HttpServletResponse response, HttpServletRequest request) {
        // Like WJResponseWrapper, isolate Tomcat 11 forwards until the original response can be written.
        super(new MockHttpServletResponse());
        this.original = response;
        this.request = request;
        this.head = "HEAD".equals(request.getMethod());
        setCharacterEncoding("UTF-8");
        privateHeaders();
    }

    private String mediaType() {
        String type = getContentType();
        if (type == null) return "";
        int parameter = type.indexOf(';');
        return (parameter < 0 ? type : type.substring(0, parameter)).trim().toLowerCase(Locale.ROOT);
    }

    private OutputStream destination(int length) throws IOException {
        if (head || discarding) return DISCARD;
        if (streaming != null) return streaming;
        String type = mediaType();
        if ("text/html".equals(type) || "text/css".equals(type)) return buffered;
        if (type.isEmpty() && length <= UNTYPED_BUFFER_LIMIT - buffered.size()) return buffered;

        copyHeaders();
        // Subsequent status/header updates and commit checks now follow normal servlet semantics.
        setResponse(original);
        streaming = original.getOutputStream();
        if (buffered.size() > 0) {
            buffered.writeTo(streaming);
            buffered.reset();
        }
        return streaming;
    }

    @Override public void addCookie(Cookie cookie){original.addCookie(cookie);}
    private void privateHeaders(){super.setHeader("Cache-Control","private, no-store, max-age=0");super.setHeader("Pragma","no-cache");super.setHeader("Expires","0");super.setHeader("X-Robots-Tag","noindex, nofollow, noarchive");}
    private boolean reserved(String name){return RESERVED_HEADERS.contains(name.toLowerCase(Locale.ROOT));}
    @Override public void setHeader(String name,String value){if(!reserved(name))super.setHeader(name,"Location".equalsIgnoreCase(name)?BranchPreviewUrls.toPreviewUrl(value,request):value);}
    @Override public void addHeader(String name,String value){if(!reserved(name))super.addHeader(name,"Location".equalsIgnoreCase(name)?BranchPreviewUrls.toPreviewUrl(value,request):value);}
    @Override public void setDateHeader(String name,long value){if(!reserved(name))super.setDateHeader(name,value);}
    @Override public void addDateHeader(String name,long value){if(!reserved(name))super.addDateHeader(name,value);}
    @Override public void setIntHeader(String name,int value){if(!reserved(name))super.setIntHeader(name,value);}
    @Override public void addIntHeader(String name,int value){if(!reserved(name))super.addIntHeader(name,value);}
    @Override public void setContentLength(int size){ }
    @Override public void setContentLengthLong(long size){ }
    @Override public ServletOutputStream getOutputStream(){return output;}
    @Override public PrintWriter getWriter() throws IOException {
        if (writer == null) writer = new PrintWriter(new OutputStreamWriter(output, getCharacterEncoding()));
        return writer;
    }
    @Override public void flushBuffer() throws IOException {
        if (writer != null) writer.flush();
        if (streaming != null) original.flushBuffer();
    }
    @Override public void resetBuffer() {
        super.resetBuffer();
        // Discard characters still in the writer's encoder as well as bytes already buffered.
        discarding = true;
        try {
            if (writer != null) writer.flush();
        } finally {
            discarding = false;
        }
        streaming = null;
        buffered.reset();
    }
    @Override public void reset() {
        super.reset();
        if (getResponse() == original) setResponse(new MockHttpServletResponse());
        streaming = null;
        writer = null;
        buffered.reset();
        setCharacterEncoding("UTF-8");
        privateHeaders();
    }
    @Override public void sendRedirect(String location) {sendRedirect(location,302,true);}
    @Override public void sendRedirect(String location,int status,boolean clearBuffer) {
        if (isCommitted()) throw new IllegalStateException("Preview response already committed");
        if (clearBuffer) resetBuffer();
        setStatus(status);
        setHeader("Location",location);
    }
    @Override public void sendError(int status,String message) throws IOException {resetBuffer();setStatus(status);setContentType("text/plain;charset=UTF-8");getWriter().write(message==null?"Preview error":message);}
    @Override public void sendError(int status) throws IOException {sendError(status,"Preview resource unavailable");}

    private void copyHeaders() {
        if (getResponse() == original) return;
        privateHeaders();
        original.setStatus(getStatus());
        original.setCharacterEncoding(getCharacterEncoding());
        if (getContentType() != null) original.setContentType(getContentType());
        for (String name : getHeaderNames()) {
            boolean first = true;
            for (String value : getHeaders(name)) {
                if (first) original.setHeader(name, value);
                else original.addHeader(name, value);
                first = false;
            }
        }
    }

    public void finish() throws IOException {
        if (writer != null) writer.flush();
        if (streaming != null) return;
        copyHeaders();
        if (head) return;
        String type = mediaType();
        if ("text/html".equals(type) || "text/css".equals(type)) {
            String value = buffered.toString(getCharacterEncoding());
            value = "text/html".equals(type) ? BranchPreviewUrls.rewriteHtml(value,request) : BranchPreviewUrls.rewriteCss(value,request);
            original.getOutputStream().write(value.getBytes(getCharacterEncoding()));
        } else if (buffered.size() > 0) {
            buffered.writeTo(original.getOutputStream());
        }
    }
}
