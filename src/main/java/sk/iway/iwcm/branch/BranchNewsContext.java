package sk.iway.iwcm.branch;

/** Explicit template helpers for News previews; shared metadata reads cannot mutate LIVE content. */
public final class BranchNewsContext {
    private BranchNewsContext() { }

    /** Resolves core facades and narrowly scoped helpers instead of arbitrary context classes. */
    public static Class<?> resolve(String className) {
        return switch (className) {
            case "sk.iway.iwcm.Tools" -> sk.iway.iwcm.Tools.class;
            case "sk.iway.iwcm.doc.DocDB" -> sk.iway.iwcm.doc.DocDB.class;
            case "sk.iway.iwcm.doc.GroupsDB" -> sk.iway.iwcm.doc.GroupsDB.class;
            case "sk.iway.spirit.MediaDB" -> sk.iway.spirit.MediaDB.class;
            case "sk.iway.iwcm.FileTools" -> FileTools.class;
            case "sk.iway.iwcm.forum.ForumDB" -> ForumDB.class;
            default -> throw new IllegalStateException("Custom News contextClasses require a branch preview adapter: " + className);
        };
    }

    /** File-type icons are shared application assets, independent of staged file bytes. */
    public static final class FileTools {
        private FileTools() { }

        public static String getFileIcon(String url) {
            return sk.iway.iwcm.FileTools.getFileIcon(url);
        }
    }

    /** Discussion configuration remains shared; rendering only reads its current availability. */
    public static final class ForumDB {
        private ForumDB() { }

        public static boolean isActive(int docId) {
            return sk.iway.iwcm.forum.ForumDB.isActive(docId);
        }
    }
}
