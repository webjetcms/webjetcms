package sk.iway.iwcm.branch;

import java.io.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

/** Buffers supported text output; private headers cannot be overwritten downstream. */
public final class BranchPreviewResponseWrapper extends HttpServletResponseWrapper {
    private final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
    private final HttpServletRequest request;
    private final HttpServletResponse original;
    private PrintWriter writer;
    private final ServletOutputStream output=new ServletOutputStream() {
        @Override public boolean isReady(){return true;}
        @Override public void setWriteListener(WriteListener listener){throw new UnsupportedOperationException("Synchronous preview only");}
        @Override public void write(int value){bytes.write(value);}
    };
    public BranchPreviewResponseWrapper(HttpServletResponse response,HttpServletRequest request){
        // Like WJResponseWrapper, isolate Tomcat 11 forwards from the real response buffer.
        super(new org.springframework.mock.web.MockHttpServletResponse());
        this.original=response;this.request=request;setCharacterEncoding("UTF-8");privateHeaders();
    }
    @Override public void addCookie(Cookie cookie){original.addCookie(cookie);}
    private void privateHeaders(){super.setHeader("Cache-Control","private, no-store, max-age=0");super.setHeader("Pragma","no-cache");super.setHeader("Expires","0");super.setHeader("X-Robots-Tag","noindex, nofollow, noarchive");}
    private boolean reserved(String name){return java.util.Set.of("cache-control","pragma","expires","etag","last-modified","content-length").contains(name.toLowerCase(java.util.Locale.ROOT));}
    @Override public void setHeader(String name,String value){if(!reserved(name))super.setHeader(name,"Location".equalsIgnoreCase(name)?BranchPreviewUrls.toPreviewUrl(value,request):value);}
    @Override public void addHeader(String name,String value){if(!reserved(name))super.addHeader(name,"Location".equalsIgnoreCase(name)?BranchPreviewUrls.toPreviewUrl(value,request):value);}
    @Override public void setDateHeader(String name,long value){if(!reserved(name))super.setDateHeader(name,value);}
    @Override public void setIntHeader(String name,int value){if(!reserved(name))super.setIntHeader(name,value);}
    @Override public void setContentLength(int size){ }
    @Override public void setContentLengthLong(long size){ }
    @Override public ServletOutputStream getOutputStream(){return output;}
    @Override public PrintWriter getWriter() throws IOException {if(writer==null)writer=new PrintWriter(new OutputStreamWriter(bytes,getCharacterEncoding()));return writer;}
    @Override public void flushBuffer(){if(writer!=null)writer.flush();}
    @Override public void resetBuffer(){bytes.reset();}
    @Override public void reset(){super.reset();bytes.reset();privateHeaders();}
    @Override public void sendRedirect(String location) {setStatus(302);setHeader("Location",location);}
    @Override public void sendRedirect(String location,int status,boolean clearBuffer) {if(clearBuffer)resetBuffer();setStatus(status);setHeader("Location",location);}
    @Override public void sendError(int status,String message) throws IOException {resetBuffer();setStatus(status);setContentType("text/plain;charset=UTF-8");getWriter().write(message==null?"Preview error":message);}
    @Override public void sendError(int status) throws IOException {sendError(status,"Preview resource unavailable");}
    public void finish() throws IOException {
        flushBuffer();privateHeaders();
        byte[] body=bytes.toByteArray();String type=getContentType();
        if(type!=null && (type.contains("text/html")||type.contains("text/css"))) {
            String value=new String(body,getCharacterEncoding());
            value=type.contains("text/html")?BranchPreviewUrls.rewriteHtml(value,request):BranchPreviewUrls.rewriteCss(value,request);
            body=value.getBytes(getCharacterEncoding());
        }
        original.setStatus(getStatus());
        original.setCharacterEncoding(getCharacterEncoding());
        if(type!=null)original.setContentType(type);
        for(String name:getHeaderNames()) {
            boolean first=true;
            for(String value:getHeaders(name)) {if(first)original.setHeader(name,value);else original.addHeader(name,value);first=false;}
        }
        if(!"HEAD".equals(request.getMethod())) original.getOutputStream().write(body);
    }
}
