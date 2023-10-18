package sk.iway.iwcm.components.response_header.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Service;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.response_header.jpa.ResponseHeaderEntity;
import sk.iway.iwcm.components.response_header.jpa.ResponseHeaderRepository;

@Service
public class ResponseHeaderService {

    private static final String CACHE_KEY = "apps.response-header.headersList-";
    private static final Integer cacheInMinutes = 1440; //24 hours

    public Map<String, ResponseHeaderEntity> getResponseHeaders(String path) {
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

        return  this.filterRequestHeadersByPath(headersList, path);
    }

    private Map<String, ResponseHeaderEntity> filterRequestHeadersByPath(List<ResponseHeaderEntity> headersInput, String path) {
        //Empty list
        if(headersInput.size() < 1) return null;

        //Prepare output map
        Map<String, ResponseHeaderEntity> headersMap = new HashMap<>();

        //Now filter headers using path
        for(ResponseHeaderEntity header : headersInput) {
            if(path.startsWith(header.getUrl())) {
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
    public static void setResponseHeaders(String path, HttpServletResponse response) {
        try {
            ResponseHeaderService pes = new ResponseHeaderService();
            Map<String, ResponseHeaderEntity> headersMap = pes.getResponseHeaders(path);
            if (headersMap!=null) {
                for(ResponseHeaderEntity header : headersMap.values()) {
                    response.setHeader(Constants.executeMacro(header.getHeaderName()), Constants.executeMacro(header.getHeaderValue()));
                }
            }
        } catch (Exception ex) {
            Logger.error(ResponseHeaderService.class, ex);
        }
    }
}
