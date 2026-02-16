package sk.iway.iwcm.sync.export;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.GalleryToolsForCore;
import sk.iway.iwcm.components.banner.model.BannerBean;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.gallery.GalleryBean;
import sk.iway.iwcm.gallery.GalleryDB;
import sk.iway.iwcm.gallery.GalleryDimension;
import sk.iway.iwcm.inquiry.AnswerForm;
import sk.iway.iwcm.inquiry.InquiryBean;
import sk.iway.iwcm.io.IwcmFile;

/**
 * Callback objekt na pridavanie liniek zo stranok.
 * Jednotlive exportery mu povedia, ze treba pridat linku,
 * on si pamata kam a pre ktoru stranku. 
 *
 *@Title        webjet7
 *@Company      Interway s.r.o. (www.interway.sk)
 *@Copyright    Interway s.r.o. (c) 2001-2012
 *@author       $Author: jeeff vbur $
 *@version      $Revision: 1.3 $
 *@created      Date: 11.6.2012 9:37:24
 *@modified     $Date: 2004/08/16 06:26:11 $
 */
public class ContentBuilder
{

	// napriklad: !INCLUDE(/components/xxx/yyy.jsp, param1=value1, param2=value2)!
	final static Pattern COMPONENT = Pattern.compile("!INCLUDE\\((.*?)\\)!");

	// HTML atributy obsahujuce URL
	// Zdroj: http://www.w3.org/TR/2012/WD-html5-20120329/section-index.html#attributes-1
	final static Pattern[] HTML_ATTRIBUTES = {
		attribute("action",     "form"),
		attribute("cite",       "blockquote", "del", "ins", "q"),
		attribute("data",       "object"),
		attribute("formaction", "button", "input"),
		attribute("href",       "a", "area", "base", "link"),
		attribute("icon",       "command"),
		attribute("manifest",   "html"),
		attribute("poster",     "video"),
		attribute("src",        "audio", "embed", "iframe", "img", "input", "script", "source", "track", "video"),
	};

	// HTML prvok STYLE
	final static Pattern STYLE = Pattern.compile("<style(\\s[^>]*)>(.*?)</style>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	// Flash video SWF ma subor zakodovany v parametri.
	final static Pattern HTML_PARAM_VALUE = attribute("value", "param");
	final static String FLASH_PRELOAD = "/components/_common/preload.swf?path=";

	private final Content content;

	private final String baseHref;

	private final HttpServletRequest request;

	private Integer docId = null;

	/**
	 * Vytvori callback objekt na pridavanie suborov z danej stranky do daneho kontentu.
	 * 
	 * @param content  objekt, do ktoreho zapisujeme subory urcene na export
	 */
	public ContentBuilder(Content content, HttpServletRequest request)
	{
		this.content = content;
		this.baseHref = Tools.getBaseHref(request);
		this.request = request;
	}

	/**
	 * Nastavi dokument, pre ktory exportujeme veci.
	 * Treba volat pred exportom obsahu stranky, a potom pre istotu zase nastavit null.
	 * <pre>
	 * callback.setDoc(doc);
	 * callback.add... // linky, beany
	 * callback.setDoc(null);
	 * </pre>
	 * 
	 * @param doc
	 */
	public void setDoc(DocDetails doc)
	{
		docId = (null == doc) ? null : Integer.valueOf(doc.getDocId());
	}

	private void checkDoc()
	{
		if (null == docId)
		{
			throw new IllegalStateException("ContentBuilder.setDoc must be called first");
		}
	}

	public void addBanner(BannerBean banner)
	{
		checkDoc();
		List<BannerBean> banners = content.getBanners();
		for (BannerBean b : banners)
		{
			if (Tools.areSame(banner.getBannerGroup(), b.getBannerGroup()) && Tools.areSame(banner.getName(), b.getName()))
			{
				return;  // rovnaka skupina aj nazov = rovnaky banner
			}
		}
		banners.add(banner);
	}

	public void addInquiry(InquiryBean inquiry)
	{
		checkDoc();

		List<AnswerForm> answers = inquiry.getAnswers();
		if (answers.isEmpty()) return;  // bez odpovedi nevieme zistit skupinu :(
		List<InquiryBean> inquiries = content.getInquiries();
		for (InquiryBean i : inquiries)
		{
			if (Tools.areSame(inquiry.getQuestion(), i.getQuestion()))
			{
				List<AnswerForm> a = i.getAnswers();
				if (Tools.areSame(answers.get(0).getGroup(), a.get(0).getGroup()))
				{
					return;  // rovnaka skupina aj otazka = rovnaka anketa
				}
			}
		}
		inquiries.add(inquiry);
	}

	/**
	 * Prida obrazok z galerie.
	 * Kedze GalleryBean obsahuje iba popis v jednom jazyku, ako parameter dame vsetky preklady.
	 * 
	 * @param translations  mapa GalleryBean pre jednotlive jazyky
	 */
	public void addGalleryImage(Map<String, GalleryBean> translations)
	{
		checkDoc();

		GalleryBean i = translations.values().iterator().next();
		if (null == i) return;  // prazdna mapa

		List<Map<String, GalleryBean>> galleryImages = content.getGalleryImages();
		for (Map<String, GalleryBean> image : galleryImages)
		{
			GalleryBean img = image.values().iterator().next();
			if (Tools.areSame(img.getImageUrl(), i.getImageUrl()))
			{
				return;  // rovnake URL = rovnaky obrazok
			}
		}
		galleryImages.add(translations);
	}

	/**
	 * Prida adresar z galerie.
	 * 
	 * @param dimension
	 */
	public void addGalleryInfo(GalleryDimension dimension, Dimension dim, Dimension dimNormal)
	{
		checkDoc();

		List<Content.GalleryInfo> galleryInfos = content.getGalleryInfos();
		for (Content.GalleryInfo info : galleryInfos)
		{
			if (Tools.areSame(info.getInfo().getGalleryPath(), dimension.getGalleryPath()))
			{
				return;  // rovnake URL = rovnaky adresar
			}
		}
		Content.GalleryInfo info = new Content.GalleryInfo();
		info.setInfo(dimension);
		info.setDim(dim);
		info.setDimNormal(dimNormal);
		galleryInfos.add(info);
	}
	
	// TODO: prida veci vnutri elementu STYLE, alebo linkovane cez STYLE SRC=...
	public void addCss(String data)
	{
		//...
	}

	/**
	 * Prida vsetko, co je v danom HTML kode; cize vyberie linky z tela stranky.
	 */
	public void addHtml(String data)
	{
		if (null == data) return;

		for (Pattern pattern : HTML_ATTRIBUTES)
		{
			Matcher m = pattern.matcher(data);
			while (m.find())
			{
				addLink(m.group(2));
			}
		}

		Matcher m = COMPONENT.matcher(data);
		while (m.find())
		{
			ComponentExporter exporter = new DefaultComponentExporterResolver().forInclude(m.group(1));
			if (null != exporter)
			{
				exporter.export(this);
			}
		}

		m = HTML_PARAM_VALUE.matcher(data);
		while (m.find())
		{
			String value = m.group(2);
			if (value.startsWith(FLASH_PRELOAD))
			{
				addLink(value.substring(FLASH_PRELOAD.length()));
			}
		}

	}

	/**
	 * Prida linku, ak vyhovuje danym pravidlam.
	 * Nie vsetko, co vyzera ako linka, je naozaj linka, moze to byt napriklad Javascript alebo e-mail.
	 * Linky mimo servera ignorujeme.
	 * 
	 * @param link  potencialna linka v HTML, napriklad obsah atributu A HREF
	 */
	public void addLink(String link)
	{
		checkDoc();

		if (null == link) return;

		if (link.contains("..")) return;  // security

		// zrusime vsetko co nezacina "/" alebo "http://$NAS_SERVER/"
		String virtualPath = null;
		if (link.startsWith("/"))
		{
			virtualPath = link;
		}
		else if (link.startsWith(baseHref))
		{
			virtualPath = link.substring(baseHref.length());
		}
		if (null == virtualPath) return;

		// zrusime anchor "#"
		int pos = virtualPath.indexOf('#');
		if (0 <= pos)
		{
			virtualPath = virtualPath.substring(0, pos);
		}
		//zrusime ?v=timestamp
		pos = virtualPath.indexOf("?v=");
		if (0 <= pos)
		{
			virtualPath = virtualPath.substring(0, pos);
		}

		if (Tools.isEmpty(virtualPath)) return;

		if ("/".equals(virtualPath)) return;
		
		String realPath = Tools.getRealPath(virtualPath);
		IwcmFile file = new IwcmFile(realPath);
		if (!file.exists() || file.isDirectory()) return;  // stranka alebo zla linka

		if (GalleryDB.isGalleryFolder(virtualPath))
		{
			addDocFile(GalleryToolsForCore.getImagePathSmall(virtualPath));
			addDocFile(GalleryToolsForCore.getImagePathNormal(virtualPath));
			addDocFile(GalleryToolsForCore.getImagePathOriginal(virtualPath));
		}
		else
		{
			addDocFile(virtualPath);
		}
		
		//ak pridavam odkaz na video subor treba pridat aj jpg nahlad
		if (link.endsWith(".mp4") || link.endsWith(".flv"))
		{
			//otestuj a pridaj odkaz na nahladovy obrazok
			String jpgUrl = link.substring(0, link.lastIndexOf("."))+".jpg";
			if (FileTools.isFile(jpgUrl))
			{
				Logger.debug(DocExporter.class, "Adding media jpg: "+jpgUrl);
				addLink(jpgUrl);
			}
		}
	}

	// prida subor do "docFiles"
	private void addDocFile(String virtualPath)
	{
		Map<Integer, Collection<String>> docFiles = content.getDocFiles();
		Collection<String> thisDocFiles = docFiles.get(docId);
		if (null == thisDocFiles)
		{
			thisDocFiles = new ArrayList<String>();
			docFiles.put(docId, thisDocFiles);
		}
		thisDocFiles.add(virtualPath);
		if (!content.containsFile(virtualPath))
		{
			addFile(virtualPath);
		}
	}

	// prida subor do "files"
	private void addFile(String virtualPath)
	{
		List<Content.File> files = content.getFiles();
		int number = files.size();
		Content.File file = new Content.File();
		file.setVirtualPath(virtualPath);
		file.setZipPath(zipEntryName(number, virtualPath));
		files.add(file);
	}

	public HttpServletRequest getRequest()
	{
		return request;
	}

	/**
	 * Vymysli novy nazov pre subor, podobny na stary, ale jedinecny.
	 * @param number       jedinecne cislo
	 * @param virtualPath  virtualna cesta k povodnemu suboru
	 * @return
	 */
	static String zipEntryName(int number, String virtualPath)
	{
		int pos = virtualPath.lastIndexOf('/');
		String filename = (0 <= pos) ? virtualPath.substring(pos + 1) : virtualPath;
		return String.format("%04d_%s", Integer.valueOf(number), filename);
	}

	/**
	 * Vytvori regular expression pre dany HTML atribut.
	 * 
	 * @param attribute  atribut
	 * @param element    zoznam elementov, v ktorych sa atribut moze vyskytovat (prazdny ak v lubovolnom)
	 * @return
	 */
	private static Pattern attribute(String attribute, String... element)
	{
		String elementRegex = (0 == element.length) ? "\\w+" : StringUtils.join(element, "|");
		String regex = "<(" + elementRegex + ")\\s[^>]*\\b" + attribute + "=[\"']([^\"']*)[\"'][^>]*>";
		return Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	}

}
