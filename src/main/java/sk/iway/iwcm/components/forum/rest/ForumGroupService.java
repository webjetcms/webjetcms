package sk.iway.iwcm.components.forum.rest;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.forum.jpa.DocForumEntity;
import sk.iway.iwcm.components.forum.jpa.ForumGroupEntity;
import sk.iway.iwcm.components.forum.jpa.ForumGroupRepository;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.TemplateDetails;
import sk.iway.iwcm.doc.TemplatesDB;
import sk.iway.iwcm.i18n.Prop;

public class ForumGroupService {

    private ForumGroupService() {}

    private static final String USER_GROUPS_DELIMITER = "+";

    private static boolean dataHasForumText(String data)  {
        if (data.indexOf("/forum/forum_mb")!=-1 || data.indexOf("forum_mb.jsp")!=-1) {
			if (data.indexOf("type=topics") != -1 || data.indexOf("rootGroup=") != -1 || data.indexOf("bbRootGroupId=") != -1) {
				return true;
            }
        }

		if (data.indexOf("/forum/forum") != -1 && ( data.indexOf("type=topics") != -1 || data.indexOf("bbRootGroupId=") != -1) ) {
			return true;
        }

		return false;
    }

    public static boolean isMessageBoard(DocDetails doc, TemplateDetails temp) {
        if( dataHasForumText(doc.getData()) ) return true;
        if( dataHasForumText(temp.getHeaderDocData()) ) return true;
        if( dataHasForumText(temp.getFooterDocData()) ) return true;
        if( dataHasForumText(temp.getMenuDocData()) ) return true;
        if( dataHasForumText(temp.getRightMenuDocData()) ) return true;
        if( dataHasForumText(temp.getObjectADocData()) ) return true;
        if( dataHasForumText(temp.getObjectBDocData()) ) return true;
        if( dataHasForumText(temp.getObjectCDocData()) ) return true;
        if( dataHasForumText(temp.getObjectDDocData()) ) return true;

        return false;
    }

    public static void prepareForumGroup(DocForumEntity docForumEntity) {
        //Value check
        Integer docId = docForumEntity.getDocId();
        if(docForumEntity == null || docId < 1) return;

        Prop prop = Prop.getInstance();
		ForumGroupRepository forumGroupRepository = getForumGroupRespository();
		Optional<ForumGroupEntity> forumGroupEntityOpt = forumGroupRepository.findFirstByDocIdOrderById(docId);

        ForumGroupEntity forumGroupEntity;
		if(forumGroupEntityOpt.isPresent())
			forumGroupEntity = forumGroupEntityOpt.get();
		else {
			forumGroupEntity = new ForumGroupEntity();
            forumGroupEntity.setDocId(docId);
            forumGroupEntity.setActive(true);
            forumGroupEntity.setHoursAfterLastMessage(0);

            //Find out, if this is message board
            DocDetails doc = DocDB.getInstance().getDoc(docId);
            if(doc != null) {
                TemplatesDB tempDB = TemplatesDB.getInstance();
				TemplateDetails temp = tempDB.getTemplate(doc.getTempId());
				if (temp == null)
					temp = new TemplateDetails();

				forumGroupEntity.setMessageBoard( isMessageBoard(doc, temp) );
            }
		}

        //
		if(Tools.isTrue(forumGroupEntity.getActive())) forumGroupEntity.setForumStatus( prop.getText("components.forum.status_true") );
		else forumGroupEntity.setForumStatus( prop.getText("components.forum.status_false"));

		//
		if(Boolean.TRUE.equals(forumGroupEntity.getMessageBoard())) {
            forumGroupEntity.setForumType( prop.getText("components.forum.admin.forumType.mb") );

            //Take string of id's adminGroups
            // -> convert it to int[] (using Tools.getTokensInt)
            // -> this array convert to Integer[] and save into adminPerms (using Arrays.stream)
            if(!Tools.isEmpty( forumGroupEntity.getAdminGroups() ))
                forumGroupEntity.setAdminPerms(
                    Arrays.stream(
                        Tools.getTokensInt( forumGroupEntity.getAdminGroups(), USER_GROUPS_DELIMITER)
                    ).boxed().toArray( Integer[]::new )
                );
        } else forumGroupEntity.setForumType( prop.getText("components.forum.admin.forumType.simple") );

        //Take string of id's add messageGroups
		// -> convert it to int[] (using Tools.getTokensInt)
		// -> this array convert to Integer[] and save into addMessagePerms (using Arrays.stream)
		if(!Tools.isEmpty( forumGroupEntity.getAddmessageGroups() ))
			forumGroupEntity.setAddMessagePerms(
				Arrays.stream(
					Tools.getTokensInt( forumGroupEntity.getAddmessageGroups(), USER_GROUPS_DELIMITER)
				).boxed().toArray( Integer[]::new )
			);

        docForumEntity.setForumGroupEntity(forumGroupEntity);
    }

    /**
     * Create forum group after webpage page is saved, so user can immediately change forum settings
     * @param docId
     * @param pageData
     */
    public static void createForumAfterPage(int docId, String pageData) {
        if (Tools.isEmpty(pageData)) return;

        if(pageData.contains("/forum/forum.jsp") || pageData.contains("/forum/forum_mb.jsp")) {
            ForumGroupRepository forumGroupRepository = getForumGroupRespository();

            ForumGroupEntity entity = forumGroupRepository.findFirstByDocIdOrderById(docId).orElse(null);

            if(entity == null) {
                entity = new ForumGroupEntity();
                entity.setDocId(docId);
                entity.setActive(true);
                entity.setDomainId( CloudToolsForCore.getDomainId() );
                entity.setMessageConfirmation(false);
                entity.setHoursAfterLastMessage(0);
                entity.setMessageBoard( pageData.contains("/forum/forum_mb.jsp") );
            }

            Pattern pattern = Pattern.compile("notifyPageAuthor=([^,)]+)");
            Matcher matcher = pattern.matcher(pageData);
            boolean notifyPageAuthor = false;
            if(matcher.find()) notifyPageAuthor = Tools.getBooleanValue(matcher.group(1), false);
            entity.setNotifyPageAuthor(notifyPageAuthor);

            forumGroupRepository.save(entity);
        }
    }

    public static void saveForum(ForumGroupEntity entityToSave) {
        ForumGroupRepository forumGroupRepository = getForumGroupRespository();

        /* Convert selected user groups to string */

        //!! only message board can select admin users
        if(Tools.isTrue(entityToSave.getMessageBoard())) {
            StringBuilder adminPermsString = new StringBuilder("");
            boolean first = true;
            for(Integer userGroupId : entityToSave.getAdminPerms()) {
                if(first) {
                    first = false;
                    adminPermsString.append(userGroupId);
                } else adminPermsString.append("+").append(userGroupId);
            }

            //Set this string into entity
            entityToSave.setAdminGroups(adminPermsString.toString());
        } else {
            //This params can be set if forum type != message board
            entityToSave.setAdminGroups(null);
            entityToSave.setAdvertisementType(false);
        }

        //Both types can set add message perms
        boolean first = true;
        StringBuilder addMessagePermsString = new StringBuilder("");
        for(Integer userGroupId : entityToSave.getAddMessagePerms()) {
            if(first) {
                first = false;
                addMessagePermsString.append(userGroupId);
            } else addMessagePermsString.append("+").append(userGroupId);
        }

        //Set this string into entity
        entityToSave.setAddmessageGroups(addMessagePermsString.toString());

        // "null" string fix !!
        if("null".equals(entityToSave.getAdminGroups())) entityToSave.setAdminGroups("");
        if("null".equals(entityToSave.getAddmessageGroups())) entityToSave.setAddmessageGroups("");

        //Set domainId !
        entityToSave.setDomainId( CloudToolsForCore.getDomainId() );

        //Save this entity
        forumGroupRepository.save(entityToSave);

        //If it's new entity, set generated ID from DB
        if(entityToSave.getId() < 1)
            entityToSave.setId(
                Long.valueOf(
                    new SimpleQuery().forInt("SELECT max(id) FROM forum WHERE doc_id = ?" + CloudToolsForCore.getDomainIdSqlWhere(true), entityToSave.getDocId())
                )
            );
    }

	public static ForumGroupEntity getForum(int docId, boolean returnNull) {
        ForumGroupRepository forumGroupRepository = getForumGroupRespository();

		//Forum group aka FORUM
		Optional<ForumGroupEntity> forum = forumGroupRepository.findFirstByDocIdOrderById(docId);

		//Check if value is present
		if(forum.isPresent()) return forum.get();
		else if(!returnNull) {
			//Value is null but we dont want null value -> create default one
			ForumGroupEntity defaultForum = new ForumGroupEntity();
			defaultForum.setMessageConfirmation(false);
			defaultForum.setActive(true);
            defaultForum.setAdminGroups("");
            defaultForum.setAddmessageGroups("");
			return defaultForum;
		}

		//Return null
		return null;
	}

    public static boolean isActive(int docId) {
        boolean ret = true;

        if (docId > 0) {
            ForumGroupEntity fge = getForum(docId, true);
            if (fge != null) {
                //Check active param
                if (Tools.isFalse(fge.getActive())) ret = false;

                //Check is we are NOW between FROM - TO range
                if(fge.getDateFrom() != null && Tools.getNow() <= fge.getDateFrom().getTime())
                    ret = false;

                if(fge.getDateTo() != null && Tools.getNow() >= fge.getDateTo().getTime())
                    ret = false;

                //Now check, if forum have set HoursAfterLastMessage param
                //If yes, check that this amount of hour haven't passed since QuestionDate
                if(fge.getHoursAfterLastMessage() > 0) {
                    Date questionDate = (new SimpleQuery()).forDate("SELECT question_date FROM document_forum WHERE doc_id=? " + CloudToolsForCore.getDomainIdSqlWhere(true) + "ORDER BY question_date DESC", docId);
                    if(questionDate != null) {
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(questionDate);
                        cal.add(Calendar.HOUR_OF_DAY, fge.getHoursAfterLastMessage());
                        //
                        if (Tools.getNow() < cal.getTimeInMillis()) ret = false;
                    }
                }
            }
        }
        return ret;
    }

    private static ForumGroupRepository getForumGroupRespository() {
        return Tools.getSpringBean("forumGroupRepository", ForumGroupRepository.class);
    }
}
