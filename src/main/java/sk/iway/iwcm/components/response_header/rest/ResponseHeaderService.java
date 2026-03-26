package sk.iway.iwcm.components.response_header.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.response_header.jpa.ResponseHeaderEntity;
import sk.iway.iwcm.components.response_header.jpa.ResponseHeaderRepository;

@Service
public class ResponseHeaderService {

    private static final String CACHE_KEY = "apps.response-header.headersList-";
    private static final Integer cacheInMinutes = 1440; //24 hours

    public Map<String, ResponseHeaderEntity> getResponseHeaders(String url) {
        int domainId = CloudToolsForCore.getDomainId();

        //FIRST step, check Cache for values, in specific domain
        @SuppressWarnings("unchecked")
        List<ResponseHeaderEntity> headersList = (List<ResponseHeaderEntity>)Cache.getInstance().getObject(CACHE_KEY + domainId);

        if(headersList == null) {

            //Cache is empty for this domain, lets fill it up with values from DB
            //For that we need get Repository
            //!! Bug fix, @Autowired does not work so we must get repostiry using "getSpringBean"
            RequestBean requestBean = SetCharacterEncodingFilter.getCurrentRequestBean();
            if (requestBean != null) {
                ResponseHeaderRepository rhr = requestBean.getSpringBean("responseHeaderRepository", ResponseHeaderRepository.class);

                //Get DB data
                headersList = rhr.findByDomainId(domainId);
            }
            if(headersList == null) headersList = new ArrayList<>();

            //Set new object to Cache, reasponse headers value from DB
            Cache.getInstance().setObject(CACHE_KEY + domainId, headersList, cacheInMinutes);
        }

        return  this.filterRequestHeadersByUrl(headersList, url);
    }

    private Map<String, ResponseHeaderEntity> filterRequestHeadersByUrl(List<ResponseHeaderEntity> headersInput, String url) {
        //Empty list
        if(headersInput.size() < 1) return null;

        //Prepare output map
        Map<String, ResponseHeaderEntity> headersMap = new HashMap<>();

        //Now filter headers using path
        for(ResponseHeaderEntity header : headersInput) {
            if(isPathCorrect(header.getUrl(), url)) {
                //be smart, try to find headers with best match (longest) URL address
                String key = header.getHeaderName();
                ResponseHeaderEntity current = headersMap.get(key);
                if (current == null) {
                    headersMap.put(key, header);
                } else {
                    if (current.getUrl().length()<header.getUrl().length()) {
                        headersMap.put(key, header);
                    }
                }
            }
        }

        //return filtred values
        return headersMap;
    }

    public static void deleteDomainCache() {
        int domainId = CloudToolsForCore.getDomainId();
        Cache.getInstance().removeObject(CACHE_KEY + domainId, true);
    }

    /**
     * Set HTTP headers for path
     */
    public static void setResponseHeaders(String path, HttpServletRequest request, HttpServletResponse response) {
        try {
            ResponseHeaderService pes = new ResponseHeaderService();
            Map<String, ResponseHeaderEntity> headersMap = pes.getResponseHeaders(path);
            if (headersMap!=null) {
                for(ResponseHeaderEntity header : headersMap.values()) {
                    String name = Constants.executeMacro(header.getHeaderName());
                    String value = Constants.executeMacro(header.getHeaderValue());

                    //remove new lines from header value
                    value = Tools.replace(value, "\r", " ");
                    value = Tools.replace(value, "\n", " ");

                    if (name.equalsIgnoreCase("content-language")) {
                        setContentLanguageHeader(value, true, request, response);
                    } else {
                        response.setHeader(name, value);
                    }
                }
            }
        } catch (Exception ex) {
            Logger.error(ResponseHeaderService.class, ex);
        }
    }

    /**
     * Set Content-Language header, also set response.setLocale
     * @param lngContryPair - pair eg sk-SK, cs-CZ, en-GB
     * @param forceSet - set to true to owerwrite previously set value
     * @param request
     * @param response
     */
    public static void setContentLanguageHeader(String lngContryPair, boolean forceSet, HttpServletRequest request, HttpServletResponse response) {
        if (request.getAttribute("contentLanguageHeaderSet")==null || forceSet) {
            response.setHeader("Content-Language", lngContryPair);
            response.setLocale(org.springframework.util.StringUtils.parseLocaleString(lngContryPair.replaceAll("-", "_")));
            request.setAttribute("contentLanguageHeaderSet", Boolean.TRUE);
        }
    }

    /**
     * Check if path is correct for URL, it accepts path in format
     * - /path/subpath/ - use startsWith
     * - /path/subpath/*.pdf - use startsWith for /path/subpath/ and endsWith for *.pdf
     * - /path/subpath/*.pdf,*.jpg - use startsWith for /path/subpath/ and endsWith for *.pdf or *.jpg
     * - ^/path/subpath/$ - use equals
     * @param path - path to test
     * @param url - URL address to test
     * @return
     */
    public static boolean isPathCorrect(String path, String url) {

        // check if path is null
        if (path == null || url == null) {
            return false;
        }

        // check if path is in format ^/path/subpath/$
        if (path.startsWith("^") && path.endsWith("$") && path.length() > 2) {
            return path.substring(1, path.length() - 1).equals(url);
        }
        int pos = path.indexOf("*");
        if (pos == -1) {
            // check if path is in format /path/subpath/
            return url.startsWith(path);
        } else if (pos == path.length()-1) {
            // check if path is in format /path/subpath/*
            return url.startsWith(path.substring(0, pos));
        } else {
            // check if path is in format /path/subpath/*.pdf
            String pathStart = path.substring(0, pos);
            String pathEnd = path.substring(pos);
            if (pathEnd.indexOf(",") == -1 && pathEnd.length() > 1) {
                // check if path is in format /path/subpath/*.pdf
                return url.startsWith(pathStart) && url.endsWith(pathEnd.substring(1));
            } else {
                // check if path is in format /path/subpath/*.pdf,*.jpg
                String[] pathEnds = Tools.getTokens(pathEnd, ",", true);
                for (String pathEndItem : pathEnds) {
                    if (url.startsWith(pathStart) && pathEndItem.length()>1 && url.endsWith(pathEndItem.substring(1))) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
