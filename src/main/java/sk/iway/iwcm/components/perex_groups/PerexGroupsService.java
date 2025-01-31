package sk.iway.iwcm.components.perex_groups;

import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.doc.PerexGroupBean;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.cluster.ClusterDB;

import java.util.List;

import javax.validation.ConstraintViolationException;

public class PerexGroupsService {

    /**
     * Get all perex groups via repo
     * @return
     */
    public static List<PerexGroupsEntity> getPerexGroups() {
        PerexGroupsRepository perexGroupsRepository = Tools.getSpringBean("perexGroupsRepository", PerexGroupsRepository.class);
        return perexGroupsRepository.findAllByOrderByPerexGroupNameAsc();
    }

    /**
     * Greate new perex group and save it
     * @param groupId
     * @param groupName
     * @param availableGroups
     */
    public static PerexGroupsEntity savePerexGroup(int groupId, String groupName, String availableGroups) {
        PerexGroupsRepository perexGroupsRepository = Tools.getSpringBean("perexGroupsRepository", PerexGroupsRepository.class);

        //Prepare entity
        PerexGroupsEntity perexGroupsEntity = new PerexGroupsEntity();
        perexGroupsEntity.setId( Long.valueOf(groupId) );
        perexGroupsEntity.setPerexGroupName(groupName);
        perexGroupsEntity.setAvailableGroups(availableGroups);

        return save(perexGroupsEntity, perexGroupsRepository);
    }

    /**
     * Delete perex group by id
     * @param id
     */
    public static void deletePerexGroup(int id) {
        PerexGroupsRepository perexGroupsRepository = Tools.getSpringBean("perexGroupsRepository", PerexGroupsRepository.class);
        perexGroupsRepository.deleteById(Long.valueOf(id));
    }

    /**
     * Create multiple perex group docs, where id (docId) is same and perexGroupId is from prerexGroupIds arr.
     * Create number of perexGroupDocs for each perexGroupId in prerexGroupIds, IF perexGroupId is number bigger than 0.
     * @param docId - docId
     * @param prerexGroupIds - array of perexGroupIds
     */
    public static void insertPerexGroupDocs(int docId, String[] prerexGroupIds) {
        PerexGroupDocRepository perexGroupDocRepository = Tools.getSpringBean("perexGroupDocRepository", PerexGroupDocRepository.class);

        for (String perexGroupId : prerexGroupIds) {
            if(Tools.getIntValue(perexGroupId, -1) > 0) {
                //Prepare entity
                PerexGroupDocEntity perexGroupDocEntity = new PerexGroupDocEntity();
                perexGroupDocEntity.setDocId(Long.valueOf(docId));
                perexGroupDocEntity.setPerexGroupId(Long.valueOf(perexGroupId));
                perexGroupDocRepository.save(perexGroupDocEntity);
            }
        }
    }

    /**
     * Delete all perex group docs by perexGroupId
     * @param perexGroupId
     */
    public static void deletePerexGroupDocsByPerexGroupId(int perexGroupId)  {
        PerexGroupDocRepository perexGroupDocRepository = Tools.getSpringBean("perexGroupDocRepository", PerexGroupDocRepository.class);
        perexGroupDocRepository.deleteAllByPerexGroupId(Long.valueOf(perexGroupId));
    }

    /**
     * Delete all perex group docs by docId
     * @param docId
     */
    public static void deletePerexGroupDocsByDocId(int docId)  {
        PerexGroupDocRepository perexGroupDocRepository = Tools.getSpringBean("perexGroupDocRepository", PerexGroupDocRepository.class);
        perexGroupDocRepository.deleteAllByDocId(Long.valueOf(docId));
    }

    /**
     * Save perex groups for doc, detects changes and update db
     * @param docId
     * @param perexGroupIds
     */
    public static void savePerexGroupsDoc(int docId, int[] perexGroupIds) {
        PerexGroupDocRepository perexGroupDocRepository = Tools.getSpringBean("perexGroupDocRepository", PerexGroupDocRepository.class);

        List<PerexGroupDocEntity> perexGroupDocs = perexGroupDocRepository.findAllByDocId(Long.valueOf(docId));
        //delete not found in perexGroupIds
        for (PerexGroupDocEntity perexGroupDoc : perexGroupDocs) {
            boolean found = false;
            for (int perexGroupId : perexGroupIds) {
                if (perexGroupDoc.getPerexGroupId().intValue() == perexGroupId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                perexGroupDocRepository.delete(perexGroupDoc);
            }
        }
        //insert not found/new in perexGroupIds
        for (int perexGroupId : perexGroupIds) {
            boolean found = false;
            for (PerexGroupDocEntity perexGroupDoc : perexGroupDocs) {
                if (perexGroupDoc.getPerexGroupId().intValue() == perexGroupId) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                PerexGroupDocEntity perexGroupDocEntity = new PerexGroupDocEntity();
                perexGroupDocEntity.setDocId(Long.valueOf(docId));
                perexGroupDocEntity.setPerexGroupId(Long.valueOf(perexGroupId));
                perexGroupDocRepository.save(perexGroupDocEntity);
            }
        }
    }

    public static PerexGroupsEntity save(PerexGroupsEntity entity, PerexGroupsRepository perexGroupsRepository) {
        DocDB docDB = DocDB.getInstance();
        String availableGroups = entity.getAvailableGroups();
        if (InitServlet.isTypeCloud() && Tools.isEmpty(availableGroups))
		{
			availableGroups = String.valueOf(CloudToolsForCore.getDomainId());
		}

        Prop prop = Prop.getInstance();

		PerexGroupsEntity saved = null;
		boolean found = false;
		StringBuilder ulozeneAdresare = new StringBuilder();
        if (Tools.isNotEmpty(entity.getPerexGroupName()))
        {
            if( InitServlet.isTypeCloud() ) {
                //filter available groups from only current domain

                if(Tools.isEmpty(availableGroups)) {
                    availableGroups = CloudToolsForCore.getDomainId() + "";
                } else {
                    Logger.debug(PerexGroupsService.class, " Removing availableGroups [ "+availableGroups+" ] from other domains");
                    GroupDetails gd = null;
                    int[] newAvailableGroupsCd = Tools.getTokensInt(availableGroups, ",");
                    availableGroups = ","+availableGroups+",";
                    boolean removeGroup = false;
                    for(int avgGrup : newAvailableGroupsCd)
                    {
                        removeGroup = false;
                        if((gd = GroupDetails.getById(avgGrup)) != null )
                        {
                            if (!gd.getDomainName().equalsIgnoreCase(CloudToolsForCore.getDomainName()))
                                removeGroup = true;  //vymazeme cislo zo zoznamu, pretoze je z inej domeny
                        }
                        else
                            removeGroup = true;	//je null (nepatri do ziadnej domeny) nema tu co hladat

                        if(removeGroup)
                            availableGroups = Tools.replace(availableGroups, ","+avgGrup+",", ",");
                    }
                    //odstranim pridane ciarky
                    availableGroups = availableGroups.substring(1,availableGroups.length());
                    //ak bolo v availableGroups iba jedno cislo a bolo zmazane, neostala tam uz ziadna ciarka
                    if(availableGroups.length() > 0) availableGroups = availableGroups.substring(0,availableGroups.length()-1);
                }
            }
            int groupId = -1;
            if (entity.getId() != null) {
                groupId = entity.getId().intValue();
            }
            PerexGroupBean pg = docDB.getPerexGroup(groupId, null);

            for(PerexGroupBean pgBean : docDB.getPerexGroups())
            {
                //duplicitu kontrolujem pri novej perex skupine, alebo editacii perex skupiny s ostatnymi skupinami
                if((pg == null || pg.getPerexGroupId() != pgBean.getPerexGroupId()) && pgBean.getPerexGroupName().equalsIgnoreCase(entity.getPerexGroupName().trim()))
                {
                    //ak naslo rovnaky nazov perex skupiny, skontrolujem este aj zhodnost skupin
                    int[] pgBeanAvailableGroupsInt = pgBean.getAvailableGroupsInt();
                    int[] newAvailableGroups = Tools.getTokensInt(availableGroups, ",");
                    GroupsDB groupsDB = GroupsDB.getInstance();

                    //ak je zadane pre vsetky adresare, tak je zhoda
                    if(pgBeanAvailableGroupsInt.length == 0 || newAvailableGroups.length == 0)
                    {
                        found = true;
                        if(pgBeanAvailableGroupsInt.length == 0)
                        {
                            ulozeneAdresare.delete(0, ulozeneAdresare.length());
                            ulozeneAdresare.append(prop.getText("editor.perex_group.vsetky"));
                        }
                        else
                        {
                            for(int groupIdTmp : pgBeanAvailableGroupsInt)
                                ulozeneAdresare.append(groupsDB.getPath(groupIdTmp)).append(", ");
                            if(Tools.isNotEmpty(ulozeneAdresare)){
                                ulozeneAdresare = new StringBuilder(ulozeneAdresare.substring(0, ulozeneAdresare.length()-2));
                            }
                        }
                        break;
                    }
                    else
                    {
                        for(int groupIdTmp : pgBeanAvailableGroupsInt)
                        {
                            if(DocDB.isGroupAvailable(newAvailableGroups, groupsDB.getParentGroups(groupIdTmp)))
                            {
                                found = true;
                                break;
                            }
                        }
                        if(found)
                        {
                            for(int groupIdTmp : pgBeanAvailableGroupsInt)
                                ulozeneAdresare.append(groupsDB.getPath(groupIdTmp)).append(", ");
                            if(Tools.isNotEmpty(ulozeneAdresare))
                                ulozeneAdresare = new StringBuilder(ulozeneAdresare.substring(0, ulozeneAdresare.length()-2));
                            break;
                        }
                    }

                }
            }

            //Logger.println(this,"UPDATE: " +groupName+ "  " +groupId);

            if (!found) {
                entity.setAvailableGroups(availableGroups);
                saved = perexGroupsRepository.save(entity);
                docDB.getPerexGroups(true);
            } else {
                saved = null;
                throw new ConstraintViolationException(prop.getText("editor.perex_group.skupina_je_uz_definovana", ulozeneAdresare.toString()), null);
            }
        }


		if (saved != null) {
			ClusterDB.addRefresh(PerexGroupsService.class);
		}

		return saved;
    }
}
