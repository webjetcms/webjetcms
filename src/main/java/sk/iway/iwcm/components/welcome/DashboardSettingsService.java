package sk.iway.iwcm.components.welcome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import sk.iway.iwcm.components.welcome.DashboardSettingsDto.Item;

/** Validates the small dashboard-specific settings contract before any database writes. */
@Service
public class DashboardSettingsService {
    static final int MAX_INSTANCES = 32;
    static final int MAX_RECORD_LENGTH = 2000;
    private static final Pattern INSTANCE_ID = Pattern.compile("[A-Za-z0-9_-]{1,36}");
    private static final Set<String> SINGLETONS = Set.of("recent-pages", "approvals", "publishing", "sessions", "news", "search");
    private static final Map<String, Set<String>> SIZES = Map.ofEntries(
        Map.entry("shortcut", Set.of("1x1")),
        Map.entry("recent-pages", Set.of("2x3", "3x3")),
        Map.entry("approvals", Set.of("1x1", "3x3")),
        Map.entry("publishing", Set.of("2x3")),
        Map.entry("forms", Set.of("1x1", "3x3")),
        Map.entry("traffic", Set.of("1x1", "3x3")),
        Map.entry("top-pages", Set.of("2x3", "3x3")),
        Map.entry("search-terms", Set.of("2x3")),
        Map.entry("referrers", Set.of("2x3", "3x3")),
        Map.entry("newsletter", Set.of("2x2", "3x3")),
        Map.entry("errors", Set.of("1x1", "3x3")),
        Map.entry("sessions", Set.of("2x3")),
        Map.entry("news", Set.of("3x2")),
        Map.entry("search", Set.of("fullauto"))
    );

    private final DashboardSettingsRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public DashboardSettingsService(DashboardSettingsRepository repository) {
        this.repository = repository;
    }

    /** Returns a fresh shared layout with only the requested domain's options. */
    public DashboardSettingsDto load(int userId, String domainKey) {
        Map<String, String> records = repository.read(userId);
        String layout = records.get(DashboardSettingsRepository.LAYOUT_KEY);
        if (layout == null) return new DashboardSettingsDto();
        try {
            JsonNode metadata = mapper.readTree(layout);
            if (metadata == null || metadata.path("version").asInt() != 1 || !metadata.path("order").isArray()) return new DashboardSettingsDto();
            DashboardSettingsDto settings = new DashboardSettingsDto();
            List<Item> items = new ArrayList<>();
            for (JsonNode id : metadata.get("order")) {
                String itemJson = records.get(DashboardSettingsRepository.WIDGET_PREFIX + id.asText());
                if (itemJson == null) return new DashboardSettingsDto();
                Item item = mapper.readValue(itemJson, Item.class);
                if (item == null || !id.asText().equals(item.getId())) return new DashboardSettingsDto();
                items.add(item);
                String options = records.get(domainPrefix(domainKey) + item.getId());
                if (options != null) settings.getDomainOptions().put(item.getId(), mapper.readValue(options, new TypeReference<Map<String, Object>>() {}));
            }
            settings.setItems(items);
            String newsJson = records.get(DashboardSettingsRepository.NEWS_KEY);
            if (newsJson != null) {
                JsonNode news = mapper.readTree(newsJson);
                if (news != null && news.path("version").isTextual()) settings.setAcknowledgedNewsVersion(news.get("version").asText());
            }
            validateAndSerialize(settings, domainKey);
            settings.setConfigured(true);
            return settings;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            // Invalid legacy or manually edited preferences must not prevent login.
            return new DashboardSettingsDto();
        }
    }

    /** Validates the entire request before replacing any stored records. */
    public DashboardSettingsDto save(int userId, String domainKey, DashboardSettingsDto settings) {
        Map<String, String> records = validateAndSerialize(settings, domainKey);
        require(settings.getItems().stream().anyMatch(item -> "sessions".equals(item.getType())), "The session management widget is required");
        Set<String> ids = new HashSet<>();
        settings.getItems().forEach(item -> ids.add(item.getId()));
        repository.replace(userId, domainKey, records, ids);
        settings.setConfigured(true);
        return settings;
    }

    /** Returns an unconfigured profile only after all dashboard preferences have been removed. */
    public DashboardSettingsDto reset(int userId) {
        repository.reset(userId);
        return new DashboardSettingsDto();
    }

    Map<String, String> validateAndSerialize(DashboardSettingsDto settings, String domainKey) {
        require(settings != null && settings.getVersion() == 1, "Unsupported dashboard settings version");
        require(settings.getItems() != null && settings.getItems().size() <= MAX_INSTANCES, "A dashboard supports at most 32 widgets");
        require(settings.getDomainOptions() != null, "Domain options must be an object");
        require(settings.getAcknowledgedNewsVersion() == null || settings.getAcknowledgedNewsVersion().length() <= 80, "News version is too long");
        require(domainKey != null && domainKey.matches("[0-9]+"), "Invalid dashboard domain");

        Set<String> ids = new HashSet<>();
        Set<String> singletonTypes = new HashSet<>();
        Map<String, String> records = new LinkedHashMap<>();
        ObjectNode metadata = mapper.createObjectNode().put("version", 1);
        ArrayNode order = metadata.putArray("order");
        if (settings.getAcknowledgedNewsVersion() != null) records.put(DashboardSettingsRepository.NEWS_KEY,
            serializeBounded(mapper.createObjectNode().put("version", settings.getAcknowledgedNewsVersion())));

        for (Item item : settings.getItems()) {
            require(item != null && item.getId() != null && INSTANCE_ID.matcher(item.getId()).matches(), "Invalid widget instance ID");
            require(ids.add(item.getId()), "Widget instance IDs must be unique");
            Set<String> sizes = item.getType() == null ? null : SIZES.get(item.getType());
            require(sizes != null && item.getSize() != null && sizes.contains(item.getSize()), "Unknown widget type or unsupported size");
            if (SINGLETONS.contains(item.getType())) require(singletonTypes.add(item.getType()), "This widget may only appear once");
            if (item.getOptions() == null) item.setOptions(new LinkedHashMap<>());
            if ("shortcut".equals(item.getType())) validateShortcut(item.getOptions());
            records.put(DashboardSettingsRepository.WIDGET_PREFIX + item.getId(), serializeBounded(item));
            order.add(item.getId());
        }
        for (Map.Entry<String, Map<String, Object>> entry : settings.getDomainOptions().entrySet()) {
            require(ids.contains(entry.getKey()) && entry.getValue() != null, "Domain options must reference an existing widget instance");
            records.put(domainPrefix(domainKey) + entry.getKey(), serializeBounded(entry.getValue()));
        }
        records.put(DashboardSettingsRepository.LAYOUT_KEY, serializeBounded(metadata));
        return records;
    }

    /** Custom shortcuts accept only explicit HTTP(S) URLs or root-relative application paths. */
    static void validateShortcut(Map<String, Object> options) {
        Object source = options.getOrDefault("source", "menu");
        require("menu".equals(source) || "url".equals(source), "Invalid shortcut source");
        Object href = options.getOrDefault("href", "");
        Object title = options.getOrDefault("title", "");
        require(href instanceof String && ((String) href).length() <= 1024, "Invalid shortcut URL");
        require(title instanceof String && ((String) title).length() <= 120, "Invalid shortcut title");
        String target = (String) href;
        if ("url".equals(source)) {
            require(!((String) title).isBlank(), "A custom shortcut requires a title");
            require(isSafeShortcutUrl(target), "Invalid shortcut URL");
        } else {
            // Empty menu shortcuts are allowed while the user chooses an authorized destination.
            require(target.isEmpty() || target.startsWith("/") && isSafeShortcutUrl(target), "Invalid shortcut URL");
        }
    }

    static boolean isSafeShortcutUrl(String value) {
        if (value == null || value.isBlank() || !value.equals(value.trim()) || value.startsWith("//")
                || value.indexOf('\\') >= 0 || value.chars().anyMatch(Character::isISOControl)) return false;
        try {
            java.net.URI uri = new java.net.URI(value);
            if (value.startsWith("/")) return uri.getScheme() == null && uri.getRawAuthority() == null;
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null && uri.getRawUserInfo() == null;
        } catch (java.net.URISyntaxException exception) {
            return false;
        }
    }

    private String serializeBounded(Object value) {
        try {
            String json = mapper.writeValueAsString(value);
            require(json.length() <= MAX_RECORD_LENGTH, "A dashboard settings record cannot exceed 2000 characters");
            return json;
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid dashboard settings JSON", exception);
        }
    }

    private static String domainPrefix(String domainKey) {
        return DashboardSettingsRepository.DOMAIN_PREFIX + domainKey + ".";
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalArgumentException(message);
    }
}
