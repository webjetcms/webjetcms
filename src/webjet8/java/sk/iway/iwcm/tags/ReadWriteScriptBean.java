package sk.iway.iwcm.tags;

import javax.servlet.jsp.tagext.BodyContent;

import sk.iway.iwcm.Tools;

/**
 *  ReadWriteScriptBean.java
 *
 *  #20624 - WebJET - Dynamicke vkladanie skriptov do hlavicky
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2016
 *@author       $Author: jeeff prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 20.10.2016 8:18:30
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ReadWriteScriptBean {
	private String type = null;
	private String src = null;
	private String rel = null;
	private String href = null;
	private String flush = null;
	private String body = null;
	private String mode = null;
	private String media = null;
	private String jspFileName = null;
	private String charset = null;
	private String id = null;


	public ReadWriteScriptBean(String type, String src, String rel, String href, String flush, BodyContent bodyContent, String mode, String media, String jspFileName, String charset, String id)
	{
		super();
		this.type = type;
		this.src = src;
		this.rel = rel;
		this.href = href;
		this.flush = flush;
		this.mode = mode;
		this.media = media;
		this.jspFileName = jspFileName;
		this.charset = charset;
		this.id = id;
		if(bodyContent != null && bodyContent.getString() != null)
			this.body = bodyContent.getString();
	}

	public ReadWriteScriptBean(String type, String src, String rel, String href, String flush, String bodyContent, String mode, String media, String jspFileName)
	{
		super();
		this.type = type;
		this.src = src;
		this.rel = rel;
		this.href = href;
		this.flush = flush;
		this.mode = mode;
		this.media = media;
		this.jspFileName = jspFileName;
		if(Tools.isNotEmpty(bodyContent))
			this.body = bodyContent;
	}

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getSrc() {
		return src;
	}
	public void setSrc(String src) {
		this.src = src;
	}
	public String getRel() {
		return rel;
	}
	public void setRel(String rel) {
		this.rel = rel;
	}
	public String getHref() {
		return href;
	}
	public void setHref(String href) {
		this.href = href;
	}
	public String getFlush() {
		return flush;
	}
	public void setFlush(String flush) {
		this.flush = flush;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getMedia() {
		return media;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	public String getJspFileName() {
		return jspFileName;
	}

	public void setJspFileName(String jspFileName) {
		this.jspFileName = jspFileName;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}


}
