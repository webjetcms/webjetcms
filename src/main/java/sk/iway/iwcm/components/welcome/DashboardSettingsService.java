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

/** Validates and persists shared dashboard layouts and domain-specific options, preserving shortcuts during widget resets. */
@Service
public class DashboardSettingsService {
    static final int MAX_INSTANCES = 48;
    static final int MAX_RECORD_LENGTH = 2000;
    private static final Pattern INSTANCE_ID = Pattern.compile("[A-Za-z0-9_-]{1,36}");
    private static final Set<String> SINGLETONS = Set.of("sessions", "news", "search");
    private static final Map<String, Set<String>> SIZES = Map.ofEntries(
        Map.entry("shortcut", Set.of("1x1")),
        Map.entry("recent-pages", Set.of("2x3", "3x2", "3x3")),
        Map.entry("approvals", Set.of("1x1", "3x3")),
        Map.entry("publishing", Set.of("2x2", "2x3")),
        Map.entry("forms", Set.of("1x1", "3x3")),
        Map.entry("traffic", Set.of("1x1", "3x3")),
        Map.entry("top-pages", Set.of("2x3", "3x3")),
        Map.entry("search-terms", Set.of("2x3", "3x3")),
        Map.entry("referrers", Set.of("2x2", "2x3", "3x3")),
        Map.entry("newsletter", Set.of("2x2", "3x3")),
        Map.entry("errors", Set.of("1x1", "3x3")),
        Map.entry("sessions", Set.of("2x3")),
        Map.entry("news", Set.of("3x2")),
        Map.entry("search", Set.of("fullauto")),
        Map.entry("changed-pages", Set.of("3x2", "3x3")),
        Map.entry("audit", Set.of("3x2", "3x3")),
        Map.entry("logged-admins", Set.of("2x2", "2x3")),
        Map.entry("server-memory", Set.of("3x2", "3x3")),
        Map.entry("server-cpu", Set.of("3x2", "3x3"))
    );

    private final DashboardSettingsRepository repository;
    private final ObjectMapper mapper = new ObjectMapper();

    public DashboardSettingsService(DashboardSettingsRepository repository) {
        this.repository = repository;
    }

    /**
     * Reads the shared layout with only the requested domain's options.
     *
     * @param userId ID of the account that owns the settings
     * @param domainKey decimal root group ID identifying the active domain
     * @return stored settings, or an unconfigured DTO when the stored layout is absent or invalid
     * @throws IllegalStateException if the settings cannot be read from the database
     */
    public DashboardSettingsDto load(int userId, String domainKey) {
        return readSettings(repository.read(userId), domainKey);
    }

    /**
     * Reconstructs and validates a layout from its metadata, widget records and selected domain options.
     *
     * @param records stored dashboard records keyed by their administration settings keys
     * @param domainKey decimal root group ID selecting the domain options to include
     * @return reconstructed settings, or an unconfigured DTO for missing, unsupported or invalid layout data
     */
    private DashboardSettingsDto readSettings(Map<String, String> records, String domainKey) {
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
            settings.setConfigured(metadata.path("configured").asBoolean(true));
            settings.setShortcutsConfigured(metadata.path("shortcutsConfigured").asBoolean(true));
            settings.setLegacyBookmarksHandled(metadata.path("legacyBookmarksHandled").asBoolean(false));
            return settings;
        } catch (JsonProcessingException | IllegalArgumentException exception) {
            // Invalid legacy or manually edited preferences must not prevent login.
            return new DashboardSettingsDto();
        }
    }

    /**
     * Validates and replaces the shared layout and current-domain options in one transaction.
     * The layout must include the session management widget.
     *
     * @param userId ID of the account that owns the settings
     * @param domainKey decimal root group ID identifying the active domain
     * @param settings settings to validate and persist; missing widget options are initialized in place
     * @return the supplied settings with the layout and shortcuts marked as configured
     * @throws IllegalArgumentException if the settings are invalid or the session management widget is missing
     * @throws IllegalStateException if the settings cannot be persisted
     */
    public DashboardSettingsDto save(int userId, String domainKey, DashboardSettingsDto settings) {
        Map<String, String> records = validateAndSerialize(settings, domainKey);
        require(settings.getItems().stream().anyMatch(item -> "sessions".equals(item.getType())), "The session management widget is required");
        Set<String> ids = new HashSet<>();
        settings.getItems().forEach(item -> ids.add(item.getId()));
        repository.replace(userId, domainKey, records, ids);
        settings.setConfigured(true);
        settings.setShortcutsConfigured(true);
        return settings;
    }

    /**
     * Clears widget preferences, all domain options and the news acknowledgement while retaining shortcuts.
     * Shortcut configuration and migration flags are preserved; the layout is marked as unconfigured
     * so the client can supply default widgets.
     *
     * @param userId ID of the account whose widget preferences are reset
     * @return retained shortcuts and configuration flags after the reset
     * @throws IllegalStateException if the reset cannot be persisted
     */
    public DashboardSettingsDto reset(int userId) {
        return reset(userId, "0", null);
    }

    /**
     * Replaces widget preferences atomically while preserving shortcuts read under the account lock.
     * Existing domain options and the news acknowledgement are cleared. A supplied layout provides
     * replacement widgets and current-domain options; its shortcuts are included only when the account's
     * shortcuts have not been configured. The legacy bookmark migration flag is preserved.
     *
     * @param userId ID of the account whose widget preferences are reset
     * @param domainKey decimal root group ID identifying the active domain
     * @param layout replacement layout, or {@code null} to leave widgets unconfigured for client defaults
     * @return persisted settings containing retained shortcuts and any supplied replacement widgets
     * @throws IllegalArgumentException if the supplied or combined layout is invalid
     * @throws IllegalStateException if the reset cannot be persisted
     */
    public DashboardSettingsDto reset(int userId, String domainKey, DashboardSettingsDto layout) {
        if (layout != null) {
            validateAndSerialize(layout, domainKey);
            require(layout.getItems().stream().anyMatch(item -> "sessions".equals(item.getType())), "The session management widget is required");
        }
        Map<String, String> retained = repository.reset(userId, previous -> {
            DashboardSettingsDto settings = readSettings(previous, domainKey);
            settings.getItems().removeIf(item -> !"shortcut".equals(item.getType()));
            settings.getDomainOptions().clear();
            settings.setAcknowledgedNewsVersion(null);
            if (layout != null) {
                for (Item item : layout.getItems()) {
                    if ("shortcut".equals(item.getType()) && settings.isShortcutsConfigured()) continue;
                    settings.getItems().add(item);
                    if (layout.getDomainOptions().containsKey(item.getId())) settings.getDomainOptions().put(item.getId(), layout.getDomainOptions().get(item.getId()));
                }
                settings.setShortcutsConfigured(true);
            }
            Map<String, String> records = validateAndSerialize(settings, domainKey);
            ObjectNode metadata = mapper.createObjectNode().put("version", 1).put("configured", layout != null)
                .put("shortcutsConfigured", settings.isShortcutsConfigured())
                .put("legacyBookmarksHandled", settings.isLegacyBookmarksHandled());
            ArrayNode order = metadata.putArray("order");
            settings.getItems().forEach(item -> order.add(item.getId()));
            records.put(DashboardSettingsRepository.LAYOUT_KEY, serializeBounded(metadata));
            return records;
        });
        return readSettings(retained, domainKey);
    }

    /**
     * Validates the settings structure and serializes it into records within the storage size limit.
     * Missing widget option maps are initialized in place. This validation permits layouts without
     * session management so that stored reset states can be read; save operations enforce its presence.
     *
     * @param settings settings to validate and serialize
     * @param domainKey decimal root group ID used to namespace domain option records
     * @return JSON records for the shared layout, widgets, current-domain options and any news acknowledgement
     * @throws IllegalArgumentException if the settings or domain key are invalid, serialization fails or a record is too large
     */
    Map<String, String> validateAndSerialize(DashboardSettingsDto settings, String domainKey) {
        require(settings != null && settings.getVersion() == 1, "Unsupported dashboard settings version");
        require(settings.getItems() != null && settings.getItems().size() <= MAX_INSTANCES, "A dashboard supports at most 48 widgets");
        require(settings.getDomainOptions() != null, "Domain options must be an object");
        require(settings.getAcknowledgedNewsVersion() == null || settings.getAcknowledgedNewsVersion().length() <= 80, "News version is too long");
        require(domainKey != null && domainKey.matches("[0-9]+"), "Invalid dashboard domain");

        Set<String> ids = new HashSet<>();
        Set<String> singletonTypes = new HashSet<>();
        Map<String, String> records = new LinkedHashMap<>();
        ObjectNode metadata = mapper.createObjectNode().put("version", 1)
            .put("shortcutsConfigured", true).put("legacyBookmarksHandled", settings.isLegacyBookmarksHandled());
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

    /**
     * Validates shortcut fields and restricts destinations according to the shortcut source.
     * Custom shortcuts require a title and an HTTP(S) or root-relative URL; menu shortcuts may have
     * an empty destination while awaiting selection, otherwise they require a root-relative path.
     *
     * @param options non-null shortcut options; an omitted source defaults to {@code menu}
     * @throws IllegalArgumentException if any shortcut field or destination is invalid
     */
    static void validateShortcut(Map<String, Object> options) {
        Object source = options.getOrDefault("source", "menu");
        require("menu".equals(source) || "url".equals(source), "Invalid shortcut source");
        Object href = options.getOrDefault("href", "");
        Object title = options.getOrDefault("title", "");
        require(href instanceof String && ((String) href).length() <= 1024, "Invalid shortcut URL");
        require(title instanceof String && ((String) title).length() <= 120, "Invalid shortcut title");
        Object icon = options.getOrDefault("icon", "");
        require(icon instanceof String && ((String) icon).length() <= 80
            && (((String) icon).isEmpty() || ((String) icon).matches("ti-[a-z0-9]+(?:-[a-z0-9]+)*")), "Invalid shortcut icon");
        Object color = options.getOrDefault("color", "default");
        require(color instanceof String && (Set.of("default", "mint", "lavender", "blue", "amber", "peach", "rose", "cyan", "gray", "red").contains(color)
            || ((String) color).matches("#[a-fA-F0-9]{6}(?:[a-fA-F0-9]{2})?")), "Invalid shortcut color");
        String target = (String) href;
        if ("url".equals(source)) {
            require(!((String) title).isBlank(), "A custom shortcut requires a title");
            require(isSafeShortcutUrl(target), "Invalid shortcut URL");
        } else {
            // Empty menu shortcuts are allowed while the user chooses an authorized destination.
            require(target.isEmpty() || target.startsWith("/") && isSafeShortcutUrl(target), "Invalid shortcut URL");
        }
    }

    /**
     * Checks the syntax of a root-relative path or an absolute HTTP(S) URL without credentials.
     * Protocol-relative URLs, backslashes, control characters and surrounding whitespace are rejected.
     * This check does not verify the user's permission to access the destination.
     *
     * @param value candidate shortcut destination, or {@code null}
     * @return {@code true} if the destination has an accepted URL form
     */
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

    /**
     * Serializes a value as JSON and enforces the per-record character limit.
     *
     * @param value value to serialize into an administration settings record
     * @return JSON representation within the allowed record length
     * @throws IllegalArgumentException if serialization fails or the JSON exceeds the record length limit
     */
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
