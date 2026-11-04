<%@ page pageEncoding="UTF-8" trimDirectiveWhitespaces="true" import="java.io.*,java.net.*,java.net.http.*,java.nio.charset.StandardCharsets,java.time.Duration,java.util.*,java.util.regex.*,org.jsoup.Jsoup,sk.iway.iwcm.Identity,sk.iway.iwcm.users.UsersDB" %><%!
// Test-only adapter installed temporarily by BranchPreviewPocTest, never shipped in the WAR.
private String p01AdminUrl(String contextPath, String target) {
    String dimensions = "";
    if (target.endsWith("?w=16&h=16")) {
        target = target.substring(0, target.indexOf('?'));
        dimensions = "&w=16&h=16";
    }
    return contextPath + "/admin/branch-p01-autotest.jsp?target=" + URLEncoder.encode(target, StandardCharsets.UTF_8) + dimensions;
}
%><%
response.setHeader("Cache-Control", "private, no-store");
response.setHeader("X-Robots-Tag", "noindex");
Identity admin = UsersDB.getCurrentUser(request);
if (admin == null || !admin.isAdmin()) { response.setStatus(403); return; }
if (!Set.of("GET", "HEAD").contains(request.getMethod())) { response.setStatus(405); return; }
String target = request.getParameter("target");
if (target == null || !target.matches("/__branch-preview/(42|43)/.*")
    || target.contains("..") || target.contains("%") || target.contains("\\")) {
    response.setStatus(400); return;
}
URI route;
try { route = URI.create(target); } catch (IllegalArgumentException invalid) { response.setStatus(400); return; }
String branchRoot = target.startsWith("/__branch-preview/42/") ? "/__branch-preview/42" : "/__branch-preview/43";
String logicalPath = route.getPath().substring(branchRoot.length());
if (!Set.of("/home.html", "/new.html", "/existing.html", "/target.html", "/images/p01.png",
        "/thumb/images/p01.png", "/templates/p01/probe.css", "/templates/p01/probe.js").contains(logicalPath)
    || route.getRawQuery() != null) {
    response.setStatus(400); return;
}
if (logicalPath.equals("/thumb/images/p01.png")) {
    if (!"16".equals(request.getParameter("w")) || !"16".equals(request.getParameter("h"))) {
        response.setStatus(400); return;
    }
    target += "?w=16&h=16";
}
Properties descriptor = new Properties();
try (InputStream input = application.getResourceAsStream("/WEB-INF/p01-gateway-autotest.properties")) {
    if (input == null) { response.setStatus(404); return; }
    descriptor.load(input);
}
String origin = descriptor.getProperty("origin", "");
if (!origin.matches("http://127\\.0\\.0\\.1:[0-9]+")) { response.setStatus(503); return; }
try {
CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).connectTimeout(Duration.ofSeconds(5)).build();
HttpResponse<byte[]> login = client.send(HttpRequest.newBuilder(URI.create(origin + "/_p01_login?token="
    + URLEncoder.encode(descriptor.getProperty("loginToken", ""), StandardCharsets.UTF_8))).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
if (login.statusCode() != 204) { response.setStatus(503); return; }
HttpResponse<byte[]> upstream = client.send(HttpRequest.newBuilder(URI.create(origin + target)).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
response.setStatus(upstream.statusCode());
String type = upstream.headers().firstValue("content-type").orElse("application/octet-stream");
response.setContentType(type);
if ("HEAD".equals(request.getMethod())) return;
if (type.startsWith("text/html")) {
    var html = Jsoup.parse(new String(upstream.body(), StandardCharsets.UTF_8));
    for (var element : html.select("[href],[src]")) {
        for (String attribute : List.of("href", "src")) {
            if (element.hasAttr(attribute) && element.attr(attribute).startsWith("/__branch-preview/"))
                element.attr(attribute, p01AdminUrl(request.getContextPath(), element.attr(attribute)));
        }
    }
    html.body().prependElement("aside").attr("id", "p01-admin-context")
        .attr("data-logical-domain", descriptor.getProperty("logicalDomain", ""))
        .text("P01 admin-host preview; logical domain fixture: " + descriptor.getProperty("logicalDomain", ""));
    out.print(html.outerHtml());
} else if (type.startsWith("text/css") || type.startsWith("text/javascript")) {
    String text = new String(upstream.body(), StandardCharsets.UTF_8);
    Matcher urls = Pattern.compile("(['\"])(/__branch-preview/(?:42|43)/[^'\"\\s]+)\\1").matcher(text);
    StringBuffer rewritten = new StringBuffer();
    while (urls.find()) urls.appendReplacement(rewritten, Matcher.quoteReplacement(urls.group(1)
        + p01AdminUrl(request.getContextPath(), urls.group(2)) + urls.group(1)));
    urls.appendTail(rewritten);
    out.print(rewritten.toString());
} else {
    out.clearBuffer();
    response.getOutputStream().write(upstream.body());
}
} catch (InterruptedException interrupted) {
    Thread.currentThread().interrupt();
    response.setStatus(503);
}
%>
