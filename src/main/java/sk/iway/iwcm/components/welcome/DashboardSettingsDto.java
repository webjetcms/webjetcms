package sk.iway.iwcm.components.welcome;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * Personal dashboard layout and the options for the currently selected domain.
 * The authenticated user and the domain are always resolved on the server.
 */
@Getter
@Setter
public class DashboardSettingsDto {
    private int version = 1;
    private boolean configured;
    private boolean shortcutsConfigured;
    private boolean legacyBookmarksHandled;
    private List<Item> items = new ArrayList<>();
    private Map<String, Map<String, Object>> domainOptions = new LinkedHashMap<>();
    private String acknowledgedNewsVersion;

    /** A widget instance; its position is determined by its index in {@code items}. */
    @Getter
    @Setter
    @JsonIgnoreProperties("collapsed") // Accept old profiles without retaining the removed minimization state.
    public static class Item {
        private String id;
        private String type;
        private String size;
        private Map<String, Object> options;
    }
}
