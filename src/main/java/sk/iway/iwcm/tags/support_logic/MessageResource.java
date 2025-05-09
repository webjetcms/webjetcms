package sk.iway.iwcm.tags.support_logic;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class MessageResource {

    private MessageResource() {
        // Private constructor to prevent instantiation
    }

    public static Map<String, String> getAllKeyMap() {
        String filePath = "src/main/java/sk/iway/iwcm/tags/support_logic/LocalStrings.properties";
        Properties properties = new Properties();
        Map<String, String> keyValueMap = new HashMap<>();

        try (FileInputStream input = new FileInputStream(filePath)) {
            properties.load(input);

            Set<String> keys = properties.stringPropertyNames();
            for (String key : keys) {
                keyValueMap.put(key, properties.getProperty(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return keyValueMap;
    }
}