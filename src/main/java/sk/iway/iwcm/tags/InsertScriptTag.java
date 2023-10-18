package sk.iway.iwcm.tags;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;

//import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.components.insertScript.InsertScriptBean;
import sk.iway.iwcm.components.insertScript.InsertScriptDB;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;

public class InsertScriptTag extends BodyTagSupport {

	private static final long serialVersionUID = -1;
	private String position = null;

	@Override
	public int doStartTag() throws JspTagException
	{
		try
		{
			StringBuilder outPut = new StringBuilder();
			HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
			int docId = Tools.getDocId(request);
			DocDetails docDetails = DocDB.getInstance().getBasicDocDetails(docId, false);
			int groupId = -1;
			if(docDetails != null)
			{
				groupId = docDetails.getGroupId();
			}
			if(Tools.isNotEmpty(position) && groupId != -1)
			{
				Date now = new Date(Tools.getNow());
				List<InsertScriptBean> listInsertScript = InsertScriptDB.getInstance().filter(null, position, null, docId, groupId, now, now);

				for (InsertScriptBean isb : listInsertScript) {
					//Logger.debug(InsertScriptTag.class,"listInsertScript.size(): {}", listInsertScript.size());
					if (!Tools.canSetCookie(isb.getCookieClass(), request.getCookies())) {
						//Logger.debug(this,"continue: "+listInsertScript.get(i).getCookieClass());
						continue;
					}
					if (outPut.length()>0) outPut.append("\n");
					outPut.append(isb.getScriptBody());
				}

			}
			if(outPut.length() > 0)
				pageContext.getOut().write(outPut.toString());

		}
		catch (IOException e)
		{
			throw new JspTagException("InsertScriptTag: " +	e.getMessage());
		}
		return EVAL_PAGE;
	}


	public String getPosition() {
		return position;
	}

	public void setPosition(String position) {
		this.position = position;
	}
}
