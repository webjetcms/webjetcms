package sk.iway.iwcm.doc.clone_structure;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.PkeyGenerator;
import sk.iway.iwcm.RequestBean;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.components.structuremirroring.GroupMirroringServiceV9;
import sk.iway.iwcm.components.structuremirroring.MirroringService;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.system.ConfDB;
import sk.iway.iwcm.system.spring.events.WebjetEvent;
import sk.iway.iwcm.system.spring.events.WebjetEventType;
import sk.iway.iwcm.users.UsersDB;

public class CloneStructureService {

    private static final String CONFIG_KEY = "structureMirroringConfig";
    private static final String DISABLE_ON_CREATE_KEY = "structureMirroringDisabledOnCreate";

    private CloneStructureService() {
        //Utility class
    }

    /**
     * Check if "structureMirroringConfig" conf contain combination of entered id's.
     * If yes, this folder are allready set for mirroring -> do nothing.
     * If no, add to this conf combination of entered id's + current domain comment.
     *
     * @param srcGroupId
     * @param destGroupId
     * @param domainName - name of current domain
     */
	private static void setMirroringConfig(int srcGroupId, int destGroupId, String domainName) {
		String mirroringConfig = Constants.getString(CONFIG_KEY);

		if(mirroringConfig.isEmpty()==false) {
			String[] lines = Tools.getTokens(mirroringConfig, "\n");
			for(String line : lines) {
				String stringIds = "";
				int i = line.indexOf(":");

         		if (i > 0) stringIds = line.substring(0, i);

				String[] ids = stringIds.split(",");

				if(Tools.containsOneItem(ids, ""+srcGroupId)) {
                    //mapping is allready set
                    if (Tools.containsOneItem(ids, ""+destGroupId)) return;

                    //add mapping to this line
					String newLine = line.substring(0, i) + "," + destGroupId + line.substring(i);
                    mirroringConfig = mirroringConfig.replace(line, newLine);
                    Constants.setString(CONFIG_KEY, mirroringConfig);
                    return;
				}
			}
		}

		//NO, this mirroring combination is not set YET -> DO IT
        if (mirroringConfig.isEmpty()==false) mirroringConfig += "\n";
		mirroringConfig += srcGroupId + "," + destGroupId + ":" + domainName;

        Constants.setString(CONFIG_KEY, mirroringConfig);
	}

    /**
     * Check if GroupDetails entity contain's sync_id.
     * If yes -> do nothing.
     * If no -> generate new sync_id and set it into DB via SimpleQuery.
     *
     * @param group
     * @param domainName - name of current domain
     */
	private static int setSyncId(GroupDetails group, String domainName) {
        int syncId = group.getSyncId();
        if(syncId < 1) {
            syncId = PkeyGenerator.getNextValue("structuremirroring");
            if (Constants.getBoolean("multiDomainEnabled")) {
                (new SimpleQuery()).execute("UPDATE groups SET sync_id=? WHERE group_id=? AND domain_name=?", syncId, group.getGroupId(), domainName);
            } else {
                (new SimpleQuery()).execute("UPDATE groups SET sync_id=? WHERE group_id=?", syncId, group.getGroupId());
            }
            group.setSyncId(syncId);
            GroupDetails cached = GroupsDB.getInstance().getGroup(group.getGroupId());
            if (cached != null) cached.setSyncId(syncId);
        }
        return syncId;
    }

    /**
     * Check if DocDetails entity contain's sync_id.
     * If yes -> do nothing.
     * If no -> generate new sync_id and set it into DB via SimpleQuery.
     *
     * @param doc
     */
    private static int setSyncId(DocDetails doc) {
        int syncId = doc.getSyncId();
        if(syncId < 1) {
            syncId = PkeyGenerator.getNextValue("structuremirroring");
            (new SimpleQuery()).execute("UPDATE documents SET sync_id=? WHERE doc_id=?", syncId, doc.getDocId());
            doc.setSyncId(syncId);
        }
        return syncId;
    }

    private static void println(PrintWriter out, String text)
	{
		if (out != null)
		{
			out.println(text + "<br>");
            out.flush();
		}
		else
		{
			Logger.println(CloneStructureService.class, text);
		}
	}

    /**
     * Clone structure of group and all subgroups and docs.
     * @param srcGroupId - ID of source group
     * @param destGroupId - ID of destination group
     * @param keepMirroring - keep mirroring config after cloning
     * @param keepVirtualPath - keep virtual path during cloning
     * @param request
     * @param response
     * @return - String of HTML response
     * @throws IOException
     * @throws ServletException
     */
	public static String cloneStructure(int srcGroupId, int destGroupId, boolean keepMirroring, boolean keepVirtualPath, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		String domainName = CloudToolsForCore.getDomainName();
        Prop prop = Prop.getInstance(request);
        HttpSession session = request.getSession();

        //Prepare DB instances
        GroupsDB groupsDB = GroupsDB.getInstance();
        DocDB docDB = DocDB.getInstance();

        //Check that id's and domain are present
        if(srcGroupId < 1 || destGroupId < 1 || domainName.isEmpty()) {
            handleError(response, session, prop, "datatable.error.unknown", "srcGroupId OR destGroupId invalid value");
            return null;
        }

        //Obtain and check source group to clone
        GroupDetails srcGroup = groupsDB.getGroup(srcGroupId);
        GroupDetails destGroup = groupsDB.getGroup(destGroupId);
        if(srcGroup == null || destGroup == null) {
            handleError(response, session, prop, "datatable.error.unknown", "srcGroup OR destGroup not found");
            return null;
        }

        if(Constants.getBoolean("multiDomainEnabled") == true) {
            //Need to check if ids are domain safe
            if(domainName.equals(srcGroup.getDomainName()) == false || domainName.equals(destGroup.getDomainName()) == false) {
                handleError(response, session, prop, "admin.editor_dir.dontHavePermsForThisDir", "srcGroup OR destGroup belongs to another domain");
                return null;
            }
        }

        //Perms check for groups
        Identity user = UsersDB.getCurrentUser(request);
        if(GroupsDB.isGroupEditable(user,srcGroupId) == false || GroupsDB.isGroupEditable(user,destGroupId) == false) {
            handleError(response, session, prop, "admin.editor_dir.dontHavePermsForThisDir", "user have not right for group's");
            return null;
        }

        //Constants that are needed in Mirror logic to chnage how we do thinks
        setCloneConstants(true, srcGroupId, destGroupId);

        //save parameters to requestBean to use in services
        RequestBean.addParameter("keepVirtualPath", ""+keepVirtualPath);
        RequestBean.addParameter("srcGroupId", ""+srcGroupId);
        RequestBean.addParameter("destGroupId", ""+destGroupId);

        int srcSyncId = GroupMirroringServiceV9.getSyncId(srcGroupId);

		//Set id's to mirroring (if they are not set allready)
        String mirroringConfig = Constants.getString(CONFIG_KEY);
		setMirroringConfig(srcGroupId, destGroupId, domainName);

        sk.iway.iwcm.Encoding.setResponseEnc(request, response, "text/html");
        request.setAttribute("iconLink", "");
        request.setAttribute("dialogTitle", prop.getText("admin.clone.dialogTitle"));
        request.setAttribute("dialogDesc", prop.getText("admin.clone.dialogDesc")+ ".");
        request.getRequestDispatcher("/admin/layout_top_dialog.jsp").include(request, response);

        PrintWriter out = response.getWriter();
        out.println("<div class=\"padding10\">");
        out.println("<h2>"+prop.getText("components.clone.cloning_please_wait")+"</h2>");
        out.flush();
        println(out, prop.getText("components.clone.copy_dir") + ": " + srcGroupId + " -> " + destGroupId);

        //disable structure mirroring available=false on create
        boolean structureMirroringDisabledOnCreate = Constants.getBoolean(DISABLE_ON_CREATE_KEY);
        Constants.setBoolean(DISABLE_ON_CREATE_KEY, false);

        try {
            //Get all group's that belongs under source group (even with source group)
            List<GroupDetails> groups = groupsDB.getGroupsTree(srcGroupId, true, true);
            for(GroupDetails nextGroup : groups) {

                println(out, "<br/>"+prop.getText("components.clone.creating_dir") + ": " + nextGroup.getGroupIdName());

                //If group is present, set sync_id (if it's needed)
                setSyncId(nextGroup, domainName);

                //Publish event -> fake GROUP editing, so creating clone action of group will be performed by MIRRORING logic
                (new WebjetEvent<GroupDetails>(nextGroup, WebjetEventType.AFTER_SAVE)).publishEvent();

                //Get all doc's (web pages) that belongs under current looped group
                List<DocDetails> groupDocs = docDB.getDocByGroup( nextGroup.getGroupId(), DocDB.ORDER_ID, true, -1, -1, false, false);
                for(DocDetails groupDoc : groupDocs) {

                    println(out, "&nbsp;&nbsp;&nbsp;" + prop.getText("components.clone.creating_page") + ": " + groupDoc.getDocId() + " " + groupDoc.getTitle());

                    //If doc is present, set sync_id (if it's needed)
                    setSyncId(groupDoc);

                    //Publish event -> fake DOC editing, so creating clone action of doc will be performed by MIRRORING logic
                    (new WebjetEvent<DocDetails>(groupDoc, WebjetEventType.AFTER_SAVE)).publishEvent();
                }
            }
        } catch (Exception e) {
            Logger.error(e);
            println(out, "ERROR: "+e.getLocalizedMessage());

            //Reset values
            setCloneConstants(false, -1, -1);
        }

        Constants.setBoolean(DISABLE_ON_CREATE_KEY, structureMirroringDisabledOnCreate);
        if (keepMirroring) {
            //use current mirroring config updated before cloning
            mirroringConfig = Constants.getString(CONFIG_KEY);
            //Set it to DB
            ConfDB.setName(CONFIG_KEY, mirroringConfig);
        } else {
            //set original mirroring config
            Constants.setString(CONFIG_KEY, mirroringConfig);

            //it there was no syncId on sourceFolder clear also source
            if (srcSyncId<1) MirroringService.clearSyncId(srcGroupId);
            MirroringService.clearSyncId(destGroupId);
        }

        //Reset values
        setCloneConstants(false, -1, -1);

        DocDB.getInstance(true);
        GroupsDB.getInstance(true);

        out.println("<br/><br/><hr/><strong>"+prop.getText("components.clone.done")+"</strong>");
        out.println("<script type='text/javascript'>window.opener.location.reload(); function Ok() {window.close();} </script>");
        out.print("</div>");
        request.getRequestDispatcher("/admin/layout_bottom_dialog.jsp").include(request, response);

		return null;
	}

    private static void handleError(HttpServletResponse response, HttpSession session, Prop prop, String errMsgKey, String debugMsg) throws IOException{
        session.setAttribute("err_msg", prop.getText(errMsgKey));
        response.sendRedirect("/components/clone_structure/clone_structure.jsp");
        Logger.debug(CloneStructureService.class, "cloneStructure() - " + debugMsg);
    }

    private static void setCloneConstants(boolean isCloneAction, int srcId, int destId) {
        if(isCloneAction == true) Constants.setString("mirroringMode", MirroringService.MIRRORING_MODE_CLONE);
        else Constants.setString("mirroringMode", MirroringService.MIRRORING_MODE_MIRROR);

        Constants.setInt("cloneActionSrcId", srcId);
        Constants.setInt("cloneActionDestId", destId);
    }
}
