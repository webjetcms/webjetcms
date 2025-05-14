package sk.iway.iwcm.tags.support_logic;

public class RequestUtils {

    public static Class applicationClass(String className) throws ClassNotFoundException {
        return applicationClass(className, (ClassLoader)null);
    }

    public static Class applicationClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = RequestUtils.class.getClassLoader();
            }
        }

        return classLoader.loadClass(className);
    }
}