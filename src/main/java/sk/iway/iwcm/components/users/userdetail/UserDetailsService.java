package sk.iway.iwcm.components.users.userdetail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;

import sk.iway.Password;
import sk.iway.iwcm.Adminlog;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.SendMail;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.admin.jstree.JsTreeItem;
import sk.iway.iwcm.admin.layout.MenuService;
import sk.iway.iwcm.common.LogonTools;
import sk.iway.iwcm.common.UserTools;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.stat.SessionHolder;
import sk.iway.iwcm.users.AuthorizeAction;
import sk.iway.iwcm.users.PasswordSecurity;
import sk.iway.iwcm.users.PasswordsHistoryBean;
import sk.iway.iwcm.users.PasswordsHistoryDB;
import sk.iway.iwcm.users.PermissionGroupBean;
import sk.iway.iwcm.users.UserDetails;
import sk.iway.iwcm.users.UserGroupDetails;
import sk.iway.iwcm.users.UserGroupsDB;
import sk.iway.iwcm.users.UsersDB;

@Service
public class UserDetailsService {

    /**
     * Metoda vykona kroky nastavujuce dodatocne udaje po ulozeni hlavnej entity
     * Je potrebne ju volat VZDY po ulozeni entity do DB
     * @param entity
     * @return
     */
    public boolean afterSave(UserDetailsEntity entity, UserDetailsEntity saved) {
        Integer userId = saved.getId().intValue();

        if (entity.getEditorFields() != null) {

            //Get userPerGroup from DB as "oldUserPermGroups"
            List<PermissionGroupBean> oldUserPermGroups = UserGroupsDB.getPermissionGroupsFor(userId);

            //Get userPermGroup ids from editor as "newUserPermGroupIds"
            Integer[] newUserPermGroupIds = entity.getEditorFields().getPermGroups();
            if (newUserPermGroupIds == null) newUserPermGroupIds = new Integer[0];

            /*
                Loop oldUserPermGroup and compare if this perm group id is inside newUserPermGroupId.
                If yes, this user permmission group can stay in DB (beacuase is actual).
                If no, call deleteUserFromPermissionGroup from UsersDB and delete this permGroup for this user.
            */
            for(PermissionGroupBean oldUserPermGroup : oldUserPermGroups) {

                boolean deletePerm = true;

                for(Integer newUserPermGroupId : newUserPermGroupIds) {
                    if(newUserPermGroupId != null) {
                        if(oldUserPermGroup.getUserPermGroupId() == newUserPermGroupId) {
                            deletePerm = false;
                            break;
                        }
                    }
                }

                if(deletePerm) {
                    UsersDB.deleteUserFromPermissionGroup(userId, oldUserPermGroup.getUserPermGroupId());
                }
            }

            /*
                Loop newUserPermGroupId and compare if this id is inside one of oldUserPermGroup values.
                If yes, userPermGroup already exist in DB so we dont need any action.
                If no, we need add this new userPermGroup into DB by calling addUserToPermissionGroup from UserDB to add this permGroup for this user.
            */
            for(Integer newUserPermGroupId : newUserPermGroupIds) {

                if (newUserPermGroupId==null) continue;

                boolean addPerm = true;

                for(PermissionGroupBean oldUserPermGroup : oldUserPermGroups) {

                    if(newUserPermGroupId == oldUserPermGroup.getUserPermGroupId()) {
                        addPerm = false;
                        break;
                    }
                }

                if(addPerm && newUserPermGroupId != null) {
                    UsersDB.addUserToPermissionGroup(userId, newUserPermGroupId);
                }
            }

            //Get enabled items, find disabled items and update user_disabled_items table
            List<JsTreeItem> allModuleItems = MenuService.getAllPermissions().stream().filter(item -> item.getId().endsWith("-leaf")==false).collect(Collectors.toList());

            Identity user = new Identity(UsersDB.getUser(userId));
            UsersDB.loadDisabledItemsFromDB(user, false);

            Set<String> disabledItemsKeys = user.getDisabledItemsTable().keySet();

            String[] newEnabledItems = entity.getEditorFields().getEnabledItems();

            if(newEnabledItems != null) {

                //Loop new enabled items and compare them with allready disabled items in DB
                //If equals is true enable this item using UsersDB.enableItem
                for(String newEnabledItem : newEnabledItems) {
                    newEnabledItem = MenuService.removePermsIdPrefix(newEnabledItem);

                    boolean isDisabled = false;
                    for(String disabledItemKey : disabledItemsKeys) {
                        if(disabledItemKey.equals(newEnabledItem)) {
                            isDisabled = true;
                            break;
                        }
                    }

                    if(isDisabled) UsersDB.enableItem(userId, newEnabledItem);
                }

                //Loop all existed items
                for(JsTreeItem moduleInfo : allModuleItems) {

                    //Get item key
                    String actualControledItem = MenuService.removePermsIdPrefix(moduleInfo.getId());

                    boolean isEnabled = false;

                    //Loop all enabled items and find if is actualControledItem enabled
                    for(String enabledItem : newEnabledItems) {
                        enabledItem = MenuService.removePermsIdPrefix(enabledItem);
                        if(actualControledItem.equals(enabledItem)) {
                            isEnabled = true;
                            break;
                        }
                    }

                    //actualControledItem is not enabled
                    if(!isEnabled) {
                        boolean isAllreadyDisabledInDB = false;

                        //Loop all old disabled items and find if is actualControledItem allready disabled in DB
                        for(String disabledItem : disabledItemsKeys) {
                            if(actualControledItem.equals(disabledItem)) {
                                isAllreadyDisabledInDB = true;
                                break;
                            }
                        }

                        //So if actualControledItem is not set as Enabled but, its not Disabled in DB call UsersDB.disableItem
                        if(!isAllreadyDisabledInDB) UsersDB.disableItem(userId, actualControledItem);
                    }
                }
            }
            fixEditorMiniEdit(userId);
        }

        //entity.getId is null for new users, saved ID is in userId parameter
        if (entity.getId()==null || entity.getId()<1) {
            if (Tools.isEmpty(entity.getPassword()) || UserTools.PASS_UNCHANGED.equals(entity.getPassword())) {
                //set random password for new users
                entity.setPassword(Password.generatePassword(10));
            }
        }

        boolean saveok = savePassword(entity.getPassword(), userId);
        if (saveok==false) return false;

        if (Tools.isNotEmpty(entity.getApiKey()) && entity.getApiKey().equals(UserTools.PASS_UNCHANGED)==false) {
            String apiKey = null;
            //ak obsahuje len znak - chceme ho zmazat
            if ("-".equals(entity.getApiKey())) {
                apiKey = "";
            } else {
                //zahashuj token v databaze
                String salt = PasswordSecurity.generateSalt();
                String tokenHashed = PasswordSecurity.calculateHash(entity.getApiKey(), salt);

                apiKey = salt+"|"+tokenHashed;
            }

            if (apiKey != null) (new SimpleQuery()).execute("UPDATE users SET api_key=? WHERE user_id=?", apiKey, entity.getId());
        }

        return true;
    }

    public static boolean savePassword(UserDetailsBasic entity, int userId) {
        //entity.getId is null for new users, saved ID is in userId parameter
        if (entity.getId()==null || entity.getId()<1) {
            if (Tools.isEmpty(entity.getPassword()) || UserTools.PASS_UNCHANGED.equals(entity.getPassword())) {
                //set random password for new users
                entity.setPassword(Password.generatePassword(10));
            }
        }
        return savePassword(entity.getPassword(), userId);
    }

    /**
     * Set user password to database, also set PasswordHistory
     * @param userId - ID of user
     * @param password - new PLAIN TEXT password or UserTools.PASS_UNCHANGED if password is not changed
     * @return
     */
    @SuppressWarnings("java:S1871")
    public static boolean savePassword(String password, int userId) {

        if (Tools.isNotEmpty(password) && password.equals(UserTools.PASS_UNCHANGED)==false && userId>0) {

            String currentHash = (new SimpleQuery()).forString("SELECT password FROM users WHERE user_id=?", userId);
            if (password.startsWith("bcrypt:$2a$12") && password.length()>64) {
                //it's allready bcrypt hash, skip save
                return true;
            } else if (password.length()==128) {
                //it's old password hash
                return true;
            } else if (currentHash != null && Tools.isNotEmpty(currentHash) && currentHash.equals(password)) {
                //it's allready current hash/password, skip save
                return true;
            }

            //ulozit heslo
            Logger.debug(UserDetailsService.class, "Heslo je zmenene, ukladam");

            UserDetails user = UsersDB.getUser(userId);
            if (user != null) {
                try {

                String salt = "";
                String hash = "";
                sk.iway.Password pass = new sk.iway.Password();
                if (Constants.getBoolean("passwordUseHash"))
                {
                    salt = PasswordSecurity.generateSalt();
                    hash = PasswordSecurity.calculateHash(password, salt);
                }
                else
                {
                    hash = pass.encrypt(password);
                }

                PasswordsHistoryBean.insertAndSaveNew(userId, hash, salt);

                //uloz do DB
                (new SimpleQuery()).execute("UPDATE users SET password=?, password_salt=? WHERE user_id=?", hash, salt, userId);

                String login = (new SimpleQuery()).forString("SELECT login FROM users WHERE user_id=?", userId);
                //zaauditovat zmenu hesla
                Adminlog.add(Adminlog.TYPE_USER_CHANGE_PASSWORD, userId, "SaveUserAction - user ("+login+") successfully changed password", -1, -1);

                //invalidate other user sessions
                SessionHolder.getInstance().invalidateOtherUserSessions(userId);

                } catch (Exception ex) {
                    Logger.error(UserDetailsService.class, ex);
                    return false;
                }
            }
        }
        return true;
    }

    private static void fixEditorMiniEdit(int userId) {
        String permName = "editorFullMenu";
        //zisti, ci ma zakazane editorFullMenu
        int editorFullMenuCount = new SimpleQuery().forInt("SELECT COUNT(*) FROM user_disabled_items WHERE user_id=? AND item_name=?", userId, permName);

        //najskor zmazeme a ak treba potom insertneme
        UsersDB.enableItem(userId, "editorMiniEdit");

        if (editorFullMenuCount==0) {
            //pravo plnej editacie NIE je zakazane (cize je zaskrtnute), takze musime insertnut editorMiniEdit
            UsersDB.disableItem(userId, "editorMiniEdit");
        }

        //zmaz z DB toto pravo, nema to tam co robit
        UsersDB.enableItem(userId, permName);
    }

    /**
     * Zo zadaneho pola ID pouzivatelov vytvori samostatne pole pre skupiny pouzivatelov a emailov
     * @param userGroupsIds
     * @return - zoznam prav, v get(0) su prava a v get(1) emaily
     */
    public static List<Integer[]> splitGroupsToPermsAndEmails(int[] userGroupsIds) {
        List<Integer> permissionsList = new ArrayList<>();
        List<Integer> emailsList = new ArrayList<>();
        UserGroupsDB ugdb = UserGroupsDB.getInstance();
        for(int userGroupId : userGroupsIds) {
            UserGroupDetails ug = ugdb.getUserGroup(userGroupId);
            if (ug == null) continue;

            Integer id = Integer.valueOf(userGroupId);

            if (ug.getUserGroupType()==UserGroupDetails.TYPE_EMAIL) {
                if (emailsList.contains(id)==false) emailsList.add(id);
            } else {
                if (permissionsList.contains(id)==false) permissionsList.add(id);
            }
        }

        List<Integer[]> ret = new ArrayList<>();
        ret.add(permissionsList.toArray(new Integer[0]));
        ret.add(emailsList.toArray(new Integer[0]));

        return ret;
    }

    /**
     * So zoznamu skupin pouzivatelov a emailov vytvori ciarkou oddeleny zoznam
     * @param permisions
     * @param emails
     * @return - null, ak nie je ziadna skupina zvolena
     */
    public static String getUserGroupIds(Integer[] permisions, Integer[] emails) {
        StringBuilder userGroupIds = new StringBuilder();
        //Get selected permisions user group ids and add them to string
        if(permisions != null) {
            for(Integer id : permisions) {
                if(id != null) {
                    if (userGroupIds.length()>0) userGroupIds.append(",");
                    userGroupIds.append(String.valueOf(id));
                }
            }
        }

        //Get selected eMails user group ids and add them to string
        if(emails != null) {
            for(Integer id : emails) {
                if(id != null) {
                    if (userGroupIds.length()>0) userGroupIds.append(",");
                    userGroupIds.append(String.valueOf(id));
                }
            }
        }

        String str = userGroupIds.toString();
        if (Tools.isEmpty(str) || ",".equals(str)) return null;

        return str;
    }

    /**
     * Set into session userGrousps of this user.
     * @param entity
     */
    public void setBeforeSaveUserGroups(UserDetailsEntity entity) {
        Long  userId = entity.getId();

        //Get Actual value from DB
        String oldValue = (new SimpleQuery()).forString("SELECT user_groups FROM users WHERE user_id=?", userId);

        entity.getEditorFields().setBeforeSaveUserGroupIds(oldValue);
    }

    /**
     * Check if user was added into new userGroup. If yes and other terms are fulfilled, send email for every userGroup (that was added to user) that has set docId.
     * @param user - saved user
     * @param userBeforeSave - user entity from DT request (before save)
     * @param admin - currently logged user (sender of email)
     * @param request
     */
    public void sendUserGroupsEmails(UserDetailsEntity user, UserDetailsEntity userBeforeSave, Identity admin, HttpServletRequest request) {

        //This Feature must be allowed
        if(userBeforeSave == null || userBeforeSave.getEditorFields()==null || !userBeforeSave.getEditorFields().isSendAllUserGroupsEmails()) return;

        String oldSelectedValues = userBeforeSave.getEditorFields().getBeforeSaveUserGroupIds();

        //User must be authorized
        if(user == null || !user.getAuthorized()) return;

        //We need selected userGroups
        if(user.getEditorFields() == null || user.getEditorFields().getPermisions() == null || user.getEditorFields().getPermisions().length < 1) return;

        //User email must by valid
        if(!Tools.isEmail(user.getEmail())) return;

        List<Integer> newAddedUserGrousps = new ArrayList<>();
        if(user.getId() < 0) {
            //New user, all user's groups are new, for this user
            newAddedUserGrousps = Arrays.asList( user.getEditorFields().getPermisions() );
        } else {
            //Obtain old selected user groups from session
            //String oldSelectedValues = getUserGroupsFromSession(session, user.getId());

            if(Tools.isEmpty(oldSelectedValues)) {
                //There are no old values, all user's groups are new, for this user
                newAddedUserGrousps = Arrays.asList( user.getEditorFields().getPermisions() );
            } else {
                //Take old oldSelectedValues -> convert tham from string to int[] -> split them to perms and emails -> return only perms
                List<Integer> oldUserGroups = Arrays.asList( splitGroupsToPermsAndEmails( Tools.getTokensInt(oldSelectedValues, ",") ).get(0) );

                for(Integer userGroupId : user.getEditorFields().getPermisions()) {
                    if(!oldUserGroups.contains(userGroupId)) {
                        newAddedUserGrousps.add(userGroupId);
                    }
                }
            }
        }

        //Nothing is selected (there is only 1 default value null)
        if(newAddedUserGrousps.size() == 1 && newAddedUserGrousps.get(0) == null) return;

        try {
            UserGroupsDB ugDB = UserGroupsDB.getInstance();
            DocDB docDB = DocDB.getInstance();
            for(Integer userGroupId : newAddedUserGrousps) {
                UserGroupDetails userGroup = ugDB.getUserGroup(userGroupId);
                if(userGroup != null && userGroup.getEmailDocId() > 0) {
                    DocDetails docDetails = docDB.getDoc(userGroup.getEmailDocId());

                    String body = docDetails.getData();
                    body = AuthorizeAction.updateEmailText(body, UserDetailsEntityMapper.INSTANCE.userDetailsEntityToUserDetails(user), null, request);


                    SendMail.send(admin.getFullName(), admin.getEmail(), user.getEmail(), docDetails.getTitle(), body);
                }
            }
        } catch(Exception ex) {
            sk.iway.iwcm.Logger.error(ex);
        }
    }

    /**
     * Update currently logged user, if it's same as saved user
     * @param form
     * @param user
     * @param request
     */
    public boolean updateSelf(UserDetailsBasic form, Identity user, HttpServletRequest request) {
        if (form.getId().intValue() != user.getUserId()) return false;

        user.setTitle(form.getTitle());
        user.setFirstName(form.getFirstName());
        user.setLastName(form.getLastName());



        user.setCompany(form.getCompany());
        user.setAdress(form.getAddress());
        user.setPSC(form.getPsc());
        user.setCountry(form.getCountry());
        user.setPhone(form.getPhone());

        user.setCity(form.getCity());

        user.setDateOfBirth(Tools.formatDate(form.getDateOfBirth()));
        user.setSexMale(form.getSexMale().booleanValue());
        user.setPhoto(form.getPhoto());
        user.setSignature(form.getSignature());


        if (form instanceof UserDetailsEntity) {
            UserDetailsEntity form2 = (UserDetailsEntity)form;

            user.setEmail(form2.getEmail());
            user.setLoginName(form2.getLogin());

            user.setAuthorized(form2.getAuthorized().booleanValue());

            user.setAdmin(form2.getAdmin().booleanValue());
            user.setUserGroupsIds(form2.getUserGroupsIds());

            if (user.isEnabledItem("users.edit_admins"))
            {
                user.setEditableGroups(form2.getEditableGroups());
                user.setEditablePages(form2.getEditablePages());
                user.setWritableFolders(form2.getWritableFolders());
            }

            user.setFieldA(form2.getFieldA());
            user.setFieldB(form2.getFieldB());
            user.setFieldC(form2.getFieldC());
            user.setFieldD(form2.getFieldD());
            user.setFieldE(form2.getFieldE());

            user.setPosition(form2.getPosition());
            user.setParentId(form2.getParentId());

            user.setDisabledItemsTable(new Hashtable<>());

        } else if (form instanceof UserDetailsSelfEntity) {
            UserDetailsSelfEntity form2 = (UserDetailsSelfEntity)form;

            user.setEmail(form2.getEmail());
        }

        LogonTools.setUserPerms(user);
        UsersDB.setDisabledItems(user);

        LogonTools.setUserToSession(request.getSession(), user);

        return true;
    }

    /**
     * Validate user password and set errors if required criteria is not met
     * @param entity
     * @param allowWeakPassword
     * @param admin
     * @param prop
     * @param errors
     */
    public void validatePassword(UserDetailsBasic entity, boolean allowWeakPassword, boolean admin, Prop prop, Errors errors) {
        if (UserTools.PASS_UNCHANGED.equals(entity.getPassword())==false && allowWeakPassword==false) {
            //kontrola, ci je heslo validne a splna kriteria

            String password = entity.getPassword();
            int userId = -1;
            if (entity.getId()!=null) userId = entity.getId().intValue();

            String constStr = "";
            if(admin)
            {
                constStr = "Admin";
            }

            int dlzkaHesla = Constants.getInt("password"+constStr+"MinLength");
            int pocetZnakov = Constants.getInt("password"+constStr+"MinCountOfSpecialSigns");
            int pocetVelkychPismen = Constants.getInt("password"+constStr+"MinUpperCaseLetters");
            int pocetCisel = Constants.getInt("password"+constStr+"MinCountOfDigits");

            if(Password.checkPassword(false, password, admin, userId, null, null)==false)
            {
                String errorText = prop.getText("useredit.change_password.nesplna_nastavenia")+"<br/>";
                if(dlzkaHesla > 0)
                    errorText += "- "+prop.getText("logon.change_password.min_length", String.valueOf(dlzkaHesla))+".<br/>";
                if(pocetVelkychPismen > 0)
                    errorText += "- "+prop.getText("logon.change_password.count_of_upper_case", String.valueOf(pocetVelkychPismen))+".<br/>";
                if(pocetCisel > 0)
                    errorText += "- "+prop.getText("logon.change_password.count_of_digits", String.valueOf(pocetCisel))+".<br/>";
                if(pocetZnakov > 0)
                    errorText += "- "+prop.getText("logon.change_password.count_of_special_sign", String.valueOf(pocetZnakov))+".<br/>";
                if(PasswordsHistoryDB.getInstance().existsPassword(password, userId))
                    errorText += "- "+prop.getText("logon.change_password.used_in_history")+".<br/>";

                errors.rejectValue("errorField.password", "403", errorText);
            }
        } else if (Tools.isEmpty(entity.getPassword())) {
            errors.rejectValue("errorField.password", "403", prop.getText("jakarta.validation.constraints.NotBlank.message"));
        }
    }

    /**
     * Normally users are global for all domains.
     * - they are split in MultiWeb installation
     * - or when conf usersSplitByDomain is set to true (mainly for autotest purposes)
     */
    public static boolean isUsersSplitByDomain() {
        return Constants.getBoolean("usersSplitByDomain") || InitServlet.isTypeCloud();
    }

    /*
     * Check if user is authorized AND can login
     * @param userDetails
     * @return true if user is disabled
     */
    public static boolean isUserDisabled(UserDetails userDetails) {
        if(userDetails == null) return false;

        //isAuthorized ?
        if(userDetails.isAuthorized() == false) return false;

        //can user login ?
        long startL = 0;
        long endL = Long.MAX_VALUE;

        if (Tools.isEmpty(userDetails.getAllowLoginStart()) == false) {
            Date start = Tools.getDateFromString(userDetails.getAllowLoginStart(), "dd.MM.yyyy");
            startL = start.getTime();
        }

        if (Tools.isEmpty(userDetails.getAllowLoginEnd()) == false) {
            Date end = Tools.getDateFromString(userDetails.getAllowLoginEnd(), "dd.MM.yyyy");
            endL = end.getTime()+(60*60*24 * 1000);
        }

        long now = Tools.getNow();

        if (now < startL || now > endL) return true;
        return false;
    }

    /**
     * Check if user is authorized AND can login
     * @param userDetails
     * @return true if user is disabled
     */
    public static boolean isUserDisabled(UserDetailsEntity userDetails) {
        if(userDetails == null) return true;

        //isAuthorized ?
        if(Boolean.FALSE.equals(userDetails.getAuthorized())) return true;

        //can user login ?
        long startL = 0;
        long endL = Long.MAX_VALUE;
        if (userDetails.getAllowLoginStart() != null) startL = userDetails.getAllowLoginStart().getTime();
        if (userDetails.getAllowLoginEnd() != null) endL = userDetails.getAllowLoginEnd().getTime()+(60*60*24 * 1000);
        long now = Tools.getNow();

        if (now < startL || now > endL) return true;

        return false;
    }
}
