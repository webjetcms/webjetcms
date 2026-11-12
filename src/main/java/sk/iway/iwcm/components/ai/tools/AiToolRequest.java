package sk.iway.iwcm.components.ai.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.system.stripes.CSRF;
import tools.jackson.databind.json.JsonMapper;

/** Presents the catalog's concrete REST request to the same servlet chain, session and domain. */
final class AiToolRequest extends HttpServletRequestWrapper {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Set<String> ENTITY_HEADERS = Set.of("content-type", "content-length", "transfer-encoding", "accept");
    static final String CHECKING_PERMISSION = AiToolRequest.class.getName() + ".checkingPermission";
    private final String path;
    private final String method;
    private final String queryString;
    private final Map<String, String[]> parameters = new LinkedHashMap<>();
    private final Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final byte[] body;

    AiToolRequest(HttpServletRequest original, AiToolRegistry.Entry entry, AiToolInputs.Bound inputs) {
        super(original);
        for (String value : inputs.pathVariables().values()) {
            if (value == null || value.isEmpty() || value.equals(".") || value.equals("..") || value.contains("/")
                    || value.contains("\\") || value.contains("%") || value.contains(";") || value.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException("Invalid path segment");
            }
        }
        path = UriComponentsBuilder.fromPath(entry.path()).encode().buildAndExpand(inputs.pathVariables()).toUriString();
        method = entry.method().name();
        List<String> pairs = new ArrayList<>();
        for (var param : inputs.query().entrySet()) {
            String name = param.getKey();
            if (isReservedParameter(name)) {
                throw new IllegalArgumentException("Reserved query parameter");
            }
            parameters.put(name, param.getValue().toArray(String[]::new));
            for (String value : param.getValue()) {
                pairs.add(UriUtils.encode(name, StandardCharsets.UTF_8) + "=" + UriUtils.encode(value, StandardCharsets.UTF_8));
            }
        }
        queryString = pairs.isEmpty() ? null : String.join("&", pairs);
        body = encodeBody(inputs.body(), entry);
        inputs.headers().forEach((name, value) -> {
            if (safeMappingHeader(name)) headers.put(name, value);
        });
        // The target sees only its own entity headers, never the JSON call envelope.
        headers.put("Content-Type", body.length == 0 ? null : contentType(entry).toString());
        headers.put("Content-Length", body.length == 0 ? null : Integer.toString(body.length));
        headers.put("Transfer-Encoding", null);
        if (!entry.mapping().getProducesCondition().getProducibleMediaTypes().isEmpty()) {
            headers.put("Accept", entry.mapping().getProducesCondition().getProducibleMediaTypes().iterator().next().toString());
        }
    }

    private static MediaType contentType(AiToolRegistry.Entry entry) {
        return entry.mapping().getConsumesCondition().getConsumableMediaTypes().stream()
                .filter(MediaType::isConcrete).findFirst().orElse(MediaType.APPLICATION_JSON);
    }

    private static byte[] encodeBody(Object value, AiToolRegistry.Entry entry) {
        if (value == null) return new byte[0];
        MediaType media = contentType(entry);
        if (media.isCompatibleWith(MediaType.APPLICATION_JSON) || media.getSubtype().endsWith("+json")) {
            return JSON.writeValueAsBytes(value);
        }
        if (value instanceof String s) return s.getBytes(StandardCharsets.UTF_8);
        throw new IllegalArgumentException("Unsupported request body");
    }

    private static boolean isReservedParameter(String name) {
        // These names are read by the global filters before a mapped controller can authorize the operation.
        return Set.of("_logLevel", "_packagerMode", "doFilterLogon", "doShowdocAction", "jspFileName", "showPathFilterTime",
                "docid", "historyid", "fHistoryId", "extURL", "_method", "_printAsPdf").contains(name)
                || name.equalsIgnoreCase(CSRF.getParameterName()) || name.equals(Constants.getString("dmailStatParam"));
    }

    static boolean safeMappingHeader(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        // Preserve credentials, principal, domain and proxy information supplied by the original HTTP transport.
        if (key.equalsIgnoreCase(Constants.getString("logonTokenHeaderName"))
                || key.equalsIgnoreCase(Constants.getString("requestIsSecureHeaderName"))
                || key.equalsIgnoreCase(Constants.getString("xForwardedForHeader", "").replace("::last", ""))) return false;
        if (Set.of("host", "origin", "referer", "forwarded", "connection", "content-length", "transfer-encoding",
                "userinservletcontext", "remote-user", "true-client-ip", "x-real-ip", "x-http-method-override",
                "x-method-override", "x-http-method", "user-agent", "x-remote-user", "x-role", "x-user").contains(key)) return false;
        return !(key.startsWith("sec-") || key.startsWith("proxy-") || key.startsWith("x-forwarded-") || key.startsWith("x-original-")
                || key.contains("auth") || key.contains("cookie") || key.contains("token") || key.contains("csrf")
                || key.contains("api-key") || key.contains("apikey") || key.contains("domain") || key.contains("principal")
                || key.contains("identity") || key.contains("cert") || key.contains("user") || key.contains("role")
                || key.contains("group") || key.contains("session") || key.contains("impersonat") || key.contains("secret")
                || key.contains("tenant") || key.contains("signature"));
    }

    private boolean isTargetRequest() {
        return getDispatcherType() == DispatcherType.REQUEST;
    }

    @Override public String getMethod() { return isTargetRequest() ? method : super.getMethod(); }
    @Override public String getRequestURI() { return isTargetRequest() ? getContextPath() + path : super.getRequestURI(); }
    @Override public String getServletPath() { return isTargetRequest() ? path : super.getServletPath(); }
    @Override public String getPathInfo() { return isTargetRequest() ? null : super.getPathInfo(); }
    @Override public String getQueryString() { return isTargetRequest() ? queryString : super.getQueryString(); }
    @Override public StringBuffer getRequestURL() {
        if (!isTargetRequest()) return super.getRequestURL();
        StringBuffer url = new StringBuffer(super.getRequestURL());
        return url.replace(url.length() - super.getRequestURI().length(), url.length(), getRequestURI());
    }

    @Override public String getParameter(String name) {
        String[] values = parameters.get(name);
        return values == null || values.length == 0 ? null : values[0];
    }
    @Override public String[] getParameterValues(String name) {
        String[] values = parameters.get(name);
        return values == null ? null : values.clone();
    }
    @Override public Enumeration<String> getParameterNames() { return Collections.enumeration(parameters.keySet()); }
    @Override public Map<String, String[]> getParameterMap() { return Collections.unmodifiableMap(parameters); }

    private boolean suppliesHeader(String name) {
        // Authentication and other servlet filters always see the inbound values of custom headers.
        // DispatcherServlet publishes its context immediately before resolving MVC mapping conditions.
        return headers.containsKey(name) && (ENTITY_HEADERS.contains(name.toLowerCase(Locale.ROOT))
                || (isTargetRequest() && getAttribute(CHECKING_PERMISSION) == null
                    && getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE) != null
                    && RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes context
                    && WebUtils.getNativeRequest(context.getRequest(), AiToolRequest.class) == this));
    }

    @Override public String getHeader(String name) { return suppliesHeader(name) ? headers.get(name) : super.getHeader(name); }
    @Override public Enumeration<String> getHeaders(String name) {
        if (!suppliesHeader(name)) return super.getHeaders(name);
        String value = headers.get(name);
        return Collections.enumeration(value == null ? List.of() : List.of(value));
    }
    @Override public Enumeration<String> getHeaderNames() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        names.addAll(Collections.list(super.getHeaderNames()));
        headers.forEach((name, value) -> {
            if (suppliesHeader(name)) {
                if (value == null) names.remove(name);
                else names.add(name);
            }
        });
        return Collections.enumeration(names);
    }
    @Override public int getIntHeader(String name) {
        if (!suppliesHeader(name)) return super.getIntHeader(name);
        return headers.get(name) == null ? -1 : Integer.parseInt(headers.get(name));
    }
    @Override public String getContentType() { return getHeader("Content-Type"); }
    @Override public int getContentLength() { return body.length; }
    @Override public long getContentLengthLong() { return body.length; }
    @Override public String getCharacterEncoding() { return StandardCharsets.UTF_8.name(); }
    @Override public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }
    @Override public ServletInputStream getInputStream() {
        ByteArrayInputStream input = new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override public int read() { return input.read(); }
            @Override public int read(byte[] bytes, int offset, int length) { return input.read(bytes, offset, length); }
            @Override public boolean isFinished() { return input.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) { throw new IllegalStateException("Synchronous request"); }
        };
    }
}
