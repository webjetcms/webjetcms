package sk.iway.iwcm.editor.rest;


import jakarta.servlet.http.HttpServletRequest;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import lombok.Data;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.attributes.jpa.DocAtrDefRepository;
import sk.iway.iwcm.users.UsersDB;

/**
 * Options for method getAllItems from class WebpagesRestController
 */
@Data
public class GetAllItemsDocOptions {

    //groupId of groups we want
    private int groupId;

    /**
     * If userGroupIdString != null ... userGroupId param will serve as PasswordProtected param to get DocPages
     *
     * Else if tempId != null ... tempId param will be used to get DocPages that use this template
     *
     * Else, this param will specify, what type of pages we want
     *
     *     - userGroupId = Constants.getInt("systemPagesRecentPages"), mean we want DocDetails RecentPages from table documents
     *     - groupId = Constants.getInt("systemPagesDocsToApprove"), mean we want DocDetails PagesToApprove from table documents_history
     *     - else we want just DocPages specified by groupId from table documents
     */
    private int userGroupId;
    private int tempId;
    private Pageable pageable;

    private DocDetailsRepository docDetailsRepository;
    private DocAtrDefRepository docAtrDefRepository;

    private boolean recursiveSubfolders = false;

    private final HttpServletRequest request;
    private final Identity currentUser;
    private final boolean userGroupIdRequested ;
    private final int userId;
    private final boolean tempIdRequested;

    private Specification<DocDetails> columnsSpecification;

    public GetAllItemsDocOptions(HttpServletRequest request) {

        this.request = request;
        this.currentUser = UsersDB.getCurrentUser(request);
        this.userId = this.currentUser.getUserId();
        this.columnsSpecification = null;

        this.userGroupId = -1;
        if(request.getParameter("userGroupId") != null) this.userGroupIdRequested = true;
        else this.userGroupIdRequested = false;

        this.tempId = -1;
        if(request.getParameter("tempId") != null) this.tempIdRequested = true;
        else this.tempIdRequested = false;
    }

    public String getRequestPrameter(String paramName) {
        return request.getParameter(paramName);
    }
}
