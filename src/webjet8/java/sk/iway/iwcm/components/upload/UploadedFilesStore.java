package sk.iway.iwcm.components.upload;

import sk.iway.iwcm.PathFilter;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * UploadedFilesStore is a utility class that manages the storage of uploaded files in the HTTP session.
 * It provides methods to retrieve, store, remove, and check the existence of uploaded files.
 * The files are stored in a map where each file is associated with a unique key.
 */
class UploadedFilesStore {
    private static final String SESSION_MAP_KEY = "xhrUpload_filesMap";

    /** Retrieves the map of uploaded files from the current HTTP session.
     * If the map does not exist, it creates a new one and stores it in the session.
     *
     * @return a map that holds PathHolder objects associated with their keys
     */
    private static Map<String, PathHolder> getFilesMap() {
        HttpSession session = PathFilter.getRequest().getSession(true);
        Map<String, PathHolder> map = (Map<String, PathHolder>) session.getAttribute(SESSION_MAP_KEY);
        if (map == null) {
            map = new ConcurrentHashMap<>();
            session.setAttribute(SESSION_MAP_KEY, map);
        }
        return map;
    }

    /**
     * Sets the session map with the provided map.
     * This method is used to replace the existing session map.
     * Need to be called after map is modified to ensure the session is updated (in case of using session sharing in redis).
     *
     * @param map the new map to set in the session
     */
    private static void setFilesMap(Map<String, PathHolder> map) {
        HttpSession session = PathFilter.getRequest().getSession(true);
        session.setAttribute(SESSION_MAP_KEY, map);
    }

    /** Retrieves a PathHolder object associated with the given key from the session map.
     * If the key does not exist, it returns null.
     *
     * @param key the key associated with the PathHolder
     * @return the PathHolder object if found, otherwise null
     */
    public static PathHolder get(String key) {
        return getFilesMap().get(key);
    }

    /** Stores a PathHolder object in the session map with the specified key.
     * If the key already exists, it updates the existing entry.
     *
     * @param key   the key to associate with the PathHolder
     * @param value the PathHolder object to store
     */
    public static void put(String key, PathHolder value) {
        getFilesMap().put(key, value);
        setFilesMap(getFilesMap());
    }

    /** Removes the PathHolder object associated with the specified key from the session map.
     * If the key does not exist, no action is taken.
     *
     * @param key the key of the PathHolder to remove
     */
    public static void remove(String key) {
        getFilesMap().remove(key);
        setFilesMap(getFilesMap());
    }

    /** Retrieves the entire map of PathHolder objects from the session.
     * This method is useful for accessing all uploaded files at once.
     *
     * @return a map containing all PathHolder objects
     */
    public static Map<String, PathHolder> getMap() {
        return getFilesMap();
    }

    /** Checks if the session map contains a PathHolder object associated with the specified key.
     * This method is useful for verifying if a file has been uploaded with a specific key.
     *
     * @param key the key to check in the session map
     * @return true if the key exists in the map, false otherwise
     */
    public static boolean containsKey(String key) {
        return getFilesMap().containsKey(key);
    }

    /** Clears the session map of all PathHolder objects.
     * This method is typically used to reset the upload state, removing all uploaded files from the session.
     */
    public static void clear() {
        getFilesMap().clear();
        setFilesMap(getFilesMap());
    }
}
