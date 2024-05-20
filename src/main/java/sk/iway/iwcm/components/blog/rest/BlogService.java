package sk.iway.iwcm.components.blog.rest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONObject;

import sk.iway.Password;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.UploadFileTools;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.components.blog.jpa.BloggerBean;
import sk.iway.iwcm.components.users.AuthorizeUserService;
import sk.iway.iwcm.components.users.userdetail.UserDetailsEntity;
import sk.iway.iwcm.components.users.userdetail.UserDetailsRepository;
import sk.iway.iwcm.components.users.userdetail.UserDetailsService;
import sk.iway.iwcm.database.ComplexQuery;
import sk.iway.iwcm.database.Mapper;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.facade.EditorFacade;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.ModuleInfo;
import sk.iway.iwcm.system.Modules;
import sk.iway.iwcm.users.UsersDB;

public class BlogService {

    private BlogService() {}

    private static final String QUERY_PREFIX_ALL = "SELECT user_id, first_name, last_name, login, email, editable_groups FROM users WHERE";
    private static final String QUERY_PREFIX_ID = "SELECT user_id FROM users WHERE";
    private static final String DOMAIN_ID = " AND domain_id = " + CloudToolsForCore.getDomainId();

    /********************************* GET *********************************************/

    /**
     * Get BloggerBean by bloggerId
     * @param bloggerId
     * @return
     */
    public static BloggerBean getBloggerBean(long bloggerId) {
        StringBuilder query = new StringBuilder(QUERY_PREFIX_ALL);
        query.append(" user_id = ").append(bloggerId);
        query.append(DOMAIN_ID);

        GroupsDB groupsDB = GroupsDB.getInstance();
        List<BloggerBean> blogger = new ArrayList<>();
        new ComplexQuery().setSql(query.toString()).list(new Mapper<BloggerBean>() {
			@Override
			public BloggerBean map(ResultSet rs) throws SQLException {
				blogger.add( resultSetToBean(rs, groupsDB) );
                return null;
			}
		});
        return blogger.isEmpty() ? null : blogger.get(0);
    }

    /**
     * Get all bloggers (aka user that belongs to BLOG group)
     * @return
     */
    public static List<BloggerBean> getAllBloggers() {
        StringBuilder query = new StringBuilder(QUERY_PREFIX_ALL);
        addQueryConditions(query);

        GroupsDB groupsDB = GroupsDB.getInstance();
        List<BloggerBean> bloggers = new ArrayList<>();

        new ComplexQuery().setSql(query.toString()).list(new Mapper<BloggerBean>() {
			@Override
			public BloggerBean map(ResultSet rs) throws SQLException {
                bloggers.add( resultSetToBean(rs, groupsDB) );
				return null;
			}
		});

        return bloggers;
    }

    /**
     * Convert result set to BloggerBean, and return bean
     * @param rs
     * @param groupsDB
     * @return
     * @throws SQLException
     */
    private static BloggerBean resultSetToBean(ResultSet rs, GroupsDB groupsDB) throws SQLException{
        BloggerBean blogger = new BloggerBean();
        blogger.setId(rs.getLong("user_id"));
        blogger.setFirstName(rs.getString("first_name"));
        blogger.setLastName(rs.getString("last_name"));
        blogger.setLogin(rs.getString("login"));
        blogger.setEmail(rs.getString("email"));

        //Set password as unchanged
        blogger.setPassword(UserTools.PASS_UNCHANGED);

        String editableGroups = rs.getString("editable_groups");
        int[] editableGroupsIds = Tools.getTokensInt(editableGroups, ",");
        if(editableGroupsIds.length > 0) blogger.setEditableGroup( groupsDB.getGroup(editableGroupsIds[0]) );

        return blogger;
    }

    /********************************* EDIT *********************************************/

    /**
     * Edit blogger -> aka get UserDetailsEntity by bloggerId, and edit/save it
     * @param bloggerToEdit
     * @param userDetailsRepository
     * @return
     */
    public static boolean editBlogger(BloggerBean bloggerToEdit, UserDetailsRepository userDetailsRepository, HttpServletRequest request) {
        UserDetailsEntity userBlogger = userDetailsRepository.getById(bloggerToEdit.getId());

        //Field's login AND editableGroups CANT BE CHANGE
        //Copy ONLY remain fields
        userBlogger.setFirstName( bloggerToEdit.getFirstName() );
        userBlogger.setLastName( bloggerToEdit.getLastName() );
        userBlogger.setEmail( bloggerToEdit.getEmail() );

        //Update user
        userDetailsRepository.save(userBlogger);

        //After save, set password back
        boolean sendWelcomeEmail = false;
        String password = bloggerToEdit.getPassword();
        if (Tools.isEmpty(password) || "*".equals(password)) {
            password = Password.generatePassword(8);
            bloggerToEdit.setPassword( password );
            sendWelcomeEmail = true;
        }

        //Now CHANGE password
        userBlogger.setPassword( bloggerToEdit.getPassword() );

        //Update user password (if needed)
        boolean passwordSaved = UserDetailsService.savePassword(bloggerToEdit.getPassword(), userBlogger.getId().intValue());

        if (passwordSaved && sendWelcomeEmail) {
            //Send welcome email again
            AuthorizeUserService.sendInfoEmail(userBlogger, password, UsersDB.getCurrentUser(request), request);
        }
        return passwordSaved;
    }

    /********************************* INSERT *********************************************/

    /**
     * Perform whooole process of saving new blogger.
     * Including: creating new user of type blogger (with all needed rights and perms), create bloger structure (root group + 1doc, default sub group + 2doc's) AND send email to user (aka blogger) about adding him to blog group
     * @param bloggerToSave
     * @param userDetailsRepository
     * @param editorFacade
     * @param request
     * @return
     */
    public static boolean saveBlogger(BloggerBean bloggerToSave, UserDetailsRepository userDetailsRepository, EditorFacade editorFacade, HttpServletRequest request) {
        UserDetailsEntity newUser = new UserDetailsEntity();
        newUser.setId((long)-1);
        newUser.setFirstName( bloggerToSave.getFirstName() );
        newUser.setLastName( bloggerToSave.getLastName() );
        newUser.setEmail( bloggerToSave.getEmail() );
        newUser.setRegDate(new Date());

        //Set loggin and Writable folders
        String login = bloggerToSave.getLogin();
        newUser.setLogin( login );

        //Blog groupID (without this group id, user not gonna be showed in blogger's list)
        newUser.setUserGroupsIds( "" + DocDB.getBlogGroupId() );

        //Need to set
        newUser.setForumRank(0);
        newUser.setRatingRank(0);
        newUser.setParentId(0);
        newUser.setAdmin(true);
        newUser.setAuthorized(true);

        //First save password as null for save
        newUser.setPassword(null);

        //Save new blogger user
        userDetailsRepository.save(newUser);

        //Get new saved user
        Integer newUserId = UsersDB.getUserIdByLogin(login);
        if(newUserId == null) return false;
        newUser.setId( newUserId.longValue() );
        bloggerToSave.setId( newUserId.longValue() );

        //After save, set password back
        String password = bloggerToSave.getPassword();
        if (Tools.isEmpty(password) || "*".equals(password)) password = Password.generatePassword(8);
        newUser.setPassword( password );

        //Set user rights
        setUserRights(newUserId);

        //Set user password
        boolean savedPassword = UserDetailsService.savePassword(password, newUserId);
        if(!savedPassword) return false;

        //MAKE - blogger structure
        prepareBloggerStructure(newUser, bloggerToSave.getEditableGroup(), userDetailsRepository, editorFacade, request);

        //Send welcome email
        AuthorizeUserService.sendInfoEmail(newUser, password, UsersDB.getCurrentUser(request), request);

        return true;
    }

    private static void setUserRights(int userId) {
        (new SimpleQuery()).execute("DELETE FROM user_disabled_items WHERE user_id=?", userId);

        StringBuilder sb = new StringBuilder("INSERT INTO user_disabled_items VALUES");
        List<ModuleInfo> modules = Modules.getInstance().getModules();

        String[] defaulPerm = {"cmp_blog", "addPage", "pageSave", "deletePage", "addSubdir", "cmp_diskusia" };
        String[] bonusPerm = Tools.getTokens( Constants.getString("bloggerAppPermissions") , ",");
        Set<String> duplicity = new HashSet<>();
        for(ModuleInfo module : modules){
            //
            if(module == null || module.getItemKey() == null || Arrays.stream(defaulPerm).anyMatch(module.getItemKey()::equals) || Arrays.stream(bonusPerm).anyMatch(module.getItemKey()::equals)) continue;

            //If duplicity, skip
            if(duplicity.contains(module.getItemKey())==false) {
                duplicity.add(module.getItemKey());
                sb.append(" (").append(userId).append(",'").append(module.getItemKey()).append("'),");
            }

            //Submodules
            if(module.getSubmenus() == null) continue;
            for (ModuleInfo subModule : module.getSubmenus()) {
                //
                if(subModule == null || subModule.getItemKey() == null || Arrays.stream(defaulPerm).anyMatch(subModule.getItemKey()::equals) || Arrays.stream(bonusPerm).anyMatch(subModule.getItemKey()::equals)) continue;

                //If duplicity, skip
                if(duplicity.contains(subModule.getItemKey())) continue;
                duplicity.add(subModule.getItemKey());

                sb.append(" (").append(userId).append(",'").append(subModule.getItemKey()).append("'),");
            }
        }

        //If SB does not contain cmp_blog_admin, add it
        if( sb.indexOf("(" + userId + ",'cmp_blog_admin'),") == -1 ) sb.append(" (").append(userId).append(",'cmp_blog_admin'),");

        //Remove last ','
        sb.deleteCharAt(sb.length() - 1);
        (new SimpleQuery()).execute(sb.toString());
    }

    /**
     * Prepare blogger structure of 2 groups and 3 docs.
     * @param bloggerUser
     * @param parentGroup
     * @param userDetailsRepository
     * @param editorFacade
     * @param request
     */
    private static void prepareBloggerStructure(UserDetailsEntity bloggerUser, GroupDetails parentGroup, UserDetailsRepository userDetailsRepository, EditorFacade editorFacade, HttpServletRequest request) {
        //Create root group
        GroupDetails rootGroup = createGroup(bloggerUser.getLogin(), bloggerUser.getId().intValue(), parentGroup, null);
        //Check if root group was made right now
        if(rootGroup != null) {

            int authorId = ((Identity) request.getSession().getAttribute(Constants.USER_KEY)).getUserId();
            Prop prop = Prop.getInstance(request);

            StringBuilder writableFolders = new StringBuilder();
            writableFolders.append(UploadFileTools.getPageUploadSubDir(-1, rootGroup.getGroupId(), null, "/images")).append("/*\n");
            writableFolders.append(UploadFileTools.getPageUploadSubDir(-1, rootGroup.getGroupId(), null, "/files")).append("/*\n");
            writableFolders.append(UploadFileTools.getPageUploadSubDir(-1, rootGroup.getGroupId(), null, "/images/gallery")).append("/*\n");

            //Set for user new editable group (blogger root group)
            userDetailsRepository.updateEditableGroupsWritableFolders(""+rootGroup.getGroupId(), writableFolders.toString(), bloggerUser.getId());

            //Create root group NEWS DOC
            createGroupNewsDoc(editorFacade, rootGroup.getGroupId(), rootGroup.getGroupName(), authorId, bloggerUser.getId().intValue());

            //Create default sub group of root group
            GroupDetails defaultSubGroup = createGroup(bloggerUser.getLogin(), bloggerUser.getId().intValue(), rootGroup, prop.getText("components.blog.default_group_name"));
            if (defaultSubGroup != null) {
                //Create default sub group NEWS DOC
                createGroupNewsDoc(editorFacade, defaultSubGroup.getGroupId(), defaultSubGroup.getGroupName(), authorId, bloggerUser.getId().intValue());

                // //Create example doc
                createExampleDoc(editorFacade, defaultSubGroup.getGroupId(), prop.getText("components.blog.default_page_title"), authorId, bloggerUser.getId().intValue() , prop.getText("components.blog.default_page_text"));
            }

            //
            createDirectoriesBloggerCanAccess( writableFolders.toString() );
        }
    }

    /**
     * Create new group
     * @param userLogin
     * @param userId
     * @param parentGroup
     * @param groupName
     * @return
     */
    private static GroupDetails createGroup(String userLogin, int userId, GroupDetails parentGroup, String groupName) {
        //Check if group allready exist
        GroupDetails newGroup = GroupsDB.getInstance().getGroup(userLogin, parentGroup.getGroupId());
        if(newGroup != null) return null;

        //Create new root group
        newGroup = new GroupDetails();
		newGroup.setParentGroupId( parentGroup.getGroupId() );
		newGroup.setGroupName( Tools.isEmpty(groupName) ? userLogin : groupName );
		newGroup.setTempId( parentGroup.getTempId() );
		newGroup.setFieldA( String.valueOf(userId) );
		GroupsDB.getInstance().setGroup(newGroup);
		return newGroup;
    }

    private static void createGroupNewsDoc(EditorFacade editorFacade, int rootGroupId, String docTitle, int authorId, int bloggerId) {
        DocDetails groupDoc = editorFacade.getDocForEditor(-1, -1, rootGroupId);

        //Set doc
        groupDoc.setTitle(docTitle);
        groupDoc.setNavbar(docTitle);
        groupDoc.setData(Prop.getInstance().getText("components.blog.atricles-code", ""+rootGroupId));
		groupDoc.setAuthorId(authorId);
		groupDoc.setAvailable(true);
		groupDoc.setSearchable(true);
		groupDoc.setCacheable(false);

        //Save doc
        editorFacade.save(groupDoc);

        updateAuthorId(groupDoc.getDocId(), bloggerId);
    }

    private static void updateAuthorId(int docId, int authorId) {
        (new SimpleQuery()).execute("UPDATE documents SET author_id=? WHERE doc_id=?", authorId, docId);
    }

    private static void createExampleDoc(EditorFacade editorFacade, int rootGroupId, String docTitle, int authorId, int bloggerId, String docData) {
        DocDetails groupDoc = editorFacade.getDocForEditor(-1, -1, rootGroupId);

        //Set doc
        groupDoc.setTitle(docTitle);
        groupDoc.setNavbar(docTitle);
        groupDoc.setData(docData);
        groupDoc.setAuthorId(authorId);
        groupDoc.setAvailable(true);
		groupDoc.setSearchable(true);
		groupDoc.setCacheable(false);

        //Save doc
        editorFacade.save(groupDoc);

        updateAuthorId(groupDoc.getDocId(), bloggerId);
    }

    private static void createDirectoriesBloggerCanAccess(String writableFolders) {
        String[] folders = Tools.getTokens(writableFolders, "\n");
        for(String folder : folders) {
            folder = folder.substring(0, folder.lastIndexOf("/")+1);
            IwcmFile file = new IwcmFile(Tools.getRealPath(folder));
            file.mkdirs();
        }
	}

    /********************************* SUPPORT METHODS *********************************************/

    /**
     * Return groupID's of all users of type bloggers
     * @return
     */
    public static List<Integer> getAllBloggersGroupIds() {
        List<Integer> allGroupIds = new ArrayList<>();

		for(Integer rootGroupId : getAllBloggersRootGroupIds())
            addBloggerGroupIds(rootGroupId, allGroupIds);

        return allGroupIds;
    }

    /**
     * Get groupTree based on rootGroupId and push groupIds into bloggerGroupIds list
     * @param bloggerRootGroupId
     * @param bloggerGroupIds
     */
    private static void addBloggerGroupIds(int bloggerRootGroupId, List<Integer> bloggerGroupIds) {
        //Get groups tree from user editable root group
		List<GroupDetails> groopsTree = GroupsDB.getInstance().getGroupsTree(bloggerRootGroupId, true, true);
		for(GroupDetails group : groopsTree) {
			bloggerGroupIds.add(group.getGroupId());
		}
    }

    /**
     * Get all rootGroupIds (aka editabelGroup) of all users of type bloggers
     * @return
     */
    public static List<Integer> getAllBloggersRootGroupIds() {
        List<Integer> bloggerIds = getAllBloggersIds();
        if (bloggerIds.isEmpty()) return new ArrayList<>(0);

        StringBuilder bloggerIdsSb = new StringBuilder("(");
        for(Integer bloggerId : bloggerIds) bloggerIdsSb.append(bloggerId).append(",");
        //Remove last ','
        bloggerIdsSb.deleteCharAt(bloggerIdsSb.length() - 1);
        //Add enclosing ')'
        bloggerIdsSb.append(")");

        //
        List<String> rootGroupIdsString = (new SimpleQuery()).forListString("SELECT editable_groups FROM users WHERE user_id IN " + bloggerIdsSb.toString() + DOMAIN_ID);

        //
        List<Integer> rootGroupIds = new ArrayList<>();
        for(String rootGroupId : rootGroupIdsString) {
            if(Tools.isEmpty(rootGroupId)) continue;
            rootGroupIds.add( Tools.getTokensInt(rootGroupId, ",")[0] );
        }
        return rootGroupIds;
    }

    /**
     * Get id of all users of type bloggers
     * @return
     */
    private static List<Integer> getAllBloggersIds() {
        StringBuilder query = new StringBuilder(QUERY_PREFIX_ID);
        addQueryConditions(query);
        return (new SimpleQuery()).forListInteger(query.toString());
    }

    /**
     * A LIKE condtion to find by user_group (all combinations)
     * @param query
     */
    private static void addQueryConditions(StringBuilder query) {
        int blogGroupId = DocDB.getBlogGroupId();
        query.append("(user_groups LIKE '").append(blogGroupId).append("' OR ");
        query.append("user_groups LIKE '%,").append(blogGroupId).append("' OR ");
        query.append("user_groups LIKE '").append(blogGroupId).append(",%' OR ");
        query.append("user_groups LIKE '%,").append(blogGroupId).append(",%') ");
        query.append(DOMAIN_ID);
    }

    /**
     * User is considerred blogger if he have perm cmp_blog AND he is inside of blog userGroup
     * @param user
     * @return
     */
    public static boolean isUserBlogger(Identity user) {
        //FIRST check if user have cmp_blog permission
        if(!user.isEnabledItem("cmp_blog")) return false;

        //SECOND check if user is inside of blog userGroup
        int blogGroupId = DocDB.getBlogGroupId();
        int[] userGroupIds = Tools.getTokensInt(user.getUserGroupsIds(), ",");
        return IntStream.of( userGroupIds ).anyMatch(x -> x == blogGroupId);
    }

    /**
     * User is considered blogger admin if he have perm cmp_blog_admin AND he is admin
     * @param user
     * @return
     */
    public static boolean isUserBloggerAdmin(Identity user) {
        return (user.isAdmin() && user.isEnabledItem("cmp_blog_admin"));
    }

    /**
     * Verify if user is blogger or blogger admin
     * @param user
     * @return
     */
    public static boolean isUserBloggerOrBloggerAdmin(Identity user) {
        return isUserBlogger(user) || isUserBloggerAdmin(user);
    }

    /**
     * Add new GROUP to blogger structure
     * @param currentUser
     * @param customData
     * @return
     */
    public static boolean addNewBloggerGroup(EditorFacade editorFacade, Identity currentUser, String customData) {
        boolean isBloggerAdmin = BlogService.isUserBloggerAdmin( currentUser );
        boolean isBlogger = BlogService.isUserBlogger( currentUser );

        //Check perms
        if(isBloggerAdmin==false && isBlogger==false) return false;

        int groupId = -1;
        String newGroupName = "";
        if(customData != null && !customData.isEmpty()) {
            try {
                JSONObject jsonObject = new JSONObject(customData);
                groupId = Tools.getIntValue((String) jsonObject.get("groupId"), -1);
                newGroupName = (String) jsonObject.get("newGroupName");
            } catch (Exception ex) {
                sk.iway.iwcm.Logger.error(ex);
                return false;
            }
        }

        if(Tools.isEmpty(newGroupName)) return false;

        if(groupId < 1) {
            if(isBlogger) groupId = Tools.getTokensInt(currentUser.getEditableGroups(), ",")[0];
            else return false; //It's admin not casual blogger
        } else {
            //Check perms

            //If user is blogger admin, check if group is blogger group
            if(isBloggerAdmin) {
                List<Integer> allBloggersGroupIds = BlogService.getAllBloggersRootGroupIds();
                if(Boolean.FALSE.equals( allBloggersGroupIds.contains(groupId) )) return false;
            } //If user is blogger, check if he has perm to selected group
            else if(GroupsDB.isGroupEditable(currentUser, groupId)==false) {
                return false;
            }
        }

        //All is ok, create group
        GroupDetails newGroup = createGroup(currentUser.getLogin(), currentUser.getUserId(), GroupsDB.getInstance().getGroup(groupId), newGroupName);
        if (newGroup != null) {
            createGroupNewsDoc(editorFacade, newGroup.getGroupId(), newGroup.getGroupName(), currentUser.getUserId(), currentUser.getUserId());
            return true;
        }
        return false;
    }
}