/**
 * patchnute podla:
 * https://github.com/bingcai/struts-mini/commit/df4da55bc2b0c3c1f4687a61c49458dfbde0e3c3
 *
 * CVE-2015-0899
 * The MultiPageValidator implementation in Apache Struts 1 1.1 through 1.3.10 allows remote attackers to bypass
 * intended access restrictions via a modified page parameter.
 *
 */

/*
 * $Id$
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.struts.util;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.upload.FormFile;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * <p>General purpose utility methods related to processing a servlet request
 * in the Struts controller framework.</p>
 *
 * @version $Rev$ $Date$
 */
public class RequestUtils {


    /**
     * <p>Populate the properties of the specified JavaBean from the specified
     * HTTP request, based on matching each parameter name against the
     * corresponding JavaBeans "property setter" methods in the bean's class.
     * Suitable conversion is done for argument types as described under
     * <code>convert()</code>.</p>
     *
     * @param bean    The JavaBean whose properties are to be set
     * @param request The HTTP request whose parameters are to be used to
     *                populate bean properties
     * @throws ServletException if an exception is thrown while setting
     *                          property values
     */
    public static void populate(Object bean, HttpServletRequest request)
        throws ServletException {
        populate(bean, null, null, request);
    }

    /**
     * <p>Populate the properties of the specified JavaBean from the specified
     * HTTP request, based on matching each parameter name (plus an optional
     * prefix and/or suffix) against the corresponding JavaBeans "property
     * setter" methods in the bean's class. Suitable conversion is done for
     * argument types as described under <code>setProperties</code>.</p>
     *
     * <p>If you specify a non-null <code>prefix</code> and a non-null
     * <code>suffix</code>, the parameter name must match
     * <strong>both</strong> conditions for its value(s) to be used in
     * populating bean properties. If the request's content type is
     * "multipart/form-data" and the method is "POST", the
     * <code>HttpServletRequest</code> object will be wrapped in a
     * <code>MultipartRequestWrapper</code object.</p>
     *
     * @param bean    The JavaBean whose properties are to be set
     * @param prefix  The prefix (if any) to be prepend to bean property names
     *                when looking for matching parameters
     * @param suffix  The suffix (if any) to be appended to bean property
     *                names when looking for matching parameters
     * @param request The HTTP request whose parameters are to be used to
     *                populate bean properties
     * @throws ServletException if an exception is thrown while setting
     *                          property values
     */
    public static void populate(Object bean, String prefix, String suffix,
        HttpServletRequest request)
        throws ServletException {
        // Build a list of relevant request parameters from this request
        HashMap properties = new HashMap();

        // Iterator of parameter names
        Enumeration names = null;

        // Map for multipart parameters
        Map multipartParameters = null;

        String contentType = request.getContentType();
        String method = request.getMethod();

            names = request.getParameterNames();

        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();
            String stripped = name;

            if (prefix != null) {
                if (stripped.startsWith(prefix)) {
                    /*
                        continue;
                    } */

                    stripped = stripped.substring(prefix.length());
                }

                stripped = stripped.substring(prefix.length());
            }

            if (suffix != null) {
                if (stripped.endsWith(suffix)) {
                    /*
                        continue;
                    } */
                    stripped =
                        stripped.substring(0, stripped.length() - suffix.length());
                }
            }

            /* Fix the issue in defect S1207965.
            */
            if (!stripped.startsWith("class.")) {
                    /* In defect S1207965, we fixed the same issue for struts 1.3.10.
                    here the fixing is for struts 1.3.8
                    */

                Object parameterValue = null;

                parameterValue = request.getParameterValues(name);

                // Populate parameters, except "standard" struts attributes
                // such as 'org.apache.struts.action.CANCEL'
                if (!(stripped.startsWith("org.apache.struts."))) {
                    properties.put(stripped, parameterValue);
                }
            }
        }

        // Set the corresponding properties of our bean
        try {
            BeanUtils.populate(bean, properties);
        } catch (Exception e) {
            throw new ServletException("BeanUtils.populate", e);
        } finally {

        }
    }


}
