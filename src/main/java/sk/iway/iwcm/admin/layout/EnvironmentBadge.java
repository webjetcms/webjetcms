package sk.iway.iwcm.admin.layout;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;
import sk.iway.iwcm.Constants;

/** Shared, request-specific environment identity for the administration and login page. */
@Getter
public class EnvironmentBadge {
    private static final Pattern IDENTITY = Pattern.compile("^(PROD|UAT|CIT|INT|TEST|DEMO|LOCAL|DEV)(?=[/\\s-]|$)", Pattern.CASE_INSENSITIVE);
    private static final Map<String, Palette> PALETTES = Map.of(
        "PROD", new Palette("#FFD6D7", "#790011", "#C4001F", "#FFFFFF", "ti-alert-triangle"),
        "TEST", new Palette("#FFE0CC", "#7A2E00", "#C24E00", "#FFFFFF", "ti-test-pipe"),
        "DEMO", new Palette("#FFF0B3", "#5C4300", "#F6BE3F", "#13151B", "ti-eye"),
        "DEV", new Palette("#CFF5E4", "#00533D", "#007E69", "#FFFFFF", "ti-code"),
        "UAT", new Palette("#C8F0F4", "#004F59", "#00717F", "#FFFFFF", ""),
        "INT", new Palette("#DCE4FF", "#0037A6", "#0049BE", "#FFFFFF", "ti-plug-connected"),
        "CIT", new Palette("#EADFFF", "#5200A3", "#6E00DC", "#FFFFFF", "ti-user-check"),
        "LOCAL", new Palette("#E6E8EE", "#353944", "#353944", "#FFFFFF", "ti-device-laptop")
    );

    private final String name;
    private final String description;
    private final String icon;
    private final String background;
    private final String foreground;

    /** Reads the existing dashboard configuration, also used by the shared v9 layout. */
    public EnvironmentBadge() {
        String fullName = Constants.getStringExecuteMacro("dashboardEnvironmentName").trim().replaceAll("/+$", "").trim();
        String upperName = fullName.toUpperCase(Locale.ROOT);
        name = upperName.substring(0, upperName.offsetByCodePoints(0, Math.min(8, upperName.codePointCount(0, upperName.length()))));
        String configuredDescription = Constants.getStringExecuteMacro("dashboardEnvironmentDescription").trim();
        description = configuredDescription.isEmpty() ? fullName : fullName + " – " + configuredDescription;

        Matcher identity = IDENTITY.matcher(fullName);
        String type = identity.find() ? identity.group(1).toUpperCase(Locale.ROOT) : Constants.getEnvironmentName();
        Palette palette = PALETTES.getOrDefault(type, PALETTES.get("DEV"));
        String style = Constants.getString("dashboardEnvironmentStyle", "auto");
        boolean strong = "strong".equalsIgnoreCase(style) || ("auto".equalsIgnoreCase(style) && "PROD".equals(type));
        String color = Constants.getString("dashboardEnvironmentColor").trim();
        if (color.matches("(?i)#[a-f0-9]{3}(?:[a-f0-9]{3})?")) {
            background = color;
            foreground = contrastColor(color);
        } else {
            background = strong ? palette.strongBackground() : palette.background();
            foreground = strong ? palette.strongForeground() : palette.foreground();
        }

        String configuredIcon = Constants.getString("dashboardEnvironmentIcon").trim();
        if (configuredIcon.isEmpty() || "none".equalsIgnoreCase(configuredIcon)) icon = "";
        else if ("auto".equalsIgnoreCase(configuredIcon)) icon = palette.icon();
        else icon = normalizeIcon(configuredIcon);
    }

    public boolean isVisible() {
        return !name.isEmpty();
    }

    /** Returns CSS containing only validated hexadecimal colors. */
    public String getCssStyle() {
        return "--wj-environment-bg:" + background + ";--wj-environment-text:" + foreground;
    }

    private static String normalizeIcon(String value) {
        return value.startsWith("ti-") ? value : "ti-" + value;
    }

    /** Chooses the higher WCAG contrast of black and white (at least 4.5:1 for any opaque RGB). */
    private static String contrastColor(String color) {
        String hex = color.substring(1);
        if (hex.length() == 3) hex = hex.replaceAll("(.)", "$1$1");
        double luminance = 0;
        double[] weights = { .2126, .7152, .0722 };
        for (int i = 0; i < 3; i++) {
            double channel = Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16) / 255.0;
            luminance += weights[i] * (channel <= .04045 ? channel / 12.92 : Math.pow((channel + .055) / 1.055, 2.4));
        }
        return (luminance + .05) / .05 >= 1.05 / (luminance + .05) ? "#000000" : "#FFFFFF";
    }

    private record Palette(String background, String foreground, String strongBackground, String strongForeground, String icon) { }
}
