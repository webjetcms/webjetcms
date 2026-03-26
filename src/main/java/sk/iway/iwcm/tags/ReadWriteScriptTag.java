package sk.iway.iwcm.tags;

import java.io.IOException;
import java.util.LinkedList;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.jsp.JspTagException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.PageContext;
import jakarta.servlet.jsp.tagext.BodyTagSupport;

import sk.iway.iwcm.Cache;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.PageLng;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.io.IwcmFile;

/**
 *  ReadWriteScript.java
 *
 *@Title        webjet8
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2016
 *@author       $Author: jeeff prau $
 *@version      $Revision: 1.3 $
 *@created      Date: 19.10.2016 10:37:51
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ReadWriteScriptTag extends BodyTagSupport
{
	private static final long serialVersionUID = 1L;

	private String type = null;
	private String src = null;
	private String rel = null;
	private String href = null;
	private String flush = null;
	private String media = null;
//	private static Scriptt JAVASCRIPT = "javascript-source-to-flush";
//	private static String CSS_STYLE = "css-source-to-flush";
//	private static String UNKNOWN = "unknown-source-to-flush";
	//private static String TO_BE_REPLACED_SOURCE = "thisStringWillBeReplacedBySource";
	//private static String TO_BE_REPLACED_REL = "thisStringWillBeReplacedByRel";
	private static final String MODE_ATTR_NAME = "mode_read_write_attr";
	private static final String MODE_NONE = "none";
	private static final String MODE_COMBINED = "combined";
	private String mode = null;//MODE_NONE;
	private String charset = null;

	@Override
	public void release()
	{

		super.release();
		type = null;
		src = null;
		rel = null;
		href = null;
		flush = null;
		mode = null;
		media = null;
		charset = null;
		id = null;
		doPrint("release()");
	}

	public static void addScriptFromClass(ReadWriteScriptBean rwdWriteBean, HttpServletRequest request, JspWriter out)
	{
		rwdWriteBean.getType();
		rwdWriteBean.getSrc();
		rwdWriteBean.getRel();
		rwdWriteBean.getHref();
		rwdWriteBean.getFlush();
		rwdWriteBean.getMode();
		rwdWriteBean.getMedia();
		rwdWriteBean.getCharset();
		rwdWriteBean.getId();

		String mode = MODE_NONE;
		if(request != null  && request.getAttribute(MODE_ATTR_NAME) != null && Tools.isNotEmpty(request.getAttribute(MODE_ATTR_NAME).toString()))
		{
			mode = request.getAttribute(MODE_ATTR_NAME).toString();
		}
		//Logger.debug(ReadWriteScriptTag.class, "mode: " + mode);

		//TODO Pozor treba zmenit aj pri primamom volani triedy pri zmene
		if(Tools.isNotEmpty(rwdWriteBean.getSrc()) && rwdWriteBean.getSrc().toLowerCase().endsWith(".js"))
			addContentFromClass(ScriptTypeEnum.JAVASCRIPT, rwdWriteBean, request, mode, out);
		else if(rwdWriteBean.getType() != null && Tools.isNotEmpty(rwdWriteBean.getType()) && "text/javascript".equalsIgnoreCase(rwdWriteBean.getType()))
			addContentFromClass(ScriptTypeEnum.JAVASCRIPT, rwdWriteBean, request, mode, out);
		else if(Tools.isNotEmpty(rwdWriteBean.getHref()) && rwdWriteBean.getHref().toLowerCase().endsWith(".css"))
			addContentFromClass(ScriptTypeEnum.CSS_STYLE, rwdWriteBean, request, mode, out);
		else if(rwdWriteBean.getType() != null && Tools.isNotEmpty(rwdWriteBean.getType()) && "text/css".equalsIgnoreCase(rwdWriteBean.getType()))
			addContentFromClass(ScriptTypeEnum.CSS_STYLE, rwdWriteBean, request, mode, out);
		else if(Tools.isNotEmpty(rwdWriteBean.getType()) && "image/x-icon".equalsIgnoreCase(rwdWriteBean.getType()))
			addContentFromClass(ScriptTypeEnum.CSS_STYLE, rwdWriteBean, request, mode, out);
		else
			addContentFromClass(ScriptTypeEnum.UNKNOWN, rwdWriteBean, request, mode, out);
	}

	private static void addContentFromClass(ScriptTypeEnum scriptType, ReadWriteScriptBean bean, HttpServletRequest request, String mode, JspWriter out)
	{
		doPrint("addContentFromClass()", bean);
		//ReadWriteScriptBean rwsb = new ReadWriteScriptBean(type, src, rel, href, flush, getBodyContent(), mode, media, getJspFileName(), charset, id);
		LinkedList<ReadWriteScriptBean> listRwsb = getScripts(scriptType, request, null);
		listRwsb.addLast(bean);
		// dalej sa o vypisanie nestaram
		request.setAttribute(scriptType.toString(), listRwsb);


		// vypiseme a zmazeme ak mode none
		if(MODE_NONE.equals(mode))
		{
			String flush = "one_flush";
			//#23413 #2 tu nema zmysel zistovat ci ideme vypisovat css alebo js pretoze je to staticka metoda a mód musi byt vopred nastaveny
			flush(ScriptTypeEnum.CSS_STYLE, null, request, flush, mode, bean.getCharset(), bean.getId(), out, bean.getMedia());
			flush(ScriptTypeEnum.JAVASCRIPT, null, request, flush, mode, bean.getCharset(), bean.getId(), out, bean.getMedia());
		}
	}

	//paths - adresy oddelene ciarkou (cached)
	private static long getTimeStamp(String paths)
	{
		String cacheKey = "ReadWriteScriptTag - "+paths;
		if(Cache.getInstance().getObject(cacheKey) != null)
			return (long) Cache.getInstance().getObject(cacheKey);
		long ts = InitServlet.getServerStartDatetime().getTime();
		for(String path:Tools.getTokens(paths, ","))
		{
			if(FileTools.isFile(path))
			{
				IwcmFile f = new IwcmFile(Tools.getRealPath(path));
				if(ts < f.lastModified())
					ts = f.lastModified();
			}
		}
		Cache.getInstance().setObject(cacheKey, ts, 60);//1 hodina
		return ts;
	}

	public void setMode()
	{
		doPrint("setMode()");
		//vypiseme aky mod mame prave teraz
		if(Tools.isEmpty(mode) && pageContext != null && pageContext.getRequest() != null  && pageContext.getRequest().getAttribute(MODE_ATTR_NAME) != null && Tools.isNotEmpty(pageContext.getRequest().getAttribute(MODE_ATTR_NAME).toString()))
		{
			mode = pageContext.getRequest().getAttribute(MODE_ATTR_NAME).toString();
			doPrint("actual mode:"+mode);
		}

		//nastavime mod z requestu
		if(Tools.isEmpty(mode))
		{
			mode = MODE_NONE;
			doPrint("setMode(2:"+mode+")");
		}
		else
		{
			if (pageContext!=null) {
				pageContext.getRequest().setAttribute(MODE_ATTR_NAME, mode);
				doPrint("setMode(3:"+mode+" / "+pageContext.getRequest().getAttribute(MODE_ATTR_NAME)+")");
			}
		}
	}

	private void write(String text)
	{
		write(text, pageContext, null);
	}

	private static void write(String text, PageContext pageContext, JspWriter out)
	{
		try
		{
			if(text != null)
			{
				if(out == null)
					pageContext.getOut().write(text);
				else
					out.println(text);
			}
		}
		catch (IOException e)
		{
			sk.iway.iwcm.Logger.error(e);
		}
	}

	@Override
	public int doStartTag() throws JspTagException
	{
		doPrint("doStartTag()");
		setMode();
		// iba pre flush atribut
		//#23413 #2
		if("css".equals(flush))
			flush(ScriptTypeEnum.CSS_STYLE);
		else if("js".equals(flush) || "javascript".equals(flush))
			flush(ScriptTypeEnum.JAVASCRIPT);
		else
		{
			flush(ScriptTypeEnum.CSS_STYLE);
			flush(ScriptTypeEnum.JAVASCRIPT);
		}

		if(pageContext.getRequest().getAttribute(ScriptTypeEnum.UNKNOWN.toString()) != null && Tools.isNotEmpty(flush) )
		{
			if(Tools.isEmpty(mode))
			{
				write("\n<!-- START  This code cannot be added-->\n");
				flush(ScriptTypeEnum.UNKNOWN);
				write("\n<!-- END  This code cannot be added-->\n"+getJspFileName());
			}
			else
				write("\n <!--IS mode: "+mode+" -->");
		}
		return EVAL_PAGE;
	}

	@Override
	public int doEndTag() throws JspTagException
	{
		doPrint("doEndTag()");
		setMode();
		//TODO Pozor treba zmenit aj pri primamom volani triedy pri zmene
		if(Tools.isNotEmpty(src) && src.toLowerCase().endsWith(".js"))
			addContent(ScriptTypeEnum.JAVASCRIPT);
		else if(type != null && Tools.isNotEmpty(type) && "text/javascript".equalsIgnoreCase(type))
			addContent(ScriptTypeEnum.JAVASCRIPT);
		else if(Tools.isNotEmpty(href) && href.toLowerCase().endsWith(".css"))
            addContent(ScriptTypeEnum.CSS_STYLE);
		else if(type != null && Tools.isNotEmpty(type) && "text/css".equalsIgnoreCase(type))
        {
            if("base_css_link".equals(href))
            {
                addContent("base_css_link");
            }
            else
            {
                addContent(ScriptTypeEnum.CSS_STYLE);
            }
        }
		else if(type!=null && Tools.isNotEmpty(type) && "image/x-icon".equalsIgnoreCase(type))
			addContent(ScriptTypeEnum.CSS_STYLE);
		else
			addContent(ScriptTypeEnum.UNKNOWN);

		release();
		return EVAL_PAGE;
	}

	private void flush(ScriptTypeEnum scriptType)
	{
		doPrint("flush()");
		setMode();
		//#23413 #2
		if("css".equals(flush))
			flush(ScriptTypeEnum.CSS_STYLE, pageContext, null, flush, mode, charset, id, null, media);
		else if("js".equals(flush) || "javascript".equals(flush))
			flush(ScriptTypeEnum.JAVASCRIPT, pageContext, null, flush, mode, charset, id, null, media);
		else
			flush(scriptType, pageContext, null, flush, mode, charset, id, null, media);
	}

	private static boolean isWritePerfStat(HttpServletRequest request)
    {
        String writePerfStat = request.getParameter("_writePerfStat");
        if(Tools.isNotEmpty(writePerfStat) && "true".equals(writePerfStat))
            return true;
        return false;
    }

	private static void flush(ScriptTypeEnum scriptType, PageContext pageContext, HttpServletRequest request, String flush, String mode, String charset, String id, JspWriter out, String media )
	{
		if(Tools.isEmpty(flush) || getScripts(scriptType, request, pageContext).size() == 0)
			return;

		String rel = null;
		String type = null;
		String lineSeparator = "\n";

		LinkedList<ReadWriteScriptBean> listBeans = getScripts(scriptType, request, pageContext);
		StringBuilder combined = new StringBuilder();

		// prvy cyklus robime len kvoli COMBINE.JSP aby sa vypisal prvy pred inline JS
		ReadWriteScriptBean bean = null;
		for(int i=0;i<listBeans.size();i++)
		//for(int i=listBeans.size()-1;i>=0;i--)
		{
			bean = listBeans.get(i);
			// po vypisani tagu sa pri dalsom moze pouzit iny mod a skript by sa tak vypisal, co bolo zle
			if("one_flush".equals(flush) && ("expanded".equals(bean.getMode()) || "combined".equals(bean.getMode())))
			{
				continue;
			}

			if(MODE_COMBINED.equals(mode))//#23077 #12
            {
                lineSeparator = "";
                if(isWritePerfStat((HttpServletRequest)pageContext.getRequest()))
                {
                    lineSeparator = "\n";
                }
            }

			if(scriptType.equals(ScriptTypeEnum.JAVASCRIPT))
			{
				if(MODE_COMBINED.equals(mode) && Tools.isNotEmpty(bean.getSrc()))
				{
					combined.append(bean.getSrc()).append(",").append(lineSeparator);
					listBeans.remove(i);
					i--; //NOSONAR
				}
			}
			else if(scriptType.equals(ScriptTypeEnum.CSS_STYLE))
			{
				if(Tools.isNotEmpty(bean.getHref()))//external style
				{
					if(MODE_COMBINED.equals(mode))
					{
						combined.append(bean.getHref()).append(",").append(lineSeparator);
						listBeans.remove(i);
						i--; //NOSONAR
					}
				}
			}
		}

		//COMBINED
		if(MODE_COMBINED.equals(mode) && Tools.isNotEmpty(combined))
		{
            if(isWritePerfStat((HttpServletRequest)pageContext.getRequest()))
            {
                combined = new StringBuilder(combined.substring(0, combined.length() - 2));
            }

			if(scriptType.equals(ScriptTypeEnum.CSS_STYLE))
			{
				write(lineSeparator+"<link rel=\"stylesheet\" type=\"text/css\" media=\"all\" href=\"/components/_common/combine.jsp?t=css&amp;f="+lineSeparator+combined+"&amp;v="+getTimeStamp(combined.toString())+"\" />"+getJspFileName(null,pageContext),pageContext, out);
			}
			else if(scriptType.equals(ScriptTypeEnum.JAVASCRIPT))
			{
				write(lineSeparator+"<script type=\"text/javascript\" src=\"/components/_common/combine.jsp?t=js&amp;f="+lineSeparator+combined+"&amp;v="+getTimeStamp(combined.toString())+"&amp;lng="+ PageLng.getUserLng((HttpServletRequest)pageContext.getRequest())+"\" ></script>"+getJspFileName(null, pageContext), pageContext, out);
			}
		}

		combined = new StringBuilder();

		boolean canRemoveAttribute = true;
		//for(int i=listBeans.size()-1;i>=0;i--)
		for(int i=0;i<listBeans.size();i++)
		{
			bean = listBeans.get(i);
			// po vypisani tagu sa pri dalsom moze pouzit iny mod a skript by sa tak vypisal, co bolo zle
			if("one_flush".equals(flush) && ("expanded".equals(bean.getMode()) || "combined".equals(bean.getMode())))
			{
				canRemoveAttribute = false;
				continue;
			}
			//TODO vymazat z listu, nasetovat do atributu a nezmazat attribut

			type = ""; rel = "";
			if(Tools.isNotEmpty(bean.getType()))
				type = " type=\""+bean.getType()+"\"";

			String printCharset = "";
			if(Tools.isNotEmpty(charset))
				printCharset = "charset=\""+charset+"\"";

			String printId = "";
			if(Tools.isNotEmpty(id))
				printId = "id=\""+id+"\"";

			if(scriptType.equals(ScriptTypeEnum.JAVASCRIPT))
			{
				if(MODE_COMBINED.equals(mode) && Tools.isNotEmpty(bean.getSrc()))
					combined.append(bean.getSrc()).append(",").append(lineSeparator);
				else if(Tools.isNotEmpty(bean.getSrc()))//external style
					write("<script "+type+" "+printId+" "+printCharset+" src=\""+bean.getSrc()+"\"></script>"+getJspFileName(bean, pageContext)+"\n",pageContext, out);
				else
					write("<script "+type+">"+bean.getBody()+"</script>"+getJspFileName(bean, pageContext)+"\n",pageContext, out);
			}
			else if(scriptType.equals(ScriptTypeEnum.CSS_STYLE))
			{
				String linkMedia = media;
				if(Tools.isEmpty(linkMedia))
					linkMedia = "";
				else
					linkMedia = " media=\""+bean.getMedia()+"\"";

				if(Tools.isNotEmpty(bean.getHref()))//external style
				{
					if(Tools.isNotEmpty(bean.getRel()))
						rel = " rel=\""+bean.getRel()+"\" ";
					if(MODE_COMBINED.equals(mode))
						combined.append(bean.getHref()).append(",");
					else
						write("<link "+type+" "+printId+" "+ linkMedia+" "+printCharset + " " + rel+" href=\""+bean.getHref()+"\"></link>"+getJspFileName(bean, pageContext)+"\n",pageContext, out);//write("<link type=\"text/css\""+linkMedia+" "+rel+" href=\""+bean.getHref()+"\"></link>"+getJspFileName(bean));
				}
				else
					write("<style "+type+" >"+bean.getBody()+"</style>"+getJspFileName(bean, pageContext)+"\n",pageContext, out);
			}
			else
			{
				write(bean.getBody(),pageContext, out);
			}

			listBeans.remove(i);
			i--; //NOSONAR
		}


		if(canRemoveAttribute)
		{
			//zmazeme obsah atributu, aby sa mohol znovu pouzit
			if(pageContext != null)
				pageContext.getRequest().removeAttribute(scriptType.toString());
//			else
//				Logger.debug(ReadWriteScriptTag.class, "pageContext je null nemazeme");
		}

	}

    private String removeCrLf(String set)
    {
        String newSet = Tools.replace(set, "\n", ",");
        newSet = Tools.replace(newSet, "\r", "");
        newSet = Tools.replace(newSet, "\t", "");
        return newSet;
    }

    /** metoda pre base_css_link
     *
     * @param attributeName - base_css_link
     */
    private void addContent(String attributeName)
    {
        // bud su csska v tvare /components/_common/combine.jsp?t=css&f=/templates/zsr/assets/css/bootstrap.min.css,/templates/zsr/assets/css/page.css?v=1513936639914
        // alebo su oddelene enterom
        String baseCssLink = pageContext.getRequest().getAttribute(attributeName)+"";
        if(pageContext.getRequest().getAttribute("base_css_link") != null && baseCssLink.indexOf("combine.jsp") != -1 && baseCssLink.indexOf("f=/") != -1
                && baseCssLink.indexOf("t=css") != -1 && baseCssLink.lastIndexOf(".css") > -1 && baseCssLink.length() > baseCssLink.lastIndexOf(".css"))
        {
            //Logger.debug(this,"base_css_link indexes: "+baseCssLink.substring(baseCssLink.indexOf("f=")+2,baseCssLink.lastIndexOf(".css")+4));
            for(String tokenHref:Tools.getTokens(baseCssLink.substring(baseCssLink.indexOf("f=")+2,baseCssLink.lastIndexOf(".css")+4),","))
            {
                href = tokenHref;
                addContent(ScriptTypeEnum.CSS_STYLE);
            }
        }
        else
        {
            //css oddelene enterom
            baseCssLink = removeCrLf(pageContext.getRequest().getAttribute(attributeName)+"");
            for(String tokenHref:Tools.getTokens(baseCssLink,","))
            {
                href = tokenHref;
                addContent(ScriptTypeEnum.CSS_STYLE);
            }
        }
    }

	/** Vlozi do atributu, alebo rovno vypise. Podla tagu "mode"
	 *
	 * @param name
	 */
	private void addContent(ScriptTypeEnum name)
	{
		doPrint("addContent()");
		setMode();
		ReadWriteScriptBean rwsb = new ReadWriteScriptBean(type, src, rel, href, flush, getBodyContent(), mode, media, getJspFileName(), charset, id);
		LinkedList<ReadWriteScriptBean> listRwsb = getScripts(name, null, pageContext);
		listRwsb.add(rwsb);
		pageContext.getRequest().setAttribute(name.toString(), listRwsb);

		// vypiseme a zmazeme
		if(MODE_NONE.equals(mode))
		{
			flush = "one_flush";
			try {
				doStartTag();
			} catch (JspTagException e) {
				sk.iway.iwcm.Logger.error(e);
			}
		}
	}

	/*private LinkedList<ReadWriteScriptBean> getScripts(ScriptTypeEnum name, PageContext pageContext )
	{
		return getScripts(name, null, pageContext);
	}*/

	@SuppressWarnings("unchecked")
	private static LinkedList<ReadWriteScriptBean> getScripts(ScriptTypeEnum name, HttpServletRequest request2, PageContext pageContext)
	{
		//Logger.debug(ReadWriteScriptTag.class, "getScripts()");
		HttpServletRequest request = request2;

		if(request == null)
			request = (HttpServletRequest)pageContext.getRequest();

		LinkedList<ReadWriteScriptBean> rwsb = new LinkedList<>();
		if(request.getAttribute(name.toString()) instanceof LinkedList)
		{
			rwsb = (LinkedList<ReadWriteScriptBean>)request.getAttribute(name.toString());
			if(rwsb == null)
				rwsb = new LinkedList<>();
		}
		return rwsb;
	}

	@SuppressWarnings("unused")
	private void doPrint(String method)
	{
		String attr = "x";
		if(pageContext != null && pageContext.getRequest() != null)
			attr = ""+pageContext.getRequest().getAttribute(MODE_ATTR_NAME);
		//System.out.println("Method: "+method+" mode: "+mode+" attr: "+attr+" type: "+type+" flush: "+flush);
		//System.out.println(toString());

		//spageContext.getRequest().getAttribute(MODE_ATTR_NAME).toString();
		//if("setMode()".equals(method))
			//System.out.println("- - - pageContext:"+pageContext+" request: "+pageContext.getRequest()+" getAttribute(MODE_ATTR_NAME) "+pageContext.getRequest().getAttribute(MODE_ATTR_NAME));
	}

	private static void doPrint(String method, ReadWriteScriptBean bean)
	{
		//System.out.println("Method: "+method+" mode: "+bean.getMode()+" type: "+bean.getType());
		//System.out.println(bean.toString());
	}

	private String getJspFileName()
	{
		return getJspFileName(null, pageContext);
	}

	/** Ak je v url parameter _writePerfStat=true tak vypise subor z ktoreho sa skript vkalada.
	 *
	 * @return
	 */
	private static String getJspFileName(ReadWriteScriptBean rwsb, PageContext pageContext)
	{
		if(pageContext == null)
			return"";

		String writePerfStat = ((HttpServletRequest)pageContext.getRequest()).getParameter("_writePerfStat");
		if(Tools.isNotEmpty(writePerfStat) && "true".equals(writePerfStat))
		{
			String jspName = null;
			String jspPath = null;
			if(rwsb != null)
			{
				return rwsb.getJspFileName();
			}

			jspPath = Tools.replace(pageContext.getPage().getClass().getPackage().getName(), ".", "/");
			jspName = pageContext.getPage().getClass().getSimpleName().replace("_", ".");
			jspName = Tools.replace(jspName, ".005f", "_");
			if(jspName.startsWith("."))
				jspPath = "!another file! "+jspPath+"/"+jspName.substring(1); //NOSONAR
			else
				jspPath = jspPath +"/"+ jspName; //NOSONAR

			return "<!--IS mode: "+(pageContext.getRequest().getAttribute(MODE_ATTR_NAME))+"- included from: "+jspPath+"-->\n";
		}
		return "<!--IS mode: "+(rwsb != null ? rwsb.getMode() :pageContext.getRequest().getAttribute(MODE_ATTR_NAME))+"-->\n";
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
	public String getFlush() {
		return flush;
	}
	public void setFlush(String flush) {
		this.flush = flush;
	}
	public String getHref() {
		return href;
	}
	public void setHref(String href) {
		this.href = href;
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

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	@Override
	public String toString() {
		return "ReadWriteScriptTag [type=" + type + ", src=" + src + ", rel=" + rel + ", href=" + href + ", flush="
				+ flush + ", media=" + media + ", mode=" + mode + ", charset=" + charset + ", id=" + id + "]";
	}


}