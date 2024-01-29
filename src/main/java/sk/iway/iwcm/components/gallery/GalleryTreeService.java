package sk.iway.iwcm.components.gallery;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import sk.iway.iwcm.Constants;
import sk.iway.iwcm.FileTools;
import sk.iway.iwcm.Identity;
import sk.iway.iwcm.Tools;
import sk.iway.iwcm.common.CloudToolsForCore;
import sk.iway.iwcm.doc.DocDB;
import sk.iway.iwcm.io.IwcmFile;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.ConstantsV9;
import sk.iway.iwcm.system.multidomain.MultiDomainFilter;
import sk.iway.iwcm.users.UsersDB;

@Service
@RequestScope
public class GalleryTreeService {

    private final GalleryDimensionRepository repository;
    private HttpServletRequest request;

    public GalleryTreeService(GalleryDimensionRepository repository, HttpServletRequest request) {
        this.repository = repository;
        this.request = request;
    }

    /**
     * Get List of GalleryJsTreeItem for specified URL address (e.g. /images/gallery)
     * @param url
     * @return
     */
    public List<GalleryJsTreeItem> getItems(String url) {
        IwcmFile directory = new IwcmFile(Tools.getRealPath(url));
        final String urlFinal = url;
        List<IwcmFile> files = Arrays.asList(directory.listFiles(file -> {
            if (!file.isDirectory()) {
                return false;
            }
            Set<String> blacklistedNames = getBlacklistedNames();
            //odstran domenove aliasy z inych domen
            if (blacklistedNames.size()>0 && blacklistedNames.contains(file.getName())) return false;

            //toto chceme vzdy
            if ("gallery".equals(file.getName())) return true;
            if (file.getVirtualPath().contains("gallery")) return true;

            //ak ma /images/tento-priecinok podpriecinok gallery tiez ho pridaj (testuje sa len pre prvu uroven)
            if ("/images".equals(urlFinal) && FileTools.isDirectory(urlFinal+"/"+file.getName()+"/gallery")) return true;

            //ak je nastaveny GalleryDimension povazuj to tiez za galeriu
            Optional<GalleryDimension> gallery = repository.findFirstByPathLikeAndDomainId(urlFinal+"/"+file.getName()+"%", CloudToolsForCore.getDomainId());
            if (gallery.isPresent()) return true;

            return false;
        }));

        files = FileTools.sortFilesByName(files);

        String dir = getRequest().getParameter("dir");
        Identity user = UsersDB.getCurrentUser(getRequest());

        List<GalleryJsTreeItem> items = files.stream().map(f -> new GalleryJsTreeItem(f, dir, repository, user)).collect(Collectors.toList());

        return items;
    }

    /**
     * Black listed names are all domain aliases except current domain alias
     * It's to hide other than current domain alias in multidomain setup (without external folders)
     * @return
     */
    private Set<String> getBlacklistedNames() {
        Set<String> blacklistedNames = new HashSet<>();
        if (Constants.getBoolean("multiDomainEnabled")) {
            String domainAlias = MultiDomainFilter.getDomainAlias(DocDB.getDomain(getRequest()));
            if (Tools.isNotEmpty(domainAlias)) {
                //blacklistni ostatne aliasy
                List<ConfDetails> aliases = ConstantsV9.getValuesStartsWith("multiDomainAlias:");
                for (ConfDetails conf : aliases) {
                    String alias = conf.getValue();
                    if (Tools.isNotEmpty(alias) && alias.equals(domainAlias)==false) blacklistedNames.add(alias);
                }
            }
        }
        return blacklistedNames;
    }

    private HttpServletRequest getRequest() {
        return request;
    }

}
