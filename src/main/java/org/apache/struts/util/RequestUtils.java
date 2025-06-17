package org.apache.struts.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.BeanWrapperImpl;

/**
 * Wrapper for backward compatibility with Struts 1.x.
 */
public class RequestUtils {

    /**
     * Populate the specified bean with the values from the request parameters.
     * This method is deprecated and should not be used in new applications.
     * Instead, consider using Spring's `BeanWrapper` or similar utilities.
     * @param bean
     * @param request
     * @deprecated Use Spring's `BeanWrapper` or similar utilities instead.
     */
    @Deprecated
    public static void populate(Object bean, HttpServletRequest request) {
        BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);
		wrapper.setPropertyValues(request.getParameterMap());
    }

}
