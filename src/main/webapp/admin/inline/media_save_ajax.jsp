<%@page import="java.util.List"%><%@ page pageEncoding="utf-8" import="sk.iway.iwcm.*,sk.iway.spirit.*,sk.iway.spirit.model.*,java.util.*" %>

<%@ taglib uri="/WEB-INF/iwcm.tld" prefix="iwcm"%>
<%@ taglib uri="/WEB-INF/iway.tld" prefix="iway"%>
<%@ taglib uri="/WEB-INF/displaytag.tld" prefix="display" %><iwcm:checkLogon admin="true" perms="menuWebpages"/>

<%@page import="sk.iway.iwcm.i18n.Prop"%>
<%@page import="sk.iway.iwcm.users.UsersDB"%><%

Identity user = UsersDB.getCurrentUser(request);
Prop prop = Prop.getInstance(request);

if (user == null || user.isAdmin()==false)
{
	out.print(prop.getText("error.userNotLogged"));
	return;
}


int docId = Tools.getIntValue(Tools.getRequestParameter(request, "docId"),-1);
String act = Tools.getStringValue(Tools.getRequestParameter(request, "act"),"");
String mediaThumbLink = Tools.getStringValue(Tools.getRequestParameter(request, "mediaThumbLink"), "");

if(act.equals("save") && docId>0 && !mediaThumbLink.equals("")){

	Media mf = null;

	//ziskaj max hodnotu aktualneho sortOrder
	List medias = MediaDB.getMedia(session, "documents", docId, null);
	int maxOrder = 0;
	Iterator iter = medias.iterator();
	while (iter.hasNext())
	{
		Media m = (Media)iter.next();
		if (m.getMediaSortOrder()!=null && maxOrder < m.getMediaSortOrder().intValue())
			maxOrder = m.getMediaSortOrder().intValue();
	}
	maxOrder += 10;

	mf = new Media();
	mf.setMediaFkId(docId);
	mf.setMediaFkTableName("documents");
	mf.setMediaSortOrder(new Integer(maxOrder));
	mf.setMediaGroup("eshop");
	mf.setMediaThumbLink(mediaThumbLink);
	mf.save();

}else if(act.equals("delete")){
	int mediaId = Tools.getIntValue(Tools.getRequestParameter(request, "mediaId"),-1);
	if(mediaId>0){
		Media media = MediaDB.getMedia(mediaId);
		if(media!=null){
			media.delete();
		}
	}
}
%>
