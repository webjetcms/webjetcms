package sk.iway.iwcm.editor.service;

import sk.iway.iwcm.RequestBean;

/** Tracks completed deletions so cascades and selected rows process each item once per request. */
public final class DeleteRequestContext {

    private DeleteRequestContext() {
        // Utility class.
    }

    /**
     * Checks whether this request has already deleted the item, including a move to Trash.
     * @param itemType item class, separating page IDs from folder IDs
     * @param id item ID
     * @return true after a completed deletion in the current request
     */
    public static boolean isDeleted(Class<?> itemType, int id) {
        return Boolean.TRUE.equals(RequestBean.getAttribute(key(itemType, id)));
    }

    /**
     * Records a completed deletion; failed operations and approval requests must not be recorded.
     * @param itemType item class, separating page IDs from folder IDs
     * @param id item ID
     */
    public static void markDeleted(Class<?> itemType, int id) {
        RequestBean.setAttribute(key(itemType, id), Boolean.TRUE);
    }

    private static String key(Class<?> itemType, int id) {
        return DeleteRequestContext.class.getName() + "." + itemType.getName() + "." + id;
    }
}
