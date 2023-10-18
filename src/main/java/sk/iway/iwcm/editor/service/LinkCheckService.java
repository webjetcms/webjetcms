package sk.iway.iwcm.editor.service;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import cvu.html.HTMLTokenizer;
import cvu.html.TagToken;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.doc.DocDetails;
import sk.iway.iwcm.doc.DocDetailsRepository;
import sk.iway.iwcm.doc.GroupDetails;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.editor.rest.LinkCheckDto;
import sk.iway.iwcm.editor.rest.LinkCheckErrorDto;
import sk.iway.iwcm.i18n.Prop;

/**
 * Functions and logic to prepare linkCheck arrays
 */
@Service
public class LinkCheckService {

    public LinkCheckService() {
		//
	}

	//Enum of posible error types
	public static enum ErrorTypes {
		PAGE_NOT_EXIST,
		DOC_NOT_AVAILABLE,
		FILE_NOT_EXIST,
		IMAGE_NOT_EXIST,
		LINK_NOT_EXIST
	}

    private String stripParameters(String url) {
		//Remove /thumb/
		if (url.indexOf("/thumb/") != -1)
            url = Tools.replace(url, "/thumb", "");

		int i = url.indexOf('?');
		int j = url.indexOf('#');

		if (i==-1 && j==-1) return url;

		try {
			if (i!=-1) url = url.substring(0, i);
			if (j!=-1) url = url.substring(0, j);
		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}

		return url;
	}

	/**
	 * Check doc and link to check if any error occurs. Return list of found errors.
	 * @param doc doc to check
	 * @param docDB DocDB instance
	 * @param groupsDB GroupsDB instance
	 * @param prop Prop instance
	 * @return List of LinkCheckErrorDto objects, evry object represend one found specific error (if no error was found, return instance of empty List)
	 */
    private List<LinkCheckErrorDto> checkLinks(DocDetails doc, DocDB docDB, GroupsDB groupsDB, Prop prop) {
		if(doc.getData() == null) return null;

		List<LinkCheckErrorDto> errors = new ArrayList<>();
		HTMLTokenizer htmlTokenizer = new HTMLTokenizer(Tools.replace(doc.getData(), "/>", ">").toCharArray());

        //HTMLTree htmlTree = new HTMLTree(htmlTokenizer);
		@SuppressWarnings("unchecked")
		Enumeration<Object> e = htmlTokenizer.getTokens();
		TagToken tagToken;
		Object o;
		String tagName;
		String href;
		String src;
		int docId;
		while (e.hasMoreElements()) {
			o = e.nextElement();
			if (o instanceof TagToken) {

				tagToken = (TagToken) o;
				tagName = tagToken.getName();
				if (tagName == null || tagToken.isEndTag()) continue;

				href = tagToken.getAttribute("href");
				if (href != null && href.startsWith("#")==false) {

					if (href.equalsIgnoreCase("javascript:")) {
						//
					} else if (href.toLowerCase().startsWith("http://") || href.toLowerCase().startsWith("https://")) {
                        //External link
						//Its OK, we dont know check that
					} else if (href.toLowerCase().startsWith("javascript:")) {
						//Its OK, we dont know check that
					} else if (href.toLowerCase().startsWith("mailto:")) {
						//Its OK, we dont know check that
					} else if (href.toLowerCase().startsWith("#") && href.length() > 1) {
						//Its OK, we dont know check that
					} else if (href.toLowerCase().equals("/")) {
						//Its OK, we dont know check that
					} else if (href.startsWith("/showdoc.do")) {
                        //Get docId param
						docId = Tools.getIntValue(Tools.getParameterFromUrl(href, "docid"), -1);
						DocDetails checkDoc = docDB.getBasicDocDetails(docId, false);

						if (checkDoc == null)
							errors.add(new LinkCheckErrorDto(ErrorTypes.PAGE_NOT_EXIST, href));
						else if (checkDoc.isAvailable() == false)
							errors.add(new LinkCheckErrorDto(ErrorTypes.DOC_NOT_AVAILABLE, href));
					} else {
						href = stripParameters(href);
						//Check in DocDB
						String domain = null;
						GroupDetails group = groupsDB.getGroup(doc.getGroupId());

						if (group != null) domain = group.getDomainName();

						docId = docDB.getVirtualPathDocId(href, domain);

						if (docId < 0) {
							if (FileTools.isFile(href)==false)
							{
								errors.add(new LinkCheckErrorDto(isFileOrLink(href), href));
							}
						} else {
							DocDetails checkDoc = docDB.getBasicDocDetails(docId, false);

                            if (checkDoc == null) errors.add(new LinkCheckErrorDto(ErrorTypes.PAGE_NOT_EXIST, href));
							else if (checkDoc.isAvailable() == false) errors.add(new LinkCheckErrorDto(ErrorTypes.DOC_NOT_AVAILABLE, href));
						}
					}
				}

				src = tagToken.getAttribute("src");
				if (Tools.isNotEmpty(src)) {

					if (src.toLowerCase().startsWith("http://") || src.toLowerCase().startsWith("https://")) {
						//We dont know
					} else {
						src = stripParameters(src);

                        //Check if exist on disc
						if (FileTools.isFile(src) == false)
							errors.add(new LinkCheckErrorDto(isFileOrLink(src), src));
					}
				}
			}
		}

		return(errors);
	}

	/**
	 * Transform input DocDetails object to LinkCheckDto object.
	 * @param doc DocDetails object to transform
	 * @param error string of error to by add into LinkCheckDto class
	 * @param link string of url of page to by add into LinkCheckDto class
	 * @return new LinkCheckDto object created from entered params (if entered doc is null, null will be returned)
	 */
	private LinkCheckDto docDetailsToLinkCheck(DocDetails doc, String error, String link) {
		if(doc == null) return null;

		LinkCheckDto linkCheck = new LinkCheckDto();
		linkCheck.setId(doc.getId());
		linkCheck.setPage(doc.getFullPath());
		linkCheck.setError(error);
		linkCheck.setLink(link);

		return linkCheck;
	}

	/**
	 * Get error specification text from translate key, based on input ErrorType
	 * @param errorType represents which error specification text we want
	 * @param prop Prop instance
	 * @return string error specification text from translate key (if entered ErrorType is not mentioned in function, return empty string "")
	 */
	private String getErrorTextFromEnum(ErrorTypes errorType, Prop prop) {
		if(errorType == ErrorTypes.DOC_NOT_AVAILABLE) return prop.getText("linkcheck.doc_not_available");
		else if(errorType == ErrorTypes.FILE_NOT_EXIST) return prop.getText("linkcheck.file_not_exist");
		else if(errorType == ErrorTypes.IMAGE_NOT_EXIST) return prop.getText("linkcheck.image_not_exist");
		else if(errorType == ErrorTypes.LINK_NOT_EXIST) return prop.getText("linkcheck.link_not_exist");
		else if(errorType == ErrorTypes.PAGE_NOT_EXIST) return prop.getText("linkcheck.page_not_exist");
		else return "";
	}

	/**
	 * Get DocDetails under entered group and DocDetails of child groups. For every DocDetails run error test (link check). If error was found,
	 *  create LinkCheckDto object that represent specific error of specifi DocDetails and divide them into one of List of errors. Return
	 * specific list based on entered tableType param.
	 * @param groupId id of group, whose DocDetails will be checked (even every child group)
	 * @param tableType which one of assembled Lists of errors will be returned (if entered tableType is not mentioned in function, return instance of empty List)
	 * @param docDetailsRepository DocDetailsRepository instance
	 * @return List of LinkCheckDto objects, based on entered groupId and tableType choise
	 */
	public List<LinkCheckDto> linkCheckList(int groupId, String tableType, DocDetailsRepository docDetailsRepository) {
		GroupsDB groupsDB = GroupsDB.getInstance();
		DocDB docDB = DocDB.getInstance();
        Prop prop = Prop.getInstance();

		List<LinkCheckDto> emptyPageList = new ArrayList<>();
		List<LinkCheckDto> brokenLinksList = new ArrayList<>();
		List<LinkCheckDto> hiddenPageList = new ArrayList<>();
        List<LinkCheckErrorDto> errors;

		int linkCheckEmptyPageSize = Constants.getInt("linkCheckEmptyPageSize");
		String searchGroups = null;

        //najdi child grupy
		for (GroupDetails group : groupsDB.getGroupsTree(groupId, true, true)) {
			if (group != null) {
				if (searchGroups == null)
				    searchGroups = Integer.toString(group.getGroupId());
				else
				    searchGroups += "," + group.getGroupId();
			}
		}

        List<DocDetails> posibleValues = docDetailsRepository.findAllByGroupIdIn(Tools.getTokensInt(searchGroups, ","), Pageable.ofSize(500)).getContent();

        for(DocDetails doc : posibleValues) {
            errors = checkLinks(doc, docDB, groupsDB, prop);

			for(LinkCheckErrorDto error : errors) {
				ErrorTypes errorType = error.getErrorType();
				if(errorType == ErrorTypes.DOC_NOT_AVAILABLE)
					hiddenPageList.add(docDetailsToLinkCheck(doc, getErrorTextFromEnum(errorType, prop), error.getLink())); //ak ma stranka zakazane zobrazovanie -> iba pre odkaz - ak sa zo stranky odkazuje na stranku so zakazanym zobrazovanim
				else
					brokenLinksList.add(docDetailsToLinkCheck(doc, getErrorTextFromEnum(errorType, prop), error.getLink()));
			}

			//vynecham stranky s nastavenym externym odkazom
            if(Tools.isEmpty(doc.getExternalLink()) && (doc.getData() == null || doc.getData().length() < linkCheckEmptyPageSize)) {
                String errorText = "";
                if(doc.getData() == null) errorText = prop.getText("linkcheck.page_probably_empty");
                else errorText = prop.getText("linkcheck.page_probably_empty") + ": " + doc.getData().length() + " " + prop.getText("linkcheck.chars");

                emptyPageList.add(docDetailsToLinkCheck(doc, errorText, doc.getVirtualPath()));
            }
        }

        if(tableType.equals("brokenLinks")) return brokenLinksList;
        else if(tableType.equals("hiddenPages")) return hiddenPageList;
        else if(tableType.equals("emptyPages")) return emptyPageList;

        return new ArrayList<>();
    }

	/**
	 * By href link determines if it's probably File or general link
	 * @param href - URL path
	 * @return
	 */
	private ErrorTypes isFileOrLink(String href) {
		if (href == null) return ErrorTypes.LINK_NOT_EXIST;
		if (href.startsWith("/images") || href.startsWith("/shared/images")) return ErrorTypes.IMAGE_NOT_EXIST;
		if (href.startsWith("/files") || href.startsWith("/shared")) return ErrorTypes.FILE_NOT_EXIST;
		return ErrorTypes.LINK_NOT_EXIST;
	}
}
