package sk.iway.iwcm.system.multidomain;

import org.springframework.util.ClassUtils;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.common.CloudToolsForCore;

/**
 * Resolves the effective domain ID declared by an entity class.
 */
public final class DomainIdScopeResolver {

    private DomainIdScopeResolver() {
    }

    /**
     * Resolves the effective domain ID for an entity class.
     *
     * @param entityClass entity class carrying the optional common-domain marker
     * @return common domain ID for common entities, otherwise the current domain ID
     */
    public static int resolve(Class<?> entityClass) {
        if (isCommon(entityClass)) return getCommonDomainId();
        return CloudToolsForCore.getDomainId();
    }

    /**
     * Resolves the effective domain ID for an entity class name.
     *
     * @param entityClassName fully qualified entity class name
     * @return resolved domain ID, or the current domain ID when the class cannot be resolved
     */
    public static int resolve(String entityClassName) {
        if (isCommon(entityClassName)) return getCommonDomainId();
        return CloudToolsForCore.getDomainId();
    }

    /**
     * Checks whether an entity class uses the common domain.
     *
     * @param entityClass entity class to inspect
     * @return true when the class carries the common-domain marker
     */
    public static boolean isCommon(Class<?> entityClass) {
        return entityClass != null && entityClass.isAnnotationPresent(DomainIdCommon.class);
    }

    /**
     * Checks whether an entity class name resolves to a common-domain entity.
     *
     * @param entityClassName fully qualified entity class name
     * @return true when the resolved class carries the common-domain marker
     */
    public static boolean isCommon(String entityClassName) {
        if (entityClassName == null || entityClassName.isBlank()) return false;

        try {
            return isCommon(ClassUtils.forName(entityClassName, ClassUtils.getDefaultClassLoader()));
        } catch (ClassNotFoundException | LinkageError ex) {
            return false;
        }
    }

    /**
     * Returns the configured common domain ID.
     *
     * @return configured common domain ID, or 1 when it is not configured
     */
    public static int getCommonDomainId() {
        int commonDomainId = Constants.getInt("domainIdCommon");
        return commonDomainId > 0 ? commonDomainId : 1;
    }
}
