package sk.iway.iwcm.components.news.templates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.InitServlet;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.common.WriteTagToolsForCore;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesEntity;
import sk.iway.iwcm.components.news.templates.jpa.NewsTemplatesRepository;
import sk.iway.iwcm.components.translation_keys.rest.TranslationKeyService;
import sk.iway.iwcm.doc.DebugTimer;
import sk.iway.iwcm.doc.GroupsDB;
import sk.iway.iwcm.i18n.Prop;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.UpdateDatabase;

/**
 * Migrate news templates from translation keys to NewsTemplatesEntity.
 * This service is used in UpdateDatabase to migrate old templates.
 */
public class UpdateDatabaseService {

    /* OLD VERSION */
    private static final String PREFIX = "news.template.";
    private static final String PAGING_KEY = "_paging";
	private static final String PAGING_POSITION_KEY = "_paging_position";
	private static final String IMAGE_PATH = "/components/news/images";

	/**
	 * Convert translation keys with prefix "news.template." to records in news_templates table
	 */
	public static void setNewsTemplates() {
		try {
			String note = "28.07.2025 [sivan] prekonvertovanie news šablón z prekladových kľučov do news_templates tabuľky";
			if(UpdateDatabase.isAllreadyUpdated(note)) return;

			NewsTemplatesRepository ntr = Tools.getSpringBean("newsTemplatesRepository", NewsTemplatesRepository.class);
			TranslationKeyService tks = Tools.getSpringBean("translationKeyService", TranslationKeyService.class);
			if(ntr == null || tks == null) {
				Logger.error(UpdateDatabase.class, "NewsTemplatesRepository or TranslationKeyService bean not found");
				return;
			}

			DebugTimer dt = new DebugTimer("Converting news templates to db");

			//Prepare domain name - id combination map
			Map<String, Integer> domainsMap = new Hashtable<>();
			if(InitServlet.isTypeCloud() || Constants.getBoolean("enableStaticFilesExternalDir")==true) {
				for(String domainName : GroupsDB.getInstance().getAllDomainsList()) {
					domainsMap.putIfAbsent(domainName, GroupsDB.getDomainId(domainName));
				}
			} else {
				domainsMap.putIfAbsent("", CloudToolsForCore.getDomainId());
			}


			Prop prop = Prop.getInstance("sk");
			Map<String, String> templatesMap = prop.getTextStartingWith("news.template.");

			Map<String, NewsTemplatesEntity> baseTemplatesMap = getBaseNewsTemplates(templatesMap);
			List<NewsTemplatesEntity> templates = getFilledNewsTemplates(baseTemplatesMap, templatesMap);
			for (NewsTemplatesEntity template : templates) {

				if (template.getDomainId() == null || template.getDomainId() < 0) {
					//we need to save template for every domain
					for (Map.Entry<String, Integer> entry : domainsMap.entrySet()) {
						template.setId(null); // reset ID to create a new record
						template.setDomainId(entry.getValue());
						ntr.save(template);
					}
				} else {
					ntr.save(template);
				}
			}

			dt.diffInfo("DONE");

			UpdateDatabase.saveSuccessUpdate(note);

		} catch (Exception e) {
			sk.iway.iwcm.Logger.error(e);
		}
	}

	/**
	 * Loop translation keys from input and return map of NewsTemplatesEntity.
	 * The key in map is name of template (without prefix "news.template."), value is NewsTemplatesEntity.
	 * Entities in map have set only name, templateCode, engine and domainId.
	 * @param translationKeys
	 * @return
	 */
    public static Map<String, NewsTemplatesEntity> getBaseNewsTemplates(Map<String, String> templatesMap) {
        Map<String, NewsTemplatesEntity> baseTemplatesMap = new HashMap<>();
        if(templatesMap == null) return baseTemplatesMap;

        for(Map.Entry<String, String> entry : templatesMap.entrySet()) {
			String translationKey = entry.getKey();
			if(Tools.isEmpty(translationKey) == true || translationKey.startsWith(PREFIX) == false) continue;

			//Looking for main value
			if(translationKey.endsWith(PAGING_KEY) || translationKey.endsWith(PAGING_POSITION_KEY)) continue;

			String tempName = translationKey.substring(PREFIX.length());
			if(Tools.isEmpty(tempName)) continue;

			//Its base key AKA name of news template
			if(baseTemplatesMap.get(tempName) != null) {
				//PROBLEM
				Logger.error(UpdateDatabase.class, "DUPLICATE id for news template : " + tempName);
				continue;
			}

			//Prepare entity
			NewsTemplatesEntity entity = new NewsTemplatesEntity();
			entity.setName(tempName);
			entity.setTemplateCode(entry.getValue());
			entity.setEngine("velocity");

			//Set domain ID's
			if(translationKey.contains("(") == false) {
				//No dmain alias, use default domain id
				entity.setDomainId(-1);
			} else {
				//Domain allias is present, extract it
				Pattern pattern = Pattern.compile("\\(([^)]+)\\)");
        		Matcher matcher = pattern.matcher(translationKey);

				if (matcher.find()) {
					//We found domain allias, use it to get real domain id
					entity.setDomainId( CloudToolsForCore.getDomainIdByAlias( matcher.group(1) ) );
				} else {
					//Something wrong - set default domainId
					entity.setDomainId(-1);
				}
			}

			baseTemplatesMap.put(tempName, entity);
		}

        return baseTemplatesMap;
    }

    public static List<NewsTemplatesEntity> getFilledNewsTemplates(Map<String, NewsTemplatesEntity> baseTemplatesMap, Map<String, String> templatesMap) {
        List<NewsTemplatesEntity> newsTemplatesList = new ArrayList<>();
        if(baseTemplatesMap == null) return newsTemplatesList;

		//Loop through base templates map and set additional values from translation keys
        baseTemplatesMap.forEach((tempName, tempEntity) -> {
			//Loop through translation keys to find paging and image values for this template
			for(Map.Entry<String, String> entry : templatesMap.entrySet()) {
                String translationKey = entry.getKey();
                if(Tools.isEmpty(translationKey) == true || translationKey.startsWith(PREFIX) == false) continue;

                if( translationKey.equals(PREFIX + tempName + PAGING_KEY) ) {
                    // Set paging code
                    tempEntity.setPagingCode( entry.getValue() );
                } else if( translationKey.equals(PREFIX + tempName + PAGING_POSITION_KEY) ) {
                    // Set temp paging position
                    String pagingPosition = entry.getValue();
                    tempEntity.setPagingPosition( Tools.getIntValue(pagingPosition, 0) );
                } else {
                    //NOTHING
                }

            	//Set image for news temp
				tempEntity.setImagePath( getImagePath(tempName) );

			}
        });

		return new ArrayList<>( baseTemplatesMap.values() );
    }

	/**
	 * Return image path for news template based on its name.
	 * @param tempName
	 * @return
	 */
    private static String getImagePath(String tempName)
	{
		List<String> extensions = Arrays.asList("jpg", "png");

		String imagePath = "";

		for (String extension : extensions) {

			//PRUSER nemame request a bez toho nebudu vetky obrazky

			String path = IMAGE_PATH + "/" + tempName + "." + extension;
			IwcmFile imageFile = new IwcmFile(Tools.getRealPath(WriteTagToolsForCore.getCustomPage(path, null)));

			if (imageFile.isFile()) {
				imagePath = imageFile.getVirtualPath();
				break;
			}
		}

		if (Tools.isEmpty(imagePath)) {
			imagePath = IMAGE_PATH + "/placeholder-380-200.png";
		}

		return imagePath;
	}
}
