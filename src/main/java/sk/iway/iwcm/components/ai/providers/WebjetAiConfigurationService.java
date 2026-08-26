package sk.iway.iwcm.components.ai.providers;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;

import com.webjetcms.ai.AiProviderConfig;

import jakarta.servlet.http.HttpServletRequest;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.system.multidomain.DomainRequestBeanScope;

/** Resolves a request-scoped WebJET configuration snapshot for the standalone library. */
@Service
public class WebjetAiConfigurationService {

    static final String FILE_NAME_PROMPT = "ai_generateFileNamePrompt";
    static final String AUDIT_MAX_LENGTH = "ai_auditMaxLength";
    static final String PROVIDER_CONNECT_TIMEOUT_SECONDS = "ai_providerConnectTimeoutSeconds";
    static final String PROVIDER_RESPONSE_TIMEOUT_SECONDS = "ai_providerResponseTimeoutSeconds";

    private final ConcurrentMap<String, VersionedConfiguration> versions = new ConcurrentHashMap<>();
    private final AtomicLong revisionSequence = new AtomicLong();

    public AiProviderConfig resolve(AiInterface provider) {
        return resolve(provider, (HttpServletRequest) null);
    }

    public AiProviderConfig resolve(AiInterface provider, HttpServletRequest request) {
        return buildConfiguration(provider, trustedReferer(request, null));
    }

    public AiProviderConfig resolveForDomain(AiInterface provider, String domainName) {
        try (DomainRequestBeanScope ignored = DomainRequestBeanScope.open(domainName)) {
            return buildConfiguration(provider, trustedReferer(null, domainName));
        }
    }

    private AiProviderConfig buildConfiguration(AiInterface provider, String referer) {
        AiProviderConfig.Builder builder = AiProviderConfig.builder(provider.getApiKey())
            .connectTimeout(timeout(PROVIDER_CONNECT_TIMEOUT_SECONDS))
            .responseTimeout(timeout(PROVIDER_RESPONSE_TIMEOUT_SECONDS));

        provider.configure(builder, referer);

        return builder.build();
    }

    public boolean isConfigured(AiInterface provider) {
        return resolve(provider).isConfigured();
    }

    public String imageNamePrompt() {
        return Constants.getString(FILE_NAME_PROMPT);
    }

    public int auditMaxLength() {
        return Constants.getInt(AUDIT_MAX_LENGTH);
    }

    /**
     * Returns a non-secret cache scope that changes when the effective provider
     * configuration changes for the current WebJET domain.
     */
    public String modelCacheDiscriminator(AiInterface provider) {
        return modelCacheDiscriminator(provider, null);
    }

    public String modelCacheDiscriminator(AiInterface provider, HttpServletRequest request) {
        String providerId = provider.getProviderId();
        AiProviderConfig config = resolve(provider, request);
        String scope = CloudToolsForCore.getDomainId() + ":" + trustedReferer(request) + ":" + providerId;
        String fingerprint = fingerprint(config);
        VersionedConfiguration version = versions.compute(scope, (key, current) -> {
            if (current != null && current.fingerprint().equals(fingerprint)) return current;
            return new VersionedConfiguration(fingerprint, revisionSequence.incrementAndGet());
        });
        return CloudToolsForCore.getDomainId() + "." + version.revision();
    }

    private String trustedReferer(HttpServletRequest request) {
        return trustedReferer(request, null);
    }

    private String trustedReferer(HttpServletRequest request, String explicitDomain) {
        String currentDomain = CloudToolsForCore.getDomainName();
        String requestDomain = request == null ? null : Tools.getServerName(request, false);
        String trustedDomain = validDomain(explicitDomain)
            ? explicitDomain
            : (validDomain(currentDomain) ? currentDomain : requestDomain);

        if (request != null) {
            String incomingReferer = Tools.sanitizeHttpHeaderParam(request.getHeader("Referer"));
            String trustedIncomingReferer = sameDomainReferer(incomingReferer, currentDomain, requestDomain);
            if (trustedIncomingReferer != null) return trustedIncomingReferer;
        }

        if (validDomain(trustedDomain)) {
            String scheme = request == null ? "https" : Tools.getScheme(request);
            return scheme + "://" + trustedDomain + "/";
        }
        return "https://www.webjetcms.com/";
    }

    private String sameDomainReferer(String referer, String currentDomain, String requestDomain) {
        if (Tools.isEmpty(referer)) return null;
        try {
            URI uri = new URI(referer);
            if (("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) == false) {
                return null;
            }
            if (uri.getUserInfo() != null || validDomain(uri.getHost()) == false) return null;
            if (sameDomain(uri.getHost(), currentDomain) || sameDomain(uri.getHost(), requestDomain)) {
                int port = uri.getPort();
                if (("http".equalsIgnoreCase(uri.getScheme()) && port == 80)
                    || ("https".equalsIgnoreCase(uri.getScheme()) && port == 443)) {
                    port = -1;
                }
                return new URI(
                    uri.getScheme().toLowerCase(),
                    null,
                    uri.getHost().toLowerCase(),
                    port,
                    "/",
                    null,
                    null
                ).toASCIIString();
            }
        } catch (URISyntaxException exception) {
            return null;
        }
        return null;
    }

    private boolean sameDomain(String first, String second) {
        return validDomain(first) && validDomain(second) && first.equalsIgnoreCase(second);
    }

    private boolean validDomain(String domain) {
        if (Tools.isEmpty(domain) || "unknown".equalsIgnoreCase(domain)) return false;
        try {
            URI uri = new URI("https", null, domain, -1, "/", null, null);
            return domain.equalsIgnoreCase(uri.getHost());
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    private Duration timeout(String constantName) {
        long seconds = Math.max(0, Constants.getInt(constantName));
        long millis = Math.min(seconds * 1000L, Integer.MAX_VALUE);
        return Duration.ofMillis(millis);
    }

    private String fingerprint(AiProviderConfig config) {
        String value = String.valueOf(config.apiKey()) + '\u0000' + config.baseUri() + '\u0000'
            + config.trustedHeaders();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private record VersionedConfiguration(String fingerprint, long revision) { }
}
