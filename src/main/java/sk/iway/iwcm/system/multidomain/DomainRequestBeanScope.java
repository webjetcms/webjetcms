package sk.iway.iwcm.system.multidomain;

import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;

/**
 * Temporarily installs a domain-specific {@link RequestBean} for legacy code
 * that resolves domain configuration from the current request context.
 */
public final class DomainRequestBeanScope implements AutoCloseable {

    private final RequestBean previousRequestBean;
    private boolean closed;

    private DomainRequestBeanScope(String domainName) {
        previousRequestBean = SetCharacterEncodingFilter.getCurrentRequestBean();

        RequestBean domainRequestBean = new RequestBean();
        domainRequestBean.setUser(null);
        domainRequestBean.setDomain(domainName);
        SetCharacterEncodingFilter.setCurrentRequestBean(domainRequestBean);
    }

    /**
     * Opens a request context for the supplied domain, including an empty domain.
     *
     * @param domainName domain used by domain-aware configuration lookups
     * @return scope that restores the previous request context when closed
     */
    public static DomainRequestBeanScope open(String domainName) {
        return new DomainRequestBeanScope(domainName);
    }

    @Override
    public void close() {
        if (closed) return;
        SetCharacterEncodingFilter.setCurrentRequestBean(previousRequestBean);
        closed = true;
    }
}
