package sk.iway.iwcm.admin.layout;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UsersDB;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocDetailsDto {
    private int docId;
    private String virtualPath;
    private String fullPath;
    private String title;
    private String saveDate;
    private int createdByUserId;
    private String createdByUserName;
    private String createdByUserLogin;
    private int groupId;

    public DocDetailsDto()
    {
        docId = -1;
        title = "";
    }

    public DocDetailsDto(DocDetails doc) {
        docId = doc.getDocId();
        virtualPath = doc.getVirtualPath();
        fullPath = doc.getFullPath();
        title = Tools.replace(doc.getTitle(), "&#47;", "/");
        saveDate = Tools.formatDateTimeSeconds(doc.getDateCreated());
        createdByUserId = doc.getAuthorId();
        UserDetails user = UsersDB.getUser(createdByUserId);
        if (user != null) {
            createdByUserName = user.getFullName();
            createdByUserLogin = user.getLogin();
        }
        groupId = doc.getGroupId();
    }
}
