package sk.iway.iwcm.system.datatables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.spirit.MediaDB;
import sk.iway.spirit.model.Media;
import sk.iway.spirit.model.MediaGroupBean;

/**
 *  DataTablesMediaController.java
 **
 *  Title        webjet8
 *  Company      Interway a.s. (www.interway.sk)
 *  Copyright    Interway a.s (c) 2001-2018
 *  author       $Author: lzlatohlavek $
 *  created      Date: 28.05.2018
 */
@RestController
@PreAuthorize("@WebjetSecurityService.hasPermission('menuWebpages')")
@RequestMapping("/admin/rest/datatables/media/")
public class DataTablesMediaController {
    /**
     * Vráti media skupiny média
     * @return
     */
    @RequestMapping(path = "/groups_list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, Object>> getGroups(@RequestParam(value = "mediaId") int mediaIdParam, @RequestParam(value = "docId") int docIdParam) throws JSONException {

        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();

        List<Map<String, Object>> result = new ArrayList<>();

        if (rb != null && rb.isUserAdmin())
        {


            int mediaId = Tools.getIntValue(mediaIdParam, -1);
            int docId = Tools.getIntValue(docIdParam, -1);

            Media medium = MediaDB.getMedia(mediaId);
            List<MediaGroupBean> allGroups = new ArrayList<>();


            DocDetails doc = DocDB.getInstance().getDoc(docId);
            int groupId = -1;
            if (doc != null)
            {
                groupId = doc.getGroupId();
            }

            Map<String, Object> map = new HashMap<>();
            map.put("label", " ");
            map.put("value", "");
            result.add(map);


            allGroups = MediaDB.getGroups(groupId);

            if (medium != null)
            {
                medium.getGroups().forEach(group ->
                {
                    Map<String, Object> g = new HashMap<>();
                    g.put("label", group.getMediaGroupName());
                    g.put("value", group.getMediaGroupId());
                    g.put("selected", "true");

                    result.add(g);
                });

            }

            allGroups.forEach(group ->
            {
                Map<String, Object> g = new HashMap<>();
                g.put("label", group.getMediaGroupName());
                g.put("value", group.getMediaGroupId());
                result.add(g);
            });
        }
        return result;
    }

    /**
     * Vráti vsetky media skupiny
     * @return
     */
    @RequestMapping(path = "/groups_all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getGroups() throws JSONException {

        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();

        Map<String, Object> result = new HashMap<>();
        if (rb!=null && rb.isUserAdmin())
        {
            List<Map<String, Object>> list = new ArrayList<>();
            List<MediaGroupBean> allGroups = MediaDB.getGroups();
            allGroups.forEach(group ->
            {
                Map<String, Object> map = new HashMap<>();

                map.put("name", group.getMediaGroupName());
                map.put("id", group.getMediaGroupId());
                    String groups = group.getAvailableGroups() == null ? " " : group.getAvailableGroups();
                map.put("groups", groups);

                list.add(map);
            });
            result.put("data", list);
        }
        return result;
    }

}



