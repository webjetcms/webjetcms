package sk.iway.iwcm.components.templates;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sk.iway.iwcm.Constants;
import sk.iway.iwcm.Logger;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.database.SimpleQuery;
import sk.iway.iwcm.doc.*;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UserDetails;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TemplateDetailsService {

    private final DocDetailsRepository docDetailsRepository;

    @Autowired
    public TemplateDetailsService(DocDetailsRepository docDetailsRepository) {
        this.docDetailsRepository = docDetailsRepository;
    }

    public TemplateDetails getOne(int templateId) {
        TemplatesDB templatesDB = TemplatesDB.getInstance();
        return templatesDB.getTemplate(templateId);
    }

    public List<TemplateDetails> getAllTemplateDetails(UserDetails currentUser) {
        Map<Integer, Integer> tableOfUsage = TemplatesDB.getInstance().numberOfPages();
        TemplatesDB templatesDB = TemplatesDB.getInstance();

        List<TemplateDetails> templateDetails = filterByCurrentDomainAndUser(currentUser, templatesDB.getTemplatesSaved());

        for (TemplateDetails detail : templateDetails) {
            Integer number = tableOfUsage.get(detail.getTempId());
            if (null != number) {
                detail.setPocetPouziti(number);
            }
        }

        return templateDetails;
    }

    /**
     * Filter templates by current domain
     * @param templateDetails
     * @return
     */
    public List<TemplateDetails> filterByCurrentDomainAndUser(UserDetails currentUser, List<TemplateDetails> templateDetails) {

        templateDetails = TemplatesDB.filterTemplatesByUser(currentUser, templateDetails);

        if (Constants.getBoolean("multiDomainEnabled")) {
            //show templates available only on current domain
            List<TemplateDetails> filtered = new ArrayList<>();

            String currentDomain = CloudToolsForCore.getDomainName();
            if (Tools.isNotEmpty(currentDomain)) {
                for (TemplateDetails temp : templateDetails) {
                    List<GroupDetails> groups = temp.getAvailableGrooupsList();
                    if (groups==null || groups.isEmpty()) {
                        filtered.add(temp);
                        continue;
                    }
                    for (GroupDetails group : groups) {
                        if (Tools.isEmpty(group.getDomainName()) || currentDomain.equals(group.getDomainName())) {
                            filtered.add(temp);
                            break;
                        }
                    }
                }
            }

            templateDetails = filtered;
        }
        return templateDetails;
    }

    TemplateDetails insertTemplateDetail(TemplateDetails templateDetails) {
        try {
            templateDetails.setTempId(-1);
            TemplatesDB.getInstance().saveTemplate(templateDetails);
        } catch (Exception e) {
            Logger.error(getClass(), "Error while saving templateDetail with name " + templateDetails.getTempName() +
                    ". Error message: " + e.getMessage() + ".");
        }

        return templateDetails;
    }

    TemplateDetails editTemplateDetail(TemplateDetails templateDetails, long id) {
        templateDetails.setTempId((int) id);

        try {
            TemplatesDB.getInstance().saveTemplate(templateDetails);
        } catch (Exception e) {
            Logger.error(getClass(), "Error while editing templateDetail [" + id + ", " + templateDetails.getTempName() +
                    "]. Error message: " + e.getMessage() + ".");
        }

        return templateDetails;
    }

    boolean deleteTemplateDetails(long id) {
        boolean deleted = TemplatesDB.getInstance().remove((int) id);
        if (deleted) {
            TemplatesDB.getInstance(true);
            GroupsDB.getInstance(true);
            DocDB.getInstance(true);
        }
        return deleted;
    }

    /**
     * Vrati zoznam moznych JSP sablon pre zadanu sablonu
     * Hlada v adresaroch:
     * /templates/INSTALL_NAME/TEMPLATE_GROUP_DIR - pre taketo vrati len TEMPLATE_GROUP_DIR/cesta.jsp
     * /templates/INSTALL_NAME
     * /templates/DOMAIN_ALIAS/ - pre tieto vrati cestu aj s DOMAIN_ALIAS
     *
     * @param installNameParam - hodnota installName zadana v editore sablony
     * @param templatesGroupId - ID skupiny sablon (alebo null)
     * @param searchTerm - zadany hladany vyraz (autocomplete filter)
     * @return
     */
    public List<String> getTemplateForwards(String installNameParam, Integer templatesGroupId, String searchTerm) {
        List<String> forwards = new ArrayList<>();

        String installName = installNameParam;
        if (Tools.isEmpty(installName)) installName = Constants.getInstallName();

        File dir = new File(sk.iway.iwcm.Tools.getRealPath("/templates"));
		if (dir.isDirectory())
		{
            String templatesGroupDir = "";
            if (templatesGroupId != null && templatesGroupId.intValue()>0) templatesGroupDir = (new SimpleQuery().forString("SELECT directory FROM templates_group WHERE templates_group_id = ?", templatesGroupId));
            // nepriradena skupina ma directory = "/"
		    if ("/".equals(templatesGroupDir)) templatesGroupDir = "";

			File dirSpec = new File(sk.iway.iwcm.Tools.getRealPath("/templates/" + installName + "/" + templatesGroupDir)); //NOSONAR
			if (dirSpec.exists() && dirSpec.isDirectory()){
				dir = dirSpec;
			} else {
                dirSpec = new File(sk.iway.iwcm.Tools.getRealPath("/templates/" + templatesGroupDir));
                if (Tools.isNotEmpty(templatesGroupDir) && dirSpec.exists() && dirSpec.isDirectory()){
                    dir = dirSpec;
                } else {
                    // ak nenajde templaters/{INSTALL NAME}/{templatesGroupDir} ide o directory vyssie a resetne templatesGroupDir
                    dirSpec = new File(sk.iway.iwcm.Tools.getRealPath("/templates/"+installName));
                    templatesGroupDir = "";
                    //out.println("<br/>dir2="+dir+" dirSpec="+dirSpec+" exists="+dirSpec.exists()+" isDir="+dirSpec.isDirectory()+" templatesGroupDir="+templatesGroupDir);
                    if (dirSpec.exists() && dirSpec.isDirectory())
                        dir = dirSpec;
                }
            }

			String domainAlias = MultiDomainFilter.getDomainAlias(CloudToolsForCore.getDomainName());
            //installNameParam (from editor) owerwrites domainAlias
			if (Tools.isEmpty(installNameParam) && Tools.isNotEmpty(domainAlias) && domainAlias.equals(installName)==false)
			{
				dirSpec = new File(sk.iway.iwcm.Tools.getRealPath("/templates/"+domainAlias));
				//out.println("dir3="+dir+" dirSpec="+dirSpec+" templatesGroupDir="+templatesGroupDir);
				if (dirSpec.exists() && dirSpec.isDirectory())
				{
					templatesGroupDir = domainAlias;
					dir = dirSpec;
				}
            }

            forwards.addAll(getTemplateForwards(dir, templatesGroupDir, searchTerm));
        }

        return forwards;
    }

    /**
     * Ziska zoznam JSP suborov sablony
     * @param dir - zakladny adresar z ktoreho sa citaju JSP
     * @param baseDir - meno adresara pridaneho na zaciatok do vystupu (pretoze listujeme /templates/DOMAIN_ALIAS/ a DOMAIN_ALIAS chceme mat ako prefix v subore)
     * @return
     */
    private List<String> getTemplateForwards(File dir, String baseDir, String searchTerm)
    {
        List<String> forwards = new ArrayList<>();

        File[] files = dir.listFiles();
        if (files!=null)
        {
            int i;
            for (i = 0; i < files.length; i++)
            {
                if (!files[i].isDirectory() && files[i].canRead())
                {
                    // upravene pre zobrazenie sablon aj s koncovkou .html
                    if (files[i].getName().startsWith(".") || (files[i].getName().endsWith(".jsp")==false && files[i].getName().endsWith(".html")==false))
                    {
                        continue;
                    }
                    String fName;
                    if (Tools.isNotEmpty(baseDir)) fName = baseDir+"/"+files[i].getName();
                    else fName = files[i].getName();
                    forwards.add(fName);
                }
            }
            //pridaj podadresare
            for (i = 0; i < files.length; i++)
            {
                if (files[i].isDirectory() && files[i].canRead())
                {
                    if ("includes".equals(files[i].getName())) continue;
                    if ("node_modules".equals(files[i].getName())) continue;
                    if ("pagebuilder".equals(files[i].getName())) continue;

                    String newBaseDir = baseDir;
                    if (Tools.isNotEmpty(baseDir)) newBaseDir = baseDir + "/" + files[i].getName();
                    else newBaseDir = files[i].getName();
                    forwards.addAll(getTemplateForwards(files[i], newBaseDir, searchTerm));
                }
            }
        }

        if(forwards != null && forwards.size() > 0)
        {
            Collections.sort(forwards, new Comparator<String>()
                {
                    public int compare(String lvd1, String lvd2)
                    {
                        return lvd1.compareToIgnoreCase(lvd2);
                    }
                }
            );
        }

        if(forwards != null && forwards.size() > 0 && Tools.isNotEmpty(searchTerm) && searchTerm.equals("*")==false && searchTerm.equals("%")==false) {
            forwards = forwards.stream()
                .filter(lvb -> lvb!=null && lvb.toLowerCase().contains(searchTerm.toLowerCase()))
                .collect(Collectors.toList());
        }

        return forwards;
    }

    public void mergeTemplate(long oldTemplateId, long mergeToTempId) {
        //Replace template id in DOCUMENTS
        docDetailsRepository.replaceTemplate(mergeToTempId, oldTemplateId);
        //Replace template id in GROUPS
        (new SimpleQuery()).execute("UPDATE groups SET temp_id=? WHERE temp_id=?", mergeToTempId, oldTemplateId);
        //Delete OLD template
        deleteTemplateDetails(oldTemplateId);
    }
}
