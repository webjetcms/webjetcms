package sk.iway.spirit;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.json.JSONException;

import sk.iway.iwcm.DB;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.SetCharacterEncodingFilter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.system.datatables.DataTablesFieldError;
import sk.iway.iwcm.system.datatables.DataTablesInterface;
import sk.iway.spirit.model.Media;
import sk.iway.spirit.model.MediaGroupBean;

/**
 *  MediaDataController.java
 *
 *  Class EnumerationDataController is used for working with DataTables
 *
 *
 *@author       $Author: lzl $
 *@version      $Revision: 1.0 $
 */

public class MediaDataController implements DataTablesInterface {

    @SuppressWarnings("unused")
    private class MediumForReturn implements Serializable{
        private static final long serialVersionUID = 1L;

        private int id;
        private int order;
        private String title;
        private String thumbLink;
        private String group;
        private String groupsArray = "";
        private String link_url = "";
        private String link_exist = "";

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getThumbLink() {
            return thumbLink;
        }

        public void setThumbLink(String thumbLink) {
            this.thumbLink = thumbLink;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public String getGroupsArray() {
            return groupsArray;
        }

        public void setGroupsArray(String groupsArray) {
            this.groupsArray = groupsArray;
        }

        public String getLink_url() {
            return link_url;
        }

        public void setLink_url(String link_url) {
            this.link_url = link_url;
        }

        public String getLink_exist() {
            return link_exist;
        }

        public void setLink_exist(String link_exist) {
            this.link_exist = link_exist;
        }

        private  MediumForReturn(Media data)  throws JSONException
        {
            this.id = data.getMediaId();
            this.order = data.getMediaSortOrder();
            this.title = DB.filterHtml(data.getMediaTitleSk());
            this.thumbLink = DB.filterHtml(data.getMediaThumbLink());
            this.group = data.getGroupsToString();
            this.link_url = DB.filterHtml(data.getMediaLink());
            this.link_exist = (MediaDB.isExists(data) && Tools.isNotEmpty(data.getMediaLink()))?"true": "false";
        }
    }

    private List<DataTablesFieldError> fieldErrors = new ArrayList<>();


    @Override
    public List<Object> list(HttpServletRequest request) {
        return null;
    }

    @Override
    public List<Object> save(HttpServletRequest request, Map<Integer, Map<String, String>> parsedRequest)
    {
        List<Object> ret = new ArrayList<>();
        boolean order = false;
        if(parsedRequest.size() > 1) order = true;

        int userId = -1;
        RequestBean rb = SetCharacterEncodingFilter.getCurrentRequestBean();
        if (rb != null)
        {
            userId = rb.getUserId();
        }

        for (Map.Entry<Integer, Map<String, String>> entry : parsedRequest.entrySet())
        {
            int mediaIdFromMap = entry.getKey();

            System.out.println(entry.getKey() + "/" + entry.getValue());
            Map<String,String>  parsedMap = entry.getValue();
            Media data;

            switch(parsedMap.get("action")) {
                case "create":
                    int mediaId =Tools.getIntValue(parsedMap.get("id"),-1);
                    int mediaFkId =  Tools.getIntValue(parsedMap.get("mediaFkId"), -1);
                    int mediaSortOrder = 0;

                    if(mediaFkId != -1)
                    {
                        mediaSortOrder = MediaDB.getLastOrder(mediaFkId, "documents");
                    }
                    else
                    {
                        mediaSortOrder = MediaDB.getLastOrder(userId, "documents_temp");
                    }

                    if(mediaId ==-1){ // create
                        data = new Media();
                        parsedMap.put("order", (mediaSortOrder+10)+""); // editor vracia defaultne pri create order = 0, preto to prepisujem
                        data.setMediaFkId(mediaFkId);
                        data.setMediaFkTableName("documents");

                    }else{ // duplicate
                        data = MediaDB.getMedia(Tools.getIntValue(parsedMap.get("id"),-1));
                        data = MediaDB.duplicateMedia(data.getMediaId());
                        if(data == null) break;
                    }

                    if(mediaFkId == -1)
                    {
                        //stranka este nie je ulozena
                        data.setMediaFkId(userId);
                        data.setMediaFkTableName("documents_temp");
                    }

                    data = fillDataFromRequest(data, parsedMap, false);
                    data.setMediaId(-1);
                    data.setLastUpdate(new Date(Tools.getNow()));
                    data.save();
                    try {
                        ret.add(new MediumForReturn(data));
                    } catch (JSONException e) {
                        sk.iway.iwcm.Logger.error(e);
                    }
                    break;
                case "edit":
                    data = MediaDB.getMedia(Tools.getIntValue(mediaIdFromMap,-1));
                    if(data !=null){
                        data = fillDataFromRequest(data, parsedMap, order);
                        data.setLastUpdate(new Date(Tools.getNow()));
                        data.save();
                        try {
                            ret.add(new MediumForReturn(data));
                        } catch (JSONException e) {
                            sk.iway.iwcm.Logger.error(e);
                        }
                    }
                    break;
                case "remove":
                    mediaId = Tools.getIntValue(parsedMap.get("id"), -1);
                    if(mediaId > -1){
                        new MediaDB().deleteByIds(mediaId);
                    }
                    break;
                default:
                    //System.out.println("!!!!!Action not found!!!!");
                    break;
            }

        }




        return ret;
    }

    @Override
    public String getError() {
        return null;
    }


    /**
     * Getter for form input errors.
     * @return List of DataTablesFieldError
     */
    @Override
    public List<DataTablesFieldError> getFieldErrors() {
        return fieldErrors;
    }

    @Override
    public boolean canSave(Identity user) {
        //TODO: nejako lepsie kontrolovat pravo, napr. ci mozem editovat danu stranku
        return !(user==null || !user.isAdmin());
    }

    private Media fillDataFromRequest(Media data,Map<String, String> parsedRequest, boolean checkEmpty ){
        if(data != null) {
            int order = Tools.getIntValue(parsedRequest.get("order"), 0);
            String title = parsedRequest.get("title");
            String link_url = parsedRequest.get("link_url");
            String thumbLink = parsedRequest.get("thumbLink");
            String groupsInput = parsedRequest.get("groups");
            data.setMediaSortOrder(order);

            String[] groupsIntputArray = groupsInput.split(",");

            // naplnim grupy
            List<MediaGroupBean> mediaGroups = new ArrayList<>();
            for (String g : groupsIntputArray) {
                MediaGroupBean mediaGroup = MediaDB.getGroup(Tools.getIntValue(g, -1));
                if (mediaGroup != null) mediaGroups.add(mediaGroup);
            }
            if(checkEmpty) {
                if (mediaGroups.size() > 0) data.setGroups(mediaGroups);
                if (Tools.isNotEmpty(title)) data.setMediaTitleSk(title);
                if (Tools.isNotEmpty(link_url)) data.setMediaLink(link_url);
                if (Tools.isNotEmpty(thumbLink)) data.setMediaThumbLink(thumbLink);
            }else{
                data.setGroups(mediaGroups);
                data.setMediaTitleSk(title);
                data.setMediaLink(link_url);
                data.setMediaThumbLink(thumbLink);
            }

            String mediaGroupsNames = mediaGroups.stream().map( n -> n.getMediaGroupName() )
                    .collect( Collectors.joining( ", " ) );
            data.setMediaGroup(mediaGroupsNames);
        }
        return data;
    }

}